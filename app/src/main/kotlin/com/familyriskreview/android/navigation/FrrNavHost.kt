package com.familyriskreview.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.familyriskreview.android.splash.SplashRoute
import com.familyriskreview.core.data.repository.UserPreferencesRepository
import com.familyriskreview.core.data.usecase.CreateReviewUseCase
import com.familyriskreview.core.model.AppLanguage
import com.familyriskreview.core.model.ReviewMode
import com.familyriskreview.core.model.result.DomainResult
import com.familyriskreview.core.ui.navigation.FrrRoutes
import com.familyriskreview.feature.dashboard.DashboardRoute
import com.familyriskreview.feature.review.ReviewPlaceholderRoute
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

@HiltViewModel
class ShellViewModel @Inject constructor(
    private val createReview: CreateReviewUseCase,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val defaultLanguage = preferencesRepository.preferences
        .map { it.defaultLanguage }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppLanguage.ENGLISH)

    suspend fun startReview(mode: ReviewMode): String {
        val language = preferencesRepository.preferences.first().defaultLanguage
        return when (val result = createReview(mode = mode, language = language)) {
            is DomainResult.Success -> result.value.id
            is DomainResult.Failure -> error("Unable to create review: ${result.error}")
        }
    }
}

@Composable
fun FrrNavHost(
    modifier: Modifier = Modifier,
    shellViewModel: ShellViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = FrrRoutes.Splash,
        modifier = modifier,
    ) {
        composable<FrrRoutes.Splash> {
            SplashRoute(
                onFinished = {
                    navController.navigate(FrrRoutes.Dashboard) {
                        popUpTo(FrrRoutes.Splash) { inclusive = true }
                    }
                },
            )
        }

        composable<FrrRoutes.Dashboard> {
            DashboardRoute(
                onStartNewReview = {
                    navController.navigate(FrrRoutes.Welcome)
                },
                onOpenSettings = {
                    navController.navigate(FrrRoutes.Settings)
                },
            )
        }

        composable<FrrRoutes.Welcome> {
            WelcomeRoute(
                onBeginQuickReview = {
                    scope.launch {
                        val id = shellViewModel.startReview(ReviewMode.QUICK)
                        navController.navigate(FrrRoutes.Review(id))
                    }
                },
                onBeginGuidedReview = {
                    scope.launch {
                        val id = shellViewModel.startReview(ReviewMode.GUIDED)
                        navController.navigate(FrrRoutes.Review(id))
                    }
                },
                onBackToDashboard = {
                    navController.popBackStack(FrrRoutes.Dashboard, inclusive = false)
                },
            )
        }

        composable<FrrRoutes.Review> { entry ->
            val route = entry.toRoute<FrrRoutes.Review>()
            ReviewPlaceholderRoute(
                reviewId = route.reviewId,
                onContinue = {
                    navController.navigate(FrrRoutes.Summary(route.reviewId))
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
