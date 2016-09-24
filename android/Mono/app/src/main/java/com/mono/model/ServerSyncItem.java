package com.mono.model;

import com.mono.db.DatabaseValues;

/**
 * Created by haichuand on 9/22/2016.
 * Items that need to be synced with server, corresponding with DatabaseValues.ServerSync table
 */

public class ServerSyncItem {
    public String itemId; //must be event, conversation or message primary key
    public String itemType; //must be one from DatabaseValues.ServerSync item type
    public short server; //must be one from DatabaseValues.ServerSync server type

    public ServerSyncItem (String itemId, String itemType, short server) {
        this.itemId = itemId;
        this.itemType = itemType;
        this.server = server;
    }
}
