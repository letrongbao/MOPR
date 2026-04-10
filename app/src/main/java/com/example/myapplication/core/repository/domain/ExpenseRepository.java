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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpenseRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION = "expenses";

    private CollectionReference getScopedCollection() {
        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId != null && !tenantId.isEmpty()) {
            return db.collection("tenants").document(tenantId).collection(COLLECTION);
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
            throw new IllegalStateException("User not logged in");
        return db.collection("users").document(user.getUid()).collection(COLLECTION);
    }

    public MutableLiveData<List<Expense>> listAll() {
        MutableLiveData<List<Expense>> data = new MutableLiveData<>();
        getScopedCollection()
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null)
                        return;
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

    public void add(Expense expense, Runnable onSuccess, Runnable onFail) {
        if (expense.getCreatedAt() == null)
            expense.setCreatedAt(Timestamp.now());
        getScopedCollection().add(toPayload(expense))
                .addOnSuccessListener(ref -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    public void update(Expense expense, Runnable onSuccess, Runnable onFail) {
        if (expense.getId() == null || expense.getId().trim().isEmpty()) {
            onFail.run();
            return;
        }
        getScopedCollection().document(expense.getId()).set(toPayload(expense))
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

    private Map<String, Object> toPayload(Expense expense) {
        Map<String, Object> payload = new HashMap<>();
        putIfNotBlank(payload, "category", expense.getCategory());
        if (expense.getAmount() > 0) {
            payload.put("amount", expense.getAmount());
        }
        putIfNotBlank(payload, "paidAt", expense.getPaidAt());
        putIfNotBlank(payload, "periodMonth", expense.getPeriodMonth());
        putIfNotBlank(payload, "status", expense.getStatus());
        putIfNotBlank(payload, "houseId", expense.getHouseId());
        putIfNotBlank(payload, "note", expense.getNote());
        if (expense.getCreatedAt() != null) {
            payload.put("createdAt", expense.getCreatedAt());
        }
        return payload;
    }

    private static void putIfNotBlank(Map<String, Object> payload, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            payload.put(key, value.trim());
        }
    }
}
