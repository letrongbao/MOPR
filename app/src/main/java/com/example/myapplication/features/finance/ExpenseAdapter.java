package com.example.myapplication.features.finance;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.util.MoneyFormatter;
import com.example.myapplication.domain.Expense;

import java.util.ArrayList;
import java.util.List;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ViewHolder> {

    public interface OnItemActionListener {
        void onEdit(Expense item);

        void onDelete(Expense item);
    }

    private List<Expense> dataList = new ArrayList<>();
    private final OnItemActionListener listener;
    private boolean readOnly;

    public ExpenseAdapter(OnItemActionListener listener) {
        this.listener = listener;
    }

    public void setDataList(List<Expense> list) {
        this.dataList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Expense item = dataList.get(position);
        String cat = item.getCategory() != null
            ? item.getCategory()
            : h.itemView.getContext().getString(R.string.expense_uncategorized);
        h.tvCategory.setText(cat);

        h.tvAmount.setText(MoneyFormatter.format(item.getAmount()));

        String date = item.getPaidAt() != null ? item.getPaidAt() : "";
        h.tvDate.setText(date);

        String note = item.getNote() != null ? item.getNote().trim() : "";
        h.tvNote.setVisibility(note.isEmpty() ? View.GONE : View.VISIBLE);
        h.tvNote.setText(note);

        h.btnEdit.setVisibility(readOnly ? View.GONE : View.VISIBLE);
        h.btnDelete.setVisibility(readOnly ? View.GONE : View.VISIBLE);

        h.btnEdit.setOnClickListener(v -> {
            if (!readOnly)
                listener.onEdit(item);
        });
        h.btnDelete.setOnClickListener(v -> {
            if (!readOnly)
                listener.onDelete(item);
        });
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvAmount, tvDate, tvNote;
        ImageButton btnEdit, btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvNote = itemView.findViewById(R.id.tvNote);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}

