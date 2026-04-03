package com.example.myapplication.features.finance;

import android.graphics.Color;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import android.widget.Toast;
import com.example.myapplication.core.util.FinancePeriodUtil;
import com.example.myapplication.domain.Expense;
import com.example.myapplication.domain.Invoice;
import com.example.myapplication.domain.Room;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RevenueActivity extends AppCompatActivity {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private List<Invoice> lastInvoices;
    private final List<PaymentEntry> lastPayments = new ArrayList<>();
    private List<Expense> lastExpenses;
    private String selectedMonth;
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
    private TextView tvTongThuLuyKe;
    private TextView tvTongChiLuyKe;
    private TextView tvTyLeLapDay;
    private TextView tvSelectedMonth;
    private LinearLayout llTrendContainer;
    private LinearLayout llTopCategoryContainer;

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

        // Làm status bar trong suốt giống HomeActivity
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_revenue);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Thống kê doanh thu");
        }

        // Tự động thêm padding cho Toolbar để tránh bị Status Bar che mất
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

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
        tvTongThuLuyKe = findViewById(R.id.tvTongThuLuyKe);
        tvTongChiLuyKe = findViewById(R.id.tvTongChiLuyKe);
        tvTyLeLapDay = findViewById(R.id.tvTyLeLapDay);
        tvSelectedMonth = findViewById(R.id.tvSelectedMonth);
        llTrendContainer = findViewById(R.id.llTrendContainer);
        llTopCategoryContainer = findViewById(R.id.llTopCategoryContainer);

        TextView btnPickMonth = findViewById(R.id.btnPickMonth);
        View btnExportPdf = findViewById(R.id.btnExportPdf);

        selectedMonth = FinancePeriodUtil
                .normalizeMonthYear(new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(new Date()));
        if (tvSelectedMonth != null) {
            tvSelectedMonth.setText("Tháng " + selectedMonth);
        }
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        ViewModelProvider viewModelProvider = new ViewModelProvider(this);

        if (btnPickMonth != null) {
            btnPickMonth.setOnClickListener(v -> showMonthPicker(tvSelectedMonth, fmt));
        }
        if (btnExportPdf != null) {
            btnExportPdf.setOnClickListener(v -> exportMonthlyReportPdf(fmt));
        }

        viewModelProvider.get(RoomViewModel.class)
                .getRoomList().observe(this, list -> {
                    if (list == null)
                        return;
                    totalRooms = list.size();
                    tvTongPhong.setText(String.valueOf(totalRooms));
                    long daThua = 0;
                    for (Room p : list) {
                        if (RoomStatus.RENTED.equals(p.getTrangThai()))
                            daThua++;
                    }
                    rentedRooms = (int) daThua;
                    tvPhongDaThua.setText(String.valueOf(rentedRooms));
                    if (tvTyLeLapDay != null) {
                        double rate = totalRooms > 0 ? (100.0 * rentedRooms / totalRooms) : 0;
                        tvTyLeLapDay.setText("Tỷ lệ lấp đầy: " + String.format(Locale.getDefault(), "%.1f%%", rate));
                    }
                });

        viewModelProvider.get(TenantViewModel.class)
                .getTenantList().observe(this, list -> {
                    if (list != null)
                        tvSoTenant.setText(String.valueOf(list.size()));
                });

        // Lắng nghe payments để tính "đã thu" theo tổng payment, không phụ thuộc toggle
        // trạng thái
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
        List<String> monthValues = new ArrayList<>();
        List<String> monthLabels = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            java.util.Calendar c = java.util.Calendar.getInstance();
            c.add(java.util.Calendar.MONTH, -i);
            int m = c.get(java.util.Calendar.MONTH) + 1;
            int y = c.get(java.util.Calendar.YEAR);
            String normalized = String.format(Locale.US, "%02d/%04d", m, y);
            monthValues.add(normalized);
            monthLabels.add("Tháng " + normalized);
        }

        int checked = Math.max(0, monthValues.indexOf(selectedMonth));
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Chọn tháng")
                .setSingleChoiceItems(monthLabels.toArray(new String[0]), checked, (dialog, which) -> {
                    selectedMonth = monthValues.get(which);
                    if (tvSelectedMonth != null) {
                        tvSelectedMonth.setText("Tháng " + selectedMonth);
                    }
                    updateReportStats(fmt,
                            findViewById(R.id.tvDoanhThuThang),
                            findViewById(R.id.tvSoHDChuaTT),
                            findViewById(R.id.tvTongCanThuThang),
                            findViewById(R.id.tvCongNoThang),
                            findViewById(R.id.tvTiLeThuTien),
                            findViewById(R.id.tvTongChiThang),
                            findViewById(R.id.tvLoiNhuanThang),
                            findViewById(R.id.tvTongThuLuyKe),
                            findViewById(R.id.tvTongChiLuyKe));
                    dialog.dismiss();
                })
                .setNegativeButton("Hủy", null)
                .show();
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
        double tongThuLuyKe = 0;
        double tongChiThang = 0;
        double tongChiLuyKe = 0;
        Map<String, String> invoiceMonthById = new HashMap<>();
        Map<String, Double> invoiceTotalById = new HashMap<>();
        Map<String, Double> paidByInvoice = new HashMap<>();
        Map<String, Double> expenseByCategoryInMonth = new HashMap<>();

        for (Invoice h : lastInvoices) {
            if (h == null)
                continue;

            String invoiceId = h.getId();
            String month = FinancePeriodUtil.normalizeMonthYear(h.getThangNam());
            if (invoiceId != null && !invoiceId.trim().isEmpty()) {
                invoiceMonthById.put(invoiceId, month);
                invoiceTotalById.put(invoiceId, h.getTongTien());
            }

            if (selectedMonth.equals(month)) {
                tongCanThuThang += h.getTongTien();
            }

            String st = h.getTrangThai();
            if (st == null || st.trim().isEmpty())
                st = InvoiceStatus.UNREPORTED;

            if (selectedMonth.equals(month)
                    && (InvoiceStatus.REPORTED.equals(st) || InvoiceStatus.PARTIAL.equals(st))) {
                chuaThu++;
            }
        }

        for (PaymentEntry payment : lastPayments) {
            if (payment == null)
                continue;

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
            String month = invoiceMonthById.getOrDefault(invoiceId, "");
            if (!selectedMonth.equals(month))
                continue;
            double paid = paidByInvoice.getOrDefault(invoiceId, 0.0);
            daThuTheoInvoiceThang += Math.min(paid, entry.getValue());
        }

        if (lastExpenses != null) {
            for (Expense cp : lastExpenses) {
                if (cp == null)
                    continue;
                tongChiLuyKe += cp.getAmount();
                String month = FinancePeriodUtil.normalizeMonthYear(cp.getPaidAt());
                if (selectedMonth.equals(month)) {
                    tongChiThang += cp.getAmount();
                    String category = cp.getCategory() != null && !cp.getCategory().trim().isEmpty()
                            ? cp.getCategory().trim()
                            : "Khác";
                    expenseByCategoryInMonth.put(category,
                            expenseByCategoryInMonth.getOrDefault(category, 0.0) + cp.getAmount());
                }
            }
        }

        double congNoThang = Math.max(0, tongCanThuThang - daThuTheoInvoiceThang);
        double tiLeThuTien = tongCanThuThang > 0 ? (100.0 * daThuTheoInvoiceThang / tongCanThuThang) : 0;
        double loiNhuanThang = daThuThang - tongChiThang;

        tvSoHDChuaTT.setText(String.valueOf(chuaThu));
        tvDoanhThuThang.setText(fmt.format(daThuThang));
        tvTongCanThuThang.setText(fmt.format(tongCanThuThang));
        tvCongNoThang.setText(fmt.format(congNoThang));
        tvTiLeThuTien.setText(String.format(Locale.getDefault(), "%.1f%%", tiLeThuTien));
        tvTongChiThang.setText(fmt.format(tongChiThang));
        tvLoiNhuanThang.setText(fmt.format(loiNhuanThang));
        tvTongThuLuyKe.setText("Tổng đã thu: " + fmt.format(tongThuLuyKe));
        tvTongChiLuyKe.setText("Tổng đã chi: " + fmt.format(tongChiLuyKe));

        renderTopCategories(expenseByCategoryInMonth, fmt);
        renderTrend6Months(fmt);
    }

    private void renderTopCategories(Map<String, Double> expenseByCategoryInMonth, NumberFormat fmt) {
        if (llTopCategoryContainer == null)
            return;
        llTopCategoryContainer.removeAllViews();

        List<Map.Entry<String, Double>> entries = new ArrayList<>(expenseByCategoryInMonth.entrySet());
        entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        if (entries.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("Chưa có dữ liệu chi trong tháng này");
            tv.setTextColor(Color.parseColor("#9E9E9E"));
            tv.setTextSize(12f);
            llTopCategoryContainer.addView(tv);
            return;
        }

        int limit = Math.min(5, entries.size());
        for (int i = 0; i < limit; i++) {
            Map.Entry<String, Double> e = entries.get(i);
            TextView tv = new TextView(this);
            tv.setText((i + 1) + ". " + e.getKey() + " - " + fmt.format(e.getValue()));
            tv.setTextColor(Color.parseColor("#424242"));
            tv.setTextSize(12.5f);
            tv.setPadding(0, i == 0 ? 0 : 6, 0, 0);
            llTopCategoryContainer.addView(tv);
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

        if (lastPayments != null) {
            for (PaymentEntry p : lastPayments) {
                if (p == null)
                    continue;
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
                String month = FinancePeriodUtil.normalizeMonthYear(cp.getPaidAt());
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
            TextView label = new TextView(this);
            label.setText(trend.month + " | Thu: " + fmt.format(trend.revenue)
                    + " | Chi: " + fmt.format(trend.expense)
                    + " | LN: " + fmt.format(trend.profit));
            label.setTextSize(11.5f);
            label.setTextColor(Color.parseColor("#616161"));
            label.setPadding(0, 6, 0, 4);
            llTrendContainer.addView(label);

            LinearLayout bars = new LinearLayout(this);
            bars.setOrientation(LinearLayout.HORIZONTAL);
            bars.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            View vRevenue = new View(this);
            View vExpense = new View(this);
            View vProfit = new View(this);
            vRevenue.setBackgroundColor(Color.parseColor("#2E7D32"));
            vExpense.setBackgroundColor(Color.parseColor("#E65100"));
            vProfit.setBackgroundColor(Color.parseColor("#1565C0"));

            int minWeight = 1;
            int rw = Math.max(minWeight, (int) Math.round((trend.revenue / maxValue) * 40));
            int ew = Math.max(minWeight, (int) Math.round((trend.expense / maxValue) * 40));
            int pw = Math.max(minWeight, (int) Math.round((Math.abs(trend.profit) / maxValue) * 40));

            LinearLayout.LayoutParams lpR = new LinearLayout.LayoutParams(0, 14, rw);
            LinearLayout.LayoutParams lpE = new LinearLayout.LayoutParams(0, 14, ew);
            LinearLayout.LayoutParams lpP = new LinearLayout.LayoutParams(0, 14, pw);
            lpE.setMargins(6, 0, 0, 0);
            lpP.setMargins(6, 0, 0, 0);

            bars.addView(vRevenue, lpR);
            bars.addView(vExpense, lpE);
            bars.addView(vProfit, lpP);
            llTrendContainer.addView(bars);
        }
    }

    private void exportMonthlyReportPdf(NumberFormat fmt) {
        String html = buildMonthlyReportHtml(fmt);
        PrintManager printManager = (PrintManager) getSystemService(PRINT_SERVICE);
        if (printManager == null) {
            Toast.makeText(this, "Thiết bị không hỗ trợ in/PDF", Toast.LENGTH_SHORT).show();
            return;
        }

        String jobName = "BaoCao_" + selectedMonth.replace("/", "_");
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
        String title = "Báo cáo tháng " + selectedMonth;
        String content = "<h2>" + title + "</h2>"
                + "<p>Tổng phòng: " + tvTongPhong.getText() + "</p>"
                + "<p>Phòng đang thuê: " + tvPhongDaThua.getText() + "</p>"
                + "<p>Dòng tiền đã thu: " + tvDoanhThuThang.getText() + "</p>"
                + "<p>Tổng cần thu tháng: " + tvTongCanThuThang.getText() + "</p>"
                + "<p>Công nợ tháng: " + tvCongNoThang.getText() + "</p>"
                + "<p>Tỷ lệ thu tiền: " + tvTiLeThuTien.getText() + "</p>"
                + "<p>Tổng chi tháng: " + tvTongChiThang.getText() + "</p>"
                + "<p>Lợi nhuận tháng: " + tvLoiNhuanThang.getText() + "</p>"
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
                + "<h3 style='margin-top:16px;'>Top hạng mục chi</h3>"
                + top
                + "<p style='margin-top:18px;color:#666;'>Xuất lúc: "
                + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date())
                + "</p>"
                + "</body></html>";
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
