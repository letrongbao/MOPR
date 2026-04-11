package com.example.myapplication.features.report;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TenantReportListActivity extends AppCompatActivity {

    public static final String EXTRA_OPEN_TICKET_ID = "OPEN_TICKET_ID";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final TicketRepository repository = new TicketRepository();

    private final List<Ticket> allTickets = new ArrayList<>();
    private ReportTicketAdapter adapter;
    private TextView tvEmpty;
    private TabLayout tabStatuses;

    private String tenantId;
    private String roomId;
    private String currentUserId;
    private String pendingOpenTicketId;
    private String selectedStatus = TicketStatus.OPEN;

    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScreenUiHelper.enableEdgeToEdge(this, false);
        setContentView(R.layout.activity_tenant_report_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        ScreenUiHelper.applyTopInset(toolbar);
        ScreenUiHelper.setupBackToolbar(this, toolbar, getString(R.string.tenant_report_title));

        RecyclerView rvReports = findViewById(R.id.rvReports);
        rvReports.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportTicketAdapter(this::showTicketDetails);
        rvReports.setAdapter(adapter);

        tvEmpty = findViewById(R.id.tvEmpty);

        tabStatuses = findViewById(R.id.tabStatuses);
        tabStatuses.addTab(tabStatuses.newTab().setText(getString(R.string.report_tab_open)));
        tabStatuses.addTab(tabStatuses.newTab().setText(getString(R.string.report_tab_in_progress)));
        tabStatuses.addTab(tabStatuses.newTab().setText(getString(R.string.report_tab_done)));
        tabStatuses.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab == null) {
                    return;
                }
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

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> showCreateReportDialog());

        pendingOpenTicketId = getIntent().getStringExtra(EXTRA_OPEN_TICKET_ID);

        setupTenantContextAndObserve();
    }

    private void setupTenantContextAndObserve() {
        tenantId = TenantSession.getActiveTenantId();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (tenantId == null || tenantId.trim().isEmpty() || user == null) {
            Toast.makeText(this, getString(R.string.no_active_tenant), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = user.getUid();

        db.collection("tenants").document(tenantId)
                .collection("members").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String role = doc.getString("role");
                    roomId = doc.getString("roomId");
                    if (!TenantRoles.TENANT.equals(role) || roomId == null || roomId.trim().isEmpty()) {
                        Toast.makeText(this, getString(R.string.missing_room_id_for_tenant), Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    observeTicketsByRoom(roomId);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.error_load_data) + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void observeTicketsByRoom(String roomId) {
        repository.listByRoom(roomId).observe(this, tickets -> {
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
            if (currentUserId != null && ticket.getCreatedBy() != null && !currentUserId.equals(ticket.getCreatedBy())) {
                continue;
            }
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
        openPendingTicketIfNeeded(filtered);
    }

    private void openPendingTicketIfNeeded(List<Ticket> filtered) {
        if (pendingOpenTicketId == null || pendingOpenTicketId.trim().isEmpty()) {
            return;
        }

        Ticket target = null;
        for (Ticket ticket : allTickets) {
            if (pendingOpenTicketId.equals(ticket.getId())) {
                target = ticket;
                break;
            }
        }
        if (target == null) {
            return;
        }

        String targetStatus = target.getStatus();
        if (targetStatus == null) {
            targetStatus = TicketStatus.OPEN;
        }

        if (!matchesSelectedStatus(targetStatus)) {
            selectedStatus = TicketStatus.DONE.equals(targetStatus) || TicketStatus.REJECTED.equals(targetStatus)
                    ? TicketStatus.DONE
                    : targetStatus;
            selectTabForStatus(selectedStatus);
            return;
        }

        for (Ticket ticket : filtered) {
            if (pendingOpenTicketId.equals(ticket.getId())) {
                pendingOpenTicketId = null;
                showTicketDetails(ticket);
                return;
            }
        }
    }

    private boolean matchesSelectedStatus(String status) {
        if (TicketStatus.DONE.equals(selectedStatus)) {
            return TicketStatus.DONE.equals(status) || TicketStatus.REJECTED.equals(status);
        }
        return selectedStatus.equals(status);
    }

    private void selectTabForStatus(String status) {
        if (tabStatuses == null) {
            return;
        }
        int index = 0;
        if (TicketStatus.IN_PROGRESS.equals(status)) {
            index = 1;
        } else if (TicketStatus.DONE.equals(status) || TicketStatus.REJECTED.equals(status)) {
            index = 2;
        }
        TabLayout.Tab tab = tabStatuses.getTabAt(index);
        if (tab != null) {
            tab.select();
        }
    }

    private void updateTabCounts() {
        if (tabStatuses == null || tabStatuses.getTabCount() < 3) {
            return;
        }
        int countOpen = 0;
        int countInProgress = 0;
        int countDone = 0;
        for (Ticket ticket : allTickets) {
            if (currentUserId != null && ticket.getCreatedBy() != null && !currentUserId.equals(ticket.getCreatedBy())) {
                continue;
            }
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

    private void showCreateReportDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || roomId == null || roomId.trim().isEmpty()) {
            Toast.makeText(this, getString(R.string.missing_room_id_for_tenant), Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_ticket, null);
        EditText etTitle = dialogView.findViewById(R.id.etTitle);
        EditText etDesc = dialogView.findViewById(R.id.etDescription);
        View spinnerRoom = dialogView.findViewById(R.id.spinnerRoom);
        View etRoomId = dialogView.findViewById(R.id.etRoomId);
        spinnerRoom.setVisibility(View.GONE);
        etRoomId.setVisibility(View.GONE);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.tenant_report_create_title))
                .setView(dialogView)
                .setNegativeButton(getString(R.string.cancel), null)
                .setPositiveButton(getString(R.string.send), (d, w) -> {
                    String title = etTitle.getText().toString().trim();
                    String desc = etDesc.getText().toString().trim();
                    if (title.isEmpty()) {
                        Toast.makeText(this, getString(R.string.please_enter_title), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Ticket ticket = new Ticket();
                    ticket.setRoomId(roomId);
                    ticket.setTitle(title);
                    ticket.setDescription(desc);
                    ticket.setStatus(TicketStatus.OPEN);
                    ticket.setCreatedBy(user.getUid());
                    ticket.setCreatedAt(Timestamp.now());

                    repository.add(ticket,
                            () -> runOnUiThread(() -> Toast.makeText(this, getString(R.string.ticket_sent), Toast.LENGTH_SHORT).show()),
                            () -> runOnUiThread(() -> Toast.makeText(this, getString(R.string.ticket_send_failed), Toast.LENGTH_SHORT).show()));
                })
                .show();
    }

    private void showTicketDetails(Ticket ticket) {
        String msg = "RoomId: " + (ticket.getRoomId() != null ? ticket.getRoomId() : "")
            + "\n" + getString(R.string.status_label) + " " + toVietnameseStatus(ticket.getStatus())
                + (TicketStatus.REJECTED.equals(ticket.getStatus()) && ticket.getRejectReason() != null
            ? "\n" + getString(R.string.report_reject_reason_prefix) + " " + ticket.getRejectReason() : "")
                + buildTimelineText(ticket)
                + "\n\n" + (ticket.getDescription() != null ? ticket.getDescription() : "");

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(ticket.getTitle() != null ? ticket.getTitle() : getString(R.string.ticket_title))
                .setMessage(msg)
                .setPositiveButton(getString(R.string.close), null);

        if (TicketStatus.REJECTED.equals(ticket.getStatus())) {
            builder.setNeutralButton(getString(R.string.report_action_resubmit), (d, w) -> showResubmitDialog(ticket));
        }

        builder.show();
    }

    private void showResubmitDialog(Ticket ticket) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_ticket, null);
        EditText etTitle = dialogView.findViewById(R.id.etTitle);
        EditText etDesc = dialogView.findViewById(R.id.etDescription);
        View spinnerRoom = dialogView.findViewById(R.id.spinnerRoom);
        View etRoomId = dialogView.findViewById(R.id.etRoomId);
        spinnerRoom.setVisibility(View.GONE);
        etRoomId.setVisibility(View.GONE);

        etTitle.setText(ticket.getTitle());
        etDesc.setText(ticket.getDescription());

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.report_resubmit_title))
                .setView(dialogView)
                .setNegativeButton(getString(R.string.cancel), null)
                .setPositiveButton(getString(R.string.update), (d, w) -> {
                    String title = etTitle.getText().toString().trim();
                    String desc = etDesc.getText().toString().trim();
                    if (title.isEmpty()) {
                        Toast.makeText(this, getString(R.string.please_enter_title), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("title", title);
                    updates.put("description", desc);
                    updates.put("status", TicketStatus.OPEN);
                    updates.put("rejectReason", null);
                    repository.updateFields(ticket.getId(), updates,
                            () -> runOnUiThread(() -> Toast.makeText(this, getString(R.string.report_resubmit_success), Toast.LENGTH_SHORT).show()),
                            () -> runOnUiThread(() -> Toast.makeText(this, getString(R.string.ticket_update_failed), Toast.LENGTH_SHORT).show()));
                })
                .show();
    }

    private String toVietnameseStatus(String status) {
        if (TicketStatus.OPEN.equals(status)) return getString(R.string.report_tab_open);
        if (TicketStatus.IN_PROGRESS.equals(status)) return getString(R.string.report_tab_in_progress);
        if (TicketStatus.DONE.equals(status)) return getString(R.string.completed);
        if (TicketStatus.REJECTED.equals(status)) return getString(R.string.report_status_rejected);
        return status != null ? status : "";
    }

    private String buildTimelineText(Ticket ticket) {
        StringBuilder sb = new StringBuilder();
        if (ticket.getProcessedAt() != null) {
            sb.append("\n").append(getString(R.string.report_timeline_processed, DATE_TIME_FORMAT.format(ticket.getProcessedAt().toDate())));
        }
        if (ticket.getRejectedAt() != null) {
            sb.append("\n").append(getString(R.string.report_timeline_rejected, DATE_TIME_FORMAT.format(ticket.getRejectedAt().toDate())));
        }
        if (ticket.getDoneAt() != null) {
            sb.append("\n").append(getString(R.string.report_timeline_done, DATE_TIME_FORMAT.format(ticket.getDoneAt().toDate())));
        }
        if (ticket.getReopenedAt() != null) {
            sb.append("\n").append(getString(R.string.report_timeline_reopened, DATE_TIME_FORMAT.format(ticket.getReopenedAt().toDate())));
        }
        return sb.toString();
    }
}
