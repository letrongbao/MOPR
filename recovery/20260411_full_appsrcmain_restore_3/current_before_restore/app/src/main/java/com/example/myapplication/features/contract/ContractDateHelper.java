package com.example.myapplication.features.contract;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class ContractDateHelper {

    private ContractDateHelper() {
    }

    @NonNull
    public static String computeEndDate(String start, int months) {
        try {
            Calendar c = parseContractDate(start);
            if (c == null) {
                return "";
            }
            c.add(Calendar.MONTH, months);
            return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(c.getTime());
        } catch (Exception e) {
            return "";
        }
    }

    @NonNull
    public static String formatMonthYearForInput(String rawDate) {
        Calendar c = parseContractDate(rawDate);
        if (c == null) {
            return rawDate == null ? "" : rawDate;
        }
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(c.getTime());
    }

    @NonNull
    public static String normalizeMonthYearToStorage(String input) {
        Calendar c = parseContractDate(input);
        if (c == null) {
            return "";
        }
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(c.getTime());
    }

    public static Calendar parseContractDate(String value) {
        if (value == null) {
            return null;
        }
        String input = value.trim();
        if (input.isEmpty()) {
            return null;
        }

        Calendar parsed = parseWithPattern(input, "dd/MM/yyyy");
        if (parsed != null) {
            return parsed;
        }

        parsed = parseWithPattern(input, "MM/yyyy");
        if (parsed != null) {
            parsed.set(Calendar.DAY_OF_MONTH, 1);
            return parsed;
        }

        return null;
    }

    private static Calendar parseWithPattern(String input, String pattern) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
            sdf.setLenient(false);
            Date d = sdf.parse(input);
            if (d == null) {
                return null;
            }
            Calendar c = Calendar.getInstance();
            c.setTime(d);
            return c;
        } catch (Exception e) {
            return null;
        }
    }
}
