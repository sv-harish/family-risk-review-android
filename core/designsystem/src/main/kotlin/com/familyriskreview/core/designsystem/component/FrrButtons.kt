package com.familyriskreview.core.designsystem.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.familyriskreview.core.designsystem.theme.FrrColors

/**
 * Primary CTA sized for tablet touch targets (52–56dp preferred).
 */
@Composable
fun FrrPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .heightIn(min = 52.dp)
            .defaultMinSize(minWidth = 120.dp)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics {
                        this.contentDescription = contentDescription
                        role = Role.Button
                    }
                } else {
                    Modifier
                },
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = FrrColors.TealBlue,
            contentColor = FrrColors.CloudWhite,
            disabledContainerColor = FrrColors.WarmMist,
            disabledContentColor = FrrColors.CharcoalText.copy(alpha = 0.4f),
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
    ) {
        Text(text = text)
    }
}

@Composable
fun FrrSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 52.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = FrrColors.SlateNavy,
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
    ) {
        Text(text = text)
    }
}

@Composable
fun FrrTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
    ) {
        Text(text = text)
    }
}

@Composable
fun FrrWidePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FrrPrimaryButton(
        text = text,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
    )
}
