package com.soturine.volumeok.platform.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import com.soturine.volumeok.application.SafeToneListener
import com.soturine.volumeok.application.SafeTonePlayer
import com.soturine.volumeok.application.SafeToneRequest
import com.soturine.volumeok.application.SafeToneStartResult
import com.soturine.volumeok.domain.SafePlaybackFailure

class AndroidSafeTonePlayer(context: Context) : SafeTonePlayer {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var active: ActivePlayback? = null
    private var nextGeneration = 1L

    @Synchronized
    @Suppress("ReturnCount")
    override fun start(request: SafeToneRequest, listener: SafeToneListener): SafeToneStartResult {
        if (active != null) {
            return SafeToneStartResult.Rejected(SafePlaybackFailure.ALREADY_PLAYING)
        }

        val generation = nextGeneration++
        val focusRequest = createFocusRequest(generation)
        val focusResult = try {
            audioManager.requestAudioFocus(focusRequest)
        } catch (_: RuntimeException) {
            AudioManager.AUDIOFOCUS_REQUEST_FAILED
        }
        if (focusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return SafeToneStartResult.Rejected(SafePlaybackFailure.AUDIO_FOCUS_DENIED)
        }

        val tone = try {
            ToneGenerator(AudioManager.STREAM_RING, request.gainPercent())
        } catch (_: RuntimeException) {
            audioManager.abandonAudioFocusRequest(focusRequest)
            return SafeToneStartResult.Rejected(SafePlaybackFailure.START_FAILED)
        }

        val completion = Runnable { complete(generation) }
        active = ActivePlayback(generation, tone, focusRequest, completion, listener)
        val started = try {
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, request.durationMillis.toInt())
        } catch (_: RuntimeException) {
            false
        }
        if (!started) {
            releaseActive(generation)
            return SafeToneStartResult.Rejected(SafePlaybackFailure.START_FAILED)
        }

        if (!handler.postDelayed(completion, request.durationMillis)) {
            releaseActive(generation)
            return SafeToneStartResult.Rejected(SafePlaybackFailure.START_FAILED)
        }
        return SafeToneStartResult.Started
    }

    @Synchronized
    override fun stop() {
        active?.let { releaseActive(it.generation) }
    }

    private fun createFocusRequest(generation: Long): AudioFocusRequest {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        return AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener(
                { change ->
                    if (
                        change == AudioManager.AUDIOFOCUS_LOSS ||
                        change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                        change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
                    ) {
                        interrupt(generation)
                    }
                },
                handler
            )
            .build()
    }

    private fun complete(generation: Long) {
        val listener = synchronized(this) {
            val playback = active?.takeIf { it.generation == generation } ?: return
            releaseActive(generation)
            playback.listener
        }
        listener.onCompleted()
    }

    private fun interrupt(generation: Long) {
        val listener = synchronized(this) {
            val playback = active?.takeIf { it.generation == generation } ?: return
            releaseActive(generation)
            playback.listener
        }
        listener.onFailure(SafePlaybackFailure.INTERRUPTED)
    }

    private fun releaseActive(generation: Long) {
        val playback = active?.takeIf { it.generation == generation } ?: return
        active = null
        handler.removeCallbacks(playback.completion)
        try {
            playback.tone.stopTone()
        } catch (_: RuntimeException) {
            // Release below remains mandatory even if the platform rejects stop.
        }
        try {
            playback.tone.release()
        } finally {
            try {
                audioManager.abandonAudioFocusRequest(playback.focusRequest)
            } catch (_: RuntimeException) {
                // The local tone is already stopped and released.
            }
        }
    }

    private fun SafeToneRequest.gainPercent(): Int =
        ((step.value.toLong() * MAX_TONE_GAIN_PERCENT) / step.platformMaximum)
            .toInt()
            .coerceIn(MIN_TONE_GAIN_PERCENT, MAX_SAFE_TONE_GAIN_PERCENT)

    private data class ActivePlayback(
        val generation: Long,
        val tone: ToneGenerator,
        val focusRequest: AudioFocusRequest,
        val completion: Runnable,
        val listener: SafeToneListener
    )

    private companion object {
        const val MIN_TONE_GAIN_PERCENT = 1
        const val MAX_TONE_GAIN_PERCENT = 100
        const val MAX_SAFE_TONE_GAIN_PERCENT = 99
    }
}
