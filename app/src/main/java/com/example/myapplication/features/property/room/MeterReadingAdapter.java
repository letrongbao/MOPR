package com.example.myapplication.features.property.room;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.domain.MeterReading;

import java.util.ArrayList;
import java.util.List;

public class MeterReadingAdapter extends RecyclerView.Adapter<MeterReadingAdapter.ViewHolder> {

    public interface OnActionListener {
        void onDelete(MeterReading reading);
    }

    private List<MeterReading> danhSach = new ArrayList<>();
    private final OnActionListener listener;

    public MeterReadingAdapter(OnActionListener listener) {
        this.listener = listener;
    }

    public void setDanhSach(List<MeterReading> list) {
        this.danhSach = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_meter_reading, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MeterReading r = danhSach.get(position);

        holder.tvPeriod.setText("Kỳ: " + safe(r.getPeriod()));
        holder.tvElec.setText("Điện: " + fmt(r.getElecStart()) + " → " + fmt(r.getElecEnd()) + " (" + fmt(r.getElecEnd() - r.getElecStart()) + ")");
        holder.tvWater.setText("Nước: " + fmt(r.getWaterStart()) + " → " + fmt(r.getWaterEnd()) + " (" + fmt(r.getWaterEnd() - r.getWaterStart()) + ")");

        holder.btnXoa.setOnClickListener(v -> listener.onDelete(r));
    }

    @Override
    public int getItemCount() {
        return danhSach.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPeriod, tvElec, tvWater;
        ImageButton btnXoa;

        ViewHolder(View v) {
            super(v);
            tvPeriod = v.findViewById(R.id.tvPeriod);
            tvElec = v.findViewById(R.id.tvElec);
            tvWater = v.findViewById(R.id.tvWater);
            btnXoa = v.findViewById(R.id.btnXoa);
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String fmt(double value) {
        return value % 1 == 0 ? String.valueOf((long) value) : String.valueOf(value);
    }
}
