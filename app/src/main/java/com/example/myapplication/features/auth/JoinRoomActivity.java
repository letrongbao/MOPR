package com.example.myapplication.features.auth;

import android.content.Intent;
import android.animation.ObjectAnimator;
import android.animation.AnimatorSet;
import android.view.animation.AccelerateDecelerateInterpolator;
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
    private View cardHero;
    private View cardInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_room);

        inviteRepository = new InviteRepository();

        edtInviteCode = findViewById(R.id.edtInviteCode);
        btnJoin = findViewById(R.id.btnJoin);
        progressBar = findViewById(R.id.progressBar);
        cardHero = findViewById(R.id.cardHero);
        cardInput = findViewById(R.id.cardInput);

        btnJoin.setOnClickListener(v -> joinRoom());
        playEnterAnimation();
    }

    private void joinRoom() {
        String code = edtInviteCode.getText().toString().trim().toUpperCase();

        if (code.isEmpty()) {
            playValidationAnimation();
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
        edtInviteCode.setEnabled(!isLoading);
        btnJoin.setText(isLoading ? getString(R.string.join_room_connecting) : getString(R.string.join_room_connect));
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void playEnterAnimation() {
        if (cardHero == null || cardInput == null) {
            return;
        }

        cardHero.setAlpha(0f);
        cardHero.setTranslationY(40f);
        cardInput.setAlpha(0f);
        cardInput.setTranslationY(54f);

        ObjectAnimator heroFade = ObjectAnimator.ofFloat(cardHero, View.ALPHA, 0f, 1f);
        ObjectAnimator heroSlide = ObjectAnimator.ofFloat(cardHero, View.TRANSLATION_Y, 40f, 0f);
        ObjectAnimator inputFade = ObjectAnimator.ofFloat(cardInput, View.ALPHA, 0f, 1f);
        ObjectAnimator inputSlide = ObjectAnimator.ofFloat(cardInput, View.TRANSLATION_Y, 54f, 0f);

        AnimatorSet heroSet = new AnimatorSet();
        heroSet.playTogether(heroFade, heroSlide);
        heroSet.setDuration(440);
        heroSet.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet inputSet = new AnimatorSet();
        inputSet.playTogether(inputFade, inputSlide);
        inputSet.setDuration(500);
        inputSet.setStartDelay(90);
        inputSet.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet all = new AnimatorSet();
        all.playTogether(heroSet, inputSet);
        all.start();
    }

    private void playValidationAnimation() {
        if (edtInviteCode == null) {
            return;
        }
        ObjectAnimator shake = ObjectAnimator.ofFloat(edtInviteCode,
                View.TRANSLATION_X,
                0f, 18f, -14f, 10f, -8f, 4f, 0f);
        shake.setDuration(360);
        shake.start();
        edtInviteCode.requestFocus();
    }
}
