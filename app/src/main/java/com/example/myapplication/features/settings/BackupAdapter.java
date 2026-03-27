package com.example.myapplication.features.settings;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BackupAdapter extends RecyclerView.Adapter<BackupAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onClick(BackupRestoreActivity.BackupItem item);
    }

    private List<BackupRestoreActivity.BackupItem> danhSach = new ArrayList<>();
    private final OnItemClickListener listener;

    public BackupAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setDanhSach(List<BackupRestoreActivity.BackupItem> list) {
        this.danhSach = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_backup, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        BackupRestoreActivity.BackupItem it = danhSach.get(position);
        h.tvId.setText(it.id);

        String note = it.note != null ? it.note.trim() : "";
        h.tvNote.setVisibility(note.isEmpty() ? View.GONE : View.VISIBLE);
        h.tvNote.setText(note);

        Timestamp ts = it.createdAt;
        if (ts != null) {
            Date d = ts.toDate();
            h.tvTime.setText(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(d));
        } else {
            h.tvTime.setText("");
        }

        h.itemView.setOnClickListener(v -> listener.onClick(it));
    }

    @Override
    public int getItemCount() {
        return danhSach.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvId, tvTime, tvNote;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvId = itemView.findViewById(R.id.tvBackupId);
            tvTime = itemView.findViewById(R.id.tvBackupTime);
            tvNote = itemView.findViewById(R.id.tvBackupNote);
        }
    }
}
