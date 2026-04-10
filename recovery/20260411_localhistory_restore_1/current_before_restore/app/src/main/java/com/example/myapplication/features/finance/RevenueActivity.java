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
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.formatter.ValueFormatter;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import android.widget.Toast;
import com.google.android.material.appbar.AppBarLayout;
import com.example.myapplication.core.util.FinancePeriodUtil;
import com.example.myapplication.core.util.ScreenUiHelper;
import com.example.myapplication.domain.Expense;
import com.example.myapplication.domain.House;
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
    
    private Spinner spinnerHouse;
    private List<House> allHouses = new ArrayList<>();
    private List<Room> allRooms = new ArrayList<>();
    private String selectedHouseId = null;
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
    private BarChart barChart;
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



        tvDoanhThuThang = findViewById(R.id.tvDoanhThuThang);



        tvCongNoThang = findViewById(R.id.tvCongNoThang);






        tvSelectedMonth = findViewById(R.id.tvSelectedMonth);

        spinnerHouse = findViewById(R.id.spinnerHouse);
        scopedCollection("houses").addSnapshotListener((snapshot, e) -> {
            if (e != null || snapshot == null) return;
            allHouses.clear();
            for (QueryDocumentSnapshot doc : snapshot) {
                House h = doc.toObject(House.class);
                if (h != null) {
                    h.setId(doc.getId());
                    allHouses.add(h);
                }
            }
            List<String> houseNames = new ArrayList<>();
            houseNames.add("Tất cả nhà");
            for (House h : allHouses) {
                houseNames.add(h.getHouseName() != null ? h.getHouseName() : "Nhà " + h.getId());
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, houseNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerHouse.setAdapter(adapter);
        });

        spinnerHouse.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedHouseId = null;
                } else {
                    selectedHouseId = allHouses.get(position - 1).getId();
                }
                updateReportStats(NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN")));
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedHouseId = null;
                updateReportStats(NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN")));
            }
        });


        barChart = findViewById(R.id.barChart);

        TextView btnPickMonth = findViewById(R.id.btnPickMonth);
        View btnExportPdf = findViewById(R.id.btnExportPdf);

        selectedMonth = FinancePeriodUtil
                .normalizeMonthYear(new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(new Date()));
        if (tvSelectedMonth != null) {
            tvSelectedMonth.setText(getString(R.string.month_with_value, selectedMonth));
        }
        NumberFormat fmt = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));

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
                        if (RoomStatus.RENTED.equals(p.getStatus()))
                            daThua++;
                    }
                    rentedRooms = (int) daThua;
                    tvPhongDaThua.setText(String.valueOf(rentedRooms));
                    if (tvTyLeLapDay != null) {
                        double rate = totalRooms > 0 ? (100.0 * rentedRooms / totalRooms) : 0;
                        tvTyLeLapDay.setText(getString(R.string.occupancy_rate_label,
                                String.format(Locale.getDefault(), "%.1f%%", rate)));
                    }
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
                    updateReportStats(NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN")));
                });

        viewModelProvider.get(InvoiceViewModel.class)
                .getInvoiceList().observe(this, list -> {
                    lastInvoices = list;
                    updateReportStats(NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN")));
                });

        viewModelProvider.get(ExpenseViewModel.class)
                .getExpenseList().observe(this, list -> {
                    lastExpenses = list;
                    updateReportStats(NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN")));
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
            monthLabels.add(getString(R.string.month_with_value, normalized));
        }

        int checked = Math.max(0, monthValues.indexOf(selectedMonth));
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.select_month))
                .setSingleChoiceItems(monthLabels.toArray(new String[0]), checked, (dialog, which) -> {
                    selectedMonth = monthValues.get(which);
                    if (tvSelectedMonth != null) {
                        tvSelectedMonth.setText(getString(R.string.month_with_value, selectedMonth));
                    }
                    updateReportStats(NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN")));
                    dialog.dismiss();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void updateReportStats(@androidx.annotation.NonNull NumberFormat fmt) {
        if (lastInvoices == null)
            return;

        int totalRoomCount = 0;
        int rentedRoomCount = 0;
        Map<String, String> roomToHouseMap = new HashMap<>();
        for (Room r : allRooms) {
            if (r != null && r.getId() != null) {
                roomToHouseMap.put(r.getId(), r.getHouseId());
                if (selectedHouseId == null || selectedHouseId.equals(r.getHouseId())) {
                    totalRoomCount++;
                    if (RoomStatus.RENTED.equals(r.getStatus())) {
                        rentedRoomCount++;
                    }
                }
            }
        }
        
        if (tvTongPhong != null) tvTongPhong.setText(String.valueOf(totalRoomCount));
        if (tvPhongDaThua != null) tvPhongDaThua.setText(String.valueOf(rentedRoomCount));
        if (tvTyLeLapDay != null) {
            double rate = totalRoomCount > 0 ? (100.0 * rentedRoomCount / totalRoomCount) : 0;
            tvTyLeLapDay.setText(getString(R.string.occupancy_rate_label,
                    String.format(Locale.getDefault(), "%.1f%%", rate)));
        }

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

            String roomId = h.getRoomId();
            if (roomId != null) {
                String hId = roomToHouseMap.get(roomId);
                if (selectedHouseId != null && !selectedHouseId.equals(hId)) {
                    continue;
                }
            }
            String invoiceId = h.getId();
            String month = FinancePeriodUtil.normalizeMonthYear(h.getBillingPeriod());
            if (invoiceId != null && !invoiceId.trim().isEmpty()) {
                invoiceMonthById.put(invoiceId, month);
                invoiceTotalById.put(invoiceId, h.getTotalAmount());
            }

            if (selectedMonth.equals(month)) {
                tongCanThuThang += h.getTotalAmount();
            }

            String st = h.getStatus();
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
                            : getString(R.string.other);
                    expenseByCategoryInMonth.put(category,
                            expenseByCategoryInMonth.getOrDefault(category, 0.0) + cp.getAmount());
                }
            }
        }

        double congNoThang = Math.max(0, tongCanThuThang - daThuTheoInvoiceThang);
        double tiLeThuTien = tongCanThuThang > 0 ? (100.0 * daThuTheoInvoiceThang / tongCanThuThang) : 0;
        double loiNhuanThang = daThuThang - tongChiThang;

        if (tvDoanhThuThang != null) tvDoanhThuThang.setText(fmt.format(daThuThang));
        if (tvCongNoThang != null) tvCongNoThang.setText(fmt.format(congNoThang));

        renderBarChart(daThuThang, congNoThang);
    }

    private void renderBarChart(double paidAmount, double unpaidAmount) {
        if (barChart == null) return;

        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, (float) paidAmount));
        entries.add(new BarEntry(1f, (float) unpaidAmount));

        BarDataSet dataSet = new BarDataSet(entries, "Revenue Status");
        dataSet.setColors(new int[]{Color.parseColor("#1565C0"), Color.parseColor("#F44336")});
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value == 0) return "";
                NumberFormat fmt = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
                return fmt.format(value);
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);

        barChart.setData(barData);
        
        Description desc = new Description();
        desc.setText("");
        barChart.setDescription(desc);
        barChart.getLegend().setEnabled(false);
        barChart.setDrawGridBackground(false);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{getString(R.string.revenue_monthly_collected_label), getString(R.string.revenue_unpaid_invoices_short)}));

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        
        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setEnabled(false);

        barChart.setExtraBottomOffset(10f);
        barChart.animateY(1000);
        barChart.invalidate();
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
                + "<p>" + getString(R.string.pdf_monthly_profit_label) + " " + tvLoiNhuanThang.getText() + "</p>"
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




