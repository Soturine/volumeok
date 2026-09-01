@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.soturine.volumeok.feature.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.soturine.volumeok.R
import com.soturine.volumeok.domain.CapabilityStatus
import com.soturine.volumeok.domain.SoundCapability

@Composable
fun HomeScreen(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    onOpenSafeTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() }
            )
            ReadinessCard(state, onEvent)
            ChecksCard(state)
            SafeTestCard(onOpenSafeTest)
            CorrectionResult(state)
            DiagnosticDetails(state, onEvent)
            TextButton(
                onClick = { onEvent(HomeEvent.Refresh) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.refresh_status))
            }
        }
    }
}

@Composable
private fun ReadinessCard(state: HomeUiState, onEvent: (HomeEvent) -> Unit) {
    val status = state.readiness?.status
    Card(
        modifier = Modifier.fillMaxWidth().testTag("readiness_card").semantics {
            liveRegion = LiveRegionMode.Polite
        }
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(readinessTitleResource(status)),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() }
            )
            Text(stringResource(readinessSummaryResource(status)))
            state.readiness?.issues?.forEach { issue ->
                Text(stringResource(issueResource(issue.code)))
            }
            if (state.canCorrectLowRingVolume()) {
                Button(
                    onClick = { onEvent(HomeEvent.CorrectLowRingVolume) },
                    modifier = Modifier.fillMaxWidth().testTag("correct_low_volume")
                ) {
                    Text(stringResource(R.string.correct_low_volume))
                }
            }
        }
    }
}

@Composable
private fun ChecksCard(state: HomeUiState) {
    val snapshot = state.snapshot
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = stringResource(R.string.checks_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() }
            )
            CheckValue(R.string.check_ringtone, ringVolumeText(snapshot?.ringVolume))
            CheckValue(R.string.check_ringer_mode, ringerModeText(snapshot?.ringerMode))
            CheckValue(R.string.check_dnd, dndText(snapshot?.dndState))
            CheckValue(R.string.check_outputs, routeText(snapshot?.audioRoute))
        }
    }
}

@Composable
private fun CheckValue(@StringRes label: Int, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = stringResource(label), style = MaterialTheme.typography.labelLarge)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SafeTestCard(onOpenSafeTest: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.safe_test_home_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() }
            )
            Text(stringResource(R.string.safe_test_home_summary))
            OutlinedButton(
                onClick = onOpenSafeTest,
                modifier = Modifier.fillMaxWidth().testTag("open_safe_test")
            ) {
                Text(stringResource(R.string.safe_test_open))
            }
        }
    }
}

@Composable
private fun CorrectionResult(state: HomeUiState) {
    val report = state.correctionReport ?: return
    Card(
        modifier = Modifier.fillMaxWidth().testTag("correction_result").semantics {
            liveRegion = LiveRegionMode.Polite
        }
    ) {
        Text(
            text = stringResource(correctionStatusResource(report.status)),
            modifier = Modifier.padding(20.dp)
        )
    }
}

@Composable
private fun DiagnosticDetails(state: HomeUiState, onEvent: (HomeEvent) -> Unit) {
    OutlinedButton(
        onClick = { onEvent(HomeEvent.ToggleDiagnosticDetails) },
        modifier = Modifier.fillMaxWidth().testTag("toggle_diagnostics")
    ) {
        Text(
            stringResource(
                if (state.diagnosticDetailsExpanded) {
                    R.string.hide_diagnostic_details
                } else {
                    R.string.show_diagnostic_details
                }
            )
        )
    }
    if (!state.diagnosticDetailsExpanded) return

    Card(modifier = Modifier.fillMaxWidth().testTag("diagnostic_details")) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.diagnostic_details_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() }
            )
            Text(stringResource(R.string.protection_unavailable_detail))
            state.controlledTestReport?.let {
                Text(stringResource(controlledStatusResource(it.status)))
            }
            Button(
                onClick = { onEvent(HomeEvent.RunControlledTest) },
                enabled = state.snapshot?.capabilities?.get(SoundCapability.WRITE_RING_VOLUME) ==
                    CapabilityStatus.SUPPORTED,
                modifier = Modifier.fillMaxWidth().testTag("controlled_test")
            ) {
                Text(stringResource(R.string.controlled_test))
            }
        }
    }
}
