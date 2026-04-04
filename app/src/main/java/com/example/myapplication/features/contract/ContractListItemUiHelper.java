package com.example.myapplication.features.contract;

import android.graphics.Color;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.myapplication.domain.ContractStatus;
import com.example.myapplication.domain.Tenant;
import com.google.android.material.chip.Chip;

import java.util.Locale;

public final class ContractListItemUiHelper {

    private static final long THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000;

    private ContractListItemUiHelper() {
    }

    @NonNull
    public static String formatMoney(long value) {
        return String.format(Locale.US, "%,d ₫", value).replace(',', '.');
    }

    @NonNull
    public static String displayRepresentativeName(@NonNull Tenant contract) {
        String representativeName = contract.getRepresentativeName();
        if (representativeName == null || representativeName.trim().isEmpty()) {
            representativeName = contract.getFullName();
        }
        return representativeName != null ? representativeName : "—";
    }

    public static void updateContractStatusChip(@NonNull Chip chip,
            @NonNull ContractStatus status,
            long daysLeft,
            @NonNull Tenant contract) {
        android.content.Context context = chip.getContext();
        long contractEndTimestamp = contract.getContractEndTimestamp();
        if (contractEndTimestamp > 0) {
            long currentTime = System.currentTimeMillis();
            long timeRemaining = contractEndTimestamp - currentTime;

            if (timeRemaining < 0) {
                chip.setText(context.getString(com.example.myapplication.R.string.expired));
                chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
                chip.setTextColor(Color.WHITE);
                return;
            } else if (timeRemaining < THIRTY_DAYS_MS) {
                long daysLeftNew = timeRemaining / (24 * 60 * 60 * 1000);
                chip.setText(context.getString(com.example.myapplication.R.string.contract_status_days_left, daysLeftNew));
                chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336")));
                chip.setTextColor(Color.WHITE);
                return;
            }

            chip.setText(context.getString(com.example.myapplication.R.string.active_valid));
            chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            chip.setTextColor(Color.WHITE);
            return;
        }

        switch (status) {
            case ENDED:
                chip.setText(context.getString(com.example.myapplication.R.string.expired));
                chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
                chip.setTextColor(Color.WHITE);
                break;
            case EXPIRING_SOON:
                String text = context.getString(com.example.myapplication.R.string.contract_status_expiring_soon);
                if (daysLeft >= 0) {
                    text = context.getString(com.example.myapplication.R.string.contract_status_days_left, daysLeft);
                }
                chip.setText(text);
                chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336")));
                chip.setTextColor(Color.WHITE);
                break;
            case ACTIVE_RENTAL:
            default:
                chip.setText(context.getString(com.example.myapplication.R.string.active_valid));
                chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                chip.setTextColor(Color.WHITE);
                break;
        }
    }

    public static void updateDepositStatusDisplay(@NonNull TextView tvDepositStatus,
            @NonNull Tenant contract,
            @NonNull ContractStatus status) {
        if (status == ContractStatus.ENDED) {
            tvDepositStatus.setVisibility(android.view.View.GONE);
            return;
        }

        tvDepositStatus.setVisibility(android.view.View.VISIBLE);
        if (contract.isDepositCollected()) {
            tvDepositStatus.setText(tvDepositStatus.getContext().getString(com.example.myapplication.R.string.deposit_collected));
            tvDepositStatus.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            tvDepositStatus.setText(tvDepositStatus.getContext().getString(com.example.myapplication.R.string.deposit_pending));
            tvDepositStatus.setTextColor(Color.parseColor("#FF9800"));
        }
    }
}
