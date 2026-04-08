package com.example.myapplication.features.chat.model;

import com.google.firebase.Timestamp;

public class ChatMessage {
    public String id;
    public String senderId;
    public String senderName;
    public String text;
    public Timestamp createdAt;

    public ChatMessage() {
    }
}
