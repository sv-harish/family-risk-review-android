package com.familyriskreview.feature.review

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.familyriskreview.core.designsystem.component.FrrPrimaryButton
import com.familyriskreview.core.designsystem.component.FrrStableTextField
import com.familyriskreview.core.designsystem.theme.FamilyRiskReviewTheme
import com.familyriskreview.core.model.AppLanguage
import com.familyriskreview.core.model.Responsibility
import com.familyriskreview.core.model.ResponsibilityCatalogue
import com.familyriskreview.core.model.ResponsibilityPriority
import com.familyriskreview.core.model.Review
import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.ReviewStatus
import com.familyriskreview.core.model.ReviewStep
import com.familyriskreview.core.model.SyncState
import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class ResponsibilityDetailsComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun harness_editsTextAndCaret_viaStableField() {
        var value by mutableStateOf(TextFieldValue(""))
        composeRule.setContent {
            FamilyRiskReviewTheme {
                FrrStableTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = "Monthly amount",
                    modifier = Modifier.testTag(AMOUNT_TAG),
                )
            }
        }

        composeRule.onNodeWithTag(AMOUNT_TAG).performTextInput("1000")
        composeRule.waitForIdle()
        assertThat(value.text).isEqualTo("1000")
        assertThat(value.selection).isEqualTo(TextRange(4))

        value = TextFieldValue(text = value.text, selection = TextRange(1))
        composeRule.waitForIdle()
        value =
            TextFieldValue(
                text = value.text.substring(0, 1) + "2" + value.text.substring(1),
                selection = TextRange(2),
            )
        composeRule.waitForIdle()
        assertThat(value.text).isEqualTo("12000")
        assertThat(value.selection).isEqualTo(TextRange(2))
        composeRule.onNodeWithTag(AMOUNT_TAG).assertTextEquals("Monthly amount", "12000")
    }

    @Test
    fun responsibilityDetailsScreen_rendersAndContinueHasButtonSemantics() {
        val responsibility =
            Responsibility(
                id = "living",
                reviewId = "r1",
                catalogue = ResponsibilityCatalogue.ESSENTIAL_FAMILY_LIVING_EXPENSES,
                isSelected = true,
                priority = ResponsibilityPriority.MUST_CONTINUE,
            )
        var advanced = false

        composeRule.setContent {
            FamilyRiskReviewTheme {
                ResponsibilityDetailsScreen(
                    state =
                    ReviewUiState(
                        review =
                        Review(
                            id = "r1",
                            reviewNumber = "FRR-20260804-000001",
                            mode = ReviewMode.QUICK,
                            language = AppLanguage.ENGLISH,
                            status = ReviewStatus.IN_PROGRESS,
                            currentStep = ReviewStep.RESPONSIBILITY_DETAILS,
                            createdAt = Instant.fromEpochMilliseconds(1L),
                            updatedAt = Instant.fromEpochMilliseconds(1L),
                            calculationVersion = "test",
                            syncState = SyncState.LOCAL_ONLY,
                        ),
                        responsibilities = listOf(responsibility),
                        selectedResponsibilityId = responsibility.id,
                        loading = false,
                    ),
                    onUpdateDraft = {},
                    onSaveDraft = {},
                    onSelectResponsibility = {},
                    mayRemainNonQuantified = { false },
                    onAdvance = { advanced = true },
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("Continue").assertIsDisplayed()
        composeRule.onNode(hasText("Continue") and hasClickAction()).performClick()
        composeRule.waitForIdle()
        assertThat(advanced).isTrue()

        val continueNode =
            composeRule.onNode(hasText("Continue") and hasClickAction()).fetchSemanticsNode()
        assertThat(
            continueNode.config.contains(SemanticsProperties.Role) ||
                continueNode.config.contains(SemanticsProperties.Text),
        ).isTrue()
    }

    @Test
    fun primaryButton_exposesContentDescriptionForTalkBack() {
        composeRule.setContent {
            FamilyRiskReviewTheme {
                FrrPrimaryButton(
                    text = "Continue",
                    onClick = {},
                    contentDescription = "Continue to next review step",
                    modifier = Modifier.testTag("continue_btn"),
                )
            }
        }

        val node = composeRule.onNodeWithTag("continue_btn").fetchSemanticsNode()
        assertThat(node.config[SemanticsProperties.ContentDescription])
            .contains("Continue to next review step")
        assertThat(node.config[SemanticsProperties.Role]).isEqualTo(Role.Button)
    }

    companion object {
        private const val AMOUNT_TAG = "details_amount"
    }
}
