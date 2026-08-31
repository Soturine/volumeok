package com.soturine.volumeok.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OscillationCircuitBreakerTest {
    private val breaker = OscillationCircuitBreaker()

    @Test
    fun `alternating conflicting changes open the breaker`() {
        val changes =
            listOf(
                VolumeChange(1_000, 70, 0),
                VolumeChange(2_000, 0, 70),
                VolumeChange(3_000, 70, 0),
                VolumeChange(4_000, 0, 70),
                VolumeChange(5_000, 70, 0),
                VolumeChange(6_000, 0, 70)
            )

        assertTrue(breaker.evaluate(changes, nowMillis = 6_000) is CircuitBreakerDecision.Open)
    }

    @Test
    fun `monotonic or old changes do not open the breaker`() {
        val changes =
            listOf(
                VolumeChange(1_000, 1, 2),
                VolumeChange(2_000, 2, 3),
                VolumeChange(3_000, 3, 4),
                VolumeChange(40_000, 4, 3)
            )

        assertEquals(
            CircuitBreakerDecision.Closed(opposingTransitions = 0),
            breaker.evaluate(changes, nowMillis = 40_000)
        )
    }
}
