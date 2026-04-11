# Triển Khai Tính Năng Broadcast Notifications (Thông báo hàng loạt)

Tính năng này giúp chủ trọ (`Owner`) có thể gửi thông báo tùy chỉnh (Khẩn cấp, Bảo trì, Thông tin) tới toàn bộ phòng (`ALL`) hoặc một phòng cụ thể. Đồng thời, khách thuê (`Tenant`) nhận được tin nhắn hiển thị đúng định dạng.

## Proposed Changes

### Domain Models
#### [NEW] [BroadcastNotification.java](file:///d:/AndroidProject/MOPR/app/src/main/java/com/example/myapplication/domain/BroadcastNotification.java)
- Tạo model định nghĩa các trường cấu trúc: `id`, `title`, `content`, `senderName`, `type` (URGENT, INFO, MAINTENANCE), `targetRoomId`, và `sentAt` (kiểu `Object` để parse đa hình (poly-typed) an toàn).

---

### Giao Diện Chủ Trọ (Owner)
#### [NEW] [activity_owner_send_broadcast.xml](file:///d:/AndroidProject/MOPR/app/src/main/res/layout/activity_owner_send_broadcast.xml)
- Form nhập: Tiêu đề, Nội dung.
- Spinner (Dropdown) chọn phòng: option đầu sẽ là "Tất cả các phòng" (`ALL`), theo sau là danh sách lấy động từ collection contracts (chỉ những hợp đồng `ACTIVE`).
- Spinner/Radio chọn loại thông báo: URGENT, INFO, MAINTENANCE.
- Nút "Gửi thông báo".

#### [NEW] [OwnerSendBroadcastActivity.java](file:///d:/AndroidProject/MOPR/app/src/main/java/com/example/myapplication/features/home/OwnerSendBroadcastActivity.java)
- Truy vấn lấy danh sách phòng từ `contracts` (có trạng thái `ACTIVE`).
- Gắn chức năng lưu một đối tượng `BroadcastNotification` lên `broadcasts` (Dùng `FieldValue.serverTimestamp()` cho `sentAt`).

#### [MODIFY] [AndroidManifest.xml](file:///d:/AndroidProject/MOPR/app/src/main/AndroidManifest.xml)
- Đăng kí `OwnerSendBroadcastActivity` vào hệ thống.

---

### Giao Diện Khách Thuê (Tenant)
#### [MODIFY] [NotificationItem.java](file:///d:/AndroidProject/MOPR/app/src/main/java/com/example/myapplication/features/home/NotificationItem.java)
- Cập nhật thêm các type: `TYPE_URGENT` và `TYPE_MAINTENANCE` để định nghĩa loại card.
- Thêm icon: `ICON_URGENT = "⚠️"`, `ICON_MAINTENANCE = "🛠️"`.

#### [MODIFY] [NotificationAdapter.java](file:///d:/AndroidProject/MOPR/app/src/main/java/com/example/myapplication/features/home/NotificationAdapter.java)
- Bổ sung cấu hình màu sắc trong `onBindViewHolder`:
    - Dòng code if `TYPE_URGENT`: Card viền/nền màu Đỏ nhẹ, chữ Đỏ.
    - Dòng code if `TYPE_MAINTENANCE`: Card viền/nền màu Xanh dương nhạt, chữ Xanh dương.

#### [MODIFY] [NotificationActivity.java](file:///d:/AndroidProject/MOPR/app/src/main/java/com/example/myapplication/features/home/NotificationActivity.java)
- Thêm một luồng Query song song để bắt toàn bộ các document nằm ở mảng `broadcasts` thoả mãn 2 điều kiện `targetRoomId == "ALL"` hoặc `targetRoomId == roomId cá nhân`.
- Tiến hành truy vấn 2 đợt cho 2 query (ALL và phòng riêng) -> trộn với dữ liệu system và notification cũ -> Sắp xếp lại.
- Dùng `instanceof` + try-catch giống như cách đã làm cho `parseDateFromDoc()`. Nếu lỗi định dạng `sentAt`, nó vẫn in ra thẻ nhưng ghi thời gian '--/--/----'.

## User Review Required
> [!IMPORTANT]
> Firestore hiện tại không hỗ trợ query dạng `OR` (targetRoomId == "ALL" HOẶC targetRoomId == roomId) bằng code Android cơ bản một cách hiệu quả trong một lệnh Query đơn lẻ. Chúng ta cần gõ 2 Query riêng biệt (Một lấy "ALL", Một lấy "roomId") và trộn chúng lại trên RAM trước khi hiển thị cho Adapter. Bạn có đồng ý với cách tiếp cận tải 2 query kết hợp này không?

## Verification Plan
1. **Gửi tin nhắn**: Đứng tại màn hình `OwnerSendBroadcastActivity`, gửi tin rác phân loại Khẩn cấp/Sửa chữa vào phòng `ALL`.
2. **Kiểm tra crash**: Vô màn hình Notification của Tenant chọc phá liên tục để đảm bảo `instanceof` xử lý trơn tru không sập app.
3. **Màu sắc Alert**: Xem các tin nhắn này có đúng thiết kế UI màu Đỏ / Xanh nổi bật hay không.
