package com.familyriskreview.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.familyriskreview.core.designsystem.component.FrrSecondaryButton
import com.familyriskreview.core.designsystem.theme.FrrColors
import com.familyriskreview.core.designsystem.theme.FrrTypography
import com.familyriskreview.feature.settings.R

/**
 * Phase 0 settings shell.
 * Inflation assumptions, reduced motion and sync toggles expand in Phase 6/9.
 */
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = FrrTypography.displayMedium,
            color = FrrColors.MidnightBlue,
        )
        Text(
            text = stringResource(R.string.settings_body),
            style = FrrTypography.bodyLarge,
        )
        FrrSecondaryButton(
            text = stringResource(R.string.settings_back),
            onClick = onBack,
        )
    }
}
