package com.example.myapplication.features.report;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.constants.TicketStatus;
import com.example.myapplication.domain.Ticket;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OwnerReportAdapter extends RecyclerView.Adapter<OwnerReportAdapter.VH> {

    public interface OnItemClickListener {
        void onClick(Ticket ticket);
    }

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    private final OnItemClickListener listener;
    private final List<Ticket> list = new ArrayList<>();
    private final Map<String, String> roomLabelById = new HashMap<>();
    private final Map<String, String> houseLabelByRoomId = new HashMap<>();

    public OwnerReportAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void submit(List<Ticket> tickets) {
        list.clear();
        if (tickets != null) {
            list.addAll(tickets);
        }
        notifyDataSetChanged();
    }

    public void updateLocationLabels(Map<String, String> roomLabels, Map<String, String> houseLabels) {
        roomLabelById.clear();
        houseLabelByRoomId.clear();
        if (roomLabels != null) {
            roomLabelById.putAll(roomLabels);
        }
        if (houseLabels != null) {
            houseLabelByRoomId.putAll(houseLabels);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_owner_report, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Ticket ticket = list.get(position);

        holder.tvReportTitle.setText(ticket.getTitle() != null ? ticket.getTitle() : holder.itemView.getContext().getString(R.string.ticket_no_title));
        String roomId = ticket.getRoomId();
        String roomLabel = roomId != null ? roomLabelById.get(roomId) : null;
        if (roomLabel == null || roomLabel.trim().isEmpty()) {
            roomLabel = roomId != null && !roomId.trim().isEmpty()
                    ? holder.itemView.getContext().getString(R.string.room_number, roomId)
                    : holder.itemView.getContext().getString(R.string.report_unknown_room);
        }
        String houseLabel = roomId != null ? houseLabelByRoomId.get(roomId) : null;
        if (houseLabel == null || houseLabel.trim().isEmpty()) {
            houseLabel = holder.itemView.getContext().getString(R.string.report_unknown_house);
        }
        holder.tvTenantName.setText(holder.itemView.getContext().getString(R.string.report_house_line, houseLabel));
        holder.tvRoomNumber.setText(holder.itemView.getContext().getString(R.string.report_room_line, roomLabel));

        holder.tvCreatedAt.setText(ticket.getCreatedAt() != null
                ? holder.itemView.getContext().getString(
                        R.string.report_created_at_line,
                        DATE_FORMAT.format(ticket.getCreatedAt().toDate()))
                : holder.itemView.getContext().getString(R.string.report_created_at_line, "--/--/----"));

        if (ticket.getAppointmentTime() != null) {
            holder.rowAppointment.setVisibility(View.VISIBLE);
            holder.tvAppointment.setText(holder.itemView.getContext().getString(
                    R.string.report_appointment_line,
                    DATE_FORMAT.format(ticket.getAppointmentTime().toDate())));
        } else {
            holder.rowAppointment.setVisibility(View.GONE);
        }

        if (TicketStatus.REJECTED.equals(ticket.getStatus()) && ticket.getRejectReason() != null && !ticket.getRejectReason().trim().isEmpty()) {
            holder.rowRejectReason.setVisibility(View.VISIBLE);
            holder.tvRejectReason.setText(ticket.getRejectReason());
        } else {
            holder.rowRejectReason.setVisibility(View.GONE);
        }

        styleStatus(holder.tvStatus, ticket.getStatus());
        holder.itemView.setOnClickListener(v -> listener.onClick(ticket));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private void styleStatus(TextView view, String status) {
        GradientDrawable bg = (GradientDrawable) view.getBackground().mutate();
        if (TicketStatus.DONE.equals(status)) {
            view.setText(view.getContext().getString(R.string.completed));
            view.setTextColor(Color.parseColor("#2E7D32"));
            bg.setColor(Color.parseColor("#E8F5E9"));
        } else if (TicketStatus.REJECTED.equals(status)) {
            view.setText(view.getContext().getString(R.string.report_status_rejected));
            view.setTextColor(Color.parseColor("#C62828"));
            bg.setColor(Color.parseColor("#FDECEA"));
        } else if (TicketStatus.IN_PROGRESS.equals(status)) {
            view.setText(view.getContext().getString(R.string.report_tab_in_progress));
            view.setTextColor(Color.parseColor("#1565C0"));
            bg.setColor(Color.parseColor("#E3F2FD"));
        } else {
            view.setText(view.getContext().getString(R.string.report_tab_open));
            view.setTextColor(Color.parseColor("#EF6C00"));
            bg.setColor(Color.parseColor("#FFF3E0"));
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvReportTitle;
        final TextView tvTenantName;
        final TextView tvRoomNumber;
        final TextView tvCreatedAt;
        final View rowAppointment;
        final TextView tvAppointment;
        final View rowRejectReason;
        final TextView tvRejectReason;
        final TextView tvStatus;

        VH(@NonNull View itemView) {
            super(itemView);
            tvReportTitle = itemView.findViewById(R.id.tvReportTitle);
            tvTenantName = itemView.findViewById(R.id.tvTenantName);
            tvRoomNumber = itemView.findViewById(R.id.tvRoomNumber);
            tvCreatedAt = itemView.findViewById(R.id.tvCreatedAt);
            rowAppointment = itemView.findViewById(R.id.rowAppointment);
            tvAppointment = itemView.findViewById(R.id.tvAppointment);
            rowRejectReason = itemView.findViewById(R.id.rowRejectReason);
            tvRejectReason = itemView.findViewById(R.id.tvRejectReason);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
