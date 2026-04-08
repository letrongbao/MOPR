package com.example.myapplication.features.notification;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

import java.util.HashSet;
import java.util.Set;

public class NotificationRealtimeObserver {

    public interface UnreadCountCallback {
        void onCountChanged(int count);
    }

    private final Context appContext;
    private final FirebaseFirestore db;
    private final String tenantId;
    private final String userId;
    private final UnreadCountCallback callback;

    private final Set<String> knownNotificationIds = new HashSet<>();
    private ListenerRegistration listenerRegistration;
    private boolean initialSynced;

    public NotificationRealtimeObserver(
            @NonNull Context context,
            @NonNull FirebaseFirestore db,
            @NonNull String tenantId,
            @NonNull String userId,
            @Nullable UnreadCountCallback callback) {
        this.appContext = context.getApplicationContext();
        this.db = db;
        this.tenantId = tenantId;
        this.userId = userId;
        this.callback = callback;
    }

    public void start() {
        stop();

        listenerRegistration = db.collection("tenants").document(tenantId)
                .collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .addSnapshotListener((snap, err) -> {
                    if (err != null || snap == null) {
                        notifyCount(0);
                        return;
                    }

                    notifyCount(snap.size());

                    if (!initialSynced) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            knownNotificationIds.add(doc.getId());
                        }
                        initialSynced = true;
                        return;
                    }

                    for (DocumentChange change : snap.getDocumentChanges()) {
                        String id = change.getDocument().getId();
                        if (change.getType() == DocumentChange.Type.REMOVED) {
                            knownNotificationIds.remove(id);
                            continue;
                        }
                        if (change.getType() != DocumentChange.Type.ADDED) {
                            continue;
                        }
                        if (knownNotificationIds.contains(id)) {
                            continue;
                        }
                        knownNotificationIds.add(id);

                        String conversationId = change.getDocument().getString("conversationId");
                        String activeConversationId = ChatForegroundState.getActiveConversationId();
                        if (activeConversationId != null && activeConversationId.equals(conversationId)) {
                            Map<String, Object> update = new HashMap<>();
                            update.put("isRead", true);
                            update.put("readAt", com.google.firebase.Timestamp.now());
                            change.getDocument().getReference().set(update, SetOptions.merge());
                            continue;
                        }

                        String title = change.getDocument().getString("title");
                        String body = change.getDocument().getString("body");
                        NotificationDisplayUtil.showChatNotification(appContext, title, body);
                    }
                });
    }

    public void stop() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
        knownNotificationIds.clear();
        initialSynced = false;
    }

    private void notifyCount(int count) {
        if (callback != null) {
            callback.onCountChanged(Math.max(0, count));
        }
    }
}
