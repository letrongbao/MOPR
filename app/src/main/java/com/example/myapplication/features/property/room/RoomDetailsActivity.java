package com.example.myapplication.features.property.room;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.core.constants.RoomStatus;
import com.example.myapplication.core.session.InviteRepository;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.ScreenUiHelper;
import com.example.myapplication.domain.Tenant;
import com.example.myapplication.domain.Room;
import com.google.android.material.appbar.AppBarLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.Locale;

public class RoomDetailsActivity extends AppCompatActivity {

    private ImageView imgRoom;
    private TextView tvRoomNumber, tvRoomType, tvArea, tvRentAmount, tvStatus, tvStatusRow;
    private TextView tvTenantName, tvTenantPhone;
    private View cardTenant;
    private View btnCall, btnMessage, llActionButtons;
    private Button btnGenerateCode;
    private Room currentRoom;

    private String tenantPhoneNumber;
    private String currentRoomNumber;

    private ListenerRegistration roomListener;
    private ListenerRegistration tenantListener;

    private String roomId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScreenUiHelper.enableEdgeToEdge(this, false);

        setContentView(R.layout.activity_room_details);

        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            ScreenUiHelper.applyTopInset(appBarLayout);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        ScreenUiHelper.setupBackToolbar(this, toolbar, getString(R.string.room_details_title));

        imgRoom = findViewById(R.id.imgPhongChiTiet);
        tvRoomNumber = findViewById(R.id.tvSoPhongChiTiet);
        tvStatus = findViewById(R.id.tvTrangThaiChiTiet);

        // Detail rows
        View rowLoai = findViewById(R.id.rowLoaiPhong);
        ((TextView) rowLoai.findViewById(R.id.tvLabel)).setText(R.string.room_type_label);
        tvRoomType = rowLoai.findViewById(R.id.tvValue);

        View rowDienTich = findViewById(R.id.rowDienTich);
        ((TextView) rowDienTich.findViewById(R.id.tvLabel)).setText(R.string.room_area_label);
        tvArea = rowDienTich.findViewById(R.id.tvValue);

        View rowGia = findViewById(R.id.rowGiaThue);
        ((TextView) rowGia.findViewById(R.id.tvLabel)).setText(R.string.room_rent_label);
        tvRentAmount = rowGia.findViewById(R.id.tvValue);
        tvRentAmount.setTextColor(getResources().getColor(R.color.primary));

        View rowTrangThai = findViewById(R.id.rowTrangThai);
        ((TextView) rowTrangThai.findViewById(R.id.tvLabel)).setText(R.string.room_status_label);
        tvStatusRow = rowTrangThai.findViewById(R.id.tvValue);

        tvTenantName = findViewById(R.id.tvTenTenant);
        tvTenantPhone = findViewById(R.id.tvSdtTenant);
        cardTenant = findViewById(R.id.cardTenant);

        btnCall = findViewById(R.id.btnGoiDien);
        btnMessage = findViewById(R.id.btnNhanTin);
        llActionButtons = findViewById(R.id.llActionButtons);
        btnGenerateCode = findViewById(R.id.btnGenerateCode);

        roomId = getIntent().getStringExtra("ROOM_ID");
        if (roomId == null) {
            Toast.makeText(this, R.string.room_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnCall.setOnClickListener(v -> {
            if (tenantPhoneNumber == null || tenantPhoneNumber.isEmpty()) {
                Toast.makeText(this, R.string.room_no_tenant, Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + tenantPhoneNumber));
            startActivity(intent);
        });

        btnMessage.setOnClickListener(v -> {
            if (tenantPhoneNumber == null || tenantPhoneNumber.isEmpty()) {
                Toast.makeText(this, R.string.room_no_tenant, Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("smsto:" + tenantPhoneNumber));
            intent.putExtra("sms_body", getString(R.string.room_contact_sms, currentRoomNumber));
            startActivity(intent);
        });

        btnGenerateCode.setOnClickListener(v -> generateAnonymousCode());

        loadRoomData(roomId);
        loadTenantData(roomId);
    }

    private void generateAnonymousCode() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.not_logged_in, Toast.LENGTH_SHORT).show();
            return;
        }

        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId == null || tenantId.isEmpty()) {
            tenantId = user.getUid();
        }

        btnGenerateCode.setEnabled(false);
        btnGenerateCode.setText("Đang tạo mã...");

        new InviteRepository().createAnonymousTenantInvite(tenantId, roomId, new InviteRepository.InviteCallback() {
            @Override
            public void onSuccess(@NonNull String code) {
                btnGenerateCode.setEnabled(true);
                btnGenerateCode.setText("Tạo mã vào phòng (Mã ẩn danh)");

                new AlertDialog.Builder(RoomDetailsActivity.this)
                        .setTitle("Mã phòng được tạo thành công!")
                        .setMessage("Đây là mã dùng 1 lần. Hãy sao chép và gửi cho khách để họ kết nối phòng.\n\nCode: " + code)
                        .setPositiveButton("Copy Mã", (dialog, which) -> {
                            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("Room Code", code);
                            clipboard.setPrimaryClip(clip);
                            Toast.makeText(RoomDetailsActivity.this, "Đã sao chép: " + code, Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Đóng", null)
                        .setCancelable(false)
                        .show();
            }

            @Override
            public void onError(@NonNull Exception e) {
                btnGenerateCode.setEnabled(true);
                btnGenerateCode.setText("Tạo mã vào phòng (Mã ẩn danh)");
                Toast.makeText(RoomDetailsActivity.this, "Lỗi tạo mã: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadRoomData(String roomId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.not_logged_in, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String uid = user.getUid();

        String tenantId = TenantSession.getActiveTenantId();
        DocumentReference scopeDoc = (tenantId != null && !tenantId.isEmpty())
                ? FirebaseFirestore.getInstance().collection("tenants").document(tenantId)
                : FirebaseFirestore.getInstance().collection("users").document(uid);

        roomListener = scopeDoc
                .collection("rooms").document(roomId)
                .addSnapshotListener((doc, e) -> {
                    if (isFinishing() || isDestroyed())
                        return;
                    if (e != null || doc == null || !doc.exists()) {
                        Toast.makeText(this, R.string.room_load_error, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Room room = doc.toObject(Room.class);
                    if (room == null)
                        return;
                    room.setId(doc.getId());
                    displayRoom(room);
                });
    }

    private void loadTenantData(String roomId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
            return;
        String uid = user.getUid();

        String tenantId = TenantSession.getActiveTenantId();
        DocumentReference scopeDoc = (tenantId != null && !tenantId.isEmpty())
                ? FirebaseFirestore.getInstance().collection("tenants").document(tenantId)
                : FirebaseFirestore.getInstance().collection("users").document(uid);

        tenantListener = scopeDoc
                .collection("contracts")
                .whereEqualTo("roomId", roomId)
                .addSnapshotListener((value, error) -> {
                    if (isFinishing() || isDestroyed())
                        return;
                    if (error != null || value == null || value.isEmpty()) {
                        cardTenant.setVisibility(View.GONE);
                        llActionButtons.setVisibility(View.GONE);
                        tenantPhoneNumber = null;
                        return;
                    }
                    for (QueryDocumentSnapshot doc : value) {
                        Tenant tenant = doc.toObject(Tenant.class);
                        if (tenant != null) {
                            cardTenant.setVisibility(View.VISIBLE);
                            llActionButtons.setVisibility(View.VISIBLE);
                            tvTenantName.setText(tenant.getFullName());
                            tvTenantPhone.setText(tenant.getPhoneNumber());
                            tenantPhoneNumber = tenant.getPhoneNumber();
                            break;
                        }
                    }
                });
    }

    private void displayRoom(Room room) {
        currentRoom = room;
        currentRoomNumber = room.getRoomNumber();

        String houseName = room.getHouseName();
        tvRoomNumber.setText(
                getString(
                        R.string.room_number_with_house,
                        room.getRoomNumber(),
                        (houseName != null && !houseName.trim().isEmpty()) ? (" • " + houseName.trim()) : ""));
        tvRoomType.setText(room.getRoomType());
        tvArea.setText(getString(R.string.room_area_value, (int) room.getArea()));

        NumberFormat fmt = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        tvRentAmount.setText(getString(R.string.room_rent_value, fmt.format(room.getRentAmount())));

        boolean isVacant = RoomStatus.VACANT.equals(room.getStatus());
        int color = Color.parseColor(isVacant ? "#4CAF50" : "#F44336");

        // Status badge overlay on image
        tvStatus.setText(room.getStatus());
        GradientDrawable badge = new GradientDrawable();
        badge.setShape(GradientDrawable.RECTANGLE);
        badge.setCornerRadius(32f);
        badge.setColor(color);
        tvStatus.setBackground(badge);

        tvStatusRow.setText(room.getStatus());
        tvStatusRow.setTextColor(color);

        if (room.getImageUrl() != null && !room.getImageUrl().isEmpty() && !isDestroyed()) {
            Glide.with(this)
                    .load(room.getImageUrl())
                    .centerCrop()
                    .placeholder(R.drawable.baseline_home_24)
                    .into(imgRoom);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.room_number, room.getRoomNumber()));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (roomListener != null)
            roomListener.remove();
        if (tenantListener != null)
            tenantListener.remove();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
