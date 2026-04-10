package com.example.myapplication.features.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.features.chat.model.ChatConversation;

import java.util.ArrayList;
import java.util.List;

public class ChatConversationAdapter extends RecyclerView.Adapter<ChatConversationAdapter.ViewHolder> {

    public interface OnConversationClick {
        void onClick(ChatConversation item);
    }

    private final List<ChatConversation> items = new ArrayList<>();
    private final OnConversationClick clickListener;

    public ChatConversationAdapter(OnConversationClick clickListener) {
        this.clickListener = clickListener;
    }

    public void submit(List<ChatConversation> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_conversation, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatConversation item = items.get(position);
        holder.title.setText(item.title != null ? item.title : "...");
        holder.subtitle.setText(item.subtitle != null ? item.subtitle : "");
        if (item.unreadCount > 0) {
            holder.unreadBadge.setVisibility(View.VISIBLE);
            holder.unreadBadge.setText(item.unreadCount > 99 ? "99+" : String.valueOf(item.unreadCount));
            holder.subtitle.setTextColor(0xFF1F1F1F);
        } else {
            holder.unreadBadge.setVisibility(View.GONE);
            holder.subtitle.setTextColor(0xFF5F6368);
        }
        holder.itemView.setOnClickListener(v -> clickListener.onClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView subtitle;
        final TextView unreadBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvConversationTitle);
            subtitle = itemView.findViewById(R.id.tvConversationSubtitle);
            unreadBadge = itemView.findViewById(R.id.tvConversationUnreadBadge);
        }
    }
}
