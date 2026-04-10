package com.example.myapplication.core.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

public final class FinancePeriodUtil {
    private FinancePeriodUtil() {
    }

    @NonNull
    public static String normalizeMonthYear(@Nullable String text) {
        if (text == null) {
            return "";
        }

        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        String[] parts = trimmed.split("/");
        try {
            if (parts.length == 2) {
                int month = Integer.parseInt(parts[0].trim());
                int year = Integer.parseInt(parts[1].trim());
                if (month < 1 || month > 12 || year < 1) {
                    return "";
                }
                return String.format(Locale.US, "%02d/%04d", month, year);
            }

            if (parts.length == 3) {
                int month = Integer.parseInt(parts[1].trim());
                int year = Integer.parseInt(parts[2].trim());
                if (month < 1 || month > 12 || year < 1) {
                    return "";
                }
                return String.format(Locale.US, "%02d/%04d", month, year);
            }
        } catch (NumberFormatException ignored) {
            return "";
        }

        return "";
    }
}