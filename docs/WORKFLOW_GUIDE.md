# Workflow Guide For Agents

## Objective
This document defines the standard execution flow so any agent in the team can work consistently, keep commits clean, and avoid data regression.

## Required Reading Before Editing Code

1. `docs/WORKFLOW_GUIDE.md` (this document)
2. `docs/FIRESTORE_DATA_CONTRACT.md` (Firestore schema and data rules)
3. `docs/AGENT_HANDOFF.md` (scope and execution principles)

## Standard Workflow

1. Quickly inspect repository state
   - `git status --short`
2. Confirm task scope
   - If the task touches Firestore data, cross-check `FIRESTORE_DATA_CONTRACT.md`
3. Implement in small batches
   - Rebuild after each batch
4. Validate build
   - `./gradlew.bat assembleDebug`
5. Run smoke tests for related screens
   - Room/Tenant/Invoice/Contract/Revenue
6. Re-check git state before commit
   - Do not include IDE files, build outputs, or local secrets

## Clean Commit Rules

1. Do not commit IDE/local files:
   - `.idea/`, `.vscode/`, `local.properties`
2. Do not commit build output:
   - `build/`, `**/build/`, `.gradle/`
3. Do not commit local secrets:
   - `scripts/serviceAccountKey.json`
4. Commit only business-related files and necessary guidance docs

## Firestore Editing Rules

1. Always use the scope helper (`TenantSession` with `users/{uid}` fallback)
2. Do not rename collections/fields without a migration plan
3. Prefer existing foreign keys:
   - `roomId`, `contractId`, `invoiceId`
4. New screens must map to existing schema; do not introduce isolated schema variants

## Role/Onboarding Rules

1. Public signup defaults to `TENANT`.
2. `OWNER` and `STAFF` must be provisioned via bootstrap/invite, not self-selected in public signup.
3. Access control for tenant business data must follow tenant-scope `membership`.
4. Current Home shell rendering is a separate UI gate based on `users/{uid}.primaryRole` (OWNER vs non-OWNER), while domain authorization remains membership + house/room scope.

## UI Reuse Rules (Mandatory Priority)

1. Before editing UI, look for existing reusable helpers/components (`core/util`, `core/widget`).
2. If repeated scaffolding appears (edge-to-edge, insets, toolbar/back), use shared helpers.
3. Do not copy/paste UI boilerplate between screens when helper abstractions already exist.
4. If a special screen cannot use shared helpers, document the reason in the batch description.

## Vietnamese Copy Convention

1. Use `lí` for management wording in Vietnamese UI/docs.
2. Brand phrase must be exactly `Quản lí trọ`.

## Definition Of Done (DoD)

1. Build passes: `./gradlew.bat assembleDebug`
2. No merge conflict markers (`UU`) in `git status --short`
3. No noisy local changes (IDE/build/local files)
4. Updated functionality works with current Firestore data conventions
