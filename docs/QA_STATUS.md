# QA status — Family Risk Review

## Phase 1.1 gates (local)

| Gate | Status |
|------|--------|
| `./gradlew spotlessCheck` | **Passed** |
| `./gradlew lintDebug` | **Passed** (0 errors; warnings present — see unresolved) |
| `./gradlew test` | **Passed** |
| `./gradlew assembleDebug` | **Passed** |
| `./gradlew :app:verifyPhase1` | **Passed** |

## Phase 1.1 correctness hardening

Repaired before PR #2 merge:

- CAS conflict rolls back child writes / snapshots (Room abort exception)
- Sync enqueue only after commit
- Progression gates validate before advancing
- Explicit `ReviewModePolicy` Quick vs Guided differences
- Open-ended timing requires modelling horizon or numeric exclusion
- Catalogue-specific amount rules
- Assumption corruption fails closed (`CorruptData`)
- Lifecycle editability + explicit reopen
- Focused-contributor integrity (`FocusUpdate`)
- Review-number collision classification
- Explicit `calculationVersion` + `calculationInputRevision` staleness

## Test inventory

| Kind | Notes |
|------|-------|
| JVM unit tests | lifecycle, progression gates, validation, calculation, assumptions parse |
| Robolectric | CAS rollback asserts persisted DB state + sync intents; lifecycle; focus |
| Instrumentation executed | 0 |

### Representative regression tests

- `Phase11HardeningRobolectricTest` (rollback, sync-after-commit, editability, focus, staleness)
- `ProgressionGatesTest`
- `ResponsibilityRulesTest` (timing/amount/Quick vs Guided)
- `ResponsibilityCalculatorTest` (no silent zero; exclusion omitted from totals)
- `ReviewLifecycleTest` (reopen + mode policies)

## Unresolved warnings / gaps

- Android Lint informational/warning findings; no lint errors.
- kotlinx-datetime `Instant` deprecation warnings.
- Fonts / Dareus One logo / advisor portrait still placeholders.
- Emulator IME / device recreation QA not run.
- Polished customer UI intentionally deferred (Phase 2+).

## Honesty rule

Do not claim device testing without running it. Do not claim PR verification passed until GitHub Actions completes successfully on the final PR head.

## CI status

Phase 1.1 GitHub Actions **passed** on branch `cursor/phase-1-domain-engine-1a7a`:

- push run: success — https://github.com/sv-harish/family-risk-review-android/actions/runs/30864687192
- pull_request run: success — https://github.com/sv-harish/family-risk-review-android/actions/runs/30864688987

**PR #2 remains a draft** until Phase 1.1 corrections are reviewed.
