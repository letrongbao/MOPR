package com.example.myapplication.model;

public class NguoiThue {
    private String id;
    private String hoTen;
    private String cccd;
    private String soDienThoai;
    private String idPhong;         // tham chiếu tới PhongTro.id
    private int soThanhVien;
    private String ngayBatDauThue;  // định dạng "dd/MM/yyyy"
    private String ngayKetThucHopDong;
    private double tienCoc;

    public NguoiThue() {}

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
        this.tienCoc = tienCoc;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }
    public String getCccd() { return cccd; }
    public void setCccd(String cccd) { this.cccd = cccd; }
    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }
    public String getIdPhong() { return idPhong; }
    public void setIdPhong(String idPhong) { this.idPhong = idPhong; }
    public int getSoThanhVien() { return soThanhVien; }
    public void setSoThanhVien(int soThanhVien) { this.soThanhVien = soThanhVien; }
    public String getNgayBatDauThue() { return ngayBatDauThue; }
    public void setNgayBatDauThue(String ngayBatDauThue) { this.ngayBatDauThue = ngayBatDauThue; }
    public String getNgayKetThucHopDong() { return ngayKetThucHopDong; }
    public void setNgayKetThucHopDong(String ngayKetThucHopDong) { this.ngayKetThucHopDong = ngayKetThucHopDong; }
    public double getTienCoc() { return tienCoc; }
    public void setTienCoc(double tienCoc) { this.tienCoc = tienCoc; }

    // Tên phòng lưu kèm để hiển thị (denormalized)
    private String soPhong;
    public String getSoPhong() { return soPhong; }
    public void setSoPhong(String soPhong) { this.soPhong = soPhong; }
}
