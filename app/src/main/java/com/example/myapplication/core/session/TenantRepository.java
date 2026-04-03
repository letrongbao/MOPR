package com.example.myapplication.core.session;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.myapplication.core.constants.TenantRoles;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TenantRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static class TenantInfo {
        public final String id;
        public final String name;

        public TenantInfo(@NonNull String id, @NonNull String name) {
            this.id = id;
            this.name = name;
        }
    }

    public interface TenantReadyCallback {
        void onReady(@NonNull String tenantId);

        void onError(@NonNull Exception e);
    }

    public interface TenantListCallback {
        void onSuccess(@NonNull List<TenantInfo> tenants);

        void onError(@NonNull Exception e);
    }

    public void ensureActiveTenant(@NonNull Context context, @NonNull TenantReadyCallback cb) {
        TenantSession.init(context);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            cb.onError(new IllegalStateException("User not logged in"));
            return;
        }

        String cached = TenantSession.getActiveTenantId();
        if (cached != null && !cached.isEmpty()) {
            cb.onReady(cached);
            return;
        }

        String uid = user.getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    String tenantId = doc.getString("activeTenantId");
                    if (tenantId != null && !tenantId.isEmpty()) {
                        TenantSession.setActiveTenantId(context, tenantId);
                        cb.onReady(tenantId);
                    } else {
                        createDefaultTenant(context, user, cb);
                    }
                })
                .addOnFailureListener(cb::onError);
    }

    public void createTenant(@NonNull Context context, @NonNull String tenantName, @NonNull TenantReadyCallback cb) {
        TenantSession.init(context);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            cb.onError(new IllegalStateException("User not logged in"));
            return;
        }

        DocumentReference tenantRef = db.collection("tenants").document();
        String tenantId = tenantRef.getId();

        String ownerUid = user.getUid();

        Map<String, Object> tenantDoc = new HashMap<>();
        tenantDoc.put("name", tenantName);
        tenantDoc.put("ownerUid", ownerUid);
        tenantDoc.put("timezone", "Asia/Ho_Chi_Minh");
        tenantDoc.put("currency", "VND");
        tenantDoc.put("billingCycleDay", 1);

        // Phase 4 (no billing yet): simple plan limits (client-enforced)
        tenantDoc.put("plan", "FREE");
        tenantDoc.put("maxRooms", 50);
        tenantDoc.put("maxStaff", 3);
        tenantDoc.put("maxInvoicesPerMonth", 200);

        tenantDoc.put("createdAt", Timestamp.now());

        Map<String, Object> memberDoc = new HashMap<>();
        memberDoc.put("uid", ownerUid);
        memberDoc.put("role", TenantRoles.OWNER);
        memberDoc.put("status", "ACTIVE");
        memberDoc.put("assignedHouseIds", new java.util.ArrayList<>());
        memberDoc.put("assignedRoomIds", new java.util.ArrayList<>());
        memberDoc.put("createdAt", Timestamp.now());
        memberDoc.put("updatedAt", Timestamp.now());

        Map<String, Object> userUpdate = new HashMap<>();
        userUpdate.put("activeTenantId", tenantId);
        userUpdate.put("primaryRole", TenantRoles.OWNER);
        userUpdate.put("updatedAt", Timestamp.now());

        // 1) create tenant doc
        tenantRef.set(tenantDoc)
                .addOnSuccessListener(v -> {
                    // 2) create owner membership
                    tenantRef.collection("members").document(ownerUid)
                            .set(memberDoc)
                            .addOnSuccessListener(v2 -> {
                                // 3) update user.activeTenantId
                                db.collection("users").document(ownerUid)
                                        .set(userUpdate, SetOptions.merge())
                                        .addOnSuccessListener(v3 -> {
                                            TenantSession.setActiveTenantId(context, tenantId);
                                            cb.onReady(tenantId);
                                        })
                                        .addOnFailureListener(cb::onError);
                            })
                            .addOnFailureListener(cb::onError);
                })
                .addOnFailureListener(cb::onError);
    }

    private void createDefaultTenant(@NonNull Context context, @NonNull FirebaseUser user,
            @NonNull TenantReadyCallback cb) {
        String displayName = user.getDisplayName();
        String defaultName = (displayName != null && !displayName.trim().isEmpty())
                ? ("Nhà trọ của " + displayName.trim())
                : "Nhà trọ";

        createTenant(context, defaultName, new TenantReadyCallback() {
            @Override
            public void onReady(@NonNull String tenantId) {
                migrateLegacyData(user.getUid(), tenantId, () -> cb.onReady(tenantId));
            }

            @Override
            public void onError(@NonNull Exception e) {
                cb.onError(e);
            }
        });
    }

    public void setActiveTenant(@NonNull Context context, @NonNull String tenantId, @NonNull TenantReadyCallback cb) {
        TenantSession.setActiveTenantId(context, tenantId);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            cb.onError(new IllegalStateException("User not logged in"));
            return;
        }

        Map<String, Object> update = new HashMap<>();
        update.put("activeTenantId", tenantId);
        update.put("updatedAt", Timestamp.now());
        db.collection("users").document(user.getUid())
                .set(update, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onReady(tenantId))
                .addOnFailureListener(cb::onError);
    }

    public void listMyTenants(@NonNull TenantListCallback cb) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            cb.onError(new IllegalStateException("User not logged in"));
            return;
        }

        String uid = user.getUid();
        db.collectionGroup("members")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener(qs -> {
                    List<DocumentSnapshot> memberDocs = qs.getDocuments();
                    if (memberDocs == null || memberDocs.isEmpty()) {
                        cb.onSuccess(new ArrayList<>());
                        return;
                    }

                    List<String> tenantIds = new ArrayList<>();
                    List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
                    for (DocumentSnapshot memberDoc : memberDocs) {
                        DocumentReference tenantRef = memberDoc.getReference().getParent().getParent();
                        if (tenantRef == null)
                            continue;
                        tenantIds.add(tenantRef.getId());
                        tasks.add(tenantRef.get());
                    }

                    Tasks.whenAllSuccess(tasks)
                            .addOnSuccessListener(results -> {
                                List<TenantInfo> tenants = new ArrayList<>();
                                for (int i = 0; i < results.size(); i++) {
                                    DocumentSnapshot tdoc = (DocumentSnapshot) results.get(i);
                                    String name = tdoc.getString("name");
                                    if (name == null || name.trim().isEmpty())
                                        name = tenantIds.get(i);
                                    tenants.add(new TenantInfo(tenantIds.get(i), name));
                                }
                                cb.onSuccess(tenants);
                            })
                            .addOnFailureListener(cb::onError);
                })
                .addOnFailureListener(cb::onError);
    }

    private void migrateLegacyData(@NonNull String uid, @NonNull String tenantId, @NonNull Runnable onDone) {
        migrateCollection(uid, tenantId, "phong_tro", () -> migrateCollection(uid, tenantId, "nguoi_thue",
                () -> migrateCollection(uid, tenantId, "hoa_don", onDone)));
    }

    private void migrateCollection(@NonNull String uid, @NonNull String tenantId, @NonNull String collectionName,
            @NonNull Runnable next) {
        db.collection("users").document(uid).collection(collectionName)
                .get()
                .addOnSuccessListener(qs -> {
                    List<DocumentSnapshot> docs = qs.getDocuments();
                    if (docs == null || docs.isEmpty()) {
                        next.run();
                        return;
                    }

                    WriteBatch batch = db.batch();
                    int opCount = 0;
                    for (DocumentSnapshot doc : docs) {
                        Map<String, Object> data = doc.getData();
                        if (data == null)
                            continue;

                        batch.set(
                                db.collection("tenants").document(tenantId).collection(collectionName)
                                        .document(doc.getId()),
                                data,
                                SetOptions.merge());
                        opCount++;
                        if (opCount >= 450)
                            break; // keep it simple (avoid batch limit)
                    }

                    batch.commit()
                            .addOnSuccessListener(v -> next.run())
                            .addOnFailureListener(e -> next.run());
                })
                .addOnFailureListener(e -> next.run());
    }
}
