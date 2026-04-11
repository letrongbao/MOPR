package com.example.myapplication.features.report;

import android.app.AlertDialog;
import android.content.Context;
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

public class TenantReportAdapter extends RecyclerView.Adapter<TenantReportAdapter.ReportViewHolder> {

    // ================================================================
    //  Interface callback để báo lên Activity reload danh sách
    // ================================================================
    public interface OnReportActionListener {
        void onReportCancelled(); // Gọi khi hủy thành công → Activity tải lại
    }

    // ================================================================
    //  Fields
    // ================================================================
    private final List<DocumentSnapshot> reportList;
    private final Context context;
    private final OnReportActionListener listener;
    private final FirebaseFirestore db;

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    // ================================================================
    //  Constructor
    // ================================================================
    public TenantReportAdapter(List<DocumentSnapshot> reportList,
                               Context context,
                               OnReportActionListener listener) {
        this.reportList = reportList;
        this.context    = context;
        this.listener   = listener;
        this.db         = FirebaseFirestore.getInstance();
    }

    // ================================================================
    //  RecyclerView overrides
    // ================================================================
    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_tenant_report, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        DocumentSnapshot doc = reportList.get(position);

        // ── Đọc các trường từ Firestore ──────────────────────────────
        String title           = doc.getString("title");
        String status          = doc.getString("status");
        String priority        = doc.getString("priority");
        Timestamp createdAt    = doc.getTimestamp("createdAt");
        Timestamp appointment  = doc.getTimestamp("appointmentTime");

        // ── Title ────────────────────────────────────────────────────
        holder.tvReportTitle.setText(title != null ? title : "Không có tiêu đề");

        // ── Ngày gửi ─────────────────────────────────────────────────
        holder.tvCreatedAt.setText(
                createdAt != null ? DATE_FORMAT.format(createdAt.toDate()) : "--/--/----");

        // ── Lịch hẹn (chỉ hiện khi có giá trị) ──────────────────────
        if (appointment != null) {
            holder.rowAppointment.setVisibility(View.VISIBLE);
            holder.tvAppointment.setText(DATE_FORMAT.format(appointment.toDate()));
        } else {
            holder.rowAppointment.setVisibility(View.GONE);
        }

        // ── Priority badge ────────────────────────────────────────────
        holder.tvPriority.setText(priority != null ? priority : "Bình thường");
        applyPriorityColor(holder.tvPriority, priority);

        // ── Status chip + nút Hủy bỏ ─────────────────────────────────
        applyStatusStyle(holder, status);

        // ── Nút Liên hệ (placeholder, mở rộng sau) ───────────────────
        holder.btnContact.setOnClickListener(v ->
                Toast.makeText(context, "Tính năng liên hệ đang phát triển!", Toast.LENGTH_SHORT).show());

        // ── Nút Hủy bỏ ───────────────────────────────────────────────
        holder.btnCancel.setOnClickListener(v ->
                showCancelConfirmDialog(doc.getId()));
    }

    @Override
    public int getItemCount() {
        return reportList != null ? reportList.size() : 0;
    }

    // ================================================================
    //  Hiển thị màu Priority
    // ================================================================
    private void applyPriorityColor(TextView tvPriority, String priority) {
        GradientDrawable bg = (GradientDrawable) tvPriority.getBackground().mutate();
        if ("Cao".equalsIgnoreCase(priority)) {
            bg.setColor(Color.parseColor("#E74C3C")); // Đỏ
        } else if ("Thấp".equalsIgnoreCase(priority)) {
            bg.setColor(Color.parseColor("#3498DB")); // Xanh dương
        } else {
            bg.setColor(Color.parseColor("#F39C12")); // Cam (Trung bình / mặc định)
        }
    }

    // ================================================================
    //  Hiển thị Status + ẩn/hiện nút Hủy bỏ
    // ================================================================
    private void applyStatusStyle(ReportViewHolder holder, String status) {
        GradientDrawable bg = (GradientDrawable) holder.tvStatus.getBackground().mutate();

        switch (status != null ? status : "") {
            case "PENDING":
            case "Chưa làm":
                holder.tvStatus.setText("Chưa làm");
                holder.tvStatus.setTextColor(Color.parseColor("#E67E22"));
                bg.setColor(Color.parseColor("#FDEBD0"));
                bg.setStroke(1, Color.parseColor("#E67E22"));
                // Cho phép hủy khi đang PENDING
                holder.btnCancel.setVisibility(View.VISIBLE);
                break;

            case "IN_PROGRESS":
            case "Đang làm":
                holder.tvStatus.setText("Đang làm");
                holder.tvStatus.setTextColor(Color.parseColor("#2980B9"));
                bg.setColor(Color.parseColor("#D4E6F1"));
                bg.setStroke(1, Color.parseColor("#2980B9"));
                // Cho phép hủy khi đang IN_PROGRESS
                holder.btnCancel.setVisibility(View.VISIBLE);
                break;

            case "DONE":
            case "Đã xong":
                holder.tvStatus.setText("Đã xong");
                holder.tvStatus.setTextColor(Color.parseColor("#27AE60"));
                bg.setColor(Color.parseColor("#D5F5E3"));
                bg.setStroke(1, Color.parseColor("#27AE60"));
                // Ẩn nút Hủy khi đã xong
                holder.btnCancel.setVisibility(View.GONE);
                break;

            case "CANCELLED":
            case "Đã hủy":
                holder.tvStatus.setText("Đã hủy");
                holder.tvStatus.setTextColor(Color.parseColor("#7F8C8D"));
                bg.setColor(Color.parseColor("#F2F3F4"));
                bg.setStroke(1, Color.parseColor("#BDC3C7"));
                // Ẩn nút Hủy khi đã hủy rồi
                holder.btnCancel.setVisibility(View.GONE);
                break;

            default:
                holder.tvStatus.setText(status != null ? status : "—");
                holder.tvStatus.setTextColor(Color.parseColor("#7F8C8D"));
                bg.setColor(Color.parseColor("#F2F3F4"));
                bg.setStroke(1, Color.parseColor("#BDC3C7"));
                holder.btnCancel.setVisibility(View.VISIBLE);
        }
    }

    // ================================================================
    //  Hộp thoại xác nhận hủy
    // ================================================================
    private void showCancelConfirmDialog(String docId) {
        new AlertDialog.Builder(context)
                .setTitle("Xác nhận hủy phản ánh")
                .setMessage("Bạn có chắc chắn muốn hủy phản ánh này không?")
                .setNegativeButton("Không", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Đồng ý", (dialog, which) -> {
                    dialog.dismiss();
                    cancelReport(docId);
                })
                .setCancelable(true)
                .show();
    }

    // ================================================================
    //  Cập nhật Firestore → status = "CANCELLED"
    // ================================================================
    private void cancelReport(String docId) {
        db.collection("issues").document(docId)
                .update("status", "CANCELLED")
                .addOnSuccessListener(unused -> {
                    Log.d("ReportAdapter", "Đã hủy phản ánh: " + docId);
                    Toast.makeText(context, "Đã hủy phản ánh.", Toast.LENGTH_SHORT).show();
                    // Báo ngược lên Activity để tải lại danh sách
                    if (listener != null) {
                        listener.onReportCancelled();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ReportAdapter", "Lỗi khi hủy: " + e.getMessage(), e);
                    Toast.makeText(context, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ================================================================
    //  ViewHolder
    // ================================================================
    public static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView     tvReportTitle, tvPriority, tvCreatedAt, tvAppointment, tvStatus;
        TextView     btnContact, btnCancel;
        LinearLayout rowAppointment;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReportTitle  = itemView.findViewById(R.id.tvReportTitle);
            tvPriority     = itemView.findViewById(R.id.tvPriority);
            tvCreatedAt    = itemView.findViewById(R.id.tvCreatedAt);
            tvAppointment  = itemView.findViewById(R.id.tvAppointment);
            tvStatus       = itemView.findViewById(R.id.tvStatus);
            btnContact     = itemView.findViewById(R.id.btnContact);
            btnCancel      = itemView.findViewById(R.id.btnCancel);
            rowAppointment = itemView.findViewById(R.id.rowAppointment);
        }
    }
}
