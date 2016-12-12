package com.mono.chat;

import android.app.Activity;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mono.AccountManager;
import com.mono.EventManager;
import com.mono.R;
import com.mono.contacts.ContactsActivity;
import com.mono.db.DatabaseValues;
import com.mono.details.EventDetailsActivity;
import com.mono.model.Attendee;
import com.mono.model.Contact;
import com.mono.model.Conversation;
import com.mono.model.Event;
import com.mono.model.Media;
import com.mono.model.Message;
import com.mono.model.ServerSyncItem;
import com.mono.network.ChatServerManager;
import com.mono.network.HttpServerManager;
import com.mono.network.ServerSyncManager;
import com.mono.util.Common;
import com.mono.util.GestureActivity;
import com.mono.util.Strings;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

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

    private String conversationId;
    private String myId;
    private Toolbar toolbar;
    private DrawerLayout drawer;
    private LinearLayout chatAttendeeListLayout;
    private RecyclerView chatView;
    private ChatRoomAdapter chatRoomAdapter;
    private TextView sendMessageText;

    private AttachmentPanel attachmentPanel;

    private LinearLayoutManager chatLayoutManager;
    private Conversation chat;
    private List<Message> chatMessages;
    private List<String> chatAttendeeIdList; //list of chat attendee ids for sending message
    private List<String> updateChatAttendeeIdList; //list of updated chat attendee ids from navigation drawer
    private ConversationManager conversationManager;
    private HttpServerManager httpServerManager;
    private ChatServerManager chatServerManager;
    private CompoundButton.OnCheckedChangeListener checkedChangeListener;
    private Map<Long, CountDownTimer> timerMap;
    private static final long SERVER_TIMEOUT_MS = 5000;
    private ServerSyncManager serverSyncManager;
    private Random random = new Random();

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

        long eventStartTime = intent.getLongExtra(EVENT_START_TIME, 0);
        long eventEndTime = intent.getLongExtra(EVENT_END_TIME, 0);
        boolean isAllDay = intent.getBooleanExtra(EVENT_ALL_DAY, false);
        conversationId = intent.getStringExtra(CONVERSATION_ID);
        myId = String.valueOf(AccountManager.getInstance(this).getAccount().id);

        if (conversationId == null) {
            Log.e(TAG, "Error: conversationId");
            finish();
        }

        conversationManager = ConversationManager.getInstance(this);
        chat = conversationManager.getCompleteConversation(conversationId);
        chatAttendeeIdList = chat.getAttendeeIdList();
        chatMessages = chat.getMessages();
        if (chat.eventId != null && eventStartTime == 0) {
            EventManager eventManager = EventManager.getInstance(this);
            Event event = eventManager.getEvent(chat.eventId);
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
            title.setText(chat.name);

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
        for (int i = 0; i < chatMessages.size() - 1; i++) {
            Message currentMsg = chatMessages.get(i);
            Message nextMsg = chatMessages.get(i + 1);
            nextMsg.showMessageTime = nextMsg.getTimestamp() - currentMsg.getTimestamp() >= Message.GROUP_TIME_THRESHOLD;
            nextMsg.showMessageSender = nextMsg.showMessageTime || !nextMsg.getSenderId().equals(currentMsg.getSenderId());
        }
        chatRoomAdapter = new ChatRoomAdapter(this, myId, chatMessages);
        chatView.setAdapter(chatRoomAdapter);

        //check if user self has been dropped from chat
        if (!chat.getAttendeeIdList().contains(myId)) {
            Toast.makeText(this, R.string.dropped_from_chat, Toast.LENGTH_LONG).show();
        }
        updateChatAttendeeIdList = new ArrayList<>();

        //set up listener for attendee list check boxes
        checkedChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton checkBox, boolean isChecked) {
                String id = (String) checkBox.getTag();
                if (isChecked) {
                    if (!updateChatAttendeeIdList.contains(id))
                        updateChatAttendeeIdList.add(id);
                } else {
                    updateChatAttendeeIdList.remove(id);
                }
            }
        };

        //set up contact input and contact picker
        View contactPicker = findViewById(R.id.contact_picker);
        contactPicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ChatRoomActivity.this, ContactsActivity.class);
                intent.putExtra(ContactsActivity.EXTRA_MODE, ContactsActivity.MODE_PICKER);
                startActivityForResult(intent, EventDetailsActivity.REQUEST_CONTACT_PICKER);
            }
        });

        //
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
        conversationManager.resetConversationMissCount(conversationId);
        conversationManager.notifyListenersMissCountReset(conversationId);
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
            case EventDetailsActivity.REQUEST_CONTACT_PICKER:
                handleContactPickerResult(resultCode, data);
                break;
        }
    }

    public void onSendButtonClicked(View view) {
        if (chatAttendeeIdList == null || !chatAttendeeIdList.contains(myId)) {
            Toast.makeText(this, R.string.dropped_from_chat, Toast.LENGTH_LONG).show();
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
            }
        };
        timerMap.put(message.getMessageId(), timer);
        timer.start();
    }

    // note: local database updates are done in MyFcmReceiver after receiving confirmation
    private void updateChatAttendees() {
        if (updateChatAttendeeIdList.size() == 1) { //size()==0 indicates user has been dropped
            Toast.makeText(this, R.string.error_chat_participant, Toast.LENGTH_LONG).show();
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

        if (!chat.creatorId.equals(myId)) {
            Toast.makeText(this, R.string.only_creator_change_attendee, Toast.LENGTH_LONG).show();
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
                    message = "Cannot find chat with id: " + conversationId + "in database";
                    break;
                case -1:
                    message = "Server error. Please try again";
                    //TODO: mark Conversation_Attendees table as Sync_Needed
                    break;
            }
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (!addAttendeeIdList.isEmpty()) {
            // save unregistered attendees (those with negative ids)
            for (int j = 0; j < addAttendeeIdList.size(); j++) {
                String attendeeId = addAttendeeIdList.get(j);
                if (attendeeId.startsWith("-")) {
                    Attendee attendee = null;
                    for (int i = 0; i < chat.attendees.size(); i++) {
                        if (attendeeId.equals(chat.attendees.get(i).id)) {
                            attendee = chat.attendees.get(i);
                            break;
                        }
                    }
                    if (attendee == null || !conversationManager.saveUnregisteredAttendee(attendee)) {
                        Toast.makeText(this, R.string.error_create_chat, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    chatAttendeeIdList.add(attendee.id);
                    updateChatAttendeeIdList.remove(attendeeId);
                    updateChatAttendeeIdList.add(attendee.id);
                    addAttendeeIdList.set(j, attendee.id);
                }
            }
            String message = null;
            switch (httpServerManager.addConversationAttendees(conversationId, addAttendeeIdList)) {
                case 3: //ok
                    break;
                case 1: //no user
                    message = "Cannot find one or more users in database";
                    break;
                case 4:
                    message = "Cannot find chat with id: " + conversationId + "in database";
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

        //notify dropped chat attendees
        if (!dropAttendeeIdList.isEmpty()) {
            chatServerManager.dropConversationAttendees(myId, conversationId, dropAttendeeIdList, chatAttendeeIdList);
        }

        //notifiy existing chat attendees of new users and new users to start chat
        if (!addAttendeeIdList.isEmpty()) {
            List<String> recipients = new ArrayList<>();
            recipients.addAll(updateChatAttendeeIdList);
            recipients.removeAll(addAttendeeIdList);
            chatServerManager.addConversationAttendees(myId, conversationId, addAttendeeIdList, recipients);
            chatServerManager.startConversation(myId, conversationId, addAttendeeIdList);
        }
    }

    private void addAttendeeCheckBox(LinearLayout checkBoxLayout, Attendee attendee) {
        List<String> allAttendeeIdList = chat.getAttendeeIdList();
        if (allAttendeeIdList.contains(attendee.id)) { //do not add duplicate attendees
            return;
        }

        if (!updateChatAttendeeIdList.contains(attendee.id)) {
            updateChatAttendeeIdList.add(attendee.id);
        }

        if (!chat.attendees.contains(attendee)) {
            chat.attendees.add(attendee);
        }

        if (!attendee.id.equals(myId)) { // do not show user self
            addCheckBox(checkBoxLayout, attendee, allAttendeeIdList);
        }
    }

    private void addCheckBox(LinearLayout checkBoxLayout, Attendee attendee, List<String> chatAttendeeIdList) {
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        CheckBox checkBox = new CheckBox(this);
        checkBox.setLayoutParams(params);
        checkBox.setTag(attendee.id);
        checkBox.setText(attendee.toString());
        checkBox.setChecked(true);
        checkBox.setOnCheckedChangeListener(checkedChangeListener);
        checkBoxLayout.addView(checkBox);
    }

    private void initializeCheckBoxAttendeeList() {
        chatAttendeeListLayout.removeAllViews();
        updateChatAttendeeIdList.clear();
        for (Attendee attendee : chat.attendees) {
            if (!chatAttendeeIdList.contains(attendee.id)) {
                continue;
            }
            updateChatAttendeeIdList.add(attendee.id);
            if (!attendee.id.equals(myId)) {
                addCheckBox(chatAttendeeListLayout, attendee, chatAttendeeIdList);
            }
        }
    }

    @Override
    public void onNewConversation(Conversation conversation, int index) {
    }

    @Override
    public void onNewConversationAttendees(String incomingConversationId, List<String> newAttendeeIds) {
        if (conversationId.equals(incomingConversationId)) {
            List<String> names = new ArrayList<>();
            for (String id : newAttendeeIds) {
                Attendee attendee = conversationManager.getUserById(id);
                if (!chatAttendeeIdList.contains(id)) {
                    chatAttendeeIdList.add(id);
                }
                addAttendeeCheckBox(chatAttendeeListLayout, attendee);
                names.add(attendee.toString());
            }
            Toast.makeText(this, getString(R.string.add_chat_attendee) + Common.implode(", ", names), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDropConversationAttendees(String incomingConversationId, List<String> dropAttendeeIds) {
        if (conversationId.equals(incomingConversationId)) {
            chatAttendeeIdList.removeAll(dropAttendeeIds);
            updateChatAttendeeIdList.removeAll(dropAttendeeIds);
            List<String> names = new ArrayList<>();
            for (String id : dropAttendeeIds) {
                names.add(conversationManager.getUserById(id).toString());
            }
            chat.attendees = conversationManager.getConversationAttendees(conversationId);
            Toast.makeText(this, getString(R.string.drop_chat_attendee) + Common.implode(", ", names), Toast.LENGTH_LONG).show();
            initializeCheckBoxAttendeeList();
        }
    }

    @Override
    public void onConversationMissCountReset(String conversationId) {

    }

    @Override
    public void onNewConversationMessage(Message message, int missCount) {
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

    private void handleContactPickerResult (int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            List<Contact> contacts =
                    data.getParcelableArrayListExtra(ContactsActivity.EXTRA_CONTACTS);

            for (Contact contact : contacts) {
                String[] emails = contact.getEmails();
                if (emails == null || emails.length == 0) {
                    Toast.makeText(this, R.string.error_no_email, Toast.LENGTH_SHORT).show();
                    continue;
                }

                String id = String.valueOf(contact.id);

                Attendee user = conversationManager.getUserById(id);

                //non-registered user: generate Attendee with negative id for saving on server later
                if (user == null) {
                    String attendeeId = String.valueOf(-random.nextInt(Integer.MAX_VALUE - 1) - 1);
                    while (chat.getAttendeeIdList().contains(attendeeId)) {
                        attendeeId = String.valueOf(-random.nextInt(Integer.MAX_VALUE - 1) - 1);
                    }
                    user = new Attendee(
                            attendeeId,
                            null,
                            emails[0],
                            null,
                            contact.firstName,
                            contact.lastName,
                            contact.toString(),
                            false,
                            true
                    );
                }
                user.isFriend = true;
                addAttendeeCheckBox(chatAttendeeListLayout, user);
            }
        }
    }
}
