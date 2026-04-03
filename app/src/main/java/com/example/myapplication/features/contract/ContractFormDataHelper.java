package com.example.myapplication.features.contract;

import androidx.annotation.NonNull;

import com.example.myapplication.domain.Tenant;

public final class ContractFormDataHelper {

    public interface MonthYearNormalizer {
        @NonNull
        String normalize(String input);
    }

    public interface EndDateComputer {
        @NonNull
        String compute(@NonNull String start, int months);
    }

    public static final class FormData {
        public final String soHopDong;
        public final String hoTen;
        public final String soDienThoai;
        public final String cccd;
        public final int soNguoi;
        public final String ngayKy;
        public final int soThang;
        public final int chiSoDien;
        public final boolean dichVuGuiXe;
        public final int soXe;
        public final boolean dichVuInternet;
        public final boolean dichVuGiatSay;
        public final boolean nhacTruoc1Thang;
        public final boolean hienThiTienCoc;
        public final boolean hienThiGhiChu;
        public final String nhacBaoPhiVao;
        public final double tienPhong;
        public final double tienCoc;
        public final String ghiChu;

        FormData(String soHopDong,
                String hoTen,
                String soDienThoai,
                String cccd,
                int soNguoi,
                String ngayKy,
                int soThang,
                int chiSoDien,
                boolean dichVuGuiXe,
                int soXe,
                boolean dichVuInternet,
                boolean dichVuGiatSay,
                boolean nhacTruoc1Thang,
                boolean hienThiTienCoc,
                boolean hienThiGhiChu,
                String nhacBaoPhiVao,
                double tienPhong,
                double tienCoc,
                String ghiChu) {
            this.soHopDong = soHopDong;
            this.hoTen = hoTen;
            this.soDienThoai = soDienThoai;
            this.cccd = cccd;
            this.soNguoi = soNguoi;
            this.ngayKy = ngayKy;
            this.soThang = soThang;
            this.chiSoDien = chiSoDien;
            this.dichVuGuiXe = dichVuGuiXe;
            this.soXe = soXe;
            this.dichVuInternet = dichVuInternet;
            this.dichVuGiatSay = dichVuGiatSay;
            this.nhacTruoc1Thang = nhacTruoc1Thang;
            this.hienThiTienCoc = hienThiTienCoc;
            this.hienThiGhiChu = hienThiGhiChu;
            this.nhacBaoPhiVao = nhacBaoPhiVao;
            this.tienPhong = tienPhong;
            this.tienCoc = tienCoc;
            this.ghiChu = ghiChu;
        }
    }

    public static final class ValidationException extends Exception {
        ValidationException(@NonNull String message) {
            super(message);
        }
    }

    private ContractFormDataHelper() {
    }

    @NonNull
    public static FormData parseAndValidate(@NonNull String soHopDong,
            @NonNull String hoTen,
            @NonNull String soDienThoai,
            @NonNull String cccd,
            @NonNull String soNguoiStr,
            @NonNull String ngayKyInput,
            @NonNull String soThangStr,
            @NonNull String chiSoDienStr,
            boolean dichVuGuiXe,
            @NonNull String soXeStr,
            boolean dichVuInternet,
            boolean dichVuGiatSay,
            boolean nhacTruoc1Thang,
            boolean hienThiTienCoc,
            boolean hienThiGhiChu,
            @NonNull String nhacBaoPhiVao,
            double tienPhong,
            double tienCoc,
            @NonNull String ghiChu,
            @NonNull MonthYearNormalizer monthYearNormalizer) throws ValidationException {
        if (hoTen.isEmpty() || soDienThoai.isEmpty() || cccd.isEmpty() || soNguoiStr.isEmpty() || ngayKyInput.isEmpty()
                || soThangStr.isEmpty() || chiSoDienStr.isEmpty()) {
            throw new ValidationException("Vui lòng nhập đủ các trường bắt buộc (*)");
        }

        String ngayKy = monthYearNormalizer.normalize(ngayKyInput);
        if (ngayKy.isEmpty()) {
            throw new ValidationException("Ngày ký không hợp lệ (định dạng MM/yyyy)");
        }

        if (dichVuGuiXe && soXeStr.isEmpty()) {
            throw new ValidationException("Vui lòng nhập số lượng xe");
        }

        int soNguoi;
        int soThang;
        int chiSoDien;
        int soXe = 0;
        try {
            soNguoi = Integer.parseInt(soNguoiStr);
            soThang = Integer.parseInt(soThangStr);
            chiSoDien = Integer.parseInt(chiSoDienStr);
            if (dichVuGuiXe) {
                soXe = Integer.parseInt(soXeStr);
            }
        } catch (Exception e) {
            throw new ValidationException("Số liệu không hợp lệ");
        }

        return new FormData(
                soHopDong,
                hoTen,
                soDienThoai,
                cccd,
                soNguoi,
                ngayKy,
                soThang,
                chiSoDien,
                dichVuGuiXe,
                soXe,
                dichVuInternet,
                dichVuGiatSay,
                nhacTruoc1Thang,
                hienThiTienCoc,
                hienThiGhiChu,
                nhacBaoPhiVao,
                tienPhong,
                tienCoc,
                ghiChu);
    }

    public static void applyToContract(@NonNull Tenant contract,
            @NonNull FormData data,
            @NonNull String phongId,
            String soPhong,
            @NonNull EndDateComputer endDateComputer) {
        contract.setSoHopDong(data.soHopDong);
        contract.setHoTen(data.hoTen);
        contract.setSoDienThoai(data.soDienThoai);
        contract.setDiaChi("");
        contract.setCccd(data.cccd);
        contract.setIdPhong(phongId);
        contract.setSoPhong(soPhong);
        contract.setSoThanhVien(data.soNguoi);
        contract.setNgayBatDauThue(data.ngayKy);
        contract.setSoThangHopDong(data.soThang);
        contract.setNhacTruoc1Thang(data.nhacTruoc1Thang);
        contract.setNhacBaoPhiVao(data.nhacBaoPhiVao);

        // Keep both old and new fields for backward compatibility.
        contract.setTienPhong(data.tienPhong);
        contract.setGiaThue((long) data.tienPhong);
        contract.setTienCoc((long) data.tienCoc);
        contract.setHienThiTienCocTrenInvoice(data.hienThiTienCoc);
        contract.setChiSoDienDau(data.chiSoDien);
        contract.setDichVuGuiXe(data.dichVuGuiXe);
        contract.setSoLuongXe(data.soXe);
        contract.setDichVuInternet(data.dichVuInternet);
        contract.setDichVuGiatSay(data.dichVuGiatSay);
        contract.setGhiChu(data.ghiChu);
        contract.setHienThiGhiChuTrenInvoice(data.hienThiGhiChu);
        contract.setTrangThaiHopDong("ACTIVE");
        contract.setNgayKetThucHopDong(endDateComputer.compute(data.ngayKy, data.soThang));
    }
}
