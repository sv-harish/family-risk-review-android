package com.familyriskreview.feature.review

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import java.io.Serializable

/**
 * Restoration-safe editable drafts kept separate from parsed domain values.
 *
 * Survives Activity recreation via [androidx.lifecycle.SavedStateHandle].
 * Focused raw input must not be overwritten when Room emits the last saved model.
 */
data class TextDraft(
    val text: String = "",
    val selectionStart: Int = 0,
    val selectionEnd: Int = 0,
) : Serializable {
    fun toTextFieldValue(): TextFieldValue {
        val start = selectionStart.coerceIn(0, text.length)
        val end = selectionEnd.coerceIn(0, text.length)
        return TextFieldValue(text = text, selection = TextRange(start, end))
    }

    companion object {
        private const val serialVersionUID = 1L

        fun from(value: TextFieldValue): TextDraft = TextDraft(
            text = value.text,
            selectionStart = value.selection.start,
            selectionEnd = value.selection.end,
        )

        fun of(text: String): TextDraft = TextDraft(text = text, selectionStart = text.length, selectionEnd = text.length)
    }
}

data class HouseholdMemberDraft(
    val memberId: String,
    val label: TextDraft = TextDraft(),
    val age: TextDraft = TextDraft(),
    val relationshipName: String? = null,
    val contributionName: String? = null,
    val dependencyName: String? = null,
    val showAgeValidation: Boolean = false,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

data class ResponsibilityDetailsDraft(
    val responsibilityId: String,
    val label: TextDraft = TextDraft(),
    val monthlyAmount: TextDraft = TextDraft(),
    val currentAmount: TextDraft = TextDraft(),
    val years: TextDraft = TextDraft(),
    val inflationPercent: TextDraft = TextDraft(),
    val amountModelName: String? = null,
    val notYetQuantified: Boolean = false,
    val dirty: Boolean = false,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

data class ReviewEditableDraftState(
    val selectedMemberId: String? = null,
    val selectedResponsibilityId: String? = null,
    val activeOtherResponsibilityId: String? = null,
    val otherLabel: TextDraft = TextDraft(),
    val householdDrafts: Map<String, HouseholdMemberDraft> = emptyMap(),
    val detailsDrafts: Map<String, ResponsibilityDetailsDraft> = emptyMap(),
    val draftDiscardedAfterConflict: Boolean = false,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
