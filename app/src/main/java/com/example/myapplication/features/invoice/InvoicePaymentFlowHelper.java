package com.example.myapplication.features.invoice;

import android.app.AlertDialog;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.core.constants.InvoiceStatus;
import com.example.myapplication.core.repository.domain.PaymentRepository;
import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.domain.Invoice;
import com.example.myapplication.domain.Payment;
import com.example.myapplication.viewmodel.InvoiceViewModel;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class InvoicePaymentFlowHelper {

    public interface CollectionProvider {
        CollectionReference get(@NonNull String collection);
    }

    private InvoicePaymentFlowHelper() {
    }

    public static void recomputeAndUpdateInvoiceStatus(@NonNull Invoice invoice,
            @NonNull CollectionProvider scopedCollection,
            @NonNull InvoiceViewModel viewModel) {
        String invoiceId = invoice.getId();
        if (invoiceId == null || invoiceId.trim().isEmpty()) {
            return;
        }

        scopedCollection.get("payments")
                .whereEqualTo("invoiceId", invoiceId)
                .get()
                .addOnSuccessListener(qs -> {
                    double paid = 0;
                    if (qs != null) {
                        for (QueryDocumentSnapshot doc : qs) {
                            Double amt = doc.getDouble("amount");
                            if (amt != null) {
                                paid += amt;
                            }
                        }
                    }

                    String newStatus;
                    if (paid <= 0) {
                        newStatus = InvoiceStatus.REPORTED;
                    } else if (paid + 0.01 < invoice.getTotalAmount()) {
                        newStatus = InvoiceStatus.PARTIAL;
                    } else {
                        newStatus = InvoiceStatus.PAID;
                    }

                    viewModel.updateStatus(invoiceId, newStatus,
                            () -> {
                            },
                            () -> {
                            });
                });
    }

    public static void showCollectPaymentDialog(@NonNull AppCompatActivity activity,
            @NonNull FirebaseFirestore db,
            @NonNull Invoice invoice,
            @NonNull CollectionProvider scopedCollection,
            @NonNull PaymentRepository paymentRepository,
            @NonNull InvoiceViewModel viewModel) {
        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_add_payment, null);
        EditText etAmount = dialogView.findViewById(R.id.etAmount);
        Spinner spinnerMethod = dialogView.findViewById(R.id.spinnerMethod);
        EditText etPaidAt = dialogView.findViewById(R.id.etPaidAt);
        EditText etNote = dialogView.findViewById(R.id.etNote);

        etAmount.setText(formatDouble(invoice.getTotalAmount()));
        etPaidAt.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));

        if (invoice.getId() != null && !invoice.getId().trim().isEmpty()) {
            scopedCollection.get("payments")
                    .whereEqualTo("invoiceId", invoice.getId())
                    .get()
                    .addOnSuccessListener(qs -> {
                        double paid = 0;
                        if (qs != null) {
                            for (QueryDocumentSnapshot doc : qs) {
                                Double amt = doc.getDouble("amount");
                                if (amt != null) {
                                    paid += amt;
                                }
                            }
                        }
                        double remaining = Math.max(0, invoice.getTotalAmount() - paid);
                        if (remaining > 0) {
                            etAmount.setText(formatDouble(remaining));
                        }
                    });
        }

        ArrayAdapter<String> methodAdapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item,
                new String[] { activity.getString(R.string.cash), activity.getString(R.string.bank_transfer) });
        methodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMethod.setAdapter(methodAdapter);

        new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.collect_payment_room, invoice.getRoomNumber()))
                .setView(dialogView)
                .setPositiveButton(activity.getString(R.string.confirm), (d, w) -> {
                    try {
                        double amount = parseDouble(etAmount);
                        if (amount <= 0) {
                            Toast.makeText(activity, activity.getString(R.string.amount_must_positive),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String method = spinnerMethod.getSelectedItemPosition() == 0 ? "CASH" : "BANK";
                        String paidAt = etPaidAt.getText().toString().trim();
                        String note = etNote.getText().toString().trim();

                        submitPayment(activity, invoice, amount, method, paidAt, note,
                                scopedCollection, paymentRepository, viewModel);
                    } catch (NumberFormatException e) {
                        Toast.makeText(activity, activity.getString(R.string.invalid_amount), Toast.LENGTH_SHORT)
                                .show();
                    }
                })
                .setNeutralButton(activity.getString(R.string.qr_transfer),
                        (d, w) -> showVietQrDialog(activity, db, invoice))
                .setNegativeButton(activity.getString(R.string.cancel), null)
                .show();
    }

    private static void showVietQrDialog(@NonNull AppCompatActivity activity,
            @NonNull FirebaseFirestore db,
            @NonNull Invoice invoice) {
        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId == null || tenantId.trim().isEmpty()) {
            Toast.makeText(activity, activity.getString(R.string.qr_tenant_only), Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("tenants").document(tenantId).get().addOnSuccessListener(tdoc -> {
            String bankCode = tdoc.getString("bankCode");
            String bankNo = tdoc.getString("bankAccountNo");
            String bankName = tdoc.getString("bankAccountName");

            if (bankCode == null || bankCode.trim().isEmpty() || bankNo == null || bankNo.trim().isEmpty()) {
                Toast.makeText(activity, activity.getString(R.string.bank_not_configured), Toast.LENGTH_LONG).show();
                return;
            }

            String addInfo = "HD " + invoice.getRoomNumber() + " " + invoice.getBillingPeriod();
            String url = buildVietQrUrl(
                    bankCode.trim(),
                    bankNo.trim(),
                    bankName != null ? bankName.trim() : "",
                    (long) invoice.getTotalAmount(),
                    addInfo);

            View v = activity.getLayoutInflater().inflate(R.layout.dialog_vietqr, null);
            android.widget.ImageView img = v.findViewById(R.id.imgQr);
            android.widget.TextView tvBank = v.findViewById(R.id.tvQrBank);
            android.widget.TextView tvNote = v.findViewById(R.id.tvQrNote);

            tvBank.setText(activity.getString(R.string.transfer_info_colon) + bankCode + " - " + bankNo
                    + (bankName != null && !bankName.isEmpty() ? (" (" + bankName + ")") : ""));
            tvNote.setText(activity.getString(R.string.content_colon) + addInfo + "\n"
                    + activity.getString(R.string.amount_colon) + formatDouble(invoice.getTotalAmount()));

            com.bumptech.glide.Glide.with(activity).load(url).into(img);

            new androidx.appcompat.app.AlertDialog.Builder(activity)
                    .setTitle(activity.getString(R.string.vietqr_title))
                    .setView(v)
                    .setPositiveButton(activity.getString(R.string.close), null)
                    .show();
        });
    }

    private static String buildVietQrUrl(@NonNull String bankCode,
            @NonNull String accountNo,
            @NonNull String accountName,
            long amount,
            @NonNull String addInfo) {
        try {
            String base = "https://img.vietqr.io/image/" + bankCode + "-" + accountNo + "-compact2.png";
            String q = "?amount=" + amount
                    + "&addInfo=" + java.net.URLEncoder.encode(addInfo, StandardCharsets.UTF_8.name())
                    + "&accountName=" + java.net.URLEncoder.encode(accountName, StandardCharsets.UTF_8.name());
            return base + q;
        } catch (Exception e) {
            return "";
        }
    }

    private static void submitPayment(@NonNull AppCompatActivity activity,
            @NonNull Invoice invoice,
            double amount,
            @NonNull String method,
            @NonNull String paidAt,
            @NonNull String note,
            @NonNull CollectionProvider scopedCollection,
            @NonNull PaymentRepository paymentRepository,
            @NonNull InvoiceViewModel viewModel) {
        String invoiceId = invoice.getId();
        if (invoiceId == null || invoiceId.trim().isEmpty()) {
            Toast.makeText(activity, activity.getString(R.string.missing_invoice_id), Toast.LENGTH_SHORT).show();
            return;
        }

        scopedCollection.get("payments")
                .whereEqualTo("invoiceId", invoiceId)
                .get()
                .addOnSuccessListener(qs -> {
                    double paid = 0;
                    if (qs != null) {
                        for (QueryDocumentSnapshot doc : qs) {
                            Double amt = doc.getDouble("amount");
                            if (amt != null) {
                                paid += amt;
                            }
                        }
                    }

                    double remaining = Math.max(0, invoice.getTotalAmount() - paid);
                    if (amount > remaining + 0.01) {
                        Toast.makeText(activity,
                                activity.getString(R.string.amount_exceeds_remaining, formatDouble(remaining)),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    Payment p = new Payment();
                    p.setInvoiceId(invoiceId);
                    p.setRoomId(invoice.getRoomId());
                    p.setAmount(amount);
                    p.setMethod(method);
                    p.setPaidAt(paidAt);
                    p.setNote(note);

                    paymentRepository.add(p,
                            () -> {
                                recomputeAndUpdateInvoiceStatus(invoice, scopedCollection, viewModel);
                                activity.runOnUiThread(() -> Toast
                                        .makeText(activity, activity.getString(R.string.payment_recorded),
                                                Toast.LENGTH_SHORT)
                                        .show());
                            },
                            () -> activity.runOnUiThread(() -> Toast
                                    .makeText(activity, activity.getString(R.string.payment_record_failed),
                                            Toast.LENGTH_SHORT)
                                    .show()));
                })
                .addOnFailureListener(e -> Toast
                        .makeText(activity, activity.getString(R.string.cannot_check_debt), Toast.LENGTH_SHORT).show());
    }

    private static double parseDouble(@NonNull EditText et) {
        String s = et.getText() == null ? "" : et.getText().toString().replace(",", "").trim();
        return s.isEmpty() ? 0 : Double.parseDouble(s);
    }

    private static String formatDouble(double value) {
        return value % 1 == 0 ? String.valueOf((long) value) : String.valueOf(value);
    }
}
