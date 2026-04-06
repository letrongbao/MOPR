# ✅ Checklist - Sửa lỗi Routing theo Role

## Vấn đề đã giải quyết
❌ **Trước:** Đăng ký tài khoản với role TENANT nhưng vào màn hình Admin (HomeMenuActivity)  
✅ **Sau:** Đăng ký tài khoản với role TENANT → Tự động vào TenantDashboardActivity

---

## Files đã tạo mới

### 1. TenantDashboardActivity
- [x] **Layout XML:** `app/src/main/res/layout/activity_tenant_dashboard.xml`
  - Header màu xanh dương với 3 chỉ số (Số dư nợ, Ngày đến hạn, Trạng thái)
  - 4 CardView màu Pastel (Tím, Hồng, Xanh dương, Xanh lá)
  - Bo góc 16dp, hiệu ứng chạm selectableItemBackground
  
- [x] **Java Activity:** `app/src/main/java/com/example/myapplication/features/tenant/TenantDashboardActivity.java`
  - Khai báo và khởi tạo Views
  - Xử lý sự kiện click với Toast
  - Các method cập nhật dữ liệu (updateDebtAmount, updateDaysUntilDue, updateRoomStatus)

### 2. Colors Pastel
- [x] **colors.xml:** Thêm 4 màu Pastel
  ```xml
  <color name="pastel_purple">#9C7BE8</color>
  <color name="pastel_pink">#F47BA8</color>
  <color name="pastel_cyan">#4DD6E8</color>
  <color name="pastel_green">#4FD9A0</color>
  ```

### 3. AndroidManifest
- [x] Đăng ký `TenantDashboardActivity` trong manifest

### 4. Documentation
- [x] `TENANT_DASHBOARD_README.md` - Hướng dẫn sử dụng TenantDashboardActivity
- [x] `docs/fix-tenant-routing.md` - Giải thích vấn đề và giải pháp routing

---

## Files đã sửa đổi

### 1. MainActivity.java ✅
**Thay đổi:**
- [x] Thêm import `TenantDashboardActivity` và `FirebaseFirestore`
- [x] Thêm biến `FirebaseFirestore db`
- [x] Sửa `onStart()`: Gọi `checkUserRoleAndNavigate()` thay vì chuyển thẳng vào HomeMenuActivity
- [x] Sửa logic đăng nhập: Gọi `checkUserRoleAndNavigate()` sau khi login thành công
- [x] Thêm method mới: `checkUserRoleAndNavigate(String uid)`
  - Đọc `primaryRole` từ Firestore
  - Nếu `primaryRole = "TENANT"` → TenantDashboardActivity
  - Nếu khác → HomeMenuActivity

**Code mới:**
```java
private void checkUserRoleAndNavigate(String uid) {
    db.collection("users").document(uid)
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String primaryRole = doc.getString("primaryRole");
                    if ("TENANT".equals(primaryRole)) {
                        startActivity(new Intent(this, TenantDashboardActivity.class));
                    } else {
                        startActivity(new Intent(this, HomeMenuActivity.class));
                    }
                } else {
                    startActivity(new Intent(this, HomeMenuActivity.class));
                }
                finish();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Không thể tải thông tin người dùng", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, HomeMenuActivity.class));
                finish();
            });
}
```

### 2. SignUpActivity.java ✅
**Thay đổi:**
- [x] Thêm import `TenantDashboardActivity`
- [x] Sửa logic sau khi đăng ký thành công:
  - Thay `startActivity(new Intent(this, HomeMenuActivity.class))`
  - Thành `startActivity(new Intent(this, TenantDashboardActivity.class))`

**Lý do:** Vì đăng ký mặc định là TENANT, nên phải chuyển thẳng đến TenantDashboardActivity

---

## Luồng hoạt động mới

### 📱 Đăng ký tài khoản mới
```
User điền form đăng ký
     ↓
Firebase Auth tạo tài khoản
     ↓
Lưu Firestore: primaryRole = "TENANT"
     ↓
✅ Chuyển đến TenantDashboardActivity
```

### 🔐 Đăng nhập
```
User nhập email/password
     ↓
Firebase Auth xác thực
     ↓
Query Firestore: lấy primaryRole
     ↓
  ┌─────────────┐
  │ primaryRole?│
  └─────────────┘
       ↓
  ┌────────┬────────┐
  │ TENANT │ OWNER  │
  └────────┴────────┘
     ↓           ↓
TenantDashboard HomeMenu
   Activity     Activity
```

### 🚀 Mở app (đã login)
```
onStart() kiểm tra current user
     ↓
Gọi checkUserRoleAndNavigate()
     ↓
Query Firestore: lấy primaryRole
     ↓
Chuyển đến màn hình phù hợp với role
```

---

## Testing Checklist

### Test Case 1: Đăng ký tài khoản mới
- [ ] Mở app
- [ ] Click "Đăng ký"
- [ ] Điền: Họ tên, Email, Số điện thoại, Mật khẩu
- [ ] Click "Đăng ký"
- [ ] **✅ Kết quả:** Hiển thị TenantDashboardActivity với:
  - Header xanh dương
  - 3 chỉ số: Số dư nợ (0), Ngày đến hạn (5), Trạng thái (Đang ở)
  - 4 CardView màu Pastel
- [ ] Click vào từng Card → Hiển thị Toast tương ứng

### Test Case 2: Đăng nhập tài khoản TENANT
- [ ] Logout khỏi tài khoản hiện tại (nếu có)
- [ ] Nhập email/password của tài khoản TENANT
- [ ] Click "Đăng nhập"
- [ ] **✅ Kết quả:** Vào TenantDashboardActivity

### Test Case 3: Đăng nhập tài khoản OWNER
- [ ] Logout khỏi tài khoản hiện tại
- [ ] Nhập email/password của tài khoản OWNER
- [ ] Click "Đăng nhập"
- [ ] **✅ Kết quả:** Vào HomeMenuActivity (màn hình Admin)

### Test Case 4: Reopen app (TENANT logged in)
- [ ] Đăng nhập với tài khoản TENANT
- [ ] Close app (không logout)
- [ ] Mở lại app
- [ ] **✅ Kết quả:** Tự động vào TenantDashboardActivity

### Test Case 5: Reopen app (OWNER logged in)
- [ ] Đăng nhập với tài khoản OWNER
- [ ] Close app (không logout)
- [ ] Mở lại app
- [ ] **✅ Kết quả:** Tự động vào HomeMenuActivity

### Test Case 6: Click các chức năng trong TenantDashboard
- [ ] Đăng nhập với tài khoản TENANT
- [ ] Click "Phòng của bạn" → Toast: "Xem thông tin phòng của bạn"
- [ ] Click "Hóa đơn tháng" → Toast: "Xem hóa đơn tháng"
- [ ] Click "Báo cáo sự cố" → Toast: "Báo cáo sự cố"
- [ ] Click "Thông báo trọ" → Toast: "Xem thông báo trọ"
- [ ] Click nút Profile (góc trên phải) → Toast: "Mở trang cá nhân"

---

## Build & Deploy

### Bước 1: Sync Gradle
```bash
File > Sync Project with Gradle Files
```

### Bước 2: Build Project
```bash
Build > Make Project
```

### Bước 3: Run on Device/Emulator
```bash
Run > Run 'app'
```

### Nếu gặp lỗi "SDK location not found":
1. Tạo file `local.properties` ở thư mục root project
2. Thêm dòng: `sdk.dir=C\:\\Users\\YOUR_USERNAME\\AppData\\Local\\Android\\Sdk`
   (Thay YOUR_USERNAME bằng tên người dùng Windows của bạn)

---

## Firestore Structure

### Collection: users/{uid}
```json
{
  "hoTen": "Nguyễn Văn A",
  "email": "user@example.com",
  "soDienThoai": "0987654321",
  "uid": "firebase_uid_here",
  "primaryRole": "TENANT",  ← Quan trọng: Xác định role
  "activeTenantId": null,
  "createdAt": Timestamp,
  "updatedAt": Timestamp
}
```

### Các giá trị hợp lệ cho primaryRole:
- **"TENANT"** → TenantDashboardActivity
- **"OWNER"** → HomeMenuActivity
- **"STAFF"** → HomeMenuActivity

---

## Lưu ý quan trọng

### 🔒 Firestore Security Rules
Đảm bảo người dùng có quyền đọc document của chính họ:
```javascript
match /users/{userId} {
  allow read: if request.auth.uid == userId;
  allow write: if request.auth.uid == userId;
}
```

### ⚡ Performance
- Logic kiểm tra role chỉ query Firestore 1 lần khi login/start
- Firestore cache sẽ giúp app hoạt động khi offline

### 🎨 Customization
- Màu sắc: Sửa trong `colors.xml`
- Icons: Thay `android:src` trong layout
- Text: Nên đưa vào `strings.xml` để hỗ trợ đa ngôn ngữ

---

## Các bước tiếp theo (Optional)

- [ ] Tích hợp Firebase Authentication email verification
- [ ] Thêm loading indicator khi check role
- [ ] Cache `primaryRole` vào SharedPreferences để giảm query
- [ ] Implement các Activity chi tiết:
  - [ ] RoomDetailsActivity (Xem phòng của tenant)
  - [ ] TenantInvoiceActivity (Xem hóa đơn)
  - [ ] ReportIssueActivity (Báo cáo sự cố)
  - [ ] NotificationActivity (Thông báo)
- [ ] Thêm biểu đồ/thống kê trong TenantDashboard
- [ ] Push notification khi có hóa đơn mới
- [ ] Offline mode với Room Database

---

## 📞 Support

Nếu gặp vấn đề, kiểm tra:
1. Firebase đã được cấu hình đúng chưa (`google-services.json`)
2. Firestore rules có cho phép đọc collection `users` không
3. AndroidManifest.xml đã đăng ký TenantDashboardActivity chưa
4. Internet connection có hoạt động không

---

✅ **Hoàn thành:** Vấn đề routing theo role đã được sửa thành công!
