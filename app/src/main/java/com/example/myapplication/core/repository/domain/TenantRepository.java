package com.example.myapplication.core.repository.domain;

import androidx.lifecycle.MutableLiveData;
import android.util.Log;
import com.example.myapplication.core.constants.RoomStatus;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.domain.Tenant;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TenantRepository {

    private static final String TAG = "TenantRepository";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION = "contracts";

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

    public MutableLiveData<List<Tenant>> getTenantList() {
        MutableLiveData<List<Tenant>> data = new MutableLiveData<>();
        getUserCollection().addSnapshotListener((snapshot, e) -> {
            if (e != null || snapshot == null)
                return;
            List<Tenant> list = new ArrayList<>();
            snapshot.forEach(doc -> {
                Tenant n = safeToTenant(doc);
                if (n != null) {
                    list.add(n);
                }
            });
            data.setValue(list);
        });
        return data;
    }

    public MutableLiveData<List<Tenant>> getTenantsByRoom(String roomId) {
        MutableLiveData<List<Tenant>> data = new MutableLiveData<>();
        getUserCollection().whereEqualTo("roomId", roomId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null)
                        return;
                    List<Tenant> list = new ArrayList<>();
                    snapshot.forEach(doc -> {
                        Tenant n = safeToTenant(doc);
                        if (n != null) {
                            list.add(n);
                        }
                    });
                    data.setValue(list);
                });
        return data;
    }

    /**
     * Internal note.
     */
    public MutableLiveData<List<Tenant>> getRentalHistory() {
        MutableLiveData<List<Tenant>> data = new MutableLiveData<>();
        getUserCollection().whereEqualTo("contractStatus", "ENDED")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null) {
                        data.setValue(new ArrayList<>());
                        return;
                    }
                    List<Tenant> list = new ArrayList<>();
                    snapshot.forEach(doc -> {
                        Tenant n = safeToTenant(doc);
                        if (n != null) {
                            list.add(n);
                        }
                    });
                    // Internal note.
                    list.sort((a, b) -> Long.compare(nullSafeTime(b.getEndedAt()), nullSafeTime(a.getEndedAt())));
                    data.setValue(list);
                });
        return data;
    }

    private Tenant safeToTenant(DocumentSnapshot doc) {
        try {
            Tenant tenant = doc.toObject(Tenant.class);
            if (tenant == null) {
                return null;
            }
            tenant.setId(doc.getId());
            return tenant;
        } catch (RuntimeException ex) {
            Log.w(TAG, "Skip malformed contract document: " + doc.getId(), ex);
            return null;
        }
    }

    private long nullSafeTime(Long value) {
        return value != null ? value : 0L;
    }

    public void addTenant(Tenant tenant, Runnable onSuccess, Runnable onFail) {
        getUserCollection().add(toPayload(tenant))
                .addOnSuccessListener(ref -> {
                    // If tenant is ACTIVE, atomically update room status
                    if ("ACTIVE".equals(tenant.getContractStatus()) && tenant.getRoomId() != null) {
                        updateRoomStatusAtomic(tenant.getRoomId(), RoomStatus.RENTED, onSuccess, onFail);
                    } else {
                        onSuccess.run();
                    }
                })
                .addOnFailureListener(e -> onFail.run());
    }

    public void updateTenant(Tenant tenant, Runnable onSuccess, Runnable onFail) {
        getUserCollection().document(tenant.getId()).set(toPayload(tenant))
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    /**
     * End contract atomically - updates tenant status AND room status in
     * transaction
     */
    public void endContractAtomic(String tenantId, String roomId, Runnable onSuccess, Runnable onFail) {
        DocumentReference tenantRef = getUserCollection().document(tenantId);
        DocumentReference roomRef = getRoomCollection().document(roomId);

        db.runTransaction(transaction -> {
            // Update tenant
            Map<String, Object> tenantUpdates = new HashMap<>();
            tenantUpdates.put("contractStatus", "ENDED");
            tenantUpdates.put("endedAt", System.currentTimeMillis());
            tenantUpdates.put("updatedAt", System.currentTimeMillis());
            tenantUpdates.put("previousRoomId", roomId); // Save room history
            tenantUpdates.put("roomId", null); // Clear current room
            transaction.update(tenantRef, tenantUpdates);

            // Update room status
            Map<String, Object> roomUpdates = new HashMap<>();
            roomUpdates.put("status", RoomStatus.VACANT);
            roomUpdates.put("updatedAt", com.google.firebase.Timestamp.now());
            transaction.update(roomRef, roomUpdates);

            return null;
        })
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    /**
     * Start new contract atomically - updates tenant AND room status
     */
    public void startContractAtomic(Tenant tenant, Runnable onSuccess, Runnable onFail) {
        if (tenant.getRoomId() == null) {
            onFail.run();
            return;
        }

        DocumentReference tenantRef = tenant.getId() != null ? getUserCollection().document(tenant.getId())
                : getUserCollection().document();
        DocumentReference roomRef = getRoomCollection().document(tenant.getRoomId());

        db.runTransaction(transaction -> {
            // Set tenant active
            tenant.setContractStatus("ACTIVE");
            tenant.setCreatedAt(System.currentTimeMillis());
            tenant.setUpdatedAt(System.currentTimeMillis());
            transaction.set(tenantRef, toPayload(tenant));

            // Set room rented
            Map<String, Object> roomUpdates = new HashMap<>();
            roomUpdates.put("status", RoomStatus.RENTED);
            roomUpdates.put("updatedAt", com.google.firebase.Timestamp.now());
            transaction.update(roomRef, roomUpdates);

            return null;
        })
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    private Map<String, Object> toPayload(Tenant tenant) {
        Map<String, Object> payload = new HashMap<>();

        putIfNotBlank(payload, "fullName", tenant.getFullName());
        putIfNotBlank(payload, "personalId", tenant.getPersonalId());
        putIfNotBlank(payload, "phoneNumber", tenant.getPhoneNumber());
        putIfNotBlank(payload, "address", tenant.getAddress());
        putIfNotBlank(payload, "contractNumber", tenant.getContractNumber());
        putIfNotBlank(payload, "personalIdFrontUrl", tenant.getPersonalIdFrontUrl());
        putIfNotBlank(payload, "personalIdBackUrl", tenant.getPersonalIdBackUrl());
        putIfNotBlank(payload, "representativeName", tenant.getRepresentativeName());
        putIfNotBlank(payload, "representativeId", tenant.getRepresentativeId());

        putIfNotBlank(payload, "roomId", tenant.getRoomId());
        putIfNotBlank(payload, "previousRoomId", tenant.getPreviousRoomId());
        putIfNotBlank(payload, "roomNumber", tenant.getRoomNumber());

        putIfPositiveInt(payload, "memberCount", tenant.getMemberCount());
        putIfNotBlank(payload, "rentalStartDate", tenant.getRentalStartDate());
        putIfNotBlank(payload, "contractEndDate", tenant.getContractEndDate());

        if (tenant.getRentAmount() > 0) {
            payload.put("rentAmount", tenant.getRentAmount());
            payload.put("roomPrice", (double) tenant.getRentAmount());
        }
        if (tenant.getDepositAmount() > 0) {
            payload.put("depositAmount", tenant.getDepositAmount());
            payload.put("legacyDepositAmount", (double) tenant.getDepositAmount());
        }
        if (tenant.getContractEndTimestamp() > 0) {
            payload.put("contractEndTimestamp", tenant.getContractEndTimestamp());
        }

        payload.put("showDepositOnInvoice", tenant.isShowDepositOnInvoice());
        payload.put("showNoteOnInvoice", tenant.isShowNoteOnInvoice());
        putIfPositiveInt(payload, "contractDurationMonths", tenant.getContractDurationMonths());
        payload.put("remindOneMonthBefore", tenant.isRemindOneMonthBefore());
        putIfNotBlank(payload, "billingReminderAt", tenant.getBillingReminderAt());
        putIfPositiveInt(payload, "electricStartReading", tenant.getElectricStartReading());
        putIfPositiveInt(payload, "waterStartReading", tenant.getWaterStartReading());

        payload.put("hasParkingService", tenant.hasParkingService());
        putIfPositiveInt(payload, "vehicleCount", tenant.getVehicleCount());
        payload.put("hasInternetService", tenant.hasInternetService());
        payload.put("hasLaundryService", tenant.hasLaundryService());

        putIfNotBlank(payload, "note", tenant.getNote());
        payload.put("depositCollectionStatus", tenant.isDepositCollected());

        putIfNotBlank(payload, "contractStatus", tenant.getContractStatus());

        Long createdAt = tenant.getCreatedAt();
        if (createdAt != null && createdAt > 0) {
            payload.put("createdAt", createdAt);
        }
        Long updatedAt = tenant.getUpdatedAt();
        if (updatedAt != null && updatedAt > 0) {
            payload.put("updatedAt", updatedAt);
        }
        Long endedAt = tenant.getEndedAt();
        if (endedAt != null && endedAt > 0) {
            payload.put("endedAt", endedAt);
        }

        payload.put("primaryContact", tenant.isPrimaryContact());
        payload.put("contractRepresentative", tenant.isContractRepresentative());
        payload.put("temporaryResident", tenant.isTemporaryResident());
        payload.put("fullyDocumented", tenant.isFullyDocumented());

        return payload;
    }

    private static void putIfNotBlank(Map<String, Object> payload, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            payload.put(key, value.trim());
        }
    }

    private static void putIfPositiveInt(Map<String, Object> payload, String key, int value) {
        if (value > 0) {
            payload.put(key, value);
        }
    }

    private void updateRoomStatusAtomic(String roomId, String status, Runnable onSuccess, Runnable onFail) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("updatedAt", com.google.firebase.Timestamp.now());

        getRoomCollection().document(roomId)
                .update(updates)
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    private CollectionReference getRoomCollection() {
        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId != null && !tenantId.isEmpty()) {
            return db.collection("tenants").document(tenantId).collection("rooms");
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
            throw new IllegalStateException("User not logged in");
        return db.collection("users").document(user.getUid()).collection("rooms");
    }

    public void deleteTenant(String id, Runnable onSuccess, Runnable onFail) {
        getUserCollection().document(id).delete()
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    /**
     * Internal note.
     */
    public Task<Void> updateStatusThuCoc(String contractId, boolean depositCollectionStatus) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("depositCollectionStatus", depositCollectionStatus);
        updates.put("updatedAt", System.currentTimeMillis());

        return getUserCollection().document(contractId).update(updates);
    }

    /**
     * Internal note.
     * 
     * Internal note.
     * Internal note.
     * Internal note.
     */
    public void deleteContract(String contractId, Runnable onSuccess, Runnable onFail) {
        if (contractId == null || contractId.trim().isEmpty()) {
            if (onFail != null)
                onFail.run();
            return;
        }

        getUserCollection().document(contractId).delete()
                .addOnSuccessListener(aVoid -> {
                    if (onSuccess != null)
                        onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    if (onFail != null)
                        onFail.run();
                });
    }
}
