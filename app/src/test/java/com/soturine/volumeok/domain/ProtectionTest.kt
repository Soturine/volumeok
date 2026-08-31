package com.soturine.volumeok.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ProtectionTest {
    private val clock = MutableClock(1_000)
    private val engine = ProtectionDecisionEngine(clock)
    private val policy =
        ProtectionPolicy(
            enabled = true,
            minimumRingVolume = 3,
            maximumRingVolume = 6,
            restoreMode = RestoreMode.AUTO_RESTORE
        )

    @Test
    fun `restore target respects policy and platform bounds`() {
        assertEquals(
            ProtectionDecision.RestoreTo(3),
            engine.decide(policy, ProtectionRuntimeState.ACTIVE, 0, platformMaximum = 7, override = null)
        )
        assertEquals(PolicyValidation.Invalid, policy.validate(platformMaximum = 5))
    }

    @Test
    fun `disabled policy never restores`() {
        val disabled = policy.copy(enabled = false)

        assertEquals(
            ProtectionDecision.NoRestore(NoRestoreReason.POLICY_DISABLED),
            engine.decide(disabled, ProtectionRuntimeState.ACTIVE, 0, 7, null)
        )
    }

    @Test
    fun `pause suppresses restore until expiry`() {
        val override = ProtectionOverride(startedAtMillis = 500, expiresAtMillis = 2_000)

        assertEquals(
            ProtectionDecision.NoRestore(NoRestoreReason.PAUSED),
            engine.decide(policy, ProtectionRuntimeState.ACTIVE, 0, 7, override)
        )

        clock.current = 2_000

        assertEquals(
            ProtectionDecision.RestoreTo(3),
            engine.decide(policy, ProtectionRuntimeState.ACTIVE, 0, 7, override)
        )
    }

    @Test
    fun `inactive runtime cannot restore or appear active after restart`() {
        assertEquals(
            ProtectionDecision.NoRestore(NoRestoreReason.RUNTIME_INACTIVE),
            engine.decide(policy, ProtectionRuntimeState.STOPPED, 0, 7, null)
        )
        assertEquals(
            ProtectionRuntimeState.STOPPED,
            ProtectionRuntimeTruth.resolve(policyEnabled = true, liveRuntimeState = null)
        )
    }

    private class MutableClock(var current: Long) : Clock {
        override fun nowMillis(): Long = current
    }
}
