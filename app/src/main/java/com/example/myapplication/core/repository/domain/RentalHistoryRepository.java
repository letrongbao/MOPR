package com.example.myapplication.core.repository.domain;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.domain.RentalHistory;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

/**
 * Repository quản lý Lịch sử cho thuê trong Firestore
 */
public class RentalHistoryRepository {
    private static final String TAG = "RentalHistoryRepository";
    private static final String COLLECTION_RENTAL_HISTORY = "rental_history";

    private final FirebaseFirestore db;

    public RentalHistoryRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Lấy collection reference cho rental_history
     */
    @NonNull
    private CollectionReference getHistoryCollection() {
        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId != null && !tenantId.isEmpty()) {
            return db.collection("tenants")
                    .document(tenantId)
                    .collection(COLLECTION_RENTAL_HISTORY);
        } else {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            return db.collection("users")
                    .document(uid)
                    .collection(COLLECTION_RENTAL_HISTORY);
        }
    }

    /**
     * Thêm một bản ghi lịch sử cho thuê mới
     */
    public Task<DocumentReference> addHistory(RentalHistory history) {
        Log.d(TAG, "Adding rental history for contract: " + history.getIdHopDong());
        return getHistoryCollection().add(history);
    }

    /**
     * Lấy tất cả lịch sử cho thuê, sắp xếp theo ngày kết thúc mới nhất
     */
    public Task<QuerySnapshot> getAllHistory() {
        return getHistoryCollection()
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get();
    }

    /**
     * Lấy lịch sử cho thuê theo phòng
     */
    public Task<QuerySnapshot> getHistoryByRoom(String roomId) {
        return getHistoryCollection()
                .whereEqualTo("idPhong", roomId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get();
    }

    /**
     * Lấy lịch sử cho thuê theo người thuê
     */
    public Task<QuerySnapshot> getHistoryByTenant(String tenantId) {
        return getHistoryCollection()
                .whereEqualTo("idTenant", tenantId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get();
    }

    /**
     * Lấy lịch sử cho thuê theo hợp đồng
     */
    public Task<QuerySnapshot> getHistoryByContract(String contractId) {
        return getHistoryCollection()
                .whereEqualTo("idHopDong", contractId)
                .limit(1)
                .get();
    }

    /**
     * Xóa một bản ghi lịch sử
     */
    public Task<Void> deleteHistory(String historyId) {
        Log.d(TAG, "Deleting rental history: " + historyId);
        return getHistoryCollection().document(historyId).delete();
    }

    /**
     * Cập nhật thông tin lịch sử
     */
    public Task<Void> updateHistory(String historyId, RentalHistory history) {
        Log.d(TAG, "Updating rental history: " + historyId);
        return getHistoryCollection().document(historyId).set(history);
    }

    /**
     * Lấy thống kê lịch sử cho thuê
     */
    public void getStatistics(StatisticsCallback callback) {
        getAllHistory().addOnSuccessListener(querySnapshot -> {
            int totalContracts = querySnapshot.size();
            double totalRevenue = 0;
            int totalDays = 0;

            for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                RentalHistory history = doc.toObject(RentalHistory.class);
                if (history != null) {
                    totalRevenue += history.getTongTienDaThanhToan();
                    totalDays += history.getSoNgayThueThucTe();
                }
            }

            callback.onStatistics(totalContracts, totalRevenue, totalDays);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to get statistics", e);
            callback.onStatistics(0, 0, 0);
        });
    }

    public interface StatisticsCallback {
        void onStatistics(int totalContracts, double totalRevenue, int totalDays);
    }
}

