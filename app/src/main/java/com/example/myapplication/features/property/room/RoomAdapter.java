package com.example.myapplication.features.property.room;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
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
    private Map<String, String> tenantByRoomId = new HashMap<>();
    private boolean readOnly;
    private final OnItemActionListener listener;

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

    public void setTenantByRoomId(@NonNull Map<String, String> map) {
        Map<String, String> oldMap = tenantByRoomId != null ? tenantByRoomId : new HashMap<>();
        tenantByRoomId = map != null ? map : new HashMap<>();

        for (int i = 0; i < dataList.size(); i++) {
            Room room = dataList.get(i);
            if (room == null || room.getId() == null)
                continue;
            String roomId = room.getId();
            String oldTenant = oldMap.get(roomId);
            String newTenant = tenantByRoomId.get(roomId);
            if (!safeEq(oldTenant, newTenant)) {
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

        // Load room image
        if (room.getImageUrl() != null && !room.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(room.getImageUrl())
                    .centerCrop()
                    .placeholder(R.drawable.baseline_home_24)
                    .error(R.drawable.baseline_home_24)
                    .into(holder.ivRoomImage);
        } else {
            holder.ivRoomImage.setImageResource(R.drawable.baseline_home_24);
        }

        // Chip background
        GradientDrawable chipBg = new GradientDrawable();
        chipBg.setCornerRadius(dp(holder.itemView, 14));
        chipBg.setColor(Color.argb(30, Color.red(color), Color.green(color), Color.blue(color)));
        chipBg.setStroke(dp(holder.itemView, 1),
                Color.argb(90, Color.red(color), Color.green(color), Color.blue(color)));
        holder.tvStatus.setBackground(chipBg);

        String roomId = room.getId();
        String tenant = (roomId != null) ? tenantByRoomId.get(roomId) : null;
        if (holder.tvTenant != null) {
            if (!trong && tenant != null && !tenant.trim().isEmpty()) {
                holder.tvTenant.setVisibility(View.VISIBLE);
                holder.tvTenant.setText(holder.itemView.getContext().getString(R.string.room_tenant_label, tenant));
            } else {
                holder.tvTenant.setVisibility(View.GONE);
            }
        }

        holder.itemView.setOnClickListener(v -> listener.onViewDetails(room));

        if (holder.btnMore != null) {
            // Hide menu button when room is occupied or in read-only mode
            boolean isRented = !trong;
            holder.btnMore.setVisibility((readOnly || isRented) ? View.GONE : View.VISIBLE);
            if (!readOnly && !isRented) {
                holder.btnMore.setOnClickListener(v -> {
                    PopupMenu pm = new PopupMenu(v.getContext(), v);
                    pm.getMenu().add(0, 1, 0, v.getContext().getString(R.string.room_edit));
                    pm.getMenu().add(0, 2, 1, v.getContext().getString(R.string.room_delete));
                    pm.setOnMenuItemClickListener(item -> {
                        if (item.getItemId() == 1) {
                            listener.onSelect(room);
                            return true;
                        }
                        if (item.getItemId() == 2) {
                            listener.onDelete(room);
                            return true;
                        }
                        return false;
                    });
                    pm.show();
                });
            }
        }

        holder.btnCreateContract.setText(trong ? holder.itemView.getContext().getString(R.string.room_create_contract)
                : holder.itemView.getContext().getString(R.string.room_view_contract));
        holder.btnCreateContract.setOnClickListener(v -> listener.onCreateContract(room));
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoomNumber, tvRentAmount, tvStatus, tvTenant;
        ImageView btnMore, ivRoomImage;
        com.google.android.material.button.MaterialButton btnCreateContract;

        ViewHolder(View v) {
            super(v);
            ivRoomImage = v.findViewById(R.id.ivRoomImage);
            tvRoomNumber = v.findViewById(R.id.tvSoPhong);
            tvRentAmount = v.findViewById(R.id.tvGiaThue);
            tvStatus = v.findViewById(R.id.tvTrangThai);
            tvTenant = v.findViewById(R.id.tvTenant);
            btnMore = v.findViewById(R.id.btnMore);
            btnCreateContract = v.findViewById(R.id.btnCreateContract);
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
}
