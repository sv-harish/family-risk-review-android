# Architecture decision log

## ADR-001 — Greenfield native Android (not Flutter / WebView)

**Status:** Accepted  
**Context:** Product must be a native Android tablet app for offline advisor sessions.  
**Decision:** Kotlin + Jetpack Compose multi-module app. No Flutter, React, PWA, or WebView shell.  
**Consequences:** Full control over offline storage, IME behaviour, PDF, and TalkBack.

## ADR-002 — Modular structure without excessive fragmentation

**Status:** Accepted  
**Context:** Need maintainability without speculative enterprise modules.  
**Decision:** `app` + `core:*` + `feature:*` as listed in ARCHITECTURE.md. Add modules only when justified.  
**Consequences:** Clear boundaries; DI wiring stays manageable for a single-advisor first release.

## ADR-003 — Room as local source of truth; sync behind interface

**Status:** Accepted  
**Context:** Sessions must work offline; remote sync may be enabled later (Supabase).  
**Decision:** Room + WorkManager queue + `SyncClient` with Fake default and Supabase stub.  
**Consequences:** Development without live credentials; production sync can be enabled via env config.

## ADR-004 — Separate editable / normalised / display numeric values

**Status:** Accepted  
**Context:** Prior project cursor-jumping defect from formatting on every keystroke.  
**Decision:** Format Indian currency on blur/confirm; keep raw edit state local while focused.  
**Consequences:** Input foundation (Phase 2) must be proven before widespread field reuse.

## ADR-005 — Light Continuity Line visual system

**Status:** Accepted  
**Context:** Distinct FRR identity; avoid generic fintech / purple Material looks.  
**Decision:** Token set (Midnight Blue … Charcoal Text); predominantly light theme.  
**Consequences:** Dark theme not product-default for customer flow.

## ADR-006 — Preserve supplied advisor credential wording

**Status:** Accepted  
**Context:** Credential string supplied as “LUGI CIP Completed”.  
**Decision:** Ship as-is with documented production TODO to verify before public release.  
**Consequences:** No silent wording changes in code or strings.

## ADR-007 — SDK and library versions (Phase 0)

**Status:** Accepted  
**Decision:** compile/target 35, min 26, Kotlin 2.0.21, AGP 8.7.3, Compose BOM 2024.12.01, stable Room/Hilt/Nav. Avoid alpha APIs unless documented.
