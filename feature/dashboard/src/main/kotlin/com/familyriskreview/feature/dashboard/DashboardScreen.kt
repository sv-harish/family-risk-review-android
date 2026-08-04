package com.familyriskreview.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.familyriskreview.core.designsystem.component.FrrPrimaryButton
import com.familyriskreview.core.designsystem.component.FrrSecondaryButton
import com.familyriskreview.core.designsystem.component.FrrTextAction
import com.familyriskreview.core.designsystem.theme.FrrSpacing
import com.familyriskreview.core.designsystem.theme.FrrTypography
import com.familyriskreview.core.designsystem.theme.frrColors
import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.ReviewStep
import com.familyriskreview.core.ui.shell.FrrAppShell
import com.familyriskreview.core.ui.shell.FrrShellDestination
import com.familyriskreview.feature.dashboard.R
import kotlinx.coroutines.flow.collectLatest

@Composable
fun DashboardRoute(
    onOpenReview: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.createdReviewId.collectLatest(onOpenReview)
    }

    FrrAppShell(
        selectedDestination = FrrShellDestination.Dashboard,
        onDestinationSelected = { destination ->
            when (destination) {
                FrrShellDestination.Dashboard -> Unit
                FrrShellDestination.Settings -> onOpenSettings()
            }
        },
        dashboardLabel = stringResource(R.string.dashboard_nav_dashboard),
        settingsLabel = stringResource(R.string.dashboard_settings),
        modifier = modifier,
    ) {
        DashboardScreen(
            uiState = uiState,
            onStartQuick = viewModel::startQuick,
            onStartGuided = viewModel::startGuided,
            onResume = onOpenReview,
            onOpenSettings = onOpenSettings,
            onClearError = viewModel::clearError,
        )
    }
}

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onStartQuick: () -> Unit,
    onStartGuided: () -> Unit,
    onResume: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = frrColors()
    val scroll = rememberScrollState()

    Column(
        modifier =
        modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(bottom = FrrSpacing.xl),
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            text = stringResource(R.string.dashboard_title),
            style = FrrTypography.displayMedium,
            color = colors.onSurface,
        )
        Spacer(Modifier.height(FrrSpacing.xs))
        Text(
            text = stringResource(R.string.dashboard_subtitle),
            style = FrrTypography.bodyLarge,
            color = colors.mutedText,
        )

        Spacer(Modifier.height(FrrSpacing.lg))

        Text(
            text = stringResource(R.string.dashboard_start_section),
            style = FrrTypography.titleLarge,
            color = colors.onSurface,
        )
        Spacer(Modifier.height(FrrSpacing.xs))
        Text(
            text = stringResource(R.string.dashboard_quick_difference),
            style = FrrTypography.bodyMedium,
            color = colors.mutedText,
        )
        Spacer(Modifier.height(FrrSpacing.sm))
        FrrPrimaryButton(
            text = stringResource(R.string.dashboard_start_quick),
            onClick = onStartQuick,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(FrrSpacing.sm))
        Text(
            text = stringResource(R.string.dashboard_guided_difference),
            style = FrrTypography.bodyMedium,
            color = colors.mutedText,
        )
        Spacer(Modifier.height(FrrSpacing.sm))
        FrrSecondaryButton(
            text = stringResource(R.string.dashboard_start_guided),
            onClick = onStartGuided,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(FrrSpacing.sm))
        FrrTextAction(
            text = stringResource(R.string.dashboard_settings),
            onClick = onOpenSettings,
        )

        uiState.createError?.let { message ->
            Spacer(Modifier.height(FrrSpacing.md))
            Column(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .background(colors.blockingError.copy(alpha = 0.08f))
                    .padding(FrrSpacing.md)
                    .semantics { contentDescription = message },
            ) {
                Text(
                    text = stringResource(R.string.dashboard_create_error_title),
                    style = FrrTypography.titleMedium,
                    color = colors.blockingError,
                )
                Spacer(Modifier.height(FrrSpacing.xxs))
                Text(
                    text = message,
                    style = FrrTypography.bodyMedium,
                    color = colors.onSurface,
                )
                Spacer(Modifier.height(FrrSpacing.xs))
                FrrTextAction(
                    text = stringResource(R.string.dashboard_dismiss_error),
                    onClick = onClearError,
                )
            }
        }

        Spacer(Modifier.height(FrrSpacing.lg))
        HorizontalDivider(color = colors.outline)
        Spacer(Modifier.height(FrrSpacing.lg))

        when {
            uiState.loading -> {
                BoxLoading()
            }
            uiState.inProgress.isEmpty() &&
                uiState.completed.isEmpty() &&
                uiState.archived.isEmpty() -> {
                EmptyState()
            }
            else -> {
                if (uiState.inProgress.isNotEmpty()) {
                    SectionHeader(stringResource(R.string.dashboard_section_in_progress))
                    Spacer(Modifier.height(FrrSpacing.sm))
                    uiState.inProgress.forEach { item ->
                        ReviewRow(
                            item = item,
                            actionLabel = stringResource(R.string.dashboard_resume),
                            onAction = { onResume(item.id) },
                        )
                        Spacer(Modifier.height(FrrSpacing.sm))
                    }
                }

                if (uiState.completed.isNotEmpty()) {
                    Spacer(Modifier.height(FrrSpacing.md))
                    SectionHeader(stringResource(R.string.dashboard_section_completed))
                    Spacer(Modifier.height(FrrSpacing.sm))
                    uiState.completed.forEach { item ->
                        ReviewRow(
                            item = item,
                            actionLabel = stringResource(R.string.dashboard_open),
                            onAction = { onResume(item.id) },
                        )
                        Spacer(Modifier.height(FrrSpacing.sm))
                    }
                }

                if (uiState.archived.isNotEmpty()) {
                    Spacer(Modifier.height(FrrSpacing.md))
                    SectionHeader(stringResource(R.string.dashboard_section_archived))
                    Spacer(Modifier.height(FrrSpacing.xxs))
                    Text(
                        text = stringResource(R.string.dashboard_archived_hint),
                        style = FrrTypography.bodyMedium,
                        color = colors.mutedText,
                    )
                    Spacer(Modifier.height(FrrSpacing.sm))
                    uiState.archived.forEach { item ->
                        ReviewRow(
                            item = item,
                            actionLabel = null,
                            onAction = null,
                        )
                        Spacer(Modifier.height(FrrSpacing.sm))
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxLoading() {
    val colors = frrColors()
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = FrrSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = colors.primaryAction)
        Spacer(Modifier.height(FrrSpacing.sm))
        Text(
            text = stringResource(R.string.dashboard_loading),
            style = FrrTypography.bodyMedium,
            color = colors.mutedText,
        )
    }
}

@Composable
private fun EmptyState() {
    val colors = frrColors()
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(vertical = FrrSpacing.md),
        verticalArrangement = Arrangement.spacedBy(FrrSpacing.xs),
    ) {
        Text(
            text = stringResource(R.string.dashboard_empty_title),
            style = FrrTypography.titleLarge,
            color = colors.onSurface,
        )
        Text(
            text = stringResource(R.string.dashboard_empty_body),
            style = FrrTypography.bodyLarge,
            color = colors.mutedText,
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    val colors = frrColors()
    Text(
        text = title,
        style = FrrTypography.titleLarge,
        color = colors.onSurface,
    )
}

@Composable
private fun ReviewRow(
    item: DashboardReviewItem,
    actionLabel: String?,
    onAction: (() -> Unit)?,
) {
    val colors = frrColors()
    val modeLabel =
        when (item.mode) {
            ReviewMode.QUICK -> stringResource(R.string.dashboard_mode_quick)
            ReviewMode.GUIDED -> stringResource(R.string.dashboard_mode_guided)
        }
    val stageLabel = stringResource(stageLabelRes(item.stage))
    val rowDescription =
        stringResource(
            R.string.dashboard_review_a11y,
            item.reviewNumber,
            modeLabel,
            stageLabel,
            item.updatedAtLabel,
        )

    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .background(colors.elevatedSurface)
            .padding(FrrSpacing.md)
            .semantics { contentDescription = rowDescription },
        verticalArrangement = Arrangement.spacedBy(FrrSpacing.xxs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.reviewNumber,
                style = FrrTypography.titleMedium,
                color = colors.onSurface,
            )
            Text(
                text = modeLabel,
                style = FrrTypography.labelLarge,
                color = colors.primaryAction,
            )
        }
        Text(
            text = stringResource(R.string.dashboard_stage_label, stageLabel),
            style = FrrTypography.bodyMedium,
            color = colors.onSurface,
        )
        Text(
            text = stringResource(R.string.dashboard_updated_label, item.updatedAtLabel),
            style = FrrTypography.bodyMedium,
            color = colors.mutedText,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(FrrSpacing.xxs))
            FrrSecondaryButton(
                text = actionLabel,
                onClick = onAction,
            )
        }
    }
}

private fun stageLabelRes(step: ReviewStep): Int = when (step) {
    ReviewStep.HOUSEHOLD_SUPPORT_MAP -> R.string.dashboard_stage_household
    ReviewStep.RESPONSIBILITIES -> R.string.dashboard_stage_responsibilities
    ReviewStep.PRIORITISATION -> R.string.dashboard_stage_prioritisation
    ReviewStep.RESPONSIBILITY_DETAILS -> R.string.dashboard_stage_details
    ReviewStep.TIMELINE -> R.string.dashboard_stage_timeline
    ReviewStep.INCOME_RISK_EDUCATION -> R.string.dashboard_stage_income_risk
    ReviewStep.KEY_REALIZATION -> R.string.dashboard_stage_key_realization
    ReviewStep.GROSS_RESPONSIBILITY -> R.string.dashboard_stage_gross
    ReviewStep.EDUCATIONAL_COMPARISON -> R.string.dashboard_stage_comparison
    ReviewStep.AWARENESS_SUMMARY -> R.string.dashboard_stage_summary
    ReviewStep.ADVISOR_HANDOFF -> R.string.dashboard_stage_handoff
}
