package com.example.myapplication.features.property.room;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.domain.MeterReading;
import com.example.myapplication.core.repository.domain.MeterReadingRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MeterReadingHistoryActivity extends AppCompatActivity {

    private final MeterReadingRepository repository = new MeterReadingRepository();
    private MeterReadingAdapter adapter;
    private TextView tvEmpty;

    private String roomId;
    private double lastElecEnd = 0;
    private double lastWaterEnd = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_meter_reading_history);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Lịch sử công tơ");
        }

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        roomId = getIntent().getStringExtra("PHONG_ID");
        if (roomId == null || roomId.trim().isEmpty()) {
            Toast.makeText(this, "Thiếu PHONG_ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvEmpty = findViewById(R.id.tvEmpty);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        FloatingActionButton fabThem = findViewById(R.id.fabThem);

        adapter = new MeterReadingAdapter(new MeterReadingAdapter.OnActionListener() {
            @Override
            public void onDelete(MeterReading reading) {
                new AlertDialog.Builder(MeterReadingHistoryActivity.this)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Xóa chỉ số kỳ " + safe(reading.getPeriod()) + "?")
                        .setPositiveButton("Xóa", (d, w) -> repository.delete(reading.getId(),
                                () -> runOnUiThread(() -> Toast.makeText(MeterReadingHistoryActivity.this, "Đã xóa", Toast.LENGTH_SHORT).show()),
                                () -> runOnUiThread(() -> Toast.makeText(MeterReadingHistoryActivity.this, "Xóa thất bại", Toast.LENGTH_SHORT).show())))
                        .setNegativeButton("Hủy", null)
                        .show();
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        repository.listByRoom(roomId).observe(this, list -> {
            if (list == null) return;
            List<MeterReading> sorted = new ArrayList<>(list);
            Collections.sort(sorted, (a, b) -> safe(a.getPeriodKey()).compareTo(safe(b.getPeriodKey())));
            Collections.reverse(sorted);
            adapter.setDanhSach(sorted);
            tvEmpty.setVisibility(sorted.isEmpty() ? View.VISIBLE : View.GONE);

            if (!sorted.isEmpty()) {
                MeterReading latest = sorted.get(0);
                lastElecEnd = latest.getElecEnd();
                lastWaterEnd = latest.getWaterEnd();
            }
        });

        fabThem.setOnClickListener(v -> showAddMeterReadingDialog());
    }

    private void showAddMeterReadingDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_finalize_meter_reading, null);
        EditText etThangNam = dialogView.findViewById(R.id.etThangNam);
        EditText etDienDau = dialogView.findViewById(R.id.etDienDau);
        EditText etDienCuoi = dialogView.findViewById(R.id.etDienCuoi);
        EditText etNuocDau = dialogView.findViewById(R.id.etNuocDau);
        EditText etNuocCuoi = dialogView.findViewById(R.id.etNuocCuoi);

        etThangNam.setText(new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(new Date()));

        etDienDau.setText(formatDouble(lastElecEnd));
        etNuocDau.setText(formatDouble(lastWaterEnd));
        etDienDau.setEnabled(false);
        etNuocDau.setEnabled(false);

        if (etDienCuoi.getText().toString().trim().isEmpty()) etDienCuoi.setText(formatDouble(lastElecEnd));
        if (etNuocCuoi.getText().toString().trim().isEmpty()) etNuocCuoi.setText(formatDouble(lastWaterEnd));

        new AlertDialog.Builder(this)
                .setTitle("Thêm chỉ số công tơ")
                .setView(dialogView)
                .setPositiveButton("Lưu", (d, w) -> {
                    try {
                        String period = etThangNam.getText().toString().trim();
                        String periodKey = toPeriodKey(period);
                        if (periodKey.isEmpty()) {
                            Toast.makeText(this, "Kỳ không hợp lệ", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        double elecStart = lastElecEnd;
                        double waterStart = lastWaterEnd;
                        double elecEnd = parseDouble(etDienCuoi);
                        double waterEnd = parseDouble(etNuocCuoi);

                        if (elecEnd < elecStart || waterEnd < waterStart) {
                            Toast.makeText(this, "Chỉ số cuối không được nhỏ hơn chỉ số đầu", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        MeterReading r = new MeterReading();
                        r.setRoomId(roomId);
                        r.setPeriod(period);
                        r.setPeriodKey(periodKey);
                        r.setElecStart(elecStart);
                        r.setElecEnd(elecEnd);
                        r.setWaterStart(waterStart);
                        r.setWaterEnd(waterEnd);

                        String docId = roomId + "_" + periodKey;
                        repository.createIfAbsent(docId, r,
                                () -> runOnUiThread(() -> Toast.makeText(this, "Đã lưu công tơ", Toast.LENGTH_SHORT).show()),
                                () -> runOnUiThread(() -> Toast.makeText(this, "Kỳ này đã tồn tại", Toast.LENGTH_SHORT).show()),
                                () -> runOnUiThread(() -> Toast.makeText(this, "Lưu thất bại", Toast.LENGTH_SHORT).show()));
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

    private String toPeriodKey(String period) {
        if (period == null) return "";
        String[] parts = period.trim().split("/");
        if (parts.length != 2) return "";
        try {
            int month = Integer.parseInt(parts[0]);
            int year = Integer.parseInt(parts[1]);
            if (month < 1 || month > 12) return "";
            return String.format(Locale.US, "%04d%02d", year, month);
        } catch (NumberFormatException e) {
            return "";
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}


