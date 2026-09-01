package com.soturine.volumeok.feature.home

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.soturine.volumeok.R
import com.soturine.volumeok.application.ControlledTestStatus
import com.soturine.volumeok.application.LowRingVolumeCorrectionStatus
import com.soturine.volumeok.domain.AudioRouteEvidence
import com.soturine.volumeok.domain.CapabilityStatus
import com.soturine.volumeok.domain.DndState
import com.soturine.volumeok.domain.ReadinessIssueCode
import com.soturine.volumeok.domain.ReadinessStatus
import com.soturine.volumeok.domain.Reading
import com.soturine.volumeok.domain.RingVolume
import com.soturine.volumeok.domain.RingerMode
import com.soturine.volumeok.domain.SoundCapability

internal fun HomeUiState.canCorrectLowRingVolume(): Boolean {
    val currentSnapshot = snapshot
    val currentReadiness = readiness
    val volume = (currentSnapshot?.ringVolume as? Reading.Available)?.value
    return currentSnapshot != null &&
        currentReadiness?.status == ReadinessStatus.ATTENTION &&
        currentReadiness.issues.any { it.code == ReadinessIssueCode.RING_VOLUME_LOWEST_NONZERO } &&
        volume?.current == 1 &&
        volume.maximum > 2 &&
        (currentSnapshot.ringerMode as? Reading.Available)?.value == RingerMode.NORMAL &&
        (currentSnapshot.dndState as? Reading.Available)?.value == DndState.OFF &&
        currentSnapshot.capabilities[SoundCapability.WRITE_RING_VOLUME] == CapabilityStatus.SUPPORTED
}

@Composable
internal fun ringVolumeText(reading: Reading<RingVolume>?): String = when (reading) {
    is Reading.Available -> stringResource(R.string.volume_fraction, reading.value.current, reading.value.maximum)
    else -> stringResource(R.string.unavailable)
}

@Composable
internal fun ringerModeText(reading: Reading<RingerMode>?): String = when ((reading as? Reading.Available)?.value) {
    RingerMode.NORMAL -> stringResource(R.string.ringer_normal)
    RingerMode.VIBRATE -> stringResource(R.string.ringer_vibrate)
    RingerMode.SILENT -> stringResource(R.string.ringer_silent)
    null -> stringResource(R.string.unavailable)
}

@Composable
internal fun dndText(reading: Reading<DndState>?): String = when ((reading as? Reading.Available)?.value) {
    DndState.OFF -> stringResource(R.string.dnd_off)
    DndState.PRIORITY_ONLY -> stringResource(R.string.dnd_priority)
    DndState.ALARMS_ONLY -> stringResource(R.string.dnd_alarms)
    DndState.TOTAL_SILENCE -> stringResource(R.string.dnd_silence)
    null -> stringResource(R.string.unavailable)
}

@Composable
internal fun routeText(reading: Reading<AudioRouteEvidence>?): String = when ((reading as? Reading.Available)?.value) {
    AudioRouteEvidence.BUILT_IN_DEVICE_AVAILABLE -> stringResource(R.string.route_builtin_present)
    AudioRouteEvidence.WIRED_DEVICE_PRESENT -> stringResource(R.string.route_wired_present)
    AudioRouteEvidence.BLUETOOTH_DEVICE_PRESENT -> stringResource(R.string.route_bluetooth_present)
    AudioRouteEvidence.REMOTE_DEVICE_PRESENT -> stringResource(R.string.route_remote_present)
    AudioRouteEvidence.MULTIPLE_OUTPUT_DEVICES_PRESENT -> stringResource(R.string.route_multiple_present)
    null -> stringResource(R.string.unavailable)
}

@StringRes
internal fun readinessTitleResource(status: ReadinessStatus?): Int = when (status) {
    ReadinessStatus.READY -> R.string.readiness_ready_title
    ReadinessStatus.ATTENTION -> R.string.readiness_attention_title
    ReadinessStatus.ACTION_REQUIRED -> R.string.readiness_action_title
    ReadinessStatus.UNKNOWN, null -> R.string.readiness_unknown_title
}

@StringRes
internal fun readinessSummaryResource(status: ReadinessStatus?): Int = when (status) {
    ReadinessStatus.READY -> R.string.readiness_ready_summary
    ReadinessStatus.ATTENTION -> R.string.readiness_attention_summary
    ReadinessStatus.ACTION_REQUIRED -> R.string.readiness_action_summary
    ReadinessStatus.UNKNOWN, null -> R.string.readiness_unknown_summary
}

@StringRes
internal fun issueResource(code: ReadinessIssueCode): Int = when (code) {
    ReadinessIssueCode.RING_VOLUME_UNKNOWN -> R.string.issue_ring_volume_unknown
    ReadinessIssueCode.RING_VOLUME_INVALID -> R.string.issue_ring_volume_invalid
    ReadinessIssueCode.RING_VOLUME_MUTED -> R.string.issue_ring_volume_muted
    ReadinessIssueCode.RING_VOLUME_LOWEST_NONZERO -> R.string.issue_ring_volume_low
    ReadinessIssueCode.RINGER_MODE_UNKNOWN -> R.string.issue_ringer_mode_unknown
    ReadinessIssueCode.RINGER_MODE_SILENT -> R.string.issue_ringer_silent
    ReadinessIssueCode.RINGER_MODE_VIBRATE_ONLY -> R.string.issue_ringer_vibrate
    ReadinessIssueCode.DND_STATE_UNKNOWN -> R.string.issue_dnd_unknown
    ReadinessIssueCode.DND_ACTIVE -> R.string.issue_dnd_active
    ReadinessIssueCode.AUDIO_ROUTE_UNKNOWN -> R.string.issue_output_unknown
    ReadinessIssueCode.CAPABILITY_STATUS_INCONSISTENT -> R.string.issue_capability_inconsistent
}

@StringRes
internal fun correctionStatusResource(status: LowRingVolumeCorrectionStatus): Int = when (status) {
    LowRingVolumeCorrectionStatus.EFFECTIVE -> R.string.correction_effective
    LowRingVolumeCorrectionStatus.NOT_EFFECTIVE -> R.string.correction_not_effective
    LowRingVolumeCorrectionStatus.UNEXPECTED_READBACK -> R.string.correction_unexpected_readback
    LowRingVolumeCorrectionStatus.READ_UNAVAILABLE -> R.string.correction_read_unavailable
    LowRingVolumeCorrectionStatus.WRITE_REJECTED -> R.string.correction_write_rejected
    LowRingVolumeCorrectionStatus.NO_SAFE_TARGET -> R.string.correction_no_safe_target
}

@StringRes
internal fun controlledStatusResource(status: ControlledTestStatus): Int = when (status) {
    ControlledTestStatus.EFFECTIVE_AND_RESTORED -> R.string.controlled_effective_restored
    ControlledTestStatus.NOT_EFFECTIVE -> R.string.controlled_not_effective
    ControlledTestStatus.UNEXPECTED_READBACK -> R.string.controlled_unexpected_readback
    ControlledTestStatus.RESTORE_FAILED -> R.string.controlled_restore_failed
    ControlledTestStatus.READ_UNAVAILABLE -> R.string.controlled_read_unavailable
    ControlledTestStatus.NO_SAFE_TEST_CHANGE -> R.string.controlled_no_safe_change
}
