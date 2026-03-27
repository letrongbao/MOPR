package com.example.myapplication.domain;

import com.google.firebase.firestore.PropertyName;

public class CanNha {
    private String id;

    // Basic info
    private String tenCanNha; // tên quản lý
    private String sdtQuanLy;
    private String diaChi;
    private String ghiChu;

    // Extra fees + payment code + billing reminder
    private java.util.List<PhiKhac> phiKhac;
    private String qrThanhToanUrl;
    // "start_month" | "end_month"
    private String nhacBaoPhi;

    // Bank transfer info
    private String chuTaiKhoan;
    private String nganHang;
    private String soTaiKhoan;

    // Fee table (VND)
    private double giaDien;
    private double giaNuoc;
    private String cachTinhNuoc; // "nguoi" | "dong_ho" | "phong"
    private double giaXe;
    private double giaInternet;
    private double giaGiatSay;
    private double giaThangMay;
    private double giaTiviCap;
    private double giaRac;
    private double giaDichVu;

    // Audit timestamps
    private com.google.firebase.Timestamp createdAt;
    private com.google.firebase.Timestamp updatedAt;

    public CanNha() {
    }

    public CanNha(String tenCanNha, String diaChi, String ghiChu) {
        this.tenCanNha = tenCanNha;
        this.diaChi = diaChi;
        this.ghiChu = ghiChu;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @PropertyName("tenCanNha")
    public String getTenCanNha() {
        return tenCanNha;
    }

    @PropertyName("tenCanNha")
    public void setTenCanNha(String tenCanNha) {
        this.tenCanNha = tenCanNha;
    }

    public String getSdtQuanLy() {
        return sdtQuanLy;
    }

    public void setSdtQuanLy(String sdtQuanLy) {
        this.sdtQuanLy = sdtQuanLy;
    }

    public String getDiaChi() {
        return diaChi;
    }

    public void setDiaChi(String diaChi) {
        this.diaChi = diaChi;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }

    public String getChuTaiKhoan() {
        return chuTaiKhoan;
    }

    public void setChuTaiKhoan(String chuTaiKhoan) {
        this.chuTaiKhoan = chuTaiKhoan;
    }

    public String getNganHang() {
        return nganHang;
    }

    public void setNganHang(String nganHang) {
        this.nganHang = nganHang;
    }

    public String getSoTaiKhoan() {
        return soTaiKhoan;
    }

    public void setSoTaiKhoan(String soTaiKhoan) {
        this.soTaiKhoan = soTaiKhoan;
    }

    public double getGiaDien() {
        return giaDien;
    }

    public void setGiaDien(double giaDien) {
        this.giaDien = giaDien;
    }

    public double getGiaNuoc() {
        return giaNuoc;
    }

    public void setGiaNuoc(double giaNuoc) {
        this.giaNuoc = giaNuoc;
    }

    public String getCachTinhNuoc() {
        return cachTinhNuoc;
    }

    public void setCachTinhNuoc(String cachTinhNuoc) {
        this.cachTinhNuoc = cachTinhNuoc;
    }

    public double getGiaXe() {
        return giaXe;
    }

    public void setGiaXe(double giaXe) {
        this.giaXe = giaXe;
    }

    public double getGiaInternet() {
        return giaInternet;
    }

    public void setGiaInternet(double giaInternet) {
        this.giaInternet = giaInternet;
    }

    public double getGiaGiatSay() {
        return giaGiatSay;
    }

    public void setGiaGiatSay(double giaGiatSay) {
        this.giaGiatSay = giaGiatSay;
    }

    public double getGiaThangMay() {
        return giaThangMay;
    }

    public void setGiaThangMay(double giaThangMay) {
        this.giaThangMay = giaThangMay;
    }

    public double getGiaTiviCap() {
        return giaTiviCap;
    }

    public void setGiaTiviCap(double giaTiviCap) {
        this.giaTiviCap = giaTiviCap;
    }

    public double getGiaRac() {
        return giaRac;
    }

    public void setGiaRac(double giaRac) {
        this.giaRac = giaRac;
    }

    public double getGiaDichVu() {
        return giaDichVu;
    }

    public void setGiaDichVu(double giaDichVu) {
        this.giaDichVu = giaDichVu;
    }

    public java.util.List<PhiKhac> getPhiKhac() {
        return phiKhac;
    }

    public void setPhiKhac(java.util.List<PhiKhac> phiKhac) {
        this.phiKhac = phiKhac;
    }

    public String getQrThanhToanUrl() {
        return qrThanhToanUrl;
    }

    public void setQrThanhToanUrl(String qrThanhToanUrl) {
        this.qrThanhToanUrl = qrThanhToanUrl;
    }

    public String getNhacBaoPhi() {
        return nhacBaoPhi;
    }

    public void setNhacBaoPhi(String nhacBaoPhi) {
        this.nhacBaoPhi = nhacBaoPhi;
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

    public static class PhiKhac {
        private String tenPhi;
        private String donViTinh;
        private double gia;

        public PhiKhac() {
        }

        public PhiKhac(String tenPhi, String donViTinh, double gia) {
            this.tenPhi = tenPhi;
            this.donViTinh = donViTinh;
            this.gia = gia;
        }

        public String getTenPhi() {
            return tenPhi;
        }

        public void setTenPhi(String tenPhi) {
            this.tenPhi = tenPhi;
        }

        public String getDonViTinh() {
            return donViTinh;
        }

        public void setDonViTinh(String donViTinh) {
            this.donViTinh = donViTinh;
        }

        public double getGia() {
            return gia;
        }

        public void setGia(double gia) {
            this.gia = gia;
        }
    }
}
