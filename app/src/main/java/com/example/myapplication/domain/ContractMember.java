package com.example.myapplication.domain;

public class ContractMember {
    private String id;
    private String contractId;
    private String roomId;
    private String roomNumber;
    private String uid;
    private String fullName;
    private String personalId;
    private String personalIdImageUrl;
    private String phoneNumber;
    private boolean primaryContact;
    private boolean contractRepresentative;
    private boolean temporaryResident;
    private boolean temporaryAbsent;
    private boolean fullyDocumented;
    private boolean accountLinked;
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

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
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

    public String getPersonalIdImageUrl() {
        return personalIdImageUrl;
    }

    public void setPersonalIdImageUrl(String personalIdImageUrl) {
        this.personalIdImageUrl = personalIdImageUrl;
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

    public boolean isTemporaryAbsent() {
        return temporaryAbsent;
    }

    public void setTemporaryAbsent(boolean temporaryAbsent) {
        this.temporaryAbsent = temporaryAbsent;
    }

    public void setFullyDocumented(boolean fullyDocumented) {
        this.fullyDocumented = fullyDocumented;
    }

    public boolean isAccountLinked() {
        return accountLinked;
    }

    public void setAccountLinked(boolean accountLinked) {
        this.accountLinked = accountLinked;
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
