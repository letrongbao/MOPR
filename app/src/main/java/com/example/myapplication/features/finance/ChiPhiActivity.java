package com.example.myapplication.features.finance;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.constants.TenantRoles;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.FinancePeriodUtil;
import com.example.myapplication.core.util.MoneyFormatter;
import com.example.myapplication.domain.ChiPhi;
import com.example.myapplication.viewmodel.ChiPhiViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChiPhiActivity extends AppCompatActivity {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private ChiPhiViewModel viewModel;
    private ChiPhiAdapter adapter;

    private TextView tvEmpty;
    private TextView tvSummary;
    private TextView tvSummaryCount;
    private TextView tvSummaryAvg;
    private TextView tvTopCategory;
    private TextView tvSelectedMonth;
    private TextView btnPickMonth;
    private FloatingActionButton fabAdd;

    private boolean readOnly;
    private String selectedMonth;
    private List<ChiPhi> allExpenses = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_chi_phi);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Quản lý chi");
        }

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        tvEmpty = findViewById(R.id.tvEmpty);
        tvSummary = findViewById(R.id.tvSummary);
        tvSummaryCount = findViewById(R.id.tvSummaryCount);
        tvSummaryAvg = findViewById(R.id.tvSummaryAvg);
        tvTopCategory = findViewById(R.id.tvTopCategory);
        tvSelectedMonth = findViewById(R.id.tvSelectedMonth);
        btnPickMonth = findViewById(R.id.btnPickMonth);
        fabAdd = findViewById(R.id.fabAdd);

        selectedMonth = FinancePeriodUtil
                .normalizeMonthYear(new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(new Date()));
        updateSelectedMonthLabel();

        RecyclerView rv = findViewById(R.id.recyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ChiPhiAdapter(new ChiPhiAdapter.OnItemActionListener() {
            @Override
            public void onEdit(ChiPhi item) {
                hienDialogCapNhat(item);
            }

            @Override
            public void onDelete(ChiPhi item) {
                if (item == null || item.getId() == null)
                    return;
                new AlertDialog.Builder(ChiPhiActivity.this)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Xóa khoản chi này?")
                        .setPositiveButton("Xóa", (d, w) -> viewModel.xoa(item.getId(),
                                () -> runOnUiThread(
                                        () -> Toast.makeText(ChiPhiActivity.this, "Đã xóa", Toast.LENGTH_SHORT).show()),
                                () -> runOnUiThread(() -> Toast
                                        .makeText(ChiPhiActivity.this, "Xóa thất bại", Toast.LENGTH_SHORT).show())))
                        .setNegativeButton("Hủy", null)
                        .show();
            }
        });
        rv.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(ChiPhiViewModel.class);

        ensurePermissionsThenLoad();

        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> hienDialogThem());
        }
        if (btnPickMonth != null) {
            btnPickMonth.setOnClickListener(v -> showMonthPickerDialog());
        }
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
                        Toast.makeText(this, "Tài khoản người thuê không có quyền xem quản lý chi", Toast.LENGTH_SHORT)
                                .show();
                        finish();
                        return;
                    }

                    readOnly = false;
                    adapter.setReadOnly(false);

                    bindListObserver();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Không tải được quyền truy cập", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void bindListObserver() {
        viewModel.getDanhSach().observe(this, list -> {
            allExpenses = list != null ? list : new ArrayList<>();
            applyMonthFilterAndSummary();
        });
    }

    private void updateSelectedMonthLabel() {
        if (tvSelectedMonth != null) {
            tvSelectedMonth.setText("Tháng " + selectedMonth);
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
            monthLabels.add("Tháng " + normalized);
        }

        int checked = Math.max(0, monthValues.indexOf(selectedMonth));
        new AlertDialog.Builder(this)
                .setTitle("Chọn tháng")
                .setSingleChoiceItems(monthLabels.toArray(new String[0]), checked, (dialog, which) -> {
                    selectedMonth = monthValues.get(which);
                    updateSelectedMonthLabel();
                    applyMonthFilterAndSummary();
                    dialog.dismiss();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void applyMonthFilterAndSummary() {
        List<ChiPhi> filtered = new ArrayList<>();
        for (ChiPhi cp : allExpenses) {
            if (cp == null)
                continue;
            String month = FinancePeriodUtil.normalizeMonthYear(cp.getPaidAt());
            if (selectedMonth.equals(month)) {
                filtered.add(cp);
            }
        }

        adapter.setDanhSach(filtered);
        tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        updateSummary(filtered);
    }

    private void updateSummary(List<ChiPhi> list) {
        if (tvSummary == null)
            return;

        double total = 0;
        int count = 0;
        Map<String, Double> byCategory = new HashMap<>();
        if (list != null) {
            for (ChiPhi cp : list) {
                if (cp == null)
                    continue;
                total += cp.getAmount();
                count++;

                String category = cp.getCategory() != null && !cp.getCategory().trim().isEmpty()
                        ? cp.getCategory().trim()
                        : "Khác";
                byCategory.put(category, byCategory.getOrDefault(category, 0.0) + cp.getAmount());
            }
        }

        double avg = count > 0 ? (total / count) : 0;
        String topCategory = "-";
        double topAmount = 0;
        for (Map.Entry<String, Double> entry : byCategory.entrySet()) {
            if (entry.getValue() > topAmount) {
                topAmount = entry.getValue();
                topCategory = entry.getKey();
            }
        }

        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        tvSummary.setText("Tổng chi: " + fmt.format(total));
        if (tvSummaryCount != null)
            tvSummaryCount.setText("Số khoản: " + count);
        if (tvSummaryAvg != null)
            tvSummaryAvg.setText("TB/khoản: " + fmt.format(avg));
        if (tvTopCategory != null) {
            if (count == 0) {
                tvTopCategory.setText("Chi nhiều nhất: -");
            } else {
                tvTopCategory.setText("Chi nhiều nhất: " + topCategory + " (" + fmt.format(topAmount) + ")");
            }
        }
    }

    private void hienDialogThem() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_them_chi_phi, null);
        EditText etCategory = dialogView.findViewById(R.id.etCategory);
        EditText etAmount = dialogView.findViewById(R.id.etAmount);
        EditText etPaidAt = dialogView.findViewById(R.id.etPaidAt);
        EditText etNote = dialogView.findViewById(R.id.etNote);

        // Apply money formatter
        MoneyFormatter.applyTo(etAmount);

        etPaidAt.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));

        new AlertDialog.Builder(this)
                .setTitle("Thêm khoản chi")
                .setView(dialogView)
                .setPositiveButton("Lưu", (d, w) -> {
                    try {
                        String cat = etCategory.getText().toString().trim();
                        double amount = MoneyFormatter.getValue(etAmount);
                        String paidAt = etPaidAt.getText().toString().trim();
                        String note = etNote.getText().toString().trim();

                        if (cat.isEmpty()) {
                            Toast.makeText(this, "Vui lòng nhập hạng mục", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (amount <= 0) {
                            Toast.makeText(this, "Số tiền phải > 0", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (paidAt.isEmpty()) {
                            Toast.makeText(this, "Vui lòng nhập ngày", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        ChiPhi cp = new ChiPhi();
                        cp.setCategory(cat);
                        cp.setAmount(amount);
                        cp.setPaidAt(paidAt);
                        cp.setNote(note);
                        cp.setCreatedAt(Timestamp.now());

                        viewModel.them(cp,
                                () -> runOnUiThread(() -> Toast.makeText(this, "Đã lưu", Toast.LENGTH_SHORT).show()),
                                () -> runOnUiThread(
                                        () -> Toast.makeText(this, "Lưu thất bại", Toast.LENGTH_SHORT).show()));
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Số liệu không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void hienDialogCapNhat(@NonNull ChiPhi item) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_them_chi_phi, null);
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
                .setTitle("Cập nhật khoản chi")
                .setView(dialogView)
                .setPositiveButton("Cập nhật", (d, w) -> {
                    try {
                        String cat = etCategory.getText().toString().trim();
                        double amount = MoneyFormatter.getValue(etAmount);
                        String paidAt = etPaidAt.getText().toString().trim();
                        String note = etNote.getText().toString().trim();

                        if (cat.isEmpty()) {
                            Toast.makeText(this, "Vui lòng nhập hạng mục", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (amount <= 0) {
                            Toast.makeText(this, "Số tiền phải > 0", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (paidAt.isEmpty()) {
                            Toast.makeText(this, "Vui lòng nhập ngày", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        ChiPhi updated = new ChiPhi();
                        updated.setId(item.getId());
                        updated.setCategory(cat);
                        updated.setAmount(amount);
                        updated.setPaidAt(paidAt);
                        updated.setNote(note);
                        updated.setCreatedAt(item.getCreatedAt());

                        viewModel.capNhat(updated,
                                () -> runOnUiThread(
                                        () -> Toast.makeText(this, "Đã cập nhật", Toast.LENGTH_SHORT).show()),
                                () -> runOnUiThread(
                                        () -> Toast.makeText(this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show()));
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Số liệu không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
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
