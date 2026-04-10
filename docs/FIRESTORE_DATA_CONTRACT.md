# Firestore Data Contract (MOPR)

## Objective
This document is the single source of truth for Firestore data usage across screens and agents, especially for Room Management, Invoices, and Payments.

Canonical contract note:

- This document reflects the canonical English keys and persisted status values used by current repository/domain code.

Source API naming note:

- Domain/source code should use English accessor names (for example: `getStatus()`, `getBillingPeriod()`, `getTotalAmount()`).

## 1) Current Data Scope

Important naming clarification:

- There is NO top-level Firestore collection named `org` or `organizations` in the current runtime schema.
- "Org" in UI/module names (for example `OrgAdminActivity`) maps to tenant-level data under `tenants/{tenantId}`.

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
4. `contractMembers`
5. `invoices`
6. `payments`
7. `meterReadings`
8. `expenses`
9. `tickets`
10. `rentalHistory`
11. `chat_conversations`
12. `notifications`

Additional tenant-root collections:

1. `tenants/{tenantId}` (organization profile)
2. `tenants/{tenantId}/members` (OWNER/STAFF/TENANT)
3. `tenants/{tenantId}/invites`
4. `tenants/{tenantId}/auditLogs`
5. `tenants/{tenantId}/backups` (in-app backups)

Additional user-root collections:

1. `users/{uid}/fcm_tokens` (client device token sync)

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
- `billingReminderAt`: String (`start_month` / `mid_month`)
   - Legacy `end_month` values are still treated as compatible input and normalized to `mid_month` in current app flow.

Legacy compatibility fields still read by current domain model:

- `roomPrice`: Number (legacy fallback for `rentAmount`)
- `legacyDepositAmount`: Number (legacy fallback for `depositAmount`)

### 3.3 `invoices/{invoiceId}`

Primary source: `domain/Invoice.java`

Canonical fields (current code path):

- `roomId`: String (FK -> `rooms/{roomId}`)
- `contractId`: String (FK -> `contracts/{contractId}`)
- `roomNumber`: String (denormalized for display)
- `billingPeriod`: String (`MM/yyyy`, example `03/2026`)
- Electricity/water: `electricStartReading`, `electricEndReading`, `electricUnitPrice`, `waterStartReading`, `waterEndReading`, `waterUnitPrice`
- Fees: `trashFee`, `internetFee`, `parkingFee`
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

### 3.4 `contractMembers/{memberId}`

Primary source: `domain/ContractMember.java`

Canonical fields (current code path):

- `contractId`: String (FK -> `contracts/{contractId}`)
- `roomId`: String (FK -> `rooms/{roomId}`)
- `roomNumber`: String (denormalized)
- `fullName`, `personalId`, `phoneNumber`
- Role flags: `primaryContact`, `contractRepresentative`
- Compliance flags: `temporaryResident`, `fullyDocumented`
- `active`: Boolean
- `createdAt`, `updatedAt`: Int64 millis

### 3.5 `payments/{paymentId}`

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

### 3.6 `chat_conversations/{conversationId}`

Primary source: `features/chat/ChatHubActivity.java`, `features/chat/ChatRoomActivity.java`

Canonical fields (current code path):

- `type`: String (`HOUSE` / `ROOM` / `PRIVATE`)
- `roomId`: String|null (required for room-thread mapping)
- `participantIds`: Array<String> (contains user UIDs)
- `displayName`: String
- `createdAt`: Timestamp
- `createdBy`: String (uid)
- `updatedAt`: Timestamp
- `lastMessage`: String
- `lastSenderId`: String
- `lastMessageAt`: Timestamp

Write/update constraints (aligned with current `firestore.rules`):

- Create requires:
   - `createdBy == request.auth.uid`
   - `participantIds` includes creator and has at least 2 members
   - `type` in `HOUSE` / `ROOM` / `PRIVATE`
   - `PRIVATE`: `roomId == null` and exactly 2 participants
   - `ROOM`: `roomId` is non-empty string
   - `HOUSE`: `roomId == null`
- Update allows only these mutable fields:
   - `displayName`, `updatedAt`, `lastMessage`, `lastSenderId`, `lastMessageAt`
- Immutable after create:
   - `type`, `roomId`, `participantIds`, `createdBy`, `createdAt`

Message subcollection:

- Path: `chat_conversations/{conversationId}/messages/{messageId}`
- Fields: `senderId`, `senderName`, `text`, `createdAt`
- Validation constraints:
   - `senderId == request.auth.uid`
   - sender must be a participant of parent conversation
   - `text` is non-empty string with max length 1000

### 3.7 `notifications/{notificationId}`

Primary source: `features/chat/ChatRoomActivity.java`, `features/notification/NotificationCenterActivity.java`

Canonical fields (current code path):

- `userId`: String (receiver uid)
- `type`: String (currently `CHAT_MESSAGE`)
- `title`: String
- `body`: String
- `conversationId`: String
- `senderId`: String
- `isRead`: Boolean
- `createdAt`: Timestamp
- `readAt`: Timestamp (optional)
- `pushState`: String
   - create flow currently writes `PENDING_SERVER_DISPATCH`
   - Cloud Function dispatcher updates to one of:
      - `NO_TOKEN`
      - `SENT`
      - `PARTIAL`
      - `FAILED`
- `pushUpdatedAt`: Timestamp (set by dispatcher)
- `pushError`: String (optional error reason, set on failure/no-token)

### 3.8 `users/{uid}/fcm_tokens/{tokenDoc}`

Primary source: `features/notification/push/AppFirebaseMessagingService.java`

Canonical fields (current code path):

- `uid`: String
- `token`: String
- `updatedAt`: Timestamp

## 3.9 Cloud Function Dispatcher Contract

Source: `functions/index.js`

Function:

- `dispatchTenantNotificationPush`

Trigger:

- Firestore document create on `tenants/{tenantId}/notifications/{notificationId}`

Behavior:

1. Reads recipient tokens from `users/{userId}/fcm_tokens/*`.
2. Sends FCM multicast with notification + data payload (`tenantId`, `notificationId`, `type`, `conversationId`).
3. Updates notification `pushState` and related metadata fields.
4. Removes invalid registration tokens when Firebase returns token-invalid errors.

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

Current payment write rule:

- `payments` write is currently restricted to `OWNER`/`STAFF` in `firestore.rules`.
- Tenant self-service payment write is not enabled in rules yet.

This is why storing `roomId` in `payments`, `tickets`, `invoices`, and similar collections is required for tenant data isolation.

Rules compatibility note:

- `firestore.rules` is aligned with current English collection names.
- Active collections in rules: `houses`, `rooms`, `contracts`, `contractMembers`, `invoices`, `payments`, `meterReadings`, `expenses`, `tickets`, `rentalHistory`, `backups`, `chat_conversations/messages`, `notifications`.

Chat/notification rules note:

- App now reads/writes `chat_conversations`, `messages`, and `notifications` under tenant scope.
- Ensure `firestore.rules` is updated for these collections before production rollout if strict deny-by-default rules are applied.
- In free-only mode, realtime fallback marks active-conversation notifications as read on client side; docs/rules should keep `isRead` + `readAt` updates available for the recipient.

## 7) Meaning of "Current Existing Data"

This repository does not include a live Firebase production snapshot.

Therefore, this document records:

1. The data structure currently used by the app.
2. Collections that are currently included in in-app backup.
3. Fields and relationships actively read/written by current screens.

Current in-app backup snapshot set (`BackupRestoreActivity.BACKUP_COLLECTIONS`):

- `houses`, `rooms`, `contracts`, `contractMembers`, `invoices`, `rentalHistory`, `payments`, `meterReadings`, `expenses`

Not included in the current backup array:

- `tickets`, `members`, `invites`, `auditLogs`

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

## 10) Role-Based Flow (Current Runtime)

Invoice/payment role flow currently runs in compatibility mode:

1. `InvoiceActivity` keeps tenant self-service behind feature flag (`ENABLE_TENANT_SELF_SERVICE = false`).
2. `PaymentHistoryActivity` also keeps tenant self-service flag disabled (`ENABLE_TENANT_SELF_SERVICE = false`), so owner/staff style add/delete is used there by default.
3. `TenantPaymentHistoryActivity` exists as read-only tenant payment history UI:
   - add button hidden
   - delete disabled
4. Firestore rules enforce payment write for owner/staff only.

Planned direction (not fully enabled yet):

1. TENANT on invoice screen: wait/pay/history actions by invoice status.
2. TENANT payment submission flow requires coordinated app flag + rules update.

## 11) Role and Membership Schema Baseline

### 11.1 Principles

1. `TENANT` is the default role at public signup.
2. `OWNER` and `STAFF` are assigned through bootstrap/admin/invite flows only.
3. Role meaning is valid only with tenant-scope membership.
4. Business data linkage must use `houseId` and `roomId`; `houseCode` and `roomCode` are for join/onboarding/search only.

### 11.2 Account Profile

Suggested root user document:

- `users/{uid}`
   - `uid`: String
   - `fullName`: String
   - `email`: String
   - `phoneNumber`: String
   - `avatarUrl`: String
   - `primaryRole`: String (`OWNER`/`STAFF`/`TENANT`)
   - `activeTenantId`: String or `null`
   - `createdAt`: Timestamp
   - `updatedAt`: Timestamp

`primaryRole` is account-level metadata and is currently used by Home shell UI gating (`OWNER` vs non-`OWNER`).
Actual tenant-domain permission must still be resolved from tenant membership.

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
  - `inviteId` is currently the invite code itself.
   - `email`: String
   - `role`: String (`STAFF` or `TENANT`)
   - `houseId`: String (optional, STAFF)
   - `houseCode`: String (optional)
   - `roomId`: String (required for TENANT invites)
   - `code`: String (invite code)
   - `status`: String (`PENDING`/`ACCEPTED`)
   - `createdAt`: Timestamp
   - `createdBy`: String
   - `acceptedAt`: Timestamp (set on join)
   - `acceptedBy`: String (set on join)

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

1. App signup -> create `users/{uid}` with `primaryRole = TENANT`, `activeTenantId = null`.
2. OWNER bootstrap tenant via `core/session/TenantRepository.createTenant(...)`.
3. STAFF/TENANT join through invite code via `core/session/InviteRepository.joinByInvite(...)`.
4. App updates `tenants/{tenantId}/members/{uid}` + invite status + `users/{uid}.activeTenantId` in one batch.

## 12) Bootstrap Migration Note

When creating default tenant from legacy user scope, current migration copies only these collections:

- `rooms`
- `contracts`
- `invoices`

Implementation note:

- Migration uses batched writes with a simple guard (`opCount >= 450`), so very large legacy datasets may require manual follow-up migration for remaining records.

This schema supports all three roles without requiring future data-model rewrites.

## 13) Current Implementation Status

Part of membership schema is already implemented:

1. App signup creates `users/{uid}` with `primaryRole = TENANT`.
2. Tenant creation (OWNER bootstrap) creates member with `status = ACTIVE`, `assignedHouseIds = []`, `assignedRoomIds = []`.
3. Invite join creates member with:
   - `status = ACTIVE`
   - `assignedHouseIds` when invite includes `houseId`
   - `assignedRoomIds` when TENANT invite includes `roomId`

Remaining work (roomCode/houseCode join UI and role-based screen switching) is planned for upcoming batches.

## 14) Current Update (2026-04-04)

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
