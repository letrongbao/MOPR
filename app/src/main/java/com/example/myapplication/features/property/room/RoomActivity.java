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

    // Internal note.
    private Room pendingUploadRoom;
    private BroadcastReceiver uploadReceiver;

    private com.google.firebase.firestore.ListenerRegistration tenantsListener;

    private String presetHouseId;
    private String presetHouseName;
    private String presetHouseAddr;
    private String initialStatusFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Internal note.
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

        // Internal note.
        uploadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String imageUrl = intent.getStringExtra(ImageUploadService.EXTRA_IMAGE_URL);
                if (imageUrl != null && pendingUploadRoom != null) {
                    pendingUploadRoom.setImageUrl(imageUrl);
                    if (pendingUploadRoom.getId() != null) {
                        viewModel.updateRoom(pendingUploadRoom,
                                () -> runOnUiThread(() -> Toast
                                        .makeText(RoomActivity.this, R.string.update_success, Toast.LENGTH_SHORT)
                                        .show()),
                                () -> runOnUiThread(() -> Toast
                                        .makeText(RoomActivity.this, R.string.update_failed, Toast.LENGTH_SHORT)
                                        .show()));
                    } else {
                        viewModel.addRoom(pendingUploadRoom,
                                () -> runOnUiThread(() -> Toast
                                        .makeText(RoomActivity.this, R.string.add_success, Toast.LENGTH_SHORT)
                                        .show()),
                                () -> runOnUiThread(() -> Toast
                                        .makeText(RoomActivity.this, R.string.operation_failed, Toast.LENGTH_LONG)
                                        .show()));
                    }
                    pendingUploadRoom = null;
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

        presetHouseId = getIntent().getStringExtra("HOUSE_ID");
        presetHouseName = getIntent().getStringExtra("HOUSE_NAME");
        presetHouseAddr = getIntent().getStringExtra("HOUSE_ADDRESS");
        initialStatusFilter = getIntent().getStringExtra("FILTER_STATUS");
        final String finalFilterHouseId = presetHouseId;

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        TextView tvTitle1 = findViewById(R.id.tvTitleLine1);
        TextView tvTitle2 = findViewById(R.id.tvTitleLine2);
        if (tvTitle1 != null) {
            tvTitle1.setText(R.string.room_list_of_house);
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
            public void onDelete(Room room) {
                new AlertDialog.Builder(RoomActivity.this)
                        .setTitle(R.string.confirm_delete)
                        .setMessage(getString(R.string.room_delete_confirm, room.getRoomNumber()))
                        .setPositiveButton(R.string.delete, (d, w) -> {
                            String roomId = room.getId();
                            if (roomId == null || roomId.trim().isEmpty()) {
                                Toast.makeText(RoomActivity.this, R.string.missing_room_id, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            hasTenantsInRoom(roomId, true, has -> {
                                if (has) {
                                    Toast.makeText(RoomActivity.this, R.string.room_delete_has_tenant,
                                            Toast.LENGTH_LONG).show();
                                    return;
                                }
                                viewModel.deleteRoom(roomId,
                                        () -> runOnUiThread(() -> Toast
                                                .makeText(RoomActivity.this, R.string.deleted, Toast.LENGTH_SHORT)
                                                .show()),
                                        () -> runOnUiThread(() -> Toast
                                                .makeText(RoomActivity.this, R.string.delete_failed,
                                                        Toast.LENGTH_SHORT)
                                                .show()));
                            });
                        })
                        .setNegativeButton(R.string.cancel, null).show();
            }

            @Override
            public void onSelect(Room room) {
                showEditRoomDialog(room);
            }

            @Override
            public void onViewDetails(Room room) {
                Intent intent = new Intent(RoomActivity.this, RoomDetailsActivity.class);
                intent.putExtra("ROOM_ID", room.getId());
                startActivity(intent);
            }

            @Override
            public void onCreateContract(Room room) {
                Intent it = new Intent(RoomActivity.this, ContractActivity.class);
                it.putExtra(ContractActivity.EXTRA_ROOM_ID, room.getId());
                startActivity(it);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(false);
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
                    .whereEqualTo("contractStatus", "ACTIVE")
                    .addSnapshotListener((snap, err) -> {
                        if (snap == null)
                            return;

                        java.util.Map<String, java.util.List<com.google.firebase.firestore.DocumentSnapshot>> byRoom = new java.util.HashMap<>();
                        for (com.google.firebase.firestore.DocumentSnapshot d : snap.getDocuments()) {
                            String roomId = d.getString("roomId");
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
                            String name = first.getString("fullName");
                            String phone = first.getString("phoneNumber");
                            String base = (name != null ? name : "")
                                    + (phone != null && !phone.trim().isEmpty()
                                            ? getString(R.string.room_tenant_phone_prefix, phone)
                                            : "");
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
            boolean vacant = RoomStatus.VACANT.equals(p.getStatus());
            if (tabIndex == 0 && !vacant)
                continue;
            if (tabIndex == 1 && vacant)
                continue;
            out.add(p);
        }

        adapter.setDataList(out);
        tvEmpty.setVisibility(out.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private interface BoolCallback {
        void onResult(boolean value);
    }

    private String normalizeHouseId(String houseId) {
        return houseId == null ? "" : houseId.trim();
    }

    private boolean hasDuplicateRoomNumberInSameHouse(@NonNull String roomNumber, String houseId,
            String excludeRoomId) {
        if (viewModel == null || viewModel.getRoomList().getValue() == null) {
            return false;
        }

        String targetRoomNumber = roomNumber.trim();
        String targetHouseId = normalizeHouseId(houseId);

        for (Room item : viewModel.getRoomList().getValue()) {
            if (item == null) {
                continue;
            }
            if (excludeRoomId != null && excludeRoomId.equals(item.getId())) {
                continue;
            }

            String itemRoomNumber = item.getRoomNumber() == null ? "" : item.getRoomNumber().trim();
            String itemHouseId = normalizeHouseId(item.getHouseId());
            if (itemHouseId.equals(targetHouseId) && itemRoomNumber.equalsIgnoreCase(targetRoomNumber)) {
                return true;
            }
        }
        return false;
    }

    private void hasDuplicateRoomNumberInSameHouseRemote(
            @NonNull String roomNumber,
            String houseId,
            String excludeRoomId,
            @NonNull BoolCallback cb) {
        com.google.firebase.firestore.Query q = scopedCollection("rooms")
                .whereEqualTo("roomNumber", roomNumber.trim());

        q.get()
                .addOnSuccessListener(qs -> {
                    String targetHouseId = normalizeHouseId(houseId);
                    boolean exists = false;
                    for (com.google.firebase.firestore.DocumentSnapshot doc : qs.getDocuments()) {
                        if (excludeRoomId != null && excludeRoomId.equals(doc.getId())) {
                            continue;
                        }
                        String docHouseId = normalizeHouseId(doc.getString("houseId"));
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

        col.whereEqualTo("roomId", roomId)
                .limit(1)
                .get()
                .addOnSuccessListener(qs -> cb.onResult(qs != null && !qs.isEmpty()))
                .addOnFailureListener(e -> cb.onResult(failClosed));
    }

    private void enforceRentedIfHasTenants(@NonNull Room room, @NonNull Runnable onReady) {
        if (room.getId() == null || room.getId().trim().isEmpty()) {
            onReady.run();
            return;
        }
        if (!RoomStatus.VACANT.equals(room.getStatus())) {
            onReady.run();
            return;
        }

        hasTenantsInRoom(room.getId(), false, has -> {
            if (has) {
                room.setStatus(RoomStatus.RENTED);
                Toast.makeText(this, R.string.room_status_auto_rented, Toast.LENGTH_LONG)
                        .show();
            }
            onReady.run();
        });
    }

    private void showAddRoomDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_room, null);
        EditText etRoomNumber = dialogView.findViewById(R.id.etSoPhong);
        EditText etDienTich = dialogView.findViewById(R.id.etDienTich);
        EditText etRentAmount = dialogView.findViewById(R.id.etGiaThue);
        EditText etMaxOccupancy = dialogView.findViewById(R.id.etSoNguoiToiDa);
        Spinner spinnerHouse = dialogView.findViewById(R.id.spinnerHouse);
        Spinner spinnerLoai = dialogView.findViewById(R.id.spinnerLoaiPhong);
        View layoutHouseField = dialogView.findViewById(R.id.layoutHouseField);

        MoneyFormatter.applyTo(etRentAmount);

        final boolean lockHouse = presetHouseId != null && !presetHouseId.trim().isEmpty();

        java.util.List<String> houseIds = new java.util.ArrayList<>();
        java.util.List<String> houseLabels = new java.util.ArrayList<>();
        ArrayAdapter<String> houseAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, houseLabels);
        houseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (spinnerHouse != null && !lockHouse) {
            spinnerHouse.setAdapter(houseAdapter);
            loadHouseOptions(spinnerHouse, houseIds, houseLabels, null, houseAdapter);
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
                getResources().getStringArray(R.array.room_type_options));
        loaiAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLoai.setAdapter(loaiAdapter);

        new AlertDialog.Builder(this)
                .setTitle(R.string.room_add_new_title)
                .setView(dialogView)
                .setPositiveButton(R.string.add, (d, w) -> {
                    String roomNumber = etRoomNumber.getText().toString().trim();
                    String dienTichStr = etDienTich.getText().toString().trim();
                    double rentAmount = MoneyFormatter.getValue(etRentAmount);
                    String maxOccupancyStr = etMaxOccupancy.getText().toString().trim();
                    if (roomNumber.isEmpty() || dienTichStr.isEmpty() || rentAmount == 0) {
                        Toast.makeText(this, R.string.please_fill_all_information, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int maxOccupancy;
                    try {
                        maxOccupancy = Integer.parseInt(maxOccupancyStr);
                        if (maxOccupancy <= 0) {
                            Toast.makeText(this, R.string.room_max_occupancy_required, Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } catch (Exception parseError) {
                        Toast.makeText(this, R.string.room_max_occupancy_required, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        String selectedHouseId = lockHouse ? presetHouseId.trim() : "";
                        String selectedHouseLabel = lockHouse
                                ? ((presetHouseName != null && !presetHouseName.trim().isEmpty()) ? presetHouseName
                                        : (presetHouseAddr != null ? presetHouseAddr : ""))
                                : "";
                        if (!lockHouse && spinnerHouse != null) {
                            int houseIndex = spinnerHouse.getSelectedItemPosition();
                            if (houseIndex >= 0 && houseIndex < houseIds.size()) {
                                selectedHouseId = houseIds.get(houseIndex);
                                selectedHouseLabel = houseLabels.get(houseIndex);
                            }
                        }
                        final String finalSelectedHouseId = selectedHouseId;
                        final String finalSelectedHouseLabel = selectedHouseLabel;

                        if (hasDuplicateRoomNumberInSameHouse(roomNumber, finalSelectedHouseId, null)) {
                            Toast.makeText(this, R.string.room_duplicate_in_house, Toast.LENGTH_LONG).show();
                            return;
                        }

                        hasDuplicateRoomNumberInSameHouseRemote(roomNumber, finalSelectedHouseId, null, exists -> {
                            if (exists) {
                                Toast.makeText(this, R.string.room_duplicate_in_house, Toast.LENGTH_LONG).show();
                                return;
                            }

                            Room room = new Room(
                                    roomNumber,
                                    spinnerLoai.getSelectedItem().toString(),
                                    Double.parseDouble(dienTichStr),
                                    rentAmount,
                                    RoomStatus.VACANT);
                            room.setMaxOccupancy(maxOccupancy);

                            if (!finalSelectedHouseId.isEmpty()) {
                                room.setHouseId(finalSelectedHouseId);
                                room.setHouseName(finalSelectedHouseLabel);
                            }

                            ensureRoomQuotaThen(() -> {
                                if (selectedImageUri != null) {
                                    uploadImageAndSave(room);
                                } else {
                                    viewModel.addRoom(room,
                                            () -> runOnUiThread(() -> Toast
                                                    .makeText(this, R.string.add_success, Toast.LENGTH_SHORT).show()),
                                            () -> runOnUiThread(() -> Toast.makeText(this,
                                                    R.string.room_add_failed_check_firebase, Toast.LENGTH_LONG)
                                                    .show()));
                                }
                            });
                        });
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, R.string.invalid_data, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null).show();
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
                                    Toast.makeText(this, getString(R.string.room_limit_exceeded, maxRooms),
                                            Toast.LENGTH_LONG)
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
        labels.add(getString(R.string.none_option));
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
                String name = doc.getString("houseName");
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

    private void showEditRoomDialog(Room room) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_room, null);
        EditText etRoomNumber = dialogView.findViewById(R.id.etSoPhong);
        EditText etDienTich = dialogView.findViewById(R.id.etDienTich);
        EditText etRentAmount = dialogView.findViewById(R.id.etGiaThue);
        EditText etMaxOccupancy = dialogView.findViewById(R.id.etSoNguoiToiDa);
        Spinner spinnerHouse = dialogView.findViewById(R.id.spinnerHouse);
        Spinner spinnerLoai = dialogView.findViewById(R.id.spinnerLoaiPhong);
        View layoutHouseField = dialogView.findViewById(R.id.layoutHouseField);

        final boolean lockHouse = presetHouseId != null && !presetHouseId.trim().isEmpty();

        // Apply money formatter to price field
        MoneyFormatter.applyTo(etRentAmount);

        java.util.List<String> houseIds = new java.util.ArrayList<>();
        java.util.List<String> houseLabels = new java.util.ArrayList<>();
        ArrayAdapter<String> houseAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, houseLabels);
        houseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (spinnerHouse != null && !lockHouse) {
            spinnerHouse.setAdapter(houseAdapter);
            loadHouseOptions(spinnerHouse, houseIds, houseLabels, room.getHouseId(), houseAdapter);
        }
        if (layoutHouseField != null && lockHouse) {
            layoutHouseField.setVisibility(View.GONE);
        }

        // Image picker
        dialogImgPreview = dialogView.findViewById(R.id.imgPreview);
        selectedImageUri = null;
        dialogView.findViewById(R.id.btnChonAnh).setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        // Internal note.
        if (room.getImageUrl() != null && !room.getImageUrl().isEmpty()) {
            Glide.with(this).load(room.getImageUrl()).centerCrop().into(dialogImgPreview);
        }

        String[] loaiOptions = getResources().getStringArray(R.array.room_type_options);
        ArrayAdapter<String> loaiAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, loaiOptions);
        loaiAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLoai.setAdapter(loaiAdapter);

        etRoomNumber.setText(room.getRoomNumber());
        etDienTich.setText(room.getArea() % 1 == 0 ? String.valueOf((long) room.getArea())
                : String.valueOf(room.getArea()));
        MoneyFormatter.setValue(etRentAmount, room.getRentAmount());
        etMaxOccupancy.setText(room.getMaxOccupancy() > 0 ? String.valueOf(room.getMaxOccupancy()) : "");
        for (int i = 0; i < loaiOptions.length; i++) {
            if (loaiOptions[i].equals(room.getRoomType())) {
                spinnerLoai.setSelection(i);
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.room_edit_title, room.getRoomNumber()))
                .setView(dialogView)
                .setPositiveButton(R.string.update, (d, w) -> {
                    String roomNumber = etRoomNumber.getText().toString().trim();
                    String dienTichStr = etDienTich.getText().toString().trim();
                    double rentAmount = MoneyFormatter.getValue(etRentAmount);
                    String maxOccupancyStr = etMaxOccupancy.getText().toString().trim();
                    if (roomNumber.isEmpty() || dienTichStr.isEmpty() || rentAmount == 0) {
                        Toast.makeText(this, R.string.please_fill_all_information, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int maxOccupancy;
                    try {
                        maxOccupancy = Integer.parseInt(maxOccupancyStr);
                        if (maxOccupancy <= 0) {
                            Toast.makeText(this, R.string.room_max_occupancy_required, Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } catch (Exception parseError) {
                        Toast.makeText(this, R.string.room_max_occupancy_required, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        String selectedHouseId = lockHouse ? presetHouseId.trim() : "";
                        String selectedHouseLabel = lockHouse
                                ? ((presetHouseName != null && !presetHouseName.trim().isEmpty()) ? presetHouseName
                                        : (presetHouseAddr != null ? presetHouseAddr : ""))
                                : "";
                        if (!lockHouse && spinnerHouse != null) {
                            int houseIndex = spinnerHouse.getSelectedItemPosition();
                            if (houseIndex >= 0 && houseIndex < houseIds.size()) {
                                selectedHouseId = houseIds.get(houseIndex);
                                selectedHouseLabel = houseLabels.get(houseIndex);
                            }
                        }
                        final String finalSelectedHouseId = selectedHouseId;
                        final String finalSelectedHouseLabel = selectedHouseLabel;

                        if (hasDuplicateRoomNumberInSameHouse(roomNumber, finalSelectedHouseId, room.getId())) {
                            Toast.makeText(this, R.string.room_duplicate_in_house, Toast.LENGTH_LONG).show();
                            return;
                        }

                        hasDuplicateRoomNumberInSameHouseRemote(roomNumber, finalSelectedHouseId, room.getId(),
                                exists -> {
                                    if (exists) {
                                        Toast.makeText(this, R.string.room_duplicate_in_house, Toast.LENGTH_LONG)
                                                .show();
                                        return;
                                    }

                                    Room updated = new Room(roomNumber,
                                            spinnerLoai.getSelectedItem().toString(),
                                            Double.parseDouble(dienTichStr),
                                            rentAmount,
                                            room.getStatus());
                                    updated.setId(room.getId());
                                    updated.setMaxOccupancy(maxOccupancy);

                                    // Preserve existing extended fields to avoid overwriting old data
                                    updated.setFloor(room.getFloor());
                                    updated.setDescription(room.getDescription());
                                    updated.setAmenities(room.getAmenities());
                                    updated.setCreatedAt(room.getCreatedAt());

                                    if (!finalSelectedHouseId.isEmpty()) {
                                        updated.setHouseId(finalSelectedHouseId);
                                        updated.setHouseName(finalSelectedHouseLabel);
                                    }

                                    enforceRentedIfHasTenants(updated, () -> {
                                        if (selectedImageUri != null) {
                                            uploadImageAndSave(updated);
                                        } else {
                                            // Internal note.
                                            updated.setImageUrl(room.getImageUrl());
                                            viewModel.updateRoom(updated,
                                                    () -> runOnUiThread(() -> Toast
                                                            .makeText(this, R.string.update_success, Toast.LENGTH_SHORT)
                                                            .show()),
                                                    () -> runOnUiThread(() -> Toast
                                                            .makeText(this, R.string.update_failed, Toast.LENGTH_SHORT)
                                                            .show()));
                                        }
                                    });
                                });
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, R.string.invalid_data, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null).show();
    }

    // Internal note.
    private void uploadImageAndSave(Room room) {
        pendingUploadRoom = room;
        Intent serviceIntent = new Intent(this, ImageUploadService.class);
        serviceIntent.putExtra(ImageUploadService.EXTRA_IMAGE_URI, selectedImageUri.toString());
        ContextCompat.startForegroundService(this, serviceIntent);
        Toast.makeText(this, R.string.uploading_image_to_system, Toast.LENGTH_SHORT).show();
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
