package com.mono.chat;


import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;
import java.util.List;
import java.util.Random;

/**
 * Created by xuejing on 2/28/16.
 */
public class GcmMessage {
    private static final String TAG = "GcmMessage";
    public static final String SENDER_ID = "sender_id";
    public static final String CONVERSATION_ID = "conversation_id";
    public static final String RECIPIENTS = "recipients";
    public static final String MESSAGE = "message";
    public static final String ACTION = "action";

    private static GcmMessage instance = null;
    private Context context;
    AsyncTask sendTask;
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
    public void sendMessage(String sender_id, String conversation_id, String message, String action, List<String> recipients, final GoogleCloudMessaging gcm) {
        Bundle data = generateMessageBody(sender_id, conversation_id, message, action, recipients);
        sendMessage(data, gcm);
    }
    public void sendMessage(final Bundle data, final GoogleCloudMessaging gcm ) {

        new AsyncTask<Void, Object, String>() {
            @Override
            protected String doInBackground(Void... params) {

                String id = "monoApp" + (System.currentTimeMillis()) + Long.toString(random.nextLong());

                try {
                    Log.d(TAG, "messageid: " + id);

                    gcm.send(SERVER_ID+ "@gcm.googleapis.com", id,
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
    private Bundle generateMessageBody (String sender_id, String conversation_id, String message, String action, List<String> recipients) {
        Bundle data = new Bundle();
        StringBuilder builder = new StringBuilder();
        for(String recip: recipients)
            builder.append(recip).append(",");

        data.putString(this.MESSAGE, message);
        data.putString(this.CONVERSATION_ID, conversation_id);
        data.putString(this.SENDER_ID, sender_id);
        data.putString(this.RECIPIENTS, builder.toString());
        data.putString(this.ACTION, action);

        return data;
    }
}

