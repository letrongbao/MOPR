package com.example.myapplication.features.invoice;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.core.constants.InvoiceStatus;
import com.example.myapplication.R;
import com.example.myapplication.domain.HoaDon;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class HoaDonAdapter extends RecyclerView.Adapter<HoaDonAdapter.ViewHolder> {

    private List<HoaDon> danhSach = new ArrayList<>();
    private final OnItemActionListener listener;
    private boolean readOnly;
    private int currentTab = 0;

    public interface OnItemActionListener {
        void onXoa(HoaDon hoaDon);

        void onBaoPhi(HoaDon hoaDon);

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

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        notifyDataSetChanged();
    }

    public void setCurrentTab(int tab) {
        this.currentTab = tab;
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

        holder.tvPhong.setText(h.getSoPhong() != null ? ("P." + h.getSoPhong()) : "P.???");

        // Tenant info
        holder.tvTenantName.setText("Người thuê: Đang cập nhật - ĐT: 0000000000");

        // Status ribbon
        String st = h.getTrangThai();
        if (st == null || st.trim().isEmpty())
            st = InvoiceStatus.UNREPORTED;

        if (holder.tvRibbonStatus != null) {
            holder.tvRibbonStatus.setText(st);
            if (InvoiceStatus.PAID.equals(st)) {
                holder.tvRibbonStatus.setBackgroundColor(
                        ContextCompat.getColor(holder.itemView.getContext(), R.color.success));
            } else if (InvoiceStatus.PARTIAL.equals(st)) {
                holder.tvRibbonStatus.setBackgroundColor(
                        ContextCompat.getColor(holder.itemView.getContext(), R.color.btn_orange));
            } else if (InvoiceStatus.REPORTED.equals(st)) {
                holder.tvRibbonStatus.setBackgroundColor(
                        ContextCompat.getColor(holder.itemView.getContext(), R.color.btn_blue_action));
            } else {
                holder.tvRibbonStatus.setBackgroundColor(
                        ContextCompat.getColor(holder.itemView.getContext(), R.color.warning));
            }
        }

        // View reported fees link
        holder.tvViewReportedFees.setOnClickListener(v -> listener.onXuat(h));

        // Main action button changes based on tab
        String buttonText;
        int buttonColor;
        View.OnClickListener buttonAction;

        if (currentTab == 0) {
            // Tab "Chưa báo"
            String month = h.getThangNam() != null ? h.getThangNam() : "X";
            buttonText = "Báo phí tháng " + month + " cho khách";
            buttonColor = R.color.purple_500;
            buttonAction = v -> listener.onBaoPhi(h);
        } else if (currentTab == 1) {
            // Tab "Đã báo"
            buttonText = "Xác nhận đã thu phí";
            buttonColor = R.color.btn_orange;
            buttonAction = v -> listener.onDoiTrangThai(h);
        } else if (currentTab == 2) {
            // Tab "Đóng một phần"
            buttonText = "Xem lịch sử thanh toán";
            buttonColor = R.color.btn_blue_action;
            buttonAction = v -> listener.onDoiTrangThai(h);
        } else {
            // Tab "Đã đóng"
            buttonText = "Xem lịch sử thanh toán";
            buttonColor = R.color.success;
            buttonAction = v -> listener.onDoiTrangThai(h);
        }

        holder.btnMainAction.setText(buttonText);
        holder.btnMainAction.setBackgroundColor(
                ContextCompat.getColor(holder.itemView.getContext(), buttonColor));
        holder.btnMainAction.setOnClickListener(buttonAction);

        // Hide button for read-only users
        holder.btnMainAction.setVisibility(readOnly ? View.GONE : View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return danhSach.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPhong, tvTenantName, tvRibbonStatus, tvViewReportedFees;
        MaterialButton btnMainAction;

        ViewHolder(View v) {
            super(v);
            tvPhong = v.findViewById(R.id.tvPhong);
            tvTenantName = v.findViewById(R.id.tvTenantName);
            tvRibbonStatus = v.findViewById(R.id.tvRibbonStatus);
            tvViewReportedFees = v.findViewById(R.id.tvViewReportedFees);
            btnMainAction = v.findViewById(R.id.btnMainAction);
        }
    }
}
