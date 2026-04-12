package com.example.myapplication.features.report;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.myapplication.R;

public class ReportAlarmReceiver extends BroadcastReceiver {

    public static final String EXTRA_REQUEST_CODE = "REQUEST_CODE";
    public static final String EXTRA_TITLE = "TITLE";
    public static final String EXTRA_MESSAGE = "MESSAGE";

    private static final String CHANNEL_ID = "report_alarm_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        int requestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, 0);
        String title = intent.getStringExtra(EXTRA_TITLE);
        String message = intent.getStringExtra(EXTRA_MESSAGE);

        if (title == null || title.trim().isEmpty()) {
            title = context.getString(R.string.report_alarm_default_title);
        }
        if (message == null || message.trim().isEmpty()) {
            message = context.getString(R.string.report_alarm_default_message);
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.report_alarm_channel_name),
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(context.getString(R.string.report_alarm_channel_desc));
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(requestCode, builder.build());
        ReportReminderScheduler.removeReminder(context, requestCode);
    }
}
