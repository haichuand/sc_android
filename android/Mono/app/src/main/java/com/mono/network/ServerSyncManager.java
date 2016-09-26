package com.mono.network;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import com.mono.chat.AttachmentPanel;
import com.mono.db.DatabaseHelper;
import com.mono.db.DatabaseValues;
import com.mono.db.dao.ConversationDataSource;
import com.mono.db.dao.ServerSyncDataSource;
import com.mono.model.Message;
import com.mono.model.ServerSyncItem;
import com.mono.util.Common;

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
    private ConversationDataSource conversationDataSource;
    private ConcurrentLinkedQueue<ServerSyncItem> syncQueue;
    private ServerSyncItem lastSyncItem;
    private long lastSyncTime;
    private static final long SYNC_WAIT_TIME = 10000; //time to wait before syncing the same item

    private ServerSyncManager () {}

    private ServerSyncManager (Context context) {
        appContext = context.getApplicationContext();
        isNetworkStateReceiver = true;
        syncDataSource = DatabaseHelper.getDataSource(appContext, ServerSyncDataSource.class);
        httpServerManager = HttpServerManager.getInstance(appContext);
        chatServerManager = ChatServerManager.getInstance(appContext);
        conversationDataSource = DatabaseHelper.getDataSource(appContext, ConversationDataSource.class);
        syncQueue = new ConcurrentLinkedQueue<>(syncDataSource.getAllSyncItems());
    }

    public static ServerSyncManager getInstance (Context context) {
        if (instance == null) {
            instance = new ServerSyncManager(context);
        }
        return instance;
    }

    /**
     * Enable NetworkStateReceiver if it's disabled
     */
    public void disableNetworkStateReceiver () {
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
     * Disable NetworkStateReceiver if it's enabled
     */
    public void enableNetworkStateReceiver () {
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

        //prevent same message sent multiple times because of frequent network state changes
        if (lastSyncItem == syncItem && System.currentTimeMillis() - lastSyncTime < SYNC_WAIT_TIME) {
            return;
        }
        switch (syncItem.itemType) {
            case DatabaseValues.ServerSync.TYPE_CONVERSATION:
                break;
            case DatabaseValues.ServerSync.TYPE_EVENT:
                break;
            case DatabaseValues.ServerSync.TYPE_EVENT_CONVERSATION:
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
        Message message = conversationDataSource.getMessageByMessageId(syncItem.itemId);
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
                        } else {
                            lastSyncItem = syncItem;
                            lastSyncTime = System.currentTimeMillis();
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
                conversationDataSource.getConversationAttendeesIds(message.getConversationId()),
                message.getMessageText(),
                String.valueOf(message.getMessageId()),
                attachments
        );
        lastSyncItem = syncItem;
        lastSyncTime = System.currentTimeMillis();
    }

    /**
     * Call back method when an ack message is receive by MyGcmListenerService. Check if the message
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
}
