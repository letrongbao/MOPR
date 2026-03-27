package com.example.myapplication.features.home;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.core.constants.RoomStatus;
import com.example.myapplication.features.auth.MainActivity;
import com.example.myapplication.features.settings.ChangePasswordActivity;
import com.example.myapplication.features.settings.EditProfileActivity;
import com.example.myapplication.features.history.RentalHistoryActivity;
import com.example.myapplication.features.finance.ChiPhiActivity;
import com.example.myapplication.features.finance.DoanhThuActivity;
import com.example.myapplication.features.invoice.HoaDonActivity;
import com.example.myapplication.features.property.house.CanNhaActivity;
import com.example.myapplication.domain.PhongTro;
import com.example.myapplication.core.repository.domain.CanNhaRepository;
import com.example.myapplication.viewmodel.PhongTroViewModel;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeMenuActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DrawerLayout drawerLayout;
    private TextView tvTotalHouses, tvVacantRooms, tvRentedRooms;
    private TextView drawerUserName, drawerUserEmail;
    private ShapeableImageView drawerAvatar;
    private ActivityResultLauncher<Intent> editProfileLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_home_menu);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Launcher để reload drawer khi edit profile xong
        editProfileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        loadUserInfoToDrawer();
                    }
                });

        // Setup DrawerLayout
        drawerLayout = findViewById(R.id.drawerLayout);

        View appBarLayout = findViewById(R.id.appBarLayout);
        ViewCompat.setOnApplyWindowInsetsListener(appBarLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        View drawerHeader = findViewById(R.id.drawerHeader);
        if (drawerHeader != null) {
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

        View profileDrawer = findViewById(R.id.profileDrawer);
        if (profileDrawer != null) {
            profileDrawer.setOnClickListener(v -> {
                // Consume taps on empty drawer area so touches do not pass through to Home
                // content.
            });
        }

        tvTotalHouses = findViewById(R.id.tvTotalHouses);
        tvVacantRooms = findViewById(R.id.tvVacantRooms);
        tvRentedRooms = findViewById(R.id.tvRentedRooms);

        // Drawer views
        drawerUserName = findViewById(R.id.drawerUserName);
        drawerUserEmail = findViewById(R.id.drawerUserEmail);
        drawerAvatar = findViewById(R.id.drawerAvatar);

        // Profile button opens drawer
        View btnProfile = findViewById(R.id.btnProfile);
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                drawerLayout.openDrawer(GravityCompat.END);
            });
        }

        // Setup drawer menu items
        setupDrawerMenu();

        // Load user info to drawer
        loadUserInfoToDrawer();

        updateStatistics();

        // Setup menu card click listeners
        setupMenuCards();
    }

    private void setupMenuCards() {
        MaterialCardView cardHouse = findViewById(R.id.cardHouse);
        MaterialCardView cardInvoice = findViewById(R.id.cardInvoice);
        MaterialCardView cardExpense = findViewById(R.id.cardExpense);
        MaterialCardView cardReport = findViewById(R.id.cardReport);

        if (cardHouse != null) {
            cardHouse.setOnClickListener(v -> startActivity(new Intent(this, CanNhaActivity.class)));
        }

        if (cardInvoice != null) {
            cardInvoice.setOnClickListener(v -> startActivity(new Intent(this, HoaDonActivity.class)));
        }

        if (cardExpense != null) {
            cardExpense.setOnClickListener(v -> startActivity(new Intent(this, ChiPhiActivity.class)));
        }

        if (cardReport != null) {
            cardReport.setOnClickListener(v -> startActivity(new Intent(this, DoanhThuActivity.class)));
        }
    }

    private void setupDrawerMenu() {
        // Edit Profile
        LinearLayout menuEditProfile = findViewById(R.id.menuEditProfile);
        if (menuEditProfile != null) {
            menuEditProfile.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.END);
                editProfileLauncher.launch(new Intent(this, EditProfileActivity.class));
            });
        }

        // Rental History
        LinearLayout menuRentalHistory = findViewById(R.id.menuRentalHistory);
        if (menuRentalHistory != null) {
            menuRentalHistory.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.END);
                startActivity(new Intent(this, RentalHistoryActivity.class));
            });
        }

        // Change Password
        LinearLayout menuChangePassword = findViewById(R.id.menuChangePassword);
        if (menuChangePassword != null) {
            menuChangePassword.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.END);
                startActivity(new Intent(this, ChangePasswordActivity.class));
            });
        }

        // Logout
        LinearLayout menuLogout = findViewById(R.id.menuLogout);
        if (menuLogout != null) {
            menuLogout.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Đăng xuất")
                        .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                        .setPositiveButton("Đăng xuất", (dialog, which) -> {
                            mAuth.signOut();
                            Intent intent = new Intent(this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            });
        }

        // Feedback
        LinearLayout menuFeedback = findViewById(R.id.menuFeedback);
        if (menuFeedback != null) {
            menuFeedback.setOnClickListener(v -> {
                Toast.makeText(this, "Liên hệ hỗ trợ: 0987.654.321", Toast.LENGTH_SHORT).show();
            });
        }

        // Share
        LinearLayout menuShare = findViewById(R.id.menuShare);
        if (menuShare != null) {
            menuShare.setOnClickListener(v -> {
                shareApp();
            });
        }
    }

    private void loadUserInfoToDrawer() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null)
            return;

        String displayName = user.getDisplayName();
        String email = user.getEmail();

        if (drawerUserEmail != null) {
            drawerUserEmail.setText(email);
        }

        if (displayName != null && !displayName.isEmpty()) {
            if (drawerUserName != null) {
                drawerUserName.setText(displayName);
            }
        } else if (email != null) {
            if (drawerUserName != null) {
                drawerUserName.setText(email.split("@")[0]);
            }
        }

        // Load from Firestore for more details
        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String hoTen = doc.getString("hoTen");
                        String avatarUrl = doc.getString("avatarUrl");

                        if (hoTen != null && !hoTen.isEmpty() && drawerUserName != null) {
                            drawerUserName.setText(hoTen);
                        }
                        if (avatarUrl != null && !avatarUrl.isEmpty() && drawerAvatar != null) {
                            Glide.with(this)
                                    .load(avatarUrl)
                                    .circleCrop()
                                    .into(drawerAvatar);
                        }
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserInfoToDrawer();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
    }

    private void updateStatistics() {
        new CanNhaRepository().listAll().observe(this, list -> {
            if (list != null && tvTotalHouses != null)
                tvTotalHouses.setText(String.valueOf(list.size()));
        });

        PhongTroViewModel phongViewModel = new ViewModelProvider(this).get(PhongTroViewModel.class);
        phongViewModel.getDanhSachPhong().observe(this, list -> {
            if (list != null) {
                long vacant = 0;
                long rented = 0;
                for (PhongTro p : list) {
                    if (RoomStatus.VACANT.equals(p.getTrangThai()))
                        vacant++;
                    else
                        rented++;
                }
                if (tvVacantRooms != null)
                    tvVacantRooms.setText(String.valueOf(vacant));
                if (tvRentedRooms != null)
                    tvRentedRooms.setText(String.valueOf(rented));
            }
        });
    }

    private void shareApp() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Quản Lý Trọ");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Ứng dụng quản lý nhà trọ tuyệt vời dành cho bạn!");
        startActivity(Intent.createChooser(shareIntent, "Chia sẻ ứng dụng"));
    }
}
