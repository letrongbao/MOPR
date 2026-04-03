# Workflow Guide For Agents

## Muc tieu
Tai lieu nay la huong dan luong lam viec chuan de agent khac trong team co the lam dung cach, commit gon, va khong pha vo du lieu.

## Tai lieu bat buoc phai doc truoc khi sua code

1. `docs/WORKFLOW_GUIDE.md` (tai lieu nay)
2. `docs/FIRESTORE_DATA_CONTRACT.md` (schema va quy tac du lieu Firestore)
3. `docs/AGENT_HANDOFF.md` (pham vi va nguyen tac thuc thi)

## Luong lam viec chuan

1. Kiem tra nhanh trang thai repo
   - `git status --short`
2. Xac dinh scope task
   - Neu lien quan du lieu Firestore, bat buoc doi chieu `FIRESTORE_DATA_CONTRACT.md`
3. Sua code theo batch nho
   - Moi batch xong thi build lai
4. Build kiem tra
   - `./gradlew.bat assembleDebug`
5. Smoke test cac man lien quan
   - Room/Tenant/Invoice/Contract/Revenue
6. Kiem tra lai git truoc commit
   - Khong de file IDE/build/local secrets trong commit

## Nguyen tac commit sach

1. Khong commit file IDE/local:
   - `.idea/`, `.vscode/`, `local.properties`
2. Khong commit build output:
   - `build/`, `**/build/`, `.gradle/`
3. Khong commit local secrets:
   - `scripts/serviceAccountKey.json`
4. Chi commit file nghiep vu va docs huong dan can thiet

## Quy tac khi sua Firestore

1. Luon dung dung scope helper (`TenantSession` + fallback `users/{uid}`)
2. Khong doi ten collection/field khi chua co migration plan
3. Uu tien dung cac FK da co:
   - `idPhong`, `idTenant`, `invoiceId`
4. Neu them man moi, phai map voi schema hien co, khong tao schema rieng

## Definition Of Done (DoD)

1. Build pass: `./gradlew.bat assembleDebug`
2. Khong co conflict (`UU`) trong `git status --short`
3. Khong co thay doi rac (IDE/build/local files)
4. Chuc nang vua sua chay dung voi du lieu Firestore hien co
