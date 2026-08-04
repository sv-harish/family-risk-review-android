# QA status — Family Risk Review

## Phase status

**Phase 1 complete — Phase 2.1 reliability pass in progress on PR #3**

Branch: `cursor/phase-2-product-experience-1a7a`

## Phase 2.1 reliability gates (local)

| Gate | Status |
|------|--------|
| `./gradlew spotlessCheck` | **Passed** |
| `./gradlew lintDebug` | **Passed** |
| `./gradlew test` | **Passed** |
| `./gradlew assembleDebug` | **Passed** |
| `./gradlew :app:verifyPhase1` | **Passed** |
| Managed-device UI test (`frrTabletApi30DebugAndroidTest`) | **Wired in CI** (KVM + aosp-atd API 30) |

## Phase 2.1 corrections delivered

1. Serial mutation coordinator (`Mutex`) — no `mutateJob?.cancel()` on new actions
2. Editable drafts in `SavedStateHandle` (`ReviewEditableDraftState`)
3. Robolectric Compose caret tests + instrumented `StableTextFieldInstrumentedTest` on managed device
4. Quantification UI from `ResponsibilityRules.mayRemainNonQuantified` for all catalogues
5. Welcome reachable: Splash → Dashboard → Welcome(mode) → create on confirm; no Phase 2 → Summary navigation
6. Conflict recovery: Reload latest + Return to dashboard
7. Localised validation / suggestion / a11y (en/ta/hi) via `DomainMessageMapper`
8. Other responsibility identity by ID + Add another custom
9. `isCreating` guards rapid double-create on Welcome confirmation
10. `ReviewSaveState` surfaced in fixed-height save indicator
11. Screenshots under `docs/screenshots/phase-2/`

## Honesty rule

Do not claim device testing without running it. Do not claim PR verification passed until GitHub Actions completes successfully on the final exact PR head.

## CI status

Awaiting green Actions on the Phase 2.1 head. **PR #3 remains a draft** until this reliability pass is reviewed.

## Out of scope (Phase 3+)

Summary polish, PDF, printing, and sharing are not started.
