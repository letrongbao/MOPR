package com.example.myapplication.features.notification;

import androidx.annotation.Nullable;

public final class ChatForegroundState {

    private static volatile String activeConversationId;

    private ChatForegroundState() {
    }

    public static void setActiveConversationId(@Nullable String conversationId) {
        activeConversationId = conversationId;
    }

    public static @Nullable String getActiveConversationId() {
        return activeConversationId;
    }
}
