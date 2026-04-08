package com.example.myapplication.features.notification.push;

import com.example.myapplication.R;
import com.example.myapplication.features.notification.NotificationDisplayUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

public class AppFirebaseMessagingService extends FirebaseMessagingService {

    public static void syncTokenForCurrentUser() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
            if (token == null || token.trim().isEmpty()) {
                return;
            }
            Map<String, Object> payload = new HashMap<>();
            payload.put("uid", user.getUid());
            payload.put("token", token);
            payload.put("updatedAt", com.google.firebase.Timestamp.now());

            FirebaseFirestore.getInstance().collection("users")
                    .document(user.getUid())
                    .collection("fcm_tokens")
                    .document("primary")
                    .set(payload, SetOptions.merge());
        });
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        syncTokenForCurrentUser();
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = remoteMessage.getNotification() != null
                ? remoteMessage.getNotification().getTitle()
                : remoteMessage.getData().get("title");
        String body = remoteMessage.getNotification() != null
                ? remoteMessage.getNotification().getBody()
                : remoteMessage.getData().get("body");

        if (title == null || title.trim().isEmpty()) {
            title = getString(R.string.notification_center_title);
        }
        if (body == null) {
            body = "";
        }

        NotificationDisplayUtil.showChatNotification(this, title, body);
    }
}
