# Legacy Frontend Retirement Plan

This document defines the retirement boundary for the remaining Thymeleaf, CSS and JavaScript surfaces after the VNext production migration.

## Principles

1. **Do not delete by age or filename.** An asset is removable only after every production route that consumes it has an equivalent VNext/shared replacement and a regression gate.
2. **One owner per responsibility.** Markup loads styles, behavior scripts own behavior, and application/runtime code must not dynamically inject canonical design layers.
3. **Stable public contracts first.** URL, form IDs, account/auth behavior, API payloads and production smoke expectations remain stable while presentation assets are retired.
4. **Retire from leaves inward.** Remove page-local overrides before shared compatibility layers; remove compatibility layers only after the last legacy page is migrated.

## Canonical public visual stack

For mature Thymeleaf public pages, `templates/fragments/site.html` is the only owner of canonical public VNext style loading. The order is intentional:

1. `site-experience-v2.css` — legacy structural/base compatibility
2. `vnext-public-finish-v4.css` — formal public-page bridge
3. `vnext-final-qa-fix-v5.css` — final wide-screen/QA corrections
4. `vnext-mobile-focus-v7.css` — mobile information architecture
5. `vnext-mobile-polish-v8.css` — mobile/WebKit refinement

`site-shell.js` is behavior-only. It may enhance announcement, support entry and brand favicon behavior, but it must not inject the canonical CSS stack.

The React/Vite build owns its equivalent style order through `frontend-vnext/src/formal-entry.css`.

## Asset classes

### A. Still active business/runtime assets

Examples include account/profile controllers, frontend control runtime, public field sanitization, upload/record/account scripts and admin control scripts. These are not deletion candidates until the corresponding business workflow moves to a feature-owned implementation.

### B. Visual compatibility layers

The VNext v4/v5/v7/v8 CSS layers and remaining account/admin bridge styles keep mature Thymeleaf surfaces visually aligned with VNext. They are temporary, but still production dependencies.

### C. Retirement candidates

A legacy CSS/JS/template becomes a retirement candidate only when:

- no controller returns its template, or the route is fully served by VNext;
- no template/shared fragment references the asset;
- no runtime script injects or depends on it;
- Chromium + WebKit regression tests cover the replacement;
- production smoke checks cover the replacement route/assets.

## Planned sequence

1. Centralize shared asset loading and remove runtime style injection. **Started in #129.**
2. Inventory account/admin/page-local CSS and JS consumers.
3. Migrate page-local visual overrides into shared feature/design-system ownership.
4. Migrate business JS into feature-owned modules where equivalent VNext flows exist.
5. Delete unreferenced assets in small batches with route-specific regression tests.
6. Remove v4/v5 compatibility layers only after no mature Thymeleaf public page requires them.
7. Remove the remaining legacy templates only after controller routing and production smoke prove the VNext replacement.

## Deletion gate

Every deletion PR must include:

- exact routes affected;
- old asset/template references removed;
- replacement owner;
- Maven contract coverage;
- Chromium and WebKit coverage;
- production smoke impact;
- rollback path.
