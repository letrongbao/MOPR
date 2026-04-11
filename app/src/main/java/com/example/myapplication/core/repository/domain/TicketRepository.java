package com.example.myapplication.core.repository.domain;

import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.domain.Ticket;
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

public class TicketRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION = "tickets";

    private CollectionReference getScopedCollection() {
        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId != null && !tenantId.isEmpty()) {
            return db.collection("tenants").document(tenantId).collection(COLLECTION);
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) throw new IllegalStateException("User not logged in");
        return db.collection("users").document(user.getUid()).collection(COLLECTION);
    }

    public MutableLiveData<List<Ticket>> listAll() {
        MutableLiveData<List<Ticket>> data = new MutableLiveData<>();
        getScopedCollection().orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null) return;
                    List<Ticket> list = new ArrayList<>();
                    snapshot.forEach(doc -> {
                        Ticket t = doc.toObject(Ticket.class);
                        t.setId(doc.getId());
                        list.add(t);
                    });
                    data.setValue(list);
                });
        return data;
    }

    public MutableLiveData<List<Ticket>> listByRoom(String roomId) {
        MutableLiveData<List<Ticket>> data = new MutableLiveData<>();
        getScopedCollection().whereEqualTo("roomId", roomId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null) return;
                    List<Ticket> list = new ArrayList<>();
                    snapshot.forEach(doc -> {
                        Ticket t = doc.toObject(Ticket.class);
                        t.setId(doc.getId());
                        list.add(t);
                    });
                    data.setValue(list);
                });
        return data;
    }

    public void add(Ticket ticket, Runnable onSuccess, Runnable onFail) {
        if (ticket.getCreatedAt() == null) ticket.setCreatedAt(Timestamp.now());
        ticket.setUpdatedAt(Timestamp.now());

        getScopedCollection().add(ticket)
                .addOnSuccessListener(ref -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    public void updateStatus(String id, String status, Runnable onSuccess, Runnable onFail) {
        if (id == null || id.trim().isEmpty()) {
            onFail.run();
            return;
        }
        getScopedCollection().document(id)
                .update("status", status, "updatedAt", Timestamp.now())
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    public void updateFields(String id, Map<String, Object> updates, Runnable onSuccess, Runnable onFail) {
        if (id == null || id.trim().isEmpty()) {
            onFail.run();
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        if (updates != null) {
            payload.putAll(updates);
        }
        payload.put("updatedAt", Timestamp.now());

        getScopedCollection().document(id)
                .update(payload)
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
