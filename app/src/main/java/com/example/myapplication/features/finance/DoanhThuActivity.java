package com.example.myapplication.features.finance;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import com.example.myapplication.domain.HoaDon;
import com.example.myapplication.viewmodel.HoaDonViewModel;
import com.example.myapplication.viewmodel.NguoiThueViewModel;
import com.example.myapplication.viewmodel.PhongTroViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import com.example.myapplication.core.constants.InvoiceStatus;
import com.example.myapplication.R;
import com.example.myapplication.core.constants.RoomStatus;
import com.example.myapplication.core.session.TenantSession;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DoanhThuActivity extends AppCompatActivity {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private List<HoaDon> lastInvoices;
    private final Map<String, Double> paidByInvoiceId = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Làm status bar trong suốt giống HomeActivity
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.getDecorView()
                .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        window.setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_doanh_thu);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Thống kê doanh thu");
        }

        // Tự động thêm padding cho Toolbar để tránh bị Status Bar che mất
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        TextView tvTongPhong = findViewById(R.id.tvTongPhong);
        TextView tvPhongDaThua = findViewById(R.id.tvPhongDaThua);
        TextView tvDoanhThuThang = findViewById(R.id.tvDoanhThuThang);
        TextView tvSoHDChuaTT = findViewById(R.id.tvSoHDChuaTT);
        TextView tvSoNguoiThue = findViewById(R.id.tvSoNguoiThue);

        String thangNay = new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(new Date());
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        ViewModelProvider viewModelProvider = new ViewModelProvider(this);

        viewModelProvider.get(PhongTroViewModel.class)
                .getDanhSachPhong().observe(this, list -> {
                    if (list == null)
                        return;
                    tvTongPhong.setText(String.valueOf(list.size()));
                    long daThua = 0;
                    for (com.example.myapplication.domain.PhongTro p : list) {
                        if (RoomStatus.RENTED.equals(p.getTrangThai()))
                            daThua++;
                    }
                    tvPhongDaThua.setText(String.valueOf(daThua));
                });

        viewModelProvider.get(NguoiThueViewModel.class)
                .getDanhSachNguoiThue().observe(this, list -> {
                    if (list != null)
                        tvSoNguoiThue.setText(String.valueOf(list.size()));
                });

        // Lắng nghe payments để tính "đã thu" theo tổng payment, không phụ thuộc toggle
        // trạng thái
        scopedCollection("payments")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null)
                        return;
                    paidByInvoiceId.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        String invoiceId = doc.getString("invoiceId");
                        Double amt = doc.getDouble("amount");
                        if (invoiceId == null || invoiceId.trim().isEmpty())
                            continue;
                        paidByInvoiceId.put(invoiceId,
                                paidByInvoiceId.getOrDefault(invoiceId, 0.0) + (amt != null ? amt : 0.0));
                    }
                    updateInvoiceStats(thangNay, fmt);
                });

        viewModelProvider.get(HoaDonViewModel.class)
                .getDanhSachHoaDon().observe(this, list -> {
                    lastInvoices = list;
                    updateInvoiceStats(thangNay, fmt);
                });
    }

    private void updateInvoiceStats(@androidx.annotation.NonNull String thangNay,
            @androidx.annotation.NonNull NumberFormat fmt) {
        if (lastInvoices == null)
            return;

        int chuaThu = 0;
        double daThuThang = 0;

        for (HoaDon h : lastInvoices) {
            if (h == null)
                continue;

            String st = h.getTrangThai();
            if (st == null || st.trim().isEmpty())
                st = InvoiceStatus.UNPAID;

            if (InvoiceStatus.UNPAID.equals(st) || InvoiceStatus.PARTIAL.equals(st)) {
                chuaThu++;
            }

            if (h.getThangNam() != null && h.getThangNam().trim().equals(thangNay)) {
                String invoiceId = h.getId();
                if (invoiceId != null && !invoiceId.trim().isEmpty()) {
                    double paid = paidByInvoiceId.getOrDefault(invoiceId, 0.0);
                    daThuThang += Math.min(paid, h.getTongTien());
                }
            }
        }

        TextView tvSoHDChuaTT = findViewById(R.id.tvSoHDChuaTT);
        TextView tvDoanhThuThang = findViewById(R.id.tvDoanhThuThang);
        tvSoHDChuaTT.setText(String.valueOf(chuaThu));
        tvDoanhThuThang.setText(fmt.format(daThuThang));
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

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}