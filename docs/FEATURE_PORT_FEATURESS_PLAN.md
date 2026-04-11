# Feature Port Plan: featuress -> dev (incremental)

## Goal
Port useful features from branch `featuress` into `dev` incrementally, without direct merge, to protect stable data flow and existing optimized UI on dev.

## Branching model
- Base branch: `dev`
- Working branch: `feature/port-featuress-on-dev`
- Integration style: selective port by feature slice, not full-file overwrite.

## Non-negotiable guardrails
1. Do not replace current dev notification/chat flow.
2. Do not hardcode owner/tenant identity values.
3. Do not overwrite existing `item_notification.xml` structure used by dev.
4. Any data model write must follow Firestore contract and tenancy rules.
5. Every slice must pass build + smoke checks before merge.

## Slice roadmap

### Slice A (Priority: High): Report Core
Scope:
- Tenant report list/create/edit/resubmit/cancel
- Owner report list + status transitions
- Required adapters/layouts/drawables

Must-fix before merge:
- Remove hardcoded ownerId/ownerName
- Wire reject action button end-to-end
- Ensure image selection is persisted correctly (or hide image UI until fully supported)
- Fix list merge logic to avoid mutability/runtime risk

Acceptance:
- `./gradlew.bat assembleDebug` passes
- Tenant can create report and owner sees correct records
- Status flow works: PENDING -> CONFIRMED/IN_PROGRESS -> DONE/REJECTED

### Slice B (Priority: Medium): Entry points from existing menus
Scope:
- Add owner entry card for issue management
- Add tenant entry to report list

Rules:
- Keep existing chat route for `cardKhachThue`
- Keep NotificationCenter route and unread badge behavior

Acceptance:
- Existing home menu behavior unchanged except new report entry points
- No regression in notification badge and drawer navigation

### Slice C (Priority: Medium): Tenant contract details screen
Scope:
- Add tenant contract details activity/layout
- Connect from tenant menu only when room/tenant context is valid

Acceptance:
- Screen opens reliably
- Missing contract fallback handled gracefully

### Slice D (Priority: Low): New notification UI variant
Scope:
- Introduce new visual style for system notification cards

Rules:
- Do not reuse conflicting filename/id contract of current notification item layout
- Prefer new layout/resource names and opt-in integration

Acceptance:
- No breakage in current notification center UI

## Merge policy for each slice
1. Implement only one slice per PR.
2. Attach short risk note + test evidence in PR description.
3. If regression found, revert slice commit only.

## Suggested PR sequence
1. PR-1: Slice A (Report Core)
2. PR-2: Slice B (Menu entry points)
3. PR-3: Slice C (Tenant contract details)
4. PR-4: Slice D (Optional notification UI variant)

## Quick commands
- Build: `./gradlew.bat assembleDebug`
- Branch status: `git status --short --branch`
- Compare current work: `git diff --name-status dev...feature/port-featuress-on-dev`
