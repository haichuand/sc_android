package com.mono.chat;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;
import com.mono.MainActivity;
import com.mono.R;
import com.mono.model.Message;
import com.mono.network.GCMHelper;

import java.util.Date;

/**
 * Created by xuejing on 2/25/16.
 */
public class MyGcmListenerService extends GcmListenerService {
    public static final String GCM_INCOMING_INTENT = "com.mono.chat.MyGcmListenerService.INCOMING_INTENT";
    public static final String GCM_MESSAGE_DATA = "com.mono.chat.MyGcmListenerService.MESSAGE_DATA";

    private static final String TAG = "MyGcmListenerService";
    private LocalBroadcastManager broadcaster;
    private ConversationManager conversationManager;

    @Override
    public void onCreate() {
        broadcaster = LocalBroadcastManager.getInstance(this);
        conversationManager = ConversationManager.getInstance(this);
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
        String message = data.getString(GCMHelper.MESSAGE);
        String sender_id = data.getString(GCMHelper.SENDER_ID);
        String conversation_id = data.getString(GCMHelper.CONVERSATION_ID);
        Log.d(TAG, "From: " + from);
        Log.d(TAG, "From user: " + sender_id);
        Log.d(TAG, "Message: " + message);
        Log.d(TAG, "Conversaiton_id: " + conversation_id);
        sendNotification(message);
        conversationManager.saveChatMessageToDB(new Message(sender_id, conversation_id, message, new Date().getTime()));
        broadcastMessage(data);
    }

    public void onMessageSent(String msgId) {
        Log.d(TAG, "Message: " + msgId + " has been successfully sent");
    }

    public void onSendError(String msgId, String error) {
        Log.d(TAG, "Fail to send Message: " + msgId );
        Log.d(TAG, "Error while sending: " + error);
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

    private void broadcastMessage(Bundle data) {
        Intent intent = new Intent(GCM_INCOMING_INTENT);
        if (data != null) {
            intent.putExtra(GCM_MESSAGE_DATA, data);
        }
        broadcaster.sendBroadcast(intent);
    }
}
