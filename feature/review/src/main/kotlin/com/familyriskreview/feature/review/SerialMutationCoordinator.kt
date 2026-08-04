package com.familyriskreview.feature.review

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Serialises review persistence mutations so rapid UI actions never cancel earlier writes.
 *
 * Each enqueued block runs to completion (or fails) before the next starts. Callers must
 * re-read the latest persisted revision inside [block] rather than trusting stale UI state.
 *
 * Cancelling the enclosing [kotlinx.coroutines.CoroutineScope] (e.g. ViewModel cleared)
 * still cancels in-flight work; ordinary rapid interaction must not cancel prior writes.
 */
class SerialMutationCoordinator {
    private val mutex = Mutex()

    suspend fun <T> enqueue(block: suspend () -> T): T = mutex.withLock { block() }

    val isLocked: Boolean
        get() = mutex.isLocked
}
