package com.example.myapplication.features.property.room;

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
import androidx.annotation.NonNull;
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
import com.example.myapplication.core.service.ImageUploadService;
import com.example.myapplication.R;
import com.example.myapplication.core.constants.RoomStatus;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.MoneyFormatter;
import com.example.myapplication.features.contract.HopDongActivity;
import com.example.myapplication.domain.PhongTro;
import com.example.myapplication.viewmodel.PhongTroViewModel;
import com.google.android.material.button.MaterialButtonToggleGroup;

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

    private com.google.firebase.firestore.ListenerRegistration tenantsListener;

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
                });

        // === BROADCAST RECEIVER: Nhận kết quả upload từ Foreground Service ===
        uploadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String imageUrl = intent.getStringExtra(ImageUploadService.EXTRA_IMAGE_URL);
                if (imageUrl != null && pendingUploadPhong != null) {
                    pendingUploadPhong.setHinhAnh(imageUrl);
                    if (pendingUploadPhong.getId() != null) {
                        viewModel.capNhatPhong(pendingUploadPhong,
                                () -> runOnUiThread(() -> Toast
                                        .makeText(PhongTroActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT)
                                        .show()),
                                () -> runOnUiThread(() -> Toast
                                        .makeText(PhongTroActivity.this, "Cập nhật thất bại", Toast.LENGTH_SHORT)
                                        .show()));
                    } else {
                        viewModel.themPhong(pendingUploadPhong,
                                () -> runOnUiThread(() -> Toast
                                        .makeText(PhongTroActivity.this, "Thêm thành công!", Toast.LENGTH_SHORT)
                                        .show()),
                                () -> runOnUiThread(() -> Toast
                                        .makeText(PhongTroActivity.this, "Thất bại", Toast.LENGTH_LONG).show()));
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
            getSupportActionBar().setTitle("");
        }

        // Tự động đẩy Toolbar xuống để tránh bị Status Bar che
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        tvEmpty = findViewById(R.id.tvEmpty);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);

        String filterKhuId = getIntent().getStringExtra("KHU_ID");
        String khuName = getIntent().getStringExtra("KHU_NAME");
        String khuAddr = getIntent().getStringExtra("KHU_ADDR");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        TextView tvTitle1 = findViewById(R.id.tvTitleLine1);
        TextView tvTitle2 = findViewById(R.id.tvTitleLine2);
        if (tvTitle1 != null) {
            tvTitle1.setText("Danh sách phòng của nhà");
        }
        if (tvTitle2 != null) {
            String sub = (khuAddr != null && !khuAddr.trim().isEmpty()) ? khuAddr
                    : (khuName != null ? khuName : "");
            tvTitle2.setText(sub);
        }

        View btnHome = findViewById(R.id.btnHome);
        if (btnHome != null) {
            btnHome.setOnClickListener(v -> finish());
        }

        View btnAddRoom = findViewById(R.id.btnAddRoom);
        if (btnAddRoom != null) {
            btnAddRoom.setOnClickListener(v -> hienDialogThemPhong());
        }

        MaterialButtonToggleGroup toggle = findViewById(R.id.toggleRoomStatus);
        if (toggle != null) {
            toggle.check(R.id.btnSegmentVacant);
        }

        adapter = new PhongTroAdapter(new PhongTroAdapter.OnItemActionListener() {
            @Override
            public void onXoa(PhongTro phong) {
                new AlertDialog.Builder(PhongTroActivity.this)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Xóa phòng " + phong.getSoPhong() + "?")
                        .setPositiveButton("Xóa", (d, w) -> {
                            String roomId = phong.getId();
                            if (roomId == null || roomId.trim().isEmpty()) {
                                Toast.makeText(PhongTroActivity.this, "Thiếu ID phòng", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            hasTenantsInRoom(roomId, true, has -> {
                                if (has) {
                                    Toast.makeText(PhongTroActivity.this, "Không thể xóa: phòng đang có người thuê",
                                            Toast.LENGTH_LONG).show();
                                    return;
                                }
                                viewModel.xoaPhong(roomId,
                                        () -> runOnUiThread(() -> Toast
                                                .makeText(PhongTroActivity.this, "Đã xóa", Toast.LENGTH_SHORT).show()),
                                        () -> runOnUiThread(() -> Toast
                                                .makeText(PhongTroActivity.this, "Xóa thất bại", Toast.LENGTH_SHORT)
                                                .show()));
                            });
                        })
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

            @Override
            public void onTaoHopDong(PhongTro phong) {
                Intent it = new Intent(PhongTroActivity.this, HopDongActivity.class);
                it.putExtra(HopDongActivity.EXTRA_PHONG_ID, phong.getId());
                startActivity(it);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        final java.util.concurrent.atomic.AtomicInteger tabIdx = new java.util.concurrent.atomic.AtomicInteger(0);
        if (toggle != null) {
            toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (!isChecked)
                    return;
                if (checkedId == R.id.btnSegmentVacant) {
                    tabIdx.set(0);
                } else if (checkedId == R.id.btnSegmentRented) {
                    tabIdx.set(1);
                }

                if (viewModel != null && viewModel.getDanhSachPhong().getValue() != null) {
                    applyFilters(viewModel.getDanhSachPhong().getValue(), filterKhuId, tabIdx.get());
                }
            });
        }

        viewModel = new ViewModelProvider(this).get(PhongTroViewModel.class);
        // Listen tenants to enrich room list UI
        try {
            tenantsListener = scopedCollection("nguoi_thue")
                    .addSnapshotListener((snap, err) -> {
                        if (snap == null)
                            return;

                        java.util.Map<String, java.util.List<com.google.firebase.firestore.DocumentSnapshot>> byRoom = new java.util.HashMap<>();
                        for (com.google.firebase.firestore.DocumentSnapshot d : snap.getDocuments()) {
                            String roomId = d.getString("idPhong");
                            if (roomId == null || roomId.trim().isEmpty())
                                continue;
                            java.util.List<com.google.firebase.firestore.DocumentSnapshot> arr = byRoom.get(roomId);
                            if (arr == null) {
                                arr = new java.util.ArrayList<>();
                                byRoom.put(roomId, arr);
                            }
                            arr.add(d);
                        }

                        java.util.Map<String, String> out = new java.util.HashMap<>();
                        for (java.util.Map.Entry<String, java.util.List<com.google.firebase.firestore.DocumentSnapshot>> e : byRoom
                                .entrySet()) {
                            java.util.List<com.google.firebase.firestore.DocumentSnapshot> arr = e.getValue();
                            if (arr == null || arr.isEmpty())
                                continue;
                            com.google.firebase.firestore.DocumentSnapshot first = arr.get(0);
                            String name = first.getString("hoTen");
                            String phone = first.getString("soDienThoai");
                            String base = (name != null ? name : "")
                                    + (phone != null && !phone.trim().isEmpty() ? (" - ĐT: " + phone) : "");
                            int extra = arr.size() - 1;
                            if (extra > 0) {
                                base = base + " ( +" + extra + ")";
                            }
                            out.put(e.getKey(), base.trim());
                        }
                        adapter.setTenantByRoomId(out);
                    });
        } catch (Exception ignore) {
        }

        viewModel.getDanhSachPhong().observe(this, list -> {
            if (list == null)
                return;
            applyFilters(list, filterKhuId, tabIdx.get());
        });
    }

    private void applyFilters(
            @androidx.annotation.NonNull java.util.List<com.example.myapplication.domain.PhongTro> list,
            String filterKhuId,
            int tabIndex) {
        java.util.List<com.example.myapplication.domain.PhongTro> out = new java.util.ArrayList<>();
        for (com.example.myapplication.domain.PhongTro p : list) {
            if (p == null)
                continue;
            if (filterKhuId != null && !filterKhuId.trim().isEmpty()) {
                if (p.getKhuId() == null || !filterKhuId.equals(p.getKhuId()))
                    continue;
            }
            boolean vacant = RoomStatus.VACANT.equals(p.getTrangThai());
            if (tabIndex == 0 && !vacant)
                continue;
            if (tabIndex == 1 && vacant)
                continue;
            out.add(p);
        }

        adapter.setDanhSach(out);
        tvEmpty.setVisibility(out.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private interface BoolCallback {
        void onResult(boolean value);
    }

    private com.google.firebase.firestore.CollectionReference scopedCollection(String collection) {
        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId != null && !tenantId.trim().isEmpty()) {
            return com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("tenants").document(tenantId).collection(collection);
        }

        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance()
                .getCurrentUser();
        if (user == null)
            throw new IllegalStateException("User not logged in");
        return com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid()).collection(collection);
    }

    private void hasTenantsInRoom(@NonNull String roomId, boolean failClosed, @NonNull BoolCallback cb) {
        com.google.firebase.firestore.CollectionReference col;
        try {
            col = scopedCollection("nguoi_thue");
        } catch (Exception e) {
            cb.onResult(failClosed);
            return;
        }

        col.whereEqualTo("idPhong", roomId)
                .limit(1)
                .get()
                .addOnSuccessListener(qs -> cb.onResult(qs != null && !qs.isEmpty()))
                .addOnFailureListener(e -> cb.onResult(failClosed));
    }

    private void enforceRentedIfHasTenants(@NonNull PhongTro phong, @NonNull Runnable onReady) {
        if (phong.getId() == null || phong.getId().trim().isEmpty()) {
            onReady.run();
            return;
        }
        if (!RoomStatus.VACANT.equals(phong.getTrangThai())) {
            onReady.run();
            return;
        }

        hasTenantsInRoom(phong.getId(), false, has -> {
            if (has) {
                phong.setTrangThai(RoomStatus.RENTED);
                Toast.makeText(this, "Phòng đang có người thuê, tự chuyển trạng thái sang 'Đã thuê'", Toast.LENGTH_LONG)
                        .show();
            }
            onReady.run();
        });
    }

    private void hienDialogThemPhong() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_them_phong, null);
        EditText etSoPhong = dialogView.findViewById(R.id.etSoPhong);
        EditText etDienTich = dialogView.findViewById(R.id.etDienTich);
        EditText etGiaThue = dialogView.findViewById(R.id.etGiaThue);
        Spinner spinnerKhu = dialogView.findViewById(R.id.spinnerKhuTro);
        Spinner spinnerLoai = dialogView.findViewById(R.id.spinnerLoaiPhong);
        Spinner spinnerTrangThai = dialogView.findViewById(R.id.spinnerTrangThai);

        java.util.List<String> khuIds = new java.util.ArrayList<>();
        java.util.List<String> khuLabels = new java.util.ArrayList<>();
        ArrayAdapter<String> khuAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, khuLabels);
        khuAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (spinnerKhu != null) {
            spinnerKhu.setAdapter(khuAdapter);
            loadKhuOptions(spinnerKhu, khuIds, khuLabels, null, khuAdapter);
        }

        // Image picker
        dialogImgPreview = dialogView.findViewById(R.id.imgPreview);
        selectedImageUri = null;
        dialogView.findViewById(R.id.btnChonAnh).setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        ArrayAdapter<String> loaiAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[] { "Đơn", "Đôi", "Ghép" });
        loaiAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLoai.setAdapter(loaiAdapter);

        ArrayAdapter<String> ttAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[] { "Trống", "Đã thuê" });
        ttAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTrangThai.setAdapter(ttAdapter);

        new AlertDialog.Builder(this)
                .setTitle("Thêm phòng mới")
                .setView(dialogView)
                .setPositiveButton("Thêm", (d, w) -> {
                    String soPhong = etSoPhong.getText().toString().trim();
                    String dienTichStr = etDienTich.getText().toString().trim();
                    double giaThue = MoneyFormatter.getValue(etGiaThue);
                    if (soPhong.isEmpty() || dienTichStr.isEmpty() || giaThue == 0) {
                        Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        PhongTro phong = new PhongTro(
                                soPhong,
                                spinnerLoai.getSelectedItem().toString(),
                                Double.parseDouble(dienTichStr),
                                giaThue,
                                spinnerTrangThai.getSelectedItem().toString());

                        if (spinnerKhu != null) {
                            int khuIdx = spinnerKhu.getSelectedItemPosition();
                            if (khuIdx >= 0 && khuIdx < khuIds.size()) {
                                phong.setKhuId(khuIds.get(khuIdx));
                                phong.setKhuTen(khuLabels.get(khuIdx));
                            }
                        }

                        ensureRoomQuotaThen(() -> {
                            if (selectedImageUri != null) {
                                uploadImageAndSave(phong);
                            } else {
                                viewModel.themPhong(phong,
                                        () -> runOnUiThread(() -> Toast
                                                .makeText(this, "Thêm thành công!", Toast.LENGTH_SHORT).show()),
                                        () -> runOnUiThread(() -> Toast.makeText(this,
                                                "Thất bại — kiểm tra kết nối Firebase", Toast.LENGTH_LONG).show()));
                            }
                        });
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Số liệu không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null).show();
    }

    private void ensureRoomQuotaThen(@NonNull Runnable onAllowed) {
        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId == null || tenantId.trim().isEmpty()) {
            onAllowed.run();
            return;
        }

        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore
                .getInstance();
        db.collection("tenants").document(tenantId).get()
                .addOnSuccessListener(tdoc -> {
                    Long maxRoomsL = tdoc.getLong("maxRooms");
                    int maxRooms = maxRoomsL != null ? maxRoomsL.intValue() : 50;

                    db.collection("tenants").document(tenantId).collection("phong_tro").get()
                            .addOnSuccessListener(qs -> {
                                int current = qs != null ? qs.size() : 0;
                                if (current >= maxRooms) {
                                    Toast.makeText(this, "Đã vượt giới hạn phòng (" + maxRooms + ")", Toast.LENGTH_LONG)
                                            .show();
                                    return;
                                }
                                onAllowed.run();
                            })
                            .addOnFailureListener(e -> onAllowed.run());
                })
                .addOnFailureListener(e -> onAllowed.run());
    }

    private void loadKhuOptions(@NonNull Spinner spinner, @NonNull java.util.List<String> ids,
            @NonNull java.util.List<String> labels,
            String selectedId, @NonNull android.widget.ArrayAdapter<String> adapter) {
        ids.clear();
        labels.clear();
        ids.add("");
        labels.add("(Không chọn)");
        adapter.notifyDataSetChanged();

        String tenantId = TenantSession.getActiveTenantId();
        com.google.firebase.firestore.CollectionReference col;
        if (tenantId != null && !tenantId.trim().isEmpty()) {
            col = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("tenants").document(tenantId).collection("khu_tro");
        } else {
            com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance()
                    .getCurrentUser();
            if (user == null)
                return;
            col = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(user.getUid()).collection("khu_tro");
        }

        col.get().addOnSuccessListener(qs -> {
            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : qs) {
                String name = doc.getString("tenKhu");
                if (name == null || name.trim().isEmpty())
                    name = doc.getId();
                ids.add(doc.getId());
                labels.add(name);
            }
            adapter.notifyDataSetChanged();

            if (selectedId != null && !selectedId.trim().isEmpty()) {
                for (int i = 0; i < ids.size(); i++) {
                    if (selectedId.equals(ids.get(i))) {
                        spinner.setSelection(i);
                        break;
                    }
                }
            }
        });
    }

    private void hienDialogSuaPhong(PhongTro phong) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_them_phong, null);
        EditText etSoPhong = dialogView.findViewById(R.id.etSoPhong);
        EditText etDienTich = dialogView.findViewById(R.id.etDienTich);
        EditText etGiaThue = dialogView.findViewById(R.id.etGiaThue);
        Spinner spinnerKhu = dialogView.findViewById(R.id.spinnerKhuTro);
        Spinner spinnerLoai = dialogView.findViewById(R.id.spinnerLoaiPhong);
        Spinner spinnerTrangThai = dialogView.findViewById(R.id.spinnerTrangThai);

        // Apply money formatter to price field
        MoneyFormatter.applyTo(etGiaThue);

        java.util.List<String> khuIds = new java.util.ArrayList<>();
        java.util.List<String> khuLabels = new java.util.ArrayList<>();
        ArrayAdapter<String> khuAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, khuLabels);
        khuAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (spinnerKhu != null) {
            spinnerKhu.setAdapter(khuAdapter);
            loadKhuOptions(spinnerKhu, khuIds, khuLabels, phong.getKhuId(), khuAdapter);
        }

        // Image picker
        dialogImgPreview = dialogView.findViewById(R.id.imgPreview);
        selectedImageUri = null;
        dialogView.findViewById(R.id.btnChonAnh).setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        // Load ảnh cũ nếu có
        if (phong.getHinhAnh() != null && !phong.getHinhAnh().isEmpty()) {
            Glide.with(this).load(phong.getHinhAnh()).centerCrop().into(dialogImgPreview);
        }

        String[] loaiOptions = { "Đơn", "Đôi", "Ghép" };
        ArrayAdapter<String> loaiAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, loaiOptions);
        loaiAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLoai.setAdapter(loaiAdapter);

        String[] ttOptions = { "Trống", "Đã thuê" };
        ArrayAdapter<String> ttAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, ttOptions);
        ttAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTrangThai.setAdapter(ttAdapter);

        etSoPhong.setText(phong.getSoPhong());
        etDienTich.setText(phong.getDienTich() % 1 == 0 ? String.valueOf((long) phong.getDienTich())
                : String.valueOf(phong.getDienTich()));
        MoneyFormatter.setValue(etGiaThue, phong.getGiaThue());
        for (int i = 0; i < loaiOptions.length; i++) {
            if (loaiOptions[i].equals(phong.getLoaiPhong())) {
                spinnerLoai.setSelection(i);
                break;
            }
        }
        for (int i = 0; i < ttOptions.length; i++) {
            if (ttOptions[i].equals(phong.getTrangThai())) {
                spinnerTrangThai.setSelection(i);
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Chỉnh sửa phòng " + phong.getSoPhong())
                .setView(dialogView)
                .setPositiveButton("Cập nhật", (d, w) -> {
                    String soPhong = etSoPhong.getText().toString().trim();
                    String dienTichStr = etDienTich.getText().toString().trim();
                    double giaThue = MoneyFormatter.getValue(etGiaThue);
                    if (soPhong.isEmpty() || dienTichStr.isEmpty() || giaThue == 0) {
                        Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        PhongTro updated = new PhongTro(soPhong,
                                spinnerLoai.getSelectedItem().toString(),
                                Double.parseDouble(dienTichStr),
                                giaThue,
                                spinnerTrangThai.getSelectedItem().toString());
                        updated.setId(phong.getId());

                        if (spinnerKhu != null) {
                            int khuIdx = spinnerKhu.getSelectedItemPosition();
                            if (khuIdx >= 0 && khuIdx < khuIds.size()) {
                                updated.setKhuId(khuIds.get(khuIdx));
                                updated.setKhuTen(khuLabels.get(khuIdx));
                            }
                        }

                        enforceRentedIfHasTenants(updated, () -> {
                            if (selectedImageUri != null) {
                                uploadImageAndSave(updated);
                            } else {
                                // Giữ ảnh cũ nếu không chọn ảnh mới
                                updated.setHinhAnh(phong.getHinhAnh());
                                viewModel.capNhatPhong(updated,
                                        () -> runOnUiThread(() -> Toast
                                                .makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()),
                                        () -> runOnUiThread(() -> Toast
                                                .makeText(this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show()));
                            }
                        });
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
        if (tenantsListener != null) {
            tenantsListener.remove();
            tenantsListener = null;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
