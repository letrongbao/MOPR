package com.example.myapplication.features.contract;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.core.util.ContractStatusHelper;
import com.example.myapplication.domain.ContractStatus;
import com.example.myapplication.domain.Tenant;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ContractListAdapter extends RecyclerView.Adapter<ContractListAdapter.ViewHolder> {

    public enum LegalFilter {
        ALL,
        ACTIVE,
        EXPIRING,
        ENDED
    }

    private List<Tenant> fullList = new ArrayList<>();
    private List<Tenant> displayList = new ArrayList<>();
    private String filterQuery = "";
    private LegalFilter legalFilter = LegalFilter.ALL;

    // Internal note.

    public void setData(List<Tenant> list) {
        this.fullList = list != null ? new ArrayList<>(list) : new ArrayList<>();
        // Internal note.
        this.fullList.sort((a, b) -> {
            int pa = priorityOf(ContractStatusHelper.resolve(a));
            int pb = priorityOf(ContractStatusHelper.resolve(b));
            if (pa != pb)
                return Integer.compare(pa, pb);
            // Internal note.
            long da = ContractStatusHelper.daysRemaining(a);
            long db = ContractStatusHelper.daysRemaining(b);
            return Long.compare(da, db);
        });
        rebuildDisplay();
    }

    public void filter(String query) {
        this.filterQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        rebuildDisplay();
    }

    public void setLegalFilter(LegalFilter filter) {
        this.legalFilter = filter != null ? filter : LegalFilter.ALL;
        rebuildDisplay();
    }

    public int getDisplayCount() {
        return displayList.size();
    }

    public int countByStatus(ContractStatus status) {
        int count = 0;
        for (Tenant c : fullList) {
            if (ContractStatusHelper.resolve(c) == status)
                count++;
        }
        return count;
    }

    // Internal note.

    private int priorityOf(ContractStatus s) {
        switch (s) {
            case EXPIRING_SOON:
                return 0;
            case ACTIVE_RENTAL:
                return 1;
            case ENDED:
                return 2;
            default:
                return 3;
        }
    }

    private void rebuildDisplay() {
        List<Tenant> filtered = new ArrayList<>();
        for (Tenant c : fullList) {
            if (matches(c))
                filtered.add(c);
        }
        this.displayList = filtered;
        notifyDataSetChanged();
    }

    private boolean matches(Tenant c) {
        ContractStatus status = ContractStatusHelper.resolve(c);
        if (!matchesLegalFilter(status)) {
            return false;
        }

        if (filterQuery.isEmpty()) {
            return true;
        }
        String name = c.getFullName() != null ? c.getFullName().toLowerCase(Locale.ROOT) : "";
        String room = c.getRoomNumber() != null ? c.getRoomNumber().toLowerCase(Locale.ROOT) : "";
        return name.contains(filterQuery) || room.contains(filterQuery);
    }

    private boolean matchesLegalFilter(ContractStatus status) {
        switch (legalFilter) {
            case ACTIVE:
                return status == ContractStatus.ACTIVE_RENTAL;
            case EXPIRING:
                return status == ContractStatus.EXPIRING_SOON;
            case ENDED:
                return status == ContractStatus.ENDED;
            case ALL:
            default:
                return true;
        }
    }

    // Internal note.

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contract, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Tenant c = displayList.get(position);
        Context context = h.itemView.getContext();
        ContractStatus status = ContractStatusHelper.resolve(c);
        long daysLeft = ContractStatusHelper.daysRemaining(c);

        // Internal note.
        h.tvRoomName
                .setText(c.getRoomNumber() != null ? context.getString(R.string.room_number, c.getRoomNumber()) : "—");

        // Internal note.
        long rentAmount = c.getRentAmount();
        String rentFormatted = ContractListItemUiHelper.formatMoney(rentAmount);
        h.tvRentAmount.setText(rentFormatted);

        // Internal note.
        long depositAmount = c.getDepositAmount();
        String depositFormatted = String.format(Locale.US, "%,d ₫", depositAmount).replace(',', '.');
        h.tvDepositAmount.setText(depositFormatted);

        // Internal note.
        h.tvTenantName.setText(ContractListItemUiHelper.displayRepresentativeName(c));
        h.tvTenantPhone.setText(c.getPhoneNumber() != null ? c.getPhoneNumber() : "—");

        // Internal note.
        ContractListItemUiHelper.updateContractStatusChip(h.chipStatus, status, daysLeft, c);

        // Internal note.
        ContractListItemUiHelper.updateDepositStatusDisplay(h.tvDepositStatus, c, status);

        bindLegalTimeline(h, status, daysLeft);

        // Internal note.
        h.btnMenu.setOnClickListener(v -> showPopupMenu(v, c, status));
        h.itemView.setOnClickListener(v -> openContractDetail(context, c));
    }

    private void bindLegalTimeline(@NonNull ViewHolder h, @NonNull ContractStatus status, long daysLeft) {
        Context context = h.itemView.getContext();

        if (status == ContractStatus.ENDED) {
            if (daysLeft < 0 && daysLeft != -999) {
                h.tvDaysRemaining.setText(
                        context.getString(R.string.contract_days_remaining_overdue, Math.abs(daysLeft)));
            } else {
                h.tvDaysRemaining.setText(context.getString(R.string.contract_days_remaining_unknown));
            }
            h.tvUrgencyBadge.setText(context.getString(R.string.contract_urgency_expired));
            h.tvUrgencyBadge.setTextColor(0xFF6D4C41);
            return;
        }

        if (daysLeft == -999) {
            h.tvDaysRemaining.setText(context.getString(R.string.contract_days_remaining_unknown));
            h.tvUrgencyBadge.setText(context.getString(R.string.contract_urgency_watch));
            h.tvUrgencyBadge.setTextColor(0xFFEF6C00);
            return;
        }

        if (daysLeft == 0) {
            h.tvDaysRemaining.setText(context.getString(R.string.contract_days_remaining_today));
        } else if (daysLeft > 0) {
            h.tvDaysRemaining.setText(context.getString(R.string.contract_days_remaining_label, daysLeft));
        } else {
            h.tvDaysRemaining.setText(context.getString(R.string.contract_days_remaining_overdue, Math.abs(daysLeft)));
        }

        if (daysLeft <= 7) {
            h.tvUrgencyBadge.setText(context.getString(R.string.contract_urgency_critical));
            h.tvUrgencyBadge.setTextColor(0xFFC62828);
        } else if (daysLeft <= 30 || status == ContractStatus.EXPIRING_SOON) {
            h.tvUrgencyBadge.setText(context.getString(R.string.contract_urgency_watch));
            h.tvUrgencyBadge.setTextColor(0xFFEF6C00);
        } else {
            h.tvUrgencyBadge.setText(context.getString(R.string.contract_urgency_normal));
            h.tvUrgencyBadge.setTextColor(0xFF2E7D32);
        }
    }

    private void sendZaloReminder(Context ctx, Tenant c) {
        String roomNumber = c.getRoomNumber() != null ? c.getRoomNumber() : "?";
        String contractEndTimestamp = c.getContractEndDate() != null ? c.getContractEndDate() : "?";
        String msg = ctx.getString(R.string.contract_renewal_reminder, roomNumber, contractEndTimestamp);

        String phoneNumber = c.getPhoneNumber();
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            // Internal note.
            String normalized = phoneNumber.replaceAll("[^0-9]", "");
            if (normalized.startsWith("0")) {
                normalized = "84" + normalized.substring(1);
            }
            try {
                Intent zaloIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://zalo.me/" + normalized));
                zaloIntent.setPackage("com.zing.zalo");
                ctx.startActivity(zaloIntent);
                return;
            } catch (Exception ignored) {
            }
        }

        // Fallback: Share chooser
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, msg);
        ctx.startActivity(Intent.createChooser(shareIntent, ctx.getString(R.string.contract_send_reminder_via)));
    }

    private void showPopupMenu(View anchor, Tenant contract, ContractStatus status) {
        PopupMenu popup = new PopupMenu(anchor.getContext(), anchor);
        popup.getMenuInflater().inflate(R.menu.menu_contract, popup.getMenu());

        // Internal note.
        android.view.MenuItem menuRenewalReminder = popup.getMenu().findItem(R.id.menu_nhac_tai_ky);
        if (menuRenewalReminder != null) {
            menuRenewalReminder.setVisible(status == ContractStatus.EXPIRING_SOON);
        }

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_nhac_tai_ky) {
                sendRenewalReminder(anchor.getContext(), contract);
                return true;
            } else if (itemId == R.id.menu_xem_chi_tiet) {
                openContractDetail(anchor.getContext(), contract);
                return true;
            }
            return false;
        });

        popup.show();
    }

    /**
     * Internal note.
     */
    private void sendRenewalReminder(Context context, Tenant contract) {
        String roomNumber = contract.getRoomNumber() != null ? contract.getRoomNumber() : "?";
        String contractEndTimestamp = contract.getContractEndDate() != null ? contract.getContractEndDate() : "?";
        long daysLeft = ContractStatusHelper.daysRemaining(contract);

        String msg = context.getString(
                R.string.contract_renewal_notice_message,
                roomNumber,
                contractEndTimestamp,
                daysLeft);

        String phoneNumber = contract.getPhoneNumber();
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            // Internal note.
            String normalized = phoneNumber.replaceAll("[^0-9]", "");
            if (normalized.startsWith("0")) {
                normalized = "84" + normalized.substring(1);
            }
            try {
                Intent zaloIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://zalo.me/" + normalized + "?text=" + Uri.encode(msg)));
                zaloIntent.setPackage("com.zing.zalo");
                context.startActivity(zaloIntent);
                return;
            } catch (Exception ignored) {
            }
        }

        // Fallback: Share chooser
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, msg);
        context.startActivity(
                Intent.createChooser(shareIntent, context.getString(R.string.contract_send_reminder_via)));
    }

    /**
     * Internal note.
     */
    private void openContractDetail(Context context, Tenant contract) {
        if (contract == null || contract.getId() == null) {
            Toast.makeText(context, context.getString(R.string.error_contract_not_found), Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(context, ContractDetailsActivity.class);
        intent.putExtra(ContractDetailsActivity.EXTRA_CONTRACT_ID, contract.getId());
        context.startActivity(intent);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoomName, tvRentAmount, tvDepositAmount, tvTenantName, tvTenantPhone, tvDepositStatus,
            tvDaysRemaining, tvUrgencyBadge;
        Chip chipStatus;
        ImageButton btnMenu;

        ViewHolder(View v) {
            super(v);
            tvRoomName = v.findViewById(R.id.tvTenPhong);
            tvRentAmount = v.findViewById(R.id.tvGiaThue);
            tvDepositAmount = v.findViewById(R.id.tvTienCoc);
            tvTenantName = v.findViewById(R.id.tvTenantName);
            tvTenantPhone = v.findViewById(R.id.tvTenantPhone);
            tvDepositStatus = v.findViewById(R.id.tvDepositStatus);
            tvDaysRemaining = v.findViewById(R.id.tvDaysRemaining);
            tvUrgencyBadge = v.findViewById(R.id.tvUrgencyBadge);
            chipStatus = v.findViewById(R.id.chipTrangThai);
            btnMenu = v.findViewById(R.id.btnMenu);
        }
    }
}
