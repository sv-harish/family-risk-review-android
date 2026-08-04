package com.familyriskreview.feature.review

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.familyriskreview.core.designsystem.component.FrrStableTextField
import com.familyriskreview.core.designsystem.theme.FamilyRiskReviewTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Managed-device / emulator instrumentation path for [FrrStableTextField] caret + text.
 * Runs via `:feature:review:frrTabletApi30DebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class StableTextFieldInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun typesIntoStableTextField_andAssertsSelection() {
        var value by mutableStateOf(TextFieldValue(""))
        composeRule.setContent {
            FamilyRiskReviewTheme {
                FrrStableTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = "Label",
                    modifier = Modifier.testTag(FIELD_TAG),
                )
            }
        }

        composeRule.onNodeWithTag(FIELD_TAG).performTextInput("Hi")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(FIELD_TAG).assertTextEquals("Label", "Hi")
        assertThat(value.text).isEqualTo("Hi")
        assertThat(value.selection).isEqualTo(TextRange(2))
    }

    companion object {
        private const val FIELD_TAG = "instrumented_stable_field"
    }
}
