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

The VNext v4/v5/v7/v8 CSS layers and the remaining shared/admin bridge styles keep mature Thymeleaf surfaces visually aligned with VNext. They are temporary, but still production dependencies until a replacement is verified without them.

### C. Retirement candidates

A legacy CSS/JS/template becomes a retirement candidate only when:

- no controller returns its template, or the route is fully served by VNext;
- no template/shared fragment references the asset;
- no runtime script injects or depends on it;
- Chromium + WebKit regression tests cover the replacement;
- production smoke checks cover the replacement route/assets.

## Current retirement progress

### Account submit page-owned stylesheet

`vnext-account-submit-v3.css` was a page-local compatibility asset despite its global VNext-style name. The retirement change moves the exact visual rules into the page-owned canonical asset `account-submit.css`; `account-submit.html` and its mobile equivalent continue to receive the same rules through the existing declarative fragment, while runtime JavaScript remains uninvolved.

The old `vnext-account-submit-v3.css` path must stay retired only if the replacement branch passes:

- Maven contract tests;
- Chromium wide-screen account-submit regression;
- WebKit mobile account-submit regression;
- the shared production smoke contract with `/assets/account-submit.css` present.

This migration does **not** prove that `vnext-legacy-bridge-v3.css` can be removed. That shared bridge still serves service detail, record/order detail, account-center and admin surfaces and must be retired separately.

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
