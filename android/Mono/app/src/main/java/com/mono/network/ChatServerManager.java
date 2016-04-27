package com.mono.network;

import android.content.Context;
import android.os.Bundle;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.mono.chat.GcmMessage;

public class ChatServerManager {

    public static final String ACTION = "action";
    public static final String SENDER_ID = "senderId";
    public static final String GCM_ID = "gcmId";

    private GoogleCloudMessaging gcm;
    private Context context;

    public ChatServerManager(Context context) {
        this.context = context;
        gcm = GoogleCloudMessaging.getInstance(context);
    }

    public void sendRegister(long uId, String gcmToken) {
        Bundle bundle = GCMHelper.getRegisterPayload(uId + "", gcmToken);
        GcmMessage.getInstance(context).sendMessage(bundle, gcm);
    }

    public void updateUserGcmId(long userId, String gcmToken) {
        Bundle bundle = GCMHelper.getUpdateGcmIdPayload(userId + "", gcmToken);
        GcmMessage.getInstance(context).sendMessage(bundle, gcm);
    }
}
