package com.familyriskreview.core.data.service

import com.familyriskreview.core.model.SecureReviewNumberProvider
import com.familyriskreview.core.model.service.Clock
import com.familyriskreview.core.model.service.IdGenerator
import com.familyriskreview.core.model.service.ReviewNumberProvider
import kotlinx.datetime.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemClock
@Inject
constructor() : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis())
}

@Singleton
class UuidIdGenerator
@Inject
constructor() : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}

@Singleton
class DefaultReviewNumberProvider
@Inject
constructor() : ReviewNumberProvider by SecureReviewNumberProvider()
