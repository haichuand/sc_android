package com.mono.network;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.mono.AccountManager;
import com.mono.EventManager;
import com.mono.MainActivity;
import com.mono.R;
import com.mono.chat.ChatRoomActivity;
import com.mono.chat.ConversationManager;
import com.mono.db.DatabaseHelper;
import com.mono.db.dao.ConversationDataSource;
import com.mono.model.Account;
import com.mono.model.Attendee;
import com.mono.model.Conversation;
import com.mono.model.Event;
import com.mono.model.Media;
import com.mono.model.Message;
import com.mono.util.Colors;
import com.mono.util.Common;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This service is used to handle downstream messages received from FCM.
 *
 * @author Xuejing Dong, Haichuan Duan, Gary Ng
 */
public class MyFcmListenerService extends FirebaseMessagingService {

    private static final String TAG = MyFcmListenerService.class.getSimpleName();

    private ConversationManager conversationManager;
    private EventManager eventManager;
    private HttpServerManager httpServerManager;
    private ServerSyncManager serverSyncManager;
    private Handler handler;

    @Override
    public void onCreate() {
        conversationManager = ConversationManager.getInstance(this);
        eventManager = EventManager.getInstance(this);
        httpServerManager = HttpServerManager.getInstance(this);
        serverSyncManager = ServerSyncManager.getInstance(this);
        handler = new Handler();
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        String from = message.getFrom();
        Map<String, String> data = message.getData();

        String action = data.get(FCMHelper.ACTION);
        if (action == null)
            return;

        switch (action) {
            case FCMHelper.ACTION_CONVERSATION_MESSAGE:
                onNewConversationMessage(from, data);
                break;
            case FCMHelper.ACTION_START_EVENT_CONVERSATION:
                onNewEventConversation(from, data);
                break;
            case FCMHelper.ACTION_START_CONVERSATION:
                onNewConversation(from, data, true);
                break;
            case FCMHelper.ACTION_ADD_CONVERSATION_ATTENDEES:
                addConversationAttendees(from, data);
                break;
            case FCMHelper.ACTION_DROP_CONVERSATION_ATTENDEES:
                dropConversationAttendees(from, data);
                break;
        }
    }

    private void onNewConversationMessage(String from, Map<String, String> data) {
        String msgText = data.get(FCMHelper.MESSAGE);
        String senderId = data.get(FCMHelper.SENDER_ID);
        String conversationId = data.get(FCMHelper.CONVERSATION_ID);
        long timestamp = Long.valueOf(data.get(FCMHelper.TIMESTAMP));
        long messageId = Long.valueOf(data.get(FCMHelper.MESSAGE_ID));

        boolean isAckMessage = String.valueOf(AccountManager.getInstance(this).getAccount().id).equals(senderId);
        Attendee sender = conversationManager.getUserById(senderId);
        if (senderId == null || conversationId == null) {
            return;
        }
        final Message message = new Message(senderId, conversationId, msgText, timestamp);
        message.setMessageId(messageId);
        if (data.containsKey(FCMHelper.ATTACHMENTS)) {
            List<Media> attachments = new ArrayList<>();
            String[] items = Common.explode(",", data.get(FCMHelper.ATTACHMENTS));
            for (String item : items) {
                if (item.isEmpty()) {
                    continue;
                }
                String[] values = Common.explode(":", item);
                attachments.add(new Media(Uri.parse(values[1]), values[0], 0));
            }

            message.attachments = attachments;
        }

        int missCount = 0;
        if (isAckMessage) {
            if (!conversationManager.setConversationMessageAckAndTimestamp(messageId, true, timestamp)) {
                Log.e(TAG, "Error changing ACK for messageId: " + messageId);
            }
            serverSyncManager.handleAckConversationMessage(message);
        } else {
            if (conversationManager.saveChatMessageToDB(message) == -1) { //conversation does not exist
                Map<String, String> map = new HashMap<>();
                map.put(FCMHelper.CONVERSATION_ID, conversationId);
                onNewConversation(null, map, false);
                conversationManager.saveChatMessageToDB(message);
            }
            if (!conversationId.equals(conversationManager.getActiveConversationId())) {
                missCount = conversationManager.incrementConversationMissCount(conversationId);
                if (missCount == -1) {
                    return;
                }
            }
        }

        final int missCountFinal = missCount;
        handler.post(new Runnable() {
            @Override
            public void run() {
                conversationManager.notifyListenersNewConversationMessage(message, missCountFinal);
                conversationManager.notifyAllChatsListenersMissCount();
            }
        });
        if (!isAckMessage && !conversationId.equals(conversationManager.getActiveConversationId())) {
            sendChatNotification(sender.toString() + ": " + msgText, conversationId);
        }
    }

    private boolean onNewEventConversation(String from, Map<String, String> data) {
        String eventId = data.get(FCMHelper.EVENT_ID);
        if (eventId == null || eventId.isEmpty()) {
            Log.e(TAG, "Error: no eventId");
            return false;
        }

        JSONObject eventObj = httpServerManager.getEvent(eventId);
        if (eventObj == null) {
            Log.e(TAG, "Error: cannot get event from server");
            return false;
        }

        //find events with matching start, end times & title in local database
        long startTime;
        long endTime;
        String eventTitle;
        List<String> eventAttendeesId = new ArrayList<>();
        String conversationId;
        int eventCreatorId;
        try {
            startTime = eventObj.getLong(FCMHelper.START_TIME);
            endTime = eventObj.getLong(FCMHelper.END_TIME);
            eventTitle = eventObj.getString(FCMHelper.TITLE);
            conversationId = eventObj.getString(FCMHelper.CONVERSATION_ID);
            eventCreatorId = eventObj.getInt(FCMHelper.CREATOR_ID);
            JSONArray attendeesId = eventObj.getJSONArray(FCMHelper.ATTENDEES_ID);
            for (int i = 0; i < attendeesId.length(); i++) {
                eventAttendeesId.add(attendeesId.get(i).toString());
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        //check if message is self-sent ack
        if (((int) AccountManager.getInstance(this).getAccount().id) == eventCreatorId) {
            conversationManager.notifyListenersChatAck(eventId);
            serverSyncManager.handleAckEventConversation(eventId);
            return true;
        }

        //create event on local database
        List<Event> events = eventManager.getLocalEvents(startTime, endTime);
        if (events.isEmpty()) {  //create new event
            if (!createEvent(eventId, startTime, endTime, eventTitle, eventAttendeesId)) {
                return false;
            }
        } else {  //find matching event, if any
            Event localEvent = null;
            for (Event event : events) {
                if (event.startTime == startTime && event.endTime == endTime && eventTitle.equals(event.title)) {
                    localEvent = event;
                    break;
                }
            }
            if (localEvent == null) {
                if (!createEvent(eventId, startTime, endTime, eventTitle, eventAttendeesId)) {
                    return false;
                }
            } else { //update eventId
                eventManager.updateEventId(localEvent.id, eventId);
            }
        }

        //get conversation details from server
        JSONObject conversationObj = httpServerManager.getConversation(conversationId);
        String conversationTitle;
        String creatorId;
        String creatorName = "";
        List<String> attendeesIdList = new ArrayList<>();
        List<String> attendeesNameList = new ArrayList<>();
        try {
            conversationTitle = conversationObj.getString(HttpServerManager.TITLE);
            creatorId = conversationObj.getString(HttpServerManager.CREATOR_ID);
            JSONArray attendeesArray = conversationObj.getJSONArray(HttpServerManager.ATTENDEES);
            if (attendeesArray != null) {
                for (int i = 0; i < attendeesArray.length(); i++) {
                    JSONObject attendeeObj = (JSONObject) attendeesArray.get(i);
                    String id = String.valueOf(attendeeObj.getInt(HttpServerManager.UID));
                    // save attendee if not in local database
                    Attendee attendee = getAttendee(id);
                    if (attendee == null) {
                        return false;
                    }
                    attendeesIdList.add(id);
                    attendeesNameList.add(attendee.toString());
                    if (id.equals(creatorId)) {
                        creatorName = attendee.toString();
                    }
                }
            }
        } catch (JSONException e) {
            System.out.println(e.getMessage());
            return false;
        }

        //create conversation in local database
        conversationManager.createEventConversation(eventId, conversationId, conversationTitle, creatorId, attendeesIdList, false, 1);

        sendNotification(creatorName + " invited you to chat: " + conversationTitle + "\nAttendees: " + Common.implode(", ", attendeesNameList));

        final Conversation conversation = conversationManager.getConversation(conversationId, false, false);
        handler.post(new Runnable() {
            @Override
            public void run() {
                conversationManager.notifyListenersNewConversation(conversation, 0);
                conversationManager.notifyAllChatsListenersMissCount();
            }
        });

        return true;
    }

    private void onNewConversation(String from, Map<String, String> data, boolean checkAck) {
        String conversationId = data.get(FCMHelper.CONVERSATION_ID);
        if (conversationId == null) {
            return;
        }
        JSONObject object = httpServerManager.getConversation(conversationId);
        if (object == null) {
            return;
        }

        try {
            int creatorId = object.getInt(FCMHelper.CREATOR_ID);
            String title = object.getString(FCMHelper.TITLE);
            //check if message is self-sent ack
            if (checkAck && ((int) AccountManager.getInstance(this).getAccount().id) == creatorId) {
                conversationManager.notifyListenersChatAck(conversationId);
                serverSyncManager.handleAckConversation(conversationId);
                return;
            }

            List<String> attendeesId = new ArrayList<>();
            List<String> attendeesName = new ArrayList<>();
            String creatorName = "";
            JSONArray attendeesArray = object.getJSONArray(HttpServerManager.ATTENDEES);
            if (attendeesArray != null) {
                for (int i = 0; i < attendeesArray.length(); i++) {
                    JSONObject attendeeObj = (JSONObject) attendeesArray.get(i);
                    String id = String.valueOf(attendeeObj.getInt(HttpServerManager.UID));
                    // save attendee if not in local database
                    Attendee attendee = getAttendee(id);
                    if (attendee == null) {
                        return;
                    }
                    attendeesId.add(id);
                    attendeesName.add(attendee.toString());
                    if (id.equals(String.valueOf(creatorId))) {
                        creatorName = attendee.toString();
                    }
                }
            }
            //save conversation to local database
            if (!conversationManager.createConversation(
                    conversationId,
                    title,
                    String.valueOf(creatorId),
                    attendeesId,
                    false,
                    0
            )) {
                Toast.makeText(this, R.string.fcm_listener_error_save_new_chat, Toast.LENGTH_SHORT).show();
                return;
            }
            conversationManager.incrementConversationMissCount(conversationId);

            //do not send notification if conversation's creator is user self because it's for self-confirmation
            sendNotification(creatorName + " invited you to chat: " + title + "\nAttendees: " + Common.implode(", ", attendeesName));
            final Conversation conversation = conversationManager.getConversation(conversationId, false, false);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    conversationManager.notifyListenersNewConversation(conversation, 0);
                    conversationManager.notifyAllChatsListenersMissCount();
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void addConversationAttendees(String from, Map<String, String> data) {
        String senderId = data.get(FCMHelper.SENDER_ID);
        final String conversationId = data.get(FCMHelper.CONVERSATION_ID);
        String[] userIds = Common.explode(",", data.get(FCMHelper.USER_IDS));
        final List<String> newAttendeeIds = Arrays.asList(userIds);
        List<String> names = new ArrayList<>();
        for (String attendeeId : newAttendeeIds) {
            Attendee attendee = getAttendee(attendeeId);
            if (attendee == null) {
                return;
            }
            names.add(attendee.toString());
        }
        conversationManager.addAttendeesToConversation(conversationId, newAttendeeIds);
        if (!senderId.equals(String.valueOf(AccountManager.getInstance(this).getAccount().id))
                && !conversationId.equals(conversationManager.getActiveConversationId())) {
            Conversation conversation = conversationManager.getConversation(conversationId, false, false);
            sendNotification(getString(R.string.add_chat_attendee) + Common.implode(", ", names) + ". " +
                    getString(R.string.chat_title) + conversation.name);
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                conversationManager.notifyListenersNewConversationAttendees(conversationId, newAttendeeIds);
            }
        });
    }

    public void onMessageSent(String msgId) {
        Log.d(TAG, "Message: " + msgId + " has been successfully sent");
    }

    public void onSendError(String msgId, String error) {
        Log.d(TAG, "Fail to send Message: " + msgId);
        Log.d(TAG, "Error while sending: " + error);
    }

    private void dropConversationAttendees(String from, Map<String, String> data) {
        String senderId = data.get(FCMHelper.SENDER_ID);
        final String conversationId = data.get(FCMHelper.CONVERSATION_ID);
        String[] userIds = Common.explode(",", data.get(FCMHelper.USER_IDS));
        final List<String> userList = Arrays.asList(userIds);
        String myId = String.valueOf(AccountManager.getInstance(this).getAccount().id);
        Conversation conversation = conversationManager.getConversation(conversationId, true, false);
        List<String> names = new ArrayList<>();
        for (String id : userList) {
            names.add(conversationManager.getUserById(id).toString());
        }

        if (userList.contains(myId)) { //myself is dropped from conversation
            conversationManager.clearConversationAttendees(conversationId);
            sendNotification(getString(R.string.dropped_from_chat_title) + conversation.name);
        } else { //other users are dropped from conversation
            conversationManager.dropAttendees(conversationId, userList);
            if (!senderId.equals(myId)) {
                sendNotification(getString(R.string.drop_chat_attendee) + Common.implode(", ", names) + ". " +
                        getString(R.string.chat_title) + conversation.name);
            }
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                conversationManager.notifyListenersDropConversationAttendees(conversationId, userList);
            }
        });
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param message FCM message received.
     */
    private void sendNotification(String message) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("SuperCaly Message")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message))
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }

    private void sendChatNotification(String text, String conversationId) {
        Intent intent = new Intent(getApplicationContext(), ChatRoomActivity.class);
        intent.putExtra(ChatRoomActivity.CONVERSATION_ID, conversationId);
        Account account = AccountManager.getInstance(this).getAccount();
        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, conversationId.hashCode() /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("SuperCaly Message")
                .setContentText(text)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(conversationId.hashCode() /* ID of notification */, notificationBuilder.build());
    }

    private boolean createEvent(String eventId, long startTime, long endTime, String title, List<String> attendeeIds) {
        List<Attendee> attendees = new ArrayList<>();
        for (String id : attendeeIds) {
            Attendee attendee = getAttendee(id);
            if (attendee == null) {
                return false;
            }
            attendees.add(attendee);
        }
        boolean isAllDay = (startTime == endTime);

        Event event = new Event(eventId, 0, null, Event.TYPE_CALENDAR);
        event.title = title;
        event.color = Colors.getColor(this, R.color.green);
        event.startTime = startTime;
        event.endTime = endTime;
        event.allDay = isAllDay;
        event.attendees = attendees;

        String id = eventManager.createEvent(EventManager.EventAction.ACTOR_SELF, event, null);
        return id != null;
    }

    /**
     * If attendee is in local database, return the attendee; otherwise get attendee from http server
     * and save in local database
     * @param attendeeId
     * @return
     */
    private Attendee getAttendee (String attendeeId) {
        Attendee attendee = null;

        if (!conversationManager.hasUser(attendeeId)) {
            JSONObject obj = httpServerManager.getUserInfo(Integer.valueOf(attendeeId));
            if (obj != null) {
                try {
                    attendee = new Attendee(
                            attendeeId,
                            obj.getString(HttpServerManager.MEDIA_ID),
                            obj.getString(HttpServerManager.EMAIL),
                            obj.getString(HttpServerManager.PHONE_NUMBER),
                            obj.getString(HttpServerManager.FIRST_NAME),
                            obj.getString(HttpServerManager.LAST_NAME),
                            obj.getString(HttpServerManager.USER_NAME),
                            false,
                            true
                    );
                    conversationManager.saveUserToDB(attendee);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else {
            attendee = conversationManager.getUserById(attendeeId);
        }

        return attendee;
    }
}
