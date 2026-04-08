package com.example.myapplication.core.repository.domain;

import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.domain.House;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HouseRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION = "houses";

    private CollectionReference scopedCollection() {
        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId != null && !tenantId.isEmpty()) {
            return db.collection("tenants").document(tenantId).collection(COLLECTION);
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
            throw new IllegalStateException("User not logged in");
        return db.collection("users").document(user.getUid()).collection(COLLECTION);
    }

    public MutableLiveData<List<House>> listAll() {
        MutableLiveData<List<House>> data = new MutableLiveData<>();
        scopedCollection().addSnapshotListener((snap, e) -> {
            if (e != null || snap == null)
                return;
            List<House> list = new ArrayList<>();
            snap.forEach(doc -> {
                House k = doc.toObject(House.class);
                k.setId(doc.getId());
                list.add(k);
            });
            data.setValue(list);
        });
        return data;
    }

    public void add(House k, Runnable onSuccess, Runnable onFail) {
        // Set creation timestamp
        k.setCreatedAt(com.google.firebase.Timestamp.now());
        k.setUpdatedAt(com.google.firebase.Timestamp.now());

        scopedCollection().add(toPayload(k))
                .addOnSuccessListener(r -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    public void update(House k, Runnable onSuccess, Runnable onFail) {
        // Set update timestamp
        k.setUpdatedAt(com.google.firebase.Timestamp.now());

        scopedCollection().document(k.getId()).set(toPayload(k))
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    public void delete(String id, Runnable onSuccess, Runnable onFail) {
        scopedCollection().document(id).delete()
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    private Map<String, Object> toPayload(House k) {
        Map<String, Object> payload = new HashMap<>();

        putIfNotBlank(payload, "houseName", k.getHouseName());
        putIfNotBlank(payload, "managerPhone", k.getManagerPhone());
        putIfNotBlank(payload, "address", k.getAddress());
        putIfNotBlank(payload, "note", k.getNote());

        putIfNotBlank(payload, "bankAccountName", k.getBankAccountName());
        putIfNotBlank(payload, "bankName", k.getBankName());
        putIfNotBlank(payload, "bankAccountNo", k.getBankAccountNo());
        putIfNotBlank(payload, "paymentQrUrl", k.getPaymentQrUrl());
        putIfNotBlank(payload, "billingReminderAt", k.getBillingReminderAt());

        putIfPositive(payload, "electricityPrice", k.getElectricityPrice());
        putIfNotBlank(payload, "electricityCalculationMethod", k.getElectricityCalculationMethod());
        putIfPositive(payload, "waterPrice", k.getWaterPrice());
        putIfNotBlank(payload, "waterCalculationMethod", k.getWaterCalculationMethod());
        putIfPositive(payload, "parkingPrice", k.getParkingPrice());
        putIfNotBlank(payload, "parkingUnit", k.getParkingUnit());
        putIfPositive(payload, "internetPrice", k.getInternetPrice());
        putIfNotBlank(payload, "internetUnit", k.getInternetUnit());
        putIfPositive(payload, "laundryPrice", k.getLaundryPrice());
        putIfNotBlank(payload, "laundryUnit", k.getLaundryUnit());
        putIfPositive(payload, "elevatorPrice", k.getElevatorPrice());
        putIfNotBlank(payload, "elevatorUnit", k.getElevatorUnit());
        putIfPositive(payload, "cableTvPrice", k.getCableTvPrice());
        putIfNotBlank(payload, "cableTvUnit", k.getCableTvUnit());
        putIfPositive(payload, "trashPrice", k.getTrashPrice());
        putIfNotBlank(payload, "trashUnit", k.getTrashUnit());
        putIfPositive(payload, "servicePrice", k.getServicePrice());
        putIfNotBlank(payload, "serviceUnit", k.getServiceUnit());

        List<Map<String, Object>> extraFeePayload = new ArrayList<>();
        List<House.ExtraFee> extras = k.getExtraFees();
        if (extras != null) {
            for (House.ExtraFee fee : extras) {
                if (fee == null)
                    continue;
                String name = trimToEmpty(fee.getFeeName());
                if (name.isEmpty() || fee.getPrice() <= 0)
                    continue;

                Map<String, Object> row = new HashMap<>();
                row.put("feeName", name);
                putIfNotBlank(row, "unit", fee.getUnit());
                row.put("price", fee.getPrice());
                extraFeePayload.add(row);
            }
        }
        if (!extraFeePayload.isEmpty()) {
            payload.put("extraFees", extraFeePayload);
        }

        if (k.getCreatedAt() != null) {
            payload.put("createdAt", k.getCreatedAt());
        }
        if (k.getUpdatedAt() != null) {
            payload.put("updatedAt", k.getUpdatedAt());
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

    private static String trimToEmpty(String s) {
        return s == null ? "" : s.trim();
    }
}
