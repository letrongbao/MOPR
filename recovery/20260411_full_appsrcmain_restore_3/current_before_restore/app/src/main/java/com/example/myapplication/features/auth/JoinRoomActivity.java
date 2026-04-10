package com.example.myapplication.features.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.core.session.InviteRepository;
import com.example.myapplication.features.home.TenantMenuActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class JoinRoomActivity extends AppCompatActivity {

    private EditText edtInviteCode;
    private Button btnJoin;
    private ProgressBar progressBar;
    private InviteRepository inviteRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_room);

        inviteRepository = new InviteRepository();

        edtInviteCode = findViewById(R.id.edtInviteCode);
        btnJoin = findViewById(R.id.btnJoin);
        progressBar = findViewById(R.id.progressBar);

        btnJoin.setOnClickListener(v -> joinRoom());
    }

    private void joinRoom() {
        String code = edtInviteCode.getText().toString().trim().toUpperCase();

        if (code.isEmpty()) {
            Toast.makeText(this, getString(R.string.join_room_enter_code_required), Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        inviteRepository.joinByAnonymousInvite(this, code, new InviteRepository.JoinCallback() {
            @Override
            public void onSuccess(String tenantId) {
                // Sau khi join thành công, lấy roomId từ Firestore members document
                fetchRoomIdAndNavigate(tenantId);
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                Toast.makeText(JoinRoomActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Lấy roomId từ document members của khách trong Firestore,
     * sau đó chuyển hướng sang TenantMenuActivity với đầy đủ dữ liệu.
     */
    private void fetchRoomIdAndNavigate(String tenantId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            setLoading(false);
            Toast.makeText(this, getString(R.string.join_room_session_expired), Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("tenants").document(tenantId)
                .collection("members").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    setLoading(false);
                    String roomId = doc.exists() ? doc.getString("roomId") : null;
                    Toast.makeText(JoinRoomActivity.this, getString(R.string.join_room_success), Toast.LENGTH_SHORT)
                            .show();
                    navigateToTenantMenu(tenantId, roomId);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    // Vẫn chuyển hướng dù không lấy được roomId
                    Toast.makeText(JoinRoomActivity.this, getString(R.string.join_room_success), Toast.LENGTH_SHORT)
                            .show();
                    navigateToTenantMenu(tenantId, null);
                });
    }

    /**
     * Chuyển hướng sang TenantMenuActivity với FLAG_ACTIVITY_CLEAR_TASK
     * để ngăn người dùng quay lại màn hình nhập mã.
     */
    private void navigateToTenantMenu(String tenantId, String roomId) {
        Intent intent = new Intent(JoinRoomActivity.this, TenantMenuActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (tenantId != null) {
            intent.putExtra("TENANT_ID", tenantId);
        }
        if (roomId != null) {
            intent.putExtra("ROOM_ID", roomId);
        }
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean isLoading) {
        btnJoin.setEnabled(!isLoading);
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
