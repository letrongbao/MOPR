# Project Status

Last updated: 2026-04-02
Workspace: D:/Study/MOPR
Branch: main

## Health Snapshot
- Build command: ./gradlew.bat assembleDebug
- Current result: BUILD SUCCESSFUL
- Build note: no blocking compile errors in latest run.

## Repository State
The working tree is not clean because a large rename/refactor is in progress.

### High-risk git states
- `UU` (unmerged) exists in critical files:
  - app/src/main/java/com/example/myapplication/features/contract/HopDongActivity.java
  - app/src/main/java/com/example/myapplication/features/finance/DoanhThuActivity.java
  - app/src/main/java/com/example/myapplication/features/invoice/HoaDonActivity.java
  - app/src/main/java/com/example/myapplication/features/invoice/HoaDonAdapter.java
  - app/src/main/res/layout/activity_hoa_don.xml
- `MD` / `AD` states exist in some files, meaning rename/move cleanup is not fully finalized.
- Many `??` files are new English-name files created by refactor.
- Many `D` files are legacy Vietnamese-name files pending final cleanup.

## Current Objective
- Finalize naming synchronization (Vietnamese -> English) without breaking runtime behavior.
- Keep Firestore backward compatibility where keys/collections still require legacy names.

## Immediate Priorities
1. Resolve all `UU` files first.
2. Normalize move/delete states (`MD`, `AD`) and verify no duplicate active paths.
3. Run build again after each conflict-resolution batch.
4. Keep `project_file_summary.txt` aligned with real file names.

## Validation Gate
Required before handoff:
1. `./gradlew.bat assembleDebug` passes.
2. `git status --short` has no `UU`.
3. Core screens open without crash: Invoice, Contract, Room, Tenant, Revenue.
