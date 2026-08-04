package com.familyriskreview.core.data.usecase

import com.familyriskreview.core.calculation.ResponsibilityCalculator
import com.familyriskreview.core.data.repository.AdvisorReferenceRepository
import com.familyriskreview.core.data.repository.CalculationSnapshotRepository
import com.familyriskreview.core.data.repository.HouseholdRepository
import com.familyriskreview.core.data.repository.ResponsibilityRepository
import com.familyriskreview.core.data.repository.ReviewMutationWriter
import com.familyriskreview.core.data.repository.ReviewReader
import com.familyriskreview.core.model.AdvisorReference
import com.familyriskreview.core.model.AppLanguage
import com.familyriskreview.core.model.CalculationAssumptions
import com.familyriskreview.core.model.CalculationScenario
import com.familyriskreview.core.model.CalculationSnapshot
import com.familyriskreview.core.model.FocusUpdate
import com.familyriskreview.core.model.GrossResponsibilityResult
import com.familyriskreview.core.model.HouseholdMember
import com.familyriskreview.core.model.Responsibility
import com.familyriskreview.core.model.ResponsibilityIndicativeLine
import com.familyriskreview.core.model.ResponsibilityPriority
import com.familyriskreview.core.model.Review
import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.ReviewStatus
import com.familyriskreview.core.model.ScenarioKind
import com.familyriskreview.core.model.lifecycle.ReviewProgression
import com.familyriskreview.core.model.result.DomainError
import com.familyriskreview.core.model.result.DomainResult
import com.familyriskreview.core.model.service.Clock
import com.familyriskreview.core.model.service.IdGenerator
import com.familyriskreview.core.model.suggestion.ResponsibilitySuggestionEngine
import com.familyriskreview.core.model.validation.CalculationFailureMapper
import com.familyriskreview.core.model.validation.HouseholdRules
import com.familyriskreview.core.model.validation.ProgressionGates
import com.familyriskreview.core.model.validation.ResponsibilityRules
import com.familyriskreview.core.model.validation.ScenarioRules
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class CreateReviewUseCase
@Inject
internal constructor(
    private val reviewWriter: ReviewMutationWriter,
) {
    suspend operator fun invoke(
        mode: ReviewMode,
        language: AppLanguage,
        assumptions: CalculationAssumptions = CalculationAssumptions.Default,
    ): DomainResult<Review> = reviewWriter.createReview(mode, language, assumptions)
}

class ResumeReviewUseCase
@Inject
constructor(
    private val reviewReader: ReviewReader,
) {
    suspend operator fun invoke(reviewId: String): DomainResult<Review> {
        val review =
            reviewReader.getReview(reviewId)
                ?: return DomainResult.failure(DomainError.NotFound("Review $reviewId not found"))
        return when (review.status) {
            ReviewStatus.IN_PROGRESS -> DomainResult.success(review)
            ReviewStatus.ARCHIVED ->
                DomainResult.failure(
                    DomainError.IllegalState("Archived review must be restored before resume"),
                )
            ReviewStatus.COMPLETED -> DomainResult.success(review)
            ReviewStatus.DELETED ->
                DomainResult.failure(DomainError.IllegalState("Deleted reviews cannot be resumed"))
        }
    }
}

class AdvanceReviewStepUseCase
@Inject
internal constructor(
    private val reviewReader: ReviewReader,
    private val reviewWriter: ReviewMutationWriter,
    private val householdRepository: HouseholdRepository,
    private val responsibilityRepository: ResponsibilityRepository,
    private val snapshotRepository: CalculationSnapshotRepository,
) {
    suspend operator fun invoke(
        reviewId: String,
        expectedRevision: Long,
        forward: Boolean = true,
    ): DomainResult<Review> {
        val review =
            reviewReader.getReview(reviewId)
                ?: return DomainResult.failure(DomainError.NotFound("Review $reviewId not found"))
        if (forward) {
            val members = householdRepository.getMembers(reviewId)
            val responsibilities = responsibilityRepository.getResponsibilities(reviewId)
            val snapshot = snapshotRepository.getLatest(reviewId)
            val gate =
                ProgressionGates.validateLeaving(
                    step = review.currentStep,
                    review = review,
                    members = members,
                    responsibilities = responsibilities,
                    latestSnapshot = snapshot,
                    authoritativeCalculationVersion = ResponsibilityCalculator.CALCULATION_VERSION,
                )
            if (!gate.isValid) {
                return DomainResult.failure(DomainError.Validation(gate.errors))
            }
        }
        val next =
            if (forward) {
                ReviewProgression.nextStep(review.mode, review.currentStep)
            } else {
                ReviewProgression.previousStep(review.mode, review.currentStep)
            } ?: return DomainResult.failure(
                DomainError.Transition("No further step from ${review.currentStep}"),
            )
        return reviewWriter.advanceStep(reviewId, expectedRevision, next)
    }
}

class SaveHouseholdMemberUseCase
@Inject
constructor(
    private val householdRepository: HouseholdRepository,
) {
    suspend operator fun invoke(
        member: HouseholdMember,
        expectedRevision: Long,
        focusUpdate: FocusUpdate = FocusUpdate.Unchanged,
    ): DomainResult<Review> = householdRepository.saveMember(member, expectedRevision, focusUpdate)
}

class RemoveHouseholdMemberUseCase
@Inject
constructor(
    private val householdRepository: HouseholdRepository,
) {
    suspend operator fun invoke(
        memberId: String,
        reviewId: String,
        expectedRevision: Long,
    ): DomainResult<Review> = householdRepository.removeMember(memberId, reviewId, expectedRevision)
}

class SelectResponsibilityUseCase
@Inject
constructor(
    private val responsibilityRepository: ResponsibilityRepository,
) {
    suspend operator fun invoke(
        responsibility: Responsibility,
        selected: Boolean,
        expectedRevision: Long,
    ): DomainResult<Review> {
        val updated =
            responsibility.copy(
                isSelected = selected,
                priority = if (selected) responsibility.priority else null,
            )
        val selectionErrors = ResponsibilityRules.validateSelection(updated)
        if (!selectionErrors.isValid) {
            return DomainResult.failure(DomainError.Validation(selectionErrors.errors))
        }
        return responsibilityRepository.saveResponsibility(updated, expectedRevision)
    }
}

class PrioritiseResponsibilityUseCase
@Inject
constructor(
    private val responsibilityRepository: ResponsibilityRepository,
    private val reviewReader: ReviewReader,
) {
    suspend operator fun invoke(
        responsibilityId: String,
        reviewId: String,
        priority: ResponsibilityPriority,
        expectedRevision: Long,
    ): DomainResult<Review> {
        val review =
            reviewReader.getReview(reviewId)
                ?: return DomainResult.failure(DomainError.NotFound("Review $reviewId not found"))
        val current =
            responsibilityRepository.getResponsibilities(reviewId).find { it.id == responsibilityId }
                ?: return DomainResult.failure(
                    DomainError.NotFound("Responsibility $responsibilityId not found"),
                )
        val updated = current.copy(priority = priority, isSelected = true)
        val projected =
            responsibilityRepository
                .getResponsibilities(reviewId)
                .map { if (it.id == responsibilityId) updated else it }
        val validation = ResponsibilityRules.validatePrioritisation(projected, review.mode)
        if (!validation.isValid) {
            return DomainResult.failure(DomainError.Validation(validation.errors))
        }
        return responsibilityRepository.saveResponsibility(updated, expectedRevision)
    }
}

/**
 * Persists responsibility details as a draft. Incomplete / non-quantified items may be
 * saved while editing; progression and calculation enforce [ResponsibilityRules.validateDetails].
 */
class SaveResponsibilityDetailsUseCase
@Inject
constructor(
    private val responsibilityRepository: ResponsibilityRepository,
    private val reviewReader: ReviewReader,
) {
    suspend operator fun invoke(
        responsibility: Responsibility,
        expectedRevision: Long,
    ): DomainResult<Review> {
        val review =
            reviewReader.getReview(responsibility.reviewId)
                ?: return DomainResult.failure(
                    DomainError.NotFound("Review ${responsibility.reviewId} not found"),
                )
        val validation = ResponsibilityRules.validateDraftDetails(responsibility, review.mode)
        if (!validation.isValid) {
            return DomainResult.failure(DomainError.Validation(validation.errors))
        }
        return responsibilityRepository.saveResponsibility(responsibility, expectedRevision)
    }
}

class SuggestResponsibilitiesUseCase
@Inject
constructor(
    private val householdRepository: HouseholdRepository,
) {
    suspend operator fun invoke(reviewId: String): List<ResponsibilitySuggestionEngine.Suggestion> {
        val members = householdRepository.getMembers(reviewId)
        return ResponsibilitySuggestionEngine.suggest(members)
    }
}

class CalculateReviewSummaryUseCase
@Inject
constructor(
    private val reviewReader: ReviewReader,
    private val responsibilityRepository: ResponsibilityRepository,
    private val snapshotRepository: CalculationSnapshotRepository,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
) {
    data class Result(
        val review: Review,
        val base: GrossResponsibilityResult,
        val scenarios: List<GrossResponsibilityResult>,
        val snapshot: CalculationSnapshot,
    )

    suspend operator fun invoke(
        reviewId: String,
        expectedRevision: Long,
        scenarios: List<CalculationScenario> = emptyList(),
    ): DomainResult<Result> {
        val review =
            reviewReader.getReview(reviewId)
                ?: return DomainResult.failure(DomainError.NotFound("Review $reviewId not found"))
        val orderedScenarios =
            when (val validated = ScenarioRules.validateAndOrder(review.mode, scenarios)) {
                is DomainResult.Success -> validated.value
                is DomainResult.Failure -> return validated
            }
        val responsibilities = responsibilityRepository.getResponsibilities(reviewId)
        val validation = ResponsibilityRules.validateForCalculation(responsibilities, review.mode)
        if (!validation.isValid) {
            return DomainResult.failure(DomainError.Validation(validation.errors))
        }
        val reviewAssumptions =
            when (val parsed = CalculationAssumptions.parseStored(review.assumptionsJson)) {
                is DomainResult.Success -> parsed.value
                is DomainResult.Failure -> return parsed
            }
        val effectiveBaseAssumptions =
            orderedScenarios
                .firstOrNull { it.kind == ScenarioKind.BASE }
                ?.assumptions
                ?: reviewAssumptions

        val computed =
            try {
                val canonicalBase =
                    ResponsibilityCalculator.indicativeGrossResponsibility(
                        responsibilities = responsibilities,
                        assumptions = effectiveBaseAssumptions,
                        scenarioKind = ScenarioKind.BASE,
                    )
                // Only Lower / Higher in ordered results; empty / Base-only → empty list.
                val scenarioResults =
                    orderedScenarios
                        .filter { it.kind != ScenarioKind.BASE }
                        .map { scenario ->
                            ResponsibilityCalculator.indicativeGrossResponsibility(
                                responsibilities = responsibilities,
                                assumptions = scenario.assumptions,
                                scenarioKind = scenario.kind,
                            )
                        }
                val lines =
                    responsibilities
                        .filter { it.isSelected }
                        .map { item ->
                            ResponsibilityIndicativeLine(
                                responsibilityId = item.id,
                                catalogue = item.catalogue,
                                priority = item.priority,
                                indicativeAmountRupees =
                                ResponsibilityCalculator.optionalIndicativeAmount(
                                    item,
                                    effectiveBaseAssumptions,
                                ),
                                quantificationStatus = item.quantificationStatus,
                            )
                        }
                Triple(canonicalBase, scenarioResults, lines)
            } catch (ex: Throwable) {
                val mapped = CalculationFailureMapper.fromKnownThrowable(ex)
                if (mapped != null) {
                    return DomainResult.failure(mapped)
                }
                throw ex
            }

        val (canonicalBase, scenarioResults, lines) = computed
        val snapshot =
            CalculationSnapshot(
                id = idGenerator.newId(),
                reviewId = reviewId,
                reviewRevision = expectedRevision + 1,
                calculationInputRevision = review.calculationInputRevision,
                assumptionVersion = effectiveBaseAssumptions.version,
                calculationVersion = ResponsibilityCalculator.CALCULATION_VERSION,
                scenarioKind = ScenarioKind.BASE,
                assumptionsJson = effectiveBaseAssumptions.toJson(),
                mustContinueTotalRupees = canonicalBase.mustContinueTotalRupees,
                adjustableTotalRupees = canonicalBase.adjustableTotalRupees,
                postponedTotalRupees = canonicalBase.postponedTotalRupees,
                perResponsibilityJson = SnapshotJson.encodeToString(lines),
                generatedAtEpochMs = clock.now().toEpochMilliseconds(),
            )
        return when (
            val saved =
                snapshotRepository.saveSnapshot(
                    snapshot = snapshot,
                    expectedReviewRevision = expectedRevision,
                    markSummaryFresh = true,
                )
        ) {
            is DomainResult.Success ->
                DomainResult.success(
                    Result(
                        review = saved.value,
                        base = canonicalBase,
                        scenarios = scenarioResults,
                        snapshot = snapshot.copy(reviewRevision = saved.value.revision),
                    ),
                )
            is DomainResult.Failure -> saved
        }
    }

    private companion object {
        val SnapshotJson =
            Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
            }
    }
}

/**
 * Only public completion path for features. Enforces a fresh summary audit before
 * delegating to the internal mutation writer.
 */
class CompleteReviewUseCase
@Inject
internal constructor(
    private val reviewReader: ReviewReader,
    private val reviewWriter: ReviewMutationWriter,
    private val snapshotRepository: CalculationSnapshotRepository,
) {
    suspend operator fun invoke(
        reviewId: String,
        expectedRevision: Long,
        customerAcknowledged: Boolean,
    ): DomainResult<Review> {
        val review =
            reviewReader.getReview(reviewId)
                ?: return DomainResult.failure(DomainError.NotFound("Review $reviewId not found"))
        if (review.status != ReviewStatus.IN_PROGRESS) {
            return DomainResult.failure(
                DomainError.IllegalState("Only in-progress reviews can be completed"),
            )
        }
        val latest = snapshotRepository.getLatest(reviewId)
        val fresh =
            ProgressionGates.validateFreshSummary(
                review = review,
                latestSnapshot = latest,
                authoritativeCalculationVersion = ResponsibilityCalculator.CALCULATION_VERSION,
            )
        if (!fresh.isValid) {
            return DomainResult.failure(DomainError.Validation(fresh.errors))
        }
        if (snapshotRepository.isStale(review, ResponsibilityCalculator.CALCULATION_VERSION)) {
            return DomainResult.failure(
                DomainError.IllegalState("Summary is stale; recalculate before completing"),
            )
        }
        return reviewWriter.completeReview(reviewId, expectedRevision, customerAcknowledged)
    }
}

class ArchiveReviewUseCase
@Inject
internal constructor(
    private val reviewWriter: ReviewMutationWriter,
) {
    suspend operator fun invoke(
        reviewId: String,
        expectedRevision: Long,
    ): DomainResult<Review> = reviewWriter.archiveReview(reviewId, expectedRevision)
}

class RestoreReviewUseCase
@Inject
internal constructor(
    private val reviewWriter: ReviewMutationWriter,
) {
    suspend operator fun invoke(
        reviewId: String,
        expectedRevision: Long,
    ): DomainResult<Review> = reviewWriter.restoreReview(reviewId, expectedRevision)
}

class ReopenReviewUseCase
@Inject
internal constructor(
    private val reviewWriter: ReviewMutationWriter,
) {
    suspend operator fun invoke(
        reviewId: String,
        expectedRevision: Long,
    ): DomainResult<Review> = reviewWriter.reopenReview(reviewId, expectedRevision)
}

class DeleteReviewUseCase
@Inject
internal constructor(
    private val reviewWriter: ReviewMutationWriter,
) {
    suspend operator fun invoke(
        reviewId: String,
        expectedRevision: Long,
    ): DomainResult<Review> = reviewWriter.softDeleteReview(reviewId, expectedRevision)
}

class SaveAdvisorReferenceUseCase
@Inject
constructor(
    private val advisorReferenceRepository: AdvisorReferenceRepository,
) {
    suspend operator fun invoke(reference: AdvisorReference): DomainResult<Unit> = advisorReferenceRepository.upsert(reference)
}

class ValidateHouseholdUseCase
@Inject
constructor(
    private val householdRepository: HouseholdRepository,
    private val reviewReader: ReviewReader,
) {
    suspend operator fun invoke(reviewId: String): DomainResult<Unit> {
        val review =
            reviewReader.getReview(reviewId)
                ?: return DomainResult.failure(DomainError.NotFound("Review $reviewId not found"))
        val members = householdRepository.getMembers(reviewId)
        val validation =
            HouseholdRules.validateHousehold(members, review.focusedIncomeContributorId)
        return if (validation.isValid) {
            DomainResult.success(Unit)
        } else {
            DomainResult.failure(DomainError.Validation(validation.errors))
        }
    }
}
