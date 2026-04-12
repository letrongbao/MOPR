package com.example.myapplication.features.home;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.AuthProviderUtil;
import com.example.myapplication.features.auth.MainActivity;
import com.example.myapplication.features.chat.ChatHubActivity;
import com.example.myapplication.features.contract.TenantContractDetailsActivity;
import com.example.myapplication.features.invoice.InvoiceActivity;
import com.example.myapplication.features.notification.NotificationCenterActivity;
import com.example.myapplication.features.notification.NotificationRealtimeObserver;
import com.example.myapplication.features.notification.push.AppFirebaseMessagingService;
import com.example.myapplication.features.report.TenantReportListActivity;
import com.example.myapplication.features.settings.ChangePasswordActivity;
import com.example.myapplication.features.settings.EditProfileActivity;
import com.example.myapplication.features.ticket.TicketActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.appbar.AppBarLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import android.graphics.Color;
import android.view.Window;

public class TenantMenuActivity extends AppCompatActivity {

    // ===== Header =====
    private TextView tvTenantName;
    private TextView tvRoomInfo;
    private TextView tvHouseInfo;
    private ImageView imgAvatar;

    // ===== Grid menu =====
    private CardView cardMyRoom;
    private CardView cardBill;
    private CardView cardContract;
    private CardView cardReport;
    private CardView cardNotification;

    // ===== Contract summary =====
    private ProgressBar contractProgress;
    private TextView tvDaysRemaining;
    private TextView tvMonthsStayed;
    private TextView tvContractStatus;
    private TextView tvStartDate;
    private TextView tvEndDate;
    private TextView btnViewContractDetail;

    // ===== DrawerLayout (tái sử dụng home_menu_profile_drawer.xml) =====
    private DrawerLayout tenantDrawerLayout;

    // IDs từ home_menu_profile_drawer.xml
    private ShapeableImageView drawerAvatar;
    private TextView drawerUserName;
    private TextView drawerUserEmail;
    private LinearLayout menuEditProfile;
    private LinearLayout menuChangePassword;
    private LinearLayout menuTenantProfiles;
    private LinearLayout menuRentalHistory;
    private LinearLayout menuLogout;
    private View btnDrawerNotification;
    private TextView tvDrawerNotificationBadge;

    // ===== Data =====
    private String tenantId;
    private String roomId;
    private String activeMemberContractId;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private NotificationRealtimeObserver notificationRealtimeObserver;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);

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
        tvHouseInfo  = findViewById(R.id.tvHouseInfo);
        imgAvatar    = findViewById(R.id.imgAvatarHeader);

        // ===== Ánh xạ Grid menu =====
        cardMyRoom       = findViewById(R.id.cardMyRoom);
        cardBill         = findViewById(R.id.cardBill);
        cardContract     = findViewById(R.id.cardContract);
        cardReport       = findViewById(R.id.cardReport);
        cardNotification = findViewById(R.id.cardNotification);

        contractProgress = findViewById(R.id.contractProgress);
        tvDaysRemaining = findViewById(R.id.tvDaysRemaining);
        tvMonthsStayed = findViewById(R.id.tvMonthsStayed);
        tvContractStatus = findViewById(R.id.tvContractStatus);
        tvStartDate = findViewById(R.id.tvStartDate);
        tvEndDate = findViewById(R.id.tvEndDate);
        btnViewContractDetail = findViewById(R.id.btnViewContractDetail);

        // ===== Ánh xạ DrawerLayout và các view bên trong drawer =====
        tenantDrawerLayout  = findViewById(R.id.tenantDrawerLayout);
        drawerAvatar        = findViewById(R.id.drawerAvatar);
        drawerUserName      = findViewById(R.id.drawerUserName);
        drawerUserEmail     = findViewById(R.id.drawerUserEmail);
        menuEditProfile     = findViewById(R.id.menuEditProfile);
        menuChangePassword  = findViewById(R.id.menuChangePassword);
        menuTenantProfiles  = findViewById(R.id.menuTenantProfiles);
        menuRentalHistory   = findViewById(R.id.menuRentalHistory);
        menuLogout          = findViewById(R.id.menuLogout);
        btnDrawerNotification = findViewById(R.id.btnDrawerNotification);
        tvDrawerNotificationBadge = findViewById(R.id.tvDrawerNotificationBadge);
        applyStylizedTenantCardLabels();

        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            final int baseTopPadding = appBarLayout.getPaddingTop();
            final int baseBottomPadding = appBarLayout.getPaddingBottom();
            final int baseStartPadding = appBarLayout.getPaddingLeft();
            final int baseEndPadding = appBarLayout.getPaddingRight();
            ViewCompat.setOnApplyWindowInsetsListener(appBarLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(baseStartPadding, baseTopPadding + systemBars.top, baseEndPadding, baseBottomPadding);
                return insets;
            });
        }

        View drawerHeader = findViewById(R.id.drawerHeader);
        if (drawerHeader != null) {
            drawerHeader.setBackgroundResource(R.drawable.bg_tenant_header_teal);
            final int baseTopPadding = drawerHeader.getPaddingTop();
            final int baseBottomPadding = drawerHeader.getPaddingBottom();
            final int baseStartPadding = drawerHeader.getPaddingLeft();
            final int baseEndPadding = drawerHeader.getPaddingRight();
            ViewCompat.setOnApplyWindowInsetsListener(drawerHeader, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(baseStartPadding, baseTopPadding + systemBars.top, baseEndPadding, baseBottomPadding);
                return insets;
            });
        }

        if (menuTenantProfiles != null) {
            menuTenantProfiles.setVisibility(View.GONE);
        }

        // Ẩn mục Rental History (không dùng cho Tenant)
        if (menuRentalHistory != null) {
            menuRentalHistory.setVisibility(View.GONE);
        }

        // ===== Load dữ liệu =====
        loadUserInfo();
        loadTenantScopedData();

        // ===== Click avatar → mở Drawer từ phải =====
        if (imgAvatar != null) {
            imgAvatar.setOnClickListener(v -> openProfileDrawer());
        }

        // ===== Setup click listeners cho drawer items =====
        setupDrawerListeners();
        setupDrawerNotificationEntry();

        // ===== Click 4 thẻ menu =====
        cardMyRoom.setOnClickListener(v -> openRoomDetail());
        cardBill.setOnClickListener(v -> openInvoicePage());
        cardContract.setOnClickListener(v -> openContractDetail());

        cardReport.setOnClickListener(v -> startActivity(new Intent(this, TenantReportListActivity.class)));

        cardNotification.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatHubActivity.class);
            intent.putExtra(ChatHubActivity.EXTRA_USE_TENANT_HEADER, true);
            startActivity(intent);
        });

        if (btnViewContractDetail != null) {
            btnViewContractDetail.setOnClickListener(v -> openContractDetail());
        }

        AppFirebaseMessagingService.syncTokenForCurrentUser();
        observeUnreadNotificationCount();
    }

    private void loadTenantScopedData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            tvRoomInfo.setText(getString(R.string.tenant_menu_room_unknown));
            if (tvHouseInfo != null) {
                tvHouseInfo.setText(getString(R.string.tenant_menu_house_unknown));
            }
            showNoContractUI();
            return;
        }

        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(userDoc -> {
                    String freshTenantId = userDoc.getString("activeTenantId");
                    if (freshTenantId == null || freshTenantId.trim().isEmpty()) {
                        freshTenantId = tenantId;
                    }
                    if (freshTenantId == null || freshTenantId.trim().isEmpty()) {
                        freshTenantId = TenantSession.getActiveTenantId();
                    }

                    if (freshTenantId == null || freshTenantId.trim().isEmpty()) {
                        tvRoomInfo.setText(getString(R.string.tenant_menu_room_unknown));
                        if (tvHouseInfo != null) {
                            tvHouseInfo.setText(getString(R.string.tenant_menu_house_unknown));
                        }
                        showNoContractUI();
                        return;
                    }

                    tenantId = freshTenantId;
                    db.collection("tenants").document(freshTenantId)
                            .collection("members")
                            .document(user.getUid())
                            .get()
                            .addOnSuccessListener(memberDoc -> {
                                if (memberDoc.exists()) {
                                    String mappedRoomId = memberDoc.getString("roomId");
                                    String mappedContractId = memberDoc.getString("contractId");

                                    if ((roomId == null || roomId.trim().isEmpty())
                                            && mappedRoomId != null
                                            && !mappedRoomId.trim().isEmpty()) {
                                        roomId = mappedRoomId.trim();
                                    }

                                    if (mappedContractId != null && !mappedContractId.trim().isEmpty()) {
                                        activeMemberContractId = mappedContractId.trim();
                                    }
                                }
                                loadRoomAndContractData();
                            })
                            .addOnFailureListener(e -> loadRoomAndContractData());
                })
                .addOnFailureListener(e -> loadRoomAndContractData());
    }

    private void loadRoomAndContractData() {
        if (roomId != null && !roomId.isEmpty()) {
            fetchRoomNumber(roomId);
            getContractSummary(roomId);
            return;
        }

        tvRoomInfo.setText(getString(R.string.tenant_menu_room_unknown));
        if (tvHouseInfo != null) {
            tvHouseInfo.setText(getString(R.string.tenant_menu_house_unknown));
        }
        showNoContractUI();
    }

    private void setupDrawerNotificationEntry() {
        if (btnDrawerNotification == null) {
            return;
        }
        btnDrawerNotification.setOnClickListener(v -> {
            tenantDrawerLayout.closeDrawer(GravityCompat.END);
            Intent intent = new Intent(this, NotificationCenterActivity.class);
            intent.putExtra(NotificationCenterActivity.EXTRA_USE_TENANT_HEADER, true);
            startActivity(intent);
        });
    }

    private void observeUnreadNotificationCount() {
        FirebaseUser user = mAuth.getCurrentUser();
        String activeTenantId = tenantId != null ? tenantId : TenantSession.getActiveTenantId();
        if (user == null || activeTenantId == null || activeTenantId.trim().isEmpty()) {
            updateUnreadBadge(0);
            return;
        }

        if (notificationRealtimeObserver != null) {
            notificationRealtimeObserver.stop();
            notificationRealtimeObserver = null;
        }

        notificationRealtimeObserver = new NotificationRealtimeObserver(
                this,
                db,
                activeTenantId,
                user.getUid(),
                this::updateUnreadBadge);
        notificationRealtimeObserver.start();
    }

    private void updateUnreadBadge(int count) {
        if (tvDrawerNotificationBadge == null) {
            return;
        }
        if (count <= 0) {
            tvDrawerNotificationBadge.setVisibility(View.GONE);
            return;
        }
        tvDrawerNotificationBadge.setVisibility(View.VISIBLE);
        tvDrawerNotificationBadge.setText(count > 99 ? "99+" : String.valueOf(count));
    }

    @Override
    protected void onDestroy() {
        if (notificationRealtimeObserver != null) {
            notificationRealtimeObserver.stop();
            notificationRealtimeObserver = null;
        }
        super.onDestroy();
    }

    // ========== MỞ MÀN HÌNH CHI TIẾT PHÒNG ==========
    private void openRoomDetail() {
        if (roomId == null || roomId.isEmpty()) {
            Toast.makeText(this, getString(R.string.tenant_menu_room_not_identified), Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, TenantRoomDetailActivity.class);
        intent.putExtra(TenantRoomDetailActivity.EXTRA_ROOM_ID,   roomId);
        intent.putExtra(TenantRoomDetailActivity.EXTRA_TENANT_ID, tenantId);
        startActivity(intent);
    }

    private void openContractDetail() {
        if (roomId == null || roomId.isEmpty()) {
            Toast.makeText(this, getString(R.string.tenant_menu_room_not_identified), Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, TenantContractDetailsActivity.class);
        intent.putExtra(TenantContractDetailsActivity.EXTRA_ROOM_ID, roomId);
        intent.putExtra(TenantContractDetailsActivity.EXTRA_TENANT_ID, tenantId);
        startActivity(intent);
    }

    private void openInvoicePage() {
        if (roomId == null || roomId.isEmpty()) {
            Toast.makeText(this, getString(R.string.tenant_menu_room_not_identified), Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, InvoiceActivity.class);
        intent.putExtra(InvoiceActivity.EXTRA_INITIAL_TAB, InvoiceActivity.TAB_REPORTED);
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
                Intent intent = new Intent(this, EditProfileActivity.class);
                intent.putExtra(EditProfileActivity.EXTRA_USE_TENANT_HEADER, true);
                startActivity(intent);
            });
        }

        // Đổi mật khẩu
        if (menuChangePassword != null) {
            menuChangePassword.setOnClickListener(v -> {
                tenantDrawerLayout.closeDrawer(GravityCompat.END);
                Intent intent = new Intent(this, ChangePasswordActivity.class);
                intent.putExtra(ChangePasswordActivity.EXTRA_USE_TENANT_HEADER, true);
                startActivity(intent);
            });
        }

        // Đăng xuất
        if (menuLogout != null) {
            menuLogout.setOnClickListener(v -> {
                tenantDrawerLayout.closeDrawer(GravityCompat.END);
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.logout))
                        .setMessage(getString(R.string.logout_confirm_message))
                        .setPositiveButton(getString(R.string.logout), (d, which) -> signOutAndOpenLogin())
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show();
            });
        }
    }

    private void signOutAndOpenLogin() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        TenantSession.clear(this);
        mAuth.signOut();

        Runnable navigateToLogin = () -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        };

        if (AuthProviderUtil.hasGoogleProvider(currentUser)) {
            GoogleSignInClient googleClient = GoogleSignIn.getClient(
                    this,
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build());
            googleClient.signOut().addOnCompleteListener(task -> navigateToLogin.run());
            return;
        }

        navigateToLogin.run();
    }

    // ========== LOAD TÊN USER LÊN HEADER ==========
    private void loadUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String displayName = user.getDisplayName();
        String email       = user.getEmail();

        if (displayName != null && !displayName.isEmpty()) {
            setGreeting(displayName);
        } else if (email != null) {
            setGreeting(email.split("@")[0]);
        }

        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String fullName = doc.getString("fullName");
                        if (fullName != null && !fullName.isEmpty()) {
                            setGreeting(fullName);
                        }
                    }
                });
    }

    private void setGreeting(String name) {
        if (tvTenantName == null) {
            return;
        }
        if (name == null || name.trim().isEmpty()) {
            tvTenantName.setText(getString(R.string.tenant_menu_greeting));
            return;
        }
        tvTenantName.setText(getString(R.string.tenant_menu_greeting_with_name, name.trim()));
    }

    private void applyStylizedTenantCardLabels() {
        setStylizedTenantCardLabel(findViewById(R.id.tvCardMyRoomLabel), getString(R.string.tenant_menu_my_room_yours));
        setStylizedTenantCardLabel(findViewById(R.id.tvCardContractLabel), getString(R.string.tenant_menu_contract_yours));
        setStylizedTenantCardLabel(findViewById(R.id.tvCardBillLabel), getString(R.string.tenant_menu_invoice_yours));
        setStylizedTenantCardLabel(findViewById(R.id.tvCardReportLabel), getString(R.string.tenant_menu_issue_incident));
    }

    private void setStylizedTenantCardLabel(TextView target, String label) {
        if (target == null) {
            return;
        }
        target.setText(buildStylizedCardLabel(label));
    }

    private CharSequence buildStylizedCardLabel(String rawLabel) {
        if (rawLabel == null) {
            return "";
        }

        String normalized = rawLabel.replace('\n', ' ').trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            return "";
        }

        String[] words = normalized.split(" ");
        String topLine;
        String bottomLine;

        if (getString(R.string.tenant_menu_my_room_yours).replace('\n', ' ').trim().equalsIgnoreCase(normalized)
                && words.length >= 3) {
            topLine = words[0];
            bottomLine = words[1] + " " + words[2];
        } else if (words.length >= 4) {
            topLine = words[0] + " " + words[1];
            bottomLine = words[words.length - 2] + " " + words[words.length - 1];
        } else if (words.length == 3) {
            topLine = words[0] + " " + words[1];
            bottomLine = words[2];
        } else if (words.length == 2) {
            topLine = words[0];
            bottomLine = words[1];
        } else {
            topLine = words[0];
            bottomLine = "";
        }

        String styled = bottomLine.isEmpty() ? topLine : topLine + "\n" + bottomLine;
        SpannableString span = new SpannableString(styled);
        span.setSpan(new RelativeSizeSpan(1.14f), 0, topLine.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        if (!bottomLine.isEmpty()) {
            int start = topLine.length() + 1;
            span.setSpan(new RelativeSizeSpan(0.92f), start, styled.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return span;
    }

    // ========== QUERY TÊN PHÒNG TỪ FIRESTORE ==========
    private void fetchRoomNumber(String roomId) {
        tvRoomInfo.setText(getString(R.string.tenant_menu_room_loading));

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            tvRoomInfo.setText(getString(R.string.tenant_menu_room_value, roomId));
            return;
        }

        // Đọc activeTenantId trực tiếp từ Firestore (nguồn đáng tin nhất)
        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(userDoc -> {
                    String freshTenantId = userDoc.getString("activeTenantId");

                    if (freshTenantId == null || freshTenantId.trim().isEmpty()) {
                        tvRoomInfo.setText(getString(R.string.tenant_menu_room_value, roomId));
                        return;
                    }

                    tenantId = freshTenantId;

                    // Bước 2a: tenants/{tenantId}/rooms/{roomId}
                    db.collection("tenants").document(freshTenantId)
                            .collection("rooms").document(roomId)
                            .get()
                            .addOnSuccessListener(roomDoc -> {
                                if (roomDoc.exists()) {
                                    applyRoomInfo(roomDoc, roomId);
                                } else {
                                    // Bước 2b: rooms lưu ở users/{tenantId}/rooms/ (khi owner không có tenant org)
                                    db.collection("users").document(freshTenantId)
                                            .collection("rooms").document(roomId)
                                            .get()
                                            .addOnSuccessListener(userRoomDoc -> {
                                                if (userRoomDoc.exists()) {
                                                    applyRoomInfo(userRoomDoc, roomId);
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
                .addOnFailureListener(e -> tvRoomInfo.setText(getString(R.string.tenant_menu_room_value, roomId)));
    }

    private void scanAllRooms(String freshTenantId, String roomId) {
        db.collection("tenants").document(freshTenantId)
                .collection("rooms").get()
                .addOnSuccessListener(qs -> {
                    DocumentSnapshot foundDoc = findRoomDoc(qs, roomId);
                    if (foundDoc != null) {
                        applyRoomInfo(foundDoc, roomId);
                    } else {
                        db.collection("users").document(freshTenantId)
                                .collection("rooms").get()
                                .addOnSuccessListener(qs2 -> {
                                    DocumentSnapshot foundDoc2 = findRoomDoc(qs2, roomId);
                                    if (foundDoc2 != null) {
                                        applyRoomInfo(foundDoc2, roomId);
                                    } else {
                                        tvRoomInfo.setText(getString(R.string.tenant_menu_room_value, roomId));
                                        if (tvHouseInfo != null) {
                                            tvHouseInfo.setText(getString(R.string.tenant_menu_house_unknown));
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    tvRoomInfo.setText(getString(R.string.tenant_menu_room_value, roomId));
                                    if (tvHouseInfo != null) {
                                        tvHouseInfo.setText(getString(R.string.tenant_menu_house_unknown));
                                    }
                                });
                    }
                })
                .addOnFailureListener(e ->
                        db.collection("users").document(freshTenantId)
                                .collection("rooms").get()
                                .addOnSuccessListener(qs2 -> {
                                    DocumentSnapshot foundDoc2 = findRoomDoc(qs2, roomId);
                                    if (foundDoc2 != null) {
                                        applyRoomInfo(foundDoc2, roomId);
                                    } else {
                                        tvRoomInfo.setText(getString(R.string.tenant_menu_room_value, roomId));
                                        if (tvHouseInfo != null) {
                                            tvHouseInfo.setText(getString(R.string.tenant_menu_house_unknown));
                                        }
                                    }
                                })
                                .addOnFailureListener(e2 -> {
                                    tvRoomInfo.setText(getString(R.string.tenant_menu_room_value, roomId));
                                    if (tvHouseInfo != null) {
                                        tvHouseInfo.setText(getString(R.string.tenant_menu_house_unknown));
                                    }
                                }));
    }

    private void applyRoomInfo(DocumentSnapshot roomDoc, String roomIdFallback) {
        String roomNumber = roomDoc.getString("roomNumber");
        String roomResolved = roomNumber != null && !roomNumber.isEmpty() ? roomNumber : roomIdFallback;
        tvRoomInfo.setText(getString(R.string.tenant_menu_room_value, roomResolved));

        if (tvHouseInfo == null) {
            return;
        }

        String houseAddress = roomDoc.getString("houseAddress");
        if (houseAddress == null || houseAddress.trim().isEmpty()) {
            houseAddress = roomDoc.getString("houseName");
        }
        if (houseAddress == null || houseAddress.trim().isEmpty()) {
            houseAddress = roomDoc.getString("address");
        }

        if (houseAddress != null && !houseAddress.trim().isEmpty()) {
            tvHouseInfo.setText(getString(R.string.tenant_menu_house_value, houseAddress.trim()));
        } else {
            tvHouseInfo.setText(getString(R.string.tenant_menu_house_unknown));
        }
    }

    private DocumentSnapshot findRoomDoc(com.google.firebase.firestore.QuerySnapshot qs, String roomId) {
        for (com.google.firebase.firestore.DocumentSnapshot doc : qs.getDocuments()) {
            if (roomId.equals(doc.getId()) || roomId.equals(doc.getString("roomId"))) {
                return doc;
            }
        }
        return null;
    }

    private void getContractSummary(String activeRoomId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            showNoContractUI();
            return;
        }

        if (activeMemberContractId != null && !activeMemberContractId.trim().isEmpty()) {
            loadContractByIdFirst(activeMemberContractId.trim(), activeRoomId);
            return;
        }

        queryContractByRoom(activeRoomId);
    }

    private void loadContractByIdFirst(String contractId, String activeRoomId) {
        db.collection("tenants").document(tenantId)
                .collection("contracts")
                .document(contractId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (isValidContractDoc(doc, activeRoomId)) {
                        applyContractToUI(doc);
                        return;
                    }

                    db.collection("users").document(tenantId)
                            .collection("contracts")
                            .document(contractId)
                            .get()
                            .addOnSuccessListener(legacyDoc -> {
                                if (isValidContractDoc(legacyDoc, activeRoomId)) {
                                    applyContractToUI(legacyDoc);
                                } else {
                                    queryContractByRoom(activeRoomId);
                                }
                            })
                            .addOnFailureListener(e -> queryContractByRoom(activeRoomId));
                })
                .addOnFailureListener(e -> queryContractByRoom(activeRoomId));
    }

    private void queryContractByRoom(String activeRoomId) {

        db.collection("tenants").document(tenantId)
                .collection("contracts")
                .whereEqualTo("roomId", activeRoomId)
                .limit(5)
                .get()
                .addOnSuccessListener(qs -> {
                    DocumentSnapshot activeContract = findActiveContract(qs);
                    if (activeContract != null) {
                        applyContractToUI(activeContract);
                    } else {
                        db.collection("users").document(tenantId)
                                .collection("contracts")
                                .whereEqualTo("roomId", activeRoomId)
                                .limit(5)
                                .get()
                                .addOnSuccessListener(qs2 -> {
                                    DocumentSnapshot c2 = findActiveContract(qs2);
                                    if (c2 != null) {
                                        applyContractToUI(c2);
                                    } else {
                                        showNoContractUI();
                                    }
                                })
                                .addOnFailureListener(e -> showNoContractUI());
                    }
                })
                .addOnFailureListener(e -> showNoContractUI());
    }

    private boolean isValidContractDoc(DocumentSnapshot doc, String activeRoomId) {
        if (doc == null || !doc.exists()) {
            return false;
        }
        if (activeRoomId == null || activeRoomId.trim().isEmpty()) {
            return true;
        }
        String roomInContract = doc.getString("roomId");
        return roomInContract == null || roomInContract.trim().isEmpty() || activeRoomId.equals(roomInContract);
    }

    private DocumentSnapshot findActiveContract(QuerySnapshot qs) {
        if (qs == null || qs.isEmpty()) {
            return null;
        }
        for (DocumentSnapshot doc : qs.getDocuments()) {
            String status = doc.getString("contractStatus");
            if ("ACTIVE".equalsIgnoreCase(status)) {
                return doc;
            }
        }
        return qs.getDocuments().get(0);
    }

    private void applyContractToUI(DocumentSnapshot doc) {
        Date startDate = parseDate(doc, "rentalStartDate", "startDate");
        Date endDate = parseDate(doc, "contractEndDate", "endDate", "contractEndTimestamp");

        if (startDate == null || endDate == null) {
            showNoContractUI();
            return;
        }

        Date today = new Date();
        long totalMs = endDate.getTime() - startDate.getTime();
        long remainingMs = endDate.getTime() - today.getTime();
        long totalDays = TimeUnit.MILLISECONDS.toDays(totalMs);
        long daysLeft = TimeUnit.MILLISECONDS.toDays(remainingMs);
        long daysStayed = TimeUnit.MILLISECONDS.toDays(today.getTime() - startDate.getTime());
        long monthsStayed = Math.max(0, daysStayed / 30);
        long extraDays = Math.max(0, daysStayed % 30);

        int progress = (totalDays > 0)
                ? (int) Math.max(0, Math.min(100, (remainingMs * 100L) / totalMs))
                : 0;

        if (contractProgress != null) {
            contractProgress.setProgress(progress);
        }
        if (tvDaysRemaining != null) {
            tvDaysRemaining.setText(String.valueOf(Math.max(0, daysLeft)));
        }
        if (tvMonthsStayed != null) {
            tvMonthsStayed.setText(getString(R.string.tenant_room_months_stayed_value, monthsStayed, extraDays));
        }
        if (tvStartDate != null) {
            tvStartDate.setText(DATE_FORMAT.format(startDate));
        }
        if (tvEndDate != null) {
            tvEndDate.setText(DATE_FORMAT.format(endDate));
        }
        if (tvContractStatus != null) {
            tvContractStatus.setText(daysLeft > 0
                    ? getString(R.string.tenant_room_contract_active)
                    : getString(R.string.tenant_room_contract_expired));
        }
    }

    private void showNoContractUI() {
        if (tvDaysRemaining != null) {
            tvDaysRemaining.setText(getString(R.string.tenant_room_value_placeholder));
        }
        if (tvMonthsStayed != null) {
            tvMonthsStayed.setText(getString(R.string.tenant_room_no_contract));
        }
        if (tvContractStatus != null) {
            tvContractStatus.setText(getString(R.string.tenant_room_contract_not_found));
        }
        if (tvStartDate != null) {
            tvStartDate.setText(getString(R.string.tenant_room_date_placeholder));
        }
        if (tvEndDate != null) {
            tvEndDate.setText(getString(R.string.tenant_room_date_placeholder));
        }
        if (contractProgress != null) {
            contractProgress.setProgress(0);
        }
    }

    private Date parseDate(DocumentSnapshot doc, String... fieldNames) {
        for (String field : fieldNames) {
            Object val = doc.get(field);
            if (val == null) {
                continue;
            }

            if (val instanceof com.google.firebase.Timestamp) {
                return ((com.google.firebase.Timestamp) val).toDate();
            }
            if (val instanceof String) {
                String str = (String) val;
                if (!str.isEmpty()) {
                    try {
                        return DATE_FORMAT.parse(str);
                    } catch (ParseException ignored) {
                        try {
                            return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(str);
                        } catch (ParseException ignored2) {
                            // Continue parsing fallback types.
                        }
                    }
                }
            }
            if (val instanceof Long) {
                return new Date((Long) val);
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
