package com.example.myapplication.domain;

public class HoaDon {
    private String id;
    private String idPhong;
    private String idNguoiThue;
    private String thangNam; // "03/2026"
    private double chiSoDienDau;
    private double chiSoDienCuoi;
    private double donGiaDien;
    private double chiSoNuocDau;
    private double chiSoNuocCuoi;
    private double donGiaNuoc;
    private double phiRac;
    private double phiWifi;
    private double phiGuiXe;
    private double giaThue;
    private double tongTien;
    private String trangThai; // "Chưa thanh toán", "Đã thanh toán"

    // Enhanced payment tracking
    private com.google.firebase.Timestamp ngayThanhToan; // Payment date
    private String phuongThucThanhToan; // "cash", "transfer", "momo", "bank"
    private double soTienDaThanhToan; // Amount paid (for partial payments)

    // Audit timestamps
    private com.google.firebase.Timestamp createdAt;
    private com.google.firebase.Timestamp updatedAt;

    public HoaDon() {
    }

    // Tính tổng tiền tự động
    public void tinhTongTien() {
        double tienDien = (chiSoDienCuoi - chiSoDienDau) * donGiaDien;
        double tienNuoc = (chiSoNuocCuoi - chiSoNuocDau) * donGiaNuoc;
        this.tongTien = giaThue + tienDien + tienNuoc + phiRac + phiWifi + phiGuiXe;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdPhong() {
        return idPhong;
    }

    public void setIdPhong(String idPhong) {
        this.idPhong = idPhong;
    }

    public String getIdNguoiThue() {
        return idNguoiThue;
    }

    public void setIdNguoiThue(String idNguoiThue) {
        this.idNguoiThue = idNguoiThue;
    }

    public String getThangNam() {
        return thangNam;
    }

    public void setThangNam(String thangNam) {
        this.thangNam = thangNam;
    }

    public double getChiSoDienDau() {
        return chiSoDienDau;
    }

    public void setChiSoDienDau(double chiSoDienDau) {
        this.chiSoDienDau = chiSoDienDau;
    }

    public double getChiSoDienCuoi() {
        return chiSoDienCuoi;
    }

    public void setChiSoDienCuoi(double chiSoDienCuoi) {
        this.chiSoDienCuoi = chiSoDienCuoi;
    }

    public double getDonGiaDien() {
        return donGiaDien;
    }

    public void setDonGiaDien(double donGiaDien) {
        this.donGiaDien = donGiaDien;
    }

    public double getChiSoNuocDau() {
        return chiSoNuocDau;
    }

    public void setChiSoNuocDau(double chiSoNuocDau) {
        this.chiSoNuocDau = chiSoNuocDau;
    }

    public double getChiSoNuocCuoi() {
        return chiSoNuocCuoi;
    }

    public void setChiSoNuocCuoi(double chiSoNuocCuoi) {
        this.chiSoNuocCuoi = chiSoNuocCuoi;
    }

    public double getDonGiaNuoc() {
        return donGiaNuoc;
    }

    public void setDonGiaNuoc(double donGiaNuoc) {
        this.donGiaNuoc = donGiaNuoc;
    }

    public double getPhiRac() {
        return phiRac;
    }

    public void setPhiRac(double phiRac) {
        this.phiRac = phiRac;
    }

    public double getPhiWifi() {
        return phiWifi;
    }

    public void setPhiWifi(double phiWifi) {
        this.phiWifi = phiWifi;
    }

    public double getPhiGuiXe() {
        return phiGuiXe;
    }

    public void setPhiGuiXe(double phiGuiXe) {
        this.phiGuiXe = phiGuiXe;
    }

    public double getGiaThue() {
        return giaThue;
    }

    public void setGiaThue(double giaThue) {
        this.giaThue = giaThue;
    }

    public double getTongTien() {
        return tongTien;
    }

    public void setTongTien(double tongTien) {
        this.tongTien = tongTien;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }

    // Tên phòng lưu kèm để hiển thị (denormalized)
    private String soPhong;

    public String getSoPhong() {
        return soPhong;
    }

    public void setSoPhong(String soPhong) {
        this.soPhong = soPhong;
    }

    public com.google.firebase.Timestamp getNgayThanhToan() {
        return ngayThanhToan;
    }

    public void setNgayThanhToan(com.google.firebase.Timestamp ngayThanhToan) {
        this.ngayThanhToan = ngayThanhToan;
    }

    public String getPhuongThucThanhToan() {
        return phuongThucThanhToan;
    }

    public void setPhuongThucThanhToan(String phuongThucThanhToan) {
        this.phuongThucThanhToan = phuongThucThanhToan;
    }

    public double getSoTienDaThanhToan() {
        return soTienDaThanhToan;
    }

    public void setSoTienDaThanhToan(double soTienDaThanhToan) {
        this.soTienDaThanhToan = soTienDaThanhToan;
    }

    public com.google.firebase.Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(com.google.firebase.Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public com.google.firebase.Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(com.google.firebase.Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
