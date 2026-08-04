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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.familyriskreview.core.designsystem.component.FrrPrimaryButton
import com.familyriskreview.core.designsystem.component.FrrSecondaryButton
import com.familyriskreview.core.designsystem.component.FrrStableTextField
import com.familyriskreview.core.designsystem.theme.FrrSpacing
import com.familyriskreview.core.designsystem.theme.FrrTypography
import com.familyriskreview.core.designsystem.theme.frrColors
import com.familyriskreview.core.model.MoneyAmount
import com.familyriskreview.core.model.QuantificationStatus
import com.familyriskreview.core.model.Responsibility
import com.familyriskreview.core.model.ResponsibilityAmountModel
import com.familyriskreview.core.model.ResponsibilityCatalogue
import com.familyriskreview.core.model.ResponsibilityTiming
import com.familyriskreview.core.model.TimingKind

@Composable
fun ResponsibilityDetailsScreen(
    state: ReviewUiState,
    onSaveDraft: (Responsibility) -> Unit,
    onAdvance: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = frrColors()
    val items = state.responsibilities.filter { it.isSelected }
    var selectedId by rememberSaveable { mutableStateOf(items.firstOrNull()?.id) }
    val current = items.find { it.id == selectedId } ?: items.firstOrNull()

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(text = stringResource(R.string.details_title), style = FrrTypography.headlineMedium, color = colors.onSurface)
        Text(
            text = stringResource(R.string.details_subtitle),
            style = FrrTypography.bodyLarge,
            color = colors.mutedText,
            modifier = Modifier.padding(top = FrrSpacing.xs, bottom = FrrSpacing.md),
        )
        state.validationIssues.forEach { issue ->
            Text(text = issue.message, style = FrrTypography.bodyMedium, color = colors.blockingError,
                modifier = Modifier.padding(bottom = FrrSpacing.xs))
        }
        if (items.isEmpty()) {
            Text(text = stringResource(R.string.details_none_selected), style = FrrTypography.bodyLarge, color = colors.mutedText)
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FrrSpacing.xs)) {
                items.forEach { item ->
                    val label = item.customLabel?.takeIf { it.isNotBlank() } ?: catalogueLabel(item.catalogue)
                    FilterChip(selected = item.id == current?.id, onClick = { selectedId = item.id },
                        label = { Text(label.take(18)) })
                }
            }
            Spacer(Modifier.height(FrrSpacing.md))
            current?.let { ResponsibilityDetailsForm(it, onSaveDraft) }
        }
        Spacer(Modifier.height(FrrSpacing.lg))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FrrSpacing.sm)) {
            FrrSecondaryButton(stringResource(R.string.review_back), onBack, Modifier.weight(1f))
            FrrPrimaryButton(stringResource(R.string.review_continue), onAdvance, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ResponsibilityDetailsForm(responsibility: Responsibility, onSaveDraft: (Responsibility) -> Unit) {
    val colors = frrColors()
    val catalogue = responsibility.catalogue
    var labelField by remember(responsibility.id) { mutableStateOf(TextFieldValue(responsibility.customLabel.orEmpty())) }
    var monthlyField by remember(responsibility.id) {
        mutableStateOf(TextFieldValue(MoneyInputFormatter.formatOrEmpty(responsibility.monthlyAmount?.amountRupees)))
    }
    var currentField by remember(responsibility.id) {
        mutableStateOf(TextFieldValue(MoneyInputFormatter.formatOrEmpty(responsibility.currentAmount?.amountRupees)))
    }
    var yearsField by remember(responsibility.id) {
        mutableStateOf(TextFieldValue(
            (responsibility.timing?.yearsUntilRequired ?: responsibility.timing?.durationYears)?.toString().orEmpty(),
        ))
    }
    var inflationField by remember(responsibility.id) {
        mutableStateOf(TextFieldValue(responsibility.explicitInflationBps?.let { (it / 100.0).toString() }.orEmpty()))
    }
    var amountModel by remember(responsibility.id) {
        mutableStateOf(responsibility.amountModel ?: ResponsibilityAmountModel.ONE_TIME)
    }
    var notYetQuantified by remember(responsibility.id) {
        mutableStateOf(responsibility.quantificationStatus == QuantificationStatus.NOT_YET_QUANTIFIED)
    }

    fun moneyOrNull(field: TextFieldValue): MoneyAmount? =
        MoneyInputFormatter.parseRupees(field.text)?.let { MoneyAmount(it) }

    fun buildDraft(): Responsibility {
        val timing = when {
            isLivingLike(catalogue) ->
                ResponsibilityTiming(TimingKind.RECURRING_DURATION, durationYears = yearsField.text.trim().toIntOrNull())
            isEducationLike(catalogue) ->
                ResponsibilityTiming(TimingKind.ONE_TIME_IN_YEARS, yearsUntilRequired = yearsField.text.trim().toIntOrNull())
            isLoan(catalogue) -> ResponsibilityTiming(TimingKind.CURRENT_OUTSTANDING)
            catalogue == ResponsibilityCatalogue.OTHER ->
                when (amountModel) {
                    ResponsibilityAmountModel.ONE_TIME ->
                        ResponsibilityTiming(TimingKind.ONE_TIME_IN_YEARS, yearsUntilRequired = yearsField.text.trim().toIntOrNull())
                    ResponsibilityAmountModel.RECURRING ->
                        ResponsibilityTiming(TimingKind.RECURRING_DURATION, durationYears = yearsField.text.trim().toIntOrNull())
                }
            else -> responsibility.timing
        }
        return responsibility.copy(
            customLabel = if (catalogue == ResponsibilityCatalogue.OTHER) labelField.text.trim().ifBlank { null } else responsibility.customLabel,
            monthlyAmount = when {
                isLivingLike(catalogue) -> moneyOrNull(monthlyField)
                catalogue == ResponsibilityCatalogue.OTHER && amountModel == ResponsibilityAmountModel.RECURRING -> moneyOrNull(monthlyField)
                else -> null
            },
            currentAmount = when {
                isEducationLike(catalogue) || isLoan(catalogue) -> moneyOrNull(currentField)
                catalogue == ResponsibilityCatalogue.OTHER && amountModel == ResponsibilityAmountModel.ONE_TIME -> moneyOrNull(currentField)
                else -> null
            },
            timing = timing,
            amountModel = if (catalogue == ResponsibilityCatalogue.OTHER) amountModel else null,
            explicitInflationBps = if (catalogue == ResponsibilityCatalogue.OTHER) {
                inflationField.text.trim().toDoubleOrNull()?.let { (it * 100).toInt() }
            } else null,
            quantificationStatus = if (notYetQuantified && isLivingLike(catalogue)) {
                QuantificationStatus.NOT_YET_QUANTIFIED
            } else QuantificationStatus.QUANTIFIED,
        )
    }

    fun save() {
        monthlyField = TextFieldValue(MoneyInputFormatter.formatOrEmpty(MoneyInputFormatter.parseRupees(monthlyField.text)))
        currentField = TextFieldValue(MoneyInputFormatter.formatOrEmpty(MoneyInputFormatter.parseRupees(currentField.text)))
        onSaveDraft(buildDraft())
    }

    Column(verticalArrangement = Arrangement.spacedBy(FrrSpacing.sm)) {
        Text(text = catalogueLabel(catalogue), style = FrrTypography.titleLarge, color = colors.onSurface)
        when {
            isLivingLike(catalogue) -> {
                FrrStableTextField(monthlyField, { monthlyField = it }, label = stringResource(R.string.details_monthly_amount),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), onFocusLost = ::save)
                FrrStableTextField(yearsField, { yearsField = it }, label = stringResource(R.string.details_duration_years),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), onFocusLost = ::save)
                Row(horizontalArrangement = Arrangement.spacedBy(FrrSpacing.sm)) {
                    FilterChip(!notYetQuantified, { notYetQuantified = false; save() },
                        label = { Text(stringResource(R.string.details_quantified)) })
                    FilterChip(notYetQuantified, { notYetQuantified = true; save() },
                        label = { Text(stringResource(R.string.details_not_yet_quantified)) })
                }
            }
            isEducationLike(catalogue) -> {
                FrrStableTextField(currentField, { currentField = it }, label = stringResource(R.string.details_current_amount),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), onFocusLost = ::save)
                FrrStableTextField(yearsField, { yearsField = it }, label = stringResource(R.string.details_years_until),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), onFocusLost = ::save)
            }
            isLoan(catalogue) -> {
                FrrStableTextField(currentField, { currentField = it }, label = stringResource(R.string.details_outstanding_amount),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), onFocusLost = ::save)
            }
            catalogue == ResponsibilityCatalogue.OTHER -> {
                FrrStableTextField(labelField, { labelField = it }, label = stringResource(R.string.responsibilities_other_label), onFocusLost = ::save)
                Row(horizontalArrangement = Arrangement.spacedBy(FrrSpacing.sm)) {
                    FilterChip(amountModel == ResponsibilityAmountModel.ONE_TIME, {
                        amountModel = ResponsibilityAmountModel.ONE_TIME; save()
                    }, label = { Text(stringResource(R.string.details_model_one_time)) })
                    FilterChip(amountModel == ResponsibilityAmountModel.RECURRING, {
                        amountModel = ResponsibilityAmountModel.RECURRING; save()
                    }, label = { Text(stringResource(R.string.details_model_recurring)) })
                }
                if (amountModel == ResponsibilityAmountModel.ONE_TIME) {
                    FrrStableTextField(currentField, { currentField = it }, label = stringResource(R.string.details_current_amount),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), onFocusLost = ::save)
                    FrrStableTextField(yearsField, { yearsField = it }, label = stringResource(R.string.details_years_until),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), onFocusLost = ::save)
                } else {
                    FrrStableTextField(monthlyField, { monthlyField = it }, label = stringResource(R.string.details_monthly_amount),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), onFocusLost = ::save)
                    FrrStableTextField(yearsField, { yearsField = it }, label = stringResource(R.string.details_duration_years),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), onFocusLost = ::save)
                }
                FrrStableTextField(inflationField, { inflationField = it }, label = stringResource(R.string.details_inflation_percent),
                    supportingText = stringResource(R.string.details_inflation_hint),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), onFocusLost = ::save)
            }
        }
        FrrSecondaryButton(stringResource(R.string.details_save_draft), ::save, Modifier.fillMaxWidth())
    }
}

private fun isLivingLike(c: ResponsibilityCatalogue) =
    c == ResponsibilityCatalogue.ESSENTIAL_FAMILY_LIVING_EXPENSES ||
        c == ResponsibilityCatalogue.PARENT_SUPPORT ||
        c == ResponsibilityCatalogue.SPOUSE_OR_PARTNER_SUPPORT ||
        c == ResponsibilityCatalogue.SPECIAL_NEEDS_DEPENDANT_SUPPORT ||
        c == ResponsibilityCatalogue.CHILDCARE_REPLACEMENT ||
        c == ResponsibilityCatalogue.HOUSEHOLD_CARE_REPLACEMENT

private fun isEducationLike(c: ResponsibilityCatalogue) =
    c == ResponsibilityCatalogue.CHILD_HIGHER_EDUCATION ||
        c == ResponsibilityCatalogue.CHILD_MARRIAGE_SUPPORT ||
        c == ResponsibilityCatalogue.BUYING_OR_COMPLETING_HOUSE

private fun isLoan(c: ResponsibilityCatalogue) =
    c == ResponsibilityCatalogue.HOME_LOAN_REPAYMENT || c == ResponsibilityCatalogue.OTHER_OUTSTANDING_LOANS
