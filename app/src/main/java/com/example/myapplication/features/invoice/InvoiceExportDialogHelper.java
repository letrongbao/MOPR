package com.example.myapplication.features.invoice;

import android.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwner;

import com.example.myapplication.R;
import com.example.myapplication.domain.Invoice;
import com.example.myapplication.domain.Payment;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;

import java.text.NumberFormat;
import java.util.Locale;

public final class InvoiceExportDialogHelper {

    public interface CollectionProvider {
        CollectionReference get(@NonNull String collection);
    }

    public interface InvoiceAction {
        void run(@NonNull Invoice invoice);
    }

    private InvoiceExportDialogHelper() {
    }

    public static void showInvoiceExportDialog(@NonNull AppCompatActivity activity,
            @NonNull LifecycleOwner owner,
            @NonNull Invoice invoice,
            @NonNull com.example.myapplication.core.repository.domain.PaymentRepository paymentRepository,
            boolean isTenantUser,
            @NonNull InvoiceAction openPaymentHistory,
            @NonNull InvoiceAction openTenantConfirmMeter) {
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_export_invoice, null);

        TextView tvTitle = view.findViewById(R.id.tvBillTitle);
        TextView tvChiTiet = view.findViewById(R.id.tvBillDetails);
        TextView tvTong = view.findViewById(R.id.tvBillTotal);

        NumberFormat fmt = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));

        tvTitle.setText(activity.getString(R.string.invoice_room, invoice.getRoomNumber()));

        StringBuilder sb = new StringBuilder();
        sb.append(activity.getString(R.string.month_colon)).append(invoice.getBillingPeriod()).append("\n\n");
        sb.append(activity.getString(R.string.room_fee_colon)).append(fmt.format(invoice.getRentAmount())).append("\n");

        double tienDien = (invoice.getElectricEndReading() - invoice.getElectricStartReading())
                * invoice.getElectricUnitPrice();
        sb.append(activity.getString(R.string.electric_fee_colon)).append(fmt.format(tienDien))
                .append(" (").append((int) invoice.getElectricStartReading()).append(" -> ")
                .append((int) invoice.getElectricEndReading())
                .append(")\n");

        double tienNuoc = (invoice.getWaterEndReading() - invoice.getWaterStartReading()) * invoice.getWaterUnitPrice();
        sb.append(activity.getString(R.string.water_fee_colon)).append(fmt.format(tienNuoc))
                .append(" (").append((int) invoice.getWaterStartReading()).append(" -> ")
                .append((int) invoice.getWaterEndReading())
                .append(")\n");

        if (invoice.getTrashFee() > 0)
            sb.append(activity.getString(R.string.trash_fee_colon)).append(fmt.format(invoice.getTrashFee()))
                    .append("\n");
        if (invoice.getWifiFee() > 0)
            sb.append(activity.getString(R.string.wifi_fee_colon)).append(fmt.format(invoice.getWifiFee()))
                    .append("\n");
        if (invoice.getParkingFee() > 0)
            sb.append(activity.getString(R.string.parking_fee_colon)).append(fmt.format(invoice.getParkingFee()))
                    .append("\n");
        if (invoice.getOtherFee() > 0) {
            sb.append(activity.getString(R.string.other_fee_colon)).append(fmt.format(invoice.getOtherFee()))
                .append("\n");
            if (invoice.getOtherFeeLines() != null && !invoice.getOtherFeeLines().isEmpty()) {
                for (String line : invoice.getOtherFeeLines()) {
                    if (line == null || line.trim().isEmpty())
                        continue;
                    sb.append("   + ").append(line.trim()).append("\n");
                }
            }
        }

        tvChiTiet.setText(sb + "\n\n" + activity.getString(R.string.loading_payments));
        tvTong.setText(activity.getString(R.string.total_amount_colon) + fmt.format(invoice.getTotalAmount()));

        paymentRepository.listByInvoice(invoice.getId()).observe(owner, payments -> {
            if (payments == null)
                return;
            double paid = 0;
            for (Payment p : payments) {
                paid += p.getAmount();
            }
            double remaining = Math.max(0, invoice.getTotalAmount() - paid);
            String extra = "\n\n" + activity.getString(R.string.payment_separator) + "\n" +
                    activity.getString(R.string.collected_colon) + fmt.format(paid) + "\n" +
                    activity.getString(R.string.remaining_colon) + fmt.format(remaining);
            tvChiTiet.setText(sb + extra);
        });

        AlertDialog.Builder b = new AlertDialog.Builder(activity)
                .setView(view)
                .setPositiveButton(activity.getString(R.string.close), null)
                .setNegativeButton(activity.getString(R.string.payment_history),
                        (d, w) -> openPaymentHistory.run(invoice));

        if (isTenantUser) {
            b.setNeutralButton(activity.getString(R.string.confirm_meter),
                    (d, w) -> openTenantConfirmMeter.run(invoice));
        } else {
            b.setNeutralButton(activity.getString(R.string.screenshot),
                    (d, w) -> Toast
                            .makeText(activity, activity.getString(R.string.screenshot_to_send), Toast.LENGTH_LONG)
                            .show());
        }

        b.show();
    }

    public static void showTenantConfirmMeterDialog(@NonNull AppCompatActivity activity,
            @NonNull Invoice invoice,
            @NonNull CollectionProvider scopedCollection) {
        if (invoice.getRoomId() == null) {
            return;
        }

        String periodKey = toPeriodKey(invoice.getBillingPeriod());
        if (periodKey.isEmpty()) {
            Toast.makeText(activity, activity.getString(R.string.invalid_period), Toast.LENGTH_SHORT).show();
            return;
        }

        android.widget.LinearLayout layout = new android.widget.LinearLayout(activity);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 10);

        android.widget.RadioGroup rg = new android.widget.RadioGroup(activity);
        android.widget.RadioButton rbOk = new android.widget.RadioButton(activity);
        rbOk.setId(View.generateViewId());
        rbOk.setText(activity.getString(R.string.agree_meter));
        android.widget.RadioButton rbNo = new android.widget.RadioButton(activity);
        rbNo.setId(View.generateViewId());
        rbNo.setText(activity.getString(R.string.disagree));
        rg.addView(rbOk);
        rg.addView(rbNo);
        rg.check(rbOk.getId());
        layout.addView(rg);

        EditText etNote = new EditText(activity);
        etNote.setHint(activity.getString(R.string.note_optional));
        layout.addView(etNote);

        new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.confirm_meter_reading))
                .setView(layout)
                .setNegativeButton(activity.getString(R.string.cancel), null)
                .setPositiveButton(activity.getString(R.string.send), (d, w) -> {
                    String status = (rg.getCheckedRadioButtonId() == rbNo.getId()) ? "DISPUTED" : "APPROVED";
                    String note = etNote.getText().toString().trim();

                    String docId = invoice.getRoomId() + "_" + periodKey;
                    java.util.Map<String, Object> update = new java.util.HashMap<>();
                    update.put("tenantConfirmStatus", status);
                    update.put("tenantConfirmNote", note);
                    update.put("tenantConfirmAt", Timestamp.now());

                    scopedCollection.get("meterReadings").document(docId)
                            .update(update)
                            .addOnSuccessListener(v -> Toast
                                    .makeText(activity, activity.getString(R.string.confirmation_sent),
                                            Toast.LENGTH_SHORT)
                                    .show())
                            .addOnFailureListener(e -> Toast
                                    .makeText(activity, activity.getString(R.string.send_failed), Toast.LENGTH_SHORT)
                                    .show());
                })
                .show();
    }

    @NonNull
    private static String toPeriodKey(String period) {
        if (period == null)
            return "";
        String[] parts = period.trim().split("/");
        if (parts.length != 2)
            return "";
        try {
            int month = Integer.parseInt(parts[0]);
            int year = Integer.parseInt(parts[1]);
            if (month < 1 || month > 12)
                return "";
            return String.format(Locale.US, "%04d%02d", year, month);
        } catch (NumberFormatException e) {
            return "";
        }
    }
}
