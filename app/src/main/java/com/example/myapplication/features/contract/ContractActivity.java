package com.example.myapplication.features.contract;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.example.myapplication.core.service.ImageUploadService;
import com.example.myapplication.R;
import com.example.myapplication.core.constants.InvoiceStatus;
import com.example.myapplication.core.constants.RoomStatus;
import com.example.myapplication.core.constants.WaterCalculationMode;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.MoneyFormatter;
import com.example.myapplication.core.util.ScreenUiHelper;
import com.example.myapplication.core.widget.MonthYearPickerDialog;
import com.example.myapplication.core.repository.domain.ContractMemberRepository;
import com.example.myapplication.domain.House;
import com.example.myapplication.domain.Tenant;
import com.example.myapplication.domain.Room;
import com.example.myapplication.domain.RentalHistory;
import com.example.myapplication.features.property.room.RoomActivity;
import com.example.myapplication.core.repository.domain.RentalHistoryRepository;
import com.google.firebase.Timestamp;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ContractActivity extends AppCompatActivity {

    public static final String EXTRA_ROOM_ID = "ROOM_ID";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private String roomId;
    private String editContractId;
    private boolean isEditMode = false;

    private Room currentRoom;
    private House currentHouse;

    private Tenant currentContract;

    private EditText etContractNumber, etCustomerName, etPhoneInput, etPersonalId;
    private ImageView ivFront, ivBack, ivEditFront, ivEditBack;
    private EditText etRoomNumber, etMemberCount, etRentAmountInput, etDepositAmount;
    private CheckBox cbShowDeposit;
    private EditText etContractDate, etContractMonths;
    private ImageView ivPickDate;
    private CheckBox cbRemind;
    private RadioGroup rgBillingReminder;
    private TextView tvElectricStartLabel, tvWaterStartLabel;
    private EditText etElectricStartReading;
    private EditText etWaterStartReading;
    private View layoutParkingService;
    private CheckBox cbParkingService, cbInternet, cbLaundryService;
    private TextView tvParkingPrice;
    private EditText etVehicleCount;
    private EditText etNote;
    private CheckBox cbShowNote;

    private MaterialButton btnSave, btnPrint, btnEnd, btnUpdate;

    private ActivityResultLauncher<String> imagePicker;
    private Uri selectedImageUri;

    private enum UploadTarget {
        FRONT, BACK
    }

    private UploadTarget pendingUploadTarget;

    private BroadcastReceiver uploadReceiver;

    private RentalHistoryRepository rentalHistoryRepo;
    private ContractMemberRepository contractMemberRepository;
    private TextView tvTitleLine1, tvTitleLine2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScreenUiHelper.enableEdgeToEdge(this, false);

        // Internal note.
        String mode = getIntent().getStringExtra(ContractIntentKeys.MODE);
        isEditMode = "EDIT".equals(mode);

        if (isEditMode) {
            // Internal note.
            editContractId = getIntent().getStringExtra(ContractIntentKeys.CONTRACT_ID);
            roomId = getIntent().getStringExtra(ContractIntentKeys.ROOM_ID);

            if (editContractId == null || editContractId.trim().isEmpty()) {
                Toast.makeText(this, getString(R.string.error_contract_not_found), Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        } else {
            // Internal note.
            roomId = getIntent().getStringExtra(EXTRA_ROOM_ID);
            if (roomId == null || roomId.trim().isEmpty()) {
                Toast.makeText(this, getString(R.string.missing_room_id), Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }

        imagePicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null)
                return;
            selectedImageUri = uri;
            startUploadSelectedImage();
        });

        uploadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String imageUrl = intent.getStringExtra(ImageUploadService.EXTRA_IMAGE_URL);
                if (imageUrl == null || pendingUploadTarget == null)
                    return;

                if (pendingUploadTarget == UploadTarget.FRONT) {
                    if (currentContract == null)
                        currentContract = new Tenant();
                    currentContract.setPersonalIdFrontUrl(imageUrl);
                    Glide.with(ContractActivity.this).load(imageUrl).centerCrop().into(ivFront);
                } else {
                    if (currentContract == null)
                        currentContract = new Tenant();
                    currentContract.setPersonalIdBackUrl(imageUrl);
                    Glide.with(ContractActivity.this).load(imageUrl).centerCrop().into(ivBack);
                }

                pendingUploadTarget = null;
                setUploadEnabled(true);
                Toast.makeText(ContractActivity.this, getString(R.string.image_upload_success), Toast.LENGTH_SHORT)
                        .show();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(uploadReceiver,
                new IntentFilter(ImageUploadService.ACTION_UPLOAD_COMPLETE));

        rentalHistoryRepo = new RentalHistoryRepository();
        contractMemberRepository = new ContractMemberRepository();

        setContentView(R.layout.activity_contract);

        Toolbar toolbar = findViewById(R.id.toolbar);
        tvTitleLine1 = findViewById(R.id.tvTitleLine1);
        tvTitleLine2 = findViewById(R.id.tvTitleLine2);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        View appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            ScreenUiHelper.applyTopInset(appBarLayout);
        }

        bindViews();
        wireUI();
        loadRoomThenContract();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(uploadReceiver);
        } catch (Exception ignored) {
        }
    }

    private void bindViews() {
        etContractNumber = findViewById(R.id.etSoHopDong);
        etCustomerName = findViewById(R.id.etTenKhach);
        etPhoneInput = findViewById(R.id.etDienThoai);
        etPersonalId = findViewById(R.id.etCccd);
        ivFront = findViewById(R.id.ivFront);
        ivBack = findViewById(R.id.ivBack);
        ivEditFront = findViewById(R.id.ivEditFront);
        ivEditBack = findViewById(R.id.ivEditBack);
        etRoomNumber = findViewById(R.id.etPhong);
        etMemberCount = findViewById(R.id.etSoNguoi);
        etRentAmountInput = findViewById(R.id.etTienPhong);
        etDepositAmount = findViewById(R.id.etTienCoc);
        cbShowDeposit = findViewById(R.id.cbShowDeposit);
        MoneyFormatter.applyTo(etRentAmountInput);
        MoneyFormatter.applyTo(etDepositAmount);
        etContractDate = findViewById(R.id.etNgayKy);
        etContractMonths = findViewById(R.id.etSoThang);
        ivPickDate = findViewById(R.id.ivPickDate);
        cbRemind = findViewById(R.id.cbRemind);
        rgBillingReminder = findViewById(R.id.rgBillingReminder);
        tvElectricStartLabel = findViewById(R.id.tvElectricStartLabel);
        tvWaterStartLabel = findViewById(R.id.tvWaterStartLabel);
        etElectricStartReading = findViewById(R.id.etChiSoDien);
        etWaterStartReading = findViewById(R.id.etChiSoNuoc);
        layoutParkingService = findViewById(R.id.layoutParkingService);
        cbParkingService = findViewById(R.id.cbGuiXe);
        tvParkingPrice = findViewById(R.id.tvGuiXePrice);
        etVehicleCount = findViewById(R.id.etSoXe);
        cbInternet = findViewById(R.id.cbInternet);
        cbLaundryService = findViewById(R.id.cbGiatSay);
        etNote = findViewById(R.id.etGhiChu);
        cbShowNote = findViewById(R.id.cbShowNote);
        btnSave = findViewById(R.id.btnSave);
        btnPrint = findViewById(R.id.btnPrint);
        btnEnd = findViewById(R.id.btnEnd);
        btnUpdate = findViewById(R.id.btnUpdate);
    }

    private void wireUI() {
        View.OnClickListener pickDate = v -> showDatePicker();
        etContractDate.setOnClickListener(pickDate);
        ivPickDate.setOnClickListener(pickDate);
        cbParkingService.setOnCheckedChangeListener((buttonView, isChecked) -> {
            etVehicleCount.setEnabled(isChecked);
            etVehicleCount
                    .setBackgroundResource(isChecked ? R.drawable.bg_input_rounded : R.drawable.bg_input_disabled);
            if (!isChecked)
                etVehicleCount.setText("");
        });
        ivEditFront.setOnClickListener(v -> {
            pendingUploadTarget = UploadTarget.FRONT;
            imagePicker.launch("image/*");
        });
        ivEditBack.setOnClickListener(v -> {
            pendingUploadTarget = UploadTarget.BACK;
            imagePicker.launch("image/*");
        });
        btnSave.setOnClickListener(v -> saveOrUpdate(true));
        btnUpdate.setOnClickListener(v -> saveOrUpdate(false));
        btnEnd.setOnClickListener(v -> confirmEndContract());
        btnPrint.setOnClickListener(v -> printContract());
    }

    private void setUploadEnabled(boolean enabled) {
        ivEditFront.setEnabled(enabled);
        ivEditBack.setEnabled(enabled);
        ivEditFront.setAlpha(enabled ? 1f : 0.5f);
        ivEditBack.setAlpha(enabled ? 1f : 0.5f);
    }

    private void startUploadSelectedImage() {
        if (selectedImageUri == null || pendingUploadTarget == null)
            return;
        setUploadEnabled(false);
        Intent i = new Intent(this, ImageUploadService.class);
        i.putExtra(ImageUploadService.EXTRA_IMAGE_URI, selectedImageUri.toString());
        startService(i);
    }

    private CollectionReference scopedCollection(@NonNull String collection) {
        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId != null && !tenantId.trim().isEmpty()) {
            return db.collection("tenants").document(tenantId).collection(collection);
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
            throw new IllegalStateException("User not logged in");
        return db.collection("users").document(user.getUid()).collection(collection);
    }

    private DocumentReference scopedDoc() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
            return null;
        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId != null && !tenantId.trim().isEmpty()) {
            return db.collection("tenants").document(tenantId);
        }
        return db.collection("users").document(user.getUid());
    }

    private void loadRoomThenContract() {
        scopedCollection("rooms").document(roomId).get()
                .addOnSuccessListener(doc -> {
                    currentRoom = doc != null && doc.exists() ? doc.toObject(Room.class) : null;
                    if (currentRoom == null) {
                        Toast.makeText(this, getString(R.string.room_not_found), Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    currentRoom.setId(doc.getId());
                    bindRoomToUI();
                    loadHouseFeesThenContract();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.room_load_error), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void loadHouseFeesThenContract() {
        String houseId = currentRoom.getHouseId();
        if (houseId == null || houseId.trim().isEmpty()) {
            currentHouse = null;
            bindFeesToUI(null);
            applyUtilityStartInputRules();
            loadExistingContract();
            return;
        }
        scopedCollection("houses").document(houseId).get()
                .addOnSuccessListener(doc -> {
                    currentHouse = doc != null && doc.exists() ? doc.toObject(House.class) : null;
                    if (currentHouse != null)
                        currentHouse.setId(doc.getId());
                    bindFeesToUI(currentHouse);
                    applyUtilityStartInputRules();
                    loadExistingContract();
                    updateFullToolbarHeader();
                })
                .addOnFailureListener(e -> {
                    bindFeesToUI(null);
                    applyUtilityStartInputRules();
                    loadExistingContract();
                    updateFullToolbarHeader();
                });
    }

    private void bindFeesToUI(House khu) {
        double parkingPrice = khu != null ? khu.getParkingPrice() : 0;
        double internetPrice = khu != null ? khu.getInternetPrice() : 0;
        double laundryPrice = khu != null ? khu.getLaundryPrice() : 0;

        boolean hasParkingFee = parkingPrice > 0;
        boolean hasInternetFee = internetPrice > 0;
        boolean hasLaundryFee = laundryPrice > 0;

        if (layoutParkingService != null) {
            layoutParkingService.setVisibility(hasParkingFee ? View.VISIBLE : View.GONE);
        }
        cbInternet.setVisibility(hasInternetFee ? View.VISIBLE : View.GONE);
        cbLaundryService.setVisibility(hasLaundryFee ? View.VISIBLE : View.GONE);

        if (!hasParkingFee) {
            cbParkingService.setChecked(false);
            etVehicleCount.setText("");
        }
        if (!hasInternetFee) {
            cbInternet.setChecked(false);
        }
        if (!hasLaundryFee) {
            cbLaundryService.setChecked(false);
        }

        tvParkingPrice.setText(getString(R.string.parking_service_price_format, formatVnd(parkingPrice)));
        cbInternet.setText(getString(R.string.internet_service_price_format, formatVnd(internetPrice)));
        cbLaundryService.setText(getString(R.string.laundry_service_price_format, formatVnd(laundryPrice)));
    }

    private void bindRoomToUI() {
        etRoomNumber.setText(currentRoom.getRoomNumber() != null ? currentRoom.getRoomNumber() : "");
        MoneyFormatter.setValue(etRentAmountInput, currentRoom.getRentAmount());
        etContractNumber.setText(generateContractNo());
        etContractDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
        MoneyFormatter.setValue(etDepositAmount, currentRoom.getRentAmount());
        applyUtilityStartInputRules();
        updateFullToolbarHeader();
    }

    private void loadExistingContract() {
        // Internal note.
        if (isEditMode && editContractId != null) {
            loadContractById(editContractId);
            return;
        }

        // Internal note.
        scopedCollection("contracts")
                .whereEqualTo("roomId", roomId)
                .whereEqualTo("contractStatus", "ACTIVE")
                .limit(1).get()
                .addOnSuccessListener(qs -> {
                    if (qs != null && !qs.isEmpty()) {
                        com.google.firebase.firestore.DocumentSnapshot d = qs.getDocuments().get(0);
                        Tenant n = d.toObject(Tenant.class);
                        if (n != null) {
                            n.setId(d.getId());
                            currentContract = n;
                            applyModeView();
                            return;
                        }
                    }
                    loadExistingContractLegacyFallback();
                })
                .addOnFailureListener(e -> loadExistingContractLegacyFallback());
    }

    /**
     * Internal note.
     */
    private void loadContractById(String contractId) {
        scopedCollection("contracts").document(contractId).get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        Tenant n = doc.toObject(Tenant.class);
                        if (n != null) {
                            n.setId(doc.getId());
                            currentContract = n;

                            // Internal note.
                            fillDataFromIntent();

                            // Apply mode EDIT
                            applyModeEdit();
                            return;
                        }
                    }
                    Toast.makeText(this, getString(R.string.contract_not_found), Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.contract_load_error) + e.getMessage(), Toast.LENGTH_SHORT)
                            .show();
                    finish();
                });
    }

    /**
     * Internal note.
     */
    private void fillDataFromIntent() {
        Intent intent = getIntent();

        if (currentContract == null) {
            currentContract = new Tenant();
        }

        // Internal note.
        if (intent.hasExtra(ContractIntentKeys.CONTRACT_NUMBER))
            currentContract.setContractNumber(intent.getStringExtra(ContractIntentKeys.CONTRACT_NUMBER));
        if (intent.hasExtra(ContractIntentKeys.FULL_NAME))
            currentContract.setFullName(intent.getStringExtra(ContractIntentKeys.FULL_NAME));
        if (intent.hasExtra(ContractIntentKeys.PHONE_NUMBER))
            currentContract.setPhoneNumber(intent.getStringExtra(ContractIntentKeys.PHONE_NUMBER));
        if (intent.hasExtra(ContractIntentKeys.PERSONAL_ID))
            currentContract.setPersonalId(intent.getStringExtra(ContractIntentKeys.PERSONAL_ID));
        if (intent.hasExtra(ContractIntentKeys.MEMBER_COUNT))
            currentContract.setMemberCount(intent.getIntExtra(ContractIntentKeys.MEMBER_COUNT, 0));
        if (intent.hasExtra(ContractIntentKeys.RENTAL_START_DATE))
            currentContract.setRentalStartDate(intent.getStringExtra(ContractIntentKeys.RENTAL_START_DATE));
        if (intent.hasExtra(ContractIntentKeys.CONTRACT_DURATION_MONTHS))
            currentContract
                    .setContractDurationMonths(intent.getIntExtra(ContractIntentKeys.CONTRACT_DURATION_MONTHS, 0));
        if (intent.hasExtra(ContractIntentKeys.RENT_AMOUNT))
            currentContract.setRentAmount(intent.getLongExtra(ContractIntentKeys.RENT_AMOUNT, 0));
        if (intent.hasExtra(ContractIntentKeys.DEPOSIT_AMOUNT))
            currentContract.setDepositAmount(intent.getLongExtra(ContractIntentKeys.DEPOSIT_AMOUNT, 0));
        if (intent.hasExtra(ContractIntentKeys.ELECTRIC_START_READING))
            currentContract
                    .setElectricStartReading(intent.getIntExtra(ContractIntentKeys.ELECTRIC_START_READING, 0));
        if (intent.hasExtra(ContractIntentKeys.WATER_START_READING))
            currentContract
                    .setWaterStartReading(intent.getIntExtra(ContractIntentKeys.WATER_START_READING, 0));
        if (intent.hasExtra(ContractIntentKeys.HAS_PARKING_SERVICE))
            currentContract.setHasParkingService(intent.getBooleanExtra(ContractIntentKeys.HAS_PARKING_SERVICE, false));
        if (intent.hasExtra(ContractIntentKeys.VEHICLE_COUNT))
            currentContract.setVehicleCount(intent.getIntExtra(ContractIntentKeys.VEHICLE_COUNT, 0));
        if (intent.hasExtra(ContractIntentKeys.HAS_INTERNET_SERVICE))
            currentContract
                    .setHasInternetService(intent.getBooleanExtra(ContractIntentKeys.HAS_INTERNET_SERVICE, false));
        if (intent.hasExtra(ContractIntentKeys.HAS_LAUNDRY_SERVICE))
            currentContract
                    .setHasLaundryService(intent.getBooleanExtra(ContractIntentKeys.HAS_LAUNDRY_SERVICE, false));
        if (intent.hasExtra(ContractIntentKeys.NOTE))
            currentContract.setNote(intent.getStringExtra(ContractIntentKeys.NOTE));
        if (intent.hasExtra(ContractIntentKeys.SHOW_DEPOSIT_ON_INVOICE))
            currentContract.setShowDepositOnInvoice(
                    intent.getBooleanExtra(ContractIntentKeys.SHOW_DEPOSIT_ON_INVOICE, false));
        if (intent.hasExtra(ContractIntentKeys.SHOW_NOTE_ON_INVOICE))
            currentContract.setShowNoteOnInvoice(
                    intent.getBooleanExtra(ContractIntentKeys.SHOW_NOTE_ON_INVOICE, false));
        if (intent.hasExtra(ContractIntentKeys.REMIND_ONE_MONTH_BEFORE))
            currentContract.setRemindOneMonthBefore(
                    intent.getBooleanExtra(ContractIntentKeys.REMIND_ONE_MONTH_BEFORE, false));
        if (intent.hasExtra(ContractIntentKeys.BILLING_REMINDER_AT))
            currentContract.setBillingReminderAt(intent.getStringExtra(ContractIntentKeys.BILLING_REMINDER_AT));
        if (intent.hasExtra(ContractIntentKeys.PERSONAL_ID_FRONT_URL))
            currentContract.setPersonalIdFrontUrl(intent.getStringExtra(ContractIntentKeys.PERSONAL_ID_FRONT_URL));
        if (intent.hasExtra(ContractIntentKeys.PERSONAL_ID_BACK_URL))
            currentContract.setPersonalIdBackUrl(intent.getStringExtra(ContractIntentKeys.PERSONAL_ID_BACK_URL));
    }

    private void loadExistingContractLegacyFallback() {
        scopedCollection("contracts").whereEqualTo("roomId", roomId).limit(1).get()
                .addOnSuccessListener(qs -> {
                    Tenant best = null;
                    if (qs != null && !qs.isEmpty()) {
                        com.google.firebase.firestore.DocumentSnapshot d = qs.getDocuments().get(0);
                        Tenant n = d.toObject(Tenant.class);
                        if (n != null) {
                            String st = n.getContractStatus();
                            if (st == null || !st.equalsIgnoreCase("ENDED")) {
                                n.setId(d.getId());
                                best = n;
                            }
                        }
                    }
                    if (best != null) {
                        currentContract = best;
                        applyModeView();
                    } else {
                        currentContract = new Tenant();
                        applyModeCreate();
                    }
                })
                .addOnFailureListener(e -> {
                    currentContract = new Tenant();
                    applyModeCreate();
                });
    }

    private void applyModeCreate() {
        btnSave.setVisibility(View.VISIBLE);
        btnPrint.setVisibility(View.GONE);
        btnEnd.setVisibility(View.GONE);
        btnUpdate.setVisibility(View.GONE);
        setUploadEnabled(true);
    }

    private void applyModeView() {
        btnSave.setVisibility(View.GONE);
        btnPrint.setVisibility(View.VISIBLE);
        btnEnd.setVisibility(View.VISIBLE);
        btnUpdate.setVisibility(View.VISIBLE);
        bindContractToUI(currentContract);
        applyUtilityStartInputRules();
        setUploadEnabled(true);
    }

    /**
     * Internal note.
     */
    private void applyModeEdit() {
        btnSave.setVisibility(View.GONE);
        btnPrint.setVisibility(View.GONE);
        btnEnd.setVisibility(View.GONE);
        btnUpdate.setVisibility(View.VISIBLE);

        // Internal note.
        btnUpdate.setText(R.string.update);

        // Internal note.
        if (tvTitleLine1 != null) {
            tvTitleLine1.setText(R.string.edit_contract);
        }

        // Internal note.
        bindContractToUI(currentContract);
        applyUtilityStartInputRules();
        setUploadEnabled(true);
    }

    private void updateFullToolbarHeader() {
        String title = getString(R.string.contract_header_title);
        if (currentRoom != null)
            title = getString(R.string.contract_header_room_title, nullToEmpty(currentRoom.getRoomNumber()));
        String subtitle = "";
        if (currentHouse != null) {
            subtitle = (currentHouse.getAddress() != null && !currentHouse.getAddress().trim().isEmpty())
                    ? currentHouse.getAddress()
                    : currentHouse.getHouseName();
        } else if (currentRoom != null)
            subtitle = currentRoom.getHouseName();
        if (tvTitleLine1 != null)
            tvTitleLine1.setText(nullToEmpty(title));
        if (tvTitleLine2 != null)
            tvTitleLine2.setText(nullToEmpty(subtitle));
    }

    private void bindContractToUI(@NonNull Tenant c) {
        if (c.getContractNumber() != null && !c.getContractNumber().trim().isEmpty())
            etContractNumber.setText(c.getContractNumber());
        etCustomerName.setText(nullToEmpty(c.getFullName()));
        etPhoneInput.setText(nullToEmpty(c.getPhoneNumber()));
        etPersonalId.setText(nullToEmpty(c.getPersonalId()));
        etMemberCount.setText(c.getMemberCount() > 0 ? String.valueOf(c.getMemberCount()) : "");

        if (c.getRentAmount() > 0) {
            MoneyFormatter.setValue(etRentAmountInput, (double) c.getRentAmount());
        }

        if (c.getDepositAmount() > 0) {
            MoneyFormatter.setValue(etDepositAmount, (double) c.getDepositAmount());
        }

        cbShowDeposit.setChecked(c.isShowDepositOnInvoice());
        etContractDate.setText(ContractDateHelper.formatMonthYearForInput(c.getRentalStartDate()));
        etContractMonths
                .setText(c.getContractDurationMonths() > 0 ? String.valueOf(c.getContractDurationMonths()) : "");
        cbRemind.setChecked(c.isRemindOneMonthBefore());
        bindBillingReminderSelection(c.getBillingReminderAt());
        etElectricStartReading
                .setText(c.getElectricStartReading() > 0 ? String.valueOf(c.getElectricStartReading()) : "");
        etWaterStartReading
            .setText(c.getWaterStartReading() > 0 ? String.valueOf(c.getWaterStartReading()) : "");
        boolean parkingVisible = layoutParkingService != null && layoutParkingService.getVisibility() == View.VISIBLE;
        cbParkingService.setChecked(parkingVisible && c.hasParkingService());
        etVehicleCount.setEnabled(cbParkingService.isChecked());
        etVehicleCount.setBackgroundResource(
            cbParkingService.isChecked() ? R.drawable.bg_input_rounded : R.drawable.bg_input_disabled);
        etVehicleCount.setText(parkingVisible && c.getVehicleCount() > 0 ? String.valueOf(c.getVehicleCount()) : "");
        cbInternet.setChecked(cbInternet.getVisibility() == View.VISIBLE && c.hasInternetService());
        cbLaundryService.setChecked(cbLaundryService.getVisibility() == View.VISIBLE && c.hasLaundryService());
        etNote.setText(nullToEmpty(c.getNote()));
        cbShowNote.setChecked(c.isShowNoteOnInvoice());
        if (c.getPersonalIdFrontUrl() != null && !c.getPersonalIdFrontUrl().trim().isEmpty())
            Glide.with(this).load(c.getPersonalIdFrontUrl()).centerCrop().into(ivFront);
        if (c.getPersonalIdBackUrl() != null && !c.getPersonalIdBackUrl().trim().isEmpty())
            Glide.with(this).load(c.getPersonalIdBackUrl()).centerCrop().into(ivBack);
    }

    private void showDatePicker() {
        final Calendar cal = Calendar.getInstance();
        String cur = etContractDate.getText() != null ? etContractDate.getText().toString().trim() : "";
        Calendar parsed = ContractDateHelper.parseContractDate(cur);
        if (parsed != null)
            cal.setTimeInMillis(parsed.getTimeInMillis());

        MonthYearPickerDialog.show(
                this,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                MonthYearPickerDialog.defaultMinYear(),
                MonthYearPickerDialog.defaultMaxYear(),
                (year, month) -> etContractDate
                        .setText(String.format(Locale.getDefault(), "%02d/%04d", month + 1, year)));
    }

    private void saveOrUpdate(boolean isCreate) {
        if (currentContract == null)
            currentContract = new Tenant();
        ContractFormDataHelper.FormData formData;
        try {
            boolean electricMeterMode = isElectricMeterMode();
            boolean waterMeterMode = isWaterMeterMode();
            formData = ContractFormDataHelper.parseAndValidate(
                    text(etContractNumber),
                    text(etCustomerName),
                    text(etPhoneInput),
                    text(etPersonalId),
                    text(etMemberCount),
                    text(etContractDate),
                    text(etContractMonths),
                    text(etElectricStartReading),
                    text(etWaterStartReading),
                    electricMeterMode,
                    waterMeterMode,
                    cbParkingService.isChecked(),
                    text(etVehicleCount),
                    cbInternet.isChecked(),
                    cbLaundryService.isChecked(),
                    cbRemind.isChecked(),
                    cbShowDeposit.isChecked(),
                    cbShowNote.isChecked(),
                    getSelectedBillingReminder(),
                    MoneyFormatter.getValue(etRentAmountInput),
                    MoneyFormatter.getValue(etDepositAmount),
                    text(etNote),
                    getString(R.string.fill_required_fields),
                    getString(R.string.contract_signing_date_invalid),
                    getString(R.string.enter_vehicle_count),
                    getString(R.string.invalid_data),
                    this::normalizeMonthYearToStorage);
        } catch (ContractFormDataHelper.ValidationException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        int maxOccupancy = currentRoom != null ? currentRoom.getMaxOccupancy() : 0;
        if (maxOccupancy > 0 && formData.memberCount > maxOccupancy) {
            Toast.makeText(this,
                    getString(R.string.contract_member_count_exceeds_room_limit, formData.memberCount, maxOccupancy),
                    Toast.LENGTH_LONG).show();
            return;
        }

        ContractFormDataHelper.applyToContract(
                currentContract,
                formData,
                roomId,
                currentRoom != null ? currentRoom.getRoomNumber() : null,
                this::computeEndDate);

        long now = System.currentTimeMillis();
        if (currentContract.getCreatedAt() == null)
            currentContract.setCreatedAt(now);
        currentContract.setUpdatedAt(now);
        if (isCreate || currentContract.getId() == null || currentContract.getId().trim().isEmpty()) {
            // Internal note.
            scopedCollection("contracts").add(currentContract)
                    .addOnSuccessListener(ref -> {
                        currentContract.setId(ref.getId());
                        contractMemberRepository.upsertPrimaryMemberFromContract(
                                currentContract,
                                () -> {
                                    markRoomStatus(RoomStatus.RENTED);
                                    Toast.makeText(this, getString(R.string.contract_saved), Toast.LENGTH_SHORT).show();
                                    navigateToRoomList(RoomStatus.RENTED);
                                },
                                () -> {
                                    markRoomStatus(RoomStatus.RENTED);
                                    Toast.makeText(this,
                                            getString(R.string.contract_saved_with_member_sync_warning),
                                            Toast.LENGTH_SHORT).show();
                                    navigateToRoomList(RoomStatus.RENTED);
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, getString(R.string.save_failed) + ": " + e.getMessage(), Toast.LENGTH_LONG)
                                .show();
                    });
        } else {
            // Internal note.
            String updateId = isEditMode && editContractId != null ? editContractId : currentContract.getId();

            scopedCollection("contracts").document(updateId).set(currentContract)
                    .addOnSuccessListener(v -> {
                        contractMemberRepository.upsertPrimaryMemberFromContract(
                                currentContract,
                                () -> {
                                    markRoomStatus(RoomStatus.RENTED);
                                    Toast.makeText(this, getString(R.string.updated), Toast.LENGTH_SHORT).show();

                                    // Internal note.
                                    if (isEditMode) {
                                        finish();
                                    }
                                },
                                () -> {
                                    markRoomStatus(RoomStatus.RENTED);
                                    Toast.makeText(this, getString(R.string.updated), Toast.LENGTH_SHORT).show();

                                    // Internal note.
                                    if (isEditMode) {
                                        finish();
                                    }
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, getString(R.string.update_failed) + ": " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        }
    }

    private void bindBillingReminderSelection(String value) {
        if (rgBillingReminder == null)
            return;
        String remind = normalizeBillingReminder(value);
        if ("end_month".equals(remind)) {
            rgBillingReminder.check(R.id.rbRemindEndMonth);
        } else if ("mid_month".equals(remind)) {
            rgBillingReminder.check(R.id.rbRemindMidMonth);
        } else {
            rgBillingReminder.check(R.id.rbRemindStartMonth);
        }
    }

    private String getSelectedBillingReminder() {
        if (rgBillingReminder == null)
            return "start_month";
        int checkedId = rgBillingReminder.getCheckedRadioButtonId();
        if (checkedId == R.id.rbRemindEndMonth)
            return "end_month";
        if (checkedId == R.id.rbRemindMidMonth)
            return "mid_month";
        return "start_month";
    }

    private String normalizeBillingReminder(String value) {
        if (value == null || value.trim().isEmpty()) {
            if (currentHouse != null && currentHouse.getBillingReminderAt() != null
                    && !currentHouse.getBillingReminderAt().trim().isEmpty()) {
                String legacy = currentHouse.getBillingReminderAt();
                if ("end_month".equals(legacy))
                    return "end_month";
                return "start_month";
            }
            return "start_month";
        }
        if ("end_month".equals(value) || "mid_month".equals(value) || "start_month".equals(value))
            return value;
        return "start_month";
    }

    private void confirmEndContract() {
        if (currentContract == null || currentContract.getId() == null)
            return;
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.contract_end_confirm_title))
                .setMessage(getString(R.string.contract_end_confirm_message))
                .setPositiveButton(getString(R.string.contract_end_action), (d, w) -> endContract())
                .setNegativeButton(getString(R.string.cancel), null).setCancelable(false).show();
    }

    private void endContract() {
        if (currentContract == null || currentContract.getId() == null)
            return;
        String oldRoomId = roomId;
        long now = System.currentTimeMillis();
        createRentalHistoryLog(currentContract, oldRoomId, now);
        currentContract.setContractStatus("ENDED");
        currentContract.setEndedAt(now);
        currentContract.setPreviousRoomId(oldRoomId);
        currentContract.setRoomId("");
        currentContract.setUpdatedAt(now);
        scopedCollection("contracts").document(currentContract.getId()).set(currentContract)
                .addOnSuccessListener(v -> {
                    contractMemberRepository.deactivateMembersByContract(currentContract.getId(), () -> {
                        markRoomStatus(RoomStatus.VACANT);
                        Toast.makeText(this, getString(R.string.contract_end_success), Toast.LENGTH_SHORT).show();
                        navigateToRoomList(RoomStatus.VACANT);
                    });
                })
                .addOnFailureListener(
                        e -> Toast.makeText(this, getString(R.string.operation_failed), Toast.LENGTH_SHORT).show());
    }

    private void navigateToRoomList(@NonNull String status) {
        Intent intent = new Intent(this, RoomActivity.class);
        intent.putExtra("FILTER_STATUS", status);
        if (currentRoom != null && currentRoom.getHouseId() != null
                && !currentRoom.getHouseId().trim().isEmpty()) {
            intent.putExtra("HOUSE_ID", currentRoom.getHouseId());
            intent.putExtra("HOUSE_NAME", currentRoom.getHouseName());
            if (currentHouse != null)
                intent.putExtra("HOUSE_ADDRESS", currentHouse.getAddress());
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void createRentalHistoryLog(Tenant contract, String roomId, long endTime) {
        RentalHistory history = new RentalHistory();
        history.setContractId(contract.getId());
        history.setRoomId(roomId);
        history.setContractId(contract.getId());
        if (currentRoom != null) {
            history.setRoomNumber(currentRoom.getRoomNumber());
            history.setHouseName(currentRoom.getHouseName());
            history.setFloor(currentRoom.getFloor());
        } else
            history.setRoomNumber(contract.getRoomNumber());
        history.setFullName(contract.getFullName());
        history.setPersonalId(contract.getPersonalId());
        history.setPhoneNumber(contract.getPhoneNumber());
        history.setAddress(contract.getAddress());
        history.setContractNumber(contract.getContractNumber());
        history.setMemberCount(contract.getMemberCount());
        history.setRentalStartDate(contract.getRentalStartDate());
        history.setContractEndDate(contract.getContractEndDate());
        history.setContractMonths(contract.getContractDurationMonths());
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        history.setActualEndDate(sdf.format(new Date(endTime)));
        if (contract.getRentalStartDate() != null && !contract.getRentalStartDate().isEmpty()) {
            try {
                Date startDate = sdf.parse(contract.getRentalStartDate());
                if (startDate != null) {
                    long diff = endTime - startDate.getTime();
                    history.setActualRentalDays((int) (diff / (1000 * 60 * 60 * 24)));
                    history.setStartTimestamp(startDate.getTime());
                }
            } catch (Exception e) {
                history.setActualRentalDays(0);
            }
        }
        history.setEndTimestamp(endTime);
        history.setRoomPrice(contract.getRentAmount());
        history.setDepositAmount(contract.getDepositAmount());
        history.setHasParkingService(contract.hasParkingService());
        history.setHasInternetService(contract.hasInternetService());
        history.setHasLaundryService(contract.hasLaundryService());
        history.setVehicleCount(contract.getVehicleCount());
        history.setNote(contract.getNote());
        history.setEndReason(getString(R.string.contract_end_reason_default));
        history.setCreatedAt(Timestamp.now());
        calculateInvoiceStats(contract.getId(), (totalPaid, paidCount, unpaidCount) -> {
            history.setTotalPaidAmount(totalPaid);
            history.setPaidInvoiceCount(paidCount);
            history.setUnpaidInvoiceCount(unpaidCount);
            rentalHistoryRepo.addHistory(history)
                    .addOnSuccessListener(ref -> {
                        history.setId(ref.getId());
                        android.util.Log.d("ContractActivity", "Rental history saved: " + ref.getId());
                    })
                    .addOnFailureListener(
                            e -> android.util.Log.e("ContractActivity",
                                    "Failed to save rental history: " + e.getMessage()));
        });
    }

    private interface InvoiceStatsCallback {
        void onDone(double totalPaid, int paidCount, int unpaidCount);
    }

    private void calculateInvoiceStats(String contractId, @NonNull InvoiceStatsCallback callback) {
        scopedCollection("invoices").whereEqualTo("contractId", contractId).get()
                .addOnSuccessListener(querySnapshot -> {
                    double totalPaid = 0;
                    int paidCount = 0, unpaidCount = 0;
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String status = doc.getString("status");
                        if (InvoiceStatus.PAID.equals(status)
                                || getString(R.string.legacy_paid_status_vi).equals(status)) {
                            Double totalAmount = doc.getDouble("totalAmount");
                            if (totalAmount != null)
                                totalPaid += totalAmount;
                            paidCount++;
                        } else
                            unpaidCount++;
                    }
                    callback.onDone(totalPaid, paidCount, unpaidCount);
                })
                .addOnFailureListener(e -> callback.onDone(0, 0, 0));
    }

    private void markRoomStatus(@NonNull String status) {
        DocumentReference scope = scopedDoc();
        if (scope == null)
            return;
        scope.collection("rooms").document(roomId).update("status", status);
    }

    private void printContract() {
        if (currentContract == null) {
            Toast.makeText(this, getString(R.string.no_data_yet), Toast.LENGTH_SHORT).show();
            return;
        }
        String html = ContractHtmlBuilder.buildContractHtml(this, currentContract, currentRoom, currentHouse);
        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        if (printManager == null) {
            Toast.makeText(this, getString(R.string.device_not_support_print), Toast.LENGTH_SHORT).show();
            return;
        }
        String jobName = "Contract_"
                + (currentContract.getContractNumber() != null ? currentContract.getContractNumber() : "");
        WebView webView = new WebView(this);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                printManager.print(jobName, view.createPrintDocumentAdapter(jobName),
                        new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build());
            }
        });
        webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null);
    }

    private String computeEndDate(String start, int months) {
        return ContractDateHelper.computeEndDate(start, months);
    }

    private String normalizeMonthYearToStorage(String input) {
        return ContractDateHelper.normalizeMonthYearToStorage(input);
    }

    private String generateContractNo() {
        return new SimpleDateFormat("ddMMyyyyHHmmss", Locale.getDefault()).format(new Date());
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private String text(@NonNull EditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private String formatVnd(double value) {
        return NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format((long) value) + " ₫";
    }

    private void applyUtilityStartInputRules() {
        applyMeterFieldMode(
                tvElectricStartLabel,
                etElectricStartReading,
                isElectricMeterMode(),
                getString(R.string.contract_form_initial_electric_meter));
        applyMeterFieldMode(
                tvWaterStartLabel,
                etWaterStartReading,
                isWaterMeterMode(),
                getString(R.string.contract_form_initial_water_meter));
    }

    private void applyMeterFieldMode(TextView label, EditText field, boolean meterMode, String meterLabel) {
        if (field == null || label == null)
            return;

        label.setVisibility(meterMode ? View.VISIBLE : View.GONE);
        field.setVisibility(meterMode ? View.VISIBLE : View.GONE);

        if (meterMode) {
            label.setText(meterLabel);
            field.setEnabled(true);
            field.setBackgroundResource(R.drawable.bg_input_rounded);
        }

        if (!meterMode) {
            field.setText("");
        }
    }

    private boolean isElectricMeterMode() {
        if (currentHouse == null)
            return true;
        String mode = currentHouse.getElectricityCalculationMethod();
        return mode == null || mode.trim().isEmpty() || "kwh".equalsIgnoreCase(mode);
    }

    private boolean isWaterMeterMode() {
        if (currentHouse == null)
            return true;
        return WaterCalculationMode.isMeter(currentHouse.getWaterCalculationMethod());
    }
}
