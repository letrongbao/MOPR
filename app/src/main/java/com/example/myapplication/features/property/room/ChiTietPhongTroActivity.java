package com.example.myapplication.features.property.room;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.core.constants.RoomStatus;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.domain.NguoiThue;
import com.example.myapplication.domain.PhongTro;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.Locale;

public class ChiTietPhongTroActivity extends AppCompatActivity {

    private ImageView imgPhong;
    private TextView tvSoPhong, tvLoaiPhong, tvDienTich, tvGiaThue, tvTrangThai, tvTrangThaiRow;
    private TextView tvTenNguoiThue, tvSdtNguoiThue;
    private CardView cardNguoiThue;
    private LinearLayout btnGoiDien, btnNhanTin, llActionButtons;
    private PhongTro currentPhong;

    private String soDienThoaiNguoiThue;
    private String tenPhongHienTai;

    private ListenerRegistration roomListener;
    private ListenerRegistration tenantListener;

    private String phongId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Transparent status bar
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_chi_tiet_phong_tro);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Chi tiết phòng");
        }
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        imgPhong = findViewById(R.id.imgPhongChiTiet);
        tvSoPhong = findViewById(R.id.tvSoPhongChiTiet);
        tvLoaiPhong = findViewById(R.id.tvLoaiPhongChiTiet);
        tvDienTich = findViewById(R.id.tvDienTichChiTiet);
        tvGiaThue = findViewById(R.id.tvGiaThueChiTiet);
        tvTrangThai = findViewById(R.id.tvTrangThaiChiTiet);
        tvTrangThaiRow = findViewById(R.id.tvTrangThaiRow);
        tvTenNguoiThue = findViewById(R.id.tvTenNguoiThue);
        tvSdtNguoiThue = findViewById(R.id.tvSdtNguoiThue);
        cardNguoiThue = findViewById(R.id.cardNguoiThue);

        btnGoiDien = findViewById(R.id.btnGoiDien);
        btnNhanTin = findViewById(R.id.btnNhanTin);
        llActionButtons = findViewById(R.id.llActionButtons);

        phongId = getIntent().getStringExtra("PHONG_ID");
        if (phongId == null) {
            Toast.makeText(this, "Không tìm thấy phòng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // === IMPLICIT INTENTS ===

        // 1. Gọi điện cho người thuê (ACTION_DIAL)
        btnGoiDien.setOnClickListener(v -> {
            if (soDienThoaiNguoiThue == null || soDienThoaiNguoiThue.isEmpty()) {
                Toast.makeText(this, "Phòng chưa có người thuê", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + soDienThoaiNguoiThue));
            startActivity(intent);
        });

        // 2. Nhắn tin SMS cho người thuê (ACTION_SENDTO)
        btnNhanTin.setOnClickListener(v -> {
            if (soDienThoaiNguoiThue == null || soDienThoaiNguoiThue.isEmpty()) {
                Toast.makeText(this, "Phòng chưa có người thuê", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("smsto:" + soDienThoaiNguoiThue));
            intent.putExtra("sms_body", "Xin chào, tôi liên hệ về phòng " + tenPhongHienTai);
            startActivity(intent);
        });

        loadRoomData(phongId);
        loadTenantData(phongId);
    }

    private void loadRoomData(String phongId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String uid = user.getUid();

        String tenantId = TenantSession.getActiveTenantId();
        DocumentReference scopeDoc = (tenantId != null && !tenantId.isEmpty())
                ? FirebaseFirestore.getInstance().collection("tenants").document(tenantId)
                : FirebaseFirestore.getInstance().collection("users").document(uid);

        roomListener = scopeDoc
                .collection("phong_tro").document(phongId)
                .addSnapshotListener((doc, e) -> {
                    if (isFinishing() || isDestroyed())
                        return;
                    if (e != null || doc == null || !doc.exists()) {
                        Toast.makeText(this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    PhongTro phong = doc.toObject(PhongTro.class);
                    if (phong == null)
                        return;
                    phong.setId(doc.getId());
                    displayRoom(phong);
                });
    }

    private void loadTenantData(String phongId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
            return;
        String uid = user.getUid();

        String tenantId = TenantSession.getActiveTenantId();
        DocumentReference scopeDoc = (tenantId != null && !tenantId.isEmpty())
                ? FirebaseFirestore.getInstance().collection("tenants").document(tenantId)
                : FirebaseFirestore.getInstance().collection("users").document(uid);

        tenantListener = scopeDoc
                .collection("nguoi_thue")
                .whereEqualTo("idPhong", phongId)
                .addSnapshotListener((value, error) -> {
                    if (isFinishing() || isDestroyed())
                        return;
                    if (error != null || value == null || value.isEmpty()) {
                        cardNguoiThue.setVisibility(View.GONE);
                        llActionButtons.setVisibility(View.GONE);
                        soDienThoaiNguoiThue = null;
                        return;
                    }
                    for (QueryDocumentSnapshot doc : value) {
                        NguoiThue nguoiThue = doc.toObject(NguoiThue.class);
                        if (nguoiThue != null) {
                            cardNguoiThue.setVisibility(View.VISIBLE);
                            llActionButtons.setVisibility(View.VISIBLE);
                            tvTenNguoiThue.setText("Họ tên: " + nguoiThue.getHoTen());
                            tvSdtNguoiThue.setText("SĐT: " + nguoiThue.getSoDienThoai());
                            soDienThoaiNguoiThue = nguoiThue.getSoDienThoai();
                            break;
                        }
                    }
                });
    }

    private void displayRoom(PhongTro phong) {
        currentPhong = phong;
        tenPhongHienTai = phong.getSoPhong();

        String khu = phong.getKhuTen();
        tvSoPhong.setText(
                "Phòng " + phong.getSoPhong() + (khu != null && !khu.trim().isEmpty() ? (" • " + khu.trim()) : ""));
        tvLoaiPhong.setText(phong.getLoaiPhong());
        tvDienTich.setText((int) phong.getDienTich() + " m²");

        NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        tvGiaThue.setText(fmt.format(phong.getGiaThue()) + " đ/tháng");

        boolean trong = RoomStatus.VACANT.equals(phong.getTrangThai());
        int color = Color.parseColor(trong ? "#4CAF50" : "#F44336");

        // Status badge overlay on image
        tvTrangThai.setText(phong.getTrangThai());
        GradientDrawable badge = new GradientDrawable();
        badge.setShape(GradientDrawable.RECTANGLE);
        badge.setCornerRadius(32f);
        badge.setColor(color);
        tvTrangThai.setBackground(badge);

        tvTrangThaiRow.setText(phong.getTrangThai());
        tvTrangThaiRow.setTextColor(color);

        // Load full image (kiểm tra Activity còn sống)
        if (phong.getHinhAnh() != null && !phong.getHinhAnh().isEmpty() && !isDestroyed()) {
            Glide.with(this)
                    .load(phong.getHinhAnh())
                    .centerCrop()
                    .placeholder(R.drawable.baseline_home_24)
                    .into(imgPhong);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Phòng " + phong.getSoPhong());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (roomListener != null)
            roomListener.remove();
        if (tenantListener != null)
            tenantListener.remove();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
