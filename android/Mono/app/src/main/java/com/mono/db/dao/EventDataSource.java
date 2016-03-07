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
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventDataSource extends DataSource {

    private EventDataSource(Database database) {
        super(database);
    }

    public long createEvent(long externalId, String type, String title, String description,
            String location, int color, long startTime, long endTime, long createTime) {
        long id = -1;

        ContentValues values = new ContentValues();
        values.put(DatabaseValues.Event.EXTERNAL_ID, externalId);
        values.put(DatabaseValues.Event.TYPE, type);
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

    public int updateValues(long id, ContentValues values) {
        return database.update(
            DatabaseValues.Event.TABLE,
            values,
            DatabaseValues.Event.ID + " = ?",
            new String[]{
                String.valueOf(id)
            }
        );
    }

    public int updateTime(long id, long startTime, long endTime) {
        ContentValues values = new ContentValues();
        values.put(DatabaseValues.Event.START_TIME, startTime);
        values.put(DatabaseValues.Event.END_TIME, endTime);

        return updateValues(id, values);
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

    public long[] getEventIdsByDay(int year, int month, int day) {
        long[] result = null;

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, 0, 0, 0);
        long startTime = calendar.getTimeInMillis();

        calendar.set(year, month, day + 1, 0, 0, 0);
        long endTime = calendar.getTimeInMillis();

        Cursor cursor = database.select(
            DatabaseValues.Event.TABLE,
            new String[]{
                DatabaseValues.Event.ID
            },
            DatabaseValues.Event.START_TIME + " >= ? AND " +
            DatabaseValues.Event.START_TIME + " < ?",
            new String[]{
                String.valueOf(startTime),
                String.valueOf(endTime)
            },
            null,
            DatabaseValues.Event.START_TIME + " DESC",
            null
        );

        int count = cursor.getCount();

        if (count > 0) {
            result = new long[count];

            for (int i = 0; i < count; i++) {
                if (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    result[i] = id;
                }
            }
        }

        cursor.close();

        return result;
    }

    public Map<Integer, Long[]> getEventIdsByMonth(int year, int month) {
        Map<Integer, Long[]> result = new HashMap<>();

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, 1, 0, 0, 0);
        long startTime = calendar.getTimeInMillis();

        calendar.set(year, month + 1, 1, 0, 0, 0);
        long endTime = calendar.getTimeInMillis();

        Cursor cursor = database.select(
            DatabaseValues.Event.TABLE,
            new String[]{
                "CAST(STRFTIME('%d', " + DatabaseValues.Event.START_TIME + " / 1000, 'UNIXEPOCH', 'LOCALTIME') AS INTEGER) AS `day`",
                "GROUP_CONCAT(" + DatabaseValues.Event.ID + ")"
            },
            DatabaseValues.Event.START_TIME + " >= ? AND " +
            DatabaseValues.Event.START_TIME + " < ?",
            new String[]{
                String.valueOf(startTime),
                String.valueOf(endTime)
            },
            "`day`",
            DatabaseValues.Event.START_TIME + " DESC",
            null
        );

        while (cursor.moveToNext()) {
            int day = cursor.getInt(0);

            String[] tempIds = cursor.getString(1).split(",");
            Long[] ids = new Long[tempIds.length];
            for (int i = 0; i < tempIds.length; i++) {
                ids[i] = Long.valueOf(tempIds[i]);
            }

            result.put(day, ids);
        }

        cursor.close();

        return result;
    }

    public Map<Integer, Integer[]> getEventColorsByMonth(int year, int month) {
        Map<Integer, Integer[]> result = new HashMap<>();

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, 1, 0, 0, 0);
        long startTime = calendar.getTimeInMillis();

        calendar.set(year, month + 1, 1, 0, 0, 0);
        long endTime = calendar.getTimeInMillis();

        Cursor cursor = database.select(
            DatabaseValues.Event.TABLE,
            new String[]{
                "CAST(STRFTIME('%d', " + DatabaseValues.Event.START_TIME + " / 1000, 'UNIXEPOCH', 'LOCALTIME') AS INTEGER) AS `day`",
                "GROUP_CONCAT(" + DatabaseValues.Event.COLOR + ")"
            },
            DatabaseValues.Event.START_TIME + " >= ? AND " +
            DatabaseValues.Event.START_TIME + " < ?",
            new String[]{
                String.valueOf(startTime),
                String.valueOf(endTime)
            },
            "`day`",
            DatabaseValues.Event.START_TIME + " DESC",
            null
        );

        while (cursor.moveToNext()) {
            int day = cursor.getInt(0);

            String[] tempColors = cursor.getString(1).split(",");
            Integer[] colors = new Integer[tempColors.length];
            for (int i = 0; i < tempColors.length; i++) {
                colors[i] = Integer.valueOf(tempColors[i]);
            }

            result.put(day, colors);
        }

        cursor.close();

        return result;
    }

    /**
     * For PROJECTION only.
     */
    private Event cursorToEvent(Cursor cursor) {
        Event event = new Event(cursor.getLong(DatabaseValues.Event.INDEX_ID));
        event.externalId = cursor.getLong(DatabaseValues.Event.INDEX_EXTERNAL_ID);
        event.type = cursor.getString(DatabaseValues.Event.INDEX_TYPE);
        event.title = cursor.getString(DatabaseValues.Event.INDEX_TITLE);
        event.description = cursor.getString(DatabaseValues.Event.INDEX_DESC);

        String location = cursor.getString(DatabaseValues.Event.INDEX_LOCATION);
        if (location != null) {
            event.location = new Location(location);
        }

        event.color = cursor.getInt(DatabaseValues.Event.INDEX_COLOR);
        event.startTime = cursor.getLong(DatabaseValues.Event.INDEX_START_TIME);
        event.endTime = cursor.getLong(DatabaseValues.Event.INDEX_END_TIME);
        event.createTime = cursor.getLong(DatabaseValues.Event.INDEX_CREATE_TIME);

        return event;
    }
}
