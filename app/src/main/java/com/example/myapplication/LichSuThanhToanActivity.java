package com.example.myapplication;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.model.Payment;
import com.example.myapplication.repository.PaymentRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LichSuThanhToanActivity extends AppCompatActivity {

    private final PaymentRepository repository = new PaymentRepository();
    private PaymentAdapter adapter;
    private TextView tvEmpty;
    private TextView tvPaid;
    private TextView tvRemaining;

    private String invoiceId;
    private String roomId;
    private double invoiceTotal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_lich_su_thanh_toan);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Lịch sử thanh toán");
        }

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        invoiceId = getIntent().getStringExtra("INVOICE_ID");
        roomId = getIntent().getStringExtra("ROOM_ID");
        invoiceTotal = getIntent().getDoubleExtra("INVOICE_TOTAL", 0);
        String title = getIntent().getStringExtra("TITLE");
        if (title != null && getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }

        if (invoiceId == null || invoiceId.trim().isEmpty()) {
            Toast.makeText(this, "Thiếu INVOICE_ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvEmpty = findViewById(R.id.tvEmpty);
        tvPaid = findViewById(R.id.tvPaid);
        tvRemaining = findViewById(R.id.tvRemaining);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        FloatingActionButton fabThem = findViewById(R.id.fabThem);

        adapter = new PaymentAdapter(new PaymentAdapter.OnActionListener() {
            @Override
            public void onDelete(Payment payment) {
                new AlertDialog.Builder(LichSuThanhToanActivity.this)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Xóa thanh toán " + fmtMoney(payment.getAmount()) + "?")
                        .setPositiveButton("Xóa", (d, w) -> repository.delete(payment.getId(),
                                () -> runOnUiThread(() -> Toast.makeText(LichSuThanhToanActivity.this, "Đã xóa", Toast.LENGTH_SHORT).show()),
                                () -> runOnUiThread(() -> Toast.makeText(LichSuThanhToanActivity.this, "Xóa thất bại", Toast.LENGTH_SHORT).show())))
                        .setNegativeButton("Hủy", null)
                        .show();
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        repository.listByInvoice(invoiceId).observe(this, list -> {
            if (list == null) return;
            List<Payment> sorted = new ArrayList<>(list);
            Collections.sort(sorted, (a, b) -> Long.compare(parseDate(b.getPaidAt()), parseDate(a.getPaidAt())));
            adapter.setDanhSach(sorted);
            tvEmpty.setVisibility(sorted.isEmpty() ? View.VISIBLE : View.GONE);

            double paid = 0;
            for (Payment p : sorted) paid += p.getAmount();
            double remaining = Math.max(0, invoiceTotal - paid);

            tvPaid.setText("Đã thu: " + fmtMoney(paid));
            tvRemaining.setText("Còn lại: " + fmtMoney(remaining));
        });

        fabThem.setOnClickListener(v -> hienDialogThemThanhToan());
    }

    private void hienDialogThemThanhToan() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_them_thanh_toan, null);
        EditText etAmount = dialogView.findViewById(R.id.etAmount);
        Spinner spinnerMethod = dialogView.findViewById(R.id.spinnerMethod);
        EditText etPaidAt = dialogView.findViewById(R.id.etPaidAt);
        EditText etNote = dialogView.findViewById(R.id.etNote);

        etPaidAt.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));

        android.widget.ArrayAdapter<String> methodAdapter = new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"Tiền mặt", "Chuyển khoản"});
        methodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMethod.setAdapter(methodAdapter);

        new AlertDialog.Builder(this)
                .setTitle("Thêm thanh toán")
                .setView(dialogView)
                .setPositiveButton("Lưu", (d, w) -> {
                    try {
                        double amount = parseDouble(etAmount);
                        if (amount <= 0) {
                            Toast.makeText(this, "Số tiền phải > 0", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Payment p = new Payment();
                        p.setInvoiceId(invoiceId);
                        if (roomId != null && !roomId.trim().isEmpty()) p.setRoomId(roomId);
                        p.setAmount(amount);
                        p.setMethod(spinnerMethod.getSelectedItemPosition() == 0 ? "CASH" : "BANK");
                        p.setPaidAt(etPaidAt.getText().toString().trim());
                        p.setNote(etNote.getText().toString().trim());

                        repository.add(p,
                                () -> runOnUiThread(() -> Toast.makeText(this, "Đã lưu thanh toán", Toast.LENGTH_SHORT).show()),
                                () -> runOnUiThread(() -> Toast.makeText(this, "Lưu thất bại", Toast.LENGTH_SHORT).show()));
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private double parseDouble(EditText et) {
        String s = et.getText().toString().trim();
        return s.isEmpty() ? 0 : Double.parseDouble(s);
    }

    private long parseDate(String s) {
        if (s == null) return 0;
        try {
            Date d = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(s);
            return d != null ? d.getTime() : 0;
        } catch (ParseException e) {
            return 0;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private String fmtMoney(double v) {
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return fmt.format(v);
    }
}
