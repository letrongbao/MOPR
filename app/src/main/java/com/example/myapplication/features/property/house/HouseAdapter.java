package com.example.myapplication.features.property.house;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.constants.WaterCalculationMode;
import com.example.myapplication.core.util.MoneyFormatter;
import com.example.myapplication.domain.House;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HouseAdapter extends RecyclerView.Adapter<HouseAdapter.VH> {

    public interface OnItemAction {
        void onEdit(@NonNull House item);

        void onDelete(@NonNull House item);

        void onViewRooms(@NonNull House item);
    }

    private final List<House> items = new ArrayList<>();
    private final OnItemAction actionCb;
    private final Set<String> expandedKeys = new HashSet<>();
    private boolean readOnly;

    public HouseAdapter(@NonNull OnItemAction actionCb) {
        this.actionCb = actionCb;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        notifyDataSetChanged();
    }

    public void setItems(List<House> newItems) {
        items.clear();
        if (newItems != null)
            items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_house, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        House k = items.get(position);
        android.content.Context context = h.itemView.getContext();

        String addr = k.getAddress() != null && !k.getAddress().trim().isEmpty()
                ? k.getAddress()
                : context.getString(R.string.house_no_address);
        h.tvHouseAddr.setText(addr);

        String manager = k.getHouseName() != null ? k.getHouseName().trim() : "";
        String phone = k.getManagerPhone() != null ? k.getManagerPhone().trim() : "";
        String managerDisplay = manager.isEmpty() ? context.getString(R.string.house_no_manager_name) : manager;
        h.tvHouseName.setText(phone.isEmpty() ? managerDisplay : (managerDisplay + "  •  " + phone));

        // Bank info (fallback to manager name)
        String chuTk = (k.getBankAccountName() != null && !k.getBankAccountName().trim().isEmpty())
                ? k.getBankAccountName().trim()
                : manager;
        String bankName = k.getBankName() != null ? k.getBankName().trim() : "";
        String soTk = k.getBankAccountNo() != null ? k.getBankAccountNo().trim() : "";

        StringBuilder bank = new StringBuilder();
        if (!chuTk.isEmpty())
            bank.append(context.getString(R.string.bank_account_owner_prefix)).append(chuTk);
        if (!bankName.isEmpty())
            bank.append(bank.length() > 0 ? "\n" : "").append(context.getString(R.string.bank_name_prefix))
                    .append(bankName);
        if (!soTk.isEmpty())
            bank.append(bank.length() > 0 ? "\n" : "").append(context.getString(R.string.bank_account_number_prefix))
                    .append(soTk);
        if (bank.length() == 0) {
            bank.append(context.getString(R.string.bank_account_owner_prefix))
                    .append(manager.isEmpty() ? context.getString(R.string.landlord_fallback) : manager)
                    .append("\n")
                    .append(context.getString(R.string.bank_name_line_fallback))
                    .append("\n")
                    .append(context.getString(R.string.bank_account_number_line_fallback));
        }
        h.tvBankInfo.setText(bank);

        // Fee values
        h.tvFeeDien.setText(formatVnd(k.getElectricityPrice()));
        h.tvFeeNuoc.setText(formatVnd(k.getWaterPrice()));
        h.tvFeeXe.setText(formatVnd(k.getParkingPrice()));
        h.tvFeeInternet.setText(formatVnd(k.getInternetPrice()));
        h.tvFeeGiatSay.setText(formatVnd(k.getLaundryPrice()));
        h.tvFeeThangMay.setText(formatVnd(k.getElevatorPrice()));
        h.tvFeeTiviCap.setText(formatVnd(k.getCableTvPrice()));
        h.tvFeeRac.setText(formatVnd(k.getTrashPrice()));
        h.tvFeeDichVu.setText(formatVnd(k.getServicePrice()));

        String nuocUnit = context.getString(R.string.item_house_unit_room);
        String mode = k.getWaterCalculationMethod();
        if (mode != null) {
            if (WaterCalculationMode.isPerPerson(mode))
                nuocUnit = context.getString(R.string.unit_person);
            else if (WaterCalculationMode.isMeter(mode))
                nuocUnit = context.getString(R.string.unit_cubic_meter);
        }
        h.tvFeeNuocUnit.setText(nuocUnit);

        // Expand/collapse
        String key = k.getId() != null ? k.getId() : ("pos_" + position);
        boolean expanded = expandedKeys.contains(key);
        h.llFeeTable.setVisibility(expanded ? View.VISIBLE : View.GONE);
        h.ivFeesExpand.setImageResource(expanded ? R.drawable.ic_expand_less : R.drawable.ic_expand_more);
        h.llToggleFees.setOnClickListener(v -> {
            int p = h.getBindingAdapterPosition();
            if (p == RecyclerView.NO_POSITION)
                return;
            String kk = items.get(p).getId() != null ? items.get(p).getId() : ("pos_" + p);
            if (expandedKeys.contains(kk))
                expandedKeys.remove(kk);
            else
                expandedKeys.add(kk);
            notifyItemChanged(p);
        });

        // Navigation
        View.OnClickListener openRooms = v -> actionCb.onViewRooms(k);
        h.headerRoot.setOnClickListener(openRooms);
        h.llRoomsAction.setOnClickListener(openRooms);

        // Actions
        h.llEditAction.setVisibility(readOnly ? View.GONE : View.VISIBLE);
        h.btnMore.setVisibility(readOnly ? View.GONE : View.VISIBLE);
        h.llEditAction.setOnClickListener(v -> actionCb.onEdit(k));
        h.btnMore.setOnClickListener(v -> {
            PopupMenu pm = new PopupMenu(v.getContext(), v);
            pm.getMenu().add(0, 1, 0, context.getString(R.string.house_delete_menu));
            pm.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) {
                    actionCb.onDelete(k);
                    return true;
                }
                return false;
            });
            pm.show();
        });
    }

    private String formatVnd(double value) {
        if (value <= 0)
            return MoneyFormatter.format(0);
        return MoneyFormatter.format(value);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        View headerRoot;
        TextView tvHouseAddr, tvHouseName, tvBankInfo;
        LinearLayout llToggleFees, llFeeTable;
        ImageView ivFeesExpand;
        TextView tvFeeDien, tvFeeNuoc, tvFeeNuocUnit, tvFeeXe, tvFeeInternet, tvFeeGiatSay, tvFeeThangMay, tvFeeTiviCap,
                tvFeeRac, tvFeeDichVu;
        View llEditAction, llRoomsAction;
        ImageView btnMore;

        VH(@NonNull View itemView) {
            super(itemView);
            headerRoot = itemView.findViewById(R.id.headerRoot);
            tvHouseAddr = itemView.findViewById(R.id.tvHouseAddr);
            tvHouseName = itemView.findViewById(R.id.tvHouseName);
            tvBankInfo = itemView.findViewById(R.id.tvBankInfo);

            llToggleFees = itemView.findViewById(R.id.llToggleFees);
            llFeeTable = itemView.findViewById(R.id.llFeeTable);
            ivFeesExpand = itemView.findViewById(R.id.ivFeesExpand);

            tvFeeDien = itemView.findViewById(R.id.tvFeeDien);
            tvFeeNuoc = itemView.findViewById(R.id.tvFeeNuoc);
            tvFeeNuocUnit = itemView.findViewById(R.id.tvFeeNuocUnit);
            tvFeeXe = itemView.findViewById(R.id.tvFeeXe);
            tvFeeInternet = itemView.findViewById(R.id.tvFeeInternet);
            tvFeeGiatSay = itemView.findViewById(R.id.tvFeeGiatSay);
            tvFeeThangMay = itemView.findViewById(R.id.tvFeeThangMay);
            tvFeeTiviCap = itemView.findViewById(R.id.tvFeeTiviCap);
            tvFeeRac = itemView.findViewById(R.id.tvFeeRac);
            tvFeeDichVu = itemView.findViewById(R.id.tvFeeDichVu);

            llEditAction = itemView.findViewById(R.id.llEditAction);
            llRoomsAction = itemView.findViewById(R.id.llRoomsAction);
            btnMore = itemView.findViewById(R.id.btnMore);
        }
    }
}
