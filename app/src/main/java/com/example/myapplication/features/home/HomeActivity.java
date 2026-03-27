package com.example.myapplication.features.home;

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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.myapplication.core.session.InviteRepository;
import com.example.myapplication.core.constants.InvoiceStatus;
import com.example.myapplication.R;
import com.example.myapplication.core.session.TenantRepository;
import com.example.myapplication.core.constants.TenantRoles;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.features.auth.MainActivity;
import com.example.myapplication.features.finance.DoanhThuActivity;
import com.example.myapplication.features.invoice.HoaDonActivity;
import com.example.myapplication.features.org.OrgAdminActivity;
import com.example.myapplication.features.property.house.CanNhaActivity;
import com.example.myapplication.features.property.room.PhongTroActivity;
import com.example.myapplication.features.settings.ProfileActivity;
import com.example.myapplication.features.ticket.TicketActivity;
import com.example.myapplication.features.tenant.NguoiThueActivity;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

@Deprecated
public class HomeActivity extends AppCompatActivity {

    private TextView tvUserName, tvStatPhong, tvStatNguoiThue, tvStatHoaDon, tvProfileEmail;
    private CardView cardPhongTro, cardNguoiThue, cardHoaDon, cardDoanhThu, cardTickets, cardProfile;
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
        cardTickets = findViewById(R.id.cardTickets);
        cardProfile = findViewById(R.id.cardProfile);

        // Xử lý Insets cho Header để không bị lẹm viền trên
        View headerLayout = findViewById(R.id.headerLayout);
        ViewCompat.setOnApplyWindowInsetsListener(headerLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Thêm padding top bằng chiều cao status bar + padding hiện tại
            v.setPadding(v.getPaddingLeft(), systemBars.top + 20, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        // === NETWORK CALLBACK: Kiểm tra trạng thái mạng (thay thế deprecated
        // CONNECTIVITY_ACTION) ===
        View rootView = findViewById(android.R.id.content);
        offlineSnackbar = Snackbar.make(rootView,
                "Bạn đang offline. Vui lòng kiểm tra kết nối mạng!", Snackbar.LENGTH_INDEFINITE)
                .setAction("OK", v -> {
                });
        offlineSnackbar.setBackgroundTint(Color.parseColor("#F44336"));
        offlineSnackbar.setTextColor(Color.WHITE);
        offlineSnackbar.setActionTextColor(Color.WHITE);

        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                runOnUiThread(() -> {
                    if (offlineSnackbar.isShown())
                        offlineSnackbar.dismiss();
                });
            }

            @Override
            public void onLost(Network network) {
                runOnUiThread(() -> {
                    if (!offlineSnackbar.isShown())
                        offlineSnackbar.show();
                });
            }
        };

        // Cho phép click email để đổi org (tenant)
        tvProfileEmail.setOnClickListener(v -> showTenantSwitcher());
        tvUserName.setOnLongClickListener(v -> {
            showOwnerTools();
            return true;
        });

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
        if (cardTickets != null) {
            cardTickets.setOnClickListener(v -> startActivity(new Intent(this, TicketActivity.class)));
        }
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
                        TenantSession.clear(HomeActivity.this);
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

        new TenantRepository().ensureActiveTenant(this, new TenantRepository.TenantReadyCallback() {
            @Override
            public void onReady(String tenantId) {
                applyRoleUI(tenantId);
                loadStats();
            }

            @Override
            public void onError(Exception e) {
                loadStats();
            }
        });

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
        if (phongListener != null)
            phongListener.remove();
        if (nguoiThueListener != null)
            nguoiThueListener.remove();
        if (hoaDonListener != null)
            hoaDonListener.remove();
    }

    private void loadStats() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null)
            return;

        String uid = user.getUid();

        // Remove listener cũ trước khi tạo mới
        removeListeners();

        String tenantId = TenantSession.getActiveTenantId();
        DocumentReference scopeDoc = (tenantId != null && !tenantId.isEmpty())
                ? db.collection("tenants").document(tenantId)
                : db.collection("users").document(uid);

        if (tenantId != null && !tenantId.isEmpty()) {
            db.collection("tenants").document(tenantId)
                    .collection("members").document(uid)
                    .get()
                    .addOnSuccessListener(member -> {
                        String role = member.getString("role");
                        if (TenantRoles.TENANT.equals(role)) {
                            String roomId = member.getString("roomId");
                            tvStatPhong.setText("1");
                            tvStatNguoiThue.setText("0");

                            hoaDonListener = scopeDoc.collection("hoa_don")
                                    .whereEqualTo("idPhong", roomId)
                                    .addSnapshotListener((value, error) -> {
                                        if (value != null) {
                                            int chuaThu = 0;
                                            for (QueryDocumentSnapshot doc : value) {
                                                String trangThai = doc.getString("trangThai");
                                                if (InvoiceStatus.UNPAID.equals(trangThai)) {
                                                    chuaThu++;
                                                }
                                            }
                                            tvStatHoaDon.setText(String.valueOf(chuaThu));
                                        }
                                    });
                            return;
                        }

                        // STAFF/OWNER
                        phongListener = scopeDoc.collection("phong_tro")
                                .addSnapshotListener((value, error) -> {
                                    if (value != null) {
                                        tvStatPhong.setText(String.valueOf(value.size()));
                                    }
                                });

                        nguoiThueListener = scopeDoc.collection("nguoi_thue")
                                .addSnapshotListener((value, error) -> {
                                    if (value != null) {
                                        tvStatNguoiThue.setText(String.valueOf(value.size()));
                                    }
                                });

                        hoaDonListener = scopeDoc.collection("hoa_don")
                                .addSnapshotListener((value, error) -> {
                                    if (value != null) {
                                        int chuaThu = 0;
                                        for (QueryDocumentSnapshot doc : value) {
                                            String trangThai = doc.getString("trangThai");
                                            if (InvoiceStatus.UNPAID.equals(trangThai)) {
                                                chuaThu++;
                                            }
                                        }
                                        tvStatHoaDon.setText(String.valueOf(chuaThu));
                                    }
                                });
                    });
            return;
        }

        // Legacy scope
        phongListener = scopeDoc.collection("phong_tro")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        tvStatPhong.setText(String.valueOf(value.size()));
                    }
                });

        nguoiThueListener = scopeDoc.collection("nguoi_thue")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        tvStatNguoiThue.setText(String.valueOf(value.size()));
                    }
                });

        hoaDonListener = scopeDoc.collection("hoa_don")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        int chuaThu = 0;
                        for (QueryDocumentSnapshot doc : value) {
                            String trangThai = doc.getString("trangThai");
                            if (InvoiceStatus.UNPAID.equals(trangThai)) {
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
                    if (isFinishing() || isDestroyed())
                        return;
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

    private void showTenantSwitcher() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null)
            return;

        TenantRepository repo = new TenantRepository();
        repo.listMyTenants(new TenantRepository.TenantListCallback() {
            @Override
            public void onSuccess(java.util.List<TenantRepository.TenantInfo> tenants) {
                if (isFinishing() || isDestroyed())
                    return;

                String current = TenantSession.getActiveTenantId();
                String[] items = new String[tenants.size()];
                for (int i = 0; i < tenants.size(); i++) {
                    TenantRepository.TenantInfo t = tenants.get(i);
                    items[i] = (current != null && current.equals(t.id)) ? ("✓ " + t.name) : t.name;
                }

                AlertDialog.Builder b = new AlertDialog.Builder(HomeActivity.this)
                        .setTitle("Chọn tổ chức")
                        .setNegativeButton("Đóng", null)
                        .setNeutralButton("Tạo mới", (d, w) -> showCreateTenantDialog(repo))
                        .setPositiveButton("Tham gia", (d, w) -> showJoinInviteDialog());

                if (items.length == 0) {
                    b.setMessage("Chưa có tổ chức. Hãy tạo mới.");
                } else {
                    b.setItems(items, (d, which) -> {
                        TenantRepository.TenantInfo selected = tenants.get(which);
                        repo.setActiveTenant(HomeActivity.this, selected.id,
                                new TenantRepository.TenantReadyCallback() {
                                    @Override
                                    public void onReady(String tenantId) {
                                        applyRoleUI(tenantId);
                                        loadStats();
                                        Toast.makeText(HomeActivity.this, "Đã chuyển: " + selected.name,
                                                Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        Toast.makeText(HomeActivity.this, "Không thể chuyển tổ chức",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                    });
                }

                b.show();
            }

            @Override
            public void onError(Exception e) {
                if (isFinishing() || isDestroyed())
                    return;
                Toast.makeText(HomeActivity.this, "Lỗi tải danh sách tổ chức", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showOwnerTools() {
        FirebaseUser user = mAuth.getCurrentUser();
        String tenantId = TenantSession.getActiveTenantId();
        if (user == null || tenantId == null || tenantId.isEmpty())
            return;

        db.collection("tenants").document(tenantId)
                .collection("members").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String role = doc.getString("role");
                    if (!TenantRoles.OWNER.equals(role)) {
                        Toast.makeText(this, "Chỉ OWNER mới dùng được công cụ tổ chức", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String[] options = { "Mời nhân viên (STAFF)", "Mời người thuê (TENANT)", "Quản trị tổ chức",
                            "Quản lý nhà" };
                    new AlertDialog.Builder(this)
                            .setTitle("Công cụ tổ chức")
                            .setItems(options, (d, which) -> {
                                if (which == 0)
                                    showCreateInviteDialog(tenantId);
                                else if (which == 1)
                                    showCreateTenantInviteDialog(tenantId);
                                else if (which == 2) {
                                    Intent it = new Intent(HomeActivity.this, OrgAdminActivity.class);
                                    it.putExtra("TENANT_ID", tenantId);
                                    startActivity(it);
                                } else {
                                    startActivity(new Intent(HomeActivity.this, CanNhaActivity.class));
                                }
                            })
                            .setNegativeButton("Đóng", null)
                            .show();
                });
    }

    private void showCreateInviteDialog(String tenantId) {
        android.widget.EditText et = new android.widget.EditText(this);
        et.setHint("Email nhân viên");
        et.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        et.setPadding(50, 30, 50, 30);

        new AlertDialog.Builder(this)
                .setTitle("Mời nhân viên")
                .setView(et)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Tạo mã", (d, w) -> {
                    String email = et.getText().toString().trim();
                    if (email.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ensureStaffQuotaThen(tenantId, () -> new InviteRepository().createStaffInvite(tenantId, email,
                            new InviteRepository.InviteCallback() {
                                @Override
                                public void onSuccess(String code) {
                                    String msg = "TenantId: " + tenantId + "\nMã mời: " + code;
                                    new AlertDialog.Builder(HomeActivity.this)
                                            .setTitle("Mã mời đã tạo")
                                            .setMessage(msg)
                                            .setPositiveButton("OK", null)
                                            .show();
                                }

                                @Override
                                public void onError(Exception e) {
                                    Toast.makeText(HomeActivity.this, "Tạo mã thất bại", Toast.LENGTH_SHORT).show();
                                }
                            }));
                })
                .show();
    }

    private void ensureStaffQuotaThen(@NonNull String tenantId, @NonNull Runnable onAllowed) {
        db.collection("tenants").document(tenantId).get()
                .addOnSuccessListener(tdoc -> {
                    Long maxStaffL = tdoc.getLong("maxStaff");
                    int maxStaff = maxStaffL != null ? maxStaffL.intValue() : 3;

                    db.collection("tenants").document(tenantId).collection("members")
                            .whereEqualTo("role", TenantRoles.STAFF)
                            .get()
                            .addOnSuccessListener(qs -> {
                                int current = qs != null ? qs.size() : 0;
                                if (current >= maxStaff) {
                                    Toast.makeText(this, "Đã vượt giới hạn nhân viên (" + maxStaff + ")",
                                            Toast.LENGTH_LONG).show();
                                    return;
                                }
                                onAllowed.run();
                            })
                            .addOnFailureListener(e -> onAllowed.run());
                })
                .addOnFailureListener(e -> onAllowed.run());
    }

    private void showCreateTenantInviteDialog(String tenantId) {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 10);

        android.widget.EditText etEmail = new android.widget.EditText(this);
        etEmail.setHint("Email người thuê");
        etEmail.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        layout.addView(etEmail);

        android.widget.Spinner spinnerRoom = new android.widget.Spinner(this);
        layout.addView(spinnerRoom);

        java.util.List<String> roomIds = new java.util.ArrayList<>();
        java.util.List<String> roomLabels = new java.util.ArrayList<>();

        android.widget.ArrayAdapter<String> roomAdapter = new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, roomLabels);
        roomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRoom.setAdapter(roomAdapter);

        db.collection("tenants").document(tenantId).collection("phong_tro")
                .get()
                .addOnSuccessListener(qs -> {
                    roomIds.clear();
                    roomLabels.clear();
                    for (QueryDocumentSnapshot doc : qs) {
                        roomIds.add(doc.getId());
                        String soPhong = doc.getString("soPhong");
                        roomLabels.add(soPhong != null && !soPhong.isEmpty() ? ("Phòng " + soPhong) : doc.getId());
                    }
                    roomAdapter.notifyDataSetChanged();
                });

        new AlertDialog.Builder(this)
                .setTitle("Mời người thuê")
                .setView(layout)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Tạo mã", (d, w) -> {
                    String email = etEmail.getText().toString().trim();
                    if (email.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (roomIds.isEmpty()) {
                        Toast.makeText(this, "Chưa có phòng để gán", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int idx = spinnerRoom.getSelectedItemPosition();
                    if (idx < 0 || idx >= roomIds.size()) {
                        Toast.makeText(this, "Vui lòng chọn phòng", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String roomId = roomIds.get(idx);

                    new InviteRepository().createTenantInvite(tenantId, email, roomId,
                            new InviteRepository.InviteCallback() {
                                @Override
                                public void onSuccess(@NonNull String code) {
                                    String msg = "TenantId: " + tenantId + "\nMã mời: " + code + "\nRoomId: " + roomId;
                                    new AlertDialog.Builder(HomeActivity.this)
                                            .setTitle("Mã mời đã tạo")
                                            .setMessage(msg)
                                            .setPositiveButton("OK", null)
                                            .show();
                                }

                                @Override
                                public void onError(@NonNull Exception e) {
                                    Toast.makeText(HomeActivity.this, "Tạo mã thất bại", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .show();
    }

    private void showJoinInviteDialog() {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 10);

        android.widget.EditText etTenant = new android.widget.EditText(this);
        etTenant.setHint("TenantId");
        layout.addView(etTenant);

        android.widget.EditText etCode = new android.widget.EditText(this);
        etCode.setHint("Mã mời");
        layout.addView(etCode);

        new AlertDialog.Builder(this)
                .setTitle("Tham gia tổ chức")
                .setView(layout)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Tham gia", (d, w) -> {
                    String tenantId = etTenant.getText().toString().trim();
                    String code = etCode.getText().toString().trim();
                    if (tenantId.isEmpty() || code.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập đầy đủ", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    new InviteRepository().joinByInvite(this, tenantId, code, new InviteRepository.JoinCallback() {
                        @Override
                        public void onSuccess(String joinedTenantId) {
                            applyRoleUI(joinedTenantId);
                            loadStats();
                            Toast.makeText(HomeActivity.this, "Đã tham gia tổ chức", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(HomeActivity.this, "Tham gia thất bại", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .show();
    }

    private void applyRoleUI(String tenantId) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || tenantId == null || tenantId.trim().isEmpty())
            return;

        db.collection("tenants").document(tenantId)
                .collection("members").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String role = doc.getString("role");
                    boolean isTenant = TenantRoles.TENANT.equals(role);

                    cardPhongTro.setVisibility(isTenant ? View.GONE : View.VISIBLE);
                    cardNguoiThue.setVisibility(isTenant ? View.GONE : View.VISIBLE);
                    cardDoanhThu.setVisibility(isTenant ? View.GONE : View.VISIBLE);
                    if (cardTickets != null)
                        cardTickets.setVisibility(View.VISIBLE);
                });
    }

    private void showCreateTenantDialog(TenantRepository repo) {
        android.widget.EditText et = new android.widget.EditText(this);
        et.setHint("Tên tổ chức (VD: Nhà trọ A)");
        et.setPadding(50, 30, 50, 30);

        new AlertDialog.Builder(this)
                .setTitle("Tạo tổ chức mới")
                .setView(et)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Tạo", (d, w) -> {
                    String name = et.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập tên", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    repo.createTenant(this, name, new TenantRepository.TenantReadyCallback() {
                        @Override
                        public void onReady(String tenantId) {
                            loadStats();
                            Toast.makeText(HomeActivity.this, "Đã tạo tổ chức", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(HomeActivity.this, "Tạo thất bại", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        removeListeners();
    }
}
