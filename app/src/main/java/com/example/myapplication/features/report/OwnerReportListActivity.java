package com.example.myapplication.features.report;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.constants.TenantRoles;
import com.example.myapplication.core.constants.TicketStatus;
import com.example.myapplication.core.repository.domain.TicketRepository;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.domain.Ticket;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class OwnerReportListActivity extends AppCompatActivity {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final TicketRepository repository = new TicketRepository();

    private final List<Ticket> allTickets = new ArrayList<>();
    private ReportTicketAdapter adapter;
    private TextView tvEmpty;

    private String tenantId;
    private String selectedStatus = TicketStatus.OPEN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_report_list);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView rvReports = findViewById(R.id.rvReports);
        rvReports.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportTicketAdapter(this::showOwnerActionDialog);
        rvReports.setAdapter(adapter);

        tvEmpty = findViewById(R.id.tvEmpty);

        TabLayout tabStatuses = findViewById(R.id.tabStatuses);
        tabStatuses.addTab(tabStatuses.newTab().setText("Chưa làm"));
        tabStatuses.addTab(tabStatuses.newTab().setText("Đang làm"));
        tabStatuses.addTab(tabStatuses.newTab().setText("Đã xong"));
        tabStatuses.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab == null) return;
                int pos = tab.getPosition();
                if (pos == 0) {
                    selectedStatus = TicketStatus.OPEN;
                } else if (pos == 1) {
                    selectedStatus = TicketStatus.IN_PROGRESS;
                } else {
                    selectedStatus = TicketStatus.DONE;
                }
                renderFilteredList();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        setupOwnerContextAndObserve();
    }

    private void setupOwnerContextAndObserve() {
        tenantId = TenantSession.getActiveTenantId();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (tenantId == null || tenantId.trim().isEmpty() || user == null) {
            Toast.makeText(this, getString(R.string.no_active_tenant), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("tenants").document(tenantId)
                .collection("members").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String role = doc.getString("role");
                    if (!TenantRoles.OWNER.equals(role) && !TenantRoles.STAFF.equals(role)) {
                        Toast.makeText(this, "Bạn không có quyền quản lí phản ánh", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    observeAllTickets();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.error_load_data) + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void observeAllTickets() {
        repository.listAll().observe(this, tickets -> {
            allTickets.clear();
            if (tickets != null) {
                allTickets.addAll(tickets);
            }
            renderFilteredList();
        });
    }

    private void renderFilteredList() {
        List<Ticket> filtered = new ArrayList<>();
        for (Ticket ticket : allTickets) {
            if (selectedStatus.equals(ticket.getStatus())) {
                filtered.add(ticket);
            }
        }
        adapter.submit(filtered);
        tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showOwnerActionDialog(Ticket ticket) {
        String msg = "RoomId: " + (ticket.getRoomId() != null ? ticket.getRoomId() : "")
                + "\nTrạng thái: " + toVietnameseStatus(ticket.getStatus())
                + "\n\n" + (ticket.getDescription() != null ? ticket.getDescription() : "");

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(ticket.getTitle() != null ? ticket.getTitle() : getString(R.string.ticket_title))
                .setMessage(msg)
                .setPositiveButton(getString(R.string.close), null);

        if (!TicketStatus.DONE.equals(ticket.getStatus())) {
            builder.setNeutralButton("Chuyển trạng thái", (d, w) -> {
                String next = nextStatus(ticket.getStatus());
                repository.updateStatus(ticket.getId(), next,
                        () -> runOnUiThread(() -> Toast.makeText(this, getString(R.string.updated), Toast.LENGTH_SHORT).show()),
                        () -> runOnUiThread(() -> Toast.makeText(this, getString(R.string.ticket_update_failed), Toast.LENGTH_SHORT).show()));
            });
        }

        builder.show();
    }

    private String nextStatus(String current) {
        if (TicketStatus.OPEN.equals(current)) return TicketStatus.IN_PROGRESS;
        if (TicketStatus.IN_PROGRESS.equals(current)) return TicketStatus.DONE;
        return TicketStatus.OPEN;
    }

    private String toVietnameseStatus(String status) {
        if (TicketStatus.OPEN.equals(status)) return "Chưa làm";
        if (TicketStatus.IN_PROGRESS.equals(status)) return "Đang làm";
        if (TicketStatus.DONE.equals(status)) return getString(R.string.completed);
        return status != null ? status : "";
    }
}
