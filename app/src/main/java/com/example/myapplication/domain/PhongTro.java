package com.example.myapplication.domain;

import com.google.firebase.firestore.PropertyName;

public class PhongTro {
    private String id;
    private String soPhong;
    private String loaiPhong; // "Đơn", "Đôi", "Ghép"
    private double dienTich;
    private double giaThue;
    private String trangThai; // "Trống", "Đã thuê"
    private String hinhAnh; // URL ảnh từ Firebase Storage

    // Group rooms by house
    private String canNhaId;
    private String canNhaTen;

    // Enhanced fields
    private int tang; // Floor number
    private String moTa; // Description
    private java.util.List<String> tienNghi; // Amenities: ["AC", "balcony", "kitchen"]
    private int soNguoiToiDa; // Max occupancy

    // Audit timestamps
    private com.google.firebase.Timestamp createdAt;
    private com.google.firebase.Timestamp updatedAt;

    public PhongTro() {
    } // Firestore cần constructor rỗng

    public PhongTro(String soPhong, String loaiPhong, double dienTich,
            double giaThue, String trangThai) {
        this.soPhong = soPhong;
        this.loaiPhong = loaiPhong;
        this.dienTich = dienTich;
        this.giaThue = giaThue;
        this.trangThai = trangThai;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSoPhong() {
        return soPhong;
    }

    public void setSoPhong(String soPhong) {
        this.soPhong = soPhong;
    }

    public String getLoaiPhong() {
        return loaiPhong;
    }

    public void setLoaiPhong(String loaiPhong) {
        this.loaiPhong = loaiPhong;
    }

    public double getDienTich() {
        return dienTich;
    }

    public void setDienTich(double dienTich) {
        this.dienTich = dienTich;
    }

    public double getGiaThue() {
        return giaThue;
    }

    public void setGiaThue(double giaThue) {
        this.giaThue = giaThue;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }

    public String getHinhAnh() {
        return hinhAnh;
    }

    public void setHinhAnh(String hinhAnh) {
        this.hinhAnh = hinhAnh;
    }

    @PropertyName("canNhaId")
    public String getCanNhaId() {
        return canNhaId;
    }

    @PropertyName("canNhaId")
    public void setCanNhaId(String canNhaId) {
        this.canNhaId = canNhaId;
    }

    @PropertyName("canNhaTen")
    public String getCanNhaTen() {
        return canNhaTen;
    }

    @PropertyName("canNhaTen")
    public void setCanNhaTen(String canNhaTen) {
        this.canNhaTen = canNhaTen;
    }

    public int getTang() {
        return tang;
    }

    public void setTang(int tang) {
        this.tang = tang;
    }

    public String getMoTa() {
        return moTa;
    }

    public void setMoTa(String moTa) {
        this.moTa = moTa;
    }

    public java.util.List<String> getTienNghi() {
        return tienNghi;
    }

    public void setTienNghi(java.util.List<String> tienNghi) {
        this.tienNghi = tienNghi;
    }

    public int getSoNguoiToiDa() {
        return soNguoiToiDa;
    }

    public void setSoNguoiToiDa(int soNguoiToiDa) {
        this.soNguoiToiDa = soNguoiToiDa;
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
