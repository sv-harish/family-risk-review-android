package com.familyriskreview.core.model.result

/**
 * Structured outcome for domain/use-case operations.
 * Prefer this over generic exceptions for user-correctable problems.
 */
sealed class DomainResult<out T> {
    data class Success<T>(val value: T) : DomainResult<T>()
    data class Failure(val error: DomainError) : DomainResult<Nothing>()

    inline fun <R> map(transform: (T) -> R): DomainResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }

    fun getOrNull(): T? = (this as? Success)?.value

    companion object {
        fun <T> success(value: T): DomainResult<T> = Success(value)

        fun failure(error: DomainError): DomainResult<Nothing> = Failure(error)
    }
}

sealed class DomainError {
    data class Validation(val issues: List<ValidationIssue>) : DomainError()

    data class Transition(val message: String) : DomainError()

    data class Conflict(val message: String, val currentRevision: Long? = null) : DomainError()

    data class NotFound(val message: String) : DomainError()

    data class CollisionExhausted(val message: String) : DomainError()

    data class IllegalState(val message: String) : DomainError()

    /** Stored aggregate data is unreadable or unsupported; fail closed. */
    data class CorruptData(val code: String, val message: String) : DomainError()

    /** Non-retryable persistence / constraint failure. */
    data class Persistence(val message: String) : DomainError()
}

data class ValidationIssue(
    val code: String,
    val message: String,
    val severity: Severity = Severity.ERROR,
    val field: String? = null,
) {
    enum class Severity { ERROR, WARNING }
}

data class ValidationResult(
    val issues: List<ValidationIssue> = emptyList(),
) {
    val errors: List<ValidationIssue>
        get() = issues.filter { it.severity == ValidationIssue.Severity.ERROR }
    val warnings: List<ValidationIssue>
        get() = issues.filter { it.severity == ValidationIssue.Severity.WARNING }
    val isValid: Boolean get() = errors.isEmpty()

    operator fun plus(other: ValidationResult): ValidationResult = ValidationResult(issues + other.issues)

    companion object {
        val Ok: ValidationResult = ValidationResult()

        fun error(code: String, message: String, field: String? = null) = ValidationResult(
            listOf(ValidationIssue(code, message, ValidationIssue.Severity.ERROR, field)),
        )

        fun warning(code: String, message: String, field: String? = null) = ValidationResult(
            listOf(ValidationIssue(code, message, ValidationIssue.Severity.WARNING, field)),
        )
    }
}
