package com.soturine.volumeok.feature.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.soturine.volumeok.R
import com.soturine.volumeok.domain.AudioRouteEvidence
import com.soturine.volumeok.domain.CapabilityStatus
import com.soturine.volumeok.domain.DndState
import com.soturine.volumeok.domain.ReadinessEvaluator
import com.soturine.volumeok.domain.Reading
import com.soturine.volumeok.domain.RingVolume
import com.soturine.volumeok.domain.RingerMode
import com.soturine.volumeok.domain.SoundCapability
import com.soturine.volumeok.domain.SoundSnapshot
import com.soturine.volumeok.ui.theme.VolumeOkTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun lowVolumeIssueShowsProductCopyAndSupportedCorrection() {
        val state = stateFor(volume = 1, writeCapability = CapabilityStatus.SUPPORTED)
        val issueCopy = targetString(R.string.issue_ring_volume_low)

        composeRule.setContent {
            VolumeOkTheme {
                HomeScreen(state, onEvent = {}, onOpenSafeTest = {})
            }
        }

        composeRule.onNodeWithText(issueCopy).assertIsDisplayed()
        composeRule.onNodeWithTag("correct_low_volume").assertIsDisplayed()
    }

    @Test
    fun unsupportedOrUnsafeCorrectionIsNotOffered() {
        val unsupported = stateFor(volume = 1, writeCapability = CapabilityStatus.UNSUPPORTED)
        composeRule.setContent {
            VolumeOkTheme {
                HomeScreen(unsupported, onEvent = {}, onOpenSafeTest = {})
            }
        }
        composeRule.onNodeWithTag("correct_low_volume").assertDoesNotExist()
    }

    @Test
    fun engineeringControlsRemainBehindExpandableDetails() {
        var state by mutableStateOf(stateFor(volume = 2, writeCapability = CapabilityStatus.SUPPORTED))
        composeRule.setContent {
            VolumeOkTheme {
                HomeScreen(
                    state,
                    onEvent = {
                        if (it == HomeEvent.ToggleDiagnosticDetails) {
                            state = state.copy(diagnosticDetailsExpanded = true)
                        }
                    },
                    onOpenSafeTest = {}
                )
            }
        }

        composeRule.onNodeWithTag("controlled_test").assertDoesNotExist()
        composeRule.onNodeWithTag("toggle_diagnostics").performClick()
        composeRule.onNodeWithTag("controlled_test").assertExists()
    }

    private fun stateFor(volume: Int, writeCapability: CapabilityStatus): HomeUiState {
        val snapshot = SoundSnapshot(
            capturedAtMillis = 1L,
            ringVolume = Reading.Available(RingVolume(volume, 7)),
            ringerMode = Reading.Available(RingerMode.NORMAL),
            dndState = Reading.Available(DndState.OFF),
            audioRoute = Reading.Available(AudioRouteEvidence.BUILT_IN_DEVICE_AVAILABLE),
            capabilities = mapOf(SoundCapability.WRITE_RING_VOLUME to writeCapability)
        )
        return HomeUiState(snapshot = snapshot, readiness = ReadinessEvaluator().evaluate(snapshot, 1L))
    }

    private fun targetString(resource: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resource)
}
