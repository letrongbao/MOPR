package com.example.myapplication.features.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.features.chat.model.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ME = 1;
    private static final int TYPE_OTHER = 2;

    private final List<ChatMessage> items = new ArrayList<>();
    private final String currentUid;

    public ChatMessageAdapter(String currentUid) {
        this.currentUid = currentUid;
    }

    public void submit(List<ChatMessage> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage item = items.get(position);
        return currentUid != null && currentUid.equals(item.senderId) ? TYPE_ME : TYPE_OTHER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ME) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message_me, parent, false);
            return new MeViewHolder(v);
        }
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message_other, parent, false);
        return new OtherViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage item = items.get(position);
        String time = "";
        if (item.createdAt != null) {
            Date date = item.createdAt.toDate();
            time = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(date);
        }

        if (holder instanceof MeViewHolder) {
            MeViewHolder vh = (MeViewHolder) holder;
            vh.body.setText(item.text != null ? item.text : "");
            vh.meta.setText(time);
            return;
        }

        OtherViewHolder vh = (OtherViewHolder) holder;
        vh.sender.setText(item.senderName != null ? item.senderName : "...");
        vh.body.setText(item.text != null ? item.text : "");
        vh.meta.setText(time);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class MeViewHolder extends RecyclerView.ViewHolder {
        final TextView body;
        final TextView meta;

        MeViewHolder(@NonNull View itemView) {
            super(itemView);
            body = itemView.findViewById(R.id.tvMessageBody);
            meta = itemView.findViewById(R.id.tvMessageMeta);
        }
    }

    static class OtherViewHolder extends RecyclerView.ViewHolder {
        final TextView sender;
        final TextView body;
        final TextView meta;

        OtherViewHolder(@NonNull View itemView) {
            super(itemView);
            sender = itemView.findViewById(R.id.tvSenderName);
            body = itemView.findViewById(R.id.tvMessageBody);
            meta = itemView.findViewById(R.id.tvMessageMeta);
        }
    }
}
