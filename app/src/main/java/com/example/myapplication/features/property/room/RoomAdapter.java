package com.example.myapplication.features.property.room;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.core.constants.RoomStatus;
import com.example.myapplication.core.util.MoneyFormatter;
import com.example.myapplication.domain.Room;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.ViewHolder> {

    private List<Room> dataList = new ArrayList<>();
    private Map<String, TenantCardInfo> tenantByRoomId = new HashMap<>();
    private boolean readOnly;
    private final OnItemActionListener listener;

    public static class TenantCardInfo {
        public final String representativeName;
        public final String phone;

        public TenantCardInfo(String representativeName, String phone) {
            this.representativeName = representativeName;
            this.phone = phone;
        }
    }

    public interface OnItemActionListener {
        void onDelete(Room room);

        void onSelect(Room room);

        void onViewDetails(Room room);

        void onCreateContract(Room room);
    }

    public RoomAdapter(OnItemActionListener listener) {
        this.listener = listener;
    }

    public void setDataList(List<Room> list) {
        List<Room> newList = list != null ? list : new ArrayList<>();
        final List<Room> oldList = this.dataList;

        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldList.size();
            }

            @Override
            public int getNewListSize() {
                return newList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                Room oldItem = oldList.get(oldItemPosition);
                Room newItem = newList.get(newItemPosition);
                String oldId = oldItem != null ? oldItem.getId() : null;
                String newId = newItem != null ? newItem.getId() : null;
                if (oldId == null || newId == null) {
                    return oldItem == newItem;
                }
                return oldId.equals(newId);
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Room oldItem = oldList.get(oldItemPosition);
                Room newItem = newList.get(newItemPosition);
                if (oldItem == null || newItem == null) {
                    return oldItem == newItem;
                }

                return safeEq(oldItem.getRoomNumber(), newItem.getRoomNumber())
                        && safeEq(oldItem.getStatus(), newItem.getStatus())
                        && safeEq(oldItem.getImageUrl(), newItem.getImageUrl())
                        && Double.compare(oldItem.getRentAmount(), newItem.getRentAmount()) == 0;
            }
        });

        this.dataList = newList;
        diff.dispatchUpdatesTo(this);
    }

    public void setTenantByRoomId(@NonNull Map<String, TenantCardInfo> map) {
        Map<String, TenantCardInfo> oldMap = tenantByRoomId != null ? tenantByRoomId : new HashMap<>();
        tenantByRoomId = map != null ? map : new HashMap<>();

        for (int i = 0; i < dataList.size(); i++) {
            Room room = dataList.get(i);
            if (room == null || room.getId() == null)
                continue;
            String roomId = room.getId();
            TenantCardInfo oldTenant = oldMap.get(roomId);
            TenantCardInfo newTenant = tenantByRoomId.get(roomId);
            if (!sameTenantInfo(oldTenant, newTenant)) {
                notifyItemChanged(i);
            }
        }
    }

    public void setReadOnly(boolean readOnly) {
        if (this.readOnly == readOnly) {
            return;
        }
        this.readOnly = readOnly;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_room, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Room room = dataList.get(position);
        holder.tvRoomNumber.setText(room.getRoomNumber() != null ? "P." + room.getRoomNumber() : "P.???");

        holder.tvRentAmount.setText(MoneyFormatter.format(room.getRentAmount())
                + holder.itemView.getContext().getString(R.string.room_per_month));

        boolean trong = RoomStatus.VACANT.equals(room.getStatus());
        int color = Color.parseColor(trong ? "#F44336" : "#2196F3"); // red=vacant, blue=rented

        holder.tvStatus.setText(trong ? holder.itemView.getContext().getString(R.string.room_status_vacant)
                : holder.itemView.getContext().getString(R.string.room_status_rented));
        holder.tvStatus.setTextColor(color);

        String roomType = room.getRoomType();
        holder.tvRoomType.setText(holder.itemView.getContext().getString(
            R.string.item_room_type_value,
            (roomType != null && !roomType.trim().isEmpty())
                ? roomType
                : holder.itemView.getContext().getString(R.string.common_not_available)));

        holder.tvArea.setText(holder.itemView.getContext().getString(
            R.string.item_room_area_value,
            room.getArea() > 0 ? String.format(Locale.getDefault(), "%.1f", room.getArea())
                : holder.itemView.getContext().getString(R.string.common_not_available)));

        holder.tvMaxOccupancy.setText(holder.itemView.getContext().getString(
            R.string.item_room_max_occupancy_value,
            room.getMaxOccupancy() > 0
                ? holder.itemView.getContext().getString(R.string.room_max_occupancy_value, room.getMaxOccupancy())
                : holder.itemView.getContext().getString(R.string.common_not_available)));

        // Load room image
        if (room.getImageUrl() != null && !room.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(room.getImageUrl())
                    .centerCrop()
                .placeholder(R.drawable.bg_room_placeholder)
                .error(R.drawable.bg_room_placeholder)
                    .into(holder.ivRoomImage);
        } else {
            holder.ivRoomImage.setImageResource(R.drawable.bg_room_placeholder);
        }

        // Chip background
        GradientDrawable chipBg = new GradientDrawable();
        chipBg.setCornerRadius(dp(holder.itemView, 14));
        chipBg.setColor(Color.argb(30, Color.red(color), Color.green(color), Color.blue(color)));
        chipBg.setStroke(dp(holder.itemView, 1),
                Color.argb(90, Color.red(color), Color.green(color), Color.blue(color)));
        holder.tvStatus.setBackground(chipBg);

        String roomId = room.getId();
        TenantCardInfo tenant = (roomId != null) ? tenantByRoomId.get(roomId) : null;
        if (holder.tvTenantName != null && holder.tvTenantPhone != null) {
            String name = tenant != null ? tenant.representativeName : null;
            String phone = tenant != null ? tenant.phone : null;
            boolean hasName = name != null && !name.trim().isEmpty();
            boolean hasPhone = phone != null && !phone.trim().isEmpty();

            if (!trong && (hasName || hasPhone)) {
                holder.tvTenantName.setVisibility(View.VISIBLE);
                holder.tvTenantName.setText(holder.itemView.getContext().getString(
                        R.string.room_representative_value,
                        hasName ? name.trim() : holder.itemView.getContext().getString(R.string.common_not_available)));

                if (hasPhone) {
                    holder.tvTenantPhone.setVisibility(View.VISIBLE);
                    holder.tvTenantPhone
                            .setText(holder.itemView.getContext().getString(R.string.room_phone_value, phone.trim()));
                } else {
                    holder.tvTenantPhone.setVisibility(View.GONE);
                }
            } else {
                holder.tvTenantName.setVisibility(View.GONE);
                holder.tvTenantPhone.setVisibility(View.GONE);
            }
        }

        holder.itemView.setOnClickListener(v -> listener.onViewDetails(room));

    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoomNumber, tvRentAmount, tvStatus, tvTenantName, tvTenantPhone, tvRoomType, tvArea, tvMaxOccupancy;
        ImageView ivRoomImage;

        ViewHolder(View v) {
            super(v);
            ivRoomImage = v.findViewById(R.id.ivRoomImage);
            tvRoomNumber = v.findViewById(R.id.tvSoPhong);
            tvRentAmount = v.findViewById(R.id.tvGiaThue);
            tvStatus = v.findViewById(R.id.tvTrangThai);
            tvTenantName = v.findViewById(R.id.tvTenantName);
            tvTenantPhone = v.findViewById(R.id.tvTenantPhone);
            tvRoomType = v.findViewById(R.id.tvRoomType);
            tvArea = v.findViewById(R.id.tvArea);
            tvMaxOccupancy = v.findViewById(R.id.tvMaxOccupancy);
        }
    }

    private static int dp(@NonNull View v, int dp) {
        float d = v.getResources().getDisplayMetrics().density;
        return (int) (dp * d);
    }

    private static boolean safeEq(String a, String b) {
        if (a == null)
            return b == null;
        return a.equals(b);
    }

    private static boolean sameTenantInfo(TenantCardInfo a, TenantCardInfo b) {
        if (a == null)
            return b == null;
        if (b == null)
            return false;
        return safeEq(a.representativeName, b.representativeName)
                && safeEq(a.phone, b.phone);
    }
}
