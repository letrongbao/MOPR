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
import com.example.myapplication.model.NguoiThue;
import com.example.myapplication.model.PhongTro;
import com.example.myapplication.viewmodel.NguoiThueViewModel;
import com.example.myapplication.viewmodel.PhongTroViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class NguoiThueActivity extends AppCompatActivity {

    private NguoiThueViewModel viewModel;
    private NguoiThueAdapter adapter;
    private TextView tvEmpty;
    private List<PhongTro> danhSachPhong = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 1. Làm trong suốt Status Bar giống HomeActivity
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        window.setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_nguoi_thue);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Quản lý người thuê");
        }

        // 2. Tự động thêm Padding cho Toolbar
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        tvEmpty = findViewById(R.id.tvEmpty);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        FloatingActionButton fabThem = findViewById(R.id.fabThem);

        adapter = new NguoiThueAdapter(new NguoiThueAdapter.OnItemActionListener() {
            @Override
            public void onXoa(NguoiThue nguoiThue) {
                new AlertDialog.Builder(NguoiThueActivity.this)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Xóa " + nguoiThue.getHoTen() + "?")
                        .setPositiveButton("Xóa", (d, w) ->
                                viewModel.xoaNguoiThue(nguoiThue.getId(),
                                        () -> runOnUiThread(() -> Toast.makeText(NguoiThueActivity.this, "Đã xóa", Toast.LENGTH_SHORT).show()),
                                        () -> runOnUiThread(() -> Toast.makeText(NguoiThueActivity.this, "Xóa thất bại", Toast.LENGTH_SHORT).show())))
                        .setNegativeButton("Hủy", null).show();
            }
            @Override
            public void onSua(NguoiThue nguoiThue) {
                hienDialogSuaNguoiThue(nguoiThue);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(NguoiThueViewModel.class);
        viewModel.getDanhSachNguoiThue().observe(this, list -> {
            adapter.setDanhSach(list);
            tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        });

        new ViewModelProvider(this).get(PhongTroViewModel.class)
                .getDanhSachPhong().observe(this, list -> danhSachPhong = list);

        fabThem.setOnClickListener(v -> hienDialogThemNguoiThue());
    }

    private void hienDialogThemNguoiThue() {
        if (danhSachPhong.isEmpty()) {
            Toast.makeText(this, "Vui lòng thêm phòng trước", Toast.LENGTH_SHORT).show();
            return;
        }
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_them_nguoi_thue, null);
        EditText etHoTen = dialogView.findViewById(R.id.etHoTen);
        EditText etCccd = dialogView.findViewById(R.id.etCccd);
        EditText etSdt = dialogView.findViewById(R.id.etSdt);
        Spinner spinnerPhong = dialogView.findViewById(R.id.spinnerPhong);
        EditText etSoThanhVien = dialogView.findViewById(R.id.etSoThanhVien);
        EditText etNgayBatDau = dialogView.findViewById(R.id.etNgayBatDau);
        EditText etNgayKetThuc = dialogView.findViewById(R.id.etNgayKetThuc);
        EditText etTienCoc = dialogView.findViewById(R.id.etTienCoc);

        String[] phongNames = danhSachPhong.stream()
                .map(p -> "Phòng " + p.getSoPhong()).toArray(String[]::new);
        ArrayAdapter<String> phongAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, phongNames);
        phongAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPhong.setAdapter(phongAdapter);

        new AlertDialog.Builder(this)
                .setTitle("Thêm người thuê")
                .setView(dialogView)
                .setPositiveButton("Thêm", (d, w) -> {
                    String hoTen = etHoTen.getText().toString().trim();
                    String cccd = etCccd.getText().toString().trim();
                    String sdt = etSdt.getText().toString().trim();
                    String ngayBD = etNgayBatDau.getText().toString().trim();
                    String ngayKT = etNgayKetThuc.getText().toString().trim();
                    String tienCocStr = etTienCoc.getText().toString().trim();
                    if (hoTen.isEmpty() || cccd.isEmpty() || sdt.isEmpty() || ngayBD.isEmpty() || ngayKT.isEmpty() || tienCocStr.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        PhongTro phongChon = danhSachPhong.get(spinnerPhong.getSelectedItemPosition());
                        String soTVStr = etSoThanhVien.getText().toString().trim();
                        NguoiThue nt = new NguoiThue(hoTen, cccd, sdt, phongChon.getId(),
                                soTVStr.isEmpty() ? 1 : Integer.parseInt(soTVStr),
                                ngayBD, ngayKT, Double.parseDouble(tienCocStr));
                        nt.setSoPhong(phongChon.getSoPhong());
                        viewModel.themNguoiThue(nt,
                                () -> runOnUiThread(() -> Toast.makeText(this, "Thêm thành công!", Toast.LENGTH_SHORT).show()),
                                () -> runOnUiThread(() -> Toast.makeText(this, "Thất bại — kiểm tra Firebase", Toast.LENGTH_LONG).show()));
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Số liệu không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null).show();
    }

    private void hienDialogSuaNguoiThue(NguoiThue nguoiThue) {
        if (danhSachPhong.isEmpty()) {
            Toast.makeText(this, "Không tải được danh sách phòng", Toast.LENGTH_SHORT).show();
            return;
        }
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_them_nguoi_thue, null);
        EditText etHoTen = dialogView.findViewById(R.id.etHoTen);
        EditText etCccd = dialogView.findViewById(R.id.etCccd);
        EditText etSdt = dialogView.findViewById(R.id.etSdt);
        Spinner spinnerPhong = dialogView.findViewById(R.id.spinnerPhong);
        EditText etSoThanhVien = dialogView.findViewById(R.id.etSoThanhVien);
        EditText etNgayBatDau = dialogView.findViewById(R.id.etNgayBatDau);
        EditText etNgayKetThuc = dialogView.findViewById(R.id.etNgayKetThuc);
        EditText etTienCoc = dialogView.findViewById(R.id.etTienCoc);

        String[] phongNames = danhSachPhong.stream()
                .map(p -> "Phòng " + p.getSoPhong()).toArray(String[]::new);
        ArrayAdapter<String> phongAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, phongNames);
        phongAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPhong.setAdapter(phongAdapter);

        etHoTen.setText(nguoiThue.getHoTen());
        etCccd.setText(nguoiThue.getCccd());
        etSdt.setText(nguoiThue.getSoDienThoai());
        etSoThanhVien.setText(String.valueOf(nguoiThue.getSoThanhVien()));
        etNgayBatDau.setText(nguoiThue.getNgayBatDauThue());
        etNgayKetThuc.setText(nguoiThue.getNgayKetThucHopDong());
        etTienCoc.setText(nguoiThue.getTienCoc() % 1 == 0 ? String.valueOf((long) nguoiThue.getTienCoc()) : String.valueOf(nguoiThue.getTienCoc()));
        for (int i = 0; i < danhSachPhong.size(); i++) {
            if (danhSachPhong.get(i).getId().equals(nguoiThue.getIdPhong())) {
                spinnerPhong.setSelection(i);
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Chỉnh sửa thông tin người thuê")
                .setView(dialogView)
                .setPositiveButton("Cập nhật", (d, w) -> {
                    String hoTen = etHoTen.getText().toString().trim();
                    String cccd = etCccd.getText().toString().trim();
                    String sdt = etSdt.getText().toString().trim();
                    String ngayBD = etNgayBatDau.getText().toString().trim();
                    String ngayKT = etNgayKetThuc.getText().toString().trim();
                    String tienCocStr = etTienCoc.getText().toString().trim();
                    if (hoTen.isEmpty() || cccd.isEmpty() || sdt.isEmpty() || ngayBD.isEmpty() || ngayKT.isEmpty() || tienCocStr.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        PhongTro phongChon = danhSachPhong.get(spinnerPhong.getSelectedItemPosition());
                        String soTVStr = etSoThanhVien.getText().toString().trim();
                        NguoiThue updated = new NguoiThue(hoTen, cccd, sdt, phongChon.getId(),
                                soTVStr.isEmpty() ? 1 : Integer.parseInt(soTVStr),
                                ngayBD, ngayKT, Double.parseDouble(tienCocStr));
                        updated.setSoPhong(phongChon.getSoPhong());
                        updated.setId(nguoiThue.getId());
                        viewModel.capNhatNguoiThue(updated,
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