package com.example.myapplication.core.util;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Utility for formatting money amounts with thousand separators and currency
 * symbol.
 * Format: 10.000 đ (Vietnamese style)
 */
public class MoneyFormatter {

    private static final DecimalFormatSymbols symbols;
    private static final DecimalFormat decimalFormat;

    static {
        symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');

        decimalFormat = new DecimalFormat("#,###", symbols);
        decimalFormat.setGroupingUsed(true);
    }

    /**
     * Format amount for display: 10000 → "10.000 đ"
     */
    public static String format(double amount) {
        if (amount == 0)
            return "0 đ";
        return decimalFormat.format(amount) + " đ";
    }

    /**
     * Format amount for display without currency: 10000 → "10.000"
     */
    public static String formatWithoutCurrency(double amount) {
        if (amount == 0)
            return "0";
        return decimalFormat.format(amount);
    }

    /**
     * Parse formatted string back to double: "10.000 đ" → 10000.0
     */
    public static double parse(String formatted) {
        if (formatted == null || formatted.trim().isEmpty())
            return 0;

        // Remove currency symbol and spaces
        String cleaned = formatted.replace("đ", "")
                .replace(" ", "")
                .replace(".", "")
                .replace(",", ".")
                .trim();

        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Create TextWatcher for EditText to auto-format money input.
     * Usage:
     * editText.addTextChangedListener(MoneyFormatter.createTextWatcher(editText));
     */
    public static TextWatcher createTextWatcher(final EditText editText) {
        return new TextWatcher() {
            private String current = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(current)) {
                    editText.removeTextChangedListener(this);

                    // Remove all non-digit characters
                    String cleanString = s.toString().replaceAll("[^\\d]", "");

                    if (cleanString.isEmpty()) {
                        current = "";
                        editText.setText("");
                    } else {
                        try {
                            double parsed = Double.parseDouble(cleanString);
                            String formatted = formatWithoutCurrency(parsed);
                            current = formatted;
                            editText.setText(formatted);
                            editText.setSelection(formatted.length());
                        } catch (NumberFormatException e) {
                            // Keep original
                        }
                    }

                    editText.addTextChangedListener(this);
                }
            }
        };
    }

    /**
     * Apply money formatting to an EditText
     */
    public static void applyTo(EditText editText) {
        editText.addTextChangedListener(createTextWatcher(editText));
    }

    /**
     * Get raw double value from formatted EditText
     */
    public static double getValue(EditText editText) {
        String text = editText.getText().toString();
        return parse(text);
    }

    /**
     * Set formatted value to EditText
     */
    public static void setValue(EditText editText, double value) {
        editText.setText(formatWithoutCurrency(value));
    }
}
