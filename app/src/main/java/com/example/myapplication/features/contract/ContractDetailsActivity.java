package com.example.myapplication.features.contract;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.myapplication.R;
import com.example.myapplication.core.repository.domain.TenantRepository;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.ContractStatusHelper;
import com.example.myapplication.core.util.ScreenUiHelper;
import com.example.myapplication.domain.ContractStatus;
import com.example.myapplication.domain.House;
import com.example.myapplication.domain.Room;
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
    public static final String EXTRA_HEADER_TITLE = "HEADER_TITLE";
    public static final String EXTRA_TENANT_ID = "TENANT_ID";

    private FirebaseFirestore db;
    private TenantRepository repository;
    private String contractId;
    private String scopedTenantId;
    private Tenant currentContract;
    private Room currentRoom;
    private House currentHouse;
    private View btnViewFullContract;

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

        Toolbar toolbar = findViewById(R.id.toolbar);
        String headerTitle = getIntent().getStringExtra(EXTRA_HEADER_TITLE);
        if (headerTitle == null || headerTitle.trim().isEmpty()) {
            headerTitle = getString(R.string.contract_details_title);
        }
        ScreenUiHelper.setupBackToolbar(this, toolbar, headerTitle);

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
        btnViewFullContract = findViewById(R.id.btnViewFullContract);
        if (btnViewFullContract != null) {
            btnViewFullContract.setOnClickListener(v -> openFullContractPreview());
        }
    }

    private void loadContractDetails() {
        String tenantId = getIntent().getStringExtra(EXTRA_TENANT_ID);
        if (tenantId == null || tenantId.trim().isEmpty()) {
            tenantId = TenantSession.getActiveTenantId();
        }
        scopedTenantId = tenantId != null ? tenantId.trim() : "";
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(this, getString(R.string.error_not_logged_in), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get contract from Firestore
        if (!scopedTenantId.isEmpty()) {
            db.collection("tenants")
                    .document(scopedTenantId)
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
                            fallbackLoadContractFromUsersScoped(user);
                        }
                    })
                    .addOnFailureListener(e -> fallbackLoadContractFromUsersScoped(user));
        } else {
            fallbackLoadContractFromUsersScoped(user);
        }
    }

    private void fallbackLoadContractFromUsersScoped(@NonNull FirebaseUser user) {
        if (!scopedTenantId.isEmpty()) {
            db.collection("users")
                    .document(scopedTenantId)
                    .collection("contracts")
                    .document(contractId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Tenant contract = documentSnapshot.toObject(Tenant.class);
                            if (contract != null) {
                                contract.setId(documentSnapshot.getId());
                                displayContractDetails(contract);
                                return;
                            }
                        }
                        fallbackLoadContractFromCurrentUser(user);
                    })
                    .addOnFailureListener(e -> fallbackLoadContractFromCurrentUser(user));
            return;
        }

        fallbackLoadContractFromCurrentUser(user);
    }

    private void fallbackLoadContractFromCurrentUser(@NonNull FirebaseUser user) {
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
                            return;
                        }
                    }
                    Toast.makeText(this, getString(R.string.contract_not_found), Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.error_colon) + e.getMessage(), Toast.LENGTH_LONG)
                            .show();
                    finish();
                });
    }

    private void displayContractDetails(Tenant contract) {
        currentContract = contract;
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

    private void openFullContractPreview() {
        if (currentContract == null) {
            Toast.makeText(this, getString(R.string.contract_not_found), Toast.LENGTH_SHORT).show();
            return;
        }

        resolveRoomAndHouseForContract(currentContract, () -> {
            String html = ContractHtmlBuilder.buildContractHtml(this, currentContract, currentRoom, currentHouse);
            Intent intent = new Intent(this, ContractPdfPreviewActivity.class);
            intent.putExtra(ContractPdfPreviewActivity.EXTRA_HTML, html);
            intent.putExtra(
                    ContractPdfPreviewActivity.EXTRA_FILE_NAME,
                    buildContractPdfFileName(currentContract));
            intent.putExtra(ContractPdfPreviewActivity.EXTRA_SHARE_SUBJECT, getString(R.string.contract_details_title));
            intent.putExtra(ContractPdfPreviewActivity.EXTRA_SHARE_TEXT, getString(R.string.room_view_contract));
            startActivity(intent);
        });
    }

    private String buildContractPdfFileName(@NonNull Tenant contract) {
        String number = contract.getContractNumber();
        if (number == null || number.trim().isEmpty()) {
            return getString(R.string.contract_pdf_filename_placeholder);
        }
        String safeNumber = number.trim().replaceAll("[^a-zA-Z0-9_-]", "_");
        return "HopDong_" + safeNumber + ".pdf";
    }

    private void resolveRoomAndHouseForContract(@NonNull Tenant contract, @NonNull Runnable onDone) {
        String roomId = contract.getRoomId();
        if (roomId == null || roomId.trim().isEmpty()) {
            currentRoom = null;
            currentHouse = null;
            onDone.run();
            return;
        }

        String tenantId = scopedTenantId;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (tenantId != null && !tenantId.trim().isEmpty()) {
            db.collection("tenants")
                    .document(tenantId)
                    .collection("rooms")
                    .document(roomId)
                    .get()
                    .addOnSuccessListener(roomDoc -> {
                        if (roomDoc != null && roomDoc.exists()) {
                            currentRoom = roomDoc.toObject(Room.class);
                            if (currentRoom != null) {
                                currentRoom.setId(roomDoc.getId());
                            }
                            resolveHouseForCurrentRoom(tenantId, user, onDone);
                            return;
                        }

                        db.collection("users")
                                .document(tenantId)
                                .collection("rooms")
                                .document(roomId)
                                .get()
                                .addOnSuccessListener(userRoomDoc -> {
                                    currentRoom = userRoomDoc.toObject(Room.class);
                                    if (currentRoom != null) {
                                        currentRoom.setId(userRoomDoc.getId());
                                    }
                                    resolveHouseForCurrentRoom(tenantId, user, onDone);
                                })
                                .addOnFailureListener(innerError -> {
                                    currentRoom = null;
                                    currentHouse = null;
                                    onDone.run();
                                });
                    })
                    .addOnFailureListener(e -> {
                        db.collection("users")
                                .document(tenantId)
                                .collection("rooms")
                                .document(roomId)
                                .get()
                                .addOnSuccessListener(userRoomDoc -> {
                                    currentRoom = userRoomDoc.toObject(Room.class);
                                    if (currentRoom != null) {
                                        currentRoom.setId(userRoomDoc.getId());
                                    }
                                    resolveHouseForCurrentRoom(tenantId, user, onDone);
                                })
                                .addOnFailureListener(innerError -> {
                                    currentRoom = null;
                                    currentHouse = null;
                                    onDone.run();
                                });
                    });
            return;
        }

        if (user == null) {
            currentRoom = null;
            currentHouse = null;
            onDone.run();
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .collection("rooms")
                .document(roomId)
                .get()
                .addOnSuccessListener(roomDoc -> {
                    currentRoom = roomDoc.toObject(Room.class);
                    if (currentRoom != null) {
                        currentRoom.setId(roomDoc.getId());
                    }
                    resolveHouseForCurrentRoom(null, user, onDone);
                })
                .addOnFailureListener(e -> {
                    currentRoom = null;
                    currentHouse = null;
                    onDone.run();
                });
    }

    private void resolveHouseForCurrentRoom(String tenantId, FirebaseUser user, @NonNull Runnable onDone) {
        if (currentRoom == null || currentRoom.getHouseId() == null || currentRoom.getHouseId().trim().isEmpty()) {
            currentHouse = null;
            onDone.run();
            return;
        }

        String houseId = currentRoom.getHouseId();
        if (tenantId != null && !tenantId.trim().isEmpty()) {
            db.collection("tenants")
                    .document(tenantId)
                    .collection("houses")
                    .document(houseId)
                    .get()
                    .addOnSuccessListener(houseDoc -> {
                        if (houseDoc != null && houseDoc.exists()) {
                            currentHouse = houseDoc.toObject(House.class);
                            if (currentHouse != null) {
                                currentHouse.setId(houseDoc.getId());
                            }
                            onDone.run();
                            return;
                        }

                        db.collection("users")
                                .document(tenantId)
                                .collection("houses")
                                .document(houseId)
                                .get()
                                .addOnSuccessListener(userHouseDoc -> {
                                    currentHouse = userHouseDoc.toObject(House.class);
                                    if (currentHouse != null) {
                                        currentHouse.setId(userHouseDoc.getId());
                                    }
                                    onDone.run();
                                })
                                .addOnFailureListener(innerError -> {
                                    currentHouse = null;
                                    onDone.run();
                                });
                    })
                    .addOnFailureListener(e -> {
                        db.collection("users")
                                .document(tenantId)
                                .collection("houses")
                                .document(houseId)
                                .get()
                                .addOnSuccessListener(userHouseDoc -> {
                                    currentHouse = userHouseDoc.toObject(House.class);
                                    if (currentHouse != null) {
                                        currentHouse.setId(userHouseDoc.getId());
                                    }
                                    onDone.run();
                                })
                                .addOnFailureListener(innerError -> {
                                    currentHouse = null;
                                    onDone.run();
                                });
                    });
            return;
        }

        if (user == null) {
            currentHouse = null;
            onDone.run();
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .collection("houses")
                .document(houseId)
                .get()
                .addOnSuccessListener(houseDoc -> {
                    currentHouse = houseDoc.toObject(House.class);
                    if (currentHouse != null) {
                        currentHouse.setId(houseDoc.getId());
                    }
                    onDone.run();
                })
                .addOnFailureListener(e -> {
                    currentHouse = null;
                    onDone.run();
                });
    }
}
