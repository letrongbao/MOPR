package com.example.myapplication.core.repository.domain;

import androidx.lifecycle.MutableLiveData;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.domain.NguoiThue;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NguoiThueRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION = "nguoi_thue";

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

    public MutableLiveData<List<NguoiThue>> layDanhSachNguoiThue() {
        MutableLiveData<List<NguoiThue>> data = new MutableLiveData<>();
        getUserCollection().addSnapshotListener((snapshot, e) -> {
            if (e != null || snapshot == null)
                return;
            List<NguoiThue> list = new ArrayList<>();
            snapshot.forEach(doc -> {
                NguoiThue n = doc.toObject(NguoiThue.class);
                n.setId(doc.getId());
                list.add(n);
            });
            data.setValue(list);
        });
        return data;
    }

    public MutableLiveData<List<NguoiThue>> layNguoiThueTheoPhong(String idPhong) {
        MutableLiveData<List<NguoiThue>> data = new MutableLiveData<>();
        getUserCollection().whereEqualTo("idPhong", idPhong)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null)
                        return;
                    List<NguoiThue> list = new ArrayList<>();
                    snapshot.forEach(doc -> {
                        NguoiThue n = doc.toObject(NguoiThue.class);
                        n.setId(doc.getId());
                        list.add(n);
                    });
                    data.setValue(list);
                });
        return data;
    }

    /**
     * Lấy danh sách hợp đồng đã kết thúc (lịch sử cho thuê)
     */
    public MutableLiveData<List<NguoiThue>> layLichSuChoThue() {
        MutableLiveData<List<NguoiThue>> data = new MutableLiveData<>();
        getUserCollection().whereEqualTo("trangThaiHopDong", "ENDED")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null) {
                        data.setValue(new ArrayList<>());
                        return;
                    }
                    List<NguoiThue> list = new ArrayList<>();
                    snapshot.forEach(doc -> {
                        NguoiThue n = doc.toObject(NguoiThue.class);
                        n.setId(doc.getId());
                        list.add(n);
                    });
                    // Sắp xếp theo thời gian kết thúc mới nhất
                    list.sort((a, b) -> Long.compare(b.getEndedAt(), a.getEndedAt()));
                    data.setValue(list);
                });
        return data;
    }

    public void themNguoiThue(NguoiThue nguoiThue, Runnable onSuccess, Runnable onFail) {
        getUserCollection().add(nguoiThue)
                .addOnSuccessListener(ref -> {
                    // If tenant is ACTIVE, atomically update room status
                    if ("ACTIVE".equals(nguoiThue.getTrangThaiHopDong()) && nguoiThue.getIdPhong() != null) {
                        updateRoomStatusAtomic(nguoiThue.getIdPhong(), "Đã thuê", onSuccess, onFail);
                    } else {
                        onSuccess.run();
                    }
                })
                .addOnFailureListener(e -> onFail.run());
    }

    public void capNhatNguoiThue(NguoiThue nguoiThue, Runnable onSuccess, Runnable onFail) {
        getUserCollection().document(nguoiThue.getId()).set(nguoiThue)
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
            tenantUpdates.put("trangThaiHopDong", "ENDED");
            tenantUpdates.put("endedAt", System.currentTimeMillis());
            tenantUpdates.put("updatedAt", System.currentTimeMillis());
            tenantUpdates.put("idPhongCu", roomId); // Save room history
            tenantUpdates.put("idPhong", null); // Clear current room
            transaction.update(tenantRef, tenantUpdates);

            // Update room status
            Map<String, Object> roomUpdates = new HashMap<>();
            roomUpdates.put("trangThai", "Trống");
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
    public void startContractAtomic(NguoiThue nguoiThue, Runnable onSuccess, Runnable onFail) {
        if (nguoiThue.getIdPhong() == null) {
            onFail.run();
            return;
        }

        DocumentReference tenantRef = nguoiThue.getId() != null ? getUserCollection().document(nguoiThue.getId())
                : getUserCollection().document();
        DocumentReference roomRef = getRoomCollection().document(nguoiThue.getIdPhong());

        db.runTransaction(transaction -> {
            // Set tenant active
            nguoiThue.setTrangThaiHopDong("ACTIVE");
            nguoiThue.setCreatedAt(System.currentTimeMillis());
            nguoiThue.setUpdatedAt(System.currentTimeMillis());
            transaction.set(tenantRef, nguoiThue);

            // Set room rented
            Map<String, Object> roomUpdates = new HashMap<>();
            roomUpdates.put("trangThai", "Đã thuê");
            roomUpdates.put("updatedAt", com.google.firebase.Timestamp.now());
            transaction.update(roomRef, roomUpdates);

            return null;
        })
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    private void updateRoomStatusAtomic(String roomId, String status, Runnable onSuccess, Runnable onFail) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("trangThai", status);
        updates.put("updatedAt", com.google.firebase.Timestamp.now());

        getRoomCollection().document(roomId)
                .update(updates)
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    private CollectionReference getRoomCollection() {
        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId != null && !tenantId.isEmpty()) {
            return db.collection("tenants").document(tenantId).collection("phong_tro");
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
            throw new IllegalStateException("User not logged in");
        return db.collection("users").document(user.getUid()).collection("phong_tro");
    }

    public void xoaNguoiThue(String id, Runnable onSuccess, Runnable onFail) {
        getUserCollection().document(id).delete()
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onFail.run());
    }

    /**
     * Cập nhật trạng thái thu tiền cọc
     */
    public Task<Void> capNhatTrangThaiThuCoc(String contractId, boolean trangThaiThuCoc) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("trangThaiThuCoc", trangThaiThuCoc);
        updates.put("updatedAt", System.currentTimeMillis());
        
        return getUserCollection().document(contractId).update(updates);
    }
}
