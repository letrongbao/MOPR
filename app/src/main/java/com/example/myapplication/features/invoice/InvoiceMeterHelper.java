package com.example.myapplication.features.invoice;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public final class InvoiceMeterHelper {

    public interface CollectionProvider {
        CollectionReference get(@NonNull String collection);
    }

    public interface PeriodKeyResolver {
        @NonNull
        String resolve(String period);
    }

    public interface MeterEndCallback {
        void onResult(double elecEnd, double waterEnd);
    }

    private InvoiceMeterHelper() {
    }

    public static void loadLatestMeterEnds(@NonNull CollectionProvider scopedCollection,
            @NonNull PeriodKeyResolver periodKeyResolver,
            @NonNull String roomId,
            @NonNull MeterEndCallback callback) {
        scopedCollection.get("meterReadings").whereEqualTo("roomId", roomId).get()
                .addOnSuccessListener(snapshot -> {
                    String bestKey = null;
                    double bestElecEnd = 0;
                    double bestWaterEnd = 0;

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String key = doc.getString("periodKey");
                        if (key == null) {
                            String period = doc.getString("period");
                            key = periodKeyResolver.resolve(period);
                        }
                        if (key == null)
                            key = "";

                        if (bestKey == null || key.compareTo(bestKey) > 0) {
                            bestKey = key;
                            Double e = doc.getDouble("elecEnd");
                            Double w = doc.getDouble("waterEnd");
                            bestElecEnd = e != null ? e : 0;
                            bestWaterEnd = w != null ? w : 0;
                        }
                    }

                    callback.onResult(bestElecEnd, bestWaterEnd);
                })
                .addOnFailureListener(e -> callback.onResult(0, 0));
    }

    public static void saveMeterReadingFromInvoice(@NonNull CollectionProvider scopedCollection,
            @NonNull PeriodKeyResolver periodKeyResolver,
            @NonNull String roomId,
            @NonNull String period,
            double elecStart,
            double elecEnd,
            double waterStart,
            double waterEnd) {
        String periodKey = periodKeyResolver.resolve(period);
        if (periodKey == null || periodKey.isEmpty())
            return;

        String docId = roomId + "_" + periodKey;
        Map<String, Object> data = new HashMap<>();
        data.put("roomId", roomId);
        data.put("period", period);
        data.put("periodKey", periodKey);
        data.put("elecStart", elecStart);
        data.put("elecEnd", elecEnd);
        data.put("waterStart", waterStart);
        data.put("waterEnd", waterEnd);

        scopedCollection.get("meterReadings").document(docId).set(data);
    }
}
