package com.familyriskreview.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import com.familyriskreview.core.designsystem.theme.FrrTouchTarget
import com.familyriskreview.core.designsystem.theme.frrColors

/**
 * Stable text field that keeps [TextFieldValue] (text + selection) as the source of truth.
 *
 * Callers should store editable raw text separately from parsed domain values and only
 * push domain-driven updates when the field is not actively focused (or via [externalValue]
 * when identity-stable and intentional).
 */
@Composable
fun FrrStableTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onFocusLost: (() -> Unit)? = null,
) {
    val colors = frrColors()
    var focused by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier =
        modifier
            .fillMaxWidth()
            .heightIn(min = FrrTouchTarget.comfortable)
            .onFocusChanged { state ->
                val nowFocused = state.isFocused
                if (focused && !nowFocused) {
                    onFocusLost?.invoke()
                }
                focused = nowFocused
            },
        enabled = enabled,
        singleLine = singleLine,
        isError = isError,
        label = label?.let { { Text(it) } },
        supportingText = supportingText?.let { { Text(it) } },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        colors =
        OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primaryAction,
            unfocusedBorderColor = colors.outline,
            errorBorderColor = colors.blockingError,
            focusedLabelColor = colors.primaryAction,
            cursorColor = colors.primaryAction,
        ),
    )
}

/** Helper for tests and ViewModels that only hold a String while preserving caret on append. */
fun TextFieldValue.appendPreservingSelection(char: String): TextFieldValue {
    val insertAt = selection.start.coerceIn(0, text.length)
    val next = text.substring(0, insertAt) + char + text.substring(selection.end.coerceAtMost(text.length))
    val caret = insertAt + char.length
    return TextFieldValue(text = next, selection = TextRange(caret))
}
