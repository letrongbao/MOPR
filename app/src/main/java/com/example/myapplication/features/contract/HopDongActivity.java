package com.example.myapplication.features.contract;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.example.myapplication.core.service.ImageUploadService;
import com.example.myapplication.R;
import com.example.myapplication.core.constants.RoomStatus;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.MoneyFormatter;
import com.example.myapplication.domain.CanNha;
import com.example.myapplication.domain.NguoiThue;
import com.example.myapplication.domain.PhongTro;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HopDongActivity extends AppCompatActivity {

    public static final String EXTRA_PHONG_ID = "PHONG_ID";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private String phongId;
    private PhongTro currentPhong;
    private CanNha currentKhu;

    private NguoiThue currentContract;

    private EditText etSoHopDong, etTenKhach, etDienThoai, etDiaChi, etCccd;
    private ImageView ivFront, ivBack, ivEditFront, ivEditBack;
    private EditText etPhong, etSoNguoi, etTienPhong, etTienCoc;
    private CheckBox cbShowDeposit;
    private EditText etNgayKy, etSoThang;
    private ImageView ivPickDate;
    private CheckBox cbRemind;
    private EditText etChiSoDien;
    private CheckBox cbGuiXe, cbInternet, cbGiatSay;
    private TextView tvGuiXePrice;
    private EditText etSoXe;
    private EditText etGhiChu;
    private CheckBox cbShowNote;

    private MaterialButton btnSave, btnPrint, btnEnd, btnUpdate;

    private ActivityResultLauncher<String> imagePicker;
    private Uri selectedImageUri;

    private enum UploadTarget {
        FRONT, BACK
    }

    private UploadTarget pendingUploadTarget;

    private BroadcastReceiver uploadReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);

        phongId = getIntent().getStringExtra(EXTRA_PHONG_ID);
        if (phongId == null || phongId.trim().isEmpty()) {
            Toast.makeText(this, "Thiếu ID phòng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        imagePicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null)
                return;
            selectedImageUri = uri;
            startUploadSelectedImage();
        });

        uploadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String imageUrl = intent.getStringExtra(ImageUploadService.EXTRA_IMAGE_URL);
                if (imageUrl == null || pendingUploadTarget == null)
                    return;

                if (pendingUploadTarget == UploadTarget.FRONT) {
                    if (currentContract == null)
                        currentContract = new NguoiThue();
                    currentContract.setCccdFrontUrl(imageUrl);
                    Glide.with(HopDongActivity.this).load(imageUrl).centerCrop().into(ivFront);
                } else {
                    if (currentContract == null)
                        currentContract = new NguoiThue();
                    currentContract.setCccdBackUrl(imageUrl);
                    Glide.with(HopDongActivity.this).load(imageUrl).centerCrop().into(ivBack);
                }

                pendingUploadTarget = null;
                setUploadEnabled(true);
                Toast.makeText(HopDongActivity.this, "Tải ảnh lên thành công", Toast.LENGTH_SHORT).show();
            }
        };
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(uploadReceiver, new IntentFilter(ImageUploadService.ACTION_UPLOAD_COMPLETE));

        setContentView(R.layout.activity_hop_dong);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Tạo hợp đồng");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        bindViews();
        wireUI();

        loadPhongThenContract();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(uploadReceiver);
        } catch (Exception ignored) {
        }
    }

    private void bindViews() {
        etSoHopDong = findViewById(R.id.etSoHopDong);
        etTenKhach = findViewById(R.id.etTenKhach);
        etDienThoai = findViewById(R.id.etDienThoai);
        etDiaChi = findViewById(R.id.etDiaChi);
        etCccd = findViewById(R.id.etCccd);

        ivFront = findViewById(R.id.ivFront);
        ivBack = findViewById(R.id.ivBack);
        ivEditFront = findViewById(R.id.ivEditFront);
        ivEditBack = findViewById(R.id.ivEditBack);

        etPhong = findViewById(R.id.etPhong);
        etSoNguoi = findViewById(R.id.etSoNguoi);
        etTienPhong = findViewById(R.id.etTienPhong);
        etTienCoc = findViewById(R.id.etTienCoc);
        cbShowDeposit = findViewById(R.id.cbShowDeposit);

        // Apply money formatters
        MoneyFormatter.applyTo(etTienPhong);
        MoneyFormatter.applyTo(etTienCoc);

        etNgayKy = findViewById(R.id.etNgayKy);
        etSoThang = findViewById(R.id.etSoThang);
        ivPickDate = findViewById(R.id.ivPickDate);
        cbRemind = findViewById(R.id.cbRemind);

        etChiSoDien = findViewById(R.id.etChiSoDien);
        cbGuiXe = findViewById(R.id.cbGuiXe);
        tvGuiXePrice = findViewById(R.id.tvGuiXePrice);
        etSoXe = findViewById(R.id.etSoXe);
        cbInternet = findViewById(R.id.cbInternet);
        cbGiatSay = findViewById(R.id.cbGiatSay);

        etGhiChu = findViewById(R.id.etGhiChu);
        cbShowNote = findViewById(R.id.cbShowNote);

        btnSave = findViewById(R.id.btnSave);
        btnPrint = findViewById(R.id.btnPrint);
        btnEnd = findViewById(R.id.btnEnd);
        btnUpdate = findViewById(R.id.btnUpdate);
    }

    private void wireUI() {
        View.OnClickListener pickDate = v -> showDatePicker();
        etNgayKy.setOnClickListener(pickDate);
        ivPickDate.setOnClickListener(pickDate);

        cbGuiXe.setOnCheckedChangeListener((buttonView, isChecked) -> {
            etSoXe.setEnabled(isChecked);
            etSoXe.setBackgroundResource(isChecked ? R.drawable.bg_input_rounded : R.drawable.bg_input_disabled);
            if (!isChecked)
                etSoXe.setText("");
        });

        ivEditFront.setOnClickListener(v -> {
            pendingUploadTarget = UploadTarget.FRONT;
            imagePicker.launch("image/*");
        });
        ivEditBack.setOnClickListener(v -> {
            pendingUploadTarget = UploadTarget.BACK;
            imagePicker.launch("image/*");
        });

        btnSave.setOnClickListener(v -> saveOrUpdate(true));
        btnUpdate.setOnClickListener(v -> saveOrUpdate(false));
        btnEnd.setOnClickListener(v -> confirmEndContract());
        btnPrint.setOnClickListener(v -> printContract());
    }

    private void setUploadEnabled(boolean enabled) {
        ivEditFront.setEnabled(enabled);
        ivEditBack.setEnabled(enabled);
        ivEditFront.setAlpha(enabled ? 1f : 0.5f);
        ivEditBack.setAlpha(enabled ? 1f : 0.5f);
    }

    private void startUploadSelectedImage() {
        if (selectedImageUri == null || pendingUploadTarget == null)
            return;

        setUploadEnabled(false);

        Intent i = new Intent(this, ImageUploadService.class);
        i.putExtra(ImageUploadService.EXTRA_IMAGE_URI, selectedImageUri.toString());
        startService(i);
    }

    private CollectionReference scopedCollection(@NonNull String collection) {
        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId != null && !tenantId.trim().isEmpty()) {
            return db.collection("tenants").document(tenantId).collection(collection);
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
            throw new IllegalStateException("User not logged in");
        return db.collection("users").document(user.getUid()).collection(collection);
    }

    private DocumentReference scopedDoc() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
            return null;

        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId != null && !tenantId.trim().isEmpty()) {
            return db.collection("tenants").document(tenantId);
        }
        return db.collection("users").document(user.getUid());
    }

    private void loadPhongThenContract() {
        scopedCollection("phong_tro").document(phongId)
                .get()
                .addOnSuccessListener(doc -> {
                    currentPhong = doc != null && doc.exists() ? doc.toObject(PhongTro.class) : null;
                    if (currentPhong == null) {
                        Toast.makeText(this, "Không tìm thấy phòng", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    currentPhong.setId(doc.getId());

                    bindPhongToUI();
                    loadKhuFeesThenContract();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải phòng", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void loadKhuFeesThenContract() {
        String khuId = currentPhong.getKhuId();
        if (khuId == null || khuId.trim().isEmpty()) {
            currentKhu = null;
            bindFeesToUI(null);
            loadExistingContract();
            return;
        }

        scopedCollection("khu_tro").document(khuId)
                .get()
                .addOnSuccessListener(doc -> {
                    currentKhu = doc != null && doc.exists() ? doc.toObject(CanNha.class) : null;
                    if (currentKhu != null)
                        currentKhu.setId(doc.getId());
                    bindFeesToUI(currentKhu);
                    loadExistingContract();
                })
                .addOnFailureListener(e -> {
                    bindFeesToUI(null);
                    loadExistingContract();
                });
    }

    private void bindFeesToUI(CanNha khu) {
        double giaXe = khu != null ? khu.getGiaXe() : 0;
        double giaInternet = khu != null ? khu.getGiaInternet() : 0;
        double giaGiatSay = khu != null ? khu.getGiaGiatSay() : 0;

        tvGuiXePrice.setText("Gửi xe " + formatVnd(giaXe) + "/chiếc");
        cbInternet.setText("Internet " + formatVnd(giaInternet) + "/phòng");
        cbGiatSay.setText("Giặt sấy " + formatVnd(giaGiatSay) + "/phòng");
    }

    private void bindPhongToUI() {
        etPhong.setText(currentPhong.getSoPhong() != null ? currentPhong.getSoPhong() : "");
        MoneyFormatter.setValue(etTienPhong, currentPhong.getGiaThue());

        // Defaults
        etSoHopDong.setText(generateContractNo());
        etNgayKy.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));

        // Default deposit = rent
        MoneyFormatter.setValue(etTienCoc, currentPhong.getGiaThue());
    }

    private void loadExistingContract() {
        scopedCollection("nguoi_thue")
                .whereEqualTo("idPhong", phongId)
                .get()
                .addOnSuccessListener(qs -> {
                    NguoiThue best = null;
                    if (qs != null) {
                        for (com.google.firebase.firestore.QueryDocumentSnapshot d : qs) {
                            NguoiThue n = d.toObject(NguoiThue.class);
                            if (n == null)
                                continue;
                            n.setId(d.getId());

                            // Backward compatible: missing status => ACTIVE
                            String st = n.getTrangThaiHopDong();
                            boolean ended = st != null && st.equalsIgnoreCase("ENDED");
                            if (ended)
                                continue;

                            best = n;
                            break;
                        }
                    }

                    if (best != null) {
                        currentContract = best;
                        applyModeView();
                    } else {
                        currentContract = new NguoiThue();
                        applyModeCreate();
                    }
                })
                .addOnFailureListener(e -> {
                    currentContract = new NguoiThue();
                    applyModeCreate();
                });
    }

    private void applyModeCreate() {
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle("Tạo hợp đồng");
        btnSave.setVisibility(View.VISIBLE);
        btnPrint.setVisibility(View.GONE);
        btnEnd.setVisibility(View.GONE);
        btnUpdate.setVisibility(View.GONE);

        setUploadEnabled(true);
    }

    private void applyModeView() {
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle("Xem hợp đồng");
        btnSave.setVisibility(View.GONE);
        btnPrint.setVisibility(View.VISIBLE);
        btnEnd.setVisibility(View.VISIBLE);
        btnUpdate.setVisibility(View.VISIBLE);

        bindContractToUI(currentContract);
        setUploadEnabled(true);
    }

    private void bindContractToUI(@NonNull NguoiThue c) {
        if (c.getSoHopDong() != null && !c.getSoHopDong().trim().isEmpty()) {
            etSoHopDong.setText(c.getSoHopDong());
        }

        etTenKhach.setText(nullToEmpty(c.getHoTen()));
        etDienThoai.setText(nullToEmpty(c.getSoDienThoai()));
        etDiaChi.setText(nullToEmpty(c.getDiaChi()));
        etCccd.setText(nullToEmpty(c.getCccd()));

        etSoNguoi.setText(c.getSoThanhVien() > 0 ? String.valueOf(c.getSoThanhVien()) : "");

        if (c.getTienPhong() > 0) {
            MoneyFormatter.setValue(etTienPhong, c.getTienPhong());
        }
        if (c.getTienCoc() > 0) {
            MoneyFormatter.setValue(etTienCoc, c.getTienCoc());
        }

        cbShowDeposit.setChecked(c.isHienThiTienCocTrenHoaDon());

        etNgayKy.setText(nullToEmpty(c.getNgayBatDauThue()));
        etSoThang.setText(c.getSoThangHopDong() > 0 ? String.valueOf(c.getSoThangHopDong()) : "");
        cbRemind.setChecked(c.isNhacTruoc1Thang());

        etChiSoDien.setText(c.getChiSoDienDau() > 0 ? String.valueOf(c.getChiSoDienDau()) : "");

        cbGuiXe.setChecked(c.isDichVuGuiXe());
        etSoXe.setEnabled(c.isDichVuGuiXe());
        etSoXe.setBackgroundResource(c.isDichVuGuiXe() ? R.drawable.bg_input_rounded : R.drawable.bg_input_disabled);
        etSoXe.setText(c.getSoLuongXe() > 0 ? String.valueOf(c.getSoLuongXe()) : "");

        cbInternet.setChecked(c.isDichVuInternet());
        cbGiatSay.setChecked(c.isDichVuGiatSay());

        etGhiChu.setText(nullToEmpty(c.getGhiChu()));
        cbShowNote.setChecked(c.isHienThiGhiChuTrenHoaDon());

        if (c.getCccdFrontUrl() != null && !c.getCccdFrontUrl().trim().isEmpty()) {
            Glide.with(this).load(c.getCccdFrontUrl()).centerCrop().into(ivFront);
        }
        if (c.getCccdBackUrl() != null && !c.getCccdBackUrl().trim().isEmpty()) {
            Glide.with(this).load(c.getCccdBackUrl()).centerCrop().into(ivBack);
        }
    }

    private void showDatePicker() {
        final Calendar cal = Calendar.getInstance();
        String cur = etNgayKy.getText() != null ? etNgayKy.getText().toString().trim() : "";
        try {
            if (!cur.isEmpty()) {
                Date d = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(cur);
                if (d != null) {
                    cal.setTime(d);
                }
            }
        } catch (Exception ignored) {
        }

        DatePickerDialog dlg = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar c = Calendar.getInstance();
            c.set(year, month, dayOfMonth);
            etNgayKy.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(c.getTime()));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dlg.show();
    }

    private void saveOrUpdate(boolean isCreate) {
        if (currentContract == null)
            currentContract = new NguoiThue();

        String soHD = text(etSoHopDong);
        String ten = text(etTenKhach);
        String sdt = text(etDienThoai);
        String diaChi = text(etDiaChi);
        String cccd = text(etCccd);
        String soNguoiStr = text(etSoNguoi);
        String ngayKy = text(etNgayKy);
        String soThangStr = text(etSoThang);
        String chiSoDienStr = text(etChiSoDien);

        if (ten.isEmpty() || sdt.isEmpty() || diaChi.isEmpty() || cccd.isEmpty() || soNguoiStr.isEmpty()
                || ngayKy.isEmpty() || soThangStr.isEmpty() || chiSoDienStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đủ các trường bắt buộc (*)", Toast.LENGTH_SHORT).show();
            return;
        }

        if (cbGuiXe.isChecked() && text(etSoXe).isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số lượng xe", Toast.LENGTH_SHORT).show();
            return;
        }

        int soNguoi;
        int soThang;
        int chiSoDien;
        int soXe = 0;
        try {
            soNguoi = Integer.parseInt(soNguoiStr);
            soThang = Integer.parseInt(soThangStr);
            chiSoDien = Integer.parseInt(chiSoDienStr);
            if (cbGuiXe.isChecked())
                soXe = Integer.parseInt(text(etSoXe));
        } catch (Exception e) {
            Toast.makeText(this, "Số liệu không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        double tienPhong = MoneyFormatter.getValue(etTienPhong);
        double tienCoc = MoneyFormatter.getValue(etTienCoc);

        currentContract.setSoHopDong(soHD);
        currentContract.setHoTen(ten);
        currentContract.setSoDienThoai(sdt);
        currentContract.setDiaChi(diaChi);
        currentContract.setCccd(cccd);

        currentContract.setIdPhong(phongId);
        currentContract.setSoPhong(currentPhong != null ? currentPhong.getSoPhong() : null);

        currentContract.setSoThanhVien(soNguoi);
        currentContract.setNgayBatDauThue(ngayKy);
        currentContract.setSoThangHopDong(soThang);
        currentContract.setNhacTruoc1Thang(cbRemind.isChecked());

        currentContract.setTienPhong(tienPhong);
        currentContract.setTienCoc(tienCoc);
        currentContract.setHienThiTienCocTrenHoaDon(cbShowDeposit.isChecked());

        currentContract.setChiSoDienDau(chiSoDien);
        currentContract.setDichVuGuiXe(cbGuiXe.isChecked());
        currentContract.setSoLuongXe(soXe);
        currentContract.setDichVuInternet(cbInternet.isChecked());
        currentContract.setDichVuGiatSay(cbGiatSay.isChecked());

        currentContract.setGhiChu(text(etGhiChu));
        currentContract.setHienThiGhiChuTrenHoaDon(cbShowNote.isChecked());

        currentContract.setTrangThaiHopDong("ACTIVE");

        String endDate = computeEndDate(ngayKy, soThang);
        currentContract.setNgayKetThucHopDong(endDate);

        long now = System.currentTimeMillis();
        if (currentContract.getCreatedAt() == null)
            currentContract.setCreatedAt(now);
        currentContract.setUpdatedAt(now);

        if (isCreate || currentContract.getId() == null || currentContract.getId().trim().isEmpty()) {
            scopedCollection("nguoi_thue").add(currentContract)
                    .addOnSuccessListener(ref -> {
                        currentContract.setId(ref.getId());
                        markRoomStatus(RoomStatus.RENTED);
                        Toast.makeText(this, "Đã lưu hợp đồng", Toast.LENGTH_SHORT).show();
                        applyModeView();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Lưu thất bại", Toast.LENGTH_SHORT).show());
        } else {
            scopedCollection("nguoi_thue").document(currentContract.getId()).set(currentContract)
                    .addOnSuccessListener(v -> {
                        markRoomStatus(RoomStatus.RENTED);
                        Toast.makeText(this, "Đã cập nhật", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show());
        }
    }

    private void confirmEndContract() {
        if (currentContract == null || currentContract.getId() == null)
            return;
        new AlertDialog.Builder(this)
                .setTitle("Kết thúc hợp đồng?")
                .setMessage("Phòng sẽ chuyển về trạng thái 'Trống'.")
                .setPositiveButton("Kết thúc", (d, w) -> endContract())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void endContract() {
        if (currentContract == null || currentContract.getId() == null)
            return;

        String oldRoomId = phongId;
        long now = System.currentTimeMillis();

        currentContract.setTrangThaiHopDong("ENDED");
        currentContract.setEndedAt(now);
        currentContract.setIdPhongCu(oldRoomId);
        currentContract.setIdPhong("");
        currentContract.setUpdatedAt(now);

        scopedCollection("nguoi_thue").document(currentContract.getId()).set(currentContract)
                .addOnSuccessListener(v -> {
                    markRoomStatus(RoomStatus.VACANT);
                    Toast.makeText(this, "Đã kết thúc hợp đồng", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Thao tác thất bại", Toast.LENGTH_SHORT).show());
    }

    private void markRoomStatus(@NonNull String status) {
        DocumentReference scope = scopedDoc();
        if (scope == null)
            return;
        scope.collection("phong_tro").document(phongId)
                .update("trangThai", status)
                .addOnFailureListener(e -> {
                    // ignore
                });
    }

    private void printContract() {
        if (currentContract == null) {
            Toast.makeText(this, "Chưa có dữ liệu", Toast.LENGTH_SHORT).show();
            return;
        }

        String html = buildContractHtml(currentContract);
        WebView webView = new WebView(this);
        webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null);

        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        if (printManager == null) {
            Toast.makeText(this, "Thiết bị không hỗ trợ in", Toast.LENGTH_SHORT).show();
            return;
        }

        String jobName = "HopDong_" + (currentContract.getSoHopDong() != null ? currentContract.getSoHopDong() : "");
        printManager.print(jobName, webView.createPrintDocumentAdapter(jobName),
                new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build());
    }

    private String buildContractHtml(@NonNull NguoiThue c) {
        String room = currentPhong != null ? nullToEmpty(currentPhong.getSoPhong()) : "";
        String khuDiaChi = currentKhu != null ? nullToEmpty(currentKhu.getDiaChi()) : "";
        String tenChuTro = currentKhu != null ? nullToEmpty(currentKhu.getTenKhu()) : "";
        String sdtChuTro = currentKhu != null ? nullToEmpty(currentKhu.getSdtQuanLy()) : "";
        String ngayKy = nullToEmpty(c.getNgayBatDauThue());
        String ngayKetThuc = nullToEmpty(c.getNgayKetThucHopDong());
        int soThang = c.getSoThangHopDong();

        // Build chi phí section
        StringBuilder chiPhi = new StringBuilder();
        if (currentKhu != null) {
            chiPhi.append("- Tiền điện: ").append(formatVndNumberOnly(currentKhu.getGiaDien())).append("/kWh");
            chiPhi.append(" (Chỉ số ban đầu: ").append(c.getChiSoDienDau()).append(")<br/>");
            String cachTinhNuoc = currentKhu.getCachTinhNuoc();
            String donViNuoc = "nguoi".equals(cachTinhNuoc) ? "/người/tháng"
                    : "dong_ho".equals(cachTinhNuoc) ? "/m³" : "/phòng";
            chiPhi.append("- Tiền nước: ").append(formatVndNumberOnly(currentKhu.getGiaNuoc())).append(donViNuoc)
                    .append("<br/>");
            if (c.isDichVuGuiXe()) {
                chiPhi.append("- Phí gửi xe: ").append(formatVndNumberOnly(currentKhu.getGiaXe()))
                        .append("/chiếc (SL: ").append(c.getSoLuongXe()).append(")<br/>");
            }
            if (c.isDichVuInternet()) {
                chiPhi.append("- Internet: ").append(formatVndNumberOnly(currentKhu.getGiaInternet()))
                        .append("/tháng<br/>");
            }
            if (c.isDichVuGiatSay()) {
                chiPhi.append("- Giặt sấy: ").append(formatVndNumberOnly(currentKhu.getGiaGiatSay()))
                        .append("/tháng<br/>");
            }
        }

        return "<!DOCTYPE html><html><head><meta charset='utf-8'/>" +
                "<style>" +
                "body{font-family:'Times New Roman',serif;font-size:13px;padding:30px 40px;line-height:1.6;color:#000;}"
                +
                ".header{text-align:center;margin-bottom:20px;}" +
                ".header h3{margin:0;font-size:14px;font-weight:bold;text-transform:uppercase;}" +
                ".header p{margin:2px 0;font-size:12px;font-style:italic;}" +
                ".title{text-align:center;margin:25px 0;}" +
                ".title h2{margin:0;font-size:18px;font-weight:bold;text-transform:uppercase;}" +
                ".info{margin-bottom:8px;}" +
                ".section{margin-top:15px;}" +
                ".section-title{font-weight:bold;margin-bottom:5px;}" +
                ".indent{padding-left:20px;}" +
                ".gia-han-table{width:100%;border-collapse:collapse;margin:10px 0;}" +
                ".gia-han-table th,.gia-han-table td{border:1px solid #000;padding:6px 8px;text-align:center;font-size:12px;}"
                +
                ".gia-han-table th{background:#f5f5f5;font-weight:bold;}" +
                ".signature{display:flex;justify-content:space-around;margin-top:40px;text-align:center;}" +
                ".signature div{width:40%;}" +
                ".signature .label{font-weight:bold;text-transform:uppercase;}" +
                ".signature .note{font-style:italic;font-size:11px;}" +
                ".signature .name{margin-top:60px;font-weight:bold;}" +
                "</style></head><body>" +

                // Header
                "<div class='header'>" +
                "<h3>CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM</h3>" +
                "<p>Độc lập – Tự do – Hạnh phúc</p>" +
                "</div>" +

                // Title
                "<div class='title'>" +
                "<h2>HỢP ĐỒNG THUÊ PHÒNG TRỌ</h2>" +
                "</div>" +

                // Opening
                "<p class='info'>Hôm nay, ngày " + formatDateFull(ngayKy) + ", tại căn nhà số " + escape(khuDiaChi)
                + "</p>" +
                "<p class='info'>Chúng tôi ký tên dưới đây gồm có:</p>" +

                // Bên A
                "<div class='section'>" +
                "<p class='section-title'>BÊN CHO THUÊ PHÒNG TRỌ (gọi tắt là Bên A):</p>" +
                "<p class='indent'>Ông/bà: " + escape(tenChuTro) + "</p>" +
                "<p class='indent'>CMND/CCCD số: .................. cấp ngày: ............... nơi cấp: ...............</p>"
                +
                "<p class='indent'>Điện thoại: " + escape(sdtChuTro) + "</p>" +
                "<p class='indent'>Thường trú tại: " + escape(khuDiaChi) + "</p>" +
                "</div>" +

                // Bên B
                "<div class='section'>" +
                "<p class='section-title'>BÊN THUÊ PHÒNG TRỌ (gọi tắt là Bên B):</p>" +
                "<p class='indent'>Ông/bà: " + escape(c.getHoTen()) + "</p>" +
                "<p class='indent'>CMND/CCCD số: " + escape(c.getCccd())
                + " cấp ngày: ............... nơi cấp: ...............</p>" +
                "<p class='indent'>Điện thoại: " + escape(c.getSoDienThoai()) + "</p>" +
                "<p class='indent'>Thường trú tại: " + escape(c.getDiaChi()) + "</p>" +
                "</div>" +

                "<p style='margin-top:15px;'>Sau khi thỏa thuận, hai bên thống nhất như sau:</p>" +

                // Điều 1
                "<div class='section'>" +
                "<p class='section-title'>1. Nội dung thuê phòng trọ</p>" +
                "<p class='indent'>Bên A cho Bên B thuê 01 phòng trọ số " + escape(room) + " tại căn nhà số "
                + escape(khuDiaChi) + ".</p>" +
                "<p class='indent'>Với thời hạn là: " + soThang + " tháng (Từ ngày " + escape(ngayKy) + " đến ngày "
                + escape(ngayKetThuc) + ").</p>" +
                "<p class='indent'>Giá thuê: " + formatVnd(c.getTienPhong()) + "/tháng.</p>" +
                "<p class='indent'>Chưa bao gồm chi phí:</p>" +
                "<p class='indent'>" + chiPhi.toString() + "</p>" +
                "</div>" +

                // Điều 2
                "<div class='section'>" +
                "<p class='section-title'>2. Trách nhiệm Bên A</p>" +
                "<p class='indent'>- Đảm bảo căn nhà cho thuê không có tranh chấp, khiếu kiện.</p>" +
                "<p class='indent'>- Đăng ký với chính quyền địa phương về thủ tục cho thuê phòng trọ.</p>" +
                "</div>" +

                // Điều 3
                "<div class='section'>" +
                "<p class='section-title'>3. Trách nhiệm Bên B</p>" +
                "<p class='indent'>- Đặt cọc với số tiền là " + formatVnd(c.getTienCoc())
                + ", thanh toán tiền thuê phòng hàng tháng + tiền điện + nước.</p>" +
                "<p class='indent'>- Đảm bảo các thiết bị và sửa chữa các hư hỏng trong phòng trong khi sử dụng. Nếu không sửa chữa thì khi trả phòng, bên A sẽ trừ vào tiền đặt cọc, giá trị cụ thể tính theo giá thị trường.</p>"
                +
                "<p class='indent'>- Chỉ sử dụng phòng trọ vào mục đích ở, với số lượng tối đa không quá "
                + c.getSoThanhVien()
                + " người; không chứa các thiết bị gây cháy nổ, hàng cấm; cung cấp giấy tờ tùy thân để đăng ký tạm trú theo quy định, giữ gìn an ninh trật tự, nếp sống văn hóa đô thị; không tụ tập nhậu nhẹt, cờ bạc và các hành vi vi phạm pháp luật khác.</p>"
                +
                "<p class='indent'>- Không được tự ý cải tạo kiến trúc phòng hoặc trang trí ảnh hưởng tới tường, cột, nền... Nếu có nhu cầu trên phải trao đổi với bên A để được thống nhất.</p>"
                +
                "</div>" +

                // Điều 4
                "<div class='section'>" +
                "<p class='section-title'>4. Điều khoản thực hiện</p>" +
                "<p class='indent'>Hai bên nghiêm túc thực hiện những quy định trên trong thời hạn cho thuê, nếu bên A lấy phòng phải báo cho bên B ít nhất 01 tháng, hoặc ngược lại.</p>"
                +
                "<p class='indent'>Sau thời hạn cho thuê " + soThang
                + " tháng nếu bên B có nhu cầu hai bên tiếp tục thương lượng giá thuê để gia hạn hợp đồng.</p>" +
                "</div>" +

                // Bảng gia hạn
                "<div class='section'>" +
                "<p class='section-title'>Bảng gia hạn hợp đồng:</p>" +
                "<table class='gia-han-table'>" +
                "<tr><th>Lần</th><th>Thời gian<br/>(tháng)</th><th>Từ ngày</th><th>Đến ngày</th><th>Giá thuê/tháng</th><th>Ký tên</th></tr>"
                +
                "<tr><td>1</td><td></td><td></td><td></td><td></td><td></td></tr>" +
                "<tr><td>2</td><td></td><td></td><td></td><td></td><td></td></tr>" +
                "</table>" +
                "</div>" +

                // Signature
                "<div class='signature'>" +
                "<div>" +
                "<p class='label'>BÊN B</p>" +
                "<p class='note'>(Ký, ghi rõ họ tên)</p>" +
                "<p class='name'>" + escape(c.getHoTen()) + "</p>" +
                "</div>" +
                "<div>" +
                "<p class='label'>BÊN A</p>" +
                "<p class='note'>(Ký, ghi rõ họ tên)</p>" +
                "<p class='name'>" + escape(tenChuTro) + "</p>" +
                "</div>" +
                "</div>" +

                "</body></html>";
    }

    private String formatDateFull(String dateStr) {
        if (dateStr == null || dateStr.isEmpty())
            return "... tháng ... năm ...";
        try {
            String[] parts = dateStr.split("/");
            if (parts.length == 3) {
                return parts[0] + " tháng " + parts[1] + " năm " + parts[2];
            }
        } catch (Exception ignored) {
        }
        return dateStr;
    }

    private String row(String k, String v) {
        return "<tr><td class='k'>" + escape(k) + "</td><td class='v'>" + escape(v) + "</td></tr>";
    }

    private String escape(String s) {
        if (s == null)
            return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String computeEndDate(String start, int months) {
        try {
            Date d = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(start);
            if (d == null)
                return "";
            Calendar c = Calendar.getInstance();
            c.setTime(d);
            c.add(Calendar.MONTH, months);
            return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(c.getTime());
        } catch (Exception e) {
            return "";
        }
    }

    private String generateContractNo() {
        return new SimpleDateFormat("ddMMyyyyHHmmss", Locale.getDefault()).format(new Date());
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private String text(@NonNull EditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private double parseMoney(String s) {
        if (s == null)
            return 0;
        String raw = s.replaceAll("[^0-9]", "");
        if (raw.isEmpty())
            return 0;
        try {
            return Double.parseDouble(raw);
        } catch (Exception e) {
            return 0;
        }
    }

    private String formatVnd(double value) {
        NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        return fmt.format((long) value) + " đ";
    }

    private String formatVndNumberOnly(double value) {
        NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        return fmt.format((long) value) + " đ";
    }
}
