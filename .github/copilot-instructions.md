# Project Guidelines

## Architecture
See [docs/ARCHITECTURE_MAP.md](docs/ARCHITECTURE_MAP.md) for details on module boundaries, layers, data flow, and push notification pipelines.
See [docs/ANDROID_SOURCE_STRUCTURE_GUIDE.md](docs/ANDROID_SOURCE_STRUCTURE_GUIDE.md) for navigating the local Android client source packages.

- The app uses an MVVM architecture with Java, Retrofit, Glide, Room Database, and Firebase (Auth, Firestore, Cloud Messaging).
- Data Layer: Repositories handle a mix of local SQLite (Room DB) and remote NoSQL (Firestore).
- Firebase Functions (Node.js 20) in `functions/` are used for push messaging and serverless workflows.

## Build and Test
### Android App
- Build: `./gradlew assembleDebug` (Windows: `.\gradlew.bat assembleDebug`)
- Testing: The project relies heavily on manual smoke tests (e.g., chat/notification multi-account testing) as defined in the Definition of Done.

### Firebase Functions
From the `functions/` directory:
- Emulate locally: `npm run serve`
- Deploy: `npm run deploy`
- View logs: `npm run logs`

## Conventions
See [docs/WORKFLOW_GUIDE.md](docs/WORKFLOW_GUIDE.md) for agent workflows, UI editing rules, UI reusability, Definition of Done, and Vietnamese copy conventions.
- **Copy:** Must use "lí" instead of "lý" (e.g., strictly use "Quản lí trọ" for the brand phrase). NEVER revert the core labels `Trò chuyện với khách`, `Hoá đơn tự động`, or `Thống kê doanh thu` back to their older legacy forms. Let these specific phrases remain protected.
- **UI Reusability:** Developers must prioritize existing UI helpers (`core/util`, `core/widget`) over duplicating app scaffolding or edge-to-edge layouts.

See [docs/FIRESTORE_DATA_CONTRACT.md](docs/FIRESTORE_DATA_CONTRACT.md) for Firestore schema definitions, validation guidelines, and strict rules in `firestore.rules`.
- Do not rename collections or fields without a migration plan.
- **Multi-tenancy:** Firestore data structure heavily favors `tenants/{tenantId}/...` over user-scoped schemas.

See [docs/DOMAIN_COMPATIBILITY.md](docs/DOMAIN_COMPATIBILITY.md) and [docs/FIREBASE_DATA_MANAGEMENT_GUIDE.md](docs/FIREBASE_DATA_MANAGEMENT_GUIDE.md) for advanced considerations on schema mappings and legacy structures.
See [docs/AGENT_HANDOFF.md](docs/AGENT_HANDOFF.md) for context regarding tasks, scopes, and execution principles.