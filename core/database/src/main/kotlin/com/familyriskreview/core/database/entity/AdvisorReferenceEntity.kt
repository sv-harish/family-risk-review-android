package com.familyriskreview.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Advisor-only reference data.
 * Must not automatically appear in customer summaries/PDFs.
 */
@Entity(
    tableName = "advisor_references",
    foreignKeys = [
        ForeignKey(
            entity = ReviewEntity::class,
            parentColumns = ["id"],
            childColumns = ["reviewId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class AdvisorReferenceEntity(
    @PrimaryKey val reviewId: String,
    val customerInitialsOrNickname: String?,
    val crmReference: String?,
    val privateNote: String?,
    val includeInCustomerSummary: Boolean,
)
