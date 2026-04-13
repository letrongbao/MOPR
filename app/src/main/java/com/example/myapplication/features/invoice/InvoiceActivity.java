package com.example.myapplication.features.invoice;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.core.constants.InvoiceStatus;
import com.example.myapplication.R;
import com.example.myapplication.core.constants.RoomStatus;
import com.example.myapplication.core.constants.TenantRoles;
import com.example.myapplication.core.constants.WaterCalculationMode;
import com.example.myapplication.core.service.ImageUploadService;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.FinancePeriodUtil;
import com.example.myapplication.core.util.MoneyFormatter;
import com.example.myapplication.core.util.ScreenUiHelper;
import com.example.myapplication.features.contract.ContractDateHelper;
import com.example.myapplication.domain.House;
import com.example.myapplication.domain.Invoice;
import com.example.myapplication.domain.Payment;
import com.example.myapplication.domain.Tenant;
import com.example.myapplication.domain.Room;
import com.example.myapplication.core.repository.domain.PaymentRepository;
import com.example.myapplication.viewmodel.InvoiceViewModel;
import com.example.myapplication.viewmodel.RoomViewModel;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
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
import java.text.ParseException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Collections;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

public class InvoiceActivity extends AppCompatActivity {

    private static final boolean ENABLE_TENANT_SELF_SERVICE = true;
    public static final String EXTRA_INITIAL_TAB = "EXTRA_INITIAL_TAB";
    public static final String EXTRA_OPEN_INVOICE_ID = "EXTRA_OPEN_INVOICE_ID";
    public static final int TAB_UNREPORTED = 0;
    public static final int TAB_REPORTED = 1;
    public static final int TAB_PAID = 2;

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
    private RecyclerView recyclerView;
    private View deepLinkLoadingView;
    private List<Room> danhSachPhong = new ArrayList<>();

    private TextView tvSelectedMonth;
    private TextView tvSelectedKhu;
    private TextView tvFilterSummary;
    private TabLayout tabLayout;
    private EditText etSearchInvoice;
    private View btnSelectKhu;
    private View btnDatePicker;
    private View layoutInvoiceSearchSection;

    private String selectedMonth;
    private String selectedKhuId;
    private String searchQuery;
    private int selectedTabIndex;
    private boolean tenantAllTimeMode;
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

    private enum TenantInvoiceSortOption {
        AMOUNT_DESC,
        AMOUNT_ASC,
        UNPAID,
        PAID
    }

    private TenantInvoiceSortOption tenantSortOption = TenantInvoiceSortOption.AMOUNT_DESC;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final PaymentRepository paymentRepository = new PaymentRepository();
    private ActivityResultLauncher<String> transferProofPickerLauncher;
    private BroadcastReceiver transferProofUploadReceiver;
    private Uri pendingTransferProofUri;
    private String pendingTransferProofUrl;
    private ImageView pendingTransferProofPreview;
    private Invoice pendingTransferProofInvoice;
    private String pendingOpenInvoiceId;
    private boolean hasOpenedDeepLinkedInvoice;
    private boolean hasHandledMissingDeepLinkInvoice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        transferProofPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) {
                        return;
                    }
                    pendingTransferProofUri = uri;
                    if (pendingTransferProofPreview != null) {
                        pendingTransferProofPreview.setImageURI(uri);
                    }
                    startTransferProofUpload(uri);
                });

        transferProofUploadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context context, Intent intent) {
                String imageUrl = intent.getStringExtra(ImageUploadService.EXTRA_IMAGE_URL);
                if (imageUrl == null || imageUrl.trim().isEmpty()) {
                    return;
                }
                pendingTransferProofUrl = imageUrl.trim();
                if (pendingTransferProofPreview != null) {
                    Glide.with(InvoiceActivity.this).load(pendingTransferProofUrl).into(pendingTransferProofPreview);
                }
            }
        };

        LocalBroadcastManager.getInstance(this).registerReceiver(
                transferProofUploadReceiver,
                new IntentFilter(ImageUploadService.ACTION_UPLOAD_COMPLETE));

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
        recyclerView = findViewById(R.id.recyclerView);
        deepLinkLoadingView = findViewById(R.id.layoutDeepLinkLoading);

        tvSelectedMonth = findViewById(R.id.tvSelectedMonth);
        tvSelectedKhu = findViewById(R.id.tvSelectedKhu);
        tvFilterSummary = findViewById(R.id.tvFilterSummary);
        etSearchInvoice = findViewById(R.id.etSearchInvoice);
        btnSelectKhu = findViewById(R.id.btnSelectKhu);
        btnDatePicker = findViewById(R.id.btnDatePicker);
        layoutInvoiceSearchSection = findViewById(R.id.layoutInvoiceSearchSection);
        selectedMonth = normalizeToAllowedBillingMonth(
                new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(new Date()));
        selectedKhuId = null;
        searchQuery = "";
        tenantAllTimeMode = true;
        selectedTabIndex = resolveRequestedInitialTab();
        pendingOpenInvoiceId = resolveRequestedInvoiceId();
        updateDeepLinkLoading(pendingOpenInvoiceId != null);
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

        tabLayout = findViewById(R.id.tabLayout);
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
                    showCollectPaymentDialog(invoice);
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
            maybeOpenInvoiceFromDeepLink(safe);
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

            if (selectedTabIndex >= TAB_UNREPORTED && selectedTabIndex <= TAB_PAID) {
                TabLayout.Tab initialTab = tabLayout.getTabAt(selectedTabIndex);
                if (initialTab != null) {
                    initialTab.select();
                }
            }
        }

        setupFilterListeners();
    }

    private void setupFilterListeners() {
        if (btnSelectKhu != null) {
            btnSelectKhu.setOnClickListener(v -> {
                if (isTenantUser) {
                    showTenantTimeFilterDialog();
                } else {
                    showHouseFilterDialog();
                }
            });
        }
        if (btnDatePicker != null) {
            btnDatePicker.setOnClickListener(v -> {
                if (isTenantUser) {
                    showTenantSortDialog();
                } else {
                    showMonthFilterDialog();
                }
            });
        }
        if (etSearchInvoice != null && !isTenantUser) {
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

    private void applyRoleSpecificChrome() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (isTenantUser) {
            if (appBarLayout != null) {
                appBarLayout.setBackgroundResource(R.drawable.bg_tenant_header_teal);
            }
            if (toolbar != null) {
                toolbar.setBackgroundResource(R.drawable.bg_tenant_header_teal);
            }
            if (tabLayout != null) {
                tabLayout.setBackgroundResource(R.drawable.bg_tenant_header_teal);
                tabLayout.setVisibility(View.GONE);
            }
            ScreenUiHelper.setupBackToolbar(this, toolbar, getString(R.string.tenant_invoice_title));
            if (btnSelectKhu != null) {
                btnSelectKhu.setVisibility(View.VISIBLE);
            }
            if (btnDatePicker != null) {
                btnDatePicker.setVisibility(View.VISIBLE);
            }
            if (etSearchInvoice != null) {
                etSearchInvoice.setVisibility(View.GONE);
            }
            if (layoutInvoiceSearchSection != null) {
                layoutInvoiceSearchSection.setVisibility(View.GONE);
            }
            if (tvFilterSummary != null) {
                tvFilterSummary.setVisibility(View.GONE);
            }
            updateTenantFilterUi();
            return;
        }

        if (appBarLayout != null) {
            appBarLayout.setBackgroundResource(R.color.primary);
        }
        if (toolbar != null) {
            toolbar.setBackgroundResource(R.color.primary);
        }
        if (tabLayout != null) {
            tabLayout.setBackgroundResource(R.color.primary);
            tabLayout.setVisibility(View.VISIBLE);
        }
        ScreenUiHelper.setupBackToolbar(this, toolbar, getString(R.string.invoice_statistics));
        if (btnSelectKhu != null) {
            btnSelectKhu.setVisibility(View.VISIBLE);
        }
        if (btnDatePicker != null) {
            btnDatePicker.setVisibility(View.VISIBLE);
        }
        if (etSearchInvoice != null) {
            etSearchInvoice.setVisibility(View.VISIBLE);
            etSearchInvoice.setHint(getString(R.string.invoice_search_hint));
        }
        if (layoutInvoiceSearchSection != null) {
            layoutInvoiceSearchSection.setVisibility(View.VISIBLE);
        }
        if (tvFilterSummary != null) {
            tvFilterSummary.setVisibility(View.VISIBLE);
        }
        if (tvSelectedKhu != null) {
            tvSelectedKhu.setText(getString(R.string.all_houses));
        }
    }

    private void updateTenantFilterUi() {
        if (!isTenantUser) {
            return;
        }

        if (tvSelectedKhu != null) {
            if (tenantAllTimeMode) {
                tvSelectedKhu.setText(getString(R.string.tenant_invoice_time_all));
            } else {
                tvSelectedKhu.setText(selectedMonth);
            }
        }

        if (btnDatePicker != null) {
            btnDatePicker.setVisibility(tenantAllTimeMode ? View.VISIBLE : View.GONE);
        }

        if (tvSelectedMonth != null) {
            if (tenantAllTimeMode) {
                tvSelectedMonth.setVisibility(View.VISIBLE);
                tvSelectedMonth.setText(getTenantSortLabel(tenantSortOption));
            } else {
                tvSelectedMonth.setVisibility(View.GONE);
            }
        }
    }

    private String getTenantSortLabel(@NonNull TenantInvoiceSortOption sort) {
        switch (sort) {
            case AMOUNT_DESC:
                return getString(R.string.tenant_invoice_sort_amount_desc);
            case AMOUNT_ASC:
                return getString(R.string.tenant_invoice_sort_amount_asc);
            case UNPAID:
                return getString(R.string.invoice_status_unpaid_tenant);
            case PAID:
                return getString(R.string.invoice_status_paid_tenant);
            default:
                return getString(R.string.tenant_invoice_sort_amount_desc);
        }
    }

    private int resolveRequestedInitialTab() {
        Intent intent = getIntent();
        if (intent == null) {
            return TAB_UNREPORTED;
        }
        int tab = intent.getIntExtra(EXTRA_INITIAL_TAB, TAB_UNREPORTED);
        if (tab < TAB_UNREPORTED || tab > TAB_PAID) {
            return TAB_UNREPORTED;
        }
        return tab;
    }

    private String resolveRequestedInvoiceId() {
        Intent intent = getIntent();
        if (intent == null) {
            return null;
        }
        String invoiceId = intent.getStringExtra(EXTRA_OPEN_INVOICE_ID);
        if (invoiceId == null || invoiceId.trim().isEmpty()) {
            return null;
        }
        return invoiceId.trim();
    }

    private void maybeOpenInvoiceFromDeepLink(@NonNull List<Invoice> invoices) {
        if (hasOpenedDeepLinkedInvoice || pendingOpenInvoiceId == null || pendingOpenInvoiceId.isEmpty()) {
            updateDeepLinkLoading(false);
            return;
        }

        Invoice matched = null;
        for (Invoice invoice : invoices) {
            if (invoice == null || invoice.getId() == null) {
                continue;
            }
            if (pendingOpenInvoiceId.equals(invoice.getId().trim())) {
                matched = invoice;
                break;
            }
        }

        if (matched == null) {
            if (!hasHandledMissingDeepLinkInvoice && !invoices.isEmpty()) {
                hasHandledMissingDeepLinkInvoice = true;
                pendingOpenInvoiceId = null;
                updateDeepLinkLoading(false);
                Toast.makeText(this, getString(R.string.invoice_open_from_notification_not_found), Toast.LENGTH_SHORT)
                        .show();
            }
            return;
        }

        hasOpenedDeepLinkedInvoice = true;
        pendingOpenInvoiceId = null;
        updateDeepLinkLoading(false);

        String status = matched.getStatus() != null ? matched.getStatus().trim() : InvoiceStatus.UNREPORTED;
        if (InvoiceStatus.PAID.equalsIgnoreCase(status)) {
            switchToPaidTab();
        } else if (InvoiceStatus.REPORTED.equalsIgnoreCase(status) || "PARTIAL".equalsIgnoreCase(status)) {
            switchToReportedTab();
        }

        highlightInvoiceCard(matched.getId());

        showInvoiceExportDialog(matched);
    }

    private void updateDeepLinkLoading(boolean loading) {
        if (deepLinkLoadingView != null) {
            deepLinkLoadingView.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    private void highlightInvoiceCard(String invoiceId) {
        if (recyclerView == null || invoiceId == null || invoiceId.trim().isEmpty()) {
            return;
        }

        String safeInvoiceId = invoiceId.trim();
        recyclerView.post(() -> {
            int target = adapter.findPositionByInvoiceId(safeInvoiceId);
            if (target < 0) {
                return;
            }
            recyclerView.scrollToPosition(target);
            adapter.setHighlightedInvoiceId(safeInvoiceId);
            recyclerView.postDelayed(adapter::clearHighlightedInvoice, 2200L);
        });
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
        applyRoleSpecificChrome();
        adapter.setTenantMode(false);
        viewModel.getInvoiceList().observe(this, onInvoicesChanged::accept);
        refreshTenantDisplayData();
        refreshAutoFillSources();
        observeRoomList();
        maybeAutoCreateDraftInvoicesForSelectedMonth();
    }

    private void applyTenantSelfServiceMode(@NonNull String roomId, @NonNull InvoiceListConsumer onInvoicesChanged) {
        isTenantUser = true;
        applyRoleSpecificChrome();
        adapter.setTenantMode(true);

        viewModel.getInvoicesByRoom(roomId).observe(this, onInvoicesChanged::accept);
        refreshTenantDisplayData();
        refreshAutoFillSources();
    }

    private void applyOwnerStaffMode(@NonNull InvoiceListConsumer onInvoicesChanged) {
        isTenantUser = false;
        applyRoleSpecificChrome();
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
                        Tenant c = safeTenantFromContractDoc(doc);
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
                        Tenant n = safeTenantFromContractDoc(doc);
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
            if (invoice.getInternetFee() <= 0 && preset.wifiFee > 0) {
                invoice.setInternetFee(preset.wifiFee);
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
            double oldWifiFee = invoice.getInternetFee();
            double oldParkingFee = invoice.getParkingFee();
            double oldOtherFee = invoice.getOtherFee();
            int oldOtherFeeLineSize = invoice.getOtherFeeLines() != null ? invoice.getOtherFeeLines().size() : 0;

            hydrateInvoiceFeesFromHouseAndContractIfMissing(invoice, room);

            int newOtherFeeLineSize = invoice.getOtherFeeLines() != null ? invoice.getOtherFeeLines().size() : 0;
            boolean changed = Math.abs(invoice.getTrashFee() - oldTrashFee) > 0.001
                    || Math.abs(invoice.getInternetFee() - oldWifiFee) > 0.001
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
                                    Toast.makeText(this,
                                        getString(R.string.exceeded_invoice_quota, String.valueOf(max)),
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
                        hd.setInternetFee(fee.wifiFee);
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
                        hd.setInternetFee(fee.wifiFee);
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
                        Tenant c = safeTenantFromContractDoc(doc);
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

    private Tenant safeTenantFromContractDoc(@NonNull QueryDocumentSnapshot doc) {
        try {
            Tenant tenant = new Tenant();
            tenant.setId(doc.getId());
            tenant.setFullName(asStringFlexible(doc.get("fullName")));
            tenant.setPersonalId(asStringFlexible(doc.get("personalId")));
            tenant.setPhoneNumber(asStringFlexible(doc.get("phoneNumber")));
            tenant.setAddress(asStringFlexible(doc.get("address")));
            tenant.setContractNumber(asStringFlexible(doc.get("contractNumber")));
            tenant.setRepresentativeName(asStringFlexible(doc.get("representativeName")));
            tenant.setRepresentativeId(asStringFlexible(doc.get("representativeId")));
            tenant.setRoomId(asStringFlexible(doc.get("roomId")));
            tenant.setPreviousRoomId(asStringFlexible(doc.get("previousRoomId")));
            tenant.setRoomNumber(asStringFlexible(doc.get("roomNumber")));
            tenant.setContractStatus(asStringFlexible(doc.get("contractStatus")));
            tenant.setRentalStartDate(asStringFlexible(doc.get("rentalStartDate")));
            tenant.setContractEndDate(asStringFlexible(doc.get("contractEndDate")));
            tenant.setBillingStartPolicy(asStringFlexible(doc.get("billingStartPolicy")));
            tenant.setBillingStartPeriod(asStringFlexible(doc.get("billingStartPeriod")));
            tenant.setBillingReminderAt(asStringFlexible(doc.get("billingReminderAt")));

            tenant.setMemberCount(asIntFlexible(doc.get("memberCount")));
            tenant.setContractDurationMonths(asIntFlexible(doc.get("contractDurationMonths")));
            tenant.setElectricStartReading(asIntFlexible(doc.get("electricStartReading")));
            tenant.setWaterStartReading(asIntFlexible(doc.get("waterStartReading")));
            tenant.setVehicleCount(asIntFlexible(doc.get("vehicleCount")));

            long rentAmount = asLongFlexible(doc.get("rentAmount"));
            if (rentAmount > 0) {
                tenant.setRentAmount(rentAmount);
            } else {
                tenant.setRoomPrice(asDoubleFlexible(doc.get("roomPrice")));
            }

            long depositAmount = asLongFlexible(doc.get("depositAmount"));
            if (depositAmount > 0) {
                tenant.setDepositAmount(depositAmount);
            } else {
                tenant.setLegacyDepositAmount(asDoubleFlexible(doc.get("legacyDepositAmount")));
            }

            tenant.setContractEndTimestamp(asLongFlexible(doc.get("contractEndTimestamp")));

            tenant.setShowDepositOnInvoice(asBooleanFlexible(doc.get("showDepositOnInvoice"), true));
            tenant.setShowNoteOnInvoice(asBooleanFlexible(doc.get("showNoteOnInvoice"), true));
            tenant.setRemindOneMonthBefore(asBooleanFlexible(doc.get("remindOneMonthBefore"), true));
            tenant.setHasParkingService(asBooleanFlexible(doc.get("hasParkingService"), false));
            tenant.setHasInternetService(asBooleanFlexible(doc.get("hasInternetService"), false));
            tenant.setHasLaundryService(asBooleanFlexible(doc.get("hasLaundryService"), false));
            tenant.setDepositCollected(asBooleanFlexible(doc.get("depositCollectionStatus"), false));
            tenant.setPrimaryContact(asBooleanFlexible(doc.get("isPrimaryContact"), false));
            tenant.setContractRepresentative(asBooleanFlexible(doc.get("contractRepresentative"), false));
            tenant.setTemporaryResident(asBooleanFlexible(doc.get("temporaryResident"), false));
            tenant.setFullyDocumented(asBooleanFlexible(doc.get("fullyDocumented"), false));

            tenant.setCreatedAt(asLongFlexible(doc.get("createdAt")));
            tenant.setUpdatedAt(asLongFlexible(doc.get("updatedAt")));
            tenant.setEndedAt(asLongFlexible(doc.get("endedAt")));
            tenant.setSelectedExtraFeeNames(asStringListFlexible(doc.get("selectedExtraFeeNames")));
            tenant.setNote(asStringFlexible(doc.get("note")));

            return tenant;
        } catch (Exception ex) {
            android.util.Log.w("InvoiceActivity", "Skip invalid contract doc: " + doc.getId(), ex);
            return null;
        }
    }

    private long asLongFlexible(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof Double) {
            return Math.round((Double) value);
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toDate().getTime();
        }
        if (value instanceof Date) {
            return ((Date) value).getTime();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong(((String) value).trim());
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    private int asIntFlexible(Object value) {
        long parsed = asLongFlexible(value);
        if (parsed > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (parsed < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) parsed;
    }

    private double asDoubleFlexible(Object value) {
        if (value == null) {
            return 0D;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble(((String) value).trim());
            } catch (NumberFormatException ignored) {
                return 0D;
            }
        }
        return 0D;
    }

    private boolean asBooleanFlexible(Object value, boolean fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            String normalized = ((String) value).trim().toLowerCase(Locale.US);
            if ("true".equals(normalized) || "1".equals(normalized)) {
                return true;
            }
            if ("false".equals(normalized) || "0".equals(normalized)) {
                return false;
            }
        }
        return fallback;
    }

    private String asStringFlexible(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String) {
            return ((String) value).trim();
        }
        return String.valueOf(value).trim();
    }

    @NonNull
    private List<String> asStringListFlexible(Object value) {
        List<String> out = new ArrayList<>();
        if (!(value instanceof List)) {
            return out;
        }
        List<?> rawList = (List<?>) value;
        for (Object raw : rawList) {
            String item = asStringFlexible(raw);
            if (!item.isEmpty()) {
                out.add(item);
            }
        }
        return out;
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
                        hd.setInternetFee(contract != null && contract.hasInternetService() ? calculatedWifi : 0);
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
                            switchToReportedTab();
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

    private void switchToReportedTab() {
        selectedTabIndex = 1;
        if (tabLayout != null) {
            TabLayout.Tab reportedTab = tabLayout.getTabAt(1);
            if (reportedTab != null) {
                reportedTab.select();
                return;
            }
        }
        applyInvoiceFilters(cachedInvoices, selectedTabIndex);
    }

    private void switchToPaidTab() {
        selectedTabIndex = 2;
        if (tabLayout != null) {
            TabLayout.Tab paidTab = tabLayout.getTabAt(2);
            if (paidTab != null) {
                paidTab.select();
                return;
            }
        }
        applyInvoiceFilters(cachedInvoices, selectedTabIndex);
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
        if (h == null) {
            return;
        }
        Room room = null;
        if (danhSachPhong != null) {
            for (Room r : danhSachPhong) {
                if (r == null) {
                    continue;
                }
                String roomId = r.getId();
                if (roomId != null && roomId.equals(h.getRoomId())) {
                    room = r;
                    break;
                }
            }
        }
        House house = resolveHouseForRoom(room);
        String resolvedElectricMode = roomElectricModeByRoom.get(h.getRoomId());
        if (resolvedElectricMode == null || resolvedElectricMode.trim().isEmpty()) {
            Integer memberCount = roomMemberCountByRoom.get(h.getRoomId());
            if (memberCount != null
                    && memberCount > 0
                    && Math.abs(h.getElectricStartReading()) < 0.001
                    && Math.abs(h.getElectricEndReading() - memberCount) < 0.001) {
                resolvedElectricMode = "per_person";
            } else if (Math.abs(h.getElectricStartReading()) < 0.001
                    && Math.abs(h.getElectricEndReading() - 1.0) < 0.001) {
                resolvedElectricMode = "room";
            } else {
                resolvedElectricMode = "kwh";
            }
        }

        String resolvedWaterMode = roomWaterModeByRoom.get(h.getRoomId());
        if (resolvedWaterMode == null || resolvedWaterMode.trim().isEmpty()) {
            Integer memberCount = roomMemberCountByRoom.get(h.getRoomId());
            if (memberCount != null
                    && memberCount > 0
                    && Math.abs(h.getWaterStartReading()) < 0.001
                    && Math.abs(h.getWaterEndReading() - memberCount) < 0.001) {
                resolvedWaterMode = WaterCalculationMode.PER_PERSON;
            } else if (Math.abs(h.getWaterStartReading()) < 0.001
                    && Math.abs(h.getWaterEndReading() - 1.0) < 0.001) {
                resolvedWaterMode = WaterCalculationMode.ROOM;
            } else {
                resolvedWaterMode = WaterCalculationMode.METER;
            }
        }

        InvoiceExportDialogHelper.showInvoiceExportDialog(
                this,
                this,
                h,
                house,
                resolvedElectricMode,
                resolvedWaterMode,
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
        if (invoice.getInternetFee() <= 0 && preset.wifiFee > 0) {
            invoice.setInternetFee(preset.wifiFee);
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

        if (isTenantUser) {
            java.util.List<Invoice> tenantOut = filterTenantInvoices(list);
            adapter.setDataList(tenantOut);
            adapter.setTenantDisplayByRoom(tenantDisplayByRoom);
            adapter.setRoomAddressByRoom(roomAddressByRoom);
            adapter.setRoomElectricModeByRoom(roomElectricModeByRoom);
            adapter.setRoomWaterModeByRoom(roomWaterModeByRoom);
            adapter.setRoomMemberCountByRoom(roomMemberCountByRoom);
            adapter.setCurrentTab(TAB_UNREPORTED);
            if (llEmpty != null)
                llEmpty.setVisibility(tenantOut.isEmpty() ? View.VISIBLE : View.GONE);
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

    private List<Invoice> filterTenantInvoices(@NonNull List<Invoice> source) {
        List<Invoice> out = new ArrayList<>();
        String selectedMonthNormalized = FinancePeriodUtil.normalizeMonthYear(selectedMonth);

        for (Invoice invoice : source) {
            if (invoice == null) {
                continue;
            }
            if (!tenantAllTimeMode) {
                String invoiceMonth = FinancePeriodUtil.normalizeMonthYear(invoice.getBillingPeriod());
                if (!selectedMonthNormalized.equals(invoiceMonth)) {
                    continue;
                }
            }

            String normalizedStatus = normalizeInvoiceStatus(invoice.getStatus());
            if (tenantSortOption == TenantInvoiceSortOption.UNPAID
                    && InvoiceStatus.PAID.equals(normalizedStatus)) {
                continue;
            }
            if (tenantSortOption == TenantInvoiceSortOption.PAID
                    && !InvoiceStatus.PAID.equals(normalizedStatus)) {
                continue;
            }

            out.add(invoice);
        }

        Collections.sort(out, (a, b) -> compareTenantInvoices(a, b));
        return out;
    }

    private int compareTenantInvoices(@NonNull Invoice a, @NonNull Invoice b) {
        switch (tenantSortOption) {
            case AMOUNT_DESC:
                return Double.compare(getInvoiceDisplayTotal(b), getInvoiceDisplayTotal(a));
            case AMOUNT_ASC:
                return Double.compare(getInvoiceDisplayTotal(a), getInvoiceDisplayTotal(b));
            case UNPAID:
            case PAID:
            default:
                return getInvoicePeriodKey(b).compareTo(getInvoicePeriodKey(a));
        }
    }

    @NonNull
    private String getInvoicePeriodKey(@NonNull Invoice invoice) {
        String normalized = FinancePeriodUtil.normalizeMonthYear(invoice.getBillingPeriod());
        String key = toPeriodKey(normalized);
        return key == null ? "" : key;
    }

    private double getInvoiceDisplayTotal(@NonNull Invoice invoice) {
        if (invoice.getTotalAmount() > 0) {
            return invoice.getTotalAmount();
        }

        double electricUsage = Math.max(0, invoice.getElectricEndReading() - invoice.getElectricStartReading());
        double waterUsage = Math.max(0, invoice.getWaterEndReading() - invoice.getWaterStartReading());
        return invoice.getRentAmount()
                + electricUsage * invoice.getElectricUnitPrice()
                + waterUsage * invoice.getWaterUnitPrice()
                + invoice.getTrashFee()
                + invoice.getInternetFee()
                + invoice.getParkingFee()
                + invoice.getOtherFee();
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

    private void showTenantTimeFilterDialog() {
        String[] options = new String[] {
                getString(R.string.tenant_invoice_time_all),
            selectedMonth
        };
        int checked = tenantAllTimeMode ? 0 : 1;

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.tenant_invoice_filter_time_title))
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    if (which == 0) {
                        tenantAllTimeMode = true;
                        updateTenantFilterUi();
                        applyInvoiceFilters(cachedInvoices, selectedTabIndex);
                        dialog.dismiss();
                    } else {
                        dialog.dismiss();
                        InvoiceFilterDialogHelper.showMonthFilterDialog(this, selectedMonth, (period, month, year) -> {
                            tenantAllTimeMode = false;
                            selectedMonth = normalizeToAllowedBillingMonth(
                                    String.format(Locale.US, "%02d/%04d", month, year));
                            updateTenantFilterUi();
                            applyInvoiceFilters(cachedInvoices, selectedTabIndex);
                        });
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showTenantSortDialog() {
        TenantInvoiceSortOption[] values = TenantInvoiceSortOption.values();
        String[] labels = new String[] {
                getString(R.string.tenant_invoice_sort_amount_desc),
                getString(R.string.tenant_invoice_sort_amount_asc),
            getString(R.string.invoice_status_unpaid_tenant),
            getString(R.string.invoice_status_paid_tenant)
        };

        int checked = tenantSortOption.ordinal();
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.tenant_invoice_sort_title))
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    tenantSortOption = values[which];
                    updateTenantFilterUi();
                    applyInvoiceFilters(cachedInvoices, selectedTabIndex);
                    dialog.dismiss();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
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
        Room room = findRoomById(invoice.getRoomId());
        String resolvedRoomName = invoice.getRoomNumber();
        if ((resolvedRoomName == null || resolvedRoomName.trim().isEmpty()) && room != null
                && room.getRoomNumber() != null) {
            resolvedRoomName = room.getRoomNumber().trim();
        }

        String resolvedHouseName = null;
        if (room != null) {
            resolvedHouseName = buildHouseDisplayLabel(room.getHouseId(), room.getHouseName());
        }

        intent.putExtra("INVOICE_ID", invoice.getId());
        intent.putExtra("INVOICE_TOTAL", invoice.getTotalAmount());
        intent.putExtra("ROOM_ID", invoice.getRoomId());
        intent.putExtra("CONTRACT_ID", invoice.getContractId());
        intent.putExtra("ROOM_NAME", resolvedRoomName);
        intent.putExtra("HOUSE_NAME", resolvedHouseName);
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
        if (isTenantUser) {
            showTenantTransferSubmitDialog(invoice);
            return;
        }
        showOwnerCollectPaymentDialog(invoice);
    }

    private void showTenantTransferSubmitDialog(@NonNull Invoice invoice) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_tenant_transfer_submit, null);
        Spinner spinnerTransferMethod = dialogView.findViewById(R.id.spinnerTransferPaymentMethod);
        View layoutTransferInfo = dialogView.findViewById(R.id.layoutTransferInfo);
        View layoutTransferProofSection = dialogView.findViewById(R.id.layoutTransferProofSection);
        TextView tvTransferInfoLabel = dialogView.findViewById(R.id.tvTransferInfoLabel);
        TextView tvTransferAccountNo = dialogView.findViewById(R.id.tvTransferAccountNo);
        TextView tvTransferBankName = dialogView.findViewById(R.id.tvTransferBankName);
        TextView tvTransferAccountName = dialogView.findViewById(R.id.tvTransferAccountName);
        TextView tvTransferContent = dialogView.findViewById(R.id.tvTransferContent);
        TextView tvTransferAmount = dialogView.findViewById(R.id.tvTransferAmount);
        TextView tvTransferQrEmpty = dialogView.findViewById(R.id.tvTransferQrEmpty);
        TextView tvTransferProofLabel = dialogView.findViewById(R.id.tvTransferProofLabel);
        EditText etNote = dialogView.findViewById(R.id.etTransferProofNote);
        ImageView imgProof = dialogView.findViewById(R.id.imgTransferProof);
        ImageView imgTransferQr = dialogView.findViewById(R.id.imgTransferQr);
        MaterialButton btnPickImage = dialogView.findViewById(R.id.btnPickTransferProof);

        pendingTransferProofInvoice = invoice;
        pendingTransferProofPreview = imgProof;
        pendingTransferProofUrl = null;
        pendingTransferProofUri = null;
        final double[] computedTransferAmount = new double[] { Math.max(0, invoice.getTotalAmount()) };
        final boolean[] receiverInfoReady = new boolean[] { false };
        final boolean[] useBankTransfer = new boolean[] { true };

        ArrayAdapter<String> methodAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[] { getString(R.string.bank_transfer), getString(R.string.cash) });
        methodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTransferMethod.setAdapter(methodAdapter);

        String roomLabel = invoice.getRoomNumber() != null ? invoice.getRoomNumber() : "--";
        String periodLabel = invoice.getBillingPeriod() != null ? invoice.getBillingPeriod() : "--";
        tvTransferContent.setText(getString(R.string.transfer_content_value, roomLabel, periodLabel));

        Runnable applyPaymentMethodUi = () -> {
            boolean isBankTransfer = useBankTransfer[0];
            if (layoutTransferInfo != null) {
                layoutTransferInfo.setVisibility(isBankTransfer ? View.VISIBLE : View.GONE);
            }
            if (layoutTransferProofSection != null) {
                layoutTransferProofSection.setVisibility(isBankTransfer ? View.VISIBLE : View.GONE);
            }
            if (tvTransferInfoLabel != null) {
                tvTransferInfoLabel.setText(isBankTransfer
                        ? getString(R.string.transfer_info_label)
                        : getString(R.string.amount_colon));
            }
            if (tvTransferProofLabel != null) {
                tvTransferProofLabel.setText(isBankTransfer
                        ? getString(R.string.transfer_proof_screenshot_label_required)
                        : getString(R.string.transfer_proof_screenshot_label));
            }
            if (etNote != null) {
                etNote.setHint(isBankTransfer
                        ? getString(R.string.transfer_proof_note_hint_transfer)
                        : getString(R.string.transfer_proof_note_hint_cash));
            }
        };

        spinnerTransferMethod.setSelection(0);
        spinnerTransferMethod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                useBankTransfer[0] = position == 0;
                applyPaymentMethodUi.run();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                useBankTransfer[0] = true;
                applyPaymentMethodUi.run();
            }
        });
        applyPaymentMethodUi.run();

        scopedCollection("payments")
                .whereEqualTo("invoiceId", invoice.getId())
                .get()
                .addOnSuccessListener(qs -> {
                    double paid = 0;
                    if (qs != null) {
                        for (QueryDocumentSnapshot doc : qs) {
                            Double amt = doc.getDouble("amount");
                            if (amt != null) {
                                paid += amt;
                            }
                        }
                    }
                    double remaining = Math.max(0, invoice.getTotalAmount() - paid);
                    double amount = remaining > 0 ? remaining : invoice.getTotalAmount();
                    computedTransferAmount[0] = Math.max(0, amount);
                    tvTransferAmount.setText(getString(R.string.transfer_amount_value,
                            MoneyFormatter.format(amount)));

                    if (!useBankTransfer[0]) {
                        receiverInfoReady[0] = true;
                        return;
                    }

                    loadTransferReceiverInfo(invoice, receiverInfo -> {
                        if (receiverInfo == null || receiverInfo.isMissingCoreInfo()) {
                            receiverInfoReady[0] = false;
                            tvTransferAccountNo.setText(getString(R.string.transfer_account_no_value, "--"));
                            tvTransferBankName.setText(getString(R.string.transfer_bank_name_value, "--"));
                            tvTransferAccountName.setText(getString(R.string.transfer_account_holder_value, "--"));
                            if (imgTransferQr != null) {
                                imgTransferQr.setVisibility(View.GONE);
                            }
                            if (tvTransferQrEmpty != null) {
                                tvTransferQrEmpty.setVisibility(View.VISIBLE);
                            }
                            Toast.makeText(this, getString(R.string.transfer_proof_missing_bank_info), Toast.LENGTH_SHORT)
                                    .show();
                            return;
                        }

                        receiverInfoReady[0] = true;

                        tvTransferAccountNo.setText(getString(R.string.transfer_account_no_value,
                                receiverInfo.bankAccountNo));
                        tvTransferBankName.setText(getString(R.string.transfer_bank_name_value,
                                receiverInfo.bankName));
                        tvTransferAccountName.setText(getString(R.string.transfer_account_holder_value,
                                receiverInfo.bankAccountName));

                        if (imgTransferQr != null) {
                            if (receiverInfo.paymentQrUrl != null && !receiverInfo.paymentQrUrl.trim().isEmpty()) {
                                imgTransferQr.setVisibility(View.VISIBLE);
                                Glide.with(this).load(receiverInfo.paymentQrUrl.trim()).into(imgTransferQr);
                                if (tvTransferQrEmpty != null) {
                                    tvTransferQrEmpty.setVisibility(View.GONE);
                                }
                            } else {
                                imgTransferQr.setVisibility(View.GONE);
                                if (tvTransferQrEmpty != null) {
                                    tvTransferQrEmpty.setVisibility(View.VISIBLE);
                                }
                            }
                        }
                    });
                });

        btnPickImage.setOnClickListener(v -> transferProofPickerLauncher.launch("image/*"));

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.transfer_proof_submit_title))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.send), (d, w) -> {
                    double amount = Math.max(0, computedTransferAmount[0]);
                    if (amount <= 0) {
                        Toast.makeText(this, getString(R.string.invalid_amount), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (useBankTransfer[0] && !receiverInfoReady[0]) {
                        Toast.makeText(this, getString(R.string.transfer_proof_missing_bank_info), Toast.LENGTH_SHORT)
                                .show();
                        return;
                    }

                    if (useBankTransfer[0] && (pendingTransferProofUrl == null || pendingTransferProofUrl.trim().isEmpty())) {
                        Toast.makeText(this, getString(R.string.transfer_proof_missing_image), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String selectedMethod = useBankTransfer[0] ? "BANK" : "CASH";
                    String proofImageUrl = useBankTransfer[0]
                            ? (pendingTransferProofUrl != null ? pendingTransferProofUrl.trim() : null)
                            : null;

                    invoice.setPaymentMethod(selectedMethod);
                    invoice.setTransferProofPending(true);
                    invoice.setTransferProofImageUrl(proofImageUrl);
                    invoice.setTransferProofAmount(amount);
                    invoice.setTransferProofNote(etNote.getText() != null ? etNote.getText().toString().trim() : "");
                    invoice.setTransferProofSubmittedAt(Timestamp.now());

                    viewModel.updateInvoice(invoice,
                            () -> runOnUiThread(() -> {
                                int successMessage = useBankTransfer[0]
                                        ? R.string.transfer_proof_submit_success
                                        : R.string.cash_proof_submit_success;
                                Toast.makeText(this, getString(successMessage), Toast.LENGTH_SHORT)
                                        .show();
                                applyInvoiceFilters(cachedInvoices, selectedTabIndex);
                            }),
                            () -> runOnUiThread(() -> Toast.makeText(this, getString(R.string.update_failed),
                                    Toast.LENGTH_SHORT).show()));
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private interface TransferReceiverCallback {
        void onResolved(TransferReceiverInfo info);
    }

    private static final class TransferReceiverInfo {
        final String bankAccountNo;
        final String bankName;
        final String bankAccountName;
        final String paymentQrUrl;

        TransferReceiverInfo(String bankAccountNo, String bankName, String bankAccountName, String paymentQrUrl) {
            this.bankAccountNo = safeTrim(bankAccountNo);
            this.bankName = safeTrim(bankName);
            this.bankAccountName = safeTrim(bankAccountName);
            this.paymentQrUrl = safeTrim(paymentQrUrl);
        }

        boolean isMissingCoreInfo() {
            return bankAccountNo.isEmpty() || bankName.isEmpty() || bankAccountName.isEmpty();
        }

        private static String safeTrim(String value) {
            return value == null ? "" : value.trim();
        }
    }

    private void loadTransferReceiverInfo(@NonNull Invoice invoice, @NonNull TransferReceiverCallback callback) {
        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId == null || tenantId.trim().isEmpty()) {
            callback.onResolved(new TransferReceiverInfo("", "", "", ""));
            return;
        }

        String roomId = invoice.getRoomId();
        if (roomId == null || roomId.trim().isEmpty()) {
            loadOwnerReceiverInfo(tenantId.trim(), callback);
            return;
        }

        scopedCollection("rooms").document(roomId.trim()).get()
                .addOnSuccessListener(roomDoc -> {
                    String houseId = roomDoc != null && roomDoc.exists() ? roomDoc.getString("houseId") : null;
                    if (houseId == null || houseId.trim().isEmpty()) {
                        loadOwnerReceiverInfo(tenantId.trim(), callback);
                        return;
                    }
                    loadManagerReceiverInfo(tenantId.trim(), houseId.trim(), callback);
                })
                .addOnFailureListener(e -> loadOwnerReceiverInfo(tenantId.trim(), callback));
    }

    private void loadManagerReceiverInfo(@NonNull String tenantId,
            @NonNull String houseId,
            @NonNull TransferReceiverCallback callback) {
        scopedCollection("members")
                .whereEqualTo("role", TenantRoles.STAFF)
                .whereEqualTo("status", "ACTIVE")
                .whereArrayContains("assignedHouseIds", houseId)
                .limit(1)
                .get()
                .addOnSuccessListener(memberQs -> {
                    if (memberQs == null || memberQs.isEmpty()) {
                        loadOwnerReceiverInfo(tenantId, callback);
                        return;
                    }

                    String managerUid = memberQs.getDocuments().get(0).getId();
                    if (managerUid == null || managerUid.trim().isEmpty()) {
                        loadOwnerReceiverInfo(tenantId, callback);
                        return;
                    }

                    db.collection("users").document(managerUid.trim()).get()
                            .addOnSuccessListener(userDoc -> {
                                TransferReceiverInfo managerInfo = userDocToTransferInfo(userDoc, true);
                                if (managerInfo == null || managerInfo.isMissingCoreInfo()) {
                                    loadOwnerReceiverInfo(tenantId, callback);
                                } else {
                                    callback.onResolved(managerInfo);
                                }
                            })
                            .addOnFailureListener(e -> loadOwnerReceiverInfo(tenantId, callback));
                })
                .addOnFailureListener(e -> loadOwnerReceiverInfo(tenantId, callback));
    }

    private void loadOwnerReceiverInfo(@NonNull String tenantId, @NonNull TransferReceiverCallback callback) {
        db.collection("tenants").document(tenantId).get()
                .addOnSuccessListener(tenantDoc -> {
                    String tenantBankAccountNo = safeTrim(tenantDoc.getString("bankAccountNo"));
                    String tenantBankNameRaw = safeTrim(tenantDoc.getString("bankName"));
                    if (tenantBankNameRaw.isEmpty()) {
                        tenantBankNameRaw = safeTrim(tenantDoc.getString("bankCode"));
                    }
                    final String tenantBankName = tenantBankNameRaw;
                    String tenantBankAccountName = safeTrim(tenantDoc.getString("bankAccountName"));
                    String tenantQrUrl = safeTrim(tenantDoc.getString("paymentQrUrl"));

                    String ownerUid = safeTrim(tenantDoc.getString("ownerUid"));
                    if (ownerUid.isEmpty()) {
                        ownerUid = tenantId;
                    }

                    db.collection("users").document(ownerUid).get()
                            .addOnSuccessListener(userDoc -> {
                                TransferReceiverInfo fromOwnerUser = userDocToTransferInfo(userDoc, true);
                                if (fromOwnerUser != null && !fromOwnerUser.isMissingCoreInfo()) {
                                    String mergedQr = fromOwnerUser.paymentQrUrl;
                                    if (mergedQr.isEmpty()) {
                                        mergedQr = tenantQrUrl;
                                    }
                                    callback.onResolved(new TransferReceiverInfo(
                                            fromOwnerUser.bankAccountNo,
                                            fromOwnerUser.bankName,
                                            fromOwnerUser.bankAccountName,
                                            mergedQr));
                                    return;
                                }

                                callback.onResolved(new TransferReceiverInfo(
                                        tenantBankAccountNo,
                                        tenantBankName,
                                        tenantBankAccountName,
                                        tenantQrUrl));
                            })
                            .addOnFailureListener(e -> callback.onResolved(new TransferReceiverInfo(
                                    tenantBankAccountNo,
                                    tenantBankName,
                                    tenantBankAccountName,
                                    tenantQrUrl)));
                })
                .addOnFailureListener(e -> callback.onResolved(new TransferReceiverInfo("", "", "", "")));
    }

    private TransferReceiverInfo userDocToTransferInfo(com.google.firebase.firestore.DocumentSnapshot userDoc,
            boolean includeDisplayNameFallback) {
        if (userDoc == null || !userDoc.exists()) {
            return null;
        }

        String bankAccountNo = safeTrim(userDoc.getString("bankAccountNo"));
        String bankName = safeTrim(userDoc.getString("bankName"));
        String bankAccountName = safeTrim(userDoc.getString("bankAccountName"));
        if (includeDisplayNameFallback && bankAccountName.isEmpty()) {
            bankAccountName = safeTrim(userDoc.getString("fullName"));
        }
        String qrUrl = safeTrim(userDoc.getString("paymentQrUrl"));

        return new TransferReceiverInfo(bankAccountNo, bankName, bankAccountName, qrUrl);
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private void showOwnerCollectPaymentDialog(@NonNull Invoice invoice) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_owner_collect_payment_simple, null);
        View layoutTransferProof = dialogView.findViewById(R.id.layoutTransferProof);
        TextView tvTransferProofAmount = dialogView.findViewById(R.id.tvTransferProofAmount);
        TextView tvTransferProofSubmittedAt = dialogView.findViewById(R.id.tvTransferProofSubmittedAt);
        TextView tvTransferProofNote = dialogView.findViewById(R.id.tvTransferProofNote);
        ImageView imgTransferProof = dialogView.findViewById(R.id.imgTransferProofPreview);
        Spinner spinnerMethod = dialogView.findViewById(R.id.spinnerPaymentMethod);
        EditText etPaymentDate = dialogView.findViewById(R.id.etPaymentDate);

        ArrayAdapter<String> methodAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[] { getString(R.string.cash), getString(R.string.bank_transfer) });
        methodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMethod.setAdapter(methodAdapter);

        String today = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        etPaymentDate.setText(today);
        etPaymentDate.setOnClickListener(v -> showDatePicker(etPaymentDate));

        boolean hasTransferProof = invoice.isTransferProofPending()
                && invoice.getTransferProofImageUrl() != null
                && !invoice.getTransferProofImageUrl().trim().isEmpty();

        if (hasTransferProof) {
            layoutTransferProof.setVisibility(View.VISIBLE);
            tvTransferProofAmount.setText(getString(R.string.transfer_proof_amount_label,
                    MoneyFormatter.format(invoice.getTransferProofAmount())));

            String submittedAtText = "--";
            if (invoice.getTransferProofSubmittedAt() != null) {
                Date submittedDate = invoice.getTransferProofSubmittedAt().toDate();
                submittedAtText = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(submittedDate);
            }
            tvTransferProofSubmittedAt.setText(getString(R.string.transfer_proof_submitted_at_label, submittedAtText));

            String proofNote = invoice.getTransferProofNote() != null ? invoice.getTransferProofNote().trim() : "";
            if (!proofNote.isEmpty()) {
                tvTransferProofNote.setVisibility(View.VISIBLE);
                tvTransferProofNote.setText(getString(R.string.transfer_proof_note_label, proofNote));
            } else {
                tvTransferProofNote.setVisibility(View.GONE);
            }
            Glide.with(this).load(invoice.getTransferProofImageUrl()).into(imgTransferProof);
        } else {
            layoutTransferProof.setVisibility(View.GONE);
        }

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.collect_payment_simple_title,
                        invoice.getRoomNumber() != null ? invoice.getRoomNumber() : "--"))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.confirm), (d, w) -> {
                    String paidAt = etPaymentDate.getText() != null ? etPaymentDate.getText().toString().trim() : "";
                    if (!isValidDate(paidAt)) {
                        Toast.makeText(this, getString(R.string.collect_payment_need_date), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String method = spinnerMethod.getSelectedItemPosition() == 0 ? "CASH" : "BANK";
                    scopedCollection("payments")
                            .whereEqualTo("invoiceId", invoice.getId())
                            .get()
                            .addOnSuccessListener(qs -> {
                                double paid = 0;
                                if (qs != null) {
                                    for (QueryDocumentSnapshot doc : qs) {
                                        Double amt = doc.getDouble("amount");
                                        if (amt != null) {
                                            paid += amt;
                                        }
                                    }
                                }
                                double remaining = Math.max(0, invoice.getTotalAmount() - paid);
                                if (remaining <= 0.01) {
                                    Toast.makeText(this, getString(R.string.payment_recorded), Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                double amountToRecord;
                                if ("BANK".equals(method)) {
                                    if (!hasTransferProof) {
                                        Toast.makeText(this, getString(R.string.collect_payment_bank_proof_required),
                                                Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    double transferAmount = Math.max(0, invoice.getTransferProofAmount());
                                    if (transferAmount + 0.01 < remaining) {
                                        Toast.makeText(this, getString(R.string.collect_payment_bank_not_enough),
                                                Toast.LENGTH_LONG).show();
                                        return;
                                    }
                                    amountToRecord = remaining;
                                } else {
                                    amountToRecord = remaining;
                                }

                                Payment payment = new Payment();
                                payment.setInvoiceId(invoice.getId());
                                payment.setRoomId(invoice.getRoomId());
                                payment.setMethod(method);
                                payment.setPaidAt(paidAt);
                                payment.setAmount(amountToRecord);
                                payment.setNote("BANK".equals(method)
                                        ? "Xác nhận theo minh chứng chuyển khoản"
                                        : "Thu tiền mặt");

                                paymentRepository.add(payment,
                                        () -> {
                                            invoice.setTransferProofPending(false);
                                            invoice.setTransferProofImageUrl(null);
                                            invoice.setTransferProofAmount(0);
                                            invoice.setTransferProofNote(null);
                                            invoice.setTransferProofSubmittedAt(null);

                                            viewModel.updateInvoice(invoice,
                                                    () -> runOnUiThread(() -> {
                                                        recomputeAndUpdateInvoiceStatus(invoice);
                                                        switchToPaidTab();
                                                        Toast.makeText(this, getString(R.string.payment_recorded),
                                                                Toast.LENGTH_SHORT).show();
                                                    }),
                                                    () -> runOnUiThread(() -> Toast
                                                            .makeText(this, getString(R.string.update_failed),
                                                                    Toast.LENGTH_SHORT)
                                                            .show()));
                                        },
                                        () -> runOnUiThread(() -> Toast
                                                .makeText(this, getString(R.string.payment_record_failed),
                                                        Toast.LENGTH_SHORT)
                                                .show()));
                            })
                            .addOnFailureListener(e -> Toast
                                    .makeText(this, getString(R.string.cannot_check_debt), Toast.LENGTH_SHORT)
                                    .show());
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showDatePicker(@NonNull EditText target) {
        Calendar initial = Calendar.getInstance();
        String currentValue = target.getText() != null ? target.getText().toString().trim() : "";
        if (!currentValue.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                sdf.setLenient(false);
                Date parsed = sdf.parse(currentValue);
                if (parsed != null) {
                    initial.setTime(parsed);
                }
            } catch (ParseException ignored) {
                // Fallback to today when existing text is invalid.
            }
        }

        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.clear();
        utc.set(
                initial.get(Calendar.YEAR),
                initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0);

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.select_payment_date))
                .setSelection(utc.getTimeInMillis())
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null) {
                return;
            }

            Calendar selectedUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            selectedUtc.setTimeInMillis(selection);

            Calendar local = Calendar.getInstance();
            local.set(
                    selectedUtc.get(Calendar.YEAR),
                    selectedUtc.get(Calendar.MONTH),
                    selectedUtc.get(Calendar.DAY_OF_MONTH),
                    0,
                    0,
                    0);
            local.set(Calendar.MILLISECOND, 0);

            target.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(local.getTime()));
        });

        picker.show(getSupportFragmentManager(), "owner_collect_payment_date_picker");
    }

    private void startTransferProofUpload(@NonNull Uri imageUri) {
        Intent serviceIntent = new Intent(this, ImageUploadService.class);
        serviceIntent.putExtra(ImageUploadService.EXTRA_IMAGE_URI, imageUri.toString());
        startService(serviceIntent);
    }

    private boolean isValidDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            sdf.setLenient(false);
            return sdf.parse(value.trim()) != null;
        } catch (ParseException e) {
            return false;
        }
    }

    private double parseAmount(@NonNull String raw) throws NumberFormatException {
        String normalized = raw.replace(",", "").trim();
        if (normalized.isEmpty()) {
            return 0;
        }
        return Double.parseDouble(normalized);
    }

    private String formatDouble(double value) {
        return value % 1 == 0 ? String.valueOf((long) value) : String.valueOf(value);
    }

    @Override
    protected void onDestroy() {
        if (transferProofUploadReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(transferProofUploadReceiver);
            transferProofUploadReceiver = null;
        }
        super.onDestroy();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

