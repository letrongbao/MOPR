package com.example.myapplication.features.invoice;

import androidx.annotation.NonNull;

import com.example.myapplication.core.util.FinancePeriodUtil;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class InvoicePeriodSuggestionHelper {

    public interface CollectionProvider {
        CollectionReference get(@NonNull String collection);
    }

    public interface PeriodKeyResolver {
        @NonNull
        String resolve(String period);
    }

    public interface SuggestedPeriodCallback {
        void onResult(@NonNull String suggestedPeriod);
    }

    private InvoicePeriodSuggestionHelper() {
    }

    public static void suggestNextPeriodForRoom(@NonNull CollectionProvider scopedCollection,
            @NonNull PeriodKeyResolver periodKeyResolver,
            @NonNull String roomId,
            @NonNull SuggestedPeriodCallback callback) {
        scopedCollection.get("invoices")
                .whereEqualTo("idPhong", roomId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    String bestKey = null;
                    String bestPeriod = "";
                    if (snapshot != null) {
                        for (QueryDocumentSnapshot doc : snapshot) {
                            String period = doc.getString("thangNam");
                            String key = periodKeyResolver.resolve(period);
                            if (key == null || key.isEmpty()) {
                                continue;
                            }
                            if (bestKey == null || key.compareTo(bestKey) > 0) {
                                bestKey = key;
                                bestPeriod = period != null ? period : "";
                            }
                        }
                    }
                    if (bestKey == null) {
                        callback.onResult(currentMonthYear());
                        return;
                    }
                    callback.onResult(nextMonth(bestPeriod));
                })
                .addOnFailureListener(e -> callback.onResult(currentMonthYear()));
    }

    @NonNull
    private static String currentMonthYear() {
        return FinancePeriodUtil
                .normalizeMonthYear(new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(new Date()));
    }

    @NonNull
    private static String nextMonth(String period) {
        String normalized = FinancePeriodUtil.normalizeMonthYear(period);
        String[] parts = normalized.split("/");
        if (parts.length != 2) {
            return currentMonthYear();
        }
        try {
            int month = Integer.parseInt(parts[0]);
            int year = Integer.parseInt(parts[1]);
            month += 1;
            if (month > 12) {
                month = 1;
                year += 1;
            }
            return String.format(Locale.US, "%02d/%04d", month, year);
        } catch (Exception e) {
            return currentMonthYear();
        }
    }
}
