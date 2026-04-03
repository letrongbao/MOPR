# Refactor Changelog (Vietnamese -> English)

## Scope
Large naming synchronization across Java classes, ViewModels, Repositories, and XML resources.

## Main Mapping Examples
- HoaDon -> Invoice
- ChiPhi -> Expense
- NguoiThue -> Tenant
- PhongTro -> Room
- CanNha -> House
- HopDong -> Contract
- LichSuThanhToan -> PaymentHistory
- LichSuCongTo -> MeterReadingHistory

## Repository Mapping Examples
- HoaDonRepository -> InvoiceRepository
- ChiPhiRepository -> ExpenseRepository
- NguoiThueRepository -> TenantRepository
- PhongTroRepository -> RoomRepository
- CanNhaRepository -> HouseRepository

## ViewModel Mapping Examples
- HoaDonViewModel -> InvoiceViewModel
- ChiPhiViewModel -> ExpenseViewModel
- NguoiThueViewModel -> TenantViewModel
- PhongTroViewModel -> RoomViewModel

## Resource Mapping Examples
- activity_hoa_don -> activity_invoice
- item_hoa_don -> item_invoice
- bottom_sheet_bao_phi -> bottom_sheet_fee_notification
- item_hop_dong_smart -> item_contract_smart
- menu_hop_dong -> menu_contract
- widget_hoa_don_info -> widget_invoice_info

## Notes
- Some legacy names still appear in git status due to transition state.
- Runtime behavior and data compatibility are higher priority than full lexical cleanup.
