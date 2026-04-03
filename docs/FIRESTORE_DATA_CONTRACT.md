# Firestore Data Contract (MOPR)

## Muc tieu
Tai lieu nay la nguon su that (single source of truth) de cac agent/man hinh moi dung chung du lieu Firestore voi cac man hien co (dac biet la Quan ly phong, Hoa don, Thanh toan).

## 1) Scope luu du lieu hien tai

App dang ho tro 2 scope du lieu:

1. Legacy per-user:
   - users/{uid}/{collection}/{docId}
2. Multi-tenant (uu tien hien tai):
   - tenants/{tenantId}/{collection}/{docId}

Quy tac scope trong code:

- Neu co active tenant (`TenantSession.getActiveTenantId()`): doc/ghi vao `tenants/{tenantId}/...`
- Neu chua co tenant: fallback `users/{uid}/...`

Pattern nay duoc dung nhat quan trong repository:

- `RoomRepository`
- `TenantRepository`
- `InvoiceRepository`
- `PaymentRepository`
- `ExpenseRepository`
- `MeterReadingRepository`
- `HouseRepository`
- `TicketRepository`
- `RentalHistoryRepository`

## 2) Collections dang duoc app su dung

Trong scope `tenants/{tenantId}` (va fallback `users/{uid}`), app dang dung cac collection sau:

1. `can_nha`
2. `phong_tro`
3. `nguoi_thue`
4. `hoa_don`
5. `payments`
6. `meterReadings`
7. `chi_phi`
8. `tickets`
9. `rental_history`

Ngoai ra o root tenant:

1. `tenants/{tenantId}` (thong tin to chuc)
2. `tenants/{tenantId}/members` (OWNER/STAFF/TENANT)
3. `tenants/{tenantId}/invites`
4. `tenants/{tenantId}/auditLogs`
5. `tenants/{tenantId}/backups` (sao luu app)

## 3) Schema cot loi cho Quan ly phong + Hoa don

### 3.1 `phong_tro/{roomId}`

Nguon chinh: `domain/Room.java`

Field chinh:

- `soPhong`: String
- `loaiPhong`: String (`"Đơn"`, `"Đôi"`, `"Ghép"`)
- `dienTich`: Number
- `giaThue`: Number
- `trangThai`: String (`"Trống"`, `"Đã thuê"`)
- `hinhAnh`: String (URL)
- `canNhaId`: String
- `canNhaTen`: String
- `tang`: Number
- `moTa`: String
- `tienNghi`: Array<String>
- `soNguoiToiDa`: Number
- `createdAt`: Timestamp
- `updatedAt`: Timestamp

### 3.2 `nguoi_thue/{tenantDocId}`

Nguon chinh: `domain/Tenant.java`

Field lien ket phong/hop dong:

- `idPhong`: String (FK -> `phong_tro/{roomId}`)
- `idPhongCu`: String
- `soPhong`: String (denormalized)
- `trangThaiHopDong`: String (`ACTIVE`/`ENDED`)
- `createdAt`, `updatedAt`, `endedAt`: Int64 millis

Field thong tin nguoi thue + hop dong:

- `hoTen`, `cccd`, `soDienThoai`, `diaChi`, `soHopDong`
- `giaThue`, `tienCoc`, `ngayKetThuc`: Int64
- Legacy compatibility: `tienPhong`, `tienCoc_old`: Number
- Flags: `trangThaiThuCoc`, `hienThiTienCocTrenHoaDon`, `hienThiGhiChuTrenHoaDon`
- Dich vu: `dichVuGuiXe`, `soLuongXe`, `dichVuInternet`, `dichVuGiatSay`

### 3.3 `hoa_don/{invoiceId}`

Nguon chinh: `domain/Invoice.java`

Field chinh:

- `idPhong`: String (FK -> `phong_tro/{roomId}`)
- `idTenant`: String (FK -> `nguoi_thue/{tenantDocId}`)
- `soPhong`: String (denormalized de hien thi)
- `thangNam`: String (`MM/yyyy`, vi du `03/2026`)
- Dien/nuoc: `chiSoDienDau`, `chiSoDienCuoi`, `donGiaDien`, `chiSoNuocDau`, `chiSoNuocCuoi`, `donGiaNuoc`
- Phi: `phiRac`, `phiWifi`, `phiGuiXe`
- `giaThue`, `tongTien`: Number
- `trangThai`: String (`Chưa báo`, `Đã báo`, `Đóng một phần`, `Đã đóng`)
- `ngayThanhToan`: Timestamp
- `phuongThucThanhToan`: String
- `soTienDaThanhToan`: Number
- `createdAt`, `updatedAt`: Timestamp

Rule tao ID chong trung hoa don theo phong-thang (dang duoc ho tro):

- `invoiceId = idPhong + "_" + yyyyMM`
- Vi du: `roomA_202603`

### 3.4 `payments/{paymentId}`

Nguon chinh: `domain/Payment.java`

Field chinh:

- `invoiceId`: String (FK -> `hoa_don/{invoiceId}`)
- `roomId`: String (ho tro phan quyen tenant-level)
- `amount`: Number
- `method`: String (`CASH`/`BANK`)
- `paidAt`: String (`dd/MM/yyyy`)
- `note`: String

Khi tong payment thay doi, man lich su thanh toan cap nhat `hoa_don.trangThai`:

- 0 dong -> `Đã báo`
- 0 < paid < tongTien -> `Đóng một phần`
- paid >= tongTien -> `Đã đóng`

## 4) Quan he du lieu lien trang (de man khac doc lai)

1. Phong -> Nguoi thue:
   - `nguoi_thue.idPhong == phong_tro.docId`
2. Hoa don -> Phong:
   - `hoa_don.idPhong == phong_tro.docId`
3. Hoa don -> Nguoi thue:
   - `hoa_don.idTenant == nguoi_thue.docId`
4. Thanh toan -> Hoa don:
   - `payments.invoiceId == hoa_don.docId`

Khi lam man moi, uu tien query theo `idPhong`, `invoiceId`, `idTenant` de bao toan dong bo voi man Hoa don hien tai.

## 5) Cach doc du lieu giong man Hoa don

### 5.1 Pattern scope chung (khuyen nghi dung lai)

```java
private CollectionReference scopedCollection(String collection) {
    String tenantId = TenantSession.getActiveTenantId();
    if (tenantId != null && !tenantId.trim().isEmpty()) {
        return FirebaseFirestore.getInstance()
                .collection("tenants").document(tenantId).collection(collection);
    }

    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    if (user == null) throw new IllegalStateException("User not logged in");
    return FirebaseFirestore.getInstance()
            .collection("users").document(user.getUid()).collection(collection);
}
```

### 5.2 Vi du doc phong + nguoi thue active + hoa don

```java
scopedCollection("phong_tro").get();

scopedCollection("nguoi_thue")
    .whereEqualTo("trangThaiHopDong", "ACTIVE")
    .get();

scopedCollection("hoa_don")
    .whereEqualTo("idPhong", roomId)
    .get();
```

### 5.3 Vi du doc lich su thanh toan cua 1 hoa don

```java
scopedCollection("payments")
    .whereEqualTo("invoiceId", invoiceId)
    .get();
```

## 6) Firestore rules lien quan

File: `firestore.rules`

Nhan su trong tenant:

- `OWNER`, `STAFF`: doc/ghi phan lon collection nghiep vu
- `TENANT`: doc du lieu bi gioi han theo `roomId` cua membership

Co che an toan nay la ly do can luu `roomId` trong `payments`, `tickets`, `hoa_don`, ... de tenant user chi thay du lieu phong cua minh.

## 7) "Du lieu hien dang co" nen hieu the nao

Repo code KHONG chua snapshot live data tu Firebase production.

Do do, tai lieu nay ghi:

1. Cau truc du lieu dang duoc app su dung thuc te.
2. Danh sach collection dang duoc backup trong app.
3. Cac field + quan he ma man hinh dang doc/ghi.

Neu can inventory du lieu live (so ban ghi thuc te), su dung:

1. Firestore Console de xem document thuc.
2. Man `Sao lưu & phục hồi` trong app (collection `backups`) de dong bang state.
3. Tao them script audit thong ke record count theo collection (neu can, co the bo sung o dot tiep theo).
4. Doc them `docs/FIREBASE_DATA_MANAGEMENT_GUIDE.md` khi can seed/reset/test role.

## 8) Quy uoc cho agent khi lam man moi

1. Luon dung scope helper (`TenantSession` + fallback `users/{uid}`).
2. Khong tu y doi ten collection/field cu khi chua co ke hoach migration.
3. Uu tien doc theo FK da ton tai (`idPhong`, `idTenant`, `invoiceId`).
4. Neu can denormalize (vi du luu them `soPhong`), van giu FK goc.
5. Khi sua trang thai hoa don tu payment, cap nhat dung 4 gia tri trong `InvoiceStatus`.
6. Kiem tra quyen trong `firestore.rules` truoc khi them collection moi.

## 9) ⚠️ Lỗi không đồng bộ: khu_tro vs can_nha

**VẤN ĐỀ TÌM RA:**

- `firestore.rules` có 2 collection:
  - `khu_tro` (theo rules, dùng cho OWNER/STAFF, có `isTenantMember` read)
  - `phong_tro` (các căn/phòng riêng lẻ)
- Code Java (Repository + Backup) dùng:
  - `can_nha` (trong `HouseRepository`, `BackupRestoreActivity`)
  - `phong_tro` (đúng)

**KẾT LUẬN:**
- `can_nha` trong code = `khu_tro` trong rules (cùng là "khu/căn nhà")
- **CẦN SỬA**: Đổi `can_nha` → `khu_tro` hoặc ngược lại để thống nhất
- Migration plan nên là: tạo script batch copy `can_nha` → `khu_tro` trong Firestore, rồi xóa `can_nha`
- Hoặc cập nhật rules để dùng `can_nha` thay `khu_tro`

## 10) Kiểm tra dữ liệu live bằng audit script

Mình đã tạo script Node.js để thống kê collection thực tế:

```bash
cd scripts
npm install firebase-admin
# Download serviceAccountKey.json từ Firebase Console > Project Settings
node audit-firestore.js
# Hoặc audit tenant cụ thể:
node audit-firestore.js <tenantId>
```

Output sẽ show:
- Số bản ghi từng collection
- Tổng thống kê theo tenant
- Phát hiện **collection nào thực sự được dùng** (can_nha hay khu_tro?)

Ví dụ output:
```
👤 Căn nhà A (ID: tenant_abc123)
========================================
  ✓ can_nha              : 5 bản ghi
  ✓ phong_tro           : 25 bản ghi
  ✓ nguoi_thue          : 18 bản ghi
  ✓ hoa_don             : 142 bản ghi
  ✓ payments            : 87 bản ghi
  ○ meterReadings       : (trống)
  Tổng cộng: 277 bản ghi
```

## 11) Role-based flow (roadmap, chua rollout tenant)

Trang hoa don va thanh toan da co khung role TENANT/OWNER-STAFF, nhung hien tai dang chay theo chien luoc rollout:

1. Active hien tai: OWNER/STAFF flow (mac dinh)
2. TENANT self-service: da co khung code, tam thoi tat bang feature flag cho den khi role tenant chinh thuc hoan tat tren DB + UI.

Khi bat TENANT self-service, flow du kien la:

1. TENANT tren man hoa don:
   - Khong duoc xoa/sua/bao phi hoa don.
   - Nhan nut hanh dong theo self-service:
     - `Chờ chủ báo phí` neu hoa don `Chưa báo`.
     - `Thanh toán ngay` neu hoa don `Đã báo`/`Đóng một phần`.
     - `Xem lịch sử thanh toán` neu hoa don `Đã đóng`.
2. TENANT tren man lich su thanh toan:
   - Duoc them giao dich thanh toan.
   - Khong duoc xoa giao dich thanh toan.
3. OWNER/STAFF:
   - Giu luong quan ly hien tai.

Muc tieu cua batch nay la chuan bi huong self-service nhung khong anh huong flow owner hien tai.

## 12) Role va membership schema de chot huong phat trien

### 12.1 Nguyen tac

1. `TENANT` la role mac dinh khi dang ky tu app.
2. `OWNER` va `STAFF` duoc gan qua luong bootstrap/admin/invite, khong cho tu khai bao tu do trong signup cong khai.
3. Role chi co nghia khi di kem `membership` trong tenant scope.
4. Du lieu lien ket nghiep vu phai dung `houseId` va `roomId`; `houseCode` va `roomCode` chi de join/onboarding/tim kiem.

### 12.2 Account profile

Doc user root (goi y):

- `users/{uid}`
   - `displayName`: String
   - `email`: String
   - `phone`: String
   - `primaryRole`: String (`OWNER`/`STAFF`/`TENANT`)
   - `activeTenantId`: String
   - `createdAt`: Timestamp
   - `updatedAt`: Timestamp

`primaryRole` chi la role hien thi/uu tien. Quyen that su phai duoc xac nhan boi membership trong tenant scope.

### 12.3 Membership trong tenant

Doc chuan:

- `tenants/{tenantId}/members/{uid}`
   - `uid`: String
   - `role`: String (`OWNER`/`STAFF`/`TENANT`)
   - `houseId`: String (co the null neu OWNER quan ly toan tenant)
   - `roomId`: String (chi can cho TENANT)
   - `assignedHouseIds`: Array<String> (dung cho STAFF/OWNER neu co nhieu house)
   - `assignedRoomIds`: Array<String> (tuong lai neu can moi room scope)
   - `inviteCode`: String
   - `status`: String (`ACTIVE`/`PENDING`/`DISABLED`)
   - `createdAt`: Timestamp
   - `updatedAt`: Timestamp

### 12.4 Invite collection cho staff

- `tenants/{tenantId}/invites/{inviteId}`
   - `email`: String
   - `role`: String (`STAFF` hoac `TENANT`)
   - `houseId`: String
   - `houseCode`: String
   - `code`: String (ma moi)
   - `status`: String (`PENDING`/`ACCEPTED`/`EXPIRED`)
   - `createdAt`: Timestamp
   - `expiresAt`: Timestamp

### 12.5 Role matrix (de dev/UI bam theo)

1. `OWNER`
    - Toan quyen trong tenant.
    - Co the lam viec cua `STAFF` ma khong can role `STAFF` rieng.
    - Co the tao invite cho `STAFF`.
2. `STAFF`
    - Quan ly 1 nhieu `houseId`/`roomId` duoc gan.
    - Khong duoc sua tenant config co ban neu khong duoc cap quyen.
3. `TENANT`
    - Chi thao tac trong `roomId` cua minh.
    - Chi xem/confirm/thanh toan hoa don cua phong minh.

### 12.6 Goi y luong onboarding

1. Signup app -> tao `users/{uid}` voi `primaryRole = TENANT`.
2. TENANT nhap `roomCode` de join phong.
3. OWNER bootstrap bang cach tao tenant/house va co the moi STAFF.
4. STAFF nhap `houseCode` hoac invite code de duoc gan vao `houseId`.

Schema nay la nen tang de phat trien 3 role ma khong phai doi lai data model o giai doan sau.

### 12.7 Trang thai implement hien tai

Da ap dung mot phan schema membership trong code:

1. Signup app tao `users/{uid}` voi `primaryRole = TENANT`.
2. Tao tenant (OWNER bootstrap) tao member co `status = ACTIVE`, `assignedHouseIds = []`, `assignedRoomIds = []`.
3. Join invite tao member co:
   - `status = ACTIVE`
   - `assignedHouseIds` neu invite co `houseId`
   - `assignedRoomIds` neu invite la TENANT co `roomId`

Phan con lai (join bang roomCode/houseCode tren UI va role switch man hinh) se tiep tuc o cac batch sau.

### 12.8 Cap nhat thuc te (2026-04-04)

Da bo sung man tenant payment history co ban, khong lam anh huong owner flow:

1. Home theo role:
   - Tenant card `Lich su thanh toan` mo man `TenantPaymentHistoryActivity`.
2. Man `TenantPaymentHistoryActivity`:
   - Doc role + `roomId` tu `tenants/{tenantId}/members/{uid}`.
   - Chi cho phep role `TENANT` truy cap man nay.
   - Hien thi danh sach payment theo `roomId` cua tenant.
   - Read-only: an nut them payment, an nut xoa payment.
3. Repository:
   - `PaymentRepository` da co them ham `listByRoom(roomId)` de phuc vu truy van tenant-level.

Luu y:
- Feature flag tenant self-service trong `InvoiceActivity` van dang tat (`ENABLE_TENANT_SELF_SERVICE = false`).
- Nghia la tenant hien tai co man xem lich su thanh toan co ban rieng, nhung luong thao tac invoice self-service day du chua bat.
