package com.example.myapplication.features.home;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotifViewHolder> {

    private final List<NotificationItem> items;
    private final Context context;

    public NotificationAdapter(List<NotificationItem> items, Context context) {
        this.items   = items;
        this.context = context;
    }

    @NonNull
    @Override
    public NotifViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_notification, parent, false);
        return new NotifViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull NotifViewHolder holder, int position) {
        NotificationItem item = items.get(position);

        holder.tvIcon.setText(item.getIcon());
        holder.tvTitle.setText(item.getTitle());
        holder.tvBody.setText(item.getBody());
        holder.tvTime.setText(item.getTimeLabel());

        if (item.isSystemNotification()) {
            // ── Thông báo hệ thống: viền cam + nền cam nhạt ──
            holder.card.setCardBackgroundColor(Color.parseColor("#FFFBF5"));
            holder.card.setStrokeColor(Color.parseColor("#E67E22"));
            holder.card.setStrokeWidth(dpToPx(1));
            holder.tvTitle.setTextColor(Color.parseColor("#C0662A"));
            holder.tvSystemBadge.setVisibility(View.VISIBLE);
        } else {
            // ── Thông báo thường: nền trắng, viền nhạt ──
            holder.card.setCardBackgroundColor(Color.WHITE);
            holder.card.setStrokeColor(Color.parseColor("#E8E8E8"));
            holder.card.setStrokeWidth(dpToPx(1));
            holder.tvTitle.setTextColor(Color.parseColor("#1A1A2E"));
            holder.tvSystemBadge.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() { return items != null ? items.size() : 0; }

    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    // ================================================================
    //  ViewHolder
    // ================================================================
    static class NotifViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView tvIcon, tvTitle, tvBody, tvTime, tvSystemBadge;

        NotifViewHolder(@NonNull View itemView) {
            super(itemView);
            card          = itemView.findViewById(R.id.cardNotificationItem);
            tvIcon        = itemView.findViewById(R.id.tvNotifIcon);
            tvTitle       = itemView.findViewById(R.id.tvNotifTitle);
            tvBody        = itemView.findViewById(R.id.tvNotifBody);
            tvTime        = itemView.findViewById(R.id.tvNotifTime);
            tvSystemBadge = itemView.findViewById(R.id.tvSystemBadge);
        }
    }
}
