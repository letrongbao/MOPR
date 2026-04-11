package com.example.myapplication.features.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.session.TenantSession;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Màn hình Thông báo cho Khách thuê.
 *
 * Quy trình:
 *  1. generateSystemNotifications() → truy vấn Firestore "contracts" để tạo thông báo
 *     thông minh (hết hạn, nhắc tiền, nhắc cọc). Kết quả đặt ở đầu danh sách.
 *  2. loadFirestoreNotifications() → lấy collection "notifications" thông thường.
 *  3. Trộn 2 danh sách, hiển thị qua NotificationAdapter.
 */
public class NotificationActivity extends AppCompatActivity {

    private static final String TAG = "NotificationActivity";

    // Collection paths khớp với TenantMenuActivity
    private static final String COL_TENANTS      = "tenants";
    private static final String COL_CONTRACTS    = "contracts";
    private static final String COL_NOTIFICATIONS = "notifications";

    private static final SimpleDateFormat DF_DISPLAY =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private static final NumberFormat MONEY_FORMAT =
            NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    // ── Views ──
    private RecyclerView  rvNotifications;
    private ProgressBar   progressLoading;
    private LinearLayout  layoutEmpty;
    private LinearLayout  bannerSystemCount;
    private TextView      tvSystemCountLabel;

    // ── Data ──
    private String tenantId;
    private FirebaseFirestore db;

    // Kết quả hợp nhất
    private final List<NotificationItem> systemNotifs   = new ArrayList<>();
    private final List<NotificationItem> regularNotifs  = new ArrayList<>();

    // Đếm task truy vấn song song hoàn thành
    private final AtomicInteger pendingTasks = new AtomicInteger(0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        db = FirebaseFirestore.getInstance();

        tenantId = getIntent().getStringExtra("TENANT_ID");
        if (tenantId == null || tenantId.isEmpty()) {
            tenantId = TenantSession.getActiveTenantId();
        }
        if (tenantId == null || tenantId.isEmpty()) {
            // Fallback: uid hiện tại
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                tenantId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            }
        }

        bindViews();
        loadAllNotifications();
    }

    @Override
    public void onBackPressed() {
        goBack();
    }

    /** Quay lại TenantMenuActivity, không tạo instance mới nếu đã có trong stack */
    private void goBack() {
        Intent intent = new Intent(this, TenantMenuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    // ================================================================
    //  Ánh xạ View
    // ================================================================
    private void bindViews() {
        // Nút Back — gán listener đầu tiên
        View backBtn = findViewById(R.id.btnBack);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> goBack());
        }

        rvNotifications   = findViewById(R.id.rvNotifications);
        progressLoading   = findViewById(R.id.progressLoading);
        layoutEmpty       = findViewById(R.id.layoutEmpty);
        bannerSystemCount = findViewById(R.id.bannerSystemCount);
        tvSystemCountLabel = findViewById(R.id.tvSystemCountLabel);

        if (rvNotifications != null) {
            rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        }
    }

    // ================================================================
    //  Điều phối load song song
    // ================================================================
    private void loadAllNotifications() {
        showLoading();
        systemNotifs.clear();
        regularNotifs.clear();
        pendingTasks.set(2); // 2 task song song

        generateSystemNotifications(); // Task 1
        loadFirestoreNotifications();  // Task 2
    }

    /** Gọi sau khi mỗi task hoàn thành — khi cả 2 xong thì render trên UI thread. */
    private void onTaskComplete() {
        if (pendingTasks.decrementAndGet() == 0) {
            runOnUiThread(this::renderList);
        }
    }

    // ================================================================
    //  Task 1: Tạo thông báo hệ thống từ dữ liệu Hợp đồng
    // ================================================================
    private void generateSystemNotifications() {
        if (tenantId == null || tenantId.isEmpty()) {
            onTaskComplete();
            return;
        }

        // Truy vấn tenants/{tenantId}/contracts nơi contractStatus == "ACTIVE"
        db.collection(COL_TENANTS).document(tenantId)
                .collection(COL_CONTRACTS)
                .whereEqualTo("contractStatus", "ACTIVE")
                .get()
                .addOnSuccessListener(qs -> {
                    if (qs == null || qs.isEmpty()) {
                        // Fallback: users/{tenantId}/contracts
                        tryFallbackContracts();
                    } else {
                        processContractDocs(qs);
                        onTaskComplete();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi load contracts: " + e.getMessage(), e);
                    onTaskComplete();
                });
    }

    /** Fallback sang collection users/{tenantId}/contracts nếu không có dưới tenants/ */
    private void tryFallbackContracts() {
        db.collection("users").document(tenantId)
                .collection(COL_CONTRACTS)
                .whereEqualTo("contractStatus", "ACTIVE")
                .get()
                .addOnSuccessListener(qs2 -> {
                    if (qs2 != null && !qs2.isEmpty()) {
                        processContractDocs(qs2);
                    }
                    onTaskComplete();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Fallback lỗi: " + e.getMessage(), e);
                    onTaskComplete();
                });
    }

    /**
     * Duyệt qua từng document contract và tạo thông báo phù hợp:
     *  (A) Sắp hết hạn hợp đồng (trong 30 ngày)
     *  (B) Nhắc đóng tiền thuê (đầu tháng ngày 01-05)
     *  (C) Tiền cọc chưa đóng đủ
     */
    private void processContractDocs(QuerySnapshot qs) {
        Date today = new Date();
        Calendar cal = Calendar.getInstance();
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);

        for (DocumentSnapshot doc : qs.getDocuments()) {
            // ── (A) Kiểm tra ngày hết hạn ──────────────────────────────
            Date endDate = parseDateFromDoc(doc, "contractEndDate", "endDate");
            if (endDate != null) {
                long daysLeft = TimeUnit.MILLISECONDS.toDays(endDate.getTime() - today.getTime());
                if (daysLeft >= 0 && daysLeft <= 30) {
                    String body = daysLeft == 0
                            ? "Hợp đồng của bạn hết hạn HÔM NAY. Hãy liên hệ chủ trọ để gia hạn!"
                            : "Hợp đồng hết hạn vào " + DF_DISPLAY.format(endDate)
                              + " (còn " + daysLeft + " ngày). Hãy liên hệ chủ trọ để gia hạn.";

                    systemNotifs.add(new NotificationItem(
                            NotificationItem.TYPE_SYSTEM,
                            NotificationItem.ICON_CONTRACT,
                            "Sắp hết hạn hợp đồng",
                            body,
                            "Hôm nay"
                    ));
                }
            }

            // ── (B) Nhắc đóng tiền thuê ─────────────────────────────────
            String billingReminder = doc.getString("billingReminderAt");
            if ("start_month".equals(billingReminder) && dayOfMonth >= 1 && dayOfMonth <= 5) {
                Object rentObj = doc.get("roomPrice");
                if (!(rentObj instanceof Number)) rentObj = doc.get("rentAmount");
                String rentStr = (rentObj instanceof Number)
                        ? formatMoney(((Number) rentObj).doubleValue()) + " đ"
                        : "theo hợp đồng";

                systemNotifs.add(new NotificationItem(
                        NotificationItem.TYPE_SYSTEM,
                        NotificationItem.ICON_PAYMENT,
                        "Nhắc nhở đóng tiền thuê",
                        "Đã đến kỳ thanh toán tháng này. Tiền thuê: " + rentStr
                        + ". Vui lòng thanh toán trước ngày 05.",
                        "Tháng này"
                ));
            }

            // ── (C) Kiểm tra tiền cọc ───────────────────────────────────
            Object depositStatus = doc.get("depositCollectionStatus");
            boolean depositPaid = Boolean.TRUE.equals(depositStatus);
            // depositCollectionStatus == false → chưa đóng đủ
            if (!depositPaid) {
                Object depositAmtObj = doc.get("depositAmount");
                String depositStr = (depositAmtObj instanceof Number)
                        ? formatMoney(((Number) depositAmtObj).doubleValue()) + " đ"
                        : "theo hợp đồng";

                systemNotifs.add(new NotificationItem(
                        NotificationItem.TYPE_SYSTEM,
                        NotificationItem.ICON_DEPOSIT,
                        "Tiền cọc chưa hoàn thiện",
                        "Bạn chưa đóng đủ tiền cọc (" + depositStr + "). "
                        + "Vui lòng liên hệ chủ trọ để hoàn thiện.",
                        "Hôm nay"
                ));
            }
        }
    }

    // ================================================================
    //  Task 2: Load thông báo thông thường từ Firestore
    // ================================================================
    private void loadFirestoreNotifications() {
        if (tenantId == null || tenantId.isEmpty()) {
            onTaskComplete();
            return;
        }

        // Không dùng orderBy kết hợp whereEqualTo để tránh crash
        // do Firestore yêu cầu Composite Index chưa tạo.
        db.collection(COL_NOTIFICATIONS)
                .whereEqualTo("tenantId", tenantId)
                .limit(30)
                .get()
                .addOnSuccessListener(qs -> {
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        String title = null;
                        String body = null;
                        try {
                            title = doc.getString("title");
                            body = doc.getString("body");
                            if (body == null) body = doc.getString("message");
                        } catch (Exception e) {
                            // Bỏ qua nếu title/body lưu sai kiểu
                        }

                        Date createdAtDate = null;
                        try {
                            Object obj = doc.get("createdAt");
                            if (obj instanceof Timestamp) {
                                createdAtDate = ((Timestamp) obj).toDate();
                            } else if (obj instanceof Long) {
                                createdAtDate = new Date((Long) obj);
                            } else if (obj instanceof Date) {
                                createdAtDate = (Date) obj;
                            }
                        } catch (Exception e) {}
                        
                        String timeLabel = createdAtDate != null ? relativeTime(createdAtDate) : "";

                        regularNotifs.add(new NotificationItem(
                                NotificationItem.TYPE_REGULAR,
                                NotificationItem.ICON_INFO,
                                title != null ? title : "Thông báo",
                                body  != null ? body  : "",
                                timeLabel
                        ));
                    }
                    onTaskComplete();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Không tải được notifications: " + e.getMessage());
                    onTaskComplete(); // Không dừng — vẫn show system notifs
                });
    }

    // ================================================================
    //  Render danh sách hợp nhất
    // ================================================================
    private void renderList() {
        // Thông báo hệ thống ở trên cùng
        List<NotificationItem> merged = new ArrayList<>(systemNotifs);
        merged.addAll(regularNotifs);

        hideLoading();

        if (merged.isEmpty()) {
            showEmpty();
            return;
        }

        // Banner đếm số thông báo hệ thống
        if (!systemNotifs.isEmpty()) {
            bannerSystemCount.setVisibility(View.VISIBLE);
            tvSystemCountLabel.setText(
                    "Bạn có " + systemNotifs.size() + " thông báo cần chú ý");
        } else {
            bannerSystemCount.setVisibility(View.GONE);
        }

        rvNotifications.setVisibility(View.VISIBLE);
        rvNotifications.setAdapter(new NotificationAdapter(merged, this));
    }

    // ================================================================
    //  Helper: Parse Date từ DocumentSnapshot (nhiều tên field)
    // ================================================================
    private Date parseDateFromDoc(DocumentSnapshot doc, String... fieldNames) {
        for (String field : fieldNames) {
            Object obj = doc.get(field);
            if (obj == null) continue;

            if (obj instanceof Timestamp) {
                return ((Timestamp) obj).toDate();
            } else if (obj instanceof Date) {
                return (Date) obj;
            } else if (obj instanceof Long) {
                return new Date((Long) obj);
            } else if (obj instanceof String) {
                String str = (String) obj;
                if (!str.isEmpty()) {
                    String[] patterns = {"dd/MM/yyyy", "yyyy-MM-dd", "MM/dd/yyyy", "yyyy-MM-dd'T'HH:mm:ss"};
                    for (String pattern : patterns) {
                        try {
                            return new SimpleDateFormat(pattern, Locale.getDefault()).parse(str);
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
        return null;
    }

    /** Định dạng số tiền VND */
    private String formatMoney(double amount) {
        return MONEY_FORMAT.format((long) amount);
    }

    /** Nhãn thời gian tương đối */
    private String relativeTime(Date date) {
        long diffMs = new Date().getTime() - date.getTime();
        long days   = TimeUnit.MILLISECONDS.toDays(diffMs);
        if (days == 0) return "Hôm nay";
        if (days == 1) return "Hôm qua";
        if (days < 7)  return days + " ngày trước";
        if (days < 30) return (days / 7) + " tuần trước";
        return DF_DISPLAY.format(date);
    }

    // ================================================================
    //  UI States
    // ================================================================
    private void showLoading() {
        progressLoading.setVisibility(View.VISIBLE);
        rvNotifications.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
        bannerSystemCount.setVisibility(View.GONE);
    }

    private void hideLoading() {
        progressLoading.setVisibility(View.GONE);
    }

    private void showEmpty() {
        rvNotifications.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
    }
}
