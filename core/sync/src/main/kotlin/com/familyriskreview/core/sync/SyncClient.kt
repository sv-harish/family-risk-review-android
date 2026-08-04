package com.familyriskreview.core.sync

import com.familyriskreview.core.model.SyncState

/**
 * Sync boundary for local-first writes with queued remote synchronisation.
 *
 * Phase 0 provides:
 * - [FakeSyncClient] for offline development and testing
 * - [SupabaseSyncClient] stub wired behind the same interface
 *
 * Production credentials must never be committed. See docs/ARCHITECTURE.md.
 */
interface SyncClient {
    suspend fun enqueueUpsert(reviewId: String)

    suspend fun enqueueDelete(reviewId: String)

    suspend fun pushPending(): SyncResult

    suspend fun pullRemote(): SyncResult

    fun isConfigured(): Boolean
}

data class SyncResult(
    val state: SyncState,
    val message: String? = null,
    val conflictReviewIds: List<String> = emptyList(),
)

class FakeSyncClient : SyncClient {
    private val pending = mutableSetOf<String>()
    private val deleted = mutableSetOf<String>()

    override suspend fun enqueueUpsert(reviewId: String) {
        deleted.remove(reviewId)
        pending.add(reviewId)
    }

    override suspend fun enqueueDelete(reviewId: String) {
        pending.remove(reviewId)
        deleted.add(reviewId)
    }

    override suspend fun pushPending(): SyncResult {
        pending.clear()
        deleted.clear()
        return SyncResult(SyncState.SYNCED, message = "Fake sync completed")
    }

    override suspend fun pullRemote(): SyncResult = SyncResult(SyncState.SYNCED, message = "Fake pull — no remote changes")

    override fun isConfigured(): Boolean = true

    fun pendingCount(): Int = pending.size + deleted.size

    fun pendingUpsertIds(): Set<String> = pending.toSet()

    fun pendingDeleteIds(): Set<String> = deleted.toSet()

    fun clearIntents() {
        pending.clear()
        deleted.clear()
    }
}

/**
 * Production Supabase implementation placeholder.
 * Enable via environment configuration; do not commit secrets.
 */
class SupabaseSyncClient(
    private val supabaseUrl: String?,
    private val supabaseAnonKey: String?,
) : SyncClient {
    override suspend fun enqueueUpsert(reviewId: String) = Unit

    override suspend fun enqueueDelete(reviewId: String) = Unit

    override suspend fun pushPending(): SyncResult = if (!isConfigured()) {
        SyncResult(SyncState.LOCAL_ONLY, message = "Supabase not configured")
    } else {
        SyncResult(SyncState.PENDING, message = "Supabase sync not yet implemented")
    }

    override suspend fun pullRemote(): SyncResult = if (!isConfigured()) {
        SyncResult(SyncState.LOCAL_ONLY, message = "Supabase not configured")
    } else {
        SyncResult(SyncState.PENDING, message = "Supabase sync not yet implemented")
    }

    override fun isConfigured(): Boolean = !supabaseUrl.isNullOrBlank() && !supabaseAnonKey.isNullOrBlank()
}
