package com.familyriskreview.feature.review

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.familyriskreview.core.designsystem.component.FrrSecondaryButton
import com.familyriskreview.core.designsystem.component.FrrTextAction
import com.familyriskreview.core.designsystem.theme.FrrSpacing
import com.familyriskreview.core.designsystem.theme.FrrTypography
import com.familyriskreview.core.designsystem.theme.frrColors
import com.familyriskreview.core.model.ReviewStep

@Composable
fun ReviewRoute(
    @Suppress("UNUSED_PARAMETER") reviewId: String,
    onContinue: () -> Unit,
    onBackToDashboard: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReviewViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = frrColors()

    Column(
        modifier = modifier.fillMaxSize().background(colors.surface).padding(FrrSpacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FrrTextAction(
                text = stringResource(R.string.review_back_dashboard),
                onClick = onBackToDashboard,
            )
            state.review?.let { review ->
                Text(
                    text = review.reviewNumber,
                    style = FrrTypography.labelLarge,
                    color = colors.mutedText,
                )
            }
        }
        Spacer(Modifier.height(FrrSpacing.sm))
        state.review?.currentStep?.toJourneyStage()?.let { stage ->
            JourneyStageIndicator(current = stage, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(FrrSpacing.md))
        state.errorMessage?.let { message ->
            Text(
                text = message,
                style = FrrTypography.bodyMedium,
                color = colors.blockingError,
                modifier = Modifier.padding(bottom = FrrSpacing.sm),
            )
            FrrTextAction(
                text = stringResource(R.string.review_dismiss_error),
                onClick = viewModel::clearError,
            )
        }
        when {
            state.loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.primaryAction)
                }
            }
            state.review == null -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.review_not_found),
                        style = FrrTypography.titleLarge,
                        color = colors.onSurface,
                    )
                    Spacer(Modifier.height(FrrSpacing.md))
                    FrrSecondaryButton(
                        text = stringResource(R.string.review_back_dashboard),
                        onClick = onBackToDashboard,
                    )
                }
            }
            else -> {
                when (state.review!!.currentStep) {
                    ReviewStep.HOUSEHOLD_SUPPORT_MAP ->
                        HouseholdSupportMapScreen(
                            state = state,
                            onAddMember = viewModel::addMember,
                            onUpdateMember = { viewModel.updateMember(it) },
                            onRemoveMember = viewModel::removeMember,
                            onSetFocus = viewModel::setFocus,
                            onSelectMember = viewModel::selectMemberForEdit,
                            onAdvance = viewModel::advance,
                            modifier = Modifier.fillMaxSize(),
                        )
                    ReviewStep.RESPONSIBILITIES ->
                        ResponsibilitySelectionScreen(
                            state = state,
                            onToggle = viewModel::toggleResponsibility,
                            onAdvance = viewModel::advance,
                            onBack = viewModel::goBack,
                            modifier = Modifier.fillMaxSize(),
                        )
                    ReviewStep.PRIORITISATION ->
                        PrioritisationScreen(
                            state = state,
                            onSetPriority = viewModel::setPriority,
                            onAdvance = viewModel::advance,
                            onBack = viewModel::goBack,
                            modifier = Modifier.fillMaxSize(),
                        )
                    ReviewStep.RESPONSIBILITY_DETAILS ->
                        ResponsibilityDetailsScreen(
                            state = state,
                            onSaveDraft = viewModel::saveDetailsDraft,
                            onAdvance = viewModel::advance,
                            onBack = viewModel::goBack,
                            modifier = Modifier.fillMaxSize(),
                        )
                    else ->
                        Phase2BoundaryScreen(
                            onBackToDashboard = onBackToDashboard,
                            onContinue = onContinue,
                            onBack = viewModel::goBack,
                            modifier = Modifier.fillMaxSize(),
                        )
                }
            }
        }
    }
}

@Composable
private fun JourneyStageIndicator(current: ReviewJourneyStage, modifier: Modifier = Modifier) {
    val colors = frrColors()
    val description =
        stringResource(R.string.stage_indicator_a11y, stringResource(current.labelResId()))
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()).semantics {
            contentDescription = description
        },
        horizontalArrangement = Arrangement.spacedBy(FrrSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OrderedJourneyStages.forEachIndexed { index, stage ->
            val active = stage == current
            val past = stage.ordinal < current.ordinal
            Text(
                text = stringResource(stage.labelResId()),
                style = if (active) FrrTypography.titleMedium else FrrTypography.labelLarge,
                color = when {
                    active -> colors.primaryAction
                    past -> colors.onSurface
                    else -> colors.mutedText
                },
            )
            if (index < OrderedJourneyStages.lastIndex) {
                Text(text = " · ", color = colors.mutedText)
            }
        }
    }
}

@Composable
fun Phase2BoundaryScreen(
    onBackToDashboard: () -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = frrColors()
    Column(
        modifier = modifier.fillMaxSize().padding(FrrSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(FrrSpacing.md),
    ) {
        Text(
            text = stringResource(R.string.phase2_boundary_title),
            style = FrrTypography.headlineMedium,
            color = colors.onSurface,
        )
        Text(
            text = stringResource(R.string.phase2_boundary_body),
            style = FrrTypography.bodyLarge,
            color = colors.mutedText,
        )
        Spacer(Modifier.height(FrrSpacing.md))
        FrrSecondaryButton(
            text = stringResource(R.string.phase2_boundary_back_step),
            onClick = onBack,
        )
        FrrSecondaryButton(
            text = stringResource(R.string.phase2_boundary_save_return),
            onClick = onBackToDashboard,
        )
        FrrTextAction(
            text = stringResource(R.string.review_continue),
            onClick = onContinue,
        )
    }
}
