# QA status — Family Risk Review

## Phase 0

| Area | Status | Notes |
|------|--------|-------|
| Project compiles | **Passed** | `./gradlew assembleDebug` — APK built |
| Unit: currency formatting | **Passed** | `IndianCurrencyFormatterTest` (3) |
| Unit: future value / recurring / gross | **Passed** | `ResponsibilityCalculatorTest` (4) |
| Unit: advisor identity wording | **Passed** | `AdvisorIdentityTest` (1) |
| Unit: fake sync | **Passed** | `FakeSyncClientTest` (2) |
| Unit: navigation routes | **Passed** | `NavigationRoutesTest` (1) |
| Unit: review entity smoke | **Passed** | `ReviewEntitySmokeTest` (1) |
| Database insert/update/migration | Not yet | Phase 1 / Phase 6 |
| Compose UI tests | Not yet | Phase 3+ |
| Emulator / device input tests | **Not run** | No emulator system image in this environment |
| PDF visual inspection EN/TA/HI | Not started | Phase 7 |
| Accessibility audit | Not started | Phase 8 |

## Test run (Phase 0 bootstrap)

```
./gradlew assembleDebug test
```

Result: **BUILD SUCCESSFUL** — 12 unique unit tests, 0 failures  
(Android library modules also execute debug+release unit-test variants.)

## Honesty rule

Do not claim device testing without running it. Do not claim a test passed merely because it compiled.

## Known Phase 0 gaps

- Font files (Manrope, Inter, Noto Sans Tamil/Devanagari) not yet bundled — system sans placeholders
- Dareus One logo and advisor portrait are labelled placeholders pending asset check-in (`assets/`)
- Room schema JSON exported at `core/database/schemas/.../1.json`
- Supabase sync is a stub behind `SyncClient`
- Spotless/ktlint may flag style issues; CI continues on Spotless failure for now
