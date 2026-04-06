# Kế hoạch Nâng cấp Màn hình Quản Lí Khách Thuê

Dựa vào 2 bản thiết kế bạn vừa cung cấp, hệ thống cần đáp ứng giao diện quản lí khách thuê phân nhóm trực quan theo từng phòng, đi kèm thanh công cụ tìm kiếm, xuất Excel và các huy hiệu (badge) chức năng/giấy tờ pháp lí cho từng cá nhân khách.

Mục tiêu của kế hoạch này là phân tích lộ trình thực hiện để đạt chính xác giao diện đó. Không có thay đổi (file) nào được thực thi lúc này.

## Trả lời: "Tôi có phải chỉnh sửa Database Firebase không?"

**CÓ, cần phải thêm trường dữ liệu nhưng RẤT AN TOÀN cho dữ liệu cũ.**
Bởi vì Firestore là NoSQL, cấu trúc linh hoạt. Tuy nhiên, căn cứ vào thiết kế UI mới, mỗi khách thuê đang xuất hiện **4 thông tin hoàn toàn mới** mà hiện tại Model `Tenant.java` trong mã nguồn chưa hề có:
1. `laNguoiLienHe` (boolean): Là người liên hệ chính của phòng.
2. `laDaiDienHopDong` (boolean): Là người đứng tên đại diện hợp đồng.
3. `dangKyTamTru` (boolean): Trạng thái Đã / Chưa đăng ký tạm trú.
4. `dayDuGiayTo` (boolean): Trạng thái Đầy đủ / Chưa đầy đủ giấy tờ.

**Điều cần làm:** Chúng ta cần thêm 4 biến này vào mô hình `Tenant.java`. Đối với các khách thuê CŨ đang có sẵn trên Firebase, khi tải xuống ứng dụng, 4 biến này sẽ nhận giá trị mặc định là `false` (nghĩa là "Chưa đăng ký tạm trú", "Chưa đủ giấy tờ"...) mà không gây lỗi ứng dụng (Crash). Ta cũng cần thiết kế lại form (Dialog) "Thêm người thuê" để cho phép nhập 4 trạng thái này khi tạo mới.

---

## Proposed Changes (Chi tiết kỹ thuật)

> [!TIP]
> Kế hoạch đề xuất sẽ **nâng cấp và tái cấu trúc** màn hình quản lí khách thuê hiện tại thay vì giữ một dạng danh sách cuộn phẳng (flat list) đơn điệu.

### 1. Model & Data Layer
#### [MODIFY] `domain/Tenant.java`
- Bổ sung 4 biến logic (boolean): `isNguoiLienHe`, `isDaiDienHopDong`, `isTamTru`, `isDayDuGiayTo` cùng Getters/Setters.

### 2. Giao diện (Layouts)
#### [NEW] `layout/activity_quan_ly_khach_thue.xml` (hoặc tái cấu trúc `activity_tenant.xml`)
- Thêm thanh tìm kiếm (Search bar) và Dropdown (Spinner) "Chọn phòng".
- Thêm hệ thống nút thao tác trên cùng: `Chia sẻ excel`, `Quản lí khách cũ` (Button styling theo màu xanh/đen trong hình).
- Một `RecyclerView` chứa toàn bộ danh sách phòng và khách thuê.

#### [NEW] `layout/item_tenant_header.xml`
- Sinh ra dòng chữ phân khu: "Phòng 2", "Phòng 3"... bao gồm cả button "(+) Thêm" và "(🔗) Tự nhập".

#### [NEW] `layout/item_tenant_card.xml` (hoặc tái cấu trúc `item_tenant.xml`)
- Card viền bo tròn chứa:
  - Tên Khách, SĐT.
  - Icon Avatar (màu xanh lá nhạt / đen).
  - Nút Call (Điện thoại nền xanh), Nút More (3 chấm góc phải).
  - 2 Dòng badge bên dưới: `(Vai trò: Liên hệ/Đại diện hợp đồng)` và `(Trạng thái: Tạm trú/Giấy tờ)` kèm logic đổi màu (Xanh = Đã làm / Cam = Chưa làm).

#### [MODIFY] `layout/dialog_add_tenant.xml`
- Bổ sung 4 Checkbox/Switch để chủ trọ tích trạng thái "Đại diện hợp đồng", "Tạm trú"... khi cập nhật thông tin khách.

### 3. Logic & Xử lí (Java)
#### [MODIFY] `features/tenant/TenantAdapter.java`
- Thay vì List phẳng `List<Tenant>`, Adapter sẽ dùng thuật toán chuyển thành `Danh sách kết hợp (Nhóm Phòng + Danh sách KH)`.
- Triển khai **Multi-ViewType** cho RecyclerView:
  - `VIEW_TYPE_HEADER`: Hiển thị tên phòng (Ví dụ: `item_tenant_header`).
  - `VIEW_TYPE_ITEM`: Hiển thị chi tiết từng người trong thẻ bài.
- Gắn tính năng gọi điện thực tế vào icon gọi (`Intent.ACTION_DIAL`).

#### [MODIFY] `features/tenant/TenantActivity.java`
- Đóng vai trò tổng hợp dữ liệu từ Firebase, nạp danh sách, rồi xử lí logic gom nhóm (Group By) khách hàng theo thuộc tính `idPhong`.
- Thêm chức năng thanh Tìm Kiếm (giúp Live-filter RecyclerView theo Tên hoặc Số điện thoại).

---

## Open Questions
> [!IMPORTANT]
> **Vui lòng trả lời để tôi có thể chỉnh sửa code một cách chuẩn sát nhất:**
> 1. Tính năng **"(+) Thêm"** và **"(🔗) Tự nhập"** ở cạnh mỗi Tên Phòng hoạt động như thế nào? Cụ thể "(+) Thêm" là chủ trọ điền form như cũ, còn "(🔗) Tự nhập" là tạo ra Link gửi cho khách tự điền thông tin đúng không? (Nếu tính năng link chưa có, tôi có thể tạm khóa nút này).
> 2. Tính năng **"Chia sẻ excel"** có cần phải hoạt động ngay (tạo tải file .xlsx/csv vào máy tính) hay chỉ làm giao diện giả lập trước?
> 3. Bạn muốn thực thi (ghi code) kế hoạch này ngay bây giờ chưa? Nếu có, tôi sẽ bắt tay vào làm.

