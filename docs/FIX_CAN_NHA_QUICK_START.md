# Quick Start: Fix `can_nha` vs `khu_tro` Issue

Vấn đề: Firestore rules dùng `khu_tro`, nhưng code Java dùng `can_nha`. Cần thống nhất.

## 5 Phút Setup + Check

### 1. Setup scripts
```bash
cd scripts
npm init -y
npm install firebase-admin
```

### 2. Download `serviceAccountKey.json`
- Firebase Console → [Project Settings](https://console.firebase.google.com)
- Service Accounts tab → Generate New Private Key
- Save vào `scripts/serviceAccountKey.json`

### 3. Audit dữ liệu hiện tại
```bash
node audit-firestore.js
```

**Xuất hiện `can_nha` trong output?**
- ✓ Có → Có dữ liệu, cần migrate
- ✗ Không → Có thể là dữ liệu chưa được tạo, hoặc chỉ có `khu_tro`

## Nếu Tìm Thấy `can_nha`

### Plan A: Migrate `can_nha` → `khu_tro` (nên chọn)

**Bước 1: Review kế hoạch**
```bash
node migrate-can_nha-to-khu_tro.js <tenantId>
```

**Bước 2: Backup dữ liệu**
- Sử dụng màn hình "Sao lưu & phục hồi" trong app, hoặc
- Export từ Firestore Console

**Bước 3: Chạy migration**
```bash
node migrate-can_nha-to-khu_tro.js <tenantId> --execute
```

**Bước 4: Verify**
```bash
node audit-firestore.js <tenantId>
# Kiểm tra: can_nha và khu_tro đều có data
```

**Bước 5: Update code Java**
```bash
# Find all: HouseRepository.java, BackupRestoreActivity.java, etc.
# Replace: "can_nha" → "khu_tro"
grep -r "can_nha" app/src/main/java/
```

**Bước 6: Build & test**
```bash
./gradlew.bat clean build
# Test quản lý căn nhà / khu trọ
```

**Bước 7: Cleanup**
```bash
node delete-can_nha.js <tenantId> --execute
```

### Plan B: Rename `khu_tro` → `can_nha` trong rules (không nên)
- Chỉnh sửa `firestore.rules`
- Rebuild & redeploy rules
- Ít an toàn hơn so với Plan A

## Kết Quả Mong Đợi

**Trước:**
```
Rules:       collection "khu_tro" { ... }
Code Java:   COLLECTION = "can_nha"
           ⚠️ KHÔNG ĐỒNG BỘ
```

**Sau:**
```
Rules:       collection "khu_tro" { ... }
Code Java:   COLLECTION = "khu_tro"
           ✓ ĐỒNG BỘ
```

## Một Số Câu Hỏi

**Q: Auditing có an toàn không?**
A: Hoàn toàn. Chỉ đọc dữ liệu, không ghi.

**Q: Nếu code fail giữa migration?**
A: Dữ liệu trong `can_nha` vẫn an toàn. Chạy lại migration.

**Q: Firestore có backup không?**
A: Có, sử dụng managed backup trong Firebase Console. Ngoài ra nên backup thủ công.

## Reference

- Scripts: [scripts/README.md](../scripts/README.md)
- Issue tracker: [docs/OPEN_ISSUES.md](../OPEN_ISSUES.md#high)
- Data contract: [docs/FIRESTORE_DATA_CONTRACT.md](../FIRESTORE_DATA_CONTRACT.md)
