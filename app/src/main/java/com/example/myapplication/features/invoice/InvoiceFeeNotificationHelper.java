package com.example.myapplication.features.invoice;

import android.view.LayoutInflater;
import android.view.View;
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
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_fee_notification, null);

        TextView tvPhong = dialogView.findViewById(R.id.tvPhong);
        TextView tvTenant = dialogView.findViewById(R.id.tvTenant);
        EditText etChiSoDienCu = dialogView.findViewById(R.id.etChiSoDienCu);
        EditText etChiSoDienMoi = dialogView.findViewById(R.id.etChiSoDienMoi);
        MaterialButton btnXemInvoice = dialogView.findViewById(R.id.btnXemInvoice);
        ImageView btnClose = dialogView.findViewById(R.id.btnClose);

        tvPhong.setText(invoice.getSoPhong() != null ? invoice.getSoPhong() : "???");
        tvTenant.setText("Đang cập nhật");
        etChiSoDienCu.setText(String.valueOf((int) invoice.getChiSoDienDau()));

        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet = new com.google.android.material.bottomsheet.BottomSheetDialog(
                activity);
        bottomSheet.setContentView(dialogView);

        btnClose.setOnClickListener(v -> bottomSheet.dismiss());

        btnXemInvoice.setOnClickListener(v -> {
            String chiSoMoiStr = etChiSoDienMoi.getText().toString().trim();
            if (chiSoMoiStr.isEmpty()) {
                Toast.makeText(activity, "Vui lòng nhập chi số điện mới", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double chiSoMoi = Double.parseDouble(chiSoMoiStr);
                double chiSoCu = invoice.getChiSoDienDau();

                if (chiSoMoi < chiSoCu) {
                    Toast.makeText(activity, "Chi số điện mới phải >= chi số cũ", Toast.LENGTH_SHORT).show();
                    return;
                }

                invoice.setChiSoDienCuoi(chiSoMoi);
                invoice.setTrangThai(InvoiceStatus.REPORTED);

                viewModel.updateInvoice(invoice,
                        () -> activity.runOnUiThread(() -> {
                            bottomSheet.dismiss();
                            openInvoiceExport.run(invoice);
                        }),
                        () -> activity.runOnUiThread(
                                () -> Toast.makeText(activity, "Cập nhật thất bại", Toast.LENGTH_SHORT).show()));

            } catch (NumberFormatException e) {
                Toast.makeText(activity, "Chi số không hợp lệ", Toast.LENGTH_SHORT).show();
            }
        });

        bottomSheet.show();
    }
}
