package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.model.KhuTro;

import java.util.ArrayList;
import java.util.List;

public class KhuTroAdapter extends RecyclerView.Adapter<KhuTroAdapter.VH> {

    public interface OnItemClick {
        void onClick(@NonNull KhuTro item);
    }

    private final List<KhuTro> items = new ArrayList<>();
    private final OnItemClick cb;

    public KhuTroAdapter(@NonNull OnItemClick cb) {
        this.cb = cb;
    }

    public void setItems(List<KhuTro> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_khu_tro, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        KhuTro k = items.get(position);
        h.tvName.setText(k.getTenKhu() != null ? k.getTenKhu() : "");
        String addr = k.getDiaChi() != null ? k.getDiaChi() : "";
        h.tvAddr.setText(addr);
        h.itemView.setOnClickListener(v -> cb.onClick(k));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvAddr;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvKhuName);
            tvAddr = itemView.findViewById(R.id.tvKhuAddr);
        }
    }
}
