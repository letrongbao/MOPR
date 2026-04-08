package com.example.myapplication.features.notification;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;
import com.example.myapplication.core.MyApplication;

public final class NotificationDisplayUtil {

    private NotificationDisplayUtil() {
    }

    public static void showChatNotification(Context context, String title, String body) {
        if (context == null) {
            return;
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        String safeTitle = title == null || title.trim().isEmpty()
                ? context.getString(R.string.notification_center_title)
                : title.trim();
        String safeBody = body == null ? "" : body;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, MyApplication.CHAT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(safeTitle)
                .setContentText(safeBody)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(safeBody))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
