package com.example.myapplication.features.invoice;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.core.constants.InvoiceStatus;
import com.example.myapplication.core.constants.TenantRoles;
import com.example.myapplication.R;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.ScreenUiHelper;
import com.example.myapplication.domain.Payment;
import com.example.myapplication.core.repository.domain.PaymentRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PaymentHistoryActivity extends AppCompatActivity {

    // Keep owner/staff flow as default until tenant role is fully launched.
    private static final boolean ENABLE_TENANT_SELF_SERVICE = false;

    private final PaymentRepository repository = new PaymentRepository();
    private PaymentAdapter adapter;
    private TextView tvEmpty;
    private TextView tvPaid;
    private TextView tvRemaining;

    private String invoiceId;
    private String roomId;
    private double invoiceTotal;
    private boolean allowDeletePayments = true;

    private String lastComputedStatus;

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

        invoiceId = getIntent().getStringExtra("INVOICE_ID");
        roomId = getIntent().getStringExtra("ROOM_ID");
        invoiceTotal = getIntent().getDoubleExtra("INVOICE_TOTAL", 0);
        String title = getIntent().getStringExtra("TITLE");
        if (title != null && getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }

        if (invoiceId == null || invoiceId.trim().isEmpty()) {
            Toast.makeText(this, getString(R.string.missing_invoice_id), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvEmpty = findViewById(R.id.tvEmpty);
        tvPaid = findViewById(R.id.tvPaid);
        tvRemaining = findViewById(R.id.tvRemaining);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        FloatingActionButton fabThem = findViewById(R.id.fabThem);

        adapter = new PaymentAdapter(new PaymentAdapter.OnActionListener() {
            @Override
            public void onDelete(Payment payment) {
                if (!allowDeletePayments)
                    return;
                new AlertDialog.Builder(PaymentHistoryActivity.this)
                        .setTitle(getString(R.string.confirm_delete))
                        .setMessage(getString(R.string.delete_payment, fmtMoney(payment.getAmount())))
                        .setPositiveButton(getString(R.string.delete), (d, w) -> repository.delete(payment.getId(),
                                () -> runOnUiThread(() -> Toast
                                        .makeText(PaymentHistoryActivity.this, getString(R.string.deleted), Toast.LENGTH_SHORT).show()),
                                () -> runOnUiThread(() -> Toast
                                        .makeText(PaymentHistoryActivity.this, getString(R.string.delete_failed), Toast.LENGTH_SHORT)
                                        .show())))
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show();
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        repository.listByInvoice(invoiceId).observe(this, list -> {
            if (list == null)
                return;
            List<Payment> sorted = new ArrayList<>(list);
            Collections.sort(sorted, (a, b) -> Long.compare(parseDate(b.getPaidAt()), parseDate(a.getPaidAt())));
            adapter.setDataList(sorted);
            tvEmpty.setVisibility(sorted.isEmpty() ? View.VISIBLE : View.GONE);

            double paid = 0;
            for (Payment p : sorted)
                paid += p.getAmount();
            double remaining = Math.max(0, invoiceTotal - paid);

            tvPaid.setText(getString(R.string.collected_colon) + fmtMoney(paid));
            tvRemaining.setText(getString(R.string.remaining_colon) + fmtMoney(remaining));

            maybeUpdateInvoiceStatus(paid);
        });

        setupRolePermissions(fabThem);

        fabThem.setOnClickListener(v -> showAddPaymentDialog());
    }

    private void setupRolePermissions(@NonNull FloatingActionButton fabThem) {
        if (!ENABLE_TENANT_SELF_SERVICE) {
            allowDeletePayments = true;
            adapter.setAllowDelete(true);
            fabThem.setVisibility(View.VISIBLE);
            return;
        }

        String tenantId = TenantSession.getActiveTenantId();
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance()
                .getCurrentUser();

        if (tenantId == null || tenantId.trim().isEmpty() || user == null) {
            allowDeletePayments = true;
            adapter.setAllowDelete(true);
            fabThem.setVisibility(View.VISIBLE);
            return;
        }

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("tenants").document(tenantId)
                .collection("members").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String role = doc != null ? doc.getString("role") : null;
                    boolean isTenant = TenantRoles.TENANT.equals(role);

                    // Tenant flow: self-pay allowed, deleting payment blocked.
                    allowDeletePayments = !isTenant;
                    adapter.setAllowDelete(allowDeletePayments);
                    fabThem.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    allowDeletePayments = true;
                    adapter.setAllowDelete(true);
                    fabThem.setVisibility(View.VISIBLE);
                });
    }

    private void showAddPaymentDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_payment, null);
        EditText etAmount = dialogView.findViewById(R.id.etAmount);
        Spinner spinnerMethod = dialogView.findViewById(R.id.spinnerMethod);
        EditText etPaidAt = dialogView.findViewById(R.id.etPaidAt);
        EditText etNote = dialogView.findViewById(R.id.etNote);

        etPaidAt.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));

        android.widget.ArrayAdapter<String> methodAdapter = new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[] { getString(R.string.cash), getString(R.string.bank_transfer) });
        methodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMethod.setAdapter(methodAdapter);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.add_payment))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.save), (d, w) -> {
                    try {
                        double amount = parseDouble(etAmount);
                        if (amount <= 0) {
                            Toast.makeText(this, getString(R.string.amount_must_positive), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Payment p = new Payment();
                        p.setInvoiceId(invoiceId);
                        if (roomId != null && !roomId.trim().isEmpty())
                            p.setRoomId(roomId);
                        p.setAmount(amount);
                        p.setMethod(spinnerMethod.getSelectedItemPosition() == 0 ? "CASH" : "BANK");
                        p.setPaidAt(etPaidAt.getText().toString().trim());
                        p.setNote(etNote.getText().toString().trim());

                        repository.add(p,
                                () -> runOnUiThread(
                                        () -> Toast.makeText(this, getString(R.string.payment_saved), Toast.LENGTH_SHORT).show()),
                                () -> runOnUiThread(
                                        () -> Toast.makeText(this, getString(R.string.save_failed), Toast.LENGTH_SHORT).show()));
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, getString(R.string.invalid_amount), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private double parseDouble(EditText et) {
        String s = et.getText().toString().trim();
        return s.isEmpty() ? 0 : Double.parseDouble(s);
    }

    private long parseDate(String s) {
        if (s == null)
            return 0;
        try {
            Date d = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(s);
            return d != null ? d.getTime() : 0;
        } catch (ParseException e) {
            return 0;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private com.google.firebase.firestore.CollectionReference scopedCollection(String collection) {
        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId != null && !tenantId.trim().isEmpty()) {
            return com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("tenants").document(tenantId).collection(collection);
        }

        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance()
                .getCurrentUser();
        if (user == null)
            throw new IllegalStateException("User not logged in");
        return com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid()).collection(collection);
    }

    private void maybeUpdateInvoiceStatus(double paid) {
        if (invoiceId == null || invoiceId.trim().isEmpty())
            return;

        String st;
        if (paid <= 0) {
            st = InvoiceStatus.REPORTED;
        } else if (paid + 0.01 < invoiceTotal) {
            st = InvoiceStatus.PARTIAL;
        } else {
            st = InvoiceStatus.PAID;
        }

        if (st.equals(lastComputedStatus))
            return;
        lastComputedStatus = st;

        try {
            scopedCollection("invoices").document(invoiceId).update("status", st);
        } catch (Exception ignored) {
        }
    }

    private String fmtMoney(double v) {
        NumberFormat fmt = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
        return fmt.format(v);
    }
}
