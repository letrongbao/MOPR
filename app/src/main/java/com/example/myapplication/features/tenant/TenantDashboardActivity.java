package com.example.myapplication.features.tenant;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.myapplication.R;

public class TenantDashboardActivity extends AppCompatActivity {

    // Header Views
    private TextView tvDebtAmount;
    private TextView tvDaysUntilDue;
    private TextView tvRoomStatus;
    private ImageView btnProfile;

    // Dashboard Cards
    private CardView cardMyRoom;
    private CardView cardInvoice;
    private CardView cardReport;
    private CardView cardNotifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_dashboard);

        // Initialize views
        initViews();

        // Setup click listeners
        setupClickListeners();

        // Load initial data
        loadDashboardData();
    }

    private void initViews() {
        // Header statistics
        tvDebtAmount = findViewById(R.id.tvDebtAmount);
        tvDaysUntilDue = findViewById(R.id.tvDaysUntilDue);
        tvRoomStatus = findViewById(R.id.tvRoomStatus);
        btnProfile = findViewById(R.id.btnProfile);

        // Dashboard cards
        cardMyRoom = findViewById(R.id.cardMyRoom);
        cardInvoice = findViewById(R.id.cardInvoice);
        cardReport = findViewById(R.id.cardReport);
        cardNotifications = findViewById(R.id.cardNotifications);
    }

    private void setupClickListeners() {
        // Profile button
        btnProfile.setOnClickListener(v ->
                Toast.makeText(this, "Mở trang cá nhân", Toast.LENGTH_SHORT).show()
        );

        // Card 1: Phòng của bạn
        cardMyRoom.setOnClickListener(v ->
                Toast.makeText(this, "Xem thông tin phòng của bạn", Toast.LENGTH_SHORT).show()
        );

        // Card 2: Hóa đơn tháng
        cardInvoice.setOnClickListener(v ->
                Toast.makeText(this, "Xem hóa đơn tháng", Toast.LENGTH_SHORT).show()
        );

        // Card 3: Báo cáo sự cố
        cardReport.setOnClickListener(v ->
                Toast.makeText(this, "Báo cáo sự cố", Toast.LENGTH_SHORT).show()
        );

        // Card 4: Thông báo trọ
        cardNotifications.setOnClickListener(v ->
                Toast.makeText(this, "Xem thông báo trọ", Toast.LENGTH_SHORT).show()
        );
    }

    private void loadDashboardData() {
        // TODO: Load actual data from Firebase or local database
        // This is sample data for demonstration

        // Set debt amount
        tvDebtAmount.setText("0");

        // Set days until due
        tvDaysUntilDue.setText("5");

        // Set room status
        tvRoomStatus.setText("Đang ở");
    }

    /**
     * Update debt amount display
     * @param amount Amount in VND
     */
    public void updateDebtAmount(long amount) {
        if (amount == 0) {
            tvDebtAmount.setText("0");
        } else {
            tvDebtAmount.setText(String.format("%,d", amount));
        }
    }

    /**
     * Update days until payment due
     * @param days Number of days remaining
     */
    public void updateDaysUntilDue(int days) {
        tvDaysUntilDue.setText(String.valueOf(days));
    }

    /**
     * Update room status
     * @param status Room status text
     */
    public void updateRoomStatus(String status) {
        tvRoomStatus.setText(status);
    }
}
