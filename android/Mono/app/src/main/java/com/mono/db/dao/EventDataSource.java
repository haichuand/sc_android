package com.mono.db.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;

import com.mono.db.Database;
import com.mono.db.DatabaseValues;
import com.mono.model.Event;
import com.mono.model.Location;

import java.util.ArrayList;
import java.util.List;

public class EventDataSource extends DataSource {

    private EventDataSource(Database database) {
        super(database);
    }

    public long createEvent(long externalId, String title, String description, String location,
            int color, long startTime, long endTime, long createTime) {
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

        return event;
    }
}
