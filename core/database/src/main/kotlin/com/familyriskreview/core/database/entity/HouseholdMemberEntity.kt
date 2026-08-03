package com.familyriskreview.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.familyriskreview.core.model.ContributionStatus
import com.familyriskreview.core.model.DependencyStatus
import com.familyriskreview.core.model.FamilyMemberType

@Entity(
    tableName = "household_members",
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
data class HouseholdMemberEntity(
    @PrimaryKey val id: String,
    val reviewId: String,
    val relationship: FamilyMemberType,
    val age: Int?,
    val contributionStatus: ContributionStatus,
    val dependencyStatus: DependencyStatus,
    val displayLabel: String?,
    val sortOrder: Int,
)
