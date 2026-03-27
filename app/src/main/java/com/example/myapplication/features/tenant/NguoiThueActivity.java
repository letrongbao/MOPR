package com.example.myapplication.features.tenant;

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
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.constants.RoomStatus;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.MoneyFormatter;
import com.example.myapplication.domain.NguoiThue;
import com.example.myapplication.domain.PhongTro;
import com.example.myapplication.viewmodel.NguoiThueViewModel;
import com.example.myapplication.viewmodel.PhongTroViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class NguoiThueActivity extends AppCompatActivity {

    private String preselectRoomId;
    private boolean didAutoOpenAddDialog = false;

    private NguoiThueViewModel viewModel;
    private NguoiThueAdapter adapter;
    private TextView tvEmpty;
    private List<PhongTro> danhSachPhong = new ArrayList<>();

    private List<NguoiThue> lastTenants = new ArrayList<>();
    private boolean didAutoMigrateLinks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Làm trong suốt Status Bar giống HomeActivity
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        WindowCompat.setDecorFitsSystemWindows(window, false);
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
                final String roomId = nguoiThue != null ? nguoiThue.getIdPhong() : null;
                new AlertDialog.Builder(NguoiThueActivity.this)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Xóa " + nguoiThue.getHoTen() + "?")
                        .setPositiveButton("Xóa", (d, w) -> viewModel.xoaNguoiThue(nguoiThue.getId(),
                                () -> runOnUiThread(() -> {
                                    maybeMarkRoomVacant(roomId);
                                    Toast.makeText(NguoiThueActivity.this, "Đã xóa", Toast.LENGTH_SHORT).show();
                                }),
                                () -> runOnUiThread(() -> Toast
                                        .makeText(NguoiThueActivity.this, "Xóa thất bại", Toast.LENGTH_SHORT).show())))
                        .setNegativeButton("Hủy", null).show();
            }

            @Override
            public void onSua(NguoiThue nguoiThue) {
                hienDialogSuaNguoiThue(nguoiThue);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        preselectRoomId = getIntent().getStringExtra("PRESELECT_ROOM_ID");

        viewModel = new ViewModelProvider(this).get(NguoiThueViewModel.class);
        viewModel.getDanhSachNguoiThue().observe(this, list -> {
            adapter.setDanhSach(list);
            tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            lastTenants = list;
            maybeAutoMigrateTenantRoomLinks();
        });

        new ViewModelProvider(this).get(PhongTroViewModel.class)
                .getDanhSachPhong().observe(this, list -> {
                    danhSachPhong = list;
                    maybeAutoMigrateTenantRoomLinks();
                    maybeAutoOpenAddDialog();
                });

        fabThem.setOnClickListener(v -> hienDialogThemNguoiThue(null));
    }

    private void maybeAutoOpenAddDialog() {
        if (didAutoOpenAddDialog)
            return;
        if (preselectRoomId == null || preselectRoomId.trim().isEmpty())
            return;
        if (danhSachPhong == null || danhSachPhong.isEmpty())
            return;

        didAutoOpenAddDialog = true;
        String roomId = preselectRoomId;
        preselectRoomId = null;
        hienDialogThemNguoiThue(roomId);
    }

    private void hienDialogThemNguoiThue(String preselectRoomId) {
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

        // Apply money formatter
        MoneyFormatter.applyTo(etTienCoc);

        String[] phongNames = danhSachPhong.stream()
                .map(p -> "Phòng " + p.getSoPhong()).toArray(String[]::new);
        ArrayAdapter<String> phongAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, phongNames);
        phongAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPhong.setAdapter(phongAdapter);

        if (preselectRoomId != null) {
            for (int i = 0; i < danhSachPhong.size(); i++) {
                if (preselectRoomId.equals(danhSachPhong.get(i).getId())) {
                    spinnerPhong.setSelection(i);
                    break;
                }
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Thêm người thuê")
                .setView(dialogView)
                .setPositiveButton("Thêm", (d, w) -> {
                    String hoTen = etHoTen.getText().toString().trim();
                    String cccd = etCccd.getText().toString().trim();
                    String sdt = etSdt.getText().toString().trim();
                    String ngayBD = etNgayBatDau.getText().toString().trim();
                    String ngayKT = etNgayKetThuc.getText().toString().trim();
                    double tienCoc = MoneyFormatter.getValue(etTienCoc);
                    if (hoTen.isEmpty() || cccd.isEmpty() || sdt.isEmpty() || ngayBD.isEmpty() || ngayKT.isEmpty()
                            || tienCoc == 0) {
                        Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        int idx = spinnerPhong.getSelectedItemPosition();
                        if (idx < 0 || idx >= danhSachPhong.size()) {
                            Toast.makeText(this, "Vui lòng chọn phòng", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        PhongTro phongChon = danhSachPhong.get(idx);
                        final String roomId = phongChon.getId();
                        String soTVStr = etSoThanhVien.getText().toString().trim();
                        NguoiThue nt = new NguoiThue(hoTen, cccd, sdt, roomId,
                                soTVStr.isEmpty() ? 1 : Integer.parseInt(soTVStr),
                                ngayBD, ngayKT, tienCoc);
                        nt.setSoPhong(phongChon.getSoPhong());
                        viewModel.themNguoiThue(nt,
                                () -> runOnUiThread(() -> {
                                    markRoomRented(roomId);
                                    Toast.makeText(this, "Thêm thành công!", Toast.LENGTH_SHORT).show();
                                }),
                                () -> runOnUiThread(() -> Toast
                                        .makeText(this, "Thất bại — kiểm tra Firebase", Toast.LENGTH_LONG).show()));
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

        // Apply money formatter
        MoneyFormatter.applyTo(etTienCoc);

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
        MoneyFormatter.setValue(etTienCoc, nguoiThue.getTienCoc());
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
                    double tienCoc = MoneyFormatter.getValue(etTienCoc);
                    if (hoTen.isEmpty() || cccd.isEmpty() || sdt.isEmpty() || ngayBD.isEmpty() || ngayKT.isEmpty()
                            || tienCoc == 0) {
                        Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        int idx = spinnerPhong.getSelectedItemPosition();
                        if (idx < 0 || idx >= danhSachPhong.size()) {
                            Toast.makeText(this, "Vui lòng chọn phòng", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        final String oldRoomId = nguoiThue.getIdPhong();
                        PhongTro phongChon = danhSachPhong.get(idx);
                        final String newRoomId = phongChon.getId();
                        String soTVStr = etSoThanhVien.getText().toString().trim();
                        NguoiThue updated = new NguoiThue(hoTen, cccd, sdt, newRoomId,
                                soTVStr.isEmpty() ? 1 : Integer.parseInt(soTVStr),
                                ngayBD, ngayKT, tienCoc);
                        updated.setSoPhong(phongChon.getSoPhong());
                        updated.setId(nguoiThue.getId());
                        viewModel.capNhatNguoiThue(updated,
                                () -> runOnUiThread(() -> {
                                    markRoomRented(newRoomId);
                                    if (oldRoomId != null && !oldRoomId.equals(newRoomId)) {
                                        maybeMarkRoomVacant(oldRoomId);
                                    }
                                    Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                                }),
                                () -> runOnUiThread(
                                        () -> Toast.makeText(this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show()));
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Số liệu không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null).show();
    }

    private DocumentReference scopedDoc() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
            return null;

        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId != null && !tenantId.trim().isEmpty()) {
            return FirebaseFirestore.getInstance().collection("tenants").document(tenantId);
        }
        return FirebaseFirestore.getInstance().collection("users").document(user.getUid());
    }

    private void setRoomStatus(String roomId, String status) {
        if (roomId == null || roomId.trim().isEmpty())
            return;
        DocumentReference scope = scopedDoc();
        if (scope == null)
            return;

        scope.collection("phong_tro").document(roomId)
                .update("trangThai", status)
                .addOnFailureListener(e -> {
                    // ignore
                });
    }

    private void maybeAutoMigrateTenantRoomLinks() {
        if (didAutoMigrateLinks)
            return;
        if (danhSachPhong == null || danhSachPhong.isEmpty())
            return;
        if (lastTenants == null || lastTenants.isEmpty())
            return;

        didAutoMigrateLinks = true;
        TenantSession.init(this);

        Set<String> roomIds = new HashSet<>();
        Map<String, String> uniqueRoomIdBySoPhong = new HashMap<>();
        Set<String> dupSoPhong = new HashSet<>();
        for (PhongTro p : danhSachPhong) {
            if (p == null)
                continue;
            if (p.getId() != null)
                roomIds.add(p.getId());
            String so = p.getSoPhong();
            if (so == null || so.trim().isEmpty() || p.getId() == null)
                continue;
            if (uniqueRoomIdBySoPhong.containsKey(so)) {
                dupSoPhong.add(so);
            } else {
                uniqueRoomIdBySoPhong.put(so, p.getId());
            }
        }
        for (String dup : dupSoPhong)
            uniqueRoomIdBySoPhong.remove(dup);

        List<NguoiThue> toFix = new ArrayList<>();
        Map<String, String> newRoomIdByTenantId = new HashMap<>();
        for (NguoiThue n : lastTenants) {
            if (n == null)
                continue;
            if (n.getId() == null || n.getId().trim().isEmpty())
                continue;

            String currentRoomId = n.getIdPhong();
            boolean valid = currentRoomId != null && roomIds.contains(currentRoomId);
            if (valid)
                continue;

            String soPhong = n.getSoPhong();
            if (soPhong == null || soPhong.trim().isEmpty())
                continue;

            String mappedRoomId = uniqueRoomIdBySoPhong.get(soPhong);
            if (mappedRoomId == null || mappedRoomId.trim().isEmpty())
                continue;
            if (mappedRoomId.equals(currentRoomId))
                continue;

            toFix.add(n);
            newRoomIdByTenantId.put(n.getId(), mappedRoomId);
        }

        if (toFix.isEmpty())
            return;

        DocumentReference scope = scopedDoc();
        if (scope == null)
            return;

        AtomicInteger remaining = new AtomicInteger(toFix.size());
        AtomicInteger migrated = new AtomicInteger(0);

        for (NguoiThue n : toFix) {
            final String tenantId = n.getId();
            final String newRoomId = newRoomIdByTenantId.get(tenantId);
            if (newRoomId == null) {
                if (remaining.decrementAndGet() == 0 && migrated.get() > 0) {
                    Toast.makeText(this, "Đã đồng bộ " + migrated.get() + " người thuê về đúng phòng",
                            Toast.LENGTH_LONG).show();
                }
                continue;
            }

            scope.collection("nguoi_thue").document(tenantId)
                    .update("idPhong", newRoomId)
                    .addOnSuccessListener(v -> {
                        migrated.incrementAndGet();
                        markRoomRented(newRoomId);
                        if (remaining.decrementAndGet() == 0 && migrated.get() > 0) {
                            Toast.makeText(this, "Đã đồng bộ " + migrated.get() + " người thuê về đúng phòng",
                                    Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (remaining.decrementAndGet() == 0 && migrated.get() > 0) {
                            Toast.makeText(this, "Đã đồng bộ " + migrated.get() + " người thuê về đúng phòng",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void markRoomRented(String roomId) {
        setRoomStatus(roomId, RoomStatus.RENTED);
    }

    private void maybeMarkRoomVacant(String roomId) {
        if (roomId == null || roomId.trim().isEmpty())
            return;
        DocumentReference scope = scopedDoc();
        if (scope == null)
            return;

        scope.collection("nguoi_thue")
                .whereEqualTo("idPhong", roomId)
                .get()
                .addOnSuccessListener(qs -> {
                    if (qs == null || qs.isEmpty()) {
                        setRoomStatus(roomId, RoomStatus.VACANT);
                    }
                })
                .addOnFailureListener(e -> {
                    // ignore
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
