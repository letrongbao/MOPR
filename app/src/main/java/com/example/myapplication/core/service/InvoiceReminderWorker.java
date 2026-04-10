package com.example.myapplication.core.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.myapplication.R;
import com.example.myapplication.core.MyApplication;
import com.example.myapplication.core.constants.InvoiceStatus;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.features.home.HomeMenuActivity;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class InvoiceReminderWorker extends Worker {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public InvoiceReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Context ctx = getApplicationContext();
            TenantSession.init(ctx);

            String todayTiming = resolveTodayTimingKey();
            if (todayTiming == null)
                return Result.success();

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null)
                return Result.success();

            String tenantId = TenantSession.getActiveTenantId();
            if (tenantId == null || tenantId.trim().isEmpty())
                return Result.success();

            com.google.firebase.firestore.QuerySnapshot contracts = Tasks.await(
                    db.collection("tenants").document(tenantId)
                            .collection("contracts")
                            .whereEqualTo("contractStatus", "ACTIVE")
                            .get());

            Set<String> eligibleContractIds = new HashSet<>();
            Set<String> eligibleRoomIds = new HashSet<>();
            if (contracts != null) {
                for (QueryDocumentSnapshot doc : contracts) {
                    String contractTiming = normalizeTiming(doc.getString("billingReminderAt"));
                    if (!todayTiming.equals(contractTiming))
                        continue;

                    String contractId = doc.getId();
                    if (contractId != null && !contractId.trim().isEmpty())
                        eligibleContractIds.add(contractId);

                    String roomId = doc.getString("roomId");
                    if (roomId != null && !roomId.trim().isEmpty())
                        eligibleRoomIds.add(roomId);
                }
            }

            if (eligibleContractIds.isEmpty() && eligibleRoomIds.isEmpty())
                return Result.success();

            com.google.firebase.firestore.QuerySnapshot qs = Tasks.await(
                    db.collection("tenants").document(tenantId)
                            .collection("invoices")
                        .whereEqualTo("status", InvoiceStatus.REPORTED)
                            .get());

            int count = 0;
            if (qs != null) {
                for (QueryDocumentSnapshot doc : qs) {
                    String contractId = doc.getString("contractId");
                    if (contractId != null && eligibleContractIds.contains(contractId)) {
                        count++;
                        continue;
                    }

                    String roomId = doc.getString("roomId");
                    if (roomId != null && eligibleRoomIds.contains(roomId)) {
                        count++;
                    }
                }
            }

            if (count > 0) {
                showNotification(count);
            }

            return Result.success();
        } catch (Exception e) {
            // Avoid noisy retries; app will try again next schedule.
            return Result.success();
        }
    }

    private String resolveTodayTimingKey() {
        Calendar cal = Calendar.getInstance();
        int day = cal.get(Calendar.DAY_OF_MONTH);

        if (day == 1)
            return "start_month";
        if (day == 15)
            return "mid_month";
        return null;
    }

    private String normalizeTiming(String value) {
        if ("mid_month".equals(value) || "end_month".equals(value))
            return "mid_month";
        if ("start_month".equals(value))
            return "start_month";
        return "start_month";
    }

    private void showNotification(int unpaidCount) {
        Context ctx = getApplicationContext();

        Intent intent = new Intent(ctx, HomeMenuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                ctx,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(ctx,
                    android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, MyApplication.REMINDER_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(ctx.getString(R.string.reminder_collect_title))
                .setContentText(ctx.getString(R.string.reminder_unpaid_count, unpaidCount))
                .setAutoCancel(true)
                .setContentIntent(pi);

        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null)
            nm.notify(1001, b.build());
    }
}
