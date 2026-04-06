package com.example.myapplication.features.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.AuthProviderUtil;
import com.example.myapplication.features.auth.MainActivity;
import com.example.myapplication.features.settings.ChangePasswordActivity;
import com.example.myapplication.features.settings.EditProfileActivity;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class TenantMenuActivity extends AppCompatActivity {

    // ===== Header =====
    private TextView tvTenantName;
    private TextView tvRoomInfo;
    private ImageView imgAvatar;

    // ===== Grid menu =====
    private CardView cardMyRoom;
    private CardView cardBill;
    private CardView cardReport;
    private CardView cardNotification;

    // ===== DrawerLayout (tái sử dụng home_menu_profile_drawer.xml) =====
    private DrawerLayout tenantDrawerLayout;

    // IDs từ home_menu_profile_drawer.xml
    private ShapeableImageView drawerAvatar;
    private TextView drawerUserName;
    private TextView drawerUserEmail;
    private LinearLayout menuEditProfile;
    private LinearLayout menuChangePassword;
    private LinearLayout menuRentalHistory;
    private LinearLayout menuLogout;

    // ===== Data =====
    private String tenantId;
    private String roomId;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_menu);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        // Nhận dữ liệu từ Intent, fallback sang TenantSession nếu Intent thiếu
        tenantId = getIntent().getStringExtra("TENANT_ID");
        if (tenantId == null || tenantId.trim().isEmpty()) {
            tenantId = TenantSession.getActiveTenantId();
        }
        roomId = getIntent().getStringExtra("ROOM_ID");

        // ===== Ánh xạ Header =====
        tvTenantName = findViewById(R.id.tvTenantName);
        tvRoomInfo   = findViewById(R.id.tvRoomInfo);
        imgAvatar    = findViewById(R.id.imgAvatarHeader);

        // ===== Ánh xạ Grid menu =====
        cardMyRoom       = findViewById(R.id.cardMyRoom);
        cardBill         = findViewById(R.id.cardBill);
        cardReport       = findViewById(R.id.cardReport);
        cardNotification = findViewById(R.id.cardNotification);

        // ===== Ánh xạ DrawerLayout và các view bên trong drawer =====
        tenantDrawerLayout  = findViewById(R.id.tenantDrawerLayout);
        drawerAvatar        = findViewById(R.id.drawerAvatar);
        drawerUserName      = findViewById(R.id.drawerUserName);
        drawerUserEmail     = findViewById(R.id.drawerUserEmail);
        menuEditProfile     = findViewById(R.id.menuEditProfile);
        menuChangePassword  = findViewById(R.id.menuChangePassword);
        menuRentalHistory   = findViewById(R.id.menuRentalHistory);
        menuLogout          = findViewById(R.id.menuLogout);

        // Ẩn mục Rental History (không dùng cho Tenant)
        if (menuRentalHistory != null) {
            menuRentalHistory.setVisibility(View.GONE);
        }

        // ===== Load dữ liệu =====
        loadUserInfo();
        if (roomId != null && !roomId.isEmpty()) {
            fetchRoomNumber(roomId);
        } else {
            tvRoomInfo.setText("Phòng: --");
        }

        // ===== Click avatar → mở Drawer từ phải =====
        if (imgAvatar != null) {
            imgAvatar.setOnClickListener(v -> openProfileDrawer());
        }

        // ===== Setup click listeners cho drawer items =====
        setupDrawerListeners();

        // ===== Click 4 thẻ menu =====
        cardMyRoom.setOnClickListener(v -> openRoomDetail());
        cardBill.setOnClickListener(v -> openRoomDetail());   // cùng màn hình, tab billing

        cardReport.setOnClickListener(v ->
                Toast.makeText(this, "Báo cáo sự cố", Toast.LENGTH_SHORT).show());

        cardNotification.setOnClickListener(v ->
                Toast.makeText(this, "Thông báo", Toast.LENGTH_SHORT).show());
    }

    // ========== MỞ MÀN HÌNH CHI TIẾT PHÒNG ==========
    private void openRoomDetail() {
        if (roomId == null || roomId.isEmpty()) {
            Toast.makeText(this, "Chưa xác định được phòng", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, TenantRoomDetailActivity.class);
        intent.putExtra(TenantRoomDetailActivity.EXTRA_ROOM_ID,   roomId);
        intent.putExtra(TenantRoomDetailActivity.EXTRA_TENANT_ID, tenantId);
        startActivity(intent);
    }

    // ========== MỞ DRAWER TỪ BÊN PHẢI ==========
    private void openProfileDrawer() {
        // Điền thông tin user vào drawer trước khi mở
        populateDrawerWithUserInfo();
        tenantDrawerLayout.openDrawer(GravityCompat.END);
    }

    // ========== ĐIỀN THÔNG TIN USER VÀO DRAWER ==========
    private void populateDrawerWithUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String email       = user.getEmail();
        String displayName = user.getDisplayName();

        if (drawerUserEmail != null) {
            drawerUserEmail.setText(email != null ? email : "");
        }

        // Hiện tên tức thì từ Auth, Firestore ghi đè sau
        if (drawerUserName != null) {
            if (displayName != null && !displayName.isEmpty()) {
                drawerUserName.setText(displayName);
            } else if (email != null) {
                drawerUserName.setText(email.split("@")[0]);
            }
        }

        // Load avatar nếu có
        if (user.getPhotoUrl() != null && drawerAvatar != null) {
            Glide.with(this).load(user.getPhotoUrl()).circleCrop().into(drawerAvatar);
        }

        // Ẩn "Đổi mật khẩu" nếu đăng nhập bằng Google
        if (menuChangePassword != null) {
            boolean canChange = AuthProviderUtil.canChangePassword(user);
            menuChangePassword.setVisibility(canChange ? View.VISIBLE : View.GONE);
        }

        // Lấy fullName và avatarUrl từ Firestore
        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String fullName  = doc.getString("fullName");
                        String avatarUrl = doc.getString("avatarUrl");
                        if (fullName != null && !fullName.isEmpty() && drawerUserName != null) {
                            drawerUserName.setText(fullName);
                        }
                        if (avatarUrl != null && !avatarUrl.isEmpty() && drawerAvatar != null) {
                            Glide.with(this).load(avatarUrl).circleCrop().into(drawerAvatar);
                        }
                    }
                });
    }

    // ========== SETUP CLICK LISTENERS CHO DRAWER ==========
    private void setupDrawerListeners() {
        // Quản lý tài khoản
        if (menuEditProfile != null) {
            menuEditProfile.setOnClickListener(v -> {
                tenantDrawerLayout.closeDrawer(GravityCompat.END);
                startActivity(new Intent(this, EditProfileActivity.class));
            });
        }

        // Đổi mật khẩu
        if (menuChangePassword != null) {
            menuChangePassword.setOnClickListener(v -> {
                tenantDrawerLayout.closeDrawer(GravityCompat.END);
                startActivity(new Intent(this, ChangePasswordActivity.class));
            });
        }

        // Đăng xuất
        if (menuLogout != null) {
            menuLogout.setOnClickListener(v -> {
                tenantDrawerLayout.closeDrawer(GravityCompat.END);
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.logout))
                        .setMessage(getString(R.string.logout_confirm_message))
                        .setPositiveButton(getString(R.string.logout), (d, which) -> {
                            // BUG FIX: Xóa TenantSession trước khi signOut
                            // Nếu không clear, khi owner đăng nhập sau sẽ dùng nhầm path tenants/
                            TenantSession.clear(this);
                            mAuth.signOut();
                            Intent intent = new Intent(this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        })
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show();
            });
        }
    }

    // ========== LOAD TÊN USER LÊN HEADER ==========
    private void loadUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String displayName = user.getDisplayName();
        String email       = user.getEmail();

        if (displayName != null && !displayName.isEmpty()) {
            tvTenantName.setText(displayName);
        } else if (email != null) {
            tvTenantName.setText(email.split("@")[0]);
        }

        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String fullName = doc.getString("fullName");
                        if (fullName != null && !fullName.isEmpty()) {
                            tvTenantName.setText(fullName);
                        }
                    }
                });
    }

    // ========== QUERY TÊN PHÒNG TỪ FIRESTORE ==========
    private void fetchRoomNumber(String roomId) {
        tvRoomInfo.setText("Phòng: ...");

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            tvRoomInfo.setText("Phòng: " + roomId);
            return;
        }

        // Đọc activeTenantId trực tiếp từ Firestore (nguồn đáng tin nhất)
        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(userDoc -> {
                    String freshTenantId = userDoc.getString("activeTenantId");

                    if (freshTenantId == null || freshTenantId.trim().isEmpty()) {
                        tvRoomInfo.setText("Phòng: " + roomId);
                        return;
                    }

                    tenantId = freshTenantId;

                    // Bước 2a: tenants/{tenantId}/rooms/{roomId}
                    db.collection("tenants").document(freshTenantId)
                            .collection("rooms").document(roomId)
                            .get()
                            .addOnSuccessListener(roomDoc -> {
                                if (roomDoc.exists()) {
                                    String rn = roomDoc.getString("roomNumber");
                                    tvRoomInfo.setText("Phòng: " + (rn != null && !rn.isEmpty() ? rn : roomId));
                                } else {
                                    // Bước 2b: rooms lưu ở users/{tenantId}/rooms/ (khi owner không có tenant org)
                                    db.collection("users").document(freshTenantId)
                                            .collection("rooms").document(roomId)
                                            .get()
                                            .addOnSuccessListener(userRoomDoc -> {
                                                if (userRoomDoc.exists()) {
                                                    String rn = userRoomDoc.getString("roomNumber");
                                                    tvRoomInfo.setText("Phòng: " + (rn != null && !rn.isEmpty() ? rn : roomId));
                                                } else {
                                                    // Bước 3: scan toàn bộ cả 2 path
                                                    scanAllRooms(freshTenantId, roomId);
                                                }
                                            })
                                            .addOnFailureListener(e -> scanAllRooms(freshTenantId, roomId));
                                }
                            })
                            .addOnFailureListener(e -> scanAllRooms(freshTenantId, roomId));
                })
                .addOnFailureListener(e -> tvRoomInfo.setText("Phòng: " + roomId));
    }

    private void scanAllRooms(String freshTenantId, String roomId) {
        db.collection("tenants").document(freshTenantId)
                .collection("rooms").get()
                .addOnSuccessListener(qs -> {
                    String found = findRoomNumber(qs, roomId);
                    if (found != null) {
                        tvRoomInfo.setText("Phòng: " + found);
                    } else {
                        db.collection("users").document(freshTenantId)
                                .collection("rooms").get()
                                .addOnSuccessListener(qs2 -> {
                                    String found2 = findRoomNumber(qs2, roomId);
                                    tvRoomInfo.setText("Phòng: " + (found2 != null ? found2 : roomId));
                                })
                                .addOnFailureListener(e -> tvRoomInfo.setText("Phòng: " + roomId));
                    }
                })
                .addOnFailureListener(e ->
                        db.collection("users").document(freshTenantId)
                                .collection("rooms").get()
                                .addOnSuccessListener(qs2 -> {
                                    String found2 = findRoomNumber(qs2, roomId);
                                    tvRoomInfo.setText("Phòng: " + (found2 != null ? found2 : roomId));
                                })
                                .addOnFailureListener(e2 -> tvRoomInfo.setText("Phòng: " + roomId)));
    }

    private String findRoomNumber(com.google.firebase.firestore.QuerySnapshot qs, String roomId) {
        for (com.google.firebase.firestore.DocumentSnapshot doc : qs.getDocuments()) {
            if (roomId.equals(doc.getId()) || roomId.equals(doc.getString("roomId"))) {
                String rn = doc.getString("roomNumber");
                return rn != null && !rn.isEmpty() ? rn : doc.getId();
            }
        }
        return null;
    }

    // ========== BACK BUTTON: đóng drawer trước khi thoát ==========
    @Override
    public void onBackPressed() {
        if (tenantDrawerLayout != null && tenantDrawerLayout.isDrawerOpen(GravityCompat.END)) {
            tenantDrawerLayout.closeDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
    }
}
