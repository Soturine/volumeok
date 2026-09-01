package com.soturine.volumeok.application

import com.soturine.volumeok.domain.RingVolume
import com.soturine.volumeok.domain.SafePlaybackFailure
import com.soturine.volumeok.domain.SafeSoundTestFailure
import com.soturine.volumeok.domain.SafeSoundTestOutcome
import com.soturine.volumeok.domain.SafeSoundTestState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeSoundTestSessionTest {
    @Test
    fun `playback starts only after explicit start`() {
        val player = FakeTonePlayer()
        val session = SafeSoundTestSession(player)

        assertEquals(SafeSoundTestState.Explaining, session.state)
        assertEquals(0, player.startCount)

        session.start(RingVolume(3, 7))

        assertEquals(1, player.startCount)
        assertTrue(session.state is SafeSoundTestState.Playing)
    }

    @Test
    fun `playback completion asks for confirmation and does not claim success`() {
        val player = FakeTonePlayer()
        val session = SafeSoundTestSession(player)
        session.start(RingVolume(3, 7))

        player.complete()

        assertTrue(session.state is SafeSoundTestState.AwaitingConfirmation)
    }

    @Test
    fun `every synchronous and asynchronous transition is observable without polling`() {
        val player = FakeTonePlayer()
        val observed = mutableListOf<SafeSoundTestState>()
        val session = SafeSoundTestSession(player, onStateChanged = observed::add)

        session.start(RingVolume(3, 7))
        player.complete()
        session.notHeard()

        assertTrue(observed[0] is SafeSoundTestState.Playing)
        assertTrue(observed[1] is SafeSoundTestState.AwaitingConfirmation)
        assertTrue(observed[2] is SafeSoundTestState.ReadyForNextAttempt)
        assertEquals(session.state, observed.last())
    }

    @Test
    fun `not heard prepares a bounded next step and requires explicit retry`() {
        val player = FakeTonePlayer()
        val session = SafeSoundTestSession(player)
        session.start(RingVolume(3, 7))
        player.complete()

        session.notHeard()

        assertEquals(1, player.startCount)
        val ready = session.state as SafeSoundTestState.ReadyForNextAttempt
        assertEquals(4, ready.step.value)
        session.tryAgain()
        assertEquals(2, player.startCount)
        assertEquals(4, player.requests.last().step.value)
    }

    @Test
    fun `last safe step finishes without ever requesting maximum`() {
        val player = FakeTonePlayer()
        val session = SafeSoundTestSession(player)
        session.start(RingVolume(6, 7))
        player.complete()

        session.notHeard()

        assertEquals(
            SafeSoundTestState.Completed(SafeSoundTestOutcome.NOT_HEARD_AT_LIMIT),
            session.state
        )
        assertTrue(player.requests.all { it.step.value < it.step.platformMaximum })
    }

    @Test
    fun `stop and lifecycle interruption are immediate and idempotent`() {
        val player = FakeTonePlayer()
        val session = SafeSoundTestSession(player)
        session.start(RingVolume(2, 7))

        session.onLifecycleInterrupted()
        session.stop()

        assertEquals(1, player.stopCount)
        assertEquals(SafeSoundTestState.Completed(SafeSoundTestOutcome.CANCELLED), session.state)
    }

    @Test
    fun `stale callback cannot reactivate cancelled session`() {
        val player = FakeTonePlayer()
        val session = SafeSoundTestSession(player)
        session.start(RingVolume(2, 7))
        val staleListener = player.listener

        session.stop()
        staleListener?.onCompleted()
        staleListener?.onFailure(SafePlaybackFailure.INTERRUPTED)

        assertEquals(SafeSoundTestState.Completed(SafeSoundTestOutcome.CANCELLED), session.state)
    }

    @Test
    fun `invalid ring volume and player rejection fail explicitly`() {
        val invalidSession = SafeSoundTestSession(FakeTonePlayer())
        invalidSession.start(RingVolume(0, 0))
        assertEquals(
            SafeSoundTestState.Failed(SafeSoundTestFailure.InvalidRingVolume),
            invalidSession.state
        )

        val rejectingPlayer = FakeTonePlayer(SafePlaybackFailure.AUDIO_FOCUS_DENIED)
        val rejectedSession = SafeSoundTestSession(rejectingPlayer)
        rejectedSession.start(RingVolume(2, 7))
        assertEquals(
            SafeSoundTestState.Failed(
                SafeSoundTestFailure.Playback(SafePlaybackFailure.AUDIO_FOCUS_DENIED)
            ),
            rejectedSession.state
        )
    }

    @Test
    fun `only one playback can be active`() {
        val player = FakeTonePlayer()
        val session = SafeSoundTestSession(player)

        session.start(RingVolume(2, 7))
        session.start(RingVolume(3, 7))
        session.tryAgain()

        assertEquals(1, player.startCount)
    }

    private class FakeTonePlayer(private val rejection: SafePlaybackFailure? = null) : SafeTonePlayer {
        val requests = mutableListOf<SafeToneRequest>()
        var listener: SafeToneListener? = null
        var startCount = 0
        var stopCount = 0

        override fun start(request: SafeToneRequest, listener: SafeToneListener): SafeToneStartResult {
            startCount++
            requests += request
            this.listener = listener
            return rejection?.let(SafeToneStartResult::Rejected) ?: SafeToneStartResult.Started
        }

        override fun stop() {
            stopCount++
        }

        fun complete() {
            listener?.onCompleted()
        }
    }
}
