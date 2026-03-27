package com.example.myapplication.features.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.domain.RentalHistory;

import java.util.List;

public class RentalHistoryAdapter extends RecyclerView.Adapter<RentalHistoryAdapter.ViewHolder> {

    private List<RentalHistory> historyList;

    public RentalHistoryAdapter(List<RentalHistory> historyList) {
        this.historyList = historyList;
    }

    public void updateData(List<RentalHistory> newList) {
        this.historyList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rental_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RentalHistory history = historyList.get(position);
        
        String initial = history.getHoTen() != null && !history.getHoTen().isEmpty() 
                ? history.getHoTen().substring(0, 1).toUpperCase() 
                : "?";
        holder.tvInitial.setText(initial);
        
        holder.tvTenantName.setText(history.getHoTen() != null ? history.getHoTen() : "N/A");
        
        String roomInfo = String.format("Phòng %s%s", 
                history.getSoPhong() != null ? history.getSoPhong() : "N/A",
                history.getCanNhaTen() != null && !history.getCanNhaTen().isEmpty() ? " - " + history.getCanNhaTen() : "");
        holder.tvRoomInfo.setText(roomInfo);
        
        String rentalPeriod = String.format("%s - %s",
                history.getNgayBatDauThue() != null ? history.getNgayBatDauThue() : "N/A",
                history.getNgayKetThucThucTe() != null ? history.getNgayKetThucThucTe() : "N/A");
        holder.tvRentalPeriod.setText(rentalPeriod);
        
        if (history.getSoNgayThueThucTe() > 0) {
            int months = history.getSoNgayThueThucTe() / 30;
            int days = history.getSoNgayThueThucTe() % 30;
            String duration = months > 0 
                    ? String.format("%d tháng %d ngày", months, days)
                    : String.format("%d ngày", days);
            holder.tvDuration.setText(duration);
        } else {
            holder.tvDuration.setText("N/A");
        }
        
        holder.tvPhone.setText(history.getSoDienThoai() != null ? history.getSoDienThoai() : "N/A");
        holder.tvCccd.setText(history.getCccd() != null ? history.getCccd() : "N/A");
        holder.tvRentAmount.setText(String.format("%,.0fđ", history.getTienPhong()));
        holder.tvDeposit.setText(String.format("Cọc: %,.0fđ", history.getTienCoc()));
        holder.tvMembers.setText(String.format("%d người", history.getSoThanhVien()));
        
        if (history.getGhiChu() != null && !history.getGhiChu().trim().isEmpty()) {
            holder.noteContainer.setVisibility(View.VISIBLE);
            holder.tvNote.setText(history.getGhiChu());
        } else {
            holder.noteContainer.setVisibility(View.GONE);
        }
        
        holder.servicesContainer.removeAllViews();
        if (history.isDichVuGuiXe() || history.isDichVuInternet() || history.isDichVuGiatSay()) {
            if (history.isDichVuGuiXe()) {
                holder.servicesContainer.addView(createServiceBadge(holder.itemView, "Xe"));
            }
            if (history.isDichVuInternet()) {
                holder.servicesContainer.addView(createServiceBadge(holder.itemView, "Internet"));
            }
            if (history.isDichVuGiatSay()) {
                holder.servicesContainer.addView(createServiceBadge(holder.itemView, "Giặt sấy"));
            }
        }
    }

    private View createServiceBadge(View parent, String text) {
        TextView badge = new TextView(parent.getContext());
        badge.setText(text);
        badge.setTextSize(10);
        badge.setTextColor(0xFF4CAF50);
        badge.setBackgroundResource(R.drawable.bg_card_rounded);
        badge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE8F5E9));
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 8, 0);
        badge.setLayoutParams(params);
        
        int padding = (int) (6 * parent.getContext().getResources().getDisplayMetrics().density);
        badge.setPadding(padding, padding / 2, padding, padding / 2);
        
        return badge;
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvInitial, tvTenantName, tvRoomInfo, tvStatus;
        TextView tvRentalPeriod, tvDuration;
        TextView tvPhone, tvCccd;
        TextView tvRentAmount, tvDeposit, tvMembers;
        TextView tvNote;
        LinearLayout noteContainer, servicesContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitial = itemView.findViewById(R.id.tvInitial);
            tvTenantName = itemView.findViewById(R.id.tvTenantName);
            tvRoomInfo = itemView.findViewById(R.id.tvRoomInfo);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvRentalPeriod = itemView.findViewById(R.id.tvRentalPeriod);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvCccd = itemView.findViewById(R.id.tvCccd);
            tvRentAmount = itemView.findViewById(R.id.tvRentAmount);
            tvDeposit = itemView.findViewById(R.id.tvDeposit);
            tvMembers = itemView.findViewById(R.id.tvMembers);
            tvNote = itemView.findViewById(R.id.tvNote);
            noteContainer = itemView.findViewById(R.id.noteContainer);
            servicesContainer = itemView.findViewById(R.id.servicesContainer);
        }
    }
}

