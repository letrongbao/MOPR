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

## Runtime Truth Snapshot (2026-04-06)

Use these facts as non-negotiable runtime behavior unless the task explicitly changes them:

1. Firestore root data scope is `tenants/{tenantId}/...` with legacy fallback `users/{uid}/...`.
2. There is no top-level `org` or `organizations` collection; `Org*` names are UI/module terminology only.
3. Google sign-in (`MainActivity`) ensures `users/{uid}` exists and must NOT overwrite existing `primaryRole`/`activeTenantId`.
4. Home shell role rendering (`HomeMenuActivity`) is currently binary by `users/{uid}.primaryRole`:
	- `OWNER` => owner UI
	- non-`OWNER` => guest UI
5. If `primaryRole` changes from non-OWNER to OWNER during active Home session, app forces sign-out and requires re-login.
6. Change password is available only when account has `password` provider; Google-only accounts must not see/use password change flow.
7. Vietnamese wording convention for UI/docs: use `lí` in management wording, and keep the brand phrase exactly `Quản lí trọ`.

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

## Latest I18n Progress (2026-04-04)
- Build status: `:app:assembleDebug` passes after each i18n batch.
- Bilingual baseline added:
	- New English resource file created: `res/values-en/strings.xml`.
	- Priority UI modules translated in EN: auth/common actions, profile, home menu/drawer, room/tenant, invoice/payment/expense/revenue core labels, and recent dialog batches.
- Newly externalized in latest continuation:
	- `layout/bottom_sheet_deposit.xml`
	- `layout/dialog_month_year_picker.xml`
	- `layout/dialog_add_tenant.xml`
	- `layout/dialog_create_house.xml`
	- `layout/item_expense.xml`
	- `layout/item_backup.xml`
	- `layout/activity_org_admin.xml`
	- `layout/dialog_add_room.xml`
	- `layout/dialog_export_invoice.xml`
	- `layout/dialog_vietqr.xml`
	- `layout/item_contract.xml`
	- `layout/item_invoice.xml`
	- `layout/item_room.xml`
	- `layout/item_tenant_card.xml`
	- `layout/item_rental_history.xml`
	- `layout/item_org_invite.xml`
	- `layout/item_org_member.xml`
	- `layout/item_payment.xml`
	- `layout/item_meter_reading.xml`
	- `layout/item_contract_smart.xml`
	- `layout/item_home_menu_tile.xml`
	- `layout/item_house.xml`
	- `layout/item_tenant_header.xml`
	- `layout/item_ticket.xml`
	- `layout/row_additional_expense.xml`
	- `layout/row_detail_info.xml`
	- `layout/widget_invoice.xml`
- Newly externalized in this round:
	- `features/contract/ContractDetailsActivity`
	- `features/contract/ContractListItemUiHelper`
	- `features/contract/ContractPdfPreviewActivity`
	- `features/invoice/InvoiceActivity` (draft creation progress/summary)
	- `features/contract/ContractListAdapter`
	- `features/org/OrgAdminActivity`
	- `features/property/house/HouseAdapter`
	- `features/finance/ExpenseAdapter`
	- `features/invoice/InvoiceDialogSubmitHelper` (messages moved to caller)
	- `features/contract/ContractFormDataHelper` (messages moved to caller)
	- `features/contract/ContractActivity` (major toast/title/fee text externalization)
	- `features/invoice/InvoiceFilterCoordinator` (summary text externalized)
	- `features/property/room/RoomDetailsActivity` (toolbar/labels/toasts/sms template externalized)
	- `features/property/room/RoomActivity` (major dialog/toast/title literals externalized)
	- XML layouts migrated in this pass:
		- `layout/activity_contract.xml`
		- `layout/activity_contract_list.xml`
		- `layout/activity_contract_details.xml`
		- `layout/activity_invoice.xml`
		- `layout/activity_expense.xml`
		- `layout/activity_payment_history.xml`
		- `layout/activity_main.xml`
		- `layout/activity_sign_up.xml`
		- `layout/activity_change_password.xml`
		- `layout/activity_backup_restore.xml`
		- `layout/activity_tickets.xml`
		- `layout/activity_house.xml`
		- `layout/activity_meter_reading_history.xml`
		- `layout/activity_edit_profile.xml`
		- `layout/activity_profile.xml`
		- `layout/activity_rental_history.xml`
		- `layout/activity_tenant.xml`
		- `layout/activity_room.xml`
		- `layout/activity_room_details.xml`
		- `layout/activity_home_menu.xml`
		- `layout/home_menu_scroll_content.xml`
		- `layout/home_menu_profile_drawer.xml`
		- `layout/dialog_create_ticket.xml`
		- `layout/dialog_create_backup.xml`
		- `layout/dialog_edit_bank.xml`
		- `layout/dialog_add_payment.xml`
		- `layout/activity_revenue.xml`
		- `layout/dialog_add_invoice.xml`
		- `layout/dialog_finalize_period.xml`
		- `layout/dialog_add_expense.xml`
		- `layout/dialog_finalize_meter_reading.xml`
		- `layout/bottom_sheet_fee_notification.xml`
		- `layout/dialog_add_room.xml`
		- `layout/dialog_export_invoice.xml`
		- `layout/dialog_vietqr.xml`
- New resource groups added:
	- Contract status/deposit labels (`expired/active/expiring soon/days left`)
	- Contract PDF preview errors + chooser titles
	- Invoice draft summary fragments
	- Profile/account/home menu labels and placeholders
	- Room module dialog/state/toast labels
	- Backup/ticket/bank/payment dialog hints and labels
	- Revenue dashboard labels/placeholders
	- Invoice/finalize-period/finalize-meter labels
	- Fee notification bottom-sheet labels/hints
	- Deposit collection bottom-sheet labels/actions
	- Month-year picker labels/actions
	- Add-room dialog labels/hints and duplicate-check guidance
	- Export invoice and VietQR dialog placeholders/labels
	- Org admin labels/placeholders
	- Item/list/widget placeholders and actions across contract/invoice/room/tenant/history/org/payment/meter/home/house modules
	- Generic fallback keys for Java-side UI formatting (`common_not_available`, `vietqr_title`, service labels)
- Guardrails re-validated:
	- No Firestore collection/key schema changes in these edits.
	- Continue with small batches + build validation after each batch.

## Next Suggested Batch
- Focus remaining scope on XML/layout literals (`android:text`, `android:hint`, `tools:text`) and residual Java literals in less-touched modules.
- Keep same strategy: small file groups -> add string keys -> build validate.
- High-priority remaining targets:
	- Layout-level hardcoded literals are now fully migrated to string resources in active XML files.
	- Continue optional deep cleanup in Java for any non-UI literal constants not tied to user-facing text.
	- residual literals in `features/property/room/RoomActivity.java` (room-type options array)
	- Expand EN coverage for remaining untranslated keys (fallback currently remains VI for non-overridden keys)
