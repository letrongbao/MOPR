package com.example.myapplication.features.invoice;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.repository.domain.InvoiceRepository;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.ScreenUiHelper;
import com.example.myapplication.domain.House;
import com.example.myapplication.domain.Invoice;
import com.example.myapplication.domain.Payment;
import com.example.myapplication.domain.Room;
import com.example.myapplication.domain.Tenant;
import com.example.myapplication.core.repository.domain.PaymentRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class PaymentHistoryActivity extends AppCompatActivity {

    private final InvoiceRepository invoiceRepository = new InvoiceRepository();
    private final PaymentRepository repository = new PaymentRepository();
    private InvoiceAdapter adapter;
    private TextView tvEmpty;
    private TextView tvHeaderTitle;
    private TextView tvHeaderSubtitle;
    private TextView tvSummaryInline;

    private String invoiceId;
    private String roomId;
    private String contractId;
    private String roomName;
    private String houseName;
    private double selectedInvoiceTotal;
    private List<Payment> allPayments = new ArrayList<>();
    private List<Invoice> scopedInvoices = new ArrayList<>();
    private final Set<String> scopedInvoiceIds = new HashSet<>();
    private double contractInvoiceTotal = 0;
    private int scopedInvoiceCount = 0;
    private Integer contractStartPeriodKey;
    private Integer contractEndPeriodKey;
    private double paidAmount = 0;
    private double remainingAmount = 0;
    private final Map<String, String> tenantDisplayByRoom = new HashMap<>();
    private final Map<String, String> roomAddressByRoom = new HashMap<>();
    private final Map<String, String> roomElectricModeByRoom = new HashMap<>();
    private final Map<String, String> roomWaterModeByRoom = new HashMap<>();
    private final Map<String, Integer> roomMemberCountByRoom = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScreenUiHelper.enableEdgeToEdge(this, false);

        setContentView(R.layout.activity_payment_history);

        com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            ScreenUiHelper.applyTopInset(appBarLayout);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        ScreenUiHelper.setupBackToolbar(this, toolbar, getString(R.string.payment_history));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        invoiceId = getIntent().getStringExtra("INVOICE_ID");
        roomId = getIntent().getStringExtra("ROOM_ID");
        contractId = getIntent().getStringExtra("CONTRACT_ID");
        roomName = getIntent().getStringExtra("ROOM_NAME");
        houseName = getIntent().getStringExtra("HOUSE_NAME");
        selectedInvoiceTotal = getIntent().getDoubleExtra("INVOICE_TOTAL", 0);

        if (invoiceId == null || invoiceId.trim().isEmpty()) {
            Toast.makeText(this, getString(R.string.missing_invoice_id), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvHeaderSubtitle = findViewById(R.id.tvHeaderSubtitle);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvSummaryInline = findViewById(R.id.tvSummaryInline);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);

        if (tvHeaderTitle != null) {
            tvHeaderTitle.setText(R.string.payment_history);
        }
        tvHeaderSubtitle.setText(buildHeaderSubtitle());
        refreshSummaryInline(0, 0);

        adapter = new InvoiceAdapter(new InvoiceAdapter.OnItemActionListener() {
            @Override
            public void onDelete(Invoice invoice) {
            }

            @Override
            public void onBaoPhi(Invoice invoice) {
            }

            @Override
            public void onDoiTrangThai(Invoice invoice) {
            }

            @Override
            public void onSua(Invoice invoice) {
            }

            @Override
            public void onXuat(Invoice invoice) {
            }

            @Override
            public void onEditOwnerNote(Invoice invoice) {
            }
        });
        adapter.setReadOnly(true);
        adapter.setTenantDisplayByRoom(tenantDisplayByRoom);
        adapter.setRoomAddressByRoom(roomAddressByRoom);
        adapter.setRoomElectricModeByRoom(roomElectricModeByRoom);
        adapter.setRoomWaterModeByRoom(roomWaterModeByRoom);
        adapter.setRoomMemberCountByRoom(roomMemberCountByRoom);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        hydrateCardMetadata();

        repository.getPaymentList().observe(this, list -> {
            if (list == null)
                return;
            allPayments = new ArrayList<>(list);
            recomputeScopedPayments();
        });

        loadContractWindowThenObserveInvoices();
    }

    private void loadContractWindowThenObserveInvoices() {
        if (contractId == null || contractId.trim().isEmpty()) {
            observeContractInvoices();
            return;
        }

        scopedCollection("contracts").document(contractId)
                .get()
                .addOnSuccessListener(doc -> {
                    contractStartPeriodKey = parseDayDateToPeriodKey(doc.getString("rentalStartDate"));
                    contractEndPeriodKey = parseDayDateToPeriodKey(doc.getString("contractEndDate"));

                    if (contractEndPeriodKey == null) {
                        Long endTimestamp = doc.getLong("contractEndTimestamp");
                        if (endTimestamp != null && endTimestamp > 0) {
                            Calendar c = Calendar.getInstance();
                            c.setTimeInMillis(endTimestamp);
                            contractEndPeriodKey = toPeriodKey(c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR));
                        }
                    }

                    observeContractInvoices();
                })
                .addOnFailureListener(e -> observeContractInvoices());
    }

    private void observeContractInvoices() {
        if (roomId == null || roomId.trim().isEmpty()) {
            scopedInvoiceIds.clear();
            scopedInvoiceIds.add(invoiceId);
            scopedInvoiceCount = 1;
            contractInvoiceTotal = selectedInvoiceTotal;
            refreshHeaderTotals();
            recomputeScopedPayments();
            return;
        }

        invoiceRepository.getInvoicesByRoom(roomId).observe(this, invoices -> {
            scopedInvoiceIds.clear();
            contractInvoiceTotal = 0;
            scopedInvoiceCount = 0;
            List<Invoice> matchedInvoices = new ArrayList<>();

            if (invoices != null) {
                for (Invoice invoice : invoices) {
                    if (invoice == null || invoice.getId() == null || invoice.getId().trim().isEmpty()) {
                        continue;
                    }

                    if (contractId != null && !contractId.trim().isEmpty()) {
                        boolean exactContractMatch = contractId.equals(invoice.getContractId());
                        boolean legacyFallbackMatch = !exactContractMatch && isLegacyInvoiceInContractWindow(invoice);
                        if (!exactContractMatch && !legacyFallbackMatch)
                            continue;
                    }

                    scopedInvoiceIds.add(invoice.getId());
                    scopedInvoiceCount++;
                    contractInvoiceTotal += Math.max(0, invoice.getTotalAmount());
                    matchedInvoices.add(invoice);
                }
            }

            if (scopedInvoiceIds.isEmpty() && invoiceId != null && !invoiceId.trim().isEmpty()) {
                if (invoices != null) {
                    for (Invoice invoice : invoices) {
                        if (invoice != null && invoiceId.equals(invoice.getId())) {
                            scopedInvoiceIds.add(invoiceId);
                            scopedInvoiceCount = 1;
                            contractInvoiceTotal = Math.max(0, invoice.getTotalAmount());
                            matchedInvoices.add(invoice);
                            break;
                        }
                    }
                }

                if (scopedInvoiceIds.isEmpty()) {
                    scopedInvoiceIds.add(invoiceId);
                    scopedInvoiceCount = 1;
                    contractInvoiceTotal = Math.max(0, selectedInvoiceTotal);
                }
            }

            scopedInvoices = matchedInvoices;
            hydrateCardMetadata();

            refreshHeaderTotals();
            recomputeScopedPayments();
        });
    }

    private void refreshHeaderTotals() {
        refreshSummaryInline(scopedInvoices.size(), scopedInvoices.size());
    }

    private void recomputeScopedPayments() {
        List<Payment> filtered = new ArrayList<>();
        for (Payment payment : allPayments) {
            if (payment == null || payment.getInvoiceId() == null || payment.getInvoiceId().trim().isEmpty()) {
                continue;
            }

            if (!scopedInvoiceIds.contains(payment.getInvoiceId())) {
                continue;
            }

            filtered.add(payment);
        }

        double paidAll = 0;
        for (Payment p : filtered) {
            paidAll += p.getAmount();
        }

        double remaining = Math.max(0, contractInvoiceTotal - paidAll);
        paidAmount = paidAll;
        remainingAmount = remaining;

        renderFilteredAndSortedList();
    }

    private boolean isLegacyInvoiceInContractWindow(@NonNull Invoice invoice) {
        String invoiceContractId = invoice.getContractId();
        if (invoiceContractId != null && !invoiceContractId.trim().isEmpty()) {
            return false;
        }

        Integer invoicePeriod = parseBillingPeriodToKey(invoice.getBillingPeriod());
        if (invoicePeriod == null) {
            return false;
        }

        if (contractStartPeriodKey != null && invoicePeriod < contractStartPeriodKey) {
            return false;
        }

        if (contractEndPeriodKey != null && invoicePeriod > contractEndPeriodKey) {
            return false;
        }

        return true;
    }

    private Integer parseDayDateToPeriodKey(String dayDate) {
        if (dayDate == null || dayDate.trim().isEmpty()) {
            return null;
        }
        try {
            Date parsed = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dayDate.trim());
            if (parsed == null) {
                return null;
            }
            Calendar c = Calendar.getInstance();
            c.setTime(parsed);
            return toPeriodKey(c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR));
        } catch (ParseException ignored) {
            return null;
        }
    }

    private Integer parseBillingPeriodToKey(String billingPeriod) {
        if (billingPeriod == null || billingPeriod.trim().isEmpty()) {
            return null;
        }
        String[] parts = billingPeriod.trim().split("/");
        if (parts.length != 2) {
            return null;
        }
        try {
            int month = Integer.parseInt(parts[0].trim());
            int year = Integer.parseInt(parts[1].trim());
            if (month < 1 || month > 12) {
                return null;
            }
            return toPeriodKey(month, year);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private int toPeriodKey(int month, int year) {
        return year * 100 + month;
    }

    private void renderFilteredAndSortedList() {
        List<Invoice> displayList = new ArrayList<>(scopedInvoices);

        Collections.sort(displayList, (a, b) -> Integer.compare(
                parseBillingPeriodToKey(b.getBillingPeriod()) != null ? parseBillingPeriodToKey(b.getBillingPeriod()) : 0,
                parseBillingPeriodToKey(a.getBillingPeriod()) != null ? parseBillingPeriodToKey(a.getBillingPeriod()) : 0));

        adapter.setDataList(displayList);
        tvEmpty.setVisibility(displayList.isEmpty() ? View.VISIBLE : View.GONE);

        refreshSummaryInline(displayList.size(), scopedInvoices.size());
    }

    private String normalizeInvoiceStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return com.example.myapplication.core.constants.InvoiceStatus.UNREPORTED;
        }
        if ("PARTIAL".equalsIgnoreCase(status.trim())) {
            return com.example.myapplication.core.constants.InvoiceStatus.REPORTED;
        }
        return status;
    }

    private void hydrateCardMetadata() {
        Set<String> targetRoomIds = new HashSet<>();
        if (roomId != null && !roomId.trim().isEmpty()) {
            targetRoomIds.add(roomId.trim());
        }
        for (Invoice invoice : scopedInvoices) {
            if (invoice == null || invoice.getRoomId() == null || invoice.getRoomId().trim().isEmpty()) {
                continue;
            }
            targetRoomIds.add(invoice.getRoomId().trim());
        }

        if (targetRoomIds.isEmpty()) {
            return;
        }

        for (String targetRoomId : targetRoomIds) {
            hydrateRoomAndHouseMetadata(targetRoomId);
            loadBestContractMetadata(targetRoomId);
        }
    }

    private void hydrateRoomAndHouseMetadata(@NonNull String targetRoomId) {
        scopedCollection("rooms").document(targetRoomId)
                .get()
                .addOnSuccessListener(roomDoc -> {
                    Room room = roomDoc != null ? roomDoc.toObject(Room.class) : null;
                    if (room == null) {
                        applyCardMetadataToAdapter();
                        return;
                    }

                    String houseId = room.getHouseId();
                    if (houseId == null || houseId.trim().isEmpty()) {
                        applyCardMetadataToAdapter();
                        return;
                    }

                    scopedCollection("houses").document(houseId.trim())
                            .get()
                            .addOnSuccessListener(houseDoc -> {
                                House house = houseDoc != null ? houseDoc.toObject(House.class) : null;
                                if (house != null) {
                                    String address = house.getAddress() != null ? house.getAddress().trim() : "";
                                    if (!address.isEmpty()) {
                                        roomAddressByRoom.put(targetRoomId, address);
                                    }
                                    if (house.getElectricityCalculationMethod() != null
                                            && !house.getElectricityCalculationMethod().trim().isEmpty()) {
                                        roomElectricModeByRoom.put(targetRoomId,
                                                house.getElectricityCalculationMethod().trim());
                                    }
                                    if (house.getWaterCalculationMethod() != null
                                            && !house.getWaterCalculationMethod().trim().isEmpty()) {
                                        roomWaterModeByRoom.put(targetRoomId,
                                                house.getWaterCalculationMethod().trim());
                                    }
                                }
                                applyCardMetadataToAdapter();
                            })
                            .addOnFailureListener(e -> applyCardMetadataToAdapter());
                })
                .addOnFailureListener(e -> applyCardMetadataToAdapter());
    }

    private void loadBestContractMetadata(@NonNull String targetRoomId) {
        if (contractId != null && !contractId.trim().isEmpty()
                && roomId != null
                && roomId.trim().equals(targetRoomId)) {
            scopedCollection("contracts").document(contractId.trim())
                    .get()
                    .addOnSuccessListener(contractDoc -> {
                        Tenant selected = contractDoc != null ? contractDoc.toObject(Tenant.class) : null;
                        if (selected != null) {
                            updateContractCardMetadata(targetRoomId, selected);
                            return;
                        }
                        loadFallbackContractByRoom(targetRoomId);
                    })
                    .addOnFailureListener(e -> loadFallbackContractByRoom(targetRoomId));
            return;
        }

        loadFallbackContractByRoom(targetRoomId);
    }

    private void loadFallbackContractByRoom(@NonNull String targetRoomId) {
        scopedCollection("contracts")
                .whereEqualTo("roomId", targetRoomId)
                .limit(30)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || snapshot.isEmpty()) {
                        return;
                    }

                    Tenant bestActive = null;
                    Tenant bestAny = null;
                    long bestActiveScore = Long.MIN_VALUE;
                    long bestAnyScore = Long.MIN_VALUE;

                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshot) {
                        Tenant tenant = doc.toObject(Tenant.class);
                        if (tenant == null) {
                            continue;
                        }

                        long score = resolveContractScore(tenant);
                        if (score > bestAnyScore) {
                            bestAnyScore = score;
                            bestAny = tenant;
                        }

                        String status = tenant.getContractStatus();
                        boolean active = status != null && "ACTIVE".equalsIgnoreCase(status.trim());
                        if (active && score > bestActiveScore) {
                            bestActiveScore = score;
                            bestActive = tenant;
                        }
                    }

                    Tenant selected = bestActive != null ? bestActive : bestAny;
                    updateContractCardMetadata(targetRoomId, selected);
                });
    }

    private long resolveContractScore(@NonNull Tenant contract) {
        if (contract.getUpdatedAt() != null && contract.getUpdatedAt() > 0) {
            return contract.getUpdatedAt();
        }
        if (contract.getEndedAt() != null && contract.getEndedAt() > 0) {
            return contract.getEndedAt();
        }
        if (contract.getCreatedAt() != null && contract.getCreatedAt() > 0) {
            return contract.getCreatedAt();
        }
        return 0;
    }

    private void updateContractCardMetadata(@NonNull String targetRoomId, Tenant contract) {
        if (contract == null) {
            return;
        }

        roomMemberCountByRoom.put(targetRoomId, Math.max(0, contract.getMemberCount()));

        String name = contract.getRepresentativeName() != null ? contract.getRepresentativeName().trim() : "";
        if (name.isEmpty()) {
            name = contract.getFullName() != null ? contract.getFullName().trim() : "";
        }
        String phone = contract.getPhoneNumber() != null ? contract.getPhoneNumber().trim() : "";

        String display = getString(R.string.invoice_representative_colon)
                + (name.isEmpty() ? getString(R.string.updating) : name)
                + (phone.isEmpty() ? "" : getString(R.string.phone_separator) + phone);
        tenantDisplayByRoom.put(targetRoomId, display);

        applyCardMetadataToAdapter();
    }

    private void applyCardMetadataToAdapter() {
        adapter.setTenantDisplayByRoom(tenantDisplayByRoom);
        adapter.setRoomAddressByRoom(roomAddressByRoom);
        adapter.setRoomElectricModeByRoom(roomElectricModeByRoom);
        adapter.setRoomWaterModeByRoom(roomWaterModeByRoom);
        adapter.setRoomMemberCountByRoom(roomMemberCountByRoom);
    }

    private void refreshSummaryInline(int displayedCount, int totalCount) {
        if (tvSummaryInline == null) {
            return;
        }

        tvSummaryInline.setText(getString(
                R.string.payment_history_inline_summary,
                fmtMoney(contractInvoiceTotal),
                fmtMoney(paidAmount),
                fmtMoney(remainingAmount),
                displayedCount,
                totalCount));
    }

    private String buildHeaderSubtitle() {
        String resolvedRoomName = (roomName != null && !roomName.trim().isEmpty())
                ? normalizeRoomDisplay(roomName.trim())
                : getString(R.string.payment_history_unknown_room);
        String resolvedHouseName = (houseName != null && !houseName.trim().isEmpty())
                ? houseName.trim()
                : getString(R.string.payment_history_unknown_house);
        return getString(R.string.payment_history_header_subtitle_format, resolvedRoomName, resolvedHouseName);
    }

    private String normalizeRoomDisplay(@NonNull String rawRoomName) {
        String value = rawRoomName.trim();
        if (value.isEmpty()) {
            return getString(R.string.payment_history_unknown_room);
        }

        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.startsWith("p.")) {
            String suffix = value.substring(2).trim();
            return suffix.isEmpty() ? "P." : "P." + suffix;
        }

        if (lower.startsWith("phòng ")) {
            String suffix = value.substring("phòng ".length()).trim();
            return suffix.isEmpty() ? "P." : "P." + suffix;
        }

        if (lower.startsWith("phong ")) {
            String suffix = value.substring("phong ".length()).trim();
            return suffix.isEmpty() ? "P." : "P." + suffix;
        }

        return "P." + value;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private String fmtMoney(double v) {
        NumberFormat fmt = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
        return fmt.format(v);
    }

    private CollectionReference scopedCollection(String collection) {
        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId != null && !tenantId.trim().isEmpty()) {
            return FirebaseFirestore.getInstance()
                    .collection("tenants").document(tenantId).collection(collection);
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
            throw new IllegalStateException("User not logged in");
        return FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid()).collection(collection);
    }
}
