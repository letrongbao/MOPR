package com.example.myapplication.features.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import android.widget.LinearLayout;

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
import com.example.myapplication.features.chat.model.ChatMessage;
import com.example.myapplication.features.notification.ChatForegroundState;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
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
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ChatHubActivity extends AppCompatActivity {

    private static final String STATE_SELECTED_TAB = "chat_hub_selected_tab";
    public static final String EXTRA_USE_TENANT_HEADER = "USE_TENANT_HEADER";
    public static final String EXTRA_INITIAL_TAB = "INITIAL_TAB";
    public static final String EXTRA_FOCUS_MESSAGE_INPUT = "FOCUS_MESSAGE_INPUT";

    public static final int TAB_HOUSE = 0;
    public static final int TAB_ROOM = 1;
    public static final int TAB_PRIVATE = 2;

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
    private LinearLayout tenantInlineChatLayout;
    private RecyclerView tenantInlineMessagesRecyclerView;
    private TextInputEditText tenantInlineMessageInput;
    private MaterialButton tenantInlineSendButton;
    private TabLayout chatTypeTabLayout;
    private FloatingActionButton ownerNewConversationFab;
    private int restoredTabPosition = 0;
    private boolean hasRestoredTab = false;
    private boolean shouldFocusMessageInput = false;

    private ChatConversationAdapter adapter;
    private ListenerRegistration conversationListener;
    private ListenerRegistration unreadNotificationListener;
    private ListenerRegistration tenantInlineMessageListener;
    private final List<ChatConversation> latestConversations = new ArrayList<>();
    private final Map<String, Integer> unreadByConversation = new HashMap<>();
    private final Map<String, String> roomLabelById = new HashMap<>();
    private ChatMessageAdapter tenantInlineMessageAdapter;
    private String activeTenantConversationId;

    private static final int MAX_MESSAGE_LENGTH = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        shouldFocusMessageInput = getIntent().getBooleanExtra(EXTRA_FOCUS_MESSAGE_INPUT, false);

        if (savedInstanceState != null) {
            restoredTabPosition = savedInstanceState.getInt(STATE_SELECTED_TAB, 0);
            hasRestoredTab = true;
        } else {
            int initialTab = getIntent().getIntExtra(EXTRA_INITIAL_TAB, TAB_HOUSE);
            if (initialTab < TAB_HOUSE || initialTab > TAB_PRIVATE) {
                initialTab = TAB_HOUSE;
            }
            restoredTabPosition = initialTab;
            hasRestoredTab = true;
        }

        ScreenUiHelper.enableEdgeToEdge(this, false);
        setContentView(R.layout.activity_chat_hub);

        AppBarLayout appBarLayout = findViewById(R.id.appBarChatHub);
        if (appBarLayout != null) {
            ScreenUiHelper.applyTopInset(appBarLayout);
        }

        boolean useTenantHeader = getIntent().getBooleanExtra(EXTRA_USE_TENANT_HEADER, false);
        if (useTenantHeader) {
            if (appBarLayout != null) {
                appBarLayout.setBackgroundResource(R.drawable.bg_tenant_header_teal);
            }

            MaterialToolbar toolbar = findViewById(R.id.toolbarChatHub);
            if (toolbar != null) {
                toolbar.setBackgroundResource(R.drawable.bg_tenant_header_teal);
            }

            TabLayout tabLayout = findViewById(R.id.tabChatType);
            if (tabLayout != null) {
                tabLayout.setBackgroundResource(R.drawable.bg_tenant_header_teal);
            }

            FloatingActionButton fab = findViewById(R.id.fabOwnerNewConversation);
            if (fab != null) {
                fab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#00796B")));
            }
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

        tenantInlineChatLayout = findViewById(R.id.layoutTenantInlineChat);
        tenantInlineMessagesRecyclerView = findViewById(R.id.rvTenantInlineMessages);
        tenantInlineMessageInput = findViewById(R.id.etTenantInlineMessage);
        tenantInlineSendButton = findViewById(R.id.btnTenantInlineSend);
        if (tenantInlineMessagesRecyclerView != null) {
            tenantInlineMessagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            tenantInlineMessageAdapter = new ChatMessageAdapter(uid);
            tenantInlineMessagesRecyclerView.setAdapter(tenantInlineMessageAdapter);
        }

        if (tenantInlineSendButton != null) {
            tenantInlineSendButton.setOnClickListener(v -> {
                String content = tenantInlineMessageInput != null && tenantInlineMessageInput.getText() != null
                        ? tenantInlineMessageInput.getText().toString().trim()
                        : "";
                if (content.isEmpty()) {
                    return;
                }
                if (content.length() > MAX_MESSAGE_LENGTH) {
                    Toast.makeText(this, getString(R.string.chat_message_too_long, MAX_MESSAGE_LENGTH), Toast.LENGTH_SHORT)
                            .show();
                    return;
                }
                sendTenantInlineMessage(content);
                if (tenantInlineMessageInput != null) {
                    tenantInlineMessageInput.setText("");
                }
            });
        }

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
                loadRoomLabels();

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
                    loadRoomLabels();
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

        updateTabUnreadBadges();

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
            if (tenantInlineChatLayout != null) {
                tenantInlineChatLayout.setVisibility(View.GONE);
            }
            renderConversationList();
            setupOwnerFabForCurrentTab();
        } else {
            if (conversationsRecyclerView != null) {
                conversationsRecyclerView.setVisibility(RecyclerView.GONE);
            }
            if (tenantInlineChatLayout != null) {
                tenantInlineChatLayout.setVisibility(View.VISIBLE);
            }
            hideOwnerFab();
            openTenantTabInline(selectedTabPosition);
        }

        hasRestoredTab = false;
    }

    private void handleSelectedTab(int position) {
        boolean isTenant = TenantRoles.TENANT.equalsIgnoreCase(currentUserRole);
        if (isTenant) {
            openTenantTabInline(position);
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

    private void openTenantTabInline(int position) {
        if (position == 0) {
            ensureHouseConversation(this::openConversationInline);
            return;
        }

        if (position == 1) {
            if (roomId == null || roomId.trim().isEmpty()) {
                Toast.makeText(this, getString(R.string.chat_no_room_found), Toast.LENGTH_SHORT).show();
                return;
            }
            ensureRoomConversation(roomId.trim(), this::openConversationInline);
            return;
        }

        openLandlordPrivateChat(this::openConversationInline);
    }

    private void setupOwnerFabForCurrentTab() {
        if (ownerNewConversationFab == null) {
            return;
        }

        if (TenantRoles.TENANT.equalsIgnoreCase(currentUserRole)) {
            hideOwnerFab();
            return;
        }

        if ("HOUSE".equalsIgnoreCase(ownerConversationTypeFilter)) {
            // House thread is unique and always available in list, so no create action is needed.
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

    private void openLandlordPrivateChat(ConversationReady callback) {
        if (ownerUid == null || ownerUid.trim().isEmpty() || ownerUid.equals(uid)) {
            Toast.makeText(this, getString(R.string.chat_no_private_target), Toast.LENGTH_SHORT).show();
            return;
        }

        ensurePrivateConversation(ownerUid.trim(), callback);
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
                    updateTabUnreadBadges();
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
                    updateTabUnreadBadges();
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
        String type = value(doc.getString("type")).trim().toUpperCase(Locale.US);
        if ("HOUSE".equals(type)) {
            return getString(R.string.chat_house);
        }
        if ("ROOM".equals(type)) {
            String roomIdValue = value(doc.getString("roomId"));
            String roomLabel = roomLabelById.get(roomIdValue);
            if (roomLabel != null && !roomLabel.trim().isEmpty()) {
                return getString(R.string.chat_room_with_id, roomLabel.trim());
            }

            String displayName = doc.getString("displayName");
            if (displayName != null && !displayName.trim().isEmpty()) {
                return displayName;
            }

            return getString(R.string.chat_room_with_id, roomIdValue);
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
        ensureHouseConversation(this::openConversationById);
    }

    private void ensureHouseConversation(ConversationReady callback) {
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
                                    callback);
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
        ensureRoomConversation(selectedRoomId, this::openConversationById);
    }

    private void ensureRoomConversation(String selectedRoomId, ConversationReady callback) {
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

                    String roomLabel = roomLabelById.get(selectedRoomId);
                    if (roomLabel == null || roomLabel.trim().isEmpty()) {
                        roomLabel = selectedRoomId;
                    }

                    ensureConversation("room_" + selectedRoomId, "ROOM", selectedRoomId, participants,
                getString(R.string.chat_room_with_id, roomLabel), callback);
                });
    }

    private void loadRoomLabels() {
        db.collection("tenants").document(tenantId)
                .collection("rooms")
                .get()
                .addOnSuccessListener(qs -> {
                    roomLabelById.clear();
                    for (DocumentSnapshot roomDoc : qs.getDocuments()) {
                        String roomIdValue = roomDoc.getId();
                        String roomNumber = roomDoc.getString("roomNumber");
                        if (roomNumber != null && !roomNumber.trim().isEmpty()) {
                            roomLabelById.put(roomIdValue, roomNumber.trim());
                        }
                    }
                    renderConversationList();
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
        ensurePrivateConversation(otherUid, this::openConversationById);
    }

    private void ensurePrivateConversation(String otherUid, ConversationReady callback) {
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
                ensureConversation(conversationId, "PRIVATE", null, participants, fullName, callback);
            } else {
                ensureConversation(conversationId, "PRIVATE", null, participants, title, callback);
            }
        }).addOnFailureListener(e -> ensureConversation(conversationId, "PRIVATE", null, participants, title, callback));
    }

    private interface ConversationReady {
        void onReady(ChatConversation conversation);
    }

    private void openConversationInline(ChatConversation conversation) {
        if (conversation == null || conversation.id == null || conversation.id.trim().isEmpty()) {
            return;
        }
        activeTenantConversationId = conversation.id.trim();
        unreadByConversation.remove(activeTenantConversationId);
        updateTabUnreadBadges();
        observeTenantInlineMessages(activeTenantConversationId);
        markConversationNotificationsRead(activeTenantConversationId);
        ChatForegroundState.setActiveConversationId(activeTenantConversationId);
        focusMessageInputIfNeeded();
    }

    private void focusMessageInputIfNeeded() {
        if (!shouldFocusMessageInput || tenantInlineMessageInput == null) {
            return;
        }
        shouldFocusMessageInput = false;
        tenantInlineMessageInput.post(() -> {
            tenantInlineMessageInput.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(tenantInlineMessageInput, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    private void updateTabUnreadBadges() {
        if (chatTypeTabLayout == null) {
            return;
        }

        int houseUnread = 0;
        int roomUnread = 0;
        int privateUnread = 0;

        boolean tenantMode = TenantRoles.TENANT.equalsIgnoreCase(currentUserRole);
        for (ChatConversation conversation : latestConversations) {
            if (conversation == null || conversation.id == null || conversation.id.trim().isEmpty()) {
                continue;
            }

            int unread = unreadByConversation.containsKey(conversation.id)
                    ? unreadByConversation.get(conversation.id)
                    : 0;
            if (unread <= 0) {
                continue;
            }

            if (tenantMode && conversation.id.equals(activeTenantConversationId)) {
                continue;
            }

            String type = value(conversation.type).trim().toUpperCase();
            switch (type) {
                case "HOUSE":
                    houseUnread += unread;
                    break;
                case "ROOM":
                    roomUnread += unread;
                    break;
                default:
                    privateUnread += unread;
                    break;
            }
        }

        applyBadgeForTab(0, houseUnread);
        applyBadgeForTab(1, roomUnread);
        applyBadgeForTab(2, privateUnread);
    }

    private void applyBadgeForTab(int position, int count) {
        if (chatTypeTabLayout == null) {
            return;
        }
        TabLayout.Tab tab = chatTypeTabLayout.getTabAt(position);
        if (tab == null) {
            return;
        }
        if (count <= 0) {
            tab.removeBadge();
            return;
        }
        BadgeDrawable badge = tab.getOrCreateBadge();
        badge.setVisible(true);
        badge.setNumber(Math.min(99, count));
    }

    private void observeTenantInlineMessages(String conversationId) {
        if (tenantInlineMessageListener != null) {
            tenantInlineMessageListener.remove();
            tenantInlineMessageListener = null;
        }

        tenantInlineMessageListener = db.collection("tenants").document(tenantId)
                .collection("chat_conversations").document(conversationId)
                .collection("messages")
                .addSnapshotListener(this, (snap, err) -> {
                    if (err != null || snap == null || tenantInlineMessageAdapter == null) {
                        return;
                    }

                    List<ChatMessage> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        ChatMessage message = new ChatMessage();
                        message.id = doc.getId();
                        message.senderId = doc.getString("senderId");
                        message.senderName = doc.getString("senderName");
                        message.text = doc.getString("text");
                        message.createdAt = doc.getTimestamp("createdAt");
                        list.add(message);
                    }

                    Collections.sort(list,
                            (left, right) -> Long.compare(
                                    left.createdAt != null ? left.createdAt.toDate().getTime() : 0L,
                                    right.createdAt != null ? right.createdAt.toDate().getTime() : 0L));

                    tenantInlineMessageAdapter.submit(list);
                    if (tenantInlineMessagesRecyclerView != null && !list.isEmpty()) {
                        tenantInlineMessagesRecyclerView.scrollToPosition(list.size() - 1);
                    }
                });
    }

    private void sendTenantInlineMessage(String content) {
        if (activeTenantConversationId == null || activeTenantConversationId.trim().isEmpty()) {
            return;
        }

        DocumentReference conversationRef = db.collection("tenants").document(tenantId)
                .collection("chat_conversations")
                .document(activeTenantConversationId);

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
            message.put("senderName", currentUserName != null && !currentUserName.trim().isEmpty() ? currentUserName : uid);
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
                    pushTenantInlineNotifications(activeTenantConversationId, content, now)
            ).addOnFailureListener(e ->
                    Toast.makeText(this, getString(R.string.send_failed), Toast.LENGTH_SHORT).show()
            );
        });
    }

    private void pushTenantInlineNotifications(String conversationId, String content, Timestamp now) {
        DocumentReference conversationRef = db.collection("tenants").document(tenantId)
                .collection("chat_conversations")
                .document(conversationId);

        conversationRef.get().addOnSuccessListener(doc -> {
            List<String> participantIds = (List<String>) doc.get("participantIds");
            if (participantIds == null || participantIds.isEmpty()) {
                return;
            }

            String title = getString(R.string.chat_notification_title,
                    currentUserName != null && !currentUserName.trim().isEmpty() ? currentUserName : uid);
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

    private void markConversationNotificationsRead(String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            return;
        }

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
    protected void onStart() {
        super.onStart();
        if (TenantRoles.TENANT.equalsIgnoreCase(currentUserRole)
                && activeTenantConversationId != null
                && !activeTenantConversationId.trim().isEmpty()) {
            ChatForegroundState.setActiveConversationId(activeTenantConversationId);
        }
    }

    @Override
    protected void onStop() {
        if (TenantRoles.TENANT.equalsIgnoreCase(currentUserRole)) {
            ChatForegroundState.setActiveConversationId(null);
        }
        super.onStop();
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
        if (tenantInlineMessageListener != null) {
            tenantInlineMessageListener.remove();
            tenantInlineMessageListener = null;
        }
        super.onDestroy();
    }
}
