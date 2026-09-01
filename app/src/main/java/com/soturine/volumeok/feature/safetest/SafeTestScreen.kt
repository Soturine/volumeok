@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.soturine.volumeok.feature.safetest

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.soturine.volumeok.domain.SafeSoundTestState

@Composable
fun SafeTestScreen(
    state: SafeSoundTestState,
    onEvent: (SafeTestEvent) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { closeTest(onEvent, onClose) }
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.safe_test_title),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() }
            )
            when (state) {
                SafeSoundTestState.Explaining -> ExplanationContent(onEvent, onClose)
                is SafeSoundTestState.Playing -> PlayingContent(state, onEvent)
                is SafeSoundTestState.AwaitingConfirmation -> ConfirmationContent(state, onEvent)
                is SafeSoundTestState.ReadyForNextAttempt -> NextAttemptContent(state, onEvent, onClose)
                is SafeSoundTestState.Completed -> CompletedContent(state, onEvent, onClose)
                is SafeSoundTestState.Failed -> FailureContent(state, onEvent, onClose)
            }
        }
    }
}

@Composable
private fun ExplanationContent(onEvent: (SafeTestEvent) -> Unit, onClose: () -> Unit) {
    Text(stringResource(R.string.safe_test_explanation))
    Text(stringResource(R.string.safe_test_safety_note))
    Button(
        onClick = { onEvent(SafeTestEvent.Start) },
        modifier = Modifier.fillMaxWidth().testTag("start_safe_test")
    ) {
        Text(stringResource(R.string.safe_test_start))
    }
    OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.safe_test_cancel))
    }
}

@Composable
private fun PlayingContent(state: SafeSoundTestState.Playing, onEvent: (SafeTestEvent) -> Unit) {
    Text(stringResource(R.string.safe_test_playing, state.step.value, state.step.platformMaximum - 1))
    OutlinedButton(
        onClick = { onEvent(SafeTestEvent.Stop) },
        modifier = Modifier.fillMaxWidth().testTag("stop_safe_test")
    ) {
        Text(stringResource(R.string.safe_test_stop))
    }
}

@Composable
private fun ConfirmationContent(state: SafeSoundTestState.AwaitingConfirmation, onEvent: (SafeTestEvent) -> Unit) {
    Text(
        text = stringResource(R.string.safe_test_did_you_hear, state.step.value),
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.semantics { heading() }
    )
    Button(
        onClick = { onEvent(SafeTestEvent.Heard) },
        modifier = Modifier.fillMaxWidth().testTag("safe_test_heard")
    ) {
        Text(stringResource(R.string.safe_test_yes))
    }
    OutlinedButton(
        onClick = { onEvent(SafeTestEvent.NotHeard) },
        modifier = Modifier.fillMaxWidth().testTag("safe_test_not_heard")
    ) {
        Text(stringResource(R.string.safe_test_no))
    }
}

@Composable
private fun NextAttemptContent(
    state: SafeSoundTestState.ReadyForNextAttempt,
    onEvent: (SafeTestEvent) -> Unit,
    onClose: () -> Unit
) {
    Text(stringResource(R.string.safe_test_next_explanation, state.step.value, state.step.platformMaximum - 1))
    Button(
        onClick = { onEvent(SafeTestEvent.TryAgain) },
        modifier = Modifier.fillMaxWidth().testTag("safe_test_try_again")
    ) {
        Text(stringResource(R.string.safe_test_try_louder))
    }
    OutlinedButton(
        onClick = { closeTest(onEvent, onClose) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.safe_test_stop))
    }
}

@Composable
private fun CompletedContent(
    state: SafeSoundTestState.Completed,
    onEvent: (SafeTestEvent) -> Unit,
    onClose: () -> Unit
) {
    Text(
        text = stringResource(outcomeResource(state.outcome)),
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.semantics { heading() }.testTag("safe_test_result")
    )
    ReturnButton(onEvent, onClose)
}

@Composable
private fun FailureContent(state: SafeSoundTestState.Failed, onEvent: (SafeTestEvent) -> Unit, onClose: () -> Unit) {
    Text(
        text = stringResource(failureResource(state.failure)),
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.semantics { heading() }.testTag("safe_test_failure")
    )
    ReturnButton(onEvent, onClose)
}

@Composable
private fun ReturnButton(onEvent: (SafeTestEvent) -> Unit, onClose: () -> Unit) {
    Button(
        onClick = {
            onEvent(SafeTestEvent.Reset)
            onClose()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.safe_test_done))
    }
}

private fun closeTest(onEvent: (SafeTestEvent) -> Unit, onClose: () -> Unit) {
    onEvent(SafeTestEvent.Stop)
    onEvent(SafeTestEvent.Reset)
    onClose()
}
