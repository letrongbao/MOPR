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
                    if (TenantRoles.TENANT.equals(role) && (roomId == null || roomId.trim().isEmpty())) {
                        cb.onError(new IllegalStateException("Invite missing roomId"));
                        return;
                    }

                    Map<String, Object> memberDoc = new HashMap<>();
                    memberDoc.put("uid", user.getUid());
                    memberDoc.put("role", role);
                    memberDoc.put("inviteCode", code);
                    if (TenantRoles.TENANT.equals(role))
                        memberDoc.put("roomId", roomId);
                    memberDoc.put("createdAt", Timestamp.now());

                    Map<String, Object> inviteUpdate = new HashMap<>();
                    inviteUpdate.put("status", "ACCEPTED");
                    inviteUpdate.put("acceptedAt", Timestamp.now());
                    inviteUpdate.put("acceptedBy", user.getUid());

                    Map<String, Object> userUpdate = new HashMap<>();
                    userUpdate.put("activeTenantId", tenantId);

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
