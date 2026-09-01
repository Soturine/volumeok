package com.soturine.volumeok.feature.safetest

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.soturine.volumeok.domain.SafeSoundTestOutcome
import com.soturine.volumeok.domain.SafeSoundTestState
import com.soturine.volumeok.domain.SafeToneStep
import com.soturine.volumeok.ui.theme.VolumeOkTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SafeTestScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun toneRequiresExplicitStartAction() {
        val events = mutableListOf<SafeTestEvent>()
        composeRule.setContent {
            VolumeOkTheme {
                SafeTestScreen(SafeSoundTestState.Explaining, events::add, onClose = {})
            }
        }

        composeRule.onNodeWithTag("start_safe_test").assertIsDisplayed().performClick()
        assertEquals(listOf(SafeTestEvent.Start), events)
    }

    @Test
    fun confirmationOffersExplicitHeardAndNotHeardOutcomes() {
        val state = SafeSoundTestState.AwaitingConfirmation(1L, SafeToneStep(2, 7))
        composeRule.setContent {
            VolumeOkTheme {
                SafeTestScreen(state, onEvent = {}, onClose = {})
            }
        }

        composeRule.onNodeWithTag("safe_test_heard").assertIsDisplayed()
        composeRule.onNodeWithTag("safe_test_not_heard").assertIsDisplayed()
    }

    @Test
    fun completedOutcomeIsAnnouncedAsUserConfirmation() {
        val state = SafeSoundTestState.Completed(SafeSoundTestOutcome.HEARD)
        composeRule.setContent {
            VolumeOkTheme {
                SafeTestScreen(state, onEvent = {}, onClose = {})
            }
        }

        composeRule.onNodeWithTag("safe_test_result").assertIsDisplayed()
    }
}
