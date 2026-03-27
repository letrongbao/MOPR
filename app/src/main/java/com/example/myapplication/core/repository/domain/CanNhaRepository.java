package com.example.myapplication.core.repository.domain;

import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.domain.CanNha;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class CanNhaRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION = "khu_tro";

    private CollectionReference scopedCollection() {
        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId != null && !tenantId.isEmpty()) {
            return db.collection("tenants").document(tenantId).collection(COLLECTION);
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
            throw new IllegalStateException("User not logged in");
        return db.collection("users").document(user.getUid()).collection(COLLECTION);
    }

    public MutableLiveData<List<CanNha>> listAll() {
        MutableLiveData<List<CanNha>> data = new MutableLiveData<>();
        scopedCollection().addSnapshotListener((snap, e) -> {
            if (e != null || snap == null)
                return;
            List<CanNha> list = new ArrayList<>();
            snap.forEach(doc -> {
                CanNha k = doc.toObject(CanNha.class);
                k.setId(doc.getId());
                list.add(k);
            });
            data.setValue(list);
        });
        return data;
    }

    public void add(CanNha k, Runnable onSuccess, Runnable onFail) {
        // Set creation timestamp
        k.setCreatedAt(com.google.firebase.Timestamp.now());
        k.setUpdatedAt(com.google.firebase.Timestamp.now());

        scopedCollection().add(k)
                .addOnSuccessListener(r -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    public void update(CanNha k, Runnable onSuccess, Runnable onFail) {
        // Set update timestamp
        k.setUpdatedAt(com.google.firebase.Timestamp.now());

        scopedCollection().document(k.getId()).set(k)
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    public void delete(String id, Runnable onSuccess, Runnable onFail) {
        scopedCollection().document(id).delete()
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }
}
