package com.example.myapplication.features.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.AuthProviderUtil;
import com.example.myapplication.features.contract.TenantContractDetailsActivity;
import com.example.myapplication.features.report.TenantReportListActivity;
import com.example.myapplication.features.auth.MainActivity;
import com.example.myapplication.features.settings.ChangePasswordActivity;
import com.example.myapplication.features.settings.EditProfileActivity;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TenantMenuActivity extends AppCompatActivity {

    // ===== Header =====
    private TextView tvTenantName;
    private TextView tvRoomInfo;
    private ImageView imgAvatar;

    // ===== Grid menu =====
    private CardView cardMyRoom;
    private CardView cardBill;
    private CardView cardReport;
    private CardView cardNotification;

    // ===== Block 1: Tóm tắt hợp đồng =====
    private ProgressBar contractProgress;
    private TextView tvDaysRemaining;
    private TextView tvMonthsStayed;
    private TextView tvContractStatus;
    private TextView tvStartDate;
    private TextView tvEndDate;
    private TextView btnViewContractDetail;

    // ===== Block 2: Danh mục tính tiền =====
    private TextView tvBillingSubtitle;
    private TextView tvPaymentDayLabel;
    private TextView tvPaymentDaysWarning;
    private TextView tvRentAmount;
    private TextView tvDepositAmount;
    private TextView tvWaterRate;
    private TextView tvElecRate;
    private TextView tvGarbageFee;
    private TextView tvWifiFee;
    private TextView tvWaterStatus;
    private TextView tvWaterLastIndex;
    private TextView tvElecStatus;
    private TextView tvElecLastIndex;
    private Button   btnMeterHistory;
    private Button   btnMeterList;
    private Button   btnConfirmMeter;

    // ===== DrawerLayout =====
    private DrawerLayout tenantDrawerLayout;

    // IDs từ home_menu_profile_drawer.xml
    private ShapeableImageView drawerAvatar;
    private TextView drawerUserName;
    private TextView drawerUserEmail;
    private LinearLayout menuEditProfile;
    private LinearLayout menuChangePassword;
    private LinearLayout menuRentalHistory;
    private LinearLayout menuLogout;

    // ===== Data =====
    private String tenantId;
    private String roomId;

    private FirebaseAuth      mAuth;
    private FirebaseFirestore db;

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    // ================================================================
    //  onCreate
    // ================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_menu);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        // Nhận dữ liệu từ Intent, fallback sang TenantSession nếu Intent thiếu
        tenantId = getIntent().getStringExtra("TENANT_ID");
        if (tenantId == null || tenantId.trim().isEmpty()) {
            tenantId = TenantSession.getActiveTenantId();
        }
        roomId = getIntent().getStringExtra("ROOM_ID");

        bindViews();
        setupClickListeners();
        loadUserInfo();

        if (roomId != null && !roomId.isEmpty()) {
            fetchRoomNumber(roomId);
            loadAllData();
        } else {
            tvRoomInfo.setText("Phòng: --");
        }
    }

    // ================================================================
    //  Ánh xạ view
    // ================================================================
    private void bindViews() {
        // Header
        tvTenantName = findViewById(R.id.tvTenantName);
        tvRoomInfo   = findViewById(R.id.tvRoomInfo);
        imgAvatar    = findViewById(R.id.imgAvatarHeader);

        // Grid menu
        cardMyRoom       = findViewById(R.id.cardMyRoom);
        cardBill         = findViewById(R.id.cardBill);
        cardReport       = findViewById(R.id.cardReport);
        cardNotification = findViewById(R.id.cardNotification);

        // Block 1: Hợp đồng
        contractProgress      = findViewById(R.id.contractProgress);
        tvDaysRemaining       = findViewById(R.id.tvDaysRemaining);
        tvMonthsStayed        = findViewById(R.id.tvMonthsStayed);
        tvContractStatus      = findViewById(R.id.tvContractStatus);
        tvStartDate           = findViewById(R.id.tvStartDate);
        tvEndDate             = findViewById(R.id.tvEndDate);
        btnViewContractDetail = findViewById(R.id.btnViewContractDetail);

        // Block 2: Tính tiền
        tvBillingSubtitle    = findViewById(R.id.tvBillingSubtitle);
        tvPaymentDayLabel    = findViewById(R.id.tvPaymentDayLabel);
        tvPaymentDaysWarning = findViewById(R.id.tvPaymentDaysWarning);
        tvRentAmount         = findViewById(R.id.tvRentAmount);
        tvDepositAmount      = findViewById(R.id.tvDepositAmount);
        tvWaterRate          = findViewById(R.id.tvWaterRate);
        tvElecRate           = findViewById(R.id.tvElecRate);
        tvGarbageFee         = findViewById(R.id.tvGarbageFee);
        tvWifiFee            = findViewById(R.id.tvWifiFee);
        tvWaterStatus        = findViewById(R.id.tvWaterStatus);
        tvWaterLastIndex     = findViewById(R.id.tvWaterLastIndex);
        tvElecStatus         = findViewById(R.id.tvElecStatus);
        tvElecLastIndex      = findViewById(R.id.tvElecLastIndex);
        btnMeterHistory      = findViewById(R.id.btnMeterHistory);
        btnMeterList         = findViewById(R.id.btnMeterList);
        btnConfirmMeter      = findViewById(R.id.btnConfirmMeter);

        // DrawerLayout và các view bên trong drawer
        tenantDrawerLayout = findViewById(R.id.tenantDrawerLayout);
        drawerAvatar       = findViewById(R.id.drawerAvatar);
        drawerUserName     = findViewById(R.id.drawerUserName);
        drawerUserEmail    = findViewById(R.id.drawerUserEmail);
        menuEditProfile    = findViewById(R.id.menuEditProfile);
        menuChangePassword = findViewById(R.id.menuChangePassword);
        menuRentalHistory  = findViewById(R.id.menuRentalHistory);
        menuLogout         = findViewById(R.id.menuLogout);

        // Ẩn mục Rental History (không dùng cho Tenant)
        if (menuRentalHistory != null) {
            menuRentalHistory.setVisibility(View.GONE);
        }
    }

    // ================================================================
    //  Setup click listeners
    // ================================================================
    private void setupClickListeners() {
        // Avatar → mở Drawer
        if (imgAvatar != null) {
            imgAvatar.setOnClickListener(v -> openProfileDrawer());
        }

        // Drawer items
        setupDrawerListeners();

        // Grid menu cards
        cardMyRoom.setOnClickListener(v -> openRoomDetail());
        cardBill.setOnClickListener(v -> openRoomDetail());
        cardReport.setOnClickListener(v -> {
                Intent reportIntent = new Intent(this, TenantReportListActivity.class);
                reportIntent.putExtra(TenantReportListActivity.EXTRA_ROOM_ID, roomId);
                reportIntent.putExtra(TenantReportListActivity.EXTRA_TENANT_ID, tenantId);
                startActivity(reportIntent);
        });
        cardNotification.setOnClickListener(v ->
                Toast.makeText(this, "Thông báo", Toast.LENGTH_SHORT).show());

        // Nút Chốt đồng hồ → mở ConfirmMeterActivity
        if (btnConfirmMeter != null) {
            btnConfirmMeter.setOnClickListener(v -> {
                if (roomId == null || roomId.isEmpty()) {
                    Toast.makeText(this, "Chưa xác định được phòng", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(this, ConfirmMeterActivity.class);
                intent.putExtra(TenantRoomDetailActivity.EXTRA_ROOM_ID,   roomId);
                intent.putExtra(TenantRoomDetailActivity.EXTRA_TENANT_ID, tenantId);
                startActivity(intent);
            });
        }

        // Nút Lịch sử đồng hồ
        if (btnMeterHistory != null) {
            btnMeterHistory.setOnClickListener(v ->
                    Toast.makeText(this, "Lịch sử đồng hồ", Toast.LENGTH_SHORT).show());
        }

        // Nút Danh sách chốt
        if (btnMeterList != null) {
            btnMeterList.setOnClickListener(v ->
                    Toast.makeText(this, "Danh sách chốt", Toast.LENGTH_SHORT).show());
        }

        // Xem chi tiết hợp đồng
        if (btnViewContractDetail != null) {
            btnViewContractDetail.setOnClickListener(v -> openContractDetails());
        }
    }

    // ================================================================
    //  Load tất cả dữ liệu (hợp đồng + đơn giá + số đồng hồ)
    // ================================================================
    private void loadAllData() {
        if (roomId == null || roomId.isEmpty()) return;
        getContractSummary(roomId);
        getLatestServiceRates(roomId);
        fetchLatestMeterReading(roomId);
    }

    // ================================================================
    //  Hàm 1: getContractSummary
    // ================================================================
    private void getContractSummary(String roomId) {
        if (tenantId == null) return;

        db.collection("tenants").document(tenantId)
                .collection("contracts")
                .whereEqualTo("roomId", roomId)
                .limit(5)
                .get()
                .addOnSuccessListener(qs -> {
                    DocumentSnapshot activeContract = findActiveContract(qs);
                    if (activeContract != null) {
                        applyContractToUI(activeContract);
                    } else {
                        // Fallback: users/{tenantId}/contracts (legacy)
                        db.collection("users").document(tenantId)
                                .collection("contracts")
                                .whereEqualTo("roomId", roomId)
                                .limit(5)
                                .get()
                                .addOnSuccessListener(qs2 -> {
                                    DocumentSnapshot c2 = findActiveContract(qs2);
                                    if (c2 != null) applyContractToUI(c2);
                                    else showNoContractUI();
                                })
                                .addOnFailureListener(e -> showNoContractUI());
                    }
                })
                .addOnFailureListener(e -> showNoContractUI());
    }

    private DocumentSnapshot findActiveContract(QuerySnapshot qs) {
        if (qs == null || qs.isEmpty()) return null;
        for (DocumentSnapshot doc : qs.getDocuments()) {
            String status = doc.getString("contractStatus");
            if ("ACTIVE".equalsIgnoreCase(status)) return doc;
        }
        return qs.getDocuments().get(0);
    }

    private void applyContractToUI(DocumentSnapshot doc) {
        Date startDate = parseDate(doc, "rentalStartDate", "startDate");
        Date endDate   = parseDate(doc, "contractEndDate", "endDate");

        if (startDate == null || endDate == null) {
            showNoContractUI();
            return;
        }

        Date today       = new Date();
        long totalMs     = endDate.getTime() - startDate.getTime();
        long remainingMs = endDate.getTime() - today.getTime();
        long totalDays   = TimeUnit.MILLISECONDS.toDays(totalMs);
        long daysLeft    = TimeUnit.MILLISECONDS.toDays(remainingMs);
        long daysStayed  = TimeUnit.MILLISECONDS.toDays(today.getTime() - startDate.getTime());

        long monthsStayed = daysStayed / 30;
        long extraDays    = daysStayed % 30;

        int progress = (totalDays > 0)
                ? (int) Math.max(0, Math.min(100, (remainingMs * 100L) / totalMs))
                : 0;
        contractProgress.setProgress(progress);

        tvDaysRemaining.setText(String.valueOf(Math.max(0, daysLeft)));
        tvMonthsStayed.setText(monthsStayed + " tháng, " + extraDays + " ngày");
        tvStartDate.setText(DATE_FORMAT.format(startDate));
        tvEndDate.setText(DATE_FORMAT.format(endDate));

        if (daysLeft > 0) {
            tvContractStatus.setText("Trong thời hạn hợp đồng");
        } else {
            tvContractStatus.setText("Hợp đồng đã hết hạn");
        }

        // Tiền cọc
        Object deposit = doc.get("depositAmount");
        if (deposit instanceof Number) {
            tvDepositAmount.setText(formatMoney(((Number) deposit).doubleValue()));
        }

        // Tiền thuê
        Object rentPrice = doc.get("roomPrice");
        if (rentPrice instanceof Number) {
            tvRentAmount.setText(formatMoney(((Number) rentPrice).doubleValue()));
        }
    }

    private void showNoContractUI() {
        if (tvDaysRemaining != null)  tvDaysRemaining.setText("--");
        if (tvMonthsStayed != null)   tvMonthsStayed.setText("Chưa có hợp đồng");
        if (tvContractStatus != null) tvContractStatus.setText("Không tìm thấy hợp đồng");
        if (tvStartDate != null)      tvStartDate.setText("--/--/----");
        if (tvEndDate != null)        tvEndDate.setText("--/--/----");
        if (contractProgress != null) contractProgress.setProgress(0);
    }

    // ================================================================
    //  Hàm 2: getLatestServiceRates
    // ================================================================
    private void getLatestServiceRates(String roomId) {
        if (tenantId == null) return;

        db.collection("users").document(tenantId)
                .collection("rooms").document(roomId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        applyServiceRatesToUI(doc);
                    } else {
                        db.collection("tenants").document(tenantId)
                                .collection("rooms").document(roomId)
                                .get()
                                .addOnSuccessListener(this::applyServiceRatesToUI)
                                .addOnFailureListener(e -> { /* no rates */ });
                    }
                })
                .addOnFailureListener(e -> { /* no rates */ });
    }

    private void applyServiceRatesToUI(DocumentSnapshot doc) {
        if (!doc.exists()) return;

        // Tiền thuê (từ room doc, chỉ ghi nếu chưa có từ contract)
        Object rent = doc.get("rentAmount");
        if (rent instanceof Number && tvRentAmount.getText().toString().contains("--")) {
            tvRentAmount.setText(formatMoney(((Number) rent).doubleValue()));
        }

        // Tên nhà
        String houseName = doc.getString("houseName");
        if (houseName != null && !houseName.isEmpty() && tvBillingSubtitle != null) {
            tvBillingSubtitle.setText("Dịch vụ & giá tại " + houseName);
        }

        // Tiền nước
        Object waterRate = doc.get("waterRate");
        if (waterRate instanceof Number) {
            tvWaterRate.setText(formatMoneyShort(((Number) waterRate).doubleValue()) + " đ / Khối");
        }

        // Tiền điện
        Object elecRate = doc.get("electricityRate");
        if (!(elecRate instanceof Number)) elecRate = doc.get("electricRate");
        if (elecRate instanceof Number) {
            tvElecRate.setText(formatMoneyShort(((Number) elecRate).doubleValue()) + " đ / KWh");
        }

        // Tiền rác
        Object garbage = doc.get("garbageFee");
        if (garbage instanceof Number) {
            tvGarbageFee.setText(formatMoneyShort(((Number) garbage).doubleValue()) + " đ / Người");
        }

        // Tiền wifi
        Object wifi = doc.get("wifiFee");
        if (!(wifi instanceof Number)) wifi = doc.get("internetFee");
        if (wifi instanceof Number) {
            tvWifiFee.setText(formatMoneyShort(((Number) wifi).doubleValue()) + " đ / Người");
        }

        // Ngày chốt tiền thuê
        Object payDay = doc.get("paymentDay");
        if (payDay instanceof Number) {
            int day = ((Number) payDay).intValue();
            tvPaymentDayLabel.setText("Ngày " + day + " chốt tiền thuê");
            updatePaymentDaysWarning(day);
        }
    }

    private void updatePaymentDaysWarning(int paymentDay) {
        Calendar cal   = Calendar.getInstance();
        int today      = cal.get(Calendar.DAY_OF_MONTH);
        int daysUntil  = paymentDay - today;
        if (daysUntil < 0) {
            int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            daysUntil = daysInMonth - today + paymentDay;
        }
        if (daysUntil <= 5) {
            tvPaymentDaysWarning.setText(daysUntil + " ngày là hạn chốt thanh toán phí");
        } else {
            tvPaymentDaysWarning.setText("Còn " + daysUntil + " ngày đến kỳ thanh toán");
        }
    }

    // ================================================================
    //  Hàm 3: fetchLatestMeterReading
    // ================================================================
    private void fetchLatestMeterReading(String roomId) {
        if (tenantId == null) return;

        Query q = db.collection("tenants").document(tenantId)
                .collection("meterReadings")
                .whereEqualTo("roomId", roomId)
                .orderBy("periodKey", Query.Direction.DESCENDING)
                .limit(1);

        q.get()
                .addOnSuccessListener(qs -> {
                    if (!qs.isEmpty()) {
                        applyMeterReadingToUI(qs.getDocuments().get(0));
                    } else {
                        // Fallback: users path
                        db.collection("users").document(tenantId)
                                .collection("meterReadings")
                                .whereEqualTo("roomId", roomId)
                                .orderBy("periodKey", Query.Direction.DESCENDING)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(qs2 -> {
                                    if (!qs2.isEmpty())
                                        applyMeterReadingToUI(qs2.getDocuments().get(0));
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    // Không có index → thử query không sort
                    db.collection("tenants").document(tenantId)
                            .collection("meterReadings")
                            .whereEqualTo("roomId", roomId)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(qs -> {
                                if (!qs.isEmpty()) applyMeterReadingToUI(qs.getDocuments().get(0));
                            });
                });
    }

    private void applyMeterReadingToUI(DocumentSnapshot doc) {
        Object elecEnd  = doc.get("elecEnd");
        Object waterEnd = doc.get("waterEnd");
        String period   = doc.getString("period");
        String suffix   = (period != null) ? " (" + period + ")" : "";

        if (elecEnd instanceof Number && ((Number) elecEnd).doubleValue() > 0) {
            tvElecStatus.setText("Đã chốt");
            tvElecLastIndex.setText("Số cuối: " + (int)((Number) elecEnd).doubleValue() + suffix);
        }
        if (waterEnd instanceof Number && ((Number) waterEnd).doubleValue() > 0) {
            tvWaterStatus.setText("Đã chốt");
            tvWaterLastIndex.setText("Số cuối: " + (int)((Number) waterEnd).doubleValue() + suffix);
        }
    }

    // ================================================================
    //  Mở màn hình chi tiết phòng (vẫn giữ để dùng nếu cần)
    // ================================================================
    private void openRoomDetail() {
        openContractDetails();
    }

    private void openContractDetails() {
        if (roomId == null || roomId.isEmpty()) {
            Toast.makeText(this, "Chưa xác định được phòng", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, TenantContractDetailsActivity.class);
        intent.putExtra(TenantContractDetailsActivity.EXTRA_ROOM_ID,   roomId);
        intent.putExtra(TenantContractDetailsActivity.EXTRA_TENANT_ID, tenantId);
        startActivity(intent);
    }

    // ================================================================
    //  Drawer
    // ================================================================
    private void openProfileDrawer() {
        populateDrawerWithUserInfo();
        tenantDrawerLayout.openDrawer(GravityCompat.END);
    }

    private void populateDrawerWithUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String email       = user.getEmail();
        String displayName = user.getDisplayName();

        if (drawerUserEmail != null) {
            drawerUserEmail.setText(email != null ? email : "");
        }
        if (drawerUserName != null) {
            if (displayName != null && !displayName.isEmpty()) {
                drawerUserName.setText(displayName);
            } else if (email != null) {
                drawerUserName.setText(email.split("@")[0]);
            }
        }
        if (user.getPhotoUrl() != null && drawerAvatar != null) {
            Glide.with(this).load(user.getPhotoUrl()).circleCrop().into(drawerAvatar);
        }
        if (menuChangePassword != null) {
            boolean canChange = AuthProviderUtil.canChangePassword(user);
            menuChangePassword.setVisibility(canChange ? View.VISIBLE : View.GONE);
        }

        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String fullName  = doc.getString("fullName");
                        String avatarUrl = doc.getString("avatarUrl");
                        if (fullName != null && !fullName.isEmpty() && drawerUserName != null) {
                            drawerUserName.setText(fullName);
                        }
                        if (avatarUrl != null && !avatarUrl.isEmpty() && drawerAvatar != null) {
                            Glide.with(this).load(avatarUrl).circleCrop().into(drawerAvatar);
                        }
                    }
                });
    }

    private void setupDrawerListeners() {
        if (menuEditProfile != null) {
            menuEditProfile.setOnClickListener(v -> {
                tenantDrawerLayout.closeDrawer(GravityCompat.END);
                startActivity(new Intent(this, EditProfileActivity.class));
            });
        }
        if (menuChangePassword != null) {
            menuChangePassword.setOnClickListener(v -> {
                tenantDrawerLayout.closeDrawer(GravityCompat.END);
                startActivity(new Intent(this, ChangePasswordActivity.class));
            });
        }
        if (menuLogout != null) {
            menuLogout.setOnClickListener(v -> {
                tenantDrawerLayout.closeDrawer(GravityCompat.END);
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.logout))
                        .setMessage(getString(R.string.logout_confirm_message))
                        .setPositiveButton(getString(R.string.logout), (d, which) -> {
                            TenantSession.clear(this);
                            mAuth.signOut();
                            Intent intent = new Intent(this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        })
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show();
            });
        }
    }

    // ================================================================
    //  Load tên user lên Header
    // ================================================================
    private void loadUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String displayName = user.getDisplayName();
        String email       = user.getEmail();

        if (displayName != null && !displayName.isEmpty()) {
            tvTenantName.setText(displayName);
        } else if (email != null) {
            tvTenantName.setText(email.split("@")[0]);
        }

        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String fullName = doc.getString("fullName");
                        if (fullName != null && !fullName.isEmpty()) {
                            tvTenantName.setText(fullName);
                        }
                    }
                });
    }

    // ================================================================
    //  Query tên phòng từ Firestore
    // ================================================================
    private void fetchRoomNumber(String roomId) {
        tvRoomInfo.setText("Phòng: ...");

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            tvRoomInfo.setText("Phòng: " + roomId);
            return;
        }

        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(userDoc -> {
                    String freshTenantId = userDoc.getString("activeTenantId");

                    if (freshTenantId == null || freshTenantId.trim().isEmpty()) {
                        tvRoomInfo.setText("Phòng: " + roomId);
                        return;
                    }

                    tenantId = freshTenantId;

                    db.collection("tenants").document(freshTenantId)
                            .collection("rooms").document(roomId)
                            .get()
                            .addOnSuccessListener(roomDoc -> {
                                if (roomDoc.exists()) {
                                    String rn = roomDoc.getString("roomNumber");
                                    tvRoomInfo.setText("Phòng: " + (rn != null && !rn.isEmpty() ? rn : roomId));
                                } else {
                                    db.collection("users").document(freshTenantId)
                                            .collection("rooms").document(roomId)
                                            .get()
                                            .addOnSuccessListener(userRoomDoc -> {
                                                if (userRoomDoc.exists()) {
                                                    String rn = userRoomDoc.getString("roomNumber");
                                                    tvRoomInfo.setText("Phòng: " + (rn != null && !rn.isEmpty() ? rn : roomId));
                                                } else {
                                                    scanAllRooms(freshTenantId, roomId);
                                                }
                                            })
                                            .addOnFailureListener(e -> scanAllRooms(freshTenantId, roomId));
                                }
                            })
                            .addOnFailureListener(e -> scanAllRooms(freshTenantId, roomId));
                })
                .addOnFailureListener(e -> tvRoomInfo.setText("Phòng: " + roomId));
    }

    private void scanAllRooms(String freshTenantId, String roomId) {
        db.collection("tenants").document(freshTenantId)
                .collection("rooms").get()
                .addOnSuccessListener(qs -> {
                    String found = findRoomNumber(qs, roomId);
                    if (found != null) {
                        tvRoomInfo.setText("Phòng: " + found);
                    } else {
                        db.collection("users").document(freshTenantId)
                                .collection("rooms").get()
                                .addOnSuccessListener(qs2 -> {
                                    String found2 = findRoomNumber(qs2, roomId);
                                    tvRoomInfo.setText("Phòng: " + (found2 != null ? found2 : roomId));
                                })
                                .addOnFailureListener(e -> tvRoomInfo.setText("Phòng: " + roomId));
                    }
                })
                .addOnFailureListener(e ->
                        db.collection("users").document(freshTenantId)
                                .collection("rooms").get()
                                .addOnSuccessListener(qs2 -> {
                                    String found2 = findRoomNumber(qs2, roomId);
                                    tvRoomInfo.setText("Phòng: " + (found2 != null ? found2 : roomId));
                                })
                                .addOnFailureListener(e2 -> tvRoomInfo.setText("Phòng: " + roomId)));
    }

    private String findRoomNumber(com.google.firebase.firestore.QuerySnapshot qs, String roomId) {
        for (com.google.firebase.firestore.DocumentSnapshot doc : qs.getDocuments()) {
            if (roomId.equals(doc.getId()) || roomId.equals(doc.getString("roomId"))) {
                String rn = doc.getString("roomNumber");
                return rn != null && !rn.isEmpty() ? rn : doc.getId();
            }
        }
        return null;
    }

    // ================================================================
    //  Utility helpers
    // ================================================================
    private Date parseDate(DocumentSnapshot doc, String... fieldNames) {
        for (String field : fieldNames) {
            Object val = doc.get(field);
            if (val == null) continue;
            if (val instanceof com.google.firebase.Timestamp) {
                return ((com.google.firebase.Timestamp) val).toDate();
            }
            if (val instanceof String) {
                String str = (String) val;
                if (!str.isEmpty()) {
                    try {
                        return DATE_FORMAT.parse(str);
                    } catch (ParseException ignored) {
                        try {
                            return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(str);
                        } catch (ParseException ignored2) { /* skip */ }
                    }
                }
            }
            if (val instanceof Long) {
                return new Date((Long) val);
            }
        }
        return null;
    }

    private String formatMoney(double amount) {
        long longVal  = (long) amount;
        String result = String.format(Locale.getDefault(), "%,d", longVal).replace(',', '.');
        return result + " đ";
    }

    private String formatMoneyShort(double amount) {
        long longVal = (long) amount;
        return String.format(Locale.getDefault(), "%,d", longVal).replace(',', '.');
    }

    // ================================================================
    //  Back button: đóng drawer trước khi thoát
    // ================================================================
    @Override
    public void onBackPressed() {
        if (tenantDrawerLayout != null && tenantDrawerLayout.isDrawerOpen(GravityCompat.END)) {
            tenantDrawerLayout.closeDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
    }
}
