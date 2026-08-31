package com.soturine.volumeok.feature.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.soturine.volumeok.application.ControlledRingVolumeTest
import com.soturine.volumeok.application.ControlledTestReport
import com.soturine.volumeok.application.SoundSnapshotGateway
import com.soturine.volumeok.domain.ProtectionRuntimeState
import com.soturine.volumeok.domain.ReadinessEvaluator
import com.soturine.volumeok.domain.ReadinessResult
import com.soturine.volumeok.domain.SoundSnapshot

data class HomeUiState(
    val snapshot: SoundSnapshot? = null,
    val readiness: ReadinessResult? = null,
    val protectionRuntime: ProtectionRuntimeState = ProtectionRuntimeState.STOPPED,
    val controlledTestReport: ControlledTestReport? = null
)

sealed interface HomeEvent {
    data object Refresh : HomeEvent
    data object RunControlledTest : HomeEvent
}

class HomeViewModel(
    private val gateway: SoundSnapshotGateway,
    private val readinessEvaluator: ReadinessEvaluator = ReadinessEvaluator()
) : ViewModel() {
    var state by mutableStateOf(HomeUiState())
        private set

    init {
        refresh()
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.Refresh -> refresh()
            HomeEvent.RunControlledTest -> runControlledTest()
        }
    }

    private fun refresh() {
        val snapshot = gateway.readSnapshot()
        state = state.copy(
            snapshot = snapshot,
            readiness = readinessEvaluator.evaluate(snapshot, System.currentTimeMillis())
        )
    }

    private fun runControlledTest() {
        val report = ControlledRingVolumeTest(gateway).execute()
        refresh()
        state = state.copy(controlledTestReport = report)
    }

    class Factory(private val gateway: SoundSnapshotGateway) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(gateway) as T
    }
}
