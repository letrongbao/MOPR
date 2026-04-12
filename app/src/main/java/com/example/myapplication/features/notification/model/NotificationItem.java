package com.example.myapplication.features.notification.model;

import com.google.firebase.Timestamp;

public class NotificationItem {
    public String id;
    public String title;
    public String body;
    public String type;
    public String ticketId;
    public String conversationId;
    public String invoiceId;
    public boolean isRead;
    public boolean isSystem;
    public Timestamp createdAt;

    public NotificationItem() {
    }
}
