# Architecture — Family Risk Review

## Stack (Phase 0.5)

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

**Rule:** feature modules must not depend on Room DAOs/entities, DataStore implementation types, Supabase clients, or WorkManager internals. They use repository contracts from `:core:data`.

The `app` module additionally depends on `:core:database`, `:core:datastore`, and `:core:sync` so Hilt `@Module` classes are on the application classpath.

## Layering

- **UI** — Compose screens; immutable UI state; temporary edit state separate from persisted domain state.
- **Domain / calculation** — pure Kotlin in `:core:model` and `:core:calculation`.
- **Data** — repositories in `:core:data` coordinate Room, DataStore, and SyncClient.

## Activity recreation

Do **not** declare broad `android:configChanges` on `MainActivity`.

> UI and editing state must survive expected Activity recreation through saved state and persisted domain state, not by preventing configuration changes.

## Review creation sequence

```
Select Quick/Guided on Welcome
→ ReviewRepository.createReview(mode, language)
→ persist Review (step = HOUSEHOLD_SUPPORT_MAP)
→ navigate to Review(reviewId)
```

Splash and Welcome are app-shell destinations only — never persisted as `ReviewStep`.

## Local-first & schema policy

Room is the local source of truth. Schema v1 is **provisional** until the schema-freeze milestone (first intentionally distributed persistence-compatible build). See `docs/DECISIONS.md`.

## Sync

`SyncClient` with `FakeSyncClient` default and `SupabaseSyncClient` stub (`FRR_SUPABASE_URL` / `FRR_SUPABASE_ANON_KEY`).

## Android 16 / API 36 notes relevant to this app

- Edge-to-edge / predictive back behaviours continue to evolve; the app already uses edge-to-edge and should validate back handling on API 36 tablets during Phase 8.
- Orientation/locale recreation is intentionally allowed; input fields (Phase 2) must use saved state.
- Large-screen / multi-window resizing remains a first-class concern for 10–13" tablets.
- Prefer compileSdk/targetSdk 36 together so runtime behaviour matches the tested platform level.
