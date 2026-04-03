package com.example.myapplication.features.invoice;

import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.example.myapplication.R;
import com.example.myapplication.core.constants.InvoiceStatus;
import com.example.myapplication.core.util.MoneyFormatter;
import com.example.myapplication.domain.Invoice;
import com.example.myapplication.domain.Room;
import com.example.myapplication.domain.Tenant;

public final class InvoiceDialogSubmitHelper {

    private InvoiceDialogSubmitHelper() {
    }

    public static final class FormRefs {
        public final EditText etThangNam;
        public final EditText etDienDau;
        public final EditText etDienCuoi;
        public final EditText etDonGiaDien;
        public final EditText etNuocDau;
        public final EditText etNuocCuoi;
        public final EditText etDonGiaNuoc;
        public final EditText etPhiRac;
        public final EditText etPhiWifi;
        public final EditText etPhiGuiXe;

        FormRefs(@NonNull View dialogView) {
            etThangNam = dialogView.findViewById(R.id.etThangNam);
            etDienDau = dialogView.findViewById(R.id.etDienDau);
            etDienCuoi = dialogView.findViewById(R.id.etDienCuoi);
            etDonGiaDien = dialogView.findViewById(R.id.etDonGiaDien);
            etNuocDau = dialogView.findViewById(R.id.etNuocDau);
            etNuocCuoi = dialogView.findViewById(R.id.etNuocCuoi);
            etDonGiaNuoc = dialogView.findViewById(R.id.etDonGiaNuoc);
            etPhiRac = dialogView.findViewById(R.id.etPhiRac);
            etPhiWifi = dialogView.findViewById(R.id.etPhiWifi);
            etPhiGuiXe = dialogView.findViewById(R.id.etPhiGuiXe);
        }

        public void applyMoneyFormatting() {
            MoneyFormatter.applyTo(etDonGiaDien);
            MoneyFormatter.applyTo(etDonGiaNuoc);
            MoneyFormatter.applyTo(etPhiRac);
            MoneyFormatter.applyTo(etPhiWifi);
            MoneyFormatter.applyTo(etPhiGuiXe);
        }
    }

    @NonNull
    public static FormRefs bind(@NonNull View dialogView) {
        return new FormRefs(dialogView);
    }

    @NonNull
    public static Invoice buildNewInvoice(@NonNull Room room,
            Tenant activeContract,
            @NonNull FormRefs form,
            double minElecStart,
            double minWaterStart) throws ValidationException {
        double dienDau = InvoiceFormValueHelper.parseDouble(form.etDienDau);
        double dienCuoi = InvoiceFormValueHelper.parseDouble(form.etDienCuoi);
        double nuocDau = InvoiceFormValueHelper.parseDouble(form.etNuocDau);
        double nuocCuoi = InvoiceFormValueHelper.parseDouble(form.etNuocCuoi);

        if (dienDau < minElecStart || nuocDau < minWaterStart) {
            throw new ValidationException("Chỉ số đầu kỳ phải >= chỉ số cuối kỳ trước");
        }
        if (dienCuoi < dienDau || nuocCuoi < nuocDau) {
            throw new ValidationException("Chỉ số cuối không được nhỏ hơn chỉ số đầu");
        }

        Invoice invoice = new Invoice();
        invoice.setIdPhong(room.getId());
        invoice.setSoPhong(room.getSoPhong());
        if (activeContract != null && activeContract.getId() != null && !activeContract.getId().trim().isEmpty()) {
            invoice.setIdTenant(activeContract.getId());
            long contractRent = activeContract.getGiaThue();
            invoice.setGiaThue(contractRent > 0 ? contractRent : room.getGiaThue());
        } else {
            invoice.setGiaThue(room.getGiaThue());
        }

        invoice.setThangNam(form.etThangNam.getText().toString().trim());
        invoice.setChiSoDienDau(dienDau);
        invoice.setChiSoDienCuoi(dienCuoi);
        invoice.setDonGiaDien(MoneyFormatter.getValue(form.etDonGiaDien));
        invoice.setChiSoNuocDau(nuocDau);
        invoice.setChiSoNuocCuoi(nuocCuoi);
        invoice.setDonGiaNuoc(MoneyFormatter.getValue(form.etDonGiaNuoc));
        invoice.setPhiRac(MoneyFormatter.getValue(form.etPhiRac));
        invoice.setPhiWifi(MoneyFormatter.getValue(form.etPhiWifi));
        invoice.setPhiGuiXe(MoneyFormatter.getValue(form.etPhiGuiXe));
        invoice.setTrangThai(InvoiceStatus.UNREPORTED);
        return invoice;
    }

    @NonNull
    public static Invoice buildUpdatedInvoice(@NonNull Invoice original,
            @NonNull Room room,
            @NonNull FormRefs form) throws ValidationException {
        double dienDau = InvoiceFormValueHelper.parseDouble(form.etDienDau);
        double dienCuoi = InvoiceFormValueHelper.parseDouble(form.etDienCuoi);
        double nuocDau = InvoiceFormValueHelper.parseDouble(form.etNuocDau);
        double nuocCuoi = InvoiceFormValueHelper.parseDouble(form.etNuocCuoi);

        if (dienCuoi < dienDau || nuocCuoi < nuocDau) {
            throw new ValidationException("Chỉ số cuối không được nhỏ hơn chỉ số đầu");
        }

        Invoice updated = new Invoice();
        updated.setId(original.getId());
        updated.setIdTenant(original.getIdTenant());
        updated.setIdPhong(room.getId());
        updated.setSoPhong(room.getSoPhong());
        updated.setGiaThue(room.getGiaThue());
        updated.setThangNam(form.etThangNam.getText().toString().trim());
        updated.setChiSoDienDau(dienDau);
        updated.setChiSoDienCuoi(dienCuoi);
        updated.setDonGiaDien(MoneyFormatter.getValue(form.etDonGiaDien));
        updated.setChiSoNuocDau(nuocDau);
        updated.setChiSoNuocCuoi(nuocCuoi);
        updated.setDonGiaNuoc(MoneyFormatter.getValue(form.etDonGiaNuoc));
        updated.setPhiRac(MoneyFormatter.getValue(form.etPhiRac));
        updated.setPhiWifi(MoneyFormatter.getValue(form.etPhiWifi));
        updated.setPhiGuiXe(MoneyFormatter.getValue(form.etPhiGuiXe));
        updated.setTrangThai(original.getTrangThai());
        return updated;
    }

    public static final class ValidationException extends Exception {
        ValidationException(@NonNull String message) {
            super(message);
        }
    }
}
