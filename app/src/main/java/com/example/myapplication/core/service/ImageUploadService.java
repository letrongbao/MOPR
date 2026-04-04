package com.example.myapplication.core.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.myapplication.R;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImageUploadService extends Service {

    private static final String TAG = "ImageUploadService";
    private static final String CHANNEL_ID = "upload_channel";
    private static final int NOTIFICATION_ID = 101;

    // Ensure these settings match the Cloudinary Console
    private static final String CLOUD_NAME = "dsvkscwti";
    private static final String UPLOAD_PRESET = "MOPRupload";

    public static final String EXTRA_IMAGE_URI = "image_uri";
    public static final String ACTION_UPLOAD_COMPLETE = "com.example.myapplication.UPLOAD_COMPLETE";
    public static final String EXTRA_IMAGE_URL = "image_url";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getStringExtra(EXTRA_IMAGE_URI) == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        Uri imageUri = Uri.parse(intent.getStringExtra(EXTRA_IMAGE_URI));

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.uploading_image_notification))
                .setContentText(getString(R.string.please_wait_moment))
                .setSmallIcon(R.drawable.ic_add)
                .setProgress(100, 0, true)
                .setOngoing(true)
                .build();

        // Android 14+ foreground service compatibility fix (API 34, 35, 36)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                byte[] imageBytes;
                try (InputStream is = getContentResolver().openInputStream(imageUri)) {
                    if (is == null) {
                        showCompleteNotification(getString(R.string.error_cannot_open_image));
                        return;
                    }
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        baos.write(buffer, 0, bytesRead);
                    }
                    imageBytes = baos.toByteArray();
                }

                updateNotification(getString(R.string.sending_data), 50);

                String mimeType = getContentResolver().getType(imageUri);
                if (mimeType == null)
                    mimeType = "image/jpeg";

                String boundary = "----Boundary" + System.currentTimeMillis();
                URL url = new URL("https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload");
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);

                try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
                    dos.writeBytes("--" + boundary + "\r\n");
                    dos.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n");
                    dos.writeBytes(UPLOAD_PRESET + "\r\n");

                    dos.writeBytes("--" + boundary + "\r\n");
                    dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"upload_image\"\r\n");
                    dos.writeBytes("Content-Type: " + mimeType + "\r\n\r\n");
                    dos.write(imageBytes);
                    dos.writeBytes("\r\n");

                    dos.writeBytes("--" + boundary + "--\r\n");
                    dos.flush();
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    StringBuilder response = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null)
                            response.append(line);
                    }

                    JSONObject json = new JSONObject(response.toString());
                    String imageUrl = json.getString("secure_url");

                    Intent resultIntent = new Intent(ACTION_UPLOAD_COMPLETE);
                    resultIntent.putExtra(EXTRA_IMAGE_URL, imageUrl);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(resultIntent);

                    showCompleteNotification(getString(R.string.image_upload_success));
                } else {
                    InputStream es = conn.getErrorStream();
                    if (es != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(es));
                        String line;
                        StringBuilder sb = new StringBuilder();
                        while ((line = reader.readLine()) != null)
                            sb.append(line);
                        Log.e(TAG, "Cloudinary Error: " + sb.toString());
                    }
                    showCompleteNotification(getString(R.string.upload_failed_code, responseCode));
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception: " + e.getMessage());
                showCompleteNotification(getString(R.string.error_colon) + e.getMessage());
            } finally {
                if (conn != null)
                    conn.disconnect();
                stopSelf();
            }
        }).start();

        return START_NOT_STICKY;
    }

    private void updateNotification(String text, int progress) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.uploading_image_notification))
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_add)
                .setProgress(100, progress, false)
                .setOngoing(true)
                .build();

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null)
            nm.notify(NOTIFICATION_ID, notification);
    }

    private void showCompleteNotification(String text) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.image_upload_title))
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_add)
                .setAutoCancel(true)
                .build();

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(NOTIFICATION_ID);
            nm.notify(NOTIFICATION_ID + 1, notification);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.upload_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(getString(R.string.upload_channel_description));
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null)
                nm.createNotificationChannel(channel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

