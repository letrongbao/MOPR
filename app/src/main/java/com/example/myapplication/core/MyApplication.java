package com.example.myapplication.core;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.myapplication.core.service.InvoiceReminderWorker;
import com.example.myapplication.core.session.TenantSession;
import com.google.firebase.FirebaseApp;

import java.util.concurrent.TimeUnit;

public class MyApplication extends Application {

        public static final String REMINDER_CHANNEL_ID = "mopr_reminders";
        private static final String WORK_INVOICE_REMINDERS = "invoice_reminders";

        @Override
        public void onCreate() {
                super.onCreate();
                TenantSession.init(this);
                ensureReminderChannel();
                scheduleInvoiceReminders();
        }

        private void ensureReminderChannel() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                        return;

                NotificationChannel channel = new NotificationChannel(
                                REMINDER_CHANNEL_ID,
                                "Nhắc nhở",
                                NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription("Nhắc thu tiền/hoá đơn");

                NotificationManager nm = getSystemService(NotificationManager.class);
                if (nm != null)
                        nm.createNotificationChannel(channel);
        }

        private void scheduleInvoiceReminders() {
                Constraints c = new Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build();

                PeriodicWorkRequest req = new PeriodicWorkRequest.Builder(
                                InvoiceReminderWorker.class,
                                1, TimeUnit.DAYS)
                                .setConstraints(c)
                                .build();

                WorkManager.getInstance(this)
                                .enqueueUniquePeriodicWork(WORK_INVOICE_REMINDERS, ExistingPeriodicWorkPolicy.UPDATE,
                                                req);
        }
}
