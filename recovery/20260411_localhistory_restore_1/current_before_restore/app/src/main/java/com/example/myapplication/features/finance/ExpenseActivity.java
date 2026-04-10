package com.example.myapplication.features.finance;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.appbar.AppBarLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import com.example.myapplication.R;
import com.example.myapplication.core.constants.TenantRoles;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.FinancePeriodUtil;
import com.example.myapplication.core.util.MoneyFormatter;
import com.example.myapplication.core.util.ScreenUiHelper;
import com.example.myapplication.domain.House;
import com.example.myapplication.domain.Expense;
import com.example.myapplication.viewmodel.ExpenseViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.CollectionReference;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExpenseActivity extends AppCompatActivity {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private ExpenseViewModel viewModel;
    private ExpenseAdapter adapter;

    private TextView tvEmpty;
    private TextView tvSummary;
    private TextView tvSelectedMonth;
    private TextView btnPickMonth;
    private FloatingActionButton fabAdd;
    private PieChart pieChart;

    private boolean readOnly;
    
    private Spinner spinnerHouse;
    private List<House> allHouses = new ArrayList<>();
    private String selectedHouseId = null;
    private String selectedMonth;
    private List<Expense> allExpenses = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        ScreenUiHelper.enableEdgeToEdge(this, false);

        setContentView(R.layout.activity_expense);

        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            ScreenUiHelper.applyTopInset(appBarLayout);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        ScreenUiHelper.setupBackToolbar(this, toolbar, getString(R.string.expense_management));

        tvEmpty = findViewById(R.id.tvEmpty);
        tvSummary = findViewById(R.id.tvSummary);
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
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, houseNames);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerHouse.setAdapter(spinnerAdapter);
        });

        spinnerHouse.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedHouseId = null;
                } else {
                    selectedHouseId = allHouses.get(position - 1).getId();
                }
                applyMonthFilterAndSummary();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedHouseId = null;
                applyMonthFilterAndSummary();
            }
        });

        btnPickMonth = findViewById(R.id.btnPickMonth);
        fabAdd = findViewById(R.id.fabAdd);
        pieChart = findViewById(R.id.pieChart);

        if (pieChart != null) {
            setupPieChart();
        }

        selectedMonth = FinancePeriodUtil
                .normalizeMonthYear(new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(new Date()));
        updateSelectedMonthLabel();

        RecyclerView rv = findViewById(R.id.recyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ExpenseAdapter(new ExpenseAdapter.OnItemActionListener() {
            @Override
            public void onEdit(Expense item) {
                showUpdateExpenseDialog(item);
            }

            @Override
            public void onDelete(Expense item) {
                if (item == null || item.getId() == null)
                    return;
                new AlertDialog.Builder(ExpenseActivity.this)
                        .setTitle(getString(R.string.confirm_delete))
                        .setMessage(getString(R.string.delete_expense_question))
                        .setPositiveButton(getString(R.string.delete), (d, w) -> viewModel.deleteExpense(item.getId(),
                                () -> runOnUiThread(
                                        () -> Toast
                                                .makeText(ExpenseActivity.this, getString(R.string.deleted),
                                                        Toast.LENGTH_SHORT)
                                                .show()),
                                () -> runOnUiThread(() -> Toast
                                        .makeText(ExpenseActivity.this, getString(R.string.delete_failed),
                                                Toast.LENGTH_SHORT)
                                        .show())))
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show();
            }
        });
        rv.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);

        ensurePermissionsThenLoad();

        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> showAddExpenseDialog());
        }
        if (btnPickMonth != null) {
            btnPickMonth.setOnClickListener(v -> showMonthPickerDialog());
        }
    }

    private CollectionReference scopedCollection(String path) {
        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId != null && !tenantId.isEmpty()) {
            return db.collection("tenants").document(tenantId).collection(path);
        }
        return db.collection(path);
    }

    private void ensurePermissionsThenLoad() {
        String tenantId = TenantSession.getActiveTenantId();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (tenantId == null || tenantId.trim().isEmpty() || user == null) {
            // Legacy (no tenant): allow full access
            bindListObserver();
            return;
        }

        db.collection("tenants").document(tenantId)
                .collection("members").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String role = doc.getString("role");
                    if (TenantRoles.TENANT.equals(role)) {
                        Toast.makeText(this, getString(R.string.tenant_no_permission_view_expenses), Toast.LENGTH_SHORT)
                                .show();
                        finish();
                        return;
                    }

                    readOnly = false;
                    adapter.setReadOnly(false);

                    bindListObserver();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.failed_load_permissions), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void bindListObserver() {
        viewModel.getExpenseList().observe(this, list -> {
            allExpenses = list != null ? list : new ArrayList<>();
            applyMonthFilterAndSummary();
        });
    }

    private void updateSelectedMonthLabel() {
        if (tvSelectedMonth != null) {
            tvSelectedMonth.setText(getString(R.string.month) + selectedMonth);
        }
    }

    private void showMonthPickerDialog() {
        List<String> monthValues = new ArrayList<>();
        List<String> monthLabels = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            java.util.Calendar c = java.util.Calendar.getInstance();
            c.add(java.util.Calendar.MONTH, -i);
            int month = c.get(java.util.Calendar.MONTH) + 1;
            int year = c.get(java.util.Calendar.YEAR);
            String normalized = String.format(Locale.US, "%02d/%04d", month, year);
            monthValues.add(normalized);
            monthLabels.add(getString(R.string.month) + normalized);
        }

        int checked = Math.max(0, monthValues.indexOf(selectedMonth));
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.select_month))
                .setSingleChoiceItems(monthLabels.toArray(new String[0]), checked, (dialog, which) -> {
                    selectedMonth = monthValues.get(which);
                    updateSelectedMonthLabel();
                    applyMonthFilterAndSummary();
                    dialog.dismiss();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void applyMonthFilterAndSummary() {
        List<Expense> filtered = new ArrayList<>();
        for (Expense cp : allExpenses) {
            if (cp == null)
                continue;
            
            if (selectedHouseId != null && !selectedHouseId.equals(cp.getHouseId())) {
                continue;
            }

            String month = FinancePeriodUtil.normalizeMonthYear(cp.getPaidAt());
            if (selectedMonth.equals(month)) {
                filtered.add(cp);
            }
        }

        adapter.setDataList(filtered);
        tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        updateSummary(filtered);
    }

    private void setupPieChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 5, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(android.graphics.Color.WHITE);
        pieChart.setTransparentCircleColor(android.graphics.Color.WHITE);
        pieChart.setTransparentCircleAlpha(110);
        pieChart.setHoleRadius(50f);
        pieChart.setTransparentCircleRadius(55f);
        pieChart.setDrawCenterText(true);
        pieChart.setRotationAngle(0f);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);

        Legend l = pieChart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.CENTER);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        l.setOrientation(Legend.LegendOrientation.VERTICAL);
        l.setDrawInside(false);
        l.setXEntrySpace(7f);
        l.setYEntrySpace(0f);
        l.setYOffset(0f);

        pieChart.setEntryLabelColor(android.graphics.Color.WHITE);
        pieChart.setEntryLabelTextSize(10f);
    }

    private void updateSummary(List<Expense> list) {
        if (tvSummary == null)
            return;
        double total = 0;
        int count = 0;
        Map<String, Double> byCategory = new HashMap<>();
        if (list != null) {
            for (Expense cp : list) {
                if (cp == null)
                    continue;
                total += cp.getAmount();
                count++;

                String category = cp.getCategory() != null && !cp.getCategory().trim().isEmpty()
                        ? cp.getCategory().trim()
                        : getString(R.string.other);
                byCategory.put(category, byCategory.getOrDefault(category, 0.0) + cp.getAmount());
            }
        }

        NumberFormat fmt = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
        tvSummary.setText(fmt.format(total));

        if (pieChart != null) {
            if (total == 0) {
                pieChart.clear();
                return;
            }

            ArrayList<PieEntry> entries = new ArrayList<>();
            for (Map.Entry<String, Double> entry : byCategory.entrySet()) {
                entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
            }

            PieDataSet dataSet = new PieDataSet(entries, "");
            dataSet.setDrawIcons(false);
            dataSet.setSliceSpace(3f);
            dataSet.setSelectionShift(5f);

            ArrayList<Integer> colors = new ArrayList<>();
            for (int c : ColorTemplate.MATERIAL_COLORS)
                colors.add(c);
            for (int c : ColorTemplate.JOYFUL_COLORS)
                colors.add(c);
            for (int c : ColorTemplate.COLORFUL_COLORS)
                colors.add(c);
            dataSet.setColors(colors);

            PieData data = new PieData(dataSet);
            data.setValueFormatter(new PercentFormatter(pieChart));
            data.setValueTextSize(12f);
            data.setValueTextColor(android.graphics.Color.WHITE);
            pieChart.setData(data);
            pieChart.highlightValues(null);
            pieChart.invalidate();
        }
    }

    private void showAddExpenseDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_expense, null);
        EditText etCategory = dialogView.findViewById(R.id.etCategory);
        EditText etAmount = dialogView.findViewById(R.id.etAmount);
        EditText etPaidAt = dialogView.findViewById(R.id.etPaidAt);
        EditText etNote = dialogView.findViewById(R.id.etNote);

        // Apply money formatter
        MoneyFormatter.applyTo(etAmount);

        etPaidAt.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.add_expense))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.save), (d, w) -> {
                    try {
                        String cat = etCategory.getText().toString().trim();
                        double amount = MoneyFormatter.getValue(etAmount);
                        String paidAt = etPaidAt.getText().toString().trim();
                        String note = etNote.getText().toString().trim();

                        if (cat.isEmpty()) {
                            Toast.makeText(this, getString(R.string.please_enter_category), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (amount <= 0) {
                            Toast.makeText(this, getString(R.string.amount_must_be_positive), Toast.LENGTH_SHORT)
                                    .show();
                            return;
                        }
                        if (paidAt.isEmpty()) {
                            Toast.makeText(this, getString(R.string.please_enter_date), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Expense cp = new Expense();
                        cp.setCategory(cat);
                        cp.setAmount(amount);
                        cp.setPaidAt(paidAt);
                        cp.setNote(note);
                        cp.setCreatedAt(Timestamp.now());

                        viewModel.addExpense(cp,
                                () -> runOnUiThread(() -> Toast
                                        .makeText(this, getString(R.string.saved), Toast.LENGTH_SHORT).show()),
                                () -> runOnUiThread(
                                        () -> Toast.makeText(this, getString(R.string.save_failed), Toast.LENGTH_SHORT)
                                                .show()));
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, getString(R.string.invalid_data), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showUpdateExpenseDialog(@NonNull Expense item) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_expense, null);
        EditText etCategory = dialogView.findViewById(R.id.etCategory);
        EditText etAmount = dialogView.findViewById(R.id.etAmount);
        EditText etPaidAt = dialogView.findViewById(R.id.etPaidAt);
        EditText etNote = dialogView.findViewById(R.id.etNote);

        // Apply money formatter
        MoneyFormatter.applyTo(etAmount);

        etCategory.setText(item.getCategory());
        MoneyFormatter.setValue(etAmount, item.getAmount());
        etPaidAt.setText(item.getPaidAt());
        etNote.setText(item.getNote());

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.update_expense))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.update), (d, w) -> {
                    try {
                        String cat = etCategory.getText().toString().trim();
                        double amount = MoneyFormatter.getValue(etAmount);
                        String paidAt = etPaidAt.getText().toString().trim();
                        String note = etNote.getText().toString().trim();

                        if (cat.isEmpty()) {
                            Toast.makeText(this, getString(R.string.please_enter_category), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (amount <= 0) {
                            Toast.makeText(this, getString(R.string.amount_must_be_positive), Toast.LENGTH_SHORT)
                                    .show();
                            return;
                        }
                        if (paidAt.isEmpty()) {
                            Toast.makeText(this, getString(R.string.please_enter_date), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Expense updated = new Expense();
                        updated.setId(item.getId());
                        updated.setCategory(cat);
                        updated.setAmount(amount);
                        updated.setPaidAt(paidAt);
                        updated.setNote(note);
                        updated.setCreatedAt(item.getCreatedAt());

                        viewModel.updateExpense(updated,
                                () -> runOnUiThread(
                                        () -> Toast.makeText(this, getString(R.string.updated), Toast.LENGTH_SHORT)
                                                .show()),
                                () -> runOnUiThread(
                                        () -> Toast
                                                .makeText(this, getString(R.string.update_failed), Toast.LENGTH_SHORT)
                                                .show()));
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, getString(R.string.invalid_data), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private double parseDouble(EditText et) {
        String s = et.getText().toString().trim();
        return s.isEmpty() ? 0 : Double.parseDouble(s);
    }

    private String formatDouble(double value) {
        return value % 1 == 0 ? String.valueOf((long) value) : String.valueOf(value);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
