package com.example.myapplication.model;

public class PhongTro {
    private String id;
    private String soPhong;
    private String loaiPhong;   // "Đơn", "Đôi", "Ghép"
    private double dienTich;
    private double giaThue;
    private String trangThai;   // "Trống", "Đã thuê"

    public PhongTro() {} // Firestore cần constructor rỗng

    public PhongTro(String soPhong, String loaiPhong, double dienTich,
                    double giaThue, String trangThai) {
        this.soPhong = soPhong;
        this.loaiPhong = loaiPhong;
        this.dienTich = dienTich;
        this.giaThue = giaThue;
        this.trangThai = trangThai;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSoPhong() { return soPhong; }
    public void setSoPhong(String soPhong) { this.soPhong = soPhong; }
    public String getLoaiPhong() { return loaiPhong; }
    public void setLoaiPhong(String loaiPhong) { this.loaiPhong = loaiPhong; }
    public double getDienTich() { return dienTich; }
    public void setDienTich(double dienTich) { this.dienTich = dienTich; }
    public double getGiaThue() { return giaThue; }
    public void setGiaThue(double giaThue) { this.giaThue = giaThue; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
}
