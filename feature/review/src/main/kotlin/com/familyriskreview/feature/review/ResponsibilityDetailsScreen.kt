package com.familyriskreview.feature.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.familyriskreview.core.designsystem.component.FrrPrimaryButton
import com.familyriskreview.core.designsystem.component.FrrSecondaryButton
import com.familyriskreview.core.designsystem.component.FrrStableTextField
import com.familyriskreview.core.designsystem.theme.FrrSpacing
import com.familyriskreview.core.designsystem.theme.FrrTypography
import com.familyriskreview.core.designsystem.theme.frrColors
import com.familyriskreview.core.model.Responsibility
import com.familyriskreview.core.model.ResponsibilityAmountModel
import com.familyriskreview.core.model.ResponsibilityCatalogue
import com.familyriskreview.core.model.ResponsibilityPriority
import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.result.ValidationIssue

@Composable
fun ResponsibilityDetailsScreen(
    state: ReviewUiState,
    onUpdateDraft: (ResponsibilityDetailsDraft) -> Unit,
    onSaveDraft: (Responsibility) -> Unit,
    onSelectResponsibility: (String) -> Unit,
    mayRemainNonQuantified: (ResponsibilityPriority?) -> Boolean,
    onAdvance: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = frrColors()
    val items = state.responsibilities.filter { it.isSelected }
    val selectedFromState = state.selectedResponsibilityId
    var fallbackSelectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedId =
        selectedFromState
            ?: fallbackSelectedId
            ?: items.firstOrNull()?.id
    val current = items.find { it.id == selectedId } ?: items.firstOrNull()
    val mode = state.review?.mode ?: ReviewMode.QUICK

    fun draftFor(responsibility: Responsibility): ResponsibilityDetailsDraft = state.drafts.detailsDrafts[responsibility.id]
        ?: ReviewViewModel.detailsDraftFrom(responsibility)

    fun flushCurrent() {
        val responsibility = current ?: return
        val draft = draftFor(responsibility)
        onSaveDraft(
            ReviewViewModel.applyDraftToResponsibility(responsibility, draft, mode),
        )
    }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(text = stringResource(R.string.details_title), style = FrrTypography.headlineMedium, color = colors.onSurface)
        Text(
            text = stringResource(R.string.details_subtitle),
            style = FrrTypography.bodyLarge,
            color = colors.mutedText,
            modifier = Modifier.padding(top = FrrSpacing.xs, bottom = FrrSpacing.md),
        )
        state.validationIssues.forEach { issue ->
            Text(
                text = localizedValidationMessage(issue),
                style = FrrTypography.bodyMedium,
                color = if (issue.severity == ValidationIssue.Severity.WARNING) colors.caution else colors.blockingError,
                modifier = Modifier.padding(bottom = FrrSpacing.xs),
            )
        }
        if (items.isEmpty()) {
            Text(
                text = stringResource(R.string.details_none_selected),
                style = FrrTypography.bodyLarge,
                color = colors.mutedText,
            )
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FrrSpacing.xs)) {
                items.forEach { item ->
                    val label = item.customLabel?.takeIf { it.isNotBlank() } ?: catalogueLabel(item.catalogue)
                    FilterChip(
                        selected = item.id == current?.id,
                        onClick = {
                            fallbackSelectedId = item.id
                            onSelectResponsibility(item.id)
                        },
                        label = { Text(label.take(18)) },
                    )
                }
            }
            Spacer(Modifier.height(FrrSpacing.md))
            current?.let { responsibility ->
                ResponsibilityDetailsForm(
                    responsibility = responsibility,
                    draft = draftFor(responsibility),
                    allowNonQuantified = mayRemainNonQuantified(responsibility.priority),
                    mode = mode,
                    onUpdateDraft = onUpdateDraft,
                    onSaveDraft = onSaveDraft,
                )
            }
        }
        Spacer(Modifier.height(FrrSpacing.lg))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FrrSpacing.sm)) {
            FrrSecondaryButton(stringResource(R.string.review_back), onBack, Modifier.weight(1f))
            FrrPrimaryButton(
                text = stringResource(R.string.review_continue),
                onClick = {
                    flushCurrent()
                    onAdvance()
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ResponsibilityDetailsForm(
    responsibility: Responsibility,
    draft: ResponsibilityDetailsDraft,
    allowNonQuantified: Boolean,
    mode: ReviewMode,
    onUpdateDraft: (ResponsibilityDetailsDraft) -> Unit,
    onSaveDraft: (Responsibility) -> Unit,
) {
    val colors = frrColors()
    val catalogue = responsibility.catalogue
    val labelField = draft.label.toTextFieldValue()
    val monthlyField = draft.monthlyAmount.toTextFieldValue()
    val currentField = draft.currentAmount.toTextFieldValue()
    val yearsField = draft.years.toTextFieldValue()
    val inflationField = draft.inflationPercent.toTextFieldValue()
    val amountModel =
        draft.amountModelName?.let { runCatching { ResponsibilityAmountModel.valueOf(it) }.getOrNull() }
            ?: responsibility.amountModel
            ?: ResponsibilityAmountModel.ONE_TIME
    val notYetQuantified = draft.notYetQuantified

    fun update(transform: (ResponsibilityDetailsDraft) -> ResponsibilityDetailsDraft) {
        onUpdateDraft(transform(draft))
    }

    fun saveFrom(next: ResponsibilityDetailsDraft = draft) {
        val formatted =
            next.copy(
                monthlyAmount =
                TextDraft.of(
                    MoneyInputFormatter.formatOrEmpty(
                        MoneyInputFormatter.parseRupees(next.monthlyAmount.text),
                    ),
                ),
                currentAmount =
                TextDraft.of(
                    MoneyInputFormatter.formatOrEmpty(
                        MoneyInputFormatter.parseRupees(next.currentAmount.text),
                    ),
                ),
            )
        onUpdateDraft(formatted.copy(dirty = true))
        onSaveDraft(
            ReviewViewModel.applyDraftToResponsibility(responsibility, formatted, mode),
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(FrrSpacing.sm)) {
        Text(text = catalogueLabel(catalogue), style = FrrTypography.titleLarge, color = colors.onSurface)
        when {
            ReviewViewModel.isLivingLike(catalogue) -> {
                FrrStableTextField(
                    value = monthlyField,
                    onValueChange = { value -> update { d -> d.copy(monthlyAmount = TextDraft.from(value)) } },
                    label = stringResource(R.string.details_monthly_amount),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    onFocusLost = { saveFrom() },
                )
                FrrStableTextField(
                    value = yearsField,
                    onValueChange = { value -> update { d -> d.copy(years = TextDraft.from(value)) } },
                    label = stringResource(R.string.details_duration_years),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    onFocusLost = { saveFrom() },
                )
            }
            ReviewViewModel.isEducationLike(catalogue) -> {
                FrrStableTextField(
                    value = currentField,
                    onValueChange = { value -> update { d -> d.copy(currentAmount = TextDraft.from(value)) } },
                    label = stringResource(R.string.details_current_amount),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    onFocusLost = { saveFrom() },
                )
                FrrStableTextField(
                    value = yearsField,
                    onValueChange = { value -> update { d -> d.copy(years = TextDraft.from(value)) } },
                    label = stringResource(R.string.details_years_until),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    onFocusLost = { saveFrom() },
                )
            }
            ReviewViewModel.isLoan(catalogue) -> {
                FrrStableTextField(
                    value = currentField,
                    onValueChange = { value -> update { d -> d.copy(currentAmount = TextDraft.from(value)) } },
                    label = stringResource(R.string.details_outstanding_amount),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    onFocusLost = { saveFrom() },
                )
            }
            catalogue == ResponsibilityCatalogue.OTHER -> {
                FrrStableTextField(
                    value = labelField,
                    onValueChange = { value -> update { d -> d.copy(label = TextDraft.from(value)) } },
                    label = stringResource(R.string.responsibilities_other_label),
                    onFocusLost = { saveFrom() },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(FrrSpacing.sm)) {
                    FilterChip(
                        selected = amountModel == ResponsibilityAmountModel.ONE_TIME,
                        onClick = {
                            val next = draft.copy(amountModelName = ResponsibilityAmountModel.ONE_TIME.name)
                            update { next }
                            saveFrom(next)
                        },
                        label = { Text(stringResource(R.string.details_model_one_time)) },
                    )
                    FilterChip(
                        selected = amountModel == ResponsibilityAmountModel.RECURRING,
                        onClick = {
                            val next = draft.copy(amountModelName = ResponsibilityAmountModel.RECURRING.name)
                            update { next }
                            saveFrom(next)
                        },
                        label = { Text(stringResource(R.string.details_model_recurring)) },
                    )
                }
                if (amountModel == ResponsibilityAmountModel.ONE_TIME) {
                    FrrStableTextField(
                        value = currentField,
                        onValueChange = { value -> update { d -> d.copy(currentAmount = TextDraft.from(value)) } },
                        label = stringResource(R.string.details_current_amount),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        onFocusLost = { saveFrom() },
                    )
                    FrrStableTextField(
                        value = yearsField,
                        onValueChange = { value -> update { d -> d.copy(years = TextDraft.from(value)) } },
                        label = stringResource(R.string.details_years_until),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        onFocusLost = { saveFrom() },
                    )
                } else {
                    FrrStableTextField(
                        value = monthlyField,
                        onValueChange = { value -> update { d -> d.copy(monthlyAmount = TextDraft.from(value)) } },
                        label = stringResource(R.string.details_monthly_amount),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        onFocusLost = { saveFrom() },
                    )
                    FrrStableTextField(
                        value = yearsField,
                        onValueChange = { value -> update { d -> d.copy(years = TextDraft.from(value)) } },
                        label = stringResource(R.string.details_duration_years),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        onFocusLost = { saveFrom() },
                    )
                }
                FrrStableTextField(
                    value = inflationField,
                    onValueChange = { value -> update { d -> d.copy(inflationPercent = TextDraft.from(value)) } },
                    label = stringResource(R.string.details_inflation_percent),
                    supportingText = stringResource(R.string.details_inflation_hint),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    onFocusLost = { saveFrom() },
                )
            }
        }

        if (allowNonQuantified) {
            Text(
                text =
                if (notYetQuantified) {
                    stringResource(R.string.details_quantification_status_not_yet)
                } else {
                    stringResource(R.string.details_quantified)
                },
                style = FrrTypography.bodyMedium,
                color = if (notYetQuantified) colors.caution else colors.primaryAction,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(FrrSpacing.sm)) {
                FilterChip(
                    selected = !notYetQuantified,
                    onClick = {
                        val next = draft.copy(notYetQuantified = false)
                        update { next }
                        saveFrom(next)
                    },
                    label = { Text(stringResource(R.string.details_quantified)) },
                )
                FilterChip(
                    selected = notYetQuantified,
                    onClick = {
                        val next = draft.copy(notYetQuantified = true)
                        update { next }
                        saveFrom(next)
                    },
                    label = { Text(stringResource(R.string.details_not_yet_quantified)) },
                )
            }
        }

        FrrSecondaryButton(
            text = stringResource(R.string.details_save_draft),
            onClick = { saveFrom() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
