package com.example.myapplication.model;

public class Contract {
    private String id;
    private String roomId;
    private String renterId;
    private String startDate; // dd/MM/yyyy
    private String endDate;   // dd/MM/yyyy
    private double deposit;
    private double rent;
    private String status; // ACTIVE/ENDED/DRAFT...

    public Contract() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getRenterId() { return renterId; }
    public void setRenterId(String renterId) { this.renterId = renterId; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public double getDeposit() { return deposit; }
    public void setDeposit(double deposit) { this.deposit = deposit; }

    public double getRent() { return rent; }
    public void setRent(double rent) { this.rent = rent; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
