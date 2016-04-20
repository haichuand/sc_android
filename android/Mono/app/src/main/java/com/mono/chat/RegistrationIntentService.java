package com.mono.chat;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.mono.AccountManager;
import com.mono.R;
import com.mono.SuperCalyPreferences;

/**
 * Created by xuejing on 2/25/16.
 */
public class RegistrationIntentService extends IntentService {
    private static final String TAG = "RegistIntentService";

    public RegistrationIntentService() {
        super(TAG);

    }

    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onHandleIntent(Intent intent) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        try {
            InstanceID instance = InstanceID.getInstance(this);
            String token = instance.getToken(getString(R.string.gcm_defaultSenderId),
                GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);

            Log.i(TAG, "GCM Registration Token: " + token);

            // TODO: Implement sendRegistrationToServer(token) to send any registration to your app's servers.
            sendRegistrationToServer(token);

            sharedPreferences.edit().putBoolean(SuperCalyPreferences.SENT_TOKEN_TO_SERVER, true).apply();
        }catch (Exception e) {
            Log.d(TAG, "Failed to complete token refresh", e);
            // If an exception happens while fetching the new token or updating our registration data
            // on a third-party server, this ensures that we'll attempt the update at a later time.     
            sharedPreferences.edit().putBoolean(SuperCalyPreferences.SENT_TOKEN_TO_SERVER, false).apply();
        }

        intent = new Intent(SuperCalyPreferences.REGISTRATION_COMPLETE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendRegistrationToServer(String token) {
        AccountManager.getInstance(getApplicationContext()).setGCMToken(token);
        //TODO: send token to server and chatserver
    }
}
