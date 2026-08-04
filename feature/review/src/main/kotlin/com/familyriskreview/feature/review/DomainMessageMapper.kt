package com.familyriskreview.feature.review

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.familyriskreview.core.model.result.DomainError
import com.familyriskreview.core.model.result.ValidationIssue
import com.familyriskreview.core.model.validation.HouseholdRules

/**
 * Maps stable domain codes to localised string resources.
 *
 * Domain/use-case layers keep English technical messages; UI resolves display copy here.
 */
object DomainMessageMapper {
    @StringRes
    fun validationIssueRes(issue: ValidationIssue): Int = validationCodeRes(issue.code)

    @StringRes
    fun validationCodeRes(code: String): Int = when (code) {
        "HOUSEHOLD_AGE_RANGE" -> R.string.household_age_range_error
        "HOUSEHOLD_DUPLICATE_SELF" -> R.string.validation_household_duplicate_self
        "HOUSEHOLD_PRIMARY_FULLY_DEPENDENT" -> R.string.validation_household_primary_fully_dependent
        "HOUSEHOLD_FOCUS_NON_CONTRIBUTOR" -> R.string.validation_household_focus_non_contributor
        "HOUSEHOLD_EMPTY" -> R.string.validation_household_empty
        "HOUSEHOLD_FOCUS_MISSING" -> R.string.validation_household_focus_missing

        "RESP_CUSTOM_LABEL_REQUIRED" -> R.string.validation_resp_custom_label_required
        "RESP_NONE_SELECTED" -> R.string.validation_resp_none_selected
        "RESP_PRIORITY_REQUIRED" -> R.string.validation_resp_priority_required
        "RESP_QUICK_MUST_CONTINUE_LIMIT" -> R.string.validation_resp_quick_must_continue_limit
        "RESP_GUIDED_REQUIRES_QUANTIFICATION" -> R.string.validation_resp_guided_requires_quantification
        "RESP_MUST_CONTINUE_REQUIRES_QUANTIFICATION" ->
            R.string.validation_resp_must_continue_requires_quantification
        "RESP_LIGHTWEIGHT_MUST_BE_NON_QUANTIFIED" ->
            R.string.validation_resp_lightweight_must_be_non_quantified
        "RESP_EXPLICIT_INFLATION_REQUIRED" -> R.string.validation_resp_explicit_inflation_required
        "RESP_TIMING_REQUIRED" -> R.string.validation_resp_timing_required
        "RESP_TIMING_NOT_ALLOWED" -> R.string.validation_resp_timing_not_allowed
        "RESP_YEARS_REQUIRED" -> R.string.validation_resp_years_required
        "RESP_DURATION_REQUIRED" -> R.string.validation_resp_duration_required
        "RESP_MODELLING_HORIZON_REQUIRED" -> R.string.validation_resp_modelling_horizon_required
        "RESP_MILESTONE_REQUIRED" -> R.string.validation_resp_milestone_required
        "RESP_MILESTONE_HORIZON_REQUIRED" -> R.string.validation_resp_milestone_horizon_required
        "RESP_CUSTOM_TIMING_REQUIRED" -> R.string.validation_resp_custom_timing_required
        "RESP_CUSTOM_HORIZON_REQUIRED" -> R.string.validation_resp_custom_horizon_required
        "RESP_MONTHLY_REQUIRED" -> R.string.validation_resp_monthly_required
        "RESP_CURRENT_NOT_PRIMARY" -> R.string.validation_resp_current_not_primary
        "RESP_CURRENT_REQUIRED" -> R.string.validation_resp_current_required
        "RESP_MONTHLY_NOT_ALLOWED" -> R.string.validation_resp_monthly_not_allowed
        "RESP_LOAN_MONTHLY_NOT_ALLOWED" -> R.string.validation_resp_loan_monthly_not_allowed
        "RESP_AMBIGUOUS_AMOUNT_MODEL" -> R.string.validation_resp_ambiguous_amount_model
        "RESP_AMOUNT_MODEL_REQUIRED" -> R.string.validation_resp_amount_model_required
        "RESP_AMOUNT_LIMIT" -> R.string.validation_resp_amount_limit
        "RESP_MONTHLY_LIMIT" -> R.string.validation_resp_monthly_limit
        "RESP_YEARS_LIMIT" -> R.string.validation_resp_years_limit
        "RESP_DURATION_LIMIT" -> R.string.validation_resp_duration_limit
        "RESP_HORIZON_LIMIT" -> R.string.validation_resp_horizon_limit

        else -> R.string.validation_fallback
    }

    @StringRes
    fun suggestionReasonRes(reasonCode: String): Int = when (reasonCode) {
        "ALWAYS_LIVING" -> R.string.suggestion_always_living
        "HAS_CHILD" -> R.string.suggestion_has_child
        "HAS_SPOUSE" -> R.string.suggestion_has_spouse
        "HAS_PARENT" -> R.string.suggestion_has_parent
        "HAS_OTHER_DEPENDANT" -> R.string.suggestion_has_other_dependant
        "OPTIONAL_LOAN" -> R.string.suggestion_optional_loan
        "OPTIONAL_HOUSE" -> R.string.suggestion_optional_house
        else -> R.string.validation_fallback
    }

    @StringRes
    fun domainErrorRes(error: DomainError): Int = when (error) {
        is DomainError.Validation -> R.string.domain_error_validation
        is DomainError.Transition -> R.string.domain_error_transition
        is DomainError.Conflict -> R.string.domain_error_conflict
        is DomainError.NotFound -> R.string.domain_error_not_found
        is DomainError.CollisionExhausted -> R.string.domain_error_collision_exhausted
        is DomainError.IllegalState -> R.string.domain_error_illegal_state
        is DomainError.CorruptData -> R.string.domain_error_corrupt_data
        is DomainError.Persistence -> R.string.domain_error_persistence
        is DomainError.Calculation -> R.string.domain_error_calculation
    }

    /** Localise a stable [ReviewUiState.errorCode] / save-failure code. */
    @StringRes
    fun errorCodeRes(code: String): Int = when (code) {
        "CONFLICT" -> R.string.domain_error_conflict
        "NOT_FOUND" -> R.string.review_error_not_found
        "TRANSITION" -> R.string.domain_error_transition
        "COLLISION_EXHAUSTED" -> R.string.domain_error_collision_exhausted
        "ILLEGAL_STATE" -> R.string.domain_error_illegal_state
        "PERSISTENCE" -> R.string.domain_error_persistence
        "VALIDATION" -> R.string.domain_error_validation
        "CORRUPT_DATA" -> R.string.domain_error_corrupt_data
        "CALCULATION" -> R.string.domain_error_calculation
        else -> {
            val validation = validationCodeRes(code)
            if (validation != R.string.validation_fallback) {
                validation
            } else {
                R.string.domain_error_fallback
            }
        }
    }

    @StringRes
    fun conflictRes(): Int = R.string.conflict_body

    @StringRes
    fun notFoundRes(): Int = R.string.review_error_not_found

    @StringRes
    fun validationFallbackRes(): Int = R.string.validation_fallback

    @StringRes
    fun domainErrorFallbackRes(): Int = R.string.domain_error_fallback
}

/** Resolve a [ValidationIssue] to localised display copy (never [ValidationIssue.message]). */
@Composable
fun localizedValidationMessage(issue: ValidationIssue): String = when (issue.code) {
    "HOUSEHOLD_AGE_RANGE" ->
        stringResource(
            R.string.household_age_range_error,
            HouseholdRules.MIN_AGE,
            HouseholdRules.MAX_AGE,
        )
    else -> stringResource(DomainMessageMapper.validationIssueRes(issue))
}
