package com.example.myapplication.model;

public class KhuTro {
    private String id;
    private String tenKhu;
    private String diaChi;
    private String ghiChu;

    public KhuTro() {}

    public KhuTro(String tenKhu, String diaChi, String ghiChu) {
        this.tenKhu = tenKhu;
        this.diaChi = diaChi;
        this.ghiChu = ghiChu;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTenKhu() { return tenKhu; }
    public void setTenKhu(String tenKhu) { this.tenKhu = tenKhu; }

    public String getDiaChi() { return diaChi; }
    public void setDiaChi(String diaChi) { this.diaChi = diaChi; }

    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
}
