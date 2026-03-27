package com.example.myapplication.features.property.room;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.myapplication.core.util.ContactHelper;
import com.example.myapplication.R;
import com.example.myapplication.core.constants.RoomStatus;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.api.RetrofitClient;
import com.example.myapplication.domain.NguoiThue;
import com.example.myapplication.domain.NominatimResult;
import com.example.myapplication.domain.PhongTro;
import com.example.myapplication.room.AppDatabase;
import com.example.myapplication.room.PhongYeuThich;
import com.example.myapplication.room.PhongYeuThichDao;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import java.text.NumberFormat;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChiTietPhongTroActivity extends AppCompatActivity {

    private ImageView imgPhong;
    private TextView tvSoPhong, tvLoaiPhong, tvDienTich, tvGiaThue, tvTrangThai, tvTrangThaiRow;
    private TextView tvTenNguoiThue, tvSdtNguoiThue;
    private CardView cardNguoiThue;
    private LinearLayout btnGoiDien, btnNhanTin, btnChiDuong, btnChiaSe;
    private FloatingActionButton fabFavorite;
    private PhongYeuThichDao favoriteDao;
    private PhongTro currentPhong;
    private volatile boolean isFavorite = false;

    private String soDienThoaiNguoiThue;
    private String tenPhongHienTai;
    private double giaThueHienTai;

    private ListenerRegistration roomListener;
    private ListenerRegistration tenantListener;

    private String phongId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Transparent status bar
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_chi_tiet_phong_tro);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Chi tiết phòng");
        }
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        imgPhong = findViewById(R.id.imgPhongChiTiet);
        tvSoPhong = findViewById(R.id.tvSoPhongChiTiet);
        tvLoaiPhong = findViewById(R.id.tvLoaiPhongChiTiet);
        tvDienTich = findViewById(R.id.tvDienTichChiTiet);
        tvGiaThue = findViewById(R.id.tvGiaThueChiTiet);
        tvTrangThai = findViewById(R.id.tvTrangThaiChiTiet);
        tvTrangThaiRow = findViewById(R.id.tvTrangThaiRow);
        tvTenNguoiThue = findViewById(R.id.tvTenNguoiThue);
        tvSdtNguoiThue = findViewById(R.id.tvSdtNguoiThue);
        cardNguoiThue = findViewById(R.id.cardNguoiThue);

        btnGoiDien = findViewById(R.id.btnGoiDien);
        btnNhanTin = findViewById(R.id.btnNhanTin);
        btnChiDuong = findViewById(R.id.btnChiDuong);
        btnChiaSe = findViewById(R.id.btnChiaSe);

        // === ROOM DATABASE: Yêu thích phòng (offline) ===
        fabFavorite = findViewById(R.id.fabFavorite);
        favoriteDao = AppDatabase.getInstance(this).phongYeuThichDao();

        phongId = getIntent().getStringExtra("PHONG_ID");
        if (phongId == null) {
            Toast.makeText(this, "Không tìm thấy phòng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Observe trạng thái yêu thích từ Room
        favoriteDao.isFavorite(phongId).observe(this, isFav -> {
            isFavorite = isFav != null && isFav;
            fabFavorite.setImageResource(isFavorite
                    ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border);
        });

        fabFavorite.setOnClickListener(v -> toggleFavorite(phongId));

        // === IMPLICIT INTENTS ===

        // 1. Gọi điện cho người thuê (ACTION_DIAL)
        btnGoiDien.setOnClickListener(v -> {
            if (soDienThoaiNguoiThue == null || soDienThoaiNguoiThue.isEmpty()) {
                Toast.makeText(this, "Phòng chưa có người thuê", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + soDienThoaiNguoiThue));
            startActivity(intent);
        });

        // 2. Nhắn tin SMS cho người thuê (ACTION_SENDTO)
        btnNhanTin.setOnClickListener(v -> {
            if (soDienThoaiNguoiThue == null || soDienThoaiNguoiThue.isEmpty()) {
                Toast.makeText(this, "Phòng chưa có người thuê", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("smsto:" + soDienThoaiNguoiThue));
            intent.putExtra("sms_body", "Xin chào, tôi liên hệ về phòng " + tenPhongHienTai);
            startActivity(intent);
        });

        // 3. Mở Google Maps chỉ đường - dùng Retrofit gọi Nominatim API geocode
        btnChiDuong.setOnClickListener(v -> {
            String query = "Phòng trọ " + (tenPhongHienTai != null ? tenPhongHienTai : "");
            Toast.makeText(this, "Đang tìm vị trí...", Toast.LENGTH_SHORT).show();

            RetrofitClient.getNominatimApi()
                    .searchAddress(query, "json", 1)
                    .enqueue(new Callback<List<NominatimResult>>() {
                        @Override
                        public void onResponse(Call<List<NominatimResult>> call, Response<List<NominatimResult>> response) {
                            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                                NominatimResult result = response.body().get(0);
                                String lat = result.getLat();
                                String lon = result.getLon();
                                if (lat == null || lon == null) {
                                    openMapsWithQuery(query);
                                    return;
                                }
                                // Mở Google Maps với tọa độ từ Nominatim
                                Uri gmmUri = Uri.parse("geo:" + lat + "," + lon + "?q=" + lat + "," + lon + "(" + Uri.encode("Phòng " + tenPhongHienTai) + ")");
                                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmUri);
                                mapIntent.setPackage("com.google.android.apps.maps");
                                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                                    startActivity(mapIntent);
                                } else {
                                    startActivity(new Intent(Intent.ACTION_VIEW,
                                            Uri.parse("https://www.google.com/maps/@" + lat + "," + lon + ",17z")));
                                }
                            } else {
                                // Fallback: mở map với query text
                                openMapsWithQuery(query);
                            }
                        }

                        @Override
                        public void onFailure(Call<List<NominatimResult>> call, Throwable t) {
                            // Nếu API fail, fallback mở map bình thường
                            openMapsWithQuery(query);
                        }
                    });
        });

        // 4. Chia sẻ thông tin phòng trọ - chọn chia sẻ chung hoặc gửi SMS từ danh bạ
        btnChiaSe.setOnClickListener(v -> {
            String[] options = {"Chia sẻ chung", "Gửi SMS cho liên hệ trong danh bạ"};
            new AlertDialog.Builder(this)
                    .setTitle("Chia sẻ phòng trọ")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            shareRoomGeneral();
                        } else {
                            shareRoomViaContacts();
                        }
                    }).show();
        });

        findViewById(R.id.cardCongTo).setOnClickListener(v -> hienDialogChotCongTo());
        findViewById(R.id.cardLichSuCongTo).setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, LichSuCongToActivity.class);
            intent.putExtra("PHONG_ID", phongId);
            startActivity(intent);
        });

        loadRoomData(phongId);
        loadTenantData(phongId);
    }

    private void loadRoomData(String phongId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String uid = user.getUid();

        String tenantId = TenantSession.getActiveTenantId();
        DocumentReference scopeDoc = (tenantId != null && !tenantId.isEmpty())
                ? FirebaseFirestore.getInstance().collection("tenants").document(tenantId)
                : FirebaseFirestore.getInstance().collection("users").document(uid);

        roomListener = scopeDoc
                .collection("phong_tro").document(phongId)
                .addSnapshotListener((doc, e) -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (e != null || doc == null || !doc.exists()) {
                        Toast.makeText(this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    PhongTro phong = doc.toObject(PhongTro.class);
                    if (phong == null) return;
                    phong.setId(doc.getId());
                    displayRoom(phong);
                });
    }

    private void hienDialogChotCongTo() {
        if (phongId == null) return;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_chot_cong_to, null);
        android.widget.EditText etThangNam = dialogView.findViewById(R.id.etThangNam);
        android.widget.EditText etDienDau = dialogView.findViewById(R.id.etDienDau);
        android.widget.EditText etDienCuoi = dialogView.findViewById(R.id.etDienCuoi);
        android.widget.EditText etNuocDau = dialogView.findViewById(R.id.etNuocDau);
        android.widget.EditText etNuocCuoi = dialogView.findViewById(R.id.etNuocCuoi);

        etThangNam.setText(new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(new Date()));

        final double[] lastElecEnd = {0};
        final double[] lastWaterEnd = {0};
        loadLatestMeterEnds(phongId, (eEnd, wEnd) -> {
            lastElecEnd[0] = eEnd;
            lastWaterEnd[0] = wEnd;
            etDienDau.setText(formatDouble(eEnd));
            etNuocDau.setText(formatDouble(wEnd));
            if (etDienCuoi.getText().toString().trim().isEmpty()) etDienCuoi.setText(formatDouble(eEnd));
            if (etNuocCuoi.getText().toString().trim().isEmpty()) etNuocCuoi.setText(formatDouble(wEnd));
        });

        new AlertDialog.Builder(this)
                .setTitle("Chốt công tơ")
                .setView(dialogView)
                .setPositiveButton("Lưu", (d, w) -> {
                    try {
                        String period = etThangNam.getText().toString().trim();
                        double elecStart = lastElecEnd[0];
                        double waterStart = lastWaterEnd[0];
                        double elecEnd = parseDouble(etDienCuoi);
                        double waterEnd = parseDouble(etNuocCuoi);

                        if (elecEnd < elecStart || waterEnd < waterStart) {
                            Toast.makeText(this, "Chỉ số cuối không được nhỏ hơn chỉ số đầu", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        saveMeterReading(phongId, period, elecStart, elecEnd, waterStart, waterEnd);
                        Toast.makeText(this, "Đã lưu chỉ số công tơ", Toast.LENGTH_SHORT).show();
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Số liệu không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
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

    private void saveMeterReading(String roomId, String period, double elecStart, double elecEnd, double waterStart, double waterEnd) {
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
            return FirebaseFirestore.getInstance().collection("tenants").document(tenantId).collection(collection);
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) throw new IllegalStateException("User not logged in");
        return FirebaseFirestore.getInstance().collection("users").document(user.getUid()).collection(collection);
    }

    private double parseDouble(android.widget.EditText et) {
        String s = et.getText().toString().trim();
        return s.isEmpty() ? 0 : Double.parseDouble(s);
    }

    private String formatDouble(double value) {
        return value % 1 == 0 ? String.valueOf((long) value) : String.valueOf(value);
    }

    private void loadTenantData(String phongId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();

        String tenantId = TenantSession.getActiveTenantId();
        DocumentReference scopeDoc = (tenantId != null && !tenantId.isEmpty())
                ? FirebaseFirestore.getInstance().collection("tenants").document(tenantId)
                : FirebaseFirestore.getInstance().collection("users").document(uid);

        tenantListener = scopeDoc
                .collection("nguoi_thue")
                .whereEqualTo("idPhong", phongId)
                .addSnapshotListener((value, error) -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (error != null || value == null || value.isEmpty()) {
                        cardNguoiThue.setVisibility(View.GONE);
                        soDienThoaiNguoiThue = null;
                        return;
                    }
                    for (QueryDocumentSnapshot doc : value) {
                        NguoiThue nguoiThue = doc.toObject(NguoiThue.class);
                        if (nguoiThue != null) {
                            cardNguoiThue.setVisibility(View.VISIBLE);
                            tvTenNguoiThue.setText("Họ tên: " + nguoiThue.getHoTen());
                            tvSdtNguoiThue.setText("SĐT: " + nguoiThue.getSoDienThoai());
                            soDienThoaiNguoiThue = nguoiThue.getSoDienThoai();
                            break;
                        }
                    }
                });
    }

    private void displayRoom(PhongTro phong) {
        currentPhong = phong;
        tenPhongHienTai = phong.getSoPhong();
        giaThueHienTai = phong.getGiaThue();

        String khu = phong.getKhuTen();
        tvSoPhong.setText("Phòng " + phong.getSoPhong() + (khu != null && !khu.trim().isEmpty() ? (" • " + khu.trim()) : ""));
        tvLoaiPhong.setText(phong.getLoaiPhong());
        tvDienTich.setText((int) phong.getDienTich() + " m²");

        NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        tvGiaThue.setText(fmt.format(phong.getGiaThue()) + " đ/tháng");

        boolean trong = RoomStatus.VACANT.equals(phong.getTrangThai());
        int color = Color.parseColor(trong ? "#4CAF50" : "#F44336");

        // Status badge overlay on image
        tvTrangThai.setText(phong.getTrangThai());
        GradientDrawable badge = new GradientDrawable();
        badge.setShape(GradientDrawable.RECTANGLE);
        badge.setCornerRadius(32f);
        badge.setColor(color);
        tvTrangThai.setBackground(badge);

        tvTrangThaiRow.setText(phong.getTrangThai());
        tvTrangThaiRow.setTextColor(color);

        // Load full image (kiểm tra Activity còn sống)
        if (phong.getHinhAnh() != null && !phong.getHinhAnh().isEmpty() && !isDestroyed()) {
            Glide.with(this)
                    .load(phong.getHinhAnh())
                    .centerCrop()
                    .placeholder(R.drawable.baseline_home_24)
                    .into(imgPhong);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Phòng " + phong.getSoPhong());
        }
    }

    private void toggleFavorite(String phongId) {
        if (currentPhong == null) return;
        boolean currentFavorite = isFavorite;
        new Thread(() -> {
            if (currentFavorite) {
                favoriteDao.deleteById(phongId);
                runOnUiThread(() -> Toast.makeText(this, "Đã bỏ yêu thích", Toast.LENGTH_SHORT).show());
            } else {
                PhongYeuThich fav = new PhongYeuThich();
                fav.setId(phongId);
                fav.setSoPhong(currentPhong.getSoPhong());
                fav.setLoaiPhong(currentPhong.getLoaiPhong());
                fav.setDienTich(currentPhong.getDienTich());
                fav.setGiaThue(currentPhong.getGiaThue());
                fav.setTrangThai(currentPhong.getTrangThai());
                fav.setHinhAnh(currentPhong.getHinhAnh());
                fav.setNgayLuu(System.currentTimeMillis());
                favoriteDao.insert(fav);
                runOnUiThread(() -> Toast.makeText(this, "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private String buildShareText() {
        NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        return "Phòng trọ cho thuê:\n"
                + "Phòng: " + tenPhongHienTai + "\n"
                + "Giá: " + fmt.format(giaThueHienTai) + " đ/tháng\n"
                + "Liên hệ: " + (soDienThoaiNguoiThue != null ? soDienThoaiNguoiThue : "Chủ trọ");
    }

    private void shareRoomGeneral() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Phòng trọ " + tenPhongHienTai);
        intent.putExtra(Intent.EXTRA_TEXT, buildShareText());
        startActivity(Intent.createChooser(intent, "Chia sẻ qua"));
    }

    // === CONTENT PROVIDER: Đọc danh bạ để gửi SMS chia sẻ phòng trọ ===
    private static final int REQUEST_READ_CONTACTS = 200;

    private void shareRoomViaContacts() {
        if (!ContactHelper.hasContactPermission(this)) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_READ_CONTACTS);
            return;
        }
        showContactPicker();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showContactPicker();
            } else {
                Toast.makeText(this, "Cần quyền đọc danh bạ để chia sẻ", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showContactPicker() {
        java.util.List<ContactHelper.Contact> contacts = ContactHelper.readContacts(getContentResolver());
        if (contacts.isEmpty()) {
            Toast.makeText(this, "Danh bạ trống", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] contactNames = new String[contacts.size()];
        for (int i = 0; i < contacts.size(); i++) {
            contactNames[i] = contacts.get(i).toString();
        }

        new AlertDialog.Builder(this)
                .setTitle("Chọn liên hệ để gửi SMS")
                .setItems(contactNames, (dialog, which) -> {
                    ContactHelper.Contact selected = contacts.get(which);
                    // Gửi SMS qua Implicit Intent (ACTION_SENDTO)
                    Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
                    smsIntent.setData(Uri.parse("smsto:" + selected.phone));
                    smsIntent.putExtra("sms_body", buildShareText());
                    startActivity(smsIntent);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void openMapsWithQuery(String query) {
        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(query));
        Intent intent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        intent.setPackage("com.google.android.apps.maps");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query))));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (roomListener != null) roomListener.remove();
        if (tenantListener != null) tenantListener.remove();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
