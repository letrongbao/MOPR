package com.example.myapplication.features.property.room;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.core.constants.RoomStatus;
import com.example.myapplication.core.service.ImageUploadService;
import com.example.myapplication.core.session.InviteRepository;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.MoneyFormatter;
import com.example.myapplication.core.util.ScreenUiHelper;
import com.example.myapplication.domain.ContractMember;
import com.example.myapplication.domain.Tenant;
import com.example.myapplication.domain.Room;
import com.example.myapplication.features.contract.ContractActivity;
import com.google.android.material.appbar.AppBarLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.text.NumberFormat;
import java.util.Locale;

public class RoomDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_DELETED_ROOM_NUMBER = "EXTRA_DELETED_ROOM_NUMBER";

    private ImageView imgRoom;
    private Toolbar toolbar;
    private TextView tvHeaderTitle, tvHeaderSubtitle;
    private TextView tvRoomNumber, tvRoomType, tvArea, tvRentAmount, tvStatus, tvStatusRow;
    private TextView tvMaxOccupancy, tvCurrentOccupancy;
    private TextView tvTenantName, tvTenantPhone;
    private TextView tvTenantRepresentativeLine, tvManagerRow;
    private View cardTenant;
    private View cardTenantProfiles;
    private TextView tvTenantProfileEmpty;
    private RecyclerView rvTenantProfiles;
    private View rowMaxOccupancy;
    private View rowCurrentOccupancy;
    private View btnCall, btnMessage, llActionButtons;
    private Button btnGenerateCode;
    private Room currentRoom;
    private String activeContractId;

    private String tenantPhoneNumber;
    private String representativePersonalId;
    private String currentRoomNumber;
    private String currentRepresentativeName;
    private String currentManagerName;
    private String currentHouseDisplayName;
    private Uri selectedImageUri;
    private ImageView dialogImgPreview;
    private AlertDialog pendingEditDialog;
    private Map<String, Object> pendingRoomUpdates;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private BroadcastReceiver uploadReceiver;

    private ListenerRegistration roomListener;
    private ListenerRegistration tenantListener;
    private ListenerRegistration memberListener;

    private String roomId;
    private int declaredMemberCount;
    private int currentMemberCount;
    private boolean isVacantRoom = true;
    private TenantProfileQuickAdapter tenantProfileAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        if (dialogImgPreview != null) {
                            Glide.with(this).load(uri).centerCrop().into(dialogImgPreview);
                        }
                    }
                });

        uploadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String imageUrl = intent.getStringExtra(ImageUploadService.EXTRA_IMAGE_URL);
                if (imageUrl == null || pendingRoomUpdates == null || roomId == null || roomId.trim().isEmpty()) {
                    return;
                }
                pendingRoomUpdates.put("imageUrl", imageUrl);
                commitRoomUpdates(pendingRoomUpdates, pendingEditDialog);
            }
        };
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(uploadReceiver, new IntentFilter(ImageUploadService.ACTION_UPLOAD_COMPLETE));

        ScreenUiHelper.enableEdgeToEdge(this, false);

        setContentView(R.layout.activity_room_details);

        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            ScreenUiHelper.applyTopInset(appBarLayout);
        }

        toolbar = findViewById(R.id.toolbar);
        ScreenUiHelper.setupBackToolbar(this, toolbar, getString(R.string.room_details_title));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setOverflowIcon(AppCompatResources.getDrawable(this, R.drawable.ic_more_vert_white));
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvHeaderSubtitle = findViewById(R.id.tvHeaderSubtitle);

        imgRoom = findViewById(R.id.imgPhongChiTiet);
        tvRoomNumber = findViewById(R.id.tvSoPhongChiTiet);
        tvTenantRepresentativeLine = findViewById(R.id.tvTenantRepresentativeLine);
        tvStatus = findViewById(R.id.tvTrangThaiChiTiet);

        // Detail rows
        View rowLoai = findViewById(R.id.rowLoaiPhong);
        ((TextView) rowLoai.findViewById(R.id.tvLabel)).setText(R.string.room_type_label);
        tvRoomType = rowLoai.findViewById(R.id.tvValue);

        View rowDienTich = findViewById(R.id.rowDienTich);
        ((TextView) rowDienTich.findViewById(R.id.tvLabel)).setText(R.string.room_area_label);
        tvArea = rowDienTich.findViewById(R.id.tvValue);

        View rowGia = findViewById(R.id.rowGiaThue);
        ((TextView) rowGia.findViewById(R.id.tvLabel)).setText(R.string.room_rent_label);
        tvRentAmount = rowGia.findViewById(R.id.tvValue);
        tvRentAmount.setTextColor(getResources().getColor(R.color.primary));

        View rowMax = findViewById(R.id.rowSucChuaToiDa);
        rowMaxOccupancy = rowMax;
        ((TextView) rowMax.findViewById(R.id.tvLabel)).setText(R.string.room_max_occupancy_label);
        tvMaxOccupancy = rowMax.findViewById(R.id.tvValue);

        View rowCurrent = findViewById(R.id.rowSoNguoiDangO);
        rowCurrentOccupancy = rowCurrent;
        ((TextView) rowCurrent.findViewById(R.id.tvLabel)).setText(R.string.room_current_occupancy_label);
        tvCurrentOccupancy = rowCurrent.findViewById(R.id.tvValue);
        tvCurrentOccupancy.setText(getString(R.string.room_current_occupancy_unknown, 0));

        View rowTrangThai = findViewById(R.id.rowTrangThai);
        ((TextView) rowTrangThai.findViewById(R.id.tvLabel)).setText(R.string.room_status_label);
        tvStatusRow = rowTrangThai.findViewById(R.id.tvValue);

        View rowManager = findViewById(R.id.rowQuanLy);
        ((TextView) rowManager.findViewById(R.id.tvLabel)).setText(R.string.room_manager_label);
        tvManagerRow = rowManager.findViewById(R.id.tvValue);
        currentManagerName = resolveFallbackManagerName();
        tvManagerRow.setText(currentManagerName);

        tvTenantName = findViewById(R.id.tvTenTenant);
        tvTenantPhone = findViewById(R.id.tvSdtTenant);
        cardTenant = findViewById(R.id.cardTenant);
        cardTenantProfiles = findViewById(R.id.cardTenantProfiles);
        tvTenantProfileEmpty = findViewById(R.id.tvTenantProfileEmpty);
        rvTenantProfiles = findViewById(R.id.rvTenantProfiles);

        tenantProfileAdapter = new TenantProfileQuickAdapter();
        if (rvTenantProfiles != null) {
            rvTenantProfiles.setLayoutManager(new LinearLayoutManager(this));
            rvTenantProfiles.setAdapter(tenantProfileAdapter);
        }

        btnCall = findViewById(R.id.btnGoiDien);
        btnMessage = findViewById(R.id.btnNhanTin);
        llActionButtons = findViewById(R.id.llActionButtons);
        View btnManageContract = findViewById(R.id.btnQuanLyHopDong);
        btnGenerateCode = findViewById(R.id.btnGenerateCode);

        roomId = getIntent().getStringExtra("ROOM_ID");
        if (roomId == null) {
            Toast.makeText(this, R.string.room_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (btnManageContract != null) {
            btnManageContract.setOnClickListener(v -> openContractScreen());
        }

        btnCall.setOnClickListener(v -> {
            if (tenantPhoneNumber == null || tenantPhoneNumber.isEmpty()) {
                Toast.makeText(this, R.string.room_no_tenant, Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + tenantPhoneNumber));
            startActivity(intent);
        });

        btnMessage.setOnClickListener(v -> {
            if (tenantPhoneNumber == null || tenantPhoneNumber.isEmpty()) {
                Toast.makeText(this, R.string.room_no_tenant, Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("smsto:" + tenantPhoneNumber));
            intent.putExtra("sms_body", getString(R.string.room_contact_sms, currentRoomNumber));
            startActivity(intent);
        });

        btnGenerateCode.setOnClickListener(v -> generateAnonymousCode());

        loadRoomData(roomId);
        loadTenantData(roomId);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_room_details, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean allow = canManageRoomActions();
        MenuItem edit = menu.findItem(R.id.action_room_edit);
        MenuItem delete = menu.findItem(R.id.action_room_delete);
        if (edit != null) {
            edit.setVisible(allow);
        }
        if (delete != null) {
            delete.setVisible(allow);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_room_edit) {
            openEditRoomScreen();
            return true;
        }
        if (itemId == R.id.action_room_delete) {
            confirmDeleteRoom();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void generateAnonymousCode() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.not_logged_in, Toast.LENGTH_SHORT).show();
            return;
        }

        if (activeContractId == null || activeContractId.trim().isEmpty()) {
            Toast.makeText(this, getString(R.string.room_invite_requires_active_contract), Toast.LENGTH_LONG)
                    .show();
            return;
        }

        if (isInviteQuotaFull()) {
            Toast.makeText(this, getString(R.string.room_invite_quota_full), Toast.LENGTH_LONG).show();
            return;
        }

        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId == null || tenantId.isEmpty()) {
            tenantId = user.getUid();
        }

        btnGenerateCode.setEnabled(false);
        btnGenerateCode.setText(getString(R.string.room_invite_generating));

        new InviteRepository().createAnonymousTenantInvite(tenantId, roomId, new InviteRepository.InviteCallback() {
            @Override
            public void onSuccess(@NonNull String code) {
                btnGenerateCode.setEnabled(true);
                btnGenerateCode.setText(getString(R.string.room_invite_generate_button));

                new AlertDialog.Builder(RoomDetailsActivity.this)
                        .setTitle(getString(R.string.room_invite_created_title))
                    .setMessage(getString(R.string.room_invite_created_message, code))
                        .setPositiveButton(getString(R.string.room_invite_copy_code), (dialog, which) -> {
                            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText(getString(R.string.room_invite_clipboard_label), code);
                            clipboard.setPrimaryClip(clip);
                            Toast.makeText(RoomDetailsActivity.this,
                                    getString(R.string.room_invite_copied, code),
                                    Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton(getString(R.string.close), null)
                        .setCancelable(false)
                        .show();
            }

            @Override
            public void onError(@NonNull Exception e) {
                btnGenerateCode.setEnabled(true);
                btnGenerateCode.setText(getString(R.string.room_invite_generate_button));
                Toast.makeText(RoomDetailsActivity.this,
                        getString(R.string.room_invite_create_error, e.getMessage()),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadRoomData(String roomId) {
        DocumentReference scopeDoc = resolveScopeDoc();
        if (scopeDoc == null) {
            Toast.makeText(this, R.string.not_logged_in, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        roomListener = scopeDoc
                .collection("rooms").document(roomId)
                .addSnapshotListener((doc, e) -> {
                    if (isFinishing() || isDestroyed())
                        return;
                    if (e != null || doc == null || !doc.exists()) {
                        Toast.makeText(this, R.string.room_load_error, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Room room = doc.toObject(Room.class);
                    if (room == null)
                        return;
                    room.setId(doc.getId());
                    displayRoom(room);
                    invalidateOptionsMenu();
                });
    }

    private void loadTenantData(String roomId) {
        DocumentReference scopeDoc = resolveScopeDoc();
        if (scopeDoc == null)
            return;

        tenantListener = scopeDoc
                .collection("contracts")
                .whereEqualTo("roomId", roomId)
                .limit(20)
                .addSnapshotListener((value, error) -> {
                    if (isFinishing() || isDestroyed())
                        return;
                    if (error != null || value == null || value.isEmpty()) {
                        cardTenant.setVisibility(View.GONE);
                        updateTenantProfiles(new ArrayList<>(), false);
                        llActionButtons.setVisibility(View.GONE);
                        tenantPhoneNumber = null;
                        currentRepresentativeName = null;
                        representativePersonalId = null;
                        activeContractId = null;
                        declaredMemberCount = 0;
                        currentMemberCount = 0;
                        stopMemberListener();
                        updateOccupancySection();
                        updateInfoHeaderMeta();
                        updateContractButtonState();
                        invalidateOptionsMenu();
                        return;
                    }

                    QueryDocumentSnapshot selectedDoc = null;
                    Tenant selectedTenant = null;
                    long selectedSortTime = Long.MIN_VALUE;

                    // Ưu tiên ACTIVE, sau đó chọn hợp đồng mới nhất để tránh hiển thị không ổn định.
                    for (QueryDocumentSnapshot doc : value) {
                        Tenant tenant = doc.toObject(Tenant.class);
                        if (tenant == null) {
                            continue;
                        }

                        int candidatePriority = resolveContractPriority(tenant);
                        if (candidatePriority <= 0) {
                            continue;
                        }

                        long candidateSortTime = resolveContractSortTime(tenant);
                        int selectedPriority = selectedTenant != null ? resolveContractPriority(selectedTenant) : -1;
                        if (selectedDoc == null
                                || candidatePriority > selectedPriority
                                || (candidatePriority == selectedPriority && candidateSortTime > selectedSortTime)) {
                            selectedDoc = doc;
                            selectedTenant = tenant;
                            selectedSortTime = candidateSortTime;
                        }
                    }

                    if (selectedDoc == null || selectedTenant == null) {
                        cardTenant.setVisibility(View.GONE);
                        updateTenantProfiles(new ArrayList<>(), false);
                        llActionButtons.setVisibility(View.GONE);
                        tenantPhoneNumber = null;
                        currentRepresentativeName = null;
                        representativePersonalId = null;
                        activeContractId = null;
                        declaredMemberCount = 0;
                        currentMemberCount = 0;
                        stopMemberListener();
                        updateOccupancySection();
                        updateInfoHeaderMeta();
                        updateContractButtonState();
                        invalidateOptionsMenu();
                        return;
                    }

                    cardTenant.setVisibility(View.VISIBLE);
                    llActionButtons.setVisibility(View.VISIBLE);
                    tvTenantName.setText(selectedTenant.getFullName());
                    tvTenantPhone.setText(selectedTenant.getPhoneNumber());
                    tenantPhoneNumber = selectedTenant.getPhoneNumber();
                    currentRepresentativeName = selectedTenant.getFullName();
                    representativePersonalId = selectedTenant.getPersonalId();
                    activeContractId = selectedDoc.getId();
                    declaredMemberCount = Math.max(selectedTenant.getMemberCount(), 0);
                    currentMemberCount = hasRepresentativeTenant() ? 1 : 0;
                    ArrayList<ContractMember> bootstrap = new ArrayList<>();
                    bootstrap.add(buildRepresentativeFallbackMember(selectedTenant));
                    updateTenantProfiles(bootstrap, true);
                    updateOccupancySection();
                    startMemberListener(scopeDoc, activeContractId);
                    updateInfoHeaderMeta();
                    updateContractButtonState();
                    invalidateOptionsMenu();
                });
    }

    private DocumentReference resolveScopeDoc() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return null;
        }
        String uid = user.getUid();
        String tenantId = TenantSession.getActiveTenantId();
        return (tenantId != null && !tenantId.isEmpty())
                ? FirebaseFirestore.getInstance().collection("tenants").document(tenantId)
                : FirebaseFirestore.getInstance().collection("users").document(uid);
    }

    private void startMemberListener(DocumentReference scopeDoc, String contractId) {
        if (scopeDoc == null || contractId == null || contractId.trim().isEmpty()) {
            return;
        }
        stopMemberListener();
        memberListener = scopeDoc
                .collection("contractMembers")
                .whereEqualTo("contractId", contractId)
                .whereEqualTo("active", true)
                .addSnapshotListener((snapshot, e) -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (e != null || snapshot == null) {
                        return;
                    }
                    ArrayList<ContractMember> members = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        ContractMember member = doc.toObject(ContractMember.class);
                        if (member == null) {
                            continue;
                        }
                        member.setId(doc.getId());
                        members.add(member);
                    }
                    if (members.isEmpty() && hasRepresentativeTenant()) {
                        Tenant representative = new Tenant();
                        representative.setFullName(currentRepresentativeName);
                        representative.setPhoneNumber(tenantPhoneNumber);
                        representative.setPersonalId(representativePersonalId);
                        members.add(buildRepresentativeFallbackMember(representative));
                    }
                    Collections.sort(members, (left, right) -> {
                        if (left.isContractRepresentative() != right.isContractRepresentative()) {
                            return left.isContractRepresentative() ? -1 : 1;
                        }
                        if (left.isPrimaryContact() != right.isPrimaryContact()) {
                            return left.isPrimaryContact() ? -1 : 1;
                        }
                        String leftName = left.getFullName() != null ? left.getFullName() : "";
                        String rightName = right.getFullName() != null ? right.getFullName() : "";
                        return leftName.compareToIgnoreCase(rightName);
                    });
                    updateTenantProfiles(members, true);
                    currentMemberCount = Math.max(snapshot.size(), 0);
                    if (currentMemberCount == 0 && hasRepresentativeTenant()) {
                        currentMemberCount = 1;
                    }
                    updateOccupancySection();
                });
    }

    private void stopMemberListener() {
        if (memberListener != null) {
            memberListener.remove();
            memberListener = null;
        }
    }

    private ContractMember buildRepresentativeFallbackMember(@NonNull Tenant tenant) {
        ContractMember member = new ContractMember();
        member.setFullName(tenant.getFullName());
        member.setPhoneNumber(tenant.getPhoneNumber());
        member.setPersonalId(tenant.getPersonalId());
        member.setPrimaryContact(true);
        member.setContractRepresentative(true);
        member.setTemporaryResident(tenant.isTemporaryResident());
        member.setFullyDocumented(tenant.isFullyDocumented());
        member.setActive(true);
        return member;
    }

    private void updateTenantProfiles(@NonNull List<ContractMember> members, boolean hasActiveContract) {
        if (cardTenantProfiles == null || tvTenantProfileEmpty == null || tenantProfileAdapter == null) {
            return;
        }

        updateTenantProfileLocationContext();

        if (!hasActiveContract || isVacantRoom) {
            cardTenantProfiles.setVisibility(View.GONE);
            tenantProfileAdapter.submitList(new ArrayList<>());
            return;
        }

        cardTenantProfiles.setVisibility(View.VISIBLE);
        tenantProfileAdapter.submitList(members);
        boolean empty = members.isEmpty();
        tvTenantProfileEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (rvTenantProfiles != null) {
            rvTenantProfiles.setVisibility(empty ? View.GONE : View.VISIBLE);
        }
    }

    private void displayRoom(Room room) {
        currentRoom = room;
        currentRoomNumber = room.getRoomNumber();

        tvRoomNumber.setText(getString(R.string.room_number, room.getRoomNumber()));
        if (tvHeaderTitle != null) {
            tvHeaderTitle.setText(getString(R.string.room_number, room.getRoomNumber()));
        }

        String roomHouseName = room.getHouseName();
        currentHouseDisplayName = (roomHouseName != null && !roomHouseName.trim().isEmpty())
                ? roomHouseName.trim()
                : getString(R.string.common_not_available);
        applyHouseDisplayName();
        updateTenantProfileLocationContext();

        loadHouseMetaForHeader(room.getHouseId());
        updateInfoHeaderMeta();

        tvRoomType.setText(room.getRoomType());
        tvArea.setText(getString(R.string.room_area_value, (int) room.getArea()));

        NumberFormat fmt = NumberFormat.getNumberInstance(Locale.getDefault());
        tvRentAmount.setText(getString(R.string.room_rent_value, fmt.format(room.getRentAmount())));
        tvMaxOccupancy.setText(room.getMaxOccupancy() > 0
            ? getString(R.string.room_max_occupancy_value, room.getMaxOccupancy())
            : getString(R.string.common_not_available));

        String rawStatus = room.getStatus();
        String localizedStatus = getLocalizedRoomStatus(rawStatus);
        boolean isVacant = isVacantStatus(rawStatus);
        isVacantRoom = isVacant;
        int color = Color.parseColor(isVacant ? "#4CAF50" : "#F44336");

        if (btnGenerateCode != null) {
            updateInviteCodeButtonState();
        }

        // Status badge overlay on image
        tvStatus.setText(localizedStatus);
        GradientDrawable badge = new GradientDrawable();
        badge.setShape(GradientDrawable.RECTANGLE);
        badge.setCornerRadius(32f);
        badge.setColor(color);
        tvStatus.setBackground(badge);

        tvStatusRow.setText(localizedStatus);
        tvStatusRow.setTextColor(color);

        if (room.getImageUrl() != null && !room.getImageUrl().isEmpty() && !isDestroyed()) {
            Glide.with(this)
                    .load(room.getImageUrl())
                    .centerCrop()
                    .placeholder(R.drawable.bg_room_placeholder)
                    .into(imgRoom);
        }

        updateOccupancySection();

    }

    private void updateInfoHeaderMeta() {
        if (tvTenantRepresentativeLine != null) {
            if (currentRepresentativeName != null && !currentRepresentativeName.trim().isEmpty()) {
                tvTenantRepresentativeLine.setVisibility(View.VISIBLE);
                tvTenantRepresentativeLine
                    .setText(getString(R.string.room_representative_value, currentRepresentativeName.trim()));
            } else {
                tvTenantRepresentativeLine.setVisibility(View.GONE);
            }
        }

        if (tvManagerRow != null) {
            String manager = (currentManagerName != null && !currentManagerName.trim().isEmpty())
                    ? currentManagerName.trim()
                    : resolveFallbackManagerName();
            tvManagerRow.setText(manager);
        }
    }

    private String resolveFallbackManagerName() {
        return getString(R.string.common_not_available);
    }

    private void applyHouseDisplayName() {
        String display = (currentHouseDisplayName != null && !currentHouseDisplayName.trim().isEmpty())
                ? currentHouseDisplayName.trim()
                : getString(R.string.common_not_available);
        if (tvHeaderSubtitle != null) {
            tvHeaderSubtitle.setText(display);
        }
    }

    private boolean canManageRoomActions() {
        if (currentRoom == null) {
            return false;
        }
        if (!isVacantStatus(currentRoom.getStatus())) {
            return false;
        }
        return activeContractId == null || activeContractId.trim().isEmpty();
    }

    private void loadHouseMetaForHeader(String houseId) {
        if (houseId == null || houseId.trim().isEmpty()) {
            currentManagerName = resolveFallbackManagerName();
            applyHouseDisplayName();
            updateTenantProfileLocationContext();
            updateInfoHeaderMeta();
            return;
        }

        DocumentReference scopeDoc = resolveScopeDoc();
        if (scopeDoc == null) {
            currentManagerName = resolveFallbackManagerName();
            applyHouseDisplayName();
            updateTenantProfileLocationContext();
            updateInfoHeaderMeta();
            return;
        }

        scopeDoc.collection("houses").document(houseId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc == null || !doc.exists()) {
                        currentManagerName = resolveFallbackManagerName();
                        applyHouseDisplayName();
                        updateTenantProfileLocationContext();
                        updateInfoHeaderMeta();
                        return;
                    }

                    String houseAddress = doc.getString("address");
                    String houseNameField = doc.getString("houseName");
                    if (houseAddress != null && !houseAddress.trim().isEmpty()) {
                        currentHouseDisplayName = houseAddress.trim();
                    } else if (houseNameField != null && !houseNameField.trim().isEmpty()) {
                        currentHouseDisplayName = houseNameField.trim();
                    }

                    String managerName = doc.getString("managerName");
                    if (managerName == null || managerName.trim().isEmpty()) {
                        managerName = doc.getString("manager");
                    }
                    if (managerName == null || managerName.trim().isEmpty()) {
                        // Legacy data stores manager name in houseName field.
                        managerName = doc.getString("houseName");
                    }

                    currentManagerName = (managerName != null && !managerName.trim().isEmpty())
                            ? managerName.trim()
                            : resolveFallbackManagerName();
                    applyHouseDisplayName();
                    updateTenantProfileLocationContext();
                    updateInfoHeaderMeta();
                })
                .addOnFailureListener(e -> {
                    currentManagerName = resolveFallbackManagerName();
                    applyHouseDisplayName();
                    updateTenantProfileLocationContext();
                    updateInfoHeaderMeta();
                });
    }

    private int resolveContractPriority(@NonNull Tenant tenant) {
        String status = tenant.getContractStatus();
        if (status != null && "ACTIVE".equalsIgnoreCase(status.trim())) {
            return 2;
        }
        if (status == null || !"ENDED".equalsIgnoreCase(status.trim())) {
            return 1;
        }
        return 0;
    }

    private long resolveContractSortTime(@NonNull Tenant tenant) {
        Long updatedAt = tenant.getUpdatedAt();
        if (updatedAt != null && updatedAt > 0) {
            return updatedAt;
        }
        Long createdAt = tenant.getCreatedAt();
        if (createdAt != null && createdAt > 0) {
            return createdAt;
        }
        return Long.MIN_VALUE;
    }

    private void updateTenantProfileLocationContext() {
        if (tenantProfileAdapter == null) {
            return;
        }

        Map<String, String> roomLabels = new HashMap<>();
        Map<String, String> roomHouses = new HashMap<>();
        Map<String, String> houseLabels = new HashMap<>();

        String resolvedRoomLabel = (currentRoomNumber != null && !currentRoomNumber.trim().isEmpty())
                ? currentRoomNumber.trim()
                : (currentRoom != null && currentRoom.getRoomNumber() != null ? currentRoom.getRoomNumber().trim() : "");

        if (roomId != null && !roomId.trim().isEmpty() && !resolvedRoomLabel.isEmpty()) {
            roomLabels.put(roomId.trim(), resolvedRoomLabel);
        }

        if (currentRoom != null && currentRoom.getId() != null && !currentRoom.getId().trim().isEmpty()
                && !resolvedRoomLabel.isEmpty()) {
            roomLabels.put(currentRoom.getId().trim(), resolvedRoomLabel);
        }

        String houseId = currentRoom != null ? currentRoom.getHouseId() : null;
        String houseLabel = (currentHouseDisplayName != null && !currentHouseDisplayName.trim().isEmpty())
                ? currentHouseDisplayName.trim()
                : getString(R.string.common_not_available);

        if (houseId != null && !houseId.trim().isEmpty()) {
            String safeHouseId = houseId.trim();
            houseLabels.put(safeHouseId, houseLabel);

            if (roomId != null && !roomId.trim().isEmpty()) {
                roomHouses.put(roomId.trim(), safeHouseId);
            }
            if (currentRoom != null && currentRoom.getId() != null && !currentRoom.getId().trim().isEmpty()) {
                roomHouses.put(currentRoom.getId().trim(), safeHouseId);
            }
        }

        tenantProfileAdapter.setLocationContext(roomLabels, roomHouses, houseLabels);
    }

    private void updateOccupancySection() {
        if (tvMaxOccupancy == null || tvCurrentOccupancy == null) {
            return;
        }

        if (isVacantRoom) {
            if (rowMaxOccupancy != null)
                rowMaxOccupancy.setVisibility(View.VISIBLE);
            if (rowCurrentOccupancy != null)
                rowCurrentOccupancy.setVisibility(View.GONE);

            int maxOccupancy = currentRoom != null ? currentRoom.getMaxOccupancy() : 0;
            tvMaxOccupancy.setText(maxOccupancy > 0
                    ? getString(R.string.room_max_occupancy_value, maxOccupancy)
                    : getString(R.string.common_not_available));
                updateInviteCodeButtonState();
            return;
        }

        if (rowMaxOccupancy != null)
            rowMaxOccupancy.setVisibility(View.GONE);
        if (rowCurrentOccupancy != null)
            rowCurrentOccupancy.setVisibility(View.VISIBLE);

        if (declaredMemberCount > 0) {
            tvCurrentOccupancy.setText(getString(R.string.room_current_occupancy_value,
                    Math.max(currentMemberCount, 0), declaredMemberCount));
        } else {
            tvCurrentOccupancy.setText(getString(R.string.room_current_occupancy_unknown,
                    Math.max(currentMemberCount, 0)));
        }

        updateInviteCodeButtonState();
    }

    private boolean isInviteQuotaFull() {
        return declaredMemberCount > 0 && Math.max(currentMemberCount, 0) >= declaredMemberCount;
    }

    private void updateInviteCodeButtonState() {
        if (btnGenerateCode == null) {
            return;
        }

        boolean hasActiveContract = activeContractId != null && !activeContractId.trim().isEmpty();
        boolean shouldShow = !isVacantRoom && hasActiveContract && !isInviteQuotaFull();
        btnGenerateCode.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
    }

    private boolean hasRepresentativeTenant() {
        return currentRepresentativeName != null && !currentRepresentativeName.trim().isEmpty();
    }

    private void updateContractButtonState() {
        TextView btnManageContract = findViewById(R.id.btnQuanLyHopDong);
        if (btnManageContract == null) {
            return;
        }
        btnManageContract.setText(activeContractId != null && !activeContractId.trim().isEmpty()
            ? R.string.room_view_contract
                : R.string.create_contract);
    }

    private void openContractScreen() {
        if (roomId == null || roomId.trim().isEmpty()) {
            Toast.makeText(this, R.string.room_not_found, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, ContractActivity.class);
        intent.putExtra(ContractActivity.EXTRA_ROOM_ID, roomId);
        startActivity(intent);
    }

    private void openEditRoomScreen() {
        if (roomId == null || roomId.trim().isEmpty()) {
            Toast.makeText(this, R.string.room_not_found, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!canManageRoomActions()) {
            Toast.makeText(this, R.string.room_actions_disabled_for_rented, Toast.LENGTH_SHORT).show();
            return;
        }
        showEditRoomDialog();
    }

    private void showEditRoomDialog() {
        if (currentRoom == null) {
            Toast.makeText(this, R.string.room_not_found, Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_room, null);
        View houseField = dialogView.findViewById(R.id.layoutHouseField);
        if (houseField != null) {
            houseField.setVisibility(View.GONE);
        }
        dialogImgPreview = dialogView.findViewById(R.id.imgPreview);
        selectedImageUri = null;
        if (currentRoom.getImageUrl() != null && !currentRoom.getImageUrl().trim().isEmpty()) {
            Glide.with(this).load(currentRoom.getImageUrl()).centerCrop().into(dialogImgPreview);
        }
        View imagePickerButton = dialogView.findViewById(R.id.btnChonAnh);
        if (imagePickerButton != null) {
            imagePickerButton.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        }

        EditText etRoomNumber = dialogView.findViewById(R.id.etSoPhong);
        EditText etArea = dialogView.findViewById(R.id.etDienTich);
        EditText etRent = dialogView.findViewById(R.id.etGiaThue);
        EditText etMaxOccupancy = dialogView.findViewById(R.id.etSoNguoiToiDa);
        Spinner spinnerLoai = dialogView.findViewById(R.id.spinnerLoaiPhong);

        MoneyFormatter.applyTo(etRent);

        String[] roomTypes = getResources().getStringArray(R.array.room_type_options);
        ArrayAdapter<String> roomTypeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, roomTypes);
        roomTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLoai.setAdapter(roomTypeAdapter);

        etRoomNumber.setText(currentRoom.getRoomNumber());
        etArea.setText(currentRoom.getArea() % 1 == 0
                ? String.valueOf((long) currentRoom.getArea())
                : String.valueOf(currentRoom.getArea()));
        MoneyFormatter.setValue(etRent, currentRoom.getRentAmount());
        etMaxOccupancy.setText(currentRoom.getMaxOccupancy() > 0
                ? String.valueOf(currentRoom.getMaxOccupancy())
                : "");

        String currentRoomType = currentRoom.getRoomType();
        for (int i = 0; i < roomTypes.length; i++) {
            if (roomTypes[i].equals(currentRoomType)) {
                spinnerLoai.setSelection(i);
                break;
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(getString(R.string.room_edit_title, currentRoom.getRoomNumber()))
            .setView(dialogView)
            .setPositiveButton(R.string.update, null)
            .setNegativeButton(R.string.cancel, null)
            .create();
        dialog.show();
        pendingEditDialog = dialog;

        AlertDialog finalDialog = dialog;
        finalDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String roomNumber = etRoomNumber.getText().toString().trim();
            String areaText = etArea.getText().toString().trim();
            double rentAmount = MoneyFormatter.getValue(etRent);
            String maxOccupancyText = etMaxOccupancy.getText().toString().trim();

            clearInputError(etRoomNumber);
            clearInputError(etArea);
            clearInputError(etRent);
            clearInputError(etMaxOccupancy);

            if (roomNumber.isEmpty() || areaText.isEmpty() || rentAmount <= 0) {
                if (roomNumber.isEmpty()) {
                    showInputError(etRoomNumber, getString(R.string.please_fill_all_information));
                } else if (areaText.isEmpty()) {
                    showInputError(etArea, getString(R.string.please_fill_all_information));
                } else {
                    showInputError(etRent, getString(R.string.please_fill_all_information));
                }
                return;
            }

            int maxOccupancy;
            try {
                maxOccupancy = Integer.parseInt(maxOccupancyText);
                if (maxOccupancy <= 0) {
                    showInputError(etMaxOccupancy, getString(R.string.room_max_occupancy_required));
                    return;
                }
            } catch (Exception ignored) {
                showInputError(etMaxOccupancy, getString(R.string.room_max_occupancy_required));
                return;
            }

            double areaValue;
            try {
                areaValue = Double.parseDouble(areaText);
            } catch (Exception ignored) {
                showInputError(etArea, getString(R.string.invalid_data));
                return;
            }

            checkDuplicateRoomNumber(roomNumber, exists -> {
                if (exists) {
                    Toast.makeText(this, R.string.room_duplicate_in_house, Toast.LENGTH_LONG).show();
                    showInputError(etRoomNumber, getString(R.string.room_duplicate_in_house));
                    return;
                }

                updateRoomFromDetail(
                    roomNumber,
                    spinnerLoai.getSelectedItem() != null ? spinnerLoai.getSelectedItem().toString()
                        : currentRoom.getRoomType(),
                    areaValue,
                    rentAmount,
                    maxOccupancy,
                    finalDialog);
            });
        });
    }

    private void checkDuplicateRoomNumber(@NonNull String roomNumber, @NonNull DuplicateCallback callback) {
        DocumentReference scopeDoc = resolveScopeDoc();
        if (scopeDoc == null || currentRoom == null) {
            callback.onResult(false);
            return;
        }

        String currentHouseId = currentRoom.getHouseId();
        if (currentHouseId == null || currentHouseId.trim().isEmpty()) {
            callback.onResult(false);
            return;
        }

        scopeDoc.collection("rooms")
                .whereEqualTo("houseId", currentHouseId)
                .whereEqualTo("roomNumber", roomNumber)
                .limit(5)
                .get()
                .addOnSuccessListener(snapshot -> {
                    boolean exists = false;
                    if (snapshot != null) {
                        for (QueryDocumentSnapshot doc : snapshot) {
                            if (!doc.getId().equals(roomId)) {
                                exists = true;
                                break;
                            }
                        }
                    }
                    callback.onResult(exists);
                })
                .addOnFailureListener(e -> callback.onResult(false));
    }

    private void updateRoomFromDetail(
            @NonNull String roomNumber,
            @NonNull String roomType,
            double area,
            double rentAmount,
            int maxOccupancy,
            @NonNull AlertDialog dialog) {
        DocumentReference scopeDoc = resolveScopeDoc();
        if (scopeDoc == null || roomId == null || roomId.trim().isEmpty()) {
            Toast.makeText(this, R.string.update_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("roomNumber", roomNumber);
        updates.put("roomType", roomType);
        updates.put("area", area);
        updates.put("rentAmount", rentAmount);
        updates.put("maxOccupancy", maxOccupancy);
        updates.put("updatedAt", Timestamp.now());

        if (selectedImageUri != null) {
            pendingRoomUpdates = updates;
            pendingEditDialog = dialog;
            Intent serviceIntent = new Intent(this, ImageUploadService.class);
            serviceIntent.putExtra(ImageUploadService.EXTRA_IMAGE_URI, selectedImageUri.toString());
            ContextCompat.startForegroundService(this, serviceIntent);
            Toast.makeText(this, R.string.uploading_image_to_system, Toast.LENGTH_SHORT).show();
            return;
        }

        commitRoomUpdates(updates, dialog);
    }

    private void commitRoomUpdates(@NonNull Map<String, Object> updates, AlertDialog dialog) {
        DocumentReference scopeDoc = resolveScopeDoc();
        if (scopeDoc == null || roomId == null || roomId.trim().isEmpty()) {
            Toast.makeText(this, R.string.update_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        scopeDoc.collection("rooms").document(roomId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, R.string.update_success, Toast.LENGTH_SHORT).show();
                    if (dialog != null && dialog.isShowing()) {
                        dialog.dismiss();
                    }
                    selectedImageUri = null;
                    pendingRoomUpdates = null;
                    pendingEditDialog = null;
                })
                .addOnFailureListener(e -> Toast.makeText(this, R.string.update_failed, Toast.LENGTH_SHORT).show());
    }

    private void confirmDeleteRoom() {
        if (roomId == null || roomId.trim().isEmpty()) {
            Toast.makeText(this, R.string.room_not_found, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!canManageRoomActions()) {
            Toast.makeText(this, R.string.room_actions_disabled_for_rented, Toast.LENGTH_SHORT).show();
            return;
        }
        String roomNumber = currentRoom != null ? currentRoom.getRoomNumber() : "?";
        new AlertDialog.Builder(this)
            .setTitle(R.string.confirm_delete)
            .setMessage(getString(R.string.room_delete_confirm, roomNumber))
            .setPositiveButton(R.string.delete, (dialog, which) -> deleteRoomIfAllowed())
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void deleteRoomIfAllowed() {
        DocumentReference scopeDoc = resolveScopeDoc();
        if (scopeDoc == null) {
            Toast.makeText(this, R.string.not_logged_in, Toast.LENGTH_SHORT).show();
            return;
        }

        scopeDoc.collection("contracts")
            .whereEqualTo("roomId", roomId)
            .whereEqualTo("contractStatus", "ACTIVE")
            .limit(1)
            .get()
            .addOnSuccessListener(snap -> {
                if (snap != null && !snap.isEmpty()) {
                    Toast.makeText(this, R.string.room_delete_has_tenant, Toast.LENGTH_LONG).show();
                    return;
                }

                scopeDoc.collection("rooms").document(roomId)
                    .delete()
                    .addOnSuccessListener(unused -> {
                        Intent result = new Intent();
                        result.putExtra(EXTRA_DELETED_ROOM_NUMBER, currentRoom != null ? currentRoom.getRoomNumber() : "?");
                        setResult(RESULT_OK, result);
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, R.string.delete_failed, Toast.LENGTH_SHORT).show());
            })
            .addOnFailureListener(e -> Toast.makeText(this, R.string.delete_failed, Toast.LENGTH_SHORT).show());
    }

    private interface DuplicateCallback {
        void onResult(boolean exists);
    }

    private static void clearInputError(EditText editText) {
        if (editText != null) {
            editText.setError(null);
        }
    }

    private static void showInputError(EditText editText, String message) {
        if (editText != null) {
            editText.setError(message);
            editText.requestFocus();
        }
    }

    private boolean isVacantStatus(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim();
        return RoomStatus.VACANT.equalsIgnoreCase(normalized)
                || getString(R.string.room_status_vacant).equalsIgnoreCase(normalized)
                || "Vacant".equalsIgnoreCase(normalized);
    }

    private String getLocalizedRoomStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return getString(R.string.room_status_vacant);
        }

        String normalized = status.trim();
        if (RoomStatus.VACANT.equalsIgnoreCase(normalized)
                || getString(R.string.room_status_vacant).equalsIgnoreCase(normalized)
                || "Vacant".equalsIgnoreCase(normalized)) {
            return getString(R.string.room_status_vacant);
        }

        if (RoomStatus.RENTED.equalsIgnoreCase(normalized)
                || getString(R.string.room_status_rented).equalsIgnoreCase(normalized)
                || "Rented".equalsIgnoreCase(normalized)) {
            return getString(R.string.room_status_rented);
        }

        return normalized;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (roomListener != null)
            roomListener.remove();
        if (tenantListener != null)
            tenantListener.remove();
        stopMemberListener();
        if (uploadReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(uploadReceiver);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
