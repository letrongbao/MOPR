package com.example.myapplication.features.invoice;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.myapplication.R;
import com.example.myapplication.core.constants.InvoiceStatus;
import com.example.myapplication.core.util.FinancePeriodUtil;
import com.example.myapplication.core.util.MoneyFormatter;
import com.example.myapplication.domain.Invoice;
import com.example.myapplication.domain.Room;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class InvoiceFilterCoordinator {

    private InvoiceFilterCoordinator() {
    }

    @NonNull
    public static List<Invoice> filter(@NonNull List<Invoice> invoices,
            @NonNull List<Room> rooms,
            @NonNull Map<String, String> tenantDisplayByRoom,
            String selectedMonth,
            String selectedHouseId,
            String searchQuery,
            int tabIndex) {
        String normalizedSelectedMonth = FinancePeriodUtil.normalizeMonthYear(selectedMonth);
        String normalizedSearch = searchQuery != null ? searchQuery.trim().toLowerCase(Locale.getDefault()) : "";

        Map<String, String> roomToHouse = new HashMap<>();
        for (Room room : rooms) {
            if (room == null || room.getId() == null) {
                continue;
            }
            roomToHouse.put(room.getId(), room.getHouseId());
        }

        List<Invoice> out = new ArrayList<>();
        for (Invoice invoice : invoices) {
            if (invoice == null) {
                continue;
            }

            String invoiceMonth = FinancePeriodUtil.normalizeMonthYear(invoice.getBillingPeriod());
            if (!normalizedSelectedMonth.isEmpty() && !normalizedSelectedMonth.equals(invoiceMonth)) {
                continue;
            }

            if (selectedHouseId != null && !selectedHouseId.trim().isEmpty()) {
                String invoiceHouseId = roomToHouse.get(invoice.getRoomId());
                if (invoiceHouseId == null || !selectedHouseId.equals(invoiceHouseId)) {
                    continue;
                }
            }

            if (!normalizedSearch.isEmpty()) {
                String roomName = invoice.getRoomNumber() != null
                        ? invoice.getRoomNumber().toLowerCase(Locale.getDefault())
                        : "";
                String tenant = tenantDisplayByRoom.get(invoice.getRoomId());
                tenant = tenant != null ? tenant.toLowerCase(Locale.getDefault()) : "";
                String month = invoice.getBillingPeriod() != null
                        ? invoice.getBillingPeriod().toLowerCase(Locale.getDefault())
                        : "";
                if (!roomName.contains(normalizedSearch)
                        && !tenant.contains(normalizedSearch)
                        && !month.contains(normalizedSearch)) {
                    continue;
                }
            }

            if (matchesTab(invoice, tabIndex)) {
                out.add(invoice);
            }
        }

        return out;
    }

    private static boolean matchesTab(@NonNull Invoice invoice, int tabIndex) {
        String status = invoice.getStatus();
        if (status == null || status.trim().isEmpty()) {
            status = InvoiceStatus.UNREPORTED;
        }

        if (tabIndex == 0) {
            return InvoiceStatus.UNREPORTED.equals(status);
        }
        if (tabIndex == 1) {
            return InvoiceStatus.REPORTED.equals(status);
        }
        if (tabIndex == 2) {
            return InvoiceStatus.PARTIAL.equals(status);
        }
        return InvoiceStatus.PAID.equals(status);
    }

    @NonNull
    public static String buildSummaryText(@NonNull Context context, @NonNull List<Invoice> visibleInvoices) {
        double total = 0;
        int unpaidCount = 0;
        for (Invoice invoice : visibleInvoices) {
            if (invoice == null) {
                continue;
            }
            total += invoice.getTotalAmount();
            String status = invoice.getStatus();
            if (status == null || status.trim().isEmpty()
                    || InvoiceStatus.UNREPORTED.equals(status)
                    || InvoiceStatus.REPORTED.equals(status)
                    || InvoiceStatus.PARTIAL.equals(status)) {
                unpaidCount++;
            }
        }

        return context.getString(
                R.string.invoice_filter_summary,
                visibleInvoices.size(),
                MoneyFormatter.format(total),
                unpaidCount);
    }
}

