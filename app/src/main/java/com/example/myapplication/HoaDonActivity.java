package com.example.myapplication;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.model.HoaDon;
import com.example.myapplication.model.PhongTro;
import com.example.myapplication.viewmodel.HoaDonViewModel;
import com.example.myapplication.viewmodel.PhongTroViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HoaDonActivity extends AppCompatActivity {

    private HoaDonViewModel viewModel;
    private HoaDonAdapter adapter;
    private TextView tvEmpty;
    private List<PhongTro> danhSachPhong = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        window.setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_hoa_don);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Quản lý hóa đơn");
        }

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        tvEmpty = findViewById(R.id.tvEmpty);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        FloatingActionButton fabThem = findViewById(R.id.fabThem);

        adapter = new HoaDonAdapter(new HoaDonAdapter.OnItemActionListener() {
            @Override
            public void onXoa(HoaDon hoaDon) {
                new AlertDialog.Builder(HoaDonActivity.this)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Xóa hóa đơn tháng " + hoaDon.getThangNam() + "?")
                        .setPositiveButton("Xóa", (d2, w2) ->
                                viewModel.xoaHoaDon(hoaDon.getId(),
                                        () -> runOnUiThread(() -> Toast.makeText(HoaDonActivity.this, "Đã xóa", Toast.LENGTH_SHORT).show()),
                                        () -> runOnUiThread(() -> Toast.makeText(HoaDonActivity.this, "Xóa thất bại", Toast.LENGTH_SHORT).show())))
                        .setNegativeButton("Hủy", null).show();
            }

            @Override
            public void onDoiTrangThai(HoaDon hoaDon) {
                String moiTT = "Đã thanh toán".equals(hoaDon.getTrangThai()) ? "Chưa thanh toán" : "Đã thanh toán";
                viewModel.capNhatTrangThai(hoaDon.getId(), moiTT, 
                    () -> runOnUiThread(() -> Toast.makeText(HoaDonActivity.this, "Đã cập nhật trạng thái", Toast.LENGTH_SHORT).show()), 
                    () -> {});
            }

            @Override
            public void onSua(HoaDon hoaDon) {
                hienDialogSuaHoaDon(hoaDon);
            }

            @Override
            public void onXuat(HoaDon hoaDon) {
                hienDialogXuatHoaDon(hoaDon);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(HoaDonViewModel.class);
        viewModel.getDanhSachHoaDon().observe(this, list -> {
            adapter.setDanhSach(list);
            tvEmpty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
        });

        new ViewModelProvider(this).get(PhongTroViewModel.class)
                .getDanhSachPhong().observe(this, list -> danhSachPhong = list);

        fabThem.setOnClickListener(v -> hienDialogThemHoaDon());
    }

    private void hienDialogXuatHoaDon(HoaDon h) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_xuat_hoa_don, null);
        
        TextView tvTitle = view.findViewById(R.id.tvBillTitle);
        TextView tvChiTiet = view.findViewById(R.id.tvBillDetails);
        TextView tvTong = view.findViewById(R.id.tvBillTotal);
        
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        
        tvTitle.setText("HÓA ĐƠN PHÒNG " + h.getSoPhong());
        
        StringBuilder sb = new StringBuilder();
        sb.append("Tháng: ").append(h.getThangNam()).append("\n\n");
        sb.append("- Tiền phòng: ").append(fmt.format(h.getGiaThue())).append("\n");
        
        double tienDien = (h.getChiSoDienCuoi() - h.getChiSoDienDau()) * h.getDonGiaDien();
        sb.append("- Tiền điện: ").append(fmt.format(tienDien))
          .append(" (").append((int)h.getChiSoDienDau()).append(" -> ").append((int)h.getChiSoDienCuoi()).append(")\n");
          
        double tienNuoc = (h.getChiSoNuocCuoi() - h.getChiSoNuocDau()) * h.getDonGiaNuoc();
        sb.append("- Tiền nước: ").append(fmt.format(tienNuoc))
          .append(" (").append((int)h.getChiSoNuocDau()).append(" -> ").append((int)h.getChiSoNuocCuoi()).append(")\n");
          
        if (h.getPhiRac() > 0) sb.append("- Phí rác: ").append(fmt.format(h.getPhiRac())).append("\n");
        if (h.getPhiWifi() > 0) sb.append("- Phí Wifi: ").append(fmt.format(h.getPhiWifi())).append("\n");
        if (h.getPhiGuiXe() > 0) sb.append("- Phí gửi xe: ").append(fmt.format(h.getPhiGuiXe())).append("\n");
        
        tvChiTiet.setText(sb.toString());
        tvTong.setText("TỔNG CỘNG: " + fmt.format(h.getTongTien()));

        new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton("Đóng", null)
                .setNeutralButton("Chụp màn hình", (d, w) -> {
                    Toast.makeText(this, "Hãy chụp màn hình để gửi hóa đơn", Toast.LENGTH_LONG).show();
                })
                .show();
    }

    private void hienDialogThemHoaDon() {
        if (danhSachPhong.isEmpty()) {
            Toast.makeText(this, "Vui lòng thêm phòng trước", Toast.LENGTH_SHORT).show();
            return;
        }
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_them_hoa_don, null);
        Spinner spinnerPhong = dialogView.findViewById(R.id.spinnerPhong);
        EditText etThangNam = dialogView.findViewById(R.id.etThangNam);
        EditText etDienDau = dialogView.findViewById(R.id.etDienDau);
        EditText etDienCuoi = dialogView.findViewById(R.id.etDienCuoi);
        EditText etDonGiaDien = dialogView.findViewById(R.id.etDonGiaDien);
        EditText etNuocDau = dialogView.findViewById(R.id.etNuocDau);
        EditText etNuocCuoi = dialogView.findViewById(R.id.etNuocCuoi);
        EditText etDonGiaNuoc = dialogView.findViewById(R.id.etDonGiaNuoc);
        EditText etPhiRac = dialogView.findViewById(R.id.etPhiRac);
        EditText etPhiWifi = dialogView.findViewById(R.id.etPhiWifi);
        EditText etPhiGuiXe = dialogView.findViewById(R.id.etPhiGuiXe);

        String[] phongNames = danhSachPhong.stream()
                .map(p -> "Phòng " + p.getSoPhong()).toArray(String[]::new);
        ArrayAdapter<String> phongAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, phongNames);
        phongAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPhong.setAdapter(phongAdapter);
        etThangNam.setText(new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(new Date()));

        new AlertDialog.Builder(this)
                .setTitle("Tạo hóa đơn")
                .setView(dialogView)
                .setPositiveButton("Tạo", (d, w) -> {
                    try {
                        PhongTro phongChon = danhSachPhong.get(spinnerPhong.getSelectedItemPosition());
                        HoaDon hd = new HoaDon();
                        hd.setIdPhong(phongChon.getId());
                        hd.setSoPhong(phongChon.getSoPhong());
                        hd.setGiaThue(phongChon.getGiaThue());
                        hd.setThangNam(etThangNam.getText().toString().trim());
                        hd.setChiSoDienDau(parseDouble(etDienDau));
                        hd.setChiSoDienCuoi(parseDouble(etDienCuoi));
                        hd.setDonGiaDien(parseDouble(etDonGiaDien));
                        hd.setChiSoNuocDau(parseDouble(etNuocDau));
                        hd.setChiSoNuocCuoi(parseDouble(etNuocCuoi));
                        hd.setDonGiaNuoc(parseDouble(etDonGiaNuoc));
                        hd.setPhiRac(parseDouble(etPhiRac));
                        hd.setPhiWifi(parseDouble(etPhiWifi));
                        hd.setPhiGuiXe(parseDouble(etPhiGuiXe));
                        hd.setTrangThai("Chưa thanh toán");
                        viewModel.themHoaDon(hd,
                                () -> runOnUiThread(() -> Toast.makeText(this, "Tạo hóa đơn thành công!", Toast.LENGTH_SHORT).show()),
                                () -> runOnUiThread(() -> Toast.makeText(this, "Thất bại — kiểm tra Firebase", Toast.LENGTH_LONG).show()));
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Số liệu không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null).show();
    }

    private double parseDouble(EditText et) {
        String s = et.getText().toString().trim();
        return s.isEmpty() ? 0 : Double.parseDouble(s);
    }

    private void hienDialogSuaHoaDon(HoaDon hoaDon) {
        if (danhSachPhong.isEmpty()) {
            Toast.makeText(this, "Không tải được danh sách phòng", Toast.LENGTH_SHORT).show();
            return;
        }
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_them_hoa_don, null);
        Spinner spinnerPhong = dialogView.findViewById(R.id.spinnerPhong);
        EditText etThangNam = dialogView.findViewById(R.id.etThangNam);
        EditText etDienDau = dialogView.findViewById(R.id.etDienDau);
        EditText etDienCuoi = dialogView.findViewById(R.id.etDienCuoi);
        EditText etDonGiaDien = dialogView.findViewById(R.id.etDonGiaDien);
        EditText etNuocDau = dialogView.findViewById(R.id.etNuocDau);
        EditText etNuocCuoi = dialogView.findViewById(R.id.etNuocCuoi);
        EditText etDonGiaNuoc = dialogView.findViewById(R.id.etDonGiaNuoc);
        EditText etPhiRac = dialogView.findViewById(R.id.etPhiRac);
        EditText etPhiWifi = dialogView.findViewById(R.id.etPhiWifi);
        EditText etPhiGuiXe = dialogView.findViewById(R.id.etPhiGuiXe);

        String[] phongNames = danhSachPhong.stream()
                .map(p -> "Phòng " + p.getSoPhong()).toArray(String[]::new);
        ArrayAdapter<String> phongAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, phongNames);
        phongAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPhong.setAdapter(phongAdapter);

        for (int i = 0; i < danhSachPhong.size(); i++) {
            if (danhSachPhong.get(i).getId().equals(hoaDon.getIdPhong())) {
                spinnerPhong.setSelection(i);
                break;
            }
        }
        etThangNam.setText(hoaDon.getThangNam());
        etDienDau.setText(formatDouble(hoaDon.getChiSoDienDau()));
        etDienCuoi.setText(formatDouble(hoaDon.getChiSoDienCuoi()));
        etDonGiaDien.setText(formatDouble(hoaDon.getDonGiaDien()));
        etNuocDau.setText(formatDouble(hoaDon.getChiSoNuocDau()));
        etNuocCuoi.setText(formatDouble(hoaDon.getChiSoNuocCuoi()));
        etDonGiaNuoc.setText(formatDouble(hoaDon.getDonGiaNuoc()));
        etPhiRac.setText(formatDouble(hoaDon.getPhiRac()));
        etPhiWifi.setText(formatDouble(hoaDon.getPhiWifi()));
        etPhiGuiXe.setText(formatDouble(hoaDon.getPhiGuiXe()));

        new AlertDialog.Builder(this)
                .setTitle("Chỉnh sửa hóa đơn tháng " + hoaDon.getThangNam())
                .setView(dialogView)
                .setPositiveButton("Cập nhật", (d, w) -> {
                    try {
                        PhongTro phongChon = danhSachPhong.get(spinnerPhong.getSelectedItemPosition());
                        HoaDon updated = new HoaDon();
                        updated.setId(hoaDon.getId());
                        updated.setIdPhong(phongChon.getId());
                        updated.setSoPhong(phongChon.getSoPhong());
                        updated.setGiaThue(phongChon.getGiaThue());
                        updated.setThangNam(etThangNam.getText().toString().trim());
                        updated.setChiSoDienDau(parseDouble(etDienDau));
                        updated.setChiSoDienCuoi(parseDouble(etDienCuoi));
                        updated.setDonGiaDien(parseDouble(etDonGiaDien));
                        updated.setChiSoNuocDau(parseDouble(etNuocDau));
                        updated.setChiSoNuocCuoi(parseDouble(etNuocCuoi));
                        updated.setDonGiaNuoc(parseDouble(etDonGiaNuoc));
                        updated.setPhiRac(parseDouble(etPhiRac));
                        updated.setPhiWifi(parseDouble(etPhiWifi));
                        updated.setPhiGuiXe(parseDouble(etPhiGuiXe));
                        updated.setTrangThai(hoaDon.getTrangThai());
                        viewModel.capNhatHoaDon(updated,
                                () -> runOnUiThread(() -> Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()),
                                () -> runOnUiThread(() -> Toast.makeText(this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show()));
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Số liệu không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null).show();
    }

    private String formatDouble(double value) {
        return value % 1 == 0 ? String.valueOf((long) value) : String.valueOf(value);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}