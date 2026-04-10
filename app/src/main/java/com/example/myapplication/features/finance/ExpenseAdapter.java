package com.example.myapplication.features.finance;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.constants.ExpenseStatus;
import com.example.myapplication.core.util.MoneyFormatter;
import com.example.myapplication.domain.Expense;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ViewHolder> {

    public static final int TAB_PENDING = 0;
    public static final int TAB_PAID = 1;

    public interface OnItemActionListener {
        void onEdit(Expense item);

        void onDelete(Expense item);

        void onMarkPaid(Expense item);
    }

    private List<Expense> dataList = new ArrayList<>();
    private final OnItemActionListener listener;
    private boolean readOnly;
    private int currentTab = TAB_PENDING;
    private Map<String, String> houseNameMap = new HashMap<>();

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

    public void setCurrentTab(int currentTab) {
        this.currentTab = currentTab;
        notifyDataSetChanged();
    }

    public void setHouseNameMap(Map<String, String> houseNameMap) {
        this.houseNameMap = houseNameMap != null ? houseNameMap : new HashMap<>();
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

        String status = item.getStatus();
        boolean isPending = ExpenseStatus.PENDING.equalsIgnoreCase(status);
        h.tvStatus.setText(isPending
            ? h.itemView.getContext().getString(R.string.expense_status_pending)
            : h.itemView.getContext().getString(R.string.expense_status_paid));
        h.tvStatus.setBackgroundResource(isPending ? R.drawable.bg_chip_light_orange : R.drawable.bg_chip_light_blue);

        String period = item.getPeriodMonth() != null ? item.getPeriodMonth().trim() : "";
        if (period.isEmpty()) {
            period = h.itemView.getContext().getString(R.string.month) + "--";
        } else {
            period = h.itemView.getContext().getString(R.string.month) + period;
        }
        h.tvPeriod.setText(period);

        String date = item.getPaidAt() != null ? item.getPaidAt() : "";
        h.tvDate.setText(date);

        String houseId = item.getHouseId() != null ? item.getHouseId().trim() : "";
        String houseName = houseNameMap.get(houseId);
        if (houseName == null || houseName.trim().isEmpty()) {
            houseName = h.itemView.getContext().getString(R.string.expense_house_unknown);
        }
        h.tvHouse.setText(h.itemView.getContext().getString(R.string.expense_house_label, houseName));

        String note = item.getNote() != null ? item.getNote().trim() : "";
        h.tvNote.setVisibility(note.isEmpty() ? View.GONE : View.VISIBLE);
        h.tvNote.setText(note);

        h.btnEdit.setVisibility(readOnly ? View.GONE : View.VISIBLE);
        h.btnDelete.setVisibility(readOnly ? View.GONE : View.VISIBLE);
        h.btnMarkPaid.setVisibility(!readOnly && currentTab == TAB_PENDING ? View.VISIBLE : View.GONE);

        h.btnEdit.setOnClickListener(v -> {
            if (!readOnly)
                listener.onEdit(item);
        });
        h.btnDelete.setOnClickListener(v -> {
            if (!readOnly)
                listener.onDelete(item);
        });
        h.btnMarkPaid.setOnClickListener(v -> {
            if (!readOnly)
                listener.onMarkPaid(item);
        });
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvAmount, tvStatus, tvPeriod, tvDate, tvHouse, tvNote;
        com.google.android.material.button.MaterialButton btnMarkPaid;
        ImageButton btnEdit, btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvPeriod = itemView.findViewById(R.id.tvPeriod);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvHouse = itemView.findViewById(R.id.tvHouse);
            tvNote = itemView.findViewById(R.id.tvNote);
            btnMarkPaid = itemView.findViewById(R.id.btnMarkPaid);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
