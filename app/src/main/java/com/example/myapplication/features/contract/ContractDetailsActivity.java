package com.example.myapplication.features.contract;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.core.repository.domain.TenantRepository;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.ContractStatusHelper;
import com.example.myapplication.core.util.ScreenUiHelper;
import com.example.myapplication.domain.ContractStatus;
import com.example.myapplication.domain.Tenant;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ContractDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_CONTRACT_ID = "CONTRACT_ID";

    private FirebaseFirestore db;
    private TenantRepository repository;
    private String contractId;

    // UI Elements
    private TextView tvRoomName, tvContractNumber, tvRepresentativeName, tvPhoneNumber;
    private TextView tvCreatedDate, tvEndDate, tvRentAmount, tvDepositAmount;
    private Chip chipStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScreenUiHelper.enableEdgeToEdge(this, false);

        setContentView(R.layout.activity_contract_details);

        // Get contract ID from intent
        contractId = getIntent().getStringExtra(EXTRA_CONTRACT_ID);
        if (contractId == null || contractId.trim().isEmpty()) {
            Toast.makeText(this, getString(R.string.error_contract_id_not_found), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // AppBar vertical padding
        View appBar = findViewById(R.id.appBarLayout);
        if (appBar != null) {
            ScreenUiHelper.applyTopInset(appBar);
        }

        // Initialize
        db = FirebaseFirestore.getInstance();
        repository = new TenantRepository();

        // Bind views
        bindViews();

        // Setup listeners
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Load data
        loadContractDetails();
    }

    private void bindViews() {
        tvRoomName = findViewById(R.id.tvTenPhong);
        tvContractNumber = findViewById(R.id.tvSoHopDong);
        tvRepresentativeName = findViewById(R.id.tvNguoiDaiDien);
        tvPhoneNumber = findViewById(R.id.tvSoDienThoai);
        tvCreatedDate = findViewById(R.id.tvNgayLap);
        tvEndDate = findViewById(R.id.tvNgayHetHan);
        tvRentAmount = findViewById(R.id.tvGiaThue);
        tvDepositAmount = findViewById(R.id.tvTienCoc);
        chipStatus = findViewById(R.id.chipTrangThai);
    }

    private void loadContractDetails() {
        String tenantId = TenantSession.getActiveTenantId();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(this, getString(R.string.error_not_logged_in), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get contract from Firestore
        if (tenantId != null && !tenantId.isEmpty()) {
            db.collection("tenants")
                    .document(tenantId)
                    .collection("contracts")
                    .document(contractId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Tenant contract = documentSnapshot.toObject(Tenant.class);
                            if (contract != null) {
                                contract.setId(documentSnapshot.getId());
                                displayContractDetails(contract);
                            }
                        } else {
                            Toast.makeText(this, getString(R.string.contract_not_found), Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, getString(R.string.error_colon) + e.getMessage(), Toast.LENGTH_LONG).show();
                        finish();
                    });
        } else {
            db.collection("users")
                    .document(user.getUid())
                    .collection("contracts")
                    .document(contractId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Tenant contract = documentSnapshot.toObject(Tenant.class);
                            if (contract != null) {
                                contract.setId(documentSnapshot.getId());
                                displayContractDetails(contract);
                            }
                        } else {
                            Toast.makeText(this, getString(R.string.contract_not_found), Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, getString(R.string.error_colon) + e.getMessage(), Toast.LENGTH_LONG).show();
                        finish();
                    });
        }
    }

    private void displayContractDetails(Tenant contract) {
        // Internal note.
        if (tvRoomName != null) {
            String roomLabel = contract.getRoomNumber() != null
                    ? getString(R.string.room_number, contract.getRoomNumber())
                    : "—";
            tvRoomName.setText(roomLabel);
        }

        // Internal note.
        if (tvContractNumber != null) {
            String contractNumberText = contract.getContractNumber() != null ? contract.getContractNumber() : "—";
            tvContractNumber.setText(contractNumberText);
        }

        // Internal note.
        if (tvRepresentativeName != null) {
            String representative = contract.getRepresentativeName();
            if (representative == null || representative.trim().isEmpty()) {
                representative = contract.getFullName(); // Fallback to fullName
            }
            tvRepresentativeName.setText(representative != null ? representative : "—");
        }

        // Internal note.
        if (tvPhoneNumber != null) {
            String phoneNumber = contract.getPhoneNumber() != null ? contract.getPhoneNumber() : "—";
            tvPhoneNumber.setText(phoneNumber);
        }

        // Internal note.
        if (tvCreatedDate != null) {
            String createdDateText = contract.getRentalStartDate() != null ? contract.getRentalStartDate() : "—";
            tvCreatedDate.setText(createdDateText);
        }

        // Internal note.
        if (tvEndDate != null) {
            long contractEndTimestamp = contract.getContractEndTimestamp();
            if (contractEndTimestamp > 0) {
                // Internal note.
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                String endDateText = sdf.format(new Date(contractEndTimestamp));
                tvEndDate.setText(endDateText);

                // Internal note.
                long currentTime = System.currentTimeMillis();
                long timeRemaining = contractEndTimestamp - currentTime;
                final long THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000; // 2592000000

                if (timeRemaining < THIRTY_DAYS_MS && timeRemaining > 0) {
                    tvEndDate.setTextColor(Color.parseColor("#F44336"));
                } else {
                    tvEndDate.setTextColor(Color.parseColor("#FF6D00"));
                }
            } else {
                // Internal note.
                String endDateText = contract.getContractEndDate() != null ? contract.getContractEndDate() : "—";
                tvEndDate.setText(endDateText);
                tvEndDate.setTextColor(Color.parseColor("#FF6D00")); // Cam
            }
        }

        // Internal note.
        if (tvRentAmount != null) {
            long rentAmount = contract.getRentAmount();
            String rentFormatted = String.format(Locale.US, "%,d ₫", rentAmount).replace(',', '.');
            tvRentAmount.setText(rentFormatted);
        }

        // Internal note.
        if (tvDepositAmount != null) {
            long depositAmount = contract.getDepositAmount();
            String depositFormatted = String.format(Locale.US, "%,d ₫", depositAmount).replace(',', '.');
            tvDepositAmount.setText(depositFormatted);
        }

        // Internal note.
        if (chipStatus != null) {
            ContractStatus status = ContractStatusHelper.resolve(contract);
            long daysLeft = ContractStatusHelper.daysRemaining(contract);

            switch (status) {
                case ENDED:
                    chipStatus.setText(R.string.expired);
                    chipStatus.setChipBackgroundColor(
                            android.content.res.ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
                    break;

                case EXPIRING_SOON:
                    String text = getString(R.string.contract_status_expiring_soon);
                    if (daysLeft >= 0) {
                        text = getString(R.string.contract_status_days_left, daysLeft);
                    }
                    chipStatus.setText(text);
                    chipStatus.setChipBackgroundColor(
                            android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336")));
                    break;

                case ACTIVE_RENTAL:
                default:
                    chipStatus.setText(R.string.active_valid);
                    chipStatus.setChipBackgroundColor(
                            android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                    break;
            }
        }
    }
}
