package com.example.myapplication.features.notification;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.features.notification.model.NotificationItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    public interface OnNotificationClick {
        void onClick(NotificationItem item);
    }

    private final List<NotificationItem> items = new ArrayList<>();
    private final OnNotificationClick clickListener;

    public NotificationAdapter(OnNotificationClick clickListener) {
        this.clickListener = clickListener;
    }

    public void submit(List<NotificationItem> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationItem item = items.get(position);
        holder.title.setText(item.title != null ? item.title : "");
        holder.body.setText(item.body != null ? item.body : "");

        String time = "";
        if (item.createdAt != null) {
            Date date = item.createdAt.toDate();
            time = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(date);
        }
        holder.meta.setText(time);

        int style = item.isRead ? Typeface.NORMAL : Typeface.BOLD;
        holder.title.setTypeface(holder.title.getTypeface(), style);
        holder.itemView.setAlpha(item.isRead ? 0.72f : 1f);
        holder.itemView.setOnClickListener(v -> clickListener.onClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView body;
        final TextView meta;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvNotificationTitle);
            body = itemView.findViewById(R.id.tvNotificationBody);
            meta = itemView.findViewById(R.id.tvNotificationMeta);
        }
    }
}
