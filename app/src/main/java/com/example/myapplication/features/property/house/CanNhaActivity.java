package com.example.myapplication.features.property.house;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.core.service.ImageUploadService;
import com.example.myapplication.R;
import com.example.myapplication.core.constants.TenantRoles;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.MoneyFormatter;
import com.example.myapplication.features.property.room.PhongTroActivity;
import com.example.myapplication.domain.CanNha;
import com.example.myapplication.core.repository.domain.CanNhaRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class CanNhaActivity extends AppCompatActivity {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CanNhaRepository repo = new CanNhaRepository();

    private CanNhaAdapter adapter;
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

        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_khu_tro);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Các căn nhà của bạn");
        }

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

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
                dialogQrStatus.setText("Đang tải mã thanh toán...");
            }
        });

        uploadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String imageUrl = intent.getStringExtra(ImageUploadService.EXTRA_IMAGE_URL);
                if (imageUrl == null)
                    return;

                // Only handle when a CanNha dialog is open and we started a QR upload
                if (dialogQrStatus == null || !uploadingQr)
                    return;

                uploadingQr = false;
                dialogQrUrl = imageUrl;
                dialogQrStatus.setText("Đã thêm mã thanh toán");
                Toast.makeText(CanNhaActivity.this, "Đã tải mã thanh toán", Toast.LENGTH_SHORT).show();
            }
        };
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(uploadReceiver, new IntentFilter(ImageUploadService.ACTION_UPLOAD_COMPLETE));

        View ivAddHouse = findViewById(R.id.ivAddHouse);
        if (ivAddHouse != null) {
            ivAddHouse.setOnClickListener(v -> showCreateDialog(null));
        }

        RecyclerView rv = findViewById(R.id.recyclerView);
        adapter = new CanNhaAdapter(new CanNhaAdapter.OnItemAction() {
            @Override
            public void onEdit(@NonNull CanNha item) {
                showCreateDialog(item);
            }

            @Override
            public void onDelete(@NonNull CanNha item) {
                confirmDelete(item);
            }

            @Override
            public void onViewRooms(@NonNull CanNha item) {
                Intent it = new Intent(CanNhaActivity.this, PhongTroActivity.class);
                it.putExtra("KHU_ID", item.getId());
                it.putExtra("KHU_NAME", item.getTenKhu());
                it.putExtra("KHU_ADDR", item.getDiaChi());
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

    private void confirmDelete(CanNha k) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa nhà?")
                .setMessage("Bạn có chắc chắn muốn xóa '" + k.getTenKhu() + "'?")
                .setPositiveButton("Xóa", (d, w) -> repo.delete(k.getId(),
                        () -> runOnUiThread(() -> Toast.makeText(this, "Đã xóa", Toast.LENGTH_SHORT).show()),
                        () -> runOnUiThread(() -> Toast.makeText(this, "Xóa thất bại", Toast.LENGTH_SHORT).show())))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showCreateDialog(CanNha existing) {
        View v = getLayoutInflater().inflate(R.layout.dialog_create_khu, null);
        EditText etName = v.findViewById(R.id.etKhuName);
        EditText etPhone = v.findViewById(R.id.etKhuPhone);
        EditText etAddr = v.findViewById(R.id.etKhuAddr);

        LinearLayout llExtraFees = v.findViewById(R.id.llExtraFeesContainer);
        View btnAddExtraFee = v.findViewById(R.id.btnAddExtraFee);

        TextView tvQr = v.findViewById(R.id.tvQrStatus);
        View btnPickQr = v.findViewById(R.id.btnPickQr);

        RadioGroup rgRemindFee = v.findViewById(R.id.rgRemindFee);

        EditText etChuTK = v.findViewById(R.id.etChuTaiKhoan);
        EditText etNganHang = v.findViewById(R.id.etNganHang);
        EditText etSoTK = v.findViewById(R.id.etSoTaiKhoan);

        EditText etGiaDien = v.findViewById(R.id.etGiaDien);
        EditText etGiaNuoc = v.findViewById(R.id.etGiaNuoc);
        RadioGroup rgNuoc = v.findViewById(R.id.rgNuoc);

        EditText etGiaXe = v.findViewById(R.id.etGiaXe);
        EditText etGiaInternet = v.findViewById(R.id.etGiaInternet);
        EditText etGiaGiatSay = v.findViewById(R.id.etGiaGiatSay);
        EditText etGiaThangMay = v.findViewById(R.id.etGiaThangMay);
        EditText etGiaTiviCap = v.findViewById(R.id.etGiaTiviCap);
        EditText etGiaRac = v.findViewById(R.id.etGiaRac);
        EditText etGiaDichVu = v.findViewById(R.id.etGiaDichVu);

        EditText etNote = v.findViewById(R.id.etKhuNote);

        // Apply money formatters to all price fields
        applyMoneyFormatters(etGiaDien, etGiaNuoc, etGiaXe, etGiaInternet,
                etGiaGiatSay, etGiaThangMay, etGiaTiviCap, etGiaRac, etGiaDichVu);

        boolean isEdit = existing != null;

        dialogQrStatus = tvQr;
        dialogQrUrl = isEdit ? existing.getQrThanhToanUrl() : null;
        if (dialogQrStatus != null) {
            dialogQrStatus.setText(dialogQrUrl != null && !dialogQrUrl.trim().isEmpty()
                    ? "Đã thêm mã thanh toán"
                    : "(Chưa thêm mã)");
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
            etName.setText(existing.getTenKhu());
            etPhone.setText(existing.getSdtQuanLy());
            etAddr.setText(existing.getDiaChi());

            etChuTK.setText(existing.getChuTaiKhoan());
            etNganHang.setText(existing.getNganHang());
            etSoTK.setText(existing.getSoTaiKhoan());

            etGiaDien.setText(formatMoneyInput(existing.getGiaDien()));
            etGiaNuoc.setText(formatMoneyInput(existing.getGiaNuoc()));

            String waterMode = existing.getCachTinhNuoc();
            if (waterMode != null) {
                if (waterMode.equals("nguoi")) {
                    ((android.widget.RadioButton) v.findViewById(R.id.rbNuocNguoi)).setChecked(true);
                } else if (waterMode.equals("dong_ho")) {
                    ((android.widget.RadioButton) v.findViewById(R.id.rbNuocDongHo)).setChecked(true);
                } else {
                    ((android.widget.RadioButton) v.findViewById(R.id.rbNuocPhong)).setChecked(true);
                }
            }

            etGiaXe.setText(formatMoneyInput(existing.getGiaXe()));
            etGiaInternet.setText(formatMoneyInput(existing.getGiaInternet()));
            etGiaGiatSay.setText(formatMoneyInput(existing.getGiaGiatSay()));
            etGiaThangMay.setText(formatMoneyInput(existing.getGiaThangMay()));
            etGiaTiviCap.setText(formatMoneyInput(existing.getGiaTiviCap()));
            etGiaRac.setText(formatMoneyInput(existing.getGiaRac()));
            etGiaDichVu.setText(formatMoneyInput(existing.getGiaDichVu()));

            etNote.setText(existing.getGhiChu());

            if (rgRemindFee != null) {
                String remind = existing.getNhacBaoPhi();
                if ("end_month".equals(remind)) {
                    rgRemindFee.check(R.id.rbRemindEndMonth);
                } else {
                    rgRemindFee.check(R.id.rbRemindStartMonth);
                }
            }

            if (llExtraFees != null) {
                java.util.List<CanNha.PhiKhac> fees = existing.getPhiKhac();
                if (fees != null && !fees.isEmpty()) {
                    for (CanNha.PhiKhac f : fees)
                        addExtraFeeRow(llExtraFees, f);
                }
            }
        } else {
            if (rgRemindFee != null) {
                rgRemindFee.check(R.id.rbRemindStartMonth);
            }
        }

        if (llExtraFees != null && llExtraFees.getChildCount() == 0) {
            addExtraFeeRow(llExtraFees, null);
        }

        AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle(isEdit ? "Cập nhật căn nhà" : "Tạo thông tin căn nhà")
                .setView(v)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu thông tin", null)
                .create();

        dlg.setOnShowListener(dd -> {
            dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
                String addr = etAddr.getText().toString().trim();
                if (addr.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập địa chỉ", Toast.LENGTH_SHORT).show();
                    return;
                }

                String name = etName.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập tên quản lý", Toast.LENGTH_SHORT).show();
                    return;
                }
                String phone = etPhone.getText().toString().trim();
                if (phone.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập số điện thoại quản lý", Toast.LENGTH_SHORT).show();
                    return;
                }

                String note = etNote.getText().toString().trim();

                String waterMode = "phong";
                if (rgNuoc != null) {
                    int checkedId = rgNuoc.getCheckedRadioButtonId();
                    if (checkedId == R.id.rbNuocNguoi)
                        waterMode = "nguoi";
                    else if (checkedId == R.id.rbNuocDongHo)
                        waterMode = "dong_ho";
                    else
                        waterMode = "phong";
                }

                String remind = "start_month";
                if (rgRemindFee != null && rgRemindFee.getCheckedRadioButtonId() == R.id.rbRemindEndMonth) {
                    remind = "end_month";
                }

                java.util.List<CanNha.PhiKhac> extraFees = collectExtraFees(llExtraFees);

                CanNha target = isEdit ? existing : new CanNha();
                target.setTenKhu(name);
                target.setSdtQuanLy(phone);
                target.setDiaChi(addr);
                target.setGhiChu(note);

                target.setChuTaiKhoan(etChuTK.getText().toString().trim());
                target.setNganHang(etNganHang.getText().toString().trim());
                target.setSoTaiKhoan(etSoTK.getText().toString().trim());

                target.setGiaDien(parseMoney(etGiaDien));
                target.setGiaNuoc(parseMoney(etGiaNuoc));
                target.setCachTinhNuoc(waterMode);

                target.setGiaXe(parseMoney(etGiaXe));
                target.setGiaInternet(parseMoney(etGiaInternet));
                target.setGiaGiatSay(parseMoney(etGiaGiatSay));
                target.setGiaThangMay(parseMoney(etGiaThangMay));
                target.setGiaTiviCap(parseMoney(etGiaTiviCap));
                target.setGiaRac(parseMoney(etGiaRac));
                target.setGiaDichVu(parseMoney(etGiaDichVu));

                target.setPhiKhac(extraFees);
                target.setQrThanhToanUrl(dialogQrUrl);
                target.setNhacBaoPhi(remind);

                if (isEdit) {
                    repo.update(target,
                            () -> runOnUiThread(() -> Toast.makeText(this, "Đã lưu", Toast.LENGTH_SHORT).show()),
                            () -> runOnUiThread(() -> Toast.makeText(this, "Lưu thất bại", Toast.LENGTH_SHORT).show()));
                } else {
                    repo.add(target,
                            () -> runOnUiThread(() -> Toast.makeText(this, "Đã thêm", Toast.LENGTH_SHORT).show()),
                            () -> runOnUiThread(
                                    () -> Toast.makeText(this, "Thêm thất bại", Toast.LENGTH_SHORT).show()));
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

    private void addExtraFeeRow(@NonNull LinearLayout container, CanNha.PhiKhac existing) {
        View row = LayoutInflater.from(this).inflate(R.layout.row_phi_khac, container, false);

        EditText etTenPhi = row.findViewById(R.id.etTenPhi);
        Spinner spDonVi = row.findViewById(R.id.spDonViTinh);
        EditText etGia = row.findViewById(R.id.etGia);
        ImageView ivRemove = row.findViewById(R.id.ivRemove);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.extra_fee_units, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDonVi.setAdapter(adapter);

        if (existing != null) {
            etTenPhi.setText(existing.getTenPhi());
            etGia.setText(formatMoneyInput(existing.getGia()));

            String unit = existing.getDonViTinh();
            if (unit != null) {
                for (int i = 0; i < adapter.getCount(); i++) {
                    if (unit.equalsIgnoreCase(String.valueOf(adapter.getItem(i)))) {
                        spDonVi.setSelection(i);
                        break;
                    }
                }
            }
        }

        ivRemove.setOnClickListener(v -> container.removeView(row));

        container.addView(row);
    }

    private java.util.List<CanNha.PhiKhac> collectExtraFees(LinearLayout container) {
        java.util.List<CanNha.PhiKhac> out = new java.util.ArrayList<>();
        if (container == null)
            return out;

        for (int i = 0; i < container.getChildCount(); i++) {
            View row = container.getChildAt(i);
            if (row == null)
                continue;

            EditText etTenPhi = row.findViewById(R.id.etTenPhi);
            Spinner spDonVi = row.findViewById(R.id.spDonViTinh);
            EditText etGia = row.findViewById(R.id.etGia);

            String ten = etTenPhi != null && etTenPhi.getText() != null ? etTenPhi.getText().toString().trim() : "";
            if (ten.isEmpty())
                continue;

            String unit = spDonVi != null && spDonVi.getSelectedItem() != null ? spDonVi.getSelectedItem().toString()
                    : "";
            double gia = parseMoney(etGia);

            out.add(new CanNha.PhiKhac(ten, unit, gia));
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
