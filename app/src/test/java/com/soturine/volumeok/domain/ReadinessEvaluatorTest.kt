package com.soturine.volumeok.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadinessEvaluatorTest {
    private val evaluator = ReadinessEvaluator(minimumAudibleRingVolume = 2)

    @Test
    fun `complete healthy evidence is ready`() {
        val result = evaluator.evaluate(healthySnapshot(), evaluatedAtMillis = 2)

        assertEquals(ReadinessStatus.READY, result.status)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun `missing mandatory evidence is never ready`() {
        val snapshot =
            healthySnapshot().copy(
                dndState =
                Reading.Unavailable(
                    CapabilityStatus.PERMISSION_REQUIRED,
                    FailureCode.PERMISSION_REQUIRED
                )
            )

        val result = evaluator.evaluate(snapshot, evaluatedAtMillis = 2)

        assertEquals(ReadinessStatus.UNKNOWN, result.status)
        assertTrue(result.issues.any { it.code == ReadinessIssueCode.DND_UNKNOWN })
    }

    @Test
    fun `silent ringer requires action`() {
        val snapshot = healthySnapshot().copy(ringerMode = Reading.Available(RingerMode.SILENT))

        assertEquals(
            ReadinessStatus.ACTION_REQUIRED,
            evaluator.evaluate(snapshot, evaluatedAtMillis = 2).status
        )
    }

    @Test
    fun `dnd is evaluated independently from ringtone percentage`() {
        val snapshot =
            healthySnapshot().copy(
                ringVolume = Reading.Available(RingVolume(current = 7, maximum = 7)),
                dndState = Reading.Available(DndState.TOTAL_SILENCE)
            )

        val result = evaluator.evaluate(snapshot, evaluatedAtMillis = 2)

        assertEquals(ReadinessStatus.ACTION_REQUIRED, result.status)
        assertTrue(result.issues.any { it.code == ReadinessIssueCode.DND_ENABLED })
    }

    @Test
    fun `connected device evidence does not claim active routing`() {
        val snapshot =
            healthySnapshot().copy(
                audioRoute = Reading.Available(AudioRouteEvidence.BLUETOOTH_DEVICE_PRESENT)
            )

        assertEquals(ReadinessStatus.READY, evaluator.evaluate(snapshot, 2).status)
    }

    @Test
    fun `unknown route prevents a green status`() {
        val snapshot =
            healthySnapshot().copy(
                audioRoute =
                Reading.Unavailable(
                    CapabilityStatus.UNKNOWN,
                    FailureCode.AUDIO_ROUTE_UNKNOWN
                )
            )

        assertEquals(ReadinessStatus.ATTENTION, evaluator.evaluate(snapshot, 2).status)
    }

    private fun healthySnapshot() = SoundSnapshot(
        capturedAtMillis = 1,
        ringVolume = Reading.Available(RingVolume(current = 4, maximum = 7)),
        ringerMode = Reading.Available(RingerMode.NORMAL),
        dndState = Reading.Available(DndState.OFF),
        audioRoute = Reading.Available(AudioRouteEvidence.BUILT_IN_DEVICE_AVAILABLE)
    )
}
