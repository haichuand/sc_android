package com.mono.chat;

/**
 * Created by hduan on 3/8/2016.
 */
public class ChatMessage {
    private long userId;
    private boolean isMyMessage;
    private String messageText;

    public ChatMessage(long userId, boolean isMyMessage, String messageText) {
        this.userId = userId;
        this.isMyMessage = isMyMessage;
        this.messageText = messageText;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public boolean isMyMessage() {
        return isMyMessage;
    }

    public void setIsMyMessage(boolean isMyMessage) {
        this.isMyMessage = isMyMessage;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }


}
