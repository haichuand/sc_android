package com.mono.chat;

import android.content.Context;

import com.mono.db.DatabaseHelper;
import com.mono.db.dao.AttendeeDataSource;
import com.mono.db.dao.ConversationDataSource;
import com.mono.model.Attendee;
import com.mono.model.Conversation;
import com.mono.model.Message;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by hduan on 3/28/2016.
 */
public class ConversationManager {

    private static ConversationManager instance;
    private Context context;
    private ConversationDataSource conversationDataSource;
    private AttendeeDataSource attendeeDataSource;
    private final List<ConversationBroadcastListener> listeners = new LinkedList<>();
    private String activeConversationId = null;

    private ConversationManager(Context context) {
        this.context = context;
        conversationDataSource = DatabaseHelper.getDataSource(context, ConversationDataSource.class);
        attendeeDataSource = DatabaseHelper.getDataSource(context, AttendeeDataSource.class);
    }

    public static ConversationManager getInstance(Context context) {
        if (instance == null) {
            instance = new ConversationManager(context.getApplicationContext());
        }
        return instance;
    }

    public String getActiveConversationId() {
        return activeConversationId;
    }

    public void setActiveConversationId(String activeConversationId) {
        this.activeConversationId = activeConversationId;
    }

//    public String createConversation(String name, String eventId, List<String> attendeeIds, String creatorId) {
//        return conversationDataSource.createConversationWithSelectedAttendees(name, eventId, attendeeIds, creatorId);
//    }

    public String getUniqueConversationId () {
        return conversationDataSource.getUniqueConversationId();
    }

    public List<Conversation> getConversations() {
        return conversationDataSource.getConversations();
    }

    public List<Conversation> getAllConversations() {
        return conversationDataSource.getAllConversationsOrderByLastMessageTime();
    }

    /**
     * Get conversation including attendees and messages
     * @param conversationId
     * @return
     */
    public Conversation getCompleteConversation (String conversationId) {
        return conversationDataSource.getConversation(conversationId, true, true);
    }

    public List<Conversation> getConversations(String eventId) {
        return conversationDataSource.getConversations(eventId);
    }

    public List<Message> getChatMessages(String conversationId) {
        return conversationDataSource.getConversationMessages(conversationId);
    }

    public ChatAttendeeMap getChatAttendeeMap(String conversationId) {
        List<Attendee> attendeeList = conversationDataSource.getConversationAttendees(conversationId);
        return new ChatAttendeeMap(attendeeList);
    }

    public long saveChatMessageToDB(Message message) {
        return conversationDataSource.addMessageToConversation(message);
    }

    public void addAttendee(String conversationId, String attendeeId) {
        conversationDataSource.addAttendeeToConversation(conversationId, attendeeId);
    }

    public void addAttendees(String conversationId, List<String> attendeeIds) {
        conversationDataSource.addAttendeesToConversation(conversationId, attendeeIds);
    }

    public void setAttendees(String conversationId, List<String> attendeeIds) {
        conversationDataSource.clearConversationAttendees(conversationId);
        conversationDataSource.addAttendeesToConversation(conversationId, attendeeIds);
    }

    public void dropAttendees (String conversationId, List<String> dropAttendeesId) {
        conversationDataSource.dropAttendeesFromConversation(conversationId, dropAttendeesId);
    }

    public boolean setConversationSyncNeeded(String conversationId, boolean isSynNeeded) {
        return conversationDataSource.setConversationSyncNeeded(conversationId, isSynNeeded);
    }

    public boolean setConversationMessageAckAndTimestamp (long messageId, boolean ack, long timestamp) {
        return conversationDataSource.setConversationMessageAckAndTimestamp(messageId, ack, timestamp);
    }

    public List<String> getChatAttendeeIdList(ChatAttendeeMap attendeeMap, String myId) {
        ArrayList<String> attendeeIdList = new ArrayList<>(attendeeMap.getAttendeeMap().keySet());
//        attendeeIdList.remove(myId); //commented out to send message to user self for confirmation
        return attendeeIdList;
    }

    public Attendee getAttendeeById(String id) {
        return attendeeDataSource.getAttendeeById(id);
    }

    public List<Attendee> getAllUserList() {
        return attendeeDataSource.getAttendees();
    }

    public void addListener (ConversationBroadcastListener listener) {
        listeners.add(listener);
    }

    public void removeListener (ConversationBroadcastListener listener) {
        listeners.remove(listener);
    }

    public void notifyListenersNewConversation(Conversation conversation, int index) {
        for (ConversationBroadcastListener listener : listeners) {
            listener.onNewConversation(conversation, index);
        }
    }

    public void notifyListenersNewConversationAttendees (String conversationId, List<String> newAttendeeIds) {
        for (ConversationBroadcastListener listener : listeners) {
            listener.onNewConversationAttendees(conversationId, newAttendeeIds);
        }
    }

    public void notifyListenersNewConversationMessage (Message message) {
        for (ConversationBroadcastListener listener : listeners) {
            listener.onNewConversationMessage(message);
        }
    }

    public interface ConversationBroadcastListener {
        void onNewConversation(Conversation conversation, int index);
        void onNewConversationMessage(Message message);
        void onNewConversationAttendees (String conversationId, List<String> newAttendeeIds);
        void onDropConversationAttendees (String conversationId, List<String> dropAttendeeIds);
    }
}
