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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.model.HoaDon;
import com.example.myapplication.model.Payment;
import com.example.myapplication.model.PhongTro;
import com.example.myapplication.repository.PaymentRepository;
import com.example.myapplication.viewmodel.HoaDonViewModel;
import com.example.myapplication.viewmodel.PhongTroViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import android.widget.AdapterView;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HoaDonActivity extends AppCompatActivity {

    private boolean isTenantUser;
    private String tenantRoomId;

    private HoaDonViewModel viewModel;
    private HoaDonAdapter adapter;
    private TextView tvEmpty;
    private List<PhongTro> danhSachPhong = new ArrayList<>();

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final PaymentRepository paymentRepository = new PaymentRepository();

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
        FloatingActionButton fabChotKy = findViewById(R.id.fabChotKy);

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
                if (InvoiceStatus.PAID.equals(hoaDon.getTrangThai())) {
                    viewModel.capNhatTrangThai(hoaDon.getId(), InvoiceStatus.UNPAID,
                            () -> runOnUiThread(() -> Toast.makeText(HoaDonActivity.this, "Đã cập nhật trạng thái", Toast.LENGTH_SHORT).show()),
                            () -> {});
                    return;
                }

                hienDialogThuTien(hoaDon);
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
        setupInvoiceObserverAndPermissions();

        fabThem.setOnClickListener(v -> hienDialogThemHoaDon());
        if (fabChotKy != null) {
            fabChotKy.setOnClickListener(v -> hienDialogChotKy());
        }
    }

    private void setupInvoiceObserverAndPermissions() {
        String tenantId = TenantSession.getActiveTenantId();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // Legacy mode (no active tenant) -> keep old behavior
        if (tenantId == null || tenantId.isEmpty() || user == null) {
            viewModel.getDanhSachHoaDon().observe(this, list -> {
                adapter.setDanhSach(list);
                tvEmpty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
            });

            new ViewModelProvider(this).get(PhongTroViewModel.class)
                    .getDanhSachPhong().observe(this, list -> danhSachPhong = list);
            return;
        }

        db.collection("tenants").document(tenantId)
                .collection("members").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String role = doc.getString("role");
                    boolean isTenant = TenantRoles.TENANT.equals(role);

                    if (isTenant) {
                        isTenantUser = true;
                        String roomId = doc.getString("roomId");
                        tenantRoomId = roomId;
                        if (roomId == null || roomId.trim().isEmpty()) {
                            Toast.makeText(this, "Thiếu roomId cho tài khoản TENANT", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }

                        adapter.setReadOnly(true);
                        fabThem.setVisibility(View.GONE);
                        if (fabChotKy != null) fabChotKy.setVisibility(View.GONE);

                        viewModel.getHoaDonTheoPhong(roomId).observe(this, list -> {
                            adapter.setDanhSach(list);
                            tvEmpty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
                        });
                        return;
                    }

                    isTenantUser = false;
                    tenantRoomId = null;
                    adapter.setReadOnly(false);
                    viewModel.getDanhSachHoaDon().observe(this, list -> {
                        adapter.setDanhSach(list);
                        tvEmpty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
                    });

                    new ViewModelProvider(this).get(PhongTroViewModel.class)
                            .getDanhSachPhong().observe(this, list -> danhSachPhong = list);
                })
                .addOnFailureListener(e -> {
                    viewModel.getDanhSachHoaDon().observe(this, list -> {
                        adapter.setDanhSach(list);
                        tvEmpty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
                    });

                    new ViewModelProvider(this).get(PhongTroViewModel.class)
                            .getDanhSachPhong().observe(this, list -> danhSachPhong = list);
                });
    }

    private void ensureInvoiceQuotaThen(@NonNull String period, int toCreate, @NonNull Runnable onAllowed) {
        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId == null || tenantId.trim().isEmpty()) {
            onAllowed.run();
            return;
        }

        db.collection("tenants").document(tenantId).get()
                .addOnSuccessListener(tdoc -> {
                    Long maxL = tdoc.getLong("maxInvoicesPerMonth");
                    int max = maxL != null ? maxL.intValue() : 200;

                    db.collection("tenants").document(tenantId).collection("hoa_don")
                            .whereEqualTo("thangNam", period)
                            .get()
                            .addOnSuccessListener(qs -> {
                                int current = qs != null ? qs.size() : 0;
                                if (current + toCreate > max) {
                                    Toast.makeText(this, "Đã vượt quota hoá đơn/tháng (" + max + ")", Toast.LENGTH_LONG).show();
                                    return;
                                }
                                onAllowed.run();
                            })
                            .addOnFailureListener(e -> onAllowed.run());
                })
                .addOnFailureListener(e -> onAllowed.run());
    }

    private void hienDialogChotKy() {
        if (danhSachPhong == null || danhSachPhong.isEmpty()) {
            Toast.makeText(this, "Chưa có phòng để chốt kỳ", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_chot_ky, null);
        EditText etThangNam = dialogView.findViewById(R.id.etThangNam);
        EditText etDonGiaDien = dialogView.findViewById(R.id.etDonGiaDien);
        EditText etDonGiaNuoc = dialogView.findViewById(R.id.etDonGiaNuoc);
        EditText etPhiRac = dialogView.findViewById(R.id.etPhiRac);
        EditText etPhiWifi = dialogView.findViewById(R.id.etPhiWifi);
        EditText etPhiGuiXe = dialogView.findViewById(R.id.etPhiGuiXe);

        etThangNam.setText(new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(new Date()));

        new AlertDialog.Builder(this)
                .setTitle("Chốt kỳ (tạo nháp hoá đơn)")
                .setView(dialogView)
                .setPositiveButton("Tạo", (d, w) -> {
                    String period = etThangNam.getText().toString().trim();
                    double donGiaDien = parseDouble(etDonGiaDien);
                    double donGiaNuoc = parseDouble(etDonGiaNuoc);
                    double phiRac = parseDouble(etPhiRac);
                    double phiWifi = parseDouble(etPhiWifi);
                    double phiGuiXe = parseDouble(etPhiGuiXe);

                    ensureInvoiceQuotaThen(period, danhSachPhong.size(), () -> {
                        Toast.makeText(this, "Đang tạo hoá đơn nháp...", Toast.LENGTH_SHORT).show();

                        for (PhongTro phong : danhSachPhong) {
                            loadLatestMeterEnds(phong.getId(), (elecEnd, waterEnd) -> {
                                HoaDon hd = new HoaDon();
                                hd.setIdPhong(phong.getId());
                                hd.setSoPhong(phong.getSoPhong());
                                hd.setGiaThue(phong.getGiaThue());
                                hd.setThangNam(period);

                            // Nháp: đầu = cuối = chỉ số kỳ trước (0 tiêu thụ). Bạn sửa lại sau.
                            hd.setChiSoDienDau(elecEnd);
                            hd.setChiSoDienCuoi(elecEnd);
                            hd.setDonGiaDien(donGiaDien);
                            hd.setChiSoNuocDau(waterEnd);
                            hd.setChiSoNuocCuoi(waterEnd);
                            hd.setDonGiaNuoc(donGiaNuoc);
                            hd.setPhiRac(phiRac);
                            hd.setPhiWifi(phiWifi);
                            hd.setPhiGuiXe(phiGuiXe);
                            hd.setTrangThai(InvoiceStatus.UNPAID);

                                viewModel.themHoaDonUnique(hd,
                                        () -> saveMeterReadingFromInvoice(phong.getId(), period, elecEnd, elecEnd, waterEnd, waterEnd),
                                        () -> {},
                                        () -> {});
                            });
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
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
        
        tvChiTiet.setText(sb.toString() + "\n\n(Đang tải thanh toán...) ");
        tvTong.setText("TỔNG CỘNG: " + fmt.format(h.getTongTien()));

        paymentRepository.listByInvoice(h.getId()).observe(this, payments -> {
            if (payments == null) return;
            double paid = 0;
            for (Payment p : payments) {
                paid += p.getAmount();
            }
            double remaining = Math.max(0, h.getTongTien() - paid);
            String extra = "\n\n── Thanh toán ──\n" +
                    "Đã thu: " + fmt.format(paid) + "\n" +
                    "Còn lại: " + fmt.format(remaining);
            tvChiTiet.setText(sb.toString() + extra);
        });

        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton("Đóng", null)
                .setNegativeButton("Lịch sử thanh toán", (d, w) -> {
                    android.content.Intent intent = new android.content.Intent(HoaDonActivity.this, LichSuThanhToanActivity.class);
                    intent.putExtra("INVOICE_ID", h.getId());
                    intent.putExtra("INVOICE_TOTAL", h.getTongTien());
                    intent.putExtra("ROOM_ID", h.getIdPhong());
                    intent.putExtra("TITLE", "Thanh toán • Phòng " + h.getSoPhong() + " • " + h.getThangNam());
                    startActivity(intent);
                });

        if (isTenantUser) {
            b.setNeutralButton("Xác nhận công tơ", (d, w) -> showTenantConfirmMeterDialog(h));
        } else {
            b.setNeutralButton("Chụp màn hình", (d, w) -> Toast.makeText(this, "Hãy chụp màn hình để gửi hóa đơn", Toast.LENGTH_LONG).show());
        }

        b.show();
    }

    private void showTenantConfirmMeterDialog(HoaDon h) {
        if (!isTenantUser) return;
        if (h == null || h.getIdPhong() == null) return;

        String periodKey = toPeriodKey(h.getThangNam());
        if (periodKey.isEmpty()) {
            Toast.makeText(this, "Kỳ không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 10);

        android.widget.RadioGroup rg = new android.widget.RadioGroup(this);
        android.widget.RadioButton rbOk = new android.widget.RadioButton(this);
        rbOk.setId(View.generateViewId());
        rbOk.setText("Đồng ý chỉ số");
        android.widget.RadioButton rbNo = new android.widget.RadioButton(this);
        rbNo.setId(View.generateViewId());
        rbNo.setText("Không đồng ý");
        rg.addView(rbOk);
        rg.addView(rbNo);
        rg.check(rbOk.getId());
        layout.addView(rg);

        EditText etNote = new EditText(this);
        etNote.setHint("Ghi chú (tuỳ chọn)");
        layout.addView(etNote);

        new AlertDialog.Builder(this)
                .setTitle("Xác nhận chốt số")
                .setView(layout)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Gửi", (d, w) -> {
                    String status = (rg.getCheckedRadioButtonId() == rbNo.getId()) ? "DISPUTED" : "APPROVED";
                    String note = etNote.getText().toString().trim();

                    String docId = h.getIdPhong() + "_" + periodKey;
                    java.util.Map<String, Object> update = new java.util.HashMap<>();
                    update.put("tenantConfirmStatus", status);
                    update.put("tenantConfirmNote", note);
                    update.put("tenantConfirmAt", com.google.firebase.Timestamp.now());

                    scopedCollection("meterReadings").document(docId)
                            .update(update)
                            .addOnSuccessListener(v -> Toast.makeText(this, "Đã gửi xác nhận", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Gửi thất bại", Toast.LENGTH_SHORT).show());
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

        final double[] lastElecEnd = {0};
        final double[] lastWaterEnd = {0};
        spinnerPhong.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PhongTro phongChon = danhSachPhong.get(position);
                loadLatestMeterEnds(phongChon.getId(), (elecEnd, waterEnd) -> {
                    lastElecEnd[0] = elecEnd;
                    lastWaterEnd[0] = waterEnd;
                    if (etDienDau.getText().toString().trim().isEmpty() || "0".equals(etDienDau.getText().toString().trim())) {
                        etDienDau.setText(formatDouble(elecEnd));
                    }
                    if (etNuocDau.getText().toString().trim().isEmpty() || "0".equals(etNuocDau.getText().toString().trim())) {
                        etNuocDau.setText(formatDouble(waterEnd));
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

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

                        double dienDau = parseDouble(etDienDau);
                        double dienCuoi = parseDouble(etDienCuoi);
                        double nuocDau = parseDouble(etNuocDau);
                        double nuocCuoi = parseDouble(etNuocCuoi);

                        if (dienDau < lastElecEnd[0] || nuocDau < lastWaterEnd[0]) {
                            Toast.makeText(this, "Chỉ số đầu kỳ phải >= chỉ số cuối kỳ trước", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (dienCuoi < dienDau || nuocCuoi < nuocDau) {
                            Toast.makeText(this, "Chỉ số cuối không được nhỏ hơn chỉ số đầu", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        hd.setChiSoDienDau(dienDau);
                        hd.setChiSoDienCuoi(dienCuoi);
                        hd.setDonGiaDien(parseDouble(etDonGiaDien));
                        hd.setChiSoNuocDau(nuocDau);
                        hd.setChiSoNuocCuoi(nuocCuoi);
                        hd.setDonGiaNuoc(parseDouble(etDonGiaNuoc));
                        hd.setPhiRac(parseDouble(etPhiRac));
                        hd.setPhiWifi(parseDouble(etPhiWifi));
                        hd.setPhiGuiXe(parseDouble(etPhiGuiXe));
                        hd.setTrangThai(InvoiceStatus.UNPAID);
                        viewModel.themHoaDonUnique(hd,
                                () -> {
                                    saveMeterReadingFromInvoice(phongChon.getId(), hd.getThangNam(), dienDau, dienCuoi, nuocDau, nuocCuoi);
                                    runOnUiThread(() -> Toast.makeText(this, "Tạo hóa đơn thành công!", Toast.LENGTH_SHORT).show());
                                },
                                () -> runOnUiThread(() -> Toast.makeText(this, "Hóa đơn kỳ này đã tồn tại", Toast.LENGTH_SHORT).show()),
                                () -> runOnUiThread(() -> Toast.makeText(this, "Thất bại — kiểm tra Firebase", Toast.LENGTH_LONG).show())) ;
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

        // Tránh phát sinh trùng/move document: không cho đổi Phòng/Kỳ khi sửa
        spinnerPhong.setEnabled(false);
        spinnerPhong.setAlpha(0.6f);
        etThangNam.setEnabled(false);
        etThangNam.setAlpha(0.6f);

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

                        double dienDau = parseDouble(etDienDau);
                        double dienCuoi = parseDouble(etDienCuoi);
                        double nuocDau = parseDouble(etNuocDau);
                        double nuocCuoi = parseDouble(etNuocCuoi);

                        if (dienCuoi < dienDau || nuocCuoi < nuocDau) {
                            Toast.makeText(this, "Chỉ số cuối không được nhỏ hơn chỉ số đầu", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        updated.setChiSoDienDau(dienDau);
                        updated.setChiSoDienCuoi(dienCuoi);
                        updated.setDonGiaDien(parseDouble(etDonGiaDien));
                        updated.setChiSoNuocDau(nuocDau);
                        updated.setChiSoNuocCuoi(nuocCuoi);
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

    private interface MeterEndCallback {
        void onResult(double elecEnd, double waterEnd);
    }

    private void loadLatestMeterEnds(String roomId, MeterEndCallback callback) {
        scopedCollection("meterReadings").whereEqualTo("roomId", roomId).get()
                .addOnSuccessListener(snapshot -> {
                    String bestKey = null;
                    double bestElecEnd = 0;
                    double bestWaterEnd = 0;

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String key = doc.getString("periodKey");
                        if (key == null) {
                            String period = doc.getString("period");
                            key = toPeriodKey(period);
                        }
                        if (key == null) key = "";

                        if (bestKey == null || key.compareTo(bestKey) > 0) {
                            bestKey = key;
                            Double e = doc.getDouble("elecEnd");
                            Double w = doc.getDouble("waterEnd");
                            bestElecEnd = e != null ? e : 0;
                            bestWaterEnd = w != null ? w : 0;
                        }
                    }

                    callback.onResult(bestElecEnd, bestWaterEnd);
                })
                .addOnFailureListener(e -> callback.onResult(0, 0));
    }

    private void saveMeterReadingFromInvoice(String roomId, String period, double elecStart, double elecEnd, double waterStart, double waterEnd) {
        String periodKey = toPeriodKey(period);
        if (periodKey == null || periodKey.isEmpty()) return;

        String docId = roomId + "_" + periodKey;
        Map<String, Object> data = new HashMap<>();
        data.put("roomId", roomId);
        data.put("period", period);
        data.put("periodKey", periodKey);
        data.put("elecStart", elecStart);
        data.put("elecEnd", elecEnd);
        data.put("waterStart", waterStart);
        data.put("waterEnd", waterEnd);

        scopedCollection("meterReadings").document(docId).set(data);
    }

    private String toPeriodKey(String period) {
        if (period == null) return "";
        String[] parts = period.trim().split("/");
        if (parts.length != 2) return "";
        try {
            int month = Integer.parseInt(parts[0]);
            int year = Integer.parseInt(parts[1]);
            if (month < 1 || month > 12) return "";
            return String.format(Locale.US, "%04d%02d", year, month);
        } catch (NumberFormatException e) {
            return "";
        }
    }

    private CollectionReference scopedCollection(String collection) {
        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId != null && !tenantId.isEmpty()) {
            return db.collection("tenants").document(tenantId).collection(collection);
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) throw new IllegalStateException("User not logged in");
        return db.collection("users").document(user.getUid()).collection(collection);
    }

    private void hienDialogThuTien(HoaDon hoaDon) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_them_thanh_toan, null);
        EditText etAmount = dialogView.findViewById(R.id.etAmount);
        Spinner spinnerMethod = dialogView.findViewById(R.id.spinnerMethod);
        EditText etPaidAt = dialogView.findViewById(R.id.etPaidAt);
        EditText etNote = dialogView.findViewById(R.id.etNote);

        etAmount.setText(formatDouble(hoaDon.getTongTien()));
        etPaidAt.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));

        ArrayAdapter<String> methodAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"Tiền mặt", "Chuyển khoản"});
        methodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMethod.setAdapter(methodAdapter);

        new AlertDialog.Builder(this)
                .setTitle("Thu tiền — Phòng " + hoaDon.getSoPhong())
                .setView(dialogView)
                .setPositiveButton("Xác nhận", (d, w) -> {
                    try {
                        Payment p = new Payment();
                        p.setInvoiceId(hoaDon.getId());
                        p.setRoomId(hoaDon.getIdPhong());
                        p.setAmount(parseDouble(etAmount));
                        p.setMethod(spinnerMethod.getSelectedItemPosition() == 0 ? "CASH" : "BANK");
                        p.setPaidAt(etPaidAt.getText().toString().trim());
                        p.setNote(etNote.getText().toString().trim());

                        paymentRepository.add(p,
                                () -> viewModel.capNhatTrangThai(hoaDon.getId(), InvoiceStatus.PAID,
                                        () -> runOnUiThread(() -> Toast.makeText(this, "Đã ghi nhận thanh toán", Toast.LENGTH_SHORT).show()),
                                        () -> runOnUiThread(() -> Toast.makeText(this, "Cập nhật trạng thái thất bại", Toast.LENGTH_SHORT).show())),
                                () -> runOnUiThread(() -> Toast.makeText(this, "Ghi nhận thanh toán thất bại", Toast.LENGTH_SHORT).show()));
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Số liệu không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("QR chuyển khoản", (d, w) -> showVietQrDialog(hoaDon))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showVietQrDialog(@NonNull HoaDon hoaDon) {
        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId == null || tenantId.trim().isEmpty()) {
            Toast.makeText(this, "Chỉ hỗ trợ VietQR khi dùng tenant scope", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("tenants").document(tenantId).get().addOnSuccessListener(tdoc -> {
            String bankCode = tdoc.getString("bankCode");
            String bankNo = tdoc.getString("bankAccountNo");
            String bankName = tdoc.getString("bankAccountName");

            if (bankCode == null || bankCode.trim().isEmpty() || bankNo == null || bankNo.trim().isEmpty()) {
                Toast.makeText(this, "Chưa cấu hình thông tin chuyển khoản (Org Admin)", Toast.LENGTH_LONG).show();
                return;
            }

            String addInfo = "HD " + hoaDon.getSoPhong() + " " + hoaDon.getThangNam();
            String url = buildVietQrUrl(
                    bankCode.trim(),
                    bankNo.trim(),
                    bankName != null ? bankName.trim() : "",
                    (long) hoaDon.getTongTien(),
                    addInfo
            );

            View v = getLayoutInflater().inflate(R.layout.dialog_vietqr, null);
            android.widget.ImageView img = v.findViewById(R.id.imgQr);
            android.widget.TextView tvBank = v.findViewById(R.id.tvQrBank);
            android.widget.TextView tvNote = v.findViewById(R.id.tvQrNote);

            tvBank.setText("CK: " + bankCode + " - " + bankNo + (bankName != null && !bankName.isEmpty() ? (" (" + bankName + ")") : ""));
            tvNote.setText("Nội dung: " + addInfo + "\nSố tiền: " + formatDouble(hoaDon.getTongTien()));

            com.bumptech.glide.Glide.with(this).load(url).into(img);

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("VietQR")
                    .setView(v)
                    .setPositiveButton("Đóng", null)
                    .show();
        });
    }

    private String buildVietQrUrl(@NonNull String bankCode, @NonNull String accountNo, @NonNull String accountName, long amount, @NonNull String addInfo) {
        try {
            String base = "https://img.vietqr.io/image/" + bankCode + "-" + accountNo + "-compact2.png";
            String q = "?amount=" + amount
                    + "&addInfo=" + java.net.URLEncoder.encode(addInfo, "UTF-8")
                    + "&accountName=" + java.net.URLEncoder.encode(accountName, "UTF-8");
            return base + q;
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}