package com.example.myapplication.features.property.room;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.core.constants.RoomStatus;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.ScreenUiHelper;
import com.example.myapplication.domain.Tenant;
import com.example.myapplication.domain.Room;
import com.google.android.material.appbar.AppBarLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.Locale;

public class RoomDetailsActivity extends AppCompatActivity {

    private ImageView imgPhong;
    private TextView tvSoPhong, tvLoaiPhong, tvDienTich, tvGiaThue, tvTrangThai, tvTrangThaiRow;
    private TextView tvTenTenant, tvSdtTenant;
    private View cardTenant;
    private View btnGoiDien, btnNhanTin, llActionButtons;
    private Room currentPhong;

    private String soDienThoaiTenant;
    private String tenPhongHienTai;

    private ListenerRegistration roomListener;
    private ListenerRegistration tenantListener;

    private String phongId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScreenUiHelper.enableEdgeToEdge(this, false);

        setContentView(R.layout.activity_room_details);

        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            ScreenUiHelper.applyTopInset(appBarLayout);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        ScreenUiHelper.setupBackToolbar(this, toolbar, "Chi tiết phòng");

        imgPhong = findViewById(R.id.imgPhongChiTiet);
        tvSoPhong = findViewById(R.id.tvSoPhongChiTiet);
        tvTrangThai = findViewById(R.id.tvTrangThaiChiTiet);

        // Detail rows
        View rowLoai = findViewById(R.id.rowLoaiPhong);
        ((TextView) rowLoai.findViewById(R.id.tvLabel)).setText("Loại phòng");
        tvLoaiPhong = rowLoai.findViewById(R.id.tvValue);

        View rowDienTich = findViewById(R.id.rowDienTich);
        ((TextView) rowDienTich.findViewById(R.id.tvLabel)).setText("Diện tích");
        tvDienTich = rowDienTich.findViewById(R.id.tvValue);

        View rowGia = findViewById(R.id.rowGiaThue);
        ((TextView) rowGia.findViewById(R.id.tvLabel)).setText("Giá thuê");
        tvGiaThue = rowGia.findViewById(R.id.tvValue);
        tvGiaThue.setTextColor(getResources().getColor(R.color.primary));

        View rowTrangThai = findViewById(R.id.rowTrangThai);
        ((TextView) rowTrangThai.findViewById(R.id.tvLabel)).setText("Trạng thái");
        tvTrangThaiRow = rowTrangThai.findViewById(R.id.tvValue);

        tvTenTenant = findViewById(R.id.tvTenTenant);
        tvSdtTenant = findViewById(R.id.tvSdtTenant);
        cardTenant = findViewById(R.id.cardTenant);

        btnGoiDien = findViewById(R.id.btnGoiDien);
        btnNhanTin = findViewById(R.id.btnNhanTin);
        llActionButtons = findViewById(R.id.llActionButtons);

        phongId = getIntent().getStringExtra("PHONG_ID");
        if (phongId == null) {
            Toast.makeText(this, "Không tìm thấy phòng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnGoiDien.setOnClickListener(v -> {
            if (soDienThoaiTenant == null || soDienThoaiTenant.isEmpty()) {
                Toast.makeText(this, "Phòng chưa có người thuê", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + soDienThoaiTenant));
            startActivity(intent);
        });

        btnNhanTin.setOnClickListener(v -> {
            if (soDienThoaiTenant == null || soDienThoaiTenant.isEmpty()) {
                Toast.makeText(this, "Phòng chưa có người thuê", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("smsto:" + soDienThoaiTenant));
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
                    Room phong = doc.toObject(Room.class);
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
                        cardTenant.setVisibility(View.GONE);
                        llActionButtons.setVisibility(View.GONE);
                        soDienThoaiTenant = null;
                        return;
                    }
                    for (QueryDocumentSnapshot doc : value) {
                        Tenant nguoiThue = doc.toObject(Tenant.class);
                        if (nguoiThue != null) {
                            cardTenant.setVisibility(View.VISIBLE);
                            llActionButtons.setVisibility(View.VISIBLE);
                            tvTenTenant.setText(nguoiThue.getHoTen());
                            tvSdtTenant.setText(nguoiThue.getSoDienThoai());
                            soDienThoaiTenant = nguoiThue.getSoDienThoai();
                            break;
                        }
                    }
                });
    }

    private void displayRoom(Room phong) {
        currentPhong = phong;
        tenPhongHienTai = phong.getSoPhong();

        String khu = phong.getHouseTen();
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
