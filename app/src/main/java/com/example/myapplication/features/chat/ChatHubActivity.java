package com.example.myapplication.features.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.constants.TenantRoles;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.ScreenUiHelper;
import com.example.myapplication.features.chat.model.ChatConversation;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
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
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChatHubActivity extends AppCompatActivity {

    private static final String STATE_SELECTED_TAB = "chat_hub_selected_tab";

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String tenantId;
    private String uid;
    private String roomId;
    private String currentUserRole;
    private String ownerUid;
    private String currentUserName;
    private String ownerConversationTypeFilter = "HOUSE";
    private RecyclerView conversationsRecyclerView;
    private TabLayout chatTypeTabLayout;
    private FloatingActionButton ownerNewConversationFab;
    private int restoredTabPosition = 0;
    private boolean hasRestoredTab = false;

    private ChatConversationAdapter adapter;
    private ListenerRegistration conversationListener;
    private ListenerRegistration unreadNotificationListener;
    private final List<ChatConversation> latestConversations = new ArrayList<>();
    private final Map<String, Integer> unreadByConversation = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            restoredTabPosition = savedInstanceState.getInt(STATE_SELECTED_TAB, 0);
            hasRestoredTab = true;
        }

        ScreenUiHelper.enableEdgeToEdge(this, false);
        setContentView(R.layout.activity_chat_hub);

        AppBarLayout appBarLayout = findViewById(R.id.appBarChatHub);
        if (appBarLayout != null) {
            ScreenUiHelper.applyTopInset(appBarLayout);
        }

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

    private void resolveActiveTenantAndInit(@NonNull FirebaseUser user) {
        String sessionTenantId = TenantSession.getActiveTenantId();
        if (sessionTenantId != null && !sessionTenantId.trim().isEmpty()) {
            tenantId = sessionTenantId.trim();
            initChatUi(user);
            return;
        }

        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(userDoc -> {
                    String profileTenantId = userDoc.getString("activeTenantId");
                    if (profileTenantId != null && !profileTenantId.trim().isEmpty()) {
                        tenantId = profileTenantId.trim();
                        TenantSession.setActiveTenantId(this, tenantId);
                        initChatUi(user);
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
                                .addOnCompleteListener(task -> initChatUi(user));
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

    private void initChatUi(@NonNull FirebaseUser user) {
        uid = user.getUid();
        currentUserName = user.getDisplayName();
        currentUserRole = "";

        MaterialToolbar toolbar = findViewById(R.id.toolbarChatHub);
        ScreenUiHelper.setupBackToolbar(this, (Toolbar) toolbar, getString(R.string.chat_hub_title));

        conversationsRecyclerView = findViewById(R.id.rvConversations);
        conversationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatConversationAdapter(this::openConversation);
        conversationsRecyclerView.setAdapter(adapter);

        chatTypeTabLayout = findViewById(R.id.tabChatType);
        ownerNewConversationFab = findViewById(R.id.fabOwnerNewConversation);

        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            String fullName = doc.getString("fullName");
            if (fullName != null && !fullName.trim().isEmpty()) {
                currentUserName = fullName.trim();
            }
        });

        db.collection("tenants").document(tenantId)
                .get()
                .addOnSuccessListener(tenantDoc -> {
                    ownerUid = tenantDoc.getString("ownerUid");

                    db.collection("tenants").document(tenantId).collection("members").document(uid)
                            .get()
                            .addOnSuccessListener(memberDoc -> {
                                roomId = memberDoc.getString("roomId");
                                currentUserRole = value(memberDoc.getString("role"));
                                setupTopTabs(chatTypeTabLayout);
                            })
                            .addOnFailureListener(e -> setupTopTabs(chatTypeTabLayout));
                })
                .addOnFailureListener(e -> {
                    ownerUid = null;
                    setupTopTabs(chatTypeTabLayout);
                });

        observeConversations();
    }

    private void setupTopTabs(TabLayout tabLayout) {
        if (tabLayout == null) {
            return;
        }

        tabLayout.clearOnTabSelectedListeners();
        tabLayout.removeAllTabs();

        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.chat_tab_house)), true);
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.chat_tab_room)));

        boolean isTenant = TenantRoles.TENANT.equalsIgnoreCase(currentUserRole);
        tabLayout.addTab(tabLayout.newTab().setText(
                isTenant ? getString(R.string.chat_tab_landlord) : getString(R.string.chat_tab_guest)));

        int selectedTabPosition = hasRestoredTab ? restoredTabPosition : 0;
        if (selectedTabPosition < 0 || selectedTabPosition > 2) {
            selectedTabPosition = 0;
        }
        TabLayout.Tab selectedTab = tabLayout.getTabAt(selectedTabPosition);
        if (selectedTab != null) {
            selectedTab.select();
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab == null) {
                    return;
                }
                handleSelectedTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                if (tab == null) {
                    return;
                }
                handleSelectedTab(tab.getPosition());
            }
        });

        boolean tenantMode = TenantRoles.TENANT.equalsIgnoreCase(currentUserRole);
        if (!tenantMode) {
            ownerConversationTypeFilter = tabPositionToType(selectedTabPosition);
            if (conversationsRecyclerView != null) {
                conversationsRecyclerView.setVisibility(RecyclerView.VISIBLE);
            }
            renderConversationList();
            setupOwnerFabForCurrentTab();
        } else {
            if (conversationsRecyclerView != null) {
                conversationsRecyclerView.setVisibility(RecyclerView.GONE);
            }
            hideOwnerFab();

            if (!hasRestoredTab) {
                ensureHouseConversationAndOpen();
            }
        }

        hasRestoredTab = false;
    }

    private void handleSelectedTab(int position) {
        boolean isTenant = TenantRoles.TENANT.equalsIgnoreCase(currentUserRole);
        if (isTenant) {
            if (position == 0) {
                ensureHouseConversationAndOpen();
            } else if (position == 1) {
                openRoomChatFlow();
            } else {
                openLandlordPrivateChat();
            }
            return;
        }

        if (position == 0) {
            ownerConversationTypeFilter = "HOUSE";
        } else if (position == 1) {
            ownerConversationTypeFilter = "ROOM";
        } else {
            ownerConversationTypeFilter = "PRIVATE";
        }
        renderConversationList();
        setupOwnerFabForCurrentTab();
    }

    private void setupOwnerFabForCurrentTab() {
        if (ownerNewConversationFab == null) {
            return;
        }

        if (TenantRoles.TENANT.equalsIgnoreCase(currentUserRole)) {
            hideOwnerFab();
            return;
        }

        ownerNewConversationFab.setVisibility(View.VISIBLE);
        ownerNewConversationFab.setOnClickListener(v -> openOwnerCreateFlowForCurrentTab());
    }

    private void hideOwnerFab() {
        if (ownerNewConversationFab == null) {
            return;
        }
        ownerNewConversationFab.setVisibility(View.GONE);
        ownerNewConversationFab.setOnClickListener(null);
    }

    private void openOwnerCreateFlowForCurrentTab() {
        String type = ownerConversationTypeFilter;
        if ((type == null || type.trim().isEmpty()) && chatTypeTabLayout != null && chatTypeTabLayout.getSelectedTabPosition() >= 0) {
            type = tabPositionToType(chatTypeTabLayout.getSelectedTabPosition());
        }

        if ("ROOM".equalsIgnoreCase(type)) {
            openRoomChatFlow();
        } else if ("PRIVATE".equalsIgnoreCase(type)) {
            openPrivateChatPicker();
        } else {
            ensureHouseConversationAndOpen();
        }
    }

    private String tabPositionToType(int position) {
        if (position == 1) {
            return "ROOM";
        }
        if (position == 2) {
            return "PRIVATE";
        }
        return "HOUSE";
    }

    private void openLandlordPrivateChat() {
        if (ownerUid == null || ownerUid.trim().isEmpty() || ownerUid.equals(uid)) {
            Toast.makeText(this, getString(R.string.chat_no_private_target), Toast.LENGTH_SHORT).show();
            return;
        }

        ensurePrivateConversation(ownerUid.trim());
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
        if (adapter == null) {
            return;
        }

        List<ChatConversation> rendered = new ArrayList<>();
        boolean isOwner = TenantRoles.OWNER.equalsIgnoreCase(currentUserRole);
        for (ChatConversation base : latestConversations) {
            if (isOwner && !ownerConversationTypeFilter.equalsIgnoreCase(value(base.type))) {
                continue;
            }

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
                .get()
                .addOnSuccessListener(tenantDoc -> {
                    String ownerUid = tenantDoc.getString("ownerUid");

                    db.collection("tenants").document(tenantId)
                            .collection("members")
                            .get()
                            .addOnSuccessListener(qs -> {
                                Set<String> participantSet = new HashSet<>();
                                for (DocumentSnapshot doc : qs.getDocuments()) {
                                    String memberUid = doc.getString("uid");
                                    if (memberUid != null && !memberUid.trim().isEmpty()) {
                                        participantSet.add(memberUid.trim());
                                    }
                                }
                                if (ownerUid != null && !ownerUid.trim().isEmpty()) {
                                    participantSet.add(ownerUid.trim());
                                }
                                participantSet.add(uid);

                                ensureConversation(
                                        "house_" + tenantId,
                                        "HOUSE",
                                        null,
                                        new ArrayList<>(participantSet),
                                        getString(R.string.chat_house),
                                        this::openConversationById);
                            });
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
                .get()
                .addOnSuccessListener(tenantDoc -> {
                    String ownerUid = tenantDoc.getString("ownerUid");

                    db.collection("tenants").document(tenantId)
                            .collection("members")
                            .whereEqualTo("status", "ACTIVE")
                            .get()
                            .addOnSuccessListener(qs -> {
                                List<String> optionUids = new ArrayList<>();
                                List<String> labels = new ArrayList<>();

                                for (DocumentSnapshot doc : qs.getDocuments()) {
                                    String memberUid = doc.getString("uid");
                                    if (memberUid == null || memberUid.trim().isEmpty()) {
                                        continue;
                                    }
                                    memberUid = memberUid.trim();
                                    if (memberUid.equals(uid) || optionUids.contains(memberUid)) {
                                        continue;
                                    }

                                    String room = doc.getString("roomId");
                                    String label = room == null || room.trim().isEmpty()
                                            ? memberUid
                                            : getString(R.string.chat_member_with_room, memberUid, room);
                                    optionUids.add(memberUid);
                                    labels.add(label);
                                }

                                if (ownerUid != null && !ownerUid.trim().isEmpty()) {
                                    String normalizedOwnerUid = ownerUid.trim();
                                    if (!normalizedOwnerUid.equals(uid) && !optionUids.contains(normalizedOwnerUid)) {
                                        optionUids.add(normalizedOwnerUid);
                                        labels.add(normalizedOwnerUid);
                                    }
                                }

                                if (optionUids.isEmpty()) {
                                    Toast.makeText(this, getString(R.string.chat_no_private_target), Toast.LENGTH_SHORT)
                                            .show();
                                    return;
                                }

                                new AlertDialog.Builder(this)
                                        .setTitle(getString(R.string.chat_choose_private_target))
                                        .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                                            String otherUid = optionUids.get(which);
                                            if (otherUid == null || otherUid.trim().isEmpty()) {
                                                return;
                                            }
                                            ensurePrivateConversation(otherUid.trim());
                                        })
                                        .setNegativeButton(getString(R.string.cancel), null)
                                        .show();
                            });
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
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (chatTypeTabLayout != null) {
            int selectedPosition = chatTypeTabLayout.getSelectedTabPosition();
            if (selectedPosition >= 0) {
                outState.putInt(STATE_SELECTED_TAB, selectedPosition);
                return;
            }
        }
        outState.putInt(STATE_SELECTED_TAB, 0);
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
