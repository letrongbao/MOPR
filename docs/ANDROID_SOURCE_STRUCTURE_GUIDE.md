# Android Source Structure Guide (NextJS Mindset)

## Muc tieu
Tai lieu nay map tu duy chia nho cua NextJS sang Android Java de code gon, de maintain va de nang cap.

## 1) Map tu duy NextJS -> Android

1. NextJS page -> Android Activity/Fragment
2. NextJS component -> Custom View / Adapter item binder / include layout
3. NextJS hook -> ViewModel state + use-case helper
4. NextJS service/api client -> Repository
5. NextJS shared UI/util -> core/widget + core/util

## 2) Cau truc khuyen nghi theo feature

```text
features/
  invoice/
    InvoiceActivity.java           # Entry screen, dieu phoi UI
    InvoiceAdapter.java            # Recycler adapter
    InvoicePermissionResolver.java # Resolve role + access mode
    InvoiceDialogs.java            # (goi y) tach cac dialog lon
    InvoiceFormatters.java         # (goi y) format text/money/status
```

Rule:
1. Moi feature tu chua file cua no.
2. Activity/Fragment khong chua business rule lon.
3. Logic quyen, filter, mapping tach thanh helper class.

## 3) Gioi han kich thuoc file (muc tieu)

1. Activity/Fragment: <= 400 dong
2. Adapter: <= 300 dong
3. XML layout: <= 300 dong
4. Utility helper: <= 200 dong

Neu vuot nguong, uu tien tach theo chuc nang (role, filter, dialog, formatter).

## 4) Nguyen tac tach file an toan

1. Khong doi schema Firestore khi refactor UI/logic.
2. Tien hanh theo batch nho, build sau moi batch.
3. Uu tien tach class stateless truoc (resolver/formatter).
4. Khi can doi hanh vi, bat buoc co note trong docs contract.

## 5) Refactor pattern de ap dung ngay cho MOPR

### Pattern A: Role resolver

- Muc dich: dua logic role check ra khoi Activity.
- Vi du da ap dung:
  - `InvoicePermissionResolver` duoc tach khoi `InvoiceActivity`.

### Pattern B: Dialog factory

- Muc dich: dialog tao/sua/xac nhan dua ra class rieng.
- Ten goi y:
  - `InvoiceDialogFactory`
  - `ContractDialogFactory`

### Pattern C: Filter coordinator

- Muc dich: filter theo month/house/status/search dua ra class rieng.
- Ten goi y:
  - `InvoiceFilterCoordinator`

### Pattern D: Layout include decomposition

- Muc dich: tach XML lon thanh nhieu section include de de doc/de sua.
- Vi du da ap dung:
  - `activity_home_menu.xml` -> include `home_menu_scroll_content.xml` + `home_menu_profile_drawer.xml`.

### Pattern E: Payment flow helper

- Muc dich: tach luong thu tien + QR + submit payment khoi Activity.
- Vi du da ap dung:
  - `InvoicePaymentFlowHelper` duoc tach khoi `InvoiceActivity`.

### Pattern F: Export dialog helper

- Muc dich: tach dialog xuat hoa don + tenant confirm meter khoi Activity.
- Vi du da ap dung:
  - `InvoiceExportDialogHelper` duoc tach khoi `InvoiceActivity`.

### Pattern G: Fee notification helper

- Muc dich: tach bottom-sheet bao phi va cap nhat chi so khoi Activity.
- Vi du da ap dung:
  - `InvoiceFeeNotificationHelper` duoc tach khoi `InvoiceActivity`.

### Pattern H: Meter helper

- Muc dich: tach doc/ghi meter reading theo room+period khoi Activity.
- Vi du da ap dung:
  - `InvoiceMeterHelper` duoc tach khoi `InvoiceActivity`.

### Pattern I: Form value helper

- Muc dich: tach parse/format/watch tong tien cua form add/edit hoa don.
- Vi du da ap dung:
  - `InvoiceFormValueHelper` duoc tach khoi `InvoiceActivity`.

### Pattern J: Dialog submit helper

- Muc dich: tach validate + map du lieu form add/edit invoice khoi Activity.
- Vi du da ap dung:
  - `InvoiceDialogSubmitHelper` duoc tach khoi `InvoiceActivity`.

### Pattern K: Dialog UI helper

- Muc dich: tach spinner binding + form fill + read-only state + estimated-total wiring khoi Activity.
- Vi du da ap dung:
  - `InvoiceDialogUiHelper` duoc tach khoi `InvoiceActivity`.

### Pattern L: Period suggestion helper

- Muc dich: tach goi y ky hoa don tiep theo theo room khoi Activity.
- Vi du da ap dung:
  - `InvoicePeriodSuggestionHelper` duoc tach khoi `InvoiceActivity`.

## 6) Lo trinh refactor uu tien

1. InvoiceActivity (lon nhat): da giam ve muc quan ly duoc, tiep tuc tach dialog factory neu co thay doi lon tiep theo.
2. ContractActivity: tach form validator + pdf/export helper.
3. RoomActivity: tach tenant-room binding + status filter helper.
4. Layout lon: tach `include` cho header/stats/grid/drawer.

### Pattern M: Contract form data helper

- Muc dich: tach parse/validate/map du lieu form hop dong ra khoi Activity.
- Vi du da ap dung:
  - `ContractFormDataHelper` duoc tach khoi `ContractActivity`.

### Pattern N: Contract HTML builder

- Muc dich: tach template HTML hop dong + format helper khoi Activity.
- Vi du da ap dung:
  - `ContractHtmlBuilder` duoc tach khoi `ContractActivity`.

### Pattern O: Contract date helper

- Muc dich: tach parse/normalize/compute ngay hop dong khoi Activity.
- Vi du da ap dung:
  - `ContractDateHelper` duoc tach khoi `ContractActivity`.

### Pattern P: Contract list item UI helper

- Muc dich: tach format hien thi item hop dong (chip status, trang thai coc, money format) khoi adapter.
- Vi du da ap dung:
  - `ContractListItemUiHelper` duoc tach khoi `ContractListAdapter`.

### Pattern Q: Shared screen scaffold helper

- Muc dich: dong bo edge-to-edge + top inset + back-toolbar cho nhieu man hinh.
- Vi du da ap dung:
  - `ScreenUiHelper` duoc dung trong `InvoiceActivity`, `PaymentHistoryActivity`, `TenantPaymentHistoryActivity`, `ContractActivity`, `ContractListActivity`, `ContractDetailsActivity`, `ExpenseActivity`, `RevenueActivity`, `EditProfileActivity`, `ChangePasswordActivity`.

## 7) Checklist Definition of Done cho moi batch

1. Build pass: `./gradlew.bat :app:assembleDebug`
2. Khong doi Firestore key/collection ngoai y muon
3. UI behavior khong doi (tru khi co task doi behavior)
4. Cap nhat docs neu thay doi data-flow/permission flow
