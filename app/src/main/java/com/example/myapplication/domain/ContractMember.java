package com.example.myapplication.domain;

public class ContractMember {
    private String id;
    private String contractId;
    private String roomId;
    private String roomNumber;
    private String fullName;
    private String personalId;
    private String phoneNumber;
    private boolean primaryContact;
    private boolean contractRepresentative;
    private boolean temporaryResident;
    private boolean fullyDocumented;
    private boolean active = true;
    private Long createdAt;
    private Long updatedAt;

    public ContractMember() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContractId() {
        return contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
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

    public boolean isPrimaryContact() {
        return primaryContact;
    }

    public void setPrimaryContact(boolean primaryContact) {
        this.primaryContact = primaryContact;
    }

    public boolean isContractRepresentative() {
        return contractRepresentative;
    }

    public void setContractRepresentative(boolean contractRepresentative) {
        this.contractRepresentative = contractRepresentative;
    }

    public boolean isTemporaryResident() {
        return temporaryResident;
    }

    public void setTemporaryResident(boolean temporaryResident) {
        this.temporaryResident = temporaryResident;
    }

    public boolean isFullyDocumented() {
        return fullyDocumented;
    }

    public void setFullyDocumented(boolean fullyDocumented) {
        this.fullyDocumented = fullyDocumented;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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
}
