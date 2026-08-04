# QA status — Family Risk Review

## Phase 1.2 gates (local)

| Gate | Status |
|------|--------|
| `./gradlew spotlessCheck` | Pending local run this revision |
| `./gradlew lintDebug` | Pending local run this revision |
| `./gradlew test` | Pending local run this revision |
| `./gradlew assembleDebug` | Pending local run this revision |
| `./gradlew :app:verifyPhase1` | Pending local run this revision |

## Phase 1.2 final domain corrections

On PR #2 (draft) before merge:

1. Quick lightweight items → explicit `QuantificationStatus.NOT_YET_QUANTIFIED`
2. Null indicative in summary lines; excluded from totals; no silent ₹0; no `indicativeAmount` crash
3. Catalogue-specific timing via `CatalogueTimingRules` + `ResponsibilityAmountModel` for OTHER
4. `ScenarioRules` unique kinds + deterministic Lower → Base → Higher order
5. Quick rejects non-Base / duplicate Base scenarios
6. One canonical Base in use-case result (no duplicate Base row)
7. `DomainError.Calculation` via `CalculationFailureMapper`
8. `DomainLimits` enforced in domain validation
9. `ReviewRepository` no longer exposes `updateReviewCas` / `advanceStep` (`ReviewInternalWriter` internal)
10. Regression tests for each correction
11. No Phase 2 UI

## Test inventory

| Kind | Notes |
|------|-------|
| JVM unit tests | lifecycle, progression, ScenarioRules, CatalogueTiming, Quantification, DomainLimits, CalculationFailureMapper |
| Robolectric | CAS rollback, sync-after-commit, lifecycle, focus, repository API |
| Use-case | `CalculateReviewSummaryFailureTest` (typed overflow failure) |
| Instrumentation executed | 0 |

### Representative Phase 1.2 tests

- `Phase12QuantificationTest`
- `ScenarioRulesTest`
- `CatalogueTimingRulesTest`
- `CalculationFailureMapperTest` / `CalculateReviewSummaryFailureTest`
- `ResponsibilityRulesTest` (Quick non-quantified + Guided full details + limits)
- `ResponsibilityCalculatorTest` (optional indicative null; indicative throws for non-quantified)

## Unresolved warnings / gaps

- Android Lint informational/warning findings; no lint errors expected.
- kotlinx-datetime `Instant` deprecation warnings.
- Fonts / Dareus One logo / advisor portrait still placeholders.
- Emulator IME / device recreation QA not run.
- Polished customer UI intentionally deferred (Phase 2+).

## Honesty rule

Do not claim device testing without running it. Do not claim PR verification passed until GitHub Actions completes successfully on the final PR head.

## CI status

Phase 1.1 was green. Phase 1.2 CI status will be recorded after push of this revision.

**PR #2 remains a draft** until Phase 1.2 final review is complete.
