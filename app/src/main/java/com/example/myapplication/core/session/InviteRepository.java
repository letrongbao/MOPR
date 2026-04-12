package com.example.myapplication.core.session;

import androidx.annotation.NonNull;

import com.example.myapplication.core.constants.TenantRoles;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class InviteRepository {
    private static final String CONTRACT_MEMBER_ROLE_OCCUPANT = "OCCUPANT";
    private static final String CONTRACT_MEMBER_ROLE_REPRESENTATIVE = "REPRESENTATIVE";
    private static final int INVITE_CODE_MAX_RETRY = 6;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final SecureRandom rnd = new SecureRandom();

    public interface InviteCallback {
        void onSuccess(@NonNull String code);

        void onError(@NonNull Exception e);
    }

    public interface JoinCallback {
        void onSuccess(@NonNull String tenantId);

        void onError(@NonNull Exception e);
    }

    private static final class AnonymousJoinResult {
        final String tenantId;
        final String contractId;
        final String memberUid;
        final boolean representative;

        AnonymousJoinResult(@NonNull String tenantId,
                String contractId,
                @NonNull String memberUid,
                boolean representative) {
            this.tenantId = tenantId;
            this.contractId = contractId;
            this.memberUid = memberUid;
            this.representative = representative;
        }
    }

    public void createStaffInvite(@NonNull String tenantId, @NonNull String email, @NonNull InviteCallback cb) {
        createStaffInvite(tenantId, email, null, null, cb);
    }

    public void createStaffInvite(@NonNull String tenantId,
            @NonNull String email,
            String houseId,
            String houseCode,
            @NonNull InviteCallback cb) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            cb.onError(new IllegalStateException("User not logged in"));
            return;
        }

        String code = generateCode(8);

        Map<String, Object> invite = new HashMap<>();
        invite.put("code", code);
        invite.put("email", email.toLowerCase(Locale.US));
        invite.put("role", TenantRoles.STAFF);
        invite.put("status", "PENDING");
        if (houseId != null && !houseId.trim().isEmpty()) {
            invite.put("houseId", houseId.trim());
        }
        if (houseCode != null && !houseCode.trim().isEmpty()) {
            invite.put("houseCode", houseCode.trim());
        }
        invite.put("createdAt", Timestamp.now());
        invite.put("createdBy", user.getUid());

        db.collection("tenants").document(tenantId)
                .collection("invites").document(code).set(invite)
                .addOnSuccessListener(v -> cb.onSuccess(code))
                .addOnFailureListener(cb::onError);
    }

    public void createTenantInvite(@NonNull String tenantId, @NonNull String email, @NonNull String roomId,
            @NonNull InviteCallback cb) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            cb.onError(new IllegalStateException("User not logged in"));
            return;
        }

        if (roomId.trim().isEmpty()) {
            cb.onError(new IllegalArgumentException("roomId is required"));
            return;
        }

        String code = generateCode(8);

        Map<String, Object> invite = new HashMap<>();
        invite.put("code", code);
        invite.put("email", email.toLowerCase(Locale.US));
        invite.put("role", TenantRoles.TENANT);
        invite.put("roomId", roomId);
        invite.put("status", "PENDING");
        invite.put("createdAt", Timestamp.now());
        invite.put("createdBy", user.getUid());

        db.collection("tenants").document(tenantId)
                .collection("invites").document(code).set(invite)
                .addOnSuccessListener(v -> cb.onSuccess(code))
                .addOnFailureListener(cb::onError);
    }

    // --- ANONYMOUS INVITE METHODS ---

    /**
     * Tạo mã mời ẩn danh (không cần email khách)
     */
    public void createAnonymousTenantInvite(@NonNull String tenantId, @NonNull String roomId,
            @NonNull InviteCallback cb) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            cb.onError(new IllegalStateException("User not logged in"));
            return;
        }

        if (roomId.trim().isEmpty()) {
            cb.onError(new IllegalArgumentException("roomId is required"));
            return;
        }

        db.collection("tenants").document(tenantId)
                .collection("contracts")
                .whereEqualTo("roomId", roomId)
                .whereEqualTo("contractStatus", "ACTIVE")
                .limit(1)
                .get()
                .addOnSuccessListener(contracts -> {
                    if (!contracts.isEmpty()) {
                        prepareAnonymousInviteByQuota(tenantId, roomId, contracts.getDocuments().get(0), cb);
                        return;
                    }

                    // Legacy fallback path.
                    db.collection("users").document(tenantId)
                            .collection("contracts")
                            .whereEqualTo("roomId", roomId)
                            .whereEqualTo("contractStatus", "ACTIVE")
                            .limit(1)
                            .get()
                            .addOnSuccessListener(legacyContracts -> {
                                if (legacyContracts.isEmpty()) {
                                    cb.onError(new IllegalStateException(
                                            "Phòng chưa có hợp đồng đang hiệu lực, không thể tạo mã vào phòng."));
                                    return;
                                }
                                prepareAnonymousInviteByQuota(
                                        tenantId,
                                        roomId,
                                        legacyContracts.getDocuments().get(0),
                                        cb);
                            })
                            .addOnFailureListener(cb::onError);
                })
                .addOnFailureListener(cb::onError);
    }

    private void prepareAnonymousInviteByQuota(@NonNull String tenantId,
            @NonNull String roomId,
            @NonNull DocumentSnapshot activeContract,
            @NonNull InviteCallback cb) {
        int totalSlots = resolveMemberCount(activeContract);
        String contractId = activeContract.getId();

        db.collection("tenants").document(tenantId)
                .collection("contractMembers")
                .whereEqualTo("contractId", contractId)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(activeMembers -> {
                    int currentCount = activeMembers != null ? activeMembers.size() : 0;
                    int remainingSlots = Math.max(0, totalSlots - currentCount);
                    if (remainingSlots <= 0) {
                        cb.onError(new IllegalStateException("Số người ở đã đủ theo hợp đồng, không thể tạo thêm mã."));
                        return;
                    }
                    upsertAnonymousInvite(tenantId, roomId, contractId, totalSlots, remainingSlots, cb);
                })
                .addOnFailureListener(cb::onError);
    }

    private void upsertAnonymousInvite(@NonNull String tenantId,
            @NonNull String roomId,
            @NonNull String contractId,
            int totalSlots,
            int remainingSlots,
            @NonNull InviteCallback cb) {
        db.collection("tenants").document(tenantId)
                .collection("invites")
                .whereEqualTo("type", "ANONYMOUS")
                .whereEqualTo("role", TenantRoles.TENANT)
                .whereEqualTo("roomId", roomId)
                .whereEqualTo("contractId", contractId)
                .whereEqualTo("status", "PENDING")
                .limit(1)
                .get()
                .addOnSuccessListener(existing -> {
                    Timestamp now = Timestamp.now();
                    if (existing != null && !existing.isEmpty()) {
                        DocumentSnapshot doc = existing.getDocuments().get(0);
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("totalSlots", totalSlots);
                        updates.put("remainingSlots", remainingSlots);
                        updates.put("usageCount", Math.max(0, totalSlots - remainingSlots));
                        updates.put("updatedAt", now);
                        doc.getReference().set(updates, SetOptions.merge())
                                .addOnSuccessListener(v -> cb.onSuccess(doc.getString("code") != null
                                        ? doc.getString("code")
                                        : doc.getId()))
                                .addOnFailureListener(cb::onError);
                        return;
                    }

                    writeAnonymousInvite(tenantId, roomId, contractId, totalSlots, remainingSlots, cb);
                })
                .addOnFailureListener(cb::onError);
    }

    private void writeAnonymousInvite(@NonNull String tenantId,
            @NonNull String roomId,
            @NonNull String contractId,
            int totalSlots,
            int remainingSlots,
            @NonNull InviteCallback cb) {
        writeAnonymousInviteWithRetry(tenantId, roomId, contractId, totalSlots, remainingSlots, 0, cb);
        }

        private void writeAnonymousInviteWithRetry(@NonNull String tenantId,
            @NonNull String roomId,
            @NonNull String contractId,
            int totalSlots,
            int remainingSlots,
            int attempt,
            @NonNull InviteCallback cb) {
        if (attempt >= INVITE_CODE_MAX_RETRY) {
            cb.onError(new IllegalStateException("Không thể tạo mã phòng mới, vui lòng thử lại."));
            return;
        }

        String code = generateCode(8);
        Timestamp now = Timestamp.now();

        Map<String, Object> invite = new HashMap<>();
        invite.put("code", code);
        invite.put("type", "ANONYMOUS");
        invite.put("role", TenantRoles.TENANT);
        invite.put("roomId", roomId);
        invite.put("contractId", contractId);
        invite.put("status", "PENDING");
        invite.put("totalSlots", totalSlots);
        invite.put("remainingSlots", remainingSlots);
        invite.put("usageCount", Math.max(0, totalSlots - remainingSlots));
        invite.put("createdAt", now);
        invite.put("updatedAt", now);
        invite.put("createdBy", FirebaseAuth.getInstance().getCurrentUser() != null
            ? FirebaseAuth.getInstance().getCurrentUser().getUid()
            : "");

        db.collection("tenants").document(tenantId)
            .collection("invites").document(code)
            .get()
            .addOnSuccessListener(existing -> {
                if (existing.exists()) {
                writeAnonymousInviteWithRetry(
                    tenantId,
                    roomId,
                    contractId,
                    totalSlots,
                    remainingSlots,
                    attempt + 1,
                    cb);
                return;
                }

                db.collection("tenants").document(tenantId)
                    .collection("invites").document(code)
                    .set(invite)
                    .addOnSuccessListener(v -> cb.onSuccess(code))
                    .addOnFailureListener(cb::onError);
            })
            .addOnFailureListener(cb::onError);
    }

    /**
     * Tìm chéo toàn server qua Collection Group để join phòng dựa trên mã ẩn danh
     */
    public void joinByAnonymousInvite(@NonNull android.content.Context context, @NonNull String code,
            @NonNull JoinCallback cb) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            cb.onError(new IllegalStateException("Lỗi: Người dùng chưa đăng nhập."));
            return;
        }

        // Tìm tất cả Collection có tên là "invites"
        db.collectionGroup("invites")
                .whereEqualTo("code", code)
                .whereEqualTo("status", "PENDING")
                .whereEqualTo("type", "ANONYMOUS")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        cb.onError(new IllegalStateException("Mã phòng không tồn tại hoặc đã được sử dụng."));
                        return;
                    }

                    if (querySnapshot.size() > 1) {
                        cb.onError(new IllegalStateException("Mã phòng bị trùng, vui lòng yêu cầu chủ trọ tạo mã mới."));
                        return;
                    }

                    // Do mã (code) sinh ra ngẫu nhiên dài 8 ký tự, khả năng trùng lặp status
                    // PENDING rất thấp.
                    // Lấy document đầu tiên tìm thấy.
                    com.google.firebase.firestore.DocumentSnapshot snap = querySnapshot.getDocuments().get(0);

                    // Suy xuất đường dẫn để lấy tenantId
                    // Đường dẫn mẫu: tenants/{tenantId}/invites/{code}
                    com.google.firebase.firestore.DocumentReference inviteRef = snap.getReference();
                    String tenantId = inviteRef.getParent().getParent().getId();

                    // Dùng transaction để giữ tính "mã 1 lần" khi có nhiều thiết bị join đồng thời.
                    db.runTransaction(transaction -> {
                        com.google.firebase.firestore.DocumentSnapshot freshInvite = transaction.get(inviteRef);
                        if (!freshInvite.exists()) {
                            throw new IllegalStateException("Mã phòng không tồn tại hoặc đã được sử dụng.");
                        }

                        String freshStatus = freshInvite.getString("status");
                        String freshType = freshInvite.getString("type");
                        String freshCode = freshInvite.getString("code");
                        if (!"PENDING".equals(freshStatus) || !"ANONYMOUS".equals(freshType)
                                || freshCode == null || !freshCode.equals(code)) {
                            throw new IllegalStateException("Mã phòng không tồn tại hoặc đã được sử dụng.");
                        }

                        String role = freshInvite.getString("role");
                        if (role == null) {
                            role = TenantRoles.TENANT;
                        }
                        if (!TenantRoles.TENANT.equals(role)) {
                            throw new IllegalStateException("Mã phòng không hợp lệ cho khách thuê.");
                        }

                        String roomId = freshInvite.getString("roomId");
                        String contractId = freshInvite.getString("contractId");
                        String houseId = freshInvite.getString("houseId");
                        if (TenantRoles.TENANT.equals(role) && (roomId == null || roomId.trim().isEmpty())) {
                            throw new IllegalStateException("Lỗi dữ liệu: Mã mời không đính kèm phòng.");
                        }

                        com.google.firebase.firestore.DocumentSnapshot contractDoc = null;
                        com.google.firebase.firestore.DocumentReference activeContractRef = null;

                        if (contractId == null || contractId.trim().isEmpty()) {
                            throw new IllegalStateException("Mã phòng không gắn với hợp đồng đang hiệu lực.");
                        }

                        com.google.firebase.firestore.DocumentReference contractRef = db.collection("tenants")
                                .document(tenantId)
                                .collection("contracts")
                                .document(contractId);
                        contractDoc = transaction.get(contractRef);
                        activeContractRef = contractRef;

                        if (!contractDoc.exists()) {
                            // Legacy fallback path.
                            com.google.firebase.firestore.DocumentReference legacyContractRef = db
                                    .collection("users")
                                    .document(tenantId)
                                    .collection("contracts")
                                    .document(contractId);
                            contractDoc = transaction.get(legacyContractRef);
                            activeContractRef = legacyContractRef;
                        }

                        if (!contractDoc.exists()) {
                            throw new IllegalStateException("Hợp đồng gắn với mã không tồn tại.");
                        }

                        String contractStatus = contractDoc.getString("contractStatus");
                        if (contractStatus == null || !"ACTIVE".equalsIgnoreCase(contractStatus.trim())) {
                            throw new IllegalStateException("Hợp đồng không còn hiệu lực, mã phòng đã bị khóa.");
                        }

                        Long inviteTotalSlotsVal = freshInvite.getLong("totalSlots");
                        Long inviteRemainingSlotsVal = freshInvite.getLong("remainingSlots");
                        Long inviteUsageCountVal = freshInvite.getLong("usageCount");
                        int totalSlots = inviteTotalSlotsVal != null && inviteTotalSlotsVal > 0
                            ? inviteTotalSlotsVal.intValue()
                            : resolveMemberCount(contractDoc);
                        int remainingSlots = inviteRemainingSlotsVal != null
                            ? Math.max(0, inviteRemainingSlotsVal.intValue())
                            : totalSlots;

                        if (remainingSlots <= 0) {
                            throw new IllegalStateException("Mã phòng đã đủ số lượng theo hợp đồng.");
                        }

                        com.google.firebase.firestore.DocumentReference userRef = db.collection("users")
                            .document(user.getUid());
                        com.google.firebase.firestore.DocumentSnapshot userDoc = transaction.get(userRef);

                        String contractPhone = normalizePhone(contractDoc.getString("phoneNumber"));
                        if (contractPhone.isEmpty()) {
                            contractPhone = normalizePhone(contractDoc.getString("phone"));
                        }

                        String accountPhoneRaw = safeTrim(user.getPhoneNumber());
                        String accountPhone = normalizePhone(accountPhoneRaw);
                        if (accountPhone.isEmpty() && userDoc.exists()) {
                            accountPhoneRaw = safeTrim(userDoc.getString("phoneNumber"));
                            accountPhone = normalizePhone(accountPhoneRaw);
                        }
                        if (accountPhone.isEmpty() && userDoc.exists()) {
                            accountPhoneRaw = safeTrim(userDoc.getString("phone"));
                            accountPhone = normalizePhone(accountPhoneRaw);
                        }

                        String accountFullName = "";
                        if (userDoc.exists()) {
                            accountFullName = safeTrim(userDoc.getString("fullName"));
                        }
                        if (accountFullName.isEmpty()) {
                            accountFullName = safeTrim(user.getDisplayName());
                        }

                        boolean representative = !contractPhone.isEmpty() && contractPhone.equals(accountPhone);

                        if (representative && contractDoc != null) {
                            String representativeUid = contractDoc.getString("representativeUid");
                            if (representativeUid != null
                                    && !representativeUid.trim().isEmpty()
                                    && !representativeUid.equals(user.getUid())) {
                                representative = false;
                            }
                        }

                        String contractMemberRole = representative
                            ? CONTRACT_MEMBER_ROLE_REPRESENTATIVE
                            : CONTRACT_MEMBER_ROLE_OCCUPANT;
                        Timestamp now = Timestamp.now();

                        com.google.firebase.firestore.DocumentReference memberRef = db.collection("tenants")
                            .document(tenantId)
                            .collection("members")
                            .document(user.getUid());
                        com.google.firebase.firestore.DocumentSnapshot existingMember = transaction.get(memberRef);
                        boolean alreadyActive = existingMember.exists()
                            && "ACTIVE".equalsIgnoreCase(String.valueOf(existingMember.getString("status")));

                        int usageCount = inviteUsageCountVal != null ? Math.max(0, inviteUsageCountVal.intValue()) : 0;
                        int nextRemaining = alreadyActive ? remainingSlots : Math.max(0, remainingSlots - 1);
                        int nextUsage = alreadyActive ? usageCount : usageCount + 1;

                        Map<String, Object> memberDoc = new HashMap<>();
                        memberDoc.put("uid", user.getUid());
                        memberDoc.put("role", role);
                        memberDoc.put("contractMemberRole", contractMemberRole);
                        memberDoc.put("primaryContact", representative);
                        memberDoc.put("contractRepresentative", representative);
                        memberDoc.put("status", "ACTIVE");
                        memberDoc.put("inviteCode", code);

                        if (TenantRoles.TENANT.equals(role)) {
                            memberDoc.put("roomId", roomId);
                            if (contractId != null && !contractId.trim().isEmpty()) {
                                memberDoc.put("contractId", contractId);
                            }
                            java.util.List<String> assignedRooms = new java.util.ArrayList<>();
                            assignedRooms.add(roomId);
                            memberDoc.put("assignedRoomIds", assignedRooms);
                        } else {
                            memberDoc.put("assignedRoomIds", new java.util.ArrayList<>());
                        }

                        if (houseId != null && !houseId.trim().isEmpty()) {
                            memberDoc.put("houseId", houseId.trim());
                            java.util.List<String> assignedHouses = new java.util.ArrayList<>();
                            assignedHouses.add(houseId.trim());
                            memberDoc.put("assignedHouseIds", assignedHouses);
                        } else {
                            memberDoc.put("assignedHouseIds", new java.util.ArrayList<>());
                        }
                        memberDoc.put("createdAt", now);
                        memberDoc.put("updatedAt", now);

                        if (contractId != null && !contractId.trim().isEmpty()) {
                            com.google.firebase.firestore.DocumentReference contractMemberRef = db
                                    .collection("tenants")
                                    .document(tenantId)
                                    .collection("contractMembers")
                                    .document(user.getUid());
                            com.google.firebase.firestore.DocumentSnapshot existingContractMember = transaction
                                    .get(contractMemberRef);

                            long nowMillis = System.currentTimeMillis();
                            Long createdAt = existingContractMember.getLong("createdAt");
                            if (createdAt == null || createdAt <= 0) {
                                createdAt = nowMillis;
                            }

                            String roomNumber = contractDoc != null ? safeTrim(contractDoc.getString("roomNumber")) : "";
                            if (roomNumber.isEmpty()) {
                                roomNumber = roomId;
                            }

                            String fullName = accountFullName;
                            if (fullName.isEmpty() && contractDoc != null) {
                                fullName = safeTrim(contractDoc.getString("fullName"));
                            }

                            String phoneForProfile = accountPhoneRaw;
                            if (phoneForProfile.isEmpty() && contractDoc != null) {
                                phoneForProfile = safeTrim(contractDoc.getString("phoneNumber"));
                            }

                            Map<String, Object> contractMemberDoc = new HashMap<>();
                            contractMemberDoc.put("contractId", contractId);
                            contractMemberDoc.put("roomId", roomId);
                            contractMemberDoc.put("roomNumber", roomNumber);
                            contractMemberDoc.put("uid", user.getUid());
                            contractMemberDoc.put("fullName", fullName);
                            contractMemberDoc.put("phoneNumber", phoneForProfile);
                            contractMemberDoc.put("primaryContact", representative);
                            contractMemberDoc.put("contractRepresentative", representative);
                            contractMemberDoc.put("accountLinked", true);
                            contractMemberDoc.put("active", true);
                            contractMemberDoc.put("temporaryResident", false);
                            contractMemberDoc.put("createdAt", createdAt);
                            contractMemberDoc.put("updatedAt", nowMillis);

                            String personalId = userDoc.exists() ? safeTrim(userDoc.getString("personalId")) : "";
                            if (personalId.isEmpty() && contractDoc != null && representative) {
                                personalId = safeTrim(contractDoc.getString("personalId"));
                            }
                            if (!personalId.isEmpty()) {
                                contractMemberDoc.put("personalId", personalId);
                            }

                            boolean fullyDocumented = representative
                                    || (!fullName.isEmpty() && !phoneForProfile.isEmpty() && !personalId.isEmpty());
                            contractMemberDoc.put("fullyDocumented", fullyDocumented);

                            transaction.set(contractMemberRef, contractMemberDoc, SetOptions.merge());
                        }

                        Map<String, Object> inviteUpdate = new HashMap<>();
                        inviteUpdate.put("totalSlots", totalSlots);
                        inviteUpdate.put("remainingSlots", nextRemaining);
                        inviteUpdate.put("usageCount", nextUsage);
                        inviteUpdate.put("lastAcceptedAt", now);
                        inviteUpdate.put("lastAcceptedBy", user.getUid());
                        inviteUpdate.put("updatedAt", now);
                        inviteUpdate.put("status", nextRemaining <= 0 ? "CLOSED" : "PENDING");

                        Map<String, Object> userUpdate = new HashMap<>();
                        userUpdate.put("activeTenantId", tenantId);
                        userUpdate.put("primaryRole", role);
                        userUpdate.put("activeContractMemberRole", contractMemberRole);
                        userUpdate.put("updatedAt", now);

                        transaction.set(db.collection("tenants").document(tenantId).collection("members")
                                .document(user.getUid()), memberDoc, SetOptions.merge());
                        transaction.set(inviteRef, inviteUpdate, SetOptions.merge());
                        transaction.set(db.collection("users").document(user.getUid()), userUpdate, SetOptions.merge());
                        if (representative && activeContractRef != null) {
                            Map<String, Object> contractUpdate = new HashMap<>();
                            contractUpdate.put("representativeUid", user.getUid());
                            contractUpdate.put("representativePhone", accountPhone);
                            contractUpdate.put("updatedAt", now);
                            transaction.set(activeContractRef, contractUpdate, SetOptions.merge());
                        }

                        return new AnonymousJoinResult(tenantId, contractId, user.getUid(), representative);
                    }).addOnSuccessListener(joinResult -> {
                        TenantSession.setActiveTenantId(context, joinResult.tenantId);
                        if (joinResult.representative
                                && joinResult.contractId != null
                                && !joinResult.contractId.trim().isEmpty()) {
                            demoteStaleRepresentativeMembers(
                                    joinResult.tenantId,
                                    joinResult.contractId,
                                    joinResult.memberUid);
                        }
                        cb.onSuccess(joinResult.tenantId);
                    }).addOnFailureListener(e -> cb.onError(new Exception("Lỗi cập nhật dữ liệu: " + e.getMessage())));
                })
                .addOnFailureListener(e -> cb.onError(new Exception("Lỗi server, hãy thử lại: " + e.getMessage())));
    }

    // --- END ANONYMOUS INVITE METHODS ---

    public void joinByInvite(@NonNull android.content.Context context, @NonNull String tenantId, @NonNull String code,
            @NonNull JoinCallback cb) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            cb.onError(new IllegalStateException("User not logged in"));
            return;
        }

        String email = user.getEmail();
        if (email == null) {
            cb.onError(new IllegalStateException("User has no email"));
            return;
        }

        com.google.firebase.firestore.DocumentReference inviteRef = db.collection("tenants").document(tenantId)
                .collection("invites").document(code);

        inviteRef.get()
                .addOnSuccessListener(snap -> {
                    if (!snap.exists()) {
                        cb.onError(new IllegalStateException("Invite not found"));
                        return;
                    }

                    String status = snap.getString("status");
                    if (!"PENDING".equals(status)) {
                        cb.onError(new IllegalStateException("Invite not pending"));
                        return;
                    }

                    String inviteEmail = snap.getString("email");
                    if (inviteEmail == null || !inviteEmail.equalsIgnoreCase(email)) {
                        cb.onError(new IllegalStateException("Invite email mismatch"));
                        return;
                    }

                    String role = snap.getString("role");
                    if (role == null)
                        role = TenantRoles.STAFF;

                    String roomId = snap.getString("roomId");
                    String houseId = snap.getString("houseId");
                    String contractMemberRole = snap.getString("contractMemberRole");
                    if (TenantRoles.TENANT.equals(role) && (roomId == null || roomId.trim().isEmpty())) {
                        cb.onError(new IllegalStateException("Invite missing roomId"));
                        return;
                    }

                    boolean representative = CONTRACT_MEMBER_ROLE_REPRESENTATIVE
                            .equalsIgnoreCase(contractMemberRole != null ? contractMemberRole.trim() : "");

                    Map<String, Object> memberDoc = new HashMap<>();
                    memberDoc.put("uid", user.getUid());
                    memberDoc.put("role", role);
                        memberDoc.put("contractMemberRole",
                            representative ? CONTRACT_MEMBER_ROLE_REPRESENTATIVE : CONTRACT_MEMBER_ROLE_OCCUPANT);
                        memberDoc.put("primaryContact", representative);
                        memberDoc.put("contractRepresentative", representative);
                    memberDoc.put("status", "ACTIVE");
                    memberDoc.put("inviteCode", code);
                    if (TenantRoles.TENANT.equals(role)) {
                        memberDoc.put("roomId", roomId);
                        java.util.List<String> assignedRooms = new java.util.ArrayList<>();
                        assignedRooms.add(roomId);
                        memberDoc.put("assignedRoomIds", assignedRooms);
                    } else {
                        memberDoc.put("assignedRoomIds", new java.util.ArrayList<>());
                    }
                    if (houseId != null && !houseId.trim().isEmpty()) {
                        memberDoc.put("houseId", houseId.trim());
                        java.util.List<String> assignedHouses = new java.util.ArrayList<>();
                        assignedHouses.add(houseId.trim());
                        memberDoc.put("assignedHouseIds", assignedHouses);
                    } else {
                        memberDoc.put("assignedHouseIds", new java.util.ArrayList<>());
                    }
                    memberDoc.put("createdAt", Timestamp.now());
                    memberDoc.put("updatedAt", Timestamp.now());

                    Map<String, Object> inviteUpdate = new HashMap<>();
                    inviteUpdate.put("status", "ACCEPTED");
                    inviteUpdate.put("acceptedAt", Timestamp.now());
                    inviteUpdate.put("acceptedBy", user.getUid());

                    Map<String, Object> userUpdate = new HashMap<>();
                    userUpdate.put("activeTenantId", tenantId);
                    userUpdate.put("primaryRole", role);
                        userUpdate.put("activeContractMemberRole",
                            representative ? CONTRACT_MEMBER_ROLE_REPRESENTATIVE : CONTRACT_MEMBER_ROLE_OCCUPANT);
                    userUpdate.put("updatedAt", Timestamp.now());

                    db.runBatch(batch -> {
                        batch.set(db.collection("tenants").document(tenantId).collection("members")
                                .document(user.getUid()), memberDoc, SetOptions.merge());
                        batch.set(inviteRef, inviteUpdate, SetOptions.merge());
                        batch.set(db.collection("users").document(user.getUid()), userUpdate, SetOptions.merge());
                    }).addOnSuccessListener(v -> {
                        TenantSession.setActiveTenantId(context, tenantId);
                        cb.onSuccess(tenantId);
                    }).addOnFailureListener(cb::onError);
                })
                .addOnFailureListener(cb::onError);
    }

    private String generateCode(int len) {
        final String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private int resolveMemberCount(@NonNull DocumentSnapshot contractDoc) {
        Long memberCount = contractDoc.getLong("memberCount");
        if (memberCount == null || memberCount <= 0) {
            return 1;
        }
        return memberCount.intValue();
    }

    @NonNull
    private String normalizePhone(String value) {
        if (value == null) {
            return "";
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.startsWith("84") && digits.length() > 9) {
            digits = "0" + digits.substring(2);
        }
        return digits;
    }

    @NonNull
    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private void demoteStaleRepresentativeMembers(
            @NonNull String tenantId,
            @NonNull String contractId,
            @NonNull String currentMemberUid) {
        db.collection("tenants")
                .document(tenantId)
                .collection("contractMembers")
                .whereEqualTo("contractId", contractId)
                .whereEqualTo("active", true)
                .whereEqualTo("contractRepresentative", true)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || snapshot.isEmpty()) {
                        return;
                    }

                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    long nowMillis = System.currentTimeMillis();
                    boolean hasUpdate = false;

                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        if (currentMemberUid.equals(doc.getId())) {
                            continue;
                        }
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("contractRepresentative", false);
                        updates.put("primaryContact", false);
                        updates.put("active", false);
                        updates.put("updatedAt", nowMillis);
                        batch.set(doc.getReference(), updates, SetOptions.merge());
                        hasUpdate = true;
                    }

                    if (hasUpdate) {
                        batch.commit();
                    }
                });
    }
}
