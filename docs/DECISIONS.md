# Architecture decision log

## ADR-001 — Greenfield native Android (not Flutter / WebView)

**Status:** Accepted  
**Decision:** Kotlin + Jetpack Compose multi-module app.

## ADR-002 — Modular structure without excessive fragmentation

**Status:** Accepted (amended Phase 0.5)  
**Decision:** Recommended modules plus `:core:data` for repository boundary.

## ADR-003 — Room as local source of truth; sync behind interface

**Status:** Accepted

## ADR-004 — Separate editable / normalised / display numeric values

**Status:** Accepted

## ADR-005 — Light Continuity Line visual system

**Status:** Accepted

## ADR-006 — Preserve supplied advisor credential wording

**Status:** Accepted

## ADR-007 — SDK and library versions (Phase 0)

**Status:** Superseded by ADR-008

## ADR-008 — Phase 0.5 toolchain upgrade (API 36)

**Status:** Accepted  
**Decision:** AGP 9.3.1, Gradle 9.5.1, Kotlin 2.4.10, Compose BOM 2026.06.00, compile/targetSdk 36, JDK 17 toolchain.  
**Notes:** Newer Lifecycle/Hilt-Navigation artifacts requiring compileSdk 37 were pinned to SDK-36-compatible stables. AGP 9 built-in Kotlin is used (no `kotlin-android` on Android modules).

## ADR-009 — Allow normal Activity configuration recreation

**Status:** Accepted  
**Decision:** Remove broad `android:configChanges` from `MainActivity`.  
**Rule:** UI/editing state survives via saved state + persisted domain state.

## ADR-010 — `core:data` repository boundary

**Status:** Accepted  
**Decision:** Features depend on repository contracts; Room/DataStore/Sync stay behind `:core:data`. App aggregates Hilt modules.

## ADR-011 — Calculation rates as basis points + BigDecimal

**Status:** Accepted  
**Decision:** `AnnualRateBps`, catalogue-specific defaults, beginning-of-year recurring model, derived-value metadata gating, explicit scenarios only.

## ADR-012 — Provisional Room schema v1

**Status:** Accepted (amended Phase 1)  
**Decision:** Schema v1 is pre-release and may be corrected directly in Phase 1. Phase 1 corrected review aggregate fields (`statusBeforeArchive`, `summaryStale`, `assumptionsJson`), removed customer-summary opt-in from advisor references, and added `calculation_snapshots`. Destructive fallback remains enabled only while `SCHEMA_STATUS == PROVISIONAL_PRE_RELEASE`.  
**Schema-freeze milestone:** first intentionally distributed persistence-compatible build (to be declared when shipping). Migrations become mandatory thereafter; no production destructive migration after freeze.

## ADR-013 — Injectable system services

**Status:** Accepted  
**Decision:** Repositories and use cases depend on `Clock`, `IdGenerator`, and `ReviewNumberProvider`. Production bindings use system time / UUID / SecureRandom; tests inject fakes. Repositories must not call `System.currentTimeMillis`, `UUID.randomUUID`, or `SecureRandom` directly.

## ADR-014 — Aggregate CAS revision + transactional child writes

**Status:** Accepted  
**Decision:** Review `revision` is the optimistic concurrency token. Child mutations run in a Room transaction that upserts/deletes the child and CAS-bumps parent revision, `updatedAt`, sync pending, and `summaryStale`.

## ADR-015 — Advisor data never in customer summary

**Status:** Accepted  
**Decision:** Remove `includeInCustomerSummary`. Advisor initials/CRM/notes are advisor-only; `CustomerSummaryMapper` never projects them.

## ADR-016 — Quick vs Guided scope

**Status:** Accepted  
**Decision:** Same ordered `ReviewStep` list for both modes. Scope differences live in `ReviewModePolicy` (max must-continue, detail requirements for adjustable/postponed, multi-scenario allowance). Repositories must not scatter `if (mode == QUICK)` checks.

## ADR-017 — Advisor references are local-only

**Status:** Accepted  
**Decision:** Advisor-only initials/CRM/notes persist in Room but do **not** bump review revision and do **not** enqueue `SyncClient` intents. Customer projections never include them. Remote sync of advisor notes is explicitly out of scope until a future ADR revisits the policy.

## ADR-018 — CAS conflict aborts Room transactions

**Status:** Accepted  
**Decision:** Child writes that fail parent CAS throw an internal `AggregateCasConflictException` inside `withTransaction` so Room rolls back. Sync enqueue happens only after the transaction commits. Validation/domain aborts use `AbortDomainException` (also rolls back).

## ADR-019 — Calculation input fingerprint

**Status:** Accepted  
**Decision:** `Review.calculationInputRevision` bumps only when calculation inputs change. Aggregate `revision` bumps on all writes (including archive/step). Snapshots bind to `calculationInputRevision`. Non-financial revisions (archive, step) do not stale a fresh summary.

## ADR-020 — Open-ended timing requires modelling horizon or explicit non-quantified state

**Status:** Accepted (supersedes exclusion-flag wording)  
**Decision:** `AS_LONG_AS_REQUIRED`, `UNTIL_MILESTONE`, and `CUSTOM` require a calculable horizon (`modellingDurationYears` / years / duration) **or** `QuantificationStatus.NOT_YET_QUANTIFIED`. Non-quantified items appear in summary lines with a null indicative amount and are omitted from numeric totals (never silent ₹0). Do not infer quantification from nullable amount fields.

## ADR-021 — Catalogue-specific timing rules

**Status:** Accepted  
**Decision:** Allowed timing kinds are catalogue-specific (`CatalogueTimingRules`). Living expenses reject one-time and loan timing. Support catalogues reject one-time/loan timing (justified `CUSTOM` recurring allowed). Education / marriage / house are one-time oriented. Loans require `CURRENT_OUTSTANDING`. Custom (`OTHER`) requires an explicit `ResponsibilityAmountModel` (`ONE_TIME` / `RECURRING`); timing is validated against that model, not by probing which money fields are set.

## ADR-022 — Deterministic scenario sets

**Status:** Accepted  
**Decision:** `ScenarioRules.validateAndOrder` enforces unique scenario kinds, Quick Base-only (or empty), Guided Lower/Base/Higher, and always returns Lower → Base → Higher order. Empty list means the standard Base result only — missing scenarios are never invented. `CalculateReviewSummaryUseCase` keeps one canonical Base in `Result.base`; Lower/Higher appear in `Result.scenarios` without duplicating Base. Every supplied scenario assumption set must use `version in CalculationAssumptions.SUPPORTED_VERSIONS` (never silently rewritten).

## ADR-023 — Typed calculation failures and domain limits

**Status:** Accepted  
**Decision:** Known calculator `IllegalArgumentException` / `IllegalStateException` / `ArithmeticException` are mapped at the use-case boundary via `CalculationFailureMapper` to `DomainError.Calculation` with stable codes. Programming defects are not caught indiscriminately. `DomainLimits` bounds one-time/monthly amounts and year horizons and are enforced in domain validation before calculation.

## ADR-024 — Public read versus internal mutation boundaries

**Status:** Accepted (Phase 1.3)  
**Decision:** Feature modules receive `ReviewReader` (reads only) and public use cases for mutations. Lifecycle mutation is `internal interface ReviewMutationWriter` in `:core:data` (create / advance / complete / archive / restore / reopen / delete). Hilt binds the writer with an `internal` `@Binds` method; mutation-using use cases use `@Inject internal constructor`. Features cannot compile against the writer or call `completeReview` / `advanceStep` on a public repository API. `CompleteReviewUseCase` is the only public completion path and enforces fresh-summary audit before writing.

## ADR-025 — Effective Base assumption provenance

**Status:** Accepted (Phase 1.3)  
**Decision:** `CalculateReviewSummaryUseCase` computes one authoritative `effectiveBaseAssumptions` (explicit Base scenario assumptions, else review assumptions) and uses that same set for canonical Base totals, per-responsibility Base amounts, snapshot `assumptionVersion`, and snapshot `assumptionsJson`. Never calculate with one set and persist another.

## ADR-026 — Mode-specific quantification policy

**Status:** Accepted (Phase 1.3)  
**Decision:** `ResponsibilityRules.mayRemainNonQuantified` — Quick allows `NOT_YET_QUANTIFIED` only for adjustable / postponed; must-continue must be quantified and calculable. Guided never allows non-quantified selected items (`RESP_GUIDED_REQUIRES_QUANTIFICATION`). `validateDraftDetails` permits incomplete mid-edit persistence; `validateDetails` / progression / calculation enforce the mode policy.

