package com.example.myapplication.features.notification;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.features.chat.ChatRoomActivity;
import com.example.myapplication.features.notification.model.NotificationItem;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationCenterActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String tenantId;
    private String uid;
    private NotificationAdapter adapter;
    private ListenerRegistration notificationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_center);

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        tenantId = TenantSession.getActiveTenantId();
        if (user == null || tenantId == null || tenantId.trim().isEmpty()) {
            Toast.makeText(this, getString(R.string.no_active_tenant), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        uid = user.getUid();

        MaterialToolbar toolbar = findViewById(R.id.toolbarNotifications);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rvNotifications);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(this::onNotificationClick);
        rv.setAdapter(adapter);

        observeNotifications();
    }

    private void observeNotifications() {
        if (notificationListener != null) {
            notificationListener.remove();
            notificationListener = null;
        }

        notificationListener = db.collection("tenants").document(tenantId)
                .collection("notifications")
                .whereEqualTo("userId", uid)
                .addSnapshotListener(this, (snap, err) -> {
                    if (err != null || snap == null) {
                        return;
                    }

                    List<NotificationItem> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        NotificationItem item = new NotificationItem();
                        item.id = doc.getId();
                        item.title = doc.getString("title");
                        item.body = doc.getString("body");
                        item.conversationId = doc.getString("conversationId");
                        item.isRead = Boolean.TRUE.equals(doc.getBoolean("isRead"));
                        item.createdAt = doc.getTimestamp("createdAt");
                        list.add(item);
                    }

                    Collections.sort(list, (o1, o2) -> {
                        long t1 = o1.createdAt != null ? o1.createdAt.toDate().getTime() : 0L;
                        long t2 = o2.createdAt != null ? o2.createdAt.toDate().getTime() : 0L;
                        return Long.compare(t2, t1);
                    });
                    adapter.submit(list);
                });
    }

    private void onNotificationClick(NotificationItem item) {
        DocumentReference ref = db.collection("tenants").document(tenantId)
                .collection("notifications").document(item.id);

        Map<String, Object> update = new HashMap<>();
        update.put("isRead", true);
        update.put("readAt", Timestamp.now());
        ref.set(update, SetOptions.merge());

        if (item.conversationId != null && !item.conversationId.trim().isEmpty()) {
            db.collection("tenants").document(tenantId)
                    .collection("chat_conversations").document(item.conversationId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        String title = doc.getString("displayName");
                        if (title == null || title.trim().isEmpty()) {
                            title = getString(R.string.chat_hub_title);
                        }
                        Intent intent = new Intent(this, ChatRoomActivity.class);
                        intent.putExtra(ChatRoomActivity.EXTRA_CONVERSATION_ID, item.conversationId);
                        intent.putExtra(ChatRoomActivity.EXTRA_CONVERSATION_TITLE, title);
                        startActivity(intent);
                    });
        }
    }

    @Override
    protected void onDestroy() {
        if (notificationListener != null) {
            notificationListener.remove();
            notificationListener = null;
        }
        super.onDestroy();
    }
}
