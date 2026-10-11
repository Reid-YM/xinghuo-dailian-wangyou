# Legacy Frontend Retirement Plan

This document defines the retirement boundary for remaining Thymeleaf, CSS and JavaScript surfaces after the VNext production migration.

## Principles

1. **Do not delete by age or filename.** An asset is removable only after every production route that consumes it has a canonical replacement and a regression gate.
2. **One owner per responsibility.** Markup loads styles, behavior scripts own behavior, and application/runtime code must not dynamically inject canonical design layers.
3. **Stable public contracts first.** URL, form IDs, account/auth behavior, API payloads and production smoke expectations remain stable while presentation assets are retired.
4. **Retire from leaves inward.** Move page-local rules first, then remove the shared compatibility layer only after its last real consumer is gone.

## Canonical visual ownership

### Public/shared pages

`templates/fragments/site.html` remains the canonical shared public visual stack owner:

1. `site-experience-v2.css`
2. `vnext-public-finish-v4.css`
3. `vnext-final-qa-fix-v5.css`
4. `vnext-mobile-focus-v7.css`
5. `vnext-mobile-polish-v8.css`

`site-shell.js` is behavior-only. The React/Vite build owns its equivalent order through `frontend-vnext/src/formal-entry.css`.

### Feature-owned retired bridge surfaces

- Account submit → `/assets/account-submit.css`
- Account center → `/assets/account-center.css` + `/assets/account-center-refine.css`
- Service detail → `/assets/service-detail.css`
- Record / order detail → `/assets/record-detail.css`
- Frontend control center → `/assets/admin-frontend-controls.css`

### Admin canonical owners

- Main operational admin → `/assets/admin-control-center-v2.css` + `/assets/admin-light-theme.css`
- Standalone guide admin → `/assets/admin-guides-workspace.css` + `/assets/admin-light-theme.css`
- Shared brand/logo/narrow-screen fixes → `/assets/admin-brand-layout-fixes.css`

The final `admin-solid-ui` rules that remained in `vnext-legacy-bridge-v3.css` were historical dark overrides loaded after the canonical light admin styles. They were not a distinct business/runtime owner. The final retirement batch removes that contradictory layer rather than copying it into the canonical light styles.

## Completed retirement sequence

1. Centralize shared asset loading and remove runtime style injection — completed.
2. Retire `vnext-account-submit-v3.css` into `account-submit.css` — completed.
3. Move account-center rules into account-center canonical CSS — completed.
4. Move service-detail rules into `service-detail.css` — completed.
5. Move record/order rules into `record-detail.css` — completed.
6. Move frontend-control dark/wide/mobile rules into `admin-frontend-controls.css` — completed in #256.
7. Remove the final shared `vnext-legacy-bridge-v3.css` import and asset — current #257 final batch.

## Final shared bridge retirement gate

The final deletion is accepted only when all of the following are true on the same candidate SHA:

- no template/runtime stylesheet imports `/assets/vnext-legacy-bridge-v3.css`;
- `fragments/vnext-legacy.html` no longer exposes `deepPageBridge` and account submit only loads its canonical stylesheet;
- Maven contracts assert the old bridge file does not exist;
- Chromium regression proves main/feature surfaces still render from canonical assets;
- WebKit regression proves mobile surfaces remain overflow-free;
- standalone guide-admin regression proves the real production order `app.css -> admin-guides-workspace.css -> admin-light-theme.css -> admin-brand-layout-fixes.css` works without the bridge;
- production smoke on 18080, 8081 and 8080 requires `/assets/vnext-legacy-bridge-v3.css` to return 404;
- all canonical replacement assets return 200 in the same smoke contract.

## What is not being retired here

The public VNext v4/v5/v7/v8 layers are separate compatibility concerns and remain production dependencies until their own route-by-route replacement proof exists. Business/runtime scripts, account/auth behavior, APIs, database migrations and controller routes are also outside this CSS-retirement batch.

## Deletion PR requirements

Every further deletion PR must continue to include:

- exact routes/surfaces affected;
- old references removed;
- replacement owner;
- Maven contract coverage;
- Chromium and WebKit coverage;
- production smoke impact;
- rollback path.
