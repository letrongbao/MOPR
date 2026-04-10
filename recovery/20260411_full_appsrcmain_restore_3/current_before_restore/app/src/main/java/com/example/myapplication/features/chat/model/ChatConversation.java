package com.example.myapplication.features.chat.model;

import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class ChatConversation {
    public String id;
    public String type;
    public String title;
    public String subtitle;
    public String roomId;
    public List<String> participantIds = new ArrayList<>();
    public int unreadCount;
    public Timestamp lastMessageAt;
    public Timestamp updatedAt;

    public ChatConversation() {
    }
}
