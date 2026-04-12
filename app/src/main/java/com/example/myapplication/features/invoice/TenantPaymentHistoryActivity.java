package com.example.myapplication.features.invoice;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.constants.TenantRoles;
import com.example.myapplication.core.repository.domain.PaymentRepository;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.ScreenUiHelper;
import com.example.myapplication.domain.Payment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TenantPaymentHistoryActivity extends AppCompatActivity {

    private final PaymentRepository repository = new PaymentRepository();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private PaymentAdapter adapter;
    private TextView tvEmpty;
    private TextView tvSummaryInline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScreenUiHelper.enableEdgeToEdge(this, false);

        setContentView(R.layout.activity_payment_history);

        com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            ScreenUiHelper.applyTopInset(appBarLayout);
            appBarLayout.setBackgroundResource(R.drawable.bg_tenant_header_teal);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setBackgroundResource(R.drawable.bg_tenant_header_teal);
        }
        ScreenUiHelper.setupBackToolbar(this, toolbar, getString(R.string.your_payment_history));

        tvEmpty = findViewById(R.id.tvEmpty);
        tvSummaryInline = findViewById(R.id.tvSummaryInline);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        TextView tvHeaderSubtitle = findViewById(R.id.tvHeaderSubtitle);

        adapter = new PaymentAdapter(payment -> {
            // Tenant view: no delete action.
        });
        adapter.setAllowDelete(false);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        if (tvHeaderSubtitle != null)
            tvHeaderSubtitle.setVisibility(View.GONE);
        if (tvSummaryInline != null) {
            tvSummaryInline.setText(getString(R.string.total_paid_colon) + fmtMoney(0));
        }

        setupTenantData();
    }

    private void setupTenantData() {
        String tenantId = TenantSession.getActiveTenantId();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (tenantId == null || tenantId.trim().isEmpty() || user == null) {
            showEmpty(getString(R.string.no_tenant_data));
            return;
        }

        db.collection("tenants").document(tenantId)
                .collection("members").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String role = doc != null ? doc.getString("role") : null;
                    String roomId = doc != null ? doc.getString("roomId") : null;

                    if (!TenantRoles.TENANT.equals(role)) {
                        Toast.makeText(this, getString(R.string.screen_for_tenants), Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    if (roomId == null || roomId.trim().isEmpty()) {
                        showEmpty(getString(R.string.no_payments_yet));
                        return;
                    }

                    observePayments(roomId);
                })
                .addOnFailureListener(e -> showEmpty(getString(R.string.error_load_data)));
    }

    private void observePayments(String roomId) {
        repository.listByRoom(roomId).observe(this, list -> {
            List<Payment> safe = list != null ? new ArrayList<>(list) : new ArrayList<>();
            Collections.sort(safe, (a, b) -> Long.compare(parseDate(b.getPaidAt()), parseDate(a.getPaidAt())));
            adapter.setDataList(safe);
            tvEmpty.setVisibility(safe.isEmpty() ? View.VISIBLE : View.GONE);

            double paid = 0;
            for (Payment p : safe) {
                paid += p.getAmount();
            }
            if (tvSummaryInline != null) {
                tvSummaryInline.setText(getString(R.string.total_paid_colon) + fmtMoney(paid));
            }
        });
    }

    private void showEmpty(String message) {
        adapter.setDataList(new ArrayList<>());
        tvEmpty.setText(message);
        tvEmpty.setVisibility(View.VISIBLE);
        if (tvSummaryInline != null) {
            tvSummaryInline.setText(getString(R.string.total_paid_colon) + fmtMoney(0));
        }
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

    private String fmtMoney(double amount) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.getDefault());
        return nf.format(amount) + " ₫";
    }
}
