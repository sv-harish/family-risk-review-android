package com.familyriskreview.android

import com.familyriskreview.core.ui.navigation.FrrRoutes
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NavigationRoutesTest {

    @Test
    fun reviewAndSummaryRoutesIncludeId() {
        assertThat(FrrRoutes.review("abc")).isEqualTo("review/abc")
        assertThat(FrrRoutes.summary("abc")).isEqualTo("summary/abc")
        assertThat(FrrRoutes.SPLASH).isEqualTo("splash")
        assertThat(FrrRoutes.DASHBOARD).isEqualTo("dashboard")
    }
}
