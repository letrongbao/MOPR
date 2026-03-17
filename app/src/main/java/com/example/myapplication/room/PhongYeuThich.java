package com.example.myapplication.room;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "phong_yeu_thich")
public class PhongYeuThich {

    @PrimaryKey
    @NonNull
    private String id = "";
    private String soPhong;
    private String loaiPhong;
    private double dienTich;
    private double giaThue;
    private String trangThai;
    private String hinhAnh;
    private long ngayLuu;

    public PhongYeuThich() {}

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
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
    public String getHinhAnh() { return hinhAnh; }
    public void setHinhAnh(String hinhAnh) { this.hinhAnh = hinhAnh; }
    public long getNgayLuu() { return ngayLuu; }
    public void setNgayLuu(long ngayLuu) { this.ngayLuu = ngayLuu; }
}
