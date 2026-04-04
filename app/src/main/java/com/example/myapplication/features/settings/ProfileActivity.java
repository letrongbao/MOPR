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

    // Receiver for ImageUploadService results
    private final BroadcastReceiver uploadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String imageUrl = intent.getStringExtra(ImageUploadService.EXTRA_IMAGE_URL);
            if (imageUrl != null) {
                // After successful Cloudinary upload, save URL to Firebase
                saveUserInfoToFirebase(imageUrl);
            } else {
                Toast.makeText(ProfileActivity.this, getString(R.string.error_receive_image_link), Toast.LENGTH_SHORT).show();
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

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        ScreenUiHelper.applyTopInset(toolbar);
        ScreenUiHelper.setupBackToolbar(this, toolbar, getString(R.string.profile_title));

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
                        String fullName = doc.getString("fullName");
                        String phoneNumber = doc.getString("phoneNumber");
                        String avatarUrl = doc.getString("avatarUrl");

                        if (fullName != null && !fullName.isEmpty()) {
                            edtProfileName.setText(fullName);
                            tvDisplayName.setText(fullName);
                        }
                        if (phoneNumber != null) {
                            edtProfilePhone.setText(phoneNumber);
                        }
                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            Glide.with(this).load(avatarUrl).circleCrop().into(imgAvatar);
                            imgAvatar.setPadding(0, 0, 0, 0);
                        }
                    }
                });
    }

    private void startSaveProcess() {
        String fullName = edtProfileName.getText().toString().trim();
        if (fullName.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_enter_full_name), Toast.LENGTH_SHORT).show();
            return;
        }

        btnSaveProfile.setEnabled(false);

        if (selectedAvatarUri != null) {
            // Use Service-based upload flow for consistent reliability
            Intent serviceIntent = new Intent(this, ImageUploadService.class);
            serviceIntent.putExtra(ImageUploadService.EXTRA_IMAGE_URI, selectedAvatarUri.toString());
            startService(serviceIntent);
            Toast.makeText(this, getString(R.string.uploading_image_to_system), Toast.LENGTH_SHORT).show();
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

        String fullName = edtProfileName.getText().toString().trim();
        String phoneNumber = edtProfilePhone.getText().toString().trim();

        UserProfileChangeRequest.Builder profileBuilder = new UserProfileChangeRequest.Builder()
                .setDisplayName(fullName);
        if (avatarUrl != null) {
            profileBuilder.setPhotoUri(Uri.parse(avatarUrl));
        }

        user.updateProfile(profileBuilder.build())
                .addOnSuccessListener(aVoid -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("fullName", fullName);
                    updates.put("phoneNumber", phoneNumber);
                    if (avatarUrl != null) {
                        updates.put("avatarUrl", avatarUrl);
                    }

                    db.collection("users").document(user.getUid())
                            .update(updates)
                            .addOnSuccessListener(aVoid2 -> {
                                tvDisplayName.setText(fullName);
                                selectedAvatarUri = null;
                                Toast.makeText(this, getString(R.string.update_success), Toast.LENGTH_SHORT).show();
                                btnSaveProfile.setEnabled(true);
                            })
                            .addOnFailureListener(e -> {
                                // Create document if it does not exist
                                updates.put("email", user.getEmail());
                                updates.put("uid", user.getUid());
                                db.collection("users").document(user.getUid())
                                        .set(updates)
                                        .addOnSuccessListener(aVoid3 -> {
                                            tvDisplayName.setText(fullName);
                                            selectedAvatarUri = null;
                                            Toast.makeText(this, getString(R.string.update_success), Toast.LENGTH_SHORT).show();
                                            btnSaveProfile.setEnabled(true);
                                        })
                                        .addOnFailureListener(e2 -> {
                                            Toast.makeText(this, getString(R.string.error_colon) + e2.getMessage(), Toast.LENGTH_SHORT).show();
                                            btnSaveProfile.setEnabled(true);
                                        });
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.error_update_profile) + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSaveProfile.setEnabled(true);
                });
    }

    private void changePassword() {
        String newPass = edtNewPassword.getText().toString().trim();
        String confirmPass = edtConfirmNewPassword.getText().toString().trim();

        if (newPass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_fill_all_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPass.equals(confirmPass)) {
            Toast.makeText(this, getString(R.string.password_confirmation_mismatch), Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPass.length() < 6) {
            Toast.makeText(this, getString(R.string.password_min_6_chars), Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null)
            return;

        btnChangePassword.setEnabled(false);

        user.updatePassword(newPass)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, getString(R.string.password_changed_success), Toast.LENGTH_SHORT).show();
                    edtNewPassword.setText("");
                    edtConfirmNewPassword.setText("");
                    btnChangePassword.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.error_colon) + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnChangePassword.setEnabled(true);
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
