package com.mono.network;

import android.content.Context;
import android.os.Bundle;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.mono.chat.GcmMessage;
import com.mono.model.Conversation;
import com.mono.model.Event;
import com.mono.util.Common;

import java.util.List;

public class ChatServerManager {

    public static final String ACTION = "action";
    public static final String SENDER_ID = "senderId";
    public static final String GCM_ID = "gcmId";

    private static ChatServerManager instance;
    private GoogleCloudMessaging gcm;
    private GcmMessage gcmMessage;

    private ChatServerManager(Context context) {
        gcm = GoogleCloudMessaging.getInstance(context);
        gcmMessage = GcmMessage.getInstance(context);
    }

    public static ChatServerManager getInstance (Context context) {
        if (instance == null) {
            instance = new ChatServerManager(context);
        }
        return instance;
    }

    public void sendRegister(long uId, String gcmToken) {
        Bundle bundle = GCMHelper.getRegisterPayload(uId + "", gcmToken);
        gcmMessage.sendMessage(bundle, gcm);
    }

    public void updateUserGcmId(long userId, String gcmToken) {
        Bundle bundle = GCMHelper.getUpdateGcmIdPayload(userId + "", gcmToken);
        gcmMessage.sendMessage(bundle, gcm);
    }

    public void startConversation(String creatorId, String conversationId, List<String> recipients) {
        String recipientsString = Common.implode(",", recipients);
        Bundle bundle = GCMHelper.getStartConversationPayload(creatorId, conversationId, recipientsString);
        gcmMessage.sendMessage(bundle, gcm);
    }

    public void startEventConversation (String creatorId, String eventId, List<String> recipients) {
//        Bundle bundle = GCMHelper.getStartEventConversationPayload(creatorId, convId, convTitle, Common.implode(",", recipients),
//                eventStartTime, eventEndTime, eventTitle, Common.implode(",", eventAttendees));
        Bundle bundle = GCMHelper.getStartEventConversationPayload(creatorId, eventId, Common.implode(",", recipients));
        gcmMessage.sendMessage(bundle, gcm);
    }

    public void addConversationAttendees(String senderId, String conversationId, List<String> userIds, List<String> recipientIds) {
        Bundle bundle = GCMHelper.getAddConversationAttendeesPayload(
                senderId, conversationId, Common.implode(",", userIds), Common.implode(",", recipientIds));
        gcmMessage.sendMessage(bundle, gcm);
    }

    public void dropConversationAttendees(String senderId, String conversationId, List<String> userIds, List<String> recipientIds) {
        Bundle bundle = GCMHelper.getDropConversationAttendeesPayload(
                senderId, conversationId, Common.implode(",", userIds), Common.implode(",", recipientIds));
        gcmMessage.sendMessage(bundle, gcm);
    }

    public void updateConversationTitle(String senderId, String conversationId, String newTitle, List<String> recipients) {
        Bundle bundle = GCMHelper.getUpdateConversationTitlePayload(senderId, conversationId, newTitle, Common.implode(",", recipients));
        gcmMessage.sendMessage(bundle, gcm);
    }

    public void sendConversationMessage(String senderId, String conversationId, List<String> recipients, String msg, String msgId, List<String> attachments) {
        Bundle bundle = GCMHelper.getConversationMessagePayload(senderId, conversationId, recipients, msg, msgId, attachments);
        gcmMessage.sendMessage(bundle, gcm);
    }
}
