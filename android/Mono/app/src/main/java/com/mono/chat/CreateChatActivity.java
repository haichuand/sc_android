package com.mono.chat;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mono.AccountManager;
import com.mono.EventManager;
import com.mono.R;
import com.mono.contacts.ContactsActivity;
import com.mono.db.DatabaseHelper;
import com.mono.db.DatabaseValues;
import com.mono.db.dao.AttendeeDataSource;
import com.mono.db.dao.ConversationDataSource;
import com.mono.db.dao.EventDataSource;
import com.mono.details.EventDetailsActivity;
import com.mono.model.Account;
import com.mono.model.Attendee;
import com.mono.model.Contact;
import com.mono.model.Conversation;
import com.mono.model.Event;
import com.mono.model.ServerSyncItem;
import com.mono.network.ChatServerManager;
import com.mono.network.HttpServerManager;
import com.mono.network.ServerSyncManager;
import com.mono.util.Colors;
import com.mono.util.Common;
import com.mono.util.GestureActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class CreateChatActivity extends GestureActivity implements ConversationManager.ChatAckListener{

    private static final long TIMER_TIMEOUT_MS = 5000;

    private static final String DEFAULT_USER_PASSWORD = Common.md5("@SuperCalyUser");
    private static final int TIMER_TYPE_NULL = 0;
    private static final int TIMER_TYPE_EVENT_CONVERSATION = 97146;
    private static final int TIMER_TYPE_CONVERSATION = 33148;

    private CountDownTimer timer;
    private boolean isRunning = false; //timer running flag
    private int timerType;
    private String conversationId;
    private String confirmationId;
    private String myId;
    private List<String> listChatAttendeeIds = new ArrayList<>(); //all attendees in the checkbox list
    private List<String> checkedChatAttendeeIds = new ArrayList<>(); //check attendees
    private HttpServerManager httpServerManager;
    private ChatServerManager chatServerManager;
    private ServerSyncManager serverSyncManager;
    private EventManager eventManager;
    private ConversationManager conversationManager;
    private LinearLayout checkBoxLayout;
    private CompoundButton.OnCheckedChangeListener checkedChangeListener;
    private EditText titleInput;
    private Event event;

    private ConversationDataSource conversationDataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent != null) {
            event = intent.getParcelableExtra(EventDetailsActivity.EXTRA_EVENT);
        }

        setContentView(R.layout.activity_create_chat);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.create_chat_title);
        setSupportActionBar(toolbar);
//        getSupportActionBar().setTitle(R.string.create_chat_title);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        timer = new CountDownTimer(TIMER_TIMEOUT_MS, TIMER_TIMEOUT_MS) {
            @Override
            public void onTick(long l) {}

            @Override
            public void onFinish() {
                isRunning = false;
                onTimerOut();
                timerType = TIMER_TYPE_NULL;
            }
        };
        httpServerManager = HttpServerManager.getInstance(this);
        chatServerManager = ChatServerManager.getInstance(this);
        serverSyncManager = ServerSyncManager.getInstance(this);
        eventManager = EventManager.getInstance(this);
        conversationManager = ConversationManager.getInstance(this);
        conversationDataSource = DatabaseHelper.getDataSource(this, ConversationDataSource.class);

        conversationManager.addChatAckListener(this);

        titleInput = (EditText) findViewById(R.id.create_chat_title_input);
        checkBoxLayout = (LinearLayout) findViewById(R.id.create_chat_attendees);

        checkedChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton checkBox, boolean isChecked) {
                String id = checkBox.getId() + "";
                if (isChecked) {
                    if (!checkedChatAttendeeIds.contains(id))
                        checkedChatAttendeeIds.add(id);
                } else {
                    checkedChatAttendeeIds.remove(id);
                }
            }
        };

        Account account = AccountManager.getInstance(this).getAccount();
        myId = account.id + "";

        //add user self to list but does not show
        Attendee me = new Attendee(myId, null, account.email, account.phone, account.firstName, account.lastName, account.username, false, false);
        addCheckBoxFromAttendee(me);
        if (event != null) {
            if ("UTC".equals(event.timeZone)) { //holiday, all day event
                event.startTime = Common.convertHolidayMillis(event.startTime);
                event.endTime = event.startTime;
            }
            AttendeeDataSource attendeeDataSource = DatabaseHelper.getDataSource(this, AttendeeDataSource.class);
            replaceWithDatabaseAttendees(event.attendees, attendeeDataSource);
            titleInput.setText(event.title, TextView.BufferType.EDITABLE);
            //add other event attendees
            for (Attendee attendee : event.attendees) {
                if (attendee.id.equals(myId)) {
                    continue;
                }
                addCheckBoxFromAttendee(attendee);
            }
        }

        //set up contact input and contact picker
        View contactPicker = findViewById(R.id.contact_picker);
        contactPicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CreateChatActivity.this, ContactsActivity.class);
                intent.putExtra(ContactsActivity.EXTRA_MODE, ContactsActivity.MODE_PICKER);
                startActivityForResult(intent, EventDetailsActivity.REQUEST_CONTACT_PICKER);
            }
        });

        final EditText input = (EditText) findViewById(R.id.input);
        View submit = findViewById(R.id.submit);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onContactSubmit(input);
            }
        });
    }

    @Override
    protected void onDestroy() {
        conversationManager.removeChatAckListener(this);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.create_chat, menu);
        return true;
    }

    private void onTimerOut() {
        ServerSyncItem syncItem = null;
        switch (timerType) {
            case TIMER_TYPE_EVENT_CONVERSATION:
                syncItem = new ServerSyncItem(confirmationId, DatabaseValues.ServerSync.TYPE_EVENT_CONVERSATION, DatabaseValues.ServerSync.SERVER_CHAT);
                break;
            case TIMER_TYPE_CONVERSATION:
                syncItem = new ServerSyncItem(confirmationId, DatabaseValues.ServerSync.TYPE_EVENT_CONVERSATION, DatabaseValues.ServerSync.SERVER_CHAT);
                break;
        }

        if (syncItem != null) {
            serverSyncManager.addSyncItem(syncItem);
            Toast.makeText(this, R.string.network_error_sync_text, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackPressed () {
        if (isRunning) {
            Toast.makeText(this, R.string.pending_server_response, Toast.LENGTH_LONG);
            return;
        }
        if (!titleInput.getText().toString().isEmpty() || checkedChatAttendeeIds.size() > 1) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this,
                    R.style.AppTheme_Dialog_Alert);
            builder.setMessage(R.string.confirm_discard_change);

            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            finish();
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            break;
                    }

                    dialog.dismiss();
                }
            };

            builder.setPositiveButton(R.string.yes, listener);
            builder.setNegativeButton(R.string.no, listener);

            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_save:
                if (event == null) {
                    createChat();
                } else {
                    createEventChat();
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EventDetailsActivity.REQUEST_CONTACT_PICKER) {
            handleContactPickerResult(resultCode, data);
        }
    }

    private void createChat() {
        if (checkedChatAttendeeIds.size() <= 1) { //myID should always be in the list
            Toast.makeText(this, R.string.error_chat_participant, Toast.LENGTH_LONG).show();
            return;
        }
        String conversationTitle = titleInput.getText().toString();
        if (conversationTitle.isEmpty()) {
            Toast.makeText(this, R.string.error_chat_title, Toast.LENGTH_LONG).show();
            return;
        }

        // save conversation to local database
        saveConversationToDB(conversationTitle, myId, checkedChatAttendeeIds);

        // create conversation on http server
        if (!httpServerManager.createConversation(conversationId, conversationTitle, Integer.valueOf(myId), checkedChatAttendeeIds)) {
            // add conversation to sync queue if unsuccessful
            ServerSyncItem syncItem = new ServerSyncItem(
                    conversationId, DatabaseValues.ServerSync.TYPE_CONVERSATION, DatabaseValues.ServerSync.SERVER_HTTP);
            serverSyncManager.addSyncItem(syncItem);
            syncItem = new ServerSyncItem(
                    conversationId, DatabaseValues.ServerSync.TYPE_CONVERSATION, DatabaseValues.ServerSync.SERVER_CHAT);
            serverSyncManager.addSyncItem(syncItem);
            return;
        }

        // send conversation through chat server
        chatServerManager.startConversation(myId, conversationId, checkedChatAttendeeIds);

        timerType = TIMER_TYPE_CONVERSATION;
        timer.start();
        isRunning = true;

        Conversation conversation = new Conversation(conversationId, myId, conversationTitle, null, null);
        conversationManager.notifyListenersNewConversation(conversation, 0);

        startChatRoomActivity(0, 0, false, conversationId, myId);
        finish();
    }

    private void createEventChat() {
        if (checkedChatAttendeeIds.size() <= 1) { //myID should always be in the list
            Toast.makeText(this, R.string.error_chat_participant, Toast.LENGTH_LONG).show();
            return;
        }
        String conversationTitle = titleInput.getText().toString();
        if (conversationTitle.isEmpty()) {
            Toast.makeText(this, R.string.error_chat_title, Toast.LENGTH_LONG).show();
            return;
        }
        removeNonFriendAttendees(event.attendees);

        //save provider event to local database
        event.id = saveProviderEventToDB(event);
        if (event.id == null) {
            Toast.makeText(this, R.string.error_saving_chat, Toast.LENGTH_LONG).show();
            return;
        }

        //save conversation to local database
        saveEventConversationToDB(conversationTitle, event);

        //create event on http server
        confirmationId = httpServerManager.createEvent(event.id, event.type, event.title,
                event.location == null ? null : event.location.name, event.startTime, event.endTime, Integer.valueOf(myId), event.createTime, event.getAttendeeIdList());

        if (confirmationId == null) { //handle when http server did not create event: save event and conversation and put them in server sync queue
            //add event to sync queue
            ServerSyncItem syncItem = new ServerSyncItem(
                    event.id, DatabaseValues.ServerSync.TYPE_EVENT, DatabaseValues.ServerSync.SERVER_HTTP);
            serverSyncManager.addSyncItem(syncItem);
            addEventConversationToSyncQueue(event);
            Toast.makeText(this, R.string.network_error_sync_text, Toast.LENGTH_LONG).show();
        } else {
            //update eventId with server generated eventId
            eventManager.updateEventId(event.id, confirmationId);
            event.id = confirmationId;

            //handle when http server did not create event_conversation: save event_conversation to database and add to sync queue
            if (!httpServerManager.createEventConversation(confirmationId, conversationId, conversationTitle, Integer.valueOf(myId), checkedChatAttendeeIds)) {
                addEventConversationToSyncQueue(event);
                Toast.makeText(this, R.string.network_error_sync_text, Toast.LENGTH_LONG).show();
            } else {
                //send EventConversation through chat server and starts count down timer to add to sync queue
                chatServerManager.startEventConversation(myId, confirmationId, checkedChatAttendeeIds);
                timerType = TIMER_TYPE_EVENT_CONVERSATION;
                timer.start();
                isRunning = true;
            }
        }
        Conversation conversation = new Conversation(conversationId, confirmationId, myId, conversationTitle, null, null);
        conversationManager.notifyListenersNewConversation(conversation, 0);

        startChatRoomActivity(event.startTime, event.endTime, event.allDay, conversationId, myId);
        finish();
    }

    private void saveEventConversationToDB (String conversationTitle, Event event) {
        conversationId = conversationManager.getUniqueConversationId();
        if (conversationDataSource.createEventConversation(
                event.id,
                conversationId,
                conversationTitle,
                myId,
                checkedChatAttendeeIds,
                true
        )) {
            Conversation conversation = conversationDataSource.getConversation(conversationId, false, false);
            conversationManager.notifyListenersNewConversation(conversation, 0);
        } else {
            Toast.makeText(this, R.string.error_saving_chat, Toast.LENGTH_LONG).show();
        }
    }

    private void saveConversationToDB (String title, String creatorId, List<String> attendeesId) {
        conversationId = conversationManager.getUniqueConversationId();
        conversationDataSource.createConversation(
                conversationId,
                title,
                creatorId,
                attendeesId,
                true

        );
        confirmationId = conversationId;
    }

    private void addEventConversationToSyncQueue (Event event) {
        ServerSyncItem syncItem = new ServerSyncItem(
                conversationId, DatabaseValues.ServerSync.TYPE_EVENT_CONVERSATION, DatabaseValues.ServerSync.SERVER_HTTP);
        serverSyncManager.addSyncItem(syncItem);
        syncItem = new ServerSyncItem(
                event.id, DatabaseValues.ServerSync.TYPE_EVENT_CONVERSATION, DatabaseValues.ServerSync.SERVER_CHAT);
        serverSyncManager.addSyncItem(syncItem);
    }

    private void addCheckBoxFromAttendee (Attendee attendee) {
        if (listChatAttendeeIds.contains(attendee.id)) { //do not add duplicate attendees
            Toast.makeText(this, R.string.error_duplicate_attendee, Toast.LENGTH_SHORT).show();
            return;
        }

        if (attendee.id.equals(myId)) { //add user self to list but do not show
            listChatAttendeeIds.add(myId);
            checkedChatAttendeeIds.add(myId);
            return;
        }

        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        CheckBox checkBox = new CheckBox(this);
        checkBox.setLayoutParams(params);
        checkBox.setId(Integer.valueOf(attendee.id));
        checkBox.setText(attendee.toString());

        if (attendee.isFriend) {
            checkBox.setChecked(true);
            checkBox.setEnabled(true);
            checkedChatAttendeeIds.add(attendee.id);
        } else {
            checkBox.setChecked(false);
            checkBox.setEnabled(false);
        }
        listChatAttendeeIds.add(attendee.id);
        checkBox.setOnCheckedChangeListener(checkedChangeListener);
        checkBoxLayout.addView(checkBox);
    }

    private void startChatRoomActivity(long startTime, long endTime, boolean isAllDay, String conversationId, String accountId) {
        Intent intent = new Intent(this, ChatRoomActivity.class);
        intent.putExtra(ChatRoomActivity.EVENT_START_TIME, startTime);
        intent.putExtra(ChatRoomActivity.EVENT_END_TIME, endTime);
        intent.putExtra(ChatRoomActivity.EVENT_ALL_DAY, isAllDay);
        intent.putExtra(ChatRoomActivity.CONVERSATION_ID, conversationId);
        intent.putExtra(ChatRoomActivity.MY_ID, accountId);

        startActivity(intent);
    }



    private void replaceWithDatabaseAttendees (List<Attendee> attendeeList, AttendeeDataSource dataSource) {
        for (int i = 0; i < attendeeList.size(); i++) {
            Attendee dbAttendee = dataSource.getAttendeeByEmail(attendeeList.get(i).email);
            if (dbAttendee != null) {
                attendeeList.set(i, dbAttendee);
            }
        }
    }

    private void removeNonFriendAttendees (List<Attendee> attendeeList) {
        Iterator<Attendee> iterator = attendeeList.iterator();
        while (iterator.hasNext()) {
            if (!iterator.next().isFriend) {
                iterator.remove();
            }
        }
    }

    private String saveProviderEventToDB(final Event event) {
        if (event.source == Event.SOURCE_DATABASE) {
            return event.id;
        }
        Event eventCopy = new Event(event);
        eventCopy.id = null;
        eventCopy.providerId = 0;
        eventCopy.reminders.clear();
        eventCopy.source = Event.SOURCE_DATABASE;
        eventCopy.color = Colors.getColor(this, R.color.green);
        EventDataSource eventDataSource = DatabaseHelper.getDataSource(this, EventDataSource.class);
        List<Event> dbEvents = eventDataSource.getEvents(eventCopy.startTime, eventCopy.endTime);
        if (dbEvents.isEmpty()) {
            return eventManager.createEvent(EventManager.EventAction.ACTOR_NONE, eventCopy, null);
        } else {
            Event localEvent = null;
            for (Event dbEvent : dbEvents) {
                if (eventCopy.startTime == dbEvent.startTime && eventCopy.endTime == dbEvent.endTime && eventCopy.title.equals(dbEvent.title)) {
                    localEvent = dbEvent;
                    break;
                }
            }
            if (localEvent == null) {
                return eventManager.createEvent(EventManager.EventAction.ACTOR_NONE, eventCopy, null);
            } else {
                return localEvent.id;
            }
        }

    }

    private void onContactSubmit (EditText editText) {

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

                //add existing SuerCaly user to checked list
                Attendee user = conversationManager.getUserById(id);
                if (user != null) {
                    addCheckBoxFromAttendee(user);
                    continue;
                }

                //non-registered user: save user in http server first
                user = new Attendee(id);
                user.email = emails[0];

                String[] phones = contact.getPhones();
                if (phones != null && phones.length > 0) {
                    user.phoneNumber = phones[0];
                }

                user.firstName = contact.firstName;
                user.lastName = contact.lastName;

                if (contact.displayName != null && !contact.displayName.isEmpty()) {
                    user.userName = contact.displayName;
                } else {
                    user.userName = user.firstName + user.lastName;
                }

                user.isFriend = true;

                int responsCode = httpServerManager.createUser(
                        user.email,
                        user.firstName,
                        user.email,
                        user.lastName,
                        user.mediaId,
                        user.phoneNumber,
                        user.userName,
                        DEFAULT_USER_PASSWORD );

                switch (responsCode) {
                    case HttpServerManager.STATUS_EXCEPTION: //cannot connect with server
                        //TODO: put contact in server sync queue
                        continue;
                    case HttpServerManager.STATUS_ERROR: //user with email or phone already in server database
                        JSONObject object = httpServerManager.getUserByEmail(user.email);
                        try {
                            user.id = object.getString(HttpServerManager.UID);
                        } catch (JSONException e) {
                            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        break;
                    default: //user created on http server; returns userId
                        user.id = String.valueOf(responsCode);
                        break;
                }

                if (!conversationManager.saveUserToDB(user)) {
                    Toast.makeText(this, R.string.error_create_attendee, Toast.LENGTH_SHORT).show();
                    continue;
                }
                addCheckBoxFromAttendee(user);
            }
        }
    }

    @Override
    public void onHandleChatAck(String id) {
        if (isRunning && id.equals(confirmationId)) {
            timer.cancel();
            isRunning = false;
        }
    }
}
