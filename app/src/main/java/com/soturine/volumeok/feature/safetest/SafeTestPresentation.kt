package com.soturine.volumeok.feature.safetest

import androidx.annotation.StringRes
import com.soturine.volumeok.R
import com.soturine.volumeok.domain.SafePlaybackFailure
import com.soturine.volumeok.domain.SafeSoundTestFailure
import com.soturine.volumeok.domain.SafeSoundTestOutcome

@StringRes
internal fun outcomeResource(outcome: SafeSoundTestOutcome): Int = when (outcome) {
    SafeSoundTestOutcome.HEARD -> R.string.safe_test_result_heard
    SafeSoundTestOutcome.NOT_HEARD_AT_LIMIT -> R.string.safe_test_result_not_heard
    SafeSoundTestOutcome.CANCELLED -> R.string.safe_test_result_cancelled
}

@StringRes
internal fun failureResource(failure: SafeSoundTestFailure): Int = when (failure) {
    SafeSoundTestFailure.InvalidRingVolume -> R.string.safe_test_failure_volume
    is SafeSoundTestFailure.Playback -> when (failure.reason) {
        SafePlaybackFailure.ALREADY_PLAYING -> R.string.safe_test_failure_busy
        SafePlaybackFailure.AUDIO_FOCUS_DENIED -> R.string.safe_test_failure_focus
        SafePlaybackFailure.START_FAILED -> R.string.safe_test_failure_start
        SafePlaybackFailure.INTERRUPTED -> R.string.safe_test_failure_interrupted
    }
}
