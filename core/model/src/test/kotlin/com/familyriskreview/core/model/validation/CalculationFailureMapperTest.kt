package com.familyriskreview.core.model.validation

import com.familyriskreview.core.model.result.DomainError
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CalculationFailureMapperTest {
    @Test
    fun mapsAmountRange() {
        val mapped =
            CalculationFailureMapper.fromKnownThrowable(
                IllegalArgumentException("amount exceeds supported calculation range"),
            )
        assertThat(mapped).isEqualTo(
            DomainError.Calculation("CALC_AMOUNT_RANGE", "amount exceeds supported calculation range"),
        )
    }

    @Test
    fun mapsAggregateOverflow() {
        val mapped =
            CalculationFailureMapper.fromKnownThrowable(
                ArithmeticException("long overflow"),
            )
        assertThat(mapped?.code).isEqualTo("CALC_AGGREGATE_OVERFLOW")
    }

    @Test
    fun mapsMissingHorizon() {
        val mapped =
            CalculationFailureMapper.fromKnownThrowable(
                IllegalStateException("Calculable horizon is required for timing AS_LONG_AS_REQUIRED"),
            )
        assertThat(mapped?.code).isEqualTo("CALC_MISSING_HORIZON")
    }

    @Test
    fun mapsMissingAmount() {
        val mapped =
            CalculationFailureMapper.fromKnownThrowable(
                IllegalStateException("A calculable amount is required"),
            )
        assertThat(mapped?.code).isEqualTo("CALC_MISSING_AMOUNT")
    }

    @Test
    fun doesNotMapUnknownThrowable() {
        assertThat(
            CalculationFailureMapper.fromKnownThrowable(NullPointerException("boom")),
        ).isNull()
    }
}
