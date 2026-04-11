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
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class NotificationCenterActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String tenantId;
    private String uid;
    private NotificationAdapter adapter;
    private ListenerRegistration notificationListener;
    private final List<NotificationItem> firestoreItems = new ArrayList<>();
    private final List<NotificationItem> systemItems = new ArrayList<>();

    private static final int CONTRACT_EXPIRY_WARNING_DAYS = 30;
    private static final NumberFormat MONEY_FORMAT = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

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
        loadSystemNotifications();
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
                        item.isSystem = false;
                        item.createdAt = doc.getTimestamp("createdAt");
                        list.add(item);
                    }

                    Collections.sort(list, (o1, o2) -> {
                        long t1 = o1.createdAt != null ? o1.createdAt.toDate().getTime() : 0L;
                        long t2 = o2.createdAt != null ? o2.createdAt.toDate().getTime() : 0L;
                        return Long.compare(t2, t1);
                    });
                    firestoreItems.clear();
                    firestoreItems.addAll(list);
                    renderMergedNotifications();
                });
    }

    private void loadSystemNotifications() {
        db.collection("tenants").document(tenantId)
                .collection("contracts")
                .whereEqualTo("contractStatus", "ACTIVE")
                .get()
                .addOnSuccessListener(snap -> {
                    List<NotificationItem> generated = new ArrayList<>();
                    Date now = new Date();
                    Calendar calendar = Calendar.getInstance();
                    int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String contractId = doc.getId();

                        Date endDate = parseDateFromDoc(doc, "contractEndDate", "endDate", "contractEndTimestamp");
                        if (endDate != null) {
                            long daysLeft = TimeUnit.MILLISECONDS.toDays(endDate.getTime() - now.getTime());
                            if (daysLeft >= 0 && daysLeft <= CONTRACT_EXPIRY_WARNING_DAYS) {
                                NotificationItem item = new NotificationItem();
                                item.id = "system-contract-expiry-" + contractId;
                                item.title = "Sắp hết hạn hợp đồng";
                                item.body = daysLeft == 0
                                        ? "Hợp đồng của bạn hết hạn hôm nay. Hãy liên hệ chủ trọ để gia hạn."
                                        : "Hợp đồng hết hạn vào " + formatDate(endDate)
                                        + " (còn " + daysLeft + " ngày). Hãy liên hệ chủ trọ để gia hạn.";
                                item.conversationId = null;
                                item.isRead = false;
                                item.isSystem = true;
                                item.createdAt = Timestamp.now();
                                generated.add(item);
                            }
                        }

                        String billingReminder = doc.getString("billingReminderAt");
                        if ("start_month".equals(billingReminder) && dayOfMonth >= 1 && dayOfMonth <= 5) {
                            Object rentObj = doc.get("roomPrice");
                            if (!(rentObj instanceof Number)) {
                                rentObj = doc.get("rentAmount");
                            }
                            String rentText = (rentObj instanceof Number)
                                    ? formatMoney(((Number) rentObj).doubleValue()) + " đ"
                                    : "theo hợp đồng";

                            NotificationItem item = new NotificationItem();
                            item.id = "system-billing-reminder-" + contractId;
                            item.title = "Nhắc nhở đóng tiền thuê";
                            item.body = "Đã đến kỳ thanh toán tháng này. Tiền thuê: " + rentText
                                    + ". Vui lòng thanh toán trước ngày 05.";
                            item.conversationId = null;
                            item.isRead = false;
                            item.isSystem = true;
                            item.createdAt = Timestamp.now();
                            generated.add(item);
                        }

                        boolean depositComplete = Boolean.TRUE.equals(doc.get("depositCollectionStatus"));
                        if (!depositComplete) {
                            Object depositObj = doc.get("depositAmount");
                            String depositText = (depositObj instanceof Number)
                                    ? formatMoney(((Number) depositObj).doubleValue()) + " đ"
                                    : "theo hợp đồng";

                            NotificationItem item = new NotificationItem();
                            item.id = "system-deposit-reminder-" + contractId;
                            item.title = "Tiền cọc chưa hoàn thiện";
                            item.body = "Bạn chưa đóng đủ tiền cọc (" + depositText
                                    + "). Vui lòng liên hệ chủ trọ để hoàn thiện.";
                            item.conversationId = null;
                            item.isRead = false;
                            item.isSystem = true;
                            item.createdAt = Timestamp.now();
                            generated.add(item);
                        }
                    }

                    systemItems.clear();
                    systemItems.addAll(generated);
                    renderMergedNotifications();
                })
                .addOnFailureListener(err -> {
                    systemItems.clear();
                    renderMergedNotifications();
                });
    }

    private void renderMergedNotifications() {
        List<NotificationItem> merged = new ArrayList<>(systemItems);
        merged.addAll(firestoreItems);
        adapter.submit(merged);
    }

    private Date parseDateFromDoc(DocumentSnapshot doc, String... fieldNames) {
        for (String field : fieldNames) {
            Object value = doc.get(field);
            if (value == null) {
                continue;
            }
            if (value instanceof Timestamp) {
                return ((Timestamp) value).toDate();
            }
            if (value instanceof Date) {
                return (Date) value;
            }
            if (value instanceof Long) {
                return new Date((Long) value);
            }
            if (value instanceof String) {
                String text = ((String) value).trim();
                if (!text.isEmpty()) {
                    Date parsed = tryParseDate(text, "dd/MM/yyyy");
                    if (parsed != null) {
                        return parsed;
                    }
                    parsed = tryParseDate(text, "yyyy-MM-dd");
                    if (parsed != null) {
                        return parsed;
                    }
                }
            }
        }
        return null;
    }

    private Date tryParseDate(String value, String pattern) {
        try {
            return new SimpleDateFormat(pattern, Locale.getDefault()).parse(value);
        } catch (ParseException ignored) {
            return null;
        }
    }

    private String formatDate(Date date) {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date);
    }

    private String formatMoney(double amount) {
        return MONEY_FORMAT.format((long) amount);
    }

    private void onNotificationClick(NotificationItem item) {
        if (item.isSystem) {
            return;
        }

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

    @Override
    protected void onResume() {
        super.onResume();
        loadSystemNotifications();
    }
}
