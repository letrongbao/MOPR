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
    // "start_month" | "end_month"
    private String billingReminderAt;

    // Bank transfer info
    private String bankAccountName;
    private String bankName;
    private String bankAccountNo;

    // Fee table (VND)
    private double electricityPrice;
    private double waterPrice;
    private String waterCalculationMethod; // "per_person" | "meter" | "room"
    private double parkingPrice;
    private double internetPrice;
    private double laundryPrice;
    private double elevatorPrice;
    private double cableTvPrice;
    private double trashPrice;
    private double servicePrice;

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

    @PropertyName("internetPrice")
    public double getInternetPrice() {
        return internetPrice;
    }

    @PropertyName("internetPrice")
    public void setInternetPrice(double internetPrice) {
        this.internetPrice = internetPrice;
    }

    @PropertyName("laundryPrice")
    public double getLaundryPrice() {
        return laundryPrice;
    }

    @PropertyName("laundryPrice")
    public void setLaundryPrice(double laundryPrice) {
        this.laundryPrice = laundryPrice;
    }

    @PropertyName("elevatorPrice")
    public double getElevatorPrice() {
        return elevatorPrice;
    }

    @PropertyName("elevatorPrice")
    public void setElevatorPrice(double elevatorPrice) {
        this.elevatorPrice = elevatorPrice;
    }

    @PropertyName("cableTvPrice")
    public double getCableTvPrice() {
        return cableTvPrice;
    }

    @PropertyName("cableTvPrice")
    public void setCableTvPrice(double cableTvPrice) {
        this.cableTvPrice = cableTvPrice;
    }

    @PropertyName("trashPrice")
    public double getTrashPrice() {
        return trashPrice;
    }

    @PropertyName("trashPrice")
    public void setTrashPrice(double trashPrice) {
        this.trashPrice = trashPrice;
    }

    @PropertyName("servicePrice")
    public double getServicePrice() {
        return servicePrice;
    }

    @PropertyName("servicePrice")
    public void setServicePrice(double servicePrice) {
        this.servicePrice = servicePrice;
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

    public com.google.firebase.Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(com.google.firebase.Timestamp updatedAt) {
        this.updatedAt = updatedAt;
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
