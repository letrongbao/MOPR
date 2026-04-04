package com.example.myapplication.features.ticket;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.constants.TenantRoles;
import com.example.myapplication.core.util.ScreenUiHelper;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.constants.TicketStatus;
import com.example.myapplication.domain.Ticket;
import com.example.myapplication.core.repository.domain.TicketRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TicketActivity extends AppCompatActivity {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final TicketRepository repository = new TicketRepository();

    private TicketAdapter adapter;
    private TextView tvEmpty;
    private FloatingActionButton fabAdd;

    private boolean isTenant;
    private String currentRoomId;
    private String tenantId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScreenUiHelper.enableEdgeToEdge(this, false);

        setContentView(R.layout.activity_tickets);

        Toolbar toolbar = findViewById(R.id.toolbar);
        ScreenUiHelper.applyTopInset(toolbar);
        ScreenUiHelper.setupBackToolbar(this, toolbar, getString(R.string.complaints_repairs));

        tvEmpty = findViewById(R.id.tvEmpty);
        fabAdd = findViewById(R.id.fabAdd);
        RecyclerView rv = findViewById(R.id.recyclerView);

        adapter = new TicketAdapter(this::showTicketDetailDialog);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> showCreateTicketDialog());

        tenantId = TenantSession.getActiveTenantId();
        setupData();
    }

    private void setupData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, getString(R.string.not_logged_in), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (tenantId == null || tenantId.trim().isEmpty()) {
            observeTickets(repository.listAll());
            return;
        }

        db.collection("tenants").document(tenantId)
                .collection("members").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String role = doc.getString("role");
                    isTenant = TenantRoles.TENANT.equals(role);
                    currentRoomId = doc.getString("roomId");

                    if (isTenant) {
                        if (currentRoomId == null || currentRoomId.trim().isEmpty()) {
                            Toast.makeText(this, getString(R.string.missing_room_id_for_tenant), Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        observeTickets(repository.listByRoom(currentRoomId));
                        return;
                    }

                    observeTickets(repository.listAll());
                })
                .addOnFailureListener(e -> observeTickets(repository.listAll()));
    }

    private void observeTickets(@NonNull androidx.lifecycle.MutableLiveData<List<Ticket>> liveData) {
        liveData.observe(this, list -> {
            adapter.setList(list);
            tvEmpty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void showCreateTicketDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
            return;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_ticket, null);
        EditText etTitle = dialogView.findViewById(R.id.etTitle);
        EditText etDesc = dialogView.findViewById(R.id.etDescription);
        Spinner spinnerRoom = dialogView.findViewById(R.id.spinnerRoom);
        EditText etRoomId = dialogView.findViewById(R.id.etRoomId);

        // Tenant: fixed room, hide selectors
        if (isTenant) {
            spinnerRoom.setVisibility(View.GONE);
            etRoomId.setVisibility(View.GONE);
        } else {
            // Owner/Staff: prefer spinner rooms when tenant scope exists; fallback to
            // manual roomId
            if (tenantId == null || tenantId.trim().isEmpty()) {
                spinnerRoom.setVisibility(View.GONE);
                etRoomId.setVisibility(View.VISIBLE);
                etRoomId.setHint(R.string.dialog_create_ticket_room_id_hint);
                etRoomId.setInputType(InputType.TYPE_CLASS_TEXT);
            } else {
                etRoomId.setVisibility(View.GONE);
                etRoomId.setText("");
                loadRoomsIntoSpinner(spinnerRoom);
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.ticket_create_title))
                .setView(dialogView)
                .setNegativeButton(getString(R.string.cancel), null)
                .setPositiveButton(getString(R.string.send), (d, w) -> {
                    String title = etTitle.getText().toString().trim();
                    String desc = etDesc.getText().toString().trim();

                    if (title.isEmpty()) {
                        Toast.makeText(this, getString(R.string.please_enter_title), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String roomId;
                    if (isTenant) {
                        roomId = currentRoomId;
                    } else if (tenantId == null || tenantId.trim().isEmpty()) {
                        roomId = etRoomId.getText().toString().trim();
                    } else {
                        Object selected = spinnerRoom.getSelectedItem();
                        roomId = selected != null ? selected.toString() : "";
                    }

                    if (roomId == null || roomId.trim().isEmpty()) {
                        Toast.makeText(this, getString(R.string.ticket_missing_room_id), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Ticket t = new Ticket();
                    t.setRoomId(roomId);
                    t.setTitle(title);
                    t.setDescription(desc);
                    t.setStatus(TicketStatus.OPEN);
                    t.setCreatedBy(user.getUid());
                    t.setCreatedAt(Timestamp.now());

                    repository.add(t,
                            () -> runOnUiThread(
                                    () -> Toast.makeText(this, getString(R.string.ticket_sent), Toast.LENGTH_SHORT).show()),
                            () -> runOnUiThread(() -> Toast.makeText(this, getString(R.string.ticket_send_failed), Toast.LENGTH_SHORT).show()));
                })
                .show();
    }

    private void loadRoomsIntoSpinner(@NonNull Spinner spinnerRoom) {
        if (tenantId == null || tenantId.trim().isEmpty())
            return;

        List<String> roomIds = new ArrayList<>();
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, roomIds);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRoom.setAdapter(adapter);

        db.collection("tenants").document(tenantId).collection("rooms")
                .get()
                .addOnSuccessListener(qs -> {
                    roomIds.clear();
                    for (QueryDocumentSnapshot doc : qs) {
                        roomIds.add(doc.getId());
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void showTicketDetailDialog(@NonNull Ticket t) {
        String msg = "RoomId: " + (t.getRoomId() != null ? t.getRoomId() : "") +
                "\n" + getString(R.string.status_label) + " " + toVietnameseStatus(t.getStatus()) +
                "\n\n" + (t.getDescription() != null ? t.getDescription() : "");

        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle(t.getTitle() != null ? t.getTitle() : getString(R.string.ticket_title))
                .setMessage(msg)
                .setPositiveButton(getString(R.string.close), null);

        if (!isTenant) {
            b.setNeutralButton(getString(R.string.ticket_change_status), (d, w) -> {
                String next = nextStatus(t.getStatus());
                repository.updateStatus(t.getId(), next,
                        () -> runOnUiThread(() -> Toast.makeText(this, getString(R.string.updated), Toast.LENGTH_SHORT).show()),
                        () -> runOnUiThread(
                                () -> Toast.makeText(this, getString(R.string.ticket_update_failed), Toast.LENGTH_SHORT).show()));
            });
            b.setNegativeButton(getString(R.string.delete), (d, w) -> repository.delete(t.getId(),
                    () -> runOnUiThread(() -> Toast.makeText(this, getString(R.string.deleted), Toast.LENGTH_SHORT).show()),
                    () -> runOnUiThread(() -> Toast.makeText(this, getString(R.string.delete_failed), Toast.LENGTH_SHORT).show())));
        }

        b.show();
    }

    private String nextStatus(String current) {
        if (TicketStatus.OPEN.equals(current))
            return TicketStatus.IN_PROGRESS;
        if (TicketStatus.IN_PROGRESS.equals(current))
            return TicketStatus.DONE;
        return TicketStatus.OPEN;
    }

    private String toVietnameseStatus(String status) {
        if (TicketStatus.OPEN.equals(status))
            return getString(R.string.ticket_status_new);
        if (TicketStatus.IN_PROGRESS.equals(status))
            return getString(R.string.ticket_status_processing);
        if (TicketStatus.DONE.equals(status))
            return getString(R.string.completed);
        return status != null ? status : "";
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
