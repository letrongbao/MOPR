package com.example.myapplication.domain;

import com.google.firebase.firestore.PropertyName;

import java.util.ArrayList;
import java.util.List;

public class Tenant {
    private String id;
    private String fullName;
    private String personalId;
    private String phoneNumber;

    // Contract/customer extended info
    private String address;
    private String contractNumber;
    private String personalIdFrontUrl;
    private String personalIdBackUrl;

    // Contract representative (can differ from primary tenant)
    private String representativeName;
    private String representativeId;

    // Room linkage
    private String roomId;
    private String previousRoomId;
    private String roomNumber;

    private int memberCount;
    private String rentalStartDate;
    private String contractEndDate;

    // Firestore Int64 fields (preferred)
    private long rentAmount;
    private long depositAmount;
    private long contractEndTimestamp;

    // Legacy numeric fields (kept for backward compatibility)
    private double roomPrice;
    private double legacyDepositAmount;

    // Invoice display toggles
    private boolean showDepositOnInvoice = true;
    private boolean showNoteOnInvoice = true;

    private int contractDurationMonths;
    private boolean remindOneMonthBefore = true;
    private String billingStartPolicy = "current_month";
    private String billingStartPeriod;
    private String billingReminderAt = "start_month";
    private int electricStartReading;
    private int waterStartReading;

    private boolean hasParkingService;
    private int vehicleCount;
    private boolean hasInternetService;
    private boolean hasLaundryService;
    private List<String> selectedExtraFeeNames;

    private String note;

    // Deposit collection status
    private boolean depositCollectionStatus;

    // ACTIVE / ENDED
    private String contractStatus = "ACTIVE";
    private Long createdAt;
    private Long updatedAt;
    private Long endedAt;

    // Advanced tenant management features
    private boolean isPrimaryContact;
    private boolean contractRepresentative;
    private boolean temporaryResident;
    private boolean fullyDocumented;

    public Tenant() {
    }

    public Tenant(String fullName, String personalId, String phoneNumber, String roomId,
            int memberCount, String rentalStartDate,
            String contractEndDate, double depositAmount) {
        this.fullName = fullName;
        this.personalId = personalId;
        this.phoneNumber = phoneNumber;
        this.roomId = roomId;
        this.memberCount = memberCount;
        this.rentalStartDate = rentalStartDate;
        this.contractEndDate = contractEndDate;
        this.depositAmount = (long) depositAmount;
        this.legacyDepositAmount = depositAmount;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getPersonalIdFrontUrl() {
        return personalIdFrontUrl;
    }

    public void setPersonalIdFrontUrl(String personalIdFrontUrl) {
        this.personalIdFrontUrl = personalIdFrontUrl;
    }

    public String getPersonalIdBackUrl() {
        return personalIdBackUrl;
    }

    public void setPersonalIdBackUrl(String personalIdBackUrl) {
        this.personalIdBackUrl = personalIdBackUrl;
    }

    public String getRepresentativeName() {
        return representativeName;
    }

    public void setRepresentativeName(String representativeName) {
        this.representativeName = representativeName;
    }

    public String getRepresentativeId() {
        return representativeId;
    }

    public void setRepresentativeId(String representativeId) {
        this.representativeId = representativeId;
    }

    @PropertyName("roomId")
    public String getRoomId() {
        return roomId;
    }

    @PropertyName("roomId")
    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    @PropertyName("previousRoomId")
    public String getPreviousRoomId() {
        return previousRoomId;
    }

    @PropertyName("previousRoomId")
    public void setPreviousRoomId(String previousRoomId) {
        this.previousRoomId = previousRoomId;
    }

    @PropertyName("roomNumber")
    public String getRoomNumber() {
        return roomNumber;
    }

    @PropertyName("roomNumber")
    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
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

    public long getRentAmount() {
        return rentAmount > 0 ? rentAmount : (long) roomPrice;
    }

    public void setRentAmount(long rentAmount) {
        this.rentAmount = rentAmount;
        this.roomPrice = rentAmount;
    }

    public long getDepositAmount() {
        return depositAmount > 0 ? depositAmount : (long) legacyDepositAmount;
    }

    public void setDepositAmount(long depositAmount) {
        this.depositAmount = depositAmount;
        this.legacyDepositAmount = depositAmount;
    }

    @PropertyName("contractEndTimestamp")
    public long getContractEndTimestamp() {
        return contractEndTimestamp;
    }

    @PropertyName("contractEndTimestamp")
    public void setContractEndTimestamp(long contractEndTimestamp) {
        this.contractEndTimestamp = contractEndTimestamp;
    }

    public double getRoomPrice() {
        return roomPrice;
    }

    public void setRoomPrice(double roomPrice) {
        this.roomPrice = roomPrice;
    }

    @PropertyName("legacyDepositAmount")
    public double getLegacyDepositAmount() {
        return legacyDepositAmount;
    }

    @PropertyName("legacyDepositAmount")
    public void setLegacyDepositAmount(double legacyDepositAmount) {
        this.legacyDepositAmount = legacyDepositAmount;
    }

    @PropertyName("showDepositOnInvoice")
    public boolean isShowDepositOnInvoice() {
        return showDepositOnInvoice;
    }

    @PropertyName("showDepositOnInvoice")
    public void setShowDepositOnInvoice(boolean showDepositOnInvoice) {
        this.showDepositOnInvoice = showDepositOnInvoice;
    }

    @PropertyName("contractDurationMonths")
    public int getContractDurationMonths() {
        return contractDurationMonths;
    }

    @PropertyName("contractDurationMonths")
    public void setContractDurationMonths(int contractDurationMonths) {
        this.contractDurationMonths = contractDurationMonths;
    }

    @PropertyName("remindOneMonthBefore")
    public boolean isRemindOneMonthBefore() {
        return remindOneMonthBefore;
    }

    @PropertyName("remindOneMonthBefore")
    public void setRemindOneMonthBefore(boolean remindOneMonthBefore) {
        this.remindOneMonthBefore = remindOneMonthBefore;
    }

    @PropertyName("billingStartPolicy")
    public String getBillingStartPolicy() {
        return billingStartPolicy;
    }

    @PropertyName("billingStartPolicy")
    public void setBillingStartPolicy(String billingStartPolicy) {
        this.billingStartPolicy = billingStartPolicy;
    }

    @PropertyName("billingStartPeriod")
    public String getBillingStartPeriod() {
        return billingStartPeriod;
    }

    @PropertyName("billingStartPeriod")
    public void setBillingStartPeriod(String billingStartPeriod) {
        this.billingStartPeriod = billingStartPeriod;
    }

    public String getBillingReminderAt() {
        return billingReminderAt;
    }

    public void setBillingReminderAt(String billingReminderAt) {
        this.billingReminderAt = billingReminderAt;
    }

    public int getElectricStartReading() {
        return electricStartReading;
    }

    public void setElectricStartReading(int electricStartReading) {
        this.electricStartReading = electricStartReading;
    }

    public int getWaterStartReading() {
        return waterStartReading;
    }

    public void setWaterStartReading(int waterStartReading) {
        this.waterStartReading = waterStartReading;
    }

    public boolean hasParkingService() {
        return hasParkingService;
    }

    @PropertyName("hasParkingService")
    public void setHasParkingService(boolean hasParkingService) {
        this.hasParkingService = hasParkingService;
    }

    public int getVehicleCount() {
        return vehicleCount;
    }

    public void setVehicleCount(int vehicleCount) {
        this.vehicleCount = vehicleCount;
    }

    public boolean hasInternetService() {
        return hasInternetService;
    }

    @PropertyName("hasInternetService")
    public void setHasInternetService(boolean hasInternetService) {
        this.hasInternetService = hasInternetService;
    }

    public boolean hasLaundryService() {
        return hasLaundryService;
    }

    @PropertyName("hasLaundryService")
    public void setHasLaundryService(boolean hasLaundryService) {
        this.hasLaundryService = hasLaundryService;
    }

    @PropertyName("selectedExtraFeeNames")
    public List<String> getSelectedExtraFeeNames() {
        return selectedExtraFeeNames;
    }

    @PropertyName("selectedExtraFeeNames")
    public void setSelectedExtraFeeNames(List<String> selectedExtraFeeNames) {
        if (selectedExtraFeeNames == null) {
            this.selectedExtraFeeNames = null;
            return;
        }
        this.selectedExtraFeeNames = new ArrayList<>(selectedExtraFeeNames);
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @PropertyName("showNoteOnInvoice")
    public boolean isShowNoteOnInvoice() {
        return showNoteOnInvoice;
    }

    @PropertyName("showNoteOnInvoice")
    public void setShowNoteOnInvoice(boolean showNoteOnInvoice) {
        this.showNoteOnInvoice = showNoteOnInvoice;
    }

    @PropertyName("depositCollectionStatus")
    public boolean isDepositCollected() {
        return depositCollectionStatus;
    }

    @PropertyName("depositCollectionStatus")
    public void setDepositCollected(boolean depositCollectionStatus) {
        this.depositCollectionStatus = depositCollectionStatus;
    }

    @PropertyName("contractStatus")
    public String getContractStatus() {
        return contractStatus;
    }

    @PropertyName("contractStatus")
    public void setContractStatus(String contractStatus) {
        this.contractStatus = contractStatus;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Long endedAt) {
        this.endedAt = endedAt;
    }

    public boolean isPrimaryContact() {
        return isPrimaryContact;
    }

    public void setPrimaryContact(boolean primaryContact) {
        isPrimaryContact = primaryContact;
    }

    @PropertyName("contractRepresentative")
    public boolean isContractRepresentative() {
        return contractRepresentative;
    }

    @PropertyName("contractRepresentative")
    public void setContractRepresentative(boolean contractRepresentative) {
        this.contractRepresentative = contractRepresentative;
    }

    @PropertyName("temporaryResident")
    public boolean isTemporaryResident() {
        return temporaryResident;
    }

    @PropertyName("temporaryResident")
    public void setTemporaryResident(boolean temporaryResident) {
        this.temporaryResident = temporaryResident;
    }

    @PropertyName("fullyDocumented")
    public boolean isFullyDocumented() {
        return fullyDocumented;
    }

    @PropertyName("fullyDocumented")
    public void setFullyDocumented(boolean fullyDocumented) {
        this.fullyDocumented = fullyDocumented;
    }

    @PropertyName("hasInternetService")
    public boolean isHasInternetService() { return hasInternetService; }
    
    @PropertyName("hasParkingService")
    public boolean isHasParkingService() { return hasParkingService; }
    
    @PropertyName("hasLaundryService")
    public boolean isHasLaundryService() { return hasLaundryService; }
}
