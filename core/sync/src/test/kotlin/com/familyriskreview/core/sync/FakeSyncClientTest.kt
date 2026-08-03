package com.familyriskreview.core.sync

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FakeSyncClientTest {
    @Test
    fun enqueueAndPushClearsPending() = runTest {
        val client = FakeSyncClient()
        client.enqueueUpsert("review-1")
        client.enqueueDelete("review-2")
        assertThat(client.pendingCount()).isEqualTo(2)

        val result = client.pushPending()
        assertThat(result.state).isEqualTo(com.familyriskreview.core.model.SyncState.SYNCED)
        assertThat(client.pendingCount()).isEqualTo(0)
    }

    @Test
    fun supabaseWithoutCredentials_isNotConfigured() {
        val client = SupabaseSyncClient(null, null)
        assertThat(client.isConfigured()).isFalse()
    }
}
