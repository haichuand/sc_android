package com.mono.db.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.support.annotation.NonNull;

import com.mono.db.Database;
import com.mono.db.DatabaseValues;
import com.mono.model.ServerSyncItem;
import com.mono.util.Common;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by haichuand on 9/22/2016.
 */

public class ServerSyncDataSource extends DataSource {
    private ServerSyncDataSource (Database database) {
        super(database);
    }

    public boolean addSyncItem (ServerSyncItem item) {
        ContentValues values = new ContentValues();
        values.put(DatabaseValues.ServerSync.ITEM_ID, item.itemId);
        values.put(DatabaseValues.ServerSync.ITEM_TYPE, item.itemType);
        values.put(DatabaseValues.ServerSync.SERVER, item.server);

        try {
            database.insert(DatabaseValues.ServerSync.TABLE,values);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean removeSyncItem (ServerSyncItem item) {
        int rowsDeleted = database.delete(
                DatabaseValues.ServerSync.TABLE,
                DatabaseValues.ServerSync.ITEM_ID + "=? AND" + DatabaseValues.ServerSync.ITEM_TYPE + "=?",
                new String[] {
                        item.itemId,
                        item.itemType
                }
        );

        return rowsDeleted == 1;
    }

    public List<ServerSyncItem> getAllSyncItems () {
        List<ServerSyncItem> itemList = new ArrayList<>();

        String[] projection = new String[] {
                DatabaseValues.ServerSync.ITEM_ID,
                DatabaseValues.ServerSync.ITEM_TYPE,
                DatabaseValues.ServerSync.SERVER
        };

        String query =
                " SELECT " + Common.implode(", ", projection) +
                " FROM " + DatabaseValues.ServerSync.TABLE +
                " ORDER BY " + DatabaseValues.ServerSync.ID;

        Cursor cursor = database.rawQuery(query);

        while (cursor.moveToNext()) {
            ServerSyncItem item = new ServerSyncItem(
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getShort(2)
            );
            itemList.add(item);
        }

        return itemList;
    }
}
