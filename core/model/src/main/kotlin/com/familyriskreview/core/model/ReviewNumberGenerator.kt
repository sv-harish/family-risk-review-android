package com.familyriskreview.core.model

/**
 * Generates unique review numbers of the form `FRR-YYYYMMDD-XXXXXX`.
 * Uniqueness within a day is provided by a cryptographically strong random suffix.
 */
object ReviewNumberGenerator {
    private val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun generate(
        epochMs: Long = System.currentTimeMillis(),
        random: java.util.Random = java.security.SecureRandom(),
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
