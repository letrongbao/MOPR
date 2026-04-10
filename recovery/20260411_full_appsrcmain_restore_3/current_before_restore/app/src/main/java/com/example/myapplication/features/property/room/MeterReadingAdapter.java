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

    private List<MeterReading> dataList = new ArrayList<>();
    private final OnActionListener listener;

    public MeterReadingAdapter(OnActionListener listener) {
        this.listener = listener;
    }

    public void setDataList(List<MeterReading> list) {
        this.dataList = list;
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
        MeterReading r = dataList.get(position);

        holder.tvPeriod
                .setText(holder.itemView.getContext().getString(R.string.meter_period_label, safe(r.getPeriod())));
        holder.tvElec.setText(holder.itemView.getContext().getString(R.string.meter_electricity_label,
                fmt(r.getElecStart()), fmt(r.getElecEnd()), fmt(r.getElecEnd() - r.getElecStart())));
        holder.tvWater.setText(holder.itemView.getContext().getString(R.string.meter_water_label,
                fmt(r.getWaterStart()), fmt(r.getWaterEnd()), fmt(r.getWaterEnd() - r.getWaterStart())));

        holder.btnDelete.setOnClickListener(v -> listener.onDelete(r));
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPeriod, tvElec, tvWater;
        ImageButton btnDelete;

        ViewHolder(View v) {
            super(v);
            tvPeriod = v.findViewById(R.id.tvPeriod);
            tvElec = v.findViewById(R.id.tvElec);
            tvWater = v.findViewById(R.id.tvWater);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String fmt(double value) {
        return value % 1 == 0 ? String.valueOf((long) value) : String.valueOf(value);
    }
}
