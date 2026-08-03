package com.familyriskreview.core.model

import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    val defaultLanguage: AppLanguage = AppLanguage.ENGLISH,
    val reducedMotion: Boolean = false,
    val educationInflationBps: Int = AnnualRateBps.EDUCATION_DEFAULT.value,
    val marriageInflationBps: Int = AnnualRateBps.MARRIAGE_DEFAULT.value,
    val expenseInflationBps: Int = AnnualRateBps.EXPENSE_DEFAULT.value,
    val syncEnabled: Boolean = false,
) {
    fun toAssumptions(): CalculationAssumptions = CalculationAssumptions(
        educationInflation = AnnualRateBps(educationInflationBps),
        marriageInflation = AnnualRateBps(marriageInflationBps),
        expenseInflation = AnnualRateBps(expenseInflationBps),
        recurringSupportInflation = AnnualRateBps(expenseInflationBps),
    )
}
