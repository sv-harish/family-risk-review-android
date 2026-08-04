package com.familyriskreview.core.designsystem.component

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StableTextFieldTest {
    @Test
    fun appendPreservingSelection_insertsAtCaret() {
        val start = TextFieldValue(text = "abc", selection = TextRange(1))
        val next = start.appendPreservingSelection("X")
        assertThat(next.text).isEqualTo("aXbc")
        assertThat(next.selection).isEqualTo(TextRange(2))
    }

    @Test
    fun appendPreservingSelection_replacesSelectionRange() {
        val start = TextFieldValue(text = "abcd", selection = TextRange(1, 3))
        val next = start.appendPreservingSelection("Z")
        assertThat(next.text).isEqualTo("aZd")
        assertThat(next.selection).isEqualTo(TextRange(2))
    }
}
