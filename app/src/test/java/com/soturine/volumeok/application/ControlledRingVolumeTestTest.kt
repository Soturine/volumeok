package com.soturine.volumeok.application

import com.soturine.volumeok.domain.CapabilityStatus
import com.soturine.volumeok.domain.FailureCode
import com.soturine.volumeok.domain.Reading
import com.soturine.volumeok.domain.RingVolume
import org.junit.Assert.assertEquals
import org.junit.Test

class ControlledRingVolumeTestTest {
    @Test
    fun `setter is proven only by fresh readback and original is restored`() {
        val gateway = StatefulGateway(current = 3, maximum = 7, writesAreEffective = true)

        val report = ControlledRingVolumeTest(gateway).execute()

        assertEquals(ControlledTestStatus.EFFECTIVE_AND_RESTORED, report.status)
        assertEquals(4, report.observedAfterWrite)
        assertEquals(3, report.observedAfterRestore)
        assertEquals(listOf(4, 3), gateway.writes)
    }

    @Test
    fun `accepted setter without state change is not effective`() {
        val gateway = StatefulGateway(current = 3, maximum = 7, writesAreEffective = false)

        val report = ControlledRingVolumeTest(gateway).execute()

        assertEquals(ControlledTestStatus.NOT_EFFECTIVE, report.status)
        assertEquals(3, report.observedAfterWrite)
    }

    @Test
    fun `test target never forces platform maximum`() {
        val gateway = StatefulGateway(current = 6, maximum = 7, writesAreEffective = true)

        val report = ControlledRingVolumeTest(gateway).execute()

        assertEquals(5, report.requested)
        assertEquals(ControlledTestStatus.EFFECTIVE_AND_RESTORED, report.status)
    }

    @Test
    fun `unknown initial read fails visibly`() {
        val gateway =
            object : RingVolumeGateway {
                override fun read(): Reading<RingVolume> =
                    Reading.Unavailable(CapabilityStatus.UNKNOWN, FailureCode.READ_FAILED)

                override fun write(target: Int): WriteAttempt = error("write must not be attempted")
            }

        assertEquals(
            ControlledTestStatus.READ_UNAVAILABLE,
            ControlledRingVolumeTest(gateway).execute().status
        )
    }

    private class StatefulGateway(
        private var current: Int,
        private val maximum: Int,
        private val writesAreEffective: Boolean
    ) : RingVolumeGateway {
        val writes = mutableListOf<Int>()

        override fun read(): Reading<RingVolume> = Reading.Available(RingVolume(current, maximum))

        override fun write(target: Int): WriteAttempt {
            writes += target
            if (writesAreEffective) current = target
            return WriteAttempt.Accepted
        }
    }
}
