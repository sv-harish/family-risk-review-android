# Design system — Family Risk Review

**Phase:** Phase 1 complete — Phase 2 product experience in progress

## Personality

Calm, clinical-meets-human. Warm off-white surfaces, deep ink text, restrained teal accent, amber only for attention. No Dareus One colours outside splash. No dark fintech default.

## Tokens

| Token group | Location |
|-------------|----------|
| Raw palette | `FrrColors` |
| Semantic roles | `FrrSemanticColors` / `frrColors()` |
| Spacing | `FrrSpacing` |
| Radius | `FrrRadius` |
| Elevation | `FrrElevation` |
| Icon size | `FrrIconSize` |
| Touch target | `FrrTouchTarget` (≥48dp) |
| Motion | `FrrMotion` |

Feature screens must use semantic colours via `frrColors()`, not raw palette literals where avoidable.

## Typography

Intended: Manrope (display), Source Sans 3 (body), Noto Sans Tamil / Devanagari. Font files pending under `assets/fonts/`. Scale is defined in `FrrTypography`.

## Reduced motion

Respect `UserPreferences.reducedMotion`. Splash and step transitions should skip or shorten non-essential animation.

## Components

- `FrrPrimaryButton` / `FrrSecondaryButton` / `FrrTextAction`
- `FrrStableTextField` — preserves selection; keep raw text separate from domain parsing
