# Firebase Data Management Guide

## Purpose
This document guides data management for MOPR Firebase: creating users/roles, seeding test data, safe reset procedures, and testing without corrupting production data.

Naming clarification:

- Current top-level Firestore collection is `tenants` (not `org`/`organizations`).
- "Org" in screen/module names is organizational UI terminology mapped to `tenants/{tenantId}` data.

## 0) Auth Flows In Current App

### 0.1 Email/password login (`MainActivity`)

1. Sign-in uses Firebase Auth only.
2. No Firestore write occurs during login itself.
3. Remember-me stores only email in SharedPreferences (`savedEmail`), not password.

### 0.2 Google Sign-In (`MainActivity`)

1. Google credential signs in with Firebase Auth.
2. App auto-creates/merges `users/{uid}` profile right after Google sign-in.
3. If `users/{uid}` already exists, app updates basic profile fields (`uid`, `email`, `fullName`, `updatedAt`) and does NOT override existing `primaryRole`/`activeTenantId`.
4. If `users/{uid}` does not exist, default bootstrap fields include `primaryRole=TENANT`, `activeTenantId=null`, `createdAt`, `updatedAt`.
5. After sign-in succeeds, app navigates directly to Home (no second manual login step).
6. If user already has an active Firebase session, app auto-enters Home on app open.

### 0.3 Public signup (`SignUpActivity`)

Signup creates `users/{uid}` with:

- `uid`, `fullName`, `email`, `phoneNumber`
- `primaryRole = TENANT`
- `activeTenantId = null`
- `createdAt`, `updatedAt`

### 0.4 Profile update (`ProfileActivity`, `EditProfileActivity`)

1. App updates Firebase Auth displayName/photo.
2. App updates `users/{uid}` fields (`fullName`, `phoneNumber`, optional `avatarUrl`).
3. If update fails due to missing doc, app falls back to `set(...)` to create user document.

### 0.5 Change password (`ChangePasswordActivity`)

1. Uses Firebase Auth `updatePassword(...)`.
2. Available only when account has `password` provider.
3. Google-only account is blocked from change-password flow (menu hidden in Home drawer and screen auto-exits if opened directly).
4. Does not update Firestore.
5. If Firebase requires recent login, user must re-authenticate.

### 0.6 Tenant session (`TenantSession`)

1. Active tenant is cached in SharedPreferences key `activeTenantId`.
2. Most repositories resolve data scope from `TenantSession.getActiveTenantId()` first, then fallback to `users/{uid}`.

### 0.7 Home role UI switching (`HomeMenuActivity`)

1. Home defaults to guest UI (all Home action rows hidden).
2. `users/{uid}.primaryRole` + `users/{uid}.activeTenantId` are used to route after Home starts:
   - `OWNER` => show current OWNER Home UI.
   - `TENANT` + missing `activeTenantId` => redirect to `JoinRoomActivity`.
   - `TENANT` + existing `activeTenantId` => redirect to `TenantMenuActivity`.
   - `STAFF` and other non-`OWNER` roles => keep guest-compatible Home UI.
3. If `primaryRole` changes from non-OWNER to `OWNER` while user is in an active session, app forces logout and asks user to sign in again.
4. After signing in again with `primaryRole=OWNER`, user sees OWNER UI.
5. Drawer behavior by role:
   - `OWNER`: shows Rental History menu.
   - non-OWNER: hides Rental History menu.
6. Current role mapping in Home UI is owner-first with tenant redirect:
   - `OWNER` => OWNER UI.
   - `TENANT` => JoinRoom/TenantMenu depending on `activeTenantId`.
   - `STAFF` => guest-compatible UI.
7. Role-promotion auto-logout is enforced by the role listener in `HomeMenuActivity` while Home is active.

## 1) How the App Understands Data

Role-related data is distributed across 2 scopes, with different usages:

1. User scope:
   - `users/{uid}`
   - stores `primaryRole`, `activeTenantId`, basic profile information
2. Tenant scope:
   - `tenants/{tenantId}`
   - `tenants/{tenantId}/members/{uid}`
   - this is where the app checks actual permissions within the tenant

Important conventions:

- Home UI role rendering currently reads `users/{uid}.primaryRole`.
- Domain permission inside tenant (staff/tenant/owner operations) still depends on `tenants/{tenantId}/members/{uid}.role`.
- `STAFF` is not rendered as a dedicated Home UI role yet (currently falls back to guest UI).
- `activeTenantId` must match the tenant being tested.

## 2) How to Create OWNER

Do not create OWNER through public signup.

Correct flow in app:

1. User logs in.
2. Create new tenant via `TenantRepository.createTenant(...)` flow.
3. App automatically creates:
   - `tenants/{tenantId}`
   - `tenants/{tenantId}/members/{uid}` with `role = OWNER`
   - `users/{uid}` with `primaryRole = OWNER`
   - `users/{uid}.activeTenantId = tenantId`

Additional tenant fields created by app in `tenants/{tenantId}`:

- `name`, `ownerUid`, `timezone`, `currency`, `billingCycleDay`
- `plan`, `maxRooms`, `maxStaff`, `maxInvoicesPerMonth`
- `createdAt`

To seed manually on Firebase Console, create these 3 documents:

```text
users/{ownerUid}
  fullName: "Owner Test"
  email: "owner@test.com"
  phoneNumber: "0900000000"
  uid: "ownerUid"
  primaryRole: "OWNER"
  activeTenantId: "tenantA"

tenants/{tenantA}
  name: "Test Property"
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

## 3) How to Create STAFF

STAFF should be created via invite:

1. Owner logs in to tenant.
2. Create STAFF invite via app flow.
3. Firestore will have:
   - `tenants/{tenantId}/invites/{code}` with `role = STAFF`
   - after join: `tenants/{tenantId}/members/{uid}` with `role = STAFF`
   - `users/{uid}.primaryRole = STAFF`

Current invite fields for STAFF:

- required: `code`, `email`, `role`, `status`, `createdAt`, `createdBy`
- optional: `houseId`, `houseCode`

To seed manually:

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

## 4) How to Create TENANT

TENANT is the default role when signing up publicly.

Signup writes:

```text
users/{tenantUid}
  primaryRole: "TENANT"
  activeTenantId: null
```

After that, tenant user will receive an invite and join a room/tenant.

Current invite paths for TENANT:

- Email invite path: `roomId` is required in invite document.
- Anonymous invite path: invite includes `type = ANONYMOUS`, `roomId`, and can be redeemed from `JoinRoomActivity` by code only.
- Anonymous code redemption is guarded by a Firestore transaction so one code is accepted at most once under concurrent joins.

To seed manually:

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

## 5) How to Safely Reset During Testing

Do not delete the entire database if only testing role.

Prioritize reset by scope:

1. Reset one test user:
   - delete `users/{uid}` if starting fresh
   - delete `tenants/{tenantId}/members/{uid}` if role is corrupted
   - delete `activeTenantId` if user is stuck in old tenant
2. Reset one test tenant:
   - delete `tenants/{tenantId}` and all related subcollections
   - only use if this tenant is for testing and has no production data links
3. Reset one invite:
   - delete `tenants/{tenantId}/invites/{code}` or set `status = REVOKED`

Safe recommendations:

- Use one separate test tenant.
- Use 3 separate test accounts: `owner`, `staff`, `tenant`.
- Name test documents clearly, e.g. `tenant_test_01`.

Invite cleanup recommendation:

- If invite testing is stuck, set invite `status` to a non-pending state (for example `REVOKED`) or delete the invite document.

## 6) Checklist Before Testing Role

Before running the app, verify these 3 points:

1. `users/{uid}.primaryRole`
2. `users/{uid}.activeTenantId`
3. `tenants/{tenantId}/members/{uid}.role`

Expected Home UI behavior in current build:

- `primaryRole = OWNER` => OWNER Home UI (cards + rental history visible).
- `primaryRole = TENANT` and `activeTenantId` missing => redirect to join room flow.
- `primaryRole = TENANT` and `activeTenantId` set => redirect to tenant dashboard flow.
- other non-`OWNER` roles => guest-compatible Home UI (cards hidden, rental history hidden).

If these values are not synchronized, Home UI and tenant-domain permissions may look inconsistent.

## 7) How to Manage Data on Firebase Console

Recommended action sequence:

1. Create user in Firebase Auth.
2. Create `users/{uid}`.
3. Create `tenants/{tenantId}` if owner's new tenant.
4. Create `tenants/{tenantId}/members/{uid}`.
5. Create invites if testing join flow.
6. Add `houses`, `rooms`, `contracts`, `invoices`, `payments` only after role is correct.

Rules:

- Do not rename collections/fields without migration plan.
- Do not use one test user for multiple roles if testing broad UI scope.
- Do not add test data to production tenant.

Important app-specific note:

- `EditProfileActivity.applyInviteCode()` requires an active tenant id in session. If `activeTenantId` is null, invite join from that screen will not proceed.

## 8) Recommended Test Data Suite

To test quickly, create 3 accounts:

1. Owner:
   - creates new tenant
   - `role = OWNER`
2. Staff:
   - joins via STAFF invite
3. Tenant:
   - joins via TENANT invite with `roomId`

This test suite lets you verify:

- home menu by role
- permissions to management screens
- invite/join flow
- data reading per tenant

## 9) When to Delete and Recreate Data

Only reset from scratch if:

1. `users/{uid}` repeatedly has wrong role.
2. `tenants/{tenantId}` test is corrupted with too many invites/members/rooms.
3. You want to test migration or owner bootstrap from scratch.

If only testing UI and role, no need to delete everything.

## 10) Conventions for Agents

1. Owner bootstrap goes through `TenantRepository.createTenant(...)`.
2. Public signup is always `TENANT`.
3. Home shell UI role gate currently follows `users/{uid}.primaryRole` (OWNER vs non-OWNER).
4. Tenant-domain authorization must come from `tenants/{tenantId}/members/{uid}.role`.
5. Each seeding/reset, prefer modifying test documents only, do not delete global data unless necessary.

## 11) Legacy Data Migration During Tenant Bootstrap

When `ensureActiveTenant(...)` creates a default tenant for a legacy user, app migration currently copies only:

1. `rooms`
2. `contracts`
3. `invoices`

Current implementation uses batch write guard around 450 operations per collection pass.
Large legacy datasets may need manual follow-up migration for remaining documents.