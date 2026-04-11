package com.example.myapplication.core.repository.domain;

import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.core.constants.InvoiceStatus;
import com.example.myapplication.core.repository.AuditLogRepository;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.domain.Invoice;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class InvoiceRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final AuditLogRepository audit = new AuditLogRepository();
    private static final String COLLECTION = "invoices";

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

    public MutableLiveData<List<Invoice>> getInvoiceList() {
        MutableLiveData<List<Invoice>> data = new MutableLiveData<>();
        getUserCollection().addSnapshotListener((snapshot, e) -> {
            if (e != null || snapshot == null)
                return;
            List<Invoice> list = new ArrayList<>();
            snapshot.forEach(doc -> {
                Invoice h = doc.toObject(Invoice.class);
                h.setId(doc.getId());
                list.add(h);
            });
            data.setValue(list);
        });
        return data;
    }

    public MutableLiveData<List<Invoice>> getInvoicesByRoom(String roomId) {
        MutableLiveData<List<Invoice>> data = new MutableLiveData<>();
        getUserCollection().whereEqualTo("roomId", roomId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null)
                        return;
                    List<Invoice> list = new ArrayList<>();
                    snapshot.forEach(doc -> {
                        Invoice h = doc.toObject(Invoice.class);
                        h.setId(doc.getId());
                        list.add(h);
                    });
                    data.setValue(list);
                });
        return data;
    }

    public void addInvoice(Invoice invoice, Runnable onSuccess, Runnable onFail) {
        invoice.calculateTotalAmount();
        // Set creation timestamp
        invoice.setCreatedAt(com.google.firebase.Timestamp.now());
        invoice.setUpdatedAt(com.google.firebase.Timestamp.now());

        getUserCollection().add(toPayload(invoice))
                .addOnSuccessListener(ref -> {
                    audit.log("invoice.create", "invoice", ref.getId(), null);
                    onSuccess.run();
                })
                .addOnFailureListener(e -> onFail.run());
    }

    public void addInvoiceUnique(Invoice invoice, Runnable onSuccess, Runnable onDuplicate, Runnable onFail) {
        invoice.calculateTotalAmount();
        // Set creation timestamp
        invoice.setCreatedAt(com.google.firebase.Timestamp.now());
        invoice.setUpdatedAt(com.google.firebase.Timestamp.now());

        if (invoice.getRoomId() == null || invoice.getRoomId().trim().isEmpty()) {
            onFail.run();
            return;
        }
        String periodKey = toPeriodKey(invoice.getBillingPeriod());
        if (periodKey.isEmpty()) {
            onFail.run();
            return;
        }

        String docId = invoice.getRoomId() + "_" + periodKey;
        invoice.setId(docId);

        DocumentReference docRef = getUserCollection().document(docId);
        db.runTransaction(transaction -> {
            DocumentSnapshot snap = transaction.get(docRef);
            if (snap.exists()) {
                throw new RuntimeException("DUPLICATE_INVOICE");
            }
            transaction.set(docRef, toPayload(invoice));
            return null;
        })
                .addOnSuccessListener(v -> {
                    audit.log("invoice.create", "invoice", docId, null);
                    onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    String msg = e != null ? e.getMessage() : null;
                    if (msg != null && msg.contains("DUPLICATE_INVOICE")) {
                        onDuplicate.run();
                    } else {
                        onFail.run();
                    }
                });
    }

    private String toPeriodKey(String period) {
        if (period == null)
            return "";
        String[] parts = period.trim().split("/");
        if (parts.length != 2)
            return "";
        try {
            int month = Integer.parseInt(parts[0]);
            int year = Integer.parseInt(parts[1]);
            if (month < 1 || month > 12)
                return "";
            return String.format(Locale.US, "%04d%02d", year, month);
        } catch (NumberFormatException e) {
            return "";
        }
    }

    public void updateInvoice(Invoice invoice, Runnable onSuccess, Runnable onFail) {
        invoice.calculateTotalAmount();
        // Set update timestamp
        invoice.setUpdatedAt(com.google.firebase.Timestamp.now());

        getUserCollection().document(invoice.getId()).set(toPayload(invoice))
                .addOnSuccessListener(v -> {
                    audit.log("invoice.update", "invoice", invoice.getId(), null);
                    onSuccess.run();
                })
                .addOnFailureListener(e -> onFail.run());
    }

    public void updateStatus(String id, String status, Runnable onSuccess, Runnable onFail) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("updatedAt", com.google.firebase.Timestamp.now());

        // If marking as paid, set payment date
        if (InvoiceStatus.PAID.equals(status)) {
            updates.put("paymentDate", com.google.firebase.Timestamp.now());
        }

        getUserCollection().document(id)
                .update(updates)
                .addOnSuccessListener(v -> {
                    java.util.Map<String, Object> extra = new java.util.HashMap<>();
                    extra.put("status", status);
                    audit.log("invoice.status_update", "invoice", id, extra);
                    onSuccess.run();
                })
                .addOnFailureListener(e -> onFail.run());
    }

    public void deleteInvoice(String id, Runnable onSuccess, Runnable onFail) {
        getUserCollection().document(id).delete()
                .addOnSuccessListener(v -> {
                    audit.log("invoice.delete", "invoice", id, null);
                    onSuccess.run();
                })
                .addOnFailureListener(e -> onFail.run());
    }

    private Map<String, Object> toPayload(Invoice invoice) {
        Map<String, Object> payload = new HashMap<>();

        putIfNotBlank(payload, "roomId", invoice.getRoomId());
        putIfNotBlank(payload, "contractId", invoice.getContractId());
        putIfNotBlank(payload, "roomNumber", invoice.getRoomNumber());
        putIfNotBlank(payload, "billingPeriod", invoice.getBillingPeriod());

        putIfPositive(payload, "electricStartReading", invoice.getElectricStartReading());
        putIfPositive(payload, "electricEndReading", invoice.getElectricEndReading());
        putIfPositive(payload, "electricUnitPrice", invoice.getElectricUnitPrice());
        putIfPositive(payload, "waterStartReading", invoice.getWaterStartReading());
        putIfPositive(payload, "waterEndReading", invoice.getWaterEndReading());
        putIfPositive(payload, "waterUnitPrice", invoice.getWaterUnitPrice());

        payload.put("trashFee", invoice.getTrashFee());
        payload.put("internetFee", invoice.getInternetFee());
        payload.put("wifiFee", invoice.getInternetFee());
        payload.put("parkingFee", invoice.getParkingFee());
        payload.put("otherFee", invoice.getOtherFee());
        if (invoice.getOtherFeeLines() != null && !invoice.getOtherFeeLines().isEmpty()) {
            payload.put("otherFeeLines", new ArrayList<>(invoice.getOtherFeeLines()));
        }
        putIfPositive(payload, "rentAmount", invoice.getRentAmount());
        payload.put("totalAmount", Math.max(0, invoice.getTotalAmount()));

        putIfNotBlank(payload, "status", invoice.getStatus());

        if (invoice.getPaymentDate() != null) {
            payload.put("paymentDate", invoice.getPaymentDate());
        }
        putIfNotBlank(payload, "paymentMethod", invoice.getPaymentMethod());
        putIfPositive(payload, "paidAmount", invoice.getPaidAmount());
        payload.put("ownerNote", invoice.getOwnerNote() == null ? "" : invoice.getOwnerNote().trim());

        payload.put("transferProofPending", invoice.isTransferProofPending());
        putIfNotBlank(payload, "transferProofImageUrl", invoice.getTransferProofImageUrl());
        putIfPositive(payload, "transferProofAmount", invoice.getTransferProofAmount());
        putIfNotBlank(payload, "transferProofNote", invoice.getTransferProofNote());
        if (invoice.getTransferProofSubmittedAt() != null) {
            payload.put("transferProofSubmittedAt", invoice.getTransferProofSubmittedAt());
        }

        if (invoice.getCreatedAt() != null) {
            payload.put("createdAt", invoice.getCreatedAt());
        }
        if (invoice.getUpdatedAt() != null) {
            payload.put("updatedAt", invoice.getUpdatedAt());
        }

        return payload;
    }

    private static void putIfPositive(Map<String, Object> payload, String key, double value) {
        if (value > 0) {
            payload.put(key, value);
        }
    }

    private static void putIfNotBlank(Map<String, Object> payload, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            payload.put(key, value.trim());
        }
    }
}

