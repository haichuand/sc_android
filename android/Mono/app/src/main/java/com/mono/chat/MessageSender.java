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
    AtomicInteger ccsMsgId = new AtomicInteger();

    public void sendMessage(final Bundle data, final GoogleCloudMessaging gcm ) {

        sendTask = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {

                String id = "m" + Integer.toString(ccsMsgId.incrementAndGet());

                try {
                    Log.d(TAG, "messageid: " + id);
                    gcm.send(R.string.gcm_defaultSenderId + "@gcm.googleapis.com", id,
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

        };
        sendTask.execute(null, null, null);
    }

}

