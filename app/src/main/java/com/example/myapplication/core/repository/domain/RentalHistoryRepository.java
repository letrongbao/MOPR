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
 * Internal note.
 */
public class RentalHistoryRepository {
    private static final String TAG = "RentalHistoryRepository";
    private static final String COLLECTION_RENTAL_HISTORY = "rentalHistory";

    private final FirebaseFirestore db;

    public RentalHistoryRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Internal note.
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
     * Internal note.
     */
    public Task<DocumentReference> addHistory(RentalHistory history) {
        Log.d(TAG, "Adding rental history for contract: " + history.getContractId());
        return getHistoryCollection().add(history);
    }

    /**
     * Internal note.
     */
    public Task<QuerySnapshot> getAllHistory() {
        return getHistoryCollection()
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get();
    }

    /**
     * Internal note.
     */
    public Task<QuerySnapshot> getHistoryByRoom(String roomId) {
        return getHistoryCollection()
                .whereEqualTo("roomId", roomId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get();
    }

    /**
     * Internal note.
     */
    public Task<QuerySnapshot> getHistoryByTenant(String tenantId) {
        return getHistoryCollection()
                .whereEqualTo("tenantId", tenantId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get();
    }

    /**
     * Internal note.
     */
    public Task<QuerySnapshot> getHistoryByContract(String contractId) {
        return getHistoryCollection()
                .whereEqualTo("contractId", contractId)
                .limit(1)
                .get();
    }

    /**
     * Internal note.
     */
    public Task<Void> deleteHistory(String historyId) {
        Log.d(TAG, "Deleting rental history: " + historyId);
        return getHistoryCollection().document(historyId).delete();
    }

    /**
     * Internal note.
     */
    public Task<Void> updateHistory(String historyId, RentalHistory history) {
        Log.d(TAG, "Updating rental history: " + historyId);
        return getHistoryCollection().document(historyId).set(history);
    }

    /**
     * Internal note.
     */
    public void getStatistics(StatisticsCallback callback) {
        getAllHistory().addOnSuccessListener(querySnapshot -> {
            int totalContracts = querySnapshot.size();
            double totalRevenue = 0;
            int totalDays = 0;

            for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                RentalHistory history = doc.toObject(RentalHistory.class);
                if (history != null) {
                    totalRevenue += history.getTotalPaidAmount();
                    totalDays += history.getActualRentalDays();
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
