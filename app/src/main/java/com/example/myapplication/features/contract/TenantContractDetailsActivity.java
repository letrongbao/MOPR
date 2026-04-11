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
                  com.google.firebase.firestore.DocumentSnapshot activeContractDoc = null;
                  for (com.google.firebase.firestore.DocumentSnapshot doc : qs.getDocuments()) {
                      String status = doc.getString("contractStatus");
                      if ("ACTIVE".equalsIgnoreCase(status)) {
                          activeContractDoc = doc;
                          break;
                      }
                  }
                  if (activeContractDoc == null) {
                      activeContractDoc = qs.getDocuments().get(0);
                  }
                  
                  if (activeContractDoc != null) {
                      displayData(activeContractDoc);
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
                            displayData(qs2.getDocuments().get(0));
                        } else {
                            showNotFound();
                        }
                    })
                    .addOnFailureListener(e -> showNotFound());
              }
          })
          .addOnFailureListener(e -> showNotFound());
    }

    private void displayData(com.google.firebase.firestore.DocumentSnapshot doc) {
        // Thông tin chung
        String contractNumber = doc.getString("contractNumber");
        String cNum = contractNumber != null && !contractNumber.isEmpty() 
            ? contractNumber 
            : doc.getId().substring(0, Math.min(5, doc.getId().length())).toUpperCase();
        
        tvContractTitle.setText("Hợp đồng (#" + cNum + ")");
        
        String status = doc.getString("contractStatus");
        long endTs = parseDateFieldToMillis(doc, "contractEndTimestamp", "contractEndDate");
        
        if ("ENDED".equalsIgnoreCase(status)) {
            tvContractStatus.setText("Hợp đồng đã kết thúc");
            imgStatusDot.setImageTintList(ColorStateList.valueOf(Color.parseColor("#9E9E9E"))); // Xám
        } else {
            // Kiểm tra sắp hết hạn hay còn hạn
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
        String repName = doc.getString("representativeName");
        String fullName = doc.getString("fullName");
        if (repName == null || repName.trim().isEmpty()) {
            repName = fullName;
        }
        String phoneNumber = doc.getString("phoneNumber");
        
        tvRepName.setText(repName != null && !repName.trim().isEmpty() ? repName : "—");
        tvRepPhone.setText(phoneNumber != null && !phoneNumber.trim().isEmpty() ? phoneNumber : "—");
        
        // Thành viên
        Object memberCountObj = doc.get("memberCount");
        int memberCount = (memberCountObj instanceof Number) ? ((Number) memberCountObj).intValue() : 0;
        tvMemberCount.setText(memberCount > 0 ? memberCount + " người" : "—");
        
        // Tài chính
        double depositAmount = getDoubleSafe(doc, "depositAmount", "legacyDepositAmount");
        double rentAmount = getDoubleSafe(doc, "rentAmount", "roomPrice");
        
        tvDepositAmount.setText(formatMoney(depositAmount));
        tvRentAmount.setText(formatMoney(rentAmount));
        
        tvBillingCycle.setText("1 tháng / 1 lần");
        
        // Ngày tháng
        long startTs = parseDateFieldToMillis(doc, "rentalStartDate");
        long createdAtTs = parseDateFieldToMillis(doc, "createdAt", "rentalStartDate");
        
        String startDateStr = startTs > 0 ? DATE_FORMAT.format(new Date(startTs)) : "—";
        String endDateStr = endTs > 0 ? DATE_FORMAT.format(new Date(endTs)) : "—";
        String createdAtStr = createdAtTs > 0 ? DATE_FORMAT.format(new Date(createdAtTs)) : startDateStr; 
        
        tvDepositDate.setText(createdAtStr);
        tvStartDate.setText(startDateStr);
        tvEndDate.setText(endDateStr);
        
        // Thời gian ở
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
    
    private long parseDateFieldToMillis(com.google.firebase.firestore.DocumentSnapshot doc, String... fieldNames) {
        for (String field : fieldNames) {
            Object val = doc.get(field);
            if (val == null) continue;

            if (val instanceof com.google.firebase.Timestamp) {
                return ((com.google.firebase.Timestamp) val).toDate().getTime();
            } else if (val instanceof Date) {
                return ((Date) val).getTime();
            } else if (val instanceof Long) {
                return (Long) val;
            } else if (val instanceof String) {
                String str = (String) val;
                if (!str.isEmpty()) {
                    try {
                        return DATE_FORMAT.parse(str).getTime();
                    } catch (ParseException ignored) {
                        try {
                            return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(str).getTime();
                        } catch (ParseException ignored2) {}
                    }
                }
            }
        }
        return 0;
    }

    private double getDoubleSafe(com.google.firebase.firestore.DocumentSnapshot doc, String... fieldNames) {
        for (String field : fieldNames) {
            Object val = doc.get(field);
            if (val instanceof Number) {
                return ((Number) val).doubleValue();
            } else if (val instanceof String) {
                try {
                    return Double.parseDouble((String) val);
                } catch (Exception ignored) {}
            }
        }
        return 0;
    }
}
