package com.example.myapplication.core.repository.domain;

import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.domain.Expense;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ExpenseRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION = "chi_phi";

    private CollectionReference getScopedCollection() {
        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId != null && !tenantId.isEmpty()) {
            return db.collection("tenants").document(tenantId).collection(COLLECTION);
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) throw new IllegalStateException("User not logged in");
        return db.collection("users").document(user.getUid()).collection(COLLECTION);
    }

    public MutableLiveData<List<Expense>> listAll() {
        MutableLiveData<List<Expense>> data = new MutableLiveData<>();
        getScopedCollection()
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null) return;
                    List<Expense> list = new ArrayList<>();
                    snapshot.forEach(doc -> {
                        Expense cp = doc.toObject(Expense.class);
                        cp.setId(doc.getId());
                        list.add(cp);
                    });
                    data.setValue(list);
                });
        return data;
    }

    public void add(Expense chiPhi, Runnable onSuccess, Runnable onFail) {
        if (chiPhi.getCreatedAt() == null) chiPhi.setCreatedAt(Timestamp.now());
        getScopedCollection().add(chiPhi)
                .addOnSuccessListener(ref -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    public void update(Expense chiPhi, Runnable onSuccess, Runnable onFail) {
        if (chiPhi.getId() == null || chiPhi.getId().trim().isEmpty()) {
            onFail.run();
            return;
        }
        getScopedCollection().document(chiPhi.getId()).set(chiPhi)
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    public void delete(String id, Runnable onSuccess, Runnable onFail) {
        if (id == null || id.trim().isEmpty()) {
            onFail.run();
            return;
        }
        getScopedCollection().document(id).delete()
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }
}

