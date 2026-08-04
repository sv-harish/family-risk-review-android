package com.familyriskreview.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyriskreview.core.data.repository.ReviewReader
import com.familyriskreview.core.data.repository.UserPreferencesRepository
import com.familyriskreview.core.data.usecase.CreateReviewUseCase
import com.familyriskreview.core.model.Review
import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.ReviewStatus
import com.familyriskreview.core.model.ReviewStep
import com.familyriskreview.core.model.result.DomainError
import com.familyriskreview.core.model.result.DomainResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

data class DashboardReviewItem(
    val id: String,
    val reviewNumber: String,
    val mode: ReviewMode,
    val updatedAtLabel: String,
    val stage: ReviewStep,
    val status: ReviewStatus,
)

data class DashboardUiState(
    val loading: Boolean = true,
    val inProgress: List<DashboardReviewItem> = emptyList(),
    val completed: List<DashboardReviewItem> = emptyList(),
    val archived: List<DashboardReviewItem> = emptyList(),
    val createError: String? = null,
)

/**
 * Groups [ReviewReader] streams into dashboard sections.
 * Archived reviews stay separate from in-progress and completed.
 */
object DashboardReviewGrouping {
    fun toItem(review: Review): DashboardReviewItem =
        DashboardReviewItem(
            id = review.id,
            reviewNumber = review.reviewNumber,
            mode = review.mode,
            updatedAtLabel = formatUpdatedAt(review.updatedAt),
            stage = review.currentStep,
            status = review.status,
        )

    fun group(
        inProgress: List<Review>,
        completed: List<Review>,
        archived: List<Review>,
    ): Triple<List<DashboardReviewItem>, List<DashboardReviewItem>, List<DashboardReviewItem>> =
        Triple(
            inProgress.map(::toItem),
            completed.map(::toItem),
            archived.map(::toItem),
        )

    fun formatUpdatedAt(instant: Instant): String {
        val zoned =
            java.time.Instant
                .ofEpochMilli(instant.toEpochMilliseconds())
                .atZone(ZoneId.systemDefault())
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(zoned)
    }

    fun userMessage(error: DomainError): String =
        when (error) {
            is DomainError.Validation ->
                error.issues.firstOrNull()?.message ?: "Unable to create review."
            is DomainError.Transition -> error.message
            is DomainError.Conflict -> error.message
            is DomainError.NotFound -> error.message
            is DomainError.CollisionExhausted -> error.message
            is DomainError.IllegalState -> error.message
            is DomainError.CorruptData -> error.message
            is DomainError.Persistence -> error.message
            is DomainError.Calculation -> error.message
        }
}

@HiltViewModel
class DashboardViewModel
@Inject
constructor(
    private val reviewReader: ReviewReader,
    private val createReview: CreateReviewUseCase,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _createdReviewId = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val createdReviewId: SharedFlow<String> = _createdReviewId.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(
                reviewReader.observeInProgress(),
                reviewReader.observeByStatus(ReviewStatus.COMPLETED),
                reviewReader.observeByStatus(ReviewStatus.ARCHIVED),
            ) { inProgress, completed, archived ->
                DashboardReviewGrouping.group(inProgress, completed, archived)
            }.collect { (inProgress, completed, archived) ->
                _uiState.update {
                    it.copy(
                        loading = false,
                        inProgress = inProgress,
                        completed = completed,
                        archived = archived,
                    )
                }
            }
        }
    }

    fun startQuick() = startReview(ReviewMode.QUICK)

    fun startGuided() = startReview(ReviewMode.GUIDED)

    fun clearError() {
        _uiState.update { it.copy(createError = null) }
    }

    private fun startReview(mode: ReviewMode) {
        viewModelScope.launch {
            _uiState.update { it.copy(createError = null) }
            val language = preferencesRepository.preferences.first().defaultLanguage
            when (val result = createReview(mode = mode, language = language)) {
                is DomainResult.Success -> _createdReviewId.emit(result.value.id)
                is DomainResult.Failure ->
                    _uiState.update {
                        it.copy(createError = DashboardReviewGrouping.userMessage(result.error))
                    }
            }
        }
    }
}
