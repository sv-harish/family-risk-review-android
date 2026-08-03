package com.familyriskreview.core.model

import kotlinx.serialization.Serializable

/**
 * Annual inflation / return rate stored as integer basis points.
 * 800 bps = 8.00% per year.
 */
@Serializable
@JvmInline
value class AnnualRateBps(
    val value: Int,
) {
    init {
        require(value in MIN_BPS..MAX_BPS) {
            "AnnualRateBps must be in $MIN_BPS..$MAX_BPS (was $value)"
        }
    }

    fun toDecimalString(): String = "%d.%02d".format(value / 100, kotlin.math.abs(value % 100))

    companion object {
        const val MIN_BPS: Int = 0
        const val MAX_BPS: Int = 50_00 // 50%

        val ZERO: AnnualRateBps = AnnualRateBps(0)
        val EDUCATION_DEFAULT: AnnualRateBps = AnnualRateBps(800) // 8%
        val MARRIAGE_DEFAULT: AnnualRateBps = AnnualRateBps(600) // 6%
        val EXPENSE_DEFAULT: AnnualRateBps = AnnualRateBps(600) // 6%
        val RECURRING_SUPPORT_DEFAULT: AnnualRateBps = AnnualRateBps(600) // 6%

        fun fromPercent(percent: Double): AnnualRateBps {
            require(percent.isFinite()) { "percent must be finite" }
            val bps = kotlin.math.round(percent * 100.0).toInt()
            return AnnualRateBps(bps)
        }
    }
}
