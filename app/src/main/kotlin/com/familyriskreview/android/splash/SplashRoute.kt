package com.familyriskreview.android.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.familyriskreview.android.R
import com.familyriskreview.core.designsystem.theme.FrrMotion
import com.familyriskreview.core.designsystem.theme.frrColors
import com.familyriskreview.core.designsystem.theme.FrrTypography
import kotlinx.coroutines.delay

/**
 * Splash is the only screen that may show Dareus One branding.
 * When [reducedMotion] is true, entrance animation and long dwell are skipped.
 */
@Composable
fun SplashRoute(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
) {
    val colors = frrColors()
    val alpha = remember { Animatable(if (reducedMotion) 1f else 0f) }

    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            delay(FrrMotion.shortMs.toLong())
            onFinished()
        } else {
            alpha.animateTo(1f, animationSpec = tween(durationMillis = 500))
            delay(1200)
            onFinished()
        }
    }

    val brandDescription = stringResource(R.string.splash_a11y)
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.surface)
                .semantics { contentDescription = brandDescription },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .alpha(alpha.value)
                    .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = FrrTypography.displayMedium,
                color = colors.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.splash_tagline),
                style = FrrTypography.bodyLarge,
                color = colors.secondaryAction,
            )
        }

        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp)
                    .alpha(alpha.value * 0.85f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Placeholder for Dareus One logo — see assets/branding/README.md
            Text(
                text = stringResource(R.string.splash_built_by),
                style = FrrTypography.labelMedium,
                color = colors.mutedText.copy(alpha = 0.85f),
            )
            Text(
                text = stringResource(R.string.splash_dareus_placeholder),
                style = FrrTypography.labelLarge,
                color = colors.secondaryAction.copy(alpha = 0.75f),
            )
        }
    }
}
