package com.example.myapplication.model;

public class MeterReading {
    private String id;
    private String roomId;
    private String period; // MM/yyyy
    private String periodKey; // yyyyMM for sorting
    private double elecStart;
    private double elecEnd;
    private double waterStart;
    private double waterEnd;
    private String imageUrl;

    // Tenant confirmation (Phase 3)
    private String tenantConfirmStatus; // APPROVED/DISPUTED
    private String tenantConfirmNote;
    private com.google.firebase.Timestamp tenantConfirmAt;

    public MeterReading() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public String getPeriodKey() { return periodKey; }
    public void setPeriodKey(String periodKey) { this.periodKey = periodKey; }

    public double getElecStart() { return elecStart; }
    public void setElecStart(double elecStart) { this.elecStart = elecStart; }

    public double getElecEnd() { return elecEnd; }
    public void setElecEnd(double elecEnd) { this.elecEnd = elecEnd; }

    public double getWaterStart() { return waterStart; }
    public void setWaterStart(double waterStart) { this.waterStart = waterStart; }

    public double getWaterEnd() { return waterEnd; }
    public void setWaterEnd(double waterEnd) { this.waterEnd = waterEnd; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getTenantConfirmStatus() { return tenantConfirmStatus; }
    public void setTenantConfirmStatus(String tenantConfirmStatus) { this.tenantConfirmStatus = tenantConfirmStatus; }

    public String getTenantConfirmNote() { return tenantConfirmNote; }
    public void setTenantConfirmNote(String tenantConfirmNote) { this.tenantConfirmNote = tenantConfirmNote; }

    public com.google.firebase.Timestamp getTenantConfirmAt() { return tenantConfirmAt; }
    public void setTenantConfirmAt(com.google.firebase.Timestamp tenantConfirmAt) { this.tenantConfirmAt = tenantConfirmAt; }
}
