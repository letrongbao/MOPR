package com.example.myapplication.features.invoice;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.constants.InvoiceStatus;
import com.example.myapplication.core.util.MoneyFormatter;
import com.example.myapplication.domain.Invoice;

import java.util.ArrayList;
import java.util.List;

public class TenantInvoiceAdapter extends RecyclerView.Adapter<TenantInvoiceAdapter.ViewHolder> {

    private final List<Invoice> data = new ArrayList<>();

    public void submitList(List<Invoice> invoices) {
        data.clear();
        if (invoices != null) {
            data.addAll(invoices);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tenant_invoice, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Invoice invoice = data.get(position);
        holder.tvInvoicePeriod.setText(holder.itemView.getContext().getString(
                R.string.tenant_invoice_period, safe(invoice.getBillingPeriod(), "--/----")));

        holder.tvInvoiceStatus.setText(holder.itemView.getContext().getString(
                R.string.tenant_invoice_status, toDisplayStatus(holder, invoice.getStatus())));

        holder.tvInvoiceAmount.setText(MoneyFormatter.format(invoice.getTotalAmount()) + " ₫");
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    private String toDisplayStatus(@NonNull ViewHolder holder, String status) {
        String normalized = status == null ? InvoiceStatus.UNREPORTED : status.trim();
        if (InvoiceStatus.PAID.equalsIgnoreCase(normalized)) {
            return holder.itemView.getContext().getString(R.string.invoice_status_paid);
        }
        if (InvoiceStatus.REPORTED.equalsIgnoreCase(normalized) || "PARTIAL".equalsIgnoreCase(normalized)) {
            return holder.itemView.getContext().getString(R.string.invoice_status_reported);
        }
        return holder.itemView.getContext().getString(R.string.invoice_status_unreported);
    }

    private String safe(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvInvoicePeriod;
        final TextView tvInvoiceStatus;
        final TextView tvInvoiceAmount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInvoicePeriod = itemView.findViewById(R.id.tvInvoicePeriod);
            tvInvoiceStatus = itemView.findViewById(R.id.tvInvoiceStatus);
            tvInvoiceAmount = itemView.findViewById(R.id.tvInvoiceAmount);
        }
    }
}
