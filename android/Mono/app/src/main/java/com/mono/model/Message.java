package com.mono.model;

import java.util.List;

/**
 * Created by xuejing on 3/10/16.
 */
public class Message {
    public static final long GROUP_TIME_THRESHOLD = 60000; //max ms between messages to group time together

    private String senderId;
    private String conversationId;
    private String messageText;
    private long timestamp;
    public int color;
    public String title;
    public String firstName;
    public String lastName;
    public String username;
    private long messageId; //message ID in database
    public boolean ack = true; //if message ack has been received from server
    public boolean showMessageTime = true; //whether to show message time
    public boolean showMessageSender = true; //whether to show sender name

    public List<Media> attachments;

//    public Message(String senderId, String conversationId, String messageText, long timestamp) {
//        this.senderId = senderId;
//        this.conversationId = conversationId;
//        this.messageText = messageText;
//        this.timestamp = timestamp;
//    }

    public Message(String senderId, String conversationId, String messageText, long timestamp) {
        this.senderId = senderId;
        this.conversationId = conversationId;
        this.messageText = messageText;
        this.timestamp = timestamp;
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

    public long getMessageId() {
        return messageId;
    }

    public void setMessageId(long messageId) {
        this.messageId = messageId;
    }
}
