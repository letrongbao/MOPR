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
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
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
import androidx.core.view.WindowInsetsControllerCompat;
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
import com.example.myapplication.domain.RentalHistory;
import com.example.myapplication.features.property.room.PhongTroActivity;
import com.example.myapplication.core.repository.domain.RentalHistoryRepository;
import com.google.firebase.Timestamp;
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
    private String editContractId; // ID hợp đồng đang edit
    private boolean isEditMode = false; // Flag để phân biệt mode CREATE/EDIT

    private PhongTro currentPhong;
    private CanNha currentKhu;

    private NguoiThue currentContract;

    private EditText etSoHopDong, etTenKhach, etDienThoai, etCccd;
    private ImageView ivFront, ivBack, ivEditFront, ivEditBack;
    private EditText etPhong, etSoNguoi, etTienPhong, etTienCoc;
    private CheckBox cbShowDeposit;
    private EditText etNgayKy, etSoThang;
    private ImageView ivPickDate;
    private CheckBox cbRemind;
    private RadioGroup rgBillingReminder;
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

    private RentalHistoryRepository rentalHistoryRepo;
    private TextView tvTitleLine1, tvTitleLine2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);

        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(window,
                window.getDecorView());
        if (windowInsetsController != null) {
            windowInsetsController.setAppearanceLightStatusBars(false);
        }

        // Kiểm tra mode: CREATE hoặc EDIT
        String mode = getIntent().getStringExtra("MODE");
        isEditMode = "EDIT".equals(mode);

        if (isEditMode) {
            // Mode EDIT: Lấy dữ liệu từ Intent
            editContractId = getIntent().getStringExtra("CONTRACT_ID");
            phongId = getIntent().getStringExtra("PHONG_ID");

            if (editContractId == null || editContractId.trim().isEmpty()) {
                Toast.makeText(this, "Lỗi: Không tìm thấy hợp đồng", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        } else {
            // Mode CREATE: Lấy PHONG_ID như cũ
            phongId = getIntent().getStringExtra(EXTRA_PHONG_ID);
            if (phongId == null || phongId.trim().isEmpty()) {
                Toast.makeText(this, "Thiếu ID phòng", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
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
        LocalBroadcastManager.getInstance(this).registerReceiver(uploadReceiver,
                new IntentFilter(ImageUploadService.ACTION_UPLOAD_COMPLETE));

        rentalHistoryRepo = new RentalHistoryRepository();

        setContentView(R.layout.activity_hop_dong);

        Toolbar toolbar = findViewById(R.id.toolbar);
        tvTitleLine1 = findViewById(R.id.tvTitleLine1);
        tvTitleLine2 = findViewById(R.id.tvTitleLine2);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        View appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(appBarLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(0, systemBars.top, 0, 0);
                return insets;
            });
        }

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
        MoneyFormatter.applyTo(etTienPhong);
        MoneyFormatter.applyTo(etTienCoc);
        etNgayKy = findViewById(R.id.etNgayKy);
        etSoThang = findViewById(R.id.etSoThang);
        ivPickDate = findViewById(R.id.ivPickDate);
        cbRemind = findViewById(R.id.cbRemind);
        rgBillingReminder = findViewById(R.id.rgBillingReminder);
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
        scopedCollection("phong_tro").document(phongId).get()
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
        String canNhaId = currentPhong.getCanNhaId();
        if (canNhaId == null || canNhaId.trim().isEmpty()) {
            currentKhu = null;
            bindFeesToUI(null);
            loadExistingContract();
            return;
        }
        scopedCollection("can_nha").document(canNhaId).get()
                .addOnSuccessListener(doc -> {
                    currentKhu = doc != null && doc.exists() ? doc.toObject(CanNha.class) : null;
                    if (currentKhu != null)
                        currentKhu.setId(doc.getId());
                    bindFeesToUI(currentKhu);
                    loadExistingContract();
                    updateFullToolbarHeader();
                })
                .addOnFailureListener(e -> {
                    bindFeesToUI(null);
                    loadExistingContract();
                    updateFullToolbarHeader();
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
        etSoHopDong.setText(generateContractNo());
        etNgayKy.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
        MoneyFormatter.setValue(etTienCoc, currentPhong.getGiaThue());
        updateFullToolbarHeader();
    }

    private void loadExistingContract() {
        // Nếu đang ở EDIT mode, load theo ID hợp đồng
        if (isEditMode && editContractId != null) {
            loadContractById(editContractId);
            return;
        }

        // Mode CREATE: load existing contract theo phòng (như cũ)
        scopedCollection("nguoi_thue")
                .whereEqualTo("idPhong", phongId)
                .whereEqualTo("trangThaiHopDong", "ACTIVE")
                .limit(1).get()
                .addOnSuccessListener(qs -> {
                    if (qs != null && !qs.isEmpty()) {
                        com.google.firebase.firestore.DocumentSnapshot d = qs.getDocuments().get(0);
                        NguoiThue n = d.toObject(NguoiThue.class);
                        if (n != null) {
                            n.setId(d.getId());
                            currentContract = n;
                            applyModeView();
                            return;
                        }
                    }
                    loadExistingContractLegacyFallback();
                })
                .addOnFailureListener(e -> loadExistingContractLegacyFallback());
    }

    /**
     * Load hợp đồng theo ID (dùng cho EDIT mode)
     */
    private void loadContractById(String contractId) {
        scopedCollection("nguoi_thue").document(contractId).get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        NguoiThue n = doc.toObject(NguoiThue.class);
                        if (n != null) {
                            n.setId(doc.getId());
                            currentContract = n;

                            // Pre-fill tất cả dữ liệu từ Intent (fallback nếu Firestore thiếu)
                            fillDataFromIntent();

                            // Apply mode EDIT
                            applyModeEdit();
                            return;
                        }
                    }
                    Toast.makeText(this, "Không tìm thấy hợp đồng", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải hợp đồng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    /**
     * Điền dữ liệu từ Intent khi EDIT
     */
    private void fillDataFromIntent() {
        Intent intent = getIntent();

        if (currentContract == null) {
            currentContract = new NguoiThue();
        }

        // Lấy dữ liệu từ Intent và điền vào currentContract
        if (intent.hasExtra("SO_HOP_DONG"))
            currentContract.setSoHopDong(intent.getStringExtra("SO_HOP_DONG"));
        if (intent.hasExtra("HO_TEN"))
            currentContract.setHoTen(intent.getStringExtra("HO_TEN"));
        if (intent.hasExtra("SO_DIEN_THOAI"))
            currentContract.setSoDienThoai(intent.getStringExtra("SO_DIEN_THOAI"));
        if (intent.hasExtra("CCCD"))
            currentContract.setCccd(intent.getStringExtra("CCCD"));
        if (intent.hasExtra("SO_THANH_VIEN"))
            currentContract.setSoThanhVien(intent.getIntExtra("SO_THANH_VIEN", 0));
        if (intent.hasExtra("NGAY_BAT_DAU"))
            currentContract.setNgayBatDauThue(intent.getStringExtra("NGAY_BAT_DAU"));
        if (intent.hasExtra("SO_THANG"))
            currentContract.setSoThangHopDong(intent.getIntExtra("SO_THANG", 0));
        if (intent.hasExtra("GIA_THUE"))
            currentContract.setGiaThue(intent.getLongExtra("GIA_THUE", 0));
        if (intent.hasExtra("TIEN_COC"))
            currentContract.setTienCoc(intent.getLongExtra("TIEN_COC", 0));
        if (intent.hasExtra("CHI_SO_DIEN"))
            currentContract.setChiSoDienDau(intent.getIntExtra("CHI_SO_DIEN", 0));
        if (intent.hasExtra("DICH_VU_GUI_XE"))
            currentContract.setDichVuGuiXe(intent.getBooleanExtra("DICH_VU_GUI_XE", false));
        if (intent.hasExtra("SO_LUONG_XE"))
            currentContract.setSoLuongXe(intent.getIntExtra("SO_LUONG_XE", 0));
        if (intent.hasExtra("DICH_VU_INTERNET"))
            currentContract.setDichVuInternet(intent.getBooleanExtra("DICH_VU_INTERNET", false));
        if (intent.hasExtra("DICH_VU_GIAT_SAY"))
            currentContract.setDichVuGiatSay(intent.getBooleanExtra("DICH_VU_GIAT_SAY", false));
        if (intent.hasExtra("GHI_CHU"))
            currentContract.setGhiChu(intent.getStringExtra("GHI_CHU"));
        if (intent.hasExtra("HIEN_THI_COC"))
            currentContract.setHienThiTienCocTrenHoaDon(intent.getBooleanExtra("HIEN_THI_COC", false));
        if (intent.hasExtra("HIEN_THI_GHI_CHU"))
            currentContract.setHienThiGhiChuTrenHoaDon(intent.getBooleanExtra("HIEN_THI_GHI_CHU", false));
        if (intent.hasExtra("NHAC_TRUOC_1_THANG"))
            currentContract.setNhacTruoc1Thang(intent.getBooleanExtra("NHAC_TRUOC_1_THANG", false));
        if (intent.hasExtra("NHAC_BAO_PHI_VAO"))
            currentContract.setNhacBaoPhiVao(intent.getStringExtra("NHAC_BAO_PHI_VAO"));
        if (intent.hasExtra("CCCD_FRONT_URL"))
            currentContract.setCccdFrontUrl(intent.getStringExtra("CCCD_FRONT_URL"));
        if (intent.hasExtra("CCCD_BACK_URL"))
            currentContract.setCccdBackUrl(intent.getStringExtra("CCCD_BACK_URL"));
    }

    private void loadExistingContractLegacyFallback() {
        scopedCollection("nguoi_thue").whereEqualTo("idPhong", phongId).limit(1).get()
                .addOnSuccessListener(qs -> {
                    NguoiThue best = null;
                    if (qs != null && !qs.isEmpty()) {
                        com.google.firebase.firestore.DocumentSnapshot d = qs.getDocuments().get(0);
                        NguoiThue n = d.toObject(NguoiThue.class);
                        if (n != null) {
                            String st = n.getTrangThaiHopDong();
                            if (st == null || !st.equalsIgnoreCase("ENDED")) {
                                n.setId(d.getId());
                                best = n;
                            }
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
        btnSave.setVisibility(View.VISIBLE);
        btnPrint.setVisibility(View.GONE);
        btnEnd.setVisibility(View.GONE);
        btnUpdate.setVisibility(View.GONE);
        setUploadEnabled(true);
    }

    private void applyModeView() {
        btnSave.setVisibility(View.GONE);
        btnPrint.setVisibility(View.VISIBLE);
        btnEnd.setVisibility(View.VISIBLE);
        btnUpdate.setVisibility(View.VISIBLE);
        bindContractToUI(currentContract);
        setUploadEnabled(true);
    }

    /**
     * Apply mode EDIT: Hiển thị nút "Cập nhật", ẩn các nút khác, pre-fill dữ liệu
     */
    private void applyModeEdit() {
        btnSave.setVisibility(View.GONE);
        btnPrint.setVisibility(View.GONE);
        btnEnd.setVisibility(View.GONE);
        btnUpdate.setVisibility(View.VISIBLE);

        // Đổi text nút thành "Cập nhật"
        btnUpdate.setText("Cập nhật");

        // Đổi tiêu đề toolbar
        if (tvTitleLine1 != null) {
            tvTitleLine1.setText("Chỉnh sửa hợp đồng");
        }

        // Pre-fill toàn bộ dữ liệu vào form
        bindContractToUI(currentContract);
        setUploadEnabled(true);
    }

    private void updateFullToolbarHeader() {
        String title = "Hợp đồng";
        if (currentPhong != null)
            title = "Hợp đồng phòng " + nullToEmpty(currentPhong.getSoPhong());
        String subtitle = "";
        if (currentKhu != null) {
            subtitle = (currentKhu.getDiaChi() != null && !currentKhu.getDiaChi().trim().isEmpty())
                    ? currentKhu.getDiaChi()
                    : currentKhu.getTenCanNha();
        } else if (currentPhong != null)
            subtitle = currentPhong.getCanNhaTen();
        if (tvTitleLine1 != null)
            tvTitleLine1.setText(nullToEmpty(title));
        if (tvTitleLine2 != null)
            tvTitleLine2.setText(nullToEmpty(subtitle));
    }

    private void bindContractToUI(@NonNull NguoiThue c) {
        if (c.getSoHopDong() != null && !c.getSoHopDong().trim().isEmpty())
            etSoHopDong.setText(c.getSoHopDong());
        etTenKhach.setText(nullToEmpty(c.getHoTen()));
        etDienThoai.setText(nullToEmpty(c.getSoDienThoai()));
        etCccd.setText(nullToEmpty(c.getCccd()));
        etSoNguoi.setText(c.getSoThanhVien() > 0 ? String.valueOf(c.getSoThanhVien()) : "");

        // Sử dụng long fields nếu có, fallback về double cũ
        if (c.getGiaThue() > 0) {
            MoneyFormatter.setValue(etTienPhong, (double) c.getGiaThue());
        } else if (c.getTienPhong() > 0) {
            MoneyFormatter.setValue(etTienPhong, c.getTienPhong());
        }

        if (c.getTienCoc() > 0) {
            MoneyFormatter.setValue(etTienCoc, (double) c.getTienCoc());
        } else if (c.getTienCoc_old() > 0) {
            MoneyFormatter.setValue(etTienCoc, c.getTienCoc_old());
        }

        cbShowDeposit.setChecked(c.isHienThiTienCocTrenHoaDon());
        etNgayKy.setText(nullToEmpty(c.getNgayBatDauThue()));
        etSoThang.setText(c.getSoThangHopDong() > 0 ? String.valueOf(c.getSoThangHopDong()) : "");
        cbRemind.setChecked(c.isNhacTruoc1Thang());
        bindBillingReminderSelection(c.getNhacBaoPhiVao());
        etChiSoDien.setText(c.getChiSoDienDau() > 0 ? String.valueOf(c.getChiSoDienDau()) : "");
        cbGuiXe.setChecked(c.isDichVuGuiXe());
        etSoXe.setEnabled(c.isDichVuGuiXe());
        etSoXe.setBackgroundResource(c.isDichVuGuiXe() ? R.drawable.bg_input_rounded : R.drawable.bg_input_disabled);
        etSoXe.setText(c.getSoLuongXe() > 0 ? String.valueOf(c.getSoLuongXe()) : "");
        cbInternet.setChecked(c.isDichVuInternet());
        cbGiatSay.setChecked(c.isDichVuGiatSay());
        etGhiChu.setText(nullToEmpty(c.getGhiChu()));
        cbShowNote.setChecked(c.isHienThiGhiChuTrenHoaDon());
        if (c.getCccdFrontUrl() != null && !c.getCccdFrontUrl().trim().isEmpty())
            Glide.with(this).load(c.getCccdFrontUrl()).centerCrop().into(ivFront);
        if (c.getCccdBackUrl() != null && !c.getCccdBackUrl().trim().isEmpty())
            Glide.with(this).load(c.getCccdBackUrl()).centerCrop().into(ivBack);
    }

    private void showDatePicker() {
        final Calendar cal = Calendar.getInstance();
        String cur = etNgayKy.getText() != null ? etNgayKy.getText().toString().trim() : "";
        try {
            if (!cur.isEmpty()) {
                Date d = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(cur);
                if (d != null)
                    cal.setTime(d);
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
        String soHD = text(etSoHopDong), ten = text(etTenKhach), sdt = text(etDienThoai), cccd = text(etCccd);
        String soNguoiStr = text(etSoNguoi), ngayKy = text(etNgayKy), soThangStr = text(etSoThang),
                chiSoDienStr = text(etChiSoDien);
        if (ten.isEmpty() || sdt.isEmpty() || cccd.isEmpty() || soNguoiStr.isEmpty() || ngayKy.isEmpty()
                || soThangStr.isEmpty() || chiSoDienStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đủ các trường bắt buộc (*)", Toast.LENGTH_SHORT).show();
            return;
        }
        if (cbGuiXe.isChecked() && text(etSoXe).isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số lượng xe", Toast.LENGTH_SHORT).show();
            return;
        }
        int soNguoi, soThang, chiSoDien, soXe = 0;
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
        currentContract.setDiaChi("");
        currentContract.setCccd(cccd);
        currentContract.setIdPhong(phongId);
        currentContract.setSoPhong(currentPhong != null ? currentPhong.getSoPhong() : null);
        currentContract.setSoThanhVien(soNguoi);
        currentContract.setNgayBatDauThue(ngayKy);
        currentContract.setSoThangHopDong(soThang);
        currentContract.setNhacTruoc1Thang(cbRemind.isChecked());
        currentContract.setNhacBaoPhiVao(getSelectedBillingReminder());

        // Set both old (double) and new (long) fields for compatibility
        currentContract.setTienPhong(tienPhong);
        currentContract.setGiaThue((long) tienPhong); // New long field
        currentContract.setTienCoc((long) tienCoc); // New long field
        currentContract.setHienThiTienCocTrenHoaDon(cbShowDeposit.isChecked());
        currentContract.setChiSoDienDau(chiSoDien);
        currentContract.setDichVuGuiXe(cbGuiXe.isChecked());
        currentContract.setSoLuongXe(soXe);
        currentContract.setDichVuInternet(cbInternet.isChecked());
        currentContract.setDichVuGiatSay(cbGiatSay.isChecked());
        currentContract.setGhiChu(text(etGhiChu));
        currentContract.setHienThiGhiChuTrenHoaDon(cbShowNote.isChecked());
        currentContract.setTrangThaiHopDong("ACTIVE");
        currentContract.setNgayKetThucHopDong(computeEndDate(ngayKy, soThang));
        long now = System.currentTimeMillis();
        if (currentContract.getCreatedAt() == null)
            currentContract.setCreatedAt(now);
        currentContract.setUpdatedAt(now);
        if (isCreate || currentContract.getId() == null || currentContract.getId().trim().isEmpty()) {
            // Mode CREATE: Thêm mới
            scopedCollection("nguoi_thue").add(currentContract)
                    .addOnSuccessListener(ref -> {
                        currentContract.setId(ref.getId());
                        markRoomStatus(RoomStatus.RENTED);
                        Toast.makeText(this, "✓ Đã lưu hợp đồng", Toast.LENGTH_SHORT).show();
                        navigateToRoomList(RoomStatus.RENTED);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "❌ Lưu thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } else {
            // Mode UPDATE hoặc EDIT: Cập nhật
            String updateId = isEditMode && editContractId != null ? editContractId : currentContract.getId();

            scopedCollection("nguoi_thue").document(updateId).set(currentContract)
                    .addOnSuccessListener(v -> {
                        markRoomStatus(RoomStatus.RENTED);
                        Toast.makeText(this, "✓ Đã cập nhật hợp đồng", Toast.LENGTH_SHORT).show();

                        // Nếu đang ở EDIT mode, quay về màn hình danh sách
                        if (isEditMode) {
                            finish(); // Quay về HopDongListActivity
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "❌ Cập nhật thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
    }

    private void bindBillingReminderSelection(String value) {
        if (rgBillingReminder == null)
            return;
        String remind = normalizeBillingReminder(value);
        if ("end_month".equals(remind)) {
            rgBillingReminder.check(R.id.rbRemindEndMonth);
        } else if ("mid_month".equals(remind)) {
            rgBillingReminder.check(R.id.rbRemindMidMonth);
        } else {
            rgBillingReminder.check(R.id.rbRemindStartMonth);
        }
    }

    private String getSelectedBillingReminder() {
        if (rgBillingReminder == null)
            return "start_month";
        int checkedId = rgBillingReminder.getCheckedRadioButtonId();
        if (checkedId == R.id.rbRemindEndMonth)
            return "end_month";
        if (checkedId == R.id.rbRemindMidMonth)
            return "mid_month";
        return "start_month";
    }

    private String normalizeBillingReminder(String value) {
        if (value == null || value.trim().isEmpty()) {
            if (currentKhu != null && currentKhu.getNhacBaoPhi() != null
                    && !currentKhu.getNhacBaoPhi().trim().isEmpty()) {
                String legacy = currentKhu.getNhacBaoPhi();
                if ("end_month".equals(legacy))
                    return "end_month";
                return "start_month";
            }
            return "start_month";
        }
        if ("end_month".equals(value) || "mid_month".equals(value) || "start_month".equals(value))
            return value;
        return "start_month";
    }

    private void confirmEndContract() {
        if (currentContract == null || currentContract.getId() == null)
            return;
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận kết thúc hợp đồng")
                .setMessage(
                        "Bạn có chắc chắn muốn kết thúc hợp đồng này?\n\n• Phòng sẽ chuyển về trạng thái 'Trống'\n• Thông tin hợp đồng sẽ được lưu vào lịch sử")
                .setPositiveButton("Kết thúc", (d, w) -> endContract())
                .setNegativeButton("Hủy", null).setCancelable(false).show();
    }

    private void endContract() {
        if (currentContract == null || currentContract.getId() == null)
            return;
        String oldRoomId = phongId;
        long now = System.currentTimeMillis();
        createRentalHistoryLog(currentContract, oldRoomId, now);
        currentContract.setTrangThaiHopDong("ENDED");
        currentContract.setEndedAt(now);
        currentContract.setIdPhongCu(oldRoomId);
        currentContract.setIdPhong("");
        currentContract.setUpdatedAt(now);
        scopedCollection("nguoi_thue").document(currentContract.getId()).set(currentContract)
                .addOnSuccessListener(v -> {
                    markRoomStatus(RoomStatus.VACANT);
                    Toast.makeText(this, "Đã kết thúc hợp đồng và lưu lịch sử", Toast.LENGTH_SHORT).show();
                    navigateToRoomList(RoomStatus.VACANT);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Thao tác thất bại", Toast.LENGTH_SHORT).show());
    }

    private void navigateToRoomList(@NonNull String status) {
        Intent intent = new Intent(this, PhongTroActivity.class);
        intent.putExtra("FILTER_STATUS", status);
        if (currentPhong != null && currentPhong.getCanNhaId() != null
                && !currentPhong.getCanNhaId().trim().isEmpty()) {
            intent.putExtra("CAN_NHA_ID", currentPhong.getCanNhaId());
            intent.putExtra("CAN_NHA_NAME", currentPhong.getCanNhaTen());
            if (currentKhu != null)
                intent.putExtra("CAN_NHA_ADDR", currentKhu.getDiaChi());
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void createRentalHistoryLog(NguoiThue contract, String roomId, long endTime) {
        RentalHistory history = new RentalHistory();
        history.setIdHopDong(contract.getId());
        history.setIdPhong(roomId);
        history.setIdNguoiThue(contract.getId());
        if (currentPhong != null) {
            history.setSoPhong(currentPhong.getSoPhong());
            history.setCanNhaTen(currentPhong.getCanNhaTen());
            history.setTang(currentPhong.getTang());
        } else
            history.setSoPhong(contract.getSoPhong());
        history.setHoTen(contract.getHoTen());
        history.setCccd(contract.getCccd());
        history.setSoDienThoai(contract.getSoDienThoai());
        history.setDiaChi(contract.getDiaChi());
        history.setSoHopDong(contract.getSoHopDong());
        history.setSoThanhVien(contract.getSoThanhVien());
        history.setNgayBatDauThue(contract.getNgayBatDauThue());
        history.setNgayKetThucHopDong(contract.getNgayKetThucHopDong());
        history.setSoThangHopDong(contract.getSoThangHopDong());
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        history.setNgayKetThucThucTe(sdf.format(new Date(endTime)));
        if (contract.getNgayBatDauThue() != null && !contract.getNgayBatDauThue().isEmpty()) {
            try {
                Date startDate = sdf.parse(contract.getNgayBatDauThue());
                if (startDate != null) {
                    long diff = endTime - startDate.getTime();
                    history.setSoNgayThueThucTe((int) (diff / (1000 * 60 * 60 * 24)));
                    history.setStartTimestamp(startDate.getTime());
                }
            } catch (Exception e) {
                history.setSoNgayThueThucTe(0);
            }
        }
        history.setEndTimestamp(endTime);
        history.setTienPhong(contract.getTienPhong());
        history.setTienCoc(contract.getTienCoc());
        history.setDichVuGuiXe(contract.isDichVuGuiXe());
        history.setDichVuInternet(contract.isDichVuInternet());
        history.setDichVuGiatSay(contract.isDichVuGiatSay());
        history.setSoLuongXe(contract.getSoLuongXe());
        history.setGhiChu(contract.getGhiChu());
        history.setLyDoKetThuc("Kết thúc hợp đồng");
        history.setCreatedAt(Timestamp.now());
        calculateInvoiceStats(contract.getId(), (totalPaid, paidCount, unpaidCount) -> {
            history.setTongTienDaThanhToan(totalPaid);
            history.setSoHoaDonDaThanhToan(paidCount);
            history.setSoHoaDonChuaThanhToan(unpaidCount);
            rentalHistoryRepo.addHistory(history)
                    .addOnSuccessListener(ref -> {
                        history.setId(ref.getId());
                        android.util.Log.d("HopDongActivity", "Đã lưu lịch sử cho thuê: " + ref.getId());
                    })
                    .addOnFailureListener(
                            e -> android.util.Log.e("HopDongActivity", "Lỗi lưu lịch sử: " + e.getMessage()));
        });
    }

    private interface InvoiceStatsCallback {
        void onDone(double totalPaid, int paidCount, int unpaidCount);
    }

    private void calculateInvoiceStats(String tenantId, @NonNull InvoiceStatsCallback callback) {
        scopedCollection("hoa_don").whereEqualTo("idNguoiThue", tenantId).get()
                .addOnSuccessListener(querySnapshot -> {
                    double totalPaid = 0;
                    int paidCount = 0, unpaidCount = 0;
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String trangThai = doc.getString("trangThai");
                        if ("Đã thanh toán".equals(trangThai)) {
                            Double tongTien = doc.getDouble("tongTien");
                            if (tongTien != null)
                                totalPaid += tongTien;
                            paidCount++;
                        } else
                            unpaidCount++;
                    }
                    callback.onDone(totalPaid, paidCount, unpaidCount);
                })
                .addOnFailureListener(e -> callback.onDone(0, 0, 0));
    }

    private void markRoomStatus(@NonNull String status) {
        DocumentReference scope = scopedDoc();
        if (scope == null)
            return;
        scope.collection("phong_tro").document(phongId).update("trangThai", status);
    }

    private void printContract() {
        if (currentContract == null) {
            Toast.makeText(this, "Chưa có dữ liệu", Toast.LENGTH_SHORT).show();
            return;
        }
        String html = buildContractHtml(currentContract);
        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        if (printManager == null) {
            Toast.makeText(this, "Thiết bị không hỗ trợ in", Toast.LENGTH_SHORT).show();
            return;
        }
        String jobName = "HopDong_" + (currentContract.getSoHopDong() != null ? currentContract.getSoHopDong() : "");
        WebView webView = new WebView(this);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                printManager.print(jobName, view.createPrintDocumentAdapter(jobName),
                        new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build());
            }
        });
        webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null);
    }

    private String buildContractHtml(@NonNull NguoiThue c) {
        String room = currentPhong != null ? nullToEmpty(currentPhong.getSoPhong()) : "";
        String khuDiaChi = currentKhu != null ? nullToEmpty(currentKhu.getDiaChi()) : "";
        String tenChuTro = currentKhu != null ? nullToEmpty(currentKhu.getTenCanNha()) : "";
        String sdtChuTro = currentKhu != null ? nullToEmpty(currentKhu.getSdtQuanLy()) : "";
        String ngayKy = nullToEmpty(c.getNgayBatDauThue()), ngayKetThuc = nullToEmpty(c.getNgayKetThucHopDong());
        int soThang = c.getSoThangHopDong();
        StringBuilder chiPhi = new StringBuilder();
        if (currentKhu != null) {
            chiPhi.append("- Tiền điện: ").append(formatVndNumberOnly(currentKhu.getGiaDien()))
                    .append("/kWh (Chỉ số ban đầu: ").append(c.getChiSoDienDau()).append(")<br/>");
            String cachTinhNuoc = currentKhu.getCachTinhNuoc();
            String donViNuoc = "nguoi".equals(cachTinhNuoc) ? "/người/tháng"
                    : "dong_ho".equals(cachTinhNuoc) ? "/m³" : "/phòng";
            chiPhi.append("- Tiền nước: ").append(formatVndNumberOnly(currentKhu.getGiaNuoc())).append(donViNuoc)
                    .append("<br/>");
            if (c.isDichVuGuiXe())
                chiPhi.append("- Phí gửi xe: ").append(formatVndNumberOnly(currentKhu.getGiaXe()))
                        .append("/chiếc (SL: ").append(c.getSoLuongXe()).append(")<br/>");
            if (c.isDichVuInternet())
                chiPhi.append("- Internet: ").append(formatVndNumberOnly(currentKhu.getGiaInternet()))
                        .append("/tháng<br/>");
            if (c.isDichVuGiatSay())
                chiPhi.append("- Giặt sấy: ").append(formatVndNumberOnly(currentKhu.getGiaGiatSay()))
                        .append("/tháng<br/>");
        }
        return "<!DOCTYPE html><html><head><meta charset='utf-8'/><style>body{font-family:'Times New Roman',serif;font-size:13px;padding:30px 40px;line-height:1.6;color:#000;}.header{text-align:center;margin-bottom:20px;}.header h3{margin:0;font-size:14px;font-weight:bold;text-transform:uppercase;}.header p{margin:2px 0;font-size:12px;font-style:italic;}.title{text-align:center;margin:25px 0;}.title h2{margin:0;font-size:18px;font-weight:bold;text-transform:uppercase;}.info{margin-bottom:8px;}.section{margin-top:15px;}.section-title{font-weight:bold;margin-bottom:5px;}.indent{padding-left:20px;}.gia-han-table{width:100%;border-collapse:collapse;margin:10px 0;}.gia-han-table th,.gia-han-table td{border:1px solid #000;padding:6px 8px;text-align:center;font-size:12px;}.gia-han-table th{background:#f5f5f5;font-weight:bold;}.signature{display:flex;justify-content:space-around;margin-top:40px;text-align:center;}.signature div{width:40%;}.signature .label{font-weight:bold;text-transform:uppercase;}.signature .note{font-style:italic;font-size:11px;}.signature .name{margin-top:60px;font-weight:bold;}</style></head><body><div class='header'><h3>CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM</h3><p>Độc lập – Tự do – Hạnh phúc</p></div><div class='title'><h2>HỢP ĐỒNG THUÊ PHÒNG TRỌ</h2></div><p class='info'>Hôm nay, ngày "
                + formatDateFull(ngayKy) + ", tại căn nhà số " + escape(khuDiaChi)
                + "</p><p class='info'>Chúng tôi ký tên dưới đây gồm có:</p><div class='section'><p class='section-title'>BÊN CHO THUÊ PHÒNG TRỌ (gọi tắt là Bên A):</p><p class='indent'>Ông/bà: "
                + escape(tenChuTro)
                + "</p><p class='indent'>CMND/CCCD số: .................. cấp ngày: ............... nơi cấp: ...............</p><p class='indent'>Điện thoại: "
                + escape(sdtChuTro) + "</p><p class='indent'>Thường trú tại: " + escape(khuDiaChi)
                + "</p></div><div class='section'><p class='section-title'>BÊN THUÊ PHÒNG TRỌ (gọi tắt là Bên B):</p><p class='indent'>Ông/bà: "
                + escape(c.getHoTen()) + "</p><p class='indent'>CMND/CCCD số: " + escape(c.getCccd())
                + " cấp ngày: ............... nơi cấp: ...............</p><p class='indent'>Điện thoại: "
                + escape(c.getSoDienThoai())
                + "</p></div><p style='margin-top:15px;'>Sau khi thỏa thuận, hai bên thống nhất như sau:</p><div class='section'><p class='section-title'>1. Nội dung thuê phòng trọ</p><p class='indent'>Bên A cho Bên B thuê 01 phòng trọ số "
                + escape(room) + " tại căn nhà số " + escape(khuDiaChi) + ".</p><p class='indent'>Với thời hạn là: "
                + soThang + " tháng (Từ ngày " + escape(ngayKy) + " đến ngày " + escape(ngayKetThuc)
                + ").</p><p class='indent'>Giá thuê: " + formatVnd(c.getTienPhong())
                + "/tháng.</p><p class='indent'>Chưa bao gồm chi phí:</p><p class='indent'>" + chiPhi.toString()
                + "</p></div><div class='section'><p class='section-title'>2. Trách nhiệm Bên A</p><p class='indent'>- Đảm bảo căn nhà cho thuê không có tranh chấp, khiếu kiện.</p><p class='indent'>- Đăng ký với chính quyền địa phương về thủ tục cho thuê phòng trọ.</p></div><div class='section'><p class='section-title'>3. Trách nhiệm Bên B</p><p class='indent'>- Đặt cọc với số tiền là "
                + formatVnd(c.getTienCoc())
                + ", thanh toán tiền thuê phòng hàng tháng + tiền điện + nước.</p><p class='indent'>- Đảm bảo các thiết bị và sửa chữa các hư hỏng trong phòng trong khi sử dụng. Nếu không sửa chữa thì khi trả phòng, bên A sẽ trừ vào tiền đặt cọc, giá trị cụ thể tính theo giá thị trường.</p><p class='indent'>- Chỉ sử dụng phòng trọ vào mục đích ở, với số lượng tối đa không quá "
                + c.getSoThanhVien()
                + " người; không chứa các thiết bị gây cháy nổ, hàng cấm; cung cấp giấy tờ tùy thân để đăng ký tạm trú theo quy định, giữ gìn an ninh trật tự, nếp sống văn hóa đô thị; không tụ tập nhậu nhẹt, cờ bạc và các hành vi vi phạm pháp luật khác.</p><p class='indent'>- Không được tự ý cải tạo kiến trúc phòng hoặc trang trí ảnh hưởng tới tường, cột, nền... Nếu có nhu cầu trên phải trao đổi với bên A để được thống nhất.</p></div><div class='section'><p class='section-title'>4. Điều khoản thực hiện</p><p class='indent'>Hai bên nghiêm túc thực hiện những quy định trên trong thời hạn cho thuê, nếu bên A lấy phòng phải báo cho bên B ít nhất 01 tháng, hoặc ngược lại.</p><p class='indent'>Sau thời hạn cho thuê "
                + soThang
                + " tháng nếu bên B có nhu cầu hai bên tiếp tục thương lượng giá thuê để gia hạn hợp đồng.</p></div><div class='section'><p class='section-title'>Bảng gia hạn hợp đồng:</p><table class='gia-han-table'><tr><th>Lần</th><th>Thời gian<br/>(tháng)</th><th>Từ ngày</th><th>Đến ngày</th><th>Giá thuê/tháng</th><th>Ký tên</th></tr><tr><td>1</td><td></td><td></td><td></td><td></td><td></td></tr><tr><td>2</td><td></td><td></td><td></td><td></td><td></td></tr></table></div><div class='signature'><div><p class='label'>BÊN B</p><p class='note'>(Ký, ghi rõ họ tên)</p><p class='name'>"
                + escape(c.getHoTen())
                + "</p></div><div><p class='label'>BÊN A</p><p class='note'>(Ký, ghi rõ họ tên)</p><p class='name'>"
                + escape(tenChuTro) + "</p></div></div></body></html>";
    }

    private String formatDateFull(String dateStr) {
        if (dateStr == null || dateStr.isEmpty())
            return "... tháng ... năm ...";
        try {
            String[] parts = dateStr.split("/");
            if (parts.length == 3)
                return parts[0] + " tháng " + parts[1] + " năm " + parts[2];
        } catch (Exception ignored) {
        }
        return dateStr;
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

    private String formatVnd(double value) {
        return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format((long) value) + " đ";
    }

    private String formatVndNumberOnly(double value) {
        return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format((long) value) + " đ";
    }
}
