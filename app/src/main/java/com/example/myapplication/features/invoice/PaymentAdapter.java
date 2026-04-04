package com.example.myapplication.features.invoice;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.util.MoneyFormatter;
import com.example.myapplication.domain.Payment;

import java.util.ArrayList;
import java.util.List;

public class PaymentAdapter extends RecyclerView.Adapter<PaymentAdapter.ViewHolder> {

    public interface OnActionListener {
        void onDelete(Payment payment);
    }

    private List<Payment> dataList = new ArrayList<>();
    private final OnActionListener listener;
    private boolean allowDelete = true;

    public PaymentAdapter(OnActionListener listener) {
        this.listener = listener;
    }

    public void setDataList(List<Payment> list) {
        this.dataList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setAllowDelete(boolean allowDelete) {
        this.allowDelete = allowDelete;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_payment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Payment p = dataList.get(position);

        holder.tvDate.setText(holder.itemView.getContext().getString(R.string.date_colon) + safe(p.getPaidAt()));
        holder.tvMethod.setText(holder.itemView.getContext().getString(R.string.method_colon) + methodLabel(p.getMethod(), holder.itemView.getContext()));
        holder.tvAmount.setText(fmtMoney(p.getAmount()));

        String note = safe(p.getNote());
        holder.tvNote.setVisibility(note.isEmpty() ? View.GONE : View.VISIBLE);
        holder.tvNote.setText(note);

        holder.btnDelete.setVisibility(allowDelete ? View.VISIBLE : View.GONE);
        if (allowDelete) {
            holder.btnDelete.setOnClickListener(v -> listener.onDelete(p));
        } else {
            holder.btnDelete.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvMethod, tvAmount, tvNote;
        ImageButton btnDelete;

        ViewHolder(View v) {
            super(v);
            tvDate = v.findViewById(R.id.tvDate);
            tvMethod = v.findViewById(R.id.tvMethod);
            tvAmount = v.findViewById(R.id.tvAmount);
            tvNote = v.findViewById(R.id.tvNote);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String methodLabel(String method, android.content.Context context) {
        if (method == null)
            return "";
        if ("BANK".equalsIgnoreCase(method))
            return context.getString(R.string.bank_transfer);
        if ("CASH".equalsIgnoreCase(method))
            return context.getString(R.string.cash);
        return method;
    }

    private static String fmtMoney(double v) {
        return MoneyFormatter.format(v);
    }
}
