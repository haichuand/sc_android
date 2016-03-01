package com.mono.chat;


import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import com.mono.R;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by xuejing on 2/28/16.
 */
public class MessageSender {
    private static final String TAG = "MessageSender";
    AsyncTask sendTask;
    private static final String SENDER_ID = "115711938538";

    public void sendMessage(final Bundle data, final GoogleCloudMessaging gcm ) {

        new AsyncTask<Void, Object, String>() {
            @Override
            protected String doInBackground(Void... params) {

                String id = "monoApp" + (System.currentTimeMillis());

                try {
                    Log.d(TAG, "messageid: " + id);

                    gcm.send(SENDER_ID+ "@gcm.googleapis.com", id,
                            data);
                    Log.d(TAG, "After gcm.send successful.");
                } catch (IOException e) {
                    Log.d(TAG, "Exception: " + e);
                    e.printStackTrace();
                }
                return "Message ID: "+id+ " Sent.";
            }

            @Override
            protected void onPostExecute(String result) {
                sendTask = null;
                Log.d(TAG, "onPostExecute: result: " + result);
            }
        }.execute(null,null,null);
    }

}

