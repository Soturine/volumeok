package com.soturine.volumeok.platform.audio

import android.app.NotificationManager
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import com.soturine.volumeok.application.SoundSnapshotGateway
import com.soturine.volumeok.application.WriteAttempt
import com.soturine.volumeok.domain.AudioRouteEvidence
import com.soturine.volumeok.domain.CapabilityStatus
import com.soturine.volumeok.domain.DndState
import com.soturine.volumeok.domain.FailureCode
import com.soturine.volumeok.domain.Reading
import com.soturine.volumeok.domain.RingVolume
import com.soturine.volumeok.domain.RingerMode
import com.soturine.volumeok.domain.SoundCapability
import com.soturine.volumeok.domain.SoundSnapshot

class AndroidSoundStateAdapter(context: Context) : SoundSnapshotGateway {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    override fun readSnapshot(): SoundSnapshot {
        val ringVolume = read()
        val ringerMode = readRingerMode()
        val dndState = readDndState()
        val route = readRouteEvidence()
        return SoundSnapshot(
            capturedAtMillis = System.currentTimeMillis(),
            ringVolume = ringVolume,
            ringerMode = ringerMode,
            dndState = dndState,
            audioRoute = route,
            capabilities = mapOf(
                SoundCapability.READ_RING_VOLUME to ringVolume.status(),
                SoundCapability.WRITE_RING_VOLUME to CapabilityStatus.SUPPORTED,
                SoundCapability.READ_RINGER_MODE to ringerMode.status(),
                SoundCapability.READ_DND to dndState.status(),
                SoundCapability.CHANGE_DND to dndChangeCapability(),
                SoundCapability.READ_OUTPUT_DEVICES to route.status(),
                SoundCapability.OBSERVE_FOREGROUND_CHANGES to CapabilityStatus.UNKNOWN,
                SoundCapability.CONTINUOUS_PROTECTION to CapabilityStatus.UNKNOWN
            )
        )
    }

    override fun read(): Reading<RingVolume> = readPlatformValue {
        RingVolume(
            current = audioManager.getStreamVolume(AudioManager.STREAM_RING),
            maximum = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
        )
    }

    override fun write(target: Int): WriteAttempt = try {
        audioManager.setStreamVolume(AudioManager.STREAM_RING, target, 0)
        WriteAttempt.Accepted
    } catch (_: SecurityException) {
        WriteAttempt.Rejected(FailureCode.WRITE_NOT_ALLOWED)
    } catch (_: RuntimeException) {
        WriteAttempt.Rejected(FailureCode.WRITE_FAILED)
    }

    private fun readRingerMode(): Reading<RingerMode> = readPlatformValue {
        when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> RingerMode.NORMAL
            AudioManager.RINGER_MODE_VIBRATE -> RingerMode.VIBRATE
            AudioManager.RINGER_MODE_SILENT -> RingerMode.SILENT
            else -> error("Unexpected Android ringer mode")
        }
    }

    private fun readDndState(): Reading<DndState> = try {
        when (notificationManager.currentInterruptionFilter) {
            NotificationManager.INTERRUPTION_FILTER_ALL -> Reading.Available(DndState.OFF)
            NotificationManager.INTERRUPTION_FILTER_PRIORITY -> Reading.Available(DndState.PRIORITY_ONLY)
            NotificationManager.INTERRUPTION_FILTER_ALARMS -> Reading.Available(DndState.ALARMS_ONLY)
            NotificationManager.INTERRUPTION_FILTER_NONE -> Reading.Available(DndState.TOTAL_SILENCE)
            else -> Reading.Unavailable(CapabilityStatus.UNKNOWN, FailureCode.DND_STATE_UNKNOWN)
        }
    } catch (_: SecurityException) {
        Reading.Unavailable(CapabilityStatus.PERMISSION_REQUIRED, FailureCode.PERMISSION_REQUIRED)
    } catch (_: RuntimeException) {
        Reading.Unavailable(CapabilityStatus.UNKNOWN, FailureCode.READ_FAILED)
    }

    private fun readRouteEvidence(): Reading<AudioRouteEvidence> = readPlatformValue {
        val categories = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .mapNotNull(::deviceCategory)
            .toSet()
        when {
            categories.size > 1 -> AudioRouteEvidence.MULTIPLE_OUTPUT_DEVICES_PRESENT
            categories.size == 1 -> categories.single()
            else -> error("Android reported no output device evidence")
        }
    }

    private fun deviceCategory(device: AudioDeviceInfo): AudioRouteEvidence? = when (device.type) {
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE,
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE -> AudioRouteEvidence.BUILT_IN_DEVICE_AVAILABLE
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_USB_HEADSET -> AudioRouteEvidence.WIRED_DEVICE_PRESENT
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_BLE_HEADSET,
        AudioDeviceInfo.TYPE_BLE_SPEAKER,
        AudioDeviceInfo.TYPE_HEARING_AID -> AudioRouteEvidence.BLUETOOTH_DEVICE_PRESENT
        AudioDeviceInfo.TYPE_HDMI,
        AudioDeviceInfo.TYPE_HDMI_ARC,
        AudioDeviceInfo.TYPE_HDMI_EARC,
        AudioDeviceInfo.TYPE_REMOTE_SUBMIX -> AudioRouteEvidence.REMOTE_DEVICE_PRESENT
        else -> null
    }

    private fun dndChangeCapability(): CapabilityStatus = if (notificationManager.isNotificationPolicyAccessGranted) {
        CapabilityStatus.SUPPORTED
    } else {
        CapabilityStatus.PERMISSION_REQUIRED
    }

    private inline fun <T> readPlatformValue(block: () -> T): Reading<T> = try {
        Reading.Available(block())
    } catch (_: SecurityException) {
        Reading.Unavailable(CapabilityStatus.PERMISSION_REQUIRED, FailureCode.PERMISSION_REQUIRED)
    } catch (_: RuntimeException) {
        Reading.Unavailable(CapabilityStatus.UNKNOWN, FailureCode.READ_FAILED)
    }

    private fun Reading<*>.status(): CapabilityStatus = when (this) {
        is Reading.Available -> CapabilityStatus.SUPPORTED
        is Reading.Unavailable -> capability
    }
}
