package com.mono.model;

import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;

/**
 * Created by xuejing on 3/10/16.
 */
public class Message {
    private String senderId;
    private String conversationId;
    private String messageText;
    private long timestamp;

    public Message(String senderId, String conversationId, String messageText, long timestamp) {
        this.senderId = senderId;
        this.conversationId = conversationId;
        this.messageText = messageText;
        this.timestamp = timestamp;

    }

    public String getUserId() {
        return senderId;
    }

    public void setUserId(String senderId) {
        this.senderId = senderId;
    }

    public String getConversationId() { return this.conversationId; }

    public void setConversationId(String conversationId) {this.conversationId = conversationId; }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public long getTimestamp() { return this.timestamp; }

    public void setTimestamp (long timestamp) { this.timestamp = timestamp; }
}
