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
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.constants.TenantRoles;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.MoneyFormatter;
import com.example.myapplication.domain.ChiPhi;
import com.example.myapplication.viewmodel.ChiPhiViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChiPhiActivity extends AppCompatActivity {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private ChiPhiViewModel viewModel;
    private ChiPhiAdapter adapter;

    private TextView tvEmpty;
    private TextView tvSummary;
    private FloatingActionButton fabAdd;

    private boolean readOnly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.getDecorView()
                .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
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
        fabAdd = findViewById(R.id.fabAdd);

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
            adapter.setDanhSach(list);
            tvEmpty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
            updateSummary(list);
        });
    }

    private void updateSummary(List<ChiPhi> list) {
        if (tvSummary == null)
            return;
        String month = new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(new Date());
        double total = 0;
        if (list != null) {
            for (ChiPhi cp : list) {
                if (cp == null)
                    continue;
                String my = toMonthYear(cp.getPaidAt());
                if (month.equals(my))
                    total += cp.getAmount();
            }
        }
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        tvSummary.setText("Tổng chi tháng " + month + ": " + fmt.format(total));
    }

    private String toMonthYear(String paidAt) {
        if (paidAt == null)
            return "";
        String[] parts = paidAt.trim().split("/");
        if (parts.length == 3) {
            return parts[1] + "/" + parts[2];
        }
        if (parts.length == 2) {
            // in case user enters MM/yyyy
            return parts[0] + "/" + parts[1];
        }
        return "";
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
