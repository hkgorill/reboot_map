package com.rebootmap.presentation.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rebootmap.presentation.theme.RebootMapTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun onboarding_first_step_shows_age_question() {
        composeRule.setContent {
            RebootMapTheme {
                OnboardingScreen(onComplete = { _, _, _ -> })
            }
        }

        composeRule.onNodeWithText("현재 나이가 어떻게 되시나요?").assertIsDisplayed()
        composeRule.onNodeWithText("1 / 3").assertIsDisplayed()
    }
}
