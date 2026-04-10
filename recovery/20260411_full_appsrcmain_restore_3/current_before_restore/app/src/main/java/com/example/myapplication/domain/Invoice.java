package com.example.myapplication.domain;

import com.google.firebase.firestore.PropertyName;

import java.util.ArrayList;
import java.util.List;

@com.google.firebase.firestore.IgnoreExtraProperties
public class Invoice {
    private String id;
    private String roomId;
    private String contractId;
    private String billingPeriod; // "03/2026"
    private double electricStartReading;
    private double electricEndReading;
    private double electricUnitPrice;
    private double waterStartReading;
    private double waterEndReading;
    private double waterUnitPrice;
    private double trashFee;
    private double internetFee;
    private double wifiFee; // Legacy support
    private double parkingFee;
    private double laundryFee;
    private double otherFee;
    private List<String> otherFeeLines;
    private double rentAmount;
    private double totalAmount;
    private String status; // "Unpaid", "Paid"

    // Enhanced payment tracking
    private com.google.firebase.Timestamp paymentDate; // Payment date
    private String paymentMethod; // "cash", "transfer", "momo", "bank"
    private double paidAmount; // Amount paid (for partial payments)
    private String ownerNote; // Owner note for follow-up on unpaid balance

    // Audit timestamps
    private com.google.firebase.Timestamp createdAt;
    private com.google.firebase.Timestamp updatedAt;

    public Invoice() {
    }

    // Automatically compute total amount
    public void calculateTotalAmount() {
        double electricityAmount = (electricEndReading - electricStartReading) * electricUnitPrice;
        double waterAmount = (waterEndReading - waterStartReading) * waterUnitPrice;
        this.totalAmount = rentAmount + electricityAmount + waterAmount + trashFee + internetFee + parkingFee + laundryFee + otherFee;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @PropertyName("roomId")
    public String getRoomId() {
        return roomId;
    }

    @PropertyName("roomId")
    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    @PropertyName("contractId")
    public String getContractId() {
        return contractId;
    }

    @PropertyName("contractId")
    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public String getBillingPeriod() {
        return billingPeriod;
    }

    public void setBillingPeriod(String billingPeriod) {
        this.billingPeriod = billingPeriod;
    }

    public double getElectricStartReading() {
        return electricStartReading;
    }

    public void setElectricStartReading(double electricStartReading) {
        this.electricStartReading = electricStartReading;
    }

    public double getElectricEndReading() {
        return electricEndReading;
    }

    public void setElectricEndReading(double electricEndReading) {
        this.electricEndReading = electricEndReading;
    }

    public double getElectricUnitPrice() {
        return electricUnitPrice;
    }

    public void setElectricUnitPrice(double electricUnitPrice) {
        this.electricUnitPrice = electricUnitPrice;
    }

    public double getWaterStartReading() {
        return waterStartReading;
    }

    public void setWaterStartReading(double waterStartReading) {
        this.waterStartReading = waterStartReading;
    }

    public double getWaterEndReading() {
        return waterEndReading;
    }

    public void setWaterEndReading(double waterEndReading) {
        this.waterEndReading = waterEndReading;
    }

    public double getWaterUnitPrice() {
        return waterUnitPrice;
    }

    public void setWaterUnitPrice(double waterUnitPrice) {
        this.waterUnitPrice = waterUnitPrice;
    }

    public double getTrashFee() {
        return trashFee;
    }

    public void setTrashFee(double trashFee) {
        this.trashFee = trashFee;
    }

    public double getInternetFee() {
        return internetFee > 0 ? internetFee : wifiFee;
    }

    public void setInternetFee(double internetFee) {
        this.internetFee = internetFee;
    }

    public double getWifiFee() {
        return wifiFee;
    }

    public void setWifiFee(double wifiFee) {
        this.wifiFee = wifiFee;
    }

    public double getParkingFee() {
        return parkingFee;
    }

    public void setParkingFee(double parkingFee) {
        this.parkingFee = parkingFee;
    }

    public double getLaundryFee() {
        return laundryFee;
    }

    public void setLaundryFee(double laundryFee) {
        this.laundryFee = laundryFee;
    }

    public double getOtherFee() {
        return otherFee;
    }

    public void setOtherFee(double otherFee) {
        this.otherFee = otherFee;
    }

    public List<String> getOtherFeeLines() {
        return otherFeeLines;
    }

    public void setOtherFeeLines(List<String> otherFeeLines) {
        if (otherFeeLines == null) {
            this.otherFeeLines = null;
            return;
        }
        this.otherFeeLines = new ArrayList<>(otherFeeLines);
    }

    public double getRentAmount() {
        return rentAmount;
    }

    public void setRentAmount(double rentAmount) {
        this.rentAmount = rentAmount;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Store room name for display (denormalized)
    private String roomNumber;

    @PropertyName("roomNumber")
    public String getRoomNumber() {
        return roomNumber;
    }

    @PropertyName("roomNumber")
    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public com.google.firebase.Timestamp getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(com.google.firebase.Timestamp paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public double getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(double paidAmount) {
        this.paidAmount = paidAmount;
    }

    public String getOwnerNote() {
        return ownerNote;
    }

    public void setOwnerNote(String ownerNote) {
        this.ownerNote = ownerNote;
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
