package com.example.myapplication.features.contract;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ContractListAdapter extends RecyclerView.Adapter<ContractListAdapter.ViewHolder> {

    private List<Tenant> fullList = new ArrayList<>();
    private List<Tenant> displayList = new ArrayList<>();
    private String filterQuery = "";

    public interface OnNhacTaiKyListener {
        void onNhacTaiKy(Tenant contract);
    }

    public interface OnDepositUpdateListener {
        void onDepositUpdated(Tenant contract);
    }

    public interface OnContractDeleteListener {
        void onContractDeleted(String contractId);
    }

    private OnNhacTaiKyListener nhacListener;
    private OnDepositUpdateListener depositListener;
    private OnContractDeleteListener deleteListener;

    public void setOnNhacTaiKyListener(OnNhacTaiKyListener l) {
        this.nhacListener = l;
    }

    public void setOnDepositUpdateListener(OnDepositUpdateListener l) {
        this.depositListener = l;
    }

    public void setOnContractDeleteListener(OnContractDeleteListener l) {
        this.deleteListener = l;
    }

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
        if (filterQuery.isEmpty())
            return true;
        String name = c.getFullName() != null ? c.getFullName().toLowerCase(Locale.ROOT) : "";
        String room = c.getRoomNumber() != null ? c.getRoomNumber().toLowerCase(Locale.ROOT) : "";
        return name.contains(filterQuery) || room.contains(filterQuery);
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

        // Internal note.
        h.btnMenu.setOnClickListener(v -> showPopupMenu(v, c, position, status));
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

    private void showPopupMenu(View anchor, Tenant contract, int position, ContractStatus status) {
        PopupMenu popup = new PopupMenu(anchor.getContext(), anchor);
        popup.getMenuInflater().inflate(R.menu.menu_contract, popup.getMenu());

        // Internal note.
        android.view.MenuItem menuRenewalReminder = popup.getMenu().findItem(R.id.menu_nhac_tai_ky);
        if (menuRenewalReminder != null) {
            menuRenewalReminder.setVisible(status == ContractStatus.EXPIRING_SOON);
        }

        // Internal note.
        android.view.MenuItem menuCollectDeposit = popup.getMenu().findItem(R.id.menu_thu_coc);
        if (menuCollectDeposit != null) {
            menuCollectDeposit.setVisible(!contract.isDepositCollected() && status != ContractStatus.ENDED);
        }

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_thu_coc) {
                showBottomSheetThuCoc(anchor.getContext(), contract, position);
                return true;
            } else if (itemId == R.id.menu_xac_nhan_thu_coc) {
                confirmDepositReceived(anchor.getContext(), contract, position);
                return true;
            } else if (itemId == R.id.menu_nhac_tai_ky) {
                sendRenewalReminder(anchor.getContext(), contract);
                return true;
            } else if (itemId == R.id.menu_xem_chi_tiet) {
                openContractDetail(anchor.getContext(), contract);
                return true;
            } else if (itemId == R.id.menu_xoa_hop_dong) {
                showDeleteConfirmDialog(anchor.getContext(), contract, position);
                return true;
            }
            return false;
        });

        popup.show();
    }

    /**
     * Internal note.
     */
    private void confirmDepositReceived(Context context, Tenant contract, int position) {
        if (depositListener != null) {
            contract.setDepositCollected(true);
            depositListener.onDepositUpdated(contract);
            notifyItemChanged(position);
            Toast.makeText(context, context.getString(R.string.deposit_collected_confirmed), Toast.LENGTH_SHORT).show();
        }
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

    private void showBottomSheetThuCoc(Context context, Tenant contract, int position) {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(
                R.layout.bottom_sheet_deposit,
                new FrameLayout(context),
                false);
        bottomSheet.setContentView(view);

        // Bind data
        TextView tvPhongInfo = view.findViewById(R.id.tvPhongInfo);
        TextView tvTenantInfo = view.findViewById(R.id.tvTenantInfo);
        TextView tvDepositAmount = view.findViewById(R.id.tvSoTienCoc);
        MaterialButton btnGuiQR = view.findViewById(R.id.btnGuiQR);
        MaterialButton btnXacNhanDaThu = view.findViewById(R.id.btnXacNhanDaThu);
        ImageButton btnClose = view.findViewById(R.id.btnClose);

        String phongText = contract.getRoomNumber() != null
                ? context.getString(R.string.room_number, contract.getRoomNumber())
                : "—";
        tvPhongInfo.setText(phongText);

        String tenantText = context.getString(
                R.string.contract_tenant_phone_line,
                (contract.getFullName() != null ? contract.getFullName() : "—"),
                (contract.getPhoneNumber() != null ? contract.getPhoneNumber() : "—"));
        tvTenantInfo.setText(tenantText);

        // Internal note.
        long depositAmount = contract.getDepositAmount();
        String depositFormatted = String.format(Locale.US, "%,d ₫", depositAmount).replace(',', '.');
        tvDepositAmount.setText(depositFormatted);

        // Internal note.
        btnGuiQR.setOnClickListener(v -> {
            shareQRAndMessage(context, contract);
        });

        // Internal note.
        btnXacNhanDaThu.setOnClickListener(v -> {
            if (depositListener != null) {
                contract.setDepositCollected(true);
                depositListener.onDepositUpdated(contract);
                notifyItemChanged(position);
                bottomSheet.dismiss();
                Toast.makeText(context, context.getString(R.string.deposit_collected_confirmed_plain),
                        Toast.LENGTH_SHORT).show();
            }
        });

        btnClose.setOnClickListener(v -> bottomSheet.dismiss());

        bottomSheet.show();
    }

    private void shareQRAndMessage(Context context, Tenant contract) {
        String room = contract.getRoomNumber() != null ? contract.getRoomNumber() : "?";
        long depositAmount = contract.getDepositAmount();
        String depositFormatted = String.format(Locale.US, "%,d ₫", depositAmount).replace(',', '.');

        String message = context.getString(R.string.deposit_payment_request_message, room, depositFormatted);

        // Share text (QR image would need FileProvider setup)
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, message);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.deposit_payment_request_subject, room));

        context.startActivity(
                Intent.createChooser(shareIntent, context.getString(R.string.deposit_payment_request_send_via)));
    }

    /**
     * Internal note.
     */
    private void showDeleteConfirmDialog(Context context, Tenant contract, int position) {
        if (contract == null || contract.getId() == null) {
            Toast.makeText(context, context.getString(R.string.error_contract_not_found), Toast.LENGTH_SHORT).show();
            return;
        }

        String roomName = contract.getRoomNumber() != null ? contract.getRoomNumber() : "?";
        String tenantName = contract.getFullName() != null ? contract.getFullName() : "?";

        new android.app.AlertDialog.Builder(context)
                .setTitle(R.string.contract_delete_confirm_title)
                .setMessage(context.getString(R.string.contract_delete_confirm_message, roomName, tenantName))
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    deleteContract(context, contract, position);
                })
                .setNegativeButton(R.string.cancel, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Internal note.
     */
    private void deleteContract(Context context, Tenant contract, int position) {
        String contractId = contract.getId();

        // Internal note.
        Toast.makeText(context, context.getString(R.string.contract_deleting), Toast.LENGTH_SHORT).show();

        // Internal note.
        if (deleteListener != null) {
            deleteListener.onContractDeleted(contractId);
        } else {
            // Internal note.
            Toast.makeText(context, context.getString(R.string.contract_delete_error), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Internal note.
     * Internal note.
     */
    public void removeItem(int position) {
        if (position >= 0 && position < displayList.size()) {
            Tenant removed = displayList.remove(position);
            fullList.remove(removed);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, displayList.size());
        }
    }

    /**
     * Internal note.
     */
    public void removeItemById(String contractId) {
        if (contractId == null)
            return;

        for (int i = 0; i < displayList.size(); i++) {
            if (contractId.equals(displayList.get(i).getId())) {
                removeItem(i);
                return;
            }
        }
    }

    // Internal note.

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoomName, tvRentAmount, tvDepositAmount, tvTenantName, tvTenantPhone, tvDepositStatus;
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
            chipStatus = v.findViewById(R.id.chipTrangThai);
            btnMenu = v.findViewById(R.id.btnMenu);
        }
    }
}
