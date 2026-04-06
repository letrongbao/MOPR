# Thêm chức năng "Đăng tin phòng trọ"

## Mô tả (Goal Description)
Chức năng "Đăng tin phòng trọ" là **hoàn toàn khả thi**. Việc bổ sung tính năng này giúp chủ trọ tạo các bài đăng quảng cáo cho các phòng đang trống, bao gồm mô tả, giá cả, và hình ảnh.

## Lựa chọn kiến trúc (User Review Required)
Tôi cần sự xác nhận của bạn về mục đích chính của tính năng này trước khi bắt đầu code:
> [!IMPORTANT]
> - Trọng tâm hiện tại của hệ thống là quản lí nội bộ. Vậy các "tin đăng" này sẽ được hiển thị cho ai xem?
> - **Lựa chọn A**: App sẽ có một màn hình "Tìm phòng trống" cho khách vãng lai (khách không cần đăng nhập hoặc có role là "Người tìm phòng").
> - **Lựa chọn B**: App chỉ hỗ trợ chủ trọ soạn tin đăng (gồm hình ảnh, nội dung mẫu) để chủ trọ dễ dàng copy và chia sẻ lên Facebook/Zalo/Web.

## Các thay đổi dự kiến (Proposed Changes)

### Domain
#### [NEW] `com/example/myapplication/domain/TinDang.java`
- Chứa các thông tin: `id`, `idChuTro`, `idNha`, `idPhong`, `tieuDe`, `moTa`, `giaThue`, `tinhTrang`, `danhSachHinhAnh`, `ngayTao`.

### Repository
#### [NEW] `com/example/myapplication/core/repository/domain/TinDangRepository.java`
- Xử lí các thao tác thêm, sửa, xóa, lấy danh sách bài đăng từ Firestore (collection `tin_dang`).

### Feature & UI (Quản lí tin đăng)
#### [MODIFY] `com/example/myapplication/features/home/HomeMenuActivity.java`
#### [MODIFY] `res/layout/activity_home_menu.xml`
- Thêm Card Menu "Quản lí tin đăng" vào màn hình trang chủ của chủ trọ.

#### [NEW] `com/example/myapplication/features/post/QuanLyTinDangActivity.java`
- Màn hình danh sách các tin đăng của chủ trọ.

#### [NEW] `com/example/myapplication/features/post/ThemTinDangActivity.java`
- Màn hình form thêm/sửa tin đăng (chọn nhà, chọn phòng trống, nhập tiêu đề, nội dung, đính kèm ảnh).

## Câu hỏi mở (Open Questions)
> [!WARNING]
> Những câu hỏi cần làm rõ để tối ưu quy trình:
> 1. Bạn có muốn chức năng đính kèm ảnh không? (Nếu có thì sẽ cần tích hợp **Firebase Storage**).
> 2. Có tự động ẩn tin đăng khi phòng đã được chuyển trạng thái sang "Đã thuê" không?

## Kế hoạch kiểm thử (Verification Plan)
### Bằng tay (Manual Verification)
- Truy cập vào app với role chủ trọ, kiểm tra xem có nút "Quản lí tin đăng" trên màn hình Home.
- Tạo một tin đăng mới và kiểm tra xem dữ liệu có được lưu lên Firestore.
- Chọn một phòng trống để đăng tin và xem dữ liệu về Giá, Diện tích có tự trích xuất từ thông tin phòng hay không.
