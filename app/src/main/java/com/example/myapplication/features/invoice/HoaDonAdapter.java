package com.example.myapplication.features.invoice;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.core.constants.InvoiceStatus;
import com.example.myapplication.R;
import com.example.myapplication.core.util.MoneyFormatter;
import com.example.myapplication.domain.HoaDon;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class HoaDonAdapter extends RecyclerView.Adapter<HoaDonAdapter.ViewHolder> {

    private List<HoaDon> danhSach = new ArrayList<>();
    private final OnItemActionListener listener;
    private boolean readOnly;

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

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
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

        holder.tvPhong.setText(h.getSoPhong() != null ? h.getSoPhong() : "P.???");

        // In real app, tenant name would be in the model. Using placeholder to match
        // UI.
        holder.tvTenantName.setText("Người thuê: Đang cập nhật");

        holder.tvPriceMonth.setText("Giá: " + MoneyFormatter.format(h.getGiaThue()) + "/tháng");

        holder.tvReportDate.setText("Tháng: " + h.getThangNam());

        String st = h.getTrangThai();
        if (st == null || st.trim().isEmpty())
            st = InvoiceStatus.UNPAID;

        if (holder.tvRibbonStatus != null) {
            holder.tvRibbonStatus.setText(st);
            if (InvoiceStatus.PAID.equals(st)) {
                holder.tvRibbonStatus.setBackgroundColor(0xFF4CAF50); // Green
            } else if (InvoiceStatus.PARTIAL.equals(st)) {
                holder.tvRibbonStatus.setBackgroundColor(0xFFFF9800); // Orange
            } else {
                holder.tvRibbonStatus.setBackgroundColor(0xFFFFEB3B); // Yellow
            }
        }

        // Action visibility for read-only (Tenant)
        holder.btnCancelInvoice.setVisibility(readOnly ? View.GONE : View.VISIBLE);
        holder.btnConfirmPayment.setVisibility(readOnly ? View.GONE : View.VISIBLE);

        holder.btnConfirmPayment.setOnClickListener(v -> {
            if (!readOnly)
                listener.onDoiTrangThai(h);
        });

        holder.btnCancelInvoice.setOnClickListener(v -> {
            if (!readOnly)
                listener.onXoa(h);
        });

        holder.btnViewInvoice.setOnClickListener(v -> listener.onXuat(h));

        holder.btnViewDetails.setOnClickListener(v -> {
            // Logic to show fees details
            listener.onXuat(h);
        });
    }

    @Override
    public int getItemCount() {
        return danhSach.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPhong, tvTenantName, tvPriceMonth, tvReportDate, tvRibbonStatus;
        MaterialButton btnViewDetails, btnViewInvoice, btnCancelInvoice, btnConfirmPayment;

        ViewHolder(View v) {
            super(v);
            tvPhong = v.findViewById(R.id.tvPhong);
            tvTenantName = v.findViewById(R.id.tvTenantName);
            tvPriceMonth = v.findViewById(R.id.tvPriceMonth);
            tvReportDate = v.findViewById(R.id.tvReportDate);
            tvRibbonStatus = v.findViewById(R.id.tvRibbonStatus);

            btnViewDetails = v.findViewById(R.id.btnViewDetails);
            btnViewInvoice = v.findViewById(R.id.btnViewInvoice);
            btnCancelInvoice = v.findViewById(R.id.btnCancelInvoice);
            btnConfirmPayment = v.findViewById(R.id.btnConfirmPayment);
        }
    }
}
