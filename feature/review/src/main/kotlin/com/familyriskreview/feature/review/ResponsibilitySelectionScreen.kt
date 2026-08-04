package com.familyriskreview.feature.review

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.familyriskreview.core.designsystem.component.FrrPrimaryButton
import com.familyriskreview.core.designsystem.component.FrrSecondaryButton
import com.familyriskreview.core.designsystem.component.FrrStableTextField
import com.familyriskreview.core.designsystem.theme.FrrIconSize
import com.familyriskreview.core.designsystem.theme.FrrRadius
import com.familyriskreview.core.designsystem.theme.FrrSpacing
import com.familyriskreview.core.designsystem.theme.FrrTypography
import com.familyriskreview.core.designsystem.theme.frrColors
import com.familyriskreview.core.model.ResponsibilityCatalogue
import com.familyriskreview.core.model.result.ValidationIssue

@Composable
fun ResponsibilitySelectionScreen(
    state: ReviewUiState,
    /** Third arg is optional responsibilityId (required for stable Other identity). */
    onToggle: (ResponsibilityCatalogue, Boolean, String?) -> Unit,
    onUpdateOtherLabelDraft: (TextDraft) -> Unit,
    onSaveActiveOtherLabel: () -> Unit,
    onAddAnotherCustomResponsibility: () -> Unit,
    onAdvance: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = frrColors()
    val selectedCatalogues =
        state.responsibilities.filter { it.isSelected }.map { it.catalogue }.toSet()
    val suggestionByCatalogue = state.suggestions.associateBy { it.catalogue }
    val otherLabelValue = state.drafts.otherLabel.toTextFieldValue()
    val activeOtherId = state.activeOtherResponsibilityId
    val otherSelected =
        state.responsibilities.any {
            it.catalogue == ResponsibilityCatalogue.OTHER &&
                it.isSelected &&
                (activeOtherId == null || it.id == activeOtherId)
        }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.responsibilities_title),
            style = FrrTypography.headlineMedium,
            color = colors.onSurface,
        )
        Text(
            text = stringResource(R.string.responsibilities_subtitle),
            style = FrrTypography.bodyLarge,
            color = colors.mutedText,
            modifier = Modifier.padding(top = FrrSpacing.xs, bottom = FrrSpacing.md),
        )
        state.validationIssues.forEach { issue ->
            Text(
                text = localizedValidationMessage(issue),
                style = FrrTypography.bodyMedium,
                color = validationColor(issue),
                modifier = Modifier.padding(bottom = FrrSpacing.xs),
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 200.dp),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = FrrSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(FrrSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(FrrSpacing.sm),
        ) {
            items(ResponsibilityCatalogue.entries.filter { it != ResponsibilityCatalogue.OTHER }) { catalogue ->
                val selected = catalogue in selectedCatalogues
                CatalogueTile(
                    catalogue = catalogue,
                    selected = selected,
                    suggestionReasonCode = suggestionByCatalogue[catalogue]?.reasonCode,
                    onClick = { onToggle(catalogue, !selected, null) },
                )
            }
            item {
                Column(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(FrrRadius.md))
                        .border(
                            if (otherSelected) 2.dp else 1.dp,
                            if (otherSelected) colors.primaryAction else colors.outline,
                            RoundedCornerShape(FrrRadius.md),
                        )
                        .background(colors.elevatedSurface)
                        .padding(FrrSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(FrrSpacing.xs),
                ) {
                    CatalogueTileHeader(ResponsibilityCatalogue.OTHER, otherSelected) {
                        onToggle(
                            ResponsibilityCatalogue.OTHER,
                            !otherSelected,
                            activeOtherId,
                        )
                    }
                    Text(
                        text = catalogueExplanation(ResponsibilityCatalogue.OTHER),
                        style = FrrTypography.bodyMedium,
                        color = colors.mutedText,
                    )
                    FrrStableTextField(
                        value = otherLabelValue,
                        onValueChange = { onUpdateOtherLabelDraft(TextDraft.from(it)) },
                        label = stringResource(R.string.responsibilities_other_label),
                        onFocusLost = {
                            if (otherSelected) {
                                onSaveActiveOtherLabel()
                            }
                        },
                    )
                    FrrSecondaryButton(
                        text = stringResource(R.string.responsibilities_add_another_custom),
                        onClick = onAddAnotherCustomResponsibility,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FrrSpacing.sm)) {
            FrrSecondaryButton(stringResource(R.string.review_back), onBack, Modifier.weight(1f))
            FrrPrimaryButton(stringResource(R.string.review_continue), onAdvance, Modifier.weight(1f))
        }
    }
}

@Composable
private fun CatalogueTile(
    catalogue: ResponsibilityCatalogue,
    selected: Boolean,
    suggestionReasonCode: String?,
    onClick: () -> Unit,
) {
    val colors = frrColors()
    val label = catalogueLabel(catalogue)
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FrrRadius.md))
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) colors.primaryAction else colors.outline,
                RoundedCornerShape(FrrRadius.md),
            )
            .background(colors.elevatedSurface)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                this.selected = selected
                contentDescription = label
            }
            .padding(FrrSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(FrrSpacing.xs),
    ) {
        CatalogueTileHeader(catalogue, selected, onClick)
        Text(text = catalogueExplanation(catalogue), style = FrrTypography.bodyMedium, color = colors.mutedText)
        if (suggestionReasonCode != null && !selected) {
            Text(
                text =
                stringResource(
                    R.string.responsibilities_suggested,
                    stringResource(DomainMessageMapper.suggestionReasonRes(suggestionReasonCode)),
                ),
                style = FrrTypography.labelLarge,
                color = colors.caution,
                modifier =
                Modifier
                    .clip(RoundedCornerShape(FrrRadius.xs))
                    .background(colors.caution.copy(alpha = 0.12f))
                    .padding(horizontal = FrrSpacing.xs, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun CatalogueTileHeader(
    catalogue: ResponsibilityCatalogue,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = frrColors()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FrrSpacing.xs),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = ResponsibilityIcons.forCatalogue(catalogue),
            contentDescription = null,
            tint = if (selected) colors.primaryAction else colors.secondaryAction,
            modifier = Modifier.size(FrrIconSize.md),
        )
        Text(
            text = catalogueLabel(catalogue),
            style = FrrTypography.titleMedium,
            color = colors.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(if (selected) R.string.responsibilities_selected else R.string.responsibilities_select),
            style = FrrTypography.labelLarge,
            color = if (selected) colors.primaryAction else colors.mutedText,
        )
    }
}

@Composable
private fun validationColor(issue: ValidationIssue) = if (issue.severity == ValidationIssue.Severity.WARNING) {
    frrColors().caution
} else {
    frrColors().blockingError
}
