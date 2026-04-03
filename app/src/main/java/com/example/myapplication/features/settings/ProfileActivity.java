package com.example.myapplication.features.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.example.myapplication.core.service.ImageUploadService;
import com.example.myapplication.R;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.ScreenUiHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvDisplayName, tvEmail;
    private EditText edtProfileName, edtProfileEmail, edtProfilePhone;
    private EditText edtNewPassword, edtConfirmNewPassword;
    private Button btnSaveProfile, btnChangePassword;
    private ImageView imgAvatar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Uri selectedAvatarUri;
    private ActivityResultLauncher<String> avatarPickerLauncher;

    // Receiver để nhận kết quả từ ImageUploadService
    private final BroadcastReceiver uploadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String imageUrl = intent.getStringExtra(ImageUploadService.EXTRA_IMAGE_URL);
            if (imageUrl != null) {
                // Sau khi upload lên Cloudinary thành công, lưu URL vào Firebase
                saveUserInfoToFirebase(imageUrl);
            } else {
                Toast.makeText(ProfileActivity.this, "Lỗi khi nhận link ảnh", Toast.LENGTH_SHORT).show();
                btnSaveProfile.setEnabled(true);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        avatarPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedAvatarUri = uri;
                        Glide.with(this).load(uri).circleCrop().into(imgAvatar);
                        imgAvatar.setPadding(0, 0, 0, 0);
                    }
                });

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        ScreenUiHelper.enableEdgeToEdge(this, false);

        setContentView(R.layout.activity_profile);

        com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            ScreenUiHelper.applyTopInset(appBarLayout);
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        ScreenUiHelper.setupBackToolbar(this, toolbar, "Thông tin cá nhân");

        tvDisplayName = findViewById(R.id.tvDisplayName);
        tvEmail = findViewById(R.id.tvEmail);
        imgAvatar = findViewById(R.id.imgAvatar);
        edtProfileName = findViewById(R.id.edtProfileName);
        edtProfileEmail = findViewById(R.id.edtProfileEmail);
        edtProfilePhone = findViewById(R.id.edtProfilePhone);
        edtNewPassword = findViewById(R.id.edtNewPassword);
        edtConfirmNewPassword = findViewById(R.id.edtConfirmNewPassword);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        imgAvatar.setOnClickListener(v -> avatarPickerLauncher.launch("image/*"));

        loadUserInfo();

        btnSaveProfile.setOnClickListener(v -> startSaveProcess());
        btnChangePassword.setOnClickListener(v -> changePassword());
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                uploadReceiver, new IntentFilter(ImageUploadService.ACTION_UPLOAD_COMPLETE));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(uploadReceiver);
    }

    private void loadUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null)
            return;

        String displayName = user.getDisplayName();
        String email = user.getEmail();

        tvEmail.setText(email);
        edtProfileEmail.setText(email);

        if (displayName != null && !displayName.isEmpty()) {
            tvDisplayName.setText(displayName);
            edtProfileName.setText(displayName);
        } else {
            tvDisplayName.setText(email);
        }

        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String hoTen = doc.getString("hoTen");
                        String soDienThoai = doc.getString("soDienThoai");
                        String avatarUrl = doc.getString("avatarUrl");

                        if (hoTen != null && !hoTen.isEmpty()) {
                            edtProfileName.setText(hoTen);
                            tvDisplayName.setText(hoTen);
                        }
                        if (soDienThoai != null) {
                            edtProfilePhone.setText(soDienThoai);
                        }
                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            Glide.with(this).load(avatarUrl).circleCrop().into(imgAvatar);
                            imgAvatar.setPadding(0, 0, 0, 0);
                        }
                    }
                });
    }

    private void startSaveProcess() {
        String hoTen = edtProfileName.getText().toString().trim();
        if (hoTen.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập họ tên", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSaveProfile.setEnabled(false);

        if (selectedAvatarUri != null) {
            // Sử dụng Service để upload (Đảm bảo thành công như ảnh phòng)
            Intent serviceIntent = new Intent(this, ImageUploadService.class);
            serviceIntent.putExtra(ImageUploadService.EXTRA_IMAGE_URI, selectedAvatarUri.toString());
            startService(serviceIntent);
            Toast.makeText(this, "Đang tải ảnh lên hệ thống...", Toast.LENGTH_SHORT).show();
        } else {
            saveUserInfoToFirebase(null);
        }
    }

    private void saveUserInfoToFirebase(String avatarUrl) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            btnSaveProfile.setEnabled(true);
            return;
        }

        String hoTen = edtProfileName.getText().toString().trim();
        String soDienThoai = edtProfilePhone.getText().toString().trim();

        UserProfileChangeRequest.Builder profileBuilder = new UserProfileChangeRequest.Builder()
                .setDisplayName(hoTen);
        if (avatarUrl != null) {
            profileBuilder.setPhotoUri(Uri.parse(avatarUrl));
        }

        user.updateProfile(profileBuilder.build())
                .addOnSuccessListener(aVoid -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("hoTen", hoTen);
                    updates.put("soDienThoai", soDienThoai);
                    if (avatarUrl != null) {
                        updates.put("avatarUrl", avatarUrl);
                    }

                    db.collection("users").document(user.getUid())
                            .update(updates)
                            .addOnSuccessListener(aVoid2 -> {
                                tvDisplayName.setText(hoTen);
                                selectedAvatarUri = null;
                                Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                                btnSaveProfile.setEnabled(true);
                            })
                            .addOnFailureListener(e -> {
                                // Nếu chưa có doc thì tạo mới
                                updates.put("email", user.getEmail());
                                updates.put("uid", user.getUid());
                                db.collection("users").document(user.getUid())
                                        .set(updates)
                                        .addOnSuccessListener(aVoid3 -> {
                                            tvDisplayName.setText(hoTen);
                                            selectedAvatarUri = null;
                                            Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                                            btnSaveProfile.setEnabled(true);
                                        })
                                        .addOnFailureListener(e2 -> {
                                            Toast.makeText(this, "Lỗi: " + e2.getMessage(), Toast.LENGTH_SHORT).show();
                                            btnSaveProfile.setEnabled(true);
                                        });
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi cập nhật Profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSaveProfile.setEnabled(true);
                });
    }

    private void changePassword() {
        String newPass = edtNewPassword.getText().toString().trim();
        String confirmPass = edtConfirmNewPassword.getText().toString().trim();

        if (newPass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPass.equals(confirmPass)) {
            Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPass.length() < 6) {
            Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null)
            return;

        btnChangePassword.setEnabled(false);

        user.updatePassword(newPass)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                    edtNewPassword.setText("");
                    edtConfirmNewPassword.setText("");
                    btnChangePassword.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnChangePassword.setEnabled(true);
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
