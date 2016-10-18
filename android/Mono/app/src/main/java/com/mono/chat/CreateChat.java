package com.mono.chat;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.CountDownTimer;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Created by haichuand on 6/21/2016.
 */
public class CreateChat {
    private static final char[] randomIdCharPool = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".toCharArray();
    private static final long TIMER_TIMEOUT_MS = 5000;
    private static Random random = new Random();
    private static final int randomIdLength = 8;
    private static final String DEFAULT_USER_PASSWORD = Common.md5("@SuperCalyUser");

    private static CreateChat instance;
    private FragmentActivity activity;
    private static CountDownTimer timer;
    private static boolean isRunning = false; //timer running flag
    private Dialog dialog;
    private String conversationId;
    private static String eventServerId;
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
    private AttendeeDataSource attendeeDataSource;

    private CreateChat() {}

    private CreateChat(FragmentActivity activity) {
        this.activity = activity;
        timer = new CountDownTimer(TIMER_TIMEOUT_MS, TIMER_TIMEOUT_MS) {
            @Override
            public void onTick(long l) {}

            @Override
            public void onFinish() {
                isRunning = false;
                //save to sync queue
                ServerSyncItem syncItem = new ServerSyncItem(eventServerId, DatabaseValues.ServerSync.TYPE_EVENT_CONVERSATION, DatabaseValues.ServerSync.SERVER_CHAT);
                serverSyncManager.addSyncItem(syncItem);
                Toast.makeText(CreateChat.this.activity, R.string.network_error_sync_text, Toast.LENGTH_LONG).show();
            }
        };
        httpServerManager = HttpServerManager.getInstance(activity);
        chatServerManager = ChatServerManager.getInstance(activity);
        serverSyncManager = ServerSyncManager.getInstance(activity);
        eventManager = EventManager.getInstance(activity);
        conversationManager = ConversationManager.getInstance(activity);
        attendeeDataSource = DatabaseHelper.getDataSource(activity, AttendeeDataSource.class);
    }

    public static CreateChat getInstance (FragmentActivity activity) {
        if (instance == null) {
            instance = new CreateChat(activity);
        }
        return instance;
    }

    public void showCreateChatDialog (final Account account, final Event event) {
        checkedChatAttendeeIds.clear();
        listChatAttendeeIds.clear();

        if ("UTC".equals(event.timeZone)) { //holiday, all day event
            event.startTime = Common.convertHolidayMillis(event.startTime);
            event.endTime = event.startTime;
        }
        final AttendeeDataSource attendeeDataSource = DatabaseHelper.getDataSource(activity, AttendeeDataSource.class);
        replaceWithDatabaseAttendees(event.attendees, attendeeDataSource);
        myId = account.id + "";
        dialog = new Dialog(activity);

        dialog.setContentView(R.layout.dialog_create_chat);
        final EditText titleInput = (EditText) dialog.findViewById(R.id.create_chat_title_input);
        titleInput.setText(event.title, TextView.BufferType.EDITABLE);
        checkBoxLayout = (LinearLayout) dialog.findViewById(R.id.create_chat_attendees);

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


        //add user self to list but does not show
        Attendee me = new Attendee(myId, null, account.email, account.phone, account.firstName, account.lastName, account.username, false, false);
        addCheckBoxFromAttendee(me);
        //add other event attendees
        for (Attendee attendee : event.attendees) {
            if (attendee.id.equals(myId)) {
                continue;
            }
            addCheckBoxFromAttendee(attendee);
        }

/*

        //set AutoCompleteTextView to show all users
        final AutoCompleteTextView addAttendeeTextView = (AutoCompleteTextView) dialog.findViewById(R.id.create_chat_add_attendees);
        final List<Attendee> allUsersList = conversationManager.getAllUserList();
        Collections.sort(allUsersList, new AttendeeUsernameComparator());
        List<String> attendeeStringList = getAttendeeStringtWithNameAndEmail(allUsersList);
        ArrayAdapter<String> addAttendeeAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_dropdown_item_1line, attendeeStringList);
//        addAttendeeTextView.setInputType(InputType.TYPE_NULL);
        addAttendeeTextView.setAdapter(addAttendeeAdapter);
        addAttendeeTextView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    addAttendeeTextView.showDropDown();
                }
            }
        });
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
                if (listChatAttendeeIds.contains(attendee.id)) {
                    Toast.makeText(activity, "User already in list", Toast.LENGTH_SHORT).show();
                } else {
                    addCheckBoxFromAttendee(checkBoxLayout, attendee, checkedChangeListener, listChatAttendeeIds, checkedChatAttendeeIds, myId, attendeeDataSource);
                }
                addAttendeeTextView.setText("");
            }
        });
*/
        //set up contact input and contact picker
        View contactPicker = dialog.findViewById(R.id.contact_picker);
        contactPicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(activity, ContactsActivity.class);
                intent.putExtra(ContactsActivity.EXTRA_MODE, ContactsActivity.MODE_PICKER);
                activity.startActivityForResult(intent, EventDetailsActivity.REQUEST_CONTACT_PICKER);
            }
        });

        final EditText input = (EditText) dialog.findViewById(R.id.input);
        View submit = dialog.findViewById(R.id.submit);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onContactSubmit(input);
            }
        });

        //set listeners for Create and Cancel buttons
        Button createButton = (Button) dialog.findViewById(R.id.create_chat_create_button);
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkedChatAttendeeIds.size() <= 1) { //myID should always be in the list
                    Toast.makeText(activity, R.string.error_chat_participant, Toast.LENGTH_LONG).show();
                    return;
                }
                String conversationTitle = titleInput.getText().toString();
                if (conversationTitle.isEmpty()) {
                    Toast.makeText(activity, R.string.error_chat_title, Toast.LENGTH_LONG).show();
                    return;
                }
                removeNonFriendAttendees(event.attendees);

                //save provider event to local database
                event.id = saveProviderEventToDB(event);
                if (event.id == null) {
                    Toast.makeText(activity, R.string.error_saving_chat, Toast.LENGTH_LONG).show();
                    return;
                }

                //save conversation to local database
                saveEventConversationToDB(conversationTitle, event);

                //create event on http server
                eventServerId = httpServerManager.createEvent(event.id, event.type, event.title,
                        event.location == null ? null : event.location.name, event.startTime, event.endTime, Integer.valueOf(myId), event.createTime, event.getAttendeeIdList());

                if (eventServerId == null) { //handle when http server did not create event: save event and conversation and put them in server sync queue
                    //add event to sync queue
                    ServerSyncItem syncItem = new ServerSyncItem(
                            event.id, DatabaseValues.ServerSync.TYPE_EVENT, DatabaseValues.ServerSync.SERVER_HTTP);
                    serverSyncManager.addSyncItem(syncItem);
                    addEventConversationToSyncQueue(event);
                    Toast.makeText(activity, R.string.network_error_sync_text, Toast.LENGTH_LONG).show();
                } else {
                    //update eventId with server generated eventId
                    eventManager.updateEventId(event.id, eventServerId);
                    event.id = eventServerId;

                    //handle when http server did not create event_conversation: save event_conversation to database and add to sync queue
                    if (!httpServerManager.createEventConversation(eventServerId, conversationId, conversationTitle, Integer.valueOf(myId), checkedChatAttendeeIds)) {
                        addEventConversationToSyncQueue(event);
                        Toast.makeText(activity, R.string.network_error_sync_text, Toast.LENGTH_LONG).show();
                    } else {
                        //send EventConversation through chat server and starts count down timer to add to sync queue
                        chatServerManager.startEventConversation(myId, eventServerId, checkedChatAttendeeIds);
                        timer.start();
                        isRunning = true;
                    }
                }

                dialog.dismiss();
                startChatRoomActivity(event.id, event.startTime, event.endTime, event.allDay, conversationId, myId);
            }
        });
        Button cancelButton = (Button) dialog.findViewById(R.id.create_chat_cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //dismiss only if the countdown timer is not running
                if (!isRunning) {
                    dialog.dismiss();
                }
            }
        });
        dialog.show();
    }

    private void saveEventConversationToDB (String conversationTitle, Event event) {
        ConversationDataSource conversationDataSource = DatabaseHelper.getDataSource(activity, ConversationDataSource.class);
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
            Toast.makeText(activity, R.string.error_saving_chat, Toast.LENGTH_LONG).show();
        }
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
            Toast.makeText(activity, R.string.error_duplicate_attendee, Toast.LENGTH_SHORT).show();
            return;
        }

        if (attendee.id.equals(myId)) { //add user self to list but do not show
            listChatAttendeeIds.add(myId);
            checkedChatAttendeeIds.add(myId);
            return;
        }

        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        CheckBox checkBox = new CheckBox(activity);
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

    public void startChatRoomActivity(String eventId, long startTime, long endTime, boolean isAllDay, String conversationId, String accountId) {
        Intent intent = new Intent(activity, ChatRoomActivity.class);
        intent.putExtra(ChatRoomActivity.EVENT_ID, eventId);
//        intent.putExtra(ChatRoomActivity.EVENT_NAME, eventTitle);
        intent.putExtra(ChatRoomActivity.EVENT_START_TIME, startTime);
        intent.putExtra(ChatRoomActivity.EVENT_END_TIME, endTime);
        intent.putExtra(ChatRoomActivity.EVENT_ALL_DAY, isAllDay);
        intent.putExtra(ChatRoomActivity.CONVERSATION_ID, conversationId);
        intent.putExtra(ChatRoomActivity.MY_ID, accountId);

        activity.startActivity(intent, null);
    }

    public static List<String> getAttendeeStringtWithNameAndEmail (List<Attendee> attendeeList) {
        if (attendeeList == null) {
            return null;
        }
        List<String> attendeeStringList = new ArrayList<>();
        for (Attendee attendee : attendeeList) {
            String str = "";
            if (attendee.firstName != null && !attendee.firstName.isEmpty()) {
                str += attendee.firstName;
                if (attendee.lastName != null && !attendee.lastName.isEmpty()) {
                    str += (" " + attendee.lastName);
                }
            } else if (attendee.userName != null && !attendee.userName.isEmpty()) {
                str += attendee.userName;
            }
            if (attendee.email != null && !attendee.email.isEmpty()) {
                str += (" (" + attendee.email + ")");
            } else {
                str += (" (" + attendee.phoneNumber + ")");
            }
            attendeeStringList.add(str);
        }

        return attendeeStringList;
    }

    public static String getRandomId () {
        String str = "";
        for (int i = 0; i < randomIdLength; i++) {
            str += randomIdCharPool[random.nextInt(randomIdCharPool.length)];
        }
        return str;
    }

    public void replaceWithDatabaseAttendees (List<Attendee> attendeeList, AttendeeDataSource dataSource) {
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
        eventCopy.color = Colors.getColor(activity, R.color.green);
        EventDataSource eventDataSource = DatabaseHelper.getDataSource(activity, EventDataSource.class);
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

    public static void handleEventConversationAck (String eventId) {
        if (instance != null && isRunning && eventId.equals(eventServerId)) {
            timer.cancel();
            isRunning = false;
        }
    }

    private void onContactSubmit (EditText editText) {

    }

    public void handleContactPickerResult (int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            List<Contact> contacts =
                    data.getParcelableArrayListExtra(ContactsActivity.EXTRA_CONTACTS);

            for (Contact contact : contacts) {
                String[] emails = contact.getEmails();
                if (emails == null || emails.length == 0) {
                    Toast.makeText(activity, R.string.error_no_email, Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(activity, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        break;
                    default: //user created on http server; returns userId
                        user.id = String.valueOf(responsCode);
                        break;
                }

                if (!conversationManager.saveUserToDB(user)) {
                    Toast.makeText(activity, R.string.error_create_attendee, Toast.LENGTH_SHORT).show();
                    continue;
                }
                addCheckBoxFromAttendee(user);
            }
        }
    }
}
