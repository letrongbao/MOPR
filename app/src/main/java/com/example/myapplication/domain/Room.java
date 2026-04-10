package com.example.myapplication.domain;

import com.google.firebase.firestore.PropertyName;

@com.google.firebase.firestore.IgnoreExtraProperties
public class Room {
    private String id;
    private String roomNumber;
    private String roomType; // "Single", "Double", "Shared"
    private double area;
    private double rentAmount;
    private String status; // "Vacant", "Occupied"
    private String imageUrl; // Image URL from Firebase Storage

    // Group rooms by house
    private String houseId;
    private String houseName;

    // Enhanced fields
    private int floor; // Floor number
    private String description; // Description
    private java.util.List<String> amenities; // Amenities: ["AC", "balcony", "kitchen"]
    private int maxOccupancy; // Max occupancy

    // Audit timestamps
    private com.google.firebase.Timestamp createdAt;
    private com.google.firebase.Timestamp updatedAt;

    public Room() {
    } // Firestore requires an empty constructor

    public Room(String roomNumber, String roomType, double area,
            double rentAmount, String status) {
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.area = area;
        this.rentAmount = rentAmount;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public double getArea() {
        return area;
    }

    public void setArea(double area) {
        this.area = area;
    }

    public double getRentAmount() {
        return rentAmount;
    }

    public void setRentAmount(double rentAmount) {
        this.rentAmount = rentAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @PropertyName("houseId")
    public String getHouseId() {
        return houseId;
    }

    @PropertyName("houseId")
    public void setHouseId(String houseId) {
        this.houseId = houseId;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public java.util.List<String> getAmenities() {
        return amenities;
    }

    public void setAmenities(java.util.List<String> amenities) {
        this.amenities = amenities;
    }

    public int getMaxOccupancy() {
        return maxOccupancy;
    }

    public void setMaxOccupancy(int maxOccupancy) {
        this.maxOccupancy = maxOccupancy;
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
