package com.example.myapplication.domain;

public class NguoiThue {
    private String id;
    private String hoTen;
    private String cccd;
    private String soDienThoai;

    // Contract/customer extended info
    private String diaChi;
    private String soHopDong;
    private String cccdFrontUrl;
    private String cccdBackUrl;

    // Người đại diện hợp đồng (có thể khác với người thuê chính)
    private String tenNguoiDaiDien;
    private String idNguoiDaiDien;

    private String idPhong; // tham chiếu tới PhongTro.id
    private String idPhongCu; // lưu lịch sử khi kết thúc hợp đồng

    private int soThanhVien;
    private String ngayBatDauThue; // định dạng "dd/MM/yyyy" (ngày ký HĐ)
    private String ngayKetThucHopDong; // String format (để tương thích cũ)

    // Firestore Int64 fields (ưu tiên sử dụng)
    private long giaThue; // Giá thuê (VND)
    private long tienCoc; // Tiền cọc (VND)
    private long ngayKetThuc; // Timestamp milliseconds

    private double tienPhong; // @Deprecated: dùng giaThue thay thế
    private double tienCoc_old; // @Deprecated: dùng tienCoc thay thế
    private boolean hienThiTienCocTrenHoaDon = true;

    private int soThangHopDong;
    private boolean nhacTruoc1Thang = true;

    // Billing reminder timing: "start_month" | "mid_month" | "end_month"
    private String nhacBaoPhiVao = "start_month";

    private int chiSoDienDau;

    private boolean dichVuGuiXe;
    private int soLuongXe;
    private boolean dichVuInternet;
    private boolean dichVuGiatSay;

    private String ghiChu;
    private boolean hienThiGhiChuTrenHoaDon = true;

    // Trạng thái thu tiền cọc
    private boolean trangThaiThuCoc = false;

    // ACTIVE / ENDED
    private String trangThaiHopDong = "ACTIVE";
    private Long createdAt;
    private Long updatedAt;
    private Long endedAt;

    public NguoiThue() {
    }

    public NguoiThue(String hoTen, String cccd, String soDienThoai, String idPhong,
            int soThanhVien, String ngayBatDauThue,
            String ngayKetThucHopDong, double tienCoc) {
        this.hoTen = hoTen;
        this.cccd = cccd;
        this.soDienThoai = soDienThoai;
        this.idPhong = idPhong;
        this.soThanhVien = soThanhVien;
        this.ngayBatDauThue = ngayBatDauThue;
        this.ngayKetThucHopDong = ngayKetThucHopDong;

        // Convert double to long for new field
        this.tienCoc = (long) tienCoc;
        this.tienCoc_old = tienCoc; // Keep old value
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public String getCccd() {
        return cccd;
    }

    public void setCccd(String cccd) {
        this.cccd = cccd;
    }

    public String getSoDienThoai() {
        return soDienThoai;
    }

    public void setSoDienThoai(String soDienThoai) {
        this.soDienThoai = soDienThoai;
    }

    public String getIdPhong() {
        return idPhong;
    }

    public void setIdPhong(String idPhong) {
        this.idPhong = idPhong;
    }

    public int getSoThanhVien() {
        return soThanhVien;
    }

    public void setSoThanhVien(int soThanhVien) {
        this.soThanhVien = soThanhVien;
    }

    public String getNgayBatDauThue() {
        return ngayBatDauThue;
    }

    public void setNgayBatDauThue(String ngayBatDauThue) {
        this.ngayBatDauThue = ngayBatDauThue;
    }

    public String getNgayKetThucHopDong() {
        return ngayKetThucHopDong;
    }

    public void setNgayKetThucHopDong(String ngayKetThucHopDong) {
        this.ngayKetThucHopDong = ngayKetThucHopDong;
    }

    public String getDiaChi() {
        return diaChi;
    }

    public void setDiaChi(String diaChi) {
        this.diaChi = diaChi;
    }

    public String getSoHopDong() {
        return soHopDong;
    }

    public void setSoHopDong(String soHopDong) {
        this.soHopDong = soHopDong;
    }

    public String getCccdFrontUrl() {
        return cccdFrontUrl;
    }

    public void setCccdFrontUrl(String cccdFrontUrl) {
        this.cccdFrontUrl = cccdFrontUrl;
    }

    public String getCccdBackUrl() {
        return cccdBackUrl;
    }

    public void setCccdBackUrl(String cccdBackUrl) {
        this.cccdBackUrl = cccdBackUrl;
    }

    public String getIdPhongCu() {
        return idPhongCu;
    }

    public void setIdPhongCu(String idPhongCu) {
        this.idPhongCu = idPhongCu;
    }

    // Firestore Int64 getters/setters (ưu tiên)
    public long getGiaThue() {
        // Fallback to tienPhong if giaThue not set
        return giaThue > 0 ? giaThue : (long) tienPhong;
    }

    public void setGiaThue(long giaThue) {
        this.giaThue = giaThue;
        this.tienPhong = giaThue; // Keep sync for backward compatibility
    }

    public long getTienCoc() {
        // Fallback to old tienCoc if not set
        return tienCoc > 0 ? tienCoc : (long) getTienCoc_old();
    }

    public void setTienCoc(long tienCoc) {
        this.tienCoc = tienCoc;
        this.tienCoc_old = tienCoc; // Keep sync
    }

    public long getNgayKetThuc() {
        return ngayKetThuc;
    }

    public void setNgayKetThuc(long ngayKetThuc) {
        this.ngayKetThuc = ngayKetThuc;
    }

    // Old getters/setters (deprecated but kept for compatibility)
    public double getTienPhong() {
        return tienPhong;
    }

    public void setTienPhong(double tienPhong) {
        this.tienPhong = tienPhong;
    }

    public double getTienCoc_old() {
        return tienCoc_old;
    }

    public void setTienCoc_old(double tienCoc_old) {
        this.tienCoc_old = tienCoc_old;
    }

    public boolean isHienThiTienCocTrenHoaDon() {
        return hienThiTienCocTrenHoaDon;
    }

    public void setHienThiTienCocTrenHoaDon(boolean hienThiTienCocTrenHoaDon) {
        this.hienThiTienCocTrenHoaDon = hienThiTienCocTrenHoaDon;
    }

    public int getSoThangHopDong() {
        return soThangHopDong;
    }

    public void setSoThangHopDong(int soThangHopDong) {
        this.soThangHopDong = soThangHopDong;
    }

    public boolean isNhacTruoc1Thang() {
        return nhacTruoc1Thang;
    }

    public void setNhacTruoc1Thang(boolean nhacTruoc1Thang) {
        this.nhacTruoc1Thang = nhacTruoc1Thang;
    }

    public String getNhacBaoPhiVao() {
        return nhacBaoPhiVao;
    }

    public void setNhacBaoPhiVao(String nhacBaoPhiVao) {
        if (nhacBaoPhiVao == null || nhacBaoPhiVao.trim().isEmpty()) {
            this.nhacBaoPhiVao = "start_month";
            return;
        }
        this.nhacBaoPhiVao = nhacBaoPhiVao;
    }

    public int getChiSoDienDau() {
        return chiSoDienDau;
    }

    public void setChiSoDienDau(int chiSoDienDau) {
        this.chiSoDienDau = chiSoDienDau;
    }

    public boolean isDichVuGuiXe() {
        return dichVuGuiXe;
    }

    public void setDichVuGuiXe(boolean dichVuGuiXe) {
        this.dichVuGuiXe = dichVuGuiXe;
    }

    public int getSoLuongXe() {
        return soLuongXe;
    }

    public void setSoLuongXe(int soLuongXe) {
        this.soLuongXe = soLuongXe;
    }

    public boolean isDichVuInternet() {
        return dichVuInternet;
    }

    public void setDichVuInternet(boolean dichVuInternet) {
        this.dichVuInternet = dichVuInternet;
    }

    public boolean isDichVuGiatSay() {
        return dichVuGiatSay;
    }

    public void setDichVuGiatSay(boolean dichVuGiatSay) {
        this.dichVuGiatSay = dichVuGiatSay;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }

    public boolean isHienThiGhiChuTrenHoaDon() {
        return hienThiGhiChuTrenHoaDon;
    }

    public void setHienThiGhiChuTrenHoaDon(boolean hienThiGhiChuTrenHoaDon) {
        this.hienThiGhiChuTrenHoaDon = hienThiGhiChuTrenHoaDon;
    }

    public String getTrangThaiHopDong() {
        return trangThaiHopDong;
    }

    public void setTrangThaiHopDong(String trangThaiHopDong) {
        this.trangThaiHopDong = trangThaiHopDong;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Long endedAt) {
        this.endedAt = endedAt;
    }

    public boolean isTrangThaiThuCoc() {
        return trangThaiThuCoc;
    }

    public void setTrangThaiThuCoc(boolean trangThaiThuCoc) {
        this.trangThaiThuCoc = trangThaiThuCoc;
    }

    public String getTenNguoiDaiDien() {
        return tenNguoiDaiDien;
    }

    public void setTenNguoiDaiDien(String tenNguoiDaiDien) {
        this.tenNguoiDaiDien = tenNguoiDaiDien;
    }

    public String getIdNguoiDaiDien() {
        return idNguoiDaiDien;
    }

    public void setIdNguoiDaiDien(String idNguoiDaiDien) {
        this.idNguoiDaiDien = idNguoiDaiDien;
    }

    // Tính năng Quản lý Khách Thuê Nâng cao
    private boolean isNguoiLienHe = false;
    private boolean isDaiDienHopDong = false;
    private boolean isTamTru = false;
    private boolean isDayDuGiayTo = false;

    public boolean isNguoiLienHe() {
        return isNguoiLienHe;
    }

    public void setNguoiLienHe(boolean nguoiLienHe) {
        isNguoiLienHe = nguoiLienHe;
    }

    public boolean isDaiDienHopDong() {
        return isDaiDienHopDong;
    }

    public void setDaiDienHopDong(boolean daiDienHopDong) {
        isDaiDienHopDong = daiDienHopDong;
    }

    public boolean isTamTru() {
        return isTamTru;
    }

    public void setTamTru(boolean tamTru) {
        isTamTru = tamTru;
    }

    public boolean isDayDuGiayTo() {
        return isDayDuGiayTo;
    }

    public void setDayDuGiayTo(boolean dayDuGiayTo) {
        isDayDuGiayTo = dayDuGiayTo;
    }

    // Tên phòng lưu kèm để hiển thị (denormalized)
    private String soPhong;

    public String getSoPhong() {
        return soPhong;
    }

    public void setSoPhong(String soPhong) {
        this.soPhong = soPhong;
    }
}
