package com.mono.db.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;

import com.mono.db.Database;
import com.mono.db.DatabaseValues;
import com.mono.model.Event;
import com.mono.model.Location;
import com.mono.util.Common;

import java.util.ArrayList;
import java.util.List;

public class EventDataSource extends DataSource {

    private EventDataSource(Database database) {
        super(database);
    }

    public long createEvent(long externalId, String title, String description, String location,
            int color, long startTime, long endTime, long createTime, String type) {
        long id = -1;

        ContentValues values = new ContentValues();
        values.put(DatabaseValues.Event.EXTERNAL_ID, externalId);
        values.put(DatabaseValues.Event.TITLE, title);
        values.put(DatabaseValues.Event.DESC, description);
        values.put(DatabaseValues.Event.LOCATION, location);
        values.put(DatabaseValues.Event.COLOR, color);
        values.put(DatabaseValues.Event.START_TIME, startTime);
        values.put(DatabaseValues.Event.END_TIME, endTime);
        values.put(DatabaseValues.Event.CREATE_TIME, createTime);
        values.put(DatabaseValues.Event.TYPE, type);

        try {
            id = database.insert(DatabaseValues.Event.TABLE, values);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return id;
    }

    public Event getEvent(long id) {
        Event event = null;

        Cursor cursor = database.select(
            DatabaseValues.Event.TABLE,
            DatabaseValues.Event.PROJECTION,
            DatabaseValues.Event.ID + " = ?",
            new String[]{
                String.valueOf(id)
            }
        );

        if (cursor.moveToNext()) {
            event = cursorToEvent(cursor);
        }

        cursor.close();

        return event;
    }

    public boolean containsEventByExternalId(long externalId) {
        boolean status = false;

        Cursor cursor = database.select(
            DatabaseValues.Event.TABLE,
            new String[]{
                "1"
            },
            DatabaseValues.Event.EXTERNAL_ID + " = ?",
            new String[]{
                String.valueOf(externalId)
            }
        );

        if (cursor.moveToNext()) {
            status = true;
        }

        cursor.close();

        return status;
    }

    public List<Long> containsEventsByExternalIds(long... externalIds) {
        List<Long> result = new ArrayList<>();

        int count = externalIds.length;

        String[] selection = new String[count];
        String[] selectionArgs = new String[count];
        for (int i = 0; i < count; i++) {
            selectionArgs[i] = String.valueOf(externalIds[i]);
        }

        Cursor cursor = database.select(
            DatabaseValues.Event.TABLE,
            new String[]{
                DatabaseValues.Event.EXTERNAL_ID
            },
            DatabaseValues.Event.EXTERNAL_ID + " IN (" + Common.implode(", ", selection) + ")",
            selectionArgs
        );

        while (cursor.moveToNext()) {
            long externalId = cursor.getInt(0);
            result.add(externalId);
        }

        cursor.close();

        return result;
    }

    public Event getEventByExternalId(long externalId) {
        Event event = null;

        Cursor cursor = database.select(
            DatabaseValues.Event.TABLE,
            DatabaseValues.Event.PROJECTION,
            DatabaseValues.Event.EXTERNAL_ID + " = ?",
            new String[]{
                String.valueOf(externalId)
            }
        );

        if (cursor.moveToNext()) {
            event = cursorToEvent(cursor);
        }

        cursor.close();

        return event;
    }

    public int removeEvent(long id) {
        return database.delete(
            DatabaseValues.Event.TABLE,
            DatabaseValues.Event.ID + " = ?",
            new String[]{
                String.valueOf(id)
            }
        );
    }

    public List<Event> getEvents(long startTime, int limit) {
        List<Event> events = new ArrayList<>();

        Cursor cursor = database.select(
            DatabaseValues.Event.TABLE,
            DatabaseValues.Event.PROJECTION,
            DatabaseValues.Event.START_TIME + " < ?",
            new String[]{
                String.valueOf(startTime)
            },
            null,
            DatabaseValues.Event.START_TIME + " DESC",
            limit
        );

        while (cursor.moveToNext()) {
            Event event = cursorToEvent(cursor);
            events.add(event);
        }

        cursor.close();

        return events;
    }

    public List<Event> getEventsByTimePeriod(long startTime, long endTime) {
        List<Event> events = new ArrayList<>();

        Cursor cursor = database.select(
            DatabaseValues.Event.TABLE,
            DatabaseValues.Event.PROJECTION,
            DatabaseValues.Event.START_TIME + " >= ? AND " +
            DatabaseValues.Event.END_TIME + " <= ?",
            new String[]{
                String.valueOf(startTime),
                String.valueOf(endTime)
            },
            null,
            DatabaseValues.Event.START_TIME + " DESC",
            null
        );

        while (cursor.moveToNext()) {
            Event event = cursorToEvent(cursor);
            events.add(event);
        }

        cursor.close();

        return events;
    }

    /**
     * For PROJECTION only.
     */
    private Event cursorToEvent(Cursor cursor) {
        Event event = new Event(cursor.getLong(DatabaseValues.Event.INDEX_ID));
        event.externalId = cursor.getLong(DatabaseValues.Event.INDEX_EXTERNAL_ID);
        event.title = cursor.getString(DatabaseValues.Event.INDEX_TITLE);
        event.description = cursor.getString(DatabaseValues.Event.INDEX_DESC);
        event.location = new Location(cursor.getString(DatabaseValues.Event.INDEX_LOCATION));
        event.color = cursor.getInt(DatabaseValues.Event.INDEX_COLOR);
        event.startTime = cursor.getLong(DatabaseValues.Event.INDEX_START_TIME);
        event.endTime = cursor.getLong(DatabaseValues.Event.INDEX_END_TIME);
        event.createTime = cursor.getLong(DatabaseValues.Event.INDEX_CREATE_TIME);
        event.type = cursor.getString(DatabaseValues.Event.INDEX_TYPE);

        return event;
    }
}
