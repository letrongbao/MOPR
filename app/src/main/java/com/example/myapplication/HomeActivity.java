package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class HomeActivity extends AppCompatActivity {

    private TextView tvUserName, tvStatPhong, tvStatNguoiThue, tvStatHoaDon, tvProfileEmail;
    private CardView cardPhongTro, cardNguoiThue, cardHoaDon, cardDoanhThu, cardProfile;
    private ImageView btnLogout, imgAvatar;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration phongListener, nguoiThueListener, hoaDonListener;

    // Network callback thay thế deprecated BroadcastReceiver
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private Snackbar offlineSnackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Làm cho status bar trong suốt (thay thế deprecated SYSTEM_UI_FLAG)
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
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
        imgAvatar = findViewById(R.id.imgAvatar);

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

        // === NETWORK CALLBACK: Kiểm tra trạng thái mạng (thay thế deprecated CONNECTIVITY_ACTION) ===
        View rootView = findViewById(android.R.id.content);
        offlineSnackbar = Snackbar.make(rootView,
                "Bạn đang offline. Vui lòng kiểm tra kết nối mạng!", Snackbar.LENGTH_INDEFINITE)
                .setAction("OK", v -> {});
        offlineSnackbar.setBackgroundTint(Color.parseColor("#F44336"));
        offlineSnackbar.setTextColor(Color.WHITE);
        offlineSnackbar.setActionTextColor(Color.WHITE);

        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                runOnUiThread(() -> {
                    if (offlineSnackbar.isShown()) offlineSnackbar.dismiss();
                });
            }

            @Override
            public void onLost(Network network) {
                runOnUiThread(() -> {
                    if (!offlineSnackbar.isShown()) offlineSnackbar.show();
                });
            }
        };

        // Hiển thị tên user
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Hiển thị tạm từ Auth
            String displayName = user.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                tvUserName.setText(displayName);
            } else {
                String email = user.getEmail();
                tvUserName.setText(email != null ? email : "");
            }
            String email = user.getEmail();
            tvProfileEmail.setText(email != null ? email : "");

            // Lấy tên từ Firestore (chính xác hơn Auth cache)
            db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String hoTen = doc.getString("hoTen");
                        if (hoTen != null && !hoTen.isEmpty()) {
                            tvUserName.setText(hoTen);
                        }
                    }
                });
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
                    // Xóa SharedPreferences khi đăng xuất
                    getSharedPreferences("NhaTroPrefs", MODE_PRIVATE)
                        .edit().clear().apply();
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
        // Đăng ký NetworkCallback (thay thế deprecated BroadcastReceiver)
        if (connectivityManager != null) {
            NetworkRequest request = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();
            connectivityManager.registerNetworkCallback(request, networkCallback);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Hủy đăng ký NetworkCallback khi activity không active
        if (connectivityManager != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }

    private void removeListeners() {
        if (phongListener != null) phongListener.remove();
        if (nguoiThueListener != null) nguoiThueListener.remove();
        if (hoaDonListener != null) hoaDonListener.remove();
    }

    private void loadStats() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();

        // Remove listener cũ trước khi tạo mới
        removeListeners();

        // Đếm phòng (scoped theo user)
        phongListener = db.collection("users").document(uid).collection("phong_tro")
            .addSnapshotListener((value, error) -> {
                if (value != null) {
                    tvStatPhong.setText(String.valueOf(value.size()));
                }
            });

        // Đếm người thuê (scoped theo user)
        nguoiThueListener = db.collection("users").document(uid).collection("nguoi_thue")
            .addSnapshotListener((value, error) -> {
                if (value != null) {
                    tvStatNguoiThue.setText(String.valueOf(value.size()));
                }
            });

        // Đếm hóa đơn chưa thanh toán (scoped theo user)
        hoaDonListener = db.collection("users").document(uid).collection("hoa_don")
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

        // Cập nhật tên user từ Firestore
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener(doc -> {
                if (isFinishing() || isDestroyed()) return;
                if (doc.exists()) {
                    String hoTen = doc.getString("hoTen");
                    if (hoTen != null && !hoTen.isEmpty()) {
                        tvUserName.setText(hoTen);
                    }
                    String avatarUrl = doc.getString("avatarUrl");
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        Glide.with(HomeActivity.this).load(avatarUrl).circleCrop().into(imgAvatar);
                        imgAvatar.setPadding(0, 0, 0, 0);
                    }
                }
            });
        String email = user.getEmail();
        tvProfileEmail.setText(email != null ? email : "");
    }

    @Override
    protected void onStop() {
        super.onStop();
        removeListeners();
    }
}
