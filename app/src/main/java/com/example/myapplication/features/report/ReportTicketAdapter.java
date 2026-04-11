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

import java.util.ArrayList;
import java.util.List;

public class ReportTicketAdapter extends RecyclerView.Adapter<ReportTicketAdapter.VH> {

    public interface OnItemClickListener {
        void onClick(Ticket ticket);
    }

    private final OnItemClickListener listener;
    private final List<Ticket> list = new ArrayList<>();

    public ReportTicketAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void submit(List<Ticket> tickets) {
        list.clear();
        if (tickets != null) {
            list.addAll(tickets);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ticket, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Ticket ticket = list.get(position);
        holder.tvTitle.setText(ticket.getTitle() != null ? ticket.getTitle() : holder.itemView.getContext().getString(R.string.ticket_no_title));
        holder.tvRoom.setText(ticket.getRoomId() != null ? ("Room: " + ticket.getRoomId()) : "");
        holder.tvStatus.setText(toStatusLabel(
            ticket.getStatus(),
            holder.itemView.getContext().getString(R.string.report_tab_open),
            holder.itemView.getContext().getString(R.string.report_tab_in_progress),
            holder.itemView.getContext().getString(R.string.completed),
            holder.itemView.getContext().getString(R.string.report_status_rejected)));

        GradientDrawable badge = new GradientDrawable();
        badge.setShape(GradientDrawable.RECTANGLE);
        badge.setCornerRadius(20f);
        if (TicketStatus.DONE.equals(ticket.getStatus())) {
            badge.setColor(Color.parseColor("#E8F5E9"));
            holder.tvStatus.setTextColor(Color.parseColor("#2E7D32"));
        } else if (TicketStatus.REJECTED.equals(ticket.getStatus())) {
            badge.setColor(Color.parseColor("#FDECEA"));
            holder.tvStatus.setTextColor(Color.parseColor("#C62828"));
        } else if (TicketStatus.IN_PROGRESS.equals(ticket.getStatus())) {
            badge.setColor(Color.parseColor("#E3F2FD"));
            holder.tvStatus.setTextColor(Color.parseColor("#1565C0"));
        } else {
            badge.setColor(Color.parseColor("#FFF3E0"));
            holder.tvStatus.setTextColor(Color.parseColor("#EF6C00"));
        }
        holder.tvStatus.setBackground(badge);

        holder.itemView.setOnClickListener(v -> listener.onClick(ticket));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private String toStatusLabel(String status, String openText, String inProgressText, String doneText, String rejectedText) {
        if (TicketStatus.OPEN.equals(status)) {
            return openText;
        }
        if (TicketStatus.IN_PROGRESS.equals(status)) {
            return inProgressText;
        }
        if (TicketStatus.DONE.equals(status)) {
            return doneText;
        }
        if (TicketStatus.REJECTED.equals(status)) {
            return rejectedText;
        }
        return status != null ? status : "";
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvRoom;
        final TextView tvStatus;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvRoom = itemView.findViewById(R.id.tvRoom);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
