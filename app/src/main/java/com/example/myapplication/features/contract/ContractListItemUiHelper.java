package com.example.myapplication.features.contract;

import android.graphics.Color;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.myapplication.domain.ContractStatus;
import com.example.myapplication.domain.Tenant;
import com.google.android.material.chip.Chip;

public final class ContractListItemUiHelper {

    private static final long THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000;

    private ContractListItemUiHelper() {
    }

    @NonNull
    public static String formatMoney(long value) {
        return String.format("%,dđ", value).replace(',', '.');
    }

    @NonNull
    public static String displayRepresentativeName(@NonNull Tenant contract) {
        String tenNguoiDaiDien = contract.getTenNguoiDaiDien();
        if (tenNguoiDaiDien == null || tenNguoiDaiDien.trim().isEmpty()) {
            tenNguoiDaiDien = contract.getHoTen();
        }
        return tenNguoiDaiDien != null ? tenNguoiDaiDien : "—";
    }

    public static void updateContractStatusChip(@NonNull Chip chip,
            @NonNull ContractStatus status,
            long daysLeft,
            @NonNull Tenant contract) {
        long ngayKetThuc = contract.getNgayKetThuc();
        if (ngayKetThuc > 0) {
            long currentTime = System.currentTimeMillis();
            long timeRemaining = ngayKetThuc - currentTime;

            if (timeRemaining < 0) {
                chip.setText("Hết hạn");
                chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
                chip.setTextColor(Color.WHITE);
                return;
            } else if (timeRemaining < THIRTY_DAYS_MS) {
                long daysLeftNew = timeRemaining / (24 * 60 * 60 * 1000);
                chip.setText("⚠ Còn " + daysLeftNew + " ngày");
                chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336")));
                chip.setTextColor(Color.WHITE);
                return;
            }

            chip.setText("✓ Đang hiệu lực");
            chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            chip.setTextColor(Color.WHITE);
            return;
        }

        switch (status) {
            case DA_KET_THUC:
                chip.setText("Hết hạn");
                chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
                chip.setTextColor(Color.WHITE);
                break;
            case SAP_HET_HAN:
                String text = "⚠ Sắp hết hạn";
                if (daysLeft >= 0) {
                    text = "⚠ Còn " + daysLeft + " ngày";
                }
                chip.setText(text);
                chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336")));
                chip.setTextColor(Color.WHITE);
                break;
            case DANG_THUE:
            default:
                chip.setText("✓ Đang hiệu lực");
                chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                chip.setTextColor(Color.WHITE);
                break;
        }
    }

    public static void updateDepositStatusDisplay(@NonNull TextView tvDepositStatus,
            @NonNull Tenant contract,
            @NonNull ContractStatus status) {
        if (status == ContractStatus.DA_KET_THUC) {
            tvDepositStatus.setVisibility(android.view.View.GONE);
            return;
        }

        tvDepositStatus.setVisibility(android.view.View.VISIBLE);
        if (contract.isTrangThaiThuCoc()) {
            tvDepositStatus.setText("✓ Đã thu cọc");
            tvDepositStatus.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            tvDepositStatus.setText("⏳ Chờ thu cọc");
            tvDepositStatus.setTextColor(Color.parseColor("#FF9800"));
        }
    }
}
