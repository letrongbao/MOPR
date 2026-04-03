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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
    private TextView tvPaid;
    private TextView tvRemaining;

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
        ScreenUiHelper.setupBackToolbar(this, toolbar, "Lịch sử thanh toán của bạn");

        tvEmpty = findViewById(R.id.tvEmpty);
        tvPaid = findViewById(R.id.tvPaid);
        tvRemaining = findViewById(R.id.tvRemaining);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        FloatingActionButton fabThem = findViewById(R.id.fabThem);

        adapter = new PaymentAdapter(payment -> {
            // Tenant view: no delete action.
        });
        adapter.setAllowDelete(false);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        if (fabThem != null) {
            fabThem.setVisibility(View.GONE);
        }

        if (tvRemaining != null) {
            tvRemaining.setVisibility(View.GONE);
        }

        setupTenantData();
    }

    private void setupTenantData() {
        String tenantId = TenantSession.getActiveTenantId();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (tenantId == null || tenantId.trim().isEmpty() || user == null) {
            showEmpty("Chưa có dữ liệu tenant");
            return;
        }

        db.collection("tenants").document(tenantId)
                .collection("members").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String role = doc != null ? doc.getString("role") : null;
                    String roomId = doc != null ? doc.getString("roomId") : null;

                    if (!TenantRoles.TENANT.equals(role)) {
                        Toast.makeText(this, "Màn này dành cho tenant", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    if (roomId == null || roomId.trim().isEmpty()) {
                        showEmpty("Chưa có thanh toán");
                        return;
                    }

                    observePayments(roomId);
                })
                .addOnFailureListener(e -> showEmpty("Không tải được dữ liệu"));
    }

    private void observePayments(String roomId) {
        repository.listByRoom(roomId).observe(this, list -> {
            List<Payment> safe = list != null ? new ArrayList<>(list) : new ArrayList<>();
            Collections.sort(safe, (a, b) -> Long.compare(parseDate(b.getPaidAt()), parseDate(a.getPaidAt())));
            adapter.setDanhSach(safe);
            tvEmpty.setVisibility(safe.isEmpty() ? View.VISIBLE : View.GONE);

            double paid = 0;
            for (Payment p : safe) {
                paid += p.getAmount();
            }
            tvPaid.setText("Tổng đã thanh toán: " + fmtMoney(paid));
        });
    }

    private void showEmpty(String message) {
        adapter.setDanhSach(new ArrayList<>());
        tvEmpty.setText(message);
        tvEmpty.setVisibility(View.VISIBLE);
        tvPaid.setText("Tổng đã thanh toán: 0 đ");
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
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        return nf.format(amount) + " đ";
    }
}
