# Open Issues

Last updated: 2026-04-02

## Critical
1. Unmerged (`UU`) files must be resolved:
   - app/src/main/java/com/example/myapplication/features/contract/HopDongActivity.java
   - app/src/main/java/com/example/myapplication/features/finance/DoanhThuActivity.java
   - app/src/main/java/com/example/myapplication/features/invoice/HoaDonActivity.java
   - app/src/main/java/com/example/myapplication/features/invoice/HoaDonAdapter.java
   - app/src/main/res/layout/activity_hoa_don.xml

## High
1. Rename/refactor transition still has mixed git states (`MD`, `AD`, many `D` + `??`).
2. Legacy Vietnamese files and new English files coexist in repository history.
3. Need one final cleanup pass to avoid accidental duplicate usage.

## Medium
1. Some UI strings are still Vietnamese by design/compatibility.
2. `project_file_summary.txt` must stay in sync after any rename/move.

## Verification Checklist For Next Agent
1. Resolve `UU` files first.
2. Build: `./gradlew.bat assembleDebug`.
3. Check no unresolved merge markers in code.
4. Smoke test core features:
   - Invoice flow
   - Contract flow
   - Room/Tenant flow
   - Revenue flow
