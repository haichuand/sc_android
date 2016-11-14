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
import android.text.InputType;
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
import android.widget.TextView;
import android.widget.Toast;

import com.mono.EventManager;
import com.mono.R;
import com.mono.db.DatabaseValues;
import com.mono.model.Attendee;
import com.mono.model.AttendeeUsernameComparator;
import com.mono.model.Conversation;
import com.mono.model.Event;
import com.mono.model.Media;
import com.mono.model.Message;
import com.mono.model.ServerSyncItem;
import com.mono.network.ChatServerManager;
import com.mono.network.HttpServerManager;
import com.mono.network.ServerSyncManager;
import com.mono.util.GestureActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatRoomActivity extends GestureActivity implements ConversationManager.ConversationBroadcastListener{

    public static final int REQUEST_CAMERA = 1;
    public static final int REQUEST_MEDIA_PICKER = 2;

    //constants used to send and receive bundles
    public static final String CONVERSATION_ID = "conversationId";
    public static final String EVENT_START_TIME = "eventStartTime";
    public static final String EVENT_END_TIME = "eventEndTime";
    public static final String EVENT_ID = "eventId";
    public static final String EVENT_NAME = "eventName";
    public static final String EVENT_ALL_DAY = "eventAllDay";
    public static final String MY_ID = "myId";
    private static final String TAG = "ChatRoomActivity";
    private static final String DROP_ATTENDEES = "dropAttendees";
    private static final String ADD_ATTENDEES = "addAttendees";

    private static final SimpleDateFormat DATE_FORMAT;
    private static final SimpleDateFormat TIME_FORMAT;
    private static final SimpleDateFormat WEEKDAY_FORMAT;

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
//    private ProgressBar sendProgressBar;
//    private CountDownTimer countDownTimer;
    private ImageButton sendButton;
    private AutoCompleteTextView addAttendeeTextView;

    private AttachmentPanel attachmentPanel;

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
    private HttpServerManager httpServerManager;
    private ChatServerManager chatServerManager;
    private CompoundButton.OnCheckedChangeListener checkedChangeListener;
    private Map<Long, CountDownTimer> timerMap;
    private static final long SERVER_TIMEOUT_MS = 5000;
    private ServerSyncManager serverSyncManager;

    static {
        DATE_FORMAT = new SimpleDateFormat("M/d/yy", Locale.getDefault());
        TIME_FORMAT = new SimpleDateFormat("h:mm a", Locale.getDefault());
        WEEKDAY_FORMAT = new SimpleDateFormat("EEE", Locale.getDefault());
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
        boolean isAllDay = intent.getBooleanExtra(EVENT_ALL_DAY, false);
        conversationId = intent.getStringExtra(CONVERSATION_ID);
        myId = intent.getStringExtra(MY_ID);

        if (myId == null || conversationId == null) {
            Log.e(TAG, "Error: missing myId or conversationId");
            finish();
        }

        conversationManager = ConversationManager.getInstance(this);
        Conversation conversation = conversationManager.getCompleteConversation(conversationId);
        if (conversation.eventId != null && eventStartTime == 0) {
            EventManager eventManager = EventManager.getInstance(this);
            Event event = eventManager.getEvent(conversation.eventId);
            if (event != null) {
                eventStartTime = event.startTime;
                eventEndTime = event.endTime;
            }
        }

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
                Date dt = calendar.getTime();
                String date = DATE_FORMAT.format(dt);
                String start = TIME_FORMAT.format(dt);
                String weekDay = WEEKDAY_FORMAT.format(dt);

                calendar.setTimeInMillis(eventEndTime);
                String end = TIME_FORMAT.format(calendar.getTime());

                TextView description = (TextView) toolbarView.findViewById(R.id.description);
                if (isAllDay) {
                    description.setText(String.format("%s, %s", weekDay, date));
                } else {
                    description.setText(String.format("%s, %s from %s to %s", weekDay, date, start, end));
                }
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
//        sendProgressBar = (ProgressBar) findViewById(R.id.sendProgressBar);
        httpServerManager = HttpServerManager.getInstance(this);
        chatServerManager = ChatServerManager.getInstance(this);
        timerMap = new HashMap<>();
        serverSyncManager = ServerSyncManager.getInstance(this);
        initialize();

        attachmentPanel = new AttachmentPanel(this);
        attachmentPanel.onCreate(savedInstanceState);
    }

    private void initialize() {
        //set up main chat main view
        chatLayoutManager = new LinearLayoutManager(this);
        chatView.setLayoutManager(chatLayoutManager);
        chatMessages = conversationManager.getChatMessages(conversationId);
        for (int i = 0; i < chatMessages.size() - 1; i++) {
            Message currentMsg = chatMessages.get(i);
            Message nextMsg = chatMessages.get(i + 1);
            nextMsg.showMessageTime = nextMsg.getTimestamp() - currentMsg.getTimestamp() >= Message.GROUP_TIME_THRESHOLD;
            nextMsg.showMessageSender = nextMsg.showMessageTime || !nextMsg.getSenderId().equals(currentMsg.getSenderId());
        }
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
        final List<Attendee> allUsersList = conversationManager.getAllUserList();
        Collections.sort(allUsersList, new AttendeeUsernameComparator());
        List<String> allUserStringList = ConversationManager.getAttendeeStringtWithNameAndEmail(allUsersList);
        ArrayAdapter<String> addAttendeeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, allUserStringList);
        addAttendeeTextView.setAdapter(addAttendeeAdapter);
        addAttendeeTextView.setInputType(InputType.TYPE_NULL);
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
                Attendee attendee = allUsersList.get(i);
                if (checkBoxAttendeeIdList.contains(attendee.id)) {
                    Toast.makeText(ChatRoomActivity.this, "User already in chat", Toast.LENGTH_SHORT).show();
                    addAttendeeTextView.setText("");
                } else {
                    checkBoxAttendeeIdList.add(attendee.id);
                    updateChatAttendeeIdList.add(attendee.id);
                    addCheckBoxFromAttendee(chatAttendeeListLayout, attendee, checkedChangeListener);
                }
                addAttendeeTextView.setText("");
            }
        });

        final LinearLayout messagesLayout = (LinearLayout) findViewById(R.id.all_messages_linearlayout);
        if (messagesLayout != null) {
            messagesLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    messagesLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    int size = chatMessages.size();
                    if (chatLayoutManager.findLastCompletelyVisibleItemPosition() < size-1) {
                        chatLayoutManager.scrollToPosition(size - 1);
                    }
                }
            });
        }

        conversationManager.addBroadcastListner(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        conversationManager.setActiveConversationId(conversationId);
//        chatMessages.clear();
//        chatMessages.addAll(conversationManager.getChatMessages(conversationId));
//        chatRoomAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
        conversationManager.setActiveConversationId(null);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        conversationManager.removeBroadcastListener(this);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CAMERA:
                attachmentPanel.handleCamera(resultCode, data);
                break;
            case REQUEST_MEDIA_PICKER:
                attachmentPanel.handleMediaPicker(resultCode, data);
                break;
        }
    }

    public void onSendButtonClicked(View view) {
//        if (!Common.isConnectedToInternet(this)) {
//            Toast.makeText(this, "No network connection. Cannot send message", Toast.LENGTH_SHORT).show();
//            return;
//        }
        if (chatAttendeeIdList == null || chatAttendeeIdList.isEmpty()) {
            Toast.makeText(this, "No participants in chat", Toast.LENGTH_LONG).show();
            return;
        }
        String messageText = sendMessageText.getText().toString();
        List<Media> attachments = attachmentPanel.getAttachments();;
        if (messageText.isEmpty() && attachments.isEmpty()) {
            return;
        }

        Message message = new Message(myId, conversationId, messageText, System.currentTimeMillis());
        message.ack = false;
        message.attachments = attachments;
        message.setMessageId(conversationManager.saveChatMessageToDB(message));
        addMessageToUI(message);

        if (message.attachments == null || message.attachments.isEmpty()) {
            sendMessage(message, null);
        } else {
            AttachmentPanel.sendAttachments(
                this,
                message,
                new AttachmentPanel.AttachmentsListener() {
                    @Override
                    public void onFinish(Message message, List<String> result) {
                        if (result != null) {
                            sendMessage(message, result);
                        } else {
                            ServerSyncItem syncItem = new ServerSyncItem(
                                    String.valueOf(message.getMessageId()),
                                    DatabaseValues.ServerSync.TYPE_MESSAGE,
                                    DatabaseValues.ServerSync.SERVER_CHAT
                            );
                            serverSyncManager.addSyncItem(syncItem);
                            serverSyncManager.enableNetworkStateReceiver();
                        }
                    }
                }
            );
        }

        httpServerManager.addMessage(message);
        sendMessageText.setText("");
        attachmentPanel.clear();
    }

    public void sendMessage(final Message message, List<String> attachments) {
        chatServerManager.sendConversationMessage(myId, conversationId, chatAttendeeIdList, message.getMessageText(), String.valueOf(message.getMessageId()), attachments);
        CountDownTimer timer = new CountDownTimer(SERVER_TIMEOUT_MS, SERVER_TIMEOUT_MS) {
            @Override
            public void onTick(long l) {

            }

            @Override
            public void onFinish() {
                ServerSyncItem syncItem = new ServerSyncItem(
                        String.valueOf(message.getMessageId()),
                        DatabaseValues.ServerSync.TYPE_MESSAGE,
                        DatabaseValues.ServerSync.SERVER_CHAT
                );
                serverSyncManager.addSyncItem(syncItem);
                serverSyncManager.enableNetworkStateReceiver();
            }
        };
        timerMap.put(message.getMessageId(), timer);
        timer.start();
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
//                message = "Server unavailable. Mark as ACK";
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
//            chatAttendeeMap.addAttendee(conversationManager.getUserById(id));
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
            chatAttendeeMap.addAttendee(conversationManager.getUserById(id));
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
                Attendee attendee = conversationManager.getUserById(id);
                chatAttendeeMap.addAttendee(attendee);
                chatAttendeeIdList.add(attendee.id);
                updateChatAttendeeIdList.add(attendee.id);
            }
        }
    }

    @Override
    public void onDropConversationAttendees(String conversationId, List<String> dropAttendeeIds) {

    }

    @Override
    public void onNewConversationMessage(Message message) {
        String senderId = message.getSenderId();
        if (!conversationId.equals(message.getConversationId()) || senderId == null){
            return;
        }

        if (myId.equals(senderId)) { //self-sent ack message
            //cancel timer: do not add message to server sync list
            if (timerMap.containsKey(message.getMessageId())) {
                CountDownTimer timer = timerMap.remove(message.getMessageId());
                timer.cancel();
            }

            //find message and remove sending icon
            for (int i = chatMessages.size() - 1; i >= 0; i--) {
                Message chatMessage = chatMessages.get(i);
                if (chatMessage.getMessageId() == message.getMessageId()) {
                    chatMessage.ack = true;
                    chatRoomAdapter.notifyItemChanged(i);
                    break;
                }
            }
        } else {
            addMessageToUI(message);
        }
    }

    //add message, set group time flag and notify adpater
    private void addMessageToUI (Message message) {
        if (message == null) {
            return;
        }

        //find the correct position based on timestamp
        int index = chatMessages.size() - 1;
        while (index >= 0 && message.getTimestamp() < chatMessages.get(index).getTimestamp()) {
            index--;
        }

        //update inerted message show time/sender flags
        Message lastMsg = index >= 0 ? chatMessages.get(index) : null;
        message.showMessageTime = lastMsg == null || message.getTimestamp() - lastMsg.getTimestamp() > Message.GROUP_TIME_THRESHOLD;
        message.showMessageSender = lastMsg == null || message.showMessageTime || !message.getSenderId().equals(lastMsg.getSenderId());
        chatMessages.add(index + 1, message);
        chatRoomAdapter.notifyItemInserted(index + 1);

        //upate next message show time/sender flags
        Message nextMsg = index + 2 < chatMessages.size() ? chatMessages.get(index + 2) : null;
        if (nextMsg != null) {
            boolean nextMsgShowMsgTime = nextMsg.getTimestamp() - message.getTimestamp() > Message.GROUP_TIME_THRESHOLD;
            boolean nextMsgShowMsgSender = nextMsgShowMsgTime || !nextMsg.getSenderId().equals(message.getSenderId());
            if (nextMsgShowMsgTime != nextMsg.showMessageTime || nextMsgShowMsgSender != nextMsg.showMessageSender) {
                nextMsg.showMessageTime = nextMsgShowMsgTime;
                nextMsg.showMessageSender = nextMsgShowMsgSender;
                chatRoomAdapter.notifyItemChanged(index + 2);
            }
        }

        chatLayoutManager.scrollToPosition(chatMessages.size() - 1);
    }
}
