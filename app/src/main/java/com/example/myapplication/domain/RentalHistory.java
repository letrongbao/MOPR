package com.example.myapplication.domain;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

/**
 * Internal note.
 * Internal note.
 */
public class RentalHistory {
    private String id;
    private String contractId;
    private String roomId;
    private String roomNumber; // Room code (e.g., "P101")
    private String houseName; // House/property name
    private int floor; // Floor number

    // Internal note.
    private String tenantId;
    private String fullName;
    private String personalId;
    private String phoneNumber;
    private String address;
    private String contractNumber;
    private int memberCount;

    // Internal note.
    private String rentalStartDate; // Format: "dd/MM/yyyy"
    private String contractEndDate; // Format: "dd/MM/yyyy"
    private String actualEndDate;
    private int contractDurationMonths;
    private int rentalDaysCount;

    // Financial information
    private double roomPrice;
    private double depositAmount; // Deposit amount
    private double totalPaidAmount;
    private int paidInvoiceCount; // Number of paid invoices
    private int unpaidInvoiceCount;

    // Services used
    private boolean hasParkingService;
    private boolean hasInternetService;
    private boolean hasLaundryService;
    private int vehicleCount;

    // Internal note.
    private String note;
    private String terminationReason;

    // Timestamps
    private Timestamp createdAt;
    private Long startTimestamp;
    private Long endTimestamp;

    public RentalHistory() {
        // Required for Firestore
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @PropertyName("contractId")
    public String getContractId() {
        return contractId;
    }

    @PropertyName("contractId")
    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    @PropertyName("roomId")
    public String getRoomId() {
        return roomId;
    }

    @PropertyName("roomId")
    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    @PropertyName("roomNumber")
    public String getRoomNumber() {
        return roomNumber;
    }

    @PropertyName("roomNumber")
    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    @PropertyName("houseName")
    public String getHouseName() {
        return houseName;
    }

    @PropertyName("houseName")
    public void setHouseName(String houseName) {
        this.houseName = houseName;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    @PropertyName("tenantId")
    public String getTenantId() {
        return tenantId;
    }

    @PropertyName("tenantId")
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPersonalId() {
        return personalId;
    }

    public void setPersonalId(String personalId) {
        this.personalId = personalId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContractNumber() {
        return contractNumber;
    }

    public void setContractNumber(String contractNumber) {
        this.contractNumber = contractNumber;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public String getRentalStartDate() {
        return rentalStartDate;
    }

    public void setRentalStartDate(String rentalStartDate) {
        this.rentalStartDate = rentalStartDate;
    }

    public String getContractEndDate() {
        return contractEndDate;
    }

    public void setContractEndDate(String contractEndDate) {
        this.contractEndDate = contractEndDate;
    }

    public String getActualEndDate() {
        return actualEndDate;
    }

    public void setActualEndDate(String actualEndDate) {
        this.actualEndDate = actualEndDate;
    }

    public int getContractMonths() {
        return contractDurationMonths;
    }

    public void setContractMonths(int contractDurationMonths) {
        this.contractDurationMonths = contractDurationMonths;
    }

    public int getActualRentalDays() {
        return rentalDaysCount;
    }

    public void setActualRentalDays(int rentalDaysCount) {
        this.rentalDaysCount = rentalDaysCount;
    }

    public double getRoomPrice() {
        return roomPrice;
    }

    public void setRoomPrice(double roomPrice) {
        this.roomPrice = roomPrice;
    }

    public double getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(double depositAmount) {
        this.depositAmount = depositAmount;
    }

    public double getTotalPaidAmount() {
        return totalPaidAmount;
    }

    public void setTotalPaidAmount(double totalPaidAmount) {
        this.totalPaidAmount = totalPaidAmount;
    }

    public int getPaidInvoiceCount() {
        return paidInvoiceCount;
    }

    public void setPaidInvoiceCount(int paidInvoiceCount) {
        this.paidInvoiceCount = paidInvoiceCount;
    }

    public int getUnpaidInvoiceCount() {
        return unpaidInvoiceCount;
    }

    public void setUnpaidInvoiceCount(int unpaidInvoiceCount) {
        this.unpaidInvoiceCount = unpaidInvoiceCount;
    }

    public boolean hasParkingService() {
        return hasParkingService;
    }

    public void setHasParkingService(boolean hasParkingService) {
        this.hasParkingService = hasParkingService;
    }

    public boolean hasInternetService() {
        return hasInternetService;
    }

    public void setHasInternetService(boolean hasInternetService) {
        this.hasInternetService = hasInternetService;
    }

    public boolean hasLaundryService() {
        return hasLaundryService;
    }

    public void setHasLaundryService(boolean hasLaundryService) {
        this.hasLaundryService = hasLaundryService;
    }

    public int getVehicleCount() {
        return vehicleCount;
    }

    public void setVehicleCount(int vehicleCount) {
        this.vehicleCount = vehicleCount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getEndReason() {
        return terminationReason;
    }

    public void setEndReason(String terminationReason) {
        this.terminationReason = terminationReason;
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
