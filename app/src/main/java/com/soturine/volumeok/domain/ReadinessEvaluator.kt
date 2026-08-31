package com.soturine.volumeok.domain

enum class ReadinessStatus {
    READY,
    ATTENTION,
    ACTION_REQUIRED,
    UNKNOWN
}

enum class ReadinessIssueCode {
    RING_VOLUME_UNKNOWN,
    RING_VOLUME_INVALID,
    RING_VOLUME_LOW,
    RINGER_MODE_UNKNOWN,
    RINGER_NOT_NORMAL,
    DND_UNKNOWN,
    DND_ENABLED,
    AUDIO_ROUTE_UNKNOWN
}

data class ReadinessIssue(val code: ReadinessIssueCode, val failure: FailureCode? = null)

data class ReadinessResult(val status: ReadinessStatus, val issues: List<ReadinessIssue>, val evaluatedAtMillis: Long)

class ReadinessEvaluator(private val minimumAudibleRingVolume: Int = 1) {
    init {
        require(minimumAudibleRingVolume > 0)
    }

    fun evaluate(snapshot: SoundSnapshot, evaluatedAtMillis: Long): ReadinessResult {
        val issues = buildList {
            evaluateRingVolume(snapshot.ringVolume)?.let(::add)
            evaluateRingerMode(snapshot.ringerMode)?.let(::add)
            evaluateDnd(snapshot.dndState)?.let(::add)
            evaluateRoute(snapshot.audioRoute)?.let(::add)
        }

        val mandatoryEvidenceMissing =
            issues.any {
                it.code in
                    setOf(
                        ReadinessIssueCode.RING_VOLUME_UNKNOWN,
                        ReadinessIssueCode.RING_VOLUME_INVALID,
                        ReadinessIssueCode.RINGER_MODE_UNKNOWN,
                        ReadinessIssueCode.DND_UNKNOWN
                    )
            }

        val actionRequired =
            issues.any {
                it.code in
                    setOf(
                        ReadinessIssueCode.RING_VOLUME_LOW,
                        ReadinessIssueCode.RINGER_NOT_NORMAL,
                        ReadinessIssueCode.DND_ENABLED
                    )
            }

        val status =
            when {
                mandatoryEvidenceMissing -> ReadinessStatus.UNKNOWN
                actionRequired -> ReadinessStatus.ACTION_REQUIRED
                issues.isNotEmpty() -> ReadinessStatus.ATTENTION
                else -> ReadinessStatus.READY
            }

        return ReadinessResult(status = status, issues = issues, evaluatedAtMillis = evaluatedAtMillis)
    }

    private fun evaluateRingVolume(reading: Reading<RingVolume>): ReadinessIssue? = when (reading) {
        is Reading.Unavailable ->
            ReadinessIssue(ReadinessIssueCode.RING_VOLUME_UNKNOWN, reading.failure)
        is Reading.Available ->
            when {
                !reading.value.isValid -> ReadinessIssue(ReadinessIssueCode.RING_VOLUME_INVALID)
                reading.value.current < minimumAudibleRingVolume ->
                    ReadinessIssue(ReadinessIssueCode.RING_VOLUME_LOW)
                else -> null
            }
    }

    private fun evaluateRingerMode(reading: Reading<RingerMode>): ReadinessIssue? = when (reading) {
        is Reading.Unavailable ->
            ReadinessIssue(ReadinessIssueCode.RINGER_MODE_UNKNOWN, reading.failure)
        is Reading.Available ->
            if (reading.value == RingerMode.NORMAL) {
                null
            } else {
                ReadinessIssue(ReadinessIssueCode.RINGER_NOT_NORMAL)
            }
    }

    private fun evaluateDnd(reading: Reading<DndState>): ReadinessIssue? = when (reading) {
        is Reading.Unavailable -> ReadinessIssue(ReadinessIssueCode.DND_UNKNOWN, reading.failure)
        is Reading.Available ->
            if (reading.value == DndState.OFF) {
                null
            } else {
                ReadinessIssue(ReadinessIssueCode.DND_ENABLED)
            }
    }

    private fun evaluateRoute(reading: Reading<AudioRouteEvidence>): ReadinessIssue? = when (reading) {
        is Reading.Unavailable ->
            ReadinessIssue(ReadinessIssueCode.AUDIO_ROUTE_UNKNOWN, reading.failure)
        is Reading.Available -> null
    }
}
