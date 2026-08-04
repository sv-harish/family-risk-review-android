package com.familyriskreview.android

import com.familyriskreview.core.ui.navigation.FrrRoutes
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NavigationRoutesTest {
    @Test
    fun typeSafeDestinations_constructWithIds() {
        val review = FrrRoutes.Review("abc")
        val summary = FrrRoutes.Summary("abc")
        assertThat(review.reviewId).isEqualTo("abc")
        assertThat(summary.reviewId).isEqualTo("abc")
        assertThat(FrrRoutes.Splash).isEqualTo(FrrRoutes.Splash)
        assertThat(FrrRoutes.Dashboard).isEqualTo(FrrRoutes.Dashboard)
        assertThat(FrrRoutes.Welcome(mode = "QUICK").mode).isEqualTo("QUICK")
        assertThat(FrrRoutes.Welcome().mode).isNull()
    }

    @Test
    fun welcomeCarriesPreferredMode_beforeReviewCreation() {
        assertThat(FrrRoutes.Welcome("GUIDED").mode).isEqualTo("GUIDED")
        assertThat(FrrRoutes.Review("q").reviewId).isNotEqualTo("")
    }
}
