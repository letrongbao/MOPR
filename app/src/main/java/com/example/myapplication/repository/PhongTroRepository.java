package com.example.myapplication.repository;

import androidx.lifecycle.MutableLiveData;
import com.example.myapplication.TenantSession;
import com.example.myapplication.model.PhongTro;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class PhongTroRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION = "phong_tro";

    private CollectionReference getUserCollection() {
        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId != null && !tenantId.isEmpty()) {
            return db.collection("tenants").document(tenantId).collection(COLLECTION);
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) throw new IllegalStateException("User not logged in");
        return db.collection("users").document(user.getUid()).collection(COLLECTION);
    }

    public MutableLiveData<List<PhongTro>> layDanhSachPhong() {
        MutableLiveData<List<PhongTro>> data = new MutableLiveData<>();
        getUserCollection().addSnapshotListener((snapshot, e) -> {
            if (e != null || snapshot == null) return;
            List<PhongTro> list = new ArrayList<>();
            snapshot.forEach(doc -> {
                PhongTro p = doc.toObject(PhongTro.class);
                p.setId(doc.getId());
                list.add(p);
            });
            data.setValue(list);
        });
        return data;
    }

    public void themPhong(PhongTro phong, Runnable onSuccess, Runnable onFail) {
        getUserCollection().add(phong)
                .addOnSuccessListener(ref -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    public void capNhatPhong(PhongTro phong, Runnable onSuccess, Runnable onFail) {
        getUserCollection().document(phong.getId()).set(phong)
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    public void xoaPhong(String id, Runnable onSuccess, Runnable onFail) {
        getUserCollection().document(id).delete()
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }
}
