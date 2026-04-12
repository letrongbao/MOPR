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
import java.util.List;
import java.util.Locale;

public class TenantReportAdapter extends RecyclerView.Adapter<TenantReportAdapter.VH> {

    public enum Action {
        OPEN,
        RESUBMIT,
        CONTACT,
        CANCEL
    }

    public interface OnItemClickListener {
        void onClick(Ticket ticket, Action action);
    }

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    private final OnItemClickListener listener;
    private final List<Ticket> list = new ArrayList<>();

    public TenantReportAdapter(OnItemClickListener listener) {
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tenant_report, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Ticket ticket = list.get(position);

        holder.tvReportTitle.setText(ticket.getTitle() != null ? ticket.getTitle() : holder.itemView.getContext().getString(R.string.ticket_no_title));
        holder.tvCreatedAt.setText(ticket.getCreatedAt() != null
                ? DATE_FORMAT.format(ticket.getCreatedAt().toDate())
                : "--/--/----");

        if (ticket.getAppointmentTime() != null) {
            holder.rowAppointment.setVisibility(View.VISIBLE);
            holder.tvAppointment.setText(DATE_FORMAT.format(ticket.getAppointmentTime().toDate()));
        } else {
            holder.rowAppointment.setVisibility(View.GONE);
        }

        boolean rejected = TicketStatus.REJECTED.equals(ticket.getStatus());
        if (rejected && ticket.getRejectReason() != null && !ticket.getRejectReason().trim().isEmpty()) {
            holder.rowRejectReason.setVisibility(View.VISIBLE);
            holder.tvRejectReason.setText(ticket.getRejectReason());
            holder.layoutResubmitButton.setVisibility(View.VISIBLE);
        } else {
            holder.rowRejectReason.setVisibility(View.GONE);
            holder.layoutResubmitButton.setVisibility(View.GONE);
        }
        holder.btnResubmit.setEnabled(rejected);
        holder.btnResubmit.setVisibility(rejected ? View.VISIBLE : View.GONE);

        holder.tvPriority.setText(holder.itemView.getContext().getString(R.string.report_priority_default));
        styleStatus(holder.tvStatus, ticket.getStatus());

        boolean canCancel = TicketStatus.OPEN.equals(ticket.getStatus());
        holder.btnCancel.setEnabled(canCancel);
        holder.btnCancel.setVisibility(canCancel ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> listener.onClick(ticket, Action.OPEN));
        holder.btnResubmit.setOnClickListener(v -> listener.onClick(ticket, Action.RESUBMIT));
        holder.btnContact.setOnClickListener(v -> listener.onClick(ticket, Action.CONTACT));
        holder.btnCancel.setOnClickListener(v -> listener.onClick(ticket, Action.CANCEL));
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
        final TextView tvCreatedAt;
        final View rowAppointment;
        final TextView tvAppointment;
        final View rowRejectReason;
        final TextView tvRejectReason;
        final TextView tvStatus;
        final TextView tvPriority;
        final View layoutResubmitButton;
        final TextView btnResubmit;
        final TextView btnContact;
        final TextView btnCancel;

        VH(@NonNull View itemView) {
            super(itemView);
            tvReportTitle = itemView.findViewById(R.id.tvReportTitle);
            tvCreatedAt = itemView.findViewById(R.id.tvCreatedAt);
            rowAppointment = itemView.findViewById(R.id.rowAppointment);
            tvAppointment = itemView.findViewById(R.id.tvAppointment);
            rowRejectReason = itemView.findViewById(R.id.rowRejectReason);
            tvRejectReason = itemView.findViewById(R.id.tvRejectReason);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvPriority = itemView.findViewById(R.id.tvPriority);
            layoutResubmitButton = itemView.findViewById(R.id.layoutResubmitButton);
            btnResubmit = itemView.findViewById(R.id.btnResubmit);
            btnContact = itemView.findViewById(R.id.btnContact);
            btnCancel = itemView.findViewById(R.id.btnCancel);
        }
    }
}
