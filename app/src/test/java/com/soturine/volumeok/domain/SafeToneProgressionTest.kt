package com.soturine.volumeok.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeToneProgressionTest {
    private val progression = SafeToneProgression()

    @Test
    fun `first step is derived from current platform volume without using maximum`() {
        assertEquals(SafeToneStep(3, 7), progression.first(RingVolume(3, 7)))
        assertEquals(SafeToneStep(6, 7), progression.first(RingVolume(7, 7)))
        assertEquals(SafeToneStep(1, 7), progression.first(RingVolume(0, 7)))
    }

    @Test
    fun `progression is bounded below platform maximum`() {
        var step = progression.first(RingVolume(1, 7))
        val values = mutableListOf<Int>()

        while (step != null) {
            values += step.value
            assertTrue(step.value in 1 until step.platformMaximum)
            step = progression.next(step)
        }

        assertEquals(listOf(1, 2, 3, 4, 5, 6), values)
    }

    @Test
    fun `invalid or single-level platform volume cannot start`() {
        assertNull(progression.first(RingVolume(0, 0)))
        assertNull(progression.first(RingVolume(1, 1)))
    }
}
