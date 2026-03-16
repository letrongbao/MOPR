package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.myapplication.model.PhongTro;
import com.example.myapplication.viewmodel.PhongTroViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class PhongTroActivity extends AppCompatActivity {

    // TODO: Thay bằng thông tin Cloudinary của bạn
    private static final String CLOUD_NAME = "dsvkscwti";
    private static final String UPLOAD_PRESET = "MOPR";

    private PhongTroViewModel viewModel;
    private PhongTroAdapter adapter;
    private TextView tvEmpty;
    private Uri selectedImageUri;
    private ImageView dialogImgPreview;
    private ActivityResultLauncher<String> imagePickerLauncher;

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

    private void uploadImageAndSave(PhongTro phong) {
        Toast.makeText(this, "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                // Đọc ảnh từ URI
                InputStream is = getContentResolver().openInputStream(selectedImageUri);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                is.close();
                byte[] imageBytes = baos.toByteArray();

                // Upload lên Cloudinary qua HTTP POST
                String boundary = "----Boundary" + System.currentTimeMillis();
                URL url = new URL("https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

                // Field: upload_preset
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n");
                dos.writeBytes(UPLOAD_PRESET + "\r\n");

                // Field: file (image)
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"image.jpg\"\r\n");
                dos.writeBytes("Content-Type: image/jpeg\r\n\r\n");
                dos.write(imageBytes);
                dos.writeBytes("\r\n");

                dos.writeBytes("--" + boundary + "--\r\n");
                dos.flush();
                dos.close();

                // Đọc response
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();

                    JSONObject json = new JSONObject(response.toString());
                    String imageUrl = json.getString("secure_url");

                    // Lưu URL vào phòng rồi save
                    phong.setHinhAnh(imageUrl);
                    runOnUiThread(() -> {
                        if (phong.getId() != null) {
                            viewModel.capNhatPhong(phong,
                                    () -> runOnUiThread(() -> Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()),
                                    () -> runOnUiThread(() -> Toast.makeText(this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show()));
                        } else {
                            viewModel.themPhong(phong,
                                    () -> runOnUiThread(() -> Toast.makeText(this, "Thêm thành công!", Toast.LENGTH_SHORT).show()),
                                    () -> runOnUiThread(() -> Toast.makeText(this, "Thất bại", Toast.LENGTH_LONG).show()));
                        }
                    });
                } else {
                    BufferedReader errReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    StringBuilder errResponse = new StringBuilder();
                    String errLine;
                    while ((errLine = errReader.readLine()) != null) errResponse.append(errLine);
                    errReader.close();
                    runOnUiThread(() -> Toast.makeText(this, "Upload thất bại: " + errResponse, Toast.LENGTH_LONG).show());
                }
                conn.disconnect();
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Tải ảnh thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
