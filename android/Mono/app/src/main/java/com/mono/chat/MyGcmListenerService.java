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
import com.mono.MainActivity;
import com.mono.R;
import com.mono.db.DatabaseHelper;
import com.mono.db.dao.ConversationDataSource;
import com.mono.model.Conversation;
import com.mono.model.Message;
import com.mono.network.GCMHelper;
import com.mono.network.HttpServerManager;
import com.mono.util.Common;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
    private HttpServerManager httpServerManager;
    private Handler handler;

    @Override
    public void onCreate() {
//        broadcaster = LocalBroadcastManager.getInstance(this);
        conversationManager = ConversationManager.getInstance(this);
        httpServerManager = new HttpServerManager(this);
        handler = new Handler();
    }

    public void onMessageReceived(String from, Bundle data) {
        //TODO: need to handle different actions
        String action = data.getString(GCMHelper.ACTION);
        if (action == null)
            return;

        switch (action) {
            case GCMHelper.ACTION_CONVERSATION_MESSAGE:
                processMessage(from, data);
                break;
            case GCMHelper.ACTION_START_CONVERSATION:
                addNewConversation(from, data);
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

    private void processMessage(String from, Bundle data) {
        final String message = data.getString(GCMHelper.MESSAGE);
        final String sender_id = data.getString(GCMHelper.SENDER_ID);
        final String conversation_id = data.getString(GCMHelper.CONVERSATION_ID);
        Log.d(TAG, "From: " + from);
        Log.d(TAG, "From user: " + sender_id);
        Log.d(TAG, "Message: " + message);
        Log.d(TAG, "Conversaiton_id: " + conversation_id);
        sendNotification(message);
        conversationManager.saveChatMessageToDB(new Message(sender_id, conversation_id, message, new Date().getTime()));
//        broadcastMessage(data);
        handler.post(new Runnable() {
            @Override
            public void run() {
                conversationManager.notifyListenersNewConversationMessage(conversation_id, sender_id, message);
            }
        });
    }

    private boolean addNewConversation(String from, Bundle data) {
        String conversationId = data.getString(GCMHelper.CONVERSATION_ID);
        if (conversationId == null || conversationId.isEmpty()) {
            Log.e(TAG, "Error: no conversationId");
            return false;
        }
        //get conversation details from server
        JSONObject conversationInfo = httpServerManager.getConversation(conversationId);
        String title = "";
        try {
            title = conversationInfo.getString(HttpServerManager.TITLE);
            String creatorId = conversationInfo.getLong(HttpServerManager.CREATOR_ID) + "";
            JSONArray attendeesArray = conversationInfo.getJSONArray(HttpServerManager.ATTENDEES_ID);
            ArrayList<String> attendeesList = new ArrayList<>();
            if (attendeesArray != null && attendeesArray.length() > 0) {
                for (int i=0; i<attendeesArray.length(); i++) {
                    attendeesList.add(attendeesArray.get(i).toString());
                }
            }

            ConversationDataSource conversationDataSource = DatabaseHelper.getDataSource(this, ConversationDataSource.class);
            if (!conversationDataSource.createConversation(conversationId, creatorId, title, attendeesList)) {
                Log.d(TAG, "Conversation with id: " + conversationId + " already exists");
            } else {
                sendNotification("Added new conversation with title: " + title + "; attendees: " + attendeesList);
                final Conversation conversation = conversationDataSource.getConversation(conversationId);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        conversationManager.notifyListenersNewConversation(conversation, 0);
                    }
                });
            }
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
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
        Log.d(TAG, "Fail to send Message: " + msgId );
        Log.d(TAG, "Error while sending: " + error);
    }

    private void dropConversationAttendees(String from, Bundle data) {
//        String senderId = data.getString(GCMHelper.SENDER_ID);
        String conversationId = data.getString(GCMHelper.CONVERSATION_ID);
        String[] userIds = Common.explode(",", data.getString(GCMHelper.USER_IDS));
        List<String> userList = Arrays.asList(userIds);
        ConversationDataSource conversationDataSource = DatabaseHelper.getDataSource(this, ConversationDataSource.class);
        String myId = AccountManager.getInstance(this).getAccount().id + "";
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
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
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

//    private void broadcastMessage(Bundle data) {
//        Intent intent = new Intent(GCM_INCOMING_INTENT);
//        if (data != null) {
//            intent.putExtra(GCM_MESSAGE_DATA, data);
//        }
//        broadcaster.sendBroadcast(intent);
//    }
}
