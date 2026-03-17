package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private EditText username;
    private EditText password;
    private Button loginButton;
    private TextView signupText;
    private CheckBox cbRememberMe;
    private FirebaseAuth mAuth;
    private SharedPreferences prefs;

    @Override
    protected void onStart() {
        super.onStart();
        // Nếu đã đăng nhập rồi thì vào thẳng Home
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        prefs = getSharedPreferences("NhaTroPrefs", MODE_PRIVATE);

        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        loginButton = findViewById(R.id.loginButton);
        signupText = findViewById(R.id.signupText);
        cbRememberMe = findViewById(R.id.cbRememberMe);

        // Khôi phục email nếu đã lưu "Ghi nhớ" (không lưu mật khẩu plaintext)
        boolean remembered = prefs.getBoolean("rememberMe", false);
        if (remembered) {
            username.setText(prefs.getString("savedEmail", ""));
            cbRememberMe.setChecked(true);
        }

        signupText.setOnClickListener(v -> {
            startActivity(new Intent(this, SignUpActivity.class));
        });

        loginButton.setOnClickListener(v -> {
            String email = username.getText().toString().trim();
            String pass = password.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(result -> {
                    // Lưu SharedPreferences nếu chọn "Ghi nhớ" (chỉ lưu email, không lưu mật khẩu)
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

                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                })
                .addOnFailureListener(e ->
                    Toast.makeText(this, "Sai tài khoản hoặc mật khẩu", Toast.LENGTH_SHORT).show()
                );
        });
    }
}
