package com.familyriskreview.android.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.familyriskreview.android.R
import com.familyriskreview.android.splash.SplashRoute
import com.familyriskreview.core.data.repository.UserPreferencesRepository
import com.familyriskreview.core.data.usecase.CreateReviewUseCase
import com.familyriskreview.core.designsystem.component.FrrTextAction
import com.familyriskreview.core.designsystem.theme.FrrTypography
import com.familyriskreview.core.designsystem.theme.frrColors
import com.familyriskreview.core.model.AppLanguage
import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.result.DomainError
import com.familyriskreview.core.model.result.DomainResult
import com.familyriskreview.core.ui.navigation.FrrRoutes
import com.familyriskreview.feature.dashboard.DashboardRoute
import com.familyriskreview.feature.review.ReviewRoute
import com.familyriskreview.feature.review.WelcomeRoute
import com.familyriskreview.feature.settings.SettingsRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface StartReviewUiResult {
    data class Success(val reviewId: String) : StartReviewUiResult

    data class Failure(val messageResHint: String) : StartReviewUiResult
}

@HiltViewModel
class ShellViewModel
@Inject
constructor(
    private val createReview: CreateReviewUseCase,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    val defaultLanguage =
        preferencesRepository.preferences
            .map { it.defaultLanguage }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppLanguage.ENGLISH)

    val reducedMotion =
        preferencesRepository.preferences
            .map { it.reducedMotion }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    /**
     * Creates a review once. Concurrent / rapid taps are ignored while [isCreating] is true.
     */
    suspend fun startReview(mode: ReviewMode): StartReviewUiResult {
        if (_isCreating.value) {
            return StartReviewUiResult.Failure("CREATING")
        }
        _isCreating.value = true
        return try {
            val language = preferencesRepository.preferences.first().defaultLanguage
            when (val result = createReview(mode = mode, language = language)) {
                is DomainResult.Success -> StartReviewUiResult.Success(result.value.id)
                is DomainResult.Failure ->
                    StartReviewUiResult.Failure(errorCode(result.error))
            }
        } finally {
            _isCreating.value = false
        }
    }

    private fun errorCode(error: DomainError): String = when (error) {
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

@Composable
fun FrrNavHost(
    modifier: Modifier = Modifier,
    shellViewModel: ShellViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val reducedMotion by shellViewModel.reducedMotion.collectAsStateWithLifecycle()
    val isCreating by shellViewModel.isCreating.collectAsStateWithLifecycle()
    var welcomeCreateErrorCode by remember { mutableStateOf<String?>(null) }

    NavHost(
        navController = navController,
        startDestination = FrrRoutes.Splash,
        modifier = modifier,
    ) {
        composable<FrrRoutes.Splash> {
            SplashRoute(
                reducedMotion = reducedMotion,
                onFinished = {
                    navController.navigate(FrrRoutes.Dashboard) {
                        popUpTo(FrrRoutes.Splash) { inclusive = true }
                    }
                },
            )
        }

        composable<FrrRoutes.Dashboard> {
            DashboardRoute(
                onOpenReview = { reviewId ->
                    navController.navigate(FrrRoutes.Review(reviewId))
                },
                onStartWelcome = { mode ->
                    navController.navigate(FrrRoutes.Welcome(mode.name))
                },
                onOpenSettings = {
                    navController.navigate(FrrRoutes.Settings)
                },
            )
        }

        composable<FrrRoutes.Welcome> { entry ->
            val route = entry.toRoute<FrrRoutes.Welcome>()
            val preferredMode =
                route.mode?.let { runCatching { ReviewMode.valueOf(it) }.getOrNull() }
            Column {
                WelcomeRoute(
                    preferredMode = preferredMode,
                    isCreating = isCreating,
                    onConfirm = { mode ->
                        scope.launch {
                            when (val result = shellViewModel.startReview(mode)) {
                                is StartReviewUiResult.Success -> {
                                    welcomeCreateErrorCode = null
                                    navController.navigate(FrrRoutes.Review(result.reviewId)) {
                                        popUpTo(FrrRoutes.Dashboard) { inclusive = false }
                                    }
                                }
                                is StartReviewUiResult.Failure -> {
                                    if (result.messageResHint != "CREATING") {
                                        welcomeCreateErrorCode = result.messageResHint
                                    }
                                }
                            }
                        }
                    },
                    onBackToDashboard = {
                        welcomeCreateErrorCode = null
                        navController.popBackStack(FrrRoutes.Dashboard, inclusive = false)
                    },
                )
                welcomeCreateErrorCode?.let { code ->
                    val colors = frrColors()
                    Column(
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 8.dp)
                            .background(colors.blockingError.copy(alpha = 0.08f))
                            .padding(16.dp),
                    ) {
                        Text(
                            text = stringResource(createErrorStringRes(code)),
                            style = FrrTypography.bodyMedium,
                            color = colors.blockingError,
                        )
                        Spacer(Modifier.height(4.dp))
                        FrrTextAction(
                            text = stringResource(R.string.welcome_create_error_dismiss),
                            onClick = { welcomeCreateErrorCode = null },
                        )
                    }
                }
            }
        }

        composable<FrrRoutes.Review> { entry ->
            val route = entry.toRoute<FrrRoutes.Review>()
            ReviewRoute(
                reviewId = route.reviewId,
                onBackToDashboard = {
                    navController.popBackStack(FrrRoutes.Dashboard, inclusive = false)
                },
            )
        }

        // Summary route retained for Phase 3 — intentionally not linked from Phase 2 UI.
        composable<FrrRoutes.Settings> {
            SettingsRoute(
                onBack = { navController.popBackStack() },
            )
        }
    }
}

@androidx.annotation.StringRes
private fun createErrorStringRes(code: String): Int = when (code) {
    "PERSISTENCE" -> R.string.domain_error_persistence
    "CONFLICT" -> R.string.domain_error_fallback
    else -> R.string.domain_error_fallback
}
