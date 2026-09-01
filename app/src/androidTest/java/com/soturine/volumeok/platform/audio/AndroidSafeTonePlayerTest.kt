package com.soturine.volumeok.platform.audio

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.soturine.volumeok.application.SafeToneListener
import com.soturine.volumeok.application.SafeToneRequest
import com.soturine.volumeok.application.SafeToneStartResult
import com.soturine.volumeok.domain.SafePlaybackFailure
import com.soturine.volumeok.domain.SafeToneStep
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidSafeTonePlayerTest {
    @Test
    fun minimumLocalToneCanStartAndStopWithoutChangingSystemVolume() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val soundState = AndroidSoundStateAdapter(context)
        val original = soundState.read()
        val ringVolume = (original as? com.soturine.volumeok.domain.Reading.Available)?.value
        assumeTrue(ringVolume != null && ringVolume.maximum > 1)
        checkNotNull(ringVolume)
        val player = AndroidSafeTonePlayer(context)

        try {
            val result = player.start(
                request = SafeToneRequest(SafeToneStep(1, ringVolume.maximum), durationMillis = 100L),
                listener =
                object : SafeToneListener {
                    override fun onCompleted() = Unit

                    override fun onFailure(failure: SafePlaybackFailure) = Unit
                }
            )

            assertEquals(SafeToneStartResult.Started, result)
        } finally {
            player.stop()
        }

        assertEquals(original, soundState.read())
    }
}
