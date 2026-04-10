package com.example.myapplication.features.settings;

import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.myapplication.R;
import com.example.myapplication.core.util.AuthProviderUtil;
import com.example.myapplication.core.util.ScreenUiHelper;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private TextInputEditText edtNewPassword, edtConfirmNewPassword;
    private Button btnChangePassword;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        ScreenUiHelper.enableEdgeToEdge(this, false);

        setContentView(R.layout.activity_change_password);

        mAuth = FirebaseAuth.getInstance();

        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            ScreenUiHelper.applyTopInset(appBarLayout);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        ScreenUiHelper.setupBackToolbar(this, toolbar, getString(R.string.change_password));

        edtNewPassword = findViewById(R.id.edtNewPassword);
        edtConfirmNewPassword = findViewById(R.id.edtConfirmNewPassword);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        FirebaseUser user = mAuth.getCurrentUser();
        if (!AuthProviderUtil.canChangePassword(user)) {
            Toast.makeText(this, getString(R.string.change_password_not_available_for_google), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btnChangePassword.setOnClickListener(v -> changePassword());
    }

    private void changePassword() {
        String newPass = edtNewPassword.getText() != null ? edtNewPassword.getText().toString().trim() : "";
        String confirmPass = edtConfirmNewPassword.getText() != null ? edtConfirmNewPassword.getText().toString().trim()
                : "";

        if (newPass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_fill_all_information), Toast.LENGTH_SHORT).show();
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
        if (user == null) {
            Toast.makeText(this, getString(R.string.please_login_again), Toast.LENGTH_SHORT).show();
            return;
        }
        if (!AuthProviderUtil.canChangePassword(user)) {
            Toast.makeText(this, getString(R.string.change_password_not_available_for_google), Toast.LENGTH_LONG).show();
            return;
        }

        btnChangePassword.setEnabled(false);

        user.updatePassword(newPass)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, getString(R.string.password_changed_success), Toast.LENGTH_SHORT).show();
                    btnChangePassword.setEnabled(true);
                    finish();
                })
                .addOnFailureListener(e -> {
                    String errorMessage = e.getMessage();
                    if (errorMessage != null && errorMessage.contains("recent")) {
                        Toast.makeText(this, getString(R.string.please_login_again_to_change_password), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, getString(R.string.error_colon) + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                    btnChangePassword.setEnabled(true);
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
