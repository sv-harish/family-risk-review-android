# QA status — Family Risk Review

## Phase 1 gates (local)

| Gate | Status |
|------|--------|
| `./gradlew spotlessCheck` | **Passed** |
| `./gradlew lintDebug` | **Passed** (0 errors; warnings present — see unresolved) |
| `./gradlew test` | **Passed** |
| `./gradlew assembleDebug` | **Passed** |
| `./gradlew :app:verifyPhase1` | **Passed** |

## Test inventory

| Kind | Count | Notes |
|------|------:|-------|
| JVM unit tests | ~47 | model lifecycle/validation/suggestions, calculation, mappers, sync, nav |
| Robolectric unit tests | ~9 | DAO CAS, repository lifecycle/transactions, DataStore (`@Config(sdk=[34])`) |
| Instrumentation tests executed | 0 | Not run in this environment |
| Instrumentation tests compiled | present stubs only | — |

**Unique tests executed:** ~56 (0 failures) after Phase 1 additions.

### Representative test names

- `ResponsibilityCalculatorTest` (education/marriage/loan/recurring/derived/scenario/overflow/checkedSum…)
- `IndianCurrencyFormatterTest`
- `ReviewNumberGeneratorTest`
- `ReviewLifecycleTest`
- `HouseholdRulesTest` / `ResponsibilityRulesTest`
- `ResponsibilitySuggestionEngineTest`
- `EntityMapperRoundTripTest`
- `ReviewDaoRobolectricTest` (CAS conflict / soft delete)
- `ReviewRepositoryRobolectricTest` (create, CAS, archive/restore, soft delete + children, revision bump)
- `UserPreferencesDataSourceTest`
- `CustomerSummaryMapperTest` (advisor fields never projected)
- `FakeSyncClientTest`
- `NavigationRoutesTest`

## Unresolved warnings / gaps

- Android Lint reports informational/warning findings (including dependency target-api notes); no lint errors.
- kotlinx-datetime `Instant` deprecation warnings toward `kotlin.time.Instant` (migrate in a later hardening pass).
- Fonts / Dareus One logo / advisor portrait still placeholders.
- Emulator IME / device recreation QA not run.
- Polished customer UI intentionally deferred (Phase 2+).
- GitHub Actions CI status must be confirmed green on the Phase 1 PR before merge claim.

## Honesty rule

Do not claim device testing without running it. Do not claim PR verification passed until GitHub Actions completes successfully.

## CI status

Phase 0.5 (PR #1) GitHub Actions passed historically. Phase 1 CI status will be recorded after the Phase 1 PR run completes.
