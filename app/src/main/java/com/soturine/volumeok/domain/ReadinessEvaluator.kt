package com.soturine.volumeok.domain

enum class ReadinessStatus {
    READY,
    ATTENTION,
    ACTION_REQUIRED,
    UNKNOWN
}

enum class ReadinessIssueCode(val status: ReadinessStatus) {
    RING_VOLUME_UNKNOWN(ReadinessStatus.UNKNOWN),
    RING_VOLUME_INVALID(ReadinessStatus.UNKNOWN),
    RING_VOLUME_MUTED(ReadinessStatus.ACTION_REQUIRED),
    RING_VOLUME_LOWEST_NONZERO(ReadinessStatus.ATTENTION),
    RINGER_MODE_UNKNOWN(ReadinessStatus.UNKNOWN),
    RINGER_MODE_SILENT(ReadinessStatus.ACTION_REQUIRED),
    RINGER_MODE_VIBRATE_ONLY(ReadinessStatus.ACTION_REQUIRED),
    DND_STATE_UNKNOWN(ReadinessStatus.UNKNOWN),
    DND_ACTIVE(ReadinessStatus.ACTION_REQUIRED),
    AUDIO_ROUTE_UNKNOWN(ReadinessStatus.ATTENTION),
    CAPABILITY_STATUS_INCONSISTENT(ReadinessStatus.UNKNOWN)
}

data class ReadinessIssue(
    val code: ReadinessIssueCode,
    val failure: FailureCode? = null,
    val capability: SoundCapability? = null
)

data class ReadinessResult(val status: ReadinessStatus, val issues: List<ReadinessIssue>, val evaluatedAtMillis: Long)

class ReadinessEvaluator {
    fun evaluate(snapshot: SoundSnapshot, evaluatedAtMillis: Long): ReadinessResult {
        val issues = buildList {
            evaluateRingVolume(snapshot.ringVolume)?.let(::add)
            evaluateRingerMode(snapshot.ringerMode)?.let(::add)
            evaluateDnd(snapshot.dndState)?.let(::add)
            evaluateRoute(snapshot.audioRoute)?.let(::add)
            addCapabilityConsistencyIssues(snapshot)
        }

        val status = when {
            issues.any { it.code.status == ReadinessStatus.UNKNOWN } -> ReadinessStatus.UNKNOWN
            issues.any { it.code.status == ReadinessStatus.ACTION_REQUIRED } ->
                ReadinessStatus.ACTION_REQUIRED
            issues.any { it.code.status == ReadinessStatus.ATTENTION } -> ReadinessStatus.ATTENTION
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
                reading.value.current == 0 -> ReadinessIssue(ReadinessIssueCode.RING_VOLUME_MUTED)
                reading.value.current == 1 ->
                    ReadinessIssue(ReadinessIssueCode.RING_VOLUME_LOWEST_NONZERO)
                else -> null
            }
    }

    private fun evaluateRingerMode(reading: Reading<RingerMode>): ReadinessIssue? = when (reading) {
        is Reading.Unavailable ->
            ReadinessIssue(ReadinessIssueCode.RINGER_MODE_UNKNOWN, reading.failure)
        is Reading.Available ->
            when (reading.value) {
                RingerMode.NORMAL -> null
                RingerMode.SILENT -> ReadinessIssue(ReadinessIssueCode.RINGER_MODE_SILENT)
                RingerMode.VIBRATE -> ReadinessIssue(ReadinessIssueCode.RINGER_MODE_VIBRATE_ONLY)
            }
    }

    private fun evaluateDnd(reading: Reading<DndState>): ReadinessIssue? = when (reading) {
        is Reading.Unavailable -> ReadinessIssue(ReadinessIssueCode.DND_STATE_UNKNOWN, reading.failure)
        is Reading.Available ->
            if (reading.value == DndState.OFF) {
                null
            } else {
                ReadinessIssue(ReadinessIssueCode.DND_ACTIVE)
            }
    }

    private fun evaluateRoute(reading: Reading<AudioRouteEvidence>): ReadinessIssue? = when (reading) {
        is Reading.Unavailable ->
            ReadinessIssue(ReadinessIssueCode.AUDIO_ROUTE_UNKNOWN, reading.failure)
        is Reading.Available -> null
    }

    private fun MutableList<ReadinessIssue>.addCapabilityConsistencyIssues(snapshot: SoundSnapshot) {
        addCapabilityConsistencyIssue(
            SoundCapability.READ_RING_VOLUME,
            snapshot.ringVolume,
            snapshot.capabilities
        )
        addCapabilityConsistencyIssue(
            SoundCapability.READ_RINGER_MODE,
            snapshot.ringerMode,
            snapshot.capabilities
        )
        addCapabilityConsistencyIssue(
            SoundCapability.READ_DND,
            snapshot.dndState,
            snapshot.capabilities
        )
        addCapabilityConsistencyIssue(
            SoundCapability.READ_OUTPUT_DEVICES,
            snapshot.audioRoute,
            snapshot.capabilities
        )
    }

    private fun MutableList<ReadinessIssue>.addCapabilityConsistencyIssue(
        capability: SoundCapability,
        reading: Reading<*>,
        capabilities: Map<SoundCapability, CapabilityStatus>
    ) {
        val declaredStatus = capabilities[capability] ?: return
        val observedStatus = when (reading) {
            is Reading.Available -> CapabilityStatus.SUPPORTED
            is Reading.Unavailable -> reading.capability
        }
        if (declaredStatus != observedStatus) {
            add(
                ReadinessIssue(
                    code = ReadinessIssueCode.CAPABILITY_STATUS_INCONSISTENT,
                    capability = capability
                )
            )
        }
    }
}
