package com.example.myapplication;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.model.PhongTro;
import com.example.myapplication.viewmodel.PhongTroViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class PhongTroActivity extends AppCompatActivity {

    private PhongTroViewModel viewModel;
    private PhongTroAdapter adapter;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Làm trong suốt Status Bar giống HomeActivity
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        window.setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_phong_tro);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Quản lý phòng trọ");
        }

        // Tự động đẩy Toolbar xuống để tránh bị Status Bar che
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        tvEmpty = findViewById(R.id.tvEmpty);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        FloatingActionButton fabThem = findViewById(R.id.fabThem);

        adapter = new PhongTroAdapter(new PhongTroAdapter.OnItemActionListener() {
            @Override
            public void onXoa(PhongTro phong) {
                new AlertDialog.Builder(PhongTroActivity.this)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Xóa phòng " + phong.getSoPhong() + "?")
                        .setPositiveButton("Xóa", (d, w) ->
                                viewModel.xoaPhong(phong.getId(),
                                        () -> runOnUiThread(() -> Toast.makeText(PhongTroActivity.this, "Đã xóa", Toast.LENGTH_SHORT).show()),
                                        () -> runOnUiThread(() -> Toast.makeText(PhongTroActivity.this, "Xóa thất bại", Toast.LENGTH_SHORT).show())))
                        .setNegativeButton("Hủy", null).show();
            }

            @Override
            public void onChon(PhongTro phong) {
                hienDialogSuaPhong(phong);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(PhongTroViewModel.class);
        viewModel.getDanhSachPhong().observe(this, list -> {
            adapter.setDanhSach(list);
            tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        });

        fabThem.setOnClickListener(v -> hienDialogThemPhong());
    }

    private void hienDialogThemPhong() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_them_phong, null);
        EditText etSoPhong = dialogView.findViewById(R.id.etSoPhong);
        EditText etDienTich = dialogView.findViewById(R.id.etDienTich);
        EditText etGiaThue = dialogView.findViewById(R.id.etGiaThue);
        Spinner spinnerLoai = dialogView.findViewById(R.id.spinnerLoaiPhong);
        Spinner spinnerTrangThai = dialogView.findViewById(R.id.spinnerTrangThai);

        ArrayAdapter<String> loaiAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"Đơn", "Đôi", "Ghép"});
        loaiAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLoai.setAdapter(loaiAdapter);

        ArrayAdapter<String> ttAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"Trống", "Đã thuê"});
        ttAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTrangThai.setAdapter(ttAdapter);

        new AlertDialog.Builder(this)
                .setTitle("Thêm phòng mới")
                .setView(dialogView)
                .setPositiveButton("Thêm", (d, w) -> {
                    String soPhong = etSoPhong.getText().toString().trim();
                    String dienTichStr = etDienTich.getText().toString().trim();
                    String giaThueStr = etGiaThue.getText().toString().trim();
                    if (soPhong.isEmpty() || dienTichStr.isEmpty() || giaThueStr.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        PhongTro phong = new PhongTro(
                                soPhong,
                                spinnerLoai.getSelectedItem().toString(),
                                Double.parseDouble(dienTichStr),
                                Double.parseDouble(giaThueStr),
                                spinnerTrangThai.getSelectedItem().toString());
                        viewModel.themPhong(phong,
                                () -> runOnUiThread(() -> Toast.makeText(this, "Thêm thành công!", Toast.LENGTH_SHORT).show()),
                                () -> runOnUiThread(() -> Toast.makeText(this, "Thất bại — kiểm tra kết nối Firebase", Toast.LENGTH_LONG).show()));
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Số liệu không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null).show();
    }

    private void hienDialogSuaPhong(PhongTro phong) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_them_phong, null);
        EditText etSoPhong = dialogView.findViewById(R.id.etSoPhong);
        EditText etDienTich = dialogView.findViewById(R.id.etDienTich);
        EditText etGiaThue = dialogView.findViewById(R.id.etGiaThue);
        Spinner spinnerLoai = dialogView.findViewById(R.id.spinnerLoaiPhong);
        Spinner spinnerTrangThai = dialogView.findViewById(R.id.spinnerTrangThai);

        String[] loaiOptions = {"Đơn", "Đôi", "Ghép"};
        ArrayAdapter<String> loaiAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, loaiOptions);
        loaiAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLoai.setAdapter(loaiAdapter);

        String[] ttOptions = {"Trống", "Đã thuê"};
        ArrayAdapter<String> ttAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, ttOptions);
        ttAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTrangThai.setAdapter(ttAdapter);

        etSoPhong.setText(phong.getSoPhong());
        etDienTich.setText(phong.getDienTich() % 1 == 0 ? String.valueOf((long) phong.getDienTich()) : String.valueOf(phong.getDienTich()));
        etGiaThue.setText(phong.getGiaThue() % 1 == 0 ? String.valueOf((long) phong.getGiaThue()) : String.valueOf(phong.getGiaThue()));
        for (int i = 0; i < loaiOptions.length; i++) {
            if (loaiOptions[i].equals(phong.getLoaiPhong())) { spinnerLoai.setSelection(i); break; }
        }
        for (int i = 0; i < ttOptions.length; i++) {
            if (ttOptions[i].equals(phong.getTrangThai())) { spinnerTrangThai.setSelection(i); break; }
        }

        new AlertDialog.Builder(this)
                .setTitle("Chỉnh sửa phòng " + phong.getSoPhong())
                .setView(dialogView)
                .setPositiveButton("Cập nhật", (d, w) -> {
                    String soPhong = etSoPhong.getText().toString().trim();
                    String dienTichStr = etDienTich.getText().toString().trim();
                    String giaThueStr = etGiaThue.getText().toString().trim();
                    if (soPhong.isEmpty() || dienTichStr.isEmpty() || giaThueStr.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        PhongTro updated = new PhongTro(soPhong,
                                spinnerLoai.getSelectedItem().toString(),
                                Double.parseDouble(dienTichStr),
                                Double.parseDouble(giaThueStr),
                                spinnerTrangThai.getSelectedItem().toString());
                        updated.setId(phong.getId());
                        viewModel.capNhatPhong(updated,
                                () -> runOnUiThread(() -> Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()),
                                () -> runOnUiThread(() -> Toast.makeText(this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show()));
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Số liệu không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}