package com.familyriskreview.feature.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyriskreview.core.data.repository.HouseholdRepository
import com.familyriskreview.core.data.repository.ResponsibilityRepository
import com.familyriskreview.core.data.repository.ReviewReader
import com.familyriskreview.core.data.usecase.AdvanceReviewStepUseCase
import com.familyriskreview.core.data.usecase.PrioritiseResponsibilityUseCase
import com.familyriskreview.core.data.usecase.RemoveHouseholdMemberUseCase
import com.familyriskreview.core.data.usecase.SaveHouseholdMemberUseCase
import com.familyriskreview.core.data.usecase.SaveResponsibilityDetailsUseCase
import com.familyriskreview.core.data.usecase.SelectResponsibilityUseCase
import com.familyriskreview.core.data.usecase.SuggestResponsibilitiesUseCase
import com.familyriskreview.core.model.ContributionStatus
import com.familyriskreview.core.model.DependencyStatus
import com.familyriskreview.core.model.FamilyMemberType
import com.familyriskreview.core.model.FocusUpdate
import com.familyriskreview.core.model.HouseholdMember
import com.familyriskreview.core.model.MoneyAmount
import com.familyriskreview.core.model.QuantificationStatus
import com.familyriskreview.core.model.Responsibility
import com.familyriskreview.core.model.ResponsibilityAmountModel
import com.familyriskreview.core.model.ResponsibilityCatalogue
import com.familyriskreview.core.model.ResponsibilityPriority
import com.familyriskreview.core.model.ResponsibilityTiming
import com.familyriskreview.core.model.Review
import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.TimingKind
import com.familyriskreview.core.model.result.DomainError
import com.familyriskreview.core.model.result.DomainResult
import com.familyriskreview.core.model.result.ValidationIssue
import com.familyriskreview.core.model.service.IdGenerator
import com.familyriskreview.core.model.suggestion.ResponsibilitySuggestionEngine
import com.familyriskreview.core.model.validation.ResponsibilityRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ReviewSaveState {
    data object Idle : ReviewSaveState

    data object Saving : ReviewSaveState

    data object Saved : ReviewSaveState

    /** Validation blocked the write. [code] is a stable domain code for localisation. */
    data class ValidationFailure(
        val code: String,
        val issues: List<ValidationIssue> = emptyList(),
    ) : ReviewSaveState

    /** Persistence / generic failure. [code] maps via [DomainMessageMapper]. */
    data class PersistenceFailure(
        val code: String,
        val technicalDetail: String? = null,
    ) : ReviewSaveState

    /** Revision conflict — UI must offer Reload, not only Dismiss. */
    data class Conflict(
        val technicalDetail: String? = null,
        val currentRevision: Long? = null,
    ) : ReviewSaveState
}

data class ReviewUiState(
    val review: Review? = null,
    val members: List<HouseholdMember> = emptyList(),
    val responsibilities: List<Responsibility> = emptyList(),
    val suggestions: List<ResponsibilitySuggestionEngine.Suggestion> = emptyList(),
    val selectedMemberId: String? = null,
    val selectedResponsibilityId: String? = null,
    val activeOtherResponsibilityId: String? = null,
    val drafts: ReviewEditableDraftState = ReviewEditableDraftState(),
    val validationIssues: List<ValidationIssue> = emptyList(),
    val saveState: ReviewSaveState = ReviewSaveState.Idle,
    val loading: Boolean = true,
    /** Stable error category code for localisation; never customer-facing English. */
    val errorCode: String? = null,
    val conflictActive: Boolean = false,
    val draftDiscardedAfterConflict: Boolean = false,
)

@HiltViewModel
class ReviewViewModel
@Inject
constructor(
    private val savedStateHandle: SavedStateHandle,
    private val reviewReader: ReviewReader,
    private val householdRepository: HouseholdRepository,
    private val responsibilityRepository: ResponsibilityRepository,
    private val saveHouseholdMember: SaveHouseholdMemberUseCase,
    private val removeHouseholdMember: RemoveHouseholdMemberUseCase,
    private val selectResponsibility: SelectResponsibilityUseCase,
    private val prioritiseResponsibility: PrioritiseResponsibilityUseCase,
    private val saveResponsibilityDetails: SaveResponsibilityDetailsUseCase,
    private val suggestResponsibilities: SuggestResponsibilitiesUseCase,
    private val advanceReviewStep: AdvanceReviewStepUseCase,
    private val idGenerator: IdGenerator,
) : ViewModel() {
    val reviewId: String =
        checkNotNull(savedStateHandle["reviewId"]) { "Review route requires reviewId" }

    private val mutationCoordinator = SerialMutationCoordinator()

    private val drafts =
        savedStateHandle.getStateFlow(KEY_DRAFTS, ReviewEditableDraftState())

    private val validationIssues = MutableStateFlow<List<ValidationIssue>>(emptyList())
    private val saveState = MutableStateFlow<ReviewSaveState>(ReviewSaveState.Idle)
    private val loading = MutableStateFlow(true)
    private val errorCode = MutableStateFlow<String?>(null)
    private val conflictActive = MutableStateFlow(false)
    private val suggestions =
        MutableStateFlow<List<ResponsibilitySuggestionEngine.Suggestion>>(emptyList())

    /** Pending details draft awaiting flush before step advance/back. */
    @Volatile
    private var pendingDetailsFlush: Responsibility? = null

    private data class DomainSlice(
        val review: Review?,
        val members: List<HouseholdMember>,
        val responsibilities: List<Responsibility>,
    )

    private data class LocalSlice(
        val drafts: ReviewEditableDraftState,
        val validationIssues: List<ValidationIssue>,
        val saveState: ReviewSaveState,
        val loading: Boolean,
        val errorCode: String?,
        val suggestions: List<ResponsibilitySuggestionEngine.Suggestion>,
        val conflictActive: Boolean,
    )

    private val domainSlice =
        combine(
            reviewReader.observeReview(reviewId),
            householdRepository.observeMembers(reviewId),
            responsibilityRepository.observeResponsibilities(reviewId),
        ) { review, members, responsibilities -> DomainSlice(review, members, responsibilities) }

    private val localSlice =
        combine(
            combine(drafts, validationIssues, saveState) { d, v, s -> Triple(d, v, s) },
            combine(loading, errorCode, suggestions, conflictActive) { l, e, sug, c ->
                LocalFlags(l, e, sug, c)
            },
        ) { triple, flags ->
            LocalSlice(
                drafts = triple.first,
                validationIssues = triple.second,
                saveState = triple.third,
                loading = flags.loading,
                errorCode = flags.errorCode,
                suggestions = flags.suggestions,
                conflictActive = flags.conflictActive,
            )
        }

    private data class LocalFlags(
        val loading: Boolean,
        val errorCode: String?,
        val suggestions: List<ResponsibilitySuggestionEngine.Suggestion>,
        val conflictActive: Boolean,
    )

    val uiState: StateFlow<ReviewUiState> =
        combine(domainSlice, localSlice) { domain, local ->
            ReviewUiState(
                review = domain.review,
                members = domain.members.sortedBy { it.sortOrder },
                responsibilities = domain.responsibilities.sortedBy { it.sortOrder },
                suggestions = local.suggestions,
                selectedMemberId =
                local.drafts.selectedMemberId
                    ?: domain.review?.focusedIncomeContributorId,
                selectedResponsibilityId = local.drafts.selectedResponsibilityId,
                activeOtherResponsibilityId = local.drafts.activeOtherResponsibilityId,
                drafts = local.drafts,
                validationIssues = local.validationIssues,
                saveState = local.saveState,
                loading = local.loading && domain.review == null,
                errorCode = local.errorCode,
                conflictActive = local.conflictActive,
                draftDiscardedAfterConflict = local.drafts.draftDiscardedAfterConflict,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReviewUiState())

    init {
        viewModelScope.launch {
            householdRepository.observeMembers(reviewId).collect { members ->
                suggestions.value = suggestResponsibilities(reviewId)
                val current = drafts.value
                if (current.selectedMemberId == null) {
                    val focusId =
                        reviewReader.getReview(reviewId)?.focusedIncomeContributorId
                            ?: members.firstOrNull()?.id
                    updateDrafts { it.copy(selectedMemberId = focusId) }
                }
                loading.value = false
            }
        }
    }

    fun addMember(relationship: FamilyMemberType) {
        enqueueMutation { review ->
            val members = householdRepository.getMembers(reviewId)
            if (relationship == FamilyMemberType.SELF &&
                members.any { it.relationship == FamilyMemberType.SELF }
            ) {
                val issue =
                    ValidationIssue(
                        code = "HOUSEHOLD_DUPLICATE_SELF",
                        message = "A review cannot contain more than one Self member",
                        field = "relationship",
                    )
                validationIssues.value = listOf(issue)
                saveState.value = ReviewSaveState.ValidationFailure(issue.code, listOf(issue))
                return@enqueueMutation
            }
            val defaults = defaultsFor(relationship)
            val member =
                HouseholdMember(
                    id = idGenerator.newId(),
                    reviewId = reviewId,
                    relationship = relationship,
                    age = null,
                    contributionStatus = defaults.first,
                    dependencyStatus = defaults.second,
                    sortOrder = members.size,
                )
            val focus =
                if (review.focusedIncomeContributorId == null && isIncomeContributor(defaults.first)) {
                    FocusUpdate.Set(member.id)
                } else {
                    FocusUpdate.Unchanged
                }
            applyReviewResult(saveHouseholdMember(member, review.revision, focus)) {
                updateDrafts { it.copy(selectedMemberId = member.id) }
            }
        }
    }

    fun updateMember(member: HouseholdMember, focusUpdate: FocusUpdate = FocusUpdate.Unchanged) {
        enqueueMutation { review ->
            applyReviewResult(saveHouseholdMember(member, review.revision, focusUpdate))
        }
    }

    fun removeMember(memberId: String) {
        enqueueMutation { review ->
            applyReviewResult(removeHouseholdMember(memberId, reviewId, review.revision)) {
                updateDrafts { drafts ->
                    drafts.copy(
                        selectedMemberId =
                        drafts.selectedMemberId?.takeUnless { it == memberId },
                        householdDrafts = drafts.householdDrafts - memberId,
                    )
                }
            }
        }
    }

    fun setFocus(memberId: String) {
        enqueueMutation { review ->
            val member =
                householdRepository.getMembers(reviewId).find { it.id == memberId }
                    ?: return@enqueueMutation
            updateDrafts { it.copy(selectedMemberId = memberId) }
            applyReviewResult(saveHouseholdMember(member, review.revision, FocusUpdate.Set(memberId)))
        }
    }

    fun selectMemberForEdit(memberId: String) {
        updateDrafts { it.copy(selectedMemberId = memberId) }
    }

    fun updateHouseholdDraft(draft: HouseholdMemberDraft) {
        updateDrafts { state ->
            state.copy(householdDrafts = state.householdDrafts + (draft.memberId to draft))
        }
    }

    /**
     * Toggle a catalogue responsibility. For [ResponsibilityCatalogue.OTHER], identity is always
     * by responsibility id — never by editable label.
     */
    fun toggleResponsibility(
        catalogue: ResponsibilityCatalogue,
        selected: Boolean,
        responsibilityId: String? = null,
    ) {
        enqueueMutation { review ->
            val existingList = responsibilityRepository.getResponsibilities(reviewId)
            val existing =
                when {
                    responsibilityId != null -> existingList.find { it.id == responsibilityId }
                    catalogue == ResponsibilityCatalogue.OTHER -> {
                        val activeId = drafts.value.activeOtherResponsibilityId
                        existingList.find { it.id == activeId && it.catalogue == ResponsibilityCatalogue.OTHER }
                            ?: existingList.find { it.catalogue == ResponsibilityCatalogue.OTHER }
                    }
                    else -> existingList.find { it.catalogue == catalogue }
                }
            val customLabel =
                if (catalogue == ResponsibilityCatalogue.OTHER) {
                    drafts.value.otherLabel.text.trim().ifBlank { existing?.customLabel }
                } else {
                    null
                }
            val responsibility =
                existing
                    ?: Responsibility(
                        id = idGenerator.newId(),
                        reviewId = reviewId,
                        catalogue = catalogue,
                        customLabel = customLabel,
                        isSelected = selected,
                        sortOrder = existingList.size,
                    )
            val toSave =
                responsibility.copy(
                    customLabel =
                    if (catalogue == ResponsibilityCatalogue.OTHER) {
                        customLabel ?: responsibility.customLabel
                    } else {
                        responsibility.customLabel
                    },
                )
            applyReviewResult(selectResponsibility(toSave, selected, review.revision)) {
                if (catalogue == ResponsibilityCatalogue.OTHER) {
                    updateDrafts { it.copy(activeOtherResponsibilityId = toSave.id) }
                }
            }
        }
    }

    fun updateOtherLabelDraft(label: TextDraft) {
        updateDrafts { it.copy(otherLabel = label) }
    }

    /** Persist label change for the active Other item without creating a duplicate. */
    fun saveActiveOtherLabel() {
        val activeId = drafts.value.activeOtherResponsibilityId ?: return
        val label = drafts.value.otherLabel.text.trim().ifBlank { null }
        enqueueMutation { review ->
            val existing =
                responsibilityRepository.getResponsibilities(reviewId).find { it.id == activeId }
                    ?: return@enqueueMutation
            applyReviewResult(
                selectResponsibility(
                    existing.copy(customLabel = label, isSelected = true),
                    selected = true,
                    expectedRevision = review.revision,
                ),
            )
        }
    }

    /**
     * Explicit action to create a second custom responsibility with a stable new id.
     */
    fun addAnotherCustomResponsibility() {
        enqueueMutation { review ->
            val existingList = responsibilityRepository.getResponsibilities(reviewId)
            val responsibility =
                Responsibility(
                    id = idGenerator.newId(),
                    reviewId = reviewId,
                    catalogue = ResponsibilityCatalogue.OTHER,
                    // Provisional label so SelectResponsibility validation can persist a new id;
                    // the editable draft is cleared for the user to rename.
                    customLabel = "Custom",
                    isSelected = true,
                    sortOrder = existingList.size,
                )
            applyReviewResult(selectResponsibility(responsibility, selected = true, expectedRevision = review.revision)) {
                updateDrafts {
                    it.copy(
                        activeOtherResponsibilityId = responsibility.id,
                        otherLabel = TextDraft(),
                    )
                }
            }
        }
    }

    fun setPriority(responsibilityId: String, priority: ResponsibilityPriority) {
        enqueueMutation { review ->
            applyReviewResult(
                prioritiseResponsibility(responsibilityId, reviewId, priority, review.revision),
            )
        }
    }

    fun selectResponsibilityForEdit(responsibilityId: String) {
        updateDrafts { it.copy(selectedResponsibilityId = responsibilityId) }
    }

    fun updateDetailsDraft(draft: ResponsibilityDetailsDraft) {
        updateDrafts { state ->
            state.copy(detailsDrafts = state.detailsDrafts + (draft.responsibilityId to draft.copy(dirty = true)))
        }
        pendingDetailsFlush = buildResponsibilityFromDraft(draft)
    }

    fun saveDetailsDraft(responsibility: Responsibility) {
        pendingDetailsFlush = responsibility
        enqueueMutation { review ->
            applyReviewResult(saveResponsibilityDetails(responsibility, review.revision)) {
                pendingDetailsFlush = null
                updateDrafts { state ->
                    state.copy(
                        detailsDrafts =
                        state.detailsDrafts + (
                            responsibility.id to
                                (
                                    state.detailsDrafts[responsibility.id]?.copy(dirty = false)
                                        ?: detailsDraftFrom(responsibility)
                                    )
                            ),
                    )
                }
            }
        }
    }

    fun advance() {
        enqueueMutation { review ->
            flushPendingDetails(review)?.let { failed ->
                applyReviewResult(failed)
                return@enqueueMutation
            }
            val latest = requireLatestReview() ?: return@enqueueMutation
            applyReviewResult(advanceReviewStep(reviewId, latest.revision, forward = true))
        }
    }

    fun goBack() {
        enqueueMutation { review ->
            flushPendingDetails(review)?.let { failed ->
                applyReviewResult(failed)
                return@enqueueMutation
            }
            val latest = requireLatestReview() ?: return@enqueueMutation
            applyReviewResult(advanceReviewStep(reviewId, latest.revision, forward = false))
        }
    }

    fun clearError() {
        errorCode.value = null
        validationIssues.value = emptyList()
        when (saveState.value) {
            is ReviewSaveState.ValidationFailure,
            is ReviewSaveState.PersistenceFailure,
            -> saveState.value = ReviewSaveState.Idle
            else -> Unit
        }
        // Conflict is not cleared by Dismiss alone — use reloadConflict() or leave.
    }

    fun acknowledgeDraftDiscarded() {
        updateDrafts { it.copy(draftDiscardedAfterConflict = false) }
    }

    /**
     * Reload latest persisted review after a revision conflict.
     * Clears stale revision assumptions and discards unsaved local drafts with an explicit flag.
     */
    fun reloadConflict() {
        viewModelScope.launch {
            mutationCoordinator.enqueue {
                loading.value = true
                val hadDirtyDrafts =
                    drafts.value.detailsDrafts.values.any { it.dirty } ||
                        drafts.value.householdDrafts.isNotEmpty() ||
                        drafts.value.otherLabel.text.isNotBlank()
                // Re-read from persistence; observers will emit fresh domain state.
                reviewReader.getReview(reviewId)
                householdRepository.getMembers(reviewId)
                responsibilityRepository.getResponsibilities(reviewId)
                suggestions.value = suggestResponsibilities(reviewId)
                updateDrafts {
                    ReviewEditableDraftState(
                        selectedMemberId = it.selectedMemberId,
                        selectedResponsibilityId = it.selectedResponsibilityId,
                        activeOtherResponsibilityId = it.activeOtherResponsibilityId,
                        draftDiscardedAfterConflict = hadDirtyDrafts,
                    )
                }
                pendingDetailsFlush = null
                conflictActive.value = false
                errorCode.value = null
                validationIssues.value = emptyList()
                saveState.value = ReviewSaveState.Idle
                loading.value = false
            }
        }
    }

    fun reload() = reloadConflict()

    fun mayRemainNonQuantified(priority: ResponsibilityPriority?): Boolean {
        val mode = uiState.value.review?.mode ?: return false
        return ResponsibilityRules.mayRemainNonQuantified(mode, priority)
    }

    private suspend fun flushPendingDetails(review: Review): DomainResult<Review>? {
        val pending = pendingDetailsFlush ?: return null
        val result = saveResponsibilityDetails(pending, review.revision)
        if (result is DomainResult.Success) {
            pendingDetailsFlush = null
        }
        return result.takeUnless { it is DomainResult.Success }
    }

    private fun enqueueMutation(block: suspend (Review) -> Unit) {
        viewModelScope.launch {
            mutationCoordinator.enqueue {
                val review = requireLatestReview() ?: return@enqueue
                if (conflictActive.value && saveState.value is ReviewSaveState.Conflict) {
                    // Block further writes until reload; still allow reloadConflict.
                    return@enqueue
                }
                saveState.value = ReviewSaveState.Saving
                block(review)
            }
        }
    }

    private suspend fun requireLatestReview(): Review? {
        val review = reviewReader.getReview(reviewId)
        if (review == null) {
            errorCode.value = "NOT_FOUND"
            saveState.value = ReviewSaveState.PersistenceFailure("NOT_FOUND")
            loading.value = false
        }
        return review
    }

    private fun applyReviewResult(result: DomainResult<Review>, onSuccess: (() -> Unit)? = null) {
        when (result) {
            is DomainResult.Success -> {
                validationIssues.value = emptyList()
                errorCode.value = null
                conflictActive.value = false
                saveState.value = ReviewSaveState.Saved
                onSuccess?.invoke()
            }
            is DomainResult.Failure -> handleFailure(result.error)
        }
    }

    private fun handleFailure(error: DomainError) {
        when (error) {
            is DomainError.Validation -> {
                validationIssues.value = error.issues
                val code = error.issues.firstOrNull()?.code ?: "VALIDATION"
                saveState.value = ReviewSaveState.ValidationFailure(code, error.issues)
                errorCode.value = code
            }
            is DomainError.Conflict -> {
                conflictActive.value = true
                saveState.value =
                    ReviewSaveState.Conflict(
                        technicalDetail = error.message,
                        currentRevision = error.currentRevision,
                    )
                errorCode.value = "CONFLICT"
            }
            else -> {
                val code = domainErrorCode(error)
                saveState.value =
                    ReviewSaveState.PersistenceFailure(code, technicalDetail = error.toString())
                errorCode.value = code
            }
        }
    }

    private fun updateDrafts(transform: (ReviewEditableDraftState) -> ReviewEditableDraftState) {
        savedStateHandle[KEY_DRAFTS] = transform(drafts.value)
    }

    private fun buildResponsibilityFromDraft(draft: ResponsibilityDetailsDraft): Responsibility? {
        val base =
            uiState.value.responsibilities.find { it.id == draft.responsibilityId }
                ?: return null
        val mode = uiState.value.review?.mode ?: ReviewMode.QUICK
        return applyDraftToResponsibility(base, draft, mode)
    }

    private fun defaultsFor(relationship: FamilyMemberType): Pair<ContributionStatus, DependencyStatus> = when (relationship) {
        FamilyMemberType.SELF ->
            ContributionStatus.PRIMARY_INCOME to DependencyStatus.NOT_APPLICABLE
        FamilyMemberType.SPOUSE_OR_PARTNER ->
            ContributionStatus.SHARED_INCOME to DependencyStatus.PARTLY_DEPENDENT
        FamilyMemberType.CHILD ->
            ContributionStatus.NON_CONTRIBUTOR to DependencyStatus.FULLY_DEPENDENT
        FamilyMemberType.PARENT ->
            ContributionStatus.NON_CONTRIBUTOR to DependencyStatus.PARTLY_DEPENDENT
        FamilyMemberType.OTHER_DEPENDANT ->
            ContributionStatus.NON_CONTRIBUTOR to DependencyStatus.FULLY_DEPENDENT
    }

    companion object {
        const val KEY_DRAFTS = "review_editable_drafts"

        fun detailsDraftFrom(responsibility: Responsibility): ResponsibilityDetailsDraft = ResponsibilityDetailsDraft(
            responsibilityId = responsibility.id,
            label = TextDraft.of(responsibility.customLabel.orEmpty()),
            monthlyAmount =
            TextDraft.of(
                MoneyInputFormatter.formatOrEmpty(responsibility.monthlyAmount?.amountRupees),
            ),
            currentAmount =
            TextDraft.of(
                MoneyInputFormatter.formatOrEmpty(responsibility.currentAmount?.amountRupees),
            ),
            years =
            TextDraft.of(
                (
                    responsibility.timing?.yearsUntilRequired
                        ?: responsibility.timing?.durationYears
                    )?.toString().orEmpty(),
            ),
            inflationPercent =
            TextDraft.of(
                responsibility.explicitInflationBps?.let { (it / 100.0).toString() }.orEmpty(),
            ),
            amountModelName = responsibility.amountModel?.name,
            notYetQuantified =
            responsibility.quantificationStatus == QuantificationStatus.NOT_YET_QUANTIFIED,
            dirty = false,
        )

        fun applyDraftToResponsibility(
            responsibility: Responsibility,
            draft: ResponsibilityDetailsDraft,
            mode: ReviewMode,
        ): Responsibility {
            val catalogue = responsibility.catalogue
            val amountModel =
                draft.amountModelName?.let { runCatching { ResponsibilityAmountModel.valueOf(it) }.getOrNull() }
                    ?: responsibility.amountModel
                    ?: ResponsibilityAmountModel.ONE_TIME
            fun moneyOrNull(field: TextDraft): MoneyAmount? = MoneyInputFormatter.parseRupees(field.text)?.let { MoneyAmount(it) }

            val timing =
                when {
                    isLivingLike(catalogue) ->
                        ResponsibilityTiming(
                            TimingKind.RECURRING_DURATION,
                            durationYears = draft.years.text.trim().toIntOrNull(),
                        )
                    isEducationLike(catalogue) ->
                        ResponsibilityTiming(
                            TimingKind.ONE_TIME_IN_YEARS,
                            yearsUntilRequired = draft.years.text.trim().toIntOrNull(),
                        )
                    isLoan(catalogue) -> ResponsibilityTiming(TimingKind.CURRENT_OUTSTANDING)
                    catalogue == ResponsibilityCatalogue.OTHER ->
                        when (amountModel) {
                            ResponsibilityAmountModel.ONE_TIME ->
                                ResponsibilityTiming(
                                    TimingKind.ONE_TIME_IN_YEARS,
                                    yearsUntilRequired = draft.years.text.trim().toIntOrNull(),
                                )
                            ResponsibilityAmountModel.RECURRING ->
                                ResponsibilityTiming(
                                    TimingKind.RECURRING_DURATION,
                                    durationYears = draft.years.text.trim().toIntOrNull(),
                                )
                        }
                    else -> responsibility.timing
                }
            val mayRemain =
                ResponsibilityRules.mayRemainNonQuantified(mode, responsibility.priority)
            val quantification =
                if (draft.notYetQuantified && mayRemain) {
                    QuantificationStatus.NOT_YET_QUANTIFIED
                } else {
                    QuantificationStatus.QUANTIFIED
                }
            return responsibility.copy(
                customLabel =
                if (catalogue == ResponsibilityCatalogue.OTHER) {
                    draft.label.text.trim().ifBlank { null }
                } else {
                    responsibility.customLabel
                },
                monthlyAmount =
                when {
                    isLivingLike(catalogue) -> moneyOrNull(draft.monthlyAmount)
                    catalogue == ResponsibilityCatalogue.OTHER &&
                        amountModel == ResponsibilityAmountModel.RECURRING ->
                        moneyOrNull(draft.monthlyAmount)
                    else -> null
                },
                currentAmount =
                when {
                    isEducationLike(catalogue) || isLoan(catalogue) -> moneyOrNull(draft.currentAmount)
                    catalogue == ResponsibilityCatalogue.OTHER &&
                        amountModel == ResponsibilityAmountModel.ONE_TIME ->
                        moneyOrNull(draft.currentAmount)
                    else -> null
                },
                timing = timing,
                amountModel = if (catalogue == ResponsibilityCatalogue.OTHER) amountModel else null,
                explicitInflationBps =
                if (catalogue == ResponsibilityCatalogue.OTHER) {
                    draft.inflationPercent.text.trim().toDoubleOrNull()?.let { (it * 100).toInt() }
                } else {
                    null
                },
                quantificationStatus = quantification,
            )
        }

        fun isLivingLike(c: ResponsibilityCatalogue): Boolean = c == ResponsibilityCatalogue.ESSENTIAL_FAMILY_LIVING_EXPENSES ||
            c == ResponsibilityCatalogue.PARENT_SUPPORT ||
            c == ResponsibilityCatalogue.SPOUSE_OR_PARTNER_SUPPORT ||
            c == ResponsibilityCatalogue.SPECIAL_NEEDS_DEPENDANT_SUPPORT ||
            c == ResponsibilityCatalogue.CHILDCARE_REPLACEMENT ||
            c == ResponsibilityCatalogue.HOUSEHOLD_CARE_REPLACEMENT

        fun isEducationLike(c: ResponsibilityCatalogue): Boolean = c == ResponsibilityCatalogue.CHILD_HIGHER_EDUCATION ||
            c == ResponsibilityCatalogue.CHILD_MARRIAGE_SUPPORT ||
            c == ResponsibilityCatalogue.BUYING_OR_COMPLETING_HOUSE

        fun isLoan(c: ResponsibilityCatalogue): Boolean = c == ResponsibilityCatalogue.HOME_LOAN_REPAYMENT ||
            c == ResponsibilityCatalogue.OTHER_OUTSTANDING_LOANS

        fun domainErrorCode(error: DomainError): String = when (error) {
            is DomainError.Validation -> error.issues.firstOrNull()?.code ?: "VALIDATION"
            is DomainError.Transition -> "TRANSITION"
            is DomainError.Conflict -> "CONFLICT"
            is DomainError.NotFound -> "NOT_FOUND"
            is DomainError.CollisionExhausted -> "COLLISION_EXHAUSTED"
            is DomainError.IllegalState -> "ILLEGAL_STATE"
            is DomainError.CorruptData -> error.code
            is DomainError.Persistence -> "PERSISTENCE"
            is DomainError.Calculation -> error.code
        }
    }
}

internal fun isIncomeContributor(status: ContributionStatus): Boolean = status == ContributionStatus.PRIMARY_INCOME ||
    status == ContributionStatus.SHARED_INCOME ||
    status == ContributionStatus.SUPPLEMENTARY_OR_IRREGULAR
