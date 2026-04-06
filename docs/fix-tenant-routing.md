# Sửa lỗi Routing theo Role khi Đăng nhập/Đăng ký

## Vấn đề ban đầu

Khi người dùng đăng ký tài khoản mới với role mặc định là `TENANT` (khách thuê), sau khi đăng nhập hệ thống không chuyển đến màn hình dành cho khách thuê mà vào màn hình Admin (HomeMenuActivity).

### Nguyên nhân

1. **SignUpActivity.java (dòng 94):**
   - Khi đăng ký, hệ thống set `primaryRole = "TENANT"` vào Firestore
   - Nhưng sau đó lại chuyển thẳng vào `HomeMenuActivity` mà không kiểm tra role

2. **MainActivity.java:**
   - Khi đăng nhập, cũng chuyển thẳng vào `HomeMenuActivity` mà không kiểm tra role
   - `onStart()` cũng chỉ kiểm tra user đã đăng nhập chưa, không kiểm tra role

3. **HomeMenuActivity.java (dòng 349-371):**
   - Logic phân quyền dựa vào `TenantSession.getActiveTenantId()`
   - User mới đăng ký chưa có `activeTenantId`, nên mặc định hiển thị giao diện OWNER

## Giải pháp đã áp dụng

### 1. Sửa MainActivity.java

**Thêm import:**
```java
import com.example.myapplication.features.tenant.TenantDashboardActivity;
import com.google.firebase.firestore.FirebaseFirestore;
```

**Thêm FirebaseFirestore:**
```java
private FirebaseFirestore db;

@Override
protected void onCreate(Bundle savedInstanceState) {
    // ...
    db = FirebaseFirestore.getInstance();
    // ...
}
```

**Sửa onStart():**
```java
@Override
protected void onStart() {
    super.onStart();
    // Nếu đã đăng nhập rồi thì kiểm tra role và chuyển màn hình phù hợp
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    if (currentUser != null) {
        checkUserRoleAndNavigate(currentUser.getUid());
    }
}
```

**Sửa logic đăng nhập:**
```java
mAuth.signInWithEmailAndPassword(email, pass)
    .addOnSuccessListener(result -> {
        // Lưu SharedPreferences...
        
        // Kiểm tra role và chuyển màn hình phù hợp
        String uid = result.getUser().getUid();
        checkUserRoleAndNavigate(uid);
    })
    // ...
```

**Thêm method kiểm tra role:**
```java
/**
 * Kiểm tra role của user và chuyển đến màn hình phù hợp
 */
private void checkUserRoleAndNavigate(String uid) {
    db.collection("users").document(uid)
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String primaryRole = doc.getString("primaryRole");
                    
                    // Nếu là TENANT thì chuyển đến TenantDashboardActivity
                    if ("TENANT".equals(primaryRole)) {
                        startActivity(new Intent(this, TenantDashboardActivity.class));
                    } else {
                        // Mặc định chuyển đến HomeMenuActivity (cho OWNER, STAFF)
                        startActivity(new Intent(this, HomeMenuActivity.class));
                    }
                } else {
                    // Nếu không tìm thấy document, mặc định vào HomeMenuActivity
                    startActivity(new Intent(this, HomeMenuActivity.class));
                }
                finish();
            })
            .addOnFailureListener(e -> {
                // Nếu lỗi, mặc định vào HomeMenuActivity
                Toast.makeText(this, "Không thể tải thông tin người dùng", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, HomeMenuActivity.class));
                finish();
            });
}
```

### 2. Sửa SignUpActivity.java

**Thêm import:**
```java
import com.example.myapplication.features.tenant.TenantDashboardActivity;
```

**Sửa logic sau khi đăng ký thành công:**
```java
db.collection("users").document(uid)
        .set(user)
        .addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
            // Chuyển đến TenantDashboardActivity vì đăng ký mặc định là TENANT
            startActivity(new Intent(this, TenantDashboardActivity.class));
            finish();
        })
        // ...
```

## Luồng hoạt động sau khi sửa

### Khi Đăng ký:
1. User điền thông tin đăng ký
2. Hệ thống tạo tài khoản Firebase Auth
3. Lưu thông tin vào Firestore với `primaryRole = "TENANT"`
4. **Chuyển thẳng đến TenantDashboardActivity** ✅

### Khi Đăng nhập:
1. User nhập email/password
2. Firebase Auth xác thực
3. **Lấy `primaryRole` từ Firestore**
4. Nếu `primaryRole = "TENANT"` → Chuyển đến **TenantDashboardActivity** ✅
5. Nếu `primaryRole = "OWNER"` hoặc khác → Chuyển đến **HomeMenuActivity** ✅

### Khi mở app (đã đăng nhập):
1. `onStart()` kiểm tra có user đã login không
2. Nếu có → **Gọi `checkUserRoleAndNavigate()`**
3. Kiểm tra `primaryRole` và chuyển đúng màn hình ✅

## Cấu trúc Firestore

### Collection: users/{uid}
```json
{
  "hoTen": "Nguyễn Văn A",
  "email": "user@example.com",
  "soDienThoai": "0987654321",
  "uid": "firebase_uid_here",
  "primaryRole": "TENANT",  // Hoặc "OWNER", "STAFF"
  "activeTenantId": null,
  "createdAt": Timestamp,
  "updatedAt": Timestamp
}
```

## Các Role trong hệ thống

Theo file `TenantRoles.java`:
- **OWNER**: Chủ nhà trọ (vào HomeMenuActivity)
- **STAFF**: Nhân viên (vào HomeMenuActivity)
- **TENANT**: Khách thuê (vào TenantDashboardActivity)

## Testing

### Test case 1: Đăng ký tài khoản mới
1. Mở app → Click "Đăng ký"
2. Điền thông tin đầy đủ
3. Click "Đăng ký"
4. **Kết quả mong đợi:** Chuyển đến TenantDashboardActivity với màn hình khách thuê

### Test case 2: Đăng nhập với tài khoản TENANT
1. Mở app
2. Nhập email/password của tài khoản TENANT
3. Click "Đăng nhập"
4. **Kết quả mong đợi:** Chuyển đến TenantDashboardActivity

### Test case 3: Đăng nhập với tài khoản OWNER
1. Mở app
2. Nhập email/password của tài khoản OWNER
3. Click "Đăng nhập"
4. **Kết quả mong đợi:** Chuyển đến HomeMenuActivity

### Test case 4: Mở app khi đã đăng nhập (TENANT)
1. Đăng nhập với tài khoản TENANT
2. Thoát app (không logout)
3. Mở lại app
4. **Kết quả mong đợi:** Tự động vào TenantDashboardActivity

### Test case 5: Mở app khi đã đăng nhập (OWNER)
1. Đăng nhập với tài khoản OWNER
2. Thoát app (không logout)
3. Mở lại app
4. **Kết quả mong đợi:** Tự động vào HomeMenuActivity

## Lưu ý quan trọng

1. **Firestore Rules:** Đảm bảo người dùng có quyền đọc document của chính họ trong collection `users`

2. **Error Handling:** Nếu không load được thông tin từ Firestore, hệ thống mặc định chuyển đến HomeMenuActivity

3. **Offline Mode:** Firestore cache sẽ giúp app vẫn hoạt động khi offline

4. **Performance:** Logic kiểm tra role chỉ cần 1 lần query Firestore mỗi khi login/start app

## Tương lai mở rộng

Có thể cải thiện thêm:
1. Cache `primaryRole` vào SharedPreferences để giảm query Firestore
2. Thêm loading indicator khi check role
3. Hỗ trợ nhiều role hơn (ADMIN, MANAGER, etc.)
4. Cho phép user chuyển đổi giữa các role nếu có nhiều role

## Files đã sửa đổi

1. ✅ `app/src/main/java/com/example/myapplication/features/auth/MainActivity.java`
2. ✅ `app/src/main/java/com/example/myapplication/features/auth/SignUpActivity.java`
3. ✅ `app/src/main/AndroidManifest.xml` (đã đăng ký TenantDashboardActivity)
