package com.example.myapplication.features.invoice;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.myapplication.R;
import com.example.myapplication.domain.Invoice;
import com.example.myapplication.domain.Room;

import java.util.List;
import java.util.function.Supplier;

public final class InvoiceDialogUiHelper {

    private InvoiceDialogUiHelper() {
    }

    public static void bindRoomSpinner(@NonNull Context context,
            @NonNull Spinner spinner,
            @NonNull List<Room> rooms) {
        String[] roomNames = rooms.stream()
                .map(p -> context.getString(R.string.room_number, p.getRoomNumber()))
                .toArray(String[]::new);
        ArrayAdapter<String> roomAdapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_item,
                roomNames);
        roomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(roomAdapter);
    }

    public static int findRoomIndexById(@NonNull List<Room> rooms, String roomId) {
        if (roomId == null || roomId.trim().isEmpty()) {
            return -1;
        }
        for (int i = 0; i < rooms.size(); i++) {
            if (roomId.equals(rooms.get(i).getId())) {
                return i;
            }
        }
        return -1;
    }

    public static void setupEstimatedTotal(@NonNull InvoiceDialogSubmitHelper.FormRefs form,
            @NonNull TextView tvEstimatedTotal,
            @NonNull Supplier<Double> rentSupplier) {
        InvoiceFormValueHelper.setupEstimatedTotal(
                form.etDienDau,
                form.etDienCuoi,
                form.etDonGiaDien,
                form.etNuocDau,
                form.etNuocCuoi,
                form.etDonGiaNuoc,
                form.etPhiRac,
                form.etPhiInternet,
                form.etPhiGiatUi,
                form.etPhiGuiXe,
                form.etPhiKhac,
                tvEstimatedTotal,
                rentSupplier);
    }

    public static void refreshEstimatedTotal(@NonNull InvoiceDialogSubmitHelper.FormRefs form,
            @NonNull TextView tvEstimatedTotal,
            @NonNull Supplier<Double> rentSupplier) {
        InvoiceFormValueHelper.updateEstimatedTotal(
                form.etDienDau,
                form.etDienCuoi,
                form.etDonGiaDien,
                form.etNuocDau,
                form.etNuocCuoi,
                form.etDonGiaNuoc,
                form.etPhiRac,
                form.etPhiInternet,
                form.etPhiGiatUi,
                form.etPhiGuiXe,
                form.etPhiKhac,
                tvEstimatedTotal,
                rentSupplier);
    }

    public static void setMeterStartReadOnly(@NonNull InvoiceDialogSubmitHelper.FormRefs form) {
        form.etDienDau.setEnabled(false);
        form.etDienDau.setFocusable(false);
        form.etDienDau.setAlpha(0.75f);
        form.etNuocDau.setEnabled(false);
        form.etNuocDau.setFocusable(false);
        form.etNuocDau.setAlpha(0.75f);
    }

    public static void fillFormFromInvoice(@NonNull InvoiceDialogSubmitHelper.FormRefs form,
            @NonNull Invoice invoice) {
        form.etThangNam.setText(invoice.getBillingPeriod());
        form.etDienDau.setText(InvoiceFormValueHelper.formatDouble(invoice.getElectricStartReading()));
        form.etDienCuoi.setText(InvoiceFormValueHelper.formatDouble(invoice.getElectricEndReading()));
        form.etDonGiaDien.setText(InvoiceFormValueHelper.formatDouble(invoice.getElectricUnitPrice()));
        form.etNuocDau.setText(InvoiceFormValueHelper.formatDouble(invoice.getWaterStartReading()));
        form.etNuocCuoi.setText(InvoiceFormValueHelper.formatDouble(invoice.getWaterEndReading()));
        form.etDonGiaNuoc.setText(InvoiceFormValueHelper.formatDouble(invoice.getWaterUnitPrice()));
        form.etPhiRac.setText(InvoiceFormValueHelper.formatDouble(invoice.getTrashFee()));
        form.etPhiInternet.setText(InvoiceFormValueHelper.formatDouble(invoice.getInternetFee()));
        form.etPhiGiatUi.setText(InvoiceFormValueHelper.formatDouble(invoice.getLaundryFee()));
        form.etPhiGuiXe.setText(InvoiceFormValueHelper.formatDouble(invoice.getParkingFee()));
        form.etPhiKhac.setText(InvoiceFormValueHelper.formatDouble(invoice.getOtherFee()));

        if (form.tvPhiKhacChiTiet != null) {
            List<String> lines = invoice.getOtherFeeLines();
            if (lines == null || lines.isEmpty()) {
                form.tvPhiKhacChiTiet.setText("");
                form.tvPhiKhacChiTiet.setVisibility(android.view.View.GONE);
            } else {
                form.tvPhiKhacChiTiet.setText(String.join("\n", lines));
                form.tvPhiKhacChiTiet.setVisibility(android.view.View.VISIBLE);
            }
        }
    }

    public static void lockIdentityAndMeterStartFields(@NonNull Spinner spinnerPhong,
            @NonNull InvoiceDialogSubmitHelper.FormRefs form) {
        spinnerPhong.setEnabled(false);
        spinnerPhong.setAlpha(0.6f);
        form.etThangNam.setEnabled(false);
        form.etThangNam.setAlpha(0.6f);
        setMeterStartReadOnly(form);
    }
}
