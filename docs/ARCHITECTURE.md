# Architecture — Family Risk Review

## Stack (Phase 1)

| Concern | Choice | Version |
|---------|--------|---------|
| Language | Kotlin (AGP built-in for Android modules) | 2.4.10 |
| UI | Jetpack Compose + Material 3 | Compose BOM 2026.06.00 |
| Min / compile / target SDK | 26 / 36 / 36 | — |
| AGP | 9.3.1 | — |
| Gradle | 9.5.1 | — |
| Navigation | Type-safe Navigation Compose | 2.9.8 |
| DI | Hilt | 2.60.1 |
| Local DB | Room (provisional schema v1) | 2.8.4 |
| Preferences | DataStore | 1.2.1 |
| Async work | WorkManager (queued sync) | 2.11.2 |

## Module graph

```
app
 ├─ feature:*  → core:data, core:ui, core:designsystem, core:model (+ calculation where needed)
 ├─ core:data  → core:database, core:datastore, core:sync, core:model, core:calculation
 ├─ core:database → core:model
 ├─ core:datastore → core:model
 ├─ core:sync → core:model
 ├─ core:calculation → core:model
 ├─ core:ui → core:designsystem, core:model
 └─ core:designsystem
```

**Rule:** feature modules must not depend on Room DAOs/entities, DataStore implementation types, Supabase clients, or WorkManager internals. They use repository contracts and use cases from `:core:data`.

The `app` module additionally depends on `:core:database`, `:core:datastore`, and `:core:sync` so Hilt `@Module` classes are on the application classpath.

## Layering

- **UI** — Compose screens; immutable UI state; temporary edit state separate from persisted domain state.
- **Domain** — pure Kotlin in `:core:model` (lifecycle, validation, suggestions, `DomainResult`).
- **Calculation** — pure Kotlin in `:core:calculation` (authoritative `CALCULATION_VERSION`).
- **Application** — use cases in `:core:data` enforce invariants and call aggregate repositories.
- **Data** — repositories coordinate Room, DataStore, and SyncClient with injectable Clock/Id/ReviewNumber.

## Review lifecycle

```
IN_PROGRESS → COMPLETED | ARCHIVED | DELETED
COMPLETED   → ARCHIVED | DELETED
ARCHIVED    → (restore) IN_PROGRESS | COMPLETED | DELETED
DELETED     → (terminal; soft tombstone retained)
```

- Archive stores `statusBeforeArchive` so restore returns to the prior meaningful status.
- Soft delete marks `DELETED` and retains child rows (no physical CASCADE while the review row remains).
- Incomplete reviews resume from persisted `currentStep` while `status == IN_PROGRESS`.

## Quick vs Guided

Both modes share the same ordered `ReviewStep` list starting at `HOUSEHOLD_SUPPORT_MAP`.

| Rule | Quick | Guided |
|------|-------|--------|
| Facilitation target | 5–8 minutes | 10–15 minutes |
| Must-continue limit | ≤ 3 selected | No hard cap in v1 |
| Detail depth | Lighter validation still required for calculation | Full details |

## Validation & suggestions

- Household: age 0–120, single Self, focus contributor cannot be NON_CONTRIBUTOR.
- Responsibilities: custom label + explicit inflation for OTHER; timing/amount for details; Quick must-continue cap.
- `ResponsibilitySuggestionEngine` suggests from household composition — **never auto-selects**.

## Aggregate revision & sync

Every mutating write:

1. bumps `revision` (CAS: `WHERE id=? AND revision=?`)
2. updates `updatedAt`
3. sets `syncState = PENDING` (for synced aggregates)

Calculation-input mutations additionally:

4. bump `calculationInputRevision`
5. set `summaryStale = true`

Conflict → throw inside the Room transaction (full rollback) → `DomainError.Conflict`.  
`SyncClient.enqueue*` runs **only after** a successful commit.

Non-financial writes (archive, step advance, reopen) bump `revision` but not `calculationInputRevision`, so a fresh summary stays current.

## Advisor references (local-only)

Advisor initials/CRM/notes are Room-persisted but intentionally outside sync/revision tracking (ADR-017).


## Calculation snapshots

`calculation_snapshots` stores totals, per-responsibility lines, assumption/calculation versions, scenario, and the review revision at snapshot time. Summary is stale when `summaryStale` is true or assumption/calculation versions diverge from the engine.

## Advisor-only references

Initials/nickname, CRM id, and private notes are advisor-only. `CustomerSummaryMapper` never projects them into customer summaries/PDFs.

## Activity recreation

Do **not** declare broad `android:configChanges` on `MainActivity`.

> UI and editing state must survive expected Activity recreation through saved state and persisted domain state, not by preventing configuration changes.

## Review creation sequence

```
Select Quick/Guided on Welcome
→ CreateReviewUseCase(mode, language)
→ persist Review (step = HOUSEHOLD_SUPPORT_MAP)
→ navigate to Review(reviewId)
```

Splash and Welcome are app-shell destinations only — never persisted as `ReviewStep`.

## Local-first & schema policy

Room is the local source of truth. Schema v1 remains **provisional** until the schema-freeze milestone (first intentionally distributed persistence-compatible build). During provisional status, destructive fallback is allowed for developer installs. See `docs/DECISIONS.md`.

## Sync

`SyncClient` with `FakeSyncClient` default and `SupabaseSyncClient` stub (`FRR_SUPABASE_URL` / `FRR_SUPABASE_ANON_KEY`).
