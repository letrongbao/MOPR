package com.example.myapplication.repository;

import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.TenantSession;
import com.example.myapplication.model.Contract;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ContractRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION = "contracts";

    private CollectionReference getScopedCollection() {
        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId != null && !tenantId.isEmpty()) {
            return db.collection("tenants").document(tenantId).collection(COLLECTION);
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) throw new IllegalStateException("User not logged in");
        return db.collection("users").document(user.getUid()).collection(COLLECTION);
    }

    public MutableLiveData<List<Contract>> list() {
        MutableLiveData<List<Contract>> data = new MutableLiveData<>();
        getScopedCollection().addSnapshotListener((snapshot, e) -> {
            if (e != null || snapshot == null) return;
            List<Contract> list = new ArrayList<>();
            snapshot.forEach(doc -> {
                Contract c = doc.toObject(Contract.class);
                c.setId(doc.getId());
                list.add(c);
            });
            data.setValue(list);
        });
        return data;
    }

    public void add(Contract contract, Runnable onSuccess, Runnable onFail) {
        getScopedCollection().add(contract)
                .addOnSuccessListener(ref -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    public void update(Contract contract, Runnable onSuccess, Runnable onFail) {
        getScopedCollection().document(contract.getId()).set(contract)
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    public void delete(String id, Runnable onSuccess, Runnable onFail) {
        getScopedCollection().document(id).delete()
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }
}
