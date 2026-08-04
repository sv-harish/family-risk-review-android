package com.familyriskreview.feature.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.familyriskreview.core.designsystem.component.FrrPrimaryButton
import com.familyriskreview.core.designsystem.component.FrrSecondaryButton
import com.familyriskreview.core.designsystem.theme.FrrTypography
import com.familyriskreview.core.designsystem.theme.frrColors
import com.familyriskreview.core.model.AdvisorIdentity
import com.familyriskreview.core.ui.layout.TwoPaneJourneyLayout

/**
 * Welcome / language / mode selection. Review creation happens after Quick or Guided.
 */
@Composable
fun WelcomeRoute(
    onBeginQuickReview: () -> Unit,
    onBeginGuidedReview: () -> Unit,
    onBackToDashboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = frrColors()
    TwoPaneJourneyLayout(
        modifier = modifier,
        visualContext = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.welcome_brand),
                    style = FrrTypography.displayMedium,
                    color = colors.onSurface,
                )
                Text(
                    text = stringResource(R.string.welcome_supporting_line),
                    style = FrrTypography.bodyLarge,
                    color = colors.mutedText,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.welcome_guided_by),
                    style = FrrTypography.labelLarge,
                    color = colors.mutedText,
                )
                Text(
                    text = AdvisorIdentity.DISPLAY_NAME,
                    style = FrrTypography.titleLarge,
                    color = colors.onSurface,
                )
                Text(
                    text = AdvisorIdentity.TITLE,
                    style = FrrTypography.bodyMedium,
                    color = colors.onSurface,
                )
                Text(
                    text = AdvisorIdentity.CREDENTIAL,
                    style = FrrTypography.bodyMedium,
                    color = colors.mutedText,
                )
                // TODO(production): Verify final formal credential wording before public release.
            }
        },
        interaction = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(R.string.welcome_intro),
                    style = FrrTypography.bodyLarge,
                    color = colors.onSurface,
                )
                Text(
                    text = stringResource(R.string.welcome_mode_prompt),
                    style = FrrTypography.titleMedium,
                    color = colors.onSurface,
                )
                FrrPrimaryButton(
                    text = stringResource(R.string.welcome_begin_quick),
                    onClick = onBeginQuickReview,
                )
                Text(
                    text = stringResource(R.string.welcome_quick_hint),
                    style = FrrTypography.bodyMedium,
                    color = colors.mutedText,
                )
                FrrSecondaryButton(
                    text = stringResource(R.string.welcome_begin_guided),
                    onClick = onBeginGuidedReview,
                )
                Text(
                    text = stringResource(R.string.welcome_guided_hint),
                    style = FrrTypography.bodyMedium,
                    color = colors.mutedText,
                )
                FrrSecondaryButton(
                    text = stringResource(R.string.welcome_back_dashboard),
                    onClick = onBackToDashboard,
                )
            }
        },
    )
}
