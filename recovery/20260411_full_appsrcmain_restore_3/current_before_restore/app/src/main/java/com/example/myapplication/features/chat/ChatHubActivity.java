package com.example.myapplication.features.chat;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.features.chat.model.ChatConversation;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
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

public class ChatHubActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String tenantId;
    private String uid;
    private String roomId;
    private String currentUserName;

    private ChatConversationAdapter adapter;
    private ListenerRegistration conversationListener;
    private ListenerRegistration unreadNotificationListener;
    private final List<ChatConversation> latestConversations = new ArrayList<>();
    private final Map<String, Integer> unreadByConversation = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_hub);

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
        currentUserName = user.getDisplayName();

        MaterialToolbar toolbar = findViewById(R.id.toolbarChatHub);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rvConversations);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatConversationAdapter(this::openConversation);
        rv.setAdapter(adapter);

        MaterialButton btnHouseChat = findViewById(R.id.btnHouseChat);
        MaterialButton btnRoomChat = findViewById(R.id.btnRoomChat);
        MaterialButton btnPrivateChat = findViewById(R.id.btnPrivateChat);
        TextView tvHint = findViewById(R.id.tvChatHint);

        btnHouseChat.setOnClickListener(v -> ensureHouseConversationAndOpen());
        btnRoomChat.setOnClickListener(v -> openRoomChatFlow());
        btnPrivateChat.setOnClickListener(v -> openPrivateChatPicker());

        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            String fullName = doc.getString("fullName");
            if (fullName != null && !fullName.trim().isEmpty()) {
                currentUserName = fullName.trim();
            }
        });

        db.collection("tenants").document(tenantId).collection("members").document(uid)
                .get()
                .addOnSuccessListener(memberDoc -> {
                    roomId = memberDoc.getString("roomId");
                    if (roomId == null || roomId.trim().isEmpty()) {
                        tvHint.setText(getString(R.string.chat_hub_hint_owner));
                    }
                });

        observeConversations();
    }

    private void observeConversations() {
        if (conversationListener != null) {
            conversationListener.remove();
            conversationListener = null;
        }

        conversationListener = db.collection("tenants").document(tenantId)
                .collection("chat_conversations")
                .whereArrayContains("participantIds", uid)
                .addSnapshotListener(this, (snap, err) -> {
                    if (err != null || snap == null) {
                        return;
                    }

                    List<ChatConversation> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        ChatConversation c = new ChatConversation();
                        c.id = doc.getId();
                        c.type = value(doc.getString("type"));
                        c.roomId = doc.getString("roomId");
                        c.title = resolveConversationTitle(doc);
                        c.subtitle = value(doc.getString("lastMessage"));
                        c.lastMessageAt = doc.getTimestamp("lastMessageAt");
                        c.updatedAt = doc.getTimestamp("updatedAt");
                        List<String> participants = (List<String>) doc.get("participantIds");
                        if (participants != null) {
                            c.participantIds.addAll(participants);
                        }
                        list.add(c);
                    }
                    latestConversations.clear();
                    latestConversations.addAll(list);
                    renderConversationList();
                });

        observeUnreadByConversation();
    }

    private void observeUnreadByConversation() {
        if (unreadNotificationListener != null) {
            unreadNotificationListener.remove();
            unreadNotificationListener = null;
        }

        unreadNotificationListener = db.collection("tenants").document(tenantId)
                .collection("notifications")
                .whereEqualTo("userId", uid)
                .whereEqualTo("isRead", false)
                .addSnapshotListener(this, (snap, err) -> {
                    if (err != null || snap == null) {
                        return;
                    }

                    unreadByConversation.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String conversationId = doc.getString("conversationId");
                        if (conversationId == null || conversationId.trim().isEmpty()) {
                            continue;
                        }
                        int current = unreadByConversation.containsKey(conversationId)
                                ? unreadByConversation.get(conversationId)
                                : 0;
                        unreadByConversation.put(conversationId, current + 1);
                    }

                    renderConversationList();
                });
    }

    private void renderConversationList() {
        List<ChatConversation> rendered = new ArrayList<>();
        for (ChatConversation base : latestConversations) {
            ChatConversation copy = new ChatConversation();
            copy.id = base.id;
            copy.type = base.type;
            copy.title = base.title;
            copy.subtitle = base.subtitle;
            copy.roomId = base.roomId;
            copy.lastMessageAt = base.lastMessageAt;
            copy.updatedAt = base.updatedAt;
            copy.participantIds.addAll(base.participantIds);
            copy.unreadCount = unreadByConversation.containsKey(base.id) ? unreadByConversation.get(base.id) : 0;
            rendered.add(copy);
        }

        Collections.sort(rendered, (o1, o2) -> compareTimestampDesc(preferMessageTs(o1), preferMessageTs(o2)));
        adapter.submit(rendered);
    }

    private int compareTimestampDesc(Timestamp t1, Timestamp t2) {
        long v1 = t1 != null ? t1.toDate().getTime() : 0L;
        long v2 = t2 != null ? t2.toDate().getTime() : 0L;
        return Long.compare(v2, v1);
    }

    private Timestamp preferMessageTs(ChatConversation c) {
        return c.lastMessageAt != null ? c.lastMessageAt : c.updatedAt;
    }

    private String resolveConversationTitle(DocumentSnapshot doc) {
        String type = value(doc.getString("type"));
        if ("HOUSE".equals(type)) {
            return getString(R.string.chat_house);
        }
        if ("ROOM".equals(type)) {
            return getString(R.string.chat_room_with_id, value(doc.getString("roomId")));
        }
        String displayName = doc.getString("displayName");
        if (displayName != null && !displayName.trim().isEmpty()) {
            return displayName;
        }
        return getString(R.string.chat_private);
    }

    private void openConversation(@NonNull ChatConversation item) {
        Intent intent = new Intent(this, ChatRoomActivity.class);
        intent.putExtra(ChatRoomActivity.EXTRA_CONVERSATION_ID, item.id);
        intent.putExtra(ChatRoomActivity.EXTRA_CONVERSATION_TITLE, item.title);
        startActivity(intent);
    }

    private void ensureHouseConversationAndOpen() {
        db.collection("tenants").document(tenantId)
                .collection("members")
                .get()
                .addOnSuccessListener(qs -> {
                    List<String> participants = new ArrayList<>();
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        String memberUid = doc.getString("uid");
                        if (memberUid != null && !memberUid.trim().isEmpty()) {
                            participants.add(memberUid.trim());
                        }
                    }
                    if (!participants.contains(uid)) {
                        participants.add(uid);
                    }
                    ensureConversation("house_" + tenantId, "HOUSE", null, participants, getString(R.string.chat_house),
                            this::openConversationById);
                });
    }

    private void openRoomChatFlow() {
        if (roomId != null && !roomId.trim().isEmpty()) {
            ensureRoomConversation(roomId.trim());
            return;
        }

        db.collection("tenants").document(tenantId)
                .collection("rooms")
                .get()
                .addOnSuccessListener(qs -> {
                    if (qs.isEmpty()) {
                        Toast.makeText(this, getString(R.string.chat_no_room_found), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    List<DocumentSnapshot> docs = qs.getDocuments();
                    String[] items = new String[docs.size()];
                    for (int i = 0; i < docs.size(); i++) {
                        String rid = docs.get(i).getId();
                        String roomNumber = docs.get(i).getString("roomNumber");
                        items[i] = roomNumber != null && !roomNumber.trim().isEmpty() ? roomNumber : rid;
                    }

                    new AlertDialog.Builder(this)
                            .setTitle(getString(R.string.chat_choose_room))
                            .setItems(items, (dialog, which) -> ensureRoomConversation(docs.get(which).getId()))
                            .setNegativeButton(getString(R.string.cancel), null)
                            .show();
                });
    }

    private void ensureRoomConversation(String selectedRoomId) {
        db.collection("tenants").document(tenantId)
                .collection("members")
                .whereEqualTo("roomId", selectedRoomId)
                .get()
                .addOnSuccessListener(qs -> {
                    List<String> participants = new ArrayList<>();
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        String memberUid = doc.getString("uid");
                        if (memberUid != null && !memberUid.trim().isEmpty()) {
                            participants.add(memberUid.trim());
                        }
                    }
                    if (!participants.contains(uid)) {
                        participants.add(uid);
                    }

                    ensureConversation("room_" + selectedRoomId, "ROOM", selectedRoomId, participants,
                            getString(R.string.chat_room_with_id, selectedRoomId), this::openConversationById);
                });
    }

    private void openPrivateChatPicker() {
        db.collection("tenants").document(tenantId)
                .collection("members")
                .whereEqualTo("status", "ACTIVE")
                .get()
                .addOnSuccessListener(qs -> {
                    List<DocumentSnapshot> options = new ArrayList<>();
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        String memberUid = doc.getString("uid");
                        if (memberUid != null && !memberUid.equals(uid)) {
                            options.add(doc);
                        }
                    }
                    if (options.isEmpty()) {
                        Toast.makeText(this, getString(R.string.chat_no_private_target), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String[] labels = new String[options.size()];
                    for (int i = 0; i < options.size(); i++) {
                        String memberUid = options.get(i).getString("uid");
                        String room = options.get(i).getString("roomId");
                        labels[i] = room == null || room.trim().isEmpty()
                                ? memberUid
                                : getString(R.string.chat_member_with_room, memberUid, room);
                    }

                    new AlertDialog.Builder(this)
                            .setTitle(getString(R.string.chat_choose_private_target))
                            .setItems(labels, (dialog, which) -> {
                                String otherUid = options.get(which).getString("uid");
                                if (otherUid == null || otherUid.trim().isEmpty()) {
                                    return;
                                }
                                ensurePrivateConversation(otherUid.trim());
                            })
                            .setNegativeButton(getString(R.string.cancel), null)
                            .show();
                });
    }

    private void ensurePrivateConversation(String otherUid) {
        String first = uid.compareTo(otherUid) <= 0 ? uid : otherUid;
        String second = uid.compareTo(otherUid) <= 0 ? otherUid : uid;
        String conversationId = "private_" + first + "_" + second;

        List<String> participants = new ArrayList<>();
        participants.add(first);
        participants.add(second);

        String title = getString(R.string.chat_private_with_uid, otherUid);
        db.collection("users").document(otherUid).get().addOnSuccessListener(doc -> {
            String fullName = doc.getString("fullName");
            if (fullName != null && !fullName.trim().isEmpty()) {
                ensureConversation(conversationId, "PRIVATE", null, participants, fullName, this::openConversationById);
            } else {
                ensureConversation(conversationId, "PRIVATE", null, participants, title, this::openConversationById);
            }
        }).addOnFailureListener(e -> ensureConversation(conversationId, "PRIVATE", null, participants, title, this::openConversationById));
    }

    private interface ConversationReady {
        void onReady(ChatConversation conversation);
    }

    private void ensureConversation(String id,
                                    String type,
                                    String roomIdValue,
                                    List<String> participants,
                                    String displayName,
                                    ConversationReady callback) {
        DocumentReference ref = db.collection("tenants").document(tenantId)
                .collection("chat_conversations")
                .document(id);

        ref.get().addOnSuccessListener(doc -> {
            Timestamp now = Timestamp.now();
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", type);
            payload.put("roomId", roomIdValue);
            payload.put("participantIds", participants);
            payload.put("displayName", displayName);
            payload.put("updatedAt", now);
            if (!doc.exists()) {
                payload.put("createdAt", now);
                payload.put("createdBy", uid);
                payload.put("lastMessage", "");
            }

            ref.set(payload, SetOptions.merge()).addOnSuccessListener(v -> {
                ChatConversation item = new ChatConversation();
                item.id = id;
                item.type = type;
                item.roomId = roomIdValue;
                item.title = displayName;
                item.subtitle = "";
                item.participantIds.addAll(participants);
                item.updatedAt = now;
                callback.onReady(item);
            });
        });
    }

    private void openConversationById(ChatConversation conversation) {
        openConversation(conversation);
    }

    private String value(String input) {
        return input == null ? "" : input;
    }

    @Override
    protected void onDestroy() {
        if (conversationListener != null) {
            conversationListener.remove();
            conversationListener = null;
        }
        if (unreadNotificationListener != null) {
            unreadNotificationListener.remove();
            unreadNotificationListener = null;
        }
        super.onDestroy();
    }
}
