package com.mono.chat;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
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

    private String mConversationId;
    private String myId;
    private String mEventStartTime = "";
    private String mEventEndTime = "";
    private String mEventName = "";
    private String mEventDate = "";
    private Toolbar mToolbar;
    private DrawerLayout mDrawer;
    private ListView mChatAttendeeListView;
    private NavigationView mNavView;
    private RecyclerView mChatView;
    private ChatRoomAdapter mChatRoomAdapter;
    private TextView mSendMessageText;

    private LinearLayoutManager mChatLayoutManager;
    private ChatAttendeeMap mChatAttendeeMap;
    private List<Message> mChatMessages;
    private List<Map<String, String>> chatAttendeeAdapterList;
    private List<String> chatAttendeeTokenList; //GCM token list for sending messages
    private int newMessageIndex; //starting index of new messages that need to be saved to database in mChatMessages

    private Date date;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent == null)
            finish();

        mConversationId = intent.getStringExtra(CONVERSATION_ID);
        mEventStartTime = intent.getStringExtra(EVENT_START_TIME);
        mEventEndTime = intent.getStringExtra(EVENT_END_TIME);
        mEventName = intent.getStringExtra(EVENT_NAME);
        mEventDate = intent.getStringExtra(EVENT_DATE);
        myId = intent.getStringExtra(MY_ID);

        if (mEventStartTime == null || mEventEndTime == null || mEventName == null || mEventDate == null || myId == null || mConversationId == null) {
            Log.e(TAG, "Error: intent  parameters missing");
            finish();
        }

//        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_chat_room);

        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawer, mToolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawer.addDrawerListener(toggle);
        toggle.syncState();

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        View toolbarView = getLayoutInflater().inflate(R.layout.chat_tool_bar, null);
        ((TextView) toolbarView.findViewById(R.id.event_start_time)).setText(mEventStartTime);
        ((TextView) toolbarView.findViewById(R.id.event_end_time)).setText(mEventEndTime);
        ((TextView) toolbarView.findViewById(R.id.event_name)).setText(mEventName);
        ((TextView) toolbarView.findViewById(R.id.event_date)).setText(mEventDate);
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
        mChatView = (RecyclerView) findViewById(R.id.listMessages);
        mChatAttendeeMap = new ChatAttendeeMap();
        mChatMessages = new ArrayList<>();
        mChatAttendeeListView = (ListView) findViewById(R.id.chat_drawer_attendees);
        mSendMessageText = (TextView) findViewById(R.id.sendMessageText);

        setTestChatAttendees();
        setTestChatMessages();
        /* set up adapter for chat attendee list */
        HashMap<String, Attendee> newAttendeeMap = new HashMap<>(mChatAttendeeMap.getChatAttendeeMap());
        chatAttendeeAdapterList = new ArrayList<>();
        HashMap<String, String> attendeeItem = new HashMap<>();
        attendeeItem.put("name", "Me");
        chatAttendeeAdapterList.add(attendeeItem); //put my name first in attendee list
        newAttendeeMap.remove(String.valueOf(myId));
        for (Map.Entry entry : newAttendeeMap.entrySet()) { //then put other attendees in the list
            attendeeItem = new HashMap<>();
            String name = ((Attendee) entry.getValue()).name;
            attendeeItem.put("name", name);
            chatAttendeeAdapterList.add(attendeeItem);
        }
        String[] from = new String[] {"name"};  //maps name to name textView
        int[] to = new int[] {R.id.chat_attendee_name};
        SimpleAdapter chatAttendeeListAdapter = new SimpleAdapter(this, chatAttendeeAdapterList, R.layout.chat_attendee_list_item, from, to);
        mChatAttendeeListView.setAdapter(chatAttendeeListAdapter);

        //set the RecyclerView for chat messages and its adapter
        mChatLayoutManager = new LinearLayoutManager(this);
//        mchatLayoutManager.setReverseLayout(true);
        mChatLayoutManager.setStackFromEnd(true); //always scroll to bottom
        mChatView.setLayoutManager(mChatLayoutManager);
        mChatRoomAdapter = new ChatRoomAdapter(myId, mChatAttendeeMap, mChatMessages, this);
        mChatView.setAdapter(mChatRoomAdapter);
        newMessageIndex = mChatMessages.size();
        mChatLayoutManager.scrollToPosition(newMessageIndex-1);
        date = new Date();

        // TODO: get GCM tokens from server and put in chatAttendeeTokenList
        chatAttendeeTokenList = new ArrayList<>();
        setChatAttendeeTokenList();
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        return false;
    }

    private void setTestChatAttendees() {
        Attendee attendee = new Attendee(2L, "Jason", "jason@email.com");
        mChatAttendeeMap.addAttendee(attendee);
        attendee = new Attendee(4L, "Margret Johnson", "mjohnson@kmail.com");
        mChatAttendeeMap.addAttendee(attendee);
        attendee = new Attendee(5L, "Kathy", "ka234@abco.org");
        mChatAttendeeMap.addAttendee(attendee);
        attendee = new Attendee(6L, "Henry McLeland", "hmc@kdaid.com");
        mChatAttendeeMap.addAttendee(attendee);
    }

    private void setTestChatMessages() {
        mChatMessages.add(new Message(String.valueOf(2), mConversationId, "Hi!", new Date().getTime()));
        mChatMessages.add(new Message(String.valueOf(2), mConversationId, "Are we meeting here?", new Date().getTime()));
        mChatMessages.add(new Message(String.valueOf(4), mConversationId, "I thought it's Rm 462", new Date().getTime()));
        mChatMessages.add(new Message(String.valueOf(5), mConversationId, "Really?", new Date().getTime()));
        mChatMessages.add(new Message(String.valueOf(2), mConversationId, "Hmm... Let me check with Jane.", new Date().getTime()));
        mChatMessages.add(new Message(String.valueOf(2), mConversationId, "Oh Margret you're right, it's room 462. Bad memory on my part!", new Date().getTime()));
        mChatMessages.add(new Message(String.valueOf(6), mConversationId, "We gotta notify others.", new Date().getTime()));
        mChatMessages.add(new Message(String.valueOf(5), mConversationId, "We gotta notify others.", new Date().getTime()));
        mChatMessages.add(new Message(String.valueOf(2), mConversationId, "Oh Jason you're right, it's room 462. Bad memory on my part!", new Date().getTime()));
        mChatMessages.add(new Message(String.valueOf(4), mConversationId, "Too many type sizes and styles at once can wreck any layout. A typographic scale has a limited set of type sizes that work well together along with the layout grid.", new Date().getTime()));
        mChatMessages.add(new Message(String.valueOf(2), mConversationId, "Instances of this class are suitable for comparison, but little else. Use DateFormat to format a Date for display to a human.", new Date().getTime()));
        mChatMessages.add(new Message(String.valueOf(6), mConversationId, "Note that, surprisingly, instances of this class are mutable.", new Date().getTime()));
    }

    private void setChatAttendeeTokenList() {
        chatAttendeeTokenList.add("c2LxofJLjHk:APA91bHhfKaGP0rfkSzGoBQIqG_kIDCkjUFigRYf0GS4z-rNDOhO_Yf0ERJDlqWSSEVBrZoxgD395YLMXuLNGQHXvX5WwIG0TkKoldbo0cEnc8S-lwOZkwUtT4SpKDeTONyRhMMJmOHc");
        chatAttendeeTokenList.add("ftyG76-d9Yg:APA91bFJDJshrlGV-qFPjpdOtqNdusEmIrfR4I65f6us4biPlBCi2LiaC67cuteojw_lCSYsEuezVevOBYw3WwK-AI93Fm2iQik1JNS7lxt1xKUKB_PfFbgrXFMFYNA1QUvrAQ15wMm8");
        chatAttendeeTokenList.add("cqGKPB-73Ps:APA91bFQEhedJ1_KGwIBWMJFYAMMZAVkwIw8iT5FoJiuXaqij1XWglYTKtGqhhntk1snoukzLMvJQL9-s7GZP4w_j05u55IpyYgSaNnXZe6bSKBlt1iQDg_OkMbCyA-Z3r8jvEqaDYDm");
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
                if (mDrawer.isDrawerOpen(Gravity.RIGHT)) {
                    mDrawer.closeDrawer(Gravity.RIGHT);
                } else {
                    mDrawer.openDrawer(Gravity.RIGHT);
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
        String msg = mSendMessageText.getText().toString();
        mChatMessages.add(new Message(myId, mConversationId, msg, date.getTime()));
        mChatRoomAdapter.notifyItemInserted(mChatMessages.size() - 1);
        mChatLayoutManager.scrollToPosition(mChatMessages.size() - 1);
        mSendMessageText.setText("");

        String action = "MESSAGE";
        sendMessageToAttendees(msg, action);
    }

    private void sendMessageToAttendees(String message, String action) {
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        GcmMessage gcmMessage = GcmMessage.getInstance(this);
        gcmMessage.sendMessage(myId, mConversationId, message, action, chatAttendeeTokenList,gcm);
    }
}
