package com.example.myapplication;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.myapplication.model.PhongTro;
import com.example.myapplication.viewmodel.PhongTroViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class PhongTroActivity extends AppCompatActivity {

    private static final String CLOUD_NAME = "dsvkscwti";
    private static final String UPLOAD_PRESET = "MOPR";

    private PhongTroViewModel viewModel;
    private PhongTroAdapter adapter;
    private TextView tvEmpty;
    private Uri selectedImageUri;
    private ImageView dialogImgPreview;
    private ActivityResultLauncher<String> imagePickerLauncher;

    // Phòng đang chờ kết quả upload từ Foreground Service
    private PhongTro pendingUploadPhong;
    private BroadcastReceiver uploadReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register image picker trước setContentView
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        if (dialogImgPreview != null) {
                            Glide.with(this).load(uri).centerCrop().into(dialogImgPreview);
                        }
                    }
                }
        );

        // === BROADCAST RECEIVER: Nhận kết quả upload từ Foreground Service ===
        uploadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String imageUrl = intent.getStringExtra(ImageUploadService.EXTRA_IMAGE_URL);
                if (imageUrl != null && pendingUploadPhong != null) {
                    pendingUploadPhong.setHinhAnh(imageUrl);
                    if (pendingUploadPhong.getId() != null) {
                        viewModel.capNhatPhong(pendingUploadPhong,
                                () -> runOnUiThread(() -> Toast.makeText(PhongTroActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()),
                                () -> runOnUiThread(() -> Toast.makeText(PhongTroActivity.this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show()));
                    } else {
                        viewModel.themPhong(pendingUploadPhong,
                                () -> runOnUiThread(() -> Toast.makeText(PhongTroActivity.this, "Thêm thành công!", Toast.LENGTH_SHORT).show()),
                                () -> runOnUiThread(() -> Toast.makeText(PhongTroActivity.this, "Thất bại", Toast.LENGTH_LONG).show()));
                    }
                    pendingUploadPhong = null;
                }
            }
        };
        IntentFilter filter = new IntentFilter(ImageUploadService.ACTION_UPLOAD_COMPLETE);
        LocalBroadcastManager.getInstance(this).registerReceiver(uploadReceiver, filter);

        // Làm trong suốt Status Bar giống HomeActivity
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
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

            @Override
            public void onXemChiTiet(PhongTro phong) {
                Intent intent = new Intent(PhongTroActivity.this, ChiTietPhongTroActivity.class);
                intent.putExtra("PHONG_ID", phong.getId());
                startActivity(intent);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(PhongTroViewModel.class);
        viewModel.getDanhSachPhong().observe(this, list -> {
            if (list == null) return;
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

        // Image picker
        dialogImgPreview = dialogView.findViewById(R.id.imgPreview);
        selectedImageUri = null;
        dialogView.findViewById(R.id.btnChonAnh).setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

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

                        if (selectedImageUri != null) {
                            uploadImageAndSave(phong);
                        } else {
                            viewModel.themPhong(phong,
                                    () -> runOnUiThread(() -> Toast.makeText(this, "Thêm thành công!", Toast.LENGTH_SHORT).show()),
                                    () -> runOnUiThread(() -> Toast.makeText(this, "Thất bại — kiểm tra kết nối Firebase", Toast.LENGTH_LONG).show()));
                        }
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

        // Image picker
        dialogImgPreview = dialogView.findViewById(R.id.imgPreview);
        selectedImageUri = null;
        dialogView.findViewById(R.id.btnChonAnh).setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        // Load ảnh cũ nếu có
        if (phong.getHinhAnh() != null && !phong.getHinhAnh().isEmpty()) {
            Glide.with(this).load(phong.getHinhAnh()).centerCrop().into(dialogImgPreview);
        }

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

                        if (selectedImageUri != null) {
                            uploadImageAndSave(updated);
                        } else {
                            // Giữ ảnh cũ nếu không chọn ảnh mới
                            updated.setHinhAnh(phong.getHinhAnh());
                            viewModel.capNhatPhong(updated,
                                    () -> runOnUiThread(() -> Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()),
                                    () -> runOnUiThread(() -> Toast.makeText(this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show()));
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Số liệu không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null).show();
    }

    // === FOREGROUND SERVICE: Upload ảnh qua Service thay vì thread trực tiếp ===
    private void uploadImageAndSave(PhongTro phong) {
        pendingUploadPhong = phong;
        Intent serviceIntent = new Intent(this, ImageUploadService.class);
        serviceIntent.putExtra(ImageUploadService.EXTRA_IMAGE_URI, selectedImageUri.toString());
        ContextCompat.startForegroundService(this, serviceIntent);
        Toast.makeText(this, "Đang upload ảnh qua Service...", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (uploadReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(uploadReceiver);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
