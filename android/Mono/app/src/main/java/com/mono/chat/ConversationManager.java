package com.mono.chat;

import android.content.Context;
import android.widget.Toast;

import com.mono.db.DatabaseHelper;
import com.mono.db.dao.AttendeeDataSource;
import com.mono.db.dao.ConversationDataSource;
import com.mono.db.dao.EventDataSource;
import com.mono.model.Attendee;
import com.mono.model.Conversation;
import com.mono.model.Event;
import com.mono.model.Message;
import com.mono.network.HttpServerManager;
import com.mono.util.Common;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by hduan on 3/28/2016.
 */
public class ConversationManager {

    private static ConversationManager instance;
    private Context context;
    private ConversationDataSource conversationDataSource;
    private AttendeeDataSource attendeeDataSource;
    private EventDataSource eventDataSource;
    private final List<ConversationBroadcastListener> broadcastListeners = new LinkedList<>();
    private final List<ChatAckListener> chatAckListeners = new LinkedList<>();
    private String activeConversationId = null;
    private final Map<String, Attendee> allUserMap = new HashMap<>(); //id->Attendee map of all SuperCaly users in local database
    private static final char[] randomIdCharPool = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".toCharArray();
    public static final String DEFAULT_USER_PASSWORD = Common.md5("@SuperCalyUser");
    private static Random random = new Random();
    private static final int randomIdLength = 8;
    private int allChatsMissCount = 0;
    private List<ChatsMissCountListener> chatsMissCountListeners = new LinkedList<>();
    private HttpServerManager httpServerManager;

    private ConversationManager(Context context) {
        this.context = context;
        conversationDataSource = DatabaseHelper.getDataSource(context, ConversationDataSource.class);
        attendeeDataSource = DatabaseHelper.getDataSource(context, AttendeeDataSource.class);
        eventDataSource = DatabaseHelper.getDataSource(context, EventDataSource.class);
        httpServerManager = HttpServerManager.getInstance(context);
        initAllUserMap();
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

    public Conversation getConversation(String conversationId, boolean getAttendees, boolean getMessages) {
        return conversationDataSource.getConversation(conversationId, getAttendees, getMessages);
    }
    public List<Attendee> getConversationAttendees (String conversationId) {
        return conversationDataSource.getConversationAttendees(conversationId);
    }

    public List<Conversation> getConversationsDBFirst(Event event) {
        if (event.source == Event.SOURCE_DATABASE) { //local database event
            return conversationDataSource.getConversations(event.id);
        } else { //provider event, match by time and title
            List<Event> dbEvents = eventDataSource.getEvents(event.startTime, event.endTime);
            if (dbEvents.isEmpty()) {
                return new ArrayList<>();
            } else {
                Event localEvent = null;
                for (Event dbEvent : dbEvents) {
                    if (event.startTime == dbEvent.startTime && event.endTime == dbEvent.endTime && event.title.equals(dbEvent.title)) {
                        localEvent = dbEvent;
                        break;
                    }
                }
                if (localEvent == null) {
                    return new ArrayList<>();
                } else {
                    return conversationDataSource.getConversations(localEvent.id);
                }
            }
        }
    }

    public Message getMessageByMessageId (String messageId) {
        return conversationDataSource.getMessageByMessageId(messageId);
    }

    public List<String> getConversationAttendeesIds(String conversationId) {
        return conversationDataSource.getConversationAttendeesIds(conversationId);
    }

    public List<Message> getChatMessages(String conversationId) {
        return conversationDataSource.getConversationMessages(conversationId);
    }

//    public ChatAttendeeMap getChatAttendeeMap(String conversationId) {
//        List<Attendee> attendeeList = conversationDataSource.getConversationAttendees(conversationId);
//        return new ChatAttendeeMap(attendeeList);
//    }

    public long saveChatMessageToDB(Message message) {
        return conversationDataSource.addMessageToConversation(message);
    }

    public void addAttendeeToConversation(String conversationId, String attendeeId) {
        conversationDataSource.addAttendeeToConversation(conversationId, attendeeId);
    }

    public void addAttendeesToConversation(String conversationId, List<String> attendeeIds) {
        conversationDataSource.addAttendeesToConversation(conversationId, attendeeIds);
    }

    public void setAttendees(String conversationId, List<String> attendeeIds) {
        conversationDataSource.clearConversationAttendees(conversationId);
        conversationDataSource.addAttendeesToConversation(conversationId, attendeeIds);
    }

    public void dropAttendees (String conversationId, List<String> dropAttendeesId) {
        conversationDataSource.dropAttendeesFromConversation(conversationId, dropAttendeesId);
    }

    public void clearConversationAttendees (String conversationId) {
        conversationDataSource.clearConversationAttendees(conversationId);
    }

    public boolean setConversationSyncNeeded(String conversationId, boolean isSynNeeded) {
        return conversationDataSource.setConversationSyncNeeded(conversationId, isSynNeeded);
    }

    public boolean setConversationMessageAckAndTimestamp (long messageId, boolean ack, long timestamp) {
        return conversationDataSource.setConversationMessageAckAndTimestamp(messageId, ack, timestamp);
    }

//    public List<String> getChatAttendeeIdList(ChatAttendeeMap attendeeMap) {
//        ArrayList<String> attendeeIdList = new ArrayList<>(attendeeMap.getAttendeeMap().keySet());
//        return attendeeIdList;
//    }

    public boolean hasUser (String id) {
        if (allUserMap.isEmpty()) {
            initAllUserMap();
        }
        return allUserMap.containsKey(id);
    }

    public Attendee getUserById(String id) {
        if (allUserMap.isEmpty()) {
            initAllUserMap();
        }
        return allUserMap.get(id);
    }

    public List<Attendee> getAllUserList() {
        if (allUserMap.isEmpty()) {
            initAllUserMap();
        }
        return new ArrayList<>(allUserMap.values());
    }

    public boolean saveUserToDB(Attendee attendee) {
        if (attendeeDataSource.createAttendee(attendee)) {
            allUserMap.put(attendee.id, attendee);
            return true;
        }
        return false;
    }

    public boolean updateUserId(String originalId, String newId) {
        if (attendeeDataSource.updateAttendeeId(originalId, newId)) {
            Attendee attendee = allUserMap.remove(originalId);
            attendee.id = newId;
            allUserMap.put(newId, attendee);
            return true;
        }
        return false;
    }

    private void initAllUserMap() {
        List<Attendee> allUserList = attendeeDataSource.getAttendees();
        for (Attendee user : allUserList) {
            allUserMap.put(user.id, user);
        }
    }

    public void getAllChatsMissCount() {
        allChatsMissCount = conversationDataSource.getAllChatsMissCount();
    }

    public void notifyAllChatsListenersMissCount() {
        for (ChatsMissCountListener listener : chatsMissCountListeners) {
            listener.onMissCountChanged(allChatsMissCount);
        }
    }

    public void addBroadcastListner(ConversationBroadcastListener listener) {
        broadcastListeners.add(listener);
    }

    public void removeBroadcastListener(ConversationBroadcastListener listener) {
        broadcastListeners.remove(listener);
    }

    public void notifyListenersNewConversation(Conversation conversation, int index) {
        for (ConversationBroadcastListener listener : broadcastListeners) {
            listener.onNewConversation(conversation, index);
        }
    }

    public void notifyListenersNewConversationAttendees (String conversationId, List<String> newAttendeeIds) {
        for (ConversationBroadcastListener listener : broadcastListeners) {
            listener.onNewConversationAttendees(conversationId, newAttendeeIds);
        }
    }

    public void notifyListenersDropConversationAttendees(String conversationId, List<String> dropAttendeesId) {
        for (ConversationBroadcastListener listener : broadcastListeners) {
            listener.onDropConversationAttendees(conversationId, dropAttendeesId);
        }
    }

    public void notifyListenersNewConversationMessage (Message message, int missCount) {
        for (ConversationBroadcastListener listener : broadcastListeners) {
            listener.onNewConversationMessage(message, missCount);
        }
    }

    public void notifyListenersMissCountReset(String conversationId) {
        for (ConversationBroadcastListener listener : broadcastListeners) {
            listener.onConversationMissCountReset(conversationId);
        }
    }

    public void addChatAckListener(ChatAckListener listener) {
        chatAckListeners.add(listener);
    }

    public void removeChatAckListener(ChatAckListener listener) {
        chatAckListeners.remove(listener);
    }

    public void notifyListenersChatAck(String id) {
        for (ChatAckListener listener : chatAckListeners) {
            listener.onHandleChatAck(id);
        }
    }

    public void addChatsMissCountListener(ChatsMissCountListener listener) {
        chatsMissCountListeners.add(listener);
    }

    public void removeChatsMissCountListener(ChatsMissCountListener listener) {
        chatsMissCountListeners.remove(listener);
    }

    public boolean createConversation (String conversationId, String title, String creatorId, List<String> attendeesId, boolean syncNeeded, int missCount) {
        return conversationDataSource.createConversation(conversationId, title, creatorId, attendeesId, syncNeeded, missCount);
    }

    public boolean createConversation (String conversationId, String title, String creatorId, List<String> attendeesId, boolean syncNeeded) {
        return conversationDataSource.createConversation(conversationId, title, creatorId, attendeesId, syncNeeded);
    }

    public boolean createEventConversation (String eventId, String conversationId, String title, String creatorId, List<String> attendeesIdList, boolean syncNeeded, int missCount) {
        return conversationDataSource.createEventConversation(eventId, conversationId, title, creatorId, attendeesIdList, syncNeeded, missCount);
    }

    public int incrementConversationMissCount(String conversationId) {
        int newMissCount = conversationDataSource.incrementConversationMissCount(conversationId);
        allChatsMissCount++;
        return newMissCount;
    }

    public boolean resetConversationMissCount(String conversationId) {
        if(conversationDataSource.resetConversationMissCount(conversationId)) {
            allChatsMissCount = conversationDataSource.getAllChatsMissCount();
            return true;
        }

        return false;
    }

    public static List<String> getAttendeeStringtWithNameAndEmail (List<Attendee> attendeeList) {
        if (attendeeList == null) {
            return null;
        }
        List<String> attendeeStringList = new ArrayList<>();
        for (Attendee attendee : attendeeList) {
            String str = "";
            if (attendee.firstName != null && !attendee.firstName.isEmpty()) {
                str += attendee.firstName;
                if (attendee.lastName != null && !attendee.lastName.isEmpty()) {
                    str += (" " + attendee.lastName);
                }
            } else if (attendee.userName != null && !attendee.userName.isEmpty()) {
                str += attendee.userName;
            }
            if (attendee.email != null && !attendee.email.isEmpty()) {
                str += (" (" + attendee.email + ")");
            } else {
                str += (" (" + attendee.phoneNumber + ")");
            }
            attendeeStringList.add(str);
        }

        return attendeeStringList;
    }

    public Attendee getAttendeeByEmail(String email) {
        return attendeeDataSource.getAttendeeByEmail(email);
    }

    public static String getRandomId () {
        String str = "";
        for (int i = 0; i < randomIdLength; i++) {
            str += randomIdCharPool[random.nextInt(randomIdCharPool.length)];
        }
        return str;
    }

    public boolean saveUnregisteredAttendee(Attendee attendee) {
        attendee.firstName = (attendee.firstName == null ? "" : attendee.firstName);
        attendee.lastName = (attendee.lastName == null ? "" : attendee.lastName);
        attendee.userName = (attendee.userName == null || attendee.userName.isEmpty() ? attendee.email : attendee.userName);

        int responseCode = httpServerManager.createUser(
                attendee.email,
                attendee.firstName,
                attendee.email,
                attendee.lastName,
                null,
                null,
                attendee.userName,
                DEFAULT_USER_PASSWORD );

        switch (responseCode) {
            case HttpServerManager.STATUS_EXCEPTION: //cannot connect with server
                return false;
            case HttpServerManager.STATUS_ERROR: //user with email or phone already in server database
                JSONObject object = httpServerManager.getUserByEmail(attendee.email);
                try {
                    attendee.id = object.getString(HttpServerManager.UID);
                } catch (JSONException e) {
                    Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    return false;
                }
                break;
            default: //user created on http server; returns userId
                attendee.id = String.valueOf(responseCode);
                break;
        }

        saveUserToDB(attendee);
        return true;
    }

    public interface ConversationBroadcastListener {
        void onNewConversation(Conversation conversation, int index);
        void onNewConversationMessage(Message message, int missCount);
        void onNewConversationAttendees (String conversationId, List<String> newAttendeeIds);
        void onDropConversationAttendees (String conversationId, List<String> dropAttendeeIds);
        void onConversationMissCountReset(String conversationId);
    }

    public interface ChatAckListener {
        void onHandleChatAck (String id);
    }

    public interface ChatsMissCountListener {
        void onMissCountChanged (int allChatsMissCount);
    }
}
