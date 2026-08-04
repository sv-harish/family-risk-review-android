package com.familyriskreview.feature.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.familyriskreview.core.designsystem.component.FrrPrimaryButton
import com.familyriskreview.core.designsystem.component.FrrSecondaryButton
import com.familyriskreview.core.designsystem.theme.FrrTypography
import com.familyriskreview.core.designsystem.theme.frrColors
import com.familyriskreview.feature.review.R

/**
 * Temporary review host until the Phase 2 journey screens land.
 * Keeps the `ReviewRoute` import path stable for app navigation.
 */
@Composable
fun ReviewRoute(
    reviewId: String,
    onContinue: () -> Unit,
    onBackToDashboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = frrColors()
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.review_placeholder_title),
            style = FrrTypography.headlineMedium,
            color = colors.onSurface,
        )
        Text(
            text = stringResource(R.string.review_placeholder_body, reviewId),
            style = FrrTypography.bodyLarge,
            color = colors.mutedText,
        )
        FrrPrimaryButton(
            text = stringResource(R.string.review_placeholder_continue),
            onClick = onContinue,
        )
        FrrSecondaryButton(
            text = stringResource(R.string.welcome_back_dashboard),
            onClick = onBackToDashboard,
        )
    }
}

@Deprecated(
    message = "Use ReviewRoute",
    replaceWith = ReplaceWith("ReviewRoute(reviewId, onContinue, onBackToDashboard = {})"),
)
@Composable
fun ReviewPlaceholderRoute(
    reviewId: String,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ReviewRoute(
        reviewId = reviewId,
        onContinue = onContinue,
        onBackToDashboard = {},
        modifier = modifier,
    )
}
