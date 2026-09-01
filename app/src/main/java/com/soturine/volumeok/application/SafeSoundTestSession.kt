package com.soturine.volumeok.application

import com.soturine.volumeok.domain.RingVolume
import com.soturine.volumeok.domain.SafePlaybackFailure
import com.soturine.volumeok.domain.SafeSoundTestFailure
import com.soturine.volumeok.domain.SafeSoundTestOutcome
import com.soturine.volumeok.domain.SafeSoundTestState
import com.soturine.volumeok.domain.SafeToneProgression
import com.soturine.volumeok.domain.SafeToneStep

class SafeSoundTestSession(
    private val player: SafeTonePlayer,
    private val progression: SafeToneProgression = SafeToneProgression(),
    private val onStateChanged: (SafeSoundTestState) -> Unit = {}
) {
    var state: SafeSoundTestState = SafeSoundTestState.Explaining
        private set(value) {
            field = value
            onStateChanged(value)
        }

    private var nextAttemptId = 1L

    @Synchronized
    fun start(volume: RingVolume) {
        if (state != SafeSoundTestState.Explaining) return
        val firstStep = progression.first(volume)
        if (firstStep == null) {
            state = SafeSoundTestState.Failed(SafeSoundTestFailure.InvalidRingVolume)
            return
        }
        play(firstStep)
    }

    @Synchronized
    fun tryAgain() {
        val ready = state as? SafeSoundTestState.ReadyForNextAttempt ?: return
        play(ready.step)
    }

    @Synchronized
    fun heard() {
        if (state is SafeSoundTestState.AwaitingConfirmation) {
            state = SafeSoundTestState.Completed(SafeSoundTestOutcome.HEARD)
        }
    }

    @Synchronized
    fun notHeard() {
        val awaiting = state as? SafeSoundTestState.AwaitingConfirmation ?: return
        val nextStep = progression.next(awaiting.step)
        state = if (nextStep == null) {
            SafeSoundTestState.Completed(SafeSoundTestOutcome.NOT_HEARD_AT_LIMIT)
        } else {
            SafeSoundTestState.ReadyForNextAttempt(nextStep)
        }
    }

    @Synchronized
    fun stop() {
        cancelActiveSession()
    }

    @Synchronized
    fun onLifecycleInterrupted() {
        cancelActiveSession()
    }

    private fun play(step: SafeToneStep) {
        val attemptId = nextAttemptId++
        state = SafeSoundTestState.Playing(attemptId, step)
        val result = player.start(
            request = SafeToneRequest(step),
            listener =
            object : SafeToneListener {
                override fun onCompleted() {
                    playbackCompleted(attemptId)
                }

                override fun onFailure(failure: SafePlaybackFailure) {
                    playbackFailed(attemptId, failure)
                }
            }
        )
        if (
            result is SafeToneStartResult.Rejected &&
            (state as? SafeSoundTestState.Playing)?.attemptId == attemptId
        ) {
            player.stop()
            state = SafeSoundTestState.Failed(SafeSoundTestFailure.Playback(result.failure))
        }
    }

    @Synchronized
    private fun playbackCompleted(attemptId: Long) {
        val playing = state as? SafeSoundTestState.Playing ?: return
        if (playing.attemptId != attemptId) return
        state = SafeSoundTestState.AwaitingConfirmation(attemptId, playing.step)
    }

    @Synchronized
    private fun playbackFailed(attemptId: Long, failure: SafePlaybackFailure) {
        if ((state as? SafeSoundTestState.Playing)?.attemptId != attemptId) return
        player.stop()
        state = SafeSoundTestState.Failed(SafeSoundTestFailure.Playback(failure))
    }

    private fun cancelActiveSession() {
        when (state) {
            SafeSoundTestState.Explaining,
            is SafeSoundTestState.Completed,
            is SafeSoundTestState.Failed -> return
            else -> {
                player.stop()
                state = SafeSoundTestState.Completed(SafeSoundTestOutcome.CANCELLED)
            }
        }
    }
}
