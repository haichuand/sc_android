package com.mono.network;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.widget.Toast;

import com.mono.AccountManager;
import com.mono.EventManager;
import com.mono.R;
import com.mono.chat.AttachmentPanel;
import com.mono.chat.ConversationManager;
import com.mono.chat.CreateChatActivity;
import com.mono.db.DatabaseHelper;
import com.mono.db.DatabaseValues;
import com.mono.db.dao.ConversationDataSource;
import com.mono.db.dao.EventAttendeeDataSource;
import com.mono.db.dao.ServerSyncDataSource;
import com.mono.model.Account;
import com.mono.model.Attendee;
import com.mono.model.Conversation;
import com.mono.model.Event;
import com.mono.model.Message;
import com.mono.model.ServerSyncItem;
import com.mono.util.Common;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by haichuand on 9/22/2016.
 *
 * ServerSyncManager manages saving actions when the user is offline or when there is no response from server,
 * as well as re-sending the actions to server once connection is restored.
 */

public class ServerSyncManager {
    private static ServerSyncManager instance;
    private Context appContext;
    private boolean isNetworkStateReceiver;
    private ServerSyncDataSource syncDataSource;
    private HttpServerManager httpServerManager;
    private ChatServerManager chatServerManager;
    private EventManager eventManager;
    private ConversationManager conversationManager;
//    private ConversationDataSource conversationDataSource;
    private ConcurrentLinkedQueue<ServerSyncItem> syncQueue;
    private ServerSyncItem lastSyncItem;
    private long lastSyncTime;
    private static final long SYNC_WAIT_TIME = 20000; //time to wait before syncing the same item

    private ServerSyncManager () {}

    private ServerSyncManager (Context context) {
        appContext = context.getApplicationContext();
        isNetworkStateReceiver = true;
        syncDataSource = DatabaseHelper.getDataSource(appContext, ServerSyncDataSource.class);
        httpServerManager = HttpServerManager.getInstance(appContext);
        chatServerManager = ChatServerManager.getInstance(appContext);
        eventManager = EventManager.getInstance(appContext);
//        conversationDataSource = DatabaseHelper.getDataSource(appContext, ConversationDataSource.class);
        conversationManager = ConversationManager.getInstance(appContext);
        syncQueue = new ConcurrentLinkedQueue<>(syncDataSource.getAllSyncItems());
    }

    public static ServerSyncManager getInstance (Context context) {
        if (instance == null) {
            instance = new ServerSyncManager(context);
        }
        return instance;
    }

    /**
     * Disable NetworkStateReceiver if it's enabled
     */
    private void disableNetworkStateReceiver () {
        if (isNetworkStateReceiver) {
            ComponentName receiver = new ComponentName(appContext, NetworkStateReceiver.class);
            PackageManager pm = appContext.getPackageManager();
            pm.setComponentEnabledSetting(receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
            isNetworkStateReceiver = false;
        }
    }

    /**
     * Enable NetworkStateReceiver if it's disabled
     */
    private void enableNetworkStateReceiver () {
        if (!isNetworkStateReceiver) {
            ComponentName receiver = new ComponentName(appContext, NetworkStateReceiver.class);
            PackageManager pm = appContext.getPackageManager();
            pm.setComponentEnabledSetting(receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
            isNetworkStateReceiver = true;
        }
    }

    /**
     * Peek at the head of server sync queue items and process it according to item type.
     * Queue head is removed by server call back functions, not here
     */
    public void processServerSyncItems() {
        if (!Common.isConnectedToInternet(appContext)) {
            enableNetworkStateReceiver();
            return;
        }

        if (syncQueue.isEmpty()) {
            disableNetworkStateReceiver();
            return;
        }

        ServerSyncItem syncItem = syncQueue.peek();

        //prevent same item from being sent multiple times because of frequent network state changes and/or onResume() calls
        if (lastSyncItem == syncItem && System.currentTimeMillis() - lastSyncTime < SYNC_WAIT_TIME) {
            return;
        }
        lastSyncItem = syncItem;
        lastSyncTime = System.currentTimeMillis();

        switch (syncItem.itemType) {
            case DatabaseValues.ServerSync.TYPE_CONVERSATION:
                sendConversation(syncItem);
                break;
            case DatabaseValues.ServerSync.TYPE_EVENT:
                sendEvent(syncItem);
                break;
            case DatabaseValues.ServerSync.TYPE_EVENT_CONVERSATION:
                sendEventConversation(syncItem);
                break;
            case DatabaseValues.ServerSync.TYPE_MESSAGE:
                sendConversationMessage (syncItem);
                break;
        }
    }

    /**
     * Add an item to be synced with server in both the database and queue
     * @param syncItem
     */
    public void addSyncItem (ServerSyncItem syncItem) {
        syncDataSource.addSyncItem(syncItem);
        syncQueue.add(syncItem);
        enableNetworkStateReceiver();
    }

    /**
     * Remove the head item of sync queue, then remove corresponding row in database
     */
    private void removeSyncItem() {
        ServerSyncItem headItem = syncQueue.poll();
        if (headItem != null) {
            syncDataSource.removeSyncItem(headItem);
        }
    }

    /**
     * Send a conversation message to chat server
     * @param syncItem
     */
    private void sendConversationMessage (final ServerSyncItem syncItem) {
        Message message = conversationManager.getMessageByMessageId(syncItem.itemId);
        if (message == null) {
            return;
        }

        if (message.attachments == null || message.attachments.isEmpty()) {
            sendConversationMessage(message, null, syncItem);
        } else {
            AttachmentPanel.sendAttachments(
                appContext,
                message,
                new AttachmentPanel.AttachmentsListener() {
                    @Override
                    public void onFinish(Message message, List<String> result) {
                        if (result != null) {
                            sendConversationMessage(message, result, syncItem);
                        }
                    }
                }
            );
        }
    }

    private void sendConversationMessage(Message message, List<String> attachments,
            ServerSyncItem syncItem) {
        chatServerManager.sendConversationMessage(
                message.getSenderId(),
                message.getConversationId(),
                conversationManager.getConversationAttendeesIds(message.getConversationId()),
                message.getMessageText(),
                String.valueOf(message.getMessageId()),
                attachments
        );
    }

    private void sendEvent (ServerSyncItem syncItem) {
        Account account = AccountManager.getInstance(appContext).getAccount();
        if (account == null) {
            return;
        }
        if (syncItem.server == DatabaseValues.ServerSync.SERVER_HTTP) {
            Event event = eventManager.getEvent(syncItem.itemId, true);
            EventAttendeeDataSource eventAttendeeDataSource = DatabaseHelper.getDataSource(appContext, EventAttendeeDataSource.class);
            List<String> attendeesId = eventAttendeeDataSource.getAttendeeIds(syncItem.itemId);
            String eventServerId = httpServerManager.createEvent(
                    syncItem.itemId,
                    event.type,
                    event.title,
                    event.location == null ? null : event.location.name,
                    event.startTime,
                    event.endTime,
                    (int) account.id,
                    System.currentTimeMillis(),
                    attendeesId
            );
            if (eventServerId != null) {
                eventManager.updateEventId(syncItem.itemId, eventServerId);
                updateSyncItems(syncItem.itemId, eventServerId);
                removeSyncItem();
                processServerSyncItems();
            }
        } else {
            //TODO: send event through chat server
        }
    }

    private void sendEventConversation (ServerSyncItem syncItem) {
        Account account = AccountManager.getInstance(appContext).getAccount();
        if (account == null) {
            return;
        }
        Integer myId = (int) account.id;
        //for http server, syncItem.itemId = conversationId; for chat server, syncItem.itemId = eventId
        if (syncItem.server == DatabaseValues.ServerSync.SERVER_HTTP) { //sync with http server
            List<String> attendeeIds = conversationManager.getConversationAttendeesIds(syncItem.itemId);
            Conversation conversation = conversationManager.getConversation(syncItem.itemId, false, false);
            if (httpServerManager.createEventConversation(
                    conversation.eventId,
                    syncItem.itemId,
                    conversation.name,
                    myId,
                    attendeeIds
            )) {
                removeSyncItem();
                processServerSyncItems();
            }
        } else { //send through chat server
            List<Conversation> conversations = conversationManager.getConversations(syncItem.itemId);
            if (conversations.isEmpty()) {
                removeSyncItem();
                processServerSyncItems();
                return;
            }
            Conversation conversation = conversations.get(0);
            List<String> attendeesId = conversationManager.getConversationAttendeesIds(conversation.id);
            chatServerManager.startEventConversation(String.valueOf(myId), syncItem.itemId, attendeesId);
        }
    }

    private void sendConversation (ServerSyncItem syncItem) {
        Account account = AccountManager.getInstance(appContext).getAccount();
        if (account == null) {
            return;
        }
        Integer myId = (int) account.id;

        // syncItem.itemID = conversationId
        if (syncItem.server == DatabaseValues.ServerSync.SERVER_HTTP) { //sync with http server
            List<String> attendeeIds = conversationManager.getConversationAttendeesIds(syncItem.itemId);
            if (!saveUnregisteredAttendees(attendeeIds)) {
                return;
            }
            Conversation conversation = conversationManager.getConversation(syncItem.itemId, false, false);
            if (httpServerManager.createConversation(
                    syncItem.itemId,
                    conversation.name,
                    myId,
                    attendeeIds
            )) {
                removeSyncItem();
                processServerSyncItems();
            }
        } else { //send through chat server
            Conversation conversation = conversationManager.getConversation(syncItem.itemId, false, false);
            if (conversation == null) {
                removeSyncItem();
                processServerSyncItems();
                return;
            }
            List<String> attendeesId = conversationManager.getConversationAttendeesIds(syncItem.itemId);
            chatServerManager.startConversation(String.valueOf(myId), syncItem.itemId, attendeesId);
        }
    }

    private boolean saveUnregisteredAttendees(List<String> attendeesId) {
        for (int i = 0; i< attendeesId.size(); i++) {
            if (attendeesId.get(i).startsWith("-")) {
                Attendee attendee = conversationManager.getUserById(attendeesId.get(i));
                if (attendee == null) {
                    Toast.makeText(appContext, R.string.error_create_chat, Toast.LENGTH_SHORT).show();
                    return false;
                }

                int responseCode = httpServerManager.createUser(
                        attendee.email,
                        attendee.firstName,
                        attendee.email,
                        attendee.lastName,
                        null,
                        null,
                        attendee.userName,
                        ConversationManager.DEFAULT_USER_PASSWORD );

                switch (responseCode) {
                    case HttpServerManager.STATUS_EXCEPTION: //cannot connect with server
                        return false;
                    case HttpServerManager.STATUS_ERROR: //user with email or phone already in server database
                        JSONObject object = httpServerManager.getUserByEmail(attendee.email);
                        try {
                            attendee.id = object.getString(HttpServerManager.UID);
                        } catch (JSONException e) {
                            Toast.makeText(appContext, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            return false;
                        }
                        break;
                    default: //user created on http server; returns userId
                        attendee.id = String.valueOf(responseCode);
                        break;
                }

                if (conversationManager.updateUserId(attendeesId.get(i), attendee.id)) {
                    attendeesId.set(i, attendee.id);
                } else {
                    Toast.makeText(appContext, R.string.error_update_attendee, Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        }
        return true;
    }

    private void updateSyncItems(String originalId, String newId) {
        syncDataSource.updateSyncItems(originalId, newId);
        for (ServerSyncItem syncItem : syncQueue) {
            if (syncItem.itemId.equals(originalId)) {
                syncItem.itemId = newId;
            }
        }
    }

    /**
     * Call back method when an ack message is receive by MyFcmListenerService. Check if the message
     * matches the head item of sync queue. If so, remove the head item and continue processing the next item.
     * @param message
     */
    public void handleAckConversationMessage (Message message) {
        ServerSyncItem headItem = syncQueue.peek();

        if (headItem != null && headItem.itemType.equals(DatabaseValues.ServerSync.TYPE_MESSAGE)
                && headItem.itemId.equals(String.valueOf(message.getMessageId()))) {
            removeSyncItem();
            processServerSyncItems();
        }
    }

    /**
     * Callback to handle EventConversation ack message
     * @param eventId
     */
    public void handleAckEventConversation (String eventId) {
        ServerSyncItem headItem = syncQueue.peek();

        if (headItem != null && headItem.itemType.equals(DatabaseValues.ServerSync.TYPE_EVENT_CONVERSATION)
                && headItem.itemId.equals(eventId)) {
            removeSyncItem();
            processServerSyncItems();
        }
    }

    /**
     * Call back to handle conversation ack message
     * @param conversationId
     */
    public void handleAckConversation (String conversationId) {
        ServerSyncItem headItem = syncQueue.peek();

        if (headItem != null && headItem.itemType.equals(DatabaseValues.ServerSync.TYPE_CONVERSATION)
                && headItem.itemId.equals(conversationId)) {
            removeSyncItem();
            processServerSyncItems();
        }
    }
}
