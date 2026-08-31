@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.soturine.volumeok.feature.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.soturine.volumeok.R
import com.soturine.volumeok.application.ControlledTestStatus
import com.soturine.volumeok.domain.AudioRouteEvidence
import com.soturine.volumeok.domain.CapabilityStatus
import com.soturine.volumeok.domain.DndState
import com.soturine.volumeok.domain.ReadinessStatus
import com.soturine.volumeok.domain.Reading
import com.soturine.volumeok.domain.RingVolume
import com.soturine.volumeok.domain.RingerMode
import com.soturine.volumeok.domain.SoundCapability

@Composable
fun HomeScreen(state: HomeUiState, onEvent: (HomeEvent) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() }
            )
            ReadinessCard(state)
            DiagnosticCard(state)
            Text(stringResource(R.string.protection_not_validated))
            state.controlledTestReport?.let {
                Text(stringResource(controlledStatusResource(it.status)))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { onEvent(HomeEvent.Refresh) }) {
                    Text(stringResource(R.string.refresh))
                }
                Button(
                    onClick = { onEvent(HomeEvent.RunControlledTest) },
                    enabled = state.snapshot?.capabilities?.get(SoundCapability.WRITE_RING_VOLUME) ==
                        CapabilityStatus.SUPPORTED
                ) {
                    Text(stringResource(R.string.controlled_test))
                }
            }
        }
    }
}

@Composable
private fun ReadinessCard(state: HomeUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.current_readiness),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = stringResource(readinessResource(state.readiness?.status)),
                style = MaterialTheme.typography.headlineSmall
            )
            if (state.readiness?.issues?.isNotEmpty() == true) {
                Text(stringResource(R.string.review_evidence))
            }
        }
    }
}

@Composable
private fun DiagnosticCard(state: HomeUiState) {
    val snapshot = state.snapshot
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.diagnostic_evidence),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() }
            )
            Text(stringResource(R.string.ringtone_value, ringVolumeText(snapshot?.ringVolume)))
            Text(stringResource(R.string.ringer_mode_value, ringerModeText(snapshot?.ringerMode)))
            Text(stringResource(R.string.dnd_value, dndText(snapshot?.dndState)))
            Text(stringResource(R.string.audio_route_value, routeText(snapshot?.audioRoute)))
            Text(stringResource(R.string.protection_runtime_value, stringResource(R.string.runtime_stopped)))
        }
    }
}

@Composable
private fun ringVolumeText(reading: Reading<RingVolume>?): String = when (reading) {
    is Reading.Available -> stringResource(R.string.volume_fraction, reading.value.current, reading.value.maximum)
    else -> stringResource(R.string.unavailable)
}

@Composable
private fun ringerModeText(reading: Reading<RingerMode>?): String = when ((reading as? Reading.Available)?.value) {
    RingerMode.NORMAL -> stringResource(R.string.ringer_normal)
    RingerMode.VIBRATE -> stringResource(R.string.ringer_vibrate)
    RingerMode.SILENT -> stringResource(R.string.ringer_silent)
    null -> stringResource(R.string.unavailable)
}

@Composable
private fun dndText(reading: Reading<DndState>?): String = when ((reading as? Reading.Available)?.value) {
    DndState.OFF -> stringResource(R.string.dnd_off)
    DndState.PRIORITY_ONLY -> stringResource(R.string.dnd_priority)
    DndState.ALARMS_ONLY -> stringResource(R.string.dnd_alarms)
    DndState.TOTAL_SILENCE -> stringResource(R.string.dnd_silence)
    null -> stringResource(R.string.unavailable)
}

@Composable
private fun routeText(reading: Reading<AudioRouteEvidence>?): String = when ((reading as? Reading.Available)?.value) {
    AudioRouteEvidence.BUILT_IN_DEVICE_AVAILABLE -> stringResource(R.string.route_builtin_present)
    AudioRouteEvidence.WIRED_DEVICE_PRESENT -> stringResource(R.string.route_wired_present)
    AudioRouteEvidence.BLUETOOTH_DEVICE_PRESENT -> stringResource(R.string.route_bluetooth_present)
    AudioRouteEvidence.REMOTE_DEVICE_PRESENT -> stringResource(R.string.route_remote_present)
    AudioRouteEvidence.MULTIPLE_OUTPUT_DEVICES_PRESENT -> stringResource(R.string.route_multiple_present)
    null -> stringResource(R.string.unavailable)
}

@StringRes
private fun readinessResource(status: ReadinessStatus?): Int = when (status) {
    ReadinessStatus.READY -> R.string.readiness_ready
    ReadinessStatus.ATTENTION -> R.string.readiness_attention
    ReadinessStatus.ACTION_REQUIRED -> R.string.readiness_action_required
    ReadinessStatus.UNKNOWN, null -> R.string.readiness_unknown
}

@StringRes
private fun controlledStatusResource(status: ControlledTestStatus): Int = when (status) {
    ControlledTestStatus.EFFECTIVE_AND_RESTORED -> R.string.controlled_effective_restored
    ControlledTestStatus.NOT_EFFECTIVE -> R.string.controlled_not_effective
    ControlledTestStatus.UNEXPECTED_READBACK -> R.string.controlled_unexpected_readback
    ControlledTestStatus.RESTORE_FAILED -> R.string.controlled_restore_failed
    ControlledTestStatus.READ_UNAVAILABLE -> R.string.controlled_read_unavailable
    ControlledTestStatus.NO_SAFE_TEST_CHANGE -> R.string.controlled_no_safe_change
}
