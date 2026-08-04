package com.familyriskreview.core.model.validation

import com.familyriskreview.core.model.result.DomainError

/**
 * Maps known calculator validation/arithmetic failures to stable [DomainError.Calculation]
 * codes. Programming defects outside these patterns should not be caught indiscriminately.
 */
object CalculationFailureMapper {
    fun fromKnownThrowable(throwable: Throwable): DomainError.Calculation? {
        val message = throwable.message.orEmpty()
        return when (throwable) {
            is ArithmeticException ->
                DomainError.Calculation(
                    code = "CALC_AGGREGATE_OVERFLOW",
                    message = message.ifBlank { "Aggregate total overflowed supported range" },
                )
            is IllegalArgumentException -> mapIllegalArgument(message)
            is IllegalStateException -> mapIllegalState(message)
            else -> null
        }
    }

    private fun mapIllegalArgument(message: String): DomainError.Calculation {
        val code =
            when {
                message.contains("exceeds supported", ignoreCase = true) ||
                    message.contains("supported calculation range", ignoreCase = true) ->
                    "CALC_AMOUNT_RANGE"
                message.contains("overflows Long", ignoreCase = true) ||
                    message.contains("overflow", ignoreCase = true) ->
                    "CALC_RESULT_OVERFLOW"
                message.contains("years must", ignoreCase = true) ||
                    message.contains("durationYears must", ignoreCase = true) ->
                    "CALC_UNSUPPORTED_DURATION"
                message.contains("Non-quantified", ignoreCase = true) ||
                    message.contains("non-quantified", ignoreCase = true) ->
                    "CALC_NON_QUANTIFIED"
                else -> "CALC_ILLEGAL_ARGUMENT"
            }
        return DomainError.Calculation(
            code = code,
            message = message.ifBlank { "Calculation rejected illegal argument" },
        )
    }

    private fun mapIllegalState(message: String): DomainError.Calculation {
        val code =
            when {
                message.contains("Calculable duration", ignoreCase = true) ||
                    message.contains("Calculable horizon", ignoreCase = true) ||
                    message.contains("modelling", ignoreCase = true) ->
                    "CALC_MISSING_HORIZON"
                message.contains("calculable amount", ignoreCase = true) ||
                    message.contains("A calculable amount", ignoreCase = true) ->
                    "CALC_MISSING_AMOUNT"
                message.contains("Timing is required", ignoreCase = true) ->
                    "CALC_MISSING_TIMING"
                message.contains("explicitInflationBps", ignoreCase = true) ||
                    message.contains("explicit inflation", ignoreCase = true) ->
                    "CALC_INVALID_ASSUMPTION"
                else -> "CALC_ILLEGAL_STATE"
            }
        return DomainError.Calculation(
            code = code,
            message = message.ifBlank { "Calculation rejected illegal state" },
        )
    }
}
