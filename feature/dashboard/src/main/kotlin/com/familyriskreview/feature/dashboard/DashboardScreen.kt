package com.familyriskreview.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.familyriskreview.core.designsystem.component.FrrPrimaryButton
import com.familyriskreview.core.designsystem.component.FrrSecondaryButton
import com.familyriskreview.core.designsystem.theme.FrrColors
import com.familyriskreview.core.designsystem.theme.FrrTypography
import com.familyriskreview.feature.dashboard.R

/**
 * Phase 0 advisor dashboard shell.
 * Full list/resume/archive behaviour arrives in Phase 6.
 */
@Composable
fun DashboardRoute(
    onStartNewReview: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            text = stringResource(R.string.dashboard_title),
            style = FrrTypography.displayMedium,
            color = FrrColors.MidnightBlue,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.dashboard_subtitle),
            style = FrrTypography.bodyLarge,
            color = FrrColors.SlateNavy,
        )
        Spacer(Modifier.height(32.dp))
        FrrPrimaryButton(
            text = stringResource(R.string.dashboard_start_new_review),
            onClick = onStartNewReview,
        )
        Spacer(Modifier.height(12.dp))
        FrrSecondaryButton(
            text = stringResource(R.string.dashboard_settings),
            onClick = onOpenSettings,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.dashboard_phase0_note),
            style = FrrTypography.bodyMedium,
            color = FrrColors.CharcoalText.copy(alpha = 0.7f),
        )
    }
}
