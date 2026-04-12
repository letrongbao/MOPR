package com.example.myapplication.features.report;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.EditText;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.constants.TenantRoles;
import com.example.myapplication.core.constants.TicketStatus;
import com.example.myapplication.core.repository.domain.TicketRepository;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.ScreenUiHelper;
import com.example.myapplication.domain.Ticket;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OwnerReportListActivity extends AppCompatActivity {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final TicketRepository repository = new TicketRepository();

    private final List<Ticket> allTickets = new ArrayList<>();
    private OwnerReportAdapter adapter;
    private TextView tvEmpty;
    private TabLayout tabStatuses;

    private String tenantId;
    private String selectedStatus = TicketStatus.OPEN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScreenUiHelper.enableEdgeToEdge(this, false);
        setContentView(R.layout.activity_owner_report_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        ScreenUiHelper.applyTopInset(toolbar);
        ScreenUiHelper.setupBackToolbar(this, toolbar, getString(R.string.report_management_title));

        RecyclerView rvReports = findViewById(R.id.rvReports);
        rvReports.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OwnerReportAdapter(this::showOwnerActionDialog);
        rvReports.setAdapter(adapter);

        tvEmpty = findViewById(R.id.tvEmpty);

        tabStatuses = findViewById(R.id.tabStatuses);
        tabStatuses.addTab(tabStatuses.newTab().setText(getString(R.string.report_tab_open)));
        tabStatuses.addTab(tabStatuses.newTab().setText(getString(R.string.report_tab_in_progress)));
        tabStatuses.addTab(tabStatuses.newTab().setText(getString(R.string.report_tab_done)));
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
                        Toast.makeText(this, getString(R.string.owner_report_no_permission), Toast.LENGTH_SHORT).show();
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
            if (TicketStatus.DONE.equals(selectedStatus)) {
                if (TicketStatus.DONE.equals(ticket.getStatus()) || TicketStatus.REJECTED.equals(ticket.getStatus())) {
                    filtered.add(ticket);
                }
                continue;
            }
            if (selectedStatus.equals(ticket.getStatus())) {
                filtered.add(ticket);
            }
        }
        adapter.submit(filtered);
        tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        updateTabCounts();
    }

    private void updateTabCounts() {
        if (tabStatuses == null || tabStatuses.getTabCount() < 3) {
            return;
        }
        int countOpen = 0;
        int countInProgress = 0;
        int countDone = 0;
        for (Ticket ticket : allTickets) {
            if (TicketStatus.OPEN.equals(ticket.getStatus())) {
                countOpen++;
            } else if (TicketStatus.IN_PROGRESS.equals(ticket.getStatus())) {
                countInProgress++;
            } else if (TicketStatus.DONE.equals(ticket.getStatus()) || TicketStatus.REJECTED.equals(ticket.getStatus())) {
                countDone++;
            }
        }
        TabLayout.Tab tab0 = tabStatuses.getTabAt(0);
        TabLayout.Tab tab1 = tabStatuses.getTabAt(1);
        TabLayout.Tab tab2 = tabStatuses.getTabAt(2);
        if (tab0 != null) tab0.setText(getString(R.string.report_tab_count, getString(R.string.report_tab_open), countOpen));
        if (tab1 != null) tab1.setText(getString(R.string.report_tab_count, getString(R.string.report_tab_in_progress), countInProgress));
        if (tab2 != null) tab2.setText(getString(R.string.report_tab_count, getString(R.string.report_tab_done), countDone));
    }

    private void showOwnerActionDialog(Ticket ticket) {
        String msg = "RoomId: " + (ticket.getRoomId() != null ? ticket.getRoomId() : "")
            + "\n" + getString(R.string.status_label) + " " + toVietnameseStatus(ticket.getStatus())
                + (TicketStatus.REJECTED.equals(ticket.getStatus()) && ticket.getRejectReason() != null
            ? "\n" + getString(R.string.report_reject_reason_prefix) + " " + ticket.getRejectReason() : "")
                + "\n\n" + (ticket.getDescription() != null ? ticket.getDescription() : "");

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(ticket.getTitle() != null ? ticket.getTitle() : getString(R.string.ticket_title))
                .setMessage(msg)
                .setPositiveButton(getString(R.string.close), null);

        if (TicketStatus.OPEN.equals(ticket.getStatus())) {
            builder.setNeutralButton(getString(R.string.report_action_start_processing), (d, w) -> updateTicketStatus(ticket, TicketStatus.IN_PROGRESS, null));
            builder.setNegativeButton(getString(R.string.report_action_reject), (d, w) -> showRejectDialog(ticket));
        } else if (TicketStatus.IN_PROGRESS.equals(ticket.getStatus())) {
            builder.setNeutralButton(getString(R.string.report_action_mark_done), (d, w) -> updateTicketStatus(ticket, TicketStatus.DONE, null));
        } else if (TicketStatus.REJECTED.equals(ticket.getStatus())) {
            builder.setNeutralButton(getString(R.string.report_action_reopen), (d, w) -> updateTicketStatus(ticket, TicketStatus.OPEN, null));
        }

        builder.show();
    }

    private void showRejectDialog(Ticket ticket) {
        EditText etReason = new EditText(this);
        etReason.setHint(getString(R.string.report_reject_reason_hint));
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        etReason.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.report_reject_title))
                .setView(etReason)
                .setNegativeButton(getString(R.string.cancel), null)
                .setPositiveButton(getString(R.string.confirm), (d, w) -> {
                    String reason = etReason.getText().toString().trim();
                    if (reason.isEmpty()) {
                        Toast.makeText(this, getString(R.string.report_reject_reason_required), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updateTicketStatus(ticket, TicketStatus.REJECTED, reason);
                })
                .show();
    }

    private void updateTicketStatus(Ticket ticket, String status, String rejectReason) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("rejectReason", rejectReason);

        if (currentUser != null) {
            updates.put("handledBy", currentUser.getUid());
        }

        com.google.firebase.Timestamp now = com.google.firebase.Timestamp.now();
        if (TicketStatus.IN_PROGRESS.equals(status)) {
            updates.put("processedAt", now);
            updates.put("doneAt", null);
            updates.put("rejectedAt", null);
        } else if (TicketStatus.DONE.equals(status)) {
            updates.put("doneAt", now);
        } else if (TicketStatus.REJECTED.equals(status)) {
            updates.put("rejectedAt", now);
            updates.put("doneAt", null);
        } else if (TicketStatus.OPEN.equals(status)) {
            updates.put("reopenedAt", now);
            updates.put("processedAt", null);
            updates.put("doneAt", null);
            updates.put("rejectedAt", null);
            updates.put("rejectReason", null);
        }

        repository.updateFields(ticket.getId(), updates,
                () -> runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.updated), Toast.LENGTH_SHORT).show();
                    sendTicketStatusNotification(ticket, status, rejectReason);
                }),
                () -> runOnUiThread(() -> Toast.makeText(this, getString(R.string.ticket_update_failed), Toast.LENGTH_SHORT).show()));
    }

    private void sendTicketStatusNotification(Ticket ticket, String newStatus, String rejectReason) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || tenantId == null || tenantId.trim().isEmpty()) {
            return;
        }
        String receiverUid = ticket.getCreatedBy();
        if (receiverUid == null || receiverUid.trim().isEmpty()) {
            return;
        }

        String title = getString(R.string.report_notification_title);
        String ticketTitle = ticket.getTitle() != null && !ticket.getTitle().trim().isEmpty()
                ? ticket.getTitle().trim()
                : getString(R.string.ticket_title);
        String statusText = toVietnameseStatus(newStatus);

        String body = getString(R.string.report_notification_body, ticketTitle, statusText);
        if (TicketStatus.REJECTED.equals(newStatus) && rejectReason != null && !rejectReason.trim().isEmpty()) {
            body = body + " " + getString(R.string.report_notification_reject_reason, rejectReason.trim());
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("title", title);
        payload.put("body", body);
        payload.put("type", "REPORT_STATUS");
        payload.put("ticketId", ticket.getId());
        payload.put("conversationId", null);
        payload.put("senderId", currentUser.getUid());
        payload.put("userId", receiverUid);
        payload.put("isRead", false);
        payload.put("createdAt", Timestamp.now());

        db.collection("tenants").document(tenantId)
                .collection("notifications")
                .add(payload);
    }

    private String toVietnameseStatus(String status) {
        if (TicketStatus.OPEN.equals(status)) return getString(R.string.report_tab_open);
        if (TicketStatus.IN_PROGRESS.equals(status)) return getString(R.string.report_tab_in_progress);
        if (TicketStatus.DONE.equals(status)) return getString(R.string.completed);
        if (TicketStatus.REJECTED.equals(status)) return getString(R.string.report_status_rejected);
        return status != null ? status : "";
    }
}
