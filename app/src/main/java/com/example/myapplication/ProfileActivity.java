package com.example.myapplication;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ProfileActivity extends AppCompatActivity {

    private static final String CLOUD_NAME = "dsvkscwti";
    private static final String UPLOAD_PRESET = "MOPR";

    private TextView tvDisplayName, tvEmail;
    private EditText edtProfileName, edtProfileEmail, edtProfilePhone;
    private EditText edtNewPassword, edtConfirmNewPassword;
    private Button btnSaveProfile, btnChangePassword;
    private ImageView imgAvatar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Uri selectedAvatarUri;
    private ActivityResultLauncher<String> avatarPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register avatar picker
        avatarPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedAvatarUri = uri;
                        Glide.with(this).load(uri).circleCrop().into(imgAvatar);
                        imgAvatar.setPadding(0, 0, 0, 0);
                    }
                }
        );

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

        if (selectedAvatarUri != null) {
            uploadAvatarAndSave(user, hoTen, soDienThoai);
        } else {
            updateProfile(user, hoTen, soDienThoai, null);
        }
    }

    private void uploadAvatarAndSave(FirebaseUser user, String hoTen, String soDienThoai) {
        Toast.makeText(this, "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(selectedAvatarUri);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                is.close();
                byte[] imageBytes = baos.toByteArray();

                String boundary = "----Boundary" + System.currentTimeMillis();
                URL url = new URL("https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n");
                dos.writeBytes(UPLOAD_PRESET + "\r\n");

                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"avatar.jpg\"\r\n");
                dos.writeBytes("Content-Type: image/jpeg\r\n\r\n");
                dos.write(imageBytes);
                dos.writeBytes("\r\n");

                dos.writeBytes("--" + boundary + "--\r\n");
                dos.flush();
                dos.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();

                    JSONObject json = new JSONObject(response.toString());
                    String avatarUrl = json.getString("secure_url");

                    runOnUiThread(() -> updateProfile(user, hoTen, soDienThoai, avatarUrl));
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Upload ảnh thất bại", Toast.LENGTH_LONG).show();
                        btnSaveProfile.setEnabled(true);
                    });
                }
                conn.disconnect();
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Tải ảnh thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnSaveProfile.setEnabled(true);
                });
            }
        }).start();
    }

    private void updateProfile(FirebaseUser user, String hoTen, String soDienThoai, String avatarUrl) {
        UserProfileChangeRequest.Builder profileBuilder = new UserProfileChangeRequest.Builder()
                .setDisplayName(hoTen);
        if (avatarUrl != null) {
            profileBuilder.setPhotoUri(Uri.parse(avatarUrl));
        }

        user.updateProfile(profileBuilder.build())
            .addOnSuccessListener(aVoid -> {
                java.util.Map<String, Object> updates = new java.util.HashMap<>();
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
                        // Nếu document chưa tồn tại, tạo mới
                        java.util.Map<String, Object> userData = new java.util.HashMap<>();
                        userData.put("hoTen", hoTen);
                        userData.put("soDienThoai", soDienThoai);
                        userData.put("email", user.getEmail());
                        userData.put("uid", user.getUid());
                        if (avatarUrl != null) {
                            userData.put("avatarUrl", avatarUrl);
                        }

                        db.collection("users").document(user.getUid())
                            .set(userData)
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
