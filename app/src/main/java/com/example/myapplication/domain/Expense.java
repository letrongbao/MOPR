package com.example.myapplication.domain;

import com.example.myapplication.core.constants.ExpenseStatus;
import com.google.firebase.Timestamp;

@com.google.firebase.firestore.IgnoreExtraProperties
public class Expense {
    private String id;
    private String category;
    private double amount;
    private String paidAt; // dd/MM/yyyy
    private String periodMonth; // MM/yyyy for month-based reporting/filtering
    private String status; // PENDING | PAID
    private String note;
    private Timestamp createdAt;

    private String houseId;

    public Expense() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getHouseId() { return houseId; }
    public void setHouseId(String houseId) { this.houseId = houseId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getPaidAt() { return paidAt; }
    public void setPaidAt(String paidAt) { this.paidAt = paidAt; }

    public String getPeriodMonth() { return periodMonth; }
    public void setPeriodMonth(String periodMonth) { this.periodMonth = periodMonth; }

    public String getStatus() {
        if (status == null || status.trim().isEmpty()) {
            return ExpenseStatus.PAID;
        }
        return status;
    }

    public void setStatus(String status) { this.status = status; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}

