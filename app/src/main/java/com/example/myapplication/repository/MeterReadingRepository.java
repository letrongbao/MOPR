package com.example.myapplication.repository;

import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.TenantSession;
import com.example.myapplication.model.MeterReading;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MeterReadingRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION = "meterReadings";

    private CollectionReference getScopedCollection() {
        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId != null && !tenantId.isEmpty()) {
            return db.collection("tenants").document(tenantId).collection(COLLECTION);
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) throw new IllegalStateException("User not logged in");
        return db.collection("users").document(user.getUid()).collection(COLLECTION);
    }

    public MutableLiveData<List<MeterReading>> listByRoom(String roomId) {
        MutableLiveData<List<MeterReading>> data = new MutableLiveData<>();
        getScopedCollection().whereEqualTo("roomId", roomId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null) return;
                    List<MeterReading> list = new ArrayList<>();
                    snapshot.forEach(doc -> {
                        MeterReading r = doc.toObject(MeterReading.class);
                        r.setId(doc.getId());
                        list.add(r);
                    });
                    data.setValue(list);
                });
        return data;
    }

    public void add(MeterReading reading, Runnable onSuccess, Runnable onFail) {
        getScopedCollection().add(reading)
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

    public void createIfAbsent(String docId, MeterReading reading, Runnable onSuccess, Runnable onDuplicate, Runnable onFail) {
        if (docId == null || docId.trim().isEmpty()) {
            onFail.run();
            return;
        }
        DocumentReference ref = getScopedCollection().document(docId);
        db.runTransaction(transaction -> {
                    DocumentSnapshot snap = transaction.get(ref);
                    if (snap.exists()) {
                        throw new RuntimeException("DUPLICATE_METER");
                    }
                    transaction.set(ref, reading);
                    return null;
                })
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> {
                    String msg = e != null ? e.getMessage() : null;
                    if (msg != null && msg.contains("DUPLICATE_METER")) {
                        onDuplicate.run();
                    } else {
                        onFail.run();
                    }
                });
    }
}
