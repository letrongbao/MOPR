package com.example.myapplication;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.model.HoaDon;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HoaDonAdapter extends RecyclerView.Adapter<HoaDonAdapter.ViewHolder> {

    private List<HoaDon> danhSach = new ArrayList<>();
    private final OnItemActionListener listener;

    public interface OnItemActionListener {
        void onXoa(HoaDon hoaDon);
        void onDoiTrangThai(HoaDon hoaDon);
        void onSua(HoaDon hoaDon);
        void onXuat(HoaDon hoaDon);
    }

    public HoaDonAdapter(OnItemActionListener listener) {
        this.listener = listener;
    }

    public void setDanhSach(List<HoaDon> list) {
        this.danhSach = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hoa_don, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HoaDon h = danhSach.get(position);
        String tenPhong = h.getSoPhong() != null ? "Phòng " + h.getSoPhong() : "Phòng: " + h.getIdPhong();
        holder.tvPhong.setText(tenPhong);
        holder.tvThangNam.setText("Tháng: " + h.getThangNam());
        
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        holder.tvTongTien.setText(fmt.format(h.getTongTien()));

        boolean daTT = "Đã thanh toán".equals(h.getTrangThai());
        holder.tvTrangThai.setText(h.getTrangThai());
        
        GradientDrawable badge = new GradientDrawable();
        badge.setShape(GradientDrawable.RECTANGLE);
        badge.setCornerRadius(24f);
        badge.setColor(Color.parseColor(daTT ? "#E8F5E9" : "#FFEBEE"));
        holder.tvTrangThai.setBackground(badge);
        holder.tvTrangThai.setTextColor(Color.parseColor(daTT ? "#2E7D32" : "#C62828"));

        holder.tvTrangThai.setOnClickListener(v -> listener.onDoiTrangThai(h));
        holder.btnSua.setOnClickListener(v -> listener.onSua(h));
        holder.btnXoa.setOnClickListener(v -> listener.onXoa(h));
        holder.btnXuat.setOnClickListener(v -> listener.onXuat(h));
    }

    @Override
    public int getItemCount() {
        return danhSach.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPhong, tvThangNam, tvTongTien, tvTrangThai;
        ImageButton btnXoa, btnSua, btnXuat;

        ViewHolder(View v) {
            super(v);
            tvPhong = v.findViewById(R.id.tvPhong);
            tvThangNam = v.findViewById(R.id.tvThangNam);
            tvTongTien = v.findViewById(R.id.tvTongTien);
            tvTrangThai = v.findViewById(R.id.tvTrangThai);
            btnXoa = v.findViewById(R.id.btnXoa);
            btnSua = v.findViewById(R.id.btnSua);
            btnXuat = v.findViewById(R.id.btnXuat);
        }
    }
}