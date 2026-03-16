package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class HomeActivity extends AppCompatActivity {

    private TextView tvUserName, tvStatPhong, tvStatNguoiThue, tvStatHoaDon, tvProfileEmail;
    private CardView cardPhongTro, cardNguoiThue, cardHoaDon, cardDoanhThu, cardProfile;
    private ImageView btnLogout;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Làm cho status bar trong suốt
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        window.setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_home);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Bind views
        tvUserName = findViewById(R.id.tvUserName);
        tvStatPhong = findViewById(R.id.tvStatPhong);
        tvStatNguoiThue = findViewById(R.id.tvStatNguoiThue);
        tvStatHoaDon = findViewById(R.id.tvStatHoaDon);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        btnLogout = findViewById(R.id.btnLogout);

        cardPhongTro = findViewById(R.id.cardPhongTro);
        cardNguoiThue = findViewById(R.id.cardNguoiThue);
        cardHoaDon = findViewById(R.id.cardHoaDon);
        cardDoanhThu = findViewById(R.id.cardDoanhThu);
        cardProfile = findViewById(R.id.cardProfile);

        // Xử lý Insets cho Header để không bị lẹm viền trên
        View headerLayout = findViewById(R.id.headerLayout);
        ViewCompat.setOnApplyWindowInsetsListener(headerLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Thêm padding top bằng chiều cao status bar + padding hiện tại
            v.setPadding(v.getPaddingLeft(), systemBars.top + 20, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        // Hiển thị tên user
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String displayName = user.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                tvUserName.setText(displayName);
            } else {
                tvUserName.setText(user.getEmail());
            }
            tvProfileEmail.setText(user.getEmail());
        }

        // Navigation
        cardPhongTro.setOnClickListener(v -> startActivity(new Intent(this, PhongTroActivity.class)));
        cardNguoiThue.setOnClickListener(v -> startActivity(new Intent(this, NguoiThueActivity.class)));
        cardHoaDon.setOnClickListener(v -> startActivity(new Intent(this, HoaDonActivity.class)));
        cardDoanhThu.setOnClickListener(v -> startActivity(new Intent(this, DoanhThuActivity.class)));
        cardProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));

        // Logout
        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc muốn đăng xuất?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    mAuth.signOut();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .setNegativeButton("Hủy", null)
                .show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStats();
    }

    private void loadStats() {
        // Đếm phòng
        db.collection("phong_tro")
            .addSnapshotListener((value, error) -> {
                if (value != null) {
                    tvStatPhong.setText(String.valueOf(value.size()));
                }
            });

        // Đếm người thuê
        db.collection("nguoi_thue")
            .addSnapshotListener((value, error) -> {
                if (value != null) {
                    tvStatNguoiThue.setText(String.valueOf(value.size()));
                }
            });

        // Đếm hóa đơn chưa thanh toán
        db.collection("hoa_don")
            .addSnapshotListener((value, error) -> {
                if (value != null) {
                    int chuaThu = 0;
                    for (QueryDocumentSnapshot doc : value) {
                        String trangThai = doc.getString("trangThai");
                        if ("Chưa thanh toán".equals(trangThai)) {
                            chuaThu++;
                        }
                    }
                    tvStatHoaDon.setText(String.valueOf(chuaThu));
                }
            });

        // Cập nhật tên user
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String displayName = user.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                tvUserName.setText(displayName);
            }
            tvProfileEmail.setText(user.getEmail());
        }
    }
}