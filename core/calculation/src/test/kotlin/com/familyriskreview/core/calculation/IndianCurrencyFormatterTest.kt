package com.familyriskreview.core.calculation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IndianCurrencyFormatterTest {

    @Test
    fun formatsLakhs() {
        assertThat(IndianCurrencyFormatter.formatRupees(500_000)).isEqualTo("₹5,00,000")
        assertThat(IndianCurrencyFormatter.formatRupees(2_500_000)).isEqualTo("₹25,00,000")
        assertThat(IndianCurrencyFormatter.formatRupees(12_000_000)).isEqualTo("₹1,20,00,000")
    }

    @Test
    fun parsesFormattedInput() {
        assertThat(IndianCurrencyFormatter.parseRupees("₹5,00,000")).isEqualTo(500_000)
        assertThat(IndianCurrencyFormatter.parseRupees("25,00,000")).isEqualTo(2_500_000)
        assertThat(IndianCurrencyFormatter.parseRupees("")).isNull()
        assertThat(IndianCurrencyFormatter.parseRupees("abc")).isNull()
    }

    @Test
    fun smallAmountsUnchangedGrouping() {
        assertThat(IndianCurrencyFormatter.formatRupees(999)).isEqualTo("₹999")
        assertThat(IndianCurrencyFormatter.formatRupees(1_000)).isEqualTo("₹1,000")
        assertThat(IndianCurrencyFormatter.formatRupees(10_000)).isEqualTo("₹10,000")
    }
}
