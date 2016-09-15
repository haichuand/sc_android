package com.mono.chat;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;
import com.mono.AccountManager;
import com.mono.EventManager;
import com.mono.MainActivity;
import com.mono.R;
import com.mono.db.DatabaseHelper;
import com.mono.db.dao.AttendeeDataSource;
import com.mono.db.dao.ConversationDataSource;
import com.mono.db.dao.EventDataSource;
import com.mono.model.Account;
import com.mono.model.Attendee;
import com.mono.model.Conversation;
import com.mono.model.Event;
import com.mono.model.Message;
import com.mono.network.GCMHelper;
import com.mono.network.HttpServerManager;
import com.mono.util.Common;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by xuejing on 2/25/16.
 */
public class MyGcmListenerService extends GcmListenerService {
    public static final String GCM_INCOMING_INTENT = "com.mono.chat.MyGcmListenerService.INCOMING_INTENT";
    public static final String GCM_MESSAGE_DATA = "com.mono.chat.MyGcmListenerService.MESSAGE_DATA";

    private static final String TAG = "MyGcmListenerService";
    //    private LocalBroadcastManager broadcaster;
    private ConversationManager conversationManager;
    private EventManager eventManager;
    private HttpServerManager httpServerManager;
    private Handler handler;

    @Override
    public void onCreate() {
//        broadcaster = LocalBroadcastManager.getInstance(this);
        conversationManager = ConversationManager.getInstance(this);
        eventManager = EventManager.getInstance(this);
        httpServerManager = new HttpServerManager(this);
        handler = new Handler();
    }

    @Override
    public void onMessageReceived(String from, Bundle data) {
        String action = data.getString(GCMHelper.ACTION);
        if (action == null)
            return;

        switch (action) {
            case GCMHelper.ACTION_CONVERSATION_MESSAGE:
                onNewConversationMessage(from, data);
                break;
            case GCMHelper.ACTION_START_EVENT_CONVERSATION:
                onNewEventConversation(from, data);
                break;
            case GCMHelper.ACTION_ADD_CONVERSATION_ATTENDEES:
                addConversationAttendees(from, data);
                break;
            case GCMHelper.ACTION_DROP_CONVERSATION_ATTENDEES:
                dropConversationAttendees(from, data);
                break;
        }


        // [START_EXCLUDE]
        /**
         * applications would process the message here.
         * Eg: - Syncing with server.
         *     - Store message in local database.
         *     - Update UI.
         */

        /**
         * In some cases it may be useful to show a notification indicating to the user
         * that a message was received.
         */

        // [END_EXCLUDE]
    }

    private void onNewConversationMessage(String from, Bundle data) {
        final String msgText = data.getString(GCMHelper.MESSAGE);
        final String senderId = data.getString(GCMHelper.SENDER_ID);
        final String conversationId = data.getString(GCMHelper.CONVERSATION_ID);
        final String messageId = data.getString(GCMHelper.MESSAGE_ID);
        Attendee sender = conversationManager.getAttendeeById(senderId);
        if (senderId == null || conversationId == null) {
            return;
        }
        final Message message = new Message(senderId, conversationId, msgText, System.currentTimeMillis(), messageId);
        conversationManager.saveChatMessageToDB(message);
        handler.post(new Runnable() {
            @Override
            public void run() {
                conversationManager.notifyListenersNewConversationMessage(message);
            }
        });
        if (!String.valueOf(AccountManager.getInstance(this).getAccount().id).equals(senderId) && !conversationId.equals(conversationManager.getActiveConversationId())) {
            sendChatNotification(sender.toString() + ": " + msgText, conversationId);
        }
    }

    private boolean onNewEventConversation(String from, Bundle data) {
        String eventId = data.getString(GCMHelper.EVENT_ID);
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
            startTime = eventObj.getLong(GCMHelper.START_TIME);
            endTime = eventObj.getLong(GCMHelper.END_TIME);
            eventTitle = eventObj.getString(GCMHelper.TITLE);
            conversationId = eventObj.getString(GCMHelper.CONVERSATION_ID);
            eventCreatorId = eventObj.getInt(GCMHelper.CREATOR_ID);
            JSONArray attendeesId = eventObj.getJSONArray(GCMHelper.ATTENDEES_ID);
            for (int i = 0; i < attendeesId.length(); i++) {
                eventAttendeesId.add(attendeesId.get(i).toString());
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

//        if (AccountManager.getInstance(this).getAccount().id != eventCreatorId) { //skip if it's user's self-confirmation
            List<Event> events = eventManager.getEvents(startTime, endTime);
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
                } else { //copy event to local database if it's a calendar provider event
                    if (localEvent.source == Event.SOURCE_PROVIDER) {
                        if (!eventManager.saveEventToDatabase(localEvent, eventId)) {
                            return false;
                        }
                    }
                }
            }
//        }

        //get conversation details from server
        JSONObject conversationObj = httpServerManager.getConversation(conversationId);
        String conversationTitle;
        String creatorId;
        List<String> attendeesList = new ArrayList<>();
        try {
            conversationTitle = conversationObj.getString(HttpServerManager.TITLE);
            creatorId = conversationObj.getString(HttpServerManager.CREATOR_ID);
            JSONArray attendeesArray = conversationObj.getJSONArray(HttpServerManager.ATTENDEES_ID);
            if (attendeesArray != null) {
                for (int i = 0; i < attendeesArray.length(); i++) {
                    attendeesList.add(attendeesArray.get(i).toString());
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        //create conversation in local database, even for creator's self-confirmation
        ConversationDataSource conversationDataSource = DatabaseHelper.getDataSource(this, ConversationDataSource.class);
        conversationDataSource.createEventConversation(eventId, conversationId, conversationTitle, creatorId, attendeesList);
        //do not send notification if conversation's creator is user self because it's for self-confirmation
        String myId = String.valueOf(AccountManager.getInstance(this).getAccount().id);
        if (!creatorId.equals(myId)) {
            sendNotification("Added new conversation with title: " + conversationTitle + "; attendees: " + attendeesList);
        }
        final Conversation conversation = conversationDataSource.getConversation(conversationId);
        handler.post(new Runnable() {
            @Override
            public void run() {
                conversationManager.notifyListenersNewConversation(conversation, 0);
            }
        });
        return true;
    }

    private void addConversationAttendees(String from, Bundle data) {
        String senderId = data.getString(GCMHelper.SENDER_ID);
        final String conversationId = data.getString(GCMHelper.CONVERSATION_ID);
        String[] userIds = Common.explode(",", data.getString(GCMHelper.USER_IDS));
        ConversationDataSource conversationDataSource = DatabaseHelper.getDataSource(this, ConversationDataSource.class);
        final List<String> newAttendeeIds = Arrays.asList(userIds);
        conversationDataSource.addAttendeesToConversation(conversationId, newAttendeeIds);
        sendNotification("Added new users to conversation with id: " + conversationId + "; new users: " + Arrays.asList(userIds));
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

    private void dropConversationAttendees(String from, Bundle data) {
//        String senderId = data.getString(GCMHelper.SENDER_ID);
        String conversationId = data.getString(GCMHelper.CONVERSATION_ID);
        String[] userIds = Common.explode(",", data.getString(GCMHelper.USER_IDS));
        List<String> userList = Arrays.asList(userIds);
        ConversationDataSource conversationDataSource = DatabaseHelper.getDataSource(this, ConversationDataSource.class);
        String myId = String.valueOf(AccountManager.getInstance(this).getAccount().id);
        if (userList.contains(myId)) { //myself is dropped from conversation
            conversationDataSource.clearConversationAttendees(conversationId);
            sendNotification("Dropped from conversation with id: " + conversationId);
        } else { //other users are dropped from conversation
            conversationDataSource.dropAttendeesFromConversation(conversationId, userList);
            sendNotification("Removed users " + userList + " from conversation: " + conversationId);
        }
    }

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param message GCM message received.
     */
    private void sendNotification(String message) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("GCM Message")
                .setContentText(message)
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
        intent.putExtra(ChatRoomActivity.MY_ID, account.id + "");
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
        AttendeeDataSource attendeeDataSource = DatabaseHelper.getDataSource(this, AttendeeDataSource.class);
        for (String id : attendeeIds) {
            Attendee attendee = attendeeDataSource.getAttendeeById(id);
            if (attendee == null) {
                attendee = getAttendeeFromServer(id);
                if (attendee == null) {
                    return false;
                }
            }
            attendees.add(attendee);
        }
        String id = eventManager.createEvent(EventManager.EventAction.ACTOR_SELF, 0, 0, null, Event.TYPE_CALENDAR,
                title, null, null, R.color.green, startTime, endTime, null, null, false, attendees, null, null);
        return eventManager.updateEventId(id, eventId);
    }

    private Attendee getAttendeeFromServer(String attendeeId) {
        Attendee attendee = null;
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
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return attendee;
    }
}
