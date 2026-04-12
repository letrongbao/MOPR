package com.example.myapplication.features.invoice;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.MoneyFormatter;
import com.example.myapplication.core.util.ScreenUiHelper;
import com.example.myapplication.domain.Invoice;
import com.google.android.material.appbar.AppBarLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TenantInvoiceActivity extends AppCompatActivity {

    public static final String EXTRA_ROOM_ID = "ROOM_ID";
    public static final String EXTRA_TENANT_ID = "TENANT_ID";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private String roomId;
    private String tenantId;

    private TenantInvoiceAdapter adapter;
    private TextView tvEmpty;
    private TextView tvHeaderTitle;
    private TextView tvHeaderSubtitle;
    private TextView tvSummaryInline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScreenUiHelper.enableEdgeToEdge(this, false);
        setContentView(R.layout.activity_payment_history);

        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            ScreenUiHelper.applyTopInset(appBarLayout);
            appBarLayout.setBackgroundResource(R.drawable.bg_tenant_header_teal);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setBackgroundResource(R.drawable.bg_tenant_header_teal);
        }
        ScreenUiHelper.setupBackToolbar(this, toolbar, getString(R.string.tenant_invoice_title));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvHeaderSubtitle = findViewById(R.id.tvHeaderSubtitle);
        tvSummaryInline = findViewById(R.id.tvSummaryInline);
        tvEmpty = findViewById(R.id.tvEmpty);
        if (tvHeaderTitle != null) {
            tvHeaderTitle.setText(R.string.tenant_invoice_title);
        }
        if (tvHeaderSubtitle != null) {
            tvHeaderSubtitle.setText(getString(R.string.tenant_invoice_subtitle));
        }

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TenantInvoiceAdapter();
        recyclerView.setAdapter(adapter);

        roomId = getIntent().getStringExtra(EXTRA_ROOM_ID);
        tenantId = getIntent().getStringExtra(EXTRA_TENANT_ID);
        if (tenantId == null || tenantId.trim().isEmpty()) {
            tenantId = TenantSession.getActiveTenantId();
        }

        loadTenantInvoices();
    }

    private void loadTenantInvoices() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || tenantId == null || tenantId.trim().isEmpty()) {
            showInvoices(new ArrayList<>());
            return;
        }

        if (roomId == null || roomId.trim().isEmpty()) {
            db.collection("tenants").document(tenantId)
                    .collection("members").document(user.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        String mappedRoom = doc.getString("roomId");
                        String mappedRoomNumber = doc.getString("roomNumber");
                        if (mappedRoom == null || mappedRoom.trim().isEmpty()) {
                            showInvoices(new ArrayList<>());
                            return;
                        }
                        roomId = mappedRoom.trim();
                        if (tvHeaderSubtitle != null) {
                            if (mappedRoomNumber != null && !mappedRoomNumber.trim().isEmpty()) {
                                tvHeaderSubtitle.setText(getString(R.string.payment_history_room_label, mappedRoomNumber.trim()));
                            } else {
                                tvHeaderSubtitle.setText(getString(R.string.payment_history_unknown_room));
                            }
                        }
                        queryInvoicesByRoom(roomId);
                    })
                    .addOnFailureListener(e -> showInvoices(new ArrayList<>()));
            return;
        }

        queryInvoicesByRoom(roomId);
    }

    private void queryInvoicesByRoom(@NonNull String roomId) {
        db.collection("tenants").document(tenantId)
                .collection("invoices")
                .whereEqualTo("roomId", roomId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null && !snapshot.isEmpty()) {
                        List<Invoice> invoices = toInvoiceList(snapshot.getDocuments());
                        updateHeaderByInvoices(invoices);
                        showInvoices(invoices);
                        return;
                    }

                    db.collection("users").document(tenantId)
                            .collection("invoices")
                            .whereEqualTo("roomId", roomId)
                            .get()
                            .addOnSuccessListener(snapshot2 -> {
                                List<Invoice> invoices = toInvoiceList(snapshot2.getDocuments());
                                updateHeaderByInvoices(invoices);
                                showInvoices(invoices);
                            })
                            .addOnFailureListener(e -> showInvoices(new ArrayList<>()));
                })
                .addOnFailureListener(e -> showInvoices(new ArrayList<>()));
    }

    private void updateHeaderByInvoices(@NonNull List<Invoice> invoices) {
        if (tvHeaderSubtitle == null) {
            return;
        }

        String roomNumber = null;
        for (Invoice invoice : invoices) {
            if (invoice != null && invoice.getRoomNumber() != null && !invoice.getRoomNumber().trim().isEmpty()) {
                roomNumber = invoice.getRoomNumber().trim();
                break;
            }
        }

        if (roomNumber == null || roomNumber.isEmpty()) {
            tvHeaderSubtitle.setText(getString(R.string.payment_history_unknown_room));
            return;
        }
        tvHeaderSubtitle.setText(getString(R.string.payment_history_room_label, roomNumber));
    }

    private List<Invoice> toInvoiceList(List<DocumentSnapshot> docs) {
        List<Invoice> invoices = new ArrayList<>();
        for (DocumentSnapshot doc : docs) {
            Invoice invoice = doc.toObject(Invoice.class);
            if (invoice == null) {
                continue;
            }
            invoice.setId(doc.getId());
            invoices.add(invoice);
        }

        Collections.sort(invoices, (left, right) -> Long.compare(
                periodToEpoch(right.getBillingPeriod()),
                periodToEpoch(left.getBillingPeriod())));
        return invoices;
    }

    private void showInvoices(List<Invoice> invoices) {
        adapter.submitList(invoices);

        boolean empty = invoices == null || invoices.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);

        double total = 0;
        if (invoices != null) {
            for (Invoice invoice : invoices) {
                total += invoice.getTotalAmount();
            }
        }

        if (tvSummaryInline != null) {
            tvSummaryInline.setText(getString(R.string.tenant_invoice_total, MoneyFormatter.format(total) + " ₫"));
        }

    }

    private long periodToEpoch(String period) {
        if (period == null || period.trim().isEmpty()) {
            return 0L;
        }
        try {
            Date date = new SimpleDateFormat("MM/yyyy", Locale.getDefault()).parse(period.trim());
            return date != null ? date.getTime() : 0L;
        } catch (ParseException e) {
            return 0L;
        }
    }
}
