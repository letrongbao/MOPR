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
import com.example.myapplication.domain.House;
import com.example.myapplication.domain.Invoice;
import com.example.myapplication.domain.Tenant;
import com.example.myapplication.domain.Room;
import com.example.myapplication.core.repository.domain.PaymentRepository;
import com.example.myapplication.viewmodel.InvoiceViewModel;
import com.example.myapplication.viewmodel.RoomViewModel;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import android.widget.AdapterView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
        final String waterMode;

        FeePreset(double electricUnitPrice, double waterUnitPrice, double trashFee, double wifiFee, double parkingFee,
                String waterMode) {
            this.electricUnitPrice = electricUnitPrice;
            this.waterUnitPrice = waterUnitPrice;
            this.trashFee = trashFee;
            this.wifiFee = wifiFee;
            this.parkingFee = parkingFee;
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
    private FloatingActionButton fabThem;
    private FloatingActionButton fabChotKy;

    private String selectedMonth;
    private String selectedKhuId;
    private String searchQuery;
    private int selectedTabIndex;
    private List<Invoice> cachedInvoices = new ArrayList<>();
    private Map<String, String> tenantDisplayByRoom = new HashMap<>();
    private Map<String, Tenant> activeContractsByRoom = new HashMap<>();
    private Map<String, House> housesById = new HashMap<>();

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
        fabChotKy = findViewById(R.id.fabChotKy);
        fabThem = findViewById(R.id.fabThem);
        selectedMonth = FinancePeriodUtil
                .normalizeMonthYear(new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(new Date()));
        selectedKhuId = null;
        searchQuery = "";
        selectedTabIndex = 0;
        if (tvSelectedMonth != null) {
            selectedMonth = new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(new Date());
            tvSelectedMonth.setText(new SimpleDateFormat("M/yyyy", Locale.getDefault()).format(new Date()));
        }
        if (tvSelectedKhu != null) {
            tvSelectedKhu.setText(getString(R.string.all_houses));
        }

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        final java.util.concurrent.atomic.AtomicInteger tabIdx = new java.util.concurrent.atomic.AtomicInteger(0);
        if (tabLayout != null) {
            tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.not_reported)));
            tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.reported)));
            tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.partially_paid)));
            tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.paid)));
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
                if (InvoiceStatus.PAID.equals(st) || InvoiceStatus.PARTIAL.equals(st)) {
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
            applyInvoiceFilters(safe, selectedTabIndex);
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
        if (fabChotKy != null) {
            fabChotKy.setOnClickListener(v -> showClosePeriodOptions());
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
        viewModel.getInvoiceList().observe(this, onInvoicesChanged::accept);
        observeRoomList();
    }

    private void applyTenantSelfServiceMode(@NonNull String roomId, @NonNull InvoiceListConsumer onInvoicesChanged) {
        isTenantUser = true;
        adapter.setTenantMode(true);
        if (btnSelectKhu != null)
            btnSelectKhu.setVisibility(View.GONE);
        if (fabThem != null)
            fabThem.setVisibility(View.GONE);
        if (fabChotKy != null)
            fabChotKy.setVisibility(View.GONE);
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
    }

    private void observeRoomList() {
        new ViewModelProvider(this).get(RoomViewModel.class)
                .getRoomList().observe(this, list -> danhSachPhong = list);
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
                });

        scopedCollection("houses")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null)
                        return;
                    Map<String, House> map = new HashMap<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        House house = doc.toObject(House.class);
                        if (house == null)
                            continue;
                        house.setId(doc.getId());
                        map.put(doc.getId(), house);
                    }
                    housesById = map;
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
                        String display = getString(R.string.tenant_colon)
                                + (name.isEmpty() ? getString(R.string.updating) : name)
                                + (phone.isEmpty() ? "" : getString(R.string.phone_separator) + phone);
                        map.put(n.getRoomId(), display);
                    }
                    tenantDisplayByRoom = map;
                    adapter.setTenantDisplayByRoom(tenantDisplayByRoom);
                    applyInvoiceFilters(cachedInvoices, selectedTabIndex);
                });
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
            String name = room.getHouseName();
            if (name == null || name.trim().isEmpty())
                name = getString(R.string.house);
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
            FeePreset fee = resolveDefaultFeePreset(room);

            InvoiceMeterHelper.loadLatestMeterEnds(this::scopedCollection, this::toPeriodKey, room.getId(),
                    (elecEnd, waterEnd) -> {
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

                        hd.setElectricStartReading(elecEnd);
                        hd.setElectricEndReading(elecEnd);
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
                        hd.setStatus(InvoiceStatus.UNREPORTED);

                        viewModel.addInvoiceUnique(hd,
                                () -> {
                                    created[0]++;
                                    double meterWaterStart = WaterCalculationMode.isMeter(fee.waterMode)
                                        ? hd.getWaterStartReading()
                                        : waterEnd;
                                    double meterWaterEnd = WaterCalculationMode.isMeter(fee.waterMode)
                                        ? hd.getWaterEndReading()
                                        : waterEnd;
                                    InvoiceMeterHelper.saveMeterReadingFromInvoice(this::scopedCollection,
                                            this::toPeriodKey,
                                        room.getId(), period, elecEnd, elecEnd, meterWaterStart, meterWaterEnd);
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

    private interface ActiveContractsCallback {
        void onDone(@NonNull Map<String, Tenant> activeContractsByRoom);
    }

    private interface AutoTargetsCallback {
        void onDone(@NonNull List<AutoCreateTarget> targets);
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

                        if (existingRoomIds.contains(roomId))
                            continue;

                        targets.add(new AutoCreateTarget(room, contract));
                    }

                    callback.onDone(targets);
                })
                .addOnFailureListener(e -> callback.onDone(new ArrayList<>()));
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
                        Invoice hd = new Invoice();
                        hd.setRoomId(room.getId());
                        hd.setRoomNumber(room.getRoomNumber());
                        hd.setRentAmount(room.getRentAmount());
                        hd.setBillingPeriod(period);
                        if (contract != null && contract.getId() != null && !contract.getId().trim().isEmpty()) {
                            hd.setContractId(contract.getId());
                        }

                        hd.setElectricStartReading(elecEnd);
                        hd.setElectricEndReading(elecEnd);
                        hd.setElectricUnitPrice(electricUnitPrice);
                        FeePreset roomFeePreset = resolveDefaultFeePreset(room);
                        if (WaterCalculationMode.isPerPerson(roomFeePreset.waterMode)) {
                            int memberCount = contract != null ? Math.max(0, contract.getMemberCount()) : 0;
                            hd.setWaterStartReading(0);
                            hd.setWaterEndReading(memberCount);
                        } else if (WaterCalculationMode.ROOM.equals(roomFeePreset.waterMode)) {
                            hd.setWaterStartReading(0);
                            hd.setWaterEndReading(1);
                        } else {
                            hd.setWaterStartReading(waterEnd);
                            hd.setWaterEndReading(waterEnd);
                        }
                        hd.setWaterUnitPrice(waterUnitPrice);
                        hd.setTrashFee(trashFee);
                        hd.setWifiFee(wifiFee);
                        hd.setParkingFee(parkingFee);
                        hd.setStatus(InvoiceStatus.UNREPORTED);

                        viewModel.addInvoiceUnique(hd,
                                () -> {
                                    created[0]++;
                                    double meterWaterStart = WaterCalculationMode.isMeter(roomFeePreset.waterMode)
                                        ? hd.getWaterStartReading()
                                        : waterEnd;
                                    double meterWaterEnd = WaterCalculationMode.isMeter(roomFeePreset.waterMode)
                                        ? hd.getWaterEndReading()
                                        : waterEnd;
                                    InvoiceMeterHelper.saveMeterReadingFromInvoice(this::scopedCollection,
                                            this::toPeriodKey,
                                        room.getId(), period, elecEnd, elecEnd, meterWaterStart, meterWaterEnd);
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
        InvoiceFeeNotificationHelper.showFeeNotificationDialog(
                this,
                invoice,
                viewModel,
                this::showInvoiceExportDialog);
    }

    private void showInvoiceExportDialog(Invoice h) {
        InvoiceExportDialogHelper.showInvoiceExportDialog(
                this,
                this,
                h,
                paymentRepository,
                isTenantUser,
                this::openPaymentHistory,
                this::showTenantConfirmMeterDialog);
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
        final boolean[] isWaterMeterMode = { true };
        spinnerPhong.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Room selectedRoom = danhSachPhong.get(position);
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
                            lastElecEnd[0] = elecEnd;
                            lastWaterEnd[0] = waterEnd;
                            form.etDienDau.setText(InvoiceFormValueHelper.formatDouble(elecEnd));
                            applyWaterInputMode(selectedRoom, form, waterEnd, isWaterMeterMode);

                            if (form.etDienCuoi.getText().toString().trim().isEmpty()) {
                                form.etDienCuoi.setText(InvoiceFormValueHelper.formatDouble(elecEnd));
                            }
                        });

                applyDefaultFeeFromHouse(selectedRoom, form.etDonGiaDien, form.etDonGiaNuoc, form.etPhiRac,
                        form.etPhiWifi, form.etPhiGuiXe);
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
                                lastElecEnd[0],
                            isWaterMeterMode[0] ? lastWaterEnd[0] : 0,
                                getString(R.string.meter_start_must_gte_previous_end),
                                getString(R.string.meter_end_less_than_start));

                        viewModel.addInvoiceUnique(hd,
                                () -> {
                                double meterWaterStart = isWaterMeterMode[0] ? hd.getWaterStartReading()
                                    : lastWaterEnd[0];
                                double meterWaterEnd = isWaterMeterMode[0] ? hd.getWaterEndReading() : lastWaterEnd[0];
                                    InvoiceMeterHelper.saveMeterReadingFromInvoice(this::scopedCollection,
                                            this::toPeriodKey,
                                            selectedRoom.getId(), hd.getBillingPeriod(), hd.getElectricStartReading(),
                                            hd.getElectricEndReading(),
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
            boolean[] editWaterMeterMode = { true };
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
                                form,
                                getString(R.string.meter_end_less_than_start));
                        viewModel.updateInvoice(updated,
                                () -> runOnUiThread(
                                        () -> Toast.makeText(this, getString(R.string.updated), Toast.LENGTH_SHORT)
                                                .show()),
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

    private void applyDefaultFeeFromHouse(@NonNull Room room,
            @NonNull EditText etDonGiaDien,
            @NonNull EditText etDonGiaNuoc,
            @NonNull EditText etPhiRac,
            @NonNull EditText etPhiWifi,
            @NonNull EditText etPhiGuiXe) {
        if (room.getHouseId() == null || room.getHouseId().trim().isEmpty())
            return;

        House house = housesById.get(room.getHouseId());
        if (house == null)
            return;

        if (house.getElectricityPrice() > 0) {
            MoneyFormatter.setValue(etDonGiaDien, house.getElectricityPrice());
            lockFeeField(etDonGiaDien, true);
        } else {
            lockFeeField(etDonGiaDien, false);
        }
        if (house.getWaterPrice() > 0) {
            MoneyFormatter.setValue(etDonGiaNuoc, house.getWaterPrice());
            lockFeeField(etDonGiaNuoc, true);
        } else {
            lockFeeField(etDonGiaNuoc, false);
        }
        if (house.getTrashPrice() > 0) {
            MoneyFormatter.setValue(etPhiRac, house.getTrashPrice());
            lockFeeField(etPhiRac, true);
        } else {
            lockFeeField(etPhiRac, false);
        }
        if (house.getInternetPrice() > 0) {
            MoneyFormatter.setValue(etPhiWifi, house.getInternetPrice());
            lockFeeField(etPhiWifi, true);
        } else {
            lockFeeField(etPhiWifi, false);
        }
        if (house.getParkingPrice() > 0) {
            MoneyFormatter.setValue(etPhiGuiXe, house.getParkingPrice());
            lockFeeField(etPhiGuiXe, true);
        } else {
            lockFeeField(etPhiGuiXe, false);
        }
    }

    private FeePreset resolveDefaultFeePreset(@NonNull Room room) {
        if (room.getHouseId() == null || room.getHouseId().trim().isEmpty()) {
            return new FeePreset(0, 0, 0, 0, 0, WaterCalculationMode.METER);
        }
        House house = housesById.get(room.getHouseId());
        if (house == null) {
            return new FeePreset(0, 0, 0, 0, 0, WaterCalculationMode.METER);
        }
        return new FeePreset(
                Math.max(0, house.getElectricityPrice()),
                Math.max(0, house.getWaterPrice()),
                Math.max(0, house.getTrashPrice()),
                Math.max(0, house.getInternetPrice()),
                Math.max(0, house.getParkingPrice()),
                house.getWaterCalculationMethod());
    }

    private void applyWaterInputMode(@NonNull Room room,
            @NonNull InvoiceDialogSubmitHelper.FormRefs form,
            double latestWaterEnd,
            @NonNull boolean[] isWaterMeterMode) {
        House house = room.getHouseId() != null ? housesById.get(room.getHouseId()) : null;
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
            form.etNuocCuoi.setText(InvoiceFormValueHelper.formatDouble(latestWaterEnd));
        }
        lockFeeField(form.etNuocDau, true);
        lockFeeField(form.etNuocCuoi, false);
        isWaterMeterMode[0] = true;
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
        if (tvFilterSummary != null) {
            tvFilterSummary.setText(InvoiceFilterCoordinator.buildSummaryText(this, out));
        }
        adapter.setCurrentTab(tabIndex);
        if (llEmpty != null)
            llEmpty.setVisibility(out.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showMonthFilterDialog() {
        InvoiceFilterDialogHelper.showMonthFilterDialog(this, selectedMonth, (picked, month, year) -> {
            selectedMonth = picked;
            if (tvSelectedMonth != null) {
                tvSelectedMonth.setText(month + "/" + year);
            }
            applyInvoiceFilters(cachedInvoices, selectedTabIndex);
        });
    }

    private void showHouseFilterDialog() {
        InvoiceFilterDialogHelper.showHouseFilterDialog(this, danhSachPhong, selectedKhuId, (houseId, label) -> {
            selectedKhuId = (houseId == null || houseId.isEmpty()) ? null : houseId;
            if (tvSelectedKhu != null) {
                tvSelectedKhu.setText(label);
            }
            applyInvoiceFilters(cachedInvoices, selectedTabIndex);
        });
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
