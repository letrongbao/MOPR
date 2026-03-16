package com.example.myapplication;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.myapplication.model.PhongTro;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.Locale;

public class ChiTietPhongTroActivity extends AppCompatActivity {

    private ImageView imgPhong;
    private TextView tvSoPhong, tvLoaiPhong, tvDienTich, tvGiaThue, tvTrangThai, tvTrangThaiRow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Transparent status bar
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
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

        String phongId = getIntent().getStringExtra("PHONG_ID");
        if (phongId == null) {
            Toast.makeText(this, "Không tìm thấy phòng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadRoomData(phongId);
    }

    private void loadRoomData(String phongId) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("phong_tro").document(phongId)
                .addSnapshotListener((doc, e) -> {
                    if (e != null || doc == null || !doc.exists()) {
                        Toast.makeText(this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    PhongTro phong = doc.toObject(PhongTro.class);
                    if (phong == null) return;
                    phong.setId(doc.getId());
                    displayRoom(phong);
                });
    }

    private void displayRoom(PhongTro phong) {
        tvSoPhong.setText("Phòng " + phong.getSoPhong());
        tvLoaiPhong.setText(phong.getLoaiPhong());
        tvDienTich.setText((int) phong.getDienTich() + " m²");

        NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        tvGiaThue.setText(fmt.format(phong.getGiaThue()) + " đ/tháng");

        boolean trong = "Trống".equals(phong.getTrangThai());
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

        // Load full image
        if (phong.getHinhAnh() != null && !phong.getHinhAnh().isEmpty()) {
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
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
