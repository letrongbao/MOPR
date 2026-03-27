package com.example.myapplication.features.home;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.R;
import com.example.myapplication.core.constants.RoomStatus;
import com.example.myapplication.features.settings.BackupRestoreActivity;
import com.example.myapplication.features.finance.ChiPhiActivity;
import com.example.myapplication.features.finance.DoanhThuActivity;
import com.example.myapplication.features.invoice.HoaDonActivity;
import com.example.myapplication.features.property.house.CanNhaActivity;
import com.example.myapplication.features.settings.ProfileActivity;
import com.example.myapplication.domain.PhongTro;
import com.example.myapplication.core.repository.domain.CanNhaRepository;
import com.example.myapplication.viewmodel.PhongTroViewModel;
import com.google.firebase.auth.FirebaseAuth;

public class HomeMenuActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextView tvTotalHouses, tvVacantRooms, tvRentedRooms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_home_menu);

        mAuth = FirebaseAuth.getInstance();

        View appBarLayout = findViewById(R.id.appBarLayout);
        ViewCompat.setOnApplyWindowInsetsListener(appBarLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        tvTotalHouses = findViewById(R.id.tvTotalHouses);
        tvVacantRooms = findViewById(R.id.tvVacantRooms);
        tvRentedRooms = findViewById(R.id.tvRentedRooms);

        View btnProfile = findViewById(R.id.btnProfile);
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                startActivity(new Intent(this, ProfileActivity.class));
            });
        }

        updateStatistics();

        GridView gridView = findViewById(R.id.gridView);

        String[] items = new String[] {
                "Quản lý nhà,\nphòng, hợp đồng",
                "Báo phí",
                "Quản lý chi",
                "Báo cáo",
                "Sao lưu, phục hồi\ndữ liệu",
                "Chia sẻ ứng dụng",
                "Liên hệ, góp ý"
        };

        int[] images = new int[] {
                R.drawable.baseline_home_24,
                R.drawable.baseline_receipt_24,
                R.drawable.baseline_bar_chart_24,
                R.drawable.baseline_bar_chart_24,
                R.drawable.baseline_receipt_24,
                R.drawable.ic_share,
                R.drawable.ic_arrow_back
        };

        boolean[] locked = new boolean[] { false, false, false, false, false, false, false };

        GridAdapter adapter = new GridAdapter(this, items, images, locked);
        gridView.setAdapter(adapter);

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            switch (position) {
                case 0:
                    startActivity(new Intent(this, CanNhaActivity.class));
                    break;
                case 1:
                    startActivity(new Intent(this, HoaDonActivity.class));
                    break;
                case 2:
                    startActivity(new Intent(this, ChiPhiActivity.class));
                    break;
                case 3:
                    startActivity(new Intent(this, DoanhThuActivity.class));
                    break;
                case 4:
                    startActivity(new Intent(this, BackupRestoreActivity.class));
                    break;
                case 5:
                    shareApp();
                    break;
                case 6:
                    Toast.makeText(this, "Liên hệ hỗ trợ: 0987.654.321", Toast.LENGTH_SHORT).show();
                    break;
            }
        });
    }

    private void updateStatistics() {
        new CanNhaRepository().listAll().observe(this, list -> {
            if (list != null && tvTotalHouses != null)
                tvTotalHouses.setText(String.valueOf(list.size()));
        });

        PhongTroViewModel phongViewModel = new ViewModelProvider(this).get(PhongTroViewModel.class);
        phongViewModel.getDanhSachPhong().observe(this, list -> {
            if (list != null) {
                long vacant = 0;
                long rented = 0;
                for (PhongTro p : list) {
                    if (RoomStatus.VACANT.equals(p.getTrangThai()))
                        vacant++;
                    else
                        rented++;
                }
                if (tvVacantRooms != null)
                    tvVacantRooms.setText(String.valueOf(vacant));
                if (tvRentedRooms != null)
                    tvRentedRooms.setText(String.valueOf(rented));
            }
        });
    }

    private void shareApp() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Quản Lý Trọ");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Ứng dụng quản lý nhà trọ tuyệt vời dành cho bạn!");
        startActivity(Intent.createChooser(shareIntent, "Chia sẻ ứng dụng"));
    }
}
