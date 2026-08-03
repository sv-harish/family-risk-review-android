package com.familyriskreview.core.calculation

/**
 * Indian (lakh/crore grouping) currency formatting helpers.
 *
 * Editable text fields must NOT apply this formatting on every keystroke
 * unless cursor mapping is fully implemented. Prefer format-on-blur.
 */
object IndianCurrencyFormatter {
    fun formatRupees(amountRupees: Long): String {
        val negative = amountRupees < 0
        val absolute = kotlin.math.abs(amountRupees)
        val grouped = groupIndian(absolute.toString())
        val prefix = if (negative) "-₹" else "₹"
        return prefix + grouped
    }

    /**
     * Groups digits using the Indian system: last 3, then pairs.
     * Examples: 500000 -> 5,00,000 ; 12000000 -> 1,20,00,000
     */
    fun groupIndian(digits: String): String {
        if (digits.length <= 3) return digits
        val lastThree = digits.takeLast(3)
        val remaining = digits.dropLast(3)
        val pairs =
            remaining
                .reversed()
                .chunked(2)
                .joinToString(",") { it.reversed() }
                .reversed()
        // chunked approach above can leave leading order odd; rebuild carefully:
        val rebuiltPairs =
            buildString {
                var index = remaining.length
                val parts = mutableListOf<String>()
                while (index > 0) {
                    val start = (index - 2).coerceAtLeast(0)
                    parts.add(0, remaining.substring(start, index))
                    index = start
                }
                append(parts.joinToString(","))
            }
        return "$rebuiltPairs,$lastThree"
    }

    /**
     * Parses a user-entered amount, stripping currency symbols and separators.
     * Returns null when the input is empty or not a valid integer rupee amount.
     */
    fun parseRupees(raw: String): Long? {
        val cleaned =
            raw
                .trim()
                .removePrefix("₹")
                .removePrefix("Rs.")
                .removePrefix("Rs")
                .replace(",", "")
                .replace(" ", "")
        if (cleaned.isEmpty()) return null
        return cleaned.toLongOrNull()
    }
}
