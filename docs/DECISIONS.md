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

**Status:** Accepted  
**Decision:** Schema v1 is pre-release and may be corrected directly in Phase 1.  
**Schema-freeze milestone:** first intentionally distributed persistence-compatible build (to be declared when shipping). Migrations become mandatory thereafter; no production destructive migration after freeze.
