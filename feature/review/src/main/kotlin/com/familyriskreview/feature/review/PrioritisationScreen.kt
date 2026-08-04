package com.familyriskreview.feature.review

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.familyriskreview.core.designsystem.component.FrrPrimaryButton
import com.familyriskreview.core.designsystem.component.FrrSecondaryButton
import com.familyriskreview.core.designsystem.theme.FrrRadius
import com.familyriskreview.core.designsystem.theme.FrrSpacing
import com.familyriskreview.core.designsystem.theme.FrrTypography
import com.familyriskreview.core.designsystem.theme.frrColors
import com.familyriskreview.core.model.Responsibility
import com.familyriskreview.core.model.ResponsibilityPriority
import com.familyriskreview.core.model.ReviewMode

@Composable
fun PrioritisationScreen(
    state: ReviewUiState,
    onSetPriority: (String, ResponsibilityPriority) -> Unit,
    onAdvance: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = frrColors()
    val selected = state.responsibilities.filter { it.isSelected }
    val must = selected.filter { it.priority == ResponsibilityPriority.MUST_CONTINUE }
    val adjustable = selected.filter { it.priority == ResponsibilityPriority.IMPORTANT_BUT_ADJUSTABLE }
    val postponed = selected.filter { it.priority == ResponsibilityPriority.CAN_BE_POSTPONED_OR_REDUCED }
    val unassigned = selected.filter { it.priority == null }
    val quickLimitIssue =
        state.validationIssues.firstOrNull { it.code == "RESP_QUICK_MUST_CONTINUE_LIMIT" }
    val quickLimitMessage =
        when {
            quickLimitIssue != null -> localizedValidationMessage(quickLimitIssue)
            state.review?.mode == ReviewMode.QUICK && must.size >= 3 ->
                stringResource(R.string.priority_quick_max_hint)
            else -> null
        }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(text = stringResource(R.string.priority_title), style = FrrTypography.headlineMedium, color = colors.onSurface)
        Text(
            text = stringResource(R.string.priority_subtitle),
            style = FrrTypography.bodyLarge,
            color = colors.mutedText,
            modifier = Modifier.padding(top = FrrSpacing.xs, bottom = FrrSpacing.md),
        )
        quickLimitMessage?.let { message ->
            Text(
                text = message,
                style = FrrTypography.bodyMedium,
                color = colors.caution,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(FrrRadius.sm))
                    .background(colors.caution.copy(alpha = 0.12f)).padding(FrrSpacing.sm),
            )
            Spacer(Modifier.height(FrrSpacing.sm))
        }
        state.validationIssues.filter { it.code != "RESP_QUICK_MUST_CONTINUE_LIMIT" }.forEach { issue ->
            Text(
                text = localizedValidationMessage(issue),
                style = FrrTypography.bodyMedium,
                color = colors.blockingError,
                modifier = Modifier.padding(bottom = FrrSpacing.xs),
            )
        }
        if (unassigned.isNotEmpty()) {
            PriorityZone(stringResource(R.string.priority_unassigned), colors.outline, unassigned, onSetPriority)
            Spacer(Modifier.height(FrrSpacing.md))
        }
        PriorityZone(stringResource(R.string.priority_must), colors.mustContinue, must, onSetPriority)
        Spacer(Modifier.height(FrrSpacing.md))
        PriorityZone(stringResource(R.string.priority_adjustable), colors.adjustable, adjustable, onSetPriority)
        Spacer(Modifier.height(FrrSpacing.md))
        PriorityZone(stringResource(R.string.priority_postponed), colors.postponed, postponed, onSetPriority)
        Spacer(Modifier.height(FrrSpacing.lg))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FrrSpacing.sm)) {
            FrrSecondaryButton(stringResource(R.string.review_back), onBack, Modifier.weight(1f))
            FrrPrimaryButton(stringResource(R.string.review_continue), onAdvance, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PriorityZone(
    title: String,
    zoneColor: Color,
    items: List<Responsibility>,
    onAssign: (String, ResponsibilityPriority) -> Unit,
) {
    val colors = frrColors()
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(FrrRadius.md))
            .border(1.dp, zoneColor.copy(alpha = 0.5f), RoundedCornerShape(FrrRadius.md))
            .background(zoneColor.copy(alpha = 0.08f)).padding(FrrSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(FrrSpacing.xs),
    ) {
        Text(text = title, style = FrrTypography.titleMedium, color = zoneColor)
        if (items.isEmpty()) {
            Text(text = stringResource(R.string.priority_zone_empty), style = FrrTypography.bodyMedium, color = colors.mutedText)
        }
        items.forEach { item -> PriorityItemRow(item, onAssign) }
    }
}

@Composable
private fun PriorityItemRow(responsibility: Responsibility, onAssign: (String, ResponsibilityPriority) -> Unit) {
    val colors = frrColors()
    val label = responsibility.customLabel?.takeIf { it.isNotBlank() } ?: catalogueLabel(responsibility.catalogue)
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(FrrRadius.sm))
            .background(colors.elevatedSurface).padding(FrrSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(FrrSpacing.xs),
    ) {
        Text(text = label, style = FrrTypography.titleMedium, color = colors.onSurface)
        Row(horizontalArrangement = Arrangement.spacedBy(FrrSpacing.xs)) {
            PriorityChip(
                stringResource(R.string.priority_must_short),
                responsibility.priority == ResponsibilityPriority.MUST_CONTINUE,
                colors.mustContinue,
            ) {
                onAssign(responsibility.id, ResponsibilityPriority.MUST_CONTINUE)
            }
            PriorityChip(
                stringResource(R.string.priority_adjustable_short),
                responsibility.priority == ResponsibilityPriority.IMPORTANT_BUT_ADJUSTABLE,
                colors.adjustable,
            ) {
                onAssign(responsibility.id, ResponsibilityPriority.IMPORTANT_BUT_ADJUSTABLE)
            }
            PriorityChip(
                stringResource(R.string.priority_postponed_short),
                responsibility.priority == ResponsibilityPriority.CAN_BE_POSTPONED_OR_REDUCED,
                colors.postponed,
            ) {
                onAssign(responsibility.id, ResponsibilityPriority.CAN_BE_POSTPONED_OR_REDUCED)
            }
        }
    }
}

@Composable
private fun PriorityChip(text: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    val colors = frrColors()
    Text(
        text = text,
        style = FrrTypography.labelLarge,
        color = if (selected) colors.elevatedSurface else color,
        modifier = Modifier.clip(RoundedCornerShape(FrrRadius.sm))
            .background(if (selected) color else colors.elevatedSurface)
            .border(1.dp, color, RoundedCornerShape(FrrRadius.sm))
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = text
            }
            .padding(horizontal = FrrSpacing.sm, vertical = FrrSpacing.xs),
    )
}
