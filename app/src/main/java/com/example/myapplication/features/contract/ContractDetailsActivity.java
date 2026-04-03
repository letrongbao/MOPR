package com.example.myapplication.features.contract;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.myapplication.R;
import com.example.myapplication.core.repository.domain.TenantRepository;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.ContractStatusHelper;
import com.example.myapplication.domain.ContractStatus;
import com.example.myapplication.domain.Tenant;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ContractDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_CONTRACT_ID = "CONTRACT_ID";

    private FirebaseFirestore db;
    private TenantRepository repository;
    private String contractId;

    // UI Elements
    private TextView tvTenPhong, tvSoHopDong, tvNguoiDaiDien, tvSoDienThoai;
    private TextView tvNgayLap, tvNgayHetHan, tvGiaThue, tvTienCoc;
    private Chip chipTrangThai;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Edge-to-edge
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);
        WindowInsetsControllerCompat ctrl = WindowCompat.getInsetsController(window, window.getDecorView());
        if (ctrl != null) ctrl.setAppearanceLightStatusBars(false);

        setContentView(R.layout.activity_contract_details);

        // Get contract ID from intent
        contractId = getIntent().getStringExtra(EXTRA_CONTRACT_ID);
        if (contractId == null || contractId.trim().isEmpty()) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID hợp đồng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Padding cho AppBar
        View appBar = findViewById(R.id.appBarLayout);
        if (appBar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, insets) -> {
                Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(0, sys.top, 0, 0);
                return insets;
            });
        }

        // Initialize
        db = FirebaseFirestore.getInstance();
        repository = new TenantRepository();

        // Bind views
        bindViews();

        // Setup listeners
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Load data
        loadContractDetails();
    }

    private void bindViews() {
        tvTenPhong = findViewById(R.id.tvTenPhong);
        tvSoHopDong = findViewById(R.id.tvSoHopDong);
        tvNguoiDaiDien = findViewById(R.id.tvNguoiDaiDien);
        tvSoDienThoai = findViewById(R.id.tvSoDienThoai);
        tvNgayLap = findViewById(R.id.tvNgayLap);
        tvNgayHetHan = findViewById(R.id.tvNgayHetHan);
        tvGiaThue = findViewById(R.id.tvGiaThue);
        tvTienCoc = findViewById(R.id.tvTienCoc);
        chipTrangThai = findViewById(R.id.chipTrangThai);
    }

    private void loadContractDetails() {
        String tenantId = TenantSession.getActiveTenantId();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        
        if (user == null) {
            Toast.makeText(this, "Lỗi: Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get contract from Firestore
        if (tenantId != null && !tenantId.isEmpty()) {
            db.collection("tenants")
                    .document(tenantId)
                    .collection("nguoi_thue")
                    .document(contractId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Tenant contract = documentSnapshot.toObject(Tenant.class);
                            if (contract != null) {
                                contract.setId(documentSnapshot.getId());
                                displayContractDetails(contract);
                            }
                        } else {
                            Toast.makeText(this, "Không tìm thấy hợp đồng", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        finish();
                    });
        } else {
            db.collection("users")
                    .document(user.getUid())
                    .collection("nguoi_thue")
                    .document(contractId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Tenant contract = documentSnapshot.toObject(Tenant.class);
                            if (contract != null) {
                                contract.setId(documentSnapshot.getId());
                                displayContractDetails(contract);
                            }
                        } else {
                            Toast.makeText(this, "Không tìm thấy hợp đồng", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        finish();
                    });
        }
    }

    private void displayContractDetails(Tenant contract) {
        // Tên phòng
        if (tvTenPhong != null) {
            String tenPhong = contract.getSoPhong() != null ? "Phòng " + contract.getSoPhong() : "—";
            tvTenPhong.setText(tenPhong);
        }

        // Số hợp đồng
        if (tvSoHopDong != null) {
            String soHD = contract.getSoHopDong() != null ? contract.getSoHopDong() : "—";
            tvSoHopDong.setText(soHD);
        }

        // Người đại diện
        if (tvNguoiDaiDien != null) {
            String daiDien = contract.getTenNguoiDaiDien();
            if (daiDien == null || daiDien.trim().isEmpty()) {
                daiDien = contract.getHoTen(); // Fallback to hoTen
            }
            tvNguoiDaiDien.setText(daiDien != null ? daiDien : "—");
        }

        // Số điện thoại
        if (tvSoDienThoai != null) {
            String sdt = contract.getSoDienThoai() != null ? contract.getSoDienThoai() : "—";
            tvSoDienThoai.setText(sdt);
        }

        // Ngày lập (ngày bắt đầu thuê)
        if (tvNgayLap != null) {
            String ngayLap = contract.getNgayBatDauThue() != null ? contract.getNgayBatDauThue() : "—";
            tvNgayLap.setText(ngayLap);
        }

        // Ngày hết hạn - với logic màu đỏ nếu < 30 ngày
        if (tvNgayHetHan != null) {
            long ngayKetThuc = contract.getNgayKetThuc();
            if (ngayKetThuc > 0) {
                // Dùng timestamp
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                String ngayHetHan = sdf.format(new Date(ngayKetThuc));
                tvNgayHetHan.setText(ngayHetHan);
                
                // Logic màu: Đỏ nếu < 30 ngày
                long currentTime = System.currentTimeMillis();
                long timeRemaining = ngayKetThuc - currentTime;
                final long THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000; // 2592000000
                
                if (timeRemaining < THIRTY_DAYS_MS && timeRemaining > 0) {
                    tvNgayHetHan.setTextColor(Color.parseColor("#F44336")); // Đỏ
                } else {
                    tvNgayHetHan.setTextColor(Color.parseColor("#FF6D00")); // Cam (mặc định)
                }
            } else {
                // Fallback: dùng String format cũ
                String ngayHetHan = contract.getNgayKetThucHopDong() != null ? contract.getNgayKetThucHopDong() : "—";
                tvNgayHetHan.setText(ngayHetHan);
                tvNgayHetHan.setTextColor(Color.parseColor("#FF6D00")); // Cam
            }
        }

        // Giá thuê - Format với dấu phân cách
        if (tvGiaThue != null) {
            long giaThue = contract.getGiaThue();
            String giaThueFormatted = String.format("%,dđ", giaThue).replace(',', '.');
            tvGiaThue.setText(giaThueFormatted);
        }

        // Tiền cọc - Format với dấu phân cách
        if (tvTienCoc != null) {
            long tienCoc = contract.getTienCoc();
            String tienCocFormatted = String.format("%,dđ", tienCoc).replace(',', '.');
            tvTienCoc.setText(tienCocFormatted);
        }

        // Trạng thái
        if (chipTrangThai != null) {
            ContractStatus status = ContractStatusHelper.resolve(contract);
            long daysLeft = ContractStatusHelper.daysRemaining(contract);

            switch (status) {
                case DA_KET_THUC:
                    chipTrangThai.setText("Hết hạn");
                    chipTrangThai.setChipBackgroundColor(
                            android.content.res.ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
                    break;

                case SAP_HET_HAN:
                    String text = "⚠ Sắp hết hạn";
                    if (daysLeft >= 0) {
                        text = "⚠ Còn " + daysLeft + " ngày";
                    }
                    chipTrangThai.setText(text);
                    chipTrangThai.setChipBackgroundColor(
                            android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336")));
                    break;

                case DANG_THUE:
                default:
                    chipTrangThai.setText("✓ Đang hiệu lực");
                    chipTrangThai.setChipBackgroundColor(
                            android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                    break;
            }
        }
    }
}

