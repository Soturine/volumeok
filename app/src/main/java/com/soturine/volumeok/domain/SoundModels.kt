package com.soturine.volumeok.domain

enum class FailureCode {
    CAPABILITY_UNAVAILABLE,
    PERMISSION_REQUIRED,
    READ_FAILED,
    WRITE_NOT_ALLOWED,
    WRITE_FAILED,
    WRITE_NOT_EFFECTIVE,
    DND_STATE_UNKNOWN,
    AUDIO_ROUTE_UNKNOWN,
    BACKGROUND_MECHANISM_STOPPED,
    PROTECTION_DEGRADED,
    OSCILLATION_DETECTED,
    UNSUPPORTED_OEM_BEHAVIOR,
    TEST_PLAYBACK_FAILED,
    PERSISTENCE_FAILED
}

enum class CapabilityStatus {
    SUPPORTED,
    PERMISSION_REQUIRED,
    UNSUPPORTED,
    UNKNOWN
}

sealed interface Reading<out T> {
    data class Available<T>(val value: T) : Reading<T>

    data class Unavailable(val capability: CapabilityStatus, val failure: FailureCode) : Reading<Nothing>
}

data class RingVolume(val current: Int, val maximum: Int) {
    val isValid: Boolean = maximum > 0 && current in 0..maximum
}

enum class RingerMode {
    NORMAL,
    VIBRATE,
    SILENT
}

enum class DndState {
    OFF,
    PRIORITY_ONLY,
    ALARMS_ONLY,
    TOTAL_SILENCE
}

enum class AudioRouteEvidence {
    BUILT_IN_DEVICE_AVAILABLE,
    WIRED_DEVICE_PRESENT,
    BLUETOOTH_DEVICE_PRESENT,
    REMOTE_DEVICE_PRESENT,
    MULTIPLE_OUTPUT_DEVICES_PRESENT
}

data class SoundSnapshot(
    val capturedAtMillis: Long,
    val ringVolume: Reading<RingVolume>,
    val ringerMode: Reading<RingerMode>,
    val dndState: Reading<DndState>,
    val audioRoute: Reading<AudioRouteEvidence>
)
