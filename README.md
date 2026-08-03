# Family Risk Review (Android)

Native Android tablet application for advisor-led household risk awareness sessions.

**Product:** Family Risk Review  
**Builder (splash only):** Dareus One  
**Advisor:** S V Harish — Certified Insurance Planner — LUGI CIP Completed  
> TODO(production): Verify final formal credential wording before public release.

## Status

**Phase 0.5 — Hardened bootstrap** (current)

Greenfield foundation with upgraded toolchain (API 36), `core:data` repository boundary, corrected calculation semantics, enforced CI gates, and provisional Room schema policy.

## Toolchain matrix

| Item | Version |
|------|---------|
| AGP | 9.3.1 |
| Gradle | 9.5.1 |
| Kotlin | 2.4.10 |
| KSP | 2.3.11 |
| Compose BOM | 2026.06.00 |
| compileSdk / targetSdk | 36 / 36 |
| minSdk | 26 |
| JDK (toolchain) | 17 |
| Hilt | 2.60.1 |
| Navigation Compose | 2.9.8 |
| Room | 2.8.4 |
| DataStore | 1.2.1 |
| WorkManager | 2.11.2 |
| Lifecycle | 2.10.0 |
| Activity Compose | 1.12.4 |
| Coroutines | 1.10.2 |
| Serialization | 1.11.0 |
| Datetime | 0.7.1 |

Notes:

- AGP 9 uses **built-in Kotlin** for Android modules (`org.jetbrains.kotlin.android` is not applied).
- Some AndroidX artifacts newer than the matrix require `compileSdk 37+`; they were pinned to keep **compileSdk/targetSdk 36** as specified.
- Robolectric instrumented-style unit tests use `@Config(sdk = [34])` because Robolectric’s API 36 sandbox requires JDK 21.

## Modules

| Module | Responsibility |
|--------|----------------|
| `:app` | Application entry, splash, root navigation, DI aggregation |
| `:core:model` | Pure domain models |
| `:core:calculation` | Deterministic financial math |
| `:core:database` | Room entities/DAOs only |
| `:core:datastore` | Preference persistence only |
| `:core:sync` | SyncClient + Fake / Supabase stub |
| `:core:data` | Repositories coordinating storage + sync |
| `:core:designsystem` | Tokens, theme, shared components |
| `:core:ui` | Adaptive layouts, type-safe routes |
| `:feature:*` | UI features depending on repositories/domain — not DAOs |

## Build

```bash
./gradlew spotlessCheck
./gradlew lintDebug
./gradlew test
./gradlew assembleDebug
./gradlew :app:verifyPhase0
```

## Documentation

- [Product spec](docs/PRODUCT_SPEC.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Calculation method](docs/CALCULATION_METHOD.md)
- [Design system](docs/DESIGN_SYSTEM.md)
- [Accessibility](docs/ACCESSIBILITY.md)
- [QA status](docs/QA_STATUS.md)
- [Decisions](docs/DECISIONS.md)

## License

See [LICENSE](LICENSE).
