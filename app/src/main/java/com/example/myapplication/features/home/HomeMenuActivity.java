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
import com.example.myapplication.core.constants.TenantRoles;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.LanguageManager;
import com.example.myapplication.features.auth.MainActivity;
import com.example.myapplication.features.settings.ChangePasswordActivity;
import com.example.myapplication.features.settings.EditProfileActivity;
import com.example.myapplication.features.history.RentalHistoryActivity;
import com.example.myapplication.features.finance.ExpenseActivity;
import com.example.myapplication.features.finance.RevenueActivity;
import com.example.myapplication.features.invoice.InvoiceActivity;
import com.example.myapplication.features.invoice.TenantPaymentHistoryActivity;
import com.example.myapplication.features.property.house.HouseActivity;
import com.example.myapplication.features.ticket.TicketActivity;
import com.example.myapplication.features.tenant.TenantActivity;
import com.example.myapplication.features.contract.ContractListActivity;
import com.example.myapplication.domain.Room;
import com.example.myapplication.core.repository.domain.HouseRepository;
import com.example.myapplication.viewmodel.RoomViewModel;
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
    private String resolvedRole = TenantRoles.OWNER;
    private View btnLangEn;
    private View btnLangVi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_home_menu);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Launcher used to reload drawer after profile editing
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

        btnLangEn = findViewById(R.id.btnLangEnHome);
        btnLangVi = findViewById(R.id.btnLangViHome);
        setupLanguageSwitcher();

        // Setup drawer menu items
        setupDrawerMenu();

        // Load user info to drawer
        loadUserInfoToDrawer();

        applyRoleUi(TenantRoles.OWNER);
        updateStatistics();
        setupMenuCards(TenantRoles.OWNER);
        resolveRoleAndApplyUi();
    }

    private void setupMenuCards(String role) {
        MaterialCardView cardHouse = findViewById(R.id.cardHouse);
        MaterialCardView cardInvoice = findViewById(R.id.cardInvoice);
        MaterialCardView cardExpense = findViewById(R.id.cardExpense);
        MaterialCardView cardReport = findViewById(R.id.cardReport);
        MaterialCardView cardKhachThue = findViewById(R.id.cardKhachThue);
        boolean isTenant = TenantRoles.TENANT.equals(role);

        if (cardHouse != null) {
            if (isTenant) {
                cardHouse.setOnClickListener(v -> startActivity(new Intent(this, HouseActivity.class)));
            } else {
                cardHouse.setOnClickListener(v -> startActivity(new Intent(this, HouseActivity.class)));
            }
        }

        if (cardInvoice != null) {
            cardInvoice.setOnClickListener(v -> startActivity(new Intent(this, InvoiceActivity.class)));
        }

        if (cardExpense != null) {
            if (isTenant) {
                cardExpense
                        .setOnClickListener(v -> startActivity(new Intent(this, TenantPaymentHistoryActivity.class)));
            } else {
                cardExpense.setOnClickListener(v -> startActivity(new Intent(this, ExpenseActivity.class)));
            }
        }

        if (cardReport != null) {
            if (isTenant) {
                cardReport.setOnClickListener(v -> startActivity(new Intent(this, TicketActivity.class)));
            } else {
                cardReport.setOnClickListener(v -> startActivity(new Intent(this, RevenueActivity.class)));
            }
        }

        // Tenant Management card
        if (cardKhachThue != null) {
            cardKhachThue.setOnClickListener(v -> startActivity(new Intent(this, TenantActivity.class)));
        }

        // Smart Contract card
        MaterialCardView cardHopDong = findViewById(R.id.cardHopDong);
        if (cardHopDong != null) {
            cardHopDong.setOnClickListener(v -> startActivity(new Intent(this, ContractListActivity.class)));
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
                        .setTitle(getString(R.string.logout))
                        .setMessage(getString(R.string.logout_confirm_message))
                        .setPositiveButton(getString(R.string.logout), (dialog, which) -> {
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

        // Feedback
        LinearLayout menuFeedback = findViewById(R.id.menuFeedback);
        if (menuFeedback != null) {
            menuFeedback.setOnClickListener(v -> {
                Toast.makeText(this, getString(R.string.support_contact), Toast.LENGTH_SHORT).show();
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
                        String fullName = doc.getString("fullName");
                        String avatarUrl = doc.getString("avatarUrl");

                        if (fullName != null && !fullName.isEmpty() && drawerUserName != null) {
                            drawerUserName.setText(fullName);
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
        resolveRoleAndApplyUi();
        refreshLanguageButtons();
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
        new HouseRepository().listAll().observe(this, list -> {
            if (list != null && tvTotalHouses != null)
                tvTotalHouses.setText(String.valueOf(list.size()));
        });

        RoomViewModel phongViewModel = new ViewModelProvider(this).get(RoomViewModel.class);
        phongViewModel.getRoomList().observe(this, list -> {
            if (list != null) {
                long vacant = 0;
                long rented = 0;
                for (Room p : list) {
                    if (RoomStatus.VACANT.equals(p.getStatus()))
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
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app_text));
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_app_chooser_title)));
    }

    private void resolveRoleAndApplyUi() {
        String tenantId = TenantSession.getActiveTenantId();
        FirebaseUser user = mAuth.getCurrentUser();
        if (tenantId == null || tenantId.trim().isEmpty() || user == null) {
            applyRoleUi(TenantRoles.OWNER);
            setupMenuCards(TenantRoles.OWNER);
            return;
        }

        db.collection("tenants").document(tenantId)
                .collection("members").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String role = doc != null ? doc.getString("role") : null;
                    if (role == null || role.trim().isEmpty()) {
                        role = TenantRoles.OWNER;
                    }
                    applyRoleUi(role);
                    setupMenuCards(role);
                })
                .addOnFailureListener(e -> {
                    applyRoleUi(TenantRoles.OWNER);
                    setupMenuCards(TenantRoles.OWNER);
                });
    }

    private void applyRoleUi(String role) {
        resolvedRole = role;
        boolean isTenant = TenantRoles.TENANT.equals(role);

        View cardStats = findViewById(R.id.cardStats);
        View rowBottom = findViewById(R.id.rowBottom);
        TextView tvCardHouseLabel = findViewById(R.id.tvCardHouseLabel);
        TextView tvCardInvoiceLabel = findViewById(R.id.tvCardInvoiceLabel);
        TextView tvCardExpenseLabel = findViewById(R.id.tvCardExpenseLabel);
        TextView tvCardReportLabel = findViewById(R.id.tvCardReportLabel);

        if (cardStats != null) {
            cardStats.setVisibility(isTenant ? View.GONE : View.VISIBLE);
        }
        if (rowBottom != null) {
            rowBottom.setVisibility(isTenant ? View.GONE : View.VISIBLE);
        }

        if (tvCardHouseLabel != null) {
            tvCardHouseLabel.setText(
                    isTenant ? getString(R.string.home_tenant_room_label) : getString(R.string.home_owner_house_label));
        }
        if (tvCardInvoiceLabel != null) {
            tvCardInvoiceLabel.setText(isTenant ? getString(R.string.home_tenant_invoice_label)
                    : getString(R.string.home_owner_invoice_label));
        }
        if (tvCardExpenseLabel != null) {
            tvCardExpenseLabel.setText(isTenant ? getString(R.string.home_tenant_payment_history_label)
                    : getString(R.string.home_owner_expense_label));
        }
        if (tvCardReportLabel != null) {
            tvCardReportLabel.setText(isTenant ? getString(R.string.home_tenant_support_label)
                    : getString(R.string.home_owner_report_label));
        }
    }

    private void setupLanguageSwitcher() {
        refreshLanguageButtons();
        if (btnLangEn != null) {
            btnLangEn.setOnClickListener(v -> switchLanguage("en"));
        }
        if (btnLangVi != null) {
            btnLangVi.setOnClickListener(v -> switchLanguage("vi"));
        }
    }

    private void switchLanguage(String languageTag) {
        if (LanguageManager.isCurrentLanguage(this, languageTag)) {
            return;
        }
        LanguageManager.setLanguage(this, languageTag);
        recreate();
    }

    private void refreshLanguageButtons() {
        boolean isEnglish = LanguageManager.isCurrentLanguage(this, "en");
        if (btnLangEn != null) {
            btnLangEn.setAlpha(isEnglish ? 1f : 0.55f);
        }
        if (btnLangVi != null) {
            btnLangVi.setAlpha(isEnglish ? 0.55f : 1f);
        }
    }
}
