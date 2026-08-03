# Family Risk Review (Android)

Native Android tablet application for advisor-led household risk awareness sessions.

**Product:** Family Risk Review  
**Builder (splash only):** Dareus One  
**Advisor:** S V Harish — Certified Insurance Planner — LUGI CIP Completed  
> TODO(production): Verify final formal credential wording before public release.

## Status

**Phase 0 — Bootstrap** (current)

Greenfield repository foundation: modular architecture, design-system tokens, Room/DataStore shells, navigation shell, calculation stubs, CI, and documentation.

Customer journey screens and polished visual storytelling begin in later phases. Do not treat Phase 0 shells as the finished product.

## Requirements

- Android Studio Ladybug+ / JDK 17+
- Android SDK Platform 35
- Gradle Wrapper (included)

## Modules

| Module | Responsibility |
|--------|----------------|
| `:app` | Application entry, splash, root navigation |
| `:core:model` | Domain models (pure JVM) |
| `:core:calculation` | Future-value / recurring / gross responsibility math |
| `:core:database` | Room source of truth |
| `:core:datastore` | Preferences (language defaults, assumptions, motion) |
| `:core:designsystem` | Colour tokens, typography, shared components |
| `:core:ui` | Adaptive layouts, route constants |
| `:core:sync` | SyncClient interface + fake / Supabase stubs |
| `:feature:dashboard` | Advisor workspace |
| `:feature:review` | Customer review flow |
| `:feature:summary` | Awareness summary + handoff |
| `:feature:settings` | Assumptions, motion, sync settings |

## Build

```bash
./gradlew assembleDebug
./gradlew test
./gradlew :app:verifyPhase0
```

Set SDK path in `local.properties` (not committed):

```
sdk.dir=/path/to/Android/Sdk
```

## Languages

English, Tamil (`ta`), Hindi (`hi`). All customer-facing text must come from string resources.

## Secrets

Never commit Supabase keys, keystores, or `.env` files. See `docs/ARCHITECTURE.md` and `.env.example`.

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
