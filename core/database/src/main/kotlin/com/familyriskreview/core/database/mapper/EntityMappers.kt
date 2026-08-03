package com.familyriskreview.core.database.mapper

import com.familyriskreview.core.database.entity.AdvisorReferenceEntity
import com.familyriskreview.core.database.entity.HouseholdMemberEntity
import com.familyriskreview.core.database.entity.ResponsibilityEntity
import com.familyriskreview.core.database.entity.ReviewEntity
import com.familyriskreview.core.model.AdvisorReference
import com.familyriskreview.core.model.DerivedValueMetadata
import com.familyriskreview.core.model.HouseholdMember
import com.familyriskreview.core.model.MoneyAmount
import com.familyriskreview.core.model.Responsibility
import com.familyriskreview.core.model.ResponsibilityTiming
import com.familyriskreview.core.model.Review
import kotlinx.datetime.Instant

fun ReviewEntity.toDomain(): Review = Review(
    id = id,
    reviewNumber = reviewNumber,
    mode = mode,
    language = language,
    status = status,
    currentStep = currentStep,
    createdAt = Instant.fromEpochMilliseconds(createdAtEpochMs),
    updatedAt = Instant.fromEpochMilliseconds(updatedAtEpochMs),
    completedAt = completedAtEpochMs?.let(Instant::fromEpochMilliseconds),
    focusedIncomeContributorId = focusedIncomeContributorId,
    assumptionVersion = assumptionVersion,
    calculationVersion = calculationVersion,
    syncState = syncState,
    revision = revision,
    customerAcknowledged = customerAcknowledged,
)

fun Review.toEntity(serverUpdatedAtEpochMs: Long? = null): ReviewEntity = ReviewEntity(
    id = id,
    reviewNumber = reviewNumber,
    mode = mode,
    language = language,
    status = status,
    currentStep = currentStep,
    createdAtEpochMs = createdAt.toEpochMilliseconds(),
    updatedAtEpochMs = updatedAt.toEpochMilliseconds(),
    completedAtEpochMs = completedAt?.toEpochMilliseconds(),
    focusedIncomeContributorId = focusedIncomeContributorId,
    assumptionVersion = assumptionVersion,
    calculationVersion = calculationVersion,
    syncState = syncState,
    revision = revision,
    customerAcknowledged = customerAcknowledged,
    serverUpdatedAtEpochMs = serverUpdatedAtEpochMs,
)

fun HouseholdMemberEntity.toDomain(): HouseholdMember = HouseholdMember(
    id = id,
    reviewId = reviewId,
    relationship = relationship,
    age = age,
    contributionStatus = contributionStatus,
    dependencyStatus = dependencyStatus,
    displayLabel = displayLabel,
    sortOrder = sortOrder,
)

fun HouseholdMember.toEntity(): HouseholdMemberEntity = HouseholdMemberEntity(
    id = id,
    reviewId = reviewId,
    relationship = relationship,
    age = age,
    contributionStatus = contributionStatus,
    dependencyStatus = dependencyStatus,
    displayLabel = displayLabel,
    sortOrder = sortOrder,
)

fun ResponsibilityEntity.toDomain(): Responsibility = Responsibility(
    id = id,
    reviewId = reviewId,
    catalogue = catalogue,
    customLabel = customLabel,
    priority = priority,
    timing =
    timingKind?.let {
        ResponsibilityTiming(
            kind = it,
            yearsUntilRequired = yearsUntilRequired,
            durationYears = durationYears,
            milestoneLabel = milestoneLabel,
            customNote = customTimingNote,
        )
    },
    currentAmount = currentAmountRupees?.let(::MoneyAmount),
    monthlyAmount = monthlyAmountRupees?.let(::MoneyAmount),
    futureIndicativeAmount = futureIndicativeAmountRupees?.let(::MoneyAmount),
    derivedMetadata = toDerivedMetadata(),
    explicitInflationBps = explicitInflationBps,
    assumptionVersion = assumptionVersion,
    calculationVersion = calculationVersion,
    isSelected = isSelected,
    sortOrder = sortOrder,
)

private fun ResponsibilityEntity.toDerivedMetadata(): DerivedValueMetadata? {
    val inflation = derivedInflationRateBps ?: return null
    val assumption = derivedAssumptionVersion ?: return null
    val calc = derivedCalculationVersion ?: return null
    return DerivedValueMetadata(
        sourceCurrentAmountRupees = derivedSourceCurrentAmountRupees,
        sourceMonthlyAmountRupees = derivedSourceMonthlyAmountRupees,
        sourceYears = derivedSourceYears,
        sourceDurationYears = derivedSourceDurationYears,
        inflationRateBps = inflation,
        expectedNetReturnBps = derivedExpectedNetReturnBps,
        assumptionVersion = assumption,
        calculationVersion = calc,
    )
}

fun Responsibility.toEntity(): ResponsibilityEntity = ResponsibilityEntity(
    id = id,
    reviewId = reviewId,
    catalogue = catalogue,
    customLabel = customLabel,
    priority = priority,
    timingKind = timing?.kind,
    yearsUntilRequired = timing?.yearsUntilRequired,
    durationYears = timing?.durationYears,
    milestoneLabel = timing?.milestoneLabel,
    customTimingNote = timing?.customNote,
    currentAmountRupees = currentAmount?.amountRupees,
    monthlyAmountRupees = monthlyAmount?.amountRupees,
    futureIndicativeAmountRupees = futureIndicativeAmount?.amountRupees,
    explicitInflationBps = explicitInflationBps,
    derivedSourceCurrentAmountRupees = derivedMetadata?.sourceCurrentAmountRupees,
    derivedSourceMonthlyAmountRupees = derivedMetadata?.sourceMonthlyAmountRupees,
    derivedSourceYears = derivedMetadata?.sourceYears,
    derivedSourceDurationYears = derivedMetadata?.sourceDurationYears,
    derivedInflationRateBps = derivedMetadata?.inflationRateBps,
    derivedExpectedNetReturnBps = derivedMetadata?.expectedNetReturnBps,
    derivedAssumptionVersion = derivedMetadata?.assumptionVersion,
    derivedCalculationVersion = derivedMetadata?.calculationVersion,
    assumptionVersion = assumptionVersion,
    calculationVersion = calculationVersion,
    isSelected = isSelected,
    sortOrder = sortOrder,
)

fun AdvisorReferenceEntity.toDomain(): AdvisorReference = AdvisorReference(
    reviewId = reviewId,
    customerInitialsOrNickname = customerInitialsOrNickname,
    crmReference = crmReference,
    privateNote = privateNote,
    includeInCustomerSummary = includeInCustomerSummary,
)

fun AdvisorReference.toEntity(): AdvisorReferenceEntity = AdvisorReferenceEntity(
    reviewId = reviewId,
    customerInitialsOrNickname = customerInitialsOrNickname,
    crmReference = crmReference,
    privateNote = privateNote,
    includeInCustomerSummary = includeInCustomerSummary,
)
