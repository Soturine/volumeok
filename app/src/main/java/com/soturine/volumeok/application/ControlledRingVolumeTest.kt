package com.soturine.volumeok.application

import com.soturine.volumeok.domain.FailureCode
import com.soturine.volumeok.domain.Reading
import com.soturine.volumeok.domain.RingVolume

interface RingVolumeGateway {
    fun read(): Reading<RingVolume>

    fun write(target: Int): WriteAttempt
}

sealed interface WriteAttempt {
    data object Accepted : WriteAttempt

    data class Rejected(val failure: FailureCode) : WriteAttempt
}

enum class ControlledTestStatus {
    EFFECTIVE_AND_RESTORED,
    NOT_EFFECTIVE,
    UNEXPECTED_READBACK,
    RESTORE_FAILED,
    READ_UNAVAILABLE,
    NO_SAFE_TEST_CHANGE
}

data class ControlledTestReport(
    val status: ControlledTestStatus,
    val original: Int? = null,
    val requested: Int? = null,
    val observedAfterWrite: Int? = null,
    val observedAfterRestore: Int? = null,
    val writeFailure: FailureCode? = null
)

class ControlledRingVolumeTest(private val gateway: RingVolumeGateway) {
    fun execute(): ControlledTestReport {
        val initial = gateway.read()
        return if (initial is Reading.Available && initial.value.isValid) {
            executeWith(initial.value)
        } else {
            ControlledTestReport(status = ControlledTestStatus.READ_UNAVAILABLE)
        }
    }

    private fun executeWith(initial: RingVolume): ControlledTestReport {
        val original = initial.current
        val target = safeTarget(initial)
            ?: return ControlledTestReport(
                status = ControlledTestStatus.NO_SAFE_TEST_CHANGE,
                original = original
            )

        val attempt = gateway.write(target)
        val observedAfterWrite = gateway.read().availableCurrent()
        val observedAfterRestore = restoreOriginalIfNeeded(original, observedAfterWrite)

        val status =
            when {
                observedAfterWrite == target && observedAfterRestore == original ->
                    ControlledTestStatus.EFFECTIVE_AND_RESTORED
                observedAfterRestore != null && observedAfterRestore != original ->
                    ControlledTestStatus.RESTORE_FAILED
                observedAfterWrite == original -> ControlledTestStatus.NOT_EFFECTIVE
                observedAfterWrite == null -> ControlledTestStatus.READ_UNAVAILABLE
                else -> ControlledTestStatus.UNEXPECTED_READBACK
            }

        return ControlledTestReport(
            status = status,
            original = original,
            requested = target,
            observedAfterWrite = observedAfterWrite,
            observedAfterRestore = observedAfterRestore,
            writeFailure = (attempt as? WriteAttempt.Rejected)?.failure
        )
    }

    private fun safeTarget(volume: RingVolume): Int? = when {
        volume.maximum <= 1 -> null
        volume.current == 0 -> null
        volume.current >= volume.maximum - 1 -> volume.current - 1
        else -> volume.current + 1
    }

    private fun restoreOriginalIfNeeded(original: Int, observedAfterWrite: Int?): Int? {
        if (observedAfterWrite == original) return original
        gateway.write(original)
        return gateway.read().availableCurrent()
    }

    private fun Reading<RingVolume>.availableCurrent(): Int? =
        (this as? Reading.Available)?.value?.takeIf { it.isValid }?.current
}
