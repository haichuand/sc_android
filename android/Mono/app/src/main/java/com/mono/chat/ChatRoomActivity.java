package com.mono.chat;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.mono.R;

import java.util.ArrayList;

public class ChatRoomActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    //constants used to send and receive bundles
    public static final String CHAT_ROOM_ID = "chatRoomId";
    public static final String EVENT_START_TIME = "eventStartTime";
    public static final String EVENT_END_TIME = "eventEndTime";
    public static final String EVENT_NAME = "eventName";
    public static final String EVENT_DATE = "eventDate";

    private long mChatRoomId;
    private String mEventStartTime = "";
    private String mEventEndTime = "";
    private String mEventName = "";
    private String mEventDate = "";
    private Toolbar mToolbar;
    private DrawerLayout mDrawer;
    private NavigationView mNavView;
    private RecyclerView mChatView;
    private ChatRoomAdapter mChatRoomAdapter;
    private ArrayList<ChatMessage> mChatMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent != null) {
            mChatRoomId = intent.getLongExtra(CHAT_ROOM_ID, -1L);
            mEventStartTime = intent.getStringExtra(EVENT_START_TIME);
            mEventEndTime = intent.getStringExtra(EVENT_END_TIME);
            mEventName = intent.getStringExtra(EVENT_NAME);
            mEventDate = intent.getStringExtra(EVENT_DATE);
        }
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(R.layout.activity_chat_room);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        View toolbarView = getLayoutInflater().inflate(R.layout.chat_tool_bar, null);
        ((TextView) toolbarView.findViewById(R.id.event_start_time)).setText(mEventStartTime);
        ((TextView) toolbarView.findViewById(R.id.event_end_time)).setText(mEventEndTime);
        ((TextView) toolbarView.findViewById(R.id.event_name)).setText(mEventName);
        ((TextView) toolbarView.findViewById(R.id.event_date)).setText(mEventDate);
        mToolbar.addView(toolbarView);
//        this.setSupportActionBar(mToolbar);

        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawer, mToolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawer.addDrawerListener(toggle);
        toggle.syncState();

        mNavView = (NavigationView) findViewById(R.id.nav_view);
        mNavView.setNavigationItemSelectedListener(this);

        mChatView = (RecyclerView) findViewById(R.id.listMessages);
        mChatView.setLayoutManager(new LinearLayoutManager(this));
        mChatMessages = new ArrayList<>();
        setTestChatMessages();
        mChatRoomAdapter = new ChatRoomAdapter(mChatMessages, this);
        mChatView.setAdapter(mChatRoomAdapter);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        return false;
    }

    private void setTestChatMessages() {
        mChatMessages.add(new ChatMessage(2L, true, "Are we meeting in Room 362?"));
        mChatMessages.add(new ChatMessage(4L, false, "I thought it's Rm 462"));
        mChatMessages.add(new ChatMessage(5L, false, "Really?"));
        mChatMessages.add(new ChatMessage(2L, true, "Hmm... Let me check with Jane."));
        mChatMessages.add(new ChatMessage(2L, true, "Oh Jason you're right, it's room 462. Bad memory on my part!"));
        mChatMessages.add(new ChatMessage(5L, false, "We gotta notify others."));
    }
}
