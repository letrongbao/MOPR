package com.example.myapplication.repository;

import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.TenantSession;
import com.example.myapplication.model.KhuTro;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class KhuTroRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION = "khu_tro";

    private CollectionReference scopedCollection() {
        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId != null && !tenantId.isEmpty()) {
            return db.collection("tenants").document(tenantId).collection(COLLECTION);
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) throw new IllegalStateException("User not logged in");
        return db.collection("users").document(user.getUid()).collection(COLLECTION);
    }

    public MutableLiveData<List<KhuTro>> listAll() {
        MutableLiveData<List<KhuTro>> data = new MutableLiveData<>();
        scopedCollection().addSnapshotListener((snap, e) -> {
            if (e != null || snap == null) return;
            List<KhuTro> list = new ArrayList<>();
            snap.forEach(doc -> {
                KhuTro k = doc.toObject(KhuTro.class);
                k.setId(doc.getId());
                list.add(k);
            });
            data.setValue(list);
        });
        return data;
    }

    public void add(KhuTro k, Runnable onSuccess, Runnable onFail) {
        scopedCollection().add(k)
                .addOnSuccessListener(r -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    public void update(KhuTro k, Runnable onSuccess, Runnable onFail) {
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
