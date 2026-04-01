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

## Required Commands
1. `git status --short`
2. `./gradlew.bat assembleDebug`

## Done Definition
1. No `UU` in git status.
2. Build successful.
3. No broken references after rename/move.
4. Docs updated:
   - `docs/PROJECT_STATUS.md`
   - `docs/OPEN_ISSUES.md`
   - `project_file_summary.txt`

## Suggested Work Order
1. Conflict resolution (`UU`).
2. Reference consistency checks.
3. Build and quick smoke tests.
4. Documentation refresh.
