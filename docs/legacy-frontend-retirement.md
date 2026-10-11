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

The VNext v4/v5/v7/v8 CSS layers and the remaining main-admin bridge styles keep mature Thymeleaf surfaces visually aligned with VNext. They are temporary, but still production dependencies until a replacement is verified without them.

### C. Retirement candidates

A legacy CSS/JS/template becomes a retirement candidate only when:

- no controller returns its template, or the route is fully served by VNext;
- no template/shared fragment references the asset;
- no runtime script injects or depends on it;
- Chromium + WebKit regression tests cover the replacement;
- production smoke checks cover the replacement route/assets.

## Current retirement progress

Retirement is now proceeding in small feature-owned batches. Each completed batch preserves the current production visual behavior while moving ownership out of globally named compatibility assets.

### Completed feature ownership moves

- **Account submit**: retired `vnext-account-submit-v3.css`; canonical owner is `/assets/account-submit.css`.
- **Account center**: account-center bridge rules moved into `/assets/account-center-refine.css`; account-center templates no longer load the shared deep-page bridge.
- **Service detail**: service-detail bridge rules moved into `/assets/service-detail.css`; desktop/mobile service-detail templates load the feature-owned asset directly.
- **Record / order detail**: record/order bridge rules moved into `/assets/record-detail.css`; desktop/mobile record and order detail templates load the feature-owned asset directly.
- **Frontend control center**: the production dark/wide/mobile compatibility rules are being migrated into `/assets/admin-frontend-controls.css`; the shared bridge must no longer contain `frontend-control-admin` selectors after this batch passes CI.

All feature-owned replacements are covered by Maven contracts, Chromium/WebKit regression, and the shared production smoke script used on ports 18080, 8081 and 8080.

### Remaining shared bridge boundary

After the frontend-control migration, `/assets/vnext-legacy-bridge-v3.css` is intentionally limited to the **main operational admin** `body.admin-solid-ui` compatibility rules. `admin-brand-layout-fixes.css` still imports that bridge because several existing admin entrypoints share the same brand/layout layer.

Therefore the shared bridge is **not yet removable**. The final retirement step must first move the remaining `admin-solid-ui` table/panel/tab/modal/notice rules into their canonical admin owner, then prove the admin surfaces in Chromium + WebKit and production smoke without the shared bridge.

## Planned sequence

1. Centralize shared asset loading and remove runtime style injection. **Completed for public deep-page compatibility assets.**
2. Retire page-local compatibility assets into feature-owned canonical CSS. **Account submit, account center, service detail and record/order detail completed.**
3. Retire frontend-control visual rules into `admin-frontend-controls.css`. **Current batch.**
4. Retire the final `admin-solid-ui` rules into the canonical main-admin style owner.
5. Remove `vnext-legacy-bridge-v3.css` and its `@import` only after the final admin browser + production gates pass.
6. Re-evaluate v4/v5/v7/v8 public compatibility layers only after no mature Thymeleaf public page requires them.
7. Remove remaining legacy templates only after controller routing and production smoke prove the VNext replacement.

## Deletion gate

Every deletion PR must include:

- exact routes affected;
- old asset/template references removed;
- replacement owner;
- Maven contract coverage;
- Chromium and WebKit coverage;
- production smoke impact;
- rollback path.
