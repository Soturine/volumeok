package com.soturine.volumeok.application

import com.soturine.volumeok.domain.CapabilityStatus
import com.soturine.volumeok.domain.FailureCode
import com.soturine.volumeok.domain.Reading
import com.soturine.volumeok.domain.RingVolume
import org.junit.Assert.assertEquals
import org.junit.Test

class CorrectLowRingVolumeTest {
    @Test
    fun `lowest nonzero volume is corrected to two after fresh readback`() {
        val gateway = FakeGateway(readings = mutableListOf(volume(1, 7), volume(2, 7)))

        val report = CorrectLowRingVolume(gateway).execute()

        assertEquals(LowRingVolumeCorrectionStatus.EFFECTIVE, report.status)
        assertEquals(1, report.original)
        assertEquals(2, report.requested)
        assertEquals(2, report.observedAfterWrite)
        assertEquals(listOf(2), gateway.writes)
    }

    @Test
    fun `accepted write with unchanged readback is not effective`() {
        val gateway = FakeGateway(readings = mutableListOf(volume(1, 7), volume(1, 7)))

        val report = CorrectLowRingVolume(gateway).execute()

        assertEquals(LowRingVolumeCorrectionStatus.NOT_EFFECTIVE, report.status)
        assertEquals(1, report.observedAfterWrite)
    }

    @Test
    fun `unexpected fresh readback is explicit`() {
        val gateway = FakeGateway(readings = mutableListOf(volume(1, 7), volume(3, 7)))

        val report = CorrectLowRingVolume(gateway).execute()

        assertEquals(LowRingVolumeCorrectionStatus.UNEXPECTED_READBACK, report.status)
        assertEquals(3, report.observedAfterWrite)
    }

    @Test
    fun `unavailable initial read prevents write`() {
        val gateway = FakeGateway(
            readings = mutableListOf(
                Reading.Unavailable(CapabilityStatus.UNKNOWN, FailureCode.READ_FAILED)
            )
        )

        val report = CorrectLowRingVolume(gateway).execute()

        assertEquals(LowRingVolumeCorrectionStatus.READ_UNAVAILABLE, report.status)
        assertEquals(FailureCode.READ_FAILED, report.failure)
        assertEquals(emptyList<Int>(), gateway.writes)
    }

    @Test
    fun `unavailable post-write readback is explicit`() {
        val gateway = FakeGateway(
            readings = mutableListOf(
                volume(1, 7),
                Reading.Unavailable(CapabilityStatus.UNKNOWN, FailureCode.READ_FAILED)
            )
        )

        val report = CorrectLowRingVolume(gateway).execute()

        assertEquals(LowRingVolumeCorrectionStatus.READ_UNAVAILABLE, report.status)
        assertEquals(FailureCode.READ_FAILED, report.failure)
        assertEquals(listOf(2), gateway.writes)
    }

    @Test
    fun `rejected write still performs fresh readback`() {
        val gateway = FakeGateway(
            readings = mutableListOf(volume(1, 7), volume(1, 7)),
            writeAttempt = WriteAttempt.Rejected(FailureCode.WRITE_NOT_ALLOWED)
        )

        val report = CorrectLowRingVolume(gateway).execute()

        assertEquals(LowRingVolumeCorrectionStatus.WRITE_REJECTED, report.status)
        assertEquals(FailureCode.WRITE_NOT_ALLOWED, report.failure)
        assertEquals(1, report.observedAfterWrite)
        assertEquals(2, gateway.readCount)
    }

    @Test
    fun `correction is unavailable outside exact safe one to two transition`() {
        listOf(
            RingVolume(0, 7),
            RingVolume(2, 7),
            RingVolume(1, 2),
            RingVolume(1, 1)
        ).forEach { initial ->
            val gateway = FakeGateway(readings = mutableListOf(Reading.Available(initial)))

            val report = CorrectLowRingVolume(gateway).execute()

            assertEquals(LowRingVolumeCorrectionStatus.NO_SAFE_TARGET, report.status)
            assertEquals(emptyList<Int>(), gateway.writes)
            assertEquals(1, gateway.readCount)
        }
    }

    private fun volume(current: Int, maximum: Int): Reading<RingVolume> =
        Reading.Available(RingVolume(current, maximum))

    private class FakeGateway(
        private val readings: MutableList<Reading<RingVolume>>,
        private val writeAttempt: WriteAttempt = WriteAttempt.Accepted
    ) : RingVolumeGateway {
        val writes = mutableListOf<Int>()
        var readCount = 0
            private set

        override fun read(): Reading<RingVolume> {
            readCount += 1
            return readings.removeAt(0)
        }

        override fun write(target: Int): WriteAttempt {
            writes += target
            return writeAttempt
        }
    }
}
