package com.mono.chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.mono.R;
import com.mono.db.DatabaseHelper;
import com.mono.db.dao.AttendeeDataSource;
import com.mono.model.Attendee;
import com.mono.model.Message;
import com.mono.network.GCMHelper;
import com.mono.util.Common;
import com.mono.util.GestureActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatRoomActivity extends GestureActivity {
    //constants used to send and receive bundles
    public static final String CONVERSATION_ID = "conversationId";
    public static final String EVENT_START_TIME = "eventStartTime";
    public static final String EVENT_END_TIME = "eventEndTime";
    public static final String EVENT_ID = "eventId";
    public static final String EVENT_NAME = "eventName";
    public static final String MY_ID = "myId";
    private static final String TAG = "ChatRoomActivity";

    private static final SimpleDateFormat DATE_FORMAT;
    private static final SimpleDateFormat TIME_FORMAT;

    private String eventName;
    private String eventId;
    private String conversationId;
    private String myId;
    private Toolbar toolbar;
    private DrawerLayout drawer;
    private ListView chatAttendeeListView;
    private RecyclerView chatView;
    private ChatRoomAdapter chatRoomAdapter;
    private TextView sendMessageText;
    private AutoCompleteTextView addAttendeeTextView;

    private LinearLayoutManager chatLayoutManager;
    private ChatAttendeeMap chatAttendeeMap = new ChatAttendeeMap();
    private SimpleAdapter chatAttendeeListAdapter;
    private List<Message> chatMessages;
    private List<Map<String, String>> chatAttendeeAdapterList;
//    private List<String> chatAttendeeTokenList = new ArrayList<>(); //GCM token list for sending messages
    private List<String> chatAttendeeIdList; //list of chat attendee ids for sending message
    private List<Attendee> allUsersList;
    private Attendee newlyAddedAttendee;
    private ConversationManager conversationManager;

    private GoogleCloudMessaging gcm;
    private GcmMessage gcmMessage;
    private BroadcastReceiver receiver;

    static {
        DATE_FORMAT = new SimpleDateFormat("M/d/yyyy", Locale.getDefault());
        TIME_FORMAT = new SimpleDateFormat("h:mm a", Locale.getDefault());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent == null) {
            Log.e(TAG, "Error: intent is null");
            finish();
        }

        eventId = intent.getStringExtra(EVENT_ID);
        eventName = intent.getStringExtra(EVENT_NAME);
        long eventStartTime = intent.getLongExtra(EVENT_START_TIME, 0);
        long eventEndTime = intent.getLongExtra(EVENT_END_TIME, 0);
        conversationId = intent.getStringExtra(CONVERSATION_ID);
        myId = intent.getStringExtra(MY_ID);

        if (eventStartTime == 0 || eventEndTime == 0 || myId == null) {
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
        setSupportActionBar(toolbar);

        if (toolbar != null) {
            View toolbarView = toolbar.findViewById(R.id.chat_toolbar);

            TextView title = (TextView) toolbarView.findViewById(R.id.title);
            title.setText(eventName);

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(eventStartTime);
            String date = DATE_FORMAT.format(calendar.getTime());
            String start = TIME_FORMAT.format(calendar.getTime());

            calendar.setTimeInMillis(eventEndTime);
            String end = TIME_FORMAT.format(calendar.getTime());

            TextView description = (TextView) toolbarView.findViewById(R.id.description);
            description.setText(String.format("%s from %s to %s", date, start, end));
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        chatView = (RecyclerView) findViewById(R.id.listMessages);
        chatAttendeeListView = (ListView) findViewById(R.id.chat_drawer_attendees);
        sendMessageText = (TextView) findViewById(R.id.sendMessageText);
        conversationManager = ConversationManager.getInstance(this);
        initialize();
    }

    private void initialize() {
        if (conversationId == null) {
            return;
        }

        //set up main chat main view
        chatLayoutManager = new LinearLayoutManager(this);
        chatView.setLayoutManager(chatLayoutManager);
        chatMessages = conversationManager.getChatMessages(conversationId);
        chatAttendeeMap = conversationManager.getChatAttendeeMap(conversationId);
        chatRoomAdapter = new ChatRoomAdapter(this, myId, chatAttendeeMap, chatMessages);
        chatView.setAdapter(chatRoomAdapter);

        chatAttendeeIdList = conversationManager.getChatAttendeeIdList(chatAttendeeMap, myId);

        /* set up adapter for chat attendee list */
        HashMap<String, Attendee> attendeeMapCopy = new HashMap<>(chatAttendeeMap.getAttendeeMap());
        chatAttendeeAdapterList = new ArrayList<>();
        HashMap<String, String> attendeeItem = new HashMap<>();
        attendeeItem.put("name", "Me");
        chatAttendeeAdapterList.add(attendeeItem); //put my name first in attendee list
        attendeeMapCopy.remove(String.valueOf(myId));
        for (Map.Entry entry : attendeeMapCopy.entrySet()) { //then put other attendees in the list
            attendeeItem = new HashMap<>();
            String name = ((Attendee) entry.getValue()).toString();
            attendeeItem.put("name", name);
            chatAttendeeAdapterList.add(attendeeItem);
        }
        String[] from = new String[] {"name"};  //maps name to name textView
        int[] to = new int[] {R.id.chat_attendee_name};
        chatAttendeeListAdapter = new SimpleAdapter(this, chatAttendeeAdapterList, R.layout.chat_attendee_list_item, from, to);
        chatAttendeeListView.setAdapter(chatAttendeeListAdapter);

        //set up auto complete text view for inviting friends
        addAttendeeTextView = (AutoCompleteTextView) findViewById(R.id.edit_text_invite);
        addAttendeeTextView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    addAttendeeTextView.showDropDown();
                }
            }
        });
        addAttendeeTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                newlyAddedAttendee = (Attendee) adapterView.getItemAtPosition(i);
            }
        });

        AttendeeDataSource attendeeDataSource = DatabaseHelper.getDataSource(this, AttendeeDataSource.class);
        allUsersList = attendeeDataSource.getAttendees();
        Collections.sort(allUsersList, new AttendeeUsernameComparator());
        ArrayAdapter<Attendee> addAttendeeAdapter = new ArrayAdapter<Attendee>(this, android.R.layout.simple_dropdown_item_1line, allUsersList);
        addAttendeeTextView.setAdapter(addAttendeeAdapter);

        gcm = GoogleCloudMessaging.getInstance(this);
        gcmMessage = GcmMessage.getInstance(this);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle data = intent.getBundleExtra(MyGcmListenerService.GCM_MESSAGE_DATA);
                String conversation_id = data.getString(GCMHelper.CONVERSATION_ID);
                //only continue if conversationId matches
//                if (conversation_id==null || !conversation_id.equals(conversationId))
//                    return;
                String message = data.getString(GCMHelper.MESSAGE);
                String sender_id = data.getString(GCMHelper.SENDER_ID);

                if (Common.compareStrings(sender_id, myId)) {
                    // Sent Confirmation
                } else {
                    chatMessages.add(new Message(sender_id, conversationId, message, new Date().getTime()));
                    chatRoomAdapter.notifyItemInserted(chatMessages.size() - 1);
                }
            }
        };

        final LinearLayout messagesLayout = (LinearLayout) findViewById(R.id.all_messages_linearlayout);
        if (messagesLayout != null) {
            messagesLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    int size = chatMessages.size();
                    if (chatLayoutManager.findLastCompletelyVisibleItemPosition() < size-1) {
                        chatLayoutManager.scrollToPosition(size - 1);
                    }
                }
            });
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(MyGcmListenerService.GCM_INCOMING_INTENT));
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onStop();
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
                if (drawer.isDrawerOpen(GravityCompat.END)) {
                    drawer.closeDrawer(GravityCompat.END);
                } else {
                    drawer.openDrawer(GravityCompat.END);
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
        // Create New Conversation Upon 1st Message
//        if (conversationId == null) {
//            conversationId = ConversationManager.getInstance(this).createConversation(eventName, eventId);
//        }

        String msg = sendMessageText.getText().toString();
        if (msg.isEmpty())
            return;
        chatMessages.add(new Message(myId, conversationId, msg, new Date().getTime()));
        chatRoomAdapter.notifyItemInserted(chatMessages.size() - 1);
        sendMessageText.setText("");

        gcmMessage.sendMessage(GCMHelper.getConversationMessagePayload(myId, conversationId, chatAttendeeIdList, msg), gcm);
        Message message = new Message(myId, conversationId, msg, new Date().getTime());
        conversationManager.saveChatMessageToDB(message);
    }

    public void onAddAttendeeButtonClicked(View view) {
        if (newlyAddedAttendee == null) {
            return;
        }
        //do not add duplicate attendee
        for (Attendee attendee : chatAttendeeMap.toAttendeeList()) {
            if (attendee.email.equals(newlyAddedAttendee.email))
                return;
        }

        conversationManager.addAttendee(conversationId, newlyAddedAttendee.id);
        chatAttendeeMap.addAttendee(newlyAddedAttendee);
        Map<String, String> nameMap = new HashMap<>();
        nameMap.put("name", newlyAddedAttendee.toString());
        chatAttendeeAdapterList.add(nameMap);
        chatAttendeeListAdapter.notifyDataSetChanged();
        chatAttendeeIdList.add(newlyAddedAttendee.id);

        newlyAddedAttendee = null;
        addAttendeeTextView.setText("");
    }

    class AttendeeUsernameComparator implements Comparator<Attendee> {

        @Override
        public int compare(Attendee attendee1, Attendee attendee2) {
            return attendee1.toString().compareToIgnoreCase(attendee2.toString());
        }
    }
}
