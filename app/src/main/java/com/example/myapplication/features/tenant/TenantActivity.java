package com.example.myapplication.features.tenant;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.constants.RoomStatus;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.MoneyFormatter;
import com.example.myapplication.core.util.ScreenUiHelper;
import com.example.myapplication.domain.Tenant;
import com.example.myapplication.domain.Room;
import com.example.myapplication.viewmodel.TenantViewModel;
import com.example.myapplication.viewmodel.RoomViewModel;
import com.google.android.material.appbar.AppBarLayout;
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

public class TenantActivity extends AppCompatActivity {

    private String preselectRoomId;
    private boolean didAutoOpenAddDialog = false;

    private TenantViewModel viewModel;
    private TenantAdapter adapter;
    private TextView tvEmpty;
    private List<Room> danhSachPhong = new ArrayList<>();

    private List<Tenant> lastTenants = new ArrayList<>();
    private boolean didAutoMigrateLinks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScreenUiHelper.enableEdgeToEdge(this, false);

        setContentView(R.layout.activity_tenant);

        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            ScreenUiHelper.applyTopInset(appBarLayout);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        ScreenUiHelper.setupBackToolbar(this, toolbar, getString(R.string.tenant_management_title));

        tvEmpty = findViewById(R.id.tvEmpty);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        FloatingActionButton fabThem = findViewById(R.id.fabThem);
        EditText etTimKiem = findViewById(R.id.etTimKiem);

        // Internal note.
        adapter = new TenantAdapter(new TenantAdapter.OnItemActionListener() {
            @Override
            public void onDelete(Tenant tenant) {
                final String roomId = tenant != null ? tenant.getRoomId() : null;
                new AlertDialog.Builder(TenantActivity.this)
                        .setTitle(getString(R.string.confirm_delete))
                        .setMessage(getString(R.string.delete_tenant_confirm, tenant.getFullName()))
                        .setPositiveButton(getString(R.string.delete), (d, w) -> viewModel.deleteTenant(tenant.getId(),
                                () -> runOnUiThread(() -> {
                                    maybeMarkRoomVacant(roomId);
                                    Toast.makeText(TenantActivity.this, getString(R.string.deleted), Toast.LENGTH_SHORT)
                                            .show();
                                }),
                                () -> runOnUiThread(() -> Toast
                                        .makeText(TenantActivity.this, getString(R.string.delete_failed),
                                                Toast.LENGTH_SHORT)
                                        .show())))
                        .setNegativeButton(getString(R.string.cancel), null).show();
            }

            @Override
            public void onSua(Tenant tenant) {
                showEditTenantDialog(tenant);
            }
        });

        // Internal note.
        adapter.setOnAddToRoomListener((roomId, roomName) -> showAddTenantDialog(roomId));

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        preselectRoomId = getIntent().getStringExtra("PRESELECT_ROOM_ID");

        // Internal note.
        if (etTimKiem != null) {
            etTimKiem.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int st, int c, int a) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                }

                @Override
                public void onTextChanged(CharSequence s, int st, int b, int c) {
                    adapter.filter(s.toString());
                    // Internal note.
                    tvEmpty.setVisibility(
                            adapter.getTenantCount() == 0 ? View.VISIBLE : View.GONE);
                }
            });
        }

        viewModel = new ViewModelProvider(this).get(TenantViewModel.class);
        viewModel.getTenantList().observe(this, list -> {
            // Internal note.
            adapter.setData(list);
            tvEmpty.setVisibility(
                    (list == null || list.isEmpty()) ? View.VISIBLE : View.GONE);
            lastTenants = list != null ? list : new ArrayList<>();
            maybeAutoMigrateTenantRoomLinks();
        });

        new ViewModelProvider(this).get(RoomViewModel.class)
                .getRoomList().observe(this, list -> {
                    danhSachPhong = list;
                    maybeAutoMigrateTenantRoomLinks();
                    maybeAutoOpenAddDialog();
                });

        fabThem.setOnClickListener(v -> showAddTenantDialog(null));
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
        showAddTenantDialog(roomId);
    }

    private void showAddTenantDialog(String preselectRoomId) {
        if (danhSachPhong.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_add_room_first), Toast.LENGTH_SHORT).show();
            return;
        }
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_tenant, null);
        EditText etHoTen = dialogView.findViewById(R.id.etHoTen);
        EditText etPersonalId = dialogView.findViewById(R.id.etCccd);
        EditText etSdt = dialogView.findViewById(R.id.etSdt);
        Spinner spinnerPhong = dialogView.findViewById(R.id.spinnerPhong);
        EditText etSoThanhVien = dialogView.findViewById(R.id.etSoThanhVien);
        EditText etNgayBatDau = dialogView.findViewById(R.id.etNgayBatDau);
        EditText etContractEndDate = dialogView.findViewById(R.id.etNgayKetThuc);
        EditText etDepositAmount = dialogView.findViewById(R.id.etTienCoc);
        // Internal note.
        CheckBox cbPrimaryContact = dialogView.findViewById(R.id.cbPrimaryContact);
        CheckBox cbContractRepresentative = dialogView.findViewById(R.id.cbDaiDienHopDong);
        CheckBox cbTemporaryResident = dialogView.findViewById(R.id.cbTamTru);
        CheckBox cbFullyDocumented = dialogView.findViewById(R.id.cbDayDuGiayTo);

        MoneyFormatter.applyTo(etDepositAmount);

        String[] phongNames = danhSachPhong.stream()
                .map(p -> getString(R.string.room_number, p.getRoomNumber())).toArray(String[]::new);
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
                .setTitle(getString(R.string.add_tenant))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.create), (d, w) -> {
                    String fullName = etHoTen.getText().toString().trim();
                    String personalId = etPersonalId.getText().toString().trim();
                    String phoneNumber = etSdt.getText().toString().trim();
                    String rentalStartDate = etNgayBatDau.getText().toString().trim();
                    String contractEndDateText = etContractEndDate.getText().toString().trim();
                    double depositAmount = MoneyFormatter.getValue(etDepositAmount);
                    if (fullName.isEmpty() || personalId.isEmpty() || phoneNumber.isEmpty() || rentalStartDate.isEmpty()
                            || contractEndDateText.isEmpty()
                            || depositAmount == 0) {
                        Toast.makeText(this, getString(R.string.please_fill_all_information), Toast.LENGTH_SHORT)
                                .show();
                        return;
                    }
                    try {
                        int idx = spinnerPhong.getSelectedItemPosition();
                        if (idx < 0 || idx >= danhSachPhong.size()) {
                            Toast.makeText(this, getString(R.string.please_select_room), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Room selectedRoom = danhSachPhong.get(idx);
                        final String roomId = selectedRoom.getId();
                        String soTVStr = etSoThanhVien.getText().toString().trim();
                        Tenant nt = new Tenant(fullName, personalId, phoneNumber, roomId,
                                soTVStr.isEmpty() ? 1 : Integer.parseInt(soTVStr),
                                rentalStartDate, contractEndDateText, depositAmount);
                        nt.setRoomNumber(selectedRoom.getRoomNumber());
                        // Internal note.
                        nt.setPrimaryContact(cbPrimaryContact.isChecked());
                        nt.setContractRepresentative(cbContractRepresentative.isChecked());
                        nt.setTemporaryResident(cbTemporaryResident.isChecked());
                        nt.setFullyDocumented(cbFullyDocumented.isChecked());

                        // Internal note.
                        StringBuilder statusMsg = new StringBuilder(getString(R.string.add_success));
                        if (!nt.isTemporaryResident())
                            statusMsg.append(" ").append(getString(R.string.not_registered_temporary_residence));
                        if (!nt.isFullyDocumented())
                            statusMsg.append(" ").append(getString(R.string.incomplete_documents));

                        viewModel.addTenant(nt,
                                () -> runOnUiThread(() -> {
                                    markRoomRented(roomId);
                                    Toast.makeText(this, statusMsg.toString(), Toast.LENGTH_LONG).show();
                                }),
                                () -> runOnUiThread(() -> Toast
                                        .makeText(this, getString(R.string.failed_check_firebase), Toast.LENGTH_LONG)
                                        .show()));
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, getString(R.string.invalid_data), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null).show();
    }

    private void showEditTenantDialog(Tenant tenant) {
        if (danhSachPhong.isEmpty()) {
            Toast.makeText(this, getString(R.string.failed_load_rooms), Toast.LENGTH_SHORT).show();
            return;
        }
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_tenant, null);
        EditText etHoTen = dialogView.findViewById(R.id.etHoTen);
        EditText etPersonalId = dialogView.findViewById(R.id.etCccd);
        EditText etSdt = dialogView.findViewById(R.id.etSdt);
        Spinner spinnerPhong = dialogView.findViewById(R.id.spinnerPhong);
        EditText etSoThanhVien = dialogView.findViewById(R.id.etSoThanhVien);
        EditText etNgayBatDau = dialogView.findViewById(R.id.etNgayBatDau);
        EditText etContractEndDate = dialogView.findViewById(R.id.etNgayKetThuc);
        EditText etDepositAmount = dialogView.findViewById(R.id.etTienCoc);
        // Internal note.
        CheckBox cbPrimaryContact = dialogView.findViewById(R.id.cbPrimaryContact);
        CheckBox cbContractRepresentative = dialogView.findViewById(R.id.cbDaiDienHopDong);
        CheckBox cbTemporaryResident = dialogView.findViewById(R.id.cbTamTru);
        CheckBox cbFullyDocumented = dialogView.findViewById(R.id.cbDayDuGiayTo);

        MoneyFormatter.applyTo(etDepositAmount);

        String[] phongNames = danhSachPhong.stream()
                .map(p -> getString(R.string.room_number, p.getRoomNumber())).toArray(String[]::new);
        ArrayAdapter<String> phongAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, phongNames);
        phongAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPhong.setAdapter(phongAdapter);

        // Internal note.
        etHoTen.setText(tenant.getFullName());
        etPersonalId.setText(tenant.getPersonalId());
        etSdt.setText(tenant.getPhoneNumber());
        etSoThanhVien.setText(String.valueOf(tenant.getMemberCount()));
        etNgayBatDau.setText(tenant.getRentalStartDate());
        etContractEndDate.setText(tenant.getContractEndDate());
        MoneyFormatter.setValue(etDepositAmount, tenant.getDepositAmount());
        // Internal note.
        cbPrimaryContact.setChecked(tenant.isPrimaryContact());
        cbContractRepresentative.setChecked(tenant.isContractRepresentative());
        cbTemporaryResident.setChecked(tenant.isTemporaryResident());
        cbFullyDocumented.setChecked(tenant.isFullyDocumented());

        for (int i = 0; i < danhSachPhong.size(); i++) {
            if (danhSachPhong.get(i).getId().equals(tenant.getRoomId())) {
                spinnerPhong.setSelection(i);
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.edit_tenant_info))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.update), (d, w) -> {
                    String fullName = etHoTen.getText().toString().trim();
                    String personalId = etPersonalId.getText().toString().trim();
                    String phoneNumber = etSdt.getText().toString().trim();
                    String rentalStartDate = etNgayBatDau.getText().toString().trim();
                    String contractEndDateText = etContractEndDate.getText().toString().trim();
                    double depositAmount = MoneyFormatter.getValue(etDepositAmount);
                    if (fullName.isEmpty() || personalId.isEmpty() || phoneNumber.isEmpty() || rentalStartDate.isEmpty()
                            || contractEndDateText.isEmpty()
                            || depositAmount == 0) {
                        Toast.makeText(this, getString(R.string.please_fill_all_information), Toast.LENGTH_SHORT)
                                .show();
                        return;
                    }
                    try {
                        int idx = spinnerPhong.getSelectedItemPosition();
                        if (idx < 0 || idx >= danhSachPhong.size()) {
                            Toast.makeText(this, getString(R.string.please_select_room), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        final String oldRoomId = tenant.getRoomId();
                        Room selectedRoom = danhSachPhong.get(idx);
                        final String newRoomId = selectedRoom.getId();
                        String soTVStr = etSoThanhVien.getText().toString().trim();
                        Tenant updated = new Tenant(fullName, personalId, phoneNumber, newRoomId,
                                soTVStr.isEmpty() ? 1 : Integer.parseInt(soTVStr),
                                rentalStartDate, contractEndDateText, depositAmount);
                        updated.setRoomNumber(selectedRoom.getRoomNumber());
                        updated.setId(tenant.getId());
                        // Internal note.
                        updated.setPrimaryContact(cbPrimaryContact.isChecked());
                        updated.setContractRepresentative(cbContractRepresentative.isChecked());
                        updated.setTemporaryResident(cbTemporaryResident.isChecked());
                        updated.setFullyDocumented(cbFullyDocumented.isChecked());

                        // Internal note.
                        StringBuilder statusMsg = new StringBuilder(getString(R.string.update_success));
                        if (!updated.isTemporaryResident())
                            statusMsg.append(" ").append(getString(R.string.not_registered_temporary_residence));
                        if (!updated.isFullyDocumented())
                            statusMsg.append(" ").append(getString(R.string.incomplete_documents));

                        viewModel.updateTenant(updated,
                                () -> runOnUiThread(() -> {
                                    markRoomRented(newRoomId);
                                    if (oldRoomId != null && !oldRoomId.equals(newRoomId)) {
                                        maybeMarkRoomVacant(oldRoomId);
                                    }
                                    Toast.makeText(this, statusMsg.toString(), Toast.LENGTH_LONG).show();
                                }),
                                () -> runOnUiThread(
                                        () -> Toast
                                                .makeText(this, getString(R.string.update_failed), Toast.LENGTH_SHORT)
                                                .show()));
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, getString(R.string.invalid_data), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null).show();
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

        scope.collection("rooms").document(roomId)
                .update("status", status)
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
        Map<String, String> uniqueRoomIdByRoomNumber = new HashMap<>();
        Set<String> duplicateRoomNumbers = new HashSet<>();
        for (Room p : danhSachPhong) {
            if (p == null)
                continue;
            if (p.getId() != null)
                roomIds.add(p.getId());
            String so = p.getRoomNumber();
            if (so == null || so.trim().isEmpty() || p.getId() == null)
                continue;
            if (uniqueRoomIdByRoomNumber.containsKey(so)) {
                duplicateRoomNumbers.add(so);
            } else {
                uniqueRoomIdByRoomNumber.put(so, p.getId());
            }
        }
        for (String dup : duplicateRoomNumbers)
            uniqueRoomIdByRoomNumber.remove(dup);

        List<Tenant> toFix = new ArrayList<>();
        Map<String, String> newRoomIdByTenantId = new HashMap<>();
        for (Tenant n : lastTenants) {
            if (n == null)
                continue;
            if (n.getId() == null || n.getId().trim().isEmpty())
                continue;

            String currentRoomId = n.getRoomId();
            boolean valid = currentRoomId != null && roomIds.contains(currentRoomId);
            if (valid)
                continue;

            String roomNumber = n.getRoomNumber();
            if (roomNumber == null || roomNumber.trim().isEmpty())
                continue;

            String mappedRoomId = uniqueRoomIdByRoomNumber.get(roomNumber);
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

        for (Tenant n : toFix) {
            final String tenantId = n.getId();
            final String newRoomId = newRoomIdByTenantId.get(tenantId);
            if (newRoomId == null) {
                if (remaining.decrementAndGet() == 0 && migrated.get() > 0) {
                    Toast.makeText(this, getString(R.string.migrated_tenants_to_room, migrated.get()),
                            Toast.LENGTH_LONG).show();
                }
                continue;
            }

            scope.collection("contracts").document(tenantId)
                    .update("roomId", newRoomId)
                    .addOnSuccessListener(v -> {
                        migrated.incrementAndGet();
                        markRoomRented(newRoomId);
                        if (remaining.decrementAndGet() == 0 && migrated.get() > 0) {
                            Toast.makeText(this, getString(R.string.migrated_tenants_to_room, migrated.get()),
                                    Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (remaining.decrementAndGet() == 0 && migrated.get() > 0) {
                            Toast.makeText(this, getString(R.string.migrated_tenants_to_room, migrated.get()),
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

        scope.collection("contracts")
                .whereEqualTo("roomId", roomId)
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
