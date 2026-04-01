package com.example.myapplication.core.util;

import com.example.myapplication.domain.ContractStatus;
import com.example.myapplication.domain.Tenant;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Helper tính toán trạng thái hợp đồng client-side.
 * Không ghi ngược về Firestore — chỉ dùng để hiển thị UI.
 */
public class ContractStatusHelper {

    private static final long THIRTY_DAYS_MS = TimeUnit.DAYS.toMillis(30);
    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    /**
     * Phân loại trạng thái hợp đồng dựa trên ngayKetThucHopDong và trangThaiHopDong.
     *
     * Logic:
     *   - Nếu trangThaiHopDong == "ENDED" → DA_KET_THUC
     *   - Nếu ngayKetThucHopDong đã qua    → DA_KET_THUC
     *   - Nếu còn <= 30 ngày               → SAP_HET_HAN
     *   - Nếu còn > 30 ngày                → DANG_THUE
     */
    public static ContractStatus resolve(Tenant contract) {
        if (contract == null) return ContractStatus.DA_KET_THUC;

        // Nếu đã kết thúc rõ ràng
        if ("ENDED".equalsIgnoreCase(contract.getTrangThaiHopDong())) {
            return ContractStatus.DA_KET_THUC;
        }

        String ngayKetThuc = contract.getNgayKetThucHopDong();
        if (ngayKetThuc == null || ngayKetThuc.trim().isEmpty()) {
            return ContractStatus.DANG_THUE; // Không có ngày → coi như đang thuê
        }

        try {
            Date endDate = SDF.parse(ngayKetThuc);
            if (endDate == null) return ContractStatus.DANG_THUE;

            long now = System.currentTimeMillis();
            long endMs = endDate.getTime();
            long diff = endMs - now;

            if (diff < 0) {
                return ContractStatus.DA_KET_THUC;   // Đã qua ngày kết thúc
            } else if (diff <= THIRTY_DAYS_MS) {
                return ContractStatus.SAP_HET_HAN;   // Còn <= 30 ngày
            } else {
                return ContractStatus.DANG_THUE;      // Còn > 30 ngày
            }
        } catch (Exception e) {
            return ContractStatus.DANG_THUE;
        }
    }

    /**
     * Tính số ngày còn lại đến ngày kết thúc hợp đồng.
     * @return số ngày (âm nếu đã qua), hoặc -999 nếu không parse được.
     */
    public static long daysRemaining(Tenant contract) {
        if (contract == null || contract.getNgayKetThucHopDong() == null) return -999;
        try {
            Date endDate = SDF.parse(contract.getNgayKetThucHopDong());
            if (endDate == null) return -999;
            long diff = endDate.getTime() - System.currentTimeMillis();
            return TimeUnit.MILLISECONDS.toDays(diff);
        } catch (Exception e) {
            return -999;
        }
    }
}

