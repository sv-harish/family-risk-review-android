package com.familyriskreview.feature.review

import java.text.NumberFormat
import java.util.Locale

/**
 * Keeps raw text-field input separate from [com.familyriskreview.core.model.MoneyAmount].
 * Format only on blur / intentional sync — never while the caret is moving.
 */
object MoneyInputFormatter {
    private val locale: Locale = Locale.Builder().setLanguage("en").setRegion("IN").build()

    fun parseRupees(raw: String): Long? {
        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) return null
        return digits.toLongOrNull()
    }

    fun formatRupees(amountRupees: Long): String =
        NumberFormat.getNumberInstance(locale).format(amountRupees)

    fun formatOrEmpty(amountRupees: Long?): String =
        amountRupees?.let { formatRupees(it) }.orEmpty()
}
