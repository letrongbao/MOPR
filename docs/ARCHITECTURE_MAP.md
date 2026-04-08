# Architecture Map

## App Type
Android Java app for rental/property management.

## Layering
- UI layer: `features/*` activities/adapters + `res/layout`.
- Presentation layer: `viewmodel/*ViewModel.java`.
- Data layer: `core/repository/domain/*Repository.java`.
- Domain models: `domain/*.java`.
- Local persistence: `room/*` (Room DB).
- Remote persistence: Firebase Auth + Firestore.

## Firestore Scope Baseline
- Preferred runtime scope: `tenants/{tenantId}/...`
- Legacy compatibility fallback: `users/{uid}/...`
- `Org*` feature names are module/UI naming; they still map to tenant-scope Firestore data.

## Main Feature Modules
- Invoice: `features/invoice/*`
- Finance: `features/finance/*`
- Contract: `features/contract/*`
- Property/House/Room: `features/property/*`
- Tenant: `features/tenant/*`
- Chat: `features/chat/*`
- Notification: `features/notification/*`
- History: `features/history/*`
- Ticket: `features/ticket/*`
- Settings/Auth/Home/Org: corresponding `features/*`

## Core Utilities
- `core/constants/*` for business status constants.
- `core/util/*` for formatting/helpers.
- `core/service/*` for background and upload workflows.
- `core/widget/*` for app widget and picker dialogs.
- App init + notification channels: `core/MyApplication.java`

## Push Messaging (Current)
- Firebase client SDK integration is active in app module (`firebase-messaging`).
- Client entrypoint service: `features/notification/push/AppFirebaseMessagingService`.
- Device token sync location: `users/{uid}/fcm_tokens/primary`.
- Server dispatch pipeline is implemented as Firebase Cloud Function:
	- `functions/index.js`
	- trigger: `tenants/{tenantId}/notifications/{notificationId}` on create
	- export: `dispatchTenantNotificationPush`
- Free-mode fallback (no Functions deploy):
	- `features/notification/NotificationRealtimeObserver` listens Firestore unread notifications in active app session.
	- New added notifications are displayed via local notification utility `features/notification/NotificationDisplayUtil`.
	- Active-room suppression state uses `features/notification/ChatForegroundState`.
	- Notification docs for active conversation are marked read immediately to avoid duplicate unread badges.
	- Chat list unread badge is computed from `tenants/{tenantId}/notifications` grouped by `conversationId`.

## Data Flow (Typical)
1. Activity loads screen and user intent.
2. Activity calls ViewModel method.
3. ViewModel delegates to Repository.
4. Repository reads/writes Firestore or local DB.
5. LiveData updates UI.

## Naming Migration Direction
- Legacy Vietnamese classes/resources are being replaced by English names.
- During transition, both legacy and new artifacts may coexist in git status.
- Runtime compatibility takes priority over cosmetic renaming.
