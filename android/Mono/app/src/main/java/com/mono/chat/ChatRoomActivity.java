package com.mono.chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.mono.R;
import com.mono.model.Attendee;
import com.mono.model.Message;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatRoomActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    //constants used to send and receive bundles
    public static final String CONVERSATION_ID = "conversationId";
    public static final String EVENT_START_TIME = "eventStartTime";
    public static final String EVENT_END_TIME = "eventEndTime";
    public static final String EVENT_NAME = "eventName";
    public static final String EVENT_DATE = "eventDate";
    public static final String MY_ID = "myId";
    private static final String TAG = "ChatRoomActivity";

    private String conversationId;
    private String myId;
    private Toolbar toolbar;
    private DrawerLayout drawer;
    private ListView chatAttendeeListView;
    private RecyclerView chatView;
    private ChatRoomAdapter chatRoomAdapter;
    private TextView sendMessageText;

    private LinearLayoutManager chatLayoutManager;
    private ChatAttendeeMap chatAttendeeMap;
    SimpleAdapter chatAttendeeListAdapter;
    private List<Message> chatMessages;
    private List<Map<String, String>> chatAttendeeAdapterList;
    private List<String> chatAttendeeTokenList; //GCM token list for sending messages
//    private int newMessageIndex; //starting index of new messages that need to be saved to database in chatMessages
    private ConversationManager conversationManager;

    private GoogleCloudMessaging gcm;
    private GcmMessage gcmMessage;
    private BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent == null) {
            Log.e(TAG, "Error: intent is null");
            finish();
        }

        conversationId = intent.getStringExtra(CONVERSATION_ID);
        String eventStartTime = intent.getStringExtra(EVENT_START_TIME);
        String eventEndTime = intent.getStringExtra(EVENT_END_TIME);
        String eventName = intent.getStringExtra(EVENT_NAME);
        String eventDate = intent.getStringExtra(EVENT_DATE);
        myId = intent.getStringExtra(MY_ID);

        if (eventStartTime == null || eventEndTime == null || eventName == null || eventDate == null || myId == null || conversationId == null) {
            Log.e(TAG, "Error: intent parameters missing");
            finish();
        }

        setContentView(R.layout.activity_chat_room);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        View toolbarView = getLayoutInflater().inflate(R.layout.chat_tool_bar, null);
        ((TextView) toolbarView.findViewById(R.id.event_start_time)).setText(eventStartTime);
        ((TextView) toolbarView.findViewById(R.id.event_end_time)).setText(eventEndTime);
        ((TextView) toolbarView.findViewById(R.id.event_name)).setText(eventName);
        ((TextView) toolbarView.findViewById(R.id.event_date)).setText(eventDate);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
        chatView = (RecyclerView) findViewById(R.id.listMessages);
        chatAttendeeListView = (ListView) findViewById(R.id.chat_drawer_attendees);
        sendMessageText = (TextView) findViewById(R.id.sendMessageText);
        conversationManager = ConversationManager.getInstance(this);

        //set the RecyclerView for chat messages and its adapter
        chatLayoutManager = new LinearLayoutManager(this);
//        mchatLayoutManager.setReverseLayout(true);
        chatLayoutManager.setStackFromEnd(true); //always scroll to bottom
        chatView.setLayoutManager(chatLayoutManager);

        // TODO: get GCM tokens from server and put in chatAttendeeTokenList
        chatAttendeeTokenList = new ArrayList<>();
        setChatAttendeeTokenList(); //only used during development

        gcm = GoogleCloudMessaging.getInstance(this);
        gcmMessage = GcmMessage.getInstance(this);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle data = intent.getBundleExtra(MyGcmListenerService.GCM_MESSAGE_DATA);
                String conversation_id = data.getString(GcmMessage.CONVERSATION_ID);
                //only continue if conversationId matches
                if (!conversation_id.equals(conversationId))
                    return;
                String message = data.getString(GcmMessage.MESSAGE);
                String sender_id = data.getString(GcmMessage.SENDER_ID);
                //check if UI setup is complete first
                if (chatMessages == null || chatRoomAdapter == null || chatLayoutManager == null)
                    return;
                chatMessages.add(new Message(sender_id, conversationId, message, new Date().getTime()));
                chatRoomAdapter.notifyItemInserted(chatMessages.size() - 1);
                chatLayoutManager.scrollToPosition(chatMessages.size() - 1);
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(MyGcmListenerService.GCM_INCOMING_INTENT));
    }

    @Override
    protected  void onResume() {
        super.onResume();
        chatMessages = conversationManager.getChatMessages(conversationId);
        chatAttendeeMap = conversationManager.getChatAttendeeMap(conversationId);
        chatRoomAdapter = new ChatRoomAdapter(myId, chatAttendeeMap, chatMessages, this);
        chatView.setAdapter(chatRoomAdapter);
        chatLayoutManager.scrollToPosition(chatMessages.size() - 1);


        /* set up adapter for chat attendee list */
        HashMap<String, Attendee> newAttendeeMap = new HashMap<>(chatAttendeeMap.getChatAttendeeMap());
        chatAttendeeAdapterList = new ArrayList<>();
        HashMap<String, String> attendeeItem = new HashMap<>();
        attendeeItem.put("name", "Me");
        chatAttendeeAdapterList.add(attendeeItem); //put my name first in attendee list
        newAttendeeMap.remove(String.valueOf(myId));
        for (Map.Entry entry : newAttendeeMap.entrySet()) { //then put other attendees in the list
            attendeeItem = new HashMap<>();
            String name = ((Attendee) entry.getValue()).userName;
            attendeeItem.put("name", name);
            chatAttendeeAdapterList.add(attendeeItem);
        }
        String[] from = new String[] {"name"};  //maps name to name textView
        int[] to = new int[] {R.id.chat_attendee_name};
        chatAttendeeListAdapter = new SimpleAdapter(this, chatAttendeeAdapterList, R.layout.chat_attendee_list_item, from, to);
        chatAttendeeListView.setAdapter(chatAttendeeListAdapter);
//        chatAttendeeListAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onStop();
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        return false;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item == null)
            return false;

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.action_chat_group:
                if (drawer.isDrawerOpen(Gravity.RIGHT)) {
                    drawer.closeDrawer(Gravity.RIGHT);
                } else {
                    drawer.openDrawer(Gravity.RIGHT);
                }
                break;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat, menu);
        return true;
    }

    public void onSendButtonClicked(View view) {
        String msg = sendMessageText.getText().toString();
        if (msg.isEmpty())
            return;
        chatMessages.add(new Message(myId, conversationId, msg, new Date().getTime()));
        chatRoomAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatLayoutManager.scrollToPosition(chatMessages.size() - 1);
        sendMessageText.setText("");

        gcmMessage.sendMessage(myId, conversationId, msg, "MESSAGE", chatAttendeeTokenList, gcm);
        Message message = new Message(myId, conversationId, msg, new Date().getTime());
        conversationManager.saveChatMessageToDB(message);
    }

    private void setChatAttendeeTokenList() {
        if (!DemoChat.id1.equals(myId))
            chatAttendeeTokenList.add("c8DQ6fncZQs:APA91bFe1yhxv03tIcbLguOk9-irBFQrl2k3TF9h4rExNzFGsxVvjmcgpWiW5UPud21hkw62a1Z7BP-J60zGPk1-YA5halrjwK2wKG_eum3dhobczTTkptfZo8RjsbpvuvlVYej3cJPp");
        if (!DemoChat.id2.equals(myId))
            chatAttendeeTokenList.add("fhqG2CPmeoY:APA91bFjk6QkIsgixkVrXCo62VaqDARCP1Jg9JWSdxcQVi6qFdB2nr3bYqVCj3o-nMGDLowm1rSz6LnozRk9fXjtL7embsVVuCol_lilBnTnLsyIp1g8AhQQ0iFS-yvYiqd5Z1WZcsUf");
        if (!DemoChat.id3.equals(myId))
            chatAttendeeTokenList.add("dNK-zOFMQT4:APA91bE0sYrJSAtIi0fGdmqFf-f_p_WMS5kbCC1g8s3kHfujB7Y8hVWh3dYn_lsazAVPkxykAHzy8wcHXNTG3saDpbhKH6OGrMPpYt7Gzlb1Yk0dGHlcf-7h8B0iFWBLrmqG1HXeFM6G");
    }
}
