package com.familyriskreview.feature.review

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.familyriskreview.core.designsystem.component.FrrStableTextField
import com.familyriskreview.core.designsystem.component.appendPreservingSelection
import com.familyriskreview.core.designsystem.theme.FamilyRiskReviewTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class FrrStableTextFieldComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun typesCharacterByCharacter_preservesText() {
        val state = installField()

        composeRule.onNodeWithTag(FIELD_TAG).performTextInput("a")
        composeRule.onNodeWithTag(FIELD_TAG).performTextInput("b")
        composeRule.onNodeWithTag(FIELD_TAG).performTextInput("c")

        composeRule.onNodeWithTag(FIELD_TAG).assertTextEquals("Amount", "abc")
        assertThat(state.value.text).isEqualTo("abc")
        assertThat(state.value.selection).isEqualTo(TextRange(3))
    }

    @Test
    fun insertInMiddle_keepsSurroundingText() {
        val state = installField(TextFieldValue(text = "ac", selection = TextRange(1)))
        composeRule.waitForIdle()

        state.value = state.value.appendPreservingSelection("b")
        composeRule.waitForIdle()

        assertThat(state.value.text).isEqualTo("abc")
        assertThat(state.value.selection).isEqualTo(TextRange(2))
        composeRule.onNodeWithTag(FIELD_TAG).assertTextEquals("Amount", "abc")
    }

    @Test
    fun replaceSelection_substitutesRange() {
        val state = installField(TextFieldValue(text = "abcd", selection = TextRange(1, 3)))
        composeRule.waitForIdle()

        state.value = state.value.appendPreservingSelection("Z")
        composeRule.waitForIdle()

        assertThat(state.value.text).isEqualTo("aZd")
        assertThat(state.value.selection).isEqualTo(TextRange(2))
    }

    @Test
    fun delete_clearsAndAllowsRetype() {
        val state = installField(TextFieldValue("hello"))

        composeRule.onNodeWithTag(FIELD_TAG).performTextClearance()
        composeRule.onNodeWithTag(FIELD_TAG).assertTextEquals("Amount", "")
        assertThat(state.value.text).isEmpty()

        composeRule.onNodeWithTag(FIELD_TAG).performTextInput("x")
        assertThat(state.value.text).isEqualTo("x")
    }

    @Test
    fun amountFormattingPath_formatsParsedRupeesOnSync() {
        val state = installField()

        composeRule.onNodeWithTag(FIELD_TAG).performTextReplacement("12500")
        composeRule.waitForIdle()
        assertThat(MoneyInputFormatter.parseRupees(state.value.text)).isEqualTo(12_500L)

        val formatted =
            MoneyInputFormatter.formatOrEmpty(MoneyInputFormatter.parseRupees(state.value.text))
        state.value = TextFieldValue(formatted, TextRange(formatted.length))
        composeRule.waitForIdle()

        assertThat(state.value.text).isEqualTo(MoneyInputFormatter.formatRupees(12_500L))
        composeRule.onNodeWithTag(FIELD_TAG)
            .assertTextEquals("Amount", MoneyInputFormatter.formatRupees(12_500L))
    }

    private fun installField(initial: TextFieldValue = TextFieldValue("")): MutableState<TextFieldValue> {
        val state = mutableStateOf(initial)
        composeRule.setContent {
            FamilyRiskReviewTheme {
                FrrStableTextField(
                    value = state.value,
                    onValueChange = { state.value = it },
                    label = "Amount",
                    modifier = Modifier.testTag(FIELD_TAG),
                )
            }
        }
        return state
    }

    companion object {
        private const val FIELD_TAG = "frr_stable_field"
    }
}
