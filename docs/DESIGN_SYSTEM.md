# Design system — Family Risk Review

## Continuity Line

Visual motif that evolves through the journey: household connections → contribution/dependency → responsibilities → timeline → income uncertainty fade → protection reinforcement → Build vs Protect comparison.

## Colour tokens

| Token | Hex |
|-------|-----|
| Midnight Blue | `#122033` |
| Slate Navy | `#1B2D42` |
| Teal Blue | `#2D9CDB` |
| Soft Aqua | `#56C6D6` |
| Muted Gold | `#D6A85F` |
| Soft Sand | `#F5EBDD` |
| Cloud White | `#F8FAFC` |
| Warm Mist | `#EEF2F6` |
| Charcoal Text | `#243244` |

Predominantly light. Avoid Material purple, neon fintech gradients, fear-red screens, black-everywhere UI.

## Typography

| Role | Intended family |
|------|-----------------|
| English headings | Manrope |
| English body / UI | Inter |
| Tamil | Noto Sans Tamil |
| Hindi | Noto Sans Devanagari |

Phase 0 uses system sans placeholders until font files are added under `core/designsystem/src/main/res/font/` and `assets/fonts/`.

## Layout

- Early journey: two-pane (visual left, interaction right) ≥ 840dp width
- Timeline / summary: wider content, constrained line length
- Realization: reduced clutter
- Educational comparison: genuine split with synchronised motion
- Touch targets ≥ 48dp; primary actions 52–56dp

## Motion

Explain changes; respect Android reduced-motion preference and in-app toggle. No confetti, bouncing CTAs, or motion that delays data entry.

## Major visual moments (later phases)

1. Household Support Map  
2. Responsibility Timeline  
3. Build Over Time vs Protect From Today  

## Branding constraints

Dareus One only on splash. Advisor portrait only on welcome, final handoff, optional advisor panel — not every screen.
