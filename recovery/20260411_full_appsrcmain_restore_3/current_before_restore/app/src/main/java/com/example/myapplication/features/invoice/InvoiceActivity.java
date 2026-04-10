package com.example.myapplication.features.invoice;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.core.constants.InvoiceStatus;
import com.example.myapplication.R;
import com.example.myapplication.core.constants.RoomStatus;
import com.example.myapplication.core.constants.TenantRoles;
import com.example.myapplication.core.constants.WaterCalculationMode;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.FinancePeriodUtil;
import com.example.myapplication.core.util.MoneyFormatter;
import com.example.myapplication.core.util.ScreenUiHelper;
import com.example.myapplication.features.contract.ContractDateHelper;
import com.example.myapplication.domain.House;
import com.example.myapplication.domain.Invoice;
import com.example.myapplication.domain.Tenant;
import com.example.myapplication.domain.Room;
import com.example.myapplication.core.repository.domain.PaymentRepository;
import com.example.myapplication.viewmodel.InvoiceViewModel;
import com.example.myapplication.viewmodel.RoomViewModel;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.WriteBatch;

import android.widget.AdapterView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class InvoiceActivity extends AppCompatActivity {

    // Current rollout strategy: owner/staff flow first.
    // Tenant self-service UI is prepared but intentionally disabled for now.
    private static final boolean ENABLE_TENANT_SELF_SERVICE = false;

    private static final class AutoCreateTarget {
        final Room room;
        final Tenant contract;

        AutoCreateTarget(@NonNull Room room, Tenant contract) {
            this.room = room;
            this.contract = contract;
        }
    }

    private static final class FeePreset {
        final double electricUnitPrice;
        final double waterUnitPrice;
        final double trashFee;
        final double wifiFee;
        final double parkingFee;
        final double otherFee;
        final String electricMode;
        final String waterMode;

        FeePreset(double electricUnitPrice,
                double waterUnitPrice,
                double trashFee,
                double wifiFee,
                double parkingFee,
                double otherFee,
                String electricMode,
                String waterMode) {
            this.electricUnitPrice = electricUnitPrice;
            this.waterUnitPrice = waterUnitPrice;
            this.trashFee = trashFee;
            this.wifiFee = wifiFee;
            this.parkingFee = parkingFee;
            this.otherFee = otherFee;
            this.electricMode = electricMode;
            this.waterMode = waterMode;
        }
    }

    private boolean isTenantUser;

    private InvoiceViewModel viewModel;
    private InvoiceAdapter adapter;
    private TextView tvEmpty;
    private View llEmpty;
    private List<Room> danhSachPhong = new ArrayList<>();

    private TextView tvSelectedMonth;
    private TextView tvSelectedKhu;
    private TextView tvFilterSummary;
    private EditText etSearchInvoice;
    private View btnSelectKhu;
    private View btnDatePicker;

    private String selectedMonth;
    private String selectedKhuId;
    private String searchQuery;
    private int selectedTabIndex;
    private List<Invoice> cachedInvoices = new ArrayList<>();
    private Map<String, String> tenantDisplayByRoom = new HashMap<>();
    private Map<String, String> roomAddressByRoom = new HashMap<>();
    private Map<String, String> roomElectricModeByRoom = new HashMap<>();
    private Map<String, String> roomWaterModeByRoom = new HashMap<>();
    private Map<String, Integer> roomMemberCountByRoom = new HashMap<>();
    private Map<String, Tenant> activeContractsByRoom = new HashMap<>();
    private Map<String, House> housesById = new HashMap<>();
    private final Set<String> autoCreatePeriodsInFlight = new HashSet<>();
    private final Set<String> draftPricingHydrationInFlight = new HashSet<>();
    private final Set<String> feeHydrationInFlight = new HashSet<>();
    private boolean hasBackfilledMeterReadings;
    private boolean hasLoadedRoomsSnapshot;
    private boolean hasLoadedHousesSnapshot;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final PaymentRepository paymentRepository = new PaymentRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScreenUiHelper.enableEdgeToEdge(this, false);

        setContentView(R.layout.activity_invoice);

        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            ScreenUiHelper.applyTopInset(appBarLayout);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        ScreenUiHelper.setupBackToolbar(this, toolbar, getString(R.string.invoice_statistics));

        tvEmpty = findViewById(R.id.tvEmpty);
        llEmpty = findViewById(R.id.llEmpty);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);

        tvSelectedMonth = findViewById(R.id.tvSelectedMonth);
        tvSelectedKhu = findViewById(R.id.tvSelectedKhu);
        tvFilterSummary = findViewById(R.id.tvFilterSummary);
        etSearchInvoice = findViewById(R.id.etSearchInvoice);
        btnSelectKhu = findViewById(R.id.btnSelectKhu);
        btnDatePicker = findViewById(R.id.btnDatePicker);
        selectedMonth = normalizeToAllowedBillingMonth(
                new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(new Date()));
        selectedKhuId = null;
        searchQuery = "";
        selectedTabIndex = 0;
        if (tvSelectedMonth != null) {
            selectedMonth = normalizeToAllowedBillingMonth(selectedMonth);
            String[] parts = selectedMonth.split("/");
            if (parts.length == 2) {
                tvSelectedMonth.setText(Integer.parseInt(parts[0]) + "/" + parts[1]);
            } else {
                tvSelectedMonth.setText(selectedMonth);
            }
        }
        if (tvSelectedKhu != null) {
            tvSelectedKhu.setText(getString(R.string.all_houses));
        }

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        final java.util.concurrent.atomic.AtomicInteger tabIdx = new java.util.concurrent.atomic.AtomicInteger(0);
        if (tabLayout != null) {
            tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.not_reported)));
            tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.reported)));
            tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.invoice_status_paid)));
        }

        adapter = new InvoiceAdapter(new InvoiceAdapter.OnItemActionListener() {
            @Override
            public void onDelete(Invoice invoice) {
                if (isTenantUser)
                    return;
                new AlertDialog.Builder(InvoiceActivity.this)
                        .setTitle(getString(R.string.confirm_delete))
                        .setMessage(getString(R.string.delete_invoice_month, invoice.getBillingPeriod()))
                        .setPositiveButton(getString(R.string.delete), (d2, w2) -> {
                            String invoiceId = invoice.getId();
                            if (invoiceId == null || invoiceId.trim().isEmpty()) {
                                Toast.makeText(InvoiceActivity.this, getString(R.string.missing_invoice_id),
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }

                            scopedCollection("payments")
                                    .whereEqualTo("invoiceId", invoiceId)
                                    .limit(1)
                                    .get()
                                    .addOnSuccessListener(qs -> {
                                        if (qs != null && !qs.isEmpty()) {
                                            Toast.makeText(InvoiceActivity.this,
                                                    getString(R.string.cannot_delete_has_payment), Toast.LENGTH_LONG)
                                                    .show();
                                            return;
                                        }
                                        viewModel.deleteInvoice(invoiceId,
                                                () -> runOnUiThread(() -> Toast
                                                        .makeText(InvoiceActivity.this, getString(R.string.deleted),
                                                                Toast.LENGTH_SHORT)
                                                        .show()),
                                                () -> runOnUiThread(() -> Toast.makeText(InvoiceActivity.this,
                                                        getString(R.string.delete_failed), Toast.LENGTH_SHORT).show()));
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(InvoiceActivity.this,
                                            getString(R.string.cannot_check_payment), Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton(getString(R.string.cancel), null).show();
            }

            @Override
            public void onBaoPhi(Invoice invoice) {
                if (isTenantUser)
                    return;
                showFeeNotificationDialog(invoice);
            }

            @Override
            public void onDoiTrangThai(Invoice invoice) {
                if (invoice == null)
                    return;

                if (isTenantUser) {
                    openPaymentHistory(invoice);
                    return;
                }

                String st = invoice.getStatus();
                if (InvoiceStatus.PAID.equals(st)) {
                    openPaymentHistory(invoice);
                    return;
                }

                showCollectPaymentDialog(invoice);
            }

            @Override
            public void onSua(Invoice invoice) {
                if (isTenantUser)
                    return;
                showEditInvoiceDialog(invoice);
            }

            @Override
            public void onXuat(Invoice invoice) {
                showInvoiceExportDialog(invoice);
            }

            @Override
            public void onEditOwnerNote(Invoice invoice) {
                if (isTenantUser)
                    return;
                showOwnerNoteDialog(invoice);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(InvoiceViewModel.class);

        final java.util.concurrent.atomic.AtomicReference<java.util.List<Invoice>> lastInvoicesRef = new java.util.concurrent.atomic.AtomicReference<>(
                new java.util.ArrayList<>());

        setupInvoiceObserverAndPermissions(list -> {
            java.util.List<Invoice> safe = list != null ? list : new java.util.ArrayList<>();
            cachedInvoices = safe;
            lastInvoicesRef.set(safe);
            maybeHydrateInvoiceFeesFromHouseDefaults();
            applyInvoiceFilters(safe, selectedTabIndex);
            maybeRepairDraftInvoicesWithHouseDefaults();
            maybeBackfillMeterReadingsFromInvoices();
        });

        if (tabLayout != null) {
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    tabIdx.set(tab.getPosition());
                    selectedTabIndex = tab.getPosition();
                    applyInvoiceFilters(lastInvoicesRef.get(), selectedTabIndex);
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                }
            });
        }

        setupFilterListeners();
    }

    private void setupFilterListeners() {
        if (btnSelectKhu != null) {
            btnSelectKhu.setOnClickListener(v -> showHouseFilterDialog());
        }
        if (btnDatePicker != null) {
            btnDatePicker.setOnClickListener(v -> showMonthFilterDialog());
        }
        if (etSearchInvoice != null) {
            etSearchInvoice.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s != null ? s.toString().trim().toLowerCase(Locale.getDefault()) : "";
                    applyInvoiceFilters(cachedInvoices, selectedTabIndex);
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }
    }

    private interface InvoiceListConsumer {
        void accept(java.util.List<Invoice> list);
    }

    private void setupInvoiceObserverAndPermissions(@NonNull InvoiceListConsumer onInvoicesChanged) {
        InvoicePermissionResolver.resolve(db, ENABLE_TENANT_SELF_SERVICE,
                new InvoicePermissionResolver.Callback() {
                    @Override
                    public void onNoTenantContext() {
                        applyLegacyMode(onInvoicesChanged);
                    }

                    @Override
                    public void onTenantSelfService(@NonNull String roomId) {
                        applyTenantSelfServiceMode(roomId, onInvoicesChanged);
                    }

                    @Override
                    public void onTenantMissingRoom() {
                        Toast.makeText(InvoiceActivity.this, getString(R.string.missing_room_for_tenant),
                                Toast.LENGTH_SHORT)
                                .show();
                        finish();
                    }

                    @Override
                    public void onOwnerOrStaff() {
                        applyOwnerStaffMode(onInvoicesChanged);
                    }
                });
    }

    private void applyLegacyMode(@NonNull InvoiceListConsumer onInvoicesChanged) {
        isTenantUser = false;
        adapter.setTenantMode(false);
        viewModel.getInvoiceList().observe(this, onInvoicesChanged::accept);
        refreshTenantDisplayData();
        refreshAutoFillSources();
        observeRoomList();
        maybeAutoCreateDraftInvoicesForSelectedMonth();
    }

    private void applyTenantSelfServiceMode(@NonNull String roomId, @NonNull InvoiceListConsumer onInvoicesChanged) {
        isTenantUser = true;
        adapter.setTenantMode(true);
        if (btnSelectKhu != null)
            btnSelectKhu.setVisibility(View.GONE);
        if (etSearchInvoice != null)
            etSearchInvoice.setHint(getString(R.string.search_by_room));

        viewModel.getInvoicesByRoom(roomId).observe(this, onInvoicesChanged::accept);
        refreshTenantDisplayData();
        refreshAutoFillSources();
    }

    private void applyOwnerStaffMode(@NonNull InvoiceListConsumer onInvoicesChanged) {
        isTenantUser = false;
        adapter.setTenantMode(false);
        viewModel.getInvoiceList().observe(this, onInvoicesChanged::accept);
        refreshTenantDisplayData();
        refreshAutoFillSources();
        observeRoomList();
        maybeAutoCreateDraftInvoicesForSelectedMonth();
    }

    private void observeRoomList() {
        new ViewModelProvider(this).get(RoomViewModel.class)
                .getRoomList().observe(this, list -> {
                    hasLoadedRoomsSnapshot = true;
                    danhSachPhong = list;
                    refreshRoomAddressMap();
                    maybeAutoCreateDraftInvoicesForSelectedMonth();
                    maybeRepairDraftInvoicesWithHouseDefaults();
                    maybeHydrateInvoiceFeesFromHouseDefaults();
                });
    }

    private void refreshAutoFillSources() {
        scopedCollection("contracts")
                .whereEqualTo("contractStatus", "ACTIVE")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null)
                        return;

                    Map<String, Tenant> map = new HashMap<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Tenant c = doc.toObject(Tenant.class);
                        if (c == null)
                            continue;
                        c.setId(doc.getId());
                        String roomId = c.getRoomId();
                        if (roomId == null || roomId.trim().isEmpty())
                            continue;

                        Tenant old = map.get(roomId);
                        if (old == null || isNewerContract(c, old)) {
                            map.put(roomId, c);
                        }
                    }
                    activeContractsByRoom = map;
                    Map<String, Integer> memberCountMap = new HashMap<>();
                    for (Map.Entry<String, Tenant> entry : map.entrySet()) {
                        Tenant contract = entry.getValue();
                        memberCountMap.put(entry.getKey(), contract != null ? Math.max(0, contract.getMemberCount()) : 0);
                    }
                    roomMemberCountByRoom = memberCountMap;
                    adapter.setRoomMemberCountByRoom(roomMemberCountByRoom);
                    maybeAutoCreateDraftInvoicesForSelectedMonth();
                    maybeRepairDraftInvoicesWithHouseDefaults();
                    maybeHydrateInvoiceFeesFromHouseDefaults();
                });

        scopedCollection("houses")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null)
                        return;
                    hasLoadedHousesSnapshot = true;
                    Map<String, House> map = new HashMap<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        House house = doc.toObject(House.class);
                        if (house == null)
                            continue;
                        house.setId(doc.getId());
                        map.put(doc.getId(), house);
                    }
                    housesById = map;
                    refreshRoomAddressMap();
                    maybeAutoCreateDraftInvoicesForSelectedMonth();
                    maybeRepairDraftInvoicesWithHouseDefaults();
                    maybeHydrateInvoiceFeesFromHouseDefaults();
                });
    }

    private void refreshTenantDisplayData() {
        scopedCollection("contracts")
                .whereEqualTo("contractStatus", "ACTIVE")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null)
                        return;
                    Map<String, String> map = new HashMap<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Tenant n = doc.toObject(Tenant.class);
                        if (n == null || n.getRoomId() == null || n.getRoomId().trim().isEmpty())
                            continue;
                        String name = n.getFullName() != null ? n.getFullName().trim() : "";
                        String phone = n.getPhoneNumber() != null ? n.getPhoneNumber().trim() : "";
                        String representative = n.getRepresentativeName() != null ? n.getRepresentativeName().trim() : "";
                        if (!representative.isEmpty()) {
                            name = representative;
                        }
                        String display = getString(R.string.invoice_representative_colon)
                                + (name.isEmpty() ? getString(R.string.updating) : name)
                                + (phone.isEmpty() ? "" : getString(R.string.phone_separator) + phone);
                        map.put(n.getRoomId(), display);
                    }
                    tenantDisplayByRoom = map;
                    adapter.setTenantDisplayByRoom(tenantDisplayByRoom);
                    adapter.setRoomAddressByRoom(roomAddressByRoom);
                    adapter.setRoomElectricModeByRoom(roomElectricModeByRoom);
                    adapter.setRoomWaterModeByRoom(roomWaterModeByRoom);
                    adapter.setRoomMemberCountByRoom(roomMemberCountByRoom);
                    applyInvoiceFilters(cachedInvoices, selectedTabIndex);
                });
    }

    private void refreshRoomAddressMap() {
        Map<String, String> map = new HashMap<>();
        Map<String, String> electricModeMap = new HashMap<>();
        Map<String, String> waterModeMap = new HashMap<>();
        if (danhSachPhong != null) {
            for (Room room : danhSachPhong) {
                if (room == null || room.getId() == null || room.getId().trim().isEmpty()) {
                    continue;
                }
                String address = "";
                House house = resolveHouseForRoom(room);
                if (house != null && house.getAddress() != null) {
                    address = house.getAddress().trim();
                }
                if (house != null && house.getWaterCalculationMethod() != null) {
                    waterModeMap.put(room.getId(), house.getWaterCalculationMethod());
                }
                if (house != null && house.getElectricityCalculationMethod() != null) {
                    electricModeMap.put(room.getId(), house.getElectricityCalculationMethod());
                }
                map.put(room.getId(), address);
            }
        }
        roomAddressByRoom = map;
        roomElectricModeByRoom = electricModeMap;
        roomWaterModeByRoom = waterModeMap;
        adapter.setRoomAddressByRoom(roomAddressByRoom);
        adapter.setRoomElectricModeByRoom(roomElectricModeByRoom);
        adapter.setRoomWaterModeByRoom(roomWaterModeByRoom);
    }

    private void maybeAutoCreateDraftInvoicesForSelectedMonth() {
        if (isTenantUser) {
            return;
        }
        if (!hasLoadedRoomsSnapshot || !hasLoadedHousesSnapshot) {
            return;
        }
        if (danhSachPhong == null || danhSachPhong.isEmpty()) {
            return;
        }

        String period = FinancePeriodUtil.normalizeMonthYear(selectedMonth);
        String periodKey = toPeriodKey(period);
        if (periodKey.isEmpty()) {
            return;
        }
        if (autoCreatePeriodsInFlight.contains(periodKey)) {
            return;
        }
        autoCreatePeriodsInFlight.add(periodKey);

        loadActiveContractsByRoom(activeContracts -> loadMissingAutoTargets(period, activeContracts, targets -> {
            if (targets.isEmpty()) {
                autoCreatePeriodsInFlight.remove(periodKey);
                return;
            }

            createDraftInvoicesQuickForTargetsSilently(period, targets,
                    () -> autoCreatePeriodsInFlight.remove(periodKey));
        }));
    }

    private void maybeRepairDraftInvoicesWithHouseDefaults() {
        if (isTenantUser || cachedInvoices == null || cachedInvoices.isEmpty()) {
            return;
        }
        if (!hasLoadedRoomsSnapshot || !hasLoadedHousesSnapshot) {
            return;
        }

        for (Invoice invoice : cachedInvoices) {
            if (invoice == null || invoice.getId() == null || invoice.getId().trim().isEmpty()) {
                continue;
            }

            String status = invoice.getStatus();
            if (status != null && !status.trim().isEmpty() && !InvoiceStatus.UNREPORTED.equals(status)) {
                continue;
            }
            if (draftPricingHydrationInFlight.contains(invoice.getId())) {
                continue;
            }

            Room room = findRoomById(invoice.getRoomId());
            if (room == null) {
                continue;
            }

            Tenant contract = activeContractsByRoom.get(room.getId());
            FeePreset preset = resolveDefaultFeePreset(room, contract);

            boolean changed = false;

            if (invoice.getRentAmount() <= 0) {
                double rent = 0;
                if (contract != null && contract.getRentAmount() > 0) {
                    rent = contract.getRentAmount();
                } else if (room.getRentAmount() > 0) {
                    rent = room.getRentAmount();
                }
                if (rent > 0) {
                    invoice.setRentAmount(rent);
                    changed = true;
                }
            }

            if (invoice.getElectricUnitPrice() <= 0 && preset.electricUnitPrice > 0) {
                invoice.setElectricUnitPrice(preset.electricUnitPrice);
                changed = true;
            }
            if (invoice.getWaterUnitPrice() <= 0 && preset.waterUnitPrice > 0) {
                invoice.setWaterUnitPrice(preset.waterUnitPrice);
                changed = true;
            }
            if (invoice.getTrashFee() <= 0 && preset.trashFee > 0) {
                invoice.setTrashFee(preset.trashFee);
                changed = true;
            }
            if (invoice.getWifiFee() <= 0 && preset.wifiFee > 0) {
                invoice.setWifiFee(preset.wifiFee);
                changed = true;
            }
            if (invoice.getParkingFee() <= 0 && preset.parkingFee > 0) {
                invoice.setParkingFee(preset.parkingFee);
                changed = true;
            }
            if (invoice.getOtherFee() <= 0 && preset.otherFee > 0) {
                invoice.setOtherFee(preset.otherFee);
                invoice.setOtherFeeLines(resolveSelectedExtraFeeLines(room, contract));
                changed = true;
            }

            String billingPeriod = invoice.getBillingPeriod() != null ? invoice.getBillingPeriod() : selectedMonth;

            if (isElectricPerPersonMode(preset.electricMode)
                    && invoice.getElectricStartReading() == 0
                    && invoice.getElectricEndReading() == 0) {
                int memberCount = contract != null ? Math.max(0, contract.getMemberCount()) : 0;
                invoice.setElectricStartReading(0);
                invoice.setElectricEndReading(memberCount);
                changed = true;
            } else if (isElectricRoomMode(preset.electricMode)
                    && invoice.getElectricStartReading() == 0
                    && invoice.getElectricEndReading() == 0) {
                invoice.setElectricStartReading(0);
                invoice.setElectricEndReading(1);
                changed = true;
            } else if (isElectricMeterMode(preset.electricMode)
                    && invoice.getElectricStartReading() == 0
                    && invoice.getElectricEndReading() == 0) {
                double seed = resolveInitialElectricStart(0, contract, billingPeriod);
                if (seed > 0) {
                    invoice.setElectricStartReading(seed);
                    invoice.setElectricEndReading(seed);
                    changed = true;
                }
            }

            if (WaterCalculationMode.isPerPerson(preset.waterMode)
                    && invoice.getWaterStartReading() == 0
                    && invoice.getWaterEndReading() == 0) {
                int memberCount = contract != null ? Math.max(0, contract.getMemberCount()) : 0;
                invoice.setWaterStartReading(0);
                invoice.setWaterEndReading(memberCount);
                changed = true;
            } else if (WaterCalculationMode.ROOM.equals(preset.waterMode)
                    && invoice.getWaterStartReading() == 0
                    && invoice.getWaterEndReading() == 0) {
                invoice.setWaterStartReading(0);
                invoice.setWaterEndReading(1);
                changed = true;
            } else if (WaterCalculationMode.isMeter(preset.waterMode)
                    && invoice.getWaterStartReading() == 0
                    && invoice.getWaterEndReading() == 0) {
                double seed = resolveInitialWaterStart(0, contract, billingPeriod);
                if (seed > 0) {
                    invoice.setWaterStartReading(seed);
                    invoice.setWaterEndReading(seed);
                    changed = true;
                }
            }

            if (!changed) {
                continue;
            }

            draftPricingHydrationInFlight.add(invoice.getId());
            viewModel.updateInvoice(invoice,
                    () -> runOnUiThread(() -> draftPricingHydrationInFlight.remove(invoice.getId())),
                    () -> runOnUiThread(() -> draftPricingHydrationInFlight.remove(invoice.getId())));
        }
    }

    private void maybeHydrateInvoiceFeesFromHouseDefaults() {
        if (isTenantUser || cachedInvoices == null || cachedInvoices.isEmpty()) {
            return;
        }
        if (!hasLoadedRoomsSnapshot || !hasLoadedHousesSnapshot) {
            return;
        }

        for (Invoice invoice : cachedInvoices) {
            if (invoice == null || invoice.getRoomId() == null || invoice.getRoomId().trim().isEmpty()) {
                continue;
            }

            Room room = findRoomById(invoice.getRoomId());
            if (room == null) {
                continue;
            }

            String invoiceId = invoice.getId();
            double oldTrashFee = invoice.getTrashFee();
            double oldWifiFee = invoice.getWifiFee();
            double oldParkingFee = invoice.getParkingFee();
            double oldOtherFee = invoice.getOtherFee();
            int oldOtherFeeLineSize = invoice.getOtherFeeLines() != null ? invoice.getOtherFeeLines().size() : 0;

            hydrateInvoiceFeesFromHouseAndContractIfMissing(invoice, room);

            int newOtherFeeLineSize = invoice.getOtherFeeLines() != null ? invoice.getOtherFeeLines().size() : 0;
            boolean changed = Math.abs(invoice.getTrashFee() - oldTrashFee) > 0.001
                    || Math.abs(invoice.getWifiFee() - oldWifiFee) > 0.001
                    || Math.abs(invoice.getParkingFee() - oldParkingFee) > 0.001
                    || Math.abs(invoice.getOtherFee() - oldOtherFee) > 0.001
                    || newOtherFeeLineSize != oldOtherFeeLineSize;

            if (!changed || invoiceId == null || invoiceId.trim().isEmpty()) {
                continue;
            }

            String status = normalizeInvoiceStatus(invoice.getStatus());
            if (InvoiceStatus.PAID.equals(status)) {
                continue;
            }
            if (feeHydrationInFlight.contains(invoiceId)) {
                continue;
            }

            feeHydrationInFlight.add(invoiceId);
            viewModel.updateInvoice(invoice,
                    () -> runOnUiThread(() -> feeHydrationInFlight.remove(invoiceId)),
                    () -> runOnUiThread(() -> feeHydrationInFlight.remove(invoiceId)));
        }
    }

    @NonNull
    private String normalizeInvoiceStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return InvoiceStatus.UNREPORTED;
        }
        if ("PARTIAL".equalsIgnoreCase(status.trim())) {
            return InvoiceStatus.REPORTED;
        }
        return status;
    }

    private void ensureInvoiceQuotaThen(@NonNull String period, int toCreate, @NonNull Runnable onAllowed) {
        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId == null || tenantId.trim().isEmpty()) {
            onAllowed.run();
            return;
        }

        db.collection("tenants").document(tenantId).get()
                .addOnSuccessListener(tdoc -> {
                    Long maxL = tdoc.getLong("maxInvoicesPerMonth");
                    int max = maxL != null ? maxL.intValue() : 200;

                    db.collection("tenants").document(tenantId).collection("invoices")
                            .whereEqualTo("billingPeriod", period)
                            .get()
                            .addOnSuccessListener(qs -> {
                                int current = qs != null ? qs.size() : 0;
                                if (current + toCreate > max) {
                                    Toast.makeText(this, getString(R.string.exceeded_invoice_quota, max),
                                            Toast.LENGTH_LONG)
                                            .show();
                                    return;
                                }
                                onAllowed.run();
                            })
                            .addOnFailureListener(e -> onAllowed.run());
                })
                .addOnFailureListener(e -> onAllowed.run());
    }

    private void showBillingCycleDialog() {
        if (danhSachPhong == null || danhSachPhong.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_rooms_to_settle_period), Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_finalize_period, null);
        EditText etThangNam = dialogView.findViewById(R.id.etThangNam);
        EditText etDonGiaDien = dialogView.findViewById(R.id.etDonGiaDien);
        EditText etDonGiaNuoc = dialogView.findViewById(R.id.etDonGiaNuoc);
        EditText etPhiRac = dialogView.findViewById(R.id.etPhiRac);
        EditText etPhiWifi = dialogView.findViewById(R.id.etPhiWifi);
        EditText etPhiGuiXe = dialogView.findViewById(R.id.etPhiGuiXe);
        Spinner spinnerHouse = dialogView.findViewById(R.id.spinnerHouse);

        // Apply money formatters
        MoneyFormatter.applyTo(etDonGiaDien);
        MoneyFormatter.applyTo(etDonGiaNuoc);
        MoneyFormatter.applyTo(etPhiRac);
        MoneyFormatter.applyTo(etPhiWifi);
        MoneyFormatter.applyTo(etPhiGuiXe);

        etThangNam.setText(new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(new Date()));

        List<String> houseIds = new ArrayList<>();
        List<String> houseLabels = new ArrayList<>();
        houseIds.add("");
        houseLabels.add(getString(R.string.all_houses));
        LinkedHashMap<String, String> houseMap = new LinkedHashMap<>();
        for (Room room : danhSachPhong) {
            if (room == null)
                continue;
            String id = room.getHouseId();
            if (id == null || id.trim().isEmpty() || houseMap.containsKey(id))
                continue;
            String name = buildHouseDisplayLabel(id, room.getHouseName());
            houseMap.put(id, name);
        }
        houseIds.addAll(houseMap.keySet());
        houseLabels.addAll(houseMap.values());
        ArrayAdapter<String> houseAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, houseLabels);
        houseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHouse.setAdapter(houseAdapter);

        spinnerHouse.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedHouseId = houseIds.get(position);
                if (selectedHouseId == null || selectedHouseId.isEmpty()) {
                    return;
                }
                House house = housesById.get(selectedHouseId);
                if (house == null)
                    return;
                if (house.getElectricityPrice() > 0)
                    MoneyFormatter.setValue(etDonGiaDien, house.getElectricityPrice());
                if (house.getWaterPrice() > 0)
                    MoneyFormatter.setValue(etDonGiaNuoc, house.getWaterPrice());
                if (house.getTrashPrice() > 0)
                    MoneyFormatter.setValue(etPhiRac, house.getTrashPrice());
                if (house.getInternetPrice() > 0)
                    MoneyFormatter.setValue(etPhiWifi, house.getInternetPrice());
                if (house.getParkingPrice() > 0)
                    MoneyFormatter.setValue(etPhiGuiXe, house.getParkingPrice());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.settle_period_create_draft))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.create), (d, w) -> {
                    String period = etThangNam.getText().toString().trim();
                    if (toPeriodKey(period).isEmpty()) {
                        Toast.makeText(this, getString(R.string.invalid_month_year_format), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double electricUnitPrice = MoneyFormatter.getValue(etDonGiaDien);
                    double waterUnitPrice = MoneyFormatter.getValue(etDonGiaNuoc);
                    double trashFee = MoneyFormatter.getValue(etPhiRac);
                    double wifiFee = MoneyFormatter.getValue(etPhiWifi);
                    double parkingFee = MoneyFormatter.getValue(etPhiGuiXe);
                    String selectedHouseId = houseIds.get(spinnerHouse.getSelectedItemPosition());

                    loadActiveContractsByRoom(activeContractsByRoom -> loadMissingAutoTargets(period,
                            activeContractsByRoom, targets -> {
                                List<AutoCreateTarget> effectiveTargets = targets;
                                if (selectedHouseId != null && !selectedHouseId.trim().isEmpty()) {
                                    List<AutoCreateTarget> filtered = new ArrayList<>();
                                    for (AutoCreateTarget t : targets) {
                                        if (t != null && t.room != null
                                                && selectedHouseId.equals(t.room.getHouseId())) {
                                            filtered.add(t);
                                        }
                                    }
                                    effectiveTargets = filtered;
                                }

                                if (effectiveTargets.isEmpty()) {
                                    Toast.makeText(this, getString(R.string.no_new_invoices_for_period),
                                            Toast.LENGTH_SHORT)
                                            .show();
                                    return;
                                }

                                final List<AutoCreateTarget> finalTargets = effectiveTargets;

                                ensureInvoiceQuotaThen(period, finalTargets.size(), () -> createDraftInvoicesForTargets(
                                        period,
                                        electricUnitPrice,
                                        waterUnitPrice,
                                        trashFee,
                                        wifiFee,
                                        parkingFee,
                                        finalTargets));
                            }));
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showClosePeriodOptions() {
        String[] options = new String[] {
                getString(R.string.quick_create_current_month),
                getString(R.string.open_settle_config)
        };

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.settle_period))
                .setItems(options, (d, which) -> {
                    if (which == 0) {
                        runQuickCloseForCurrentMonth();
                    } else {
                        showBillingCycleDialog();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void runQuickCloseForCurrentMonth() {
        if (danhSachPhong == null || danhSachPhong.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_rooms_to_settle_period), Toast.LENGTH_SHORT).show();
            return;
        }

        String period = FinancePeriodUtil
                .normalizeMonthYear(new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(new Date()));

        loadActiveContractsByRoom(
                activeContractsByRoom -> loadMissingAutoTargets(period, activeContractsByRoom, targets -> {
                    if (targets.isEmpty()) {
                        Toast.makeText(this, getString(R.string.no_new_invoices_for_period), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ensureInvoiceQuotaThen(period, targets.size(),
                            () -> createDraftInvoicesQuickForTargets(period, targets));
                }));
    }

    private void createDraftInvoicesQuickForTargets(@NonNull String period,
            @NonNull List<AutoCreateTarget> targets) {
        Toast.makeText(this, getString(R.string.creating_quick_invoices), Toast.LENGTH_SHORT).show();

        final int[] pending = { targets.size() };
        final int[] created = { 0 };
        final int[] duplicated = { 0 };
        final int[] failed = { 0 };

        for (AutoCreateTarget target : targets) {
            Room room = target.room;
            Tenant contract = target.contract;

            InvoiceMeterHelper.loadLatestMeterEnds(this::scopedCollection, this::toPeriodKey, room.getId(),
                    (elecEnd, waterEnd) -> {
                        FeePreset fee = resolveDefaultFeePreset(room, contract);
                        double effectiveElecStart = resolveInitialElectricStart(elecEnd, contract, period);
                        double effectiveWaterStart = resolveInitialWaterStart(waterEnd, contract, period);
                        Invoice hd = new Invoice();
                        hd.setRoomId(room.getId());
                        hd.setRoomNumber(room.getRoomNumber());
                        hd.setBillingPeriod(period);

                        if (contract != null && contract.getId() != null && !contract.getId().trim().isEmpty()) {
                            hd.setContractId(contract.getId());
                            long contractRent = contract.getRentAmount();
                            hd.setRentAmount(contractRent > 0 ? contractRent : room.getRentAmount());
                        } else {
                            hd.setRentAmount(room.getRentAmount());
                        }

                        if (isElectricPerPersonMode(fee.electricMode)) {
                            int memberCount = contract != null ? Math.max(0, contract.getMemberCount()) : 0;
                            hd.setElectricStartReading(0);
                            hd.setElectricEndReading(memberCount);
                        } else if (isElectricRoomMode(fee.electricMode)) {
                            hd.setElectricStartReading(0);
                            hd.setElectricEndReading(1);
                        } else {
                            hd.setElectricStartReading(effectiveElecStart);
                            hd.setElectricEndReading(effectiveElecStart);
                        }
                        hd.setElectricUnitPrice(fee.electricUnitPrice);
                        if (WaterCalculationMode.isPerPerson(fee.waterMode)) {
                            int memberCount = contract != null ? Math.max(0, contract.getMemberCount()) : 0;
                            hd.setWaterStartReading(0);
                            hd.setWaterEndReading(memberCount);
                        } else if (WaterCalculationMode.ROOM.equals(fee.waterMode)) {
                            hd.setWaterStartReading(0);
                            hd.setWaterEndReading(1);
                        } else {
                            hd.setWaterStartReading(effectiveWaterStart);
                            hd.setWaterEndReading(effectiveWaterStart);
                        }
                        hd.setWaterUnitPrice(fee.waterUnitPrice);
                        hd.setTrashFee(fee.trashFee);
                        hd.setWifiFee(fee.wifiFee);
                        hd.setParkingFee(fee.parkingFee);
                        hd.setOtherFee(fee.otherFee);
                        hd.setOtherFeeLines(resolveSelectedExtraFeeLines(room, contract));
                        hd.setStatus(InvoiceStatus.UNREPORTED);

                        viewModel.addInvoiceUnique(hd,
                                () -> {
                                    created[0]++;
                                    double meterElecStart = isElectricMeterMode(fee.electricMode)
                                            ? hd.getElectricStartReading()
                                            : elecEnd;
                                    double meterElecEnd = isElectricMeterMode(fee.electricMode)
                                            ? hd.getElectricEndReading()
                                            : elecEnd;
                                    double meterWaterStart = WaterCalculationMode.isMeter(fee.waterMode)
                                            ? hd.getWaterStartReading()
                                            : waterEnd;
                                    double meterWaterEnd = WaterCalculationMode.isMeter(fee.waterMode)
                                            ? hd.getWaterEndReading()
                                            : waterEnd;
                                    InvoiceMeterHelper.saveMeterReadingFromInvoice(this::scopedCollection,
                                            this::toPeriodKey,
                                            room.getId(), period, meterElecStart, meterElecEnd,
                                            meterWaterStart, meterWaterEnd);
                                    onAutoCreateDone(pending, created, duplicated, failed);
                                },
                                () -> {
                                    duplicated[0]++;
                                    onAutoCreateDone(pending, created, duplicated, failed);
                                },
                                () -> {
                                    failed[0]++;
                                    onAutoCreateDone(pending, created, duplicated, failed);
                                });
                    });
        }
    }

    private void createDraftInvoicesQuickForTargetsSilently(@NonNull String period,
            @NonNull List<AutoCreateTarget> targets,
            @NonNull AutoCreateDoneCallback onDone) {
        final int[] pending = { targets.size() };

        if (targets.isEmpty()) {
            onDone.onDone();
            return;
        }

        for (AutoCreateTarget target : targets) {
            Room room = target.room;
            Tenant contract = target.contract;

            InvoiceMeterHelper.loadLatestMeterEnds(this::scopedCollection, this::toPeriodKey, room.getId(),
                    (elecEnd, waterEnd) -> {
                        FeePreset fee = resolveDefaultFeePreset(room, contract);
                        Invoice hd = new Invoice();
                        hd.setRoomId(room.getId());
                        hd.setRoomNumber(room.getRoomNumber());
                        hd.setBillingPeriod(period);

                        if (contract != null && contract.getId() != null && !contract.getId().trim().isEmpty()) {
                            hd.setContractId(contract.getId());
                            long contractRent = contract.getRentAmount();
                            hd.setRentAmount(contractRent > 0 ? contractRent : room.getRentAmount());
                        } else {
                            hd.setRentAmount(room.getRentAmount());
                        }

                        if (isElectricPerPersonMode(fee.electricMode)) {
                            int memberCount = contract != null ? Math.max(0, contract.getMemberCount()) : 0;
                            hd.setElectricStartReading(0);
                            hd.setElectricEndReading(memberCount);
                        } else if (isElectricRoomMode(fee.electricMode)) {
                            hd.setElectricStartReading(0);
                            hd.setElectricEndReading(1);
                        } else {
                            hd.setElectricStartReading(elecEnd);
                            hd.setElectricEndReading(elecEnd);
                        }
                        hd.setElectricUnitPrice(fee.electricUnitPrice);
                        if (WaterCalculationMode.isPerPerson(fee.waterMode)) {
                            int memberCount = contract != null ? Math.max(0, contract.getMemberCount()) : 0;
                            hd.setWaterStartReading(0);
                            hd.setWaterEndReading(memberCount);
                        } else if (WaterCalculationMode.ROOM.equals(fee.waterMode)) {
                            hd.setWaterStartReading(0);
                            hd.setWaterEndReading(1);
                        } else {
                            hd.setWaterStartReading(waterEnd);
                            hd.setWaterEndReading(waterEnd);
                        }
                        hd.setWaterUnitPrice(fee.waterUnitPrice);
                        hd.setTrashFee(fee.trashFee);
                        hd.setWifiFee(fee.wifiFee);
                        hd.setParkingFee(fee.parkingFee);
                        hd.setOtherFee(fee.otherFee);
                        hd.setOtherFeeLines(resolveSelectedExtraFeeLines(room, contract));
                        hd.setStatus(InvoiceStatus.UNREPORTED);

                        Runnable doneOne = () -> {
                            pending[0]--;
                            if (pending[0] <= 0) {
                                onDone.onDone();
                            }
                        };

                        viewModel.addInvoiceUnique(hd,
                                () -> {
                                double meterElecStart = isElectricMeterMode(fee.electricMode)
                                    ? hd.getElectricStartReading()
                                    : elecEnd;
                                double meterElecEnd = isElectricMeterMode(fee.electricMode)
                                    ? hd.getElectricEndReading()
                                    : elecEnd;
                                    double meterWaterStart = WaterCalculationMode.isMeter(fee.waterMode)
                                            ? hd.getWaterStartReading()
                                            : waterEnd;
                                    double meterWaterEnd = WaterCalculationMode.isMeter(fee.waterMode)
                                            ? hd.getWaterEndReading()
                                            : waterEnd;
                                    InvoiceMeterHelper.saveMeterReadingFromInvoice(this::scopedCollection,
                                            this::toPeriodKey,
                                    room.getId(), period, meterElecStart, meterElecEnd,
                                    meterWaterStart, meterWaterEnd);
                                    doneOne.run();
                                },
                                doneOne,
                                doneOne);
                    });
        }
    }

    private interface ActiveContractsCallback {
        void onDone(@NonNull Map<String, Tenant> activeContractsByRoom);
    }

    private interface AutoTargetsCallback {
        void onDone(@NonNull List<AutoCreateTarget> targets);
    }

    private interface AutoCreateDoneCallback {
        void onDone();
    }

    private void loadActiveContractsByRoom(@NonNull ActiveContractsCallback callback) {
        scopedCollection("contracts")
                .whereEqualTo("contractStatus", "ACTIVE")
                .get()
                .addOnSuccessListener(snapshot -> {
                    Map<String, Tenant> map = new HashMap<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Tenant c = doc.toObject(Tenant.class);
                        if (c == null)
                            continue;
                        c.setId(doc.getId());

                        String roomId = c.getRoomId();
                        if (roomId == null || roomId.trim().isEmpty())
                            continue;

                        Tenant existing = map.get(roomId);
                        if (existing == null || isNewerContract(c, existing)) {
                            map.put(roomId, c);
                        }
                    }
                    callback.onDone(map);
                })
                .addOnFailureListener(e -> callback.onDone(new HashMap<>()));
    }

    private boolean isNewerContract(@NonNull Tenant lhs, @NonNull Tenant rhs) {
        long l = lhs.getUpdatedAt() != null ? lhs.getUpdatedAt()
                : (lhs.getCreatedAt() != null ? lhs.getCreatedAt() : 0);
        long r = rhs.getUpdatedAt() != null ? rhs.getUpdatedAt()
                : (rhs.getCreatedAt() != null ? rhs.getCreatedAt() : 0);
        return l >= r;
    }

    private void loadMissingAutoTargets(@NonNull String period,
            @NonNull Map<String, Tenant> activeContractsByRoom,
            @NonNull AutoTargetsCallback callback) {
        scopedCollection("invoices")
                .whereEqualTo("billingPeriod", period)
                .get()
                .addOnSuccessListener(snapshot -> {
                    Set<String> existingRoomIds = new HashSet<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        String roomId = doc.getString("roomId");
                        if (roomId != null && !roomId.trim().isEmpty())
                            existingRoomIds.add(roomId);
                    }

                    List<AutoCreateTarget> targets = new ArrayList<>();
                    for (Room room : danhSachPhong) {
                        if (room == null || room.getId() == null || room.getId().trim().isEmpty())
                            continue;

                        String roomId = room.getId();
                        Tenant contract = activeContractsByRoom.get(roomId);
                        boolean isRented = RoomStatus.RENTED.equals(room.getStatus()) || contract != null;
                        if (!isRented)
                            continue;

                        if (contract != null && !isContractBillableForPeriod(contract, period))
                            continue;

                        if (existingRoomIds.contains(roomId))
                            continue;

                        targets.add(new AutoCreateTarget(room, contract));
                    }

                    callback.onDone(targets);
                })
                .addOnFailureListener(e -> callback.onDone(new ArrayList<>()));
    }

    private boolean isContractBillableForPeriod(@NonNull Tenant contract, @NonNull String period) {
        String targetPeriod = FinancePeriodUtil.normalizeMonthYear(period);
        if (targetPeriod.isEmpty()) {
            return true;
        }

        String startPeriod = resolveBillingStartPeriod(contract);
        if (startPeriod.isEmpty()) {
            return true;
        }

        String targetKey = toPeriodKey(targetPeriod);
        String startKey = toPeriodKey(startPeriod);
        if (targetKey.isEmpty() || startKey.isEmpty()) {
            return true;
        }

        return targetKey.compareTo(startKey) >= 0;
    }

    @NonNull
    private String resolveBillingStartPeriod(@NonNull Tenant contract) {
        String explicit = FinancePeriodUtil.normalizeMonthYear(contract.getBillingStartPeriod());
        if (!explicit.isEmpty()) {
            return explicit;
        }

        String policy = contract.getBillingStartPolicy();
        if (policy == null || policy.trim().isEmpty()) {
            String legacy = contract.getBillingReminderAt();
            if ("mid_month".equals(legacy) || "end_month".equals(legacy)) {
                policy = "next_month";
            } else {
                policy = "current_month";
            }
        }

        Calendar calendar = ContractDateHelper.parseContractDate(contract.getRentalStartDate());
        if (calendar == null) {
            return "";
        }

        if ("next_month".equalsIgnoreCase(policy.trim())) {
            calendar.add(Calendar.MONTH, 1);
        }

        int month = calendar.get(Calendar.MONTH) + 1;
        int year = calendar.get(Calendar.YEAR);
        return String.format(Locale.US, "%02d/%04d", month, year);
    }

    private void createDraftInvoicesForTargets(@NonNull String period,
            double electricUnitPrice,
            double waterUnitPrice,
            double trashFee,
            double wifiFee,
            double parkingFee,
            @NonNull List<AutoCreateTarget> targets) {
        Toast.makeText(this, getString(R.string.creating_draft_invoices_short), Toast.LENGTH_SHORT).show();

        final int[] pending = { targets.size() };
        final int[] created = { 0 };
        final int[] duplicated = { 0 };
        final int[] failed = { 0 };

        for (AutoCreateTarget target : targets) {
            Room room = target.room;
            Tenant contract = target.contract;

            InvoiceMeterHelper.loadLatestMeterEnds(this::scopedCollection, this::toPeriodKey, room.getId(),
                    (elecEnd, waterEnd) -> {
                        double effectiveElecStart = resolveInitialElectricStart(elecEnd, contract, period);
                        double effectiveWaterStart = resolveInitialWaterStart(waterEnd, contract, period);
                        Invoice hd = new Invoice();
                        hd.setRoomId(room.getId());
                        hd.setRoomNumber(room.getRoomNumber());
                        if (contract != null && contract.getId() != null && !contract.getId().trim().isEmpty()) {
                            long contractRent = contract.getRentAmount();
                            hd.setRentAmount(contractRent > 0 ? contractRent : room.getRentAmount());
                        } else {
                            hd.setRentAmount(room.getRentAmount());
                        }
                        hd.setBillingPeriod(period);
                        if (contract != null && contract.getId() != null && !contract.getId().trim().isEmpty()) {
                            hd.setContractId(contract.getId());
                        }

                        FeePreset roomFeePreset = resolveDefaultFeePreset(room, contract);
                        if (isElectricPerPersonMode(roomFeePreset.electricMode)) {
                            int memberCount = contract != null ? Math.max(0, contract.getMemberCount()) : 0;
                            hd.setElectricStartReading(0);
                            hd.setElectricEndReading(memberCount);
                        } else if (isElectricRoomMode(roomFeePreset.electricMode)) {
                            hd.setElectricStartReading(0);
                            hd.setElectricEndReading(1);
                        } else {
                            hd.setElectricStartReading(effectiveElecStart);
                            hd.setElectricEndReading(effectiveElecStart);
                        }
                        hd.setElectricUnitPrice(electricUnitPrice);
                        if (WaterCalculationMode.isPerPerson(roomFeePreset.waterMode)) {
                            int memberCount = contract != null ? Math.max(0, contract.getMemberCount()) : 0;
                            hd.setWaterStartReading(0);
                            hd.setWaterEndReading(memberCount);
                        } else if (WaterCalculationMode.ROOM.equals(roomFeePreset.waterMode)) {
                            hd.setWaterStartReading(0);
                            hd.setWaterEndReading(1);
                        } else {
                            hd.setWaterStartReading(effectiveWaterStart);
                            hd.setWaterEndReading(effectiveWaterStart);
                        }
                        hd.setWaterUnitPrice(waterUnitPrice);
                        
                        House house = resolveHouseForRoom(room);
                        hd.setTrashFee(calculateFeeWithUnit(trashFee, house != null ? house.getTrashUnitSafe() : null, contract));
                        double calculatedWifi = calculateFeeWithUnit(wifiFee, house != null ? house.getInternetUnitSafe() : null, contract);
                        hd.setWifiFee(contract != null && contract.hasInternetService() ? calculatedWifi : 0);
                        double calculatedParking = calculateFeeWithUnit(parkingFee, house != null ? house.getParkingUnitSafe() : null, contract);
                        hd.setParkingFee(contract != null && contract.hasParkingService() ? calculatedParking : 0);
                        
                        hd.setOtherFee(resolveSelectedExtraFeeTotal(room, contract));
                        hd.setOtherFeeLines(resolveSelectedExtraFeeLines(room, contract));
                        hd.setStatus(InvoiceStatus.UNREPORTED);

                        viewModel.addInvoiceUnique(hd,
                                () -> {
                                    created[0]++;
                                    double meterElecStart = isElectricMeterMode(roomFeePreset.electricMode)
                                            ? hd.getElectricStartReading()
                                            : elecEnd;
                                    double meterElecEnd = isElectricMeterMode(roomFeePreset.electricMode)
                                            ? hd.getElectricEndReading()
                                            : elecEnd;
                                    double meterWaterStart = WaterCalculationMode.isMeter(roomFeePreset.waterMode)
                                            ? hd.getWaterStartReading()
                                            : waterEnd;
                                    double meterWaterEnd = WaterCalculationMode.isMeter(roomFeePreset.waterMode)
                                            ? hd.getWaterEndReading()
                                            : waterEnd;
                                    InvoiceMeterHelper.saveMeterReadingFromInvoice(this::scopedCollection,
                                            this::toPeriodKey,
                                            room.getId(), period, meterElecStart, meterElecEnd,
                                            meterWaterStart, meterWaterEnd);
                                    onAutoCreateDone(pending, created, duplicated, failed);
                                },
                                () -> {
                                    duplicated[0]++;
                                    onAutoCreateDone(pending, created, duplicated, failed);
                                },
                                () -> {
                                    failed[0]++;
                                    onAutoCreateDone(pending, created, duplicated, failed);
                                });
                    });
        }
    }

    private void onAutoCreateDone(int[] pending, int[] created, int[] duplicated, int[] failed) {
        pending[0]--;
        if (pending[0] > 0)
            return;

        StringBuilder summary = new StringBuilder(getString(R.string.draft_complete_summary, created[0]));
        if (duplicated[0] > 0) {
            summary.append(getString(R.string.draft_complete_duplicate_fragment, duplicated[0]));
        }
        if (failed[0] > 0) {
            summary.append(getString(R.string.draft_complete_failed_fragment, failed[0]));
        }
        Toast.makeText(this, summary.toString(), Toast.LENGTH_LONG).show();

    }

    private void showFeeNotificationDialog(Invoice invoice) {
        if (invoice == null)
            return;

        View dialogView = getLayoutInflater().inflate(R.layout.bottom_sheet_fee_notification, null);
        TextView tvPhong = dialogView.findViewById(R.id.tvPhong);
        TextView tvTenant = dialogView.findViewById(R.id.tvTenant);
        EditText etChiSoDienCu = dialogView.findViewById(R.id.etChiSoDienCu);
        EditText etChiSoDienMoi = dialogView.findViewById(R.id.etChiSoDienMoi);
        EditText etChiSoNuocCu = dialogView.findViewById(R.id.etChiSoNuocCu);
        EditText etChiSoNuocMoi = dialogView.findViewById(R.id.etChiSoNuocMoi);
        View rowElectricOld = dialogView.findViewById(R.id.rowElectricOld);
        View rowElectricNew = dialogView.findViewById(R.id.rowElectricNew);
        View rowWaterOld = dialogView.findViewById(R.id.rowWaterOld);
        View rowWaterNew = dialogView.findViewById(R.id.rowWaterNew);
        MaterialButton btnXemInvoice = dialogView.findViewById(R.id.btnXemInvoice);
        ImageView btnClose = dialogView.findViewById(R.id.btnClose);

        Room room = findRoomById(invoice.getRoomId());
        hydrateInvoiceFeesFromHouseAndContractIfMissing(invoice, room);
        boolean electricMeterMode = resolveElectricMeterModeForNotification(room, invoice);
        boolean waterMeterMode = resolveWaterMeterModeForNotification(room, invoice);

        String roomLabel = invoice.getRoomNumber() != null && !invoice.getRoomNumber().trim().isEmpty()
            ? invoice.getRoomNumber().trim()
            : "???";
        tvPhong.setText(getString(R.string.room_colon) + " " + roomLabel);
        tvTenant.setText(getString(R.string.invoice_representative_colon)
            + " "
            + resolveRepresentativeDisplayForNotification(invoice));

        rowElectricOld.setVisibility(electricMeterMode ? View.VISIBLE : View.GONE);
        rowElectricNew.setVisibility(electricMeterMode ? View.VISIBLE : View.GONE);
        rowWaterOld.setVisibility(waterMeterMode ? View.VISIBLE : View.GONE);
        rowWaterNew.setVisibility(waterMeterMode ? View.VISIBLE : View.GONE);

        etChiSoDienCu.setText(InvoiceFormValueHelper.formatDouble(invoice.getElectricStartReading()));
        etChiSoNuocCu.setText(InvoiceFormValueHelper.formatDouble(invoice.getWaterStartReading()));
        if (electricMeterMode) {
            etChiSoDienMoi.setText("");
        }
        if (waterMeterMode) {
            etChiSoNuocMoi.setText("");
        }

        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet = new com.google.android.material.bottomsheet.BottomSheetDialog(
                this);
        bottomSheet.setContentView(dialogView);

        btnClose.setOnClickListener(v -> bottomSheet.dismiss());

        btnXemInvoice.setOnClickListener(v -> {
            try {
                if (electricMeterMode) {
                    String electricNewText = etChiSoDienMoi.getText().toString().trim();
                    if (electricNewText.isEmpty()) {
                        Toast.makeText(this, getString(R.string.please_enter_new_electric), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double newElectric = Double.parseDouble(electricNewText);
                    if (newElectric < invoice.getElectricStartReading()) {
                        Toast.makeText(this, getString(R.string.new_electric_must_gte_old), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    invoice.setElectricEndReading(newElectric);
                }

                if (waterMeterMode) {
                    String waterNewText = etChiSoNuocMoi.getText().toString().trim();
                    if (waterNewText.isEmpty()) {
                        Toast.makeText(this, getString(R.string.please_enter_new_water), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double newWater = Double.parseDouble(waterNewText);
                    if (newWater < invoice.getWaterStartReading()) {
                        Toast.makeText(this, getString(R.string.new_water_must_gte_old), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    invoice.setWaterEndReading(newWater);
                }

                invoice.setStatus(InvoiceStatus.REPORTED);

                viewModel.updateInvoice(invoice,
                        () -> runOnUiThread(() -> {
                            syncMeterReadingFromInvoice(invoice);
                            notifyRoomTenantsInvoiceReported(invoice);
                            bottomSheet.dismiss();
                            showInvoiceExportDialog(invoice);
                        }),
                        () -> runOnUiThread(() -> Toast.makeText(this, getString(R.string.update_failed),
                                Toast.LENGTH_SHORT).show()));
            } catch (NumberFormatException e) {
                Toast.makeText(this, getString(R.string.invalid_meter_reading), Toast.LENGTH_SHORT).show();
            }
        });

        bottomSheet.show();
    }

    @NonNull
    private String resolveRepresentativeDisplayForNotification(@NonNull Invoice invoice) {
        Tenant contract = activeContractsByRoom.get(invoice.getRoomId());
        if (contract != null) {
            String representative = contract.getRepresentativeName() != null
                    ? contract.getRepresentativeName().trim()
                    : "";
            String name = contract.getFullName() != null ? contract.getFullName().trim() : "";
            String phone = contract.getPhoneNumber() != null ? contract.getPhoneNumber().trim() : "";
            String displayName = !representative.isEmpty() ? representative : name;
            if (!displayName.isEmpty()) {
                return phone.isEmpty() ? displayName : displayName + getString(R.string.phone_separator) + phone;
            }
        }

        String tenantDisplay = tenantDisplayByRoom.get(invoice.getRoomId());
        if (tenantDisplay == null || tenantDisplay.trim().isEmpty()) {
            return getString(R.string.updating);
        }

        String cleaned = tenantDisplay.trim();
        String prefix = getString(R.string.invoice_representative_colon);
        if (cleaned.startsWith(prefix)) {
            cleaned = cleaned.substring(prefix.length()).trim();
        }
        return cleaned.isEmpty() ? getString(R.string.updating) : cleaned;
    }

    private void showOwnerNoteDialog(@NonNull Invoice invoice) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_payment, null);
        EditText etAmount = dialogView.findViewById(R.id.etAmount);
        Spinner spinnerMethod = dialogView.findViewById(R.id.spinnerMethod);
        EditText etPaidAt = dialogView.findViewById(R.id.etPaidAt);
        EditText etNote = dialogView.findViewById(R.id.etNote);

        // Hide unused fields and their labels
        etAmount.setVisibility(View.GONE);
        spinnerMethod.setVisibility(View.GONE);
        etPaidAt.setVisibility(View.GONE);
        
        View tvAmountLabel = dialogView.findViewById(R.id.tvAmountLabel);
        if (tvAmountLabel != null) tvAmountLabel.setVisibility(View.GONE);
        View tvMethodLabel = dialogView.findViewById(R.id.tvMethodLabel);
        if (tvMethodLabel != null) tvMethodLabel.setVisibility(View.GONE);
        View tvDateLabel = dialogView.findViewById(R.id.tvDateLabel);
        if (tvDateLabel != null) tvDateLabel.setVisibility(View.GONE);
        View tvNoteLabel = dialogView.findViewById(R.id.tvNoteLabel);
        if (tvNoteLabel != null) tvNoteLabel.setVisibility(View.GONE);

        etNote.setHint(getString(R.string.invoice_owner_note_hint));
        etNote.setText(invoice.getOwnerNote() != null ? invoice.getOwnerNote() : "");

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.invoice_owner_note_title,
                        invoice.getRoomNumber() != null ? invoice.getRoomNumber() : "--"))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.save), (d, w) -> {
                    invoice.setOwnerNote(etNote.getText() != null ? etNote.getText().toString().trim() : "");
                    viewModel.updateInvoice(invoice,
                            () -> runOnUiThread(() -> Toast
                                    .makeText(this, getString(R.string.invoice_owner_note_saved), Toast.LENGTH_SHORT)
                                    .show()),
                            () -> runOnUiThread(() -> Toast
                                    .makeText(this, getString(R.string.update_failed), Toast.LENGTH_SHORT)
                                    .show()));
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private Room findRoomById(String roomId) {
        if (roomId == null || roomId.trim().isEmpty())
            return null;
        for (Room room : danhSachPhong) {
            if (room != null && roomId.equals(room.getId())) {
                return room;
            }
        }
        return null;
    }

    private void maybeBackfillMeterReadingsFromInvoices() {
        if (hasBackfilledMeterReadings || isTenantUser || cachedInvoices == null || cachedInvoices.isEmpty()) {
            return;
        }

        for (Invoice invoice : cachedInvoices) {
            if (invoice == null) {
                continue;
            }

            String status = invoice.getStatus();
            if (!InvoiceStatus.REPORTED.equals(status) && !InvoiceStatus.PAID.equals(status)) {
                continue;
            }

            boolean hasElectricData = invoice.getElectricEndReading() > 0 || invoice.getElectricStartReading() > 0;
            boolean hasWaterData = invoice.getWaterEndReading() > 0 || invoice.getWaterStartReading() > 0;
            if (!hasElectricData && !hasWaterData) {
                continue;
            }

            syncMeterReadingFromInvoice(invoice);
        }

        hasBackfilledMeterReadings = true;
    }

    private void syncMeterReadingFromInvoice(Invoice invoice) {
        if (invoice == null || invoice.getRoomId() == null || invoice.getRoomId().trim().isEmpty()) {
            return;
        }

        String period = FinancePeriodUtil.normalizeMonthYear(invoice.getBillingPeriod());
        if (period.isEmpty()) {
            return;
        }

        InvoiceMeterHelper.saveMeterReadingFromInvoice(
                this::scopedCollection,
                this::toPeriodKey,
                invoice.getRoomId(),
                period,
                invoice.getElectricStartReading(),
                invoice.getElectricEndReading(),
                invoice.getWaterStartReading(),
                invoice.getWaterEndReading());
    }

    private boolean isElectricMeterMode(Room room) {
        if (room == null)
            return true;
        House house = resolveHouseForRoom(room);
        if (house == null)
            return true;
        return isElectricMeterMode(house.getElectricityCalculationMethod());
    }

    private boolean isWaterMeterMode(Room room) {
        if (room == null)
            return true;
        House house = resolveHouseForRoom(room);
        if (house == null)
            return true;
        return WaterCalculationMode.isMeter(house.getWaterCalculationMethod());
    }

    private boolean resolveWaterMeterModeForNotification(Room room, @NonNull Invoice invoice) {
        House house = resolveHouseForRoom(room);
        if (house != null) {
            return WaterCalculationMode.isMeter(house.getWaterCalculationMethod());
        }

        Tenant contract = activeContractsByRoom.get(invoice.getRoomId());
        if (contract != null && contract.getMemberCount() > 0
                && invoice.getWaterStartReading() == 0
                && Math.abs(invoice.getWaterEndReading() - contract.getMemberCount()) < 0.001) {
            return false;
        }

        if (invoice.getWaterStartReading() == 0 && invoice.getWaterEndReading() == 1) {
            return false;
        }

        return true;
    }

    private boolean resolveElectricMeterModeForNotification(Room room, @NonNull Invoice invoice) {
        House house = resolveHouseForRoom(room);
        if (house != null) {
            return isElectricMeterMode(house.getElectricityCalculationMethod());
        }

        Tenant contract = activeContractsByRoom.get(invoice.getRoomId());
        if (contract != null && contract.getMemberCount() > 0
                && invoice.getElectricStartReading() == 0
                && Math.abs(invoice.getElectricEndReading() - contract.getMemberCount()) < 0.001) {
            return false;
        }

        if (invoice.getElectricStartReading() == 0 && invoice.getElectricEndReading() == 1) {
            return false;
        }

        return true;
    }

    private void showInvoiceExportDialog(Invoice h) {
        Room room = null;
        if (danhSachPhong != null) {
            for (Room r : danhSachPhong) {
                if (r.getId().equals(h.getRoomId())) {
                    room = r;
                    break;
                }
            }
        }
        House house = resolveHouseForRoom(room);
        InvoiceExportDialogHelper.showInvoiceExportDialog(
                this,
                this,
                h,
                house,
                paymentRepository,
                isTenantUser,
                this::openPaymentHistory,
                this::showTenantConfirmMeterDialog,
                this::showOwnerNoteDialog);
    }

    private void showTenantConfirmMeterDialog(Invoice h) {
        if (!isTenantUser)
            return;
        if (h == null)
            return;

        InvoiceExportDialogHelper.showTenantConfirmMeterDialog(
                this,
                h,
                this::scopedCollection);
    }

    private void showAddInvoiceDialog() {
        if (danhSachPhong.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_add_rooms_first), Toast.LENGTH_SHORT).show();
            return;
        }
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_invoice, null);
        Spinner spinnerPhong = dialogView.findViewById(R.id.spinnerPhong);
        InvoiceDialogSubmitHelper.FormRefs form = InvoiceDialogSubmitHelper.bind(dialogView);
        TextView tvEstimatedTotal = dialogView.findViewById(R.id.tvEstimatedTotal);

        form.applyMoneyFormatting();
        InvoiceDialogUiHelper.setupEstimatedTotal(form, tvEstimatedTotal,
                () -> {
                    int idx = spinnerPhong.getSelectedItemPosition();
                    if (idx < 0 || idx >= danhSachPhong.size())
                        return 0.0;
                    Room room = danhSachPhong.get(idx);
                    Tenant c = activeContractsByRoom.get(room.getId());
                    if (c != null && c.getRentAmount() > 0)
                        return (double) c.getRentAmount();
                    return room.getRentAmount();
                });

        InvoiceDialogUiHelper.bindRoomSpinner(this, spinnerPhong, danhSachPhong);
        form.etThangNam.setText(new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(new Date()));

        final double[] lastElecEnd = { 0 };
        final double[] lastWaterEnd = { 0 };
        final boolean[] isElectricMeterMode = { true };
        final boolean[] isWaterMeterMode = { true };
        final java.util.concurrent.atomic.AtomicReference<List<String>> selectedOtherFeeLines = new java.util.concurrent.atomic.AtomicReference<>(new ArrayList<>());
        spinnerPhong.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Room selectedRoom = danhSachPhong.get(position);
                Tenant activeContract = activeContractsByRoom.get(selectedRoom.getId());
                InvoiceDialogUiHelper.setMeterStartReadOnly(form);

                InvoicePeriodSuggestionHelper.suggestNextPeriodForRoom(InvoiceActivity.this::scopedCollection,
                        InvoiceActivity.this::toPeriodKey,
                        selectedRoom.getId(), suggested -> {
                            if (!suggested.isEmpty()) {
                                form.etThangNam.setText(suggested);
                            }
                        });

                InvoiceMeterHelper.loadLatestMeterEnds(InvoiceActivity.this::scopedCollection,
                        InvoiceActivity.this::toPeriodKey,
                        selectedRoom.getId(),
                        (elecEnd, waterEnd) -> {
                            String period = FinancePeriodUtil
                                    .normalizeMonthYear(form.etThangNam.getText() != null
                                            ? form.etThangNam.getText().toString().trim()
                                            : "");
                            if (period.isEmpty()) {
                                period = selectedMonth;
                            }
                            double initialElecStart = resolveInitialElectricStart(elecEnd, activeContract, period);
                            double initialWaterStart = resolveInitialWaterStart(waterEnd, activeContract, period);
                            lastElecEnd[0] = elecEnd;
                            lastWaterEnd[0] = waterEnd;
                            applyElectricInputMode(selectedRoom, form, initialElecStart, isElectricMeterMode);
                            applyWaterInputMode(selectedRoom, form, initialWaterStart, isWaterMeterMode);
                        });

                applyDefaultFeeFromHouse(selectedRoom,
                        activeContract,
                        form.etDonGiaDien,
                        form.etDonGiaNuoc,
                        form.etPhiRac,
                        form.etPhiWifi,
                        form.etPhiGuiXe,
                        form.etPhiKhac,
                        form.tvPhiKhacChiTiet);
                selectedOtherFeeLines.set(resolveSelectedExtraFeeLines(selectedRoom, activeContract));
                InvoiceDialogUiHelper.refreshEstimatedTotal(form, tvEstimatedTotal,
                        () -> {
                            Tenant c = activeContractsByRoom.get(selectedRoom.getId());
                            if (c != null && c.getRentAmount() > 0)
                                return (double) c.getRentAmount();
                            return selectedRoom.getRentAmount();
                        });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.create_invoice))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.create), (d, w) -> {
                    try {
                        int idx = spinnerPhong.getSelectedItemPosition();
                        if (idx < 0 || idx >= danhSachPhong.size()) {
                            Toast.makeText(this, getString(R.string.please_select_room), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Room selectedRoom = danhSachPhong.get(idx);
                        Tenant activeContract = activeContractsByRoom.get(selectedRoom.getId());
                        Invoice hd = InvoiceDialogSubmitHelper.buildNewInvoice(
                                selectedRoom,
                                activeContract,
                                form,
                                isElectricMeterMode[0] ? lastElecEnd[0] : 0,
                                isWaterMeterMode[0] ? lastWaterEnd[0] : 0,
                                getString(R.string.meter_start_must_gte_previous_end),
                                getString(R.string.meter_end_less_than_start));
                        hd.setOtherFeeLines(new ArrayList<>(selectedOtherFeeLines.get()));

                        viewModel.addInvoiceUnique(hd,
                                () -> {
                                double meterElecStart = isElectricMeterMode[0]
                                    ? hd.getElectricStartReading()
                                    : lastElecEnd[0];
                                double meterElecEnd = isElectricMeterMode[0]
                                    ? hd.getElectricEndReading()
                                    : lastElecEnd[0];
                                double meterWaterStart = isWaterMeterMode[0]
                                    ? hd.getWaterStartReading()
                                    : lastWaterEnd[0];
                                double meterWaterEnd = isWaterMeterMode[0]
                                    ? hd.getWaterEndReading()
                                    : lastWaterEnd[0];
                                    InvoiceMeterHelper.saveMeterReadingFromInvoice(this::scopedCollection,
                                            this::toPeriodKey,
                                    selectedRoom.getId(), hd.getBillingPeriod(), meterElecStart,
                                    meterElecEnd,
                                    meterWaterStart, meterWaterEnd);
                                    runOnUiThread(() -> Toast
                                            .makeText(this, getString(R.string.invoice_created_success),
                                                    Toast.LENGTH_SHORT)
                                            .show());
                                },
                                () -> runOnUiThread(() -> Toast
                                        .makeText(this, getString(R.string.invoice_period_exists), Toast.LENGTH_SHORT)
                                        .show()),
                                () -> runOnUiThread(() -> Toast
                                        .makeText(this, getString(R.string.failed_check_firebase), Toast.LENGTH_LONG)
                                        .show()));
                    } catch (InvoiceDialogSubmitHelper.ValidationException e) {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, getString(R.string.invalid_amount), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null).show();
    }

    private void showEditInvoiceDialog(Invoice invoice) {
        if (danhSachPhong.isEmpty()) {
            Toast.makeText(this, getString(R.string.failed_load_rooms), Toast.LENGTH_SHORT).show();
            return;
        }
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_invoice, null);
        Spinner spinnerPhong = dialogView.findViewById(R.id.spinnerPhong);
        InvoiceDialogSubmitHelper.FormRefs form = InvoiceDialogSubmitHelper.bind(dialogView);
        TextView tvEstimatedTotal = dialogView.findViewById(R.id.tvEstimatedTotal);

        form.applyMoneyFormatting();
        InvoiceDialogUiHelper.setupEstimatedTotal(form, tvEstimatedTotal,
                () -> {
                    int idx = spinnerPhong.getSelectedItemPosition();
                    if (idx < 0 || idx >= danhSachPhong.size())
                        return invoice.getRentAmount();
                    Room room = danhSachPhong.get(idx);
                    Tenant c = activeContractsByRoom.get(room.getId());
                    if (c != null && c.getRentAmount() > 0)
                        return (double) c.getRentAmount();
                    return room.getRentAmount();
                });

        InvoiceDialogUiHelper.bindRoomSpinner(this, spinnerPhong, danhSachPhong);

        int roomIndex = InvoiceDialogUiHelper.findRoomIndexById(danhSachPhong, invoice.getRoomId());
        if (roomIndex >= 0) {
            spinnerPhong.setSelection(roomIndex);
        }
        InvoiceDialogUiHelper.fillFormFromInvoice(form, invoice);

        if (roomIndex >= 0 && roomIndex < danhSachPhong.size()) {
            boolean[] editElectricMeterMode = { true };
            boolean[] editWaterMeterMode = { true };
            applyElectricInputMode(danhSachPhong.get(roomIndex), form, invoice.getElectricStartReading(),
                    editElectricMeterMode);
            applyWaterInputMode(danhSachPhong.get(roomIndex), form, invoice.getWaterStartReading(), editWaterMeterMode);
        }

        InvoiceDialogUiHelper.lockIdentityAndMeterStartFields(spinnerPhong, form);
        InvoiceDialogUiHelper.refreshEstimatedTotal(form, tvEstimatedTotal,
                () -> {
                    Tenant c = activeContractsByRoom.get(invoice.getRoomId());
                    if (c != null && c.getRentAmount() > 0)
                        return (double) c.getRentAmount();
                    return invoice.getRentAmount();
                });

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.edit_invoice_month, invoice.getBillingPeriod()))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.update), (d, w) -> {
                    try {
                        int idx = spinnerPhong.getSelectedItemPosition();
                        if (idx < 0 || idx >= danhSachPhong.size()) {
                            Toast.makeText(this, getString(R.string.please_select_room), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Room selectedRoom = danhSachPhong.get(idx);
                        Invoice updated = InvoiceDialogSubmitHelper.buildUpdatedInvoice(
                                invoice,
                                selectedRoom,
                            activeContractsByRoom.get(selectedRoom.getId()),
                            invoice.getOtherFeeLines(),
                                form,
                                getString(R.string.meter_end_less_than_start));
                        viewModel.updateInvoice(updated,
                            () -> runOnUiThread(() -> {
                                syncMeterReadingFromInvoice(updated);
                                Toast.makeText(this, getString(R.string.updated), Toast.LENGTH_SHORT)
                                    .show();
                            }),
                                () -> runOnUiThread(
                                        () -> Toast
                                                .makeText(this, getString(R.string.update_failed), Toast.LENGTH_SHORT)
                                                .show()));
                    } catch (InvoiceDialogSubmitHelper.ValidationException e) {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, getString(R.string.invalid_amount), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null).show();
    }

    private double calculateUnitMultiplier(String unit, Tenant contract) {
        if ("person".equalsIgnoreCase(unit)) {
            return contract != null ? Math.max(1, contract.getMemberCount()) : 1;
        }
        if ("vehicle".equalsIgnoreCase(unit)) {
            return contract != null ? Math.max(0, contract.getVehicleCount()) : 0;
        }
        return 1.0;
    }

    private double calculateFeeWithUnit(double price, String unit, Tenant contract) {
        if (price <= 0) return 0;
        return price * calculateUnitMultiplier(unit, contract);
    }

    private void applyDefaultFeeFromHouse(@NonNull Room room,
            Tenant activeContract,
            @NonNull EditText etDonGiaDien,
            @NonNull EditText etDonGiaNuoc,
            @NonNull EditText etPhiRac,
            @NonNull EditText etPhiWifi,
            @NonNull EditText etPhiGuiXe,
            @NonNull EditText etPhiKhac,
            TextView tvPhiKhacChiTiet) {
        House house = resolveHouseForRoom(room);
        if (house == null)
            return;

        FeePreset preset = resolveDefaultFeePreset(room, activeContract);

        if (preset.electricUnitPrice > 0) {
            MoneyFormatter.setValue(etDonGiaDien, preset.electricUnitPrice);
            lockFeeField(etDonGiaDien, true);
        } else {
            MoneyFormatter.setValue(etDonGiaDien, 0);
            lockFeeField(etDonGiaDien, false);
        }
        if (preset.waterUnitPrice > 0) {
            MoneyFormatter.setValue(etDonGiaNuoc, preset.waterUnitPrice);
            lockFeeField(etDonGiaNuoc, true);
        } else {
            MoneyFormatter.setValue(etDonGiaNuoc, 0);
            lockFeeField(etDonGiaNuoc, false);
        }
        if (preset.trashFee > 0) {
            MoneyFormatter.setValue(etPhiRac, preset.trashFee);
            lockFeeField(etPhiRac, true);
        } else {
            MoneyFormatter.setValue(etPhiRac, 0);
            lockFeeField(etPhiRac, false);
        }
        if (preset.wifiFee > 0) {
            MoneyFormatter.setValue(etPhiWifi, preset.wifiFee);
            lockFeeField(etPhiWifi, true);
        } else {
            MoneyFormatter.setValue(etPhiWifi, 0);
            lockFeeField(etPhiWifi, false);
        }
        if (preset.parkingFee > 0) {
            MoneyFormatter.setValue(etPhiGuiXe, preset.parkingFee);
            lockFeeField(etPhiGuiXe, true);
        } else {
            MoneyFormatter.setValue(etPhiGuiXe, 0);
            lockFeeField(etPhiGuiXe, false);
        }

        List<String> selectedOtherFeeLines = resolveSelectedExtraFeeLines(room, activeContract);
        double selectedOtherFee = preset.otherFee;
        MoneyFormatter.setValue(etPhiKhac, selectedOtherFee);
        lockFeeField(etPhiKhac, selectedOtherFee > 0);
        if (tvPhiKhacChiTiet != null) {
            if (selectedOtherFeeLines.isEmpty()) {
                tvPhiKhacChiTiet.setText("");
                tvPhiKhacChiTiet.setVisibility(View.GONE);
            } else {
                tvPhiKhacChiTiet.setText(String.join("\n", selectedOtherFeeLines));
                tvPhiKhacChiTiet.setVisibility(View.VISIBLE);
            }
        }
    }

    private FeePreset resolveDefaultFeePreset(@NonNull Room room, Tenant activeContract) {
        House house = resolveHouseForRoom(room);
        if (house == null) {
            return new FeePreset(0, 0, 0, 0, 0, 0, "kwh", WaterCalculationMode.METER);
        }

        double trashFee = calculateFeeWithUnit(house.getTrashPrice(), house.getTrashUnitSafe(), activeContract);
        
        double wifiFee = 0;
        if (activeContract == null || activeContract.hasInternetService()) {
            wifiFee = calculateFeeWithUnit(house.getInternetPrice(), house.getInternetUnitSafe(), activeContract);
        }

        double parkingFee = 0;
        if (activeContract == null || activeContract.hasParkingService()) {
            parkingFee = calculateFeeWithUnit(house.getParkingPrice(), house.getParkingUnitSafe(), activeContract);
        }

        return new FeePreset(
                Math.max(0, house.getElectricityPrice()),
                Math.max(0, house.getWaterPrice()),
                Math.max(0, trashFee),
                Math.max(0, wifiFee),
                Math.max(0, parkingFee),
                resolveSelectedExtraFeeTotal(room, activeContract),
                house.getElectricityCalculationMethod(),
                house.getWaterCalculationMethod());
    }

    private double resolveInitialElectricStart(double latestMeterEnd, Tenant contract, @NonNull String period) {
        if (isFirstBillingPeriod(contract, period) && contract != null) {
            return Math.max(0, contract.getElectricStartReading());
        }
        if (latestMeterEnd > 0) {
            return latestMeterEnd;
        }
        if (contract != null && contract.getElectricStartReading() > 0) {
            return contract.getElectricStartReading();
        }
        return Math.max(0, latestMeterEnd);
    }

    private double resolveInitialWaterStart(double latestMeterEnd, Tenant contract, @NonNull String period) {
        if (isFirstBillingPeriod(contract, period) && contract != null) {
            return Math.max(0, contract.getWaterStartReading());
        }
        if (latestMeterEnd > 0) {
            return latestMeterEnd;
        }
        if (contract != null && contract.getWaterStartReading() > 0) {
            return contract.getWaterStartReading();
        }
        return Math.max(0, latestMeterEnd);
    }

    private boolean isFirstBillingPeriod(Tenant contract, @NonNull String period) {
        if (contract == null) {
            return false;
        }
        String normalizedTarget = FinancePeriodUtil.normalizeMonthYear(period);
        String startPeriod = resolveBillingStartPeriod(contract);
        if (normalizedTarget.isEmpty() || startPeriod.isEmpty()) {
            return false;
        }
        return normalizedTarget.equals(startPeriod);
    }

    private double resolveSelectedExtraFeeTotal(@NonNull Room room, Tenant activeContract) {
        House house = resolveHouseForRoom(room);
        if (house == null) {
            return 0;
        }

        List<String> selectedExtraNames = activeContract != null ? activeContract.getSelectedExtraFeeNames() : null;
        boolean hasSelectionFilter = selectedExtraNames != null && !selectedExtraNames.isEmpty();

        double total = 0;

        boolean includeLaundry = activeContract == null || activeContract.hasLaundryService();
        if (includeLaundry) {
            total += calculateFeeWithUnit(house.getLaundryPrice(), house.getLaundryUnitSafe(), activeContract);
        }
        total += calculateFeeWithUnit(house.getElevatorPrice(), house.getElevatorUnitSafe(), activeContract);
        total += calculateFeeWithUnit(house.getCableTvPrice(), house.getCableTvUnitSafe(), activeContract);
        total += calculateFeeWithUnit(house.getServicePrice(), house.getServiceUnitSafe(), activeContract);

        if (house.getExtraFees() != null) {
            for (House.ExtraFee fee : house.getExtraFees()) {
                if (fee == null || fee.getPrice() <= 0) {
                    continue;
                }
                String feeName = fee.getFeeName() != null ? fee.getFeeName().trim() : "";
                if (feeName.isEmpty()) {
                    continue;
                }
                if (hasSelectionFilter && !containsNormalizedFeeName(selectedExtraNames, feeName)) {
                    continue;
                }
                total += calculateFeeWithUnit(fee.getPrice(), fee.getUnit(), activeContract);
            }
        }
        return Math.max(0, total);
    }

    private void hydrateInvoiceFeesFromHouseAndContractIfMissing(@NonNull Invoice invoice, Room room) {
        if (room == null || room.getId() == null || room.getId().trim().isEmpty()) {
            return;
        }

        Tenant activeContract = activeContractsByRoom.get(room.getId());
        if (activeContract == null && invoice.getContractId() != null && !invoice.getContractId().trim().isEmpty()) {
            String contractId = invoice.getContractId().trim();
            for (Tenant contract : activeContractsByRoom.values()) {
                if (contract != null && contractId.equals(contract.getId())) {
                    activeContract = contract;
                    break;
                }
            }
        }
        FeePreset preset = resolveDefaultFeePreset(room, activeContract);

        if (invoice.getElectricUnitPrice() <= 0 && preset.electricUnitPrice > 0) {
            invoice.setElectricUnitPrice(preset.electricUnitPrice);
        }
        if (invoice.getWaterUnitPrice() <= 0 && preset.waterUnitPrice > 0) {
            invoice.setWaterUnitPrice(preset.waterUnitPrice);
        }
        if (invoice.getTrashFee() <= 0 && preset.trashFee > 0) {
            invoice.setTrashFee(preset.trashFee);
        }
        if (invoice.getWifiFee() <= 0 && preset.wifiFee > 0) {
            invoice.setWifiFee(preset.wifiFee);
        }
        if (invoice.getParkingFee() <= 0 && preset.parkingFee > 0) {
            invoice.setParkingFee(preset.parkingFee);
        }

        if (invoice.getOtherFee() <= 0 && preset.otherFee > 0) {
            invoice.setOtherFee(preset.otherFee);
        }

        if (invoice.getOtherFee() > 0) {
            List<String> selectedOtherFeeLines = resolveSelectedExtraFeeLines(room, activeContract);
            if (!selectedOtherFeeLines.isEmpty()) {
                List<String> currentLines = invoice.getOtherFeeLines();
                if (currentLines == null || currentLines.isEmpty()) {
                    invoice.setOtherFeeLines(selectedOtherFeeLines);
                }
            }
        }
    }

    @NonNull
    private List<String> resolveSelectedExtraFeeLines(@NonNull Room room, Tenant activeContract) {
        List<String> lines = new ArrayList<>();
        House house = resolveHouseForRoom(room);
        if (house == null) {
            return lines;
        }

        List<String> selectedExtraNames = activeContract != null ? activeContract.getSelectedExtraFeeNames() : null;
        boolean hasSelectionFilter = selectedExtraNames != null && !selectedExtraNames.isEmpty();

        if ((activeContract == null || activeContract.hasLaundryService()) && house.getLaundryPrice() > 0) {
            lines.add(getString(R.string.laundry_service_name) + ": " + MoneyFormatter.format(calculateFeeWithUnit(house.getLaundryPrice(), house.getLaundryUnitSafe(), activeContract)));
        }
        if (house.getElevatorPrice() > 0) {
            lines.add(getString(R.string.house_fee_elevator_name) + ": " + MoneyFormatter.format(calculateFeeWithUnit(house.getElevatorPrice(), house.getElevatorUnitSafe(), activeContract)));
        }
        if (house.getCableTvPrice() > 0) {
            lines.add(getString(R.string.house_fee_cable_tv_name) + ": " + MoneyFormatter.format(calculateFeeWithUnit(house.getCableTvPrice(), house.getCableTvUnitSafe(), activeContract)));
        }
        if (house.getServicePrice() > 0) {
            lines.add(getString(R.string.house_fee_service_name) + ": " + MoneyFormatter.format(calculateFeeWithUnit(house.getServicePrice(), house.getServiceUnitSafe(), activeContract)));
        }

        if (house.getExtraFees() != null) {
            for (House.ExtraFee fee : house.getExtraFees()) {
                if (fee == null || fee.getPrice() <= 0) {
                    continue;
                }
                String feeName = fee.getFeeName() != null ? fee.getFeeName().trim() : "";
                if (feeName.isEmpty()) {
                    continue;
                }
                if (hasSelectionFilter && !containsNormalizedFeeName(selectedExtraNames, feeName)) {
                    continue;
                }
                lines.add(feeName + ": " + MoneyFormatter.format(calculateFeeWithUnit(fee.getPrice(), fee.getUnit(), activeContract)));
            }
        }
        return lines;
    }

    private boolean containsNormalizedFeeName(@NonNull List<String> selected, String target) {
        String normalizedTarget = normalizeFeeName(target);
        if (normalizedTarget.isEmpty()) {
            return false;
        }
        for (String item : selected) {
            if (normalizedTarget.equals(normalizeFeeName(item))) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private String normalizeFeeName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private void applyWaterInputMode(@NonNull Room room,
            @NonNull InvoiceDialogSubmitHelper.FormRefs form,
            double latestWaterEnd,
            @NonNull boolean[] isWaterMeterMode) {
        House house = resolveHouseForRoom(room);
        String waterMode = house != null ? house.getWaterCalculationMethod() : null;

        if (WaterCalculationMode.isPerPerson(waterMode)) {
            int memberCount = resolveMemberCount(room.getId());
            form.etNuocDau.setText("0");
            form.etNuocCuoi.setText(String.valueOf(memberCount));
            lockFeeField(form.etNuocDau, true);
            lockFeeField(form.etNuocCuoi, true);
            isWaterMeterMode[0] = false;
            return;
        }

        if (WaterCalculationMode.ROOM.equals(waterMode)) {
            form.etNuocDau.setText("0");
            form.etNuocCuoi.setText("1");
            lockFeeField(form.etNuocDau, true);
            lockFeeField(form.etNuocCuoi, true);
            isWaterMeterMode[0] = false;
            return;
        }

        form.etNuocDau.setText(InvoiceFormValueHelper.formatDouble(latestWaterEnd));
        if (form.etNuocCuoi.getText().toString().trim().isEmpty()) {
            form.etNuocCuoi.setText("");
        }
        lockFeeField(form.etNuocDau, true);
        lockFeeField(form.etNuocCuoi, false);
        isWaterMeterMode[0] = true;
    }

    private void applyElectricInputMode(@NonNull Room room,
            @NonNull InvoiceDialogSubmitHelper.FormRefs form,
            double latestElectricEnd,
            @NonNull boolean[] isElectricMeterMode) {
        House house = resolveHouseForRoom(room);
        String electricMode = house != null ? house.getElectricityCalculationMethod() : null;

        if (isElectricPerPersonMode(electricMode)) {
            int memberCount = resolveMemberCount(room.getId());
            form.etDienDau.setText("0");
            form.etDienCuoi.setText(String.valueOf(memberCount));
            lockFeeField(form.etDienDau, true);
            lockFeeField(form.etDienCuoi, true);
            isElectricMeterMode[0] = false;
            return;
        }

        if (isElectricRoomMode(electricMode)) {
            form.etDienDau.setText("0");
            form.etDienCuoi.setText("1");
            lockFeeField(form.etDienDau, true);
            lockFeeField(form.etDienCuoi, true);
            isElectricMeterMode[0] = false;
            return;
        }

        form.etDienDau.setText(InvoiceFormValueHelper.formatDouble(latestElectricEnd));
        if (form.etDienCuoi.getText().toString().trim().isEmpty()) {
            form.etDienCuoi.setText("");
        }
        lockFeeField(form.etDienDau, true);
        lockFeeField(form.etDienCuoi, false);
        isElectricMeterMode[0] = true;
    }

    private boolean isElectricPerPersonMode(String mode) {
        return mode != null && "per_person".equalsIgnoreCase(mode.trim());
    }

    private House resolveHouseForRoom(Room room) {
        if (room == null || housesById == null || housesById.isEmpty()) {
            return null;
        }

        String houseId = room.getHouseId() != null ? room.getHouseId().trim() : "";
        if (!houseId.isEmpty()) {
            House byId = housesById.get(houseId);
            if (byId != null) {
                return byId;
            }
        }

        String roomHouseName = room.getHouseName() != null ? room.getHouseName().trim() : "";
        if (roomHouseName.isEmpty()) {
            return null;
        }

        String normalizedRoomHouseName = roomHouseName.toLowerCase(Locale.ROOT);
        for (House house : housesById.values()) {
            if (house == null) {
                continue;
            }
            String houseName = house.getHouseName() != null ? house.getHouseName().trim() : "";
            if (!houseName.isEmpty() && normalizedRoomHouseName.equals(houseName.toLowerCase(Locale.ROOT))) {
                return house;
            }
        }

        for (House house : housesById.values()) {
            if (house == null) {
                continue;
            }
            String address = house.getAddress() != null ? house.getAddress().trim() : "";
            if (!address.isEmpty() && normalizedRoomHouseName.equals(address.toLowerCase(Locale.ROOT))) {
                return house;
            }
        }
        return null;
    }

    private boolean isElectricRoomMode(String mode) {
        return mode != null && "room".equalsIgnoreCase(mode.trim());
    }

    private boolean isElectricMeterMode(String mode) {
        return mode == null || mode.trim().isEmpty() || "kwh".equalsIgnoreCase(mode.trim());
    }

    private int resolveMemberCount(String roomId) {
        if (roomId == null || roomId.trim().isEmpty())
            return 0;
        Tenant active = activeContractsByRoom.get(roomId);
        return active != null ? Math.max(0, active.getMemberCount()) : 0;
    }

    private void lockFeeField(@NonNull EditText et, boolean locked) {
        et.setEnabled(!locked);
        et.setFocusable(!locked);
        et.setFocusableInTouchMode(!locked);
        et.setAlpha(locked ? 0.75f : 1f);
    }

    private String toPeriodKey(String period) {
        if (period == null)
            return "";
        String[] parts = period.trim().split("/");
        if (parts.length != 2)
            return "";
        try {
            int month = Integer.parseInt(parts[0]);
            int year = Integer.parseInt(parts[1]);
            if (month < 1 || month > 12)
                return "";
            return String.format(Locale.US, "%04d%02d", year, month);
        } catch (NumberFormatException e) {
            return "";
        }
    }

    @NonNull
    private String getLatestAllowedBillingMonth() {
        Calendar latestAllowed = Calendar.getInstance();
        latestAllowed.add(Calendar.MONTH, -1);
        return String.format(Locale.US, "%02d/%04d",
                latestAllowed.get(Calendar.MONTH) + 1,
                latestAllowed.get(Calendar.YEAR));
    }

    @NonNull
    private String normalizeToAllowedBillingMonth(String candidate) {
        String normalized = FinancePeriodUtil.normalizeMonthYear(candidate);
        String latestAllowed = getLatestAllowedBillingMonth();

        if (normalized.isEmpty()) {
            return latestAllowed;
        }

        String normalizedKey = toPeriodKey(normalized);
        String latestAllowedKey = toPeriodKey(latestAllowed);
        if (normalizedKey.isEmpty() || latestAllowedKey.isEmpty()) {
            return latestAllowed;
        }

        if (normalizedKey.compareTo(latestAllowedKey) > 0) {
            return latestAllowed;
        }
        return normalized;
    }

    private CollectionReference scopedCollection(String collection) {
        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId != null && !tenantId.isEmpty()) {
            return db.collection("tenants").document(tenantId).collection(collection);
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
            throw new IllegalStateException("User not logged in");
        return db.collection("users").document(user.getUid()).collection(collection);
    }

    private void applyInvoiceFilters(java.util.List<Invoice> list, int tabIndex) {
        if (list == null) {
            adapter.setDataList(new java.util.ArrayList<>());
            if (llEmpty != null)
                llEmpty.setVisibility(View.VISIBLE);
            return;
        }

        java.util.List<Invoice> out = InvoiceFilterCoordinator.filter(
                list,
                danhSachPhong,
                tenantDisplayByRoom,
                selectedMonth,
                selectedKhuId,
                searchQuery,
                tabIndex);

        adapter.setDataList(out);
        adapter.setTenantDisplayByRoom(tenantDisplayByRoom);
        adapter.setRoomAddressByRoom(roomAddressByRoom);
        adapter.setRoomElectricModeByRoom(roomElectricModeByRoom);
        adapter.setRoomWaterModeByRoom(roomWaterModeByRoom);
        adapter.setRoomMemberCountByRoom(roomMemberCountByRoom);
        if (tvFilterSummary != null) {
            tvFilterSummary.setText(InvoiceFilterCoordinator.buildSummaryText(this, out));
        }
        adapter.setCurrentTab(tabIndex);
        if (llEmpty != null)
            llEmpty.setVisibility(out.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showMonthFilterDialog() {
        InvoiceFilterDialogHelper.showMonthFilterDialog(this, selectedMonth, (period, month, year) -> {
            selectedMonth = normalizeToAllowedBillingMonth(String.format(Locale.US, "%02d/%04d", month, year));
            if (tvSelectedMonth != null) {
                String[] parts = selectedMonth.split("/");
                if (parts.length == 2) {
                    tvSelectedMonth.setText(Integer.parseInt(parts[0]) + "/" + parts[1]);
                } else {
                    tvSelectedMonth.setText(selectedMonth);
                }
            }
            applyInvoiceFilters(cachedInvoices, selectedTabIndex);
            maybeAutoCreateDraftInvoicesForSelectedMonth();
        });
    }

    private void showHouseFilterDialog() {
        LinkedHashMap<String, String> houseMap = new LinkedHashMap<>();
        houseMap.put("", getString(R.string.all_houses));

        if (danhSachPhong != null) {
            for (Room room : danhSachPhong) {
                if (room == null || room.getHouseId() == null || room.getHouseId().trim().isEmpty()) {
                    continue;
                }
                String houseId = room.getHouseId();
                if (houseMap.containsKey(houseId)) {
                    continue;
                }
                String label = buildHouseDisplayLabel(houseId, room.getHouseName());
                houseMap.put(houseId, label);
            }
        }

        List<String> ids = new ArrayList<>(houseMap.keySet());
        List<String> labels = new ArrayList<>(houseMap.values());
        int checked = Math.max(0, ids.indexOf(selectedKhuId == null ? "" : selectedKhuId));

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.select_house))
                .setSingleChoiceItems(labels.toArray(new String[0]), checked, (dialog, which) -> {
                    String houseId = ids.get(which);
                    String label = labels.get(which);
                    selectedKhuId = (houseId == null || houseId.isEmpty()) ? null : houseId;
                    if (tvSelectedKhu != null) {
                        tvSelectedKhu.setText(label);
                    }
                    applyInvoiceFilters(cachedInvoices, selectedTabIndex);
                    dialog.dismiss();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    @NonNull
    private String buildHouseDisplayLabel(String houseId, String fallbackHouseName) {
        House house = houseId != null ? housesById.get(houseId) : null;
        if (house != null && house.getAddress() != null) {
            String address = house.getAddress().trim();
            if (!address.isEmpty()) {
                return address;
            }
        }

        if (fallbackHouseName != null && !fallbackHouseName.trim().isEmpty()) {
            return fallbackHouseName.trim();
        }

        if (house != null && house.getHouseName() != null) {
            String houseName = house.getHouseName().trim();
            if (!houseName.isEmpty()) {
                return houseName;
            }
        }

        return getString(R.string.house);
    }

    private void openPaymentHistory(@NonNull Invoice invoice) {
        android.content.Intent intent = new android.content.Intent(InvoiceActivity.this, PaymentHistoryActivity.class);
        intent.putExtra("INVOICE_ID", invoice.getId());
        intent.putExtra("INVOICE_TOTAL", invoice.getTotalAmount());
        intent.putExtra("ROOM_ID", invoice.getRoomId());
        intent.putExtra("TITLE",
                getString(R.string.payment_title_format, invoice.getRoomNumber(), invoice.getBillingPeriod()));
        startActivity(intent);
    }

    private void notifyRoomTenantsInvoiceReported(@NonNull Invoice invoice) {
        String tenantId = TenantSession.getActiveTenantId();
        String roomId = invoice.getRoomId();
        if (tenantId == null || tenantId.trim().isEmpty() || roomId == null || roomId.trim().isEmpty()) {
            return;
        }

        db.collection("tenants").document(tenantId)
                .collection("members")
                .whereEqualTo("role", TenantRoles.TENANT)
                .whereEqualTo("status", "ACTIVE")
                .whereEqualTo("roomId", roomId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || snapshot.isEmpty()) {
                        return;
                    }

                    Timestamp now = Timestamp.now();
                    WriteBatch batch = db.batch();

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String recipientUid = doc.getString("uid");
                        if (recipientUid == null || recipientUid.trim().isEmpty()) {
                            recipientUid = doc.getId();
                        }
                        if (recipientUid == null || recipientUid.trim().isEmpty()) {
                            continue;
                        }

                        Map<String, Object> payload = new HashMap<>();
                        payload.put("userId", recipientUid);
                        payload.put("type", "INVOICE_REPORTED");
                        payload.put("title", getString(R.string.invoice_reported_notification_title));
                        payload.put("body", getString(R.string.invoice_reported_notification_body,
                                invoice.getRoomNumber() != null ? invoice.getRoomNumber() : "--",
                                invoice.getBillingPeriod() != null ? invoice.getBillingPeriod() : "--"));
                        payload.put("invoiceId", invoice.getId());
                        payload.put("roomId", roomId);
                        payload.put("billingPeriod", invoice.getBillingPeriod());
                        payload.put("isRead", false);
                        payload.put("createdAt", now);
                        payload.put("pushState", "PENDING_SERVER_DISPATCH");

                        batch.set(db.collection("tenants").document(tenantId)
                                .collection("notifications")
                                .document(UUID.randomUUID().toString()), payload);
                    }

                    batch.commit();
                });
    }

    private void recomputeAndUpdateInvoiceStatus(@NonNull Invoice invoice) {
        InvoicePaymentFlowHelper.recomputeAndUpdateInvoiceStatus(
                invoice,
                this::scopedCollection,
                viewModel);
    }

    private void showCollectPaymentDialog(Invoice invoice) {
        InvoicePaymentFlowHelper.showCollectPaymentDialog(
                this,
                db,
                invoice,
                this::scopedCollection,
                paymentRepository,
                viewModel);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
