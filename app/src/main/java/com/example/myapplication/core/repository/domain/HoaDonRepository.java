package com.example.myapplication.core.repository.domain;

import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.core.repository.AuditLogRepository;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.domain.HoaDon;
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

public class HoaDonRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final AuditLogRepository audit = new AuditLogRepository();
    private static final String COLLECTION = "hoa_don";

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

    public MutableLiveData<List<HoaDon>> layDanhSachHoaDon() {
        MutableLiveData<List<HoaDon>> data = new MutableLiveData<>();
        getUserCollection().addSnapshotListener((snapshot, e) -> {
            if (e != null || snapshot == null)
                return;
            List<HoaDon> list = new ArrayList<>();
            snapshot.forEach(doc -> {
                HoaDon h = doc.toObject(HoaDon.class);
                h.setId(doc.getId());
                list.add(h);
            });
            data.setValue(list);
        });
        return data;
    }

    public MutableLiveData<List<HoaDon>> layHoaDonTheoPhong(String idPhong) {
        MutableLiveData<List<HoaDon>> data = new MutableLiveData<>();
        getUserCollection().whereEqualTo("idPhong", idPhong)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null)
                        return;
                    List<HoaDon> list = new ArrayList<>();
                    snapshot.forEach(doc -> {
                        HoaDon h = doc.toObject(HoaDon.class);
                        h.setId(doc.getId());
                        list.add(h);
                    });
                    data.setValue(list);
                });
        return data;
    }

    public void themHoaDon(HoaDon hoaDon, Runnable onSuccess, Runnable onFail) {
        hoaDon.tinhTongTien();
        // Set creation timestamp
        hoaDon.setCreatedAt(com.google.firebase.Timestamp.now());
        hoaDon.setUpdatedAt(com.google.firebase.Timestamp.now());

        getUserCollection().add(hoaDon)
                .addOnSuccessListener(ref -> {
                    audit.log("invoice.create", "invoice", ref.getId(), null);
                    onSuccess.run();
                })
                .addOnFailureListener(e -> onFail.run());
    }

    public void themHoaDonUnique(HoaDon hoaDon, Runnable onSuccess, Runnable onDuplicate, Runnable onFail) {
        hoaDon.tinhTongTien();
        // Set creation timestamp
        hoaDon.setCreatedAt(com.google.firebase.Timestamp.now());
        hoaDon.setUpdatedAt(com.google.firebase.Timestamp.now());

        if (hoaDon.getIdPhong() == null || hoaDon.getIdPhong().trim().isEmpty()) {
            onFail.run();
            return;
        }
        String periodKey = toPeriodKey(hoaDon.getThangNam());
        if (periodKey.isEmpty()) {
            onFail.run();
            return;
        }

        String docId = hoaDon.getIdPhong() + "_" + periodKey;
        hoaDon.setId(docId);

        DocumentReference docRef = getUserCollection().document(docId);
        db.runTransaction(transaction -> {
            DocumentSnapshot snap = transaction.get(docRef);
            if (snap.exists()) {
                throw new RuntimeException("DUPLICATE_INVOICE");
            }
            transaction.set(docRef, hoaDon);
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

    public void capNhatHoaDon(HoaDon hoaDon, Runnable onSuccess, Runnable onFail) {
        hoaDon.tinhTongTien();
        // Set update timestamp
        hoaDon.setUpdatedAt(com.google.firebase.Timestamp.now());

        getUserCollection().document(hoaDon.getId()).set(hoaDon)
                .addOnSuccessListener(v -> {
                    audit.log("invoice.update", "invoice", hoaDon.getId(), null);
                    onSuccess.run();
                })
                .addOnFailureListener(e -> onFail.run());
    }

    public void capNhatTrangThai(String id, String trangThai, Runnable onSuccess, Runnable onFail) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("trangThai", trangThai);
        updates.put("updatedAt", com.google.firebase.Timestamp.now());

        // If marking as paid, set payment date
        if ("Đã thanh toán".equals(trangThai)) {
            updates.put("ngayThanhToan", com.google.firebase.Timestamp.now());
        }

        getUserCollection().document(id)
                .update(updates)
                .addOnSuccessListener(v -> {
                    java.util.Map<String, Object> extra = new java.util.HashMap<>();
                    extra.put("status", trangThai);
                    audit.log("invoice.status_update", "invoice", id, extra);
                    onSuccess.run();
                })
                .addOnFailureListener(e -> onFail.run());
    }

    public void xoaHoaDon(String id, Runnable onSuccess, Runnable onFail) {
        getUserCollection().document(id).delete()
                .addOnSuccessListener(v -> {
                    audit.log("invoice.delete", "invoice", id, null);
                    onSuccess.run();
                })
                .addOnFailureListener(e -> onFail.run());
    }
}
