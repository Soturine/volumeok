package com.soturine.volumeok.domain

data class SafeToneStep(val value: Int, val platformMaximum: Int) {
    init {
        require(platformMaximum > 1)
        require(value in 1 until platformMaximum)
    }
}

class SafeToneProgression {
    fun first(volume: RingVolume): SafeToneStep? {
        if (!volume.isValid || volume.maximum <= 1) return null
        return SafeToneStep(
            value = volume.current.coerceIn(1, volume.maximum - 1),
            platformMaximum = volume.maximum
        )
    }

    fun next(current: SafeToneStep): SafeToneStep? {
        val nextValue = current.value + 1
        return if (nextValue < current.platformMaximum) {
            SafeToneStep(nextValue, current.platformMaximum)
        } else {
            null
        }
    }
}

enum class SafePlaybackFailure {
    ALREADY_PLAYING,
    AUDIO_FOCUS_DENIED,
    START_FAILED,
    INTERRUPTED
}

sealed interface SafeSoundTestFailure {
    data object InvalidRingVolume : SafeSoundTestFailure

    data class Playback(val reason: SafePlaybackFailure) : SafeSoundTestFailure
}

enum class SafeSoundTestOutcome {
    HEARD,
    NOT_HEARD_AT_LIMIT,
    CANCELLED
}

sealed interface SafeSoundTestState {
    data object Explaining : SafeSoundTestState

    data class Playing(val attemptId: Long, val step: SafeToneStep) : SafeSoundTestState

    data class AwaitingConfirmation(val attemptId: Long, val step: SafeToneStep) : SafeSoundTestState

    data class ReadyForNextAttempt(val step: SafeToneStep) : SafeSoundTestState

    data class Completed(val outcome: SafeSoundTestOutcome) : SafeSoundTestState

    data class Failed(val failure: SafeSoundTestFailure) : SafeSoundTestState
}
