package com.soturine.volumeok.domain

data class VolumeChange(val atMillis: Long, val from: Int, val to: Int)

sealed interface CircuitBreakerDecision {
    data class Closed(val opposingTransitions: Int) : CircuitBreakerDecision

    data class Open(val failure: FailureCode = FailureCode.OSCILLATION_DETECTED) : CircuitBreakerDecision
}

class OscillationCircuitBreaker(
    private val windowMillis: Long = DEFAULT_WINDOW_MILLIS,
    private val opposingTransitionsToOpen: Int = DEFAULT_OPPOSING_TRANSITIONS
) {
    init {
        require(windowMillis > 0)
        require(opposingTransitionsToOpen > 0)
    }

    fun evaluate(changes: List<VolumeChange>, nowMillis: Long): CircuitBreakerDecision {
        val directions =
            changes
                .asSequence()
                .filter { it.atMillis in (nowMillis - windowMillis)..nowMillis }
                .sortedBy { it.atMillis }
                .mapNotNull { change ->
                    when {
                        change.to > change.from -> 1
                        change.to < change.from -> -1
                        else -> null
                    }
                }.toList()

        val opposingTransitions = directions.zipWithNext().count { (previous, next) -> previous != next }

        return if (opposingTransitions >= opposingTransitionsToOpen) {
            CircuitBreakerDecision.Open()
        } else {
            CircuitBreakerDecision.Closed(opposingTransitions)
        }
    }

    companion object {
        const val DEFAULT_WINDOW_MILLIS: Long = 30_000
        const val DEFAULT_OPPOSING_TRANSITIONS: Int = 5
    }
}
