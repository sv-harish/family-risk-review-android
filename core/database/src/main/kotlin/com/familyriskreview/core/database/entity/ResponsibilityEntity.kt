package com.familyriskreview.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.familyriskreview.core.model.ResponsibilityCatalogue
import com.familyriskreview.core.model.ResponsibilityPriority
import com.familyriskreview.core.model.TimingKind

@Entity(
    tableName = "responsibilities",
    foreignKeys = [
        ForeignKey(
            entity = ReviewEntity::class,
            parentColumns = ["id"],
            childColumns = ["reviewId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("reviewId")],
)
data class ResponsibilityEntity(
    @PrimaryKey val id: String,
    val reviewId: String,
    val catalogue: ResponsibilityCatalogue,
    val customLabel: String?,
    val priority: ResponsibilityPriority?,
    val timingKind: TimingKind?,
    val yearsUntilRequired: Int?,
    val durationYears: Int?,
    val milestoneLabel: String?,
    val customTimingNote: String?,
    val modellingDurationYears: Int?,
    val currentAmountRupees: Long?,
    val monthlyAmountRupees: Long?,
    val futureIndicativeAmountRupees: Long?,
    val explicitInflationBps: Int?,
    val derivedSourceCurrentAmountRupees: Long?,
    val derivedSourceMonthlyAmountRupees: Long?,
    val derivedSourceYears: Int?,
    val derivedSourceDurationYears: Int?,
    val derivedInflationRateBps: Int?,
    val derivedExpectedNetReturnBps: Int?,
    val derivedAssumptionVersion: String?,
    val derivedCalculationVersion: String?,
    val assumptionVersion: String?,
    val calculationVersion: String?,
    val isSelected: Boolean,
    val sortOrder: Int,
    val excludedFromNumericCalculation: Boolean,
)
