package com.familyriskreview.feature.review

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SerialMutationCoordinatorTest {
    @Test
    fun rapidEnqueue_completesAllInOrder_withoutCancellingPriorWork() = runTest {
        val coordinator = SerialMutationCoordinator()
        val started = mutableListOf<Int>()
        val finished = mutableListOf<Int>()

        val jobs =
            (1..8).map { index ->
                async {
                    coordinator.enqueue {
                        started += index
                        delay(10)
                        finished += index
                        index
                    }
                }
            }

        val results = jobs.awaitAll()

        assertThat(results).containsExactly(1, 2, 3, 4, 5, 6, 7, 8).inOrder()
        assertThat(started).containsExactly(1, 2, 3, 4, 5, 6, 7, 8).inOrder()
        assertThat(finished).containsExactly(1, 2, 3, 4, 5, 6, 7, 8).inOrder()
        assertThat(coordinator.isLocked).isFalse()
    }

    @Test
    fun concurrentEnqueue_neverOverlapsExecution() = runTest {
        val coordinator = SerialMutationCoordinator()
        var inFlight = 0
        var maxInFlight = 0

        val jobs =
            List(12) {
                launch {
                    coordinator.enqueue {
                        inFlight += 1
                        maxInFlight = maxOf(maxInFlight, inFlight)
                        delay(5)
                        inFlight -= 1
                    }
                }
            }
        jobs.forEach { it.join() }

        assertThat(maxInFlight).isEqualTo(1)
        assertThat(inFlight).isEqualTo(0)
    }

    @Test
    fun enqueue_propagatesFailure_thenAllowsNextMutation() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val coordinator = SerialMutationCoordinator()

        val first =
            async(dispatcher) {
                runCatching {
                    coordinator.enqueue {
                        delay(1)
                        error("boom")
                    }
                }
            }
        advanceUntilIdle()
        assertThat(first.await().isFailure).isTrue()

        val second =
            async(dispatcher) {
                coordinator.enqueue { "ok" }
            }
        advanceUntilIdle()
        assertThat(second.await()).isEqualTo("ok")
        assertThat(coordinator.isLocked).isFalse()
    }
}
