package com.example.myapplication;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AuditLogRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void log(@NonNull String action, @NonNull String entity, @NonNull String entityId, Map<String, Object> extra) {
        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId == null || tenantId.isEmpty()) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        Map<String, Object> doc = new HashMap<>();
        doc.put("action", action);
        doc.put("entity", entity);
        doc.put("entityId", entityId);
        doc.put("uid", user.getUid());
        doc.put("at", Timestamp.now());
        if (extra != null && !extra.isEmpty()) doc.put("extra", extra);

        db.collection("tenants").document(tenantId)
                .collection("auditLogs")
                .add(doc);
    }
}
