package com.example.myapplication.features.report;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.myapplication.R;
import com.example.myapplication.core.constants.TenantRoles;
import com.example.myapplication.core.constants.TicketStatus;
import com.example.myapplication.core.repository.domain.TicketRepository;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.core.util.ScreenUiHelper;
import com.example.myapplication.domain.Ticket;
import com.google.android.material.appbar.AppBarLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class TenantCreateReportActivity extends AppCompatActivity {

    public static final String EXTRA_ROOM_ID = "ROOM_ID";
    public static final String EXTRA_TENANT_ID = "TENANT_ID";

    private final TicketRepository repository = new TicketRepository();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final SimpleDateFormat DATE_TIME_FORMAT =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    private EditText etTitle;
    private EditText etDescription;
    private TextView tvAppointment;

    private String roomId;
    private String tenantId;
    private Calendar appointmentCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScreenUiHelper.enableEdgeToEdge(this, false);
        setContentView(R.layout.activity_tenant_create_report);

        roomId = getIntent().getStringExtra(EXTRA_ROOM_ID);
        tenantId = getIntent().getStringExtra(EXTRA_TENANT_ID);
        if (tenantId == null || tenantId.trim().isEmpty()) {
            tenantId = TenantSession.getActiveTenantId();
        }

        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            ScreenUiHelper.applyTopInset(appBarLayout);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        ScreenUiHelper.setupBackToolbar(this, toolbar, getString(R.string.tenant_report_create_title));

        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        tvAppointment = findViewById(R.id.tvAppointmentValue);

        findViewById(R.id.layoutAppointment).setOnClickListener(v -> openDateTimePicker());

        Button btnCancel = findViewById(R.id.btnCancel);
        Button btnSubmit = findViewById(R.id.btnSubmit);
        btnCancel.setOnClickListener(v -> finish());
        btnSubmit.setOnClickListener(v -> submit());
    }

    private void openDateTimePicker() {
        Calendar now = Calendar.getInstance();
        final Calendar selected = appointmentCalendar != null ? appointmentCalendar : now;

        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    TimePickerDialog timePicker = new TimePickerDialog(
                            this,
                            (timeView, hourOfDay, minute) -> {
                                appointmentCalendar = Calendar.getInstance();
                                appointmentCalendar.set(year, month, dayOfMonth, hourOfDay, minute, 0);
                                appointmentCalendar.set(Calendar.MILLISECOND, 0);
                                tvAppointment.setText(DATE_TIME_FORMAT.format(appointmentCalendar.getTime()));
                            },
                            selected.get(Calendar.HOUR_OF_DAY),
                            selected.get(Calendar.MINUTE),
                            true);
                    timePicker.show();
                },
                selected.get(Calendar.YEAR),
                selected.get(Calendar.MONTH),
                selected.get(Calendar.DAY_OF_MONTH));

        datePicker.getDatePicker().setMinDate(now.getTimeInMillis() - 1000);
        datePicker.show();
    }

    private void submit() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, getString(R.string.error_not_logged_in), Toast.LENGTH_SHORT).show();
            return;
        }
        if (roomId == null || roomId.trim().isEmpty()) {
            Toast.makeText(this, getString(R.string.missing_room_id_for_tenant), Toast.LENGTH_SHORT).show();
            return;
        }

        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";

        if (title.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_enter_title), Toast.LENGTH_SHORT).show();
            return;
        }

        if (appointmentCalendar != null) {
            String appointmentLine = getString(
                    R.string.report_appointment_prefix,
                    DATE_TIME_FORMAT.format(appointmentCalendar.getTime()));
            if (description.isEmpty()) {
                description = appointmentLine;
            } else {
                description = description + "\n" + appointmentLine;
            }
        }

        Ticket ticket = new Ticket();
        ticket.setRoomId(roomId);
        ticket.setTitle(title);
        ticket.setDescription(description);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCreatedBy(user.getUid());
        ticket.setCreatedAt(Timestamp.now());
        if (appointmentCalendar != null) {
            ticket.setAppointmentTime(new Timestamp(appointmentCalendar.getTime()));
        }

        repository.add(ticket,
                () -> runOnUiThread(() -> {
                    pushOwnerReportCreatedNotifications(ticket, title);
                    if (appointmentCalendar != null) {
                        long triggerAt = appointmentCalendar.getTimeInMillis();
                        if (triggerAt > System.currentTimeMillis()) {
                            String alarmTitle = getString(R.string.report_alarm_default_title);
                            String alarmMessage = getString(R.string.report_alarm_message_with_title, title);
                            ReportReminderScheduler.scheduleReminder(this, triggerAt, alarmTitle, alarmMessage);
                        }
                    }
                    Toast.makeText(this, getString(R.string.ticket_sent), Toast.LENGTH_SHORT).show();
                    finish();
                }),
                () -> runOnUiThread(() -> Toast.makeText(this, getString(R.string.ticket_send_failed), Toast.LENGTH_SHORT).show()));
    }

    private void pushOwnerReportCreatedNotifications(Ticket ticket, String reportTitle) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || tenantId == null || tenantId.trim().isEmpty() || ticket == null) {
            return;
        }

        String safeTitle = (reportTitle != null && !reportTitle.trim().isEmpty())
                ? reportTitle.trim()
                : getString(R.string.ticket_title);
        String roomId = ticket.getRoomId() != null ? ticket.getRoomId().trim() : "";
        if (roomId.isEmpty()) {
            pushOwnerReportCreatedNotificationsWithLocation(currentUser.getUid(), ticket.getId(), safeTitle,
                getString(R.string.report_unknown_house),
                getString(R.string.report_unknown_room));
            return;
        }

        db.collection("tenants").document(tenantId)
            .collection("rooms").document(roomId)
            .get()
            .addOnSuccessListener(roomDoc -> {
                String roomNumber = roomDoc.getString("roomNumber");
                String houseName = roomDoc.getString("houseName");

                String roomLabel = (roomNumber != null && !roomNumber.trim().isEmpty())
                    ? getString(R.string.room_number, roomNumber.trim())
                    : getString(R.string.room_number, roomId);
                String houseLabel = (houseName != null && !houseName.trim().isEmpty())
                    ? houseName.trim()
                    : getString(R.string.report_unknown_house);

                pushOwnerReportCreatedNotificationsWithLocation(currentUser.getUid(), ticket.getId(), safeTitle, houseLabel, roomLabel);
            })
            .addOnFailureListener(e -> pushOwnerReportCreatedNotificationsWithLocation(
                currentUser.getUid(),
                ticket.getId(),
                safeTitle,
                getString(R.string.report_unknown_house),
                getString(R.string.room_number, roomId)));
        }

        private void pushOwnerReportCreatedNotificationsWithLocation(
            String senderUid,
            String ticketId,
            String reportTitle,
            String houseLabel,
            String roomLabel) {
        String locationText = getString(R.string.report_house_line, houseLabel)
            + " - " + getString(R.string.report_room_line, roomLabel);

        db.collection("tenants").document(tenantId)
            .collection("members")
            .whereIn("role", Arrays.asList(TenantRoles.OWNER, TenantRoles.STAFF))
            .get()
            .addOnSuccessListener(members -> {
                com.google.firebase.Timestamp now = com.google.firebase.Timestamp.now();
                for (com.google.firebase.firestore.DocumentSnapshot member : members.getDocuments()) {
                String receiverUid = member.getId();
                if (receiverUid == null || receiverUid.trim().isEmpty() || receiverUid.equals(senderUid)) {
                    continue;
                }

                Map<String, Object> payload = new HashMap<>();
                payload.put("title", getString(R.string.report_created_notification_title));
                payload.put("body", getString(R.string.report_created_notification_body, reportTitle, locationText));
                payload.put("type", "REPORT_CREATED");
                payload.put("ticketId", ticketId);
                payload.put("conversationId", null);
                payload.put("senderId", senderUid);
                payload.put("userId", receiverUid);
                payload.put("isRead", false);
                payload.put("createdAt", now);

                db.collection("tenants").document(tenantId)
                    .collection("notifications")
                    .document(UUID.randomUUID().toString())
                    .set(payload, SetOptions.merge());
                }
            });
    }
}
