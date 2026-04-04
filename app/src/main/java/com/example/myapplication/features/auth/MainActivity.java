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
import com.example.myapplication.core.util.LanguageManager;
import com.example.myapplication.core.util.LanguageSwitcherHelper;
import com.example.myapplication.features.home.HomeMenuActivity;
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

    @Override
    protected void onStart() {
        super.onStart();
        // If already signed in, navigate directly to Home
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(this, HomeMenuActivity.class));
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
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
                        navigateToHome();
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
                    navigateToHome();
                })
                .addOnFailureListener(e -> Toast
                        .makeText(this, getString(R.string.google_sign_in_failed), Toast.LENGTH_SHORT).show());
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
