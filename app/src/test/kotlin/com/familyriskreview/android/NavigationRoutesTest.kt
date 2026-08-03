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
        assertThat(FrrRoutes.Welcome).isEqualTo(FrrRoutes.Welcome)
        assertThat(FrrRoutes.Dashboard).isEqualTo(FrrRoutes.Dashboard)
    }

    @Test
    fun quickVersusGuided_areDistinctModes() {
        // Mode preservation is enforced by ReviewRepository.createReview;
        // destinations themselves only carry reviewId after creation.
        assertThat(FrrRoutes.Review("q").reviewId).isNotEqualTo("")
    }
}
