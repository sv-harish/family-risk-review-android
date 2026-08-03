package com.familyriskreview.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Random

class ReviewNumberGeneratorTest {
    @Test
    fun generatesExpectedFormat() {
        val number =
            ReviewNumberGenerator.generate(
                epochMs = 1_767_225_600_000L, // 2026-01-01 UTC-ish fixed
                random = Random(1L),
            )
        assertThat(number).matches("""FRR-\d{8}-[A-Z0-9]{6}""")
    }

    @Test
    fun uniquenessStrategy_differentSeedsDiffer() {
        val a = ReviewNumberGenerator.generate(epochMs = 1_000L, random = Random(1))
        val b = ReviewNumberGenerator.generate(epochMs = 1_000L, random = Random(2))
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun sameDayPrefixShared() {
        val a = ReviewNumberGenerator.generate(epochMs = 1_767_225_600_000L, random = Random(1))
        val b = ReviewNumberGenerator.generate(epochMs = 1_767_225_600_000L + 3_600_000, random = Random(2))
        assertThat(a.substring(0, 12)).isEqualTo(b.substring(0, 12))
    }
}

class AdvisorIdentityTest {
    @Test
    fun suppliedCredentialWordingIsPreserved() {
        assertThat(AdvisorIdentity.DISPLAY_NAME).isEqualTo("S V Harish")
        assertThat(AdvisorIdentity.TITLE).isEqualTo("Certified Insurance Planner")
        assertThat(AdvisorIdentity.CREDENTIAL).isEqualTo("LUGI CIP Completed")
    }
}

class AnnualRateBpsTest {
    @Test
    fun fromPercent_roundsToBps() {
        assertThat(AnnualRateBps.fromPercent(8.0).value).isEqualTo(800)
        assertThat(AnnualRateBps.fromPercent(6.0).value).isEqualTo(600)
    }
}
