package com.soturine.volumeok.platform.audio

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.soturine.volumeok.domain.Reading
import com.soturine.volumeok.domain.SoundCapability
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidSoundStateAdapterTest {
    @Test
    fun snapshotReturnsValueOrExplicitUnavailabilityForEveryRead() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val snapshot = AndroidSoundStateAdapter(context).readSnapshot()

        assertTrue(snapshot.ringVolume is Reading.Available || snapshot.ringVolume is Reading.Unavailable)
        assertTrue(snapshot.ringerMode is Reading.Available || snapshot.ringerMode is Reading.Unavailable)
        assertTrue(snapshot.dndState is Reading.Available || snapshot.dndState is Reading.Unavailable)
        assertTrue(snapshot.audioRoute is Reading.Available || snapshot.audioRoute is Reading.Unavailable)
        assertTrue(snapshot.capabilities.keys.containsAll(SoundCapability.entries))
    }
}
