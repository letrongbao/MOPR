# Agent Handoff

## Objective
Stabilize and finalize the English naming refactor while preserving runtime behavior and data compatibility.

## In Scope
- Resolve merge conflicts.
- Fix compile/runtime issues caused by rename updates.
- Align Java/XML/resource references.
- Keep documentation in sync.

## Out Of Scope
- Large UX redesign.
- Database schema redesign.
- Firestore key/collection renaming without migration plan.

## Constraints
- Do not break existing Firestore data compatibility.
- Prefer small, verifiable batches.
- Build after every risky batch.

## Data Contract
- Firestore source-of-truth for agents: `docs/FIRESTORE_DATA_CONTRACT.md`
- Firebase ops guide for seed/reset/test: `docs/FIREBASE_DATA_MANAGEMENT_GUIDE.md`
- Before adding/changing any screen that reads/writes business data, align with this contract.

## Workflow Guide
- Working process standard: `docs/WORKFLOW_GUIDE.md`
- Follow this guide for batching changes, validation, and clean commits.

## Source Structure Guide
- Android modularization guide (NextJS mindset): `docs/ANDROID_SOURCE_STRUCTURE_GUIDE.md`
- Use this when splitting large Activity/XML files into maintainable feature slices.

## Shared UI Scaffold (Must Reuse)
- Reuse `core/util/ScreenUiHelper.java` for:
	1. edge-to-edge setup
	2. top inset padding for toolbar/appbar
	3. back-toolbar setup
- Do NOT re-copy window/inset/toolbar boilerplate per screen unless screen has a proven special case.

## Current Shared-Scaffold Rollout
- Already migrated:
	- `features/invoice/InvoiceActivity`
	- `features/invoice/PaymentHistoryActivity`
	- `features/invoice/TenantPaymentHistoryActivity`
	- `features/contract/ContractActivity`
	- `features/contract/ContractListActivity`
	- `features/contract/ContractDetailsActivity`
	- `features/finance/ExpenseActivity`
	- `features/finance/RevenueActivity`
	- `features/settings/EditProfileActivity`
	- `features/settings/ChangePasswordActivity`

## Role Rollout Order
- Canonical order: OWNER bootstrap -> STAFF invite -> TENANT join room.
- Default signup role: TENANT.
- Owner/staff permissions must come from membership, not free-form UI selection.

## Required Commands
1. `git status --short`
2. `./gradlew.bat assembleDebug`

## Done Definition
1. No `UU` in git status.
2. Build successful.
3. No broken references after rename/move.
4. Docs updated only when workflow/data contract changes.

## Suggested Work Order
1. Conflict resolution (`UU`).
2. Reference consistency checks.
3. Build and quick smoke tests.
4. Documentation refresh.
