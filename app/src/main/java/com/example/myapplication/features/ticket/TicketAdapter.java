package com.example.myapplication.features.ticket;

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

public class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.VH> {

    public interface OnActionListener {
        void onClick(Ticket t);
    }

    private final OnActionListener listener;
    private List<Ticket> list = new ArrayList<>();

    public TicketAdapter(OnActionListener listener) {
        this.listener = listener;
    }

    public void setList(List<Ticket> list) {
        this.list = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ticket, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Ticket t = list.get(position);
        h.tvTitle.setText(t.getTitle() != null ? t.getTitle() : h.itemView.getContext().getString(R.string.ticket_no_title));
        h.tvRoom.setText(t.getRoomId() != null
            ? h.itemView.getContext().getString(R.string.room_number, t.getRoomId())
            : "");
        h.tvStatus.setText(toVietnameseStatus(t.getStatus(), h.itemView.getContext()));

        GradientDrawable badge = new GradientDrawable();
        badge.setShape(GradientDrawable.RECTANGLE);
        badge.setCornerRadius(20f);
        badge.setColor(Color.parseColor("#E3F2FD"));
        h.tvStatus.setBackground(badge);
        h.tvStatus.setTextColor(Color.parseColor("#1565C0"));

        h.itemView.setOnClickListener(v -> listener.onClick(t));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private String toVietnameseStatus(String status, android.content.Context context) {
        if (TicketStatus.OPEN.equals(status))
            return context.getString(R.string.ticket_status_new);
        if (TicketStatus.IN_PROGRESS.equals(status))
            return context.getString(R.string.ticket_status_processing);
        if (TicketStatus.DONE.equals(status))
            return context.getString(R.string.completed);
        return status != null ? status : "";
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvRoom, tvStatus;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvRoom = itemView.findViewById(R.id.tvRoom);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
