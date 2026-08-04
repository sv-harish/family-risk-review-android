package com.familyriskreview.core.model

import com.familyriskreview.core.model.service.ReviewNumberProvider
import kotlinx.datetime.Instant
import java.util.Random

/**
 * Generates unique review numbers of the form `FRR-YYYYMMDD-XXXXXX`.
 *
 * Production code should obtain numbers through [ReviewNumberProvider] so tests
 * can inject deterministic clocks/random sources. Prefer not calling
 * [System.currentTimeMillis] or [java.security.SecureRandom] from repositories.
 */
object ReviewNumberGenerator {
    private val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun generate(
        epochMs: Long,
        random: Random,
    ): String {
        val day =
            java.time.Instant
                .ofEpochMilli(epochMs)
                .atZone(java.time.ZoneOffset.UTC)
                .toLocalDate()
        val ymd = "%04d%02d%02d".format(day.year, day.monthValue, day.dayOfMonth)
        val suffix =
            buildString {
                repeat(6) {
                    append(alphabet[random.nextInt(alphabet.length)])
                }
            }
        return "FRR-$ymd-$suffix"
    }
}

/** Production [ReviewNumberProvider] backed by SecureRandom. */
class SecureReviewNumberProvider(
    private val random: Random = java.security.SecureRandom(),
) : ReviewNumberProvider {
    override fun generate(now: Instant): String = ReviewNumberGenerator.generate(
        epochMs = now.toEpochMilliseconds(),
        random = random,
    )
}
