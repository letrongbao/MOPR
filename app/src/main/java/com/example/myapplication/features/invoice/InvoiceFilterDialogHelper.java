package com.example.myapplication.features.invoice;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.core.util.FinancePeriodUtil;
import com.example.myapplication.domain.Room;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public final class InvoiceFilterDialogHelper {

    public interface MonthPickedCallback {
        void onPicked(@NonNull String period, int month, int year);
    }

    public interface HousePickedCallback {
        void onPicked(String houseId, @NonNull String label);
    }

    private InvoiceFilterDialogHelper() {
    }

    public static void showMonthFilterDialog(@NonNull AppCompatActivity activity,
            String selectedMonth,
            @NonNull MonthPickedCallback callback) {
        View content = LayoutInflater.from(activity).inflate(R.layout.dialog_month_year_picker, null, false);
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        dialog.setContentView(content);

        NumberPicker npMonth = content.findViewById(R.id.npMonth);
        NumberPicker npYear = content.findViewById(R.id.npYear);
        TextView tvPreviewMonth = content.findViewById(R.id.tvPreviewMonth);
        MaterialButton btnCurrentMonth = content.findViewById(R.id.btnCurrentMonth);
        MaterialButton btnPreviousMonth = content.findViewById(R.id.btnPreviousMonth);
        MaterialButton btnCancel = content.findViewById(R.id.btnCancelMonthPicker);
        MaterialButton btnApply = content.findViewById(R.id.btnApplyMonthPicker);

        Calendar now = Calendar.getInstance();
        String normalizedSelected = FinancePeriodUtil.normalizeMonthYear(selectedMonth);

        int selectedM = now.get(Calendar.MONTH) + 1;
        int selectedY = now.get(Calendar.YEAR);
        if (normalizedSelected.matches("\\d{2}/\\d{4}")) {
            String[] parts = normalizedSelected.split("/");
            selectedM = Integer.parseInt(parts[0]);
            selectedY = Integer.parseInt(parts[1]);
        }

        int minYear = Math.max(2000, now.get(Calendar.YEAR) - 8);
        int maxYear = now.get(Calendar.YEAR) + 1;
        if (selectedY < minYear)
            selectedY = minYear;
        if (selectedY > maxYear)
            selectedY = maxYear;

        npMonth.setMinValue(1);
        npMonth.setMaxValue(12);
        npMonth.setValue(selectedM);
        npMonth.setWrapSelectorWheel(true);

        npYear.setMinValue(minYear);
        npYear.setMaxValue(maxYear);
        npYear.setValue(selectedY);
        npYear.setWrapSelectorWheel(false);

        Runnable updatePreview = () -> {
            int monthValue = npMonth.getValue();
            int yearValue = npYear.getValue();
            tvPreviewMonth.setText(String.format(Locale.getDefault(), "Tháng %d/%d", monthValue, yearValue));
        };
        updatePreview.run();

        npMonth.setOnValueChangedListener((picker, oldVal, newVal) -> updatePreview.run());
        npYear.setOnValueChangedListener((picker, oldVal, newVal) -> updatePreview.run());

        btnCurrentMonth.setOnClickListener(v -> {
            npMonth.setValue(now.get(Calendar.MONTH) + 1);
            npYear.setValue(now.get(Calendar.YEAR));
            updatePreview.run();
        });

        btnPreviousMonth.setOnClickListener(v -> {
            Calendar previous = Calendar.getInstance();
            previous.add(Calendar.MONTH, -1);
            npMonth.setValue(previous.get(Calendar.MONTH) + 1);
            npYear.setValue(previous.get(Calendar.YEAR));
            updatePreview.run();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnApply.setOnClickListener(v -> {
            String picked = String.format(Locale.US, "%02d/%04d", npMonth.getValue(), npYear.getValue());
            callback.onPicked(picked, npMonth.getValue(), npYear.getValue());
            dialog.dismiss();
        });

        dialog.show();
    }

    public static void showHouseFilterDialog(@NonNull AppCompatActivity activity,
            @NonNull List<Room> rooms,
            String selectedHouseId,
            @NonNull HousePickedCallback callback) {
        if (rooms.isEmpty()) {
            Toast.makeText(activity, "Chưa có dữ liệu căn nhà", Toast.LENGTH_SHORT).show();
            return;
        }

        LinkedHashMap<String, String> houseMap = new LinkedHashMap<>();
        houseMap.put("", "Tất cả căn nhà");
        for (Room room : rooms) {
            if (room == null)
                continue;
            String houseId = room.getHouseId();
            String houseName = room.getHouseTen();
            if (houseId == null || houseId.trim().isEmpty())
                continue;
            if (houseName == null || houseName.trim().isEmpty())
                houseName = "Căn nhà";
            houseMap.put(houseId, houseName);
        }

        List<String> ids = new ArrayList<>(houseMap.keySet());
        List<String> labels = new ArrayList<>(houseMap.values());
        int checked = Math.max(0, ids.indexOf(selectedHouseId == null ? "" : selectedHouseId));

        new AlertDialog.Builder(activity)
                .setTitle("Chọn căn nhà")
                .setSingleChoiceItems(labels.toArray(new String[0]), checked, (dialog, which) -> {
                    callback.onPicked(ids.get(which), labels.get(which));
                    dialog.dismiss();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
