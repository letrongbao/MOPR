package com.example.myapplication.core.session;

import androidx.annotation.NonNull;

import com.example.myapplication.core.constants.TenantRoles;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class InviteRepository {
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

        String code = generateCode(8);

        Map<String, Object> invite = new HashMap<>();
        invite.put("code", code);
        invite.put("type", "ANONYMOUS"); // Phân biệt mã ẩn danh
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

                    // Do mã (code) sinh ra ngẫu nhiên dài 8 ký tự, khả năng trùng lặp status
                    // PENDING rất thấp.
                    // Lấy document đầu tiên tìm thấy.
                    com.google.firebase.firestore.DocumentSnapshot snap = querySnapshot.getDocuments().get(0);

                    // Suy xuất đường dẫn để lấy tenantId
                    // Đường dẫn mẫu: tenants/{tenantId}/invites/{code}
                    com.google.firebase.firestore.DocumentReference inviteRef = snap.getReference();
                    String tenantId = inviteRef.getParent().getParent().getId();

                    String role = snap.getString("role");
                    if (role == null)
                        role = TenantRoles.TENANT;

                    String roomId = snap.getString("roomId");
                    String houseId = snap.getString("houseId");
                    if (TenantRoles.TENANT.equals(role) && (roomId == null || roomId.trim().isEmpty())) {
                        cb.onError(new IllegalStateException("Lỗi dữ liệu: Mã mời không đính kèm phòng."));
                        return;
                    }

                    // 1. Tạo document Khách vào sub-collection "members" của chủ nhà
                    Map<String, Object> memberDoc = new HashMap<>();
                    memberDoc.put("uid", user.getUid());
                    memberDoc.put("role", role);
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

                    // 2. Chuyển trạng thái Mã mời thành Đã sử dụng (ACCEPTED)
                    Map<String, Object> inviteUpdate = new HashMap<>();
                    inviteUpdate.put("status", "ACCEPTED");
                    inviteUpdate.put("acceptedAt", Timestamp.now());
                    inviteUpdate.put("acceptedBy", user.getUid());

                    // 3. Cập nhật Root User của vị khách (Thêm primaryRole và activeTenantId)
                    Map<String, Object> userUpdate = new HashMap<>();
                    userUpdate.put("activeTenantId", tenantId);
                    userUpdate.put("primaryRole", role);
                    userUpdate.put("updatedAt", Timestamp.now());

                    // Đẩy dữ liệu theo lô (Batch)
                    db.runBatch(batch -> {
                        batch.set(db.collection("tenants").document(tenantId).collection("members")
                                .document(user.getUid()), memberDoc, SetOptions.merge());
                        batch.set(inviteRef, inviteUpdate, SetOptions.merge());
                        batch.set(db.collection("users").document(user.getUid()), userUpdate, SetOptions.merge());
                    }).addOnSuccessListener(v -> {
                        // Thành công -> Lưu dữ liệu cache / session
                        TenantSession.setActiveTenantId(context, tenantId);
                        cb.onSuccess(tenantId);
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
                    if (TenantRoles.TENANT.equals(role) && (roomId == null || roomId.trim().isEmpty())) {
                        cb.onError(new IllegalStateException("Invite missing roomId"));
                        return;
                    }

                    Map<String, Object> memberDoc = new HashMap<>();
                    memberDoc.put("uid", user.getUid());
                    memberDoc.put("role", role);
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
}
