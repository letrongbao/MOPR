package com.example.myapplication.features.notification.model;

import com.google.firebase.Timestamp;

public class NotificationItem {
    public String id;
    public String title;
    public String body;
    public String conversationId;
    public boolean isRead;
    public Timestamp createdAt;

    public NotificationItem() {
    }
}
