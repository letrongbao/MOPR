package com.example.myapplication.features.invoice;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.myapplication.core.util.MoneyFormatter;

import java.util.function.Supplier;

public final class InvoiceFormValueHelper {

    private InvoiceFormValueHelper() {
    }

    public static double parseDouble(@NonNull EditText et) {
        String s = et.getText() != null ? et.getText().toString().replace(",", "").trim() : "";
        return s.isEmpty() ? 0 : Double.parseDouble(s);
    }

    @NonNull
    public static String formatDouble(double value) {
        return value % 1 == 0 ? String.valueOf((long) value) : String.valueOf(value);
    }

    public static void setupEstimatedTotal(@NonNull EditText etDienDau,
            @NonNull EditText etDienCuoi,
            @NonNull EditText etDonGiaDien,
            @NonNull EditText etNuocDau,
            @NonNull EditText etNuocCuoi,
            @NonNull EditText etDonGiaNuoc,
            @NonNull EditText etPhiRac,
            @NonNull EditText etPhiWifi,
            @NonNull EditText etPhiGuiXe,
            @NonNull TextView tvEstimatedTotal,
            @NonNull Supplier<Double> rentSupplier) {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateEstimatedTotal(etDienDau, etDienCuoi, etDonGiaDien, etNuocDau, etNuocCuoi, etDonGiaNuoc,
                        etPhiRac, etPhiWifi, etPhiGuiXe, tvEstimatedTotal, rentSupplier);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        etDienDau.addTextChangedListener(watcher);
        etDienCuoi.addTextChangedListener(watcher);
        etNuocDau.addTextChangedListener(watcher);
        etNuocCuoi.addTextChangedListener(watcher);
        etDonGiaDien.addTextChangedListener(watcher);
        etDonGiaNuoc.addTextChangedListener(watcher);
        etPhiRac.addTextChangedListener(watcher);
        etPhiWifi.addTextChangedListener(watcher);
        etPhiGuiXe.addTextChangedListener(watcher);
    }

    public static void updateEstimatedTotal(@NonNull EditText etDienDau,
            @NonNull EditText etDienCuoi,
            @NonNull EditText etDonGiaDien,
            @NonNull EditText etNuocDau,
            @NonNull EditText etNuocCuoi,
            @NonNull EditText etDonGiaNuoc,
            @NonNull EditText etPhiRac,
            @NonNull EditText etPhiWifi,
            @NonNull EditText etPhiGuiXe,
            @NonNull TextView tvEstimatedTotal,
            @NonNull Supplier<Double> rentSupplier) {
        double dienDau = parseDoubleSafe(etDienDau);
        double dienCuoi = parseDoubleSafe(etDienCuoi);
        double nuocDau = parseDoubleSafe(etNuocDau);
        double nuocCuoi = parseDoubleSafe(etNuocCuoi);
        double electricUnitPrice = MoneyFormatter.getValue(etDonGiaDien);
        double waterUnitPrice = MoneyFormatter.getValue(etDonGiaNuoc);
        double trashFee = MoneyFormatter.getValue(etPhiRac);
        double wifiFee = MoneyFormatter.getValue(etPhiWifi);
        double parkingFee = MoneyFormatter.getValue(etPhiGuiXe);
        Double rent = rentSupplier.get();
        double rentAmount = rent != null ? rent : 0;

        double soDien = Math.max(0, dienCuoi - dienDau);
        double soNuoc = Math.max(0, nuocCuoi - nuocDau);
        double total = rentAmount + soDien * electricUnitPrice + soNuoc * waterUnitPrice + trashFee + wifiFee + parkingFee;
        tvEstimatedTotal.setText(MoneyFormatter.format(total));
    }

    private static double parseDoubleSafe(@NonNull EditText et) {
        try {
            return parseDouble(et);
        } catch (Exception e) {
            return 0;
        }
    }
}
