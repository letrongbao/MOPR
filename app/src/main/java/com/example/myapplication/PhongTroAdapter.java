package com.example.myapplication;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.myapplication.model.PhongTro;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PhongTroAdapter extends RecyclerView.Adapter<PhongTroAdapter.ViewHolder> {

    private List<PhongTro> danhSach = new ArrayList<>();
    private final OnItemActionListener listener;

    public interface OnItemActionListener {
        void onXoa(PhongTro phong);
        void onChon(PhongTro phong);
        void onXemChiTiet(PhongTro phong);
    }

    public PhongTroAdapter(OnItemActionListener listener) {
        this.listener = listener;
    }

    public void setDanhSach(List<PhongTro> list) {
        this.danhSach = list;
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
        holder.tvSoPhong.setText("Phòng " + phong.getSoPhong());
        String khu = phong.getKhuTen();
        String line = phong.getLoaiPhong() + " • " + (int) phong.getDienTich() + "m²";
        if (khu != null && !khu.trim().isEmpty() && !"(Không chọn)".equals(khu)) {
            line = khu + " • " + line;
        }
        holder.tvLoaiPhong.setText(line);
        NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        holder.tvGiaThue.setText(fmt.format(phong.getGiaThue()) + " đ/tháng");

        boolean trong = "Trống".equals(phong.getTrangThai());
        int color = Color.parseColor(trong ? "#4CAF50" : "#F44336");
        holder.tvTrangThai.setText(phong.getTrangThai());
        GradientDrawable badge = new GradientDrawable();
        badge.setShape(GradientDrawable.RECTANGLE);
        badge.setCornerRadius(32f);
        badge.setColor(color);
        holder.tvTrangThai.setBackground(badge);
        holder.viewStatus.setBackgroundColor(color);

        // Load thumbnail
        if (phong.getHinhAnh() != null && !phong.getHinhAnh().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(phong.getHinhAnh())
                    .centerCrop()
                    .placeholder(R.drawable.baseline_home_24)
                    .into(holder.imgPhong);
        } else {
            holder.imgPhong.setImageResource(R.drawable.baseline_home_24);
        }

        holder.itemView.setOnClickListener(v -> listener.onXemChiTiet(phong));
        holder.btnXoa.setOnClickListener(v -> listener.onXoa(phong));
        holder.btnSua.setOnClickListener(v -> listener.onChon(phong));
    }

    @Override
    public int getItemCount() {
        return danhSach.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSoPhong, tvLoaiPhong, tvGiaThue, tvTrangThai;
        View viewStatus;
        ImageView imgPhong;
        ImageButton btnXoa, btnSua;

        ViewHolder(View v) {
            super(v);
            tvSoPhong = v.findViewById(R.id.tvSoPhong);
            tvLoaiPhong = v.findViewById(R.id.tvLoaiPhong);
            tvGiaThue = v.findViewById(R.id.tvGiaThue);
            tvTrangThai = v.findViewById(R.id.tvTrangThai);
            viewStatus = v.findViewById(R.id.viewStatus);
            imgPhong = v.findViewById(R.id.imgPhong);
            btnXoa = v.findViewById(R.id.btnXoa);
            btnSua = v.findViewById(R.id.btnSua);
        }
    }
}
