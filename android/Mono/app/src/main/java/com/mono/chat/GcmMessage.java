package com.mono.chat;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;
import java.util.Random;

/**
 * Created by xuejing on 2/28/16.
 */
public class GcmMessage {

    private static final String TAG = "GcmMessage";

    private static GcmMessage instance;
    private Context context;
    private AsyncTask sendTask;
    private static final String SERVER_ID = "115711938538";
    private Random random = new Random();

    private GcmMessage(Context context) {
        this.context = context;
    }

    public static GcmMessage getInstance(Context context) {
        if (instance == null) {
            instance = new GcmMessage(context);
        }

        return instance;
    }

    public void sendMessage(final Bundle data, final GoogleCloudMessaging gcm) {
        sendTask = new AsyncTask<Void, Object, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String id = "monoApp" + (System.currentTimeMillis()) + Long.toString(random.nextLong());
                Log.d(TAG, "messageid: " + id);

                try {
                    gcm.send(SERVER_ID + "@gcm.googleapis.com", id, data);
                    Log.d(TAG, "After gcm.send successful.");
                } catch (IOException e) {
                    Log.d(TAG, "Exception: " + e);
                    e.printStackTrace();
                }

                return "Message ID: " + id + " Sent.";
            }

            @Override
            protected void onPostExecute(String result) {
                sendTask = null;
                Log.d(TAG, "onPostExecute: result: " + result);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
