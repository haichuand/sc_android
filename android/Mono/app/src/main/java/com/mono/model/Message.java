package com.mono.model;

/**
 * Created by xuejing on 3/10/16.
 */
public class Message {

    private String senderId;
    private String conversationId;
    private String messageText;
    private long timestamp;
    public int color;
    public String title;
    public String firstName;
    public String lastName;
    public String username;
    private String messageId; //used for matching user self-sent messages
    public boolean showWarningIcon = false; //flag to show warning icon

    public Message(String senderId, String conversationId, String messageText, long timestamp) {
        this.senderId = senderId;
        this.conversationId = conversationId;
        this.messageText = messageText;
        this.timestamp = timestamp;
    }

    public Message(String senderId, String conversationId, String messageText, long timestamp, String messageId) {
        this.senderId = senderId;
        this.conversationId = conversationId;
        this.messageText = messageText;
        this.timestamp = timestamp;
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getConversationId() {
        return this.conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
}
