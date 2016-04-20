package com.mono.network;

import com.google.android.gms.gcm.GoogleCloudMessaging;

public class ChatServerManager {

    private static GoogleCloudMessaging gcm;

    private ChatServerManager() {}

    static {
        gcm = new GoogleCloudMessaging();
    }

    public static void sendRegister(String uId, String gcmToken) {

    }

    public static void sendLogin(String uId, String gcmToken) {

    }
}
