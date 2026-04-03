package com.example.myapplication.features.settings;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.ScreenUiHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BackupRestoreActivity extends AppCompatActivity {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private BackupAdapter adapter;
    private TextView tvEmpty;

    private String tenantId;

    private static final String[] BACKUP_COLLECTIONS = new String[] {
            "houses",
            "rooms",
            "contracts",
            "invoices",
            "rentalHistory",
            "payments",
            "meterReadings",
            "expenses"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        ScreenUiHelper.enableEdgeToEdge(this, false);

        setContentView(R.layout.activity_backup_restore);

        com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            ScreenUiHelper.applyTopInset(appBarLayout);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        ScreenUiHelper.setupBackToolbar(this, toolbar, "Sao lưu & phục hồi");

        tvEmpty = findViewById(R.id.tvEmpty);
        RecyclerView rv = findViewById(R.id.recyclerView);
        FloatingActionButton fab = findViewById(R.id.fabAdd);

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BackupAdapter(item -> {
            Toast.makeText(this, "Khôi phục sẽ được bổ sung sau (để tránh ghi đè dữ liệu)", Toast.LENGTH_SHORT).show();
        });
        rv.setAdapter(adapter);

        fab.setOnClickListener(v -> showCreateBackupDialog());

        TenantSession.init(this);
        tenantId = TenantSession.getActiveTenantId();
        if (tenantId == null || tenantId.trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn tổ chức (tenant) để dùng sao lưu", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        observeBackups();
    }

    private void observeBackups() {
        backupsRef().orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null)
                        return;
                    java.util.List<BackupItem> list = new java.util.ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        BackupItem it = new BackupItem();
                        it.id = doc.getId();
                        it.note = doc.getString("note");
                        it.createdAt = doc.getTimestamp("createdAt");
                        it.createdBy = doc.getString("createdBy");
                        list.add(it);
                    }
                    adapter.setDanhSach(list);
                    tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void showCreateBackupDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_backup, null);
        EditText etNote = dialogView.findViewById(R.id.etNote);

        new AlertDialog.Builder(this)
                .setTitle("Tạo bản sao lưu")
                .setMessage("Sao lưu dữ liệu hiện tại lên Firestore (không ghi đè dữ liệu đang dùng).")
                .setView(dialogView)
                .setPositiveButton("Tạo", (d, w) -> startBackup(etNote.getText().toString().trim()))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void startBackup(String note) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
            return;

        String backupId = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        DocumentReference backupDoc = backupsRef().document(backupId);

        Map<String, Object> meta = new HashMap<>();
        meta.put("createdAt", Timestamp.now());
        meta.put("createdBy", user.getUid());
        meta.put("note", note);
        meta.put("version", 1);

        AlertDialog progress = new AlertDialog.Builder(this)
                .setTitle("Đang sao lưu")
                .setMessage("Vui lòng chờ...")
                .setCancelable(false)
                .create();
        progress.show();

        backupDoc.set(meta)
                .addOnSuccessListener(v -> backupCollectionsSequential(backupDoc, BACKUP_COLLECTIONS, 0, progress))
                .addOnFailureListener(e -> {
                    if (progress.isShowing())
                        progress.dismiss();
                    Toast.makeText(this, "Tạo backup thất bại", Toast.LENGTH_SHORT).show();
                });
    }

    private void backupCollectionsSequential(@NonNull DocumentReference backupDoc,
            @NonNull String[] collections,
            int idx,
            @NonNull AlertDialog progressDialog) {
        if (idx >= collections.length) {
            if (progressDialog.isShowing())
                progressDialog.dismiss();
            Toast.makeText(this, "Đã sao lưu xong", Toast.LENGTH_SHORT).show();
            return;
        }

        String col = collections[idx];
        scopedCollection(col).get()
                .addOnSuccessListener(snapshot -> writeCollectionSnapshot(backupDoc, col, snapshot, 0,
                        () -> backupCollectionsSequential(backupDoc, collections, idx + 1, progressDialog),
                        () -> {
                            if (progressDialog.isShowing())
                                progressDialog.dismiss();
                            Toast.makeText(this, "Sao lưu thất bại ở: " + col, Toast.LENGTH_SHORT).show();
                        }))
                .addOnFailureListener(e -> {
                    if (progressDialog.isShowing())
                        progressDialog.dismiss();
                    Toast.makeText(this, "Không đọc được dữ liệu: " + col, Toast.LENGTH_SHORT).show();
                });
    }

    private interface SimpleCallback {
        void run();
    }

    private void writeCollectionSnapshot(@NonNull DocumentReference backupDoc,
            @NonNull String col,
            @NonNull QuerySnapshot snapshot,
            int startIndex,
            @NonNull SimpleCallback onSuccess,
            @NonNull SimpleCallback onFail) {
        // Commit theo batch để tránh giới hạn 500 writes
        final int BATCH_SIZE = 400;

        List<com.google.firebase.firestore.DocumentSnapshot> docs = snapshot.getDocuments();
        if (startIndex >= docs.size()) {
            onSuccess.run();
            return;
        }

        WriteBatch batch = db.batch();
        int end = Math.min(docs.size(), startIndex + BATCH_SIZE);
        for (int i = startIndex; i < end; i++) {
            com.google.firebase.firestore.DocumentSnapshot d = docs.get(i);
            // Ghi vào: backups/{backupId}/{collection}/{docId}
            DocumentReference dst = backupDoc.collection(col).document(d.getId());
            Map<String, Object> data = d.getData() != null ? new HashMap<>(d.getData()) : new HashMap<>();
            data.put("_backupAt", Timestamp.now());
            batch.set(dst, data);
        }

        batch.commit()
                .addOnSuccessListener(v -> writeCollectionSnapshot(backupDoc, col, snapshot, end, onSuccess, onFail))
                .addOnFailureListener(e -> onFail.run());
    }

    private CollectionReference backupsRef() {
        return db.collection("tenants").document(tenantId).collection("backups");
    }

    private CollectionReference scopedCollection(String collection) {
        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId != null && !tenantId.isEmpty()) {
            return db.collection("tenants").document(tenantId).collection(collection);
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
            throw new IllegalStateException("User not logged in");
        return db.collection("users").document(user.getUid()).collection(collection);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    static class BackupItem {
        String id;
        String note;
        Timestamp createdAt;
        String createdBy;
    }
}
