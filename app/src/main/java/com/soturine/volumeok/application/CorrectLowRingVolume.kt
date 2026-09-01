package com.soturine.volumeok.application

import com.soturine.volumeok.domain.FailureCode
import com.soturine.volumeok.domain.Reading
import com.soturine.volumeok.domain.RingVolume

enum class LowRingVolumeCorrectionStatus {
    EFFECTIVE,
    NOT_EFFECTIVE,
    UNEXPECTED_READBACK,
    READ_UNAVAILABLE,
    WRITE_REJECTED,
    NO_SAFE_TARGET
}

data class LowRingVolumeCorrectionReport(
    val status: LowRingVolumeCorrectionStatus,
    val original: Int? = null,
    val requested: Int? = null,
    val observedAfterWrite: Int? = null,
    val failure: FailureCode? = null
)

class CorrectLowRingVolume(private val gateway: RingVolumeGateway) {
    fun execute(): LowRingVolumeCorrectionReport {
        val initial = gateway.read()
        if (initial !is Reading.Available || !initial.value.isValid) {
            return LowRingVolumeCorrectionReport(
                status = LowRingVolumeCorrectionStatus.READ_UNAVAILABLE,
                failure = (initial as? Reading.Unavailable)?.failure
            )
        }

        return if (
            initial.value.current != LOWEST_NONZERO_VOLUME ||
            initial.value.maximum <= CORRECTION_TARGET
        ) {
            LowRingVolumeCorrectionReport(
                status = LowRingVolumeCorrectionStatus.NO_SAFE_TARGET,
                original = initial.value.current
            )
        } else {
            executeWrite(initial.value.current)
        }
    }

    private fun executeWrite(original: Int): LowRingVolumeCorrectionReport {
        val writeAttempt = gateway.write(CORRECTION_TARGET)
        val readback = gateway.read()
        val observed = (readback as? Reading.Available)?.value?.takeIf { it.isValid }?.current
        val status = when {
            writeAttempt is WriteAttempt.Rejected -> LowRingVolumeCorrectionStatus.WRITE_REJECTED
            observed == null -> LowRingVolumeCorrectionStatus.READ_UNAVAILABLE
            observed == CORRECTION_TARGET -> LowRingVolumeCorrectionStatus.EFFECTIVE
            observed == original -> LowRingVolumeCorrectionStatus.NOT_EFFECTIVE
            else -> LowRingVolumeCorrectionStatus.UNEXPECTED_READBACK
        }
        return LowRingVolumeCorrectionReport(
            status = status,
            original = original,
            requested = CORRECTION_TARGET,
            observedAfterWrite = observed,
            failure = when {
                writeAttempt is WriteAttempt.Rejected -> writeAttempt.failure
                readback is Reading.Unavailable -> readback.failure
                else -> null
            }
        )
    }

    private companion object {
        const val LOWEST_NONZERO_VOLUME = 1
        const val CORRECTION_TARGET = 2
    }
}
