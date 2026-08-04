# QA status — Family Risk Review

## Phase status

**Phase 1 complete — Phase 2 product experience in progress**

Branch: `cursor/phase-2-product-experience-1a7a`

## Phase 2 gates (local)

| Gate | Status |
|------|--------|
| `./gradlew spotlessCheck` | **Passed** |
| `./gradlew lintDebug` | **Passed** |
| `./gradlew test` | **Passed** (~149 tests) |
| `./gradlew assembleDebug` | **Passed** |

## Screens implemented

- Splash (Dareus One credit only here; reduced-motion aware)
- Welcome / introduction
- Dashboard (Quick/Guided start, in-progress / completed / archived separation, resume)
- Review shell with visible journey stages
- Household support map + member editor
- Responsibility selection (suggestions never auto-select)
- Prioritisation (Quick must-continue limit surfaced)
- Responsibility detail forms (catalogue-sensitive; draft save)
- Phase 2 boundary screen for later domain steps

## Known limitations

- Font files still placeholders (Manrope / Source Sans / Noto pending under assets/fonts)
- Dareus One logo asset still placeholder text on splash
- Advisor portrait still placeholder
- Timeline / gross-responsibility / summary polish deferred to later phases
- Emulator portrait/landscape screenshot capture may be limited in CI agents
- Tamil/Hindi resources exist for implemented journey; professional translation review recommended

## Honesty rule

Do not claim device testing without running it. Do not claim PR verification passed until GitHub Actions completes successfully on the final exact PR head.

## CI status

Phase 2 GitHub Actions **passed** on branch `cursor/phase-2-product-experience-1a7a` (head `6abc0b8`):

- push run: https://github.com/sv-harish/family-risk-review-android/actions/runs/30874030964
- pull_request run: https://github.com/sv-harish/family-risk-review-android/actions/runs/30874033022

**PR #3 remains a draft** until Phase 2 review is complete.
