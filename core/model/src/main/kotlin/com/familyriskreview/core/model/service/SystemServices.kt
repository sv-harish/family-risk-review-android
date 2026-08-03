package com.familyriskreview.core.model.service

import kotlinx.datetime.Instant

/** Injectable clock for deterministic domain/use-case tests. */
interface Clock {
    fun now(): Instant
}

interface IdGenerator {
    fun newId(): String
}

interface ReviewNumberProvider {
    fun generate(now: Instant): String
}
