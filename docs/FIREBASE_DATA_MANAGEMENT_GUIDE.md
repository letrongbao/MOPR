# Firebase Data Management Guide

## Purpose
This document guides data management for MOPR Firebase: creating users/roles, seeding test data, safe reset procedures, and testing without corrupting production data.

## 1) How the App Understands Data

The source of truth for current role is not stored in a single location, but distributed across 2 scopes:

1. User scope:
   - `users/{uid}`
   - stores `primaryRole`, `activeTenantId`, basic profile information
2. Tenant scope:
   - `tenants/{tenantId}`
   - `tenants/{tenantId}/members/{uid}`
   - this is where the app checks actual permissions within the tenant

Important conventions:

- `OWNER`, `STAFF`, `TENANT` must be read from tenant membership.
- `users/{uid}.primaryRole` is only metadata/status, should not be treated as the only source of truth.
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

## 6) Checklist Before Testing Role

Before running the app, verify these 3 points:

1. `users/{uid}.primaryRole`
2. `users/{uid}.activeTenantId`
3. `tenants/{tenantId}/members/{uid}.role`

If these 3 values are not synchronized, UI role display may be incorrect.

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
3. Actual role must come from tenant membership.
4. Each seeding/reset, prefer modifying test documents only, do not delete global data unless necessary.