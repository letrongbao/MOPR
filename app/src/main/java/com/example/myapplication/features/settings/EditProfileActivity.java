package com.example.myapplication.features.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.appbar.AppBarLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.core.session.InviteRepository;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.service.ImageUploadService;
import com.example.myapplication.core.util.AuthProviderUtil;
import com.example.myapplication.core.util.ScreenUiHelper;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText edtProfileName, edtProfileEmail, edtProfilePhone;
    private TextInputEditText edtInviteCode;
    private TextView tvAuthMethod;
    private Button btnSaveProfile;
    private Button btnApplyInvite;
    private ShapeableImageView imgAvatar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Uri selectedAvatarUri;
    private ActivityResultLauncher<String> avatarPickerLauncher;

    private final BroadcastReceiver uploadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String imageUrl = intent.getStringExtra(ImageUploadService.EXTRA_IMAGE_URL);
            if (imageUrl != null) {
                saveUserInfoToFirebase(imageUrl);
            } else {
                Toast.makeText(EditProfileActivity.this, getString(R.string.error_upload_image), Toast.LENGTH_SHORT)
                        .show();
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
                    }
                });

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        ScreenUiHelper.enableEdgeToEdge(this, false);

        setContentView(R.layout.activity_edit_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            ScreenUiHelper.applyTopInset(appBarLayout);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        ScreenUiHelper.setupBackToolbar(this, toolbar, getString(R.string.account_management));

        imgAvatar = findViewById(R.id.imgAvatar);
        edtProfileName = findViewById(R.id.edtProfileName);
        edtProfileEmail = findViewById(R.id.edtProfileEmail);
        edtProfilePhone = findViewById(R.id.edtProfilePhone);
        tvAuthMethod = findViewById(R.id.tvAuthMethod);
        edtInviteCode = findViewById(R.id.edtInviteCode);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnApplyInvite = findViewById(R.id.btnApplyInvite);

        imgAvatar.setOnClickListener(v -> avatarPickerLauncher.launch("image/*"));

        loadUserInfo();

        btnSaveProfile.setOnClickListener(v -> startSaveProcess());
        if (btnApplyInvite != null) {
            btnApplyInvite.setOnClickListener(v -> applyInviteCode());
        }
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

        edtProfileEmail.setText(email);
        if (tvAuthMethod != null) {
            tvAuthMethod.setText(AuthProviderUtil.resolveLoginMethodLabel(this, user));
        }

        if (displayName != null && !displayName.isEmpty()) {
            edtProfileName.setText(displayName);
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
                        }
                        if (phoneNumber != null) {
                            edtProfilePhone.setText(phoneNumber);
                        }
                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            Glide.with(this).load(avatarUrl).circleCrop().into(imgAvatar);
                        }
                    }
                });
    }

    private void startSaveProcess() {
        String fullName = edtProfileName.getText() != null ? edtProfileName.getText().toString().trim() : "";
        if (fullName.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_enter_full_name), Toast.LENGTH_SHORT).show();
            return;
        }

        btnSaveProfile.setEnabled(false);

        if (selectedAvatarUri != null) {
            Intent serviceIntent = new Intent(this, ImageUploadService.class);
            serviceIntent.putExtra(ImageUploadService.EXTRA_IMAGE_URI, selectedAvatarUri.toString());
            startService(serviceIntent);
            Toast.makeText(this, getString(R.string.uploading_image), Toast.LENGTH_SHORT).show();
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

        String fullName = edtProfileName.getText() != null ? edtProfileName.getText().toString().trim() : "";
        String phoneNumber = edtProfilePhone.getText() != null ? edtProfilePhone.getText().toString().trim() : "";

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
                                selectedAvatarUri = null;
                                Toast.makeText(this, getString(R.string.update_success), Toast.LENGTH_SHORT).show();
                                btnSaveProfile.setEnabled(true);
                                setResult(RESULT_OK);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                updates.put("email", user.getEmail());
                                updates.put("uid", user.getUid());
                                updates.put("primaryRole", "TENANT");
                                updates.put("activeTenantId", null);
                                updates.put("createdAt", Timestamp.now());
                                updates.put("updatedAt", Timestamp.now());
                                db.collection("users").document(user.getUid())
                                        .set(updates)
                                        .addOnSuccessListener(aVoid3 -> {
                                            selectedAvatarUri = null;
                                            Toast.makeText(this, getString(R.string.update_success), Toast.LENGTH_SHORT)
                                                    .show();
                                            btnSaveProfile.setEnabled(true);
                                            setResult(RESULT_OK);
                                            finish();
                                        })
                                        .addOnFailureListener(e2 -> {
                                            Toast.makeText(this, getString(R.string.error_colon) + e2.getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                            btnSaveProfile.setEnabled(true);
                                        });
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.error_update) + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSaveProfile.setEnabled(true);
                });
    }

    private void applyInviteCode() {
        if (edtInviteCode == null || btnApplyInvite == null) {
            return;
        }

        String code = edtInviteCode.getText() != null
                ? edtInviteCode.getText().toString().trim().toUpperCase(java.util.Locale.US)
                : "";
        if (code.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_enter_invite_code), Toast.LENGTH_SHORT).show();
            return;
        }

        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId == null || tenantId.trim().isEmpty()) {
            Toast.makeText(this, getString(R.string.no_active_tenant), Toast.LENGTH_SHORT).show();
            return;
        }

        btnApplyInvite.setEnabled(false);
        new InviteRepository().joinByInvite(this, tenantId, code, new InviteRepository.JoinCallback() {
            @Override
            public void onSuccess(@androidx.annotation.NonNull String joinedTenantId) {
                runOnUiThread(() -> {
                    Toast.makeText(EditProfileActivity.this, getString(R.string.invite_code_success),
                            Toast.LENGTH_SHORT).show();
                    btnApplyInvite.setEnabled(true);
                    setResult(RESULT_OK);
                    finish();
                });
            }

            @Override
            public void onError(@androidx.annotation.NonNull Exception e) {
                runOnUiThread(() -> {
                    btnApplyInvite.setEnabled(true);
                    Toast.makeText(EditProfileActivity.this, getString(R.string.invalid_code), Toast.LENGTH_SHORT)
                            .show();
                });
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
