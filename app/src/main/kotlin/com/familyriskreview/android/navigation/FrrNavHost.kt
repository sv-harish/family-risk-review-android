package com.familyriskreview.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.familyriskreview.android.splash.SplashRoute
import com.familyriskreview.core.ui.navigation.FrrRoutes
import com.familyriskreview.feature.dashboard.DashboardRoute
import com.familyriskreview.feature.review.ReviewPlaceholderRoute
import com.familyriskreview.feature.review.WelcomeRoute
import com.familyriskreview.feature.settings.SettingsRoute
import com.familyriskreview.feature.summary.SummaryRoute
import java.util.UUID

@Composable
fun FrrNavHost(
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = FrrRoutes.SPLASH,
        modifier = modifier,
    ) {
        composable(FrrRoutes.SPLASH) {
            SplashRoute(
                onFinished = {
                    navController.navigate(FrrRoutes.DASHBOARD) {
                        popUpTo(FrrRoutes.SPLASH) { inclusive = true }
                    }
                },
            )
        }

        composable(FrrRoutes.DASHBOARD) {
            DashboardRoute(
                onStartNewReview = {
                    // Phase 0: navigate to welcome shell; persistence arrives in Phase 3/6.
                    navController.navigate("welcome")
                },
                onOpenSettings = {
                    navController.navigate(FrrRoutes.SETTINGS)
                },
            )
        }

        composable("welcome") {
            WelcomeRoute(
                onBeginQuickReview = {
                    val id = UUID.randomUUID().toString()
                    navController.navigate(FrrRoutes.review(id))
                },
                onBeginGuidedReview = {
                    val id = UUID.randomUUID().toString()
                    navController.navigate(FrrRoutes.review(id))
                },
                onBackToDashboard = {
                    navController.popBackStack(FrrRoutes.DASHBOARD, inclusive = false)
                },
            )
        }

        composable(
            route = FrrRoutes.REVIEW,
            arguments = listOf(navArgument("reviewId") { type = NavType.StringType }),
        ) { entry ->
            val reviewId = entry.arguments?.getString("reviewId").orEmpty()
            ReviewPlaceholderRoute(
                reviewId = reviewId,
                onContinue = {
                    navController.navigate(FrrRoutes.summary(reviewId))
                },
            )
        }

        composable(
            route = FrrRoutes.SUMMARY,
            arguments = listOf(navArgument("reviewId") { type = NavType.StringType }),
        ) { entry ->
            val reviewId = entry.arguments?.getString("reviewId").orEmpty()
            SummaryRoute(
                reviewId = reviewId,
                onFinishSession = {
                    navController.navigate(FrrRoutes.DASHBOARD) {
                        popUpTo(FrrRoutes.DASHBOARD) { inclusive = true }
                    }
                },
                onBackToDashboard = {
                    navController.popBackStack(FrrRoutes.DASHBOARD, inclusive = false)
                },
            )
        }

        composable(FrrRoutes.SETTINGS) {
            SettingsRoute(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
