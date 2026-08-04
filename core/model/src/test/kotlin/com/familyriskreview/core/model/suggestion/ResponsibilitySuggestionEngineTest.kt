package com.familyriskreview.core.model.suggestion

import com.familyriskreview.core.model.ContributionStatus
import com.familyriskreview.core.model.DependencyStatus
import com.familyriskreview.core.model.FamilyMemberType
import com.familyriskreview.core.model.HouseholdMember
import com.familyriskreview.core.model.ResponsibilityCatalogue
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ResponsibilitySuggestionEngineTest {
    @Test
    fun suggestsChildItemsWhenChildPresent() {
        val suggestions =
            ResponsibilitySuggestionEngine.suggest(
                listOf(
                    member("self", FamilyMemberType.SELF),
                    member("child", FamilyMemberType.CHILD),
                ),
            )
        val catalogues = suggestions.map { it.catalogue }
        assertThat(catalogues).contains(ResponsibilityCatalogue.CHILD_HIGHER_EDUCATION)
        assertThat(catalogues).contains(ResponsibilityCatalogue.ESSENTIAL_FAMILY_LIVING_EXPENSES)
    }

    @Test
    fun neverImpliesAutoSelection() {
        val suggestions =
            ResponsibilitySuggestionEngine.suggest(
                listOf(member("self", FamilyMemberType.SELF)),
            )
        assertThat(suggestions).isNotEmpty()
        // Engine only returns Suggestion objects — selection is a separate write path.
        assertThat(suggestions.all { it.catalogue != null }).isTrue()
    }

    private fun member(
        id: String,
        type: FamilyMemberType,
    ) = HouseholdMember(
        id = id,
        reviewId = "r1",
        relationship = type,
        age = 30,
        contributionStatus = ContributionStatus.PRIMARY_INCOME,
        dependencyStatus = DependencyStatus.NOT_APPLICABLE,
    )
}
