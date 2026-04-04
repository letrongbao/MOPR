package com.example.myapplication.core.util;

import com.example.myapplication.domain.ContractStatus;
import com.example.myapplication.domain.Tenant;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Internal note.
 * Internal note.
 */
public class ContractStatusHelper {

    private static final long THIRTY_DAYS_MS = TimeUnit.DAYS.toMillis(30);
    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    /**
     * Internal note.
     *
     * Logic:
     * Internal note.
     * Internal note.
     * Internal note.
     * Internal note.
     */
    public static ContractStatus resolve(Tenant contract) {
        if (contract == null) return ContractStatus.ENDED;

        // Internal note.
        if ("ENDED".equalsIgnoreCase(contract.getContractStatus())) {
            return ContractStatus.ENDED;
        }

        String contractEndTimestamp = contract.getContractEndDate();
        if (contractEndTimestamp == null || contractEndTimestamp.trim().isEmpty()) {
            return ContractStatus.ACTIVE_RENTAL;
        }

        try {
            Date endDate = SDF.parse(contractEndTimestamp);
            if (endDate == null) return ContractStatus.ACTIVE_RENTAL;

            long now = System.currentTimeMillis();
            long endMs = endDate.getTime();
            long diff = endMs - now;

            if (diff < 0) {
                return ContractStatus.ENDED;
            } else if (diff <= THIRTY_DAYS_MS) {
                return ContractStatus.EXPIRING_SOON;
            } else {
                return ContractStatus.ACTIVE_RENTAL;
            }
        } catch (Exception e) {
            return ContractStatus.ACTIVE_RENTAL;
        }
    }

    /**
     * Internal note.
     * Internal note.
     */
    public static long daysRemaining(Tenant contract) {
        if (contract == null || contract.getContractEndDate() == null) return -999;
        try {
            Date endDate = SDF.parse(contract.getContractEndDate());
            if (endDate == null) return -999;
            long diff = endDate.getTime() - System.currentTimeMillis();
            return TimeUnit.MILLISECONDS.toDays(diff);
        } catch (Exception e) {
            return -999;
        }
    }
}

