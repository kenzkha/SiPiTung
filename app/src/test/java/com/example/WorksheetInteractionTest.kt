package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.data.ChildProfile
import com.example.data.MathTopic
import com.example.ui.screens.WorksheetScreen
import com.example.ui.theme.PintarHitungTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WorksheetInteractionTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun clickingKeypadDigitsFillsActiveDigitBox() {
        val profile = ChildProfile(
            userId = "test_uid",
            childName = "Raka",
            grade = 2L,
            totalStars = 50L
        )

        composeRule.setContent {
            PintarHitungTheme {
                WorksheetScreen(
                    profile = profile,
                    selectedTopic = MathTopic.PENGURANGAN,
                    onSelectTopic = {},
                    onSelectGrade = {},
                    onCompleteWorksheet = { _, _, _, _, _, _, _ -> }
                )
            }
        }

        // Verify keypad digit 7 is clickable and writes into the active digit box
        composeRule.onNodeWithTag("keypad_digit_7").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("keypad_digit_3").assertIsDisplayed().performClick()

        // First problem for Grade 2 subtraction has 1 or 2 digits; digit_box_1_0 must now hold a digit
        composeRule.onNodeWithTag("digit_box_1_0").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("keypad_backspace").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("keypad_digit_5").performClick()
        composeRule.onNodeWithTag("digit_box_1_0").assertTextContains("5")
    }
}
