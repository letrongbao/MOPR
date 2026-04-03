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

        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        tvTitle.setText("HÓA ĐƠN PHÒNG " + invoice.getSoPhong());

        StringBuilder sb = new StringBuilder();
        sb.append("Tháng: ").append(invoice.getThangNam()).append("\n\n");
        sb.append("- Tiền phòng: ").append(fmt.format(invoice.getGiaThue())).append("\n");

        double tienDien = (invoice.getChiSoDienCuoi() - invoice.getChiSoDienDau()) * invoice.getDonGiaDien();
        sb.append("- Tiền điện: ").append(fmt.format(tienDien))
                .append(" (").append((int) invoice.getChiSoDienDau()).append(" -> ")
                .append((int) invoice.getChiSoDienCuoi())
                .append(")\n");

        double tienNuoc = (invoice.getChiSoNuocCuoi() - invoice.getChiSoNuocDau()) * invoice.getDonGiaNuoc();
        sb.append("- Tiền nước: ").append(fmt.format(tienNuoc))
                .append(" (").append((int) invoice.getChiSoNuocDau()).append(" -> ")
                .append((int) invoice.getChiSoNuocCuoi())
                .append(")\n");

        if (invoice.getPhiRac() > 0)
            sb.append("- Phí rác: ").append(fmt.format(invoice.getPhiRac())).append("\n");
        if (invoice.getPhiWifi() > 0)
            sb.append("- Phí Wifi: ").append(fmt.format(invoice.getPhiWifi())).append("\n");
        if (invoice.getPhiGuiXe() > 0)
            sb.append("- Phí gửi xe: ").append(fmt.format(invoice.getPhiGuiXe())).append("\n");

        tvChiTiet.setText(sb + "\n\n(Đang tải thanh toán...) ");
        tvTong.setText("TỔNG CỘNG: " + fmt.format(invoice.getTongTien()));

        paymentRepository.listByInvoice(invoice.getId()).observe(owner, payments -> {
            if (payments == null)
                return;
            double paid = 0;
            for (Payment p : payments) {
                paid += p.getAmount();
            }
            double remaining = Math.max(0, invoice.getTongTien() - paid);
            String extra = "\n\n── Thanh toán ──\n" +
                    "Đã thu: " + fmt.format(paid) + "\n" +
                    "Còn lại: " + fmt.format(remaining);
            tvChiTiet.setText(sb + extra);
        });

        AlertDialog.Builder b = new AlertDialog.Builder(activity)
                .setView(view)
                .setPositiveButton("Đóng", null)
                .setNegativeButton("Lịch sử thanh toán", (d, w) -> openPaymentHistory.run(invoice));

        if (isTenantUser) {
            b.setNeutralButton("Xác nhận công tơ", (d, w) -> openTenantConfirmMeter.run(invoice));
        } else {
            b.setNeutralButton("Chụp màn hình",
                    (d, w) -> Toast.makeText(activity, "Hãy chụp màn hình để gửi hóa đơn", Toast.LENGTH_LONG).show());
        }

        b.show();
    }

    public static void showTenantConfirmMeterDialog(@NonNull AppCompatActivity activity,
            @NonNull Invoice invoice,
            @NonNull CollectionProvider scopedCollection) {
        if (invoice.getIdPhong() == null) {
            return;
        }

        String periodKey = toPeriodKey(invoice.getThangNam());
        if (periodKey.isEmpty()) {
            Toast.makeText(activity, "Kỳ không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        android.widget.LinearLayout layout = new android.widget.LinearLayout(activity);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 10);

        android.widget.RadioGroup rg = new android.widget.RadioGroup(activity);
        android.widget.RadioButton rbOk = new android.widget.RadioButton(activity);
        rbOk.setId(View.generateViewId());
        rbOk.setText("Đồng ý chỉ số");
        android.widget.RadioButton rbNo = new android.widget.RadioButton(activity);
        rbNo.setId(View.generateViewId());
        rbNo.setText("Không đồng ý");
        rg.addView(rbOk);
        rg.addView(rbNo);
        rg.check(rbOk.getId());
        layout.addView(rg);

        EditText etNote = new EditText(activity);
        etNote.setHint("Ghi chú (tuỳ chọn)");
        layout.addView(etNote);

        new AlertDialog.Builder(activity)
                .setTitle("Xác nhận chốt số")
                .setView(layout)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Gửi", (d, w) -> {
                    String status = (rg.getCheckedRadioButtonId() == rbNo.getId()) ? "DISPUTED" : "APPROVED";
                    String note = etNote.getText().toString().trim();

                    String docId = invoice.getIdPhong() + "_" + periodKey;
                    java.util.Map<String, Object> update = new java.util.HashMap<>();
                    update.put("tenantConfirmStatus", status);
                    update.put("tenantConfirmNote", note);
                    update.put("tenantConfirmAt", Timestamp.now());

                    scopedCollection.get("meterReadings").document(docId)
                            .update(update)
                            .addOnSuccessListener(v -> Toast.makeText(activity, "Đã gửi xác nhận", Toast.LENGTH_SHORT)
                                    .show())
                            .addOnFailureListener(e -> Toast.makeText(activity, "Gửi thất bại", Toast.LENGTH_SHORT)
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
