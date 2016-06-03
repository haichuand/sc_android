package com.mono.dummy;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.mono.AccountManager;
import com.mono.R;
import com.mono.RequestCodes;
import com.mono.parser.SupercalyAlarmManager;
import com.mono.chat.GcmMessage;
import com.mono.dummy.KML.DownloadListener;
import com.mono.network.GCMHelper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

public class DummyFragment extends Fragment {

    private DummyActivity activity;
    private KML kml;
    private static final String TAG = "DummyFragmentDBTesting";
    private TextView text;
    private Button send_button;
    private Button db_test;
    private SupercalyAlarmManager alarmManager;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RequestCodes.Activity.DUMMY_WEB:
                if (resultCode == Activity.RESULT_OK) {
                }
                break;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof DummyActivity) {
            activity = (DummyActivity) context;
            kml = new KML(context, activity);
            alarmManager = SupercalyAlarmManager.getInstance(context);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_kml, container, false);

        text = (TextView) view.findViewById(R.id.text);
        send_button  = (Button) view.findViewById(R.id.button_send);
        db_test = (Button) view.findViewById(R.id.button_db);

        send_button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                sendRegistration(view);
            }
        });

        db_test.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                getDate(300);
            }
        });

        view.findViewById(R.id.download).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                //alarmManager.scheduleAlarm(3); // schedule an alarm for every 3-hour
                kml.getKML(getParentFragment(), RequestCodes.Activity.DUMMY_WEB,
                        new DownloadListener() {
                            @Override
                            public void onFinish(int status, Uri uri) {
                                if (uri != null) {
                                    //outputFile(uri.getPath());
                                }
                            }
                        }
                );
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        kml.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        kml.onPause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.dummy, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_login:
                activity.showWebActivity(null, RequestCodes.Activity.WEB);
                break;
            case R.id.action_logout:
                KML.clearCookies(getContext());
                break;
        }

        return false;
    }

    public void outputFile(String path) {
        try {
            StringBuilder builder = new StringBuilder(path + "\n\n");
            BufferedReader reader = new BufferedReader(new FileReader(path));
            String nextLine;

            while ((nextLine = reader.readLine()) != null) {
                builder.append(nextLine);
                builder.append('\n');
            }

            text.setText(builder.toString());

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendRegistration(View view) {
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this.getContext());
        GcmMessage gcmMessage = GcmMessage.getInstance(this.getContext());
        //sendRegistration(gcm, gcmMessage);
    }

    public void sendMessage(View view) {
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this.getContext());
        GcmMessage gcmMessage = GcmMessage.getInstance(this.getContext());
        //sendConversationMessage(gcm, gcmMessage);
    }

    public void sendRegistration(GoogleCloudMessaging gcm, GcmMessage gcmMessage) {
        String senderId = "210";
        Bundle registerBundle = GCMHelper.getRegisterPayload(senderId, AccountManager.getInstance(this.getContext()).getGCMToken());
        gcmMessage.sendMessage(registerBundle, gcm);
    }

    public void sendDropConversationAttendees(GoogleCloudMessaging gcm, GcmMessage gcmMessage) {
        String senderId = "210";
        String conversationId = "conversationId...";
        String recipients = "210";
        String userIds = "1,2";
        Bundle conversationDropBundle = GCMHelper.getDropConversationAttendeesPayload(senderId, conversationId, userIds, recipients);
        gcmMessage.sendMessage(conversationDropBundle, gcm);
    }

    public void sendConversationMessage(GoogleCloudMessaging gcm, GcmMessage gcmMessage) {
        String senderId = "210";
        String conversationId = "conversationId...";
        String recipients = "210";
        String msg = "I am sending message to myself";
        Bundle conversationMessageBundle = GCMHelper.getConversationMessagePayload(senderId, conversationId, new ArrayList<>(Arrays.asList("210")), msg);
        gcmMessage.sendMessage(conversationMessageBundle, gcm);
    }

    public void sendAddConversationAttendees(GoogleCloudMessaging gcm, GcmMessage gcmMessage) {
        String senderId = "210";
        String conversationId = "conversationId...";
        String recipients = "210";
        String userIds = "3,4";
        Bundle conversationAddBundle = GCMHelper.getAddConversationAttendeesPayload(senderId, conversationId, userIds, recipients);
        gcmMessage.sendMessage(conversationAddBundle, gcm);
    }

    public void sendUpdateConversation(GoogleCloudMessaging gcm, GcmMessage gcmMessage) {
        String senderId = "210";
        String conversationId = "conversationId...";
        String recipients = "210";
        String newTitle = "newTitle";
        Bundle conversationTitleBundle = GCMHelper.getUpdateConversationTitlePayload(senderId, conversationId, newTitle, recipients);
        gcmMessage.sendMessage(conversationTitleBundle, gcm);
    }



    private void testDB(View view) {
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this.getContext());
        GcmMessage gcmMessage = GcmMessage.getInstance(this.getContext());
        //sendUpdateConversation(gcm, gcmMessage);
        //sendAddConversationAttendees(gcm, gcmMessage);
        //sendDropConversationAttendees(gcm, gcmMessage);
    }

    private Calendar getDate(int dayBeforeToday) {
        long now = System.currentTimeMillis();
        Log.d(TAG, "timestamp of today " + now);
        long timestampLong = now - (long)dayBeforeToday*24*60*60*1000;
        Log.d(TAG, "300 days in millisecond " + (long)dayBeforeToday*24*60*60*1000);
        Log.d(TAG, "timestamp of 300 days before today " + timestampLong);
        Date d = new Date(timestampLong);
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c;
    }

}
