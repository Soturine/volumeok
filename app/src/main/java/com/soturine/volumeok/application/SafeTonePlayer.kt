package com.soturine.volumeok.application

import com.soturine.volumeok.domain.SafePlaybackFailure
import com.soturine.volumeok.domain.SafeToneStep

const val SAFE_TONE_DURATION_MILLIS = 700L

data class SafeToneRequest(val step: SafeToneStep, val durationMillis: Long = SAFE_TONE_DURATION_MILLIS) {
    init {
        require(durationMillis in 100L..2_000L)
    }
}

sealed interface SafeToneStartResult {
    data object Started : SafeToneStartResult

    data class Rejected(val failure: SafePlaybackFailure) : SafeToneStartResult
}

interface SafeToneListener {
    fun onCompleted()

    fun onFailure(failure: SafePlaybackFailure)
}

interface SafeTonePlayer {
    fun start(request: SafeToneRequest, listener: SafeToneListener): SafeToneStartResult

    fun stop()
}
