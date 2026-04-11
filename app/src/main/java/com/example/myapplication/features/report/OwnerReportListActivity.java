package com.example.myapplication.features.report;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class OwnerReportListActivity extends AppCompatActivity
        implements OwnerReportAdapter.OnOwnerReportActionListener {

    // ── Status constants ────────────────────────────────────────────
    // Tab "Chưa làm" hiển thị cả PENDING và CONFIRMED
    private static final String STATUS_PENDING     = "PENDING";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_DONE        = "DONE";

    // ── Views ───────────────────────────────────────────────────────
    private RecyclerView rvReports;
    private LinearLayout layoutEmpty;
    private ProgressBar  progressLoading;

    // Tabs
    private FrameLayout  tabChuaLam, tabDangLam, tabDaXong;
    private LinearLayout tabChuaLamContainer, tabDangLamContainer, tabDaXongContainer;
    private TextView     badgeChuaLam, badgeDangLam, badgeDaXong;

    // ── Data ────────────────────────────────────────────────────────
    private String              ownerId;
    private String              currentStatus = STATUS_PENDING;
    private OwnerReportAdapter  adapter;

    private FirebaseFirestore db;
    private FirebaseAuth      mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_report_list);

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Lấy ownerId từ tài khoản đang đăng nhập
        FirebaseUser user = mAuth.getCurrentUser();
        ownerId = (user != null) ? user.getUid() : "";
        Log.d("OwnerReportList", "ownerId = " + ownerId);

        bindViews();
        setupTabs();
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        fetchAllCounts();
        selectTab(STATUS_PENDING);
    }

    // ================================================================
    //  Ánh xạ View
    // ================================================================
    private void bindViews() {
        rvReports       = findViewById(R.id.rvReports);
        layoutEmpty     = findViewById(R.id.layoutEmpty);
        progressLoading = findViewById(R.id.progressLoading);

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
    private void setupTabs() {
        tabChuaLam.setOnClickListener(v -> selectTab(STATUS_PENDING));
        tabDangLam.setOnClickListener(v -> selectTab(STATUS_IN_PROGRESS));
        tabDaXong.setOnClickListener(v  -> selectTab(STATUS_DONE));
    }

    // ================================================================
    //  Chọn tab → cập nhật UI + load dữ liệu
    // ================================================================
    private void selectTab(String status) {
        currentStatus = status;

        // Reset về unselected
        tabChuaLamContainer.setBackgroundResource(android.R.color.transparent);
        tabDangLamContainer.setBackgroundResource(android.R.color.transparent);
        tabDaXongContainer.setBackgroundResource(android.R.color.transparent);
        setTabTextColor(tabChuaLamContainer, false);
        setTabTextColor(tabDangLamContainer, false);
        setTabTextColor(tabDaXongContainer, false);

        // Active tab
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
    //  Load danh sách từ Firestore theo ownerId + status
    //  Tab "Chưa làm" hiển thị cả PENDING và CONFIRMED
    // ================================================================
    private void loadReports(String status) {
        if (ownerId.isEmpty()) {
            showEmpty();
            return;
        }
        showLoading();

        // Tab "Chưa làm": lấy cả PENDING và CONFIRMED
        if (STATUS_PENDING.equals(status)) {
            // Không thể dùng whereIn + orderBy mà không có Composite Index
            // → Lấy PENDING và CONFIRMED riêng rồi gộp
            loadPendingAndConfirmed();
        } else {
            db.collection("issues")
                    .whereEqualTo("ownerId", ownerId)
                    .whereEqualTo("status", status)
                    .get()
                    .addOnSuccessListener(qs -> {
                        hideLoading();
                        Log.d("OwnerReportList", "Kết quả [" + status + "]: " + qs.size());
                        List<DocumentSnapshot> docs = qs.getDocuments();
                        if (docs.isEmpty()) showEmpty(); else showList(docs);
                    })
                    .addOnFailureListener(e -> {
                        hideLoading();
                        Log.e("OwnerReportList", "Lỗi query: " + e.getMessage(), e);
                        showEmpty();
                    });
        }
    }

    private void loadPendingAndConfirmed() {
        // Bước 1: lấy PENDING
        db.collection("issues")
                .whereEqualTo("ownerId", ownerId)
                .whereEqualTo("status", STATUS_PENDING)
                .get()
                .addOnSuccessListener(pending -> {
                    // Bước 2: lấy CONFIRMED
                    db.collection("issues")
                            .whereEqualTo("ownerId", ownerId)
                            .whereEqualTo("status", "CONFIRMED")
                            .get()
                            .addOnSuccessListener(confirmed -> {
                                hideLoading();
                                List<DocumentSnapshot> combined = pending.getDocuments();
                                combined.addAll(confirmed.getDocuments());
                                Log.d("OwnerReportList", "PendingAndConfirmed: " + combined.size());
                                if (combined.isEmpty()) showEmpty(); else showList(combined);
                            })
                            .addOnFailureListener(e -> { hideLoading(); showEmpty(); });
                })
                .addOnFailureListener(e -> { hideLoading(); showEmpty(); });
    }

    // ================================================================
    //  Badge counts
    // ================================================================
    private void fetchAllCounts() {
        if (ownerId.isEmpty()) return;

        // PENDING tab count (PENDING + CONFIRMED)
        db.collection("issues").whereEqualTo("ownerId", ownerId)
                .whereEqualTo("status", STATUS_PENDING).get()
                .addOnSuccessListener(p -> {
                    db.collection("issues").whereEqualTo("ownerId", ownerId)
                            .whereEqualTo("status", "CONFIRMED").get()
                            .addOnSuccessListener(c ->
                                    badgeChuaLam.setText(String.valueOf(p.size() + c.size())));
                });

        // IN_PROGRESS tab count
        db.collection("issues").whereEqualTo("ownerId", ownerId)
                .whereEqualTo("status", STATUS_IN_PROGRESS).get()
                .addOnSuccessListener(qs -> badgeDangLam.setText(String.valueOf(qs.size())))
                .addOnFailureListener(e -> Log.e("OwnerReportList", "Badge lỗi: " + e.getMessage()));

        // DONE tab count
        db.collection("issues").whereEqualTo("ownerId", ownerId)
                .whereEqualTo("status", STATUS_DONE).get()
                .addOnSuccessListener(qs -> badgeDaXong.setText(String.valueOf(qs.size())))
                .addOnFailureListener(e -> Log.e("OwnerReportList", "Badge lỗi: " + e.getMessage()));
    }

    // ================================================================
    //  Callback từ OwnerReportAdapter.OnOwnerReportActionListener
    // ================================================================
    @Override
    public void onStatusChanged() {
        Log.d("OwnerReportList", "onStatusChanged() → reload");
        fetchAllCounts();
        loadReports(currentStatus);
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
        adapter = new OwnerReportAdapter(docs, this, this);
        rvReports.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchAllCounts();
        loadReports(currentStatus);
    }
}
