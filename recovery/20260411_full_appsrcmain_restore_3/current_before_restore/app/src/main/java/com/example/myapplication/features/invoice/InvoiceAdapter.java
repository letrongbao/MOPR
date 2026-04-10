package com.example.myapplication.features.invoice;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.core.constants.InvoiceStatus;
import com.example.myapplication.core.constants.WaterCalculationMode;
import com.example.myapplication.R;
import com.example.myapplication.domain.Invoice;
import com.example.myapplication.core.util.MoneyFormatter;
import com.google.android.material.button.MaterialButton;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InvoiceAdapter extends RecyclerView.Adapter<InvoiceAdapter.ViewHolder> {

    private List<Invoice> dataList = new ArrayList<>();
    private final OnItemActionListener listener;
    private boolean tenantMode;
    private Map<String, String> tenantDisplayByRoom = new HashMap<>();
    private Map<String, String> roomAddressByRoom = new HashMap<>();
    private Map<String, String> roomElectricModeByRoom = new HashMap<>();
    private Map<String, String> roomWaterModeByRoom = new HashMap<>();
    private Map<String, Integer> roomMemberCountByRoom = new HashMap<>();
    private int currentTab = 0;

    public interface OnItemActionListener {
        void onDelete(Invoice invoice);

        void onBaoPhi(Invoice invoice);

        void onDoiTrangThai(Invoice invoice);

        void onSua(Invoice invoice);

        void onXuat(Invoice invoice);

        void onEditOwnerNote(Invoice invoice);
    }

    public InvoiceAdapter(OnItemActionListener listener) {
        this.listener = listener;
    }

    public void setDataList(List<Invoice> list) {
        this.dataList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setTenantDisplayByRoom(Map<String, String> tenantDisplayByRoom) {
        this.tenantDisplayByRoom = tenantDisplayByRoom != null ? tenantDisplayByRoom : new HashMap<>();
        notifyDataSetChanged();
    }

    public void setRoomAddressByRoom(Map<String, String> roomAddressByRoom) {
        this.roomAddressByRoom = roomAddressByRoom != null ? roomAddressByRoom : new HashMap<>();
        notifyDataSetChanged();
    }

    public void setRoomElectricModeByRoom(Map<String, String> roomElectricModeByRoom) {
        this.roomElectricModeByRoom = roomElectricModeByRoom != null ? roomElectricModeByRoom : new HashMap<>();
        notifyDataSetChanged();
    }

    public void setRoomWaterModeByRoom(Map<String, String> roomWaterModeByRoom) {
        this.roomWaterModeByRoom = roomWaterModeByRoom != null ? roomWaterModeByRoom : new HashMap<>();
        notifyDataSetChanged();
    }

    public void setRoomMemberCountByRoom(Map<String, Integer> roomMemberCountByRoom) {
        this.roomMemberCountByRoom = roomMemberCountByRoom != null ? roomMemberCountByRoom : new HashMap<>();
        notifyDataSetChanged();
    }

    public void setTenantMode(boolean tenantMode) {
        this.tenantMode = tenantMode;
        notifyDataSetChanged();
    }

    public void setCurrentTab(int tab) {
        this.currentTab = tab;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_invoice, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Invoice h = dataList.get(position);
        android.content.Context context = holder.itemView.getContext();

        String roomDisplay = h.getRoomNumber() != null ? ("P." + h.getRoomNumber()) : "P.???";
        String address = roomAddressByRoom.get(h.getRoomId());
        if (address != null && !address.trim().isEmpty()) {
            roomDisplay = roomDisplay + " • " + address.trim();
        }
        holder.tvPhong.setText(roomDisplay);

        String tenantDisplay = tenantDisplayByRoom.get(h.getRoomId());
        if (tenantDisplay == null || tenantDisplay.trim().isEmpty()) {
            tenantDisplay = context.getString(R.string.tenant_colon) + context.getString(R.string.updating);
        }
        holder.tvTenantName.setText(tenantDisplay);

        holder.tvPriceMonth.setText(context.getString(R.string.price_colon) + MoneyFormatter.format(h.getRentAmount()) + context.getString(R.string.per_month));

        holder.tvReportDate.setText(context.getString(R.string.month_colon) + h.getBillingPeriod());
        holder.tvAmountTotal.setText(context.getString(
            R.string.invoice_total_amount_label,
            MoneyFormatter.format(resolveDisplayTotal(h))));
        String extraFeeSummary = buildExtraFeeSummary(context, h);
        if (extraFeeSummary.isEmpty()) {
            holder.tvExtraFeeSummary.setVisibility(View.GONE);
        } else {
            holder.tvExtraFeeSummary.setVisibility(View.VISIBLE);
            holder.tvExtraFeeSummary.setText(extraFeeSummary);
        }
        holder.tvMeterSummary.setText(buildMeterSummary(context, h));

        if (h.getOwnerNote() != null && !h.getOwnerNote().trim().isEmpty()) {
            holder.tvOwnerNote.setVisibility(View.VISIBLE);
            holder.tvOwnerNote.setText(context.getString(R.string.note_label) + ": " + h.getOwnerNote());
        } else {
            holder.tvOwnerNote.setVisibility(View.GONE);
        }

        // Status ribbon
        String st = normalizeStatus(h.getStatus());

        if (holder.tvRibbonStatus != null) {
            holder.tvRibbonStatus.setText(toDisplayStatus(context, st));
            if (InvoiceStatus.PAID.equals(st)) {
                holder.tvRibbonStatus
                        .setBackgroundColor(ContextCompat.getColor(context, R.color.success));
            } else if (InvoiceStatus.REPORTED.equals(st)) {
                holder.tvRibbonStatus.setBackgroundColor(
                        ContextCompat.getColor(context, R.color.btn_blue_action));
            } else {
                holder.tvRibbonStatus.setBackgroundColor(
                        ContextCompat.getColor(context, R.color.warning));
            }
        }

        // View reported fees link - now bound to the whole card
        holder.itemView.setOnClickListener(v -> listener.onXuat(h));

        // Main action button changes based on tab
        String buttonText;
        int buttonColor;
        View.OnClickListener buttonAction;

        if (tenantMode) {
            if (InvoiceStatus.UNREPORTED.equals(st)) {
                buttonText = context.getString(R.string.waiting_owner_report);
                buttonColor = R.color.warning;
                buttonAction = null;
            } else {
                buttonText = InvoiceStatus.PAID.equals(st)
                        ? context.getString(R.string.view_payment_history)
                        : context.getString(R.string.pay_now);
                buttonColor = InvoiceStatus.PAID.equals(st) ? R.color.success : R.color.btn_blue_action;
                buttonAction = v -> listener.onDoiTrangThai(h);
            }

            holder.btnMainAction.setText(buttonText);
            holder.btnMainAction.setBackgroundColor(
                    ContextCompat.getColor(context, buttonColor));
            holder.btnMainAction.setVisibility(View.VISIBLE);
            holder.btnMainAction.setEnabled(buttonAction != null);
            holder.btnMainAction.setOnClickListener(buttonAction);
            return;
        }

        if (InvoiceStatus.UNREPORTED.equals(st)) {
            buttonText = context.getString(R.string.invoice_action_enter_report);
            buttonColor = R.color.btn_blue_action;
            buttonAction = v -> listener.onBaoPhi(h);
            holder.itemView.setOnClickListener(v -> listener.onBaoPhi(h));
        } else if (InvoiceStatus.REPORTED.equals(st)) {
            buttonText = context.getString(R.string.invoice_action_collect_payment);
            buttonColor = R.color.btn_orange;
            buttonAction = v -> listener.onDoiTrangThai(h);
        } else {
            buttonText = context.getString(R.string.invoice_action_view_payment_history);
            buttonColor = R.color.success;
            buttonAction = v -> listener.onDoiTrangThai(h);
        }

        holder.btnMainAction.setText(buttonText);
        holder.btnMainAction.setBackgroundColor(
            ContextCompat.getColor(context, buttonColor));
        holder.btnMainAction.setOnClickListener(buttonAction);
        holder.btnMainAction.setVisibility(View.VISIBLE);
        holder.btnMainAction.setEnabled(true);
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    private String toDisplayStatus(@NonNull android.content.Context context, String status) {
        if (InvoiceStatus.PAID.equals(status)) {
            return context.getString(R.string.invoice_status_paid);
        }
        if (InvoiceStatus.REPORTED.equals(status)) {
            return context.getString(R.string.invoice_status_reported);
        }
        return context.getString(R.string.invoice_status_unreported);
    }

    private String resolveFlowHint(@NonNull android.content.Context context, String status) {
        if (InvoiceStatus.PAID.equals(status)) {
            return context.getString(R.string.invoice_flow_hint_paid);
        }
        if (InvoiceStatus.REPORTED.equals(status)) {
            return context.getString(R.string.invoice_flow_hint_reported);
        }
        return context.getString(R.string.invoice_flow_hint_unreported);
    }

    @NonNull
    private String normalizeStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return InvoiceStatus.UNREPORTED;
        }
        if ("PARTIAL".equalsIgnoreCase(status.trim())) {
            return InvoiceStatus.REPORTED;
        }
        return status;
    }

    private String formatReading(double value) {
        return value % 1 == 0 ? String.valueOf((long) value) : String.valueOf(value);
    }

    private String resolveElectricMode(@NonNull Invoice invoice) {
        String explicit = roomElectricModeByRoom.get(invoice.getRoomId());
        if (explicit != null && !explicit.trim().isEmpty()) {
            return explicit.trim();
        }

        Integer memberCount = roomMemberCountByRoom.get(invoice.getRoomId());
        if (memberCount != null
                && memberCount > 0
                && Math.abs(invoice.getElectricStartReading()) < 0.001
                && Math.abs(invoice.getElectricEndReading() - memberCount) < 0.001) {
            return "per_person";
        }

        if (Math.abs(invoice.getElectricStartReading()) < 0.001
                && Math.abs(invoice.getElectricEndReading() - 1.0) < 0.001) {
            return "room";
        }
        return "kwh";
    }

    private String resolveWaterMode(@NonNull Invoice invoice) {
        String explicit = roomWaterModeByRoom.get(invoice.getRoomId());
        if (explicit != null && !explicit.trim().isEmpty()) {
            return explicit.trim();
        }

        Integer memberCount = roomMemberCountByRoom.get(invoice.getRoomId());
        if (memberCount != null
                && memberCount > 0
                && Math.abs(invoice.getWaterStartReading()) < 0.001
                && Math.abs(invoice.getWaterEndReading() - memberCount) < 0.001) {
            return WaterCalculationMode.PER_PERSON;
        }

        if (Math.abs(invoice.getWaterStartReading()) < 0.001
                && Math.abs(invoice.getWaterEndReading() - 1.0) < 0.001) {
            return WaterCalculationMode.ROOM;
        }
        return WaterCalculationMode.METER;
    }

    private double resolveDisplayTotal(@NonNull Invoice invoice) {
        double stored = invoice.getTotalAmount();
        if (stored > 0) {
            return stored;
        }

        String electricMode = resolveElectricMode(invoice);
        String waterMode = resolveWaterMode(invoice);
        double electricUsage = "kwh".equalsIgnoreCase(electricMode)
                ? Math.max(0, invoice.getElectricEndReading() - invoice.getElectricStartReading())
                : Math.max(0, invoice.getElectricEndReading());
        double waterUsage = WaterCalculationMode.isMeter(waterMode)
                ? Math.max(0, invoice.getWaterEndReading() - invoice.getWaterStartReading())
                : Math.max(0, invoice.getWaterEndReading());

        return invoice.getRentAmount()
                + electricUsage * invoice.getElectricUnitPrice()
                + waterUsage * invoice.getWaterUnitPrice()
                + invoice.getTrashFee()
                + invoice.getWifiFee()
                + invoice.getParkingFee()
                + invoice.getOtherFee();
    }

    private String buildMeterSummary(@NonNull android.content.Context context, @NonNull Invoice invoice) {
        String electricMode = resolveElectricMode(invoice);
        String electricPart;
        if ("per_person".equalsIgnoreCase(electricMode)) {
            electricPart = context.getString(
                R.string.invoice_meter_summary_electric_per_person,
                formatReading(invoice.getElectricEndReading()));
        } else if ("room".equalsIgnoreCase(electricMode)) {
            electricPart = context.getString(
                R.string.invoice_meter_summary_electric_per_room,
                formatReading(invoice.getElectricEndReading()));
        } else {
            electricPart = context.getString(
                R.string.invoice_meter_summary_electric,
                formatReading(invoice.getElectricStartReading()),
                formatReading(invoice.getElectricEndReading()));
        }

        String waterMode = resolveWaterMode(invoice);
        String waterPart;
        if (WaterCalculationMode.isPerPerson(waterMode)) {
            waterPart = context.getString(
                    R.string.invoice_meter_summary_water_per_person,
                    formatReading(invoice.getWaterEndReading()));
        } else if (WaterCalculationMode.ROOM.equals(waterMode)) {
            waterPart = context.getString(
                    R.string.invoice_meter_summary_water_per_room,
                    formatReading(invoice.getWaterEndReading()));
        } else {
            waterPart = context.getString(
                    R.string.invoice_meter_summary_water_meter,
                    formatReading(invoice.getWaterStartReading()),
                    formatReading(invoice.getWaterEndReading()));
        }

        return electricPart + " | " + waterPart;
    }

    private String buildExtraFeeSummary(@NonNull android.content.Context context, @NonNull Invoice invoice) {
        double trashFee = Math.max(0, invoice.getTrashFee());
        double wifiFee = Math.max(0, invoice.getWifiFee());
        double parkingFee = Math.max(0, invoice.getParkingFee());
        double otherFee = Math.max(0, invoice.getOtherFee());

        if (trashFee <= 0 && wifiFee <= 0 && parkingFee <= 0 && otherFee <= 0) {
            return "";
        }

        return context.getString(
                R.string.invoice_extra_fee_summary_label,
                MoneyFormatter.format(trashFee),
                MoneyFormatter.format(wifiFee),
                MoneyFormatter.format(parkingFee),
                MoneyFormatter.format(otherFee));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPhong, tvTenantName, tvPriceMonth, tvReportDate, tvAmountTotal, tvExtraFeeSummary, tvMeterSummary,
            tvRibbonStatus, tvOwnerNote;
        MaterialButton btnMainAction;

        ViewHolder(View v) {
            super(v);
            tvPhong = v.findViewById(R.id.tvPhong);
            tvTenantName = v.findViewById(R.id.tvTenantName);
            tvPriceMonth = v.findViewById(R.id.tvPriceMonth);
            tvReportDate = v.findViewById(R.id.tvReportDate);
            tvAmountTotal = v.findViewById(R.id.tvAmountTotal);
            tvExtraFeeSummary = v.findViewById(R.id.tvExtraFeeSummary);
            tvMeterSummary = v.findViewById(R.id.tvMeterSummary);
            tvRibbonStatus = v.findViewById(R.id.tvRibbonStatus);
            tvOwnerNote = v.findViewById(R.id.tvOwnerNote);
            btnMainAction = v.findViewById(R.id.btnMainAction);
        }
    }
}


