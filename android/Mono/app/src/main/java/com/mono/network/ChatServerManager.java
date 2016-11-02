package com.mono.network;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;
import com.mono.util.Common;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * This manager class is used to send FCM upstream messages.
 *
 * @author Haichuan Duan, Xuejing Dong, Gary Ng
 */
public class ChatServerManager {

    private static final String TAG = ChatServerManager.class.getSimpleName();

    private static final String SERVER_ID = "670096617047";

    private static ChatServerManager instance;

    private Context context;

    private AsyncTask sendTask;
    private Random random = new Random();

    private ChatServerManager(Context context) {
        this.context = context;
    }

    public static ChatServerManager getInstance (Context context) {
        if (instance == null) {
            instance = new ChatServerManager(context);
        }
        return instance;
    }

    public void sendMessage(Map<String, String> data) {
        sendTask = new AsyncTask<Object, Object, String>() {
            @Override
            protected String doInBackground(Object... params) {
                @SuppressWarnings("unchecked")
                Map<String, String> data = (Map<String, String>) params[0];

                String id = "SuperCalyMessage" + (System.currentTimeMillis()) +
                    Long.toString(random.nextLong());
                Log.d(TAG, "messageid: " + id);

                RemoteMessage.Builder builder =
                    new RemoteMessage.Builder(SERVER_ID + "@gcm.googleapis.com");
                builder.setMessageId(id);
                builder.addData("time_to_live", "0");

                for (Map.Entry<String, String> entry : data.entrySet()) {
                    builder.addData(entry.getKey(), entry.getValue());
                }

                FirebaseMessaging.getInstance().send(builder.build());

                return "Message ID: " + id + " Sent.";
            }

            @Override
            protected void onPostExecute(String result) {
                sendTask = null;
                Log.d(TAG, "onPostExecute: result: " + result);
            }
        }.execute(data);
    }

    public void sendRegister(long uId, String fcmToken) {
        Map<String, String> data = FCMHelper.getRegisterPayload(uId + "", fcmToken);
        sendMessage(data);
    }

    public void updateUserFcmId(long userId, String fcmToken) {
        Map<String, String> data = FCMHelper.getUpdateFcmIdPayload(userId + "", fcmToken);
        sendMessage(data);
    }

    public void startConversation(String creatorId, String conversationId,
            List<String> recipients) {
        Map<String, String> data = FCMHelper.getStartConversationPayload(creatorId, conversationId,
            Common.implode(",", recipients));
        sendMessage(data);
    }

    public void startEventConversation(String creatorId, String eventId, List<String> recipients) {
        Map<String, String> data = FCMHelper.getStartEventConversationPayload(creatorId, eventId,
            Common.implode(",", recipients));
        sendMessage(data);
    }

    public void addConversationAttendees(String senderId, String conversationId,
            List<String> userIds, List<String> recipientIds) {
        Map<String, String> data = FCMHelper.getAddConversationAttendeesPayload(senderId,
            conversationId, Common.implode(",", userIds), Common.implode(",", recipientIds));
        sendMessage(data);
    }

    public void dropConversationAttendees(String senderId, String conversationId,
            List<String> userIds, List<String> recipientIds) {
        Map<String, String> data = FCMHelper.getDropConversationAttendeesPayload(senderId,
            conversationId, Common.implode(",", userIds), Common.implode(",", recipientIds));
        sendMessage(data);
    }

    public void updateConversationTitle(String senderId, String conversationId, String newTitle,
            List<String> recipients) {
        Map<String, String> data = FCMHelper.getUpdateConversationTitlePayload(senderId,
            conversationId, newTitle, Common.implode(",", recipients));
        sendMessage(data);
    }

    public void sendConversationMessage(String senderId, String conversationId,
            List<String> recipients, String msg, String msgId, List<String> attachments) {
        Map<String, String> data = FCMHelper.getConversationMessagePayload(senderId,
            conversationId, recipients, msg, msgId, attachments);
        sendMessage(data);
    }
}
