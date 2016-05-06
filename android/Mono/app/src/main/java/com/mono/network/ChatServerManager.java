package com.mono.network;

import android.content.Context;
import android.os.Bundle;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.mono.chat.GcmMessage;
import com.mono.util.Common;

import java.util.List;

public class ChatServerManager {

    public static final String ACTION = "action";
    public static final String SENDER_ID = "senderId";
    public static final String GCM_ID = "gcmId";

    private GoogleCloudMessaging gcm;
    private GcmMessage gcmMessage;
    private Context context;

    public ChatServerManager(Context context) {
        this.context = context;
        gcm = GoogleCloudMessaging.getInstance(context);
        gcmMessage = GcmMessage.getInstance(context);
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

    public void addConversationAttendees(String senderId, String conversationId, List<String> userIds, List<String> recipientIds) {
        Bundle bundle = GCMHelper.getAddConversationAttendeesPayload(
                senderId, conversationId, Common.implode(",", userIds), Common.implode(",", recipientIds));
        gcmMessage.sendMessage(bundle, gcm);
    }

    public void dropConversationAttendees(String senderId, String conversationId, String userIds, List<String> recipients) {
        Bundle bundle = GCMHelper.getDropConversationAttendeesPayload(senderId, conversationId, userIds, Common.implode(",", recipients));
        gcmMessage.sendMessage(bundle, gcm);
    }

    public void updateConversationTitle(String senderId, String conversationId, String newTitle, List<String> recipients) {
        Bundle bundle = GCMHelper.getUpdateConversationTitlePayload(senderId, conversationId, newTitle, Common.implode(",", recipients));
        gcmMessage.sendMessage(bundle, gcm);
    }

    public void sendConversationMessage(String senderId, String conversationId, List<String> recipients, String msg) {
        Bundle bundle = GCMHelper.getConversationMessagePayload(senderId, conversationId, recipients, msg);
        gcmMessage.sendMessage(bundle, gcm);
    }
}
