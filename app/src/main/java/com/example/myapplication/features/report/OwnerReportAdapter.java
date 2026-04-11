package com.example.myapplication.features.report;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class OwnerReportAdapter extends RecyclerView.Adapter<OwnerReportAdapter.OwnerReportViewHolder> {

    // ================================================================
    //  Interface callback → báo Activity reload
    // ================================================================
    public interface OnOwnerReportActionListener {
        void onStatusChanged();   // Khi confirm/process → Activity reload
    }

    // ================================================================
    //  Fields
    // ================================================================
    private final List<DocumentSnapshot> reportList;
    private final Context                context;
    private final OnOwnerReportActionListener listener;
    private final FirebaseFirestore      db;
    private final AlarmManager          alarmManager;

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    // Các trạng thái cho phép bấm nút hành động
    private static final String STATUS_PENDING     = "PENDING";
    private static final String STATUS_CONFIRMED   = "CONFIRMED";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_DONE        = "DONE";
    private static final String STATUS_CANCELLED   = "CANCELLED";

    // ================================================================
    //  Constructor
    // ================================================================
    public OwnerReportAdapter(List<DocumentSnapshot> reportList,
                              Context context,
                              OnOwnerReportActionListener listener) {
        this.reportList   = reportList;
        this.context      = context;
        this.listener     = listener;
        this.db           = FirebaseFirestore.getInstance();
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    // ================================================================
    //  RecyclerView overrides
    // ================================================================
    @NonNull
    @Override
    public OwnerReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_owner_report, parent, false);
        return new OwnerReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OwnerReportViewHolder holder, int position) {
        DocumentSnapshot doc = reportList.get(position);

        String docId          = doc.getId();
        String title          = doc.getString("title");
        String status         = doc.getString("status");
        String priority       = doc.getString("priority");
        String tenantId       = doc.getString("tenantId");
        String roomId         = doc.getString("roomId");
        Timestamp createdAt   = doc.getTimestamp("createdAt");
        Timestamp appointment = doc.getTimestamp("appointmentTime");

        // ── Title ─────────────────────────────────────────────────────
        holder.tvReportTitle.setText(title != null ? title : "Không có tiêu đề");

        // ── Tên khách ──────
        String createdByName = doc.getString("createdByName");
        holder.tvTenantName.setText(createdByName != null ? createdByName : "Khách thuê");

        // ── Số phòng ──────────────────────────────────────────────────
        holder.tvRoomNumber.setText(roomId != null ? "Phòng " + roomId : "Phòng —");

        // ── Ngày gửi ─────────────────────────────────────────────────
        holder.tvCreatedAt.setText(
                createdAt != null ? DATE_FORMAT.format(createdAt.toDate()) : "--/--/----");

        // ── Lịch hẹn ─────────────────────────────────────────────────
        if (appointment != null) {
            holder.rowAppointment.setVisibility(View.VISIBLE);
            holder.tvAppointment.setText(DATE_FORMAT.format(appointment.toDate()));
            // Cho phép nhấn vào ngày hẹn để đặt lại lịch (Xác nhận lại)
            holder.tvAppointment.setOnClickListener(v ->
                    showRescheduleDialog(docId, docId.hashCode(), appointment));
        } else {
            holder.rowAppointment.setVisibility(View.GONE);
        }

        // ── Lý do từ chối (hiện nếu REJECTED) ─────────────────────────
        String rejectReason = doc.getString("rejectReason");
        if ("REJECTED".equals(status) && rejectReason != null && !rejectReason.isEmpty()) {
            holder.rowRejectReason.setVisibility(View.VISIBLE);
            holder.tvRejectReason.setText(rejectReason);
        } else {
            holder.rowRejectReason.setVisibility(View.GONE);
        }

        // ── Priority badge ────────────────────────────────────────────
        holder.tvPriority.setText(priority != null ? priority : "Bình thường");
        applyPriorityColor(holder.tvPriority, priority);

        // ── Status + nút hành động ────────────────────────────────────
        applyStatusStyle(holder, status);

        // ── Nút "Xác nhận lịch" ───────────────────────────────────────
        holder.btnConfirmSchedule.setOnClickListener(v -> {
            if (appointment == null) {
                Toast.makeText(context, "Khách chưa chọn lịch hẹn!", Toast.LENGTH_SHORT).show();
                return;
            }
            confirmSchedule(docId, docId.hashCode(), appointment);
        });

        // ── Nút "Xử lý ngay" ─────────────────────────────────────────
        holder.btnProcessNow.setOnClickListener(v ->
                processNow(docId, docId.hashCode(), appointment != null));

        // ── Nút "Hoàn thành" (chỉ hiện khi IN_PROGRESS) ─────────────
        holder.btnComplete.setOnClickListener(v -> showCompleteConfirmDialog(docId));
    }

    @Override
    public int getItemCount() {
        return reportList != null ? reportList.size() : 0;
    }

    // ================================================================
    //  Xác nhận lịch hẹn → status = CONFIRMED + đặt Alarm
    // ================================================================
    private void confirmSchedule(String docId, int alarmId, Timestamp appointmentTime) {
        db.collection("issues").document(docId)
                .update("status", STATUS_CONFIRMED)
                .addOnSuccessListener(unused -> {
                    String timeStr = DATE_FORMAT.format(appointmentTime.toDate());
                    scheduleNotification(alarmId, appointmentTime.toDate().getTime(),
                            "Nhắc nhở sửa chữa", "Đã đến lịch hẹn sửa chữa!");
                    Toast.makeText(context,
                            "Đã xác nhận lịch hẹn vào lúc " + timeStr, Toast.LENGTH_SHORT).show();
                    Log.d("OwnerAdapter", "Xác nhận lịch: " + docId + " @ " + timeStr);
                    if (listener != null) listener.onStatusChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ================================================================
    //  Xử lý ngay → status = IN_PROGRESS + hủy Alarm nếu có
    // ================================================================
    private void processNow(String docId, int alarmId, boolean hasAlarm) {
        db.collection("issues").document(docId)
                .update("status", STATUS_IN_PROGRESS)
                .addOnSuccessListener(unused -> {
                    if (hasAlarm) cancelNotification(alarmId); // Hủy alarm nếu đã đặt trước
                    Toast.makeText(context, "Đang xử lý phản ánh!", Toast.LENGTH_SHORT).show();
                    Log.d("OwnerAdapter", "Xử lý ngay: " + docId);
                    if (listener != null) listener.onStatusChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ================================================================
    //  Dialog xác nhận lại lịch (Reschedule)
    // ================================================================
    private void showRescheduleDialog(String docId, int alarmId, Timestamp currentTime) {
        String currentStr = DATE_FORMAT.format(currentTime.toDate());
        new AlertDialog.Builder(context)
                .setTitle("Đổi lịch hẹn")
                .setMessage("Lịch hẹn hiện tại: " + currentStr + "\nBạn muốn xác nhận lại với lịch hiện tại không?")
                .setNegativeButton("Hủy", (d, w) -> d.dismiss())
                .setPositiveButton("Xác nhận lại", (d, w) -> {
                    d.dismiss();
                    cancelNotification(alarmId); // Hủy alarm cũ
                    confirmSchedule(docId, alarmId, currentTime); // Đặt alarm mới
                })
                .show();
    }

    // ================================================================
    //  Dialog nhập lý do từ chối
    // ================================================================
    private void showRejectDialog(String docId) {
        android.widget.EditText etReason = new android.widget.EditText(context);
        etReason.setHint("Nhập lý do từ chối...");
        etReason.setSingleLine(false);
        etReason.setMinLines(2);
        etReason.setMaxLines(4);
        int pad = (int)(16 * context.getResources().getDisplayMetrics().density);
        etReason.setPadding(pad, pad / 2, pad, pad / 2);

        new AlertDialog.Builder(context)
                .setTitle("Cần xác nhận lại")
                .setMessage("Nhập lý do để khách thuê biết và sửa lại phản ánh:")
                .setView(etReason)
                .setNegativeButton("Hủy", (d, w) -> d.dismiss())
                .setPositiveButton("Đồng ý", (d, w) -> {
                    String reason = etReason.getText().toString().trim();
                    if (reason.isEmpty()) {
                        Toast.makeText(context, "Vui lòng nhập lý do!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    d.dismiss();
                    rejectReport(docId, reason);
                })
                .show();
    }

    // ================================================================
    //  Cập nhật Firestore: status = REJECTED + rejectReason
    // ================================================================
    private void rejectReport(String docId, String reason) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("status", "REJECTED");
        updates.put("rejectReason", reason);

        db.collection("issues").document(docId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    Log.d("OwnerAdapter", "Từ chối phản ánh: " + docId);
                    Toast.makeText(context, "Đã gửi yêu cầu xác nhận lại!", Toast.LENGTH_SHORT).show();
                    if (listener != null) listener.onStatusChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("OwnerAdapter", "Lỗi từ chối: " + e.getMessage(), e);
                    Toast.makeText(context, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ================================================================
    //  Hoàn thành: confirmation dialog
    // ================================================================
    private void showCompleteConfirmDialog(String docId) {
        new AlertDialog.Builder(context)
                .setTitle("Xác nhận hoàn thành")
                .setMessage("Bạn đã xử lý xong sự cố này chưa?")
                .setNegativeButton("Chưa", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Đồng ý", (dialog, which) -> {
                    dialog.dismiss();
                    markAsDone(docId);
                })
                .setCancelable(true)
                .show();
    }

    // ================================================================
    //  Cập nhật Firestore: status = DONE + completedAt
    // ================================================================
    private void markAsDone(String docId) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("status", STATUS_DONE);
        updates.put("completedAt", com.google.firebase.Timestamp.now());

        db.collection("issues").document(docId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    Log.d("OwnerAdapter", "Đã hoàn thành sự cố: " + docId);
                    Toast.makeText(context, "Đã đánh dấu hoàn thành!", Toast.LENGTH_SHORT).show();
                    if (listener != null) listener.onStatusChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("OwnerAdapter", "Lỗi đánh dấu hoàn thành: " + e.getMessage(), e);
                    Toast.makeText(context, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ================================================================
    //  AlarmManager: Đặt lịch nhắc
    // ================================================================
    private void scheduleNotification(int alarmId, long triggerAtMillis,
                                      String title, String message) {
        Intent intent = new Intent(context, ReportAlarmReceiver.class);
        intent.putExtra("title",   title);
        intent.putExtra("message", message);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, alarmId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (alarmManager != null) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            Log.d("OwnerAdapter", "Alarm đặt lúc: " + DATE_FORMAT.format(triggerAtMillis));
        }
    }

    // ================================================================
    //  AlarmManager: Hủy lịch nhắc
    // ================================================================
    private void cancelNotification(int alarmId) {
        Intent intent = new Intent(context, ReportAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, alarmId, intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pendingIntent != null && alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            Log.d("OwnerAdapter", "Đã hủy alarm: " + alarmId);
        }
    }

    // ================================================================
    //  Màu Priority badge
    // ================================================================
    private void applyPriorityColor(TextView tv, String priority) {
        GradientDrawable bg = (GradientDrawable) tv.getBackground().mutate();
        if ("Cao".equalsIgnoreCase(priority)) {
            bg.setColor(Color.parseColor("#E74C3C"));
        } else if ("Thấp".equalsIgnoreCase(priority)) {
            bg.setColor(Color.parseColor("#3498DB"));
        } else {
            bg.setColor(Color.parseColor("#F39C12"));
        }
    }

    // ================================================================
    //  Status chip + hiển thị nút hành động
    // ================================================================
    private void applyStatusStyle(OwnerReportViewHolder holder, String status) {
        GradientDrawable bg = (GradientDrawable) holder.tvStatus.getBackground().mutate();
        // Mặc định ẩn cả 2 khối nút
        holder.layoutActionButtons.setVisibility(View.GONE);
        holder.layoutCompleteButton.setVisibility(View.GONE);

        switch (status != null ? status : "") {
            case STATUS_PENDING:
                holder.tvStatus.setText("Chưa làm");
                holder.tvStatus.setTextColor(Color.parseColor("#E67E22"));
                bg.setColor(Color.parseColor("#FDEBD0"));
                bg.setStroke(1, Color.parseColor("#E67E22"));
                holder.layoutActionButtons.setVisibility(View.VISIBLE);  // Hiện cả 2 nút PENDING
                break;

            case STATUS_CONFIRMED:
                holder.tvStatus.setText("Đã xác nhận lịch");
                holder.tvStatus.setTextColor(Color.parseColor("#2980B9"));
                bg.setColor(Color.parseColor("#D4E6F1"));
                bg.setStroke(1, Color.parseColor("#2980B9"));
                holder.layoutActionButtons.setVisibility(View.VISIBLE);  // Vẫn cho "Xử lý ngay"
                break;

            case STATUS_IN_PROGRESS:
                holder.tvStatus.setText("Đang xử lý");
                holder.tvStatus.setTextColor(Color.parseColor("#8E44AD"));
                bg.setColor(Color.parseColor("#E8DAEF"));
                bg.setStroke(1, Color.parseColor("#8E44AD"));
                holder.layoutCompleteButton.setVisibility(View.VISIBLE); // Chỉ hiện nút Hoàn thành
                break;

            case STATUS_DONE:
                holder.tvStatus.setText("Đã xong");
                holder.tvStatus.setTextColor(Color.parseColor("#27AE60"));
                bg.setColor(Color.parseColor("#D5F5E3"));
                bg.setStroke(1, Color.parseColor("#27AE60"));
                // Ẩn hết các nút (mặc định đã gone)
                break;

            case STATUS_CANCELLED:
                holder.tvStatus.setText("Đã hủy");
                holder.tvStatus.setTextColor(Color.parseColor("#7F8C8D"));
                bg.setColor(Color.parseColor("#F2F3F4"));
                bg.setStroke(1, Color.parseColor("#BDC3C7"));
                // Ẩn hết các nút (mặc định đã gone)
                break;

            case "REJECTED":
                holder.tvStatus.setText("Từ chối");
                holder.tvStatus.setTextColor(Color.parseColor("#C0392B"));
                bg.setColor(Color.parseColor("#FDEDEC"));
                bg.setStroke(1, Color.parseColor("#C0392B"));
                // Ẩn hết các nút (mặc định đã gone)
                break;

            default:
                holder.tvStatus.setText(status != null ? status : "—");
                holder.tvStatus.setTextColor(Color.parseColor("#7F8C8D"));
                bg.setColor(Color.parseColor("#F2F3F4"));
                bg.setStroke(1, Color.parseColor("#BDC3C7"));
        }
    }

    // ================================================================
    //  ViewHolder
    // ================================================================
    public static class OwnerReportViewHolder extends RecyclerView.ViewHolder {
        TextView     tvReportTitle, tvPriority, tvTenantName, tvRoomNumber;
        TextView     tvCreatedAt, tvAppointment, tvStatus, tvRejectReason;
        TextView     btnConfirmSchedule, btnProcessNow, btnComplete, btnReject;
        LinearLayout rowAppointment, layoutActionButtons, layoutCompleteButton, rowRejectReason;

        public OwnerReportViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReportTitle       = itemView.findViewById(R.id.tvReportTitle);
            tvPriority          = itemView.findViewById(R.id.tvPriority);
            tvTenantName        = itemView.findViewById(R.id.tvTenantName);
            tvRoomNumber        = itemView.findViewById(R.id.tvRoomNumber);
            tvCreatedAt         = itemView.findViewById(R.id.tvCreatedAt);
            tvAppointment       = itemView.findViewById(R.id.tvAppointment);
            tvStatus            = itemView.findViewById(R.id.tvStatus);
            tvRejectReason      = itemView.findViewById(R.id.tvRejectReason);
            btnConfirmSchedule  = itemView.findViewById(R.id.btnConfirmSchedule);
            btnProcessNow       = itemView.findViewById(R.id.btnProcessNow);
            btnComplete         = itemView.findViewById(R.id.btnComplete);
            btnReject           = itemView.findViewById(R.id.btnReject);
            rowAppointment      = itemView.findViewById(R.id.rowAppointment);
            rowRejectReason     = itemView.findViewById(R.id.rowRejectReason);
            layoutActionButtons = itemView.findViewById(R.id.layoutActionButtons);
            layoutCompleteButton = itemView.findViewById(R.id.layoutCompleteButton);
        }
    }
}
