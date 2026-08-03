package com.familyriskreview.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AdvisorIdentityTest {

    @Test
    fun suppliedCredentialWordingIsPreserved() {
        // Documented production TODO: verify final formal credential wording
        // before public release. Do not silently change supplied wording.
        assertThat(AdvisorIdentity.DISPLAY_NAME).isEqualTo("S V Harish")
        assertThat(AdvisorIdentity.TITLE).isEqualTo("Certified Insurance Planner")
        assertThat(AdvisorIdentity.CREDENTIAL).isEqualTo("LUGI CIP Completed")
    }
}
