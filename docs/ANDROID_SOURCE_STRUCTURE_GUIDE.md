# Android Source Structure Guide (NextJS Mindset)

## Objective
This document maps NextJS architecture patterns to Android Java to create clean, maintainable, and upgradeable code.

## 1) NextJS Architecture → Android Mapping

1. NextJS page → Android Activity/Fragment
2. NextJS component → Custom View / Adapter item binder / include layout
3. NextJS hook → ViewModel state + use-case helper
4. NextJS service/api client → Repository
5. NextJS shared UI/util → core/widget + core/util

## 2) Recommended Structure per Feature

```text
features/
  invoice/
    InvoiceActivity.java           # Entry screen, UI orchestration
    InvoiceAdapter.java            # Recycler adapter
    InvoicePermissionResolver.java # Resolve role + access mode
    InvoiceDialogs.java            # (recommended) extract large dialogs
    InvoiceFormatters.java         # (recommended) format text/money/status
```

Rules:
1. Each feature contains its own files.
2. Activity/Fragment must not contain large business logic.
3. Permission logic, filtering, mapping must be extracted to helper classes.

## 3) Target File Size Limits

1. Activity/Fragment: <= 400 lines
2. Adapter: <= 300 lines
3. XML layout: <= 300 lines
4. Utility helper: <= 200 lines

If exceeding limits, prioritize extraction by functionality (role, filter, dialog, formatter).

## 4) Safe File Extraction Principles

1. Do not modify Firestore schema during UI/logic refactor.
2. Proceed in small batches, build after each batch.
3. Prioritize extracting stateless classes first (resolver/formatter).
4. If behavior changes, must document in docs contract.

## 5) Refactor Patterns to Apply Immediately to MOPR

### Pattern A: Role resolver

- Purpose: Extract role check logic from Activity.
- Applied example:
  - `InvoicePermissionResolver` extracted from `InvoiceActivity`.

### Pattern B: Dialog factory

- Purpose: Extract create/edit/confirm dialogs to separate class.
- Suggested names:
  - `InvoiceDialogFactory`
  - `ContractDialogFactory`

### Pattern C: Filter coordinator

- Purpose: Extract filtering by month/house/status/search to separate class.
- Suggested names:
  - `InvoiceFilterCoordinator`

### Pattern D: Layout include decomposition

- Purpose: Extract large XML into multiple section includes for readability.
- Applied example:
  - `activity_home_menu.xml` → includes `home_menu_scroll_content.xml` + `home_menu_profile_drawer.xml`.

### Pattern E: Payment flow helper

- Purpose: Extract payment receipt + QR + submit payment from Activity.
- Applied example:
  - `InvoicePaymentFlowHelper` extracted from `InvoiceActivity`.

### Pattern F: Export dialog helper

- Purpose: Extract invoice export dialog + tenant meter confirmation from Activity.
- Applied example:
  - `InvoiceExportDialogHelper` extracted from `InvoiceActivity`.

### Pattern G: Fee notification helper

- Purpose: Extract fee bottom-sheet and meter index update from Activity.
- Applied example:
  - `InvoiceFeeNotificationHelper` extracted from `InvoiceActivity`.

### Pattern H: Meter helper

- Purpose: Extract meter reading entry/display by room+period from Activity.
- Applied example:
  - `InvoiceMeterHelper` extracted from `InvoiceActivity`.

### Pattern I: Form value helper

- Purpose: Extract form data parsing/formatting/validation for add/edit invoice.
- Applied example:
  - `InvoiceFormValueHelper` extracted from `InvoiceActivity`.

### Pattern J: Dialog submit helper

- Purpose: Extract form validation + data mapping for add/edit invoice from Activity.
- Applied example:
  - `InvoiceDialogSubmitHelper` extracted from `InvoiceActivity`.

### Pattern K: Dialog UI helper

- Purpose: Extract spinner binding + form fill + read-only state + estimated-total wiring from Activity.
- Applied example:
  - `InvoiceDialogUiHelper` extracted from `InvoiceActivity`.

### Pattern L: Period suggestion helper

- Purpose: Extract next invoice period suggestion by room from Activity.
- Applied example:
  - `InvoicePeriodSuggestionHelper` extracted from `InvoiceActivity`.

## 6) Refactor Priority Timeline

1. InvoiceActivity (largest): already reduced to manageable size, continue extracting dialog factory if major changes occur next.
2. ContractActivity: extract form validator + pdf/export helper.
3. RoomActivity: extract tenant-room binding + status filter helper.
4. Large layouts: extract `include` for header/stats/grid/drawer.

### Pattern M: Contract form data helper

- Purpose: Extract form data parsing/validation/mapping from Activity.
- Applied example:
  - `ContractFormDataHelper` extracted from `ContractActivity`.

### Pattern N: Contract HTML builder

- Purpose: Extract invoice template HTML + formatting helper from Activity.
- Applied example:
  - `ContractHtmlBuilder` extracted from `ContractActivity`.

### Pattern O: Contract date helper

- Purpose: Extract contract date parsing/normalization/computation from Activity.
- Applied example:
  - `ContractDateHelper` extracted from `ContractActivity`.

### Pattern P: Contract list item UI helper

- Purpose: Extract contract list item display formatting (status chip, deposit state, money format) from adapter.
- Applied example:
  - `ContractListItemUiHelper` extracted from `ContractListAdapter`.

### Pattern Q: Shared screen scaffold helper

- Purpose: Synchronize edge-to-edge + top inset + back-toolbar across multiple screens.
- Applied example:
  - `ScreenUiHelper` used in `InvoiceActivity`, `PaymentHistoryActivity`, `TenantPaymentHistoryActivity`, `ContractActivity`, `ContractListActivity`, `ContractDetailsActivity`, `ExpenseActivity`, `RevenueActivity`, `EditProfileActivity`, `ChangePasswordActivity`.

## 7) Definition of Done Checklist per Batch

1. Build passes: `./gradlew.bat :app:assembleDebug`
2. Do not modify Firestore key/collection names unintentionally
3. UI behavior unchanged (except when task explicitly requires behavior change)
4. Update documentation if data-flow/permission flow changes
