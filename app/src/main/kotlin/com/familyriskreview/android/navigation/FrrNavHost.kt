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
import com.familyriskreview.feature.summary.SummaryRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface StartReviewUiResult {
    data class Success(val reviewId: String) : StartReviewUiResult

    data class Failure(val message: String) : StartReviewUiResult
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

    suspend fun startReview(mode: ReviewMode): StartReviewUiResult {
        val language = preferencesRepository.preferences.first().defaultLanguage
        return when (val result = createReview(mode = mode, language = language)) {
            is DomainResult.Success -> StartReviewUiResult.Success(result.value.id)
            is DomainResult.Failure -> StartReviewUiResult.Failure(userMessage(result.error))
        }
    }

    private fun userMessage(error: DomainError): String =
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

@Composable
fun FrrNavHost(
    modifier: Modifier = Modifier,
    shellViewModel: ShellViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val reducedMotion by shellViewModel.reducedMotion.collectAsStateWithLifecycle()
    var welcomeCreateError by remember { mutableStateOf<String?>(null) }

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
                onOpenSettings = {
                    navController.navigate(FrrRoutes.Settings)
                },
            )
        }

        composable<FrrRoutes.Welcome> {
            Column {
                WelcomeRoute(
                    onBeginQuickReview = {
                        scope.launch {
                            when (val result = shellViewModel.startReview(ReviewMode.QUICK)) {
                                is StartReviewUiResult.Success -> {
                                    welcomeCreateError = null
                                    navController.navigate(FrrRoutes.Review(result.reviewId))
                                }
                                is StartReviewUiResult.Failure -> {
                                    welcomeCreateError = result.message
                                }
                            }
                        }
                    },
                    onBeginGuidedReview = {
                        scope.launch {
                            when (val result = shellViewModel.startReview(ReviewMode.GUIDED)) {
                                is StartReviewUiResult.Success -> {
                                    welcomeCreateError = null
                                    navController.navigate(FrrRoutes.Review(result.reviewId))
                                }
                                is StartReviewUiResult.Failure -> {
                                    welcomeCreateError = result.message
                                }
                            }
                        }
                    },
                    onBackToDashboard = {
                        welcomeCreateError = null
                        navController.popBackStack(FrrRoutes.Dashboard, inclusive = false)
                    },
                )
                welcomeCreateError?.let { message ->
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
                            text = message,
                            style = FrrTypography.bodyMedium,
                            color = colors.blockingError,
                        )
                        Spacer(Modifier.height(4.dp))
                        FrrTextAction(
                            text = stringResource(R.string.welcome_create_error_dismiss),
                            onClick = { welcomeCreateError = null },
                        )
                    }
                }
            }
        }

        composable<FrrRoutes.Review> { entry ->
            val route = entry.toRoute<FrrRoutes.Review>()
            ReviewRoute(
                reviewId = route.reviewId,
                onContinue = {
                    navController.navigate(FrrRoutes.Summary(route.reviewId))
                },
                onBackToDashboard = {
                    navController.popBackStack(FrrRoutes.Dashboard, inclusive = false)
                },
            )
        }

        composable<FrrRoutes.Summary> { entry ->
            val route = entry.toRoute<FrrRoutes.Summary>()
            SummaryRoute(
                reviewId = route.reviewId,
                onFinishSession = {
                    navController.navigate(FrrRoutes.Dashboard) {
                        popUpTo(FrrRoutes.Dashboard) { inclusive = true }
                    }
                },
                onBackToDashboard = {
                    navController.popBackStack(FrrRoutes.Dashboard, inclusive = false)
                },
            )
        }

        composable<FrrRoutes.Settings> {
            SettingsRoute(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
