package com.example.myapplication.features.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.core.util.LanguageSwitcherHelper;
import com.example.myapplication.features.home.HomeMenuActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private EditText edtFullName, edtEmail, edtPassword, edtConfirmPassword, edtPhoneNumber;
    private Button btnSignUp;
    private TextView tvGoToLogin;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private LanguageSwitcherHelper languageSwitcherHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        edtFullName = findViewById(R.id.edtHoTen);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        edtPhoneNumber = findViewById(R.id.edtSoDienThoai);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        // Setup language switcher using helper
        languageSwitcherHelper = new LanguageSwitcherHelper(this);
        languageSwitcherHelper.setupLanguageSwitcher();

        btnSignUp.setOnClickListener(v -> register());

        tvGoToLogin.setOnClickListener(v -> {
            finish();
        });
    }

    private void register() {
        String fullName = edtFullName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String confirmPassword = edtConfirmPassword.getText().toString().trim();
        String phoneNumber = edtPhoneNumber.getText().toString().trim();

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()
                || confirmPassword.isEmpty() || phoneNumber.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_fill_all_information), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, getString(R.string.password_confirmation_mismatch), Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, getString(R.string.password_min_6_chars), Toast.LENGTH_SHORT).show();
            return;
        }

        btnSignUp.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();

                    // Update displayName and wait for completion before proceeding
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(fullName)
                            .build();
                    authResult.getUser().updateProfile(profileUpdates)
                            .addOnCompleteListener(profileTask -> {
                                // Save additional profile data to Firestore
                                Map<String, Object> user = new HashMap<>();
                                user.put("fullName", fullName);
                                user.put("email", email);
                                user.put("phoneNumber", phoneNumber);
                                user.put("uid", uid);
                                user.put("primaryRole", "TENANT");
                                user.put("activeTenantId", null);
                                user.put("createdAt", Timestamp.now());
                                user.put("updatedAt", Timestamp.now());

                                db.collection("users").document(uid)
                                        .set(user)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(this, getString(R.string.registration_success),
                                                    Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(this, HomeMenuActivity.class));
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, getString(R.string.error_save_info) + e.getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                            btnSignUp.setEnabled(true);
                                        });
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.registration_failed) + e.getMessage(), Toast.LENGTH_SHORT)
                            .show();
                    btnSignUp.setEnabled(true);
                });
    }
}
