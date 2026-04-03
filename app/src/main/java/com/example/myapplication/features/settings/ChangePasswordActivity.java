package com.example.myapplication.features.settings;

import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.myapplication.R;
import com.example.myapplication.core.util.ScreenUiHelper;
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

        com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            ScreenUiHelper.applyTopInset(appBarLayout);
        }

        mAuth = FirebaseAuth.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        ScreenUiHelper.setupBackToolbar(this, toolbar, "Đổi mật khẩu");

        edtNewPassword = findViewById(R.id.edtNewPassword);
        edtConfirmNewPassword = findViewById(R.id.edtConfirmNewPassword);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        btnChangePassword.setOnClickListener(v -> changePassword());
    }

    private void changePassword() {
        String newPass = edtNewPassword.getText() != null ? edtNewPassword.getText().toString().trim() : "";
        String confirmPass = edtConfirmNewPassword.getText() != null ? edtConfirmNewPassword.getText().toString().trim()
                : "";

        if (newPass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
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
        if (user == null) {
            Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            return;
        }

        btnChangePassword.setEnabled(false);

        user.updatePassword(newPass)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                    btnChangePassword.setEnabled(true);
                    finish();
                })
                .addOnFailureListener(e -> {
                    String errorMessage = e.getMessage();
                    if (errorMessage != null && errorMessage.contains("recent")) {
                        Toast.makeText(this, "Vui lòng đăng nhập lại để đổi mật khẩu", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Lỗi: " + errorMessage, Toast.LENGTH_SHORT).show();
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
