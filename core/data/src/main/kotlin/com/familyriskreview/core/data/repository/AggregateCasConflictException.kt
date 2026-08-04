package com.familyriskreview.core.data.repository

/**
 * Thrown inside a Room transaction when parent CAS fails after a child write.
 * Room rolls back the transaction; callers catch and map to [DomainError.Conflict].
 * Never expose this type to feature modules.
 */
internal class AggregateCasConflictException(
    val reviewId: String,
    val expectedRevision: Long,
    val currentRevision: Long?,
) : RuntimeException(
    "CAS conflict for review $reviewId (expected revision $expectedRevision)",
)
