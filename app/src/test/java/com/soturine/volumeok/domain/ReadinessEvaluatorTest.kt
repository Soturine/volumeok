package com.soturine.volumeok.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadinessEvaluatorTest {
    private val evaluator = ReadinessEvaluator()

    @Test
    fun `complete healthy evidence is ready`() {
        val result = evaluator.evaluate(healthySnapshot(), evaluatedAtMillis = 2)

        assertEquals(ReadinessStatus.READY, result.status)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun `muted ringtone requires action`() {
        val snapshot = healthySnapshot().copy(ringVolume = Reading.Available(RingVolume(0, 7)))

        val result = evaluator.evaluate(snapshot, evaluatedAtMillis = 2)

        assertEquals(ReadinessStatus.ACTION_REQUIRED, result.status)
        assertEquals(ReadinessIssueCode.RING_VOLUME_MUTED, result.issues.single().code)
    }

    @Test
    fun `lowest nonzero ringtone needs attention`() {
        val snapshot = healthySnapshot().copy(ringVolume = Reading.Available(RingVolume(1, 7)))

        val result = evaluator.evaluate(snapshot, evaluatedAtMillis = 2)

        assertEquals(ReadinessStatus.ATTENTION, result.status)
        assertEquals(ReadinessIssueCode.RING_VOLUME_LOWEST_NONZERO, result.issues.single().code)
    }

    @Test
    fun `ringtone at level two has no low volume issue`() {
        val snapshot = healthySnapshot().copy(ringVolume = Reading.Available(RingVolume(2, 7)))

        assertEquals(ReadinessStatus.READY, evaluator.evaluate(snapshot, 2).status)
    }

    @Test
    fun `missing mandatory evidence is never ready`() {
        val snapshot = healthySnapshot().copy(
            dndState =
            Reading.Unavailable(
                CapabilityStatus.PERMISSION_REQUIRED,
                FailureCode.PERMISSION_REQUIRED
            )
        )

        val result = evaluator.evaluate(snapshot, evaluatedAtMillis = 2)

        assertEquals(ReadinessStatus.UNKNOWN, result.status)
        assertTrue(result.issues.any { it.code == ReadinessIssueCode.DND_STATE_UNKNOWN })
    }

    @Test
    fun `unknown takes precedence without hiding known action and attention issues`() {
        val snapshot = healthySnapshot().copy(
            ringVolume = Reading.Available(RingVolume(0, 7)),
            dndState = Reading.Unavailable(CapabilityStatus.UNKNOWN, FailureCode.DND_STATE_UNKNOWN),
            audioRoute = Reading.Unavailable(CapabilityStatus.UNKNOWN, FailureCode.AUDIO_ROUTE_UNKNOWN)
        )

        val result = evaluator.evaluate(snapshot, evaluatedAtMillis = 2)

        assertEquals(ReadinessStatus.UNKNOWN, result.status)
        assertEquals(
            listOf(
                ReadinessIssueCode.RING_VOLUME_MUTED,
                ReadinessIssueCode.DND_STATE_UNKNOWN,
                ReadinessIssueCode.AUDIO_ROUTE_UNKNOWN
            ),
            result.issues.map { it.code }
        )
    }

    @Test
    fun `silent and vibrate modes have distinct issues`() {
        val silent = healthySnapshot().copy(ringerMode = Reading.Available(RingerMode.SILENT))
        val vibrate = healthySnapshot().copy(ringerMode = Reading.Available(RingerMode.VIBRATE))

        assertEquals(
            ReadinessIssueCode.RINGER_MODE_SILENT,
            evaluator.evaluate(silent, 2).issues.single().code
        )
        assertEquals(
            ReadinessIssueCode.RINGER_MODE_VIBRATE_ONLY,
            evaluator.evaluate(vibrate, 2).issues.single().code
        )
    }

    @Test
    fun `dnd is evaluated independently from nonzero ringtone`() {
        val snapshot = healthySnapshot().copy(
            ringVolume = Reading.Available(RingVolume(current = 7, maximum = 7)),
            dndState = Reading.Available(DndState.TOTAL_SILENCE)
        )

        val result = evaluator.evaluate(snapshot, evaluatedAtMillis = 2)

        assertEquals(ReadinessStatus.ACTION_REQUIRED, result.status)
        assertTrue(result.issues.any { it.code == ReadinessIssueCode.DND_ACTIVE })
    }

    @Test
    fun `connected device evidence does not claim active routing`() {
        val snapshot = healthySnapshot().copy(
            audioRoute = Reading.Available(AudioRouteEvidence.BLUETOOTH_DEVICE_PRESENT)
        )

        assertEquals(ReadinessStatus.READY, evaluator.evaluate(snapshot, 2).status)
    }

    @Test
    fun `unknown route prevents a green status without becoming mandatory unknown`() {
        val snapshot = healthySnapshot().copy(
            audioRoute =
            Reading.Unavailable(
                CapabilityStatus.UNKNOWN,
                FailureCode.AUDIO_ROUTE_UNKNOWN
            )
        )

        val result = evaluator.evaluate(snapshot, 2)

        assertEquals(ReadinessStatus.ATTENTION, result.status)
        assertEquals(ReadinessIssueCode.AUDIO_ROUTE_UNKNOWN, result.issues.single().code)
    }

    @Test
    fun `explicit capability inconsistency is unknown`() {
        val snapshot = healthySnapshot().copy(
            capabilities = mapOf(
                SoundCapability.READ_RING_VOLUME to CapabilityStatus.UNSUPPORTED
            )
        )

        val result = evaluator.evaluate(snapshot, 2)

        assertEquals(ReadinessStatus.UNKNOWN, result.status)
        assertEquals(
            ReadinessIssue(
                code = ReadinessIssueCode.CAPABILITY_STATUS_INCONSISTENT,
                capability = SoundCapability.READ_RING_VOLUME
            ),
            result.issues.single()
        )
    }

    @Test
    fun `invalid ringtone evidence is unknown`() {
        val snapshot = healthySnapshot().copy(ringVolume = Reading.Available(RingVolume(8, 7)))

        val result = evaluator.evaluate(snapshot, 2)

        assertEquals(ReadinessStatus.UNKNOWN, result.status)
        assertEquals(ReadinessIssueCode.RING_VOLUME_INVALID, result.issues.single().code)
    }

    private fun healthySnapshot() = SoundSnapshot(
        capturedAtMillis = 1,
        ringVolume = Reading.Available(RingVolume(current = 4, maximum = 7)),
        ringerMode = Reading.Available(RingerMode.NORMAL),
        dndState = Reading.Available(DndState.OFF),
        audioRoute = Reading.Available(AudioRouteEvidence.BUILT_IN_DEVICE_AVAILABLE)
    )
}
