# TenantDashboardActivity - Hướng dẫn sử dụng

## Tổng quan
TenantDashboardActivity là màn hình chính dành cho người thuê trọ với thiết kế tương tự Admin Dashboard nhưng tối ưu cho nhu cầu người thuê.

## Các file đã tạo

### 1. Layout XML
**File:** `app/src/main/res/layout/activity_tenant_dashboard.xml`

**Cấu trúc:**
- ConstraintLayout làm layout gốc
- Header màu xanh dương (#1E88E5) với logo và nút profile
- 3 chỉ số thống kê:
  - Số dư nợ (VNĐ)
  - Ngày đến hạn (số ngày)
  - Trạng thái phòng (text)
- 4 CardView với Grid 2 cột:
  - Phòng của bạn (Pastel Purple #9C7BE8)
  - Hóa đơn tháng (Pastel Pink #F47BA8)
  - Báo cáo sự cố (Pastel Cyan #4DD6E8)
  - Thông báo trọ (Pastel Green #4FD9A0)

**Đặc điểm:**
- CardView bo góc 16dp
- Hiệu ứng `android:foreground="?attr/selectableItemBackground"` khi chạm
- ScrollView để hỗ trợ màn hình nhỏ
- GridLayout 2 cột với rowWeight và columnWeight tự động điều chỉnh kích thước

### 2. Java Activity
**File:** `app/src/main/java/com/example/myapplication/features/tenant/TenantDashboardActivity.java`

**Chức năng:**
- Khai báo và khởi tạo tất cả Views
- Xử lý sự kiện click với Toast thông báo
- Các phương thức cập nhật dữ liệu:
  - `updateDebtAmount(long amount)` - Cập nhật số dư nợ
  - `updateDaysUntilDue(int days)` - Cập nhật số ngày đến hạn
  - `updateRoomStatus(String status)` - Cập nhật trạng thái phòng

### 3. Colors XML
**File:** `app/src/main/res/values/colors.xml`

**Màu Pastel đã thêm:**
```xml
<color name="pastel_purple">#9C7BE8</color>
<color name="pastel_pink">#F47BA8</color>
<color name="pastel_cyan">#4DD6E8</color>
<color name="pastel_green">#4FD9A0</color>
```

### 4. AndroidManifest.xml
Đã đăng ký TenantDashboardActivity trong manifest.

## Cách sử dụng

### Mở màn hình từ Activity khác:
```java
Intent intent = new Intent(CurrentActivity.this, TenantDashboardActivity.class);
startActivity(intent);
```

### Cập nhật dữ liệu trong TenantDashboardActivity:
```java
// Cập nhật số dư nợ
updateDebtAmount(500000); // 500,000 VNĐ

// Cập nhật số ngày đến hạn
updateDaysUntilDue(5); // Còn 5 ngày

// Cập nhật trạng thái phòng
updateRoomStatus("Đang ở");
```

## Tích hợp với Firebase/Database

### Ví dụ load dữ liệu từ Firebase:
```java
private void loadDashboardData() {
    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    DatabaseReference ref = FirebaseDatabase.getInstance()
        .getReference("tenants").child(userId);
    
    ref.addValueEventListener(new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot snapshot) {
            if (snapshot.exists()) {
                // Load số dư nợ
                Long debtAmount = snapshot.child("debtAmount").getValue(Long.class);
                if (debtAmount != null) {
                    updateDebtAmount(debtAmount);
                }
                
                // Load ngày đến hạn
                Long dueDate = snapshot.child("dueDate").getValue(Long.class);
                if (dueDate != null) {
                    int daysRemaining = calculateDaysRemaining(dueDate);
                    updateDaysUntilDue(daysRemaining);
                }
                
                // Load trạng thái phòng
                String status = snapshot.child("roomStatus").getValue(String.class);
                if (status != null) {
                    updateRoomStatus(status);
                }
            }
        }
        
        @Override
        public void onCancelled(DatabaseError error) {
            Toast.makeText(TenantDashboardActivity.this, 
                "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
        }
    });
}

private int calculateDaysRemaining(long dueDateMillis) {
    long currentTime = System.currentTimeMillis();
    long difference = dueDateMillis - currentTime;
    return (int) (difference / (1000 * 60 * 60 * 24));
}
```

## Xử lý sự kiện Click

### Mở Activity chi tiết khi click vào Card:
```java
private void setupClickListeners() {
    // Card 1: Phòng của bạn
    cardMyRoom.setOnClickListener(v -> {
        Intent intent = new Intent(this, RoomDetailsActivity.class);
        startActivity(intent);
    });

    // Card 2: Hóa đơn tháng
    cardInvoice.setOnClickListener(v -> {
        Intent intent = new Intent(this, TenantInvoiceActivity.class);
        startActivity(intent);
    });

    // Card 3: Báo cáo sự cố
    cardReport.setOnClickListener(v -> {
        Intent intent = new Intent(this, ReportIssueActivity.class);
        startActivity(intent);
    });

    // Card 4: Thông báo trọ
    cardNotifications.setOnClickListener(v -> {
        Intent intent = new Intent(this, NotificationActivity.class);
        startActivity(intent);
    });
}
```

## Tùy chỉnh giao diện

### Thay đổi màu sắc:
Chỉnh sửa file `colors.xml` để thay đổi màu của các CardView:
```xml
<color name="pastel_purple">#YOUR_COLOR</color>
<color name="pastel_pink">#YOUR_COLOR</color>
<color name="pastel_cyan">#YOUR_COLOR</color>
<color name="pastel_green">#YOUR_COLOR</color>
```

### Thay đổi icon:
Trong file `activity_tenant_dashboard.xml`, thay đổi thuộc tính `android:src` của ImageView:
```xml
<ImageView
    android:src="@drawable/your_custom_icon"
    ... />
```

### Thay đổi kích thước CardView:
```xml
<androidx.cardview.widget.CardView
    android:layout_height="140dp"  <!-- Thay đổi chiều cao -->
    app:cardCornerRadius="16dp"    <!-- Thay đổi độ bo góc -->
    ... />
```

## Build và chạy

1. Sync project với Gradle:
   - File > Sync Project with Gradle Files

2. Build project:
   - Build > Make Project

3. Chạy ứng dụng:
   - Run > Run 'app'

## Lưu ý quan trọng

1. **Icons mặc định:** Hiện tại sử dụng Android system icons. Nên thay thế bằng Material Icons hoặc custom icons.

2. **Data binding:** Có thể sử dụng ViewModel và LiveData để quản lý dữ liệu tốt hơn.

3. **Localization:** Nên đưa các text string vào `strings.xml` để hỗ trợ đa ngôn ngữ.

4. **Dependencies:** Đảm bảo đã thêm CardView dependency trong `build.gradle`:
   ```gradle
   implementation 'androidx.cardview:cardview:1.0.0'
   ```

5. **Testing:** Nên viết Unit tests và UI tests cho Activity này.

## Các bước tiếp theo

1. Tạo các Activity chi tiết cho từng chức năng:
   - RoomDetailsActivity
   - TenantInvoiceActivity
   - ReportIssueActivity
   - NotificationActivity

2. Tích hợp Firebase Authentication và Database

3. Thêm loading state và error handling

4. Implement offline mode với Room Database

5. Thêm analytics để theo dõi hành vi người dùng

## Support

Nếu cần hỗ trợ hoặc có câu hỏi, vui lòng liên hệ team phát triển.
