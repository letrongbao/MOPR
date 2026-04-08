package com.example.myapplication.features.chat;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.features.chat.model.ChatMessage;
import com.example.myapplication.features.notification.ChatForegroundState;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ChatRoomActivity extends AppCompatActivity {

    public static final String EXTRA_CONVERSATION_ID = "EXTRA_CONVERSATION_ID";
    public static final String EXTRA_CONVERSATION_TITLE = "EXTRA_CONVERSATION_TITLE";

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String tenantId;
    private String uid;
    private String userName;
    private String conversationId;

    private ChatMessageAdapter adapter;
    private ListenerRegistration messageListener;
    private static final int MAX_MESSAGE_LENGTH = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        FirebaseUser user = auth.getCurrentUser();
        tenantId = TenantSession.getActiveTenantId();
        if (user == null || tenantId == null || tenantId.trim().isEmpty()) {
            Toast.makeText(this, getString(R.string.no_active_tenant), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        uid = user.getUid();
        userName = user.getDisplayName();

        conversationId = getIntent().getStringExtra(EXTRA_CONVERSATION_ID);
        String title = getIntent().getStringExtra(EXTRA_CONVERSATION_TITLE);
        if (conversationId == null || conversationId.trim().isEmpty()) {
            Toast.makeText(this, getString(R.string.chat_invalid_conversation), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbarChatRoom);
        toolbar.setTitle(title != null ? title : getString(R.string.chat_hub_title));
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rvMessages);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatMessageAdapter(uid);
        rv.setAdapter(adapter);

        TextInputEditText etMessage = findViewById(R.id.etMessage);
        MaterialButton btnSend = findViewById(R.id.btnSendMessage);
        btnSend.setOnClickListener(v -> {
            String content = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
            if (content.isEmpty()) {
                return;
            }
            if (content.length() > MAX_MESSAGE_LENGTH) {
                Toast.makeText(this, getString(R.string.chat_message_too_long, MAX_MESSAGE_LENGTH), Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            sendMessage(content);
            etMessage.setText("");
        });

        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            String fullName = doc.getString("fullName");
            if (fullName != null && !fullName.trim().isEmpty()) {
                userName = fullName.trim();
            }
        });

        observeMessages();
        markConversationNotificationsRead();
    }

    private void observeMessages() {
        if (messageListener != null) {
            messageListener.remove();
            messageListener = null;
        }

        messageListener = db.collection("tenants").document(tenantId)
                .collection("chat_conversations").document(conversationId)
                .collection("messages")
                .addSnapshotListener(this, (snap, err) -> {
                    if (err != null || snap == null) {
                        return;
                    }
                    List<ChatMessage> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        ChatMessage m = new ChatMessage();
                        m.id = doc.getId();
                        m.senderId = doc.getString("senderId");
                        m.senderName = doc.getString("senderName");
                        m.text = doc.getString("text");
                        m.createdAt = doc.getTimestamp("createdAt");
                        list.add(m);
                    }

                    Collections.sort(list, Comparator.comparingLong(o -> o.createdAt != null ? o.createdAt.toDate().getTime() : 0L));
                    adapter.submit(list);
                    RecyclerView rv = findViewById(R.id.rvMessages);
                    if (!list.isEmpty()) {
                        rv.scrollToPosition(list.size() - 1);
                    }
                });
    }

    private void sendMessage(String content) {
        DocumentReference conversationRef = db.collection("tenants").document(tenantId)
                .collection("chat_conversations")
                .document(conversationId);
        conversationRef.get().addOnSuccessListener(doc -> {
            List<String> participantIds = (List<String>) doc.get("participantIds");
            if (participantIds == null || !participantIds.contains(uid)) {
                Toast.makeText(this, getString(R.string.chat_send_not_allowed), Toast.LENGTH_SHORT).show();
                return;
            }

            DocumentReference messageRef = conversationRef.collection("messages").document();
            Timestamp now = Timestamp.now();

            Map<String, Object> message = new HashMap<>();
            message.put("senderId", uid);
            message.put("senderName", userName != null && !userName.trim().isEmpty() ? userName : uid);
            message.put("text", content);
            message.put("createdAt", now);

            WriteBatch batch = db.batch();
            batch.set(messageRef, message, SetOptions.merge());

            Map<String, Object> conversationUpdate = new HashMap<>();
            conversationUpdate.put("lastMessage", content);
            conversationUpdate.put("lastSenderId", uid);
            conversationUpdate.put("lastMessageAt", now);
            conversationUpdate.put("updatedAt", now);
            batch.set(conversationRef, conversationUpdate, SetOptions.merge());

            batch.commit().addOnSuccessListener(v ->
                pushNotificationsForParticipants(content, now)
            ).addOnFailureListener(e ->
                Toast.makeText(this, getString(R.string.send_failed), Toast.LENGTH_SHORT).show()
            );
        });
    }

    private void pushNotificationsForParticipants(String content, Timestamp now) {
        DocumentReference conversationRef = db.collection("tenants").document(tenantId)
                .collection("chat_conversations")
                .document(conversationId);

        conversationRef.get().addOnSuccessListener(doc -> {
            List<String> participantIds = (List<String>) doc.get("participantIds");
            if (participantIds == null || participantIds.isEmpty()) {
                return;
            }

            String title = getString(R.string.chat_notification_title, userName != null ? userName : uid);
            WriteBatch batch = db.batch();
            for (String participantId : participantIds) {
                if (participantId == null || participantId.trim().isEmpty() || uid.equals(participantId)) {
                    continue;
                }
                DocumentReference notificationRef = db.collection("tenants").document(tenantId)
                        .collection("notifications").document(UUID.randomUUID().toString());
                Map<String, Object> payload = new HashMap<>();
                payload.put("userId", participantId);
                payload.put("type", "CHAT_MESSAGE");
                payload.put("title", title);
                payload.put("body", content);
                payload.put("conversationId", conversationId);
                payload.put("isRead", false);
                payload.put("createdAt", now);
                payload.put("senderId", uid);
                payload.put("pushState", "PENDING_SERVER_DISPATCH");
                batch.set(notificationRef, payload, SetOptions.merge());
            }
            batch.commit();
        });
    }

    private void markConversationNotificationsRead() {
        db.collection("tenants").document(tenantId)
                .collection("notifications")
                .whereEqualTo("userId", uid)
                .whereEqualTo("conversationId", conversationId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(qs -> {
                    if (qs == null || qs.isEmpty()) {
                        return;
                    }
                    WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot doc : qs) {
                        Map<String, Object> update = new HashMap<>();
                        update.put("isRead", true);
                        update.put("readAt", Timestamp.now());
                        batch.set(doc.getReference(), update, SetOptions.merge());
                    }
                    batch.commit();
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        ChatForegroundState.setActiveConversationId(conversationId);
        markConversationNotificationsRead();
    }

    @Override
    protected void onStop() {
        String activeConversation = ChatForegroundState.getActiveConversationId();
        if (conversationId != null && conversationId.equals(activeConversation)) {
            ChatForegroundState.setActiveConversationId(null);
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (messageListener != null) {
            messageListener.remove();
            messageListener = null;
        }
        super.onDestroy();
    }
}
