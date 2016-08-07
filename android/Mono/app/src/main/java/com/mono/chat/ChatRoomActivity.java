package com.mono.chat;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mono.R;
import com.mono.model.Attendee;
import com.mono.model.AttendeeUsernameComparator;
import com.mono.model.Conversation;
import com.mono.model.Message;
import com.mono.network.ChatServerManager;
import com.mono.network.HttpServerManager;
import com.mono.util.Common;
import com.mono.util.GestureActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatRoomActivity extends GestureActivity implements ConversationManager.ConversationBroadcastListener{
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

//    private String eventName;
//    private String eventId;
    private String conversationId;
    private String myId;
    private Toolbar toolbar;
    private DrawerLayout drawer;
//    private ListView chatAttendeeListView;
    private LinearLayout chatAttendeeListLayout;
    private RecyclerView chatView;
    private ChatRoomAdapter chatRoomAdapter;
    private TextView sendMessageText;
    private ProgressBar sendProgressBar;
    private CountDownTimer countDownTimer;
    private ImageButton sendButton;
    private AutoCompleteTextView addAttendeeTextView;

    private LinearLayoutManager chatLayoutManager;
    private ChatAttendeeMap chatAttendeeMap = new ChatAttendeeMap();
//    private SimpleAdapter chatAttendeeListAdapter;
    private List<Message> chatMessages;
//    private List<Map<String, String>> chatAttendeeAdapterList;
    private List<String> chatAttendeeIdList; //list of chat attendee ids for sending message
    private List<String> updateChatAttendeeIdList; //list of updated chat attendee ids from navigation drawer
    private List<String> checkBoxAttendeeIdList; //list of attendee ids in check box list from navigation drawer
//    private List<String> newlyAddedAttendeeIds = new ArrayList<>();
//    private Attendee mostRecentAddedAttendee = null;
    private ConversationManager conversationManager;
//    private BroadcastReceiver receiver;
    private HttpServerManager httpServerManager;
    private ChatServerManager chatServerManager;
    private CompoundButton.OnCheckedChangeListener checkedChangeListener;

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

//        eventId = intent.getStringExtra(EVENT_ID);
//        eventName = intent.getStringExtra(EVENT_NAME);
        long eventStartTime = intent.getLongExtra(EVENT_START_TIME, 0);
        long eventEndTime = intent.getLongExtra(EVENT_END_TIME, 0);
        conversationId = intent.getStringExtra(CONVERSATION_ID);
        myId = intent.getStringExtra(MY_ID);

        if (myId == null || conversationId == null) {
            Log.e(TAG, "Error: missing myId or conversationId");
            finish();
        }

        conversationManager = ConversationManager.getInstance(this);
        Conversation conversation = conversationManager.getConversationById(conversationId);

        setContentView(R.layout.activity_chat_room);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                initializeCheckBoxAttendeeList();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                updateChatAttendees();
            }
        };
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (toolbar != null) {
            View toolbarView = toolbar.findViewById(R.id.chat_toolbar);

            TextView title = (TextView) toolbarView.findViewById(R.id.title);
            title.setText(conversation.name);

            if(eventStartTime != 0 && eventEndTime != 0) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(eventStartTime);
                String date = DATE_FORMAT.format(calendar.getTime());
                String start = TIME_FORMAT.format(calendar.getTime());

                calendar.setTimeInMillis(eventEndTime);
                String end = TIME_FORMAT.format(calendar.getTime());

                TextView description = (TextView) toolbarView.findViewById(R.id.description);
                description.setText(String.format("%s from %s to %s", date, start, end));
            }
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        chatView = (RecyclerView) findViewById(R.id.listMessages);
        chatAttendeeListLayout = (LinearLayout) findViewById(R.id.chat_attendee_list);
        sendMessageText = (TextView) findViewById(R.id.sendMessageText);
        sendButton = (ImageButton) findViewById(R.id.sendButton);
        sendProgressBar = (ProgressBar) findViewById(R.id.sendProgressBar);
        httpServerManager = new HttpServerManager(this);
        chatServerManager = new ChatServerManager(this);
        initialize();
    }

    private void initialize() {
        //set up main chat main view
        chatLayoutManager = new LinearLayoutManager(this);
        chatView.setLayoutManager(chatLayoutManager);
        chatMessages = new ArrayList<>();
        chatAttendeeMap = conversationManager.getChatAttendeeMap(conversationId);
        chatRoomAdapter = new ChatRoomAdapter(this, myId, chatAttendeeMap, chatMessages);
        chatView.setAdapter(chatRoomAdapter);

        chatAttendeeIdList = conversationManager.getChatAttendeeIdList(chatAttendeeMap, myId);
        updateChatAttendeeIdList = new ArrayList<>();
        updateChatAttendeeIdList.addAll(chatAttendeeIdList);

//        /* set up adapter for chat attendee list */
//        HashMap<String, Attendee> attendeeMapCopy = new HashMap<>(chatAttendeeMap.getAttendeeMap());
//        chatAttendeeAdapterList = new ArrayList<>();
//        HashMap<String, String> attendeeItem = new HashMap<>();
//        attendeeItem.put("name", "Me");
//        chatAttendeeAdapterList.add(attendeeItem); //put my name first in attendee list
//        attendeeMapCopy.remove(String.valueOf(myId));
//        for (Map.Entry entry : attendeeMapCopy.entrySet()) { //then put other attendees in the list
//            attendeeItem = new HashMap<>();
//            String name = ((Attendee) entry.getValue()).toString();
//            attendeeItem.put("name", name);
//            chatAttendeeAdapterList.add(attendeeItem);
//        }
//        String[] from = new String[] {"name"};  //maps name to name textView
//        int[] to = new int[] {R.id.chat_attendee_name};
//        chatAttendeeListAdapter = new SimpleAdapter(this, chatAttendeeAdapterList, R.layout.chat_attendee_list_item, from, to);
//        chatAttendeeListView.setAdapter(chatAttendeeListAdapter);


        //set up listener for attendee list check boxes
        checkedChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton checkBox, boolean isChecked) {
                String id = checkBox.getId() + "";
                if (isChecked) {
                    if (!updateChatAttendeeIdList.contains(id))
                        updateChatAttendeeIdList.add(id);
                } else {
                    updateChatAttendeeIdList.remove(id);
                }
            }
        };

        //set up auto complete text view for inviting friends
        addAttendeeTextView = (AutoCompleteTextView) findViewById(R.id.edit_text_invite);
        List<Attendee> allUsersList = conversationManager.getAllUserList();
        Collections.sort(allUsersList, new AttendeeUsernameComparator());
        ArrayAdapter<Attendee> addAttendeeAdapter = new ArrayAdapter<Attendee>(this, android.R.layout.simple_dropdown_item_1line, allUsersList);
        addAttendeeTextView.setAdapter(addAttendeeAdapter);
//        addAttendeeTextView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//            @Override
//            public void onFocusChange(View view, boolean hasFocus) {
//                if (hasFocus) {
//                    addAttendeeTextView.showDropDown();
//                }
//            }
//        });
        addAttendeeTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addAttendeeTextView.showDropDown();
            }
        });
        addAttendeeTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Attendee attendee = (Attendee) adapterView.getItemAtPosition(i);
                if (checkBoxAttendeeIdList.contains(attendee.id)) {
                    Toast.makeText(ChatRoomActivity.this, "User already in chat", Toast.LENGTH_SHORT).show();
                    addAttendeeTextView.setText("");
                } else {
                    checkBoxAttendeeIdList.add(attendee.id);
                    updateChatAttendeeIdList.add(attendee.id);
                    addCheckBoxFromAttendee(chatAttendeeListLayout, attendee, checkedChangeListener);
                }
            }
        });

//        receiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                Bundle data = intent.getBundleExtra(MyGcmListenerService.GCM_MESSAGE_DATA);
//                String conversation_id = data.getString(GCMHelper.CONVERSATION_ID);
//                //only continue if conversationId matches
//                if (conversation_id==null || !conversation_id.equals(conversationId))
//                    return;
//                String message = data.getString(GCMHelper.MESSAGE);
//                String sender_id = data.getString(GCMHelper.SENDER_ID);
//
//                if (Common.compareStrings(sender_id, myId)) {
//                    // Sent Confirmation
//                } else {
//                    chatMessages.add(new Message(sender_id, conversationId, message, new Date().getTime()));
//                    chatRoomAdapter.notifyItemInserted(chatMessages.size() - 1);
//                }
//            }
//        };

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

        conversationManager.addListener(this);
        conversationManager.setActiveConversationId(conversationId);
    }

    @Override
    protected void onStart() {
        super.onStart();
//        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(MyGcmListenerService.GCM_INCOMING_INTENT));
    }

    @Override
    protected void onResume() {
        super.onResume();
        chatMessages.clear();
        chatMessages.addAll(conversationManager.getChatMessages(conversationId));
        chatRoomAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
//        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onStop();
        conversationManager.setActiveConversationId(null);
        conversationManager.removeListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
        if (!Common.isConnectedToInternet(this)) {
            Toast.makeText(this, "No network connection. Cannot send message", Toast.LENGTH_SHORT).show();
            return;
        }
        if (chatAttendeeIdList == null || chatAttendeeIdList.isEmpty()) {
            Toast.makeText(this, "No participants in chat", Toast.LENGTH_LONG).show();
            return;
        }
        String msg = sendMessageText.getText().toString();
        if (msg.isEmpty()) {
            return;
        }

        chatServerManager.sendConversationMessage(myId, conversationId, chatAttendeeIdList, msg);
        sendButton.setVisibility(View.GONE);
        sendProgressBar.setVisibility(View.VISIBLE);
        countDownTimer = new CountDownTimer(10000, 10000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                sendButton.setVisibility(View.VISIBLE);
                sendProgressBar.setVisibility(View.GONE);
                Toast.makeText(ChatRoomActivity.this, "Chat server error, cannot send message", Toast.LENGTH_LONG).show();
            }
        };
        countDownTimer.start();

//        chatMessages.add(new Message(myId, conversationId, msg, new Date().getTime()));
//        chatRoomAdapter.notifyItemInserted(chatMessages.size() - 1);
//        sendMessageText.setText("");
//
//
//        Message message = new Message(myId, conversationId, msg, new Date().getTime());
//        conversationManager.saveChatMessageToDB(message);
//        conversationManager.notifyListenersNewConversationMessage(conversationId, myId, msg);
    }

//    public void onAddAttendeeButtonClicked(View view) {
//        addAttendeeTextView.setText("");
//        if (mostRecentAddedAttendee == null) {
//            return;
//        }
//        newlyAddedAttendeeIds.add(mostRecentAddedAttendee.id);
//
//        Map<String, String> nameMap = new HashMap<>();
//        nameMap.put("name", mostRecentAddedAttendee.toString());
//        chatAttendeeAdapterList.add(nameMap);
//        chatAttendeeListAdapter.notifyDataSetChanged();
//
//        mostRecentAddedAttendee = null;
//    }

//    private void addNewAttendeesIfPresent() {
//        if (newlyAddedAttendeeIds.isEmpty()) {
//            return;
//        }
//
//        String message = null;
//        switch (httpServerManager.addConversationAttendees(conversationId, newlyAddedAttendeeIds)) {
//            case 3: //ok
//                break;
//            case 1: //no user
//                message = "Cannot find one or more users in database";
//                break;
//            case 4:
//                message = "Cannot find conversation with id: " + conversationId + "in database";
//                break;
//            case -1:
//                message = "Server unavailable. Mark as SYNC_NEEDED";
//                //TODO: mark Conversation_Attendees table as Sync_Needed
//                break;
//        }
//        if (message != null) {
//            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
//        }
//
//        //notifiy existing conversation attendees of new users
//        if (!chatAttendeeIdList.isEmpty()) {
//            chatServerManager.addConversationAttendees(myId, conversationId, newlyAddedAttendeeIds, chatAttendeeIdList);
//        }
//        //notify new users of being added to conversation
//        chatServerManager.startConversation(myId, conversationId, newlyAddedAttendeeIds);
//
//        conversationManager.addAttendees(conversationId, newlyAddedAttendeeIds);
//
//        for (String id : newlyAddedAttendeeIds) {
//            chatAttendeeMap.addAttendee(conversationManager.getAttendeeById(id));
//            chatAttendeeIdList.add(id);
//        }
//        newlyAddedAttendeeIds.clear();
//    }

    private void updateChatAttendees() {
        if (updateChatAttendeeIdList.size() < 1) {
            Toast.makeText(this, "Chat must have at least two participants", Toast.LENGTH_LONG).show();
            return;
        }
        ArrayList<String> dropAttendeeIdList = new ArrayList<>();
        dropAttendeeIdList.addAll(chatAttendeeIdList);
        dropAttendeeIdList.removeAll(updateChatAttendeeIdList);
        ArrayList<String> addAttendeeIdList = new ArrayList<>();
        addAttendeeIdList.addAll(updateChatAttendeeIdList);
        addAttendeeIdList.removeAll(chatAttendeeIdList);
        if (addAttendeeIdList.isEmpty() && dropAttendeeIdList.isEmpty()) { //no changes
            return;
        }

        if (!dropAttendeeIdList.isEmpty()) {
            String message = null;
            switch (httpServerManager.dropConversationAttendees(conversationId, dropAttendeeIdList)) {
                case 3: //ok
                    break;
                case 1: //no user
                    message = "Cannot find one or more participants in database";
                    break;
                case 4:
                    message = "Cannot find conversation with id: " + conversationId + "in database";
                    break;
                case -1:
                    message = "Server unavailable. Please check your network connection";
                    //TODO: mark Conversation_Attendees table as Sync_Needed
                    break;
            }
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (!addAttendeeIdList.isEmpty()) {
            String message = null;
            switch (httpServerManager.addConversationAttendees(conversationId, addAttendeeIdList)) {
                case 3: //ok
                    break;
                case 1: //no user
                    message = "Cannot find one or more users in database";
                    break;
                case 4:
                    message = "Cannot find conversation with id: " + conversationId + "in database";
                    break;
                case -1:
                    message = "Server unavailable. Please check your network connection";
                    //TODO: mark Conversation_Attendees table as Sync_Needed
                    break;
            }
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        //notify dropped conversation attendees
        if (!dropAttendeeIdList.isEmpty()) {
            chatServerManager.dropConversationAttendees(myId, conversationId, dropAttendeeIdList, chatAttendeeIdList);
        }

        //notifiy existing conversation attendees of new users and new users to start conversation
        if (!addAttendeeIdList.isEmpty()) {
            List<String> recipients = new ArrayList<>();
            recipients.addAll(updateChatAttendeeIdList);
            recipients.removeAll(addAttendeeIdList);
            chatServerManager.addConversationAttendees(myId, conversationId, addAttendeeIdList, recipients);
            chatServerManager.startConversation(myId, conversationId, addAttendeeIdList);
        }

        //update conversation attendees in local database
        conversationManager.setAttendees(conversationId, updateChatAttendeeIdList);

        //update member variables
        chatAttendeeMap.clear();
        chatAttendeeIdList.clear();
        for (String id : updateChatAttendeeIdList) {
            chatAttendeeMap.addAttendee(conversationManager.getAttendeeById(id));
            chatAttendeeIdList.add(id);
        }

        Toast.makeText(this, "Successfully updated chat participants", Toast.LENGTH_LONG).show();
    }

    private CheckBox addCheckBoxFromAttendee (LinearLayout checkBoxLayout, Attendee attendee, CompoundButton.OnCheckedChangeListener checkedChangeListener) {
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        CheckBox checkBox = new CheckBox(this);
        checkBox.setLayoutParams(params);
        checkBox.setId(Integer.valueOf(attendee.id));
        checkBox.setText(attendee.toString());
        checkBox.setChecked(true);
        checkBox.setOnCheckedChangeListener(checkedChangeListener);
        checkBoxLayout.addView(checkBox);
        return checkBox;
    }

    private void initializeCheckBoxAttendeeList() {
        checkBoxAttendeeIdList = new ArrayList<>();
        chatAttendeeListLayout.removeAllViews();
        Attendee me = new Attendee(myId);
        me.firstName = "Me";
        addCheckBoxFromAttendee(chatAttendeeListLayout, me, checkedChangeListener).setEnabled(false);
        checkBoxAttendeeIdList.add(myId);
        for (Attendee attendee : chatAttendeeMap.toAttendeeList()) {
            if (attendee.id.equals(myId)) { //skip myId
                continue;
            }
            addCheckBoxFromAttendee(chatAttendeeListLayout, attendee, checkedChangeListener);
            checkBoxAttendeeIdList.add(attendee.id);
        }
    }

    @Override
    public void onNewConversation(Conversation conversation, int index) {
    }

    @Override
    public void onNewConversationAttendees(String incomingConversationId, List<String> newAttendeeIds) {
        if (conversationId.equals(incomingConversationId)) {
            for (String id : newAttendeeIds) {
                Attendee attendee = conversationManager.getAttendeeById(id);
                chatAttendeeMap.addAttendee(attendee);
                chatAttendeeIdList.add(attendee.id);
                updateChatAttendeeIdList.add(attendee.id);
            }
        }
    }

    @Override
    public void onNewConversationMessage(String incomingConversationId, String senderId, String msg, long timeStamp) {
        if (!conversationId.equals(incomingConversationId) || senderId == null){
            return;
        }

        if (myId.equals(senderId)) { //self-sent ack message
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
            sendButton.setVisibility(View.VISIBLE);
            sendProgressBar.setVisibility(View.GONE);
        }

        chatMessages.add(new Message(myId, conversationId, msg, new Date().getTime()));
        chatRoomAdapter.notifyItemInserted(chatMessages.size() - 1);
        sendMessageText.setText("");
    }
}
