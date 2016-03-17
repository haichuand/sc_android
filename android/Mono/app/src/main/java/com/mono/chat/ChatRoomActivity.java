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
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.mono.R;
import com.mono.model.Attendee;
import com.mono.model.Message;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatRoomActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    //constants used to send and receive bundles
    public static final String CHAT_ROOM_ID = "chatRoomId";
    public static final String EVENT_START_TIME = "eventStartTime";
    public static final String EVENT_END_TIME = "eventEndTime";
    public static final String EVENT_NAME = "eventName";
    public static final String EVENT_DATE = "eventDate";
    public static final String MY_ID = "myId";

    private long mChatRoomId;
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
    private ChatAttendeeMap mChatAttendeeMap;
    private List<Message> mChatMessages;
    private String myId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent == null)
            finish();

        mChatRoomId = intent.getLongExtra(CHAT_ROOM_ID, -1L);
        mEventStartTime = intent.getStringExtra(EVENT_START_TIME);
        mEventEndTime = intent.getStringExtra(EVENT_END_TIME);
        mEventName = intent.getStringExtra(EVENT_NAME);
        mEventDate = intent.getStringExtra(EVENT_DATE);
        myId = intent.getStringExtra(MY_ID);

        if (mEventStartTime == null || mEventEndTime == null || mEventName == null || mEventDate == null || myId == null) {
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

        mChatAttendeeListView = (ListView) findViewById(R.id.chat_drawer_attendees);
//        mChatAttendeeListView.setAdapter();

        mChatView = (RecyclerView) findViewById(R.id.listMessages);
        mChatView.setLayoutManager(new LinearLayoutManager(this));
        mChatAttendeeMap = new ChatAttendeeMap();
        mChatMessages = new ArrayList<>();
        setTestChatAttendees();
        setTestChatMessages();
        mChatRoomAdapter = new ChatRoomAdapter(myId, mChatAttendeeMap, mChatMessages, this);
        mChatView.setAdapter(mChatRoomAdapter);
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
        String conversationId = "12345";
        mChatMessages.add(new Message(String.valueOf(2), conversationId, "Hi!", new Date().getTime()));
        mChatMessages.add(new Message(String.valueOf(2), conversationId, "Are we meeting here?", new Date().getTime()));
        mChatMessages.add(new Message(String.valueOf(4), conversationId, "I thought it's Rm 462", new Date().getTime()));
        mChatMessages.add(new Message(String.valueOf(5), conversationId, "Really?", new Date().getTime()));
        mChatMessages.add(new Message(String.valueOf(2), conversationId, "Hmm... Let me check with Jane.", new Date().getTime()));
        mChatMessages.add(new Message(String.valueOf(2), conversationId, "Oh Margret you're right, it's room 462. Bad memory on my part!", new Date().getTime()));
        mChatMessages.add(new Message(String.valueOf(6), conversationId, "We gotta notify others.", new Date().getTime()));
        mChatMessages.add(new Message(String.valueOf(5), conversationId, "We gotta notify others.", new Date().getTime()));
        mChatMessages.add(new Message(String.valueOf(2), conversationId, "Oh Jason you're right, it's room 462. Bad memory on my part!", new Date().getTime()));
        mChatMessages.add(new Message(String.valueOf(4), conversationId, "Too many type sizes and styles at once can wreck any layout. A typographic scale has a limited set of type sizes that work well together along with the layout grid.", new Date().getTime()));
        mChatMessages.add(new Message(String.valueOf(2), conversationId, "Instances of this class are suitable for comparison, but little else. Use DateFormat to format a Date for display to a human.", new Date().getTime()));
        mChatMessages.add(new Message(String.valueOf(6), conversationId, "Note that, surprisingly, instances of this class are mutable.", new Date().getTime()));
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
}
