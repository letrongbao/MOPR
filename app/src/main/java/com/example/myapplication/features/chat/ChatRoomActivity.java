package com.example.myapplication.features.chat;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.ScreenUiHelper;
import com.example.myapplication.features.chat.model.ChatMessage;
import com.example.myapplication.features.notification.ChatForegroundState;
import com.google.android.material.appbar.AppBarLayout;
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
    private TextView tvChatTitleLine1;
    private TextView tvChatTitleLine2;

    private ChatMessageAdapter adapter;
    private ListenerRegistration messageListener;
    private static final int MAX_MESSAGE_LENGTH = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScreenUiHelper.enableEdgeToEdge(this, false);
        setContentView(R.layout.activity_chat_room);

        AppBarLayout appBarLayout = findViewById(R.id.appBarChatRoom);
        if (appBarLayout != null) {
            ScreenUiHelper.applyTopInset(appBarLayout);
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbarChatRoom);
        ScreenUiHelper.setupBackToolbar(this, (Toolbar) toolbar, "");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        tvChatTitleLine1 = findViewById(R.id.tvChatTitleLine1);
        tvChatTitleLine2 = findViewById(R.id.tvChatTitleLine2);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, getString(R.string.no_active_tenant), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        resolveActiveTenantAndInit(user);
    }

    private void resolveActiveTenantAndInit(FirebaseUser user) {
        String sessionTenantId = TenantSession.getActiveTenantId();
        if (sessionTenantId != null && !sessionTenantId.trim().isEmpty()) {
            tenantId = sessionTenantId.trim();
            initChatRoomUi(user);
            return;
        }

        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(userDoc -> {
                    String profileTenantId = userDoc.getString("activeTenantId");
                    if (profileTenantId != null && !profileTenantId.trim().isEmpty()) {
                        tenantId = profileTenantId.trim();
                        TenantSession.setActiveTenantId(this, tenantId);
                        initChatRoomUi(user);
                        return;
                    }

                    findOwnedTenantId(user.getUid(), recoveredTenantId -> {
                        if (recoveredTenantId == null || recoveredTenantId.trim().isEmpty()) {
                            Toast.makeText(this, getString(R.string.no_active_tenant), Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }

                        tenantId = recoveredTenantId.trim();
                        TenantSession.setActiveTenantId(this, tenantId);

                        Map<String, Object> update = new HashMap<>();
                        update.put("activeTenantId", tenantId);
                        update.put("updatedAt", Timestamp.now());
                        db.collection("users").document(user.getUid())
                                .set(update, SetOptions.merge())
                                .addOnCompleteListener(task -> initChatRoomUi(user));
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.no_active_tenant), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private interface OwnedTenantCallback {
        void onResult(String tenantId);
    }

    private void findOwnedTenantId(String uid, OwnedTenantCallback callback) {
        db.collection("tenants")
                .whereEqualTo("ownerUid", uid)
                .limit(1)
                .get()
                .addOnSuccessListener(qs -> {
                    if (qs != null && !qs.isEmpty()) {
                        callback.onResult(qs.getDocuments().get(0).getId());
                        return;
                    }

                    db.collectionGroup("members")
                            .whereEqualTo("uid", uid)
                            .limit(20)
                            .get()
                            .addOnSuccessListener(memberQs -> {
                                if (memberQs == null || memberQs.isEmpty()) {
                                    callback.onResult(null);
                                    return;
                                }

                                String selectedTenantId = null;
                                for (DocumentSnapshot memberDoc : memberQs.getDocuments()) {
                                    DocumentReference tenantRef = memberDoc.getReference().getParent().getParent();
                                    if (tenantRef == null) {
                                        continue;
                                    }

                                    String role = memberDoc.getString("role");
                                    if (role != null && "OWNER".equalsIgnoreCase(role.trim())) {
                                        selectedTenantId = tenantRef.getId();
                                        break;
                                    }

                                    if (selectedTenantId == null || selectedTenantId.trim().isEmpty()) {
                                        selectedTenantId = tenantRef.getId();
                                    }
                                }

                                callback.onResult(selectedTenantId);
                            })
                            .addOnFailureListener(e -> callback.onResult(null));
                })
                .addOnFailureListener(e -> callback.onResult(null));
    }

    private void initChatRoomUi(FirebaseUser user) {
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
    String fallbackHeader = title != null ? title : getString(R.string.chat_hub_title);
        setToolbarHeader(fallbackHeader, null);
        resolveConversationHeader(fallbackHeader);

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

    private void resolveConversationHeader(String fallbackHeader) {
        db.collection("tenants").document(tenantId)
                .collection("chat_conversations")
                .document(conversationId)
                .get()
                .addOnSuccessListener(conversationDoc -> {
                    if (conversationDoc == null || !conversationDoc.exists()) {
                        setToolbarHeader(fallbackHeader, null);
                        return;
                    }

                    String type = conversationDoc.getString("type");
                    if ("HOUSE".equals(type)) {
                        resolveHouseHeader(fallbackHeader);
                        return;
                    }

                    if ("ROOM".equals(type)) {
                        resolveRoomHeader(conversationDoc, fallbackHeader);
                        return;
                    }

                    if ("PRIVATE".equals(type)) {
                        resolvePrivateHeader(conversationDoc, fallbackHeader);
                        return;
                    }

                    setToolbarHeader(fallbackHeader, null);
                })
                .addOnFailureListener(e -> setToolbarHeader(fallbackHeader, null));
    }

    private void resolveHouseHeader(String fallbackHeader) {
        db.collection("tenants").document(tenantId)
                .get()
                .addOnSuccessListener(tenantDoc -> {
                    String tenantName = tenantDoc.getString("name");
                    if (tenantName == null || tenantName.trim().isEmpty()) {
                        setToolbarHeader(fallbackHeader, null);
                        return;
                    }
                    setToolbarHeader(tenantName.trim(), null);
                })
                .addOnFailureListener(e -> setToolbarHeader(fallbackHeader, null));
    }

    private void resolveRoomHeader(DocumentSnapshot conversationDoc, String fallbackHeader) {
        String roomId = conversationDoc.getString("roomId");
        if (roomId == null || roomId.trim().isEmpty()) {
            setToolbarHeader(fallbackHeader, null);
            return;
        }

        String normalizedRoomId = roomId.trim();
        db.collection("tenants").document(tenantId)
                .collection("rooms").document(normalizedRoomId)
                .get()
                .addOnSuccessListener(roomDoc -> {
                    if (roomDoc == null || !roomDoc.exists()) {
                        setToolbarHeader(getString(R.string.chat_room_with_id, normalizedRoomId), null);
                        return;
                    }

                    String roomNumber = roomDoc.getString("roomNumber");
                    String roomName = roomDoc.getString("name");
                    String title = (roomNumber != null && !roomNumber.trim().isEmpty())
                            ? getString(R.string.chat_room_with_id, roomNumber.trim())
                            : ((roomName != null && !roomName.trim().isEmpty())
                            ? roomName.trim()
                            : getString(R.string.chat_room_with_id, normalizedRoomId));

                    String houseName = roomDoc.getString("houseName");
                    String houseId = roomDoc.getString("houseId");
                    if (houseName != null && !houseName.trim().isEmpty()) {
                        setToolbarHeader(title, houseName.trim());
                        return;
                    }

                    if (houseId == null || houseId.trim().isEmpty()) {
                        resolveTenantName(name -> setToolbarHeader(title, name));
                        return;
                    }

                    db.collection("tenants").document(tenantId)
                            .collection("houses").document(houseId.trim())
                            .get()
                            .addOnSuccessListener(houseDoc -> {
                                String resolvedHouseName = houseDoc.getString("name");
                                if (resolvedHouseName != null && !resolvedHouseName.trim().isEmpty()) {
                                    setToolbarHeader(title, resolvedHouseName.trim());
                                } else {
                                    resolveTenantName(name -> setToolbarHeader(title, name));
                                }
                            })
                            .addOnFailureListener(e -> resolveTenantName(name -> setToolbarHeader(title, name)));
                })
                .addOnFailureListener(e -> setToolbarHeader(getString(R.string.chat_room_with_id, normalizedRoomId), null));
    }

    private void resolvePrivateHeader(DocumentSnapshot conversationDoc, String fallbackHeader) {
        String displayName = conversationDoc.getString("displayName");
        if (displayName != null && !displayName.trim().isEmpty()) {
            setToolbarHeader(displayName.trim(), null);
            return;
        }

        List<String> participants = (List<String>) conversationDoc.get("participantIds");
        if (participants == null || participants.isEmpty()) {
            setToolbarHeader(fallbackHeader, null);
            return;
        }

        String otherUid = null;
        for (String participant : participants) {
            if (participant != null && !participant.trim().isEmpty() && !participant.equals(uid)) {
                otherUid = participant.trim();
                break;
            }
        }

        if (otherUid == null) {
            setToolbarHeader(fallbackHeader, null);
            return;
        }

        final String targetUid = otherUid;

        db.collection("users").document(targetUid)
                .get()
                .addOnSuccessListener(userDoc -> {
                    String fullName = userDoc.getString("fullName");
                    String email = userDoc.getString("email");
                    if (fullName != null && !fullName.trim().isEmpty()) {
                        setToolbarHeader(fullName.trim(), null);
                        return;
                    }
                    if (email != null && !email.trim().isEmpty()) {
                        setToolbarHeader(email.trim(), null);
                        return;
                    }
                    setToolbarHeader(targetUid, null);
                })
                .addOnFailureListener(e -> setToolbarHeader(targetUid, null));
    }

    private interface TenantNameCallback {
        void onResult(String tenantName);
    }

    private void resolveTenantName(TenantNameCallback callback) {
        db.collection("tenants").document(tenantId)
                .get()
                .addOnSuccessListener(tenantDoc -> {
                    String name = tenantDoc.getString("name");
                    callback.onResult((name != null && !name.trim().isEmpty()) ? name.trim() : null);
                })
                .addOnFailureListener(e -> callback.onResult(null));
    }

    private void setToolbarHeader(String title, String subtitle) {
        String safeTitle = (title == null || title.trim().isEmpty()) ? getString(R.string.chat_hub_title) : title.trim();
        if (tvChatTitleLine1 != null) {
            tvChatTitleLine1.setText(safeTitle);
        }

        if (tvChatTitleLine2 != null) {
            String safeSubtitle = subtitle != null ? subtitle.trim() : "";
            if (safeSubtitle.isEmpty()) {
                tvChatTitleLine2.setText("");
                tvChatTitleLine2.setVisibility(View.GONE);
            } else {
                tvChatTitleLine2.setText(safeSubtitle);
                tvChatTitleLine2.setVisibility(View.VISIBLE);
            }
        }
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
