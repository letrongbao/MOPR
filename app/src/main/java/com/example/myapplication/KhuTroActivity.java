package com.example.myapplication;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.model.KhuTro;
import com.example.myapplication.repository.KhuTroRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class KhuTroActivity extends AppCompatActivity {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final KhuTroRepository repo = new KhuTroRepository();

    private KhuTroAdapter adapter;
    private TextView tvEmpty;
    private FloatingActionButton fabAdd;

    private boolean readOnly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_khu_tro);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Khu trọ");
        }

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        tvEmpty = findViewById(R.id.tvEmpty);
        fabAdd = findViewById(R.id.fabAdd);
        RecyclerView rv = findViewById(R.id.recyclerView);

        adapter = new KhuTroAdapter(this::showDetailDialog);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> showCreateDialog(null));

        setupPermissionsThenObserve();
    }

    private void setupPermissionsThenObserve() {
        String tenantId = TenantSession.getActiveTenantId();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (tenantId == null || tenantId.trim().isEmpty() || user == null) {
            readOnly = false;
            observe();
            return;
        }

        db.collection("tenants").document(tenantId)
                .collection("members").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String role = doc.getString("role");
                    readOnly = TenantRoles.TENANT.equals(role);
                    fabAdd.setVisibility(readOnly ? View.GONE : View.VISIBLE);
                    observe();
                })
                .addOnFailureListener(e -> {
                    readOnly = false;
                    observe();
                });
    }

    private void observe() {
        repo.listAll().observe(this, list -> {
            adapter.setItems(list);
            tvEmpty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void showDetailDialog(@NonNull KhuTro k) {
        String msg = "Tên: " + safe(k.getTenKhu())
                + "\nĐịa chỉ: " + safe(k.getDiaChi())
                + (k.getGhiChu() != null && !k.getGhiChu().trim().isEmpty() ? ("\nGhi chú: " + k.getGhiChu()) : "");

        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle("Khu trọ")
                .setMessage(msg)
                .setPositiveButton("OK", null);

        if (!readOnly) {
            b.setNeutralButton("Sửa", (d, w) -> showCreateDialog(k));
            b.setNegativeButton("Xóa", (d, w) -> {
                new AlertDialog.Builder(this)
                        .setTitle("Xóa khu?")
                        .setMessage("Xóa '" + safe(k.getTenKhu()) + "'?")
                        .setPositiveButton("Xóa", (d2, w2) -> repo.delete(k.getId(),
                                () -> runOnUiThread(() -> Toast.makeText(this, "Đã xóa", Toast.LENGTH_SHORT).show()),
                                () -> runOnUiThread(() -> Toast.makeText(this, "Xóa thất bại", Toast.LENGTH_SHORT).show())))
                        .setNegativeButton("Hủy", null)
                        .show();
            });
        }

        b.show();
    }

    private void showCreateDialog(KhuTro existing) {
        View v = getLayoutInflater().inflate(R.layout.dialog_create_khu, null);
        EditText etName = v.findViewById(R.id.etKhuName);
        EditText etAddr = v.findViewById(R.id.etKhuAddr);
        EditText etNote = v.findViewById(R.id.etKhuNote);

        boolean isEdit = existing != null;
        if (isEdit) {
            etName.setText(safe(existing.getTenKhu()));
            etAddr.setText(safe(existing.getDiaChi()));
            etNote.setText(safe(existing.getGhiChu()));
        }

        new AlertDialog.Builder(this)
                .setTitle(isEdit ? "Sửa khu" : "Thêm khu")
                .setView(v)
                .setNegativeButton("Hủy", null)
                .setPositiveButton(isEdit ? "Lưu" : "Thêm", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập tên khu", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String addr = etAddr.getText().toString().trim();
                    String note = etNote.getText().toString().trim();

                    if (isEdit) {
                        existing.setTenKhu(name);
                        existing.setDiaChi(addr);
                        existing.setGhiChu(note);
                        repo.update(existing,
                                () -> runOnUiThread(() -> Toast.makeText(this, "Đã lưu", Toast.LENGTH_SHORT).show()),
                                () -> runOnUiThread(() -> Toast.makeText(this, "Lưu thất bại", Toast.LENGTH_SHORT).show()));
                    } else {
                        KhuTro k = new KhuTro(name, addr, note);
                        repo.add(k,
                                () -> runOnUiThread(() -> Toast.makeText(this, "Đã thêm", Toast.LENGTH_SHORT).show()),
                                () -> runOnUiThread(() -> Toast.makeText(this, "Thêm thất bại", Toast.LENGTH_SHORT).show()));
                    }
                })
                .show();
    }

    private String safe(String s) {
        return s != null ? s : "";
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
