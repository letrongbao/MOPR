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

    private List<Payment> danhSach = new ArrayList<>();
    private final OnActionListener listener;

    public PaymentAdapter(OnActionListener listener) {
        this.listener = listener;
    }

    public void setDanhSach(List<Payment> list) {
        this.danhSach = list;
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
        Payment p = danhSach.get(position);

        holder.tvDate.setText("Ngày: " + safe(p.getPaidAt()));
        holder.tvMethod.setText("Hình thức: " + methodLabel(p.getMethod()));
        holder.tvAmount.setText(fmtMoney(p.getAmount()));

        String note = safe(p.getNote());
        holder.tvNote.setVisibility(note.isEmpty() ? View.GONE : View.VISIBLE);
        holder.tvNote.setText(note);

        holder.btnXoa.setOnClickListener(v -> listener.onDelete(p));
    }

    @Override
    public int getItemCount() {
        return danhSach.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvMethod, tvAmount, tvNote;
        ImageButton btnXoa;

        ViewHolder(View v) {
            super(v);
            tvDate = v.findViewById(R.id.tvDate);
            tvMethod = v.findViewById(R.id.tvMethod);
            tvAmount = v.findViewById(R.id.tvAmount);
            tvNote = v.findViewById(R.id.tvNote);
            btnXoa = v.findViewById(R.id.btnXoa);
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String methodLabel(String method) {
        if (method == null)
            return "";
        if ("BANK".equalsIgnoreCase(method))
            return "Chuyển khoản";
        if ("CASH".equalsIgnoreCase(method))
            return "Tiền mặt";
        return method;
    }

    private static String fmtMoney(double v) {
        return MoneyFormatter.format(v);
    }
}
