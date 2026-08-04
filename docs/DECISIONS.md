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

## ADR-020 — Open-ended timing requires modelling horizon or exclusion

**Status:** Accepted  
**Decision:** `AS_LONG_AS_REQUIRED`, `UNTIL_MILESTONE`, and `CUSTOM` require a calculable horizon (`modellingDurationYears` / years / duration) or `excludedFromNumericCalculation=true`. Excluded items are omitted from totals (never silent ₹0).

