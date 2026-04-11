package com.example.myapplication.core.repository.domain;

import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.domain.Payment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PaymentRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION = "payments";

    public MutableLiveData<List<Payment>> getPaymentList() {
        MutableLiveData<List<Payment>> data = new MutableLiveData<>(new ArrayList<>());
        CollectionReference ref = getScopedCollection();
        if (ref == null) {
            return data;
        }

        ref.addSnapshotListener((value, error) -> {
            if (error != null) {
                return;
            }
            if (value != null) {
                List<Payment> list = new ArrayList<>();
                value.forEach(doc -> {
                    Payment item = doc.toObject(Payment.class);
                    item.setId(doc.getId());
                    list.add(item);
                });
                data.postValue(list);
            }
        });
        return data;
    }

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

    public MutableLiveData<List<Payment>> listByInvoice(String invoiceId) {
        MutableLiveData<List<Payment>> data = new MutableLiveData<>();
        getScopedCollection().whereEqualTo("invoiceId", invoiceId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null)
                        return;
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

    public MutableLiveData<List<Payment>> listByRoom(String roomId) {
        MutableLiveData<List<Payment>> data = new MutableLiveData<>();
        getScopedCollection().whereEqualTo("roomId", roomId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null)
                        return;
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
        getScopedCollection().add(toPayload(payment))
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

    public void updateInvoiceStatus(String invoiceId, String status) {
        if (invoiceId == null || invoiceId.trim().isEmpty() || status == null || status.trim().isEmpty()) {
            return;
        }

        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId != null && !tenantId.trim().isEmpty()) {
            db.collection("tenants").document(tenantId)
                    .collection("invoices").document(invoiceId)
                    .update("status", status.trim());
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        db.collection("users").document(user.getUid())
                .collection("invoices").document(invoiceId)
                .update("status", status.trim());
    }

    private Map<String, Object> toPayload(Payment payment) {
        Map<String, Object> payload = new HashMap<>();
        putIfNotBlank(payload, "invoiceId", payment.getInvoiceId());
        putIfNotBlank(payload, "roomId", payment.getRoomId());
        if (payment.getAmount() > 0) {
            payload.put("amount", payment.getAmount());
        }
        putIfNotBlank(payload, "method", payment.getMethod());
        putIfNotBlank(payload, "paidAt", payment.getPaidAt());
        putIfNotBlank(payload, "note", payment.getNote());
        return payload;
    }

    private static void putIfNotBlank(Map<String, Object> payload, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            payload.put(key, value.trim());
        }
    }
}
