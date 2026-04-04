package com.example.myapplication.core.widget;

import android.app.AlertDialog;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;

import com.example.myapplication.R;

import java.util.Calendar;
import java.util.Locale;

public final class MonthYearPickerDialog {

    public interface OnMonthYearSetListener {
        void onMonthYearSet(int year, int month);
    }

    private MonthYearPickerDialog() {
    }

    public static void show(
            @NonNull Context context,
            int initialYear,
            int initialMonth,
            int minYear,
            int maxYear,
            @NonNull OnMonthYearSetListener listener) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.HORIZONTAL);
        int pad = dp(context, 20);
        container.setPadding(pad, pad, pad, 0);

        NumberPicker monthPicker = new NumberPicker(context);
        monthPicker.setMinValue(1);
        monthPicker.setMaxValue(12);
        monthPicker.setWrapSelectorWheel(true);
        monthPicker.setValue(clamp(initialMonth + 1, 1, 12));
        monthPicker.setFormatter(value -> String.format(Locale.US, "%02d", value));

        NumberPicker yearPicker = new NumberPicker(context);
        yearPicker.setMinValue(minYear);
        yearPicker.setMaxValue(maxYear);
        yearPicker.setWrapSelectorWheel(false);
        yearPicker.setValue(clamp(initialYear, minYear, maxYear));

        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f);
        container.addView(monthPicker, itemParams);
        container.addView(yearPicker, itemParams);

        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.month_year_picker_title))
                .setView(container)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> listener.onMonthYearSet(
                        yearPicker.getValue(),
                        monthPicker.getValue() - 1))
                .setNegativeButton(context.getString(R.string.cancel), null)
                .show();
    }

    public static int defaultMinYear() {
        return 2000;
    }

    public static int defaultMaxYear() {
        return Calendar.getInstance().get(Calendar.YEAR) + 10;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int dp(@NonNull Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}
