package com.example.myapplication.features.invoice;

import androidx.annotation.NonNull;

import com.example.myapplication.core.constants.TenantRoles;
import com.example.myapplication.core.session.TenantSession;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public final class InvoicePermissionResolver {

    public interface Callback {
        void onNoTenantContext();

        void onTenantSelfService(@NonNull String roomId);

        void onTenantMissingRoom();

        void onOwnerOrStaff();
    }

    private InvoicePermissionResolver() {
    }

    public static void resolve(@NonNull FirebaseFirestore db,
            boolean enableTenantSelfService,
            @NonNull Callback callback) {
        String tenantId = TenantSession.getActiveTenantId();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (tenantId == null || tenantId.trim().isEmpty() || user == null) {
            callback.onNoTenantContext();
            return;
        }

        db.collection("tenants").document(tenantId)
                .collection("members").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String role = doc != null ? doc.getString("role") : null;
                    boolean isTenant = TenantRoles.TENANT.equals(role);
                    String roomId = doc != null ? doc.getString("roomId") : null;

                    if (isTenant && enableTenantSelfService) {
                        if (roomId == null || roomId.trim().isEmpty()) {
                            callback.onTenantMissingRoom();
                            return;
                        }
                        callback.onTenantSelfService(roomId);
                        return;
                    }

                    callback.onOwnerOrStaff();
                })
                .addOnFailureListener(e -> callback.onOwnerOrStaff());
    }
}
