package com.example.myapplication.repository;

import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.TenantSession;
import com.example.myapplication.model.Payment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class PaymentRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION = "payments";

    private CollectionReference getScopedCollection() {
        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId != null && !tenantId.isEmpty()) {
            return db.collection("tenants").document(tenantId).collection(COLLECTION);
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) throw new IllegalStateException("User not logged in");
        return db.collection("users").document(user.getUid()).collection(COLLECTION);
    }

    public MutableLiveData<List<Payment>> listByInvoice(String invoiceId) {
        MutableLiveData<List<Payment>> data = new MutableLiveData<>();
        getScopedCollection().whereEqualTo("invoiceId", invoiceId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null) return;
                    List<Payment> list = new ArrayList<>();
                    snapshot.forEach(doc -> {
                        Payment p = doc.toObject(Payment.class);
                        p.setId(doc.getId());
                        list.add(p);
                    });
                    data.setValue(list);
                });
        return data;
    }

    public void add(Payment payment, Runnable onSuccess, Runnable onFail) {
        getScopedCollection().add(payment)
                .addOnSuccessListener(ref -> onSuccess.run())
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
