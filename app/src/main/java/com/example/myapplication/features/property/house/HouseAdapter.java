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
        boolean hasAnyStandardFee = false;
        hasAnyStandardFee |= bindFeeRow(h.rowFeeDien, h.tvFeeDien, k.getElectricityPrice());
        hasAnyStandardFee |= bindFeeRow(h.rowFeeNuoc, h.tvFeeNuoc, k.getWaterPrice());
        hasAnyStandardFee |= bindFeeRow(h.rowFeeXe, h.tvFeeXe, k.getParkingPrice());
        hasAnyStandardFee |= bindFeeRow(h.rowFeeInternet, h.tvFeeInternet, k.getInternetPrice());
        hasAnyStandardFee |= bindFeeRow(h.rowFeeGiatSay, h.tvFeeGiatSay, k.getLaundryPrice());
        hasAnyStandardFee |= bindFeeRow(h.rowFeeRac, h.tvFeeRac, k.getTrashPrice());

        h.tvFeeDienUnit.setText(getElectricityUnitLabel(context, k.getElectricityCalculationMethod()));
        String nuocUnit = context.getString(R.string.item_house_unit_room);
        String mode = k.getWaterCalculationMethod();
        if (mode != null) {
            if (WaterCalculationMode.isPerPerson(mode))
                nuocUnit = context.getString(R.string.unit_person);
            else if (WaterCalculationMode.isMeter(mode))
                nuocUnit = context.getString(R.string.unit_cubic_meter);
        }
        h.tvFeeNuocUnit.setText(nuocUnit);
        h.tvFeeXeUnit.setText(getParkingUnitLabel(context, k.getParkingUnit()));
        h.tvFeeInternetUnit.setText(getRoomPersonUnitLabel(context, k.getInternetUnit()));
        h.tvFeeGiatSayUnit.setText(getRoomPersonUnitLabel(context, k.getLaundryUnit()));
        h.tvFeeRacUnit.setText(getRoomPersonUnitLabel(context, k.getTrashUnit()));

        boolean hasCustomFee = bindCustomFees(h, k.getExtraFees());
        if (h.rowFeeHeader != null) {
            h.rowFeeHeader.setVisibility(hasAnyStandardFee ? View.VISIBLE : View.GONE);
        }
        if (h.tvNoFeeConfigured != null) {
            h.tvNoFeeConfigured.setVisibility((hasAnyStandardFee || hasCustomFee) ? View.GONE : View.VISIBLE);
        }

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
        return MoneyFormatter.format(value);
    }

    private boolean bindFeeRow(View row, TextView valueView, double amount) {
        boolean visible = amount > 0;
        if (row != null) {
            row.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        if (valueView != null && visible) {
            valueView.setText(formatVnd(amount));
        }
        return visible;
    }

    private boolean bindCustomFees(@NonNull VH h, List<House.ExtraFee> fees) {
        if (h.tvExtraFeesTitle == null || h.tvExtraFees == null) {
            return false;
        }

        if (fees == null || fees.isEmpty()) {
            h.tvExtraFeesTitle.setVisibility(View.GONE);
            h.tvExtraFees.setVisibility(View.GONE);
            return false;
        }

        StringBuilder sb = new StringBuilder();
        for (House.ExtraFee fee : fees) {
            if (fee == null)
                continue;
            String name = fee.getFeeName() != null ? fee.getFeeName().trim() : "";
            if (name.isEmpty() || fee.getPrice() <= 0)
                continue;

            if (sb.length() > 0) {
                sb.append("\n");
            }
            String unit = fee.getUnit() != null ? fee.getUnit().trim() : "";
            sb.append("• ").append(name);
            if (!unit.isEmpty()) {
                sb.append(" (").append(unit).append(")");
            }
            sb.append(": ").append(formatVnd(fee.getPrice()));
        }

        if (sb.length() == 0) {
            h.tvExtraFeesTitle.setVisibility(View.GONE);
            h.tvExtraFees.setVisibility(View.GONE);
            return false;
        }

        h.tvExtraFeesTitle.setVisibility(View.VISIBLE);
        h.tvExtraFees.setVisibility(View.VISIBLE);
        h.tvExtraFees.setText(sb.toString());
        return true;
    }

    private String getElectricityUnitLabel(@NonNull android.content.Context context, String mode) {
        if (mode == null || mode.trim().isEmpty() || "kwh".equalsIgnoreCase(mode)) {
            return context.getString(R.string.item_house_unit_kwh);
        }
        if (WaterCalculationMode.isPerPerson(mode)) {
            return context.getString(R.string.unit_person);
        }
        return context.getString(R.string.item_house_unit_room);
    }

    private String getParkingUnitLabel(@NonNull android.content.Context context, String unit) {
        if (unit == null || unit.trim().isEmpty() || "vehicle".equalsIgnoreCase(unit)) {
            return context.getString(R.string.item_house_unit_vehicle);
        }
        if ("person".equalsIgnoreCase(unit)) {
            return context.getString(R.string.unit_person);
        }
        return context.getString(R.string.item_house_unit_room);
    }

    private String getRoomPersonUnitLabel(@NonNull android.content.Context context, String unit) {
        return "person".equalsIgnoreCase(unit)
                ? context.getString(R.string.unit_person)
                : context.getString(R.string.item_house_unit_room);
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
        TextView tvFeeDien, tvFeeDienUnit, tvFeeNuoc, tvFeeNuocUnit, tvFeeXe, tvFeeXeUnit, tvFeeInternet,
            tvFeeInternetUnit, tvFeeGiatSay, tvFeeGiatSayUnit, tvFeeRac, tvFeeRacUnit, tvExtraFeesTitle,
            tvExtraFees, tvNoFeeConfigured;
        View rowFeeHeader, rowFeeDien, rowFeeNuoc, rowFeeXe, rowFeeInternet, rowFeeGiatSay, rowFeeRac;
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
            rowFeeHeader = itemView.findViewById(R.id.rowFeeHeader);
            rowFeeDien = itemView.findViewById(R.id.rowFeeDien);
            rowFeeNuoc = itemView.findViewById(R.id.rowFeeNuoc);
            rowFeeXe = itemView.findViewById(R.id.rowFeeXe);
            rowFeeInternet = itemView.findViewById(R.id.rowFeeInternet);
            rowFeeGiatSay = itemView.findViewById(R.id.rowFeeGiatSay);
            rowFeeRac = itemView.findViewById(R.id.rowFeeRac);

            tvFeeDien = itemView.findViewById(R.id.tvFeeDien);
            tvFeeDienUnit = itemView.findViewById(R.id.tvFeeDienUnit);
            tvFeeNuoc = itemView.findViewById(R.id.tvFeeNuoc);
            tvFeeNuocUnit = itemView.findViewById(R.id.tvFeeNuocUnit);
            tvFeeXe = itemView.findViewById(R.id.tvFeeXe);
            tvFeeXeUnit = itemView.findViewById(R.id.tvFeeXeUnit);
            tvFeeInternet = itemView.findViewById(R.id.tvFeeInternet);
            tvFeeInternetUnit = itemView.findViewById(R.id.tvFeeInternetUnit);
            tvFeeGiatSay = itemView.findViewById(R.id.tvFeeGiatSay);
            tvFeeGiatSayUnit = itemView.findViewById(R.id.tvFeeGiatSayUnit);
            tvFeeRac = itemView.findViewById(R.id.tvFeeRac);
            tvFeeRacUnit = itemView.findViewById(R.id.tvFeeRacUnit);
            tvExtraFeesTitle = itemView.findViewById(R.id.tvExtraFeesTitle);
            tvExtraFees = itemView.findViewById(R.id.tvExtraFees);
            tvNoFeeConfigured = itemView.findViewById(R.id.tvNoFeeConfigured);

            llEditAction = itemView.findViewById(R.id.llEditAction);
            llRoomsAction = itemView.findViewById(R.id.llRoomsAction);
            btnMore = itemView.findViewById(R.id.btnMore);
        }
    }
}
