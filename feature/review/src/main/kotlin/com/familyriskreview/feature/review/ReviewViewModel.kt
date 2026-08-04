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
import com.familyriskreview.core.model.Responsibility
import com.familyriskreview.core.model.ResponsibilityCatalogue
import com.familyriskreview.core.model.ResponsibilityPriority
import com.familyriskreview.core.model.Review
import com.familyriskreview.core.model.result.DomainError
import com.familyriskreview.core.model.result.DomainResult
import com.familyriskreview.core.model.result.ValidationIssue
import com.familyriskreview.core.model.service.IdGenerator
import com.familyriskreview.core.model.suggestion.ResponsibilitySuggestionEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    data class Error(val message: String) : ReviewSaveState
    data class Conflict(val message: String) : ReviewSaveState
}

data class ReviewUiState(
    val review: Review? = null,
    val members: List<HouseholdMember> = emptyList(),
    val responsibilities: List<Responsibility> = emptyList(),
    val suggestions: List<ResponsibilitySuggestionEngine.Suggestion> = emptyList(),
    val selectedMemberId: String? = null,
    val validationIssues: List<ValidationIssue> = emptyList(),
    val saveState: ReviewSaveState = ReviewSaveState.Idle,
    val loading: Boolean = true,
    val errorMessage: String? = null,
)

@HiltViewModel
class ReviewViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
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

    private val selectedMemberId = MutableStateFlow<String?>(null)
    private val validationIssues = MutableStateFlow<List<ValidationIssue>>(emptyList())
    private val saveState = MutableStateFlow<ReviewSaveState>(ReviewSaveState.Idle)
    private val loading = MutableStateFlow(true)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val suggestions =
        MutableStateFlow<List<ResponsibilitySuggestionEngine.Suggestion>>(emptyList())
    private var mutateJob: Job? = null

    private data class DomainSlice(
        val review: Review?,
        val members: List<HouseholdMember>,
        val responsibilities: List<Responsibility>,
    )

    private data class LocalSlice(
        val selectedMemberId: String?,
        val validationIssues: List<ValidationIssue>,
        val saveState: ReviewSaveState,
        val loading: Boolean,
        val errorMessage: String?,
        val suggestions: List<ResponsibilitySuggestionEngine.Suggestion>,
    )

    private val domainSlice =
        combine(
            reviewReader.observeReview(reviewId),
            householdRepository.observeMembers(reviewId),
            responsibilityRepository.observeResponsibilities(reviewId),
        ) { review, members, responsibilities -> DomainSlice(review, members, responsibilities) }

    private val localSlice =
        combine(
            combine(selectedMemberId, validationIssues, saveState) { a, b, c -> Triple(a, b, c) },
            combine(loading, errorMessage, suggestions) { a, b, c -> Triple(a, b, c) },
        ) { x, y ->
            LocalSlice(x.first, x.second, x.third, y.first, y.second, y.third)
        }

    val uiState: StateFlow<ReviewUiState> =
        combine(domainSlice, localSlice) { domain, local ->
            ReviewUiState(
                review = domain.review,
                members = domain.members.sortedBy { it.sortOrder },
                responsibilities = domain.responsibilities.sortedBy { it.sortOrder },
                suggestions = local.suggestions,
                selectedMemberId = local.selectedMemberId ?: domain.review?.focusedIncomeContributorId,
                validationIssues = local.validationIssues,
                saveState = local.saveState,
                loading = local.loading && domain.review == null,
                errorMessage = local.errorMessage,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReviewUiState())

    init {
        viewModelScope.launch {
            householdRepository.observeMembers(reviewId).collect { members ->
                suggestions.value = suggestResponsibilities(reviewId)
                if (selectedMemberId.value == null) {
                    selectedMemberId.value =
                        reviewReader.getReview(reviewId)?.focusedIncomeContributorId
                            ?: members.firstOrNull()?.id
                }
                loading.value = false
            }
        }
    }

    fun addMember(relationship: FamilyMemberType) {
        mutate { review ->
            if (relationship == FamilyMemberType.SELF &&
                uiState.value.members.any { it.relationship == FamilyMemberType.SELF }
            ) {
                val msg = "A review cannot contain more than one Self member"
                validationIssues.value =
                    listOf(ValidationIssue("HOUSEHOLD_DUPLICATE_SELF", msg, field = "relationship"))
                saveState.value = ReviewSaveState.Error(msg)
                return@mutate
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
                    sortOrder = uiState.value.members.size,
                )
            val focus =
                if (review.focusedIncomeContributorId == null && isIncomeContributor(defaults.first)) {
                    FocusUpdate.Set(member.id)
                } else {
                    FocusUpdate.Unchanged
                }
            applyReviewResult(saveHouseholdMember(member, review.revision, focus)) {
                selectedMemberId.value = member.id
            }
        }
    }

    fun updateMember(member: HouseholdMember, focusUpdate: FocusUpdate = FocusUpdate.Unchanged) {
        mutate { review -> applyReviewResult(saveHouseholdMember(member, review.revision, focusUpdate)) }
    }

    fun removeMember(memberId: String) {
        mutate { review ->
            applyReviewResult(removeHouseholdMember(memberId, reviewId, review.revision)) {
                if (selectedMemberId.value == memberId) selectedMemberId.value = null
            }
        }
    }

    fun setFocus(memberId: String) {
        mutate { review ->
            val member = uiState.value.members.find { it.id == memberId } ?: return@mutate
            selectedMemberId.value = memberId
            applyReviewResult(saveHouseholdMember(member, review.revision, FocusUpdate.Set(memberId)))
        }
    }

    fun selectMemberForEdit(memberId: String) {
        selectedMemberId.value = memberId
    }

    fun toggleResponsibility(
        catalogue: ResponsibilityCatalogue,
        selected: Boolean,
        customLabel: String? = null,
    ) {
        mutate { review ->
            val existing =
                uiState.value.responsibilities.find {
                    it.catalogue == catalogue &&
                        (catalogue != ResponsibilityCatalogue.OTHER || it.customLabel == customLabel)
                }
            val responsibility =
                existing
                    ?: Responsibility(
                        id = idGenerator.newId(),
                        reviewId = reviewId,
                        catalogue = catalogue,
                        customLabel = customLabel,
                        isSelected = selected,
                        sortOrder = uiState.value.responsibilities.size,
                    )
            applyReviewResult(
                selectResponsibility(
                    responsibility.copy(customLabel = customLabel ?: responsibility.customLabel),
                    selected,
                    review.revision,
                ),
            )
        }
    }

    fun setPriority(responsibilityId: String, priority: ResponsibilityPriority) {
        mutate { review ->
            applyReviewResult(
                prioritiseResponsibility(responsibilityId, reviewId, priority, review.revision),
            )
        }
    }

    fun saveDetailsDraft(responsibility: Responsibility) {
        mutate { review -> applyReviewResult(saveResponsibilityDetails(responsibility, review.revision)) }
    }

    fun advance() {
        mutate { review ->
            applyReviewResult(advanceReviewStep(reviewId, review.revision, forward = true))
        }
    }

    fun goBack() {
        mutate { review ->
            applyReviewResult(advanceReviewStep(reviewId, review.revision, forward = false))
        }
    }

    fun clearError() {
        errorMessage.value = null
        validationIssues.value = emptyList()
        if (saveState.value is ReviewSaveState.Error || saveState.value is ReviewSaveState.Conflict) {
            saveState.value = ReviewSaveState.Idle
        }
    }

    fun reload() {
        viewModelScope.launch {
            loading.value = true
            errorMessage.value = null
            suggestions.value = suggestResponsibilities(reviewId)
            loading.value = false
        }
    }

    private fun mutate(block: suspend (Review) -> Unit) {
        mutateJob?.cancel()
        mutateJob =
            viewModelScope.launch {
                val review = uiState.value.review ?: reviewReader.getReview(reviewId)
                if (review == null) {
                    errorMessage.value = "Review not found"
                    loading.value = false
                    return@launch
                }
                saveState.value = ReviewSaveState.Saving
                block(review)
            }
    }

    private fun applyReviewResult(result: DomainResult<Review>, onSuccess: (() -> Unit)? = null) {
        when (result) {
            is DomainResult.Success -> {
                validationIssues.value = emptyList()
                errorMessage.value = null
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
                val message = error.issues.firstOrNull()?.message ?: "Validation failed"
                saveState.value = ReviewSaveState.Error(message)
                errorMessage.value = message
            }
            is DomainError.Conflict -> {
                saveState.value = ReviewSaveState.Conflict(error.message)
                errorMessage.value = error.message
            }
            else -> {
                val message = error.toPlainMessage()
                saveState.value = ReviewSaveState.Error(message)
                errorMessage.value = message
            }
        }
    }

    private fun defaultsFor(relationship: FamilyMemberType): Pair<ContributionStatus, DependencyStatus> =
        when (relationship) {
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
}

internal fun isIncomeContributor(status: ContributionStatus): Boolean =
    status == ContributionStatus.PRIMARY_INCOME ||
        status == ContributionStatus.SHARED_INCOME ||
        status == ContributionStatus.SUPPLEMENTARY_OR_IRREGULAR

private fun DomainError.toPlainMessage(): String =
    when (this) {
        is DomainError.Validation -> issues.firstOrNull()?.message ?: "Please check the details"
        is DomainError.Conflict -> message
        is DomainError.NotFound -> message
        is DomainError.Transition -> message
        is DomainError.IllegalState -> message
        is DomainError.CollisionExhausted -> message
        is DomainError.CorruptData -> message
        is DomainError.Persistence -> message
        is DomainError.Calculation -> message
    }
