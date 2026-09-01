[CmdletBinding()]
param(
    [string]$Serial,
    [ValidateSet(
        "all",
        "preflight",
        "baseline",
        "controlled-write",
        "foreground-observation",
        "force-stop-reopen",
        "instrumented-suite",
        "capture-only"
    )]
    [string[]]$Scenario = @("all"),
    [switch]$SkipInstall,
    [switch]$AllowDirtySource,
    [ValidateRange(5, 120)]
    [int]$WaitTimeoutSeconds = 20
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$script:RepositoryRoot = Split-Path -Parent $PSScriptRoot
$script:PackageName = "com.soturine.volumeok"
$script:ActivityName = "com.soturine.volumeok/.MainActivity"
$script:Outcomes = [System.Collections.Generic.List[object]]::new()
$script:Warnings = [System.Collections.Generic.List[string]]::new()
$script:ManualChecks = [System.Collections.Generic.List[string]]::new()
$script:InstrumentationResult = $null
$script:StateRestorationFailed = $false
$script:ArtifactRoot = $null
$script:ApkPath = $null
$script:ApkHash = $null
$script:SourceSha = $null
$script:OriginSha = $null
$script:SourceDirty = $false
$script:Device = $null
$script:AdbPath = $null

function Write-Utf8File {
    param(
        [Parameter(Mandatory)] [string]$Path,
        [Parameter(Mandatory)] [AllowEmptyString()] [string]$Content
    )

    $parent = Split-Path -Parent $Path
    if ($parent -and -not (Test-Path -LiteralPath $parent)) {
        New-Item -ItemType Directory -Path $parent -Force | Out-Null
    }
    [System.IO.File]::WriteAllText($Path, $Content, [System.Text.UTF8Encoding]::new($false))
}

function Invoke-NativeText {
    param(
        [Parameter(Mandatory)] [string]$FilePath,
        [Parameter(Mandatory)] [string[]]$Arguments,
        [switch]$AllowFailure
    )

    $outputLines = & $FilePath @Arguments 2>&1
    $exitCode = $LASTEXITCODE
    $output = ($outputLines | Out-String).TrimEnd()
    if ($exitCode -ne 0 -and -not $AllowFailure) {
        throw "Command failed ($exitCode): $FilePath $($Arguments -join ' ')`n$output"
    }
    [pscustomobject]@{
        ExitCode = $exitCode
        Output = $output
    }
}

function Invoke-AdbText {
    param(
        [Parameter(Mandatory)] [string[]]$Arguments,
        [switch]$AllowFailure
    )

    $adbArguments = @("-s", $script:Device.Serial) + $Arguments
    Invoke-NativeText -FilePath $script:AdbPath -Arguments $adbArguments -AllowFailure:$AllowFailure
}

function Invoke-AdbBinaryScreenshot {
    param([Parameter(Mandatory)] [string]$Path)

    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $script:AdbPath
    $startInfo.Arguments = "-s $($script:Device.Serial) exec-out screencap -p"
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true

    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $startInfo
    if (-not $process.Start()) {
        throw "ADB screenshot process could not start."
    }

    $fileStream = [System.IO.File]::Open($Path, [System.IO.FileMode]::Create)
    try {
        $process.StandardOutput.BaseStream.CopyTo($fileStream)
    } finally {
        $fileStream.Dispose()
    }
    $errorText = $process.StandardError.ReadToEnd()
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) {
        throw "ADB screenshot failed ($($process.ExitCode)): $errorText"
    }
    if ((Get-Item -LiteralPath $Path).Length -lt 8) {
        throw "ADB screenshot was empty or invalid: $Path"
    }
}

function Resolve-AdbPath {
    $candidates = @()
    if ($env:ANDROID_HOME) {
        $candidates += Join-Path $env:ANDROID_HOME "platform-tools\adb.exe"
    }
    if ($env:ANDROID_SDK_ROOT) {
        $candidates += Join-Path $env:ANDROID_SDK_ROOT "platform-tools\adb.exe"
    }
    if ($env:LOCALAPPDATA) {
        $candidates += Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
    }
    $command = Get-Command adb.exe -ErrorAction SilentlyContinue
    if ($command) {
        $candidates += $command.Source
    }
    $resolved = $candidates | Where-Object { $_ -and (Test-Path -LiteralPath $_) } | Select-Object -First 1
    if (-not $resolved) {
        throw "ADB was not found. Install Android SDK Platform Tools or set ANDROID_HOME."
    }
    (Resolve-Path -LiteralPath $resolved).Path
}

function Select-AdbDevice {
    param([string]$RequestedSerial)

    $result = Invoke-NativeText -FilePath $script:AdbPath -Arguments @("devices", "-l")
    $entries = @()
    foreach ($line in ($result.Output -split "`r?`n")) {
        if ($line -match "^(\S+)\s+(device|offline|unauthorized)(?:\s+.*)?$") {
            $entries += [pscustomobject]@{ Serial = $Matches[1]; State = $Matches[2]; Line = $line }
        }
    }
    if ($RequestedSerial) {
        $selected = $entries | Where-Object { $_.Serial -eq $RequestedSerial } | Select-Object -First 1
        if (-not $selected) {
            throw "Device '$RequestedSerial' was not listed by ADB. Run 'adb devices -l' and reconnect it."
        }
    } else {
        if ($entries.Count -eq 0) {
            throw "No ADB device is connected. Connect a phone, enable USB debugging, and authorize this computer."
        }
        if ($entries.Count -gt 1) {
            throw "Multiple ADB devices are present. Re-run with -Serial <adb-serial>."
        }
        $selected = $entries[0]
    }
    if ($selected.State -eq "unauthorized") {
        throw "Device '$($selected.Serial)' is unauthorized. Unlock it and accept the USB debugging prompt."
    }
    if ($selected.State -eq "offline") {
        throw "Device '$($selected.Serial)' is offline. Reconnect USB or restart the ADB connection."
    }
    $selected
}

function Get-DeviceProperty {
    param([Parameter(Mandatory)] [string]$Name)
    (Invoke-AdbText -Arguments @("shell", "getprop", $Name)).Output.Trim()
}

function ConvertTo-Slug {
    param([Parameter(Mandatory)] [string]$Value)
    (($Value.ToLowerInvariant() -replace "[^a-z0-9]+", "-").Trim("-"))
}

function Assert-SourcePreflight {
    $fetch = Invoke-NativeText -FilePath "git" -Arguments @("fetch", "origin", "main", "--quiet") -AllowFailure
    if ($fetch.ExitCode -ne 0) {
        throw "Could not refresh origin/main. Check Git credentials/network before an evidence run.`n$($fetch.Output)"
    }
    $script:SourceSha = (Invoke-NativeText -FilePath "git" -Arguments @("rev-parse", "HEAD")).Output.Trim()
    $script:OriginSha = (Invoke-NativeText -FilePath "git" -Arguments @("rev-parse", "origin/main")).Output.Trim()
    $dirty = (Invoke-NativeText -FilePath "git" -Arguments @("status", "--porcelain")).Output
    if ($script:SourceSha -ne $script:OriginSha) {
        throw "HEAD ($($script:SourceSha)) does not match origin/main ($($script:OriginSha)). Push or synchronize first."
    }
    $script:SourceDirty = [bool]$dirty
    if ($script:SourceDirty -and -not $AllowDirtySource) {
        throw "The source tree is dirty. Commit/stash changes or use -AllowDirtySource only for harness development."
    }
    if ($script:SourceDirty) {
        $script:Warnings.Add("Source tree was dirty; this run is not qualification evidence.")
    }
}

function Initialize-Run {
    $manufacturer = Get-DeviceProperty "ro.product.manufacturer"
    $model = Get-DeviceProperty "ro.product.model"
    $script:Device = [pscustomobject]@{
        Serial = $script:Device.Serial
        Manufacturer = $manufacturer
        Model = $model
        Android = Get-DeviceProperty "ro.build.version.release"
        Api = Get-DeviceProperty "ro.build.version.sdk"
        Build = Get-DeviceProperty "ro.build.display.id"
    }
    $timestamp = [DateTime]::UtcNow.ToString("yyyyMMddTHHmmssZ")
    $deviceSlug = "$(ConvertTo-Slug $manufacturer)-$(ConvertTo-Slug $model)"
    $script:ArtifactRoot = Join-Path $script:RepositoryRoot "artifacts\m0\$timestamp-$deviceSlug"
    foreach ($child in @("dumpsys", "screenshots", "ui", "logcat", "instrumentation")) {
        New-Item -ItemType Directory -Path (Join-Path $script:ArtifactRoot $child) -Force | Out-Null
    }

    $deviceText = @"
serial=$($script:Device.Serial)
manufacturer=$($script:Device.Manufacturer)
model=$($script:Device.Model)
android=$($script:Device.Android)
api=$($script:Device.Api)
build=$($script:Device.Build)
"@
    Write-Utf8File -Path (Join-Path $script:ArtifactRoot "device.txt") -Content $deviceText.Trim()

    $sourceText = @"
head=$($script:SourceSha)
origin_main=$($script:OriginSha)
clean=$(-not $script:SourceDirty)
"@
    Write-Utf8File -Path (Join-Path $script:ArtifactRoot "source.txt") -Content $sourceText.Trim()
}

function Add-Outcome {
    param(
        [Parameter(Mandatory)] [string]$Name,
        [Parameter(Mandatory)]
        [ValidateSet("PASS", "FAIL", "PARTIAL", "SKIPPED", "MANUAL_REQUIRED", "UNSUPPORTED")]
        [string]$Outcome,
        [Parameter(Mandatory)] [string]$Method,
        [Parameter(Mandatory)] [string]$Reason,
        [string[]]$Evidence = @()
    )

    $script:Outcomes.Add([pscustomobject]@{
        Scenario = $Name
        Outcome = $Outcome
        Method = $Method
        Reason = $Reason
        Evidence = $Evidence
    })
    Write-Output "[$Outcome] $Name - $Reason"
}

function Add-ManualCheck {
    param([Parameter(Mandatory)] [string]$Text)
    if (-not $script:ManualChecks.Contains($Text)) {
        $script:ManualChecks.Add($Text)
    }
}

function Build-And-Install {
    $gradlePath = Join-Path $script:RepositoryRoot "gradlew.bat"
    $build = Invoke-NativeText -FilePath $gradlePath -Arguments @("assembleDebug", "--console=plain")
    Write-Utf8File -Path (Join-Path $script:ArtifactRoot "build-output.txt") -Content $build.Output

    $apkPath = Join-Path $script:RepositoryRoot "app\build\outputs\apk\debug\app-debug.apk"
    if (-not (Test-Path -LiteralPath $apkPath)) {
        throw "Debug APK was not created at $apkPath"
    }
    $script:ApkPath = $apkPath
    $script:ApkHash = (Get-FileHash -LiteralPath $apkPath -Algorithm SHA256).Hash
    Write-Utf8File -Path (Join-Path $script:ArtifactRoot "apk-sha256.txt") -Content "$($script:ApkHash)  app-debug.apk"

    if (-not $SkipInstall) {
        $install = Invoke-AdbText -Arguments @("install", "-r", $apkPath)
        Write-Utf8File -Path (Join-Path $script:ArtifactRoot "install-output.txt") -Content $install.Output
        if ($install.Output -notmatch "Success") {
            throw "ADB install did not report success: $($install.Output)"
        }
    }
}

function Get-RingVolumeState {
    $output = (Invoke-AdbText -Arguments @("shell", "cmd", "media_session", "volume", "--stream", "2", "--get")).Output
    if ($output -notmatch "volume is\s+(\d+)\s+in range\s+\[(\d+)\.\.(\d+)\]") {
        throw "Could not parse STREAM_RING state: $output"
    }
    [pscustomobject]@{
        Current = [int]$Matches[1]
        Minimum = [int]$Matches[2]
        Maximum = [int]$Matches[3]
        Raw = $output
    }
}

function Set-RingVolumeState {
    param([Parameter(Mandatory)] [int]$Value)
    Invoke-AdbText -Arguments @(
        "shell", "cmd", "media_session", "volume", "--stream", "2", "--set", $Value.ToString()
    ) | Out-Null
}

function Wait-Until {
    param(
        [Parameter(Mandatory)] [scriptblock]$Condition,
        [Parameter(Mandatory)] [string]$FailureMessage
    )

    if (-not (Test-Until -Condition $Condition)) {
        throw $FailureMessage
    }
}

function Test-Until {
    param(
        [Parameter(Mandatory)] [scriptblock]$Condition,
        [int]$TimeoutSeconds = $WaitTimeoutSeconds
    )

    $watch = [System.Diagnostics.Stopwatch]::StartNew()
    while ($watch.Elapsed.TotalSeconds -lt $TimeoutSeconds) {
        if (& $Condition) {
            return $true
        }
        Start-Sleep -Milliseconds 250
    }
    $false
}

function Start-VolumeOk {
    Invoke-AdbText -Arguments @("shell", "am", "start", "-W", "-n", $script:ActivityName) | Out-Null
    Wait-Until -FailureMessage "VolumeOK did not obtain a process within $WaitTimeoutSeconds seconds." -Condition {
        $pidResult = Invoke-AdbText -Arguments @("shell", "pidof", $script:PackageName) -AllowFailure
        $pidResult.ExitCode -eq 0 -and $pidResult.Output.Trim()
    }
}

function Restore-AppAfterInstrumentation {
    $installed = Invoke-AdbText -Arguments @("shell", "pm", "path", $script:PackageName) -AllowFailure
    if ($installed.ExitCode -eq 0 -and $installed.Output.Trim()) {
        return
    }
    if ($SkipInstall) {
        throw "The instrumentation task removed the app, and -SkipInstall prevents restoring it for UI scenarios."
    }
    $install = Invoke-AdbText -Arguments @("install", "-r", $script:ApkPath)
    if ($install.Output -notmatch "Success") {
        throw "Could not restore the app after instrumentation: $($install.Output)"
    }
}

function Capture-UiHierarchy {
    param([Parameter(Mandatory)] [string]$Name)

    $devicePath = "/data/local/tmp/volumeok-m0b-window.xml"
    Wait-Until -FailureMessage "UI hierarchy was not available within $WaitTimeoutSeconds seconds." -Condition {
        $dump = Invoke-AdbText -Arguments @("shell", "uiautomator", "dump", $devicePath) -AllowFailure
        $dump.ExitCode -eq 0 -and $dump.Output -match "dumped"
    }
    $content = (Invoke-AdbText -Arguments @("exec-out", "cat", $devicePath)).Output
    $path = Join-Path $script:ArtifactRoot "ui\$Name.xml"
    Write-Utf8File -Path $path -Content $content
    $content
}

function Capture-Screenshot {
    param([Parameter(Mandatory)] [string]$Name)
    $path = Join-Path $script:ArtifactRoot "screenshots\$Name.png"
    Invoke-AdbBinaryScreenshot -Path $path
    $path
}

function Capture-SystemState {
    param([Parameter(Mandatory)] [string]$Name)

    $volume = Get-RingVolumeState
    $ringerMode = (Invoke-AdbText -Arguments @("shell", "settings", "get", "system", "mode_ringer") -AllowFailure).Output
    $zenMode = (Invoke-AdbText -Arguments @("shell", "settings", "get", "global", "zen_mode") -AllowFailure).Output
    $state = @"
$($volume.Raw)
settings.system.mode_ringer=$($ringerMode.Trim())
settings.global.zen_mode=$($zenMode.Trim())
"@
    $path = Join-Path $script:ArtifactRoot "dumpsys\$Name.txt"
    Write-Utf8File -Path $path -Content $state.Trim()
    $path
}

function Test-StoppedRuntimeTruth {
    param([Parameter(Mandatory)] [string]$UiXml)
    # Keep source literals ASCII-only so Windows PowerShell 5.1 parses this UTF-8 file consistently.
    $stopped = $UiXml -match "Protection runtime:\s*Stopped|:\s*Parada|:\s*Detenida"
    $unavailable = $UiXml -match "Accidental-silence protection is not available yet|prote..o contra sil.ncio acidental ainda n.o est. dispon.vel|protecci.n contra el silencio accidental a.n no est. disponible"
    $active = $UiXml -match "Protection runtime:\s*Active|:\s*Ativa|:\s*Activa"
    ($stopped -or $unavailable) -and -not $active
}

function Expand-DiagnosticDetails {
    param(
        [Parameter(Mandatory)] [string]$UiXml,
        [Parameter(Mandatory)] [string]$CaptureName
    )

    if (Test-StoppedRuntimeTruth -UiXml $UiXml) {
        return $UiXml
    }
    $pattern = "Show diagnostic details|Mostrar detalhes do diagn.stico|Mostrar detalles del diagn.stico"
    $candidate = $UiXml
    for ($attempt = 0; $attempt -lt 5; $attempt++) {
        $center = Get-UiNodeCenter -UiXml $candidate -TextPattern $pattern
        if ($center -and $center.Y -lt 2100) {
            Invoke-AdbText -Arguments @("shell", "input", "tap", $center.X.ToString(), $center.Y.ToString()) | Out-Null
            break
        }
        Invoke-AdbText -Arguments @("shell", "input", "swipe", "500", "1800", "500", "700", "250") | Out-Null
        $candidate = Capture-UiHierarchy -Name "$CaptureName-scroll-$attempt"
    }
    Wait-Until -FailureMessage "Diagnostic details did not become visible." -Condition {
        $expanded = Capture-UiHierarchy -Name $CaptureName
        if (Test-StoppedRuntimeTruth -UiXml $expanded) {
            $script:ExpandedUiBuffer = $expanded
            return $true
        }
        $false
    }
    $result = $script:ExpandedUiBuffer
    Remove-Variable -Name ExpandedUiBuffer -Scope Script -ErrorAction SilentlyContinue
    $result
}

function Invoke-BaselineScenario {
    Start-VolumeOk
    $systemPath = Capture-SystemState -Name "audio-before"
    $ui = Capture-UiHierarchy -Name "01-baseline"
    $ui = Expand-DiagnosticDetails -UiXml $ui -CaptureName "01-baseline-details"
    $screenshot = Capture-Screenshot -Name "01-baseline"
    if (-not (Test-StoppedRuntimeTruth -UiXml $ui)) {
        Add-Outcome "baseline" "FAIL" "AUTOMATED_ADB" "UI did not prove runtime STOPPED without ACTIVE." @($systemPath, $screenshot)
        return
    }

    $volume = Get-RingVolumeState
    $ready = $ui -match "Everything looks ready|Tudo parece pronto|Todo parece listo"
    if ($volume.Current -eq 1 -and $ready) {
        Add-Outcome "baseline" "FAIL" "AUTOMATED_PUBLIC_API+AUTOMATED_ADB" "Lowest non-zero ringtone volume incorrectly rendered READY." @($systemPath, $screenshot)
        return
    }
    Add-Outcome "baseline" "PASS" "AUTOMATED_PUBLIC_API+AUTOMATED_ADB" "App launched with fresh evidence and truthful STOPPED runtime." @($systemPath, $screenshot)
}

function Invoke-InstrumentedSuiteOnce {
    if ($script:InstrumentationResult) {
        return $script:InstrumentationResult
    }

    $outputPath = Join-Path $script:ArtifactRoot "instrumentation\connected-test-output.txt"
    $gradlePath = Join-Path $script:RepositoryRoot "gradlew.bat"
    $previousSerial = $env:ANDROID_SERIAL
    $env:ANDROID_SERIAL = $script:Device.Serial
    try {
        $result = Invoke-NativeText -FilePath $gradlePath -Arguments @(
            "connectedDebugAndroidTest", "--console=plain"
        ) -AllowFailure
    } finally {
        if ($null -eq $previousSerial) {
            Remove-Item Env:ANDROID_SERIAL -ErrorAction SilentlyContinue
        } else {
            $env:ANDROID_SERIAL = $previousSerial
        }
    }
    Write-Utf8File -Path $outputPath -Content $result.Output

    $tagLog = Invoke-AdbText -Arguments @(
        "logcat", "-d", "-v", "threadtime", "VolumeOKM0B:I", "AndroidRuntime:E", "*:S"
    ) -AllowFailure
    $logPath = Join-Path $script:ArtifactRoot "logcat\volumeok-m0b.txt"
    Write-Utf8File -Path $logPath -Content $tagLog.Output

    $script:InstrumentationResult = [pscustomobject]@{
        Passed = $result.ExitCode -eq 0
        OutputPath = $outputPath
        LogPath = $logPath
        Output = $result.Output
        Log = $tagLog.Output
    }
    Restore-AppAfterInstrumentation
    $script:InstrumentationResult
}

function Get-UiNodeCenter {
    param(
        [Parameter(Mandatory)] [string]$UiXml,
        [Parameter(Mandatory)] [string]$TextPattern
    )

    try {
        [xml]$document = $UiXml
    } catch {
        return $null
    }
    $node = $document.SelectNodes("//node") | Where-Object { $_.text -match $TextPattern } | Select-Object -First 1
    if (-not $node -or $node.bounds -notmatch "\[(\d+),(\d+)\]\[(\d+),(\d+)\]") {
        return $null
    }
    [pscustomobject]@{
        X = [int](([int]$Matches[1] + [int]$Matches[3]) / 2)
        Y = [int](([int]$Matches[2] + [int]$Matches[4]) / 2)
    }
}

function Invoke-UiTextTap {
    param(
        [Parameter(Mandatory)] [string]$UiXml,
        [Parameter(Mandatory)] [string]$TextPattern
    )

    $candidate = $UiXml
    for ($attempt = 0; $attempt -lt 5; $attempt++) {
        $center = Get-UiNodeCenter -UiXml $candidate -TextPattern $TextPattern
        if ($center -and $center.Y -lt 2200) {
            Invoke-AdbText -Arguments @("shell", "input", "tap", $center.X.ToString(), $center.Y.ToString()) | Out-Null
            return
        }
        Invoke-AdbText -Arguments @("shell", "input", "swipe", "500", "1800", "500", "700", "250") | Out-Null
        $candidate = Capture-UiHierarchy -Name "ui-text-tap-scroll-$attempt"
    }
    throw "Could not locate a UI control matching '$TextPattern'."
}

function Invoke-ControlledWriteScenario {
    if ($script:StateRestorationFailed) {
        Add-Outcome "controlled-write" "SKIPPED" "AUTOMATED_INSTRUMENTED" "A previous restoration failure stopped mutable scenarios."
        return
    }
    $instrumented = Invoke-InstrumentedSuiteOnce
    if (-not $instrumented.Passed) {
        Add-Outcome "controlled-write" "FAIL" "AUTOMATED_INSTRUMENTED" "Physical instrumentation failed; see captured output." @($instrumented.OutputPath, $instrumented.LogPath)
        return
    }

    Start-VolumeOk
    $original = Get-RingVolumeState
    $ui = Capture-UiHierarchy -Name "03-before-controlled-test"
    $ui = Expand-DiagnosticDetails -UiXml $ui -CaptureName "03-diagnostic-details"
    $buttonPattern = "Controlled one-step test|Teste controlado de um n.vel|Prueba controlada de un nivel"
    $center = $null
    for ($attempt = 0; $attempt -lt 5; $attempt++) {
        $center = Get-UiNodeCenter -UiXml $ui -TextPattern $buttonPattern
        if ($center -and $center.Y -lt 2200) {
            break
        }
        Invoke-AdbText -Arguments @("shell", "input", "swipe", "500", "1800", "500", "700", "250") | Out-Null
        $ui = Capture-UiHierarchy -Name "03-controlled-scroll-$attempt"
    }
    if (-not $center) {
        Add-Outcome "controlled-write" "PARTIAL" "AUTOMATED_INSTRUMENTED" "Read/write/restore instrumentation passed, but the UI button could not be located." @($instrumented.OutputPath, $instrumented.LogPath)
        return
    }

    Invoke-AdbText -Arguments @("shell", "input", "tap", $center.X.ToString(), $center.Y.ToString()) | Out-Null
    $resultUi = $null
    Wait-Until -FailureMessage "Controlled-test result did not appear in the UI." -Condition {
        $candidate = Capture-UiHierarchy -Name "03-after-controlled-test"
        if ($candidate -match "Write verified by readback|verificada por releitura|Cambio verificado por relectura") {
            $script:ResultUiBuffer = $candidate
            return $true
        }
        $false
    }
    $resultUi = $script:ResultUiBuffer
    Remove-Variable -Name ResultUiBuffer -Scope Script -ErrorAction SilentlyContinue
    $screenshot = Capture-Screenshot -Name "03-after-controlled-test"
    $restored = Get-RingVolumeState
    if ($restored.Current -ne $original.Current) {
        $script:StateRestorationFailed = $true
        Add-Outcome "controlled-write" "FAIL" "AUTOMATED_PUBLIC_API+AUTOMATED_INSTRUMENTED" "Original ring volume was not restored; mutable scenarios stopped." @($instrumented.OutputPath, $instrumented.LogPath, $screenshot)
        return
    }
    if (-not (Test-StoppedRuntimeTruth -UiXml $resultUi)) {
        Add-Outcome "controlled-write" "FAIL" "AUTOMATED_PUBLIC_API+AUTOMATED_INSTRUMENTED" "Controlled write passed but runtime truth was not STOPPED." @($instrumented.OutputPath, $screenshot)
        return
    }
    Add-Outcome "controlled-write" "PASS" "AUTOMATED_PUBLIC_API+AUTOMATED_INSTRUMENTED+AUTOMATED_ADB" "Fresh write/readback/restoration passed and the UI reported the verified result." @($instrumented.OutputPath, $instrumented.LogPath, $screenshot)
}

function Restore-ZenMode {
    param([Parameter(Mandatory)] [int]$OriginalMode)
    $value = switch ($OriginalMode) {
        0 { "off" }
        1 { "priority" }
        2 { "none" }
        3 { "alarms" }
        default { $null }
    }
    if (-not $value) {
        return $false
    }
    $result = Invoke-AdbText -Arguments @("shell", "cmd", "notification", "set_dnd", $value) -AllowFailure
    $result.ExitCode -eq 0
}

function Invoke-ForegroundObservationScenario {
    if ($script:StateRestorationFailed) {
        Add-Outcome "foreground-observation" "SKIPPED" "AUTOMATED_ADB" "A previous restoration failure stopped mutable scenarios."
        return
    }
    Start-VolumeOk
    $originalVolume = Get-RingVolumeState
    $target = $null
    if ($originalVolume.Current -eq 0) {
        Add-ManualCheck "Leave deliberate zero volume unchanged; use hardware controls to verify 0/max plus vibrate or silent is not READY."
    } elseif ($originalVolume.Current -lt $originalVolume.Maximum - 1) {
        $target = $originalVolume.Current + 1
    } else {
        $target = $originalVolume.Current - 1
    }
    if ($null -ne $target -and $target -ge $originalVolume.Maximum) {
        Add-Outcome "foreground-observation" "UNSUPPORTED" "AUTOMATED_ADB" "No non-maximum test target exists."
        return
    }

    $volumeObserved = $false
    $volumeObservedAutomatically = $false
    if ($null -ne $target) {
        try {
            Set-RingVolumeState -Value $target
            $volumeChanged = Test-Until -TimeoutSeconds ([Math]::Min(5, $WaitTimeoutSeconds)) -Condition {
            (Get-RingVolumeState).Current -eq $target
            }
            if ($volumeChanged) {
                $volumeObservedAutomatically = Test-Until -TimeoutSeconds ([Math]::Min(5, $WaitTimeoutSeconds)) -Condition {
                    $candidate = Capture-UiHierarchy -Name "02-after-volume-change"
                    if ($candidate -match "(?:Ringtone|Toque|Tono):\s*$target\s*/\s*$($originalVolume.Maximum)") {
                        $script:ObservationUiBuffer = $candidate
                        return $true
                    }
                    $false
                }
                if (-not $volumeObservedAutomatically) {
                    $staleUi = Capture-UiHierarchy -Name "02-before-manual-refresh"
                    Invoke-UiTextTap -UiXml $staleUi -TextPattern "Check again|Verificar novamente|Comprobar de nuevo"
                    Wait-Until -FailureMessage "Foreground UI did not reflect STREAM_RING=$target after explicit refresh." -Condition {
                        $candidate = Capture-UiHierarchy -Name "02-after-volume-change"
                        if ($candidate -match "(?:Ringtone|Toque|Tono):\s*$target\s*/\s*$($originalVolume.Maximum)") {
                            $script:ObservationUiBuffer = $candidate
                            return $true
                        }
                        $false
                    }
                }
                $volumeUi = $script:ObservationUiBuffer
                Remove-Variable -Name ObservationUiBuffer -Scope Script -ErrorAction SilentlyContinue
                Capture-Screenshot -Name "02-after-volume-change" | Out-Null
                $volumeObserved = $true
                if ($target -eq 0 -and $volumeUi -match "\bReady\b|\bPronto\b|\bListo\b") {
                    throw "Zero ringtone volume rendered READY."
                }
            } else {
                Add-ManualCheck "ADB could not change STREAM_RING on this device; change ring volume with hardware controls while VolumeOK is foregrounded."
            }
        } finally {
            Set-RingVolumeState -Value $originalVolume.Current
            try {
                Wait-Until -FailureMessage "Original STREAM_RING volume was not restored." -Condition {
                    (Get-RingVolumeState).Current -eq $originalVolume.Current
                }
            } catch {
                $script:StateRestorationFailed = $true
                throw
            }
        }
    }

    $zenRaw = (Invoke-AdbText -Arguments @("shell", "settings", "get", "global", "zen_mode") -AllowFailure).Output.Trim()
    $dndObserved = $false
    $dndObservedAutomatically = $false
    if ($zenRaw -match "^\d+$") {
        $originalZen = [int]$zenRaw
        $targetDnd = if ($originalZen -eq 1) { "off" } else { "priority" }
        try {
            $setDnd = Invoke-AdbText -Arguments @("shell", "cmd", "notification", "set_dnd", $targetDnd) -AllowFailure
            if ($setDnd.ExitCode -eq 0) {
                $targetZen = if ($targetDnd -eq "priority") { 1 } else { 0 }
                $dndChanged = Test-Until -TimeoutSeconds ([Math]::Min(5, $WaitTimeoutSeconds)) -Condition {
                    $observedZen = (Invoke-AdbText -Arguments @(
                        "shell", "settings", "get", "global", "zen_mode"
                    ) -AllowFailure).Output.Trim()
                    $observedZen -eq $targetZen.ToString()
                }
                if ($dndChanged) {
                    $dndObservedAutomatically = Test-Until -TimeoutSeconds ([Math]::Min(5, $WaitTimeoutSeconds)) -Condition {
                        $candidate = Capture-UiHierarchy -Name "02b-after-dnd-change"
                        $expected = if ($targetDnd -eq "priority") {
                            "Priority only|Somente prioridade|Solo prioridad"
                        } else {
                            "Do Not Disturb:\s*Off|Perturbe:\s*Desativado|No molestar:\s*Desactivado"
                        }
                        if ($candidate -match $expected) {
                            return $true
                        }
                        $false
                    }
                    if (-not $dndObservedAutomatically) {
                        $staleUi = Capture-UiHierarchy -Name "02b-before-manual-refresh"
                        Invoke-UiTextTap -UiXml $staleUi -TextPattern "Check again|Verificar novamente|Comprobar de nuevo"
                        Wait-Until -FailureMessage "Foreground UI did not reflect DND after explicit refresh." -Condition {
                            $candidate = Capture-UiHierarchy -Name "02b-after-dnd-change"
                            $expected = if ($targetDnd -eq "priority") {
                                "Priority only|Somente prioridade|Solo prioridad"
                            } else {
                                "Do Not Disturb:\s*Off|Perturbe:\s*Desativado|No molestar:\s*Desactivado"
                            }
                            $candidate -match $expected
                        }
                    }
                    Capture-Screenshot -Name "02b-after-dnd-change" | Out-Null
                    $dndObserved = $true
                } else {
                    Add-ManualCheck "ADB could not change DND on this device; toggle DND through the OEM UI while VolumeOK is foregrounded."
                }
            }
        } finally {
            if (-not (Restore-ZenMode -OriginalMode $originalZen)) {
                $script:StateRestorationFailed = $true
                throw "Original DND mode could not be restored."
            }
            try {
                Wait-Until -FailureMessage "Original DND mode could not be verified after restoration." -Condition {
                    $restoredZen = (Invoke-AdbText -Arguments @(
                        "shell", "settings", "get", "global", "zen_mode"
                    ) -AllowFailure).Output.Trim()
                    $restoredZen -eq $originalZen.ToString()
                }
            } catch {
                $script:StateRestorationFailed = $true
                throw
            }
        }
    }

    Add-ManualCheck "Change normal/vibrate/silent with the physical buttons or OEM UI and record observer latency."
    if ($volumeObserved -and $dndObserved -and $volumeObservedAutomatically -and $dndObservedAutomatically) {
        Add-Outcome "foreground-observation" "PASS" "AUTOMATED_ADB" "Foreground UI observed safe volume and DND transitions; original states were restored." @(
            (Join-Path $script:ArtifactRoot "screenshots\02-after-volume-change.png"),
            (Join-Path $script:ArtifactRoot "screenshots\02b-after-dnd-change.png")
        )
    } elseif ($volumeObserved -and $dndObserved) {
        Add-ManualCheck "Use hardware controls to verify foreground refresh notifications for volume and DND on this OEM."
        Add-Outcome "foreground-observation" "PARTIAL" "AUTOMATED_ADB+MANUAL_REQUIRED" "Safe volume and DND transitions, explicit UI refresh, and restoration passed; ADB-induced changes did not prove automatic foreground notification." @(
            (Join-Path $script:ArtifactRoot "screenshots\02-after-volume-change.png"),
            (Join-Path $script:ArtifactRoot "screenshots\02b-after-dnd-change.png")
        )
    } elseif ($volumeObserved) {
        Add-ManualCheck "Toggle DND manually while VolumeOK is foregrounded, then capture and compare the refreshed UI."
        Add-Outcome "foreground-observation" "PARTIAL" "AUTOMATED_ADB+MANUAL_REQUIRED" "Volume transition was observed and restored; DND automation was unavailable." @(
            (Join-Path $script:ArtifactRoot "screenshots\02-after-volume-change.png")
        )
    } elseif ($dndObserved) {
        if (-not $dndObservedAutomatically) {
            Add-ManualCheck "Toggle DND through the OEM UI to verify automatic foreground notification without pressing Refresh."
        }
        Add-Outcome "foreground-observation" "PARTIAL" "AUTOMATED_ADB+MANUAL_REQUIRED" "ADB DND transition, explicit UI refresh, and restoration passed; ring-volume automation was unavailable on this device." @(
            (Join-Path $script:ArtifactRoot "screenshots\02b-after-dnd-change.png")
        )
    } else {
        Add-Outcome "foreground-observation" "MANUAL_REQUIRED" "MANUAL_REQUIRED" "Safe ADB volume and DND transitions were unavailable; physical/OEM controls are required."
    }
}

function Invoke-ForceStopScenario {
    if ($script:StateRestorationFailed) {
        Add-Outcome "force-stop-reopen" "SKIPPED" "AUTOMATED_ADB" "A restoration failure stopped later scenarios."
        return
    }
    Invoke-AdbText -Arguments @("shell", "am", "force-stop", $script:PackageName) | Out-Null
    Wait-Until -FailureMessage "VolumeOK process remained alive after force-stop." -Condition {
        $pidResult = Invoke-AdbText -Arguments @("shell", "pidof", $script:PackageName) -AllowFailure
        -not $pidResult.Output.Trim()
    }
    Start-VolumeOk
    $ui = Capture-UiHierarchy -Name "04-after-restart"
    $ui = Expand-DiagnosticDetails -UiXml $ui -CaptureName "04-after-restart-details"
    $screenshot = Capture-Screenshot -Name "04-after-restart"
    if (Test-StoppedRuntimeTruth -UiXml $ui) {
        Add-Outcome "force-stop-reopen" "PASS" "AUTOMATED_ADB" "Force-stop/reopen produced fresh UI with runtime STOPPED." @($screenshot)
    } else {
        Add-Outcome "force-stop-reopen" "FAIL" "AUTOMATED_ADB" "Restarted UI did not prove runtime STOPPED." @($screenshot)
    }
}

function Invoke-InstrumentedScenario {
    $instrumented = Invoke-InstrumentedSuiteOnce
    if ($instrumented.Passed) {
        Add-Outcome "instrumented-suite" "PASS" "AUTOMATED_INSTRUMENTED" "connectedDebugAndroidTest passed on the selected physical device." @($instrumented.OutputPath, $instrumented.LogPath)
    } else {
        Add-Outcome "instrumented-suite" "FAIL" "AUTOMATED_INSTRUMENTED" "connectedDebugAndroidTest failed; inspect captured output before retrying." @($instrumented.OutputPath, $instrumented.LogPath)
    }
}

function Invoke-CaptureOnlyScenario {
    Start-VolumeOk
    $systemPath = Capture-SystemState -Name "capture-only"
    Capture-UiHierarchy -Name "capture-only" | Out-Null
    $screenshot = Capture-Screenshot -Name "capture-only"
    Add-Outcome "capture-only" "PASS" "AUTOMATED_ADB" "Current app/system state captured without intentional mutation." @($systemPath, $screenshot)
}

function Write-RunReport {
    if (-not $script:ArtifactRoot) {
        return
    }
    Add-ManualCheck "Confirm whether a test sound is actually audible and assess perceived loudness."
    Add-ManualCheck "Exercise physical hardware buttons, including any suspected stuck-button behavior."
    Add-ManualCheck "Perform TalkBack, 200% font-scale, contrast, and subjective usability review."
    Add-ManualCheck "Run any deliberate long-duration battery/wakeup endurance test only after a runtime candidate exists."
    Add-ManualCheck "Test wired and Bluetooth presence when those devices are safely available."

    $scenarioLines = foreach ($item in $script:Outcomes) {
        "| $($item.Scenario) | $($item.Outcome) | $($item.Method) | $($item.Reason -replace '\|', '/') |"
    }
    $warningLines = if ($script:Warnings.Count) {
        $script:Warnings | ForEach-Object { "- $_" }
    } else {
        @("- None recorded.")
    }
    $manualLines = $script:ManualChecks | ForEach-Object { "- $_" }
    $report = @"
# VolumeOK M0B Physical Validation Summary

- Device: $($script:Device.Manufacturer) $($script:Device.Model)
- Android/API/build: $($script:Device.Android) / $($script:Device.Api) / $($script:Device.Build)
- Serial: $($script:Device.Serial)
- Source SHA: $($script:SourceSha)
- origin/main: $($script:OriginSha)
- APK SHA-256: $($script:ApkHash)
- Run timestamp (UTC): $([DateTime]::UtcNow.ToString("o"))

## Scenarios

| Scenario | Outcome | Method | Reason |
| --- | --- | --- | --- |
$($scenarioLines -join "`n")

## Warnings

$($warningLines -join "`n")

## Manual required

$($manualLines -join "`n")

## Evidence

Raw local evidence is under this run directory and is gitignored. Review and sanitize it before promoting selected
facts into `docs/evidence/`.

## Recommended next action

Resolve any FAIL first. Otherwise complete the manual checks, review/redact artifacts, and update the device matrix with
only observed facts. Do not infer continuous/background protection from foreground results.
"@
    Write-Utf8File -Path (Join-Path $script:ArtifactRoot "test-summary.md") -Content $report.Trim()

    $manifest = [ordered]@{
        schemaVersion = 1
        generatedAtUtc = [DateTime]::UtcNow.ToString("o")
        sourceSha = $script:SourceSha
        originMainSha = $script:OriginSha
        apkSha256 = $script:ApkHash
        device = $script:Device
        scenarios = @($script:Outcomes)
        warnings = @($script:Warnings)
        manualRequired = @($script:ManualChecks)
    }
    Write-Utf8File -Path (Join-Path $script:ArtifactRoot "manifest.json") -Content (
        $manifest | ConvertTo-Json -Depth 8
    )
}

$originalLocation = Get-Location
try {
    Set-Location -LiteralPath $script:RepositoryRoot
    $script:AdbPath = Resolve-AdbPath
    $script:Device = Select-AdbDevice -RequestedSerial $Serial
    Assert-SourcePreflight
    Initialize-Run
    Add-Outcome "preflight" "PASS" "AUTOMATED_ADB" "Authorized device and synchronized source were verified." @(
        (Join-Path $script:ArtifactRoot "device.txt"),
        (Join-Path $script:ArtifactRoot "source.txt")
    )

    $selectedScenarios = @(
        if ($Scenario -contains "all") {
            @("baseline", "controlled-write", "foreground-observation", "force-stop-reopen", "instrumented-suite")
        } else {
            $Scenario | Where-Object { $_ -ne "preflight" } | Select-Object -Unique
        }
    )

    if ($selectedScenarios.Count -gt 0 -and -not (
        $selectedScenarios.Count -eq 1 -and $selectedScenarios[0] -eq "capture-only"
    )) {
        Build-And-Install
    }

    foreach ($selected in $selectedScenarios) {
        try {
            switch ($selected) {
                "baseline" { Invoke-BaselineScenario }
                "controlled-write" { Invoke-ControlledWriteScenario }
                "foreground-observation" { Invoke-ForegroundObservationScenario }
                "force-stop-reopen" { Invoke-ForceStopScenario }
                "instrumented-suite" { Invoke-InstrumentedScenario }
                "capture-only" { Invoke-CaptureOnlyScenario }
            }
        } catch {
            Add-Outcome $selected "FAIL" "HARNESS" $_.Exception.Message
            if ($script:StateRestorationFailed) {
                $script:Warnings.Add("A state restoration failure occurred; later mutable scenarios were stopped.")
            }
        }
    }
} finally {
    Write-RunReport
    Set-Location -LiteralPath $originalLocation
}

Write-Output "Artifacts: $($script:ArtifactRoot)"
if ($script:Outcomes.Outcome -contains "FAIL") {
    throw "One or more M0B scenarios failed. Review test-summary.md before retrying."
}
