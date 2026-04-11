package com.example.myapplication.features.report;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.session.TenantSession;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class TenantReportListActivity extends AppCompatActivity
        implements TenantReportAdapter.OnReportActionListener {

    public static final String EXTRA_ROOM_ID   = "ROOM_ID";
    public static final String EXTRA_TENANT_ID = "TENANT_ID";

    // ---- Tab status constants ----
    private static final String STATUS_PENDING    = "PENDING";    // Chưa làm
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS"; // Đang làm
    private static final String STATUS_DONE       = "DONE";       // Đã xong

    // ---- Views ----
    private RecyclerView   rvReports;
    private LinearLayout   layoutEmpty;
    private ProgressBar    progressLoading;
    private FloatingActionButton fabAddReport;

    // Tabs
    private FrameLayout   tabChuaLam, tabDangLam, tabDaXong;
    private LinearLayout  tabChuaLamContainer, tabDangLamContainer, tabDaXongContainer;
    private TextView      badgeChuaLam, badgeDangLam, badgeDaXong;

    // ---- Data ----
    private String roomId;
    private String tenantId;
    private String currentStatus = STATUS_PENDING;
    private TenantReportAdapter adapter;

    private FirebaseFirestore db;

    // Launcher nhận kết quả từ TenantCreateReportActivity
    private final ActivityResultLauncher<Intent> createReportLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Refresh danh sách và badge sau khi tạo thành công
                    fetchAllCounts();
                    loadReports(currentStatus);
                }
            });

    // Counts per status
    private int countPending    = 0;
    private int countInProgress = 0;
    private int countDone       = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_report_list);

        db = FirebaseFirestore.getInstance();

        roomId   = getIntent().getStringExtra(EXTRA_ROOM_ID);
        tenantId = getIntent().getStringExtra(EXTRA_TENANT_ID);

        // Fallback: lấy tenantId từ Session nếu Intent không truyền
        if (tenantId == null || tenantId.isEmpty()) {
            tenantId = TenantSession.getActiveTenantId();
        }

        Log.d("ReportList", "onCreate - tenantId = '" + tenantId + "', roomId = '" + roomId + "'");

        bindViews();
        setupTabClickListeners();
        setupFab();

        // Nút Back
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Tải dữ liệu và cập nhật badge
        fetchAllCounts();

        // Hiển thị tab mặc định: Chưa làm
        selectTab(STATUS_PENDING);
    }

    // ================================================================
    //  Ánh xạ View
    // ================================================================
    private void bindViews() {
        rvReports       = findViewById(R.id.rvReports);
        layoutEmpty     = findViewById(R.id.layoutEmpty);
        progressLoading = findViewById(R.id.progressLoading);
        fabAddReport    = findViewById(R.id.fabAddReport);

        tabChuaLam          = findViewById(R.id.tabChuaLam);
        tabDangLam          = findViewById(R.id.tabDangLam);
        tabDaXong           = findViewById(R.id.tabDaXong);
        tabChuaLamContainer = findViewById(R.id.tabChuaLamContainer);
        tabDangLamContainer = findViewById(R.id.tabDangLamContainer);
        tabDaXongContainer  = findViewById(R.id.tabDaXongContainer);
        badgeChuaLam        = findViewById(R.id.badgeChuaLam);
        badgeDangLam        = findViewById(R.id.badgeDangLam);
        badgeDaXong         = findViewById(R.id.badgeDaXong);

        rvReports.setLayoutManager(new LinearLayoutManager(this));
    }

    // ================================================================
    //  Setup click tab
    // ================================================================
    private void setupTabClickListeners() {
        tabChuaLam.setOnClickListener(v -> selectTab(STATUS_PENDING));
        tabDangLam.setOnClickListener(v -> selectTab(STATUS_IN_PROGRESS));
        tabDaXong.setOnClickListener(v -> selectTab(STATUS_DONE));
    }

    // ================================================================
    //  Chọn tab và cập nhật UI
    // ================================================================
    private void selectTab(String status) {
        currentStatus = status;

        // Reset all tabs về unselected (nền trong suốt, chữ trắng)
        tabChuaLamContainer.setBackgroundResource(android.R.color.transparent);
        tabDangLamContainer.setBackgroundResource(android.R.color.transparent);
        tabDaXongContainer.setBackgroundResource(android.R.color.transparent);

        setTabTextColor(tabChuaLamContainer, false);
        setTabTextColor(tabDangLamContainer, false);
        setTabTextColor(tabDaXongContainer, false);

        // Active tab: nền trắng chữ đen
        switch (status) {
            case STATUS_PENDING:
                tabChuaLamContainer.setBackgroundResource(R.color.white);
                setTabTextColor(tabChuaLamContainer, true);
                break;
            case STATUS_IN_PROGRESS:
                tabDangLamContainer.setBackgroundResource(R.color.white);
                setTabTextColor(tabDangLamContainer, true);
                break;
            case STATUS_DONE:
                tabDaXongContainer.setBackgroundResource(R.color.white);
                setTabTextColor(tabDaXongContainer, true);
                break;
        }

        // Load danh sách theo tab được chọn
        loadReports(status);
    }

    private void setTabTextColor(LinearLayout container, boolean isActive) {
        int color = isActive
                ? getResources().getColor(android.R.color.black)
                : getResources().getColor(android.R.color.white);
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof TextView) {
                ((TextView) child).setTextColor(color);
            }
        }
    }

    // ================================================================
    //  FAB → mở màn hình tạo báo cáo mới
    // ================================================================
    private void setupFab() {
        fabAddReport.setOnClickListener(v -> {
            Intent intent = new Intent(this, TenantCreateReportActivity.class);
            intent.putExtra(EXTRA_ROOM_ID,   roomId);
            intent.putExtra(EXTRA_TENANT_ID, tenantId);
            createReportLauncher.launch(intent); // Dùng launcher để nhận kết quả
        });
    }

    // ================================================================
    //  Load danh sách báo cáo theo status (Firestore)
    // ================================================================
    private void loadReports(String status) {
        if (tenantId == null || tenantId.isEmpty()) {
            Log.w("ReportList", "loadReports() bỏ qua: tenantId rỗng!");
            showEmpty();
            return;
        }

        Log.d("ReportList", "loadReports() - tenantId=[" + tenantId + "] status=[" + status + "]");
        showLoading();

        // Bỏ orderBy để tránh yêu cầu Composite Index khi chưa tạo trên Firestore Console
        // Sau khi tạo Index, có thể thêm lại: .orderBy("createdAt", Query.Direction.DESCENDING)
        db.collection("issues")
                .whereEqualTo("tenantId", tenantId)
                .whereEqualTo("status", status)
                .get()
                .addOnSuccessListener(qs -> {
                    hideLoading();
                    Log.d("ReportList", "Query thành công. Số tài liệu = " + qs.size());
                    List<DocumentSnapshot> docs = qs.getDocuments();
                    if (docs.isEmpty()) {
                        showEmpty();
                    } else {
                        showList(docs);
                    }
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    Log.e("ReportList", "Query thất bại: " + e.getMessage(), e);
                    showEmpty();
                });
    }

    // ================================================================
    //  Fetch số lượng từng tab để hiển thị badge
    // ================================================================
    private void fetchAllCounts() {
        if (tenantId == null || tenantId.isEmpty()) return;

        // Dùng final local variable để lambda capture an toàn
        final String[] statuses = {STATUS_PENDING, STATUS_IN_PROGRESS, STATUS_DONE};
        for (final String s : statuses) {
            db.collection("issues")
                    .whereEqualTo("tenantId", tenantId)
                    .whereEqualTo("status", s)
                    .get()
                    .addOnSuccessListener(qs -> {
                        int count = qs.size();
                        if (STATUS_PENDING.equals(s)) {
                            countPending = count;
                            badgeChuaLam.setText(String.valueOf(count));
                        } else if (STATUS_IN_PROGRESS.equals(s)) {
                            countInProgress = count;
                            badgeDangLam.setText(String.valueOf(count));
                        } else if (STATUS_DONE.equals(s)) {
                            countDone = count;
                            badgeDaXong.setText(String.valueOf(count));
                        }
                    })
                    .addOnFailureListener(e ->
                            Log.e("ReportList", "fetchAllCounts lỗi [" + s + "]: " + e.getMessage()));
        }
    }

    // ================================================================
    //  Helper: Show/Hide states
    // ================================================================
    private void showLoading() {
        progressLoading.setVisibility(View.VISIBLE);
        rvReports.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
    }

    private void hideLoading() {
        progressLoading.setVisibility(View.GONE);
    }

    private void showEmpty() {
        rvReports.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
    }

    private void showList(List<DocumentSnapshot> docs) {
        layoutEmpty.setVisibility(View.GONE);
        rvReports.setVisibility(View.VISIBLE);
        // Truyền this (OnReportActionListener) vào Adapter
        adapter = new TenantReportAdapter(docs, this, this);
        rvReports.setAdapter(adapter);
    }

    // ================================================================
    //  Callback từ TenantReportAdapter.OnReportActionListener
    // ================================================================
    @Override
    public void onReportCancelled() {
        Log.d("ReportList", "onReportCancelled() → reload danh sách và badge");
        fetchAllCounts();
        loadReports(currentStatus);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh dữ liệu khi quay lại từ màn hình tạo báo cáo
        fetchAllCounts();
        loadReports(currentStatus);
    }
}
