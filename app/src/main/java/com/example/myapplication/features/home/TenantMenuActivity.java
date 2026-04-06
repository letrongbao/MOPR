package com.example.myapplication.features.home;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.myapplication.R;

public class TenantMenuActivity extends AppCompatActivity {

    private CardView cardMyRoom;
    private CardView cardBill;
    private CardView cardReport;
    private CardView cardNotification;

    private TextView tvTenantName;
    private TextView tvRoomInfo;

    private String tenantId;
    private String roomId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_menu);

        // Nhận dữ liệu phòng được truyền sang từ JoinRoomActivity hoặc HomeMenuActivity
        tenantId = getIntent().getStringExtra("TENANT_ID");
        roomId   = getIntent().getStringExtra("ROOM_ID");

        // Ánh xạ các View
        tvTenantName     = findViewById(R.id.tvTenantName);
        tvRoomInfo       = findViewById(R.id.tvRoomInfo);
        cardMyRoom       = findViewById(R.id.cardMyRoom);
        cardBill         = findViewById(R.id.cardBill);
        cardReport       = findViewById(R.id.cardReport);
        cardNotification = findViewById(R.id.cardNotification);

        // Hiển thị thông tin phòng lên header
        if (roomId != null && !roomId.isEmpty()) {
            tvRoomInfo.setText("Phòng: " + roomId);
        } else {
            tvRoomInfo.setText("Phòng: Đang tải...");
        }

        // Xử lý sự kiện click
        cardMyRoom.setOnClickListener(v -> {
            // TODO: Mở màn hình "Phòng của tôi"
            Toast.makeText(this, "Phòng của tôi", Toast.LENGTH_SHORT).show();
        });

        cardBill.setOnClickListener(v -> {
            // TODO: Mở màn hình "Chốt Hóa đơn" (xác nhận chỉ số của khách)
            Toast.makeText(this, "Chốt Hóa đơn", Toast.LENGTH_SHORT).show();
        });

        cardReport.setOnClickListener(v -> {
            // TODO: Mở màn hình "Báo cáo sự cố"
            Toast.makeText(this, "Báo cáo sự cố", Toast.LENGTH_SHORT).show();
        });

        cardNotification.setOnClickListener(v -> {
            // TODO: Mở màn hình "Thông báo"
            Toast.makeText(this, "Thông báo", Toast.LENGTH_SHORT).show();
        });
    }
}
