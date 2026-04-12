package com.example.myapplication.features.report;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReportBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            ReportReminderScheduler.restoreScheduledReminders(context);
        }
    }
}
