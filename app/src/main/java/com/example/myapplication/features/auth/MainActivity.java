package com.example.myapplication.features.auth;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.R;
import com.example.myapplication.core.session.TenantRepository;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.LanguageManager;
import com.example.myapplication.core.util.LanguageSwitcherHelper;
import com.example.myapplication.features.home.HomeMenuActivity;
import com.example.myapplication.features.home.TenantMenuActivity;
import com.example.myapplication.core.constants.TenantRoles;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText username;
    private EditText password;
    private Button loginButton;
    private TextView signupText;
    private CheckBox cbRememberMe;
    private FirebaseAuth mAuth;
    private SharedPreferences prefs;
    private LanguageSwitcherHelper languageSwitcherHelper;
    private Button btnGoogleSignIn;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private FirebaseFirestore db;

    @Override
    protected void onStart() {
        super.onStart();
        // Nếu đã đăng nhập, kiểm tra role để điều hướng đúng màn hình.
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            ensureUserProfileDocument(currentUser, () -> checkRoleAndNavigate(currentUser));
        }
    }

    /**
     * Kiểm tra role của user trong Firestore.
     * - TENANT + activeTenantId: vào TenantMenuActivity (auto-login cho khách đã liên kết phòng)
        * - TENANT + không có activeTenantId: vào HomeMenuActivity (sau đó Home điều hướng sang JoinRoomActivity)
        * - OWNER hoặc khác: vào HomeMenuActivity
     */
    private void checkRoleAndNavigate(FirebaseUser user) {
        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        TenantSession.clear(this);
                        navigateToHome();
                        return;
                    }

                    String role = doc.getString("primaryRole");
                    String activeTenantId = doc.getString("activeTenantId");

                    boolean isTenant = TenantRoles.TENANT.equalsIgnoreCase(role);
                    boolean isOwner = TenantRoles.OWNER.equalsIgnoreCase(role);
                    boolean hasRoom  = activeTenantId != null && !activeTenantId.trim().isEmpty();

                    if (hasRoom) {
                        TenantSession.setActiveTenantId(this, activeTenantId.trim());
                    } else {
                        TenantSession.clear(this);
                    }

                    if (!hasRoom) {
                        resolveOwnerTenantAndNavigate(user, isOwner);
                        return;
                    }

                    if (isTenant && hasRoom) {
                        // Khách đã liên kết phòng: lấy roomId rồi vào TenantMenuActivity
                        fetchRoomIdAndNavigateToTenantMenu(activeTenantId, user.getUid());
                    } else {
                        // Chủ nhà hoặc khách chưa liên kết: vào HomeMenuActivity bình thường
                        navigateToHome();
                    }
                })
                .addOnFailureListener(e -> {
                    TenantSession.clear(this);
                    navigateToHome();
                });
    }

    private void resolveOwnerTenantAndNavigate(FirebaseUser user, boolean allowCreateOwnerTenantIfMissing) {
        findOwnedTenantId(user.getUid(), tenantId -> {
            if (tenantId == null || tenantId.trim().isEmpty()) {
                if (allowCreateOwnerTenantIfMissing) {
                    ensureOwnerTenantAndNavigate(user);
                    return;
                }

                TenantSession.clear(this);
                navigateToHome();
                return;
            }

            String normalizedTenantId = tenantId.trim();
            TenantSession.setActiveTenantId(this, normalizedTenantId);

            Map<String, Object> update = new HashMap<>();
            update.put("primaryRole", TenantRoles.OWNER);
            update.put("activeTenantId", normalizedTenantId);
            update.put("updatedAt", Timestamp.now());

            db.collection("users").document(user.getUid())
                    .set(update, SetOptions.merge())
                    .addOnCompleteListener(task -> navigateToHome());
        });
    }

    private void ensureOwnerTenantAndNavigate(FirebaseUser user) {
        TenantRepository repo = new TenantRepository();
        repo.ensureActiveTenant(this, new TenantRepository.TenantReadyCallback() {
            @Override
            public void onReady(String tenantId) {
                if (tenantId == null || tenantId.trim().isEmpty()) {
                    TenantSession.clear(MainActivity.this);
                    navigateToHome();
                    return;
                }

                String normalizedTenantId = tenantId.trim();
                TenantSession.setActiveTenantId(MainActivity.this, normalizedTenantId);

                Map<String, Object> update = new HashMap<>();
                update.put("primaryRole", TenantRoles.OWNER);
                update.put("activeTenantId", normalizedTenantId);
                update.put("updatedAt", Timestamp.now());

                db.collection("users").document(user.getUid())
                        .set(update, SetOptions.merge())
                        .addOnCompleteListener(task -> navigateToHome());
            }

            @Override
            public void onError(Exception e) {
                TenantSession.clear(MainActivity.this);
                navigateToHome();
            }
        });
    }

    private interface OwnedTenantCallback {
        void onResult(String tenantId);
    }

    private void findOwnedTenantId(String uid, OwnedTenantCallback callback) {
        db.collection("tenants")
                .whereEqualTo("ownerUid", uid)
                .limit(1)
                .get()
                .addOnSuccessListener(qs -> {
                    if (qs != null && !qs.isEmpty()) {
                        callback.onResult(qs.getDocuments().get(0).getId());
                        return;
                    }

                    db.collectionGroup("members")
                            .whereEqualTo("uid", uid)
                            .limit(20)
                            .get()
                            .addOnSuccessListener(memberQs -> {
                                if (memberQs == null || memberQs.isEmpty()) {
                                    callback.onResult(null);
                                    return;
                                }

                                String selectedTenantId = null;
                                for (com.google.firebase.firestore.DocumentSnapshot memberDoc : memberQs.getDocuments()) {
                                    com.google.firebase.firestore.DocumentReference tenantRef = memberDoc
                                            .getReference()
                                            .getParent()
                                            .getParent();
                                    if (tenantRef == null) {
                                        continue;
                                    }

                                    String role = memberDoc.getString("role");
                                    if (role != null && TenantRoles.OWNER.equalsIgnoreCase(role.trim())) {
                                        selectedTenantId = tenantRef.getId();
                                        break;
                                    }

                                    if (selectedTenantId == null || selectedTenantId.trim().isEmpty()) {
                                        selectedTenantId = tenantRef.getId();
                                    }
                                }

                                callback.onResult(selectedTenantId);
                            })
                            .addOnFailureListener(e -> callback.onResult(null));
                })
                .addOnFailureListener(e -> callback.onResult(null));
    }

    private void fetchRoomIdAndNavigateToTenantMenu(String tenantId, String uid) {
        db.collection("tenants").document(tenantId)
                .collection("members").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    String roomId = doc.exists() ? doc.getString("roomId") : null;
                    Intent intent = new Intent(this, TenantMenuActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.putExtra("TENANT_ID", tenantId);
                    if (roomId != null) intent.putExtra("ROOM_ID", roomId);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Fallback: vào TenantMenuActivity không có roomId
                    Intent intent = new Intent(this, TenantMenuActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.putExtra("TENANT_ID", tenantId);
                    startActivity(intent);
                    finish();
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefs = getSharedPreferences("NhaTroPrefs", MODE_PRIVATE);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                        return;
                    }

                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        firebaseAuthWithGoogle(account != null ? account.getIdToken() : null);
                    } catch (ApiException e) {
                        Toast.makeText(this, getString(R.string.google_sign_in_failed), Toast.LENGTH_SHORT).show();
                    }
                });

        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        loginButton = findViewById(R.id.loginButton);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        signupText = findViewById(R.id.signupText);
        cbRememberMe = findViewById(R.id.cbRememberMe);

        // Setup language switcher using helper
        languageSwitcherHelper = new LanguageSwitcherHelper(this);
        languageSwitcherHelper.setupLanguageSwitcher();
        setupGoogleSignIn();

        // Restore remembered email only (no plaintext password storage)
        boolean remembered = prefs.getBoolean("rememberMe", false);
        if (remembered) {
            username.setText(prefs.getString("savedEmail", ""));
            cbRememberMe.setChecked(true);
        }

        signupText.setOnClickListener(v -> {
            startActivity(new Intent(this, SignUpActivity.class));
        });

        if (btnGoogleSignIn != null) {
            btnGoogleSignIn.setOnClickListener(v -> {
                if (googleSignInClient == null) {
                    Toast.makeText(this, getString(R.string.google_sign_in_not_configured), Toast.LENGTH_LONG).show();
                    return;
                }
                googleSignInLauncher.launch(googleSignInClient.getSignInIntent());
            });
        }

        loginButton.setOnClickListener(v -> {
            String email = username.getText().toString().trim();
            String pass = password.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, getString(R.string.please_fill_all_fields), Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, pass)
                    .addOnSuccessListener(result -> {
                        saveRememberPreference(email);
                        FirebaseUser signedInUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (signedInUser == null) {
                            TenantSession.clear(this);
                            navigateToHome();
                            return;
                        }
                        ensureUserProfileDocument(signedInUser, () -> checkRoleAndNavigate(signedInUser));
                    })
                    .addOnFailureListener(
                            e -> Toast.makeText(this, getString(R.string.wrong_credentials), Toast.LENGTH_SHORT)
                                    .show());
        });
    }

    private void setupGoogleSignIn() {
        int webClientIdResId = getResources().getIdentifier("default_web_client_id", "string", getPackageName());
        if (webClientIdResId == 0) {
            googleSignInClient = null;
            return;
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(webClientIdResId))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        if (idToken == null || idToken.trim().isEmpty()) {
            Toast.makeText(this, getString(R.string.google_sign_in_failed), Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    String email = user != null ? user.getEmail() : "";
                    saveRememberPreference(email != null ? email : "");
                    if (user == null) {
                        TenantSession.clear(this);
                        navigateToHome();
                        return;
                    }
                    ensureUserProfileDocument(user, () -> checkRoleAndNavigate(user));
                })
                .addOnFailureListener(e -> Toast
                        .makeText(this, getString(R.string.google_sign_in_failed), Toast.LENGTH_SHORT).show());
    }

    private void ensureUserProfileDocument(FirebaseUser user, Runnable onDone) {
        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("uid", user.getUid());
                    payload.put("email", user.getEmail());
                    // BUG FIX: Chỉ cập nhật fullName nếu có giá trị thực
                    // (tránh ghi đè null khi login bằng email/password)
                    String displayName = user.getDisplayName();
                    if (displayName != null && !displayName.trim().isEmpty()) {
                        payload.put("fullName", displayName);
                    }
                    payload.put("updatedAt", Timestamp.now());

                    // Only set default role fields when the profile document does not exist yet.
                    if (!doc.exists()) {
                        payload.put("primaryRole", "TENANT");
                        payload.put("activeTenantId", null);
                        payload.put("createdAt", Timestamp.now());
                    }

                    db.collection("users").document(user.getUid())
                            .set(payload, SetOptions.merge())
                            .addOnSuccessListener(unused -> onDone.run())
                            .addOnFailureListener(e -> onDone.run());
                })
                .addOnFailureListener(e -> {
                    // Do not block the login flow if profile bootstrap fails.
                    onDone.run();
                });
    }

    private void saveRememberPreference(String email) {
        SharedPreferences.Editor editor = prefs.edit();
        if (cbRememberMe.isChecked()) {
            editor.putBoolean("rememberMe", true);
            editor.putString("savedEmail", email);
        } else {
            editor.putBoolean("rememberMe", false);
            editor.remove("savedEmail");
        }
        editor.remove("savedPassword");
        editor.apply();
    }

    private void navigateToHome() {
        startActivity(new Intent(this, HomeMenuActivity.class));
        finish();
    }
}
