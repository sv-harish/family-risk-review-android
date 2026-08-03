package com.familyriskreview.feature.summary

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
import com.familyriskreview.core.model.AdvisorIdentity
import com.familyriskreview.feature.summary.R

/**
 * Phase 0 summary / advisor handoff shell.
 * Multilingual PDF generation arrives in Phase 7.
 */
@Composable
fun SummaryRoute(
    reviewId: String,
    onFinishSession: () -> Unit,
    onBackToDashboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.summary_title),
            style = FrrTypography.displayMedium,
            color = FrrColors.MidnightBlue,
        )
        Text(
            text = stringResource(R.string.summary_core_insight),
            style = FrrTypography.bodyLarge,
            color = FrrColors.SlateNavy,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.summary_review_id, reviewId),
            style = FrrTypography.bodyMedium,
        )
        Text(
            text = stringResource(
                R.string.summary_advisor_handoff,
                AdvisorIdentity.DISPLAY_NAME,
            ),
            style = FrrTypography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        FrrPrimaryButton(
            text = stringResource(R.string.summary_finish_session),
            onClick = onFinishSession,
        )
        FrrSecondaryButton(
            text = stringResource(R.string.summary_back_dashboard),
            onClick = onBackToDashboard,
        )
        Text(
            text = stringResource(R.string.summary_phase0_note),
            style = FrrTypography.bodyMedium,
            color = FrrColors.CharcoalText.copy(alpha = 0.65f),
        )
    }
}
