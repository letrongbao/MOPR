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
import java.util.Locale;

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

        String initial = history.getFullName() != null && !history.getFullName().isEmpty()
                ? history.getFullName().substring(0, 1).toUpperCase(Locale.ROOT)
                : "?";
        holder.tvInitial.setText(initial);

        holder.tvTenantName.setText(history.getFullName() != null ? history.getFullName() : "N/A");

        String roomInfo = String.format(holder.itemView.getContext().getString(R.string.rental_room_info),
                history.getRoomNumber() != null ? history.getRoomNumber()
                        : holder.itemView.getContext().getString(R.string.common_not_available),
                history.getHouseName() != null && !history.getHouseName().isEmpty() ? " - " + history.getHouseName()
                        : "");
        holder.tvRoomInfo.setText(roomInfo);

        String rentalPeriod = String.format(holder.itemView.getContext().getString(R.string.rental_period_range),
                history.getRentalStartDate() != null ? history.getRentalStartDate()
                        : holder.itemView.getContext().getString(R.string.common_not_available),
                history.getActualEndDate() != null ? history.getActualEndDate()
                        : holder.itemView.getContext().getString(R.string.common_not_available));
        holder.tvRentalPeriod.setText(rentalPeriod);

        if (history.getActualRentalDays() > 0) {
            int months = history.getActualRentalDays() / 30;
            int days = history.getActualRentalDays() % 30;
            String duration = months > 0
                    ? holder.itemView.getContext().getString(R.string.rental_duration_month_day, months, days)
                    : holder.itemView.getContext().getString(R.string.rental_duration_day, days);
            holder.tvDuration.setText(duration);
        } else {
            holder.tvDuration.setText(R.string.common_not_available);
        }

        holder.tvPhone.setText(history.getPhoneNumber() != null ? history.getPhoneNumber()
                : holder.itemView.getContext().getString(R.string.common_not_available));
        holder.tvPersonalId.setText(history.getPersonalId() != null ? history.getPersonalId()
                : holder.itemView.getContext().getString(R.string.common_not_available));
        holder.tvRentAmount
                .setText(holder.itemView.getContext().getString(R.string.currency_no_decimals, history.getRoomPrice()));
        holder.tvDeposit.setText(
                holder.itemView.getContext().getString(R.string.deposit_amount_label, history.getDepositAmount()));
        holder.tvMembers
                .setText(holder.itemView.getContext().getString(R.string.member_count_label, history.getMemberCount()));

        if (history.getNote() != null && !history.getNote().trim().isEmpty()) {
            holder.noteContainer.setVisibility(View.VISIBLE);
            holder.tvNote.setText(history.getNote());
        } else {
            holder.noteContainer.setVisibility(View.GONE);
        }

        holder.servicesContainer.removeAllViews();
        if (history.hasParkingService() || history.hasInternetService() || history.hasLaundryService()) {
            if (history.hasParkingService()) {
                holder.servicesContainer.addView(createServiceBadge(holder.itemView,
                        holder.itemView.getContext().getString(R.string.parking_service_label)));
            }
            if (history.hasInternetService()) {
                holder.servicesContainer.addView(createServiceBadge(holder.itemView,
                        holder.itemView.getContext().getString(R.string.internet_service_label)));
            }
            if (history.hasLaundryService()) {
                holder.servicesContainer.addView(createServiceBadge(holder.itemView,
                        holder.itemView.getContext().getString(R.string.laundry_service_name)));
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
                LinearLayout.LayoutParams.WRAP_CONTENT);
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
        TextView tvPhone, tvPersonalId;
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
            tvPersonalId = itemView.findViewById(R.id.tvCccd);
            tvRentAmount = itemView.findViewById(R.id.tvRentAmount);
            tvDeposit = itemView.findViewById(R.id.tvDeposit);
            tvMembers = itemView.findViewById(R.id.tvMembers);
            tvNote = itemView.findViewById(R.id.tvNote);
            noteContainer = itemView.findViewById(R.id.noteContainer);
            servicesContainer = itemView.findViewById(R.id.servicesContainer);
        }
    }
}
