# Architecture — Family Risk Review

## Stack

| Concern | Choice | Version (Phase 0) |
|---------|--------|-------------------|
| Language | Kotlin | 2.0.21 |
| UI | Jetpack Compose + Material 3 | Compose BOM 2024.12.01 |
| Min / compile / target SDK | 26 / 35 / 35 | — |
| AGP | 8.7.3 | — |
| Gradle | 8.11.1 | — |
| Navigation | Navigation Compose | 2.8.5 |
| DI | Hilt | 2.53.1 |
| Local DB | Room | 2.6.1 |
| Preferences | DataStore | 1.1.1 |
| Async work | WorkManager (queued sync) | 2.10.0 |
| Serialization | Kotlinx Serialization | 1.7.3 |
| Time | Kotlinx Datetime | 0.6.1 |

## Module graph

```
app
 ├─ feature:dashboard
 ├─ feature:review
 ├─ feature:summary
 ├─ feature:settings
 ├─ core:ui
 ├─ core:designsystem
 ├─ core:database
 ├─ core:datastore
 ├─ core:calculation
 ├─ core:sync
 └─ core:model

feature:* → core:ui, core:designsystem, core:model (+ domain deps as needed)
core:database → core:model
core:datastore → core:model
core:calculation → core:model
core:sync → core:model, core:database
core:ui → core:designsystem, core:model
```

## Layering

- **UI** — Compose screens, immutable UI state, explicit events; temporary text-edit state kept separate from persisted domain state.
- **Domain / calculation** — pure Kotlin in `:core:model` and `:core:calculation`. No Room entities in UI; no calculations in composables.
- **Data** — Room is source of truth; DataStore for app preferences; SyncClient behind an interface.

## Local-first

Room stores reviews, members, responsibilities, assumptions metadata, advisor references, notes, status, calculation version, sync state. Soft delete for audited deletion. Safe migrations from schema v1 — no production destructive migration.

## Sync

`SyncClient` interface with:

- `FakeSyncClient` — default when credentials absent
- `SupabaseSyncClient` — stub; configure via `FRR_SUPABASE_URL` / `FRR_SUPABASE_ANON_KEY` env vars

Requirements for later phases: WorkManager queue, idempotent ops, revision / updated-at conflict protection, no silent overwrite of newer server records.

## Navigation shell (Phase 0)

`splash → dashboard → welcome → review/{id} → summary/{id}` plus `settings`.

## Secrets

Do not commit secrets. Use environment variables or a local `secrets.properties` excluded by `.gitignore`. See `.env.example`.
