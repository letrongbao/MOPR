# Firebase Data Management Guide

## Muc dich
Tai lieu nay huong dan cach quan ly du lieu Firebase cho MOPR: tao user/role, seed data test, reset an toan, va test roi khong lam hong du lieu that.

## 1) Cach app dang hieu du lieu

Source of truth cho role hien tai khong nam o 1 noi duy nhat, ma nam o 2 cap:

1. User scope:
   - `users/{uid}`
   - luu `primaryRole`, `activeTenantId`, thong tin profile co ban
2. Tenant scope:
   - `tenants/{tenantId}`
   - `tenants/{tenantId}/members/{uid}`
   - day la noi app kiem tra quyen thuc te trong tenant

Quy uoc quan trong:

- `OWNER`, `STAFF`, `TENANT` phai duoc doc tu membership trong tenant.
- `users/{uid}.primaryRole` chi la meta/status rong hon, khong nen coi la source of truth duy nhat.
- `activeTenantId` phai khop voi tenant dang test.

## 2) Cach tao OWNER

Khong tao OWNER tu signup public.

Flow dung trong app:

1. Dang nhap user.
2. Tao tenant moi qua flow `TenantRepository.createTenant(...)`.
3. App se tu dong tao:
   - `tenants/{tenantId}`
   - `tenants/{tenantId}/members/{uid}` voi `role = OWNER`
   - `users/{uid}` voi `primaryRole = OWNER`
   - `users/{uid}.activeTenantId = tenantId`

Neu can seed tay tren Firebase Console, tao dung 3 document nay:

```text
users/{ownerUid}
  hoTen: "Owner Test"
  email: "owner@test.com"
  soDienThoai: "0900000000"
  uid: "ownerUid"
  primaryRole: "OWNER"
  activeTenantId: "tenantA"

tenants/{tenantA}
  name: "Nha tro test"
  ownerUid: "ownerUid"
  timezone: "Asia/Ho_Chi_Minh"
  currency: "VND"
  billingCycleDay: 1
  plan: "FREE"
  maxRooms: 50
  maxStaff: 3
  maxInvoicesPerMonth: 200

tenants/{tenantA}/members/{ownerUid}
  uid: "ownerUid"
  role: "OWNER"
  status: "ACTIVE"
  assignedHouseIds: []
  assignedRoomIds: []
```

## 3) Cach tao STAFF

STAFF nen duoc tao bang invite:

1. Owner dang nhap vao tenant.
2. Tao invite STAFF qua flow app.
3. Firestore se co:
   - `tenants/{tenantId}/invites/{code}` voi `role = STAFF`
   - khi join xong, `tenants/{tenantId}/members/{uid}` voi `role = STAFF`
   - `users/{uid}.primaryRole = STAFF`

Neu seed tay:

```text
tenants/{tenantA}/members/{staffUid}
  uid: "staffUid"
  role: "STAFF"
  status: "ACTIVE"
  assignedHouseIds: []
  assignedRoomIds: []

users/{staffUid}
  primaryRole: "STAFF"
  activeTenantId: "tenantA"
```

## 4) Cach tao TENANT

TENANT la role mac dinh khi signup public.

Signup se ghi:

```text
users/{tenantUid}
  primaryRole: "TENANT"
  activeTenantId: null
```

Sau do tenant user se nhan invite va join vao 1 room/tenant.

Neu seed tay:

```text
tenants/{tenantA}/members/{tenantUid}
  uid: "tenantUid"
  role: "TENANT"
  status: "ACTIVE"
  roomId: "roomA"
  assignedRoomIds: ["roomA"]

users/{tenantUid}
  primaryRole: "TENANT"
  activeTenantId: "tenantA"
```

## 5) Cach reset an toan khi test

Khong nen xoa toan bo database neu chi test role.

Uu tien reset theo cap sau:

1. Reset 1 user test:
   - xoa `users/{uid}` neu can tao lai tu dau
   - xoa `tenants/{tenantId}/members/{uid}` neu role bi loang
   - xoa `activeTenantId` neu user bi ket tenant cu
2. Reset 1 tenant test:
   - xoa `tenants/{tenantId}` va tat ca subcollections lien quan
   - chi dung khi tenant do la tenant test va khong con lien ket du lieu that
3. Reset 1 invite:
   - xoa `tenants/{tenantId}/invites/{code}` hoac set `status = REVOKED`

Khuyen nghi an toan:

- Chi dung 1 tenant test rieng.
- Chi dung 3 account test rieng: `owner`, `staff`, `tenant`.
- Dat ten doc test ro rang, vi du `tenant_test_01`.

## 6) Checklist truoc khi test role

Truoc khi bam app, kiem tra 3 diem nay:

1. `users/{uid}.primaryRole`
2. `users/{uid}.activeTenantId`
3. `tenants/{tenantId}/members/{uid}.role`

Neu 3 gia tri nay khong dong bo, UI role co the hien sai.

## 7) Cach quan ly du lieu tren Firebase Console

Thu tu thao tac khuyen nghi:

1. Tao user trong Firebase Auth.
2. Tao `users/{uid}`.
3. Tao `tenants/{tenantId}` neu la owner tenant moi.
4. Tao `tenants/{tenantId}/members/{uid}`.
5. Tao invites neu can test luong join.
6. Chi them `houses`, `rooms`, `contracts`, `invoices`, `payments` sau khi role da dung.

Quy tac:

- Khong sua collection ten/field neu chua co migration plan.
- Khong dung 1 user test cho nhieu role neu dang can test UI scope rong.
- Khong dua du lieu test vao tenant that.

## 8) Bo test data goi y

Neu can test nhanh, tao 3 account:

1. Owner:
   - tao tenant moi
   - `role = OWNER`
2. Staff:
   - join bang invite STAFF
3. Tenant:
   - join bang invite TENANT co `roomId`

Bo test nay se cho ban kiem tra:

- menu home theo role
- quyen vao man quan ly
- luong invite/join
- doc du lieu theo tenant

## 9) Khi nao can xoa data va tao lai

Chi can reset tu dau khi:

1. `users/{uid}` bi sai role lien tiep.
2. `tenants/{tenantId}` test bi loang qua nhieu invite/member/room.
3. Ban muon test migration hoac luong bootstrap owner tu dau.

Neu chi test giao dien va role, thi khong can xoa toan bo.

## 10) Quy uoc cho agent

1. Owner bootstrap di qua `TenantRepository.createTenant(...)`.
2. Signup public luon la `TENANT`.
3. Role thuc te phai dung membership trong tenant.
4. Moi lan seeding/reset, uu tien thay doi dung document test, khong xoa global data neu khong can.