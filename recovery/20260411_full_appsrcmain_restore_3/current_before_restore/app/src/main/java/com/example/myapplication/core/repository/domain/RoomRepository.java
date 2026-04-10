package com.example.myapplication.core.repository.domain;

import androidx.lifecycle.MutableLiveData;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.domain.Room;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION = "rooms";

    private CollectionReference getUserCollection() {
        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId != null && !tenantId.isEmpty()) {
            return db.collection("tenants").document(tenantId).collection(COLLECTION);
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
            throw new IllegalStateException("User not logged in");
        return db.collection("users").document(user.getUid()).collection(COLLECTION);
    }

    public MutableLiveData<List<Room>> getRoomList() {
        MutableLiveData<List<Room>> data = new MutableLiveData<>();
        getUserCollection().addSnapshotListener((snapshot, e) -> {
            if (e != null || snapshot == null)
                return;
            List<Room> list = new ArrayList<>();
            snapshot.forEach(doc -> {
                Room p = doc.toObject(Room.class);
                p.setId(doc.getId());
                list.add(p);
            });
            data.setValue(list);
        });
        return data;
    }

    public void addRoom(Room room, Runnable onSuccess, Runnable onFail) {
        // Set creation timestamp
        room.setCreatedAt(com.google.firebase.Timestamp.now());
        room.setUpdatedAt(com.google.firebase.Timestamp.now());

        getUserCollection().add(toPayload(room))
                .addOnSuccessListener(ref -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    public void updateRoom(Room room, Runnable onSuccess, Runnable onFail) {
        // Set update timestamp
        room.setUpdatedAt(com.google.firebase.Timestamp.now());

        getUserCollection().document(room.getId()).set(toPayload(room))
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    public void deleteRoom(String id, Runnable onSuccess, Runnable onFail) {
        getUserCollection().document(id).delete()
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    private Map<String, Object> toPayload(Room room) {
        Map<String, Object> payload = new HashMap<>();

        putIfNotBlank(payload, "roomNumber", room.getRoomNumber());
        putIfNotBlank(payload, "roomType", room.getRoomType());
        putIfPositive(payload, "area", room.getArea());
        putIfPositive(payload, "rentAmount", room.getRentAmount());
        putIfNotBlank(payload, "status", room.getStatus());
        putIfNotBlank(payload, "imageUrl", room.getImageUrl());
        putIfNotBlank(payload, "houseId", room.getHouseId());
        putIfNotBlank(payload, "houseName", room.getHouseName());
        putIfPositive(payload, "floor", room.getFloor());
        putIfNotBlank(payload, "description", room.getDescription());

        List<String> amenities = room.getAmenities();
        if (amenities != null && !amenities.isEmpty()) {
            List<String> cleaned = new ArrayList<>();
            for (String amenity : amenities) {
                String value = trimToEmpty(amenity);
                if (!value.isEmpty()) {
                    cleaned.add(value);
                }
            }
            if (!cleaned.isEmpty()) {
                payload.put("amenities", cleaned);
            }
        }

        putIfPositive(payload, "maxOccupancy", room.getMaxOccupancy());
        if (room.getCreatedAt() != null) {
            payload.put("createdAt", room.getCreatedAt());
        }
        if (room.getUpdatedAt() != null) {
            payload.put("updatedAt", room.getUpdatedAt());
        }
        return payload;
    }

    private static void putIfPositive(Map<String, Object> payload, String key, double value) {
        if (value > 0) {
            payload.put(key, value);
        }
    }

    private static void putIfNotBlank(Map<String, Object> payload, String key, String value) {
        String trimmed = trimToEmpty(value);
        if (!trimmed.isEmpty()) {
            payload.put(key, trimmed);
        }
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
