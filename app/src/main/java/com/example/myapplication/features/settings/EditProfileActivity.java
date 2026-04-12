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
import com.example.myapplication.core.constants.TenantRoles;
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
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import android.view.View;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    public static final String EXTRA_USE_TENANT_HEADER = "USE_TENANT_HEADER";

    private static final String UPLOAD_PURPOSE_AVATAR = "avatar";
    private static final String UPLOAD_PURPOSE_PERSONAL_ID = "personal_id";
    private static final String UPLOAD_PURPOSE_BANK_QR = "bank_qr";

    private TextInputEditText edtProfileName, edtProfileEmail, edtProfilePhone;
    private TextInputEditText edtProfilePersonalId;
    private TextInputEditText edtOwnerBankAccountNo;
    private TextInputEditText edtOwnerBankName;
    private TextInputEditText edtOwnerBankAccountName;
    private TextInputEditText edtInviteCode;
    private TextView tvRepresentativeInfo;
    private TextView tvAuthMethod;
    private Button btnSaveProfile;
    private Button btnApplyInvite;
    private View layoutInviteSection;
    private View layoutTenantIdentitySection;
    private View layoutOwnerBankSection;
    private boolean isOwnerPrimaryRole;
    private boolean isTenantRepresentative;
    private String activeTenantId;
    private ShapeableImageView imgAvatar;
    private ShapeableImageView imgPersonalIdCard;
    private ShapeableImageView imgOwnerBankQr;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Uri selectedAvatarUri;
    private Uri selectedPersonalIdUri;
    private Uri selectedOwnerBankQrUri;
    private String uploadedAvatarUrl;
    private String uploadedPersonalIdImageUrl;
    private String uploadedOwnerBankQrUrl;
    private String currentPersonalIdImageUrl;
    private String currentOwnerBankQrUrl;
    private ActivityResultLauncher<String> avatarPickerLauncher;
    private ActivityResultLauncher<String> personalIdPickerLauncher;
    private ActivityResultLauncher<String> bankQrPickerLauncher;

    private final BroadcastReceiver uploadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String imageUrl = intent.getStringExtra(ImageUploadService.EXTRA_IMAGE_URL);
            String purpose = intent.getStringExtra(ImageUploadService.EXTRA_UPLOAD_PURPOSE);
            if (imageUrl != null) {
                if (purpose == null || UPLOAD_PURPOSE_AVATAR.equals(purpose)) {
                    uploadedAvatarUrl = imageUrl;
                    if (selectedPersonalIdUri != null) {
                        startImageUpload(selectedPersonalIdUri, UPLOAD_PURPOSE_PERSONAL_ID);
                    } else if (selectedOwnerBankQrUri != null) {
                        startImageUpload(selectedOwnerBankQrUri, UPLOAD_PURPOSE_BANK_QR);
                    } else {
                        saveUserInfoToFirebase(uploadedAvatarUrl, null, null);
                    }
                    return;
                }

                if (UPLOAD_PURPOSE_PERSONAL_ID.equals(purpose)) {
                    uploadedPersonalIdImageUrl = imageUrl;
                    if (selectedOwnerBankQrUri != null) {
                        startImageUpload(selectedOwnerBankQrUri, UPLOAD_PURPOSE_BANK_QR);
                    } else {
                        saveUserInfoToFirebase(uploadedAvatarUrl, uploadedPersonalIdImageUrl, null);
                    }
                    return;
                }

                if (UPLOAD_PURPOSE_BANK_QR.equals(purpose)) {
                    uploadedOwnerBankQrUrl = imageUrl;
                    saveUserInfoToFirebase(uploadedAvatarUrl, uploadedPersonalIdImageUrl, uploadedOwnerBankQrUrl);
                    return;
                }

                saveUserInfoToFirebase(imageUrl, null, null);
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

        personalIdPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedPersonalIdUri = uri;
                        Glide.with(this).load(uri).into(imgPersonalIdCard);
                    }
                });

        bankQrPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedOwnerBankQrUri = uri;
                        if (imgOwnerBankQr != null) {
                            Glide.with(this).load(uri).into(imgOwnerBankQr);
                        }
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
        boolean useTenantHeader = getIntent().getBooleanExtra(EXTRA_USE_TENANT_HEADER, false);
        if (useTenantHeader) {
            if (appBarLayout != null) {
                appBarLayout.setBackgroundResource(R.drawable.bg_tenant_header_teal);
            }
            if (toolbar != null) {
                toolbar.setBackgroundResource(R.drawable.bg_tenant_header_teal);
            }
        }
        ScreenUiHelper.setupBackToolbar(this, toolbar, getString(R.string.account_management));

        imgAvatar = findViewById(R.id.imgAvatar);
        imgPersonalIdCard = findViewById(R.id.imgPersonalIdCard);
        edtProfileName = findViewById(R.id.edtProfileName);
        edtProfileEmail = findViewById(R.id.edtProfileEmail);
        edtProfilePhone = findViewById(R.id.edtProfilePhone);
        edtProfilePersonalId = findViewById(R.id.edtProfilePersonalId);
        edtOwnerBankAccountNo = findViewById(R.id.edtOwnerBankAccountNo);
        edtOwnerBankName = findViewById(R.id.edtOwnerBankName);
        edtOwnerBankAccountName = findViewById(R.id.edtOwnerBankAccountName);
        tvRepresentativeInfo = findViewById(R.id.tvRepresentativeInfo);
        tvAuthMethod = findViewById(R.id.tvAuthMethod);
        edtInviteCode = findViewById(R.id.edtInviteCode);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnApplyInvite = findViewById(R.id.btnApplyInvite);
        layoutInviteSection = findViewById(R.id.layoutInviteSection);
        layoutTenantIdentitySection = findViewById(R.id.layoutTenantIdentitySection);
        layoutOwnerBankSection = findViewById(R.id.layoutOwnerBankSection);
        imgOwnerBankQr = findViewById(R.id.imgOwnerBankQr);

        imgAvatar.setOnClickListener(v -> avatarPickerLauncher.launch("image/*"));
        if (imgPersonalIdCard != null) {
            imgPersonalIdCard.setOnClickListener(v -> personalIdPickerLauncher.launch("image/*"));
        }
        if (imgOwnerBankQr != null) {
            imgOwnerBankQr.setOnClickListener(v -> bankQrPickerLauncher.launch("image/*"));
        }

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
                        updateInviteSectionVisibility(doc.getString("primaryRole"));

                        String fullName = doc.getString("fullName");
                        String phoneNumber = doc.getString("phoneNumber");
                        String avatarUrl = doc.getString("avatarUrl");
                        String personalId = doc.getString("personalId");
                        String personalIdImageUrl = doc.getString("personalIdImageUrl");
                        String ownerBankAccountNo = doc.getString("bankAccountNo");
                        String ownerBankName = doc.getString("bankName");
                        String ownerBankAccountName = doc.getString("bankAccountName");
                        String ownerBankQrUrl = doc.getString("paymentQrUrl");
                        activeTenantId = doc.getString("activeTenantId");

                        if (fullName != null && !fullName.isEmpty()) {
                            edtProfileName.setText(fullName);
                        }
                        if (phoneNumber != null) {
                            edtProfilePhone.setText(phoneNumber);
                        }
                        if (personalId != null && edtProfilePersonalId != null) {
                            edtProfilePersonalId.setText(personalId);
                        }
                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            Glide.with(this).load(avatarUrl).circleCrop().into(imgAvatar);
                        }
                        if (personalIdImageUrl != null && !personalIdImageUrl.isEmpty() && imgPersonalIdCard != null) {
                            currentPersonalIdImageUrl = personalIdImageUrl;
                            Glide.with(this).load(personalIdImageUrl).into(imgPersonalIdCard);
                        }
                        if (edtOwnerBankAccountNo != null && ownerBankAccountNo != null) {
                            edtOwnerBankAccountNo.setText(ownerBankAccountNo);
                        }
                        if (edtOwnerBankName != null && ownerBankName != null) {
                            edtOwnerBankName.setText(ownerBankName);
                        }
                        if (edtOwnerBankAccountName != null && ownerBankAccountName != null) {
                            edtOwnerBankAccountName.setText(ownerBankAccountName);
                        }
                        if (imgOwnerBankQr != null && ownerBankQrUrl != null && !ownerBankQrUrl.trim().isEmpty()) {
                            currentOwnerBankQrUrl = ownerBankQrUrl.trim();
                            Glide.with(this).load(currentOwnerBankQrUrl).into(imgOwnerBankQr);
                        }

                        resolveTenantRepresentative(user.getUid(), activeTenantId, doc.getString("activeContractMemberRole"));
                    }
                });
    }

    private void updateInviteSectionVisibility(String primaryRole) {
        if (layoutInviteSection == null) {
            return;
        }
        isOwnerPrimaryRole = primaryRole != null && TenantRoles.OWNER.equalsIgnoreCase(primaryRole.trim());
        layoutInviteSection.setVisibility(isOwnerPrimaryRole ? View.GONE : View.VISIBLE);
        applyTenantIdentitySectionVisibility();
    }

    private void resolveTenantRepresentative(String uid, String tenantId, String roleHint) {
        if (isOwnerPrimaryRole) {
            isTenantRepresentative = false;
            applyTenantIdentitySectionVisibility();
            return;
        }

        if (isRepresentativeRole(roleHint)) {
            isTenantRepresentative = true;
            applyTenantIdentitySectionVisibility();
            return;
        }

        if (tenantId == null || tenantId.trim().isEmpty() || uid == null || uid.trim().isEmpty()) {
            isTenantRepresentative = false;
            applyTenantIdentitySectionVisibility();
            return;
        }

        db.collection("tenants").document(tenantId.trim())
                .collection("members").document(uid)
                .get()
                .addOnSuccessListener(memberDoc -> {
                    boolean representative = false;
                    if (memberDoc.exists()) {
                        Boolean representativeField = memberDoc.getBoolean("contractRepresentative");
                        representative = representativeField != null && representativeField;
                        if (!representative) {
                            representative = isRepresentativeRole(memberDoc.getString("contractMemberRole"));
                        }
                    }
                    isTenantRepresentative = representative;
                    applyTenantIdentitySectionVisibility();
                })
                .addOnFailureListener(e -> {
                    isTenantRepresentative = false;
                    applyTenantIdentitySectionVisibility();
                });
    }

    private boolean isRepresentativeRole(String roleValue) {
        return roleValue != null && "REPRESENTATIVE".equalsIgnoreCase(roleValue.trim());
    }

    private void applyTenantIdentitySectionVisibility() {
        if (layoutTenantIdentitySection != null) {
            layoutTenantIdentitySection.setVisibility((isOwnerPrimaryRole || isTenantRepresentative) ? View.GONE : View.VISIBLE);
        }
        if (tvRepresentativeInfo != null) {
            tvRepresentativeInfo.setVisibility((!isOwnerPrimaryRole && isTenantRepresentative) ? View.VISIBLE : View.GONE);
        }
        if (layoutOwnerBankSection != null) {
            layoutOwnerBankSection.setVisibility(isOwnerPrimaryRole ? View.VISIBLE : View.GONE);
        }
    }

    private void startSaveProcess() {
        String fullName = edtProfileName.getText() != null ? edtProfileName.getText().toString().trim() : "";
        String phone = edtProfilePhone.getText() != null ? edtProfilePhone.getText().toString().trim() : "";
        String personalId = edtProfilePersonalId != null && edtProfilePersonalId.getText() != null
                ? edtProfilePersonalId.getText().toString().trim()
                : "";
        if (fullName.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_enter_full_name), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isOwnerPrimaryRole && !isTenantRepresentative) {
            if (phone.isEmpty() || personalId.isEmpty()) {
                Toast.makeText(this, getString(R.string.profile_missing_required_for_tenant), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        btnSaveProfile.setEnabled(false);
        uploadedAvatarUrl = null;
        uploadedPersonalIdImageUrl = null;
        uploadedOwnerBankQrUrl = null;

        if (selectedAvatarUri != null) {
            startImageUpload(selectedAvatarUri, UPLOAD_PURPOSE_AVATAR);
            Toast.makeText(this, getString(R.string.uploading_image), Toast.LENGTH_SHORT).show();
        } else if (selectedPersonalIdUri != null) {
            startImageUpload(selectedPersonalIdUri, UPLOAD_PURPOSE_PERSONAL_ID);
            Toast.makeText(this, getString(R.string.profile_uploading_personal_id_image), Toast.LENGTH_SHORT).show();
        } else if (selectedOwnerBankQrUri != null) {
            startImageUpload(selectedOwnerBankQrUri, UPLOAD_PURPOSE_BANK_QR);
            Toast.makeText(this, getString(R.string.profile_uploading_bank_qr), Toast.LENGTH_SHORT).show();
        } else {
            saveUserInfoToFirebase(null, null, null);
        }
    }

    private void startImageUpload(Uri imageUri, String purpose) {
        Intent serviceIntent = new Intent(this, ImageUploadService.class);
        serviceIntent.putExtra(ImageUploadService.EXTRA_IMAGE_URI, imageUri.toString());
        serviceIntent.putExtra(ImageUploadService.EXTRA_UPLOAD_PURPOSE, purpose);
        startService(serviceIntent);
    }

    private void saveUserInfoToFirebase(String avatarUrl, String personalIdImageUrl, String bankQrUrl) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            btnSaveProfile.setEnabled(true);
            return;
        }

        String fullName = edtProfileName.getText() != null ? edtProfileName.getText().toString().trim() : "";
        String phoneNumber = edtProfilePhone.getText() != null ? edtProfilePhone.getText().toString().trim() : "";
        String personalId = edtProfilePersonalId != null && edtProfilePersonalId.getText() != null
                ? edtProfilePersonalId.getText().toString().trim()
                : "";
        String finalPersonalIdImageUrl = personalIdImageUrl != null ? personalIdImageUrl : currentPersonalIdImageUrl;
        String bankAccountNo = edtOwnerBankAccountNo != null && edtOwnerBankAccountNo.getText() != null
            ? edtOwnerBankAccountNo.getText().toString().trim()
            : "";
        String bankName = edtOwnerBankName != null && edtOwnerBankName.getText() != null
            ? edtOwnerBankName.getText().toString().trim()
            : "";
        String bankAccountName = edtOwnerBankAccountName != null && edtOwnerBankAccountName.getText() != null
            ? edtOwnerBankAccountName.getText().toString().trim()
            : "";
        String finalBankQrUrl = bankQrUrl != null ? bankQrUrl : currentOwnerBankQrUrl;

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
                    updates.put("personalId", personalId);
                    updates.put("updatedAt", Timestamp.now());
                    if (avatarUrl != null) {
                        updates.put("avatarUrl", avatarUrl);
                    }
                    if (finalPersonalIdImageUrl != null && !finalPersonalIdImageUrl.trim().isEmpty()) {
                        updates.put("personalIdImageUrl", finalPersonalIdImageUrl);
                    }
                    if (isOwnerPrimaryRole) {
                        updates.put("bankAccountNo", bankAccountNo);
                        updates.put("bankName", bankName);
                        updates.put("bankAccountName", bankAccountName);
                        if (finalBankQrUrl != null && !finalBankQrUrl.trim().isEmpty()) {
                            updates.put("paymentQrUrl", finalBankQrUrl);
                        }
                    }

                    db.collection("users").document(user.getUid())
                            .update(updates)
                            .addOnSuccessListener(aVoid2 -> {
                                syncContractMemberProfile(user.getUid(), fullName, phoneNumber, personalId,
                                        finalPersonalIdImageUrl, () -> {
                                            selectedAvatarUri = null;
                                            selectedPersonalIdUri = null;
                                            selectedOwnerBankQrUri = null;
                                            currentPersonalIdImageUrl = finalPersonalIdImageUrl;
                                            currentOwnerBankQrUrl = finalBankQrUrl;
                                            Toast.makeText(this, getString(R.string.update_success), Toast.LENGTH_SHORT).show();
                                            btnSaveProfile.setEnabled(true);
                                            setResult(RESULT_OK);
                                            finish();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                db.collection("users").document(user.getUid())
                                        .set(updates, SetOptions.merge())
                                        .addOnSuccessListener(aVoid3 -> {
                                            syncContractMemberProfile(user.getUid(), fullName, phoneNumber, personalId,
                                                    finalPersonalIdImageUrl, () -> {
                                                        selectedAvatarUri = null;
                                                        selectedPersonalIdUri = null;
                                                        selectedOwnerBankQrUri = null;
                                                        currentPersonalIdImageUrl = finalPersonalIdImageUrl;
                                                        currentOwnerBankQrUrl = finalBankQrUrl;
                                                        Toast.makeText(this, getString(R.string.update_success), Toast.LENGTH_SHORT)
                                                                .show();
                                                        btnSaveProfile.setEnabled(true);
                                                        setResult(RESULT_OK);
                                                        finish();
                                                    });
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

    private void syncContractMemberProfile(String uid,
            String fullName,
            String phoneNumber,
            String personalId,
            String personalIdImageUrl,
            Runnable onDone) {
        if (isOwnerPrimaryRole) {
            onDone.run();
            return;
        }

        String tenantId = activeTenantId;
        if (tenantId == null || tenantId.trim().isEmpty()) {
            tenantId = TenantSession.getActiveTenantId();
        }
        if (tenantId == null || tenantId.trim().isEmpty()) {
            onDone.run();
            return;
        }

        String safeTenantId = tenantId.trim();
        db.collection("tenants").document(safeTenantId)
                .collection("members").document(uid)
                .get()
                .addOnSuccessListener(memberDoc -> {
                    String contractId = memberDoc.getString("contractId");
                    String roomId = memberDoc.getString("roomId");

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("fullName", fullName);
                    updates.put("phoneNumber", phoneNumber);
                    updates.put("personalId", personalId);
                    updates.put("updatedAt", System.currentTimeMillis());
                    updates.put("active", true);

                    if (personalIdImageUrl != null && !personalIdImageUrl.trim().isEmpty()) {
                        updates.put("personalIdImageUrl", personalIdImageUrl);
                    }

                    if (contractId != null && !contractId.trim().isEmpty()) {
                        updates.put("contractId", contractId.trim());
                    }
                    if (roomId != null && !roomId.trim().isEmpty()) {
                        updates.put("roomId", roomId.trim());
                    }

                    boolean representative = isTenantRepresentative;
                    Boolean representativeField = memberDoc.getBoolean("contractRepresentative");
                    if (representativeField != null) {
                        representative = representativeField;
                    }

                    if (representative) {
                        updates.put("contractRepresentative", true);
                        updates.put("primaryContact", true);
                    } else {
                        boolean fullyDocumented = !fullName.isEmpty() && !phoneNumber.isEmpty() && !personalId.isEmpty();
                        updates.put("fullyDocumented", fullyDocumented);
                    }

                    db.collection("tenants").document(safeTenantId)
                            .collection("contractMembers").document(uid)
                            .set(updates, SetOptions.merge())
                            .addOnSuccessListener(v -> onDone.run())
                            .addOnFailureListener(e -> onDone.run());
                })
                .addOnFailureListener(e -> onDone.run());
    }

    private void applyInviteCode() {
        if (edtInviteCode == null || btnApplyInvite == null) {
            return;
        }

        if (isOwnerPrimaryRole) {
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
