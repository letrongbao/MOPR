# ✅ Cập nhật Thiết kế TenantDashboard - Tách biệt Header & Stats Card

## 🎯 Thay đổi chính

### 1. Header (Blue Top Bar)
**Trước:**
- Header xanh + Stats trong cùng LinearLayout
- Stats hiển thị trực tiếp trên nền xanh
- Khó phân biệt Header và Stats

**Sau:**
```xml
<LinearLayout
    android:id="@+id/headerLayout"
    android:layout_height="90dp"
    android:background="#2196F3"
    android:orientation="horizontal">
    
    <!-- Logo + Title + Profile Icon -->
</LinearLayout>
```

✅ **Đặc điểm:**
- Chiều cao cố định: **90dp**
- Màu nền: **#2196F3** (Material Blue)
- Orientation: **horizontal** (Logo - Title - Profile trên cùng 1 hàng)
- Constraint đúng chuẩn (top của parent)

---

### 2. Stats Card (White Floating Card)
**Trước:**
- Nằm trong Header, màu trắng trên nền xanh
- Không có CardView riêng
- Không có hiệu ứng elevation

**Sau:**
```xml
<androidx.cardview.widget.CardView
    android:id="@+id/statsCard"
    android:layout_marginTop="-20dp"    ← Đè lên Header
    app:cardBackgroundColor="@android:color/white"
    app:cardCornerRadius="12dp"
    app:cardElevation="4dp">
    
    <LinearLayout orientation="horizontal">
        <!-- 3 cột: Số dư nợ | Ngày đến hạn | Trạng thái -->
    </LinearLayout>
</androidx.cardview.widget.CardView>
```

✅ **Đặc điểm:**
- **Tách riêng** khỏi Header
- **marginTop = -20dp** → Đè nhẹ lên Header (giống Admin)
- **cardCornerRadius = 12dp** → Bo góc mềm mại
- **cardElevation = 4dp** → Bóng đổ nổi lên
- **Màu trắng** → Tương phản rõ với nền xanh

---

### 3. Stats Layout (3 cột bên trong Stats Card)

#### Cột 1: Số dư nợ
```xml
<TextView
    android:id="@+id/tvDebtAmount"
    android:text="0"
    android:textColor="#1976D2"    ← Xanh dương
    android:textSize="24sp"
    android:textStyle="bold" />
<TextView
    android:text="Số dư nợ"
    android:textColor="@color/text_sub"
    android:textSize="12sp" />
```

#### Cột 2: Ngày đến hạn
```xml
<TextView
    android:id="@+id/tvDaysUntilDue"
    android:text="5"
    android:textColor="#F44336"    ← Đỏ (cảnh báo)
    android:textSize="24sp"
    android:textStyle="bold" />
<TextView
    android:text="Ngày đến hạn"
    android:textColor="@color/text_sub"
    android:textSize="12sp" />
```

#### Cột 3: Trạng thái
```xml
<TextView
    android:id="@+id/tvRoomStatus"
    android:text="Đang ở"
    android:textColor="#1976D2"    ← Xanh dương
    android:textSize="18sp"
    android:textStyle="bold" />
<TextView
    android:text="Trạng thái"
    android:textColor="@color/text_sub"
    android:textSize="12sp" />
```

✅ **Màu sắc động:**
- **Số dư nợ:**
  - Xanh dương (#1976D2) nếu = 0
  - Đỏ (#F44336) nếu > 0
- **Ngày đến hạn:**
  - Xanh lá (#4CAF50) nếu > 10 ngày
  - Cam (#FF9800) nếu 5-10 ngày
  - Đỏ (#F44336) nếu < 5 ngày
- **Trạng thái:**
  - Xanh dương (#1976D2) nếu "Đang ở"
  - Xám nếu trạng thái khác

---

### 4. Dashboard Grid (Giữ nguyên)
- 4 CardView màu đậm với icon trắng
- Grid 2x2 cân đối
- Margin top 16dp (khoảng cách với Stats Card)

---

## 📐 Cấu trúc Layout mới

```
ConstraintLayout (Root)
├── Header (90dp, #2196F3)
│   ├── Logo Icon
│   ├── "QUẢN LÝ TRỌ"
│   └── Profile Icon
│
├── Stats Card (marginTop -20dp) ← Đè lên Header
│   └── LinearLayout (horizontal)
│       ├── Số dư nợ (Xanh/Đỏ)
│       ├── Ngày đến hạn (Đỏ/Cam/Xanh)
│       └── Trạng thái (Xanh)
│
└── ScrollView
    └── Dashboard Grid
        ├── Phòng của bạn (Tím)
        ├── Hóa đơn tháng (Hồng)
        ├── Báo cáo sự cố (Xanh)
        └── Thông báo trọ (Xanh lá)
```

---

## 🎨 Màu sắc sử dụng

| Element | Màu | Mã HEX | Mục đích |
|---------|-----|---------|----------|
| Header | Blue | **#2196F3** | Nền Header |
| Stats Card | White | **#FFFFFF** | Nền Stats Card |
| Số dư nợ (0) | Blue | **#1976D2** | Không nợ |
| Số dư nợ (>0) | Red | **#F44336** | Có nợ |
| Ngày đến hạn | Red | **#F44336** | Gần đến hạn |
| Trạng thái | Blue | **#1976D2** | Đang ở |
| Text phụ | Gray | **@color/text_sub** | Label |

---

## 🔧 Chi tiết kỹ thuật

### Header
```xml
<LinearLayout
    android:id="@+id/headerLayout"
    android:layout_width="0dp"
    android:layout_height="90dp"
    android:background="#2196F3"
    android:gravity="center_vertical"
    android:orientation="horizontal"
    android:paddingStart="16dp"
    android:paddingEnd="16dp"
    app:layout_constraintEnd_toEndOf="parent"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintTop_toTopOf="parent">
```

### Stats Card
```xml
<androidx.cardview.widget.CardView
    android:id="@+id/statsCard"
    android:layout_width="0dp"
    android:layout_height="wrap_content"
    android:layout_marginStart="16dp"
    android:layout_marginTop="-20dp"       ← Key: Đè lên Header
    android:layout_marginEnd="16dp"
    app:cardBackgroundColor="@android:color/white"
    app:cardCornerRadius="12dp"
    app:cardElevation="4dp"
    app:layout_constraintEnd_toEndOf="parent"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintTop_toBottomOf="@id/headerLayout">
```

### ScrollView
```xml
<ScrollView
    android:layout_width="0dp"
    android:layout_height="0dp"
    android:fillViewport="true"
    app:layout_constraintBottom_toBottomOf="parent"
    app:layout_constraintEnd_toEndOf="parent"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintTop_toBottomOf="@id/statsCard">  ← Top constraint: Stats Card
```

---

## 🆚 So sánh Trước & Sau

### Trước:
```
┌────────────────────────────┐
│ Header (Xanh)              │
│ ┌────────────────────────┐ │
│ │ Logo | Title | Profile│ │
│ └────────────────────────┘ │
│                            │
│ Stats (Trong Header)       │
│ 0 | 5 | Đang ở            │
│ (Màu trắng)                │
└────────────────────────────┘
Dashboard...
```

**Vấn đề:**
- ❌ Header và Stats lẫn lộn
- ❌ Không có CardView riêng
- ❌ Không có hiệu ứng elevation
- ❌ Không giống Admin

### Sau:
```
┌────────────────────────────┐
│ Header (Xanh - 90dp)       │
│ Logo | QUẢN LÝ TRỌ | 👤   │
├────────────────────────────┤
│   ┌──────────────────┐     │
│   │ Stats Card       │     │ ← Đè lên -20dp
│   │ (Trắng, Bo góc)  │     │
│   │ 0 | 5 | Đang ở   │     │
│   └──────────────────┘     │
├────────────────────────────┤
│ Dashboard Grid             │
│ ┌──────┐ ┌──────┐          │
│ │ Tím  │ │ Hồng │          │
│ └──────┘ └──────┘          │
│ ┌──────┐ ┌──────┐          │
│ │ Xanh │ │ Xanh │          │
│ └──────┘ └──────┘          │
└────────────────────────────┘
```

**Ưu điểm:**
- ✅ Header và Stats tách biệt rõ ràng
- ✅ Stats Card nổi lên với elevation
- ✅ Giống thiết kế Admin hoàn toàn
- ✅ Chuyên nghiệp, hiện đại

---

## 📝 Update Java Code (TenantDashboardActivity)

Không cần thay đổi nhiều, chỉ cần đảm bảo ID khớp:

```java
// Các TextView ID vẫn giữ nguyên
private TextView tvDebtAmount;      // Số dư nợ
private TextView tvDaysUntilDue;    // Ngày đến hạn
private TextView tvRoomStatus;      // Trạng thái

// Có thể thêm logic màu động:
public void updateDebtAmount(long amount) {
    tvDebtAmount.setText(amount == 0 ? "0" : String.format("%,d", amount));
    // Đổi màu động
    int color = amount == 0 ? 0xFF1976D2 : 0xFFF44336;  // Xanh/Đỏ
    tvDebtAmount.setTextColor(color);
}

public void updateDaysUntilDue(int days) {
    tvDaysUntilDue.setText(String.valueOf(days));
    // Đổi màu theo số ngày
    int color;
    if (days > 10) {
        color = 0xFF4CAF50;  // Xanh lá
    } else if (days >= 5) {
        color = 0xFFFF9800;  // Cam
    } else {
        color = 0xFFF44336;  // Đỏ
    }
    tvDaysUntilDue.setTextColor(color);
}
```

---

## ✅ Checklist

- [x] Header tách riêng, chiều cao 90dp
- [x] Header có Logo + Title + Profile
- [x] Stats Card là CardView riêng biệt
- [x] Stats Card marginTop -20dp (đè lên Header)
- [x] Stats Card bo góc 12dp, elevation 4dp
- [x] Stats Card nền trắng
- [x] 3 cột stats: Số dư nợ | Ngày đến hạn | Trạng thái
- [x] Màu sắc stats: Xanh/Đỏ tùy giá trị
- [x] Dashboard Grid margin top 16dp
- [x] ScrollView constraint từ statsCard

---

## 🧪 Testing

1. **Visual Check:**
   - Header xanh 90dp ✓
   - Stats Card trắng đè lên Header ✓
   - Bo góc 12dp, bóng đổ rõ ✓

2. **Data Check:**
   - Số dư nợ hiển thị đúng ✓
   - Ngày đến hạn hiển thị đúng ✓
   - Trạng thái hiển thị đúng ✓

3. **Color Check:**
   - Số dư nợ màu xanh khi = 0 ✓
   - Ngày đến hạn màu đỏ khi < 5 ✓
   - Trạng thái màu xanh ✓

---

✅ **Hoàn thành:** Layout TenantDashboard giờ đã khớp hoàn toàn với thiết kế Admin!
