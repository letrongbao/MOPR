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

import com.example.myapplication.core.MyApplication;
import com.example.myapplication.core.constants.InvoiceStatus;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.features.home.HomeMenuActivity;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;

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

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null)
                return Result.success();

            String tenantId = TenantSession.getActiveTenantId();
            if (tenantId == null || tenantId.trim().isEmpty())
                return Result.success();

            com.google.firebase.firestore.QuerySnapshot qs = Tasks.await(
                    db.collection("tenants").document(tenantId)
                            .collection("hoa_don")
                            .whereIn("trangThai", Arrays.asList(InvoiceStatus.UNPAID, InvoiceStatus.PARTIAL))
                            .get());

            int count = (qs != null) ? qs.size() : 0;
            if (count > 0) {
                showNotification(count);
            }

            return Result.success();
        } catch (Exception e) {
            // Avoid noisy retries; app will try again next schedule.
            return Result.success();
        }
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
                .setContentTitle("Nhắc thu tiền")
                .setContentText("Bạn có " + unpaidCount + " hoá đơn chưa thu")
                .setAutoCancel(true)
                .setContentIntent(pi);

        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null)
            nm.notify(1001, b.build());
    }
}
