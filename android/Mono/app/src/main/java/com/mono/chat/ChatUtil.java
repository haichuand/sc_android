package com.mono.chat;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mono.EventManager;
import com.mono.R;
import com.mono.db.DatabaseHelper;
import com.mono.db.DatabaseValues;
import com.mono.db.dao.AttendeeDataSource;
import com.mono.db.dao.ConversationDataSource;
import com.mono.db.dao.EventDataSource;
import com.mono.model.Account;
import com.mono.model.Attendee;
import com.mono.model.AttendeeUsernameComparator;
import com.mono.model.Conversation;
import com.mono.model.Event;
import com.mono.model.Message;
import com.mono.model.ServerSyncItem;
import com.mono.network.ChatServerManager;
import com.mono.network.HttpServerManager;
import com.mono.network.ServerSyncManager;
import com.mono.util.Colors;
import com.mono.util.Common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Created by haichuand on 6/21/2016.
 */
public class ChatUtil {
    private static final char[] randomIdCharPool = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".toCharArray();
    private static final long TIMER_TIMEOUT_MS = 5000;
    private static Random random = new Random();
    private static final int randomIdLength = 8;

    private static ChatUtil instance;
    private Context context;
    private CountDownTimer timer;
    private boolean isRunning = false; //timer running flag
    private Dialog dialog;
    private String conversationId;
    private String eventServerId;
    private String myId;
    private List<String> checkedChatAttendeeIds;
    private HttpServerManager httpServerManager;
    private ChatServerManager chatServerManager;
    private ServerSyncManager serverSyncManager;
    private EventManager eventManager;
    private ConversationManager conversationManager;

    private ChatUtil() {}

    private ChatUtil(Context context) {
        this.context = context;
        timer = new CountDownTimer(TIMER_TIMEOUT_MS, TIMER_TIMEOUT_MS) {
            @Override
            public void onTick(long l) {}

            @Override
            public void onFinish() {
                isRunning = false;
                //save to sync queue
                ServerSyncItem syncItem = new ServerSyncItem(eventServerId, DatabaseValues.ServerSync.TYPE_EVENT_CONVERSATION, DatabaseValues.ServerSync.SERVER_CHAT);
                serverSyncManager.addSyncItem(syncItem);
                Toast.makeText(ChatUtil.this.context, R.string.network_error_sync_text, Toast.LENGTH_LONG).show();
            }
        };
        httpServerManager = HttpServerManager.getInstance(context);
        chatServerManager = ChatServerManager.getInstance(context);
        serverSyncManager = ServerSyncManager.getInstance(context);
        eventManager = EventManager.getInstance(context);
        conversationManager = ConversationManager.getInstance(context);
    }

    public static ChatUtil getInstance (Context context) {
        if (instance == null) {
            instance = new ChatUtil(context);
        }

        return instance;
    }

    public void showCreateChatDialog (final Account account, final Event event) {
        if ("UTC".equals(event.timeZone)) { //holiday, all day event
            event.startTime = Common.convertHolidayMillis(event.startTime);
            event.endTime = event.startTime;
        }
        final AttendeeDataSource attendeeDataSource = DatabaseHelper.getDataSource(context, AttendeeDataSource.class);
        replaceWithDatabaseAttendees(event.attendees, attendeeDataSource);
        myId = account.id + "";
        dialog = new Dialog(context);

        dialog.setContentView(R.layout.dialog_create_chat);
        final EditText titleInput = (EditText) dialog.findViewById(R.id.create_chat_title_input);
        titleInput.setText(event.title, TextView.BufferType.EDITABLE);
        final LinearLayout checkBoxLayout = (LinearLayout) dialog.findViewById(R.id.create_chat_attendees);

        checkedChatAttendeeIds = new ArrayList<>(); //attendees with checkbox checked
        final List<String> listChatAttendeeIds = new ArrayList<>(); //all attendees in the checkbox list

        final CompoundButton.OnCheckedChangeListener checkedChangeListener = new CompoundButton.OnCheckedChangeListener() {
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
        addCheckBoxFromAttendee(checkBoxLayout, me, checkedChangeListener, listChatAttendeeIds, checkedChatAttendeeIds, myId, attendeeDataSource);
        //add other event attendees
        for (Attendee attendee : event.attendees) {
            addCheckBoxFromAttendee(checkBoxLayout, attendee, checkedChangeListener, listChatAttendeeIds, checkedChatAttendeeIds, myId, attendeeDataSource);
        }

        //set AutoCompleteTextView to show all users
        final AutoCompleteTextView addAttendeeTextView = (AutoCompleteTextView) dialog.findViewById(R.id.create_chat_add_attendees);
        final List<Attendee> allUsersList = conversationManager.getAllUserList();
        Collections.sort(allUsersList, new AttendeeUsernameComparator());
        List<String> attendeeStringList = getAttendeeStringtWithNameAndEmail(allUsersList);
        ArrayAdapter<String> addAttendeeAdapter = new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, attendeeStringList);
        addAttendeeTextView.setInputType(InputType.TYPE_NULL);
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
                    Toast.makeText(context, "User already in list", Toast.LENGTH_SHORT).show();
                } else {
                    addCheckBoxFromAttendee(checkBoxLayout, attendee, checkedChangeListener, listChatAttendeeIds, checkedChatAttendeeIds, myId, attendeeDataSource);
                }
                addAttendeeTextView.setText("");
            }
        });

        //set listeners for Create and Cancel buttons
        Button createButton = (Button) dialog.findViewById(R.id.create_chat_create_button);
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkedChatAttendeeIds.size() <= 1) { //myID should always be in the list
                    Toast.makeText(context, R.string.error_chat_participant, Toast.LENGTH_LONG).show();
                    return;
                }
                String conversationTitle = titleInput.getText().toString();
                if (conversationTitle.isEmpty()) {
                    Toast.makeText(context, R.string.error_chat_title, Toast.LENGTH_LONG).show();
                    return;
                }
                removeNonFriendAttendees(event.attendees);

                //save provider event to local database
                event.id = saveProviderEventToDB(event);
                if (event.id == null) {
                    Toast.makeText(context, R.string.error_saving_chat, Toast.LENGTH_LONG).show();
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
                    Toast.makeText(context, R.string.network_error_sync_text, Toast.LENGTH_LONG).show();
                } else {
                    //update eventId with server generated eventId
                    eventManager.updateEventId(event.id, eventServerId);
                    event.id = eventServerId;

                    //handle when http server did not create event_conversation: save event_conversation to database and add to sync queue
                    if (!httpServerManager.createEventConversation(eventServerId, conversationId, conversationTitle, Integer.valueOf(myId), checkedChatAttendeeIds)) {
                        addEventConversationToSyncQueue(event);
                        Toast.makeText(context, R.string.network_error_sync_text, Toast.LENGTH_LONG).show();
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
        ConversationDataSource conversationDataSource = DatabaseHelper.getDataSource(context, ConversationDataSource.class);
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
            Toast.makeText(context, R.string.error_saving_chat, Toast.LENGTH_LONG).show();
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

    private void addCheckBoxFromAttendee (LinearLayout checkBoxLayout, Attendee attendee, CompoundButton.OnCheckedChangeListener checkedChangeListener, List<String> attendeeIdList, List<String> checkedAttendeeIdList, String myId, AttendeeDataSource attendeeDataSource) {
        if (attendeeIdList.contains(attendee.id)) { //do not add duplicate attendees
            return;
        }

        if (attendee.id.equals(myId)) { //add user self to list but do not show
            attendeeIdList.add(myId);
            checkedAttendeeIdList.add(myId);
            return;
        }

        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        CheckBox checkBox = new CheckBox(context);
        checkBox.setLayoutParams(params);
        checkBox.setId(Integer.valueOf(attendee.id));
        checkBox.setText(attendee.toString());

        if (attendee.isFriend) {
            checkBox.setChecked(true);
            checkBox.setEnabled(true);
            checkedAttendeeIdList.add(attendee.id);
        } else {
            checkBox.setChecked(false);
            checkBox.setEnabled(false);
        }
        attendeeIdList.add(attendee.id);
        checkBox.setOnCheckedChangeListener(checkedChangeListener);
        checkBoxLayout.addView(checkBox);
    }

    public void startChatRoomActivity(String eventId, long startTime, long endTime, boolean isAllDay, String conversationId, String accountId) {
        Intent intent = new Intent(context, ChatRoomActivity.class);
        intent.putExtra(ChatRoomActivity.EVENT_ID, eventId);
//        intent.putExtra(ChatRoomActivity.EVENT_NAME, eventTitle);
        intent.putExtra(ChatRoomActivity.EVENT_START_TIME, startTime);
        intent.putExtra(ChatRoomActivity.EVENT_END_TIME, endTime);
        intent.putExtra(ChatRoomActivity.EVENT_ALL_DAY, isAllDay);
        intent.putExtra(ChatRoomActivity.CONVERSATION_ID, conversationId);
        intent.putExtra(ChatRoomActivity.MY_ID, accountId);

        context.startActivity(intent, null);
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
        eventCopy.internalId = 0;
        eventCopy.reminders.clear();
        eventCopy.source = Event.SOURCE_DATABASE;
        eventCopy.color = Colors.getColor(context, R.color.green);
        EventDataSource eventDataSource = DatabaseHelper.getDataSource(context, EventDataSource.class);
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

    public void handleEventConversationAck (String eventId) {
        if (isRunning && eventId.equals(eventServerId)) {
            timer.cancel();
            isRunning = false;
        }
    }
}
