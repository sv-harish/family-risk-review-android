# QA status — Family Risk Review

## Phase 0.5 gates (local)

| Gate | Status |
|------|--------|
| `./gradlew spotlessCheck` | **Passed** |
| `./gradlew lintDebug` | **Passed** (0 errors; warnings present — see unresolved) |
| `./gradlew test` | **Passed** |
| `./gradlew assembleDebug` | **Passed** |
| `./gradlew :app:verifyPhase0` | **Passed** |

## Test inventory

| Kind | Count | Notes |
|------|------:|-------|
| JVM unit tests | 36 | model, calculation, mappers, sync, nav, summary mapping |
| Robolectric unit tests | 4 | DAO, repository, DataStore (`@Config(sdk=[34])`) |
| Instrumentation tests executed | 0 | Not run in this environment |
| Instrumentation tests compiled | present stubs only | — |

**Unique tests executed:** 40 (0 failures)

### Representative test names

- `ResponsibilityCalculatorTest` (education/marriage/loan/recurring/derived/scenario/overflow…)
- `IndianCurrencyFormatterTest`
- `ReviewNumberGeneratorTest`
- `EntityMapperRoundTripTest`
- `ReviewDaoRobolectricTest`
- `ReviewRepositoryRobolectricTest`
- `UserPreferencesDataSourceTest`
- `CustomerSummaryMapperTest`
- `FakeSyncClientTest`
- `NavigationRoutesTest`

## Unresolved warnings / gaps

- Android Lint reports informational/warning findings (including dependency target-api notes); no lint errors.
- kotlinx-datetime `Instant` deprecation warnings toward `kotlin.time.Instant` (migrate in a later hardening pass).
- Fonts / Dareus One logo / advisor portrait still placeholders.
- Emulator IME / device recreation QA not run.
- GitHub Actions CI status must be confirmed green on the PR before merge claim.

## Honesty rule

Do not claim device testing without running it. Do not claim PR verification passed until GitHub Actions completes successfully.

## CI status (Phase 0.5)

GitHub Actions **passed** on branch `cursor/phase-0-bootstrap-1a7a`:

- push run: success — https://github.com/sv-harish/family-risk-review-android/actions/runs/30860418524
- pull_request run: success — https://github.com/sv-harish/family-risk-review-android/actions/runs/30860421324

**PR #1 is ready to merge** from a Phase 0.5 foundation perspective.
