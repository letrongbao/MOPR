package com.example.myapplication.features.contract;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.domain.Tenant;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TenantContractDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_ROOM_ID = "ROOM_ID";
    public static final String EXTRA_TENANT_ID = "TENANT_ID";

    private String roomId;
    private String tenantId;
    private FirebaseFirestore db;

    // View references
    private ImageView btnBack;
    private TextView tvContractTitle, tvContractStatus;
    private ImageView imgStatusDot;
    private View btnExportPdf, btnViewDoc, btnShare;
    
    private TextView tvRepName, tvRepPhone, tvMemberCount;
    private TextView tvDepositAmount, tvRentAmount;
    private TextView tvBillingCycle, tvDepositDate, tvStartDate, tvEndDate, tvDuration;
    private Button btnClose, btnEndContract;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_contract_details);

        db = FirebaseFirestore.getInstance();
        
        roomId = getIntent().getStringExtra(EXTRA_ROOM_ID);
        tenantId = getIntent().getStringExtra(EXTRA_TENANT_ID);

        if (roomId == null || tenantId == null) {
            Toast.makeText(this, "Thiếu thông tin phòng/người thuê", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        setupListeners();
        getContractData();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        
        tvContractTitle = findViewById(R.id.tvContractTitle);
        tvContractStatus = findViewById(R.id.tvContractStatus);
        imgStatusDot = findViewById(R.id.imgStatusDot);
        
        btnExportPdf = findViewById(R.id.btnExportPdf);
        btnViewDoc = findViewById(R.id.btnViewDoc);
        btnShare = findViewById(R.id.btnShare);
        
        tvRepName = findViewById(R.id.tvRepName);
        tvRepPhone = findViewById(R.id.tvRepPhone);
        tvMemberCount = findViewById(R.id.tvMemberCount);
        tvDepositAmount = findViewById(R.id.tvDepositAmount);
        tvRentAmount = findViewById(R.id.tvRentAmount);
        tvBillingCycle = findViewById(R.id.tvBillingCycle);
        tvDepositDate = findViewById(R.id.tvDepositDate);
        tvStartDate = findViewById(R.id.tvStartDate);
        tvEndDate = findViewById(R.id.tvEndDate);
        tvDuration = findViewById(R.id.tvDuration);
        
        btnClose = findViewById(R.id.btnClose);
        btnEndContract = findViewById(R.id.btnEndContract);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnClose.setOnClickListener(v -> finish());
        
        View.OnClickListener mockListener = v -> 
            Toast.makeText(this, "Tính năng đang được phát triển!", Toast.LENGTH_SHORT).show();
            
        btnExportPdf.setOnClickListener(mockListener);
        btnViewDoc.setOnClickListener(mockListener);
        btnShare.setOnClickListener(mockListener);
        btnEndContract.setOnClickListener(mockListener);
    }

    private void getContractData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        db.collection("tenants").document(tenantId)
          .collection("contracts")
          .whereEqualTo("roomId", roomId)
          .limit(5)
          .get()
          .addOnSuccessListener(qs -> {
              if (qs != null && !qs.isEmpty()) {
                  Tenant activeContract = null;
                  for (com.google.firebase.firestore.DocumentSnapshot doc : qs.getDocuments()) {
                      String status = doc.getString("contractStatus");
                      if ("ACTIVE".equalsIgnoreCase(status)) {
                          activeContract = doc.toObject(Tenant.class);
                          if (activeContract != null) activeContract.setId(doc.getId());
                          break;
                      }
                  }
                  if (activeContract == null) {
                      activeContract = qs.getDocuments().get(0).toObject(Tenant.class);
                      if (activeContract != null) activeContract.setId(qs.getDocuments().get(0).getId());
                  }
                  
                  if (activeContract != null) {
                      displayData(activeContract);
                  }
              } else {
                  // Fallback users/
                  db.collection("users").document(tenantId)
                    .collection("contracts")
                    .whereEqualTo("roomId", roomId)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(qs2 -> {
                        if (qs2 != null && !qs2.isEmpty()) {
                            Tenant c2 = qs2.getDocuments().get(0).toObject(Tenant.class);
                            if (c2 != null) {
                                c2.setId(qs2.getDocuments().get(0).getId());
                                displayData(c2);
                            }
                        } else {
                            showNotFound();
                        }
                    })
                    .addOnFailureListener(e -> showNotFound());
              }
          })
          .addOnFailureListener(e -> showNotFound());
    }

    private void displayData(Tenant contract) {
        // Thông tin chung
        String cNum = contract.getContractNumber() != null && !contract.getContractNumber().isEmpty() 
            ? contract.getContractNumber() 
            : contract.getId() != null ? contract.getId().substring(0, Math.min(5, contract.getId().length())).toUpperCase() : "N/A";
        
        tvContractTitle.setText("Hợp đồng (#" + cNum + ")");
        
        String status = contract.getContractStatus();
        if ("ENDED".equalsIgnoreCase(status)) {
            tvContractStatus.setText("Hợp đồng đã kết thúc");
            imgStatusDot.setImageTintList(ColorStateList.valueOf(Color.parseColor("#9E9E9E"))); // Xám
        } else {
            // Kiểm tra sắp hết hạn hay còn hạn
            long endTs = contract.getContractEndTimestamp() > 0 ? contract.getContractEndTimestamp() : parseDateStringToMillis(contract.getContractEndDate());
            if (endTs > 0 && (endTs - System.currentTimeMillis() < 30L * 24 * 60 * 60 * 1000) && endTs > System.currentTimeMillis()) {
                tvContractStatus.setText("Sắp hết hạn hợp đồng");
                imgStatusDot.setImageTintList(ColorStateList.valueOf(Color.parseColor("#FF6D00"))); // Cam
            } else if (endTs > 0 && endTs <= System.currentTimeMillis()) {
                tvContractStatus.setText("Đã quá hạn hợp đồng");
                imgStatusDot.setImageTintList(ColorStateList.valueOf(Color.parseColor("#F44336"))); // Đỏ
            } else {
                tvContractStatus.setText("Trong thời hạn hợp đồng");
                imgStatusDot.setImageTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50"))); // Xanh
            }
        }
        
        // Đại diện cọc
        String repName = contract.getRepresentativeName();
        if (repName == null || repName.trim().isEmpty()) {
            repName = contract.getFullName();
        }
        tvRepName.setText(repName != null && !repName.trim().isEmpty() ? repName : "—");
        tvRepPhone.setText(contract.getPhoneNumber() != null && !contract.getPhoneNumber().isEmpty() ? contract.getPhoneNumber() : "—");
        
        // Thành viên
        tvMemberCount.setText(contract.getMemberCount() > 0 ? contract.getMemberCount() + " người" : "—");
        
        // Tài chính
        tvDepositAmount.setText(formatMoney(contract.getDepositAmount() > 0 ? contract.getDepositAmount() : contract.getLegacyDepositAmount()));
        tvRentAmount.setText(formatMoney(contract.getRentAmount() > 0 ? contract.getRentAmount() : contract.getRoomPrice()));
        
        tvBillingCycle.setText("1 tháng / 1 lần");
        
        // Ngày tháng
        String startDateStr = contract.getRentalStartDate();
        String endDateStr = contract.getContractEndTimestamp() > 0 ? DATE_FORMAT.format(new Date(contract.getContractEndTimestamp())) : contract.getContractEndDate();
        String createdAtStr = contract.getCreatedAt() != null ? DATE_FORMAT.format(new Date(contract.getCreatedAt())) : startDateStr; 
        
        tvDepositDate.setText(createdAtStr != null ? createdAtStr : "—");
        tvStartDate.setText(startDateStr != null ? startDateStr : "—");
        tvEndDate.setText(endDateStr != null ? endDateStr : "—");
        
        // Thời gian ở
        long startTs = parseDateStringToMillis(startDateStr);
        if (startTs > 0) {
            long diff = System.currentTimeMillis() - startTs;
            long days = TimeUnit.MILLISECONDS.toDays(diff);
            if (days < 0) days = 0;
            long months = days / 30;
            long extraDays = days % 30;
            tvDuration.setText(months + " tháng, " + extraDays + " ngày");
        } else {
            tvDuration.setText("—");
        }
    }
    
    private void showNotFound() {
        Toast.makeText(this, "Không tìm thấy dữ liệu hợp đồng", Toast.LENGTH_SHORT).show();
        finish();
    }
    
    private String formatMoney(double amount) {
        if (amount <= 0) return "—";
        long longVal = (long) amount;
        return String.format(Locale.getDefault(), "%,d", longVal).replace(',', '.') + " đ";
    }
    
    private long parseDateStringToMillis(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return 0;
        try {
            return DATE_FORMAT.parse(dateStr).getTime();
        } catch (ParseException e) {
            try {
                return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr).getTime();
            } catch (ParseException ex) {
                return 0;
            }
        }
    }
}
