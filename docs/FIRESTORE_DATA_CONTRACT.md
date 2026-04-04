# Firestore Data Contract (MOPR)

## Objective
This document is the single source of truth for Firestore data usage across screens and agents, especially for Room Management, Invoices, and Payments.

Canonical contract note:

- This document reflects the canonical English keys and persisted status values used by current repository/domain code.

Source API naming note:

- Domain/source code should use English accessor names (for example: `getStatus()`, `getBillingPeriod()`, `getTotalAmount()`).

## 1) Current Data Scope

The app currently supports two storage scopes:

1. Legacy per-user scope:
   - users/{uid}/{collection}/{docId}
2. Multi-tenant scope (preferred/current):
   - tenants/{tenantId}/{collection}/{docId}

Code-level scope rules:

- If active tenant exists (`TenantSession.getActiveTenantId()`), read/write to `tenants/{tenantId}/...`
- If no active tenant exists, fallback to `users/{uid}/...`

This pattern is used consistently in repositories:

- `RoomRepository`
- `TenantRepository`
- `InvoiceRepository`
- `PaymentRepository`
- `ExpenseRepository`
- `MeterReadingRepository`
- `HouseRepository`
- `TicketRepository`
- `RentalHistoryRepository`

## 2) Collections Used by the App

Under `tenants/{tenantId}` (and `users/{uid}` fallback), the app uses:

1. `houses`
2. `rooms`
3. `contracts`
4. `invoices`
5. `payments`
6. `meterReadings`
7. `expenses`
8. `tickets`
9. `rentalHistory`

Additional tenant-root collections:

1. `tenants/{tenantId}` (organization profile)
2. `tenants/{tenantId}/members` (OWNER/STAFF/TENANT)
3. `tenants/{tenantId}/invites`
4. `tenants/{tenantId}/auditLogs`
5. `tenants/{tenantId}/backups` (in-app backups)

## 3) Core Schema for Room Management + Invoices

### 3.1 `rooms/{roomId}`

Primary source: `domain/Room.java`

Canonical fields (current code path):

- `roomNumber`: String
- `roomType`: String
- `area`: Number
- `rentAmount`: Number
- `status`: String (`"VACANT"`, `"RENTED"`)
- `imageUrl`: String (URL)
- `houseId`: String
- `houseName`: String
- `floor`: Number
- `description`: String
- `amenities`: Array<String>
- `maxOccupancy`: Number
- `createdAt`: Timestamp
- `updatedAt`: Timestamp

### 3.2 `contracts/{contractId}`

Primary source: `domain/Tenant.java`

Room/contract link fields:

- `roomId`: String (FK -> `rooms/{roomId}`)
- `previousRoomId`: String
- `roomNumber`: String (denormalized)
- `contractStatus`: String (`ACTIVE`/`ENDED`)
- `createdAt`, `updatedAt`, `endedAt`: Int64 millis

Tenant/contract information fields:

- `fullName`, `personalId`, `phoneNumber`, `address`, `contractNumber`
- `representativeName`, `representativeId`
- `personalIdFrontUrl`, `personalIdBackUrl`
- `rentAmount`, `depositAmount`: Int64
- `contractEndDate`: String
- `contractEndTimestamp`: Int64 millis
- Flags: `depositCollectionStatus`, `showDepositOnInvoice`, `showNoteOnInvoice`
- Services: `hasParkingService`, `vehicleCount`, `hasInternetService`, `hasLaundryService`
- `contractDurationMonths`: Number
- `billingReminderAt`: String (`start_month` / `end_month`)

### 3.3 `invoices/{invoiceId}`

Primary source: `domain/Invoice.java`

Canonical fields (current code path):

- `roomId`: String (FK -> `rooms/{roomId}`)
- `contractId`: String (FK -> `contracts/{contractId}`)
- `roomNumber`: String (denormalized for display)
- `billingPeriod`: String (`MM/yyyy`, example `03/2026`)
- Electricity/water: `electricStartReading`, `electricEndReading`, `electricUnitPrice`, `waterStartReading`, `waterEndReading`, `waterUnitPrice`
- Fees: `trashFee`, `wifiFee`, `parkingFee`
- `rentAmount`, `totalAmount`: Number
- `status`: String
   - `UNREPORTED`
   - `REPORTED`
   - `PARTIAL`
   - `PAID`
- `paymentDate`: Timestamp
- `paymentMethod`: String
- `paidAmount`: Number
- `createdAt`, `updatedAt`: Timestamp

Duplicate-safe invoice ID rule (currently supported):

- `invoiceId = roomId + "_" + yyyyMM`
- Example: `roomA_202603`

### 3.4 `payments/{paymentId}`

Primary source: `domain/Payment.java`

Main fields:

- `invoiceId`: String (FK -> `invoices/{invoiceId}`)
- `roomId`: String (supports tenant-level access control)
- `amount`: Number
- `method`: String (`CASH`/`BANK`)
- `paidAt`: String (`dd/MM/yyyy`)
- `note`: String

When total payments change, payment history flow updates `invoices.status`:

- 0 paid -> `REPORTED`
- 0 < paid < total -> `PARTIAL`
- paid >= total -> `PAID`

## 4) Cross-Screen Data Relationships

1. Room -> Contract:
   - `contracts.roomId == rooms.docId`
2. Invoice -> Room:
   - `invoices.roomId == rooms.docId`
3. Invoice -> Contract:
   - `invoices.contractId == contracts.docId`
4. Payment -> Invoice:
   - `payments.invoiceId == invoices.docId`

For new screens, prefer queries by `roomId`, `invoiceId`, and `contractId` to stay aligned with current invoice flow.

## 5) How to Read Data Consistently with Invoice Screen

### 5.1 Shared scope pattern (recommended)

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

### 5.2 Example: load rooms + active contracts + invoices

```java
scopedCollection("rooms").get();

scopedCollection("contracts")
   .whereEqualTo("contractStatus", "ACTIVE")
    .get();

scopedCollection("invoices")
   .whereEqualTo("roomId", roomId)
    .get();
```

### 5.3 Example: load payment history for one invoice

```java
scopedCollection("payments")
    .whereEqualTo("invoiceId", invoiceId)
    .get();
```

## 6) Related Firestore Rules

File: `firestore.rules`

Tenant roles:

- `OWNER`, `STAFF`: read/write for most business collections
- `TENANT`: read access limited by membership `roomId`

This is why storing `roomId` in `payments`, `tickets`, `invoices`, and similar collections is required for tenant data isolation.

Rules compatibility note:

- `firestore.rules` is aligned with current English collection names.
- Active collections in rules: `houses`, `rooms`, `contracts`, `invoices`, `payments`, `meterReadings`, `expenses`, `tickets`, `rentalHistory`, `backups`.

## 7) Meaning of "Current Existing Data"

This repository does not include a live Firebase production snapshot.

Therefore, this document records:

1. The data structure currently used by the app.
2. Collections that are currently included in in-app backup.
3. Fields and relationships actively read/written by current screens.

To inspect real live data counts, use:

1. Firestore Console for direct document inspection.
2. In-app Backup/Restore screen (`backups` collection) to snapshot current state.
3. An optional audit script for collection-level record counts.
4. `docs/FIREBASE_DATA_MANAGEMENT_GUIDE.md` for seed/reset/role testing workflow.

## 8) Agent Conventions for New Screens

1. Always use scope helper (`TenantSession` + `users/{uid}` fallback).
2. Never rename existing collections/fields without a migration plan.
3. Prefer existing FK fields (`roomId`, `contractId`, `invoiceId`).
4. If denormalization is required (for example storing `roomNumber`), keep the original FK.
5. When invoice status is updated from payments, use valid values in `InvoiceStatus`.
6. Review `firestore.rules` before adding any new collection.

## 9) Data Audit

Use the Node.js audit script to inspect live collection counts:

```bash
cd scripts
npm install firebase-admin
# Download serviceAccountKey.json from Firebase Console > Project Settings
node audit-firestore.js
# Or audit a specific tenant:
node audit-firestore.js <tenantId>
```

Output includes:

- Record counts per collection
- Tenant totals
- Which collections are populated

Example output:

```text
Tenant A (ID: tenant_abc123)
========================================
houses            : 5 records
rooms             : 25 records
contracts         : 18 records
invoices          : 142 records
payments          : 87 records
meterReadings     : (empty)
Total: 277 records
```

## 10) Role-Based Flow (Roadmap, Tenant Rollout Pending)

Invoice and payment screens already include role-specific structure for TENANT and OWNER/STAFF. Current rollout strategy:

1. Active now: OWNER/STAFF flow (default).
2. TENANT self-service: code scaffold exists, temporarily disabled behind feature flag until tenant role rollout is fully completed in DB and UI.

Expected flow when TENANT self-service is enabled:

1. TENANT on invoice screen:
   - Cannot delete/edit/issue invoice notifications.
   - Gets self-service actions based on invoice status:
   - `Wait for owner notice` when invoice is `UNREPORTED`.
   - `Pay now` when invoice is `REPORTED` or `PARTIAL`.
   - `View payment history` when invoice is `PAID`.
2. TENANT on payment history screen:
   - Can add payment transaction.
   - Cannot delete payment transaction.
3. OWNER/STAFF:
   - Keep current management flow.

Goal of this phase: prepare self-service direction without affecting current owner flow.

## 11) Role and Membership Schema Baseline

### 11.1 Principles

1. `TENANT` is the default role at public signup.
2. `OWNER` and `STAFF` are assigned through bootstrap/admin/invite flows only.
3. Role meaning is valid only with tenant-scope membership.
4. Business data linkage must use `houseId` and `roomId`; `houseCode` and `roomCode` are for join/onboarding/search only.

### 11.2 Account Profile

Suggested root user document:

- `users/{uid}`
   - `displayName`: String
   - `email`: String
   - `phone`: String
   - `primaryRole`: String (`OWNER`/`STAFF`/`TENANT`)
   - `activeTenantId`: String
   - `createdAt`: Timestamp
   - `updatedAt`: Timestamp

`primaryRole` is display/default metadata. Actual permission must be resolved from tenant membership.

### 11.3 Tenant Membership

Standard document:

- `tenants/{tenantId}/members/{uid}`
   - `uid`: String
   - `role`: String (`OWNER`/`STAFF`/`TENANT`)
   - `houseId`: String (can be null for full-tenant OWNER)
   - `roomId`: String (typically required for TENANT)
   - `assignedHouseIds`: Array<String> (for STAFF/OWNER with multiple houses)
   - `assignedRoomIds`: Array<String> (future fine-grained room scope)
   - `inviteCode`: String
   - `status`: String (`ACTIVE`/`PENDING`/`DISABLED`)
   - `createdAt`: Timestamp
   - `updatedAt`: Timestamp

### 11.4 Invite Collection

- `tenants/{tenantId}/invites/{inviteId}`
   - `email`: String
   - `role`: String (`STAFF` or `TENANT`)
   - `houseId`: String
   - `houseCode`: String
   - `code`: String (invite code)
   - `status`: String (`PENDING`/`ACCEPTED`/`EXPIRED`)
   - `createdAt`: Timestamp
   - `expiresAt`: Timestamp

### 11.5 Role Matrix

1. `OWNER`
    - Full permissions in tenant scope.
    - Can do `STAFF` operations without separate `STAFF` role.
    - Can create `STAFF` invites.
2. `STAFF`
    - Manages assigned `houseId`/`roomId` scopes.
    - Cannot update core tenant configuration unless explicitly permitted.
3. `TENANT`
    - Operates only within assigned `roomId`.
    - Can only view/confirm/pay invoices for own room.

### 11.6 Onboarding Flow (Suggested)

1. App signup -> create `users/{uid}` with `primaryRole = TENANT`.
2. TENANT enters `roomCode` to join a room.
3. OWNER bootstraps tenant/house and can invite STAFF.
4. STAFF enters `houseCode` or invite code to join assigned house.

This schema supports all three roles without requiring future data-model rewrites.

### 11.7 Current Implementation Status

Part of membership schema is already implemented:

1. App signup creates `users/{uid}` with `primaryRole = TENANT`.
2. Tenant creation (OWNER bootstrap) creates member with `status = ACTIVE`, `assignedHouseIds = []`, `assignedRoomIds = []`.
3. Invite join creates member with:
   - `status = ACTIVE`
   - `assignedHouseIds` when invite includes `houseId`
   - `assignedRoomIds` when TENANT invite includes `roomId`

Remaining work (roomCode/houseCode join UI and role-based screen switching) is planned for upcoming batches.

### 11.8 Current Update (2026-04-04)

A basic tenant payment history flow has been added without changing owner flow:

1. Role-based home:
   - Tenant card `Lịch sử thanh toán` opens `TenantPaymentHistoryActivity`.
2. `TenantPaymentHistoryActivity`:
   - Reads role and `roomId` from `tenants/{tenantId}/members/{uid}`.
   - Allows only `TENANT` role to access this screen.
   - Shows payment list by tenant `roomId`.
   - Read-only mode: hide add-payment and delete-payment actions.
3. Repository:
   - `PaymentRepository` includes `listByRoom(roomId)` for tenant-level queries.

Notes:

- Tenant self-service feature flag in `InvoiceActivity` is still disabled (`ENABLE_TENANT_SELF_SERVICE = false`).
- Tenants currently have basic payment history view, but full invoice self-service actions are not enabled yet.
