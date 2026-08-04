package com.familyriskreview.feature.review

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class ReviewEditableDraftStateTest {
    @Test
    fun textDraft_roundTripsTextFieldValueSelection() {
        val original =
            TextFieldValue(
                text = "Family living",
                selection = TextRange(3, 9),
            )

        val draft = TextDraft.from(original)
        val restored = draft.toTextFieldValue()

        assertThat(restored.text).isEqualTo("Family living")
        assertThat(restored.selection.start).isEqualTo(3)
        assertThat(restored.selection.end).isEqualTo(9)
        assertThat(draft.selectionStart).isEqualTo(3)
        assertThat(draft.selectionEnd).isEqualTo(9)
    }

    @Test
    fun textDraft_coercesOutOfRangeSelection() {
        val draft = TextDraft(text = "abc", selectionStart = 10, selectionEnd = -1)
        val value = draft.toTextFieldValue()
        assertThat(value.selection.start).isEqualTo(3)
        assertThat(value.selection.end).isEqualTo(0)
    }

    @Test
    fun savedStateHandle_putGet_preservesMidTextCaret() {
        val handle = SavedStateHandle()
        val midCaret =
            ReviewEditableDraftState(
                selectedResponsibilityId = "resp-1",
                otherLabel =
                TextDraft(
                    text = "Custom support label",
                    selectionStart = 7,
                    selectionEnd = 7,
                ),
                detailsDrafts =
                mapOf(
                    "resp-1" to
                        ResponsibilityDetailsDraft(
                            responsibilityId = "resp-1",
                            monthlyAmount =
                            TextDraft(
                                text = "12,500",
                                selectionStart = 3,
                                selectionEnd = 3,
                            ),
                            dirty = true,
                        ),
                ),
            )

        handle[ReviewViewModel.KEY_DRAFTS] = midCaret
        val restored = handle.get<ReviewEditableDraftState>(ReviewViewModel.KEY_DRAFTS)

        assertThat(restored).isNotNull()
        assertThat(restored!!.otherLabel.text).isEqualTo("Custom support label")
        assertThat(restored.otherLabel.selectionStart).isEqualTo(7)
        assertThat(restored.otherLabel.selectionEnd).isEqualTo(7)
        assertThat(restored.otherLabel.toTextFieldValue().selection)
            .isEqualTo(TextRange(7))

        val monthly = restored.detailsDrafts["resp-1"]!!.monthlyAmount
        assertThat(monthly.text).isEqualTo("12,500")
        assertThat(monthly.selectionStart).isEqualTo(3)
        assertThat(monthly.toTextFieldValue().selection).isEqualTo(TextRange(3))
    }
}
