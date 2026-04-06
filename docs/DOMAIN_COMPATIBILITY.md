# Domain Compatibility Rules

## Why This Exists
This project has live/legacy data. Some names must remain stable to avoid data loss.

## Keep Stable Unless Data Migration Is Planned
- Firestore collection names currently used by runtime queries, such as:
  - `tenants`
  - `users`
  - `members` (under `tenants/{tenantId}`)
  - `invites` (under `tenants/{tenantId}`)
  - `invoices`
  - `contracts`
  - `rooms`
  - `houses`
  - `expenses`
- Existing Firestore field keys already persisted in production/test data.
- Intent extra keys that are already shared across screens unless all call sites are updated.

## Safe To Refactor
- Java class names and file names.
- XML layout file names and references (when all call sites are updated).
- Method names in Activity/ViewModel/Repository classes.

## Migration Rule
If changing a persisted key or collection is required:
1. Add explicit migration plan.
2. Add fallback read path for old key.
3. Verify old and new data both load.
4. Remove fallback only after data migration completion.

## Current Invoice Status Model
Use constants from `core/constants/InvoiceStatus.java`:
- `UNREPORTED`
- `REPORTED`
- `PARTIAL`
- `PAID`
