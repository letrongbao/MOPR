package com.example.myapplication.features.finance;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.CheckBox;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.appbar.AppBarLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.constants.ExpenseStatus;
import com.example.myapplication.core.constants.TenantRoles;
import com.example.myapplication.core.repository.domain.RoomRepository;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.FinancePeriodUtil;
import com.example.myapplication.core.util.MoneyFormatter;
import com.example.myapplication.core.util.ScreenUiHelper;
import com.example.myapplication.domain.Expense;
import com.example.myapplication.domain.Room;
import com.example.myapplication.features.invoice.InvoiceFilterDialogHelper;
import com.example.myapplication.viewmodel.ExpenseViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

public class ExpenseActivity extends AppCompatActivity {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private ExpenseViewModel viewModel;
    private ExpenseAdapter adapter;

    private TextView tvEmpty;
    private View llEmpty;
    private TextView tvSelectedMonth;
    private TextView tvSelectedKhu;
    private TextView tvFilterSummary;
    private EditText etSearchExpense;
    private View btnSelectKhu;
    private View btnDatePicker;
    private FloatingActionButton fabAdd;
    private TabLayout tabLayoutExpense;

    private final RoomRepository roomRepository = new RoomRepository();

    private String selectedMonth;
    private String selectedHouseId;
    private String searchQuery;
    private int selectedTabIndex;
    private List<Expense> allExpenses = new ArrayList<>();
    private List<Room> allRooms = new ArrayList<>();
    private final Map<String, String> houseNameMap = new HashMap<>();

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
        llEmpty = findViewById(R.id.llEmpty);
        tvSelectedMonth = findViewById(R.id.tvSelectedMonth);
        tvSelectedKhu = findViewById(R.id.tvSelectedKhu);
        tvFilterSummary = findViewById(R.id.tvFilterSummary);
        etSearchExpense = findViewById(R.id.etSearchExpense);
        btnSelectKhu = findViewById(R.id.btnSelectKhu);
        btnDatePicker = findViewById(R.id.btnDatePicker);
        fabAdd = findViewById(R.id.fabAdd);
        tabLayoutExpense = findViewById(R.id.tabLayoutExpense);

        selectedMonth = FinancePeriodUtil
                .normalizeMonthYear(new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(new Date()));
        selectedHouseId = null;
        searchQuery = "";
        selectedTabIndex = ExpenseAdapter.TAB_PENDING;
        updateSelectedMonthLabel();
        if (tvSelectedKhu != null) {
            tvSelectedKhu.setText(getString(R.string.all_houses));
        }

        setupTabLayout();
        setupFilterListeners();

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

            @Override
            public void onMarkPaid(Expense item) {
                markExpenseAsPaid(item);
            }
        });
        rv.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);

        ensurePermissionsThenLoad();

        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> showAddExpenseDialog());
        }
    }

    private void setupFilterListeners() {
        if (btnDatePicker != null) {
            btnDatePicker.setOnClickListener(v -> showMonthPickerDialog());
        }

        if (btnSelectKhu != null) {
            btnSelectKhu.setOnClickListener(v -> showHouseFilterDialog());
        }

        if (etSearchExpense != null) {
            etSearchExpense.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s == null ? "" : s.toString().trim().toLowerCase(Locale.getDefault());
                    applyMonthFilterAndSummary();
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }
    }

    private void setupTabLayout() {
        if (tabLayoutExpense == null) {
            return;
        }
        tabLayoutExpense.removeAllTabs();
        tabLayoutExpense.addTab(tabLayoutExpense.newTab().setText(getString(R.string.expense_tab_pending)));
        tabLayoutExpense.addTab(tabLayoutExpense.newTab().setText(getString(R.string.expense_tab_paid)));
        tabLayoutExpense.selectTab(tabLayoutExpense.getTabAt(selectedTabIndex));
        tabLayoutExpense.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedTabIndex = tab.getPosition();
                applyMonthFilterAndSummary();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
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

                    adapter.setReadOnly(false);

                    bindListObserver();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.failed_load_permissions), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void bindListObserver() {
        roomRepository.getRoomList().observe(this, list -> {
            allRooms = list != null ? list : new ArrayList<>();
            rebuildHouseNameMap();
            adapter.setHouseNameMap(houseNameMap);
            updateSelectedHouseLabel();
            applyMonthFilterAndSummary();
        });

        viewModel.getExpenseList().observe(this, list -> {
            allExpenses = list != null ? list : new ArrayList<>();
            applyMonthFilterAndSummary();
        });
    }

    private void showHouseFilterDialog() {
        InvoiceFilterDialogHelper.showHouseFilterDialog(this, allRooms, selectedHouseId, (houseId, label) -> {
            selectedHouseId = houseId;
            if (tvSelectedKhu != null) {
                tvSelectedKhu.setText(label);
            }
            applyMonthFilterAndSummary();
        });
    }

    private void updateSelectedHouseLabel() {
        if (tvSelectedKhu == null) {
            return;
        }
        if (selectedHouseId == null || selectedHouseId.trim().isEmpty()) {
            tvSelectedKhu.setText(getString(R.string.all_houses));
            return;
        }

        for (Room room : allRooms) {
            if (room == null) {
                continue;
            }
            String houseId = room.getHouseId();
            if (houseId == null || !selectedHouseId.equals(houseId)) {
                continue;
            }

            String houseName = room.getHouseName();
            if (houseName == null || houseName.trim().isEmpty()) {
                houseName = getString(R.string.house);
            }
            tvSelectedKhu.setText(houseName);
            return;
        }

        selectedHouseId = null;
        tvSelectedKhu.setText(getString(R.string.all_houses));
    }

    private void updateSelectedMonthLabel() {
        if (tvSelectedMonth != null) {
            String[] parts = selectedMonth.split("/");
            if (parts.length == 2) {
                tvSelectedMonth.setText(Integer.parseInt(parts[0]) + "/" + parts[1]);
            } else {
                tvSelectedMonth.setText(selectedMonth);
            }
        }
    }

    private void showMonthPickerDialog() {
        InvoiceFilterDialogHelper.showMonthFilterDialog(
                this,
                selectedMonth,
                true,
                R.string.month_picker_expense_title,
                (period, month, year) -> {
            selectedMonth = period;
            updateSelectedMonthLabel();
            applyMonthFilterAndSummary();
        });
    }

    private void applyMonthFilterAndSummary() {
        List<Expense> monthAndSearchMatched = new ArrayList<>();
        List<Expense> filtered = new ArrayList<>();
        for (Expense cp : allExpenses) {
            if (cp == null)
                continue;
            String month = resolvePeriodMonth(cp);
            if (!selectedMonth.equals(month)) {
                continue;
            }

            if (selectedHouseId != null && !selectedHouseId.trim().isEmpty()) {
                String expenseHouseId = cp.getHouseId();
                if (expenseHouseId == null || !selectedHouseId.equals(expenseHouseId)) {
                    continue;
                }
            }

            if (!matchesSearch(cp, searchQuery)) {
                continue;
            }

            monthAndSearchMatched.add(cp);

            String status = normalizeExpenseStatus(cp.getStatus());
            if (selectedTabIndex == ExpenseAdapter.TAB_PENDING && !ExpenseStatus.PENDING.equals(status)) {
                continue;
            }
            if (selectedTabIndex == ExpenseAdapter.TAB_PAID && !ExpenseStatus.PAID.equals(status)) {
                continue;
            }

            if (cp.getStatus() == null || cp.getStatus().trim().isEmpty()) {
                cp.setStatus(status);
            }
            if (cp.getPeriodMonth() == null || cp.getPeriodMonth().trim().isEmpty()) {
                cp.setPeriodMonth(month);
            }

            filtered.add(cp);
        }

        adapter.setDataList(filtered);
        adapter.setCurrentTab(selectedTabIndex);
        if (llEmpty != null) {
            llEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        }
        tvEmpty.setText(selectedTabIndex == ExpenseAdapter.TAB_PENDING
                ? getString(R.string.expense_empty_pending)
                : getString(R.string.expense_empty_paid));

        double pendingTotal = 0;
        double paidTotal = 0;
        for (Expense item : monthAndSearchMatched) {
            if (item == null) {
                continue;
            }
            String st = normalizeExpenseStatus(item.getStatus());
            if (ExpenseStatus.PENDING.equals(st)) {
                pendingTotal += item.getAmount();
            } else {
                paidTotal += item.getAmount();
            }
        }

        if (tvFilterSummary != null) {
            tvFilterSummary.setText(getString(
                    R.string.expense_filter_summary,
                    filtered.size(),
                    MoneyFormatter.format(pendingTotal),
                    MoneyFormatter.format(paidTotal)));
        }
    }

    private boolean matchesSearch(@NonNull Expense expense, @NonNull String query) {
        if (query.isEmpty()) {
            return true;
        }

        String category = expense.getCategory() != null ? expense.getCategory().toLowerCase(Locale.getDefault()) : "";
        String note = expense.getNote() != null ? expense.getNote().toLowerCase(Locale.getDefault()) : "";
        String paidAt = expense.getPaidAt() != null ? expense.getPaidAt().toLowerCase(Locale.getDefault()) : "";
        String periodMonth = expense.getPeriodMonth() != null
                ? expense.getPeriodMonth().toLowerCase(Locale.getDefault())
                : "";
        String amountText = MoneyFormatter.format(expense.getAmount()).toLowerCase(Locale.getDefault());

        return category.contains(query)
                || note.contains(query)
                || paidAt.contains(query)
                || periodMonth.contains(query)
                || amountText.contains(query);
    }

    private void showAddExpenseDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_expense, null);
        TextView tvExpenseScope = dialogView.findViewById(R.id.tvExpenseScope);
        EditText etCategory = dialogView.findViewById(R.id.etCategory);
        EditText etAmount = dialogView.findViewById(R.id.etAmount);
        EditText etPaidAt = dialogView.findViewById(R.id.etPaidAt);
        CheckBox cbSplitByMonths = dialogView.findViewById(R.id.cbSplitByMonths);
        TextInputLayout tilStartMonth = dialogView.findViewById(R.id.tilStartMonth);
        TextInputLayout tilMonthsCount = dialogView.findViewById(R.id.tilMonthsCount);
        EditText etStartMonth = dialogView.findViewById(R.id.etStartMonth);
        EditText etMonthsCount = dialogView.findViewById(R.id.etMonthsCount);
        EditText etNote = dialogView.findViewById(R.id.etNote);

        // Apply money formatter
        MoneyFormatter.applyTo(etAmount);

        if (tvExpenseScope != null) {
            tvExpenseScope.setText(getCurrentExpenseScopeLabel());
        }

        etPaidAt.setText(resolveDefaultDateForSelectedMonth());
        setupExpenseDatePicker(etPaidAt);
        etStartMonth.setText(selectedMonth);

        cbSplitByMonths.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (tilStartMonth != null) {
                tilStartMonth.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
            if (tilMonthsCount != null) {
                tilMonthsCount.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
            etStartMonth.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            etMonthsCount.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.add_expense))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.save), (d, w) -> {
                    try {
                        String cat = etCategory.getText().toString().trim();
                        double amount = MoneyFormatter.getValue(etAmount);
                        String paidAt = etPaidAt.getText().toString().trim();
                        boolean splitByMonths = cbSplitByMonths.isChecked();
                        String startMonthText = etStartMonth.getText() != null
                            ? etStartMonth.getText().toString().trim()
                            : "";
                        String monthsCountText = etMonthsCount.getText() != null
                            ? etMonthsCount.getText().toString().trim()
                            : "";
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

                        List<String> targetHouseIds = resolveTargetHouseIdsForCreate();

                        if (splitByMonths) {
                            int monthsCount;
                            try {
                                monthsCount = Integer.parseInt(monthsCountText);
                            } catch (NumberFormatException e) {
                                Toast.makeText(this, getString(R.string.expense_invalid_month_count), Toast.LENGTH_SHORT)
                                        .show();
                                return;
                            }

                            if (monthsCount < 2) {
                                Toast.makeText(this, getString(R.string.expense_invalid_month_count), Toast.LENGTH_SHORT)
                                        .show();
                                return;
                            }

                            String startMonth = FinancePeriodUtil.normalizeMonthYear(startMonthText);
                            if (startMonth.isEmpty()) {
                                Toast.makeText(this, getString(R.string.expense_invalid_start_month), Toast.LENGTH_SHORT)
                                        .show();
                                return;
                            }

                            createSplitExpenses(cat, amount, paidAt, note, startMonth, monthsCount, targetHouseIds);
                            return;
                        }

                        createExpenses(cat, amount, paidAt, note, targetHouseIds);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, getString(R.string.invalid_data), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void createExpenses(@NonNull String category,
            double amount,
            @NonNull String paidAt,
            @NonNull String note,
            @NonNull List<String> targetHouseIds) {
        List<String> effectiveHouseIds = new ArrayList<>(targetHouseIds);
        if (effectiveHouseIds.isEmpty()) {
            effectiveHouseIds.add(null);
        }

        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failure = new AtomicInteger(0);
        int total = effectiveHouseIds.size();
        int houseCount = effectiveHouseIds.size();

        for (String houseId : effectiveHouseIds) {
            Expense cp = new Expense();
            cp.setCategory(category);
            cp.setAmount(amount);
            cp.setPaidAt(paidAt);
            cp.setPeriodMonth(resolvePeriodFromDateOrFallback(paidAt));
            cp.setStatus(selectedTabIndex == ExpenseAdapter.TAB_PENDING
                    ? ExpenseStatus.PENDING
                    : ExpenseStatus.PAID);
            cp.setHouseId((houseId == null || houseId.trim().isEmpty()) ? null : houseId);
            cp.setNote(note);
            cp.setCreatedAt(Timestamp.now());

            viewModel.addExpense(cp,
                    () -> runOnUiThread(() -> {
                        if (success.incrementAndGet() + failure.get() == total) {
                            onBatchCreateDone(success.get(), failure.get(), total, houseCount);
                        }
                    }),
                    () -> runOnUiThread(() -> {
                        if (failure.incrementAndGet() + success.get() == total) {
                            onBatchCreateDone(success.get(), failure.get(), total, houseCount);
                        }
                    }));
        }
    }

    private void createSplitExpenses(@NonNull String category,
            double totalAmount,
            @NonNull String paidAt,
            @NonNull String note,
            @NonNull String startMonth,
            int monthsCount,
            @NonNull List<String> targetHouseIds) {
        if (targetHouseIds.isEmpty()) {
            targetHouseIds = new ArrayList<>();
            targetHouseIds.add(null);
        }

        List<Expense> splitExpenses = new ArrayList<>();
        for (String expenseHouseId : targetHouseIds) {
            double amountPerMonth = totalAmount / monthsCount;
            double consumed = 0;

            for (int i = 0; i < monthsCount; i++) {
                Expense expense = new Expense();
                expense.setCategory(category);

                double thisAmount = (i == monthsCount - 1)
                        ? Math.max(0, totalAmount - consumed)
                        : Math.round(amountPerMonth);
                consumed += thisAmount;

                expense.setAmount(thisAmount);
                expense.setPaidAt(paidAt);
                expense.setPeriodMonth(addMonthsToPeriod(startMonth, i));
                expense.setStatus(ExpenseStatus.PAID);
                expense.setHouseId((expenseHouseId == null || expenseHouseId.trim().isEmpty())
                        ? null
                        : expenseHouseId);
                String splitNote = note;
                if (!splitNote.isEmpty()) {
                    splitNote = splitNote + "\n";
                }
                splitNote = splitNote + getString(R.string.expense_split_note_suffix, i + 1, monthsCount);
                expense.setNote(splitNote);
                expense.setCreatedAt(Timestamp.now());
                splitExpenses.add(expense);
            }
        }

        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failure = new AtomicInteger(0);
        int total = splitExpenses.size();

        for (Expense expense : splitExpenses) {
            viewModel.addExpense(expense,
                    () -> runOnUiThread(() -> {
                        if (success.incrementAndGet() + failure.get() == total) {
                            onSplitCreateDone(success.get(), failure.get(), total);
                        }
                    }),
                    () -> runOnUiThread(() -> {
                        if (failure.incrementAndGet() + success.get() == total) {
                            onSplitCreateDone(success.get(), failure.get(), total);
                        }
                    }));
        }
    }

    private void onSplitCreateDone(int success, int failure, int total) {
        if (failure == 0) {
            Toast.makeText(this, getString(R.string.expense_split_success, total), Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this,
                getString(R.string.expense_split_partial_result, success, failure),
                Toast.LENGTH_LONG).show();
    }

    private void onBatchCreateDone(int success, int failure, int total, int targetHouseCount) {
        if (failure == 0) {
            if (targetHouseCount > 1) {
                Toast.makeText(this, getString(R.string.expense_created_multi_house, total, targetHouseCount),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.saved), Toast.LENGTH_SHORT).show();
            }
            return;
        }

        Toast.makeText(this,
                getString(R.string.expense_split_partial_result, success, failure),
                Toast.LENGTH_LONG).show();
    }

    private void showUpdateExpenseDialog(@NonNull Expense item) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_expense, null);
        TextView tvExpenseScope = dialogView.findViewById(R.id.tvExpenseScope);
        EditText etCategory = dialogView.findViewById(R.id.etCategory);
        EditText etAmount = dialogView.findViewById(R.id.etAmount);
        EditText etPaidAt = dialogView.findViewById(R.id.etPaidAt);
        CheckBox cbSplitByMonths = dialogView.findViewById(R.id.cbSplitByMonths);
        TextInputLayout tilStartMonth = dialogView.findViewById(R.id.tilStartMonth);
        TextInputLayout tilMonthsCount = dialogView.findViewById(R.id.tilMonthsCount);
        EditText etStartMonth = dialogView.findViewById(R.id.etStartMonth);
        EditText etMonthsCount = dialogView.findViewById(R.id.etMonthsCount);
        EditText etNote = dialogView.findViewById(R.id.etNote);

        // Apply money formatter
        MoneyFormatter.applyTo(etAmount);

        if (tvExpenseScope != null) {
            tvExpenseScope.setText(getCurrentExpenseScopeLabel());
        }
        etCategory.setText(item.getCategory());
        MoneyFormatter.setValue(etAmount, item.getAmount());
        etPaidAt.setText(item.getPaidAt());
        setupExpenseDatePicker(etPaidAt);
        etNote.setText(item.getNote());
        cbSplitByMonths.setVisibility(View.GONE);
        if (tilStartMonth != null) {
            tilStartMonth.setVisibility(View.GONE);
        }
        if (tilMonthsCount != null) {
            tilMonthsCount.setVisibility(View.GONE);
        }
        etStartMonth.setVisibility(View.GONE);
        etMonthsCount.setVisibility(View.GONE);

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
                        updated.setPeriodMonth(resolvePeriodFromDateOrFallback(paidAt));
                        updated.setStatus(normalizeExpenseStatus(item.getStatus()));
                        updated.setHouseId(item.getHouseId());
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

    private void markExpenseAsPaid(@NonNull Expense item) {
        item.setStatus(ExpenseStatus.PAID);
        if (item.getPaidAt() == null || item.getPaidAt().trim().isEmpty()) {
            item.setPaidAt(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
        }
        if (item.getPeriodMonth() == null || item.getPeriodMonth().trim().isEmpty()) {
            item.setPeriodMonth(resolvePeriodFromDateOrFallback(item.getPaidAt()));
        }

        viewModel.updateExpense(item,
                () -> runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.expense_mark_paid_success), Toast.LENGTH_SHORT).show();
                    switchToPaidTab();
                }),
                () -> runOnUiThread(
                        () -> Toast.makeText(this, getString(R.string.update_failed), Toast.LENGTH_SHORT).show()));
    }

    private void switchToPaidTab() {
        selectedTabIndex = ExpenseAdapter.TAB_PAID;
        if (tabLayoutExpense != null) {
            TabLayout.Tab paidTab = tabLayoutExpense.getTabAt(ExpenseAdapter.TAB_PAID);
            if (paidTab != null) {
                paidTab.select();
                return;
            }
        }
        applyMonthFilterAndSummary();
    }

    private void rebuildHouseNameMap() {
        houseNameMap.clear();
        for (Room room : allRooms) {
            if (room == null || room.getHouseId() == null || room.getHouseId().trim().isEmpty()) {
                continue;
            }
            String houseName = room.getHouseName();
            if (houseName == null || houseName.trim().isEmpty()) {
                houseName = getString(R.string.house);
            }
            houseNameMap.put(room.getHouseId(), houseName);
        }
    }

    @NonNull
    private String getCurrentExpenseScopeLabel() {
        if (selectedHouseId == null || selectedHouseId.trim().isEmpty()) {
            return getString(R.string.expense_scope_all_houses);
        }
        String houseName = houseNameMap.get(selectedHouseId);
        if (houseName == null || houseName.trim().isEmpty()) {
            houseName = getString(R.string.house);
        }
        return getString(R.string.expense_scope_single_house, houseName);
    }

    @NonNull
    private List<String> resolveTargetHouseIdsForCreate() {
        if (selectedHouseId != null && !selectedHouseId.trim().isEmpty()) {
            List<String> one = new ArrayList<>();
            one.add(selectedHouseId);
            return one;
        }

        LinkedHashSet<String> houseIds = new LinkedHashSet<>();
        for (Room room : allRooms) {
            if (room == null || room.getHouseId() == null || room.getHouseId().trim().isEmpty()) {
                continue;
            }
            houseIds.add(room.getHouseId());
        }

        return new ArrayList<>(houseIds);
    }

    private void setupExpenseDatePicker(@NonNull EditText etPaidAt) {
        etPaidAt.setOnClickListener(v -> showExpenseDatePicker(etPaidAt));
    }

    private void showExpenseDatePicker(@NonNull EditText etPaidAt) {
        Calendar initial = resolveInitialExpenseDate(etPaidAt.getText() != null ? etPaidAt.getText().toString() : "");
        long initialSelection = toUtcDateSelection(
            initial.get(Calendar.YEAR),
            initial.get(Calendar.MONTH),
            initial.get(Calendar.DAY_OF_MONTH));

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.expense_pick_spending_date))
            .setSelection(initialSelection)
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null) {
                return;
            }

            Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            utc.setTimeInMillis(selection);

            Calendar local = Calendar.getInstance();
            local.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
            local.set(Calendar.MILLISECOND, 0);

            etPaidAt.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(local.getTime()));
        });
        picker.show(getSupportFragmentManager(), "expense_spending_date_picker");
    }

    @NonNull
    private Calendar resolveInitialExpenseDate(@NonNull String currentDateText) {
        Calendar parsed = parseDateOrNull(currentDateText.trim());
        if (parsed != null) {
            return parsed;
        }

        return resolveDefaultDateCalendarForSelectedMonth();
    }

    @NonNull
    private Calendar resolveDefaultDateCalendarForSelectedMonth() {
        Calendar selected = parseSelectedMonthOrNow();
        Calendar now = Calendar.getInstance();

        if (selected.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                && selected.get(Calendar.MONTH) == now.get(Calendar.MONTH)) {
            selected.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));
        } else {
            selected.set(Calendar.DAY_OF_MONTH, 1);
        }

        selected.set(Calendar.HOUR_OF_DAY, 0);
        selected.set(Calendar.MINUTE, 0);
        selected.set(Calendar.SECOND, 0);
        selected.set(Calendar.MILLISECOND, 0);
        return selected;
    }

    @NonNull
    private String resolveDefaultDateForSelectedMonth() {
        Calendar selected = resolveDefaultDateCalendarForSelectedMonth();
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selected.getTime());
    }

    private long toUtcDateSelection(int year, int zeroBasedMonth, int dayOfMonth) {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.clear();
        utc.set(year, zeroBasedMonth, dayOfMonth, 0, 0, 0);
        return utc.getTimeInMillis();
    }

    @NonNull
    private Calendar parseSelectedMonthOrNow() {
        String[] parts = selectedMonth.split("/");
        Calendar c = Calendar.getInstance();
        if (parts.length == 2) {
            try {
                int month = Integer.parseInt(parts[0]);
                int year = Integer.parseInt(parts[1]);
                c.set(Calendar.YEAR, year);
                c.set(Calendar.MONTH, month - 1);
                c.set(Calendar.DAY_OF_MONTH, 1);
                return c;
            } catch (NumberFormatException ignored) {
            }
        }
        return c;
    }

    private Calendar parseDateOrNull(@NonNull String text) {
        if (!text.matches("\\d{2}/\\d{2}/\\d{4}")) {
            return null;
        }
        try {
            String[] parts = text.split("/");
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);
            Calendar c = Calendar.getInstance();
            c.setLenient(false);
            c.set(Calendar.YEAR, year);
            c.set(Calendar.MONTH, month - 1);
            c.set(Calendar.DAY_OF_MONTH, day);
            c.getTime();
            return c;
        } catch (Exception ignored) {
            return null;
        }
    }

    @NonNull
    private String normalizeExpenseStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return ExpenseStatus.PAID;
        }
        if (ExpenseStatus.PENDING.equalsIgnoreCase(status.trim())) {
            return ExpenseStatus.PENDING;
        }
        return ExpenseStatus.PAID;
    }

    @NonNull
    private String resolvePeriodMonth(@NonNull Expense expense) {
        String period = FinancePeriodUtil.normalizeMonthYear(expense.getPeriodMonth());
        if (!period.isEmpty()) {
            return period;
        }
        return resolvePeriodFromDateOrFallback(expense.getPaidAt());
    }

    @NonNull
    private String resolvePeriodFromDateOrFallback(String paidAt) {
        String fromDate = FinancePeriodUtil.normalizeMonthYear(paidAt);
        if (!fromDate.isEmpty()) {
            return fromDate;
        }
        return selectedMonth;
    }

    @NonNull
    private String addMonthsToPeriod(@NonNull String startMonth, int offset) {
        String normalized = FinancePeriodUtil.normalizeMonthYear(startMonth);
        if (normalized.isEmpty()) {
            return startMonth;
        }

        String[] parts = normalized.split("/");
        if (parts.length != 2) {
            return normalized;
        }

        try {
            int month = Integer.parseInt(parts[0]);
            int year = Integer.parseInt(parts[1]);
            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR, year);
            c.set(Calendar.MONTH, month - 1);
            c.set(Calendar.DAY_OF_MONTH, 1);
            c.add(Calendar.MONTH, offset);
            return String.format(Locale.US, "%02d/%04d", c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR));
        } catch (NumberFormatException ignored) {
            return normalized;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
