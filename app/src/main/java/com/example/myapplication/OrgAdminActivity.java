package com.example.myapplication;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrgAdminActivity extends AppCompatActivity {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String tenantId;

    private TextView tvTenantName, tvPlan, tvQuotas, tvUsage, tvBank;
    private android.widget.Button btnEditBank;
    private RecyclerView rvMembers, rvInvites;

    private OrgMemberAdapter memberAdapter;
    private OrgInviteAdapter inviteAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_org_admin);

        tvTenantName = findViewById(R.id.tvTenantName);
        tvPlan = findViewById(R.id.tvPlan);
        tvQuotas = findViewById(R.id.tvQuotas);
        tvUsage = findViewById(R.id.tvUsage);
        tvBank = findViewById(R.id.tvBank);
        btnEditBank = findViewById(R.id.btnEditBank);
        rvMembers = findViewById(R.id.rvMembers);
        rvInvites = findViewById(R.id.rvInvites);

        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        rvInvites.setLayoutManager(new LinearLayoutManager(this));

        memberAdapter = new OrgMemberAdapter();
        inviteAdapter = new OrgInviteAdapter(code -> revokeInvite(code));
        rvMembers.setAdapter(memberAdapter);
        rvInvites.setAdapter(inviteAdapter);

        TenantSession.init(this);
        tenantId = getIntent().getStringExtra("TENANT_ID");
        if (tenantId == null || tenantId.trim().isEmpty()) tenantId = TenantSession.getActiveTenantId();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || tenantId == null || tenantId.trim().isEmpty()) {
            Toast.makeText(this, "Thiếu tenant/user", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String uid = user.getUid();
        String finalTenantId = tenantId;

        // OWNER-only
        db.collection("tenants").document(finalTenantId)
                .collection("members").document(uid)
                .get()
                .addOnSuccessListener(mdoc -> {
                    String role = mdoc.getString("role");
                    if (!TenantRoles.OWNER.equals(role)) {
                        Toast.makeText(this, "Chỉ OWNER mới xem được", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    btnEditBank.setOnClickListener(v -> showEditBankDialog(finalTenantId));

                    loadTenantInfo(finalTenantId);
                    loadUsage(finalTenantId);
                    loadMembers(finalTenantId);
                    loadInvites(finalTenantId);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Không thể kiểm tra quyền", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void loadTenantInfo(@NonNull String tenantId) {
        db.collection("tenants").document(tenantId).get()
                .addOnSuccessListener(tdoc -> {
                    String name = tdoc.getString("name");
                    if (name == null || name.trim().isEmpty()) name = tenantId;
                    tvTenantName.setText(name);

                    String plan = tdoc.getString("plan");
                    if (plan == null) plan = "FREE";
                    tvPlan.setText("Plan: " + plan);

                    Long maxRooms = tdoc.getLong("maxRooms");
                    Long maxStaff = tdoc.getLong("maxStaff");
                    Long maxInv = tdoc.getLong("maxInvoicesPerMonth");
                    tvQuotas.setText("Quota: rooms=" + (maxRooms != null ? maxRooms : 50)
                            + ", staff=" + (maxStaff != null ? maxStaff : 3)
                            + ", invoices/month=" + (maxInv != null ? maxInv : 200));

                    String bankCode = tdoc.getString("bankCode");
                    String bankNo = tdoc.getString("bankAccountNo");
                    String bankName = tdoc.getString("bankAccountName");
                    String bankLine = (bankCode != null ? bankCode : "")
                            + (bankNo != null && !bankNo.isEmpty() ? (" - " + bankNo) : "")
                            + (bankName != null && !bankName.isEmpty() ? (" (" + bankName + ")") : "");
                    tvBank.setText("Bank: " + (bankLine.trim().isEmpty() ? "(chưa cấu hình)" : bankLine));
                });
    }

    private void showEditBankDialog(@NonNull String tenantId) {
        android.view.View v = getLayoutInflater().inflate(R.layout.dialog_edit_bank, null);
        android.widget.EditText etCode = v.findViewById(R.id.etBankCode);
        android.widget.EditText etNo = v.findViewById(R.id.etBankAccountNo);
        android.widget.EditText etName = v.findViewById(R.id.etBankAccountName);

        db.collection("tenants").document(tenantId).get().addOnSuccessListener(tdoc -> {
            etCode.setText(safe(tdoc.getString("bankCode")));
            etNo.setText(safe(tdoc.getString("bankAccountNo")));
            etName.setText(safe(tdoc.getString("bankAccountName")));

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Thông tin chuyển khoản")
                    .setView(v)
                    .setNegativeButton("Hủy", null)
                    .setPositiveButton("Lưu", (d, w) -> {
                        String code = safe(etCode.getText().toString()).trim().toUpperCase(Locale.US);
                        String no = safe(etNo.getText().toString()).trim();
                        String name = safe(etName.getText().toString()).trim();

                        java.util.Map<String, Object> update = new java.util.HashMap<>();
                        update.put("bankCode", code);
                        update.put("bankAccountNo", no);
                        update.put("bankAccountName", name);
                        update.put("updatedAt", com.google.firebase.Timestamp.now());

                        db.collection("tenants").document(tenantId)
                                .update(update)
                                .addOnSuccessListener(v2 -> {
                                    Toast.makeText(this, "Đã lưu", Toast.LENGTH_SHORT).show();
                                    loadTenantInfo(tenantId);
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Lưu thất bại", Toast.LENGTH_SHORT).show());
                    })
                    .show();
        });
    }

    private String safe(String s) {
        return s != null ? s : "";
    }

    private void loadUsage(@NonNull String tenantId) {
        String period = new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(new Date());

        db.collection("tenants").document(tenantId).collection("phong_tro").get()
                .addOnSuccessListener(rooms -> {
                    int roomCount = rooms != null ? rooms.size() : 0;

                    db.collection("tenants").document(tenantId).collection("members")
                            .whereEqualTo("role", TenantRoles.STAFF)
                            .get()
                            .addOnSuccessListener(staff -> {
                                int staffCount = staff != null ? staff.size() : 0;

                                db.collection("tenants").document(tenantId).collection("hoa_don")
                                        .whereEqualTo("thangNam", period)
                                        .get()
                                        .addOnSuccessListener(inv -> {
                                            int invCount = inv != null ? inv.size() : 0;
                                            tvUsage.setText("Usage: rooms=" + roomCount + ", staff=" + staffCount + ", invoices(" + period + ")=" + invCount);
                                        });
                            });
                });
    }

    private void loadMembers(@NonNull String tenantId) {
        db.collection("tenants").document(tenantId).collection("members")
                .get()
                .addOnSuccessListener(qs -> {
                    List<OrgMemberAdapter.MemberItem> items = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : qs) {
                        String uid = doc.getString("uid");
                        String role = doc.getString("role");
                        String roomId = doc.getString("roomId");
                        items.add(new OrgMemberAdapter.MemberItem(uid != null ? uid : doc.getId(), role, roomId));
                    }
                    memberAdapter.setItems(items);
                });
    }

    private void loadInvites(@NonNull String tenantId) {
        db.collection("tenants").document(tenantId).collection("invites")
                .get()
                .addOnSuccessListener(qs -> {
                    List<OrgInviteAdapter.InviteItem> items = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : qs) {
                        String code = doc.getString("code");
                        String email = doc.getString("email");
                        String role = doc.getString("role");
                        String status = doc.getString("status");
                        String roomId = doc.getString("roomId");
                        items.add(new OrgInviteAdapter.InviteItem(code != null ? code : doc.getId(), email, role, status, roomId));
                    }
                    inviteAdapter.setItems(items);
                });
    }

    private void revokeInvite(@NonNull String code) {
        String tid = tenantId != null ? tenantId : TenantSession.getActiveTenantId();
        if (tid == null || tid.trim().isEmpty()) return;

        db.collection("tenants").document(tid)
                .collection("invites").document(code)
                .update("status", "REVOKED")
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Đã thu hồi invite", Toast.LENGTH_SHORT).show();
                    loadInvites(tid);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Thu hồi thất bại", Toast.LENGTH_SHORT).show());
    }
}
