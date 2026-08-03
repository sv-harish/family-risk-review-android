package com.familyriskreview.feature.review

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
import com.familyriskreview.core.ui.layout.TwoPaneJourneyLayout
import com.familyriskreview.feature.review.R

/**
 * Phase 0 welcome / language / mode selection shell.
 * Full customer journey screens begin in Phase 3.
 */
@Composable
fun WelcomeRoute(
    onBeginQuickReview: () -> Unit,
    onBeginGuidedReview: () -> Unit,
    onBackToDashboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TwoPaneJourneyLayout(
        modifier = modifier,
        visualContext = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.welcome_brand),
                    style = FrrTypography.displayMedium,
                    color = FrrColors.MidnightBlue,
                )
                Text(
                    text = stringResource(R.string.welcome_supporting_line),
                    style = FrrTypography.bodyLarge,
                    color = FrrColors.SlateNavy,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.welcome_guided_by),
                    style = FrrTypography.labelLarge,
                    color = FrrColors.CharcoalText.copy(alpha = 0.7f),
                )
                Text(
                    text = AdvisorIdentity.DISPLAY_NAME,
                    style = FrrTypography.titleLarge,
                    color = FrrColors.MidnightBlue,
                )
                Text(
                    text = AdvisorIdentity.TITLE,
                    style = FrrTypography.bodyMedium,
                    color = FrrColors.CharcoalText,
                )
                Text(
                    text = AdvisorIdentity.CREDENTIAL,
                    style = FrrTypography.bodyMedium,
                    color = FrrColors.CharcoalText,
                )
                // TODO(production): Verify final formal credential wording before public release.
            }
        },
        interaction = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(R.string.welcome_intro),
                    style = FrrTypography.bodyLarge,
                )
                FrrPrimaryButton(
                    text = stringResource(R.string.welcome_begin_quick),
                    onClick = onBeginQuickReview,
                )
                FrrSecondaryButton(
                    text = stringResource(R.string.welcome_begin_guided),
                    onClick = onBeginGuidedReview,
                )
                FrrSecondaryButton(
                    text = stringResource(R.string.welcome_back_dashboard),
                    onClick = onBackToDashboard,
                )
                Text(
                    text = stringResource(R.string.welcome_phase0_note),
                    style = FrrTypography.bodyMedium,
                    color = FrrColors.CharcoalText.copy(alpha = 0.65f),
                )
            }
        },
    )
}

@Composable
fun ReviewPlaceholderRoute(
    reviewId: String,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.review_placeholder_title),
            style = FrrTypography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.review_placeholder_body, reviewId),
            style = FrrTypography.bodyLarge,
        )
        FrrPrimaryButton(
            text = stringResource(R.string.review_placeholder_continue),
            onClick = onContinue,
        )
    }
}
