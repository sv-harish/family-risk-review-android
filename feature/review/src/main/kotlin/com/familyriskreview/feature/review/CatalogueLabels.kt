package com.familyriskreview.feature.review

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.familyriskreview.core.model.ResponsibilityCatalogue

@Composable
internal fun catalogueLabel(catalogue: ResponsibilityCatalogue): String = when (catalogue) {
    ResponsibilityCatalogue.ESSENTIAL_FAMILY_LIVING_EXPENSES ->
        stringResource(R.string.catalogue_living)
    ResponsibilityCatalogue.CHILD_HIGHER_EDUCATION ->
        stringResource(R.string.catalogue_education)
    ResponsibilityCatalogue.CHILD_MARRIAGE_SUPPORT ->
        stringResource(R.string.catalogue_marriage)
    ResponsibilityCatalogue.SPOUSE_OR_PARTNER_SUPPORT ->
        stringResource(R.string.catalogue_spouse)
    ResponsibilityCatalogue.PARENT_SUPPORT ->
        stringResource(R.string.catalogue_parent)
    ResponsibilityCatalogue.HOME_LOAN_REPAYMENT ->
        stringResource(R.string.catalogue_home_loan)
    ResponsibilityCatalogue.OTHER_OUTSTANDING_LOANS ->
        stringResource(R.string.catalogue_other_loans)
    ResponsibilityCatalogue.BUYING_OR_COMPLETING_HOUSE ->
        stringResource(R.string.catalogue_house)
    ResponsibilityCatalogue.SPECIAL_NEEDS_DEPENDANT_SUPPORT ->
        stringResource(R.string.catalogue_special)
    ResponsibilityCatalogue.CHILDCARE_REPLACEMENT ->
        stringResource(R.string.catalogue_childcare)
    ResponsibilityCatalogue.HOUSEHOLD_CARE_REPLACEMENT ->
        stringResource(R.string.catalogue_household_care)
    ResponsibilityCatalogue.OTHER ->
        stringResource(R.string.catalogue_other)
}

@Composable
internal fun catalogueExplanation(catalogue: ResponsibilityCatalogue): String = when (catalogue) {
    ResponsibilityCatalogue.ESSENTIAL_FAMILY_LIVING_EXPENSES ->
        stringResource(R.string.catalogue_living_explanation)
    ResponsibilityCatalogue.CHILD_HIGHER_EDUCATION ->
        stringResource(R.string.catalogue_education_explanation)
    ResponsibilityCatalogue.CHILD_MARRIAGE_SUPPORT ->
        stringResource(R.string.catalogue_marriage_explanation)
    ResponsibilityCatalogue.SPOUSE_OR_PARTNER_SUPPORT ->
        stringResource(R.string.catalogue_spouse_explanation)
    ResponsibilityCatalogue.PARENT_SUPPORT ->
        stringResource(R.string.catalogue_parent_explanation)
    ResponsibilityCatalogue.HOME_LOAN_REPAYMENT ->
        stringResource(R.string.catalogue_home_loan_explanation)
    ResponsibilityCatalogue.OTHER_OUTSTANDING_LOANS ->
        stringResource(R.string.catalogue_other_loans_explanation)
    ResponsibilityCatalogue.BUYING_OR_COMPLETING_HOUSE ->
        stringResource(R.string.catalogue_house_explanation)
    ResponsibilityCatalogue.SPECIAL_NEEDS_DEPENDANT_SUPPORT ->
        stringResource(R.string.catalogue_special_explanation)
    ResponsibilityCatalogue.CHILDCARE_REPLACEMENT ->
        stringResource(R.string.catalogue_childcare_explanation)
    ResponsibilityCatalogue.HOUSEHOLD_CARE_REPLACEMENT ->
        stringResource(R.string.catalogue_household_care_explanation)
    ResponsibilityCatalogue.OTHER ->
        stringResource(R.string.catalogue_other_explanation)
}
