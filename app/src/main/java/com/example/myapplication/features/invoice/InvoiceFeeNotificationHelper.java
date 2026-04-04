package com.example.myapplication.features.invoice;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.core.constants.InvoiceStatus;
import com.example.myapplication.domain.Invoice;
import com.example.myapplication.viewmodel.InvoiceViewModel;
import com.google.android.material.button.MaterialButton;

public final class InvoiceFeeNotificationHelper {

    public interface InvoiceAction {
        void run(@NonNull Invoice invoice);
    }

    private InvoiceFeeNotificationHelper() {
    }

    public static void showFeeNotificationDialog(@NonNull AppCompatActivity activity,
            @NonNull Invoice invoice,
            @NonNull InvoiceViewModel viewModel,
            @NonNull InvoiceAction openInvoiceExport) {
        ViewGroup root = activity.findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_fee_notification, root, false);

        TextView tvPhong = dialogView.findViewById(R.id.tvPhong);
        TextView tvTenant = dialogView.findViewById(R.id.tvTenant);
        EditText etChiSoDienCu = dialogView.findViewById(R.id.etChiSoDienCu);
        EditText etChiSoDienMoi = dialogView.findViewById(R.id.etChiSoDienMoi);
        MaterialButton btnXemInvoice = dialogView.findViewById(R.id.btnXemInvoice);
        ImageView btnClose = dialogView.findViewById(R.id.btnClose);

        tvPhong.setText(invoice.getRoomNumber() != null ? invoice.getRoomNumber() : "???");
        tvTenant.setText(activity.getString(R.string.updating));
        etChiSoDienCu.setText(String.valueOf((int) invoice.getElectricStartReading()));

        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet = new com.google.android.material.bottomsheet.BottomSheetDialog(
                activity);
        bottomSheet.setContentView(dialogView);

        btnClose.setOnClickListener(v -> bottomSheet.dismiss());

        btnXemInvoice.setOnClickListener(v -> {
            String chiSoMoiStr = etChiSoDienMoi.getText().toString().trim();
            if (chiSoMoiStr.isEmpty()) {
                Toast.makeText(activity, activity.getString(R.string.please_enter_new_electric), Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double chiSoMoi = Double.parseDouble(chiSoMoiStr);
                double chiSoCu = invoice.getElectricStartReading();

                if (chiSoMoi < chiSoCu) {
                    Toast.makeText(activity, activity.getString(R.string.new_electric_must_gte_old), Toast.LENGTH_SHORT).show();
                    return;
                }

                invoice.setElectricEndReading(chiSoMoi);
                invoice.setStatus(InvoiceStatus.REPORTED);

                viewModel.updateInvoice(invoice,
                        () -> activity.runOnUiThread(() -> {
                            bottomSheet.dismiss();
                            openInvoiceExport.run(invoice);
                        }),
                        () -> activity.runOnUiThread(
                                () -> Toast.makeText(activity, activity.getString(R.string.update_failed), Toast.LENGTH_SHORT).show()));

            } catch (NumberFormatException e) {
                Toast.makeText(activity, activity.getString(R.string.invalid_meter_reading), Toast.LENGTH_SHORT).show();
            }
        });

        bottomSheet.show();
    }
}
