package com.example.myapplication.features.report;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.myapplication.R;

/**
 * BroadcastReceiver nhận Alarm từ AlarmManager và hiển thị Notification nhắc lịch sửa chữa.
 */
public class ReportAlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID   = "report_alarm_channel";
    private static final String CHANNEL_NAME = "Nhắc sửa chữa";
    private static final int    NOTIF_ID     = 9001;

    @Override
    public void onReceive(Context context, Intent intent) {
        String title   = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");

        if (title   == null) title   = "Nhắc nhở sửa chữa";
        if (message == null) message = "Đã đến lịch hẹn xử lý phản ánh!";

        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Tạo channel (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Nhắc nhở lịch hẹn xử lý phản ánh từ khách thuê");
            nm.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        nm.notify(NOTIF_ID, builder.build());
    }
}
