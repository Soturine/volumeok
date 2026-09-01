package com.soturine.volumeok.platform.audio

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.soturine.volumeok.application.ControlledRingVolumeTest
import com.soturine.volumeok.application.ControlledTestStatus
import com.soturine.volumeok.domain.CapabilityStatus
import com.soturine.volumeok.domain.ReadinessEvaluator
import com.soturine.volumeok.domain.Reading
import com.soturine.volumeok.domain.RingVolume
import com.soturine.volumeok.domain.RingerMode
import com.soturine.volumeok.domain.SoundCapability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidSoundStateAdapterTest {
    @Test
    fun publicApiSnapshotIsReadableAndInternallyConsistent() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val snapshot = AndroidSoundStateAdapter(context).readSnapshot()
        val ringVolume = snapshot.ringVolume.availableOrFail("ring volume")
        val ringerMode = snapshot.ringerMode.availableOrFail("ringer mode")
        val dndState = snapshot.dndState.availableOrFail("DND state")
        val route = snapshot.audioRoute.availableOrFail("output-device evidence")
        val readiness = ReadinessEvaluator().evaluate(snapshot, System.currentTimeMillis())

        assertTrue(ringVolume.isValid)
        assertTrue(snapshot.capabilities.keys.containsAll(SoundCapability.entries))
        assertEquals(CapabilityStatus.SUPPORTED, snapshot.capabilities[SoundCapability.READ_RING_VOLUME])
        assertEquals(CapabilityStatus.SUPPORTED, snapshot.capabilities[SoundCapability.READ_RINGER_MODE])
        assertEquals(CapabilityStatus.SUPPORTED, snapshot.capabilities[SoundCapability.READ_DND])
        assertEquals(CapabilityStatus.SUPPORTED, snapshot.capabilities[SoundCapability.READ_OUTPUT_DEVICES])
        Log.i(
            TAG,
            "baseline ring=${ringVolume.current}/${ringVolume.maximum} " +
                "ringer=$ringerMode dnd=$dndState route=$route readiness=${readiness.status}"
        )
    }

    @Test
    fun controlledWriteUsesFreshReadbackAndRestoresOriginalState() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val adapter = AndroidSoundStateAdapter(context)
        val initialSnapshot = adapter.readSnapshot()
        val initialVolume = initialSnapshot.ringVolume.availableOrFail("initial ring volume")
        val initialRingerMode = initialSnapshot.ringerMode.availableOrFail("initial ringer mode")
        var reportStatus: ControlledTestStatus? = null

        try {
            val report = ControlledRingVolumeTest(adapter).execute()
            reportStatus = report.status
            Log.i(
                TAG,
                "controlled-write status=${report.status} original=${report.original} " +
                    "requested=${report.requested} readback=${report.observedAfterWrite} " +
                    "restored=${report.observedAfterRestore}"
            )

            assertNotEquals(ControlledTestStatus.NO_SAFE_TEST_CHANGE, report.status)
            assertEquals(ControlledTestStatus.EFFECTIVE_AND_RESTORED, report.status)
            assertNotNull(report.requested)
            assertNotEquals(initialVolume.maximum, report.requested)
        } finally {
            adapter.write(initialVolume.current)
            val finalSnapshot = adapter.readSnapshot()
            val finalVolume = finalSnapshot.ringVolume.availableOrFail("restored ring volume")
            val finalRingerMode = finalSnapshot.ringerMode.availableOrFail("restored ringer mode")
            assertEquals("ring volume restoration after $reportStatus", initialVolume.current, finalVolume.current)
            assertEquals("ringer mode restoration after $reportStatus", initialRingerMode, finalRingerMode)
        }
    }

    private fun <T> Reading<T>.availableOrFail(label: String): T = when (this) {
        is Reading.Available -> value
        is Reading.Unavailable -> {
            fail("$label unavailable: capability=$capability failure=$failure")
            error("unreachable")
        }
    }

    private companion object {
        const val TAG = "VolumeOKM0B"
    }
}
