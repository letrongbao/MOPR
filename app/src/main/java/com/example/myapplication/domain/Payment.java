package com.example.myapplication.domain;

public class Payment {
    private String id;
    private String invoiceId;
    private String roomId; // for tenant isolation
    private double amount;
    private String method; // CASH/BANK
    private String paidAt; // dd/MM/yyyy
    private String note;

    public Payment() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getInvoiceId() { return invoiceId; }
    public void setInvoiceId(String invoiceId) { this.invoiceId = invoiceId; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getPaidAt() { return paidAt; }
    public void setPaidAt(String paidAt) { this.paidAt = paidAt; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
