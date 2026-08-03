package com.familyriskreview.core.database

import androidx.room.TypeConverter
import com.familyriskreview.core.model.AppLanguage
import com.familyriskreview.core.model.ContributionStatus
import com.familyriskreview.core.model.DependencyStatus
import com.familyriskreview.core.model.FamilyMemberType
import com.familyriskreview.core.model.ResponsibilityCatalogue
import com.familyriskreview.core.model.ResponsibilityPriority
import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.ReviewStatus
import com.familyriskreview.core.model.ReviewStep
import com.familyriskreview.core.model.ScenarioKind
import com.familyriskreview.core.model.SyncState
import com.familyriskreview.core.model.TimingKind

class DatabaseConverters {
    @TypeConverter fun fromReviewMode(value: ReviewMode): String = value.name

    @TypeConverter fun toReviewMode(value: String): ReviewMode = ReviewMode.valueOf(value)

    @TypeConverter fun fromReviewStatus(value: ReviewStatus?): String? = value?.name

    @TypeConverter fun toReviewStatus(value: String?): ReviewStatus? = value?.let { ReviewStatus.valueOf(it) }

    @TypeConverter fun fromReviewStep(value: ReviewStep): String = value.name

    @TypeConverter fun toReviewStep(value: String): ReviewStep = ReviewStep.valueOf(value)

    @TypeConverter fun fromAppLanguage(value: AppLanguage): String = value.name

    @TypeConverter fun toAppLanguage(value: String): AppLanguage = AppLanguage.valueOf(value)

    @TypeConverter fun fromSyncState(value: SyncState): String = value.name

    @TypeConverter fun toSyncState(value: String): SyncState = SyncState.valueOf(value)

    @TypeConverter fun fromFamilyMemberType(value: FamilyMemberType): String = value.name

    @TypeConverter fun toFamilyMemberType(value: String): FamilyMemberType = FamilyMemberType.valueOf(value)

    @TypeConverter fun fromContributionStatus(value: ContributionStatus): String = value.name

    @TypeConverter fun toContributionStatus(value: String): ContributionStatus = ContributionStatus.valueOf(value)

    @TypeConverter fun fromDependencyStatus(value: DependencyStatus): String = value.name

    @TypeConverter fun toDependencyStatus(value: String): DependencyStatus = DependencyStatus.valueOf(value)

    @TypeConverter fun fromCatalogue(value: ResponsibilityCatalogue): String = value.name

    @TypeConverter fun toCatalogue(value: String): ResponsibilityCatalogue = ResponsibilityCatalogue.valueOf(value)

    @TypeConverter fun fromPriority(value: ResponsibilityPriority?): String? = value?.name

    @TypeConverter fun toPriority(value: String?): ResponsibilityPriority? = value?.let { ResponsibilityPriority.valueOf(it) }

    @TypeConverter fun fromTimingKind(value: TimingKind?): String? = value?.name

    @TypeConverter fun toTimingKind(value: String?): TimingKind? = value?.let { TimingKind.valueOf(it) }

    @TypeConverter fun fromScenarioKind(value: ScenarioKind): String = value.name

    @TypeConverter fun toScenarioKind(value: String): ScenarioKind = ScenarioKind.valueOf(value)
}
