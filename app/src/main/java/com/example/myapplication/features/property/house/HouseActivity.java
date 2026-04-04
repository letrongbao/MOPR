package com.example.myapplication.features.property.house;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.core.service.ImageUploadService;
import com.example.myapplication.R;
import com.example.myapplication.core.constants.TenantRoles;
import com.example.myapplication.core.constants.WaterCalculationMode;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.MoneyFormatter;
import com.example.myapplication.core.util.ScreenUiHelper;
import com.example.myapplication.features.property.room.RoomActivity;
import com.example.myapplication.domain.House;
import com.example.myapplication.core.repository.domain.HouseRepository;
import com.google.android.material.appbar.AppBarLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class HouseActivity extends AppCompatActivity {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final HouseRepository repo = new HouseRepository();

    private HouseAdapter adapter;
    private View tvEmpty;
    private boolean readOnly;

    private ActivityResultLauncher<String> qrPicker;
    private boolean waitingQrUpload;
    private boolean uploadingQr;
    private String dialogQrUrl;
    private TextView dialogQrStatus;
    private BroadcastReceiver uploadReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScreenUiHelper.enableEdgeToEdge(this, false);

        setContentView(R.layout.activity_house);

        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            ScreenUiHelper.applyTopInset(appBarLayout);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        ScreenUiHelper.setupBackToolbar(this, toolbar, getString(R.string.house_list_title));

        tvEmpty = findViewById(R.id.tvEmpty);

        qrPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null || !waitingQrUpload)
                return;
            waitingQrUpload = false;

            uploadingQr = true;
            Intent i = new Intent(this, ImageUploadService.class);
            i.putExtra(ImageUploadService.EXTRA_IMAGE_URI, uri.toString());
            startService(i);

            if (dialogQrStatus != null) {
                dialogQrStatus.setText(getString(R.string.house_qr_uploading));
            }
        });

        uploadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String imageUrl = intent.getStringExtra(ImageUploadService.EXTRA_IMAGE_URL);
                if (imageUrl == null)
                    return;

                // Only handle when a House dialog is open and we started a QR upload
                if (dialogQrStatus == null || !uploadingQr)
                    return;

                uploadingQr = false;
                dialogQrUrl = imageUrl;
                dialogQrStatus.setText(getString(R.string.payment_code_added));
                Toast.makeText(HouseActivity.this, getString(R.string.house_qr_uploaded), Toast.LENGTH_SHORT).show();
            }
        };
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(uploadReceiver, new IntentFilter(ImageUploadService.ACTION_UPLOAD_COMPLETE));

        View ivAddHouse = findViewById(R.id.ivAddHouse);
        if (ivAddHouse != null) {
            ivAddHouse.setOnClickListener(v -> showCreateDialog(null));
        }

        RecyclerView rv = findViewById(R.id.recyclerView);
        adapter = new HouseAdapter(new HouseAdapter.OnItemAction() {
            @Override
            public void onEdit(@NonNull House item) {
                showCreateDialog(item);
            }

            @Override
            public void onDelete(@NonNull House item) {
                confirmDelete(item);
            }

            @Override
            public void onViewRooms(@NonNull House item) {
                Intent it = new Intent(HouseActivity.this, RoomActivity.class);
                it.putExtra("HOUSE_ID", item.getId());
                it.putExtra("HOUSE_NAME", item.getHouseName());
                it.putExtra("HOUSE_ADDRESS", item.getAddress());
                startActivity(it);
            }
        });
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
        adapter.setReadOnly(readOnly);

        setupPermissionsThenObserve();
    }

    private void setupPermissionsThenObserve() {
        String tenantId = TenantSession.getActiveTenantId();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (tenantId == null || tenantId.trim().isEmpty() || user == null) {
            readOnly = false;
            adapter.setReadOnly(false);
            observe();
            return;
        }

        db.collection("tenants").document(tenantId)
                .collection("members").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String role = doc.getString("role");
                    readOnly = TenantRoles.TENANT.equals(role);
                    View ivAddHouse = findViewById(R.id.ivAddHouse);
                    if (ivAddHouse != null) {
                        ivAddHouse.setVisibility(readOnly ? View.GONE : View.VISIBLE);
                    }
                    adapter.setReadOnly(readOnly);
                    observe();
                })
                .addOnFailureListener(e -> {
                    readOnly = false;
                    adapter.setReadOnly(false);
                    observe();
                });
    }

    private void observe() {
        repo.listAll().observe(this, list -> {
            adapter.setItems(list);
            if (tvEmpty != null) {
                tvEmpty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void confirmDelete(House k) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.house_delete_title))
                .setMessage(getString(R.string.house_delete_confirm, k.getHouseName()))
                .setPositiveButton(getString(R.string.delete), (d, w) -> repo.delete(k.getId(),
                        () -> runOnUiThread(() -> Toast.makeText(this, getString(R.string.deleted), Toast.LENGTH_SHORT).show()),
                        () -> runOnUiThread(() -> Toast.makeText(this, getString(R.string.delete_failed), Toast.LENGTH_SHORT).show())))
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showCreateDialog(House existing) {
        View v = getLayoutInflater().inflate(R.layout.dialog_create_house, null);
        EditText etName = v.findViewById(R.id.etHouseName);
        EditText etPhone = v.findViewById(R.id.etHousePhone);
        EditText etAddr = v.findViewById(R.id.etHouseAddr);

        LinearLayout llExtraFees = v.findViewById(R.id.llExtraFeesContainer);
        View btnAddExtraFee = v.findViewById(R.id.btnAddExtraFee);

        TextView tvQr = v.findViewById(R.id.tvQrStatus);
        View btnPickQr = v.findViewById(R.id.btnPickQr);

        EditText etBankAccountName = v.findViewById(R.id.etChuTaiKhoan);
        EditText etBankName = v.findViewById(R.id.etNganHang);
        EditText etBankAccountNo = v.findViewById(R.id.etSoTaiKhoan);

        EditText etElectricityPrice = v.findViewById(R.id.etGiaDien);
        EditText etWaterPrice = v.findViewById(R.id.etGiaNuoc);
        RadioGroup rgNuoc = v.findViewById(R.id.rgNuoc);

        EditText etParkingPrice = v.findViewById(R.id.etGiaXe);
        EditText etInternetPrice = v.findViewById(R.id.etGiaInternet);
        EditText etLaundryPrice = v.findViewById(R.id.etGiaGiatSay);
        EditText etElevatorPrice = v.findViewById(R.id.etGiaThangMay);
        EditText etCableTvPrice = v.findViewById(R.id.etGiaTiviCap);
        EditText etTrashPrice = v.findViewById(R.id.etGiaRac);
        EditText etServicePrice = v.findViewById(R.id.etGiaDichVu);

        EditText etNote = v.findViewById(R.id.etHouseNote);

        // Apply money formatters to all price fields
        applyMoneyFormatters(etElectricityPrice, etWaterPrice, etParkingPrice, etInternetPrice,
                etLaundryPrice, etElevatorPrice, etCableTvPrice, etTrashPrice, etServicePrice);

        boolean isEdit = existing != null;

        dialogQrStatus = tvQr;
        dialogQrUrl = isEdit ? existing.getPaymentQrUrl() : null;
        if (dialogQrStatus != null) {
            dialogQrStatus.setText(dialogQrUrl != null && !dialogQrUrl.trim().isEmpty()
                    ? getString(R.string.payment_code_added)
                    : getString(R.string.house_qr_not_added));
        }

        if (btnPickQr != null) {
            btnPickQr.setOnClickListener(vw -> {
                waitingQrUpload = true;
                qrPicker.launch("image/*");
            });
        }

        if (btnAddExtraFee != null && llExtraFees != null) {
            btnAddExtraFee.setOnClickListener(vw -> addExtraFeeRow(llExtraFees, null));
        }

        if (isEdit) {
            etName.setText(existing.getHouseName());
            etPhone.setText(existing.getManagerPhone());
            etAddr.setText(existing.getAddress());

            etBankAccountName.setText(existing.getBankAccountName());
            etBankName.setText(existing.getBankName());
            etBankAccountNo.setText(existing.getBankAccountNo());

            etElectricityPrice.setText(formatMoneyInput(existing.getElectricityPrice()));
            etWaterPrice.setText(formatMoneyInput(existing.getWaterPrice()));

            String waterMode = existing.getWaterCalculationMethod();
            if (waterMode != null) {
                if (WaterCalculationMode.isPerPerson(waterMode)) {
                    ((android.widget.RadioButton) v.findViewById(R.id.rbNuocNguoi)).setChecked(true);
                } else if (WaterCalculationMode.isMeter(waterMode)) {
                    ((android.widget.RadioButton) v.findViewById(R.id.rbNuocDongHo)).setChecked(true);
                } else {
                    ((android.widget.RadioButton) v.findViewById(R.id.rbNuocPhong)).setChecked(true);
                }
            }

            etParkingPrice.setText(formatMoneyInput(existing.getParkingPrice()));
            etInternetPrice.setText(formatMoneyInput(existing.getInternetPrice()));
            etLaundryPrice.setText(formatMoneyInput(existing.getLaundryPrice()));
            etElevatorPrice.setText(formatMoneyInput(existing.getElevatorPrice()));
            etCableTvPrice.setText(formatMoneyInput(existing.getCableTvPrice()));
            etTrashPrice.setText(formatMoneyInput(existing.getTrashPrice()));
            etServicePrice.setText(formatMoneyInput(existing.getServicePrice()));

            etNote.setText(existing.getNote());

            if (llExtraFees != null) {
                java.util.List<House.ExtraFee> fees = existing.getExtraFees();
                if (fees != null && !fees.isEmpty()) {
                    for (House.ExtraFee f : fees)
                        addExtraFeeRow(llExtraFees, f);
                }
            }
        }

        if (llExtraFees != null && llExtraFees.getChildCount() == 0) {
            addExtraFeeRow(llExtraFees, null);
        }

        AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle(isEdit ? getString(R.string.house_update_title) : getString(R.string.house_create_title))
                .setView(v)
                .setNegativeButton(getString(R.string.cancel), null)
                .setPositiveButton(getString(R.string.house_save_info), null)
                .create();

        dlg.setOnShowListener(dd -> {
            dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
                String addr = etAddr.getText().toString().trim();
                if (addr.isEmpty()) {
                    Toast.makeText(this, getString(R.string.please_enter_address), Toast.LENGTH_SHORT).show();
                    return;
                }

                String name = etName.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(this, getString(R.string.please_enter_manager_name), Toast.LENGTH_SHORT).show();
                    return;
                }
                String phone = etPhone.getText().toString().trim();
                if (phone.isEmpty()) {
                    Toast.makeText(this, getString(R.string.please_enter_manager_phone), Toast.LENGTH_SHORT).show();
                    return;
                }

                String note = etNote.getText().toString().trim();

                String waterMode = "room";
                if (rgNuoc != null) {
                    int checkedId = rgNuoc.getCheckedRadioButtonId();
                    if (checkedId == R.id.rbNuocNguoi)
                        waterMode = WaterCalculationMode.PER_PERSON;
                    else if (checkedId == R.id.rbNuocDongHo)
                        waterMode = WaterCalculationMode.METER;
                    else
                        waterMode = WaterCalculationMode.ROOM;
                }

                java.util.List<House.ExtraFee> extraFees = collectExtraFees(llExtraFees);

                House target = isEdit ? existing : new House();
                target.setHouseName(name);
                target.setManagerPhone(phone);
                target.setAddress(addr);
                target.setNote(note);

                target.setBankAccountName(etBankAccountName.getText().toString().trim());
                target.setBankName(etBankName.getText().toString().trim());
                target.setBankAccountNo(etBankAccountNo.getText().toString().trim());

                target.setElectricityPrice(parseMoney(etElectricityPrice));
                target.setWaterPrice(parseMoney(etWaterPrice));
                target.setWaterCalculationMethod(waterMode);

                target.setParkingPrice(parseMoney(etParkingPrice));
                target.setInternetPrice(parseMoney(etInternetPrice));
                target.setLaundryPrice(parseMoney(etLaundryPrice));
                target.setElevatorPrice(parseMoney(etElevatorPrice));
                target.setCableTvPrice(parseMoney(etCableTvPrice));
                target.setTrashPrice(parseMoney(etTrashPrice));
                target.setServicePrice(parseMoney(etServicePrice));

                target.setExtraFees(extraFees);
                target.setPaymentQrUrl(dialogQrUrl);

                if (isEdit) {
                    repo.update(target,
                            () -> runOnUiThread(() -> Toast.makeText(this, getString(R.string.saved), Toast.LENGTH_SHORT).show()),
                            () -> runOnUiThread(() -> Toast.makeText(this, getString(R.string.save_failed), Toast.LENGTH_SHORT).show()));
                } else {
                    repo.add(target,
                            () -> runOnUiThread(() -> Toast.makeText(this, getString(R.string.house_added), Toast.LENGTH_SHORT).show()),
                            () -> runOnUiThread(
                                    () -> Toast.makeText(this, getString(R.string.house_add_failed), Toast.LENGTH_LONG).show()));
                }

                dlg.dismiss();
            });
        });

        dlg.setOnDismissListener(d -> {
            dialogQrStatus = null;
            dialogQrUrl = null;
            waitingQrUpload = false;
            uploadingQr = false;
        });

        dlg.show();
    }

    private void addExtraFeeRow(@NonNull LinearLayout container, House.ExtraFee existing) {
        View row = LayoutInflater.from(this).inflate(R.layout.row_additional_expense, container, false);

        EditText etFeeName = row.findViewById(R.id.etTenPhi);
        Spinner spUnit = row.findViewById(R.id.spDonViTinh);
        EditText etGia = row.findViewById(R.id.etGia);
        ImageView ivRemove = row.findViewById(R.id.ivRemove);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.extra_fee_units, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spUnit.setAdapter(adapter);

        if (existing != null) {
            etFeeName.setText(existing.getFeeName());
            etGia.setText(formatMoneyInput(existing.getPrice()));

            String unit = existing.getUnit();
            if (unit != null) {
                for (int i = 0; i < adapter.getCount(); i++) {
                    if (unit.equalsIgnoreCase(String.valueOf(adapter.getItem(i)))) {
                        spUnit.setSelection(i);
                        break;
                    }
                }
            }
        }

        ivRemove.setOnClickListener(v -> container.removeView(row));

        container.addView(row);
    }

    private java.util.List<House.ExtraFee> collectExtraFees(LinearLayout container) {
        java.util.List<House.ExtraFee> out = new java.util.ArrayList<>();
        if (container == null)
            return out;

        for (int i = 0; i < container.getChildCount(); i++) {
            View row = container.getChildAt(i);
            if (row == null)
                continue;

            EditText etFeeName = row.findViewById(R.id.etTenPhi);
            Spinner spUnit = row.findViewById(R.id.spDonViTinh);
            EditText etGia = row.findViewById(R.id.etGia);

            String ten = etFeeName != null && etFeeName.getText() != null ? etFeeName.getText().toString().trim() : "";
            if (ten.isEmpty())
                continue;

            String unit = spUnit != null && spUnit.getSelectedItem() != null ? spUnit.getSelectedItem().toString()
                    : "";
            double gia = parseMoney(etGia);

            out.add(new House.ExtraFee(ten, unit, gia));
        }
        return out;
    }

    private double parseMoney(@androidx.annotation.NonNull EditText et) {
        String raw = et.getText() != null ? et.getText().toString() : "";
        raw = raw.replaceAll("[^0-9]", "");
        if (raw.isEmpty())
            return 0;
        try {
            return Double.parseDouble(raw);
        } catch (Exception e) {
            return 0;
        }
    }

    private String formatMoneyInput(double value) {
        if (value == 0)
            return "";
        return MoneyFormatter.formatWithoutCurrency(value);
    }

    private void applyMoneyFormatters(EditText... editTexts) {
        for (EditText et : editTexts) {
            if (et != null)
                MoneyFormatter.applyTo(et);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(uploadReceiver);
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
