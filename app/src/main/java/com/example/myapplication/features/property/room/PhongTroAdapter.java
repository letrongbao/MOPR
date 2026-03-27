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
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.core.constants.RoomStatus;
import com.example.myapplication.core.util.MoneyFormatter;
import com.example.myapplication.domain.PhongTro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PhongTroAdapter extends RecyclerView.Adapter<PhongTroAdapter.ViewHolder> {

    private List<PhongTro> danhSach = new ArrayList<>();
    private Map<String, String> tenantByRoomId = new HashMap<>();
    private boolean readOnly;
    private final OnItemActionListener listener;

    public interface OnItemActionListener {
        void onXoa(PhongTro phong);

        void onChon(PhongTro phong);

        void onXemChiTiet(PhongTro phong);

        void onTaoHopDong(PhongTro phong);
    }

    public PhongTroAdapter(OnItemActionListener listener) {
        this.listener = listener;
    }

    public void setDanhSach(List<PhongTro> list) {
        this.danhSach = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setTenantByRoomId(@NonNull Map<String, String> map) {
        tenantByRoomId = map;
        notifyDataSetChanged();
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_phong_tro, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PhongTro phong = danhSach.get(position);
        holder.tvSoPhong.setText(phong.getSoPhong() != null ? "P." + phong.getSoPhong() : "P.???");

        holder.tvGiaThue.setText(MoneyFormatter.format(phong.getGiaThue()) + "/tháng");

        boolean trong = RoomStatus.VACANT.equals(phong.getTrangThai());
        int color = Color.parseColor(trong ? "#F44336" : "#2196F3"); // red=vacant, blue=rented

        holder.tvTrangThai.setText(trong ? "Đang trống" : "Đang thuê");
        holder.tvTrangThai.setTextColor(color);

        // Load room image
        if (phong.getHinhAnh() != null && !phong.getHinhAnh().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(phong.getHinhAnh())
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
        holder.tvTrangThai.setBackground(chipBg);

        String roomId = phong.getId();
        String tenant = (roomId != null) ? tenantByRoomId.get(roomId) : null;
        if (holder.tvNguoiThue != null) {
            if (!trong && tenant != null && !tenant.trim().isEmpty()) {
                holder.tvNguoiThue.setVisibility(View.VISIBLE);
                holder.tvNguoiThue.setText("Người thuê: " + tenant);
            } else {
                holder.tvNguoiThue.setVisibility(View.GONE);
            }
        }

        holder.itemView.setOnClickListener(v -> listener.onXemChiTiet(phong));

        if (holder.btnMore != null) {
            // Ẩn nút menu nếu phòng đang thuê hoặc ở chế độ readOnly
            boolean isRented = !trong;
            holder.btnMore.setVisibility((readOnly || isRented) ? View.GONE : View.VISIBLE);
            if (!readOnly && !isRented) {
                holder.btnMore.setOnClickListener(v -> {
                    PopupMenu pm = new PopupMenu(v.getContext(), v);
                    pm.getMenu().add(0, 1, 0, "Sửa phòng");
                    pm.getMenu().add(0, 2, 1, "Xóa phòng");
                    pm.setOnMenuItemClickListener(item -> {
                        if (item.getItemId() == 1) {
                            listener.onChon(phong);
                            return true;
                        }
                        if (item.getItemId() == 2) {
                            listener.onXoa(phong);
                            return true;
                        }
                        return false;
                    });
                    pm.show();
                });
            }
        }

        holder.btnCreateContract.setText(trong ? "Tạo hợp đồng" : "Xem hợp đồng");
        holder.btnCreateContract.setOnClickListener(v -> listener.onTaoHopDong(phong));
    }

    @Override
    public int getItemCount() {
        return danhSach.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSoPhong, tvGiaThue, tvTrangThai, tvNguoiThue;
        ImageView btnMore, ivRoomImage;
        com.google.android.material.button.MaterialButton btnCreateContract;

        ViewHolder(View v) {
            super(v);
            ivRoomImage = v.findViewById(R.id.ivRoomImage);
            tvSoPhong = v.findViewById(R.id.tvSoPhong);
            tvGiaThue = v.findViewById(R.id.tvGiaThue);
            tvTrangThai = v.findViewById(R.id.tvTrangThai);
            tvNguoiThue = v.findViewById(R.id.tvNguoiThue);
            btnMore = v.findViewById(R.id.btnMore);
            btnCreateContract = v.findViewById(R.id.btnCreateContract);
        }
    }

    private static int dp(@NonNull View v, int dp) {
        float d = v.getResources().getDisplayMetrics().density;
        return (int) (dp * d);
    }
}
