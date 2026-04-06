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
import com.example.myapplication.domain.Invoice;
import com.example.myapplication.core.util.MoneyFormatter;
import com.google.android.material.button.MaterialButton;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InvoiceAdapter extends RecyclerView.Adapter<InvoiceAdapter.ViewHolder> {

    private List<Invoice> dataList = new ArrayList<>();
    private final OnItemActionListener listener;
    private boolean tenantMode;
    private Map<String, String> tenantDisplayByRoom = new HashMap<>();
    private int currentTab = 0;

    public interface OnItemActionListener {
        void onDelete(Invoice invoice);

        void onBaoPhi(Invoice invoice);

        void onDoiTrangThai(Invoice invoice);

        void onSua(Invoice invoice);

        void onXuat(Invoice invoice);
    }

    public InvoiceAdapter(OnItemActionListener listener) {
        this.listener = listener;
    }

    public void setDataList(List<Invoice> list) {
        this.dataList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setTenantDisplayByRoom(Map<String, String> tenantDisplayByRoom) {
        this.tenantDisplayByRoom = tenantDisplayByRoom != null ? tenantDisplayByRoom : new HashMap<>();
        notifyDataSetChanged();
    }

    public void setTenantMode(boolean tenantMode) {
        this.tenantMode = tenantMode;
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
                .inflate(R.layout.item_invoice, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Invoice h = dataList.get(position);

        holder.tvPhong.setText(h.getRoomNumber() != null ? ("P." + h.getRoomNumber()) : "P.???");

        String tenantDisplay = tenantDisplayByRoom.get(h.getRoomId());
        if (tenantDisplay == null || tenantDisplay.trim().isEmpty()) {
            tenantDisplay = holder.itemView.getContext().getString(R.string.tenant_colon) + holder.itemView.getContext().getString(R.string.updating);
        }
        holder.tvTenantName.setText(tenantDisplay);

        holder.tvPriceMonth.setText(holder.itemView.getContext().getString(R.string.price_colon) + MoneyFormatter.format(h.getRentAmount()) + holder.itemView.getContext().getString(R.string.per_month));

        holder.tvReportDate.setText(holder.itemView.getContext().getString(R.string.month_colon) + h.getBillingPeriod());

        // Status ribbon
        String st = h.getStatus();
        if (st == null || st.trim().isEmpty())
            st = InvoiceStatus.UNREPORTED;

        if (holder.tvRibbonStatus != null) {
            holder.tvRibbonStatus.setText(st);
            if (InvoiceStatus.PAID.equals(st)) {
                holder.tvRibbonStatus
                        .setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.success));
            } else if (InvoiceStatus.PARTIAL.equals(st)) {
                holder.tvRibbonStatus
                        .setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.btn_orange));
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

        if (tenantMode) {
            if (InvoiceStatus.UNREPORTED.equals(st)) {
                buttonText = holder.itemView.getContext().getString(R.string.waiting_owner_report);
                buttonColor = R.color.warning;
                buttonAction = null;
            } else {
                buttonText = (InvoiceStatus.PAID.equals(st) || InvoiceStatus.PARTIAL.equals(st))
                        ? holder.itemView.getContext().getString(R.string.view_payment_history)
                        : holder.itemView.getContext().getString(R.string.pay_now);
                buttonColor = InvoiceStatus.PAID.equals(st) ? R.color.success : R.color.btn_blue_action;
                buttonAction = v -> listener.onDoiTrangThai(h);
            }

            holder.btnMainAction.setText(buttonText);
            holder.btnMainAction.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), buttonColor));
            holder.btnMainAction.setVisibility(View.VISIBLE);
            holder.btnMainAction.setEnabled(buttonAction != null);
            holder.btnMainAction.setOnClickListener(buttonAction);
            return;
        }

        if (currentTab == 0) {
            // "Not Notified" tab
            String month = h.getBillingPeriod() != null ? h.getBillingPeriod() : "X";
            buttonText = holder.itemView.getContext().getString(R.string.report_fee_month, month);
            buttonColor = R.color.purple_500;
            buttonAction = v -> listener.onBaoPhi(h);
        } else if (currentTab == 1) {
            // "Notified" tab
            buttonText = holder.itemView.getContext().getString(R.string.confirm_collected_fee);
            buttonColor = R.color.btn_orange;
            buttonAction = v -> listener.onDoiTrangThai(h);
        } else if (currentTab == 2) {
            // "Partially Paid" tab
            buttonText = holder.itemView.getContext().getString(R.string.view_payment_history);
            buttonColor = R.color.btn_blue_action;
            buttonAction = v -> listener.onDoiTrangThai(h);
        } else {
            // "Paid" tab
            buttonText = holder.itemView.getContext().getString(R.string.view_payment_history);
            buttonColor = R.color.success;
            buttonAction = v -> listener.onDoiTrangThai(h);
        }

        holder.btnMainAction.setText(buttonText);
        holder.btnMainAction.setBackgroundColor(
                ContextCompat.getColor(holder.itemView.getContext(), buttonColor));
        holder.btnMainAction.setOnClickListener(buttonAction);
        holder.btnMainAction.setVisibility(View.VISIBLE);
        holder.btnMainAction.setEnabled(true);
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPhong, tvTenantName, tvPriceMonth, tvReportDate, tvRibbonStatus, tvViewReportedFees;
        MaterialButton btnMainAction;

        ViewHolder(View v) {
            super(v);
            tvPhong = v.findViewById(R.id.tvPhong);
            tvTenantName = v.findViewById(R.id.tvTenantName);
            tvPriceMonth = v.findViewById(R.id.tvPriceMonth);
            tvReportDate = v.findViewById(R.id.tvReportDate);
            tvRibbonStatus = v.findViewById(R.id.tvRibbonStatus);
            tvViewReportedFees = v.findViewById(R.id.tvViewReportedFees);
            btnMainAction = v.findViewById(R.id.btnMainAction);
        }
    }
}


