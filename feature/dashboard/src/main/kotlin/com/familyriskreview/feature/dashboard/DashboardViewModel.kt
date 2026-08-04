package com.familyriskreview.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyriskreview.core.data.repository.ReviewReader
import com.familyriskreview.core.model.Review
import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.ReviewStatus
import com.familyriskreview.core.model.ReviewStep
import com.familyriskreview.core.model.result.DomainError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    fun toItem(review: Review): DashboardReviewItem = DashboardReviewItem(
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
    ): Triple<List<DashboardReviewItem>, List<DashboardReviewItem>, List<DashboardReviewItem>> = Triple(
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

    fun userMessageCode(error: DomainError): String = when (error) {
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

@HiltViewModel
class DashboardViewModel
@Inject
constructor(
    private val reviewReader: ReviewReader,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

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

    fun clearError() {
        _uiState.update { it.copy(createError = null) }
    }
}
