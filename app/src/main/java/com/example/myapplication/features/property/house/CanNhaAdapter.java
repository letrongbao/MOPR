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
import com.example.myapplication.core.util.MoneyFormatter;
import com.example.myapplication.domain.CanNha;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CanNhaAdapter extends RecyclerView.Adapter<CanNhaAdapter.VH> {

    public interface OnItemAction {
        void onEdit(@NonNull CanNha item);

        void onDelete(@NonNull CanNha item);

        void onViewRooms(@NonNull CanNha item);
    }

    private final List<CanNha> items = new ArrayList<>();
    private final OnItemAction actionCb;
    private final Set<String> expandedKeys = new HashSet<>();
    private boolean readOnly;

    public CanNhaAdapter(@NonNull OnItemAction actionCb) {
        this.actionCb = actionCb;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        notifyDataSetChanged();
    }

    public void setItems(List<CanNha> newItems) {
        items.clear();
        if (newItems != null)
            items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_khu_tro, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        CanNha k = items.get(position);

        String addr = k.getDiaChi() != null && !k.getDiaChi().trim().isEmpty() ? k.getDiaChi() : "Chưa có địa chỉ";
        h.tvKhuAddr.setText(addr);

        String manager = k.getTenKhu() != null ? k.getTenKhu().trim() : "";
        String phone = k.getSdtQuanLy() != null ? k.getSdtQuanLy().trim() : "";
        String managerDisplay = manager.isEmpty() ? "Chưa có tên quản lý" : manager;
        h.tvKhuName.setText(phone.isEmpty() ? managerDisplay : (managerDisplay + "  •  " + phone));

        // Bank info (fallback to manager name)
        String chuTk = (k.getChuTaiKhoan() != null && !k.getChuTaiKhoan().trim().isEmpty()) ? k.getChuTaiKhoan().trim()
                : manager;
        String nganHang = k.getNganHang() != null ? k.getNganHang().trim() : "";
        String soTk = k.getSoTaiKhoan() != null ? k.getSoTaiKhoan().trim() : "";

        StringBuilder bank = new StringBuilder();
        if (!chuTk.isEmpty())
            bank.append("Chủ TK: ").append(chuTk);
        if (!nganHang.isEmpty())
            bank.append(bank.length() > 0 ? "\n" : "").append("Ngân hàng ").append(nganHang);
        if (!soTk.isEmpty())
            bank.append(bank.length() > 0 ? "\n" : "").append("STK: ").append(soTk);
        if (bank.length() == 0) {
            bank.append("Chủ TK: ").append(manager.isEmpty() ? "Chủ trọ" : manager)
                    .append("\nNgân hàng")
                    .append("\nSTK:");
        }
        h.tvBankInfo.setText(bank);

        // Fee values
        h.tvFeeDien.setText(formatVnd(k.getGiaDien()));
        h.tvFeeNuoc.setText(formatVnd(k.getGiaNuoc()));
        h.tvFeeXe.setText(formatVnd(k.getGiaXe()));
        h.tvFeeInternet.setText(formatVnd(k.getGiaInternet()));
        h.tvFeeGiatSay.setText(formatVnd(k.getGiaGiatSay()));
        h.tvFeeThangMay.setText(formatVnd(k.getGiaThangMay()));
        h.tvFeeTiviCap.setText(formatVnd(k.getGiaTiviCap()));
        h.tvFeeRac.setText(formatVnd(k.getGiaRac()));
        h.tvFeeDichVu.setText(formatVnd(k.getGiaDichVu()));

        String nuocUnit = "phòng";
        String mode = k.getCachTinhNuoc();
        if (mode != null) {
            if (mode.equals("nguoi"))
                nuocUnit = "người";
            else if (mode.equals("dong_ho"))
                nuocUnit = "m³";
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
            pm.getMenu().add(0, 1, 0, "Xoá nhà");
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
            return "0 đ";
        return MoneyFormatter.format(value);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        View headerRoot;
        TextView tvKhuAddr, tvKhuName, tvBankInfo;
        LinearLayout llToggleFees, llFeeTable;
        ImageView ivFeesExpand;
        TextView tvFeeDien, tvFeeNuoc, tvFeeNuocUnit, tvFeeXe, tvFeeInternet, tvFeeGiatSay, tvFeeThangMay, tvFeeTiviCap,
                tvFeeRac, tvFeeDichVu;
        View llEditAction, llRoomsAction;
        ImageView btnMore;

        VH(@NonNull View itemView) {
            super(itemView);
            headerRoot = itemView.findViewById(R.id.headerRoot);
            tvKhuAddr = itemView.findViewById(R.id.tvKhuAddr);
            tvKhuName = itemView.findViewById(R.id.tvKhuName);
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
