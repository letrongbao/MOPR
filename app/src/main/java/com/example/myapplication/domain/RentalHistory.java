package com.example.myapplication.domain;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

/**
 * Model luu tru lich su cho thue (log cua cac hop dong da hoan thanh)
 * Duoc tao tu dong khi ket thuc hop dong
 */
public class RentalHistory {
    private String id;
    private String idHopDong; // ID cua hop dong goc
    private String idPhong; // ID phong da thue
    private String soPhong; // So phong (VD: "P101")
    private String canNhaTen; // Ten can nha
    private int tang; // Tang

    // Thong tin nguoi thue
    private String idTenant;
    private String hoTen;
    private String cccd;
    private String soDienThoai;
    private String diaChi;
    private String soHopDong;
    private int soThanhVien;

    // Thong tin hop dong
    private String ngayBatDauThue; // Format: "dd/MM/yyyy"
    private String ngayKetThucHopDong; // Format: "dd/MM/yyyy"
    private String ngayKetThucThucTe; // Ngay ket thuc thuc te
    private int soThangHopDong; // So thang hop dong
    private int soNgayThueThucTe; // So ngay thue thuc te

    // Thong tin tai chinh
    private double tienPhong; // Gia thue hang thang
    private double tienCoc; // Tien coc
    private double tongTienDaThanhToan; // Tong tien da thanh toan trong thoi gian thue
    private int soInvoiceDaThanhToan; // So hoa don da thanh toan
    private int soInvoiceChuaThanhToan; // So hoa don chua thanh toan

    // Dich vu da su dung
    private boolean dichVuGuiXe;
    private boolean dichVuInternet;
    private boolean dichVuGiatSay;
    private int soLuongXe;

    // Ghi chu va ly do ket thuc
    private String ghiChu;
    private String lyDoKetThuc; // "Het han hop dong", "Nguoi thue yeu cau", "Chu tro yeu cau", "Khac"

    // Timestamps
    private Timestamp createdAt; // Thoi diem tao log (= thoi diem ket thuc hop dong)
    private Long startTimestamp; // Timestamp bat dau thue
    private Long endTimestamp; // Timestamp ket thuc thue

    public RentalHistory() {
        // Required for Firestore
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdHopDong() {
        return idHopDong;
    }

    public void setIdHopDong(String idHopDong) {
        this.idHopDong = idHopDong;
    }

    public String getIdPhong() {
        return idPhong;
    }

    public void setIdPhong(String idPhong) {
        this.idPhong = idPhong;
    }

    public String getSoPhong() {
        return soPhong;
    }

    public void setSoPhong(String soPhong) {
        this.soPhong = soPhong;
    }

    @PropertyName("canNhaTen")
    public String getHouseTen() {
        return canNhaTen;
    }

    @PropertyName("canNhaTen")
    public void setHouseTen(String canNhaTen) {
        this.canNhaTen = canNhaTen;
    }

    public int getTang() {
        return tang;
    }

    public void setTang(int tang) {
        this.tang = tang;
    }

    public String getIdTenant() {
        return idTenant;
    }

    public void setIdTenant(String idTenant) {
        this.idTenant = idTenant;
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

    public String getNgayKetThucThucTe() {
        return ngayKetThucThucTe;
    }

    public void setNgayKetThucThucTe(String ngayKetThucThucTe) {
        this.ngayKetThucThucTe = ngayKetThucThucTe;
    }

    public int getSoThangHopDong() {
        return soThangHopDong;
    }

    public void setSoThangHopDong(int soThangHopDong) {
        this.soThangHopDong = soThangHopDong;
    }

    public int getSoNgayThueThucTe() {
        return soNgayThueThucTe;
    }

    public void setSoNgayThueThucTe(int soNgayThueThucTe) {
        this.soNgayThueThucTe = soNgayThueThucTe;
    }

    public double getTienPhong() {
        return tienPhong;
    }

    public void setTienPhong(double tienPhong) {
        this.tienPhong = tienPhong;
    }

    public double getTienCoc() {
        return tienCoc;
    }

    public void setTienCoc(double tienCoc) {
        this.tienCoc = tienCoc;
    }

    public double getTongTienDaThanhToan() {
        return tongTienDaThanhToan;
    }

    public void setTongTienDaThanhToan(double tongTienDaThanhToan) {
        this.tongTienDaThanhToan = tongTienDaThanhToan;
    }

    public int getSoInvoiceDaThanhToan() {
        return soInvoiceDaThanhToan;
    }

    public void setSoInvoiceDaThanhToan(int soInvoiceDaThanhToan) {
        this.soInvoiceDaThanhToan = soInvoiceDaThanhToan;
    }

    public int getSoInvoiceChuaThanhToan() {
        return soInvoiceChuaThanhToan;
    }

    public void setSoInvoiceChuaThanhToan(int soInvoiceChuaThanhToan) {
        this.soInvoiceChuaThanhToan = soInvoiceChuaThanhToan;
    }

    public boolean isDichVuGuiXe() {
        return dichVuGuiXe;
    }

    public void setDichVuGuiXe(boolean dichVuGuiXe) {
        this.dichVuGuiXe = dichVuGuiXe;
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

    public int getSoLuongXe() {
        return soLuongXe;
    }

    public void setSoLuongXe(int soLuongXe) {
        this.soLuongXe = soLuongXe;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }

    public String getLyDoKetThuc() {
        return lyDoKetThuc;
    }

    public void setLyDoKetThuc(String lyDoKetThuc) {
        this.lyDoKetThuc = lyDoKetThuc;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Long getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(Long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public Long getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(Long endTimestamp) {
        this.endTimestamp = endTimestamp;
    }
}

