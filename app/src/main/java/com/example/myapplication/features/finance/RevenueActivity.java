package com.example.myapplication.features.finance;

import android.graphics.Color;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import android.widget.Toast;
import com.google.android.material.appbar.AppBarLayout;
import com.example.myapplication.core.util.FinancePeriodUtil;
import com.example.myapplication.core.util.ScreenUiHelper;
import com.example.myapplication.core.constants.ExpenseStatus;
import com.example.myapplication.domain.Expense;
import com.example.myapplication.domain.Invoice;
import com.example.myapplication.domain.Room;
import com.example.myapplication.features.invoice.InvoiceFilterDialogHelper;
import com.example.myapplication.viewmodel.ExpenseViewModel;
import com.example.myapplication.viewmodel.InvoiceViewModel;
import com.example.myapplication.viewmodel.TenantViewModel;
import com.example.myapplication.viewmodel.RoomViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import com.example.myapplication.core.constants.InvoiceStatus;
import com.example.myapplication.R;
import com.example.myapplication.core.constants.RoomStatus;
import com.example.myapplication.core.session.TenantSession;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class RevenueActivity extends AppCompatActivity {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private List<Invoice> lastInvoices;
    private final List<PaymentEntry> lastPayments = new ArrayList<>();
    private List<Expense> lastExpenses;
    private List<Room> lastRooms = new ArrayList<>();
    private String selectedMonth;
    private String selectedHouseId;
    private int totalRooms;
    private int rentedRooms;

    private TextView tvTongPhong;
    private TextView tvPhongDaThua;
    private TextView tvDoanhThuThang;
    private TextView tvSoHDChuaTT;
    private TextView tvSoTenant;
    private TextView tvTongCanThuThang;
    private TextView tvCongNoThang;
    private TextView tvTiLeThuTien;
    private TextView tvTongChiThang;
    private TextView tvLoiNhuanThang;
    private TextView tvDaBaoThang;
    private TextView tvChuaChiThang;
    private TextView tvDaChiThang;
    private TextView tvLoiNhuanDuKien;
    private TextView tvLoiNhuanThucTe;
    private TextView tvDoLechLoiNhuan;
    private TextView tvTongThuLuyKe;
    private TextView tvTongChiLuyKe;
    private TextView tvTyLeLapDay;
    private TextView tvSelectedMonth;
    private TextView tvSelectedKhu;
    private View btnSelectKhu;
    private LinearLayout llTrendContainer;
    private LinearLayout llTopCategoryContainer;
    private LinearLayout llCashFlowBridgeContainer;
    private final Map<String, String> houseNameMap = new HashMap<>();

    private static final class MonthTrend {
        final String month;
        final double revenue;
        final double expense;
        final double profit;

        MonthTrend(String month, double revenue, double expense) {
            this.month = month;
            this.revenue = revenue;
            this.expense = expense;
            this.profit = revenue - expense;
        }
    }

    private static final class PaymentEntry {
        final String invoiceId;
        final double amount;
        final String paidAt;

        PaymentEntry(String invoiceId, double amount, String paidAt) {
            this.invoiceId = invoiceId;
            this.amount = amount;
            this.paidAt = paidAt;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make the status bar transparent like HomeActivity
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        ScreenUiHelper.enableEdgeToEdge(this, false);

        setContentView(R.layout.activity_revenue);

        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            ScreenUiHelper.applyTopInset(appBarLayout);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        ScreenUiHelper.setupBackToolbar(this, toolbar, getString(R.string.revenue_statistics_title));

        tvTongPhong = findViewById(R.id.tvTongPhong);
        tvPhongDaThua = findViewById(R.id.tvPhongDaThua);
        tvDoanhThuThang = findViewById(R.id.tvDoanhThuThang);
        tvSoHDChuaTT = findViewById(R.id.tvSoHDChuaTT);
        tvSoTenant = findViewById(R.id.tvSoTenant);
        tvTongCanThuThang = findViewById(R.id.tvTongCanThuThang);
        tvCongNoThang = findViewById(R.id.tvCongNoThang);
        tvTiLeThuTien = findViewById(R.id.tvTiLeThuTien);
        tvTongChiThang = findViewById(R.id.tvTongChiThang);
        tvLoiNhuanThang = findViewById(R.id.tvLoiNhuanThang);
        tvDaBaoThang = findViewById(R.id.tvDaBaoThang);
        tvChuaChiThang = findViewById(R.id.tvChuaChiThang);
        tvDaChiThang = findViewById(R.id.tvDaChiThang);
        tvLoiNhuanDuKien = findViewById(R.id.tvLoiNhuanDuKien);
        tvLoiNhuanThucTe = findViewById(R.id.tvLoiNhuanThucTe);
        tvDoLechLoiNhuan = findViewById(R.id.tvDoLechLoiNhuan);
        tvTongThuLuyKe = findViewById(R.id.tvTongThuLuyKe);
        tvTongChiLuyKe = findViewById(R.id.tvTongChiLuyKe);
        tvTyLeLapDay = findViewById(R.id.tvTyLeLapDay);
        tvSelectedMonth = findViewById(R.id.tvSelectedMonth);
        tvSelectedKhu = findViewById(R.id.tvSelectedKhu);
        btnSelectKhu = findViewById(R.id.btnSelectKhu);
        llTrendContainer = findViewById(R.id.llTrendContainer);
        llTopCategoryContainer = findViewById(R.id.llTopCategoryContainer);
        llCashFlowBridgeContainer = findViewById(R.id.llCashFlowBridgeContainer);

        View btnPickMonth = findViewById(R.id.btnPickMonth);
        View btnExportPdf = findViewById(R.id.btnExportPdf);

        selectedMonth = FinancePeriodUtil
                .normalizeMonthYear(new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(new Date()));
        selectedHouseId = null;
        if (tvSelectedMonth != null) {
            tvSelectedMonth.setText(getString(R.string.month_with_value, selectedMonth));
        }
        updateSelectedHouseLabel();
        NumberFormat fmt = NumberFormat.getCurrencyInstance(Locale.getDefault());

        ViewModelProvider viewModelProvider = new ViewModelProvider(this);

        if (btnPickMonth != null) {
            btnPickMonth.setOnClickListener(v -> showMonthPicker(tvSelectedMonth, fmt));
        }
        if (btnSelectKhu != null) {
            btnSelectKhu.setOnClickListener(v -> showHouseFilterDialog(fmt));
        }
        if (btnExportPdf != null) {
            btnExportPdf.setOnClickListener(v -> exportMonthlyReportPdf(fmt));
        }

        viewModelProvider.get(RoomViewModel.class)
                .getRoomList().observe(this, list -> {
                    lastRooms = list != null ? list : new ArrayList<>();
                    rebuildHouseNameMap();
                    updateSelectedHouseLabel();
                    updateReportStats(fmt,
                            tvDoanhThuThang,
                            tvSoHDChuaTT,
                            tvTongCanThuThang,
                            tvCongNoThang,
                            tvTiLeThuTien,
                            tvTongChiThang,
                            tvLoiNhuanThang,
                            tvTongThuLuyKe,
                            tvTongChiLuyKe);
                });

        viewModelProvider.get(TenantViewModel.class)
                .getTenantList().observe(this, list -> {
                    if (list != null)
                        tvSoTenant.setText(String.valueOf(list.size()));
                });

        // Observe payments to compute collected amount from total payments, independent
        // of toggle state
        // Status
        scopedCollection("payments")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null)
                        return;
                    lastPayments.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        String invoiceId = doc.getString("invoiceId");
                        Double amt = doc.getDouble("amount");
                        if (invoiceId == null || invoiceId.trim().isEmpty())
                            continue;
                        lastPayments.add(new PaymentEntry(
                                invoiceId,
                                amt != null ? amt : 0.0,
                                doc.getString("paidAt")));
                    }
                    updateReportStats(fmt,
                            tvDoanhThuThang,
                            tvSoHDChuaTT,
                            tvTongCanThuThang,
                            tvCongNoThang,
                            tvTiLeThuTien,
                            tvTongChiThang,
                            tvLoiNhuanThang,
                            tvTongThuLuyKe,
                            tvTongChiLuyKe);
                });

        viewModelProvider.get(InvoiceViewModel.class)
                .getInvoiceList().observe(this, list -> {
                    lastInvoices = list;
                    updateReportStats(fmt,
                            tvDoanhThuThang,
                            tvSoHDChuaTT,
                            tvTongCanThuThang,
                            tvCongNoThang,
                            tvTiLeThuTien,
                            tvTongChiThang,
                            tvLoiNhuanThang,
                            tvTongThuLuyKe,
                            tvTongChiLuyKe);
                });

        viewModelProvider.get(ExpenseViewModel.class)
                .getExpenseList().observe(this, list -> {
                    lastExpenses = list;
                    updateReportStats(fmt,
                            tvDoanhThuThang,
                            tvSoHDChuaTT,
                            tvTongCanThuThang,
                            tvCongNoThang,
                            tvTiLeThuTien,
                            tvTongChiThang,
                            tvLoiNhuanThang,
                            tvTongThuLuyKe,
                            tvTongChiLuyKe);
                });
    }

    private void showMonthPicker(TextView tvSelectedMonth, NumberFormat fmt) {
        InvoiceFilterDialogHelper.showMonthFilterDialog(
                this,
                selectedMonth,
                true,
                R.string.revenue_month_picker_title,
                (period, month, year) -> {
                    selectedMonth = period;
                    if (tvSelectedMonth != null) {
                        tvSelectedMonth.setText(getString(R.string.month_with_value, selectedMonth));
                    }
                    updateReportStats(fmt,
                            tvDoanhThuThang,
                            tvSoHDChuaTT,
                            tvTongCanThuThang,
                            tvCongNoThang,
                            tvTiLeThuTien,
                            tvTongChiThang,
                            tvLoiNhuanThang,
                            tvTongThuLuyKe,
                            tvTongChiLuyKe);
                });
    }

    private void showHouseFilterDialog(NumberFormat fmt) {
        InvoiceFilterDialogHelper.showHouseFilterDialog(this, lastRooms, selectedHouseId, (houseId, label) -> {
            selectedHouseId = (houseId == null || houseId.trim().isEmpty()) ? null : houseId;
            updateSelectedHouseLabel();
            updateReportStats(fmt,
                    tvDoanhThuThang,
                    tvSoHDChuaTT,
                    tvTongCanThuThang,
                    tvCongNoThang,
                    tvTiLeThuTien,
                    tvTongChiThang,
                    tvLoiNhuanThang,
                    tvTongThuLuyKe,
                    tvTongChiLuyKe);
        });
    }

    private void updateReportStats(@androidx.annotation.NonNull NumberFormat fmt,
            TextView tvDoanhThuThang,
            TextView tvSoHDChuaTT,
            TextView tvTongCanThuThang,
            TextView tvCongNoThang,
            TextView tvTiLeThuTien,
            TextView tvTongChiThang,
            TextView tvLoiNhuanThang,
            TextView tvTongThuLuyKe,
            TextView tvTongChiLuyKe) {
        if (lastInvoices == null)
            return;

        int chuaThu = 0;
        double daThuThang = 0;
        double tongCanThuThang = 0;
        double daThuTheoInvoiceThang = 0;
        double daBaoChuaThuThang = 0;
        double tongThuLuyKe = 0;
        double tongChiThang = 0;
        double chuaChiThang = 0;
        double daChiThang = 0;
        double tongChiLuyKe = 0;
        Map<String, String> invoiceMonthById = new HashMap<>();
        Map<String, Double> invoiceTotalById = new HashMap<>();
        Map<String, String> roomHouseByRoomId = buildRoomHouseMap();
        Map<String, String> invoiceHouseByInvoiceId = new HashMap<>();
        Set<String> allowedInvoiceIds = new HashSet<>();
        Map<String, Double> paidByInvoice = new HashMap<>();
        Map<String, Double> expenseByCategoryInMonth = new HashMap<>();

        updateRoomSummaryForFilter();

        for (Invoice h : lastInvoices) {
            if (h == null)
                continue;

            String invoiceId = h.getId();
            String month = FinancePeriodUtil.normalizeMonthYear(h.getBillingPeriod());
            String houseId = roomHouseByRoomId.get(h.getRoomId());
            if (invoiceId != null && !invoiceId.trim().isEmpty()) {
                invoiceMonthById.put(invoiceId, month);
                invoiceHouseByInvoiceId.put(invoiceId, houseId);
                invoiceTotalById.put(invoiceId, h.getTotalAmount());
                if (matchesSelectedHouse(houseId)) {
                    allowedInvoiceIds.add(invoiceId);
                }
            }

            if (!matchesSelectedHouse(houseId)) {
                continue;
            }

            if (selectedMonth.equals(month)) {
                tongCanThuThang += h.getTotalAmount();
            }

            String st = h.getStatus();
            if (st == null || st.trim().isEmpty())
                st = InvoiceStatus.UNREPORTED;
            if ("PARTIAL".equalsIgnoreCase(st))
                st = InvoiceStatus.REPORTED;

            if (selectedMonth.equals(month)
                    && InvoiceStatus.REPORTED.equals(st)) {
                chuaThu++;
            }
        }

        for (PaymentEntry payment : lastPayments) {
            if (payment == null)
                continue;

            String paymentHouseId = invoiceHouseByInvoiceId.get(payment.invoiceId);
            if (!matchesSelectedHouse(paymentHouseId)) {
                continue;
            }

            tongThuLuyKe += payment.amount;
            paidByInvoice.put(payment.invoiceId, paidByInvoice.getOrDefault(payment.invoiceId, 0.0) + payment.amount);

            String paidMonth = FinancePeriodUtil.normalizeMonthYear(payment.paidAt);
            if (selectedMonth.equals(paidMonth)) {
                daThuThang += payment.amount;
                continue;
            }

            // Backward compatibility for old payment records missing paidAt.
            if (paidMonth.isEmpty()) {
                String invoiceMonth = invoiceMonthById.getOrDefault(payment.invoiceId, "");
                if (selectedMonth.equals(invoiceMonth)) {
                    daThuThang += payment.amount;
                }
            }
        }

        for (Map.Entry<String, Double> entry : invoiceTotalById.entrySet()) {
            String invoiceId = entry.getKey();
            if (!allowedInvoiceIds.contains(invoiceId)) {
                continue;
            }
            String month = invoiceMonthById.getOrDefault(invoiceId, "");
            if (!selectedMonth.equals(month))
                continue;
            double paid = paidByInvoice.getOrDefault(invoiceId, 0.0);
            double collected = Math.min(paid, entry.getValue());
            daThuTheoInvoiceThang += collected;
            daBaoChuaThuThang += Math.max(0, entry.getValue() - collected);
        }

        if (lastExpenses != null) {
            for (Expense cp : lastExpenses) {
                if (cp == null)
                    continue;

                if (!matchesSelectedHouse(cp.getHouseId())) {
                    continue;
                }

                tongChiLuyKe += cp.getAmount();
                String month = FinancePeriodUtil.normalizeMonthYear(cp.getPeriodMonth());
                if (month.isEmpty()) {
                    month = FinancePeriodUtil.normalizeMonthYear(cp.getPaidAt());
                }
                if (selectedMonth.equals(month)) {
                    tongChiThang += cp.getAmount();
                    String expenseStatus = cp.getStatus();
                    if (expenseStatus == null || expenseStatus.trim().isEmpty()) {
                        expenseStatus = ExpenseStatus.PAID;
                    }
                    if (ExpenseStatus.PENDING.equalsIgnoreCase(expenseStatus.trim())) {
                        chuaChiThang += cp.getAmount();
                    } else {
                        daChiThang += cp.getAmount();
                    }
                    String category = cp.getCategory() != null && !cp.getCategory().trim().isEmpty()
                            ? cp.getCategory().trim()
                            : getString(R.string.other);
                    expenseByCategoryInMonth.put(category,
                            expenseByCategoryInMonth.getOrDefault(category, 0.0) + cp.getAmount());
                }
            }
        }

        double congNoThang = Math.max(0, tongCanThuThang - daThuTheoInvoiceThang);
        double tiLeThuTien = tongCanThuThang > 0 ? (100.0 * daThuTheoInvoiceThang / tongCanThuThang) : 0;
        double loiNhuanDuKien = (daBaoChuaThuThang + daThuTheoInvoiceThang) - (chuaChiThang + daChiThang);
        double loiNhuanThucTe = daThuTheoInvoiceThang - daChiThang;
        double doLechLoiNhuan = loiNhuanDuKien - loiNhuanThucTe;
        double loiNhuanThang = loiNhuanThucTe;

        tvSoHDChuaTT.setText(String.valueOf(chuaThu));
        tvDoanhThuThang.setText(fmt.format(daThuThang));
        if (tvDaBaoThang != null) {
            tvDaBaoThang.setText(fmt.format(daBaoChuaThuThang));
        }
        if (tvChuaChiThang != null) {
            tvChuaChiThang.setText(fmt.format(chuaChiThang));
        }
        if (tvDaChiThang != null) {
            tvDaChiThang.setText(fmt.format(daChiThang));
        }
        if (tvLoiNhuanDuKien != null) {
            tvLoiNhuanDuKien.setText(fmt.format(loiNhuanDuKien));
        }
        if (tvLoiNhuanThucTe != null) {
            tvLoiNhuanThucTe.setText(fmt.format(loiNhuanThucTe));
        }
        if (tvDoLechLoiNhuan != null) {
            tvDoLechLoiNhuan.setText(getString(R.string.revenue_profit_delta_label, fmt.format(doLechLoiNhuan)));
        }
        tvTongCanThuThang.setText(getString(R.string.revenue_total_expected_label, fmt.format(tongCanThuThang)));
        tvCongNoThang.setText(getString(R.string.revenue_debt_label, fmt.format(congNoThang)));
        tvTiLeThuTien.setText(getString(R.string.revenue_collection_rate_label_text,
            String.format(Locale.getDefault(), "%.1f%%", tiLeThuTien)));
        tvTongChiThang.setText(fmt.format(tongChiThang));
        tvLoiNhuanThang.setText(fmt.format(loiNhuanThang));
        tvTongThuLuyKe.setText(getString(R.string.total_collected_label, fmt.format(tongThuLuyKe)));
        tvTongChiLuyKe.setText(getString(R.string.total_expense_label, fmt.format(tongChiLuyKe)));

        renderCashflowBridge(fmt,
            daBaoChuaThuThang,
            daThuTheoInvoiceThang,
            chuaChiThang,
            daChiThang,
            loiNhuanDuKien,
            loiNhuanThucTe);
        renderTopCategories(expenseByCategoryInMonth, fmt);
        renderTrend6Months(fmt);
    }

        private void renderCashflowBridge(NumberFormat fmt,
            double daBao,
            double daThu,
            double chuaChi,
            double daChi,
            double loiNhuanDuKien,
            double loiNhuanThucTe) {
        if (llCashFlowBridgeContainer == null) {
            return;
        }
        llCashFlowBridgeContainer.removeAllViews();

        double max = Math.max(1,
            Math.max(Math.max(daBao, daThu),
                Math.max(Math.max(chuaChi, daChi),
                    Math.max(Math.abs(loiNhuanDuKien), Math.abs(loiNhuanThucTe)))));

        llCashFlowBridgeContainer.addView(buildCashflowRow(
            getString(R.string.revenue_flow_reported_uncollected), daBao, max, Color.parseColor("#C62828"), fmt));
        llCashFlowBridgeContainer.addView(buildCashflowRow(
            getString(R.string.revenue_flow_collected), daThu, max, Color.parseColor("#2E7D32"), fmt));
        llCashFlowBridgeContainer.addView(buildCashflowRow(
            getString(R.string.revenue_flow_pending_expense), chuaChi, max, Color.parseColor("#EF6C00"), fmt));
        llCashFlowBridgeContainer.addView(buildCashflowRow(
            getString(R.string.revenue_flow_paid_expense), daChi, max, Color.parseColor("#E65100"), fmt));
        llCashFlowBridgeContainer.addView(buildCashflowRow(
            getString(R.string.revenue_flow_projected_profit), Math.abs(loiNhuanDuKien), max,
            loiNhuanDuKien >= 0 ? Color.parseColor("#1565C0") : Color.parseColor("#D32F2F"), fmt,
            loiNhuanDuKien));
        llCashFlowBridgeContainer.addView(buildCashflowRow(
            getString(R.string.revenue_flow_actual_profit), Math.abs(loiNhuanThucTe), max,
            loiNhuanThucTe >= 0 ? Color.parseColor("#2E7D32") : Color.parseColor("#D32F2F"), fmt,
            loiNhuanThucTe));
        }

        private View buildCashflowRow(String label, double value, double maxValue, int color, NumberFormat fmt) {
        return buildCashflowRow(label, value, maxValue, color, fmt, value);
        }

        private View buildCashflowRow(String label,
            double value,
            double maxValue,
            int color,
            NumberFormat fmt,
            double displayValue) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, 6, 0, 0);

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label + " • " + fmt.format(displayValue));
        tvLabel.setTextColor(Color.parseColor("#455A64"));
        tvLabel.setTextSize(12f);

        LinearLayout bar = buildWeightedBar(
            value,
            maxValue,
            color,
            Color.parseColor("#ECEFF1"),
            10,
            3);

        row.addView(tvLabel);
        row.addView(bar);
        return row;
        }

    private void renderTopCategories(Map<String, Double> expenseByCategoryInMonth, NumberFormat fmt) {
        if (llTopCategoryContainer == null)
            return;
        llTopCategoryContainer.removeAllViews();

        List<Map.Entry<String, Double>> entries = new ArrayList<>(expenseByCategoryInMonth.entrySet());
        entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        if (entries.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText(getString(R.string.no_expense_data_in_month));
            tv.setTextColor(Color.parseColor("#9E9E9E"));
            tv.setTextSize(12f);
            llTopCategoryContainer.addView(tv);
            return;
        }

        double maxExpense = 1;
        for (Map.Entry<String, Double> entry : entries) {
            maxExpense = Math.max(maxExpense, entry.getValue());
        }

        int limit = Math.min(5, entries.size());
        for (int i = 0; i < limit; i++) {
            Map.Entry<String, Double> e = entries.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, i == 0 ? 0 : 8, 0, 0);

            TextView title = new TextView(this);
            title.setText((i + 1) + ". " + e.getKey() + " • " + fmt.format(e.getValue()));
            title.setTextColor(Color.parseColor("#37474F"));
            title.setTextSize(12.5f);

            LinearLayout bar = buildWeightedBar(
                    e.getValue(),
                    maxExpense,
                    Color.parseColor("#FF9800"),
                    Color.parseColor("#ECEFF1"),
                    10,
                    4);

            row.addView(title);
            row.addView(bar);
            llTopCategoryContainer.addView(row);
        }
    }

    private void renderTrend6Months(NumberFormat fmt) {
        if (llTrendContainer == null)
            return;
        llTrendContainer.removeAllViews();

        List<String> months = new ArrayList<>();
        try {
            String[] parts = selectedMonth.split("/");
            int m = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            java.util.Calendar c = java.util.Calendar.getInstance();
            c.set(java.util.Calendar.YEAR, y);
            c.set(java.util.Calendar.MONTH, m - 1);
            c.set(java.util.Calendar.DAY_OF_MONTH, 1);
            for (int i = 5; i >= 0; i--) {
                java.util.Calendar t = (java.util.Calendar) c.clone();
                t.add(java.util.Calendar.MONTH, -i);
                months.add(String.format(Locale.US, "%02d/%04d",
                        t.get(java.util.Calendar.MONTH) + 1,
                        t.get(java.util.Calendar.YEAR)));
            }
        } catch (Exception e) {
            return;
        }

        Map<String, Double> revenueByMonth = new HashMap<>();
        Map<String, Double> expenseByMonth = new HashMap<>();
        Map<String, String> roomHouseByRoomId = buildRoomHouseMap();
        Map<String, String> invoiceHouseByInvoiceId = new HashMap<>();
        if (lastInvoices != null) {
            for (Invoice invoice : lastInvoices) {
                if (invoice == null || invoice.getId() == null)
                    continue;
                invoiceHouseByInvoiceId.put(invoice.getId(), roomHouseByRoomId.get(invoice.getRoomId()));
            }
        }

        if (lastPayments != null) {
            for (PaymentEntry p : lastPayments) {
                if (p == null)
                    continue;
                String houseId = invoiceHouseByInvoiceId.get(p.invoiceId);
                if (!matchesSelectedHouse(houseId)) {
                    continue;
                }
                String paidMonth = FinancePeriodUtil.normalizeMonthYear(p.paidAt);
                if (months.contains(paidMonth)) {
                    revenueByMonth.put(paidMonth, revenueByMonth.getOrDefault(paidMonth, 0.0) + p.amount);
                }
            }
        }

        if (lastExpenses != null) {
            for (Expense cp : lastExpenses) {
                if (cp == null)
                    continue;
                if (!matchesSelectedHouse(cp.getHouseId())) {
                    continue;
                }
                String month = FinancePeriodUtil.normalizeMonthYear(cp.getPeriodMonth());
                if (month.isEmpty()) {
                    month = FinancePeriodUtil.normalizeMonthYear(cp.getPaidAt());
                }
                if (months.contains(month)) {
                    expenseByMonth.put(month, expenseByMonth.getOrDefault(month, 0.0) + cp.getAmount());
                }
            }
        }

        double maxValue = 1;
        List<MonthTrend> trends = new ArrayList<>();
        for (String month : months) {
            double revenue = revenueByMonth.getOrDefault(month, 0.0);
            double expense = expenseByMonth.getOrDefault(month, 0.0);
            MonthTrend trend = new MonthTrend(month, revenue, expense);
            trends.add(trend);
            maxValue = Math.max(maxValue, Math.max(Math.max(revenue, expense), Math.abs(trend.profit)));
        }

        for (MonthTrend trend : trends) {
            LinearLayout monthBlock = new LinearLayout(this);
            monthBlock.setOrientation(LinearLayout.VERTICAL);
            monthBlock.setPadding(0, 8, 0, 4);

            TextView title = new TextView(this);
            title.setText(trend.month);
            title.setTextSize(12f);
            title.setTextColor(Color.parseColor("#2B3A4E"));
            title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);

            TextView valueLine = new TextView(this);
            valueLine.setText(getString(
                R.string.revenue_trend_line,
                fmt.format(trend.revenue),
                fmt.format(trend.expense),
                fmt.format(trend.profit)));
            valueLine.setTextSize(11f);
            valueLine.setTextColor(Color.parseColor("#607D8B"));
            valueLine.setPadding(0, 2, 0, 4);

            LinearLayout revenueBar = buildWeightedBar(
                trend.revenue,
                maxValue,
                Color.parseColor("#2E7D32"),
                Color.parseColor("#E8F5E9"),
                8,
                2);

            LinearLayout expenseBar = buildWeightedBar(
                trend.expense,
                maxValue,
                Color.parseColor("#E65100"),
                Color.parseColor("#FFF3E0"),
                8,
                2);

            LinearLayout profitBar = buildWeightedBar(
                Math.abs(trend.profit),
                maxValue,
                trend.profit >= 0 ? Color.parseColor("#1565C0") : Color.parseColor("#D32F2F"),
                Color.parseColor("#E3F2FD"),
                8,
                2);

            monthBlock.addView(title);
            monthBlock.addView(valueLine);
            monthBlock.addView(revenueBar);
            monthBlock.addView(expenseBar);
            monthBlock.addView(profitBar);
            llTrendContainer.addView(monthBlock);
        }
    }

        private LinearLayout buildWeightedBar(double value,
            double maxValue,
            int fillColor,
            int trackColor,
            int heightDp,
            int marginTopDp) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(heightDp));
        rowParams.topMargin = dp(marginTopDp);
        row.setLayoutParams(rowParams);

        int weight = Math.max(1, (int) Math.round((value / Math.max(1, maxValue)) * 100));
        int remain = Math.max(1, 100 - weight);

        View fill = new View(this);
        fill.setBackgroundColor(fillColor);
        View track = new View(this);
        track.setBackgroundColor(trackColor);

        row.addView(fill, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, weight));
        row.addView(track, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, remain));
        return row;
        }

        private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
        }

    private void exportMonthlyReportPdf(NumberFormat fmt) {
        String html = buildMonthlyReportHtml(fmt);
        PrintManager printManager = (PrintManager) getSystemService(PRINT_SERVICE);
        if (printManager == null) {
            Toast.makeText(this, getString(R.string.device_not_support_pdf), Toast.LENGTH_SHORT).show();
            return;
        }

        String jobName = getString(R.string.report_job_name_prefix) + selectedMonth.replace("/", "_");
        WebView webView = new WebView(this);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                printManager.print(jobName,
                        view.createPrintDocumentAdapter(jobName),
                        new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build());
            }
        });
        webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null);
    }

    private String buildMonthlyReportHtml(NumberFormat fmt) {
        String title = getString(R.string.report_month_title, selectedMonth);
        String content = "<h2>" + title + "</h2>"
                + "<p>" + getString(R.string.pdf_total_rooms_label) + " " + tvTongPhong.getText() + "</p>"
                + "<p>" + getString(R.string.pdf_rented_rooms_label) + " " + tvPhongDaThua.getText() + "</p>"
                + "<p>" + getString(R.string.pdf_collected_flow_label) + " " + tvDoanhThuThang.getText() + "</p>"
                + "<p>" + getString(R.string.pdf_total_expected_label) + " " + tvTongCanThuThang.getText() + "</p>"
                + "<p>" + getString(R.string.pdf_debt_label) + " " + tvCongNoThang.getText() + "</p>"
                + "<p>" + getString(R.string.pdf_collection_rate_label) + " " + tvTiLeThuTien.getText() + "</p>"
                + "<p>" + getString(R.string.pdf_monthly_expense_label) + " " + tvTongChiThang.getText() + "</p>"
                + "<p>" + getString(R.string.revenue_flow_projected_profit) + ": " + tvLoiNhuanDuKien.getText() + "</p>"
                + "<p>" + getString(R.string.revenue_flow_actual_profit) + ": " + tvLoiNhuanThucTe.getText() + "</p>"
                + "<p>" + tvDoLechLoiNhuan.getText() + "</p>"
                + "<p>" + getString(R.string.revenue_scope_pdf_label, getCurrentScopeLabel()) + "</p>"
                + "<p>" + tvTongThuLuyKe.getText() + "</p>"
                + "<p>" + tvTongChiLuyKe.getText() + "</p>"
                + "<p>" + tvTyLeLapDay.getText() + "</p>";

        StringBuilder top = new StringBuilder();
        if (llTopCategoryContainer != null) {
            for (int i = 0; i < llTopCategoryContainer.getChildCount(); i++) {
                View v = llTopCategoryContainer.getChildAt(i);
                if (v instanceof TextView) {
                    top.append("<p>").append(((TextView) v).getText()).append("</p>");
                }
            }
        }

        return "<!DOCTYPE html><html><head><meta charset='utf-8'/>"
                + "<style>body{font-family:Arial,sans-serif;padding:28px;color:#222;}h2{margin:0 0 12px;}p{margin:5px 0;}</style>"
                + "</head><body>"
                + content
                + "<h3 style='margin-top:16px;'>" + getString(R.string.top_expense_categories_title) + "</h3>"
                + top
                + "<p style='margin-top:18px;color:#666;'>" + getString(R.string.exported_at_label) + " "
                + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date())
                + "</p>"
                + "</body></html>";
    }

    private Map<String, String> buildRoomHouseMap() {
        Map<String, String> map = new HashMap<>();
        for (Room room : lastRooms) {
            if (room == null || room.getId() == null || room.getId().trim().isEmpty()) {
                continue;
            }
            map.put(room.getId(), room.getHouseId());
        }
        return map;
    }

    private boolean matchesSelectedHouse(String houseId) {
        if (selectedHouseId == null || selectedHouseId.trim().isEmpty()) {
            return true;
        }
        return selectedHouseId.equals(houseId);
    }

    private void rebuildHouseNameMap() {
        houseNameMap.clear();
        for (Room room : lastRooms) {
            if (room == null || room.getHouseId() == null || room.getHouseId().trim().isEmpty()) {
                continue;
            }
            String name = room.getHouseName();
            if (name == null || name.trim().isEmpty()) {
                name = getString(R.string.house);
            }
            houseNameMap.put(room.getHouseId(), name);
        }
    }

    private void updateSelectedHouseLabel() {
        if (tvSelectedKhu == null) {
            return;
        }
        tvSelectedKhu.setText(getCurrentScopeLabel());
    }

    private String getCurrentScopeLabel() {
        if (selectedHouseId == null || selectedHouseId.trim().isEmpty()) {
            return getString(R.string.all_houses);
        }
        String name = houseNameMap.get(selectedHouseId);
        if (name == null || name.trim().isEmpty()) {
            return getString(R.string.house);
        }
        return name;
    }

    private void updateRoomSummaryForFilter() {
        totalRooms = 0;
        rentedRooms = 0;
        for (Room room : lastRooms) {
            if (room == null) {
                continue;
            }
            if (!matchesSelectedHouse(room.getHouseId())) {
                continue;
            }
            totalRooms++;
            if (RoomStatus.RENTED.equals(room.getStatus())) {
                rentedRooms++;
            }
        }

        tvTongPhong.setText(String.valueOf(totalRooms));
        tvPhongDaThua.setText(String.valueOf(rentedRooms));
        if (tvTyLeLapDay != null) {
            double rate = totalRooms > 0 ? (100.0 * rentedRooms / totalRooms) : 0;
            tvTyLeLapDay.setText(getString(R.string.occupancy_rate_label,
                    String.format(Locale.getDefault(), "%.1f%%", rate)));
        }
    }

    private CollectionReference scopedCollection(String collection) {
        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId != null && !tenantId.isEmpty()) {
            return db.collection("tenants").document(tenantId).collection(collection);
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
            throw new IllegalStateException("User not logged in");
        return db.collection("users").document(user.getUid()).collection(collection);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
