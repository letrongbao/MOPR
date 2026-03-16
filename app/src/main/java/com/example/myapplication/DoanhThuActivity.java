package com.example.myapplication;

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
import com.example.myapplication.model.HoaDon;
import com.example.myapplication.viewmodel.HoaDonViewModel;
import com.example.myapplication.viewmodel.NguoiThueViewModel;
import com.example.myapplication.viewmodel.PhongTroViewModel;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DoanhThuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Làm status bar trong suốt giống HomeActivity
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
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
                    if (list == null) return;
                    tvTongPhong.setText(String.valueOf(list.size()));
                    long daThua = 0;
                    for (var p : list) {
                        if ("Đã thuê".equals(p.getTrangThai())) daThua++;
                    }
                    tvPhongDaThua.setText(String.valueOf(daThua));
                });

        viewModelProvider.get(NguoiThueViewModel.class)
                .getDanhSachNguoiThue().observe(this, list -> {
                    if (list != null) tvSoNguoiThue.setText(String.valueOf(list.size()));
                });

        viewModelProvider.get(HoaDonViewModel.class)
                .getDanhSachHoaDon().observe(this, list -> {
                    if (list == null) return;
                    int chuaTT = 0;
                    double doanhThu = 0;
                    for (HoaDon h : list) {
                        if ("Chưa thanh toán".equals(h.getTrangThai())) chuaTT++;
                        if ("Đã thanh toán".equals(h.getTrangThai()) && 
                            h.getThangNam() != null && h.getThangNam().trim().equals(thangNay)) {
                            doanhThu += h.getTongTien();
                        }
                    }
                    tvSoHDChuaTT.setText(String.valueOf(chuaTT));
                    tvDoanhThuThang.setText(fmt.format(doanhThu));
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}