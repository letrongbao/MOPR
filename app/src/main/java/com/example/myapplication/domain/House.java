package com.example.myapplication.domain;

import com.google.firebase.firestore.PropertyName;

import java.util.List;

public class House {
    private String id;

    // Basic info
    private String houseName;
    private String managerPhone;
    private String address;
    private String note;

    // Extra fees + payment code + billing reminder
    private List<ExtraFee> extraFees;
    private String paymentQrUrl;
    // "start_month" | "mid_month"
    private String billingReminderAt;

    // Bank transfer info
    private String bankAccountName;
    private String bankName;
    private String bankAccountNo;

    // Fee table (VND)
    private double electricityPrice;
    private String electricityCalculationMethod; // "kwh" | "per_person" | "room"
    private double waterPrice;
    private String waterCalculationMethod; // "per_person" | "meter" | "room"
    private double parkingPrice;
    private String parkingUnit; // "vehicle" | "person" | "room"
    private double internetPrice;
    private String internetUnit; // "room" | "person"
    private double laundryPrice;
    private String laundryUnit; // "room" | "person"
    private double elevatorPrice;
    private String elevatorUnit; // "room" | "person"
    private double cableTvPrice;
    private String cableTvUnit; // "room" | "person"
    private double trashPrice;
    private String trashUnit; // "room" | "person"
    private double servicePrice;
    private String serviceUnit; // "room" | "person"

    // Clean helper: only keep unit if price > 0
    public String getParkingUnitSafe() {
        return parkingPrice > 0 ? parkingUnit : null;
    }
    public String getInternetUnitSafe() {
        return internetPrice > 0 ? internetUnit : null;
    }
    public String getLaundryUnitSafe() {
        return laundryPrice > 0 ? laundryUnit : null;
    }
    public String getElevatorUnitSafe() {
        return elevatorPrice > 0 ? elevatorUnit : null;
    }
    public String getCableTvUnitSafe() {
        return cableTvPrice > 0 ? cableTvUnit : null;
    }
    public String getTrashUnitSafe() {
        return trashPrice > 0 ? trashUnit : null;
    }
    public String getServiceUnitSafe() {
        return servicePrice > 0 ? serviceUnit : null;
    }

    // Audit timestamps
    private com.google.firebase.Timestamp createdAt;
    private com.google.firebase.Timestamp updatedAt;

    public House() {
    }

    public House(String houseName, String address, String note) {
        this.houseName = houseName;
        this.address = address;
        this.note = note;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHouseName() {
        return houseName;
    }

    public void setHouseName(String houseName) {
        this.houseName = houseName;
    }

    @PropertyName("managerPhone")
    public String getManagerPhone() {
        return managerPhone;
    }

    @PropertyName("managerPhone")
    public void setManagerPhone(String managerPhone) {
        this.managerPhone = managerPhone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @PropertyName("bankAccountName")
    public String getBankAccountName() {
        return bankAccountName;
    }

    @PropertyName("bankAccountName")
    public void setBankAccountName(String bankAccountName) {
        this.bankAccountName = bankAccountName;
    }

    @PropertyName("bankName")
    public String getBankName() {
        return bankName;
    }

    @PropertyName("bankName")
    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    @PropertyName("bankAccountNo")
    public String getBankAccountNo() {
        return bankAccountNo;
    }

    @PropertyName("bankAccountNo")
    public void setBankAccountNo(String bankAccountNo) {
        this.bankAccountNo = bankAccountNo;
    }

    @PropertyName("electricityPrice")
    public double getElectricityPrice() {
        return electricityPrice;
    }

    @PropertyName("electricityPrice")
    public void setElectricityPrice(double electricityPrice) {
        this.electricityPrice = electricityPrice;
    }

    @PropertyName("electricityCalculationMethod")
    public String getElectricityCalculationMethod() {
        return electricityCalculationMethod;
    }

    @PropertyName("electricityCalculationMethod")
    public void setElectricityCalculationMethod(String electricityCalculationMethod) {
        this.electricityCalculationMethod = electricityCalculationMethod;
    }

    @PropertyName("waterPrice")
    public double getWaterPrice() {
        return waterPrice;
    }

    @PropertyName("waterPrice")
    public void setWaterPrice(double waterPrice) {
        this.waterPrice = waterPrice;
    }

    @PropertyName("waterCalculationMethod")
    public String getWaterCalculationMethod() {
        return waterCalculationMethod;
    }

    @PropertyName("waterCalculationMethod")
    public void setWaterCalculationMethod(String waterCalculationMethod) {
        this.waterCalculationMethod = waterCalculationMethod;
    }

    @PropertyName("parkingPrice")
    public double getParkingPrice() {
        return parkingPrice;
    }

    @PropertyName("parkingPrice")
    public void setParkingPrice(double parkingPrice) {
        this.parkingPrice = parkingPrice;
    }

    @PropertyName("parkingUnit")
    public String getParkingUnit() {
        return parkingUnit;
    }

    @PropertyName("parkingUnit")
    public void setParkingUnit(String parkingUnit) {
        this.parkingUnit = parkingUnit;
    }

    @PropertyName("internetPrice")
    public double getInternetPrice() {
        return internetPrice;
    }

    @PropertyName("internetPrice")
    public void setInternetPrice(double internetPrice) {
        this.internetPrice = internetPrice;
    }

    @PropertyName("internetUnit")
    public String getInternetUnit() {
        return internetUnit;
    }

    @PropertyName("internetUnit")
    public void setInternetUnit(String internetUnit) {
        this.internetUnit = internetUnit;
    }

    @PropertyName("laundryPrice")
    public double getLaundryPrice() {
        return laundryPrice;
    }

    @PropertyName("laundryPrice")
    public void setLaundryPrice(double laundryPrice) {
        this.laundryPrice = laundryPrice;
    }

    @PropertyName("laundryUnit")
    public String getLaundryUnit() {
        return laundryUnit;
    }

    @PropertyName("laundryUnit")
    public void setLaundryUnit(String laundryUnit) {
        this.laundryUnit = laundryUnit;
    }

    @PropertyName("elevatorPrice")
    public double getElevatorPrice() {
        return elevatorPrice;
    }

    @PropertyName("elevatorPrice")
    public void setElevatorPrice(double elevatorPrice) {
        this.elevatorPrice = elevatorPrice;
    }

    @PropertyName("elevatorUnit")
    public String getElevatorUnit() {
        return elevatorUnit;
    }

    @PropertyName("elevatorUnit")
    public void setElevatorUnit(String elevatorUnit) {
        this.elevatorUnit = elevatorUnit;
    }

    @PropertyName("cableTvPrice")
    public double getCableTvPrice() {
        return cableTvPrice;
    }

    @PropertyName("cableTvPrice")
    public void setCableTvPrice(double cableTvPrice) {
        this.cableTvPrice = cableTvPrice;
    }

    @PropertyName("cableTvUnit")
    public String getCableTvUnit() {
        return cableTvUnit;
    }

    @PropertyName("cableTvUnit")
    public void setCableTvUnit(String cableTvUnit) {
        this.cableTvUnit = cableTvUnit;
    }

    @PropertyName("trashPrice")
    public double getTrashPrice() {
        return trashPrice;
    }

    @PropertyName("trashPrice")
    public void setTrashPrice(double trashPrice) {
        this.trashPrice = trashPrice;
    }

    @PropertyName("trashUnit")
    public String getTrashUnit() {
        return trashUnit;
    }

    @PropertyName("trashUnit")
    public void setTrashUnit(String trashUnit) {
        this.trashUnit = trashUnit;
    }

    @PropertyName("servicePrice")
    public double getServicePrice() {
        return servicePrice;
    }

    @PropertyName("servicePrice")
    public void setServicePrice(double servicePrice) {
        this.servicePrice = servicePrice;
    }

    @PropertyName("serviceUnit")
    public String getServiceUnit() {
        return serviceUnit;
    }

    @PropertyName("serviceUnit")
    public void setServiceUnit(String serviceUnit) {
        this.serviceUnit = serviceUnit;
    }

    @PropertyName("extraFees")
    public List<ExtraFee> getExtraFees() {
        return extraFees;
    }

    @PropertyName("extraFees")
    public void setExtraFees(List<ExtraFee> extraFees) {
        this.extraFees = extraFees;
    }

    @PropertyName("paymentQrUrl")
    public String getPaymentQrUrl() {
        return paymentQrUrl;
    }

    @PropertyName("paymentQrUrl")
    public void setPaymentQrUrl(String paymentQrUrl) {
        this.paymentQrUrl = paymentQrUrl;
    }

    @PropertyName("billingReminderAt")
    public String getBillingReminderAt() {
        return billingReminderAt;
    }

    @PropertyName("billingReminderAt")
    public void setBillingReminderAt(String billingReminderAt) {
        this.billingReminderAt = billingReminderAt;
    }

    public com.google.firebase.Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(com.google.firebase.Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt != null
                ? new com.google.firebase.Timestamp(new java.util.Date(createdAt))
                : null;
    }

    public void setCreatedAt(java.util.Date createdAt) {
        this.createdAt = createdAt != null
                ? new com.google.firebase.Timestamp(createdAt)
                : null;
    }

    public com.google.firebase.Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(com.google.firebase.Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt != null
                ? new com.google.firebase.Timestamp(new java.util.Date(updatedAt))
                : null;
    }

    public void setUpdatedAt(java.util.Date updatedAt) {
        this.updatedAt = updatedAt != null
                ? new com.google.firebase.Timestamp(updatedAt)
                : null;
    }

    public static class ExtraFee {
        private String feeName;
        private String unit;
        private double price;

        public ExtraFee() {
        }

        public ExtraFee(String feeName, String unit, double price) {
            this.feeName = feeName;
            this.unit = unit;
            this.price = price;
        }

        @PropertyName("feeName")
        public String getFeeName() {
            return feeName;
        }

        @PropertyName("feeName")
        public void setFeeName(String feeName) {
            this.feeName = feeName;
        }

        @PropertyName("unit")
        public String getUnit() {
            return unit;
        }

        @PropertyName("unit")
        public void setUnit(String unit) {
            this.unit = unit;
        }

        @PropertyName("price")
        public double getPrice() {
            return price;
        }

        @PropertyName("price")
        public void setPrice(double price) {
            this.price = price;
        }
    }
}
