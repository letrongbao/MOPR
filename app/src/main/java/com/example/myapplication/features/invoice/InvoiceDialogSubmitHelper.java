package com.example.myapplication.features.invoice;

import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

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
        public final EditText etPhiInternet;
        public final EditText etPhiGiatUi;
        public final EditText etPhiGuiXe;
        public final EditText etPhiKhac;
        public final TextView tvPhiKhacChiTiet;

        FormRefs(@NonNull View dialogView) {
            etThangNam = dialogView.findViewById(R.id.etThangNam);
            etDienDau = dialogView.findViewById(R.id.etDienDau);
            etDienCuoi = dialogView.findViewById(R.id.etDienCuoi);
            etDonGiaDien = dialogView.findViewById(R.id.etDonGiaDien);
            etNuocDau = dialogView.findViewById(R.id.etNuocDau);
            etNuocCuoi = dialogView.findViewById(R.id.etNuocCuoi);
            etDonGiaNuoc = dialogView.findViewById(R.id.etDonGiaNuoc);
            etPhiRac = dialogView.findViewById(R.id.etPhiRac);
            etPhiInternet = dialogView.findViewById(R.id.etPhiInternet);
            etPhiGiatUi = dialogView.findViewById(R.id.etPhiGiatUi);
            etPhiGuiXe = dialogView.findViewById(R.id.etPhiGuiXe);
            etPhiKhac = dialogView.findViewById(R.id.etPhiKhac);
            tvPhiKhacChiTiet = dialogView.findViewById(R.id.tvPhiKhacChiTiet);
        }

        public void applyMoneyFormatting() {
            MoneyFormatter.applyTo(etDonGiaDien);
            MoneyFormatter.applyTo(etDonGiaNuoc);
            MoneyFormatter.applyTo(etPhiRac);
            MoneyFormatter.applyTo(etPhiInternet);
            MoneyFormatter.applyTo(etPhiGiatUi);
            MoneyFormatter.applyTo(etPhiGuiXe);
            MoneyFormatter.applyTo(etPhiKhac);
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
            double minWaterStart,
            @NonNull String startReadingInvalidMessage,
            @NonNull String endReadingInvalidMessage) throws ValidationException {
        double dienDau = InvoiceFormValueHelper.parseDouble(form.etDienDau);
        double dienCuoi = InvoiceFormValueHelper.parseDouble(form.etDienCuoi);
        double nuocDau = InvoiceFormValueHelper.parseDouble(form.etNuocDau);
        double nuocCuoi = InvoiceFormValueHelper.parseDouble(form.etNuocCuoi);

        if (dienDau < minElecStart || nuocDau < minWaterStart) {
            throw new ValidationException(startReadingInvalidMessage);
        }
        if (dienCuoi < dienDau || nuocCuoi < nuocDau) {
            throw new ValidationException(endReadingInvalidMessage);
        }

        Invoice invoice = new Invoice();
        invoice.setRoomId(room.getId());
        invoice.setRoomNumber(room.getRoomNumber());
        if (activeContract != null && activeContract.getId() != null && !activeContract.getId().trim().isEmpty()) {
            invoice.setContractId(activeContract.getId());
            long contractRent = activeContract.getRentAmount();
            invoice.setRentAmount(contractRent > 0 ? contractRent : room.getRentAmount());
        } else {
            invoice.setRentAmount(room.getRentAmount());
        }

        invoice.setBillingPeriod(form.etThangNam.getText().toString().trim());
        invoice.setElectricStartReading(dienDau);
        invoice.setElectricEndReading(dienCuoi);
        invoice.setElectricUnitPrice(MoneyFormatter.getValue(form.etDonGiaDien));
        invoice.setWaterStartReading(nuocDau);
        invoice.setWaterEndReading(nuocCuoi);
        invoice.setWaterUnitPrice(MoneyFormatter.getValue(form.etDonGiaNuoc));
        invoice.setTrashFee(MoneyFormatter.getValue(form.etPhiRac));
        invoice.setInternetFee(MoneyFormatter.getValue(form.etPhiInternet));
        invoice.setLaundryFee(MoneyFormatter.getValue(form.etPhiGiatUi));
        invoice.setParkingFee(MoneyFormatter.getValue(form.etPhiGuiXe));
        invoice.setOtherFee(MoneyFormatter.getValue(form.etPhiKhac));
        invoice.setStatus(InvoiceStatus.UNREPORTED);
        return invoice;
    }

    @NonNull
    public static Invoice buildUpdatedInvoice(@NonNull Invoice original,
            @NonNull Room room,
            Tenant activeContract,
            java.util.List<String> otherFeeLines,
            @NonNull FormRefs form,
            @NonNull String endReadingInvalidMessage) throws ValidationException {
        double dienDau = InvoiceFormValueHelper.parseDouble(form.etDienDau);
        double dienCuoi = InvoiceFormValueHelper.parseDouble(form.etDienCuoi);
        double nuocDau = InvoiceFormValueHelper.parseDouble(form.etNuocDau);
        double nuocCuoi = InvoiceFormValueHelper.parseDouble(form.etNuocCuoi);

        if (dienCuoi < dienDau || nuocCuoi < nuocDau) {
            throw new ValidationException(endReadingInvalidMessage);
        }

        Invoice updated = new Invoice();
        updated.setId(original.getId());
        updated.setContractId(original.getContractId());
        updated.setRoomId(room.getId());
        updated.setRoomNumber(room.getRoomNumber());
        if (activeContract != null && activeContract.getId() != null && !activeContract.getId().trim().isEmpty()) {
            long contractRent = activeContract.getRentAmount();
            updated.setRentAmount(contractRent > 0 ? contractRent : room.getRentAmount());
        } else {
            updated.setRentAmount(room.getRentAmount());
        }
        updated.setBillingPeriod(form.etThangNam.getText().toString().trim());
        updated.setElectricStartReading(dienDau);
        updated.setElectricEndReading(dienCuoi);
        updated.setElectricUnitPrice(MoneyFormatter.getValue(form.etDonGiaDien));
        updated.setWaterStartReading(nuocDau);
        updated.setWaterEndReading(nuocCuoi);
        updated.setWaterUnitPrice(MoneyFormatter.getValue(form.etDonGiaNuoc));
        updated.setTrashFee(MoneyFormatter.getValue(form.etPhiRac));
        updated.setInternetFee(MoneyFormatter.getValue(form.etPhiInternet));
        updated.setLaundryFee(MoneyFormatter.getValue(form.etPhiGiatUi));
        updated.setParkingFee(MoneyFormatter.getValue(form.etPhiGuiXe));
        updated.setOtherFee(MoneyFormatter.getValue(form.etPhiKhac));
        updated.setOtherFeeLines(otherFeeLines);
        updated.setStatus(original.getStatus());
        return updated;
    }

    public static final class ValidationException extends Exception {
        ValidationException(@NonNull String message) {
            super(message);
        }
    }
}
