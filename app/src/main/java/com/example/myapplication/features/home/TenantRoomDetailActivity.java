package com.example.myapplication.features.home;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.myapplication.R;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.ScreenUiHelper;
import com.example.myapplication.features.contract.TenantContractDetailsActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.AppBarLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Màn hình chi tiết phòng của Tenant:
 *  - Block 1: Tóm tắt hợp đồng (vòng tròn ngày còn lại, ngày vào/kết thúc)
 *  - Block 2: Danh mục tính tiền (rates điện/nước/rác/wifi + số cuối đồng hồ)
 */
public class TenantRoomDetailActivity extends AppCompatActivity {

    // --- Intent keys ---
    public static final String EXTRA_ROOM_ID   = "ROOM_ID";
    public static final String EXTRA_TENANT_ID = "TENANT_ID";

    // --- Block 1: Hợp đồng ---
    private ProgressBar contractProgress;
    private TextView tvDaysRemaining;
    private TextView tvMonthsStayed;
    private TextView tvContractStatus;
    private TextView tvStartDate;
    private TextView tvEndDate;
    private TextView btnViewContractDetail;
    private ImageView ivRoomImage;
    private TextView tvOverviewRoomName;
    private TextView tvOverviewHouseName;
    private TextView tvMemberSummary;
    private TextView tvMemberList;

    // --- Block 2: Tính tiền ---
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

    // --- Data ---
    private String roomId;
    private String tenantId;   // = activeTenantId từ Firestore (= ownerUid trong hệ thống hiện tại)
    private String contractId;
    private DocumentSnapshot activeContractDoc;
    private DocumentSnapshot activeRoomDoc;

    private FirebaseFirestore db;
    private FirebaseAuth      mAuth;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    // ================================================================
    //  onCreate
    // ================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScreenUiHelper.enableEdgeToEdge(this, false);
        setContentView(R.layout.activity_tenant_room_detail);

        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            ScreenUiHelper.applyTopInset(appBarLayout);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        ScreenUiHelper.setupBackToolbar(this, toolbar, getString(R.string.tenant_room_overview_title));

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        roomId   = getIntent().getStringExtra(EXTRA_ROOM_ID);
        tenantId = getIntent().getStringExtra(EXTRA_TENANT_ID);
        if (tenantId == null || tenantId.trim().isEmpty()) {
            tenantId = TenantSession.getActiveTenantId();
        }

        bindViews();
        loadAllData();
        setupClickListeners();
    }

    // ================================================================
    //  Ánh xạ view
    // ================================================================
    private void bindViews() {
        contractProgress        = findViewById(R.id.contractProgress);
        tvDaysRemaining         = findViewById(R.id.tvDaysRemaining);
        tvMonthsStayed          = findViewById(R.id.tvMonthsStayed);
        tvContractStatus        = findViewById(R.id.tvContractStatus);
        tvStartDate             = findViewById(R.id.tvStartDate);
        tvEndDate               = findViewById(R.id.tvEndDate);
        btnViewContractDetail   = findViewById(R.id.btnViewContractDetail);
        ivRoomImage             = findViewById(R.id.ivRoomImage);
        tvOverviewRoomName      = findViewById(R.id.tvOverviewRoomName);
        tvOverviewHouseName     = findViewById(R.id.tvOverviewHouseName);
        tvMemberSummary         = findViewById(R.id.tvMemberSummary);
        tvMemberList            = findViewById(R.id.tvMemberList);

        tvBillingSubtitle       = findViewById(R.id.tvBillingSubtitle);
        tvPaymentDayLabel       = findViewById(R.id.tvPaymentDayLabel);
        tvPaymentDaysWarning    = findViewById(R.id.tvPaymentDaysWarning);
        tvRentAmount            = findViewById(R.id.tvRentAmount);
        tvDepositAmount         = findViewById(R.id.tvDepositAmount);
        tvWaterRate             = findViewById(R.id.tvWaterRate);
        tvElecRate              = findViewById(R.id.tvElecRate);
        tvGarbageFee            = findViewById(R.id.tvGarbageFee);
        tvWifiFee               = findViewById(R.id.tvWifiFee);
        tvWaterStatus           = findViewById(R.id.tvWaterStatus);
        tvWaterLastIndex        = findViewById(R.id.tvWaterLastIndex);
        tvElecStatus            = findViewById(R.id.tvElecStatus);
        tvElecLastIndex         = findViewById(R.id.tvElecLastIndex);
    }

    // ================================================================
    //  Load tất cả dữ liệu
    // ================================================================
    private void loadAllData() {
        if (roomId == null || roomId.isEmpty()) return;

        loadRoomOverview(roomId);
        loadRoomMembers(roomId);
        getContractSummary(roomId);
        getLatestServiceRates(roomId);
        fetchLatestMeterReading(roomId);
    }

    private void loadRoomOverview(String roomId) {
        if (tenantId == null) return;

        db.collection("users").document(tenantId)
                .collection("rooms").document(roomId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        applyRoomOverview(doc);
                    } else {
                        db.collection("tenants").document(tenantId)
                                .collection("rooms").document(roomId)
                                .get()
                                .addOnSuccessListener(this::applyRoomOverview);
                    }
                });
    }

    private void applyRoomOverview(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return;

        activeRoomDoc = doc;

        String roomNumber = doc.getString("roomNumber");
        if (roomNumber == null || roomNumber.trim().isEmpty()) {
            roomNumber = roomId;
        }
        if (roomNumber != null && !roomNumber.trim().isEmpty()) {
            tvOverviewRoomName.setText(getString(R.string.tenant_room_overview_room_name, roomNumber.trim()));
        }

        String houseName = doc.getString("houseName");
        if (houseName != null && !houseName.trim().isEmpty()) {
            tvOverviewHouseName.setText(getString(R.string.tenant_room_overview_house_name, houseName.trim()));
        } else {
            tvOverviewHouseName.setText(getString(R.string.tenant_room_overview_house_placeholder));
        }

        String imageUrl = doc.getString("imageUrl");
        if (imageUrl != null && !imageUrl.trim().isEmpty() && !isDestroyed()) {
            Glide.with(this)
                    .load(imageUrl.trim())
                    .centerCrop()
                    .placeholder(R.drawable.bg_room_placeholder)
                    .into(ivRoomImage);
        }
    }

    private void loadRoomMembers(String roomId) {
        if (tenantId == null || roomId == null || roomId.isEmpty()) return;

        db.collection("tenants").document(tenantId)
                .collection("contractMembers")
                .whereEqualTo("roomId", roomId)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(qs -> {
                    if (qs == null || qs.isEmpty()) {
                        db.collection("users").document(tenantId)
                                .collection("contractMembers")
                                .whereEqualTo("roomId", roomId)
                                .whereEqualTo("active", true)
                                .get()
                                .addOnSuccessListener(this::applyRoomMembers)
                                .addOnFailureListener(e -> applyRoomMembers(null));
                        return;
                    }
                    applyRoomMembers(qs);
                })
                .addOnFailureListener(e -> applyRoomMembers(null));
    }

    private void applyRoomMembers(QuerySnapshot querySnapshot) {
        if (querySnapshot == null || querySnapshot.isEmpty()) {
            tvMemberSummary.setText(getString(R.string.tenant_room_overview_members_empty));
            tvMemberList.setText(getString(R.string.tenant_room_overview_members_empty));
            return;
        }

        List<String> lines = new ArrayList<>();
        List<DocumentSnapshot> docs = new ArrayList<>(querySnapshot.getDocuments());
        Collections.sort(docs, (left, right) -> {
            Boolean leftRep = left.getBoolean("contractRepresentative");
            Boolean rightRep = right.getBoolean("contractRepresentative");
            boolean leftRepresentative = leftRep != null && leftRep;
            boolean rightRepresentative = rightRep != null && rightRep;
            if (leftRepresentative != rightRepresentative) {
                return leftRepresentative ? -1 : 1;
            }
            String leftName = left.getString("fullName");
            String rightName = right.getString("fullName");
            if (leftName == null) leftName = "";
            if (rightName == null) rightName = "";
            return leftName.compareToIgnoreCase(rightName);
        });

        for (DocumentSnapshot doc : docs) {
            String name = doc.getString("fullName");
            String phone = doc.getString("phoneNumber");
            Boolean rep = doc.getBoolean("contractRepresentative");
            StringBuilder line = new StringBuilder("• ");
            line.append((name != null && !name.trim().isEmpty()) ? name.trim() : "Thành viên");
            if (phone != null && !phone.trim().isEmpty()) {
                line.append(" - ").append(phone.trim());
            }
            if (rep != null && rep) {
                line.append(" (người đại diện thuê)");
            }
            lines.add(line.toString());
        }

        tvMemberSummary.setText(getString(R.string.tenant_room_overview_members_count, lines.size()));
        tvMemberList.setText(String.join("\n", lines));
    }

    // ================================================================
    //  Hàm 1: getContractSummary - tính toán ngày còn lại từ Firestore
    // ================================================================
    public void getContractSummary(String roomId) {
        if (tenantId == null) return;

        // Contracts được lưu tại tenants/{tenantId}/contracts
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
                        // Fallback: thử path users/{tenantId}/contracts (legacy)
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

    /** Tìm contract ACTIVE trong QuerySnapshot (hoặc lấy contract mới nhất nếu không có ACTIVE) */
    private DocumentSnapshot findActiveContract(QuerySnapshot qs) {
        if (qs == null || qs.isEmpty()) return null;
        for (DocumentSnapshot doc : qs.getDocuments()) {
            String status = doc.getString("contractStatus");
            if ("ACTIVE".equalsIgnoreCase(status)) return doc;
        }
        // Nếu không có ACTIVE, trả về doc đầu tiên
        return qs.getDocuments().get(0);
    }

    /** Áp dụng dữ liệu hợp đồng lên UI */
    private void applyContractToUI(DocumentSnapshot doc) {
        contractId = doc.getId();
        activeContractDoc = doc;

        // Đọc ngày bắt đầu: ưu tiên rentalStartDate (String dd/MM/yyyy)
        Date startDate = parseDate(doc, "rentalStartDate", "startDate");
        Date endDate   = parseDate(doc, "contractEndDate", "endDate");

        if (startDate == null || endDate == null) {
            showNoContractUI();
            return;
        }

        Date today = new Date();
        long totalMs     = endDate.getTime() - startDate.getTime();
        long remainingMs = endDate.getTime() - today.getTime();
        long totalDays   = TimeUnit.MILLISECONDS.toDays(totalMs);
        long daysLeft    = TimeUnit.MILLISECONDS.toDays(remainingMs);
        long daysStayed  = TimeUnit.MILLISECONDS.toDays(today.getTime() - startDate.getTime());

        // Tính số tháng đã ở
        long monthsStayed = daysStayed / 30;
        long extraDays    = daysStayed % 30;

        // Cập nhật vòng tròn
        int progress = (totalDays > 0)
                ? (int) Math.max(0, Math.min(100, (remainingMs * 100L) / totalMs))
                : 0;
        contractProgress.setProgress(progress);

        // Cập nhật text
        tvDaysRemaining.setText(String.valueOf(Math.max(0, daysLeft)));
        tvMonthsStayed.setText(getString(R.string.tenant_room_months_stayed_value, monthsStayed, extraDays));
        tvStartDate.setText(DATE_FORMAT.format(startDate));
        tvEndDate.setText(DATE_FORMAT.format(endDate));

        // Trạng thái hợp đồng
        if (daysLeft > 0) {
            tvContractStatus.setText(getString(R.string.tenant_room_contract_active));
        } else {
            tvContractStatus.setText(getString(R.string.tenant_room_contract_expired));
        }

        // Tiền cọc từ contract (depositAmount)
        double deposit = getNumber(doc, "depositAmount", "legacyDepositAmount");
        if (deposit > 0) {
            tvDepositAmount.setText(formatMoney(deposit));
        }

        // Tiền thuê từ contract (ưu tiên rentAmount, fallback roomPrice)
        double rentPrice = getNumber(doc, "rentAmount", "roomPrice");
        if (rentPrice > 0) {
            tvRentAmount.setText(formatMoney(rentPrice));
        }

        // Sau khi có contract, nạp lại bảng đơn giá để phản ánh đúng dịch vụ theo hợp đồng.
        getLatestServiceRates(roomId);
    }

    private void showNoContractUI() {
        tvDaysRemaining.setText(getString(R.string.tenant_room_value_placeholder));
        tvMonthsStayed.setText(getString(R.string.tenant_room_no_contract));
        tvContractStatus.setText(getString(R.string.tenant_room_contract_not_found));
        tvStartDate.setText(getString(R.string.tenant_room_date_placeholder));
        tvEndDate.setText(getString(R.string.tenant_room_date_placeholder));
        contractProgress.setProgress(0);
    }

    // ================================================================
    //  Hàm 2: getLatestServiceRates - load đơn giá dịch vụ từ Firestore
    // ================================================================
    public void getLatestServiceRates(String roomId) {
        if (tenantId == null) return;

        // Thử users/{tenantId}/rooms/{roomId} trước (vì rooms lưu ở users path)
        db.collection("users").document(tenantId)
                .collection("rooms").document(roomId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        applyServiceRatesToUI(doc);
                    } else {
                        // Fallback: tenants/{tenantId}/rooms/{roomId}
                        db.collection("tenants").document(tenantId)
                                .collection("rooms").document(roomId)
                                .get()
                                .addOnSuccessListener(this::applyServiceRatesToUI)
                                .addOnFailureListener(e -> { /* no rates */ });
                    }
                })
                .addOnFailureListener(e -> { /* no rates */ });
    }

    /** Áp dụng đơn giá dịch vụ từ room document */
    private void applyServiceRatesToUI(DocumentSnapshot doc) {
        if (!doc.exists()) return;

        activeRoomDoc = doc;

        // Tiền thuê (rentAmount)
        double rent = getNumber(doc, "rentAmount", "roomPrice");
        if (rent > 0 && (tvRentAmount.getText() == null || tvRentAmount.getText().toString().contains("--"))) {
            tvRentAmount.setText(formatMoney(rent));
        }

        // Tên nhà
        String houseName = doc.getString("houseName");
        if (houseName != null && !houseName.isEmpty() && tvBillingSubtitle != null) {
            tvBillingSubtitle.setText(getString(R.string.tenant_room_billing_subtitle_with_house, houseName));
        }

        // Tiền nước
        double waterRate = getNumber(doc, "waterRate", "waterPrice");
        if (waterRate > 0) {
            tvWaterRate.setText(getString(R.string.tenant_room_rate_water, formatMoneyShort(waterRate)));
        }

        // Tiền điện
        double elecRate = getNumber(doc, "electricityRate", "electricRate", "electricityPrice");
        if (elecRate > 0) {
            tvElecRate.setText(getString(R.string.tenant_room_rate_electric, formatMoneyShort(elecRate)));
        }

        // Tiền rác
        double garbage = getNumber(doc, "garbageFee", "trashFee", "trashPrice");
        if (garbage > 0) {
            tvGarbageFee.setText(getString(R.string.tenant_room_rate_per_person, formatMoneyShort(garbage)));
        }

        // Tiền wifi/internet
        double wifi = getNumber(doc, "internetFee", "wifiFee", "internetPrice");
        if (wifi > 0) {
            tvWifiFee.setText(getString(R.string.tenant_room_rate_per_person, formatMoneyShort(wifi)));
        }

        // Ngày chốt tiền thuê (paymentDay)
        int day = (int) Math.round(getNumber(doc, "paymentDay", "billingDay"));
        if (day > 0 && day <= 31) {
            tvPaymentDayLabel.setText(getString(R.string.tenant_room_payment_day_label, day));
            updatePaymentDaysWarning(day);
        }

        loadHouseServiceRates(doc);
        applyServiceRatesFromLatestInvoice(roomId);
    }

    private void loadHouseServiceRates(DocumentSnapshot roomDoc) {
        if (roomDoc == null || !roomDoc.exists() || tenantId == null) {
            return;
        }

        String houseId = roomDoc.getString("houseId");
        if (houseId == null || houseId.trim().isEmpty()) {
            return;
        }

        db.collection("users").document(tenantId)
                .collection("houses").document(houseId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        applyHouseServiceRates(doc);
                    } else {
                        db.collection("tenants").document(tenantId)
                                .collection("houses").document(houseId)
                                .get()
                                .addOnSuccessListener(this::applyHouseServiceRates);
                    }
                });
    }

    private void applyHouseServiceRates(DocumentSnapshot houseDoc) {
        if (houseDoc == null || !houseDoc.exists()) {
            return;
        }

        String houseName = houseDoc.getString("houseName");
        if (houseName != null && !houseName.trim().isEmpty() && tvBillingSubtitle != null) {
            tvBillingSubtitle.setText(getString(R.string.tenant_room_billing_subtitle_with_house, houseName.trim()));
        }

        double elec = getNumber(houseDoc, "electricityPrice", "electricRate", "electricityRate");
        if (elec > 0) {
            String unit = unitLabelFromCode(houseDoc.getString("electricityCalculationMethod"), "KWh");
            tvElecRate.setText(formatRate(elec, unit));
        }

        double water = getNumber(houseDoc, "waterPrice", "waterRate");
        if (water > 0) {
            String unit = unitLabelFromCode(houseDoc.getString("waterCalculationMethod"), "Khối");
            tvWaterRate.setText(formatRate(water, unit));
        }

        double trash = getNumber(houseDoc, "trashPrice", "garbageFee", "trashFee");
        if (trash > 0) {
            String unit = unitLabelFromCode(houseDoc.getString("trashUnit"), "Người");
            tvGarbageFee.setText(formatRate(trash, unit));
        }

        double wifi = getNumber(houseDoc, "internetPrice", "wifiFee", "internetFee");
        if (wifi > 0) {
            String unit = unitLabelFromCode(houseDoc.getString("internetUnit"), "Phòng");
            tvWifiFee.setText(formatRate(wifi, unit));
        }
    }

    private void applyServiceRatesFromLatestInvoice(String activeRoomId) {
        if (tenantId == null || activeRoomId == null || activeRoomId.isEmpty()) {
            return;
        }

        db.collection("tenants").document(tenantId)
                .collection("invoices")
                .whereEqualTo("roomId", activeRoomId)
                .limit(24)
                .get()
                .addOnSuccessListener(qs -> {
                    DocumentSnapshot latest = pickLatestInvoice(qs);
                    if (latest != null) {
                        applyInvoiceRateFallback(latest);
                    } else {
                        db.collection("users").document(tenantId)
                                .collection("invoices")
                                .whereEqualTo("roomId", activeRoomId)
                                .limit(24)
                                .get()
                                .addOnSuccessListener(qs2 -> {
                                    DocumentSnapshot legacy = pickLatestInvoice(qs2);
                                    if (legacy != null) {
                                        applyInvoiceRateFallback(legacy);
                                    }
                                });
                    }
                });
    }

    private DocumentSnapshot pickLatestInvoice(QuerySnapshot qs) {
        if (qs == null || qs.isEmpty()) {
            return null;
        }
        DocumentSnapshot latest = null;
        int latestKey = -1;
        for (QueryDocumentSnapshot doc : qs) {
            String period = doc.getString("billingPeriod");
            int key = billingPeriodKey(period);
            if (key > latestKey) {
                latestKey = key;
                latest = doc;
            }
        }
        return latest != null ? latest : qs.getDocuments().get(0);
    }

    private void applyInvoiceRateFallback(DocumentSnapshot invoiceDoc) {
        if (invoiceDoc == null || !invoiceDoc.exists()) {
            return;
        }

        double elec = getNumber(invoiceDoc, "electricUnitPrice");
        if (elec > 0) {
            tvElecRate.setText(getString(R.string.tenant_room_rate_electric, formatMoneyShort(elec)));
        }

        double water = getNumber(invoiceDoc, "waterUnitPrice");
        if (water > 0) {
            tvWaterRate.setText(getString(R.string.tenant_room_rate_water, formatMoneyShort(water)));
        }

        double trash = getNumber(invoiceDoc, "trashFee", "garbageFee");
        if (trash > 0) {
            tvGarbageFee.setText(getString(R.string.tenant_room_money_amount, formatMoneyShort(trash)));
        }

        double wifi = getNumber(invoiceDoc, "internetFee", "wifiFee");
        if (wifi > 0) {
            tvWifiFee.setText(getString(R.string.tenant_room_money_amount, formatMoneyShort(wifi)));
        }
    }

    /** Tính và hiển thị cảnh báo số ngày còn lại đến kỳ chốt */
    private void updatePaymentDaysWarning(int paymentDay) {
        Calendar cal = Calendar.getInstance();
        int today = cal.get(Calendar.DAY_OF_MONTH);
        int daysUntilPayment = paymentDay - today;
        if (daysUntilPayment < 0) {
            // Tính toán sang tháng sau
            int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            daysUntilPayment = daysInMonth - today + paymentDay;
        }

        if (daysUntilPayment <= 5) {
            tvPaymentDaysWarning.setText(getString(R.string.tenant_room_payment_warning_near_due, daysUntilPayment));
        } else {
            tvPaymentDaysWarning.setText(getString(R.string.tenant_room_payment_warning_remaining, daysUntilPayment));
        }
    }

    // ================================================================
    //  Hàm 3: fetchLatestMeterReading - lấy số cuối đồng hồ gần nhất
    // ================================================================
    private void fetchLatestMeterReading(String roomId) {
        if (tenantId == null) return;

        // MeterReadings lưu tại tenants/{tenantId}/meterReadings, filter theo roomId, sort by periodKey
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
                                    if (!qs2.isEmpty()) applyMeterReadingToUI(qs2.getDocuments().get(0));
                                    // else giữ "Chưa có"
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

        String suffix = (period != null) ? " (" + period + ")" : "";

        if (elecEnd instanceof Number && ((Number) elecEnd).doubleValue() > 0) {
            tvElecStatus.setText(getString(R.string.tenant_room_meter_finalized));
            tvElecLastIndex.setText(getString(R.string.tenant_room_last_index_value,
                    (int) ((Number) elecEnd).doubleValue(), suffix));
        }

        if (waterEnd instanceof Number && ((Number) waterEnd).doubleValue() > 0) {
            tvWaterStatus.setText(getString(R.string.tenant_room_meter_finalized));
            tvWaterLastIndex.setText(getString(R.string.tenant_room_last_index_value,
                    (int) ((Number) waterEnd).doubleValue(), suffix));
        }
    }

    // ================================================================
    //  Setup click listeners
    // ================================================================
    private void setupClickListeners() {
        // Xem chi tiết hợp đồng
        if (btnViewContractDetail != null) {
            btnViewContractDetail.setOnClickListener(v -> {
                if (roomId == null || roomId.trim().isEmpty() || tenantId == null || tenantId.trim().isEmpty()) {
                    android.widget.Toast.makeText(
                            TenantRoomDetailActivity.this,
                            getString(R.string.tenant_menu_room_not_identified),
                            android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(this, TenantContractDetailsActivity.class);
                intent.putExtra(TenantContractDetailsActivity.EXTRA_ROOM_ID, roomId);
                intent.putExtra(TenantContractDetailsActivity.EXTRA_TENANT_ID, tenantId);
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    android.widget.Toast.makeText(
                            TenantRoomDetailActivity.this,
                            getString(R.string.operation_failed),
                            android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // ================================================================
    //  Utility helpers
    // ================================================================

    /**
     * Parse date từ DocumentSnapshot theo thứ tự ưu tiên field names.
     * Hỗ trợ cả String (dd/MM/yyyy) và Firestore Timestamp.
     */
    private Date parseDate(DocumentSnapshot doc, String... fieldNames) {
        for (String field : fieldNames) {
            Object val = doc.get(field);
            if (val == null) continue;

            // Case 1: Firestore Timestamp
            if (val instanceof com.google.firebase.Timestamp) {
                return ((com.google.firebase.Timestamp) val).toDate();
            }
            // Case 2: String dd/MM/yyyy
            if (val instanceof String) {
                String str = (String) val;
                if (!str.isEmpty()) {
                    try {
                        return DATE_FORMAT.parse(str);
                    } catch (ParseException ignored) {
                        // Thử yyyy-MM-dd
                        try {
                            return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(str);
                        } catch (ParseException ignored2) { /* skip */ }
                    }
                }
            }
            // Case 3: Long (epoch millis)
            if (val instanceof Long) {
                return new Date((Long) val);
            }
        }
        return null;
    }

    /** Format số thành dạng 2.000.000 đ */
    private String formatMoney(double amount) {
        long longVal = (long) amount;
        String formatted = String.format(Locale.getDefault(), "%,d", longVal)
                .replace(',', '.');
        return getString(R.string.tenant_room_money_amount, formatted);
    }

    /** Format số ngắn (không có đ ở cuối) */
    private String formatMoneyShort(double amount) {
        long longVal = (long) amount;
        return String.format(Locale.getDefault(), "%,d", longVal).replace(',', '.');
    }

    private String formatRate(double amount, String unitLabel) {
        return formatMoneyShort(amount) + " đ / " + unitLabel;
    }

    private String unitLabelFromCode(String unitCode, String fallback) {
        if (unitCode == null || unitCode.trim().isEmpty()) {
            return fallback;
        }
        String normalized = unitCode.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "kwh":
                return "KWh";
            case "meter":
                return "Khối";
            case "person":
            case "per_person":
                return "Người";
            case "room":
                return "Phòng";
            case "vehicle":
                return "Xe";
            default:
                return fallback;
        }
    }

    private double getNumber(DocumentSnapshot doc, String... keys) {
        if (doc == null || keys == null) {
            return 0;
        }
        for (String key : keys) {
            Object raw = doc.get(key);
            if (raw instanceof Number) {
                return ((Number) raw).doubleValue();
            }
        }
        return 0;
    }

    private int billingPeriodKey(String period) {
        if (period == null || period.trim().isEmpty()) {
            return -1;
        }
        String[] parts = period.trim().split("/");
        if (parts.length != 2) {
            return -1;
        }
        try {
            int month = Integer.parseInt(parts[0]);
            int year = Integer.parseInt(parts[1]);
            if (month < 1 || month > 12) {
                return -1;
            }
            return year * 100 + month;
        } catch (NumberFormatException ignore) {
            return -1;
        }
    }
}
