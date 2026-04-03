package com.example.myapplication.features.property.room;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
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
import com.example.myapplication.core.util.ScreenUiHelper;
import com.example.myapplication.features.contract.ContractActivity;
import com.example.myapplication.domain.Room;
import com.example.myapplication.viewmodel.RoomViewModel;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButtonToggleGroup;

public class RoomActivity extends AppCompatActivity {

    private static final String CLOUD_NAME = "dsvkscwti";
    private static final String UPLOAD_PRESET = "MOPR";

    private RoomViewModel viewModel;
    private RoomAdapter adapter;
    private TextView tvEmpty;
    private Uri selectedImageUri;
    private ImageView dialogImgPreview;
    private ActivityResultLauncher<String> imagePickerLauncher;

    // Phòng đang chờ kết quả upload từ Foreground Service
    private Room pendingUploadPhong;
    private BroadcastReceiver uploadReceiver;

    private com.google.firebase.firestore.ListenerRegistration tenantsListener;

    private String presetHouseId;
    private String presetHouseName;
    private String presetHouseAddr;
    private String initialStatusFilter;

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
                        viewModel.updateRoom(pendingUploadPhong,
                                () -> runOnUiThread(() -> Toast
                                        .makeText(RoomActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT)
                                        .show()),
                                () -> runOnUiThread(() -> Toast
                                        .makeText(RoomActivity.this, "Cập nhật thất bại", Toast.LENGTH_SHORT)
                                        .show()));
                    } else {
                        viewModel.addRoom(pendingUploadPhong,
                                () -> runOnUiThread(() -> Toast
                                        .makeText(RoomActivity.this, "Thêm thành công!", Toast.LENGTH_SHORT)
                                        .show()),
                                () -> runOnUiThread(() -> Toast
                                        .makeText(RoomActivity.this, "Thất bại", Toast.LENGTH_LONG).show()));
                    }
                    pendingUploadPhong = null;
                }
            }
        };
        IntentFilter filter = new IntentFilter(ImageUploadService.ACTION_UPLOAD_COMPLETE);
        LocalBroadcastManager.getInstance(this).registerReceiver(uploadReceiver, filter);

        ScreenUiHelper.enableEdgeToEdge(this, false);

        setContentView(R.layout.activity_room);

        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            ScreenUiHelper.applyTopInset(appBarLayout);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        ScreenUiHelper.setupBackToolbar(this, toolbar, "");

        tvEmpty = findViewById(R.id.tvEmpty);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);

        presetHouseId = getIntent().getStringExtra("CAN_NHA_ID");
        presetHouseName = getIntent().getStringExtra("CAN_NHA_NAME");
        presetHouseAddr = getIntent().getStringExtra("CAN_NHA_ADDR");
        initialStatusFilter = getIntent().getStringExtra("FILTER_STATUS");
        final String finalFilterHouseId = presetHouseId;

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        TextView tvTitle1 = findViewById(R.id.tvTitleLine1);
        TextView tvTitle2 = findViewById(R.id.tvTitleLine2);
        if (tvTitle1 != null) {
            tvTitle1.setText("Danh sách phòng của nhà");
        }
        if (tvTitle2 != null) {
            String sub = (presetHouseAddr != null && !presetHouseAddr.trim().isEmpty()) ? presetHouseAddr
                    : (presetHouseName != null ? presetHouseName : "");
            tvTitle2.setText(sub);
        }

        View btnAddRoom = findViewById(R.id.btnAddRoom);
        if (btnAddRoom != null) {
            btnAddRoom.setOnClickListener(v -> showAddRoomDialog());
        }

        MaterialButtonToggleGroup toggle = findViewById(R.id.toggleRoomStatus);
        final int initialTabIndex = RoomStatus.RENTED.equals(initialStatusFilter) ? 1 : 0;
        if (toggle != null) {
            toggle.check(initialTabIndex == 1 ? R.id.btnSegmentRented : R.id.btnSegmentVacant);
        }

        adapter = new RoomAdapter(new RoomAdapter.OnItemActionListener() {
            @Override
            public void onXoa(Room phong) {
                new AlertDialog.Builder(RoomActivity.this)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Xóa phòng " + phong.getSoPhong() + "?")
                        .setPositiveButton("Xóa", (d, w) -> {
                            String roomId = phong.getId();
                            if (roomId == null || roomId.trim().isEmpty()) {
                                Toast.makeText(RoomActivity.this, "Thiếu ID phòng", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            hasTenantsInRoom(roomId, true, has -> {
                                if (has) {
                                    Toast.makeText(RoomActivity.this, "Không thể xóa: phòng đang có người thuê",
                                            Toast.LENGTH_LONG).show();
                                    return;
                                }
                                viewModel.deleteRoom(roomId,
                                        () -> runOnUiThread(() -> Toast
                                                .makeText(RoomActivity.this, "Đã xóa", Toast.LENGTH_SHORT).show()),
                                        () -> runOnUiThread(() -> Toast
                                                .makeText(RoomActivity.this, "Xóa thất bại", Toast.LENGTH_SHORT)
                                                .show()));
                            });
                        })
                        .setNegativeButton("Hủy", null).show();
            }

            @Override
            public void onChon(Room phong) {
                showEditRoomDialog(phong);
            }

            @Override
            public void onXemChiTiet(Room phong) {
                Intent intent = new Intent(RoomActivity.this, RoomDetailsActivity.class);
                intent.putExtra("PHONG_ID", phong.getId());
                startActivity(intent);
            }

            @Override
            public void onTaoHopDong(Room phong) {
                Intent it = new Intent(RoomActivity.this, ContractActivity.class);
                it.putExtra(ContractActivity.EXTRA_PHONG_ID, phong.getId());
                startActivity(it);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);

        final java.util.concurrent.atomic.AtomicInteger tabIdx = new java.util.concurrent.atomic.AtomicInteger(
                initialTabIndex);
        if (toggle != null) {
            toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (!isChecked)
                    return;
                if (checkedId == R.id.btnSegmentVacant) {
                    tabIdx.set(0);
                } else if (checkedId == R.id.btnSegmentRented) {
                    tabIdx.set(1);
                }

                if (viewModel != null && viewModel.getRoomList().getValue() != null) {
                    applyFilters(viewModel.getRoomList().getValue(), finalFilterHouseId, tabIdx.get());
                }
            });
        }

        viewModel = new ViewModelProvider(this).get(RoomViewModel.class);
        // Listen tenants to enrich room list UI
        try {
            tenantsListener = scopedCollection("contracts")
                    .whereEqualTo("trangThaiHopDong", "ACTIVE")
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

        viewModel.getRoomList().observe(this, list -> {
            if (list == null)
                return;
            applyFilters(list, finalFilterHouseId, tabIdx.get());
        });
    }

    private void applyFilters(
            @androidx.annotation.NonNull java.util.List<com.example.myapplication.domain.Room> list,
            String filterHouseId,
            int tabIndex) {
        java.util.List<com.example.myapplication.domain.Room> out = new java.util.ArrayList<>();
        for (com.example.myapplication.domain.Room p : list) {
            if (p == null)
                continue;
            if (filterHouseId != null && !filterHouseId.trim().isEmpty()) {
                if (p.getHouseId() == null || !filterHouseId.equals(p.getHouseId()))
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

    private String normalizeHouseId(String canNhaId) {
        return canNhaId == null ? "" : canNhaId.trim();
    }

    private boolean hasDuplicateRoomNumberInSameHouse(@NonNull String soPhong, String canNhaId,
            String excludeRoomId) {
        if (viewModel == null || viewModel.getRoomList().getValue() == null) {
            return false;
        }

        String targetSoPhong = soPhong.trim();
        String targetHouseId = normalizeHouseId(canNhaId);

        for (Room item : viewModel.getRoomList().getValue()) {
            if (item == null) {
                continue;
            }
            if (excludeRoomId != null && excludeRoomId.equals(item.getId())) {
                continue;
            }

            String itemSoPhong = item.getSoPhong() == null ? "" : item.getSoPhong().trim();
            String itemHouseId = normalizeHouseId(item.getHouseId());
            if (itemHouseId.equals(targetHouseId) && itemSoPhong.equalsIgnoreCase(targetSoPhong)) {
                return true;
            }
        }
        return false;
    }

    private void hasDuplicateRoomNumberInSameHouseRemote(
            @NonNull String soPhong,
            String canNhaId,
            String excludeRoomId,
            @NonNull BoolCallback cb) {
        com.google.firebase.firestore.Query q = scopedCollection("rooms")
                .whereEqualTo("soPhong", soPhong.trim());

        q.get()
                .addOnSuccessListener(qs -> {
                    String targetHouseId = normalizeHouseId(canNhaId);
                    boolean exists = false;
                    for (com.google.firebase.firestore.DocumentSnapshot doc : qs.getDocuments()) {
                        if (excludeRoomId != null && excludeRoomId.equals(doc.getId())) {
                            continue;
                        }
                        String docHouseId = normalizeHouseId(doc.getString("canNhaId"));
                        if (docHouseId.equals(targetHouseId)) {
                            exists = true;
                            break;
                        }
                    }
                    cb.onResult(exists);
                })
                .addOnFailureListener(e -> cb.onResult(false));
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
            col = scopedCollection("contracts");
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

    private void enforceRentedIfHasTenants(@NonNull Room phong, @NonNull Runnable onReady) {
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

    private void showAddRoomDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_room, null);
        EditText etSoPhong = dialogView.findViewById(R.id.etSoPhong);
        EditText etDienTich = dialogView.findViewById(R.id.etDienTich);
        EditText etGiaThue = dialogView.findViewById(R.id.etGiaThue);
        Spinner spinnerHouse = dialogView.findViewById(R.id.spinnerHouse);
        Spinner spinnerLoai = dialogView.findViewById(R.id.spinnerLoaiPhong);
        View layoutHouseField = dialogView.findViewById(R.id.layoutHouseField);

        MoneyFormatter.applyTo(etGiaThue);

        final boolean lockHouse = presetHouseId != null && !presetHouseId.trim().isEmpty();

        java.util.List<String> canNhaIds = new java.util.ArrayList<>();
        java.util.List<String> canNhaLabels = new java.util.ArrayList<>();
        ArrayAdapter<String> canNhaAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, canNhaLabels);
        canNhaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (spinnerHouse != null && !lockHouse) {
            spinnerHouse.setAdapter(canNhaAdapter);
            loadHouseOptions(spinnerHouse, canNhaIds, canNhaLabels, null, canNhaAdapter);
        }
        if (layoutHouseField != null && lockHouse) {
            layoutHouseField.setVisibility(View.GONE);
        }

        // Image picker
        dialogImgPreview = dialogView.findViewById(R.id.imgPreview);
        selectedImageUri = null;
        dialogView.findViewById(R.id.btnChonAnh).setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        ArrayAdapter<String> loaiAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[] { "Studio", "Duplex", "1 phòng ngủ", "2 phòng ngủ", "3 phòng ngủ" });
        loaiAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLoai.setAdapter(loaiAdapter);

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
                        String selectedHouseId = lockHouse ? presetHouseId.trim() : "";
                        String selectedHouseLabel = lockHouse
                                ? ((presetHouseName != null && !presetHouseName.trim().isEmpty()) ? presetHouseName
                                        : (presetHouseAddr != null ? presetHouseAddr : ""))
                                : "";
                        if (!lockHouse && spinnerHouse != null) {
                            int canNhaIdx = spinnerHouse.getSelectedItemPosition();
                            if (canNhaIdx >= 0 && canNhaIdx < canNhaIds.size()) {
                                selectedHouseId = canNhaIds.get(canNhaIdx);
                                selectedHouseLabel = canNhaLabels.get(canNhaIdx);
                            }
                        }
                        final String finalSelectedHouseId = selectedHouseId;
                        final String finalSelectedHouseLabel = selectedHouseLabel;

                        if (hasDuplicateRoomNumberInSameHouse(soPhong, finalSelectedHouseId, null)) {
                            Toast.makeText(this,
                                    "Số phòng đã tồn tại trong căn nhà đã chọn",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        hasDuplicateRoomNumberInSameHouseRemote(soPhong, finalSelectedHouseId, null, exists -> {
                            if (exists) {
                                Toast.makeText(this,
                                        "Số phòng đã tồn tại trong căn nhà đã chọn",
                                        Toast.LENGTH_LONG).show();
                                return;
                            }

                            Room phong = new Room(
                                    soPhong,
                                    spinnerLoai.getSelectedItem().toString(),
                                    Double.parseDouble(dienTichStr),
                                    giaThue,
                                    RoomStatus.VACANT);

                            if (!finalSelectedHouseId.isEmpty()) {
                                phong.setHouseId(finalSelectedHouseId);
                                phong.setHouseTen(finalSelectedHouseLabel);
                            }

                            ensureRoomQuotaThen(() -> {
                                if (selectedImageUri != null) {
                                    uploadImageAndSave(phong);
                                } else {
                                    viewModel.addRoom(phong,
                                            () -> runOnUiThread(() -> Toast
                                                    .makeText(this, "Thêm thành công!", Toast.LENGTH_SHORT).show()),
                                            () -> runOnUiThread(() -> Toast.makeText(this,
                                                    "Thất bại — kiểm tra kết nối Firebase", Toast.LENGTH_LONG)
                                                    .show()));
                                }
                            });
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

                    db.collection("tenants").document(tenantId).collection("rooms").get()
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

    private void loadHouseOptions(@NonNull Spinner spinner, @NonNull java.util.List<String> ids,
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
                    .collection("tenants").document(tenantId).collection("houses");
        } else {
            com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance()
                    .getCurrentUser();
            if (user == null)
                return;
            col = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(user.getUid()).collection("houses");
        }

        col.get().addOnSuccessListener(qs -> {
            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : qs) {
                String name = doc.getString("tenHouse");
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

    private void showEditRoomDialog(Room phong) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_room, null);
        EditText etSoPhong = dialogView.findViewById(R.id.etSoPhong);
        EditText etDienTich = dialogView.findViewById(R.id.etDienTich);
        EditText etGiaThue = dialogView.findViewById(R.id.etGiaThue);
        Spinner spinnerHouse = dialogView.findViewById(R.id.spinnerHouse);
        Spinner spinnerLoai = dialogView.findViewById(R.id.spinnerLoaiPhong);
        View layoutHouseField = dialogView.findViewById(R.id.layoutHouseField);

        final boolean lockHouse = presetHouseId != null && !presetHouseId.trim().isEmpty();

        // Apply money formatter to price field
        MoneyFormatter.applyTo(etGiaThue);

        java.util.List<String> canNhaIds = new java.util.ArrayList<>();
        java.util.List<String> canNhaLabels = new java.util.ArrayList<>();
        ArrayAdapter<String> canNhaAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, canNhaLabels);
        canNhaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (spinnerHouse != null && !lockHouse) {
            spinnerHouse.setAdapter(canNhaAdapter);
            loadHouseOptions(spinnerHouse, canNhaIds, canNhaLabels, phong.getHouseId(), canNhaAdapter);
        }
        if (layoutHouseField != null && lockHouse) {
            layoutHouseField.setVisibility(View.GONE);
        }

        // Image picker
        dialogImgPreview = dialogView.findViewById(R.id.imgPreview);
        selectedImageUri = null;
        dialogView.findViewById(R.id.btnChonAnh).setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        // Load ảnh cũ nếu có
        if (phong.getHinhAnh() != null && !phong.getHinhAnh().isEmpty()) {
            Glide.with(this).load(phong.getHinhAnh()).centerCrop().into(dialogImgPreview);
        }

        String[] loaiOptions = { "Studio", "Duplex", "1 phòng ngủ", "2 phòng ngủ", "3 phòng ngủ" };
        ArrayAdapter<String> loaiAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, loaiOptions);
        loaiAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLoai.setAdapter(loaiAdapter);

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
                        String selectedHouseId = lockHouse ? presetHouseId.trim() : "";
                        String selectedHouseLabel = lockHouse
                                ? ((presetHouseName != null && !presetHouseName.trim().isEmpty()) ? presetHouseName
                                        : (presetHouseAddr != null ? presetHouseAddr : ""))
                                : "";
                        if (!lockHouse && spinnerHouse != null) {
                            int canNhaIdx = spinnerHouse.getSelectedItemPosition();
                            if (canNhaIdx >= 0 && canNhaIdx < canNhaIds.size()) {
                                selectedHouseId = canNhaIds.get(canNhaIdx);
                                selectedHouseLabel = canNhaLabels.get(canNhaIdx);
                            }
                        }
                        final String finalSelectedHouseId = selectedHouseId;
                        final String finalSelectedHouseLabel = selectedHouseLabel;

                        if (hasDuplicateRoomNumberInSameHouse(soPhong, finalSelectedHouseId, phong.getId())) {
                            Toast.makeText(this,
                                    "Số phòng đã tồn tại trong căn nhà đã chọn",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        hasDuplicateRoomNumberInSameHouseRemote(soPhong, finalSelectedHouseId, phong.getId(),
                                exists -> {
                                    if (exists) {
                                        Toast.makeText(this,
                                                "Số phòng đã tồn tại trong căn nhà đã chọn",
                                                Toast.LENGTH_LONG).show();
                                        return;
                                    }

                                    Room updated = new Room(soPhong,
                                            spinnerLoai.getSelectedItem().toString(),
                                            Double.parseDouble(dienTichStr),
                                            giaThue,
                                            phong.getTrangThai());
                                    updated.setId(phong.getId());

                                    if (!finalSelectedHouseId.isEmpty()) {
                                        updated.setHouseId(finalSelectedHouseId);
                                        updated.setHouseTen(finalSelectedHouseLabel);
                                    }

                                    enforceRentedIfHasTenants(updated, () -> {
                                        if (selectedImageUri != null) {
                                            uploadImageAndSave(updated);
                                        } else {
                                            // Giữ ảnh cũ nếu không chọn ảnh mới
                                            updated.setHinhAnh(phong.getHinhAnh());
                                            viewModel.updateRoom(updated,
                                                    () -> runOnUiThread(() -> Toast
                                                            .makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT)
                                                            .show()),
                                                    () -> runOnUiThread(() -> Toast
                                                            .makeText(this, "Cập nhật thất bại", Toast.LENGTH_SHORT)
                                                            .show()));
                                        }
                                    });
                                });
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Số liệu không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null).show();
    }

    // === FOREGROUND SERVICE: Upload ảnh qua Service thay vì thread trực tiếp ===
    private void uploadImageAndSave(Room phong) {
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
