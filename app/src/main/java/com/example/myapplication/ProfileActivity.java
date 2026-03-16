package com.example.myapplication;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvDisplayName, tvEmail;
    private EditText edtProfileName, edtProfileEmail, edtProfilePhone;
    private EditText edtNewPassword, edtConfirmNewPassword;
    private Button btnSaveProfile, btnChangePassword;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Làm status bar trong suốt giống HomeActivity
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        window.setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Thông tin cá nhân");
        }
        
        // Tự động thêm padding cho Toolbar
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        toolbar.setNavigationOnClickListener(v -> finish());

        // Bind views
        tvDisplayName = findViewById(R.id.tvDisplayName);
        tvEmail = findViewById(R.id.tvEmail);
        edtProfileName = findViewById(R.id.edtProfileName);
        edtProfileEmail = findViewById(R.id.edtProfileEmail);
        edtProfilePhone = findViewById(R.id.edtProfilePhone);
        edtNewPassword = findViewById(R.id.edtNewPassword);
        edtConfirmNewPassword = findViewById(R.id.edtConfirmNewPassword);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        loadUserInfo();

        btnSaveProfile.setOnClickListener(v -> saveProfile());
        btnChangePassword.setOnClickListener(v -> changePassword());
    }

    private void loadUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

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

        // Lấy thông tin từ Firestore
        db.collection("users").document(user.getUid())
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String hoTen = doc.getString("hoTen");
                    String soDienThoai = doc.getString("soDienThoai");

                    if (hoTen != null && !hoTen.isEmpty()) {
                        edtProfileName.setText(hoTen);
                        tvDisplayName.setText(hoTen);
                    }
                    if (soDienThoai != null) {
                        edtProfilePhone.setText(soDienThoai);
                    }
                }
            });
    }

    private void saveProfile() {
        String hoTen = edtProfileName.getText().toString().trim();
        String soDienThoai = edtProfilePhone.getText().toString().trim();

        if (hoTen.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập họ tên", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        btnSaveProfile.setEnabled(false);

        // Cập nhật displayName trên Firebase Auth
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(hoTen)
                .build();

        user.updateProfile(profileUpdates)
            .addOnSuccessListener(aVoid -> {
                // Cập nhật Firestore
                db.collection("users").document(user.getUid())
                    .update("hoTen", hoTen, "soDienThoai", soDienThoai)
                    .addOnSuccessListener(aVoid2 -> {
                        tvDisplayName.setText(hoTen);
                        Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                        btnSaveProfile.setEnabled(true);
                    })
                    .addOnFailureListener(e -> {
                        // Nếu document chưa tồn tại, tạo mới
                        java.util.Map<String, Object> userData = new java.util.HashMap<>();
                        userData.put("hoTen", hoTen);
                        userData.put("soDienThoai", soDienThoai);
                        userData.put("email", user.getEmail());
                        userData.put("uid", user.getUid());

                        db.collection("users").document(user.getUid())
                            .set(userData)
                            .addOnSuccessListener(aVoid3 -> {
                                tvDisplayName.setText(hoTen);
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
                Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
        if (user == null) return;

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