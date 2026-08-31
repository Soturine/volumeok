package com.soturine.volumeok.domain

enum class RestoreMode {
    NOTIFY_ONLY,
    AUTO_RESTORE
}

data class ProtectionPolicy(
    val enabled: Boolean,
    val minimumRingVolume: Int,
    val maximumRingVolume: Int,
    val restoreMode: RestoreMode
) {
    fun validate(platformMaximum: Int): PolicyValidation {
        val boundsAreOrdered = minimumRingVolume >= 0 && minimumRingVolume <= maximumRingVolume
        val boundsFitPlatform = platformMaximum > 0 && maximumRingVolume <= platformMaximum
        return if (boundsAreOrdered && boundsFitPlatform) {
            PolicyValidation.Valid
        } else {
            PolicyValidation.Invalid
        }
    }
}

sealed interface PolicyValidation {
    data object Valid : PolicyValidation

    data object Invalid : PolicyValidation
}

enum class ProtectionRuntimeState {
    STOPPED,
    STARTING,
    ACTIVE,
    PAUSED_BY_USER,
    SUSPENDED_BY_CIRCUIT_BREAKER,
    DEGRADED,
    ERROR
}

data class ProtectionOverride(val startedAtMillis: Long, val expiresAtMillis: Long) {
    init {
        require(expiresAtMillis > startedAtMillis)
    }

    fun isActive(atMillis: Long): Boolean = atMillis < expiresAtMillis
}

fun interface Clock {
    fun nowMillis(): Long
}

enum class NoRestoreReason {
    POLICY_DISABLED,
    NOT_AUTO_RESTORE,
    RUNTIME_INACTIVE,
    PAUSED,
    INVALID_POLICY,
    VOLUME_WITHIN_BOUNDS
}

sealed interface ProtectionDecision {
    data class RestoreTo(val target: Int) : ProtectionDecision

    data class NoRestore(val reason: NoRestoreReason) : ProtectionDecision
}

class ProtectionDecisionEngine(private val clock: Clock) {
    fun decide(
        policy: ProtectionPolicy,
        runtimeState: ProtectionRuntimeState,
        observedRingVolume: Int,
        platformMaximum: Int,
        override: ProtectionOverride?
    ): ProtectionDecision = when {
        !policy.enabled -> ProtectionDecision.NoRestore(NoRestoreReason.POLICY_DISABLED)
        policy.restoreMode != RestoreMode.AUTO_RESTORE ->
            ProtectionDecision.NoRestore(NoRestoreReason.NOT_AUTO_RESTORE)
        runtimeState != ProtectionRuntimeState.ACTIVE ->
            ProtectionDecision.NoRestore(NoRestoreReason.RUNTIME_INACTIVE)
        override?.isActive(clock.nowMillis()) == true ->
            ProtectionDecision.NoRestore(NoRestoreReason.PAUSED)
        policy.validate(platformMaximum) == PolicyValidation.Invalid ->
            ProtectionDecision.NoRestore(NoRestoreReason.INVALID_POLICY)
        observedRingVolume >= policy.minimumRingVolume ->
            ProtectionDecision.NoRestore(NoRestoreReason.VOLUME_WITHIN_BOUNDS)
        else -> ProtectionDecision.RestoreTo(policy.minimumRingVolume)
    }
}

object ProtectionRuntimeTruth {
    fun resolve(policyEnabled: Boolean, liveRuntimeState: ProtectionRuntimeState?): ProtectionRuntimeState =
        if (!policyEnabled) {
            ProtectionRuntimeState.STOPPED
        } else {
            liveRuntimeState ?: ProtectionRuntimeState.STOPPED
        }
}
