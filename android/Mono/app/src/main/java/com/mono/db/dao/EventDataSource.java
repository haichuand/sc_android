package com.mono.db.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;

import com.mono.db.Database;
import com.mono.db.DatabaseValues;
import com.mono.model.Event;
import com.mono.model.Location;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class EventDataSource extends DataSource {

    private EventDataSource(Database database) {
        super(database);
    }

    public String createEvent(long calendarId, long internalId, String externalId, String type,
            String title, String description, String location, int color, long startTime,
            long endTime, String timeZone, String endTimeZone, int allDay, long createTime) {
        String id = DataSource.UniqueIdGenerator(this.getClass().getSimpleName());

        ContentValues values = new ContentValues();
        values.put(DatabaseValues.Event.ID, id);
        values.put(DatabaseValues.Event.CALENDAR_ID, calendarId);
        values.put(DatabaseValues.Event.INTERNAL_ID, internalId);
        values.put(DatabaseValues.Event.EXTERNAL_ID, externalId);
        values.put(DatabaseValues.Event.TYPE, type);
        values.put(DatabaseValues.Event.TITLE, title);
        values.put(DatabaseValues.Event.DESC, description);
        values.put(DatabaseValues.Event.LOCATION, location);
        values.put(DatabaseValues.Event.COLOR, color);
        values.put(DatabaseValues.Event.START_TIME, startTime);
        values.put(DatabaseValues.Event.END_TIME, endTime);
        values.put(DatabaseValues.Event.TIMEZONE, timeZone);
        values.put(DatabaseValues.Event.END_TIMEZONE, endTimeZone);
        values.put(DatabaseValues.Event.ALL_DAY, allDay);
        values.put(DatabaseValues.Event.CREATE_TIME, createTime);

        try {
            database.insert(DatabaseValues.Event.TABLE, values);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return id;
    }

    public Event getEvent(String id) {
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

    public Event getEvent(long internalId, long startTime, long endTime) {
        Event event = null;

        Cursor cursor = database.select(
            DatabaseValues.Event.TABLE,
            DatabaseValues.Event.PROJECTION,
            DatabaseValues.Event.INTERNAL_ID + " = ?" +
            " AND " + DatabaseValues.Event.START_TIME + " = ?" +
            " AND " + DatabaseValues.Event.END_TIME + " = ?",
            new String[]{
                String.valueOf(internalId),
                String.valueOf(startTime),
                String.valueOf(endTime)
            }
        );

        if (cursor.moveToNext()) {
            event = cursorToEvent(cursor);
        }

        cursor.close();

        return event;
    }

    public boolean containsEvent(long internalId, long startTime, long endTime) {
        boolean status = false;

        Cursor cursor = database.select(
            DatabaseValues.Event.TABLE,
            new String[]{
                "1"
            },
            DatabaseValues.Event.INTERNAL_ID + " = ?" +
            " AND " + DatabaseValues.Event.START_TIME + " = ?" +
            " AND " + DatabaseValues.Event.END_TIME + " = ?",
            new String[]{
                String.valueOf(internalId),
                String.valueOf(startTime),
                String.valueOf(endTime)
            }
        );

        if (cursor.moveToNext()) {
            status = true;
        }

        cursor.close();

        return status;
    }

    public List<Event> getEventsByInternalId(long internalId) {
        List<Event> events = new ArrayList<>();

        Cursor cursor = database.select(
            DatabaseValues.Event.TABLE,
            DatabaseValues.Event.PROJECTION,
            DatabaseValues.Event.INTERNAL_ID + " = ?",
            new String[]{
                String.valueOf(internalId)
            }
        );

        while (cursor.moveToNext()) {
            Event event = cursorToEvent(cursor);
            events.add(event);
        }

        cursor.close();

        return events;
    }

    public int updateValues(String id, ContentValues values) {
        return database.update(
            DatabaseValues.Event.TABLE,
            values,
            DatabaseValues.Event.ID + " = ?",
            new String[]{
                String.valueOf(id)
            }
        );
    }

    public int updateTime(String id, long startTime, long endTime) {
        ContentValues values = new ContentValues();
        values.put(DatabaseValues.Event.START_TIME, startTime);
        values.put(DatabaseValues.Event.END_TIME, endTime);

        return updateValues(id, values);
    }

    public int removeEvent(String id) {
        return database.delete(
            DatabaseValues.Event.TABLE,
            DatabaseValues.Event.ID + " = ?",
            new String[]{
                id
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
            DatabaseValues.Event.START_TIME + " >= ?" +
            " AND " + DatabaseValues.Event.END_TIME + " <= ?",
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

    public List<Event> getEventsByTimePeriod(long calendarId, long startTime, long endTime) {
        List<Event> events = new ArrayList<>();

        Cursor cursor = database.select(
            DatabaseValues.Event.TABLE,
            DatabaseValues.Event.PROJECTION,
            DatabaseValues.Event.CALENDAR_ID + " = ?" +
            " AND " + DatabaseValues.Event.START_TIME + " >= ?" +
            " AND " + DatabaseValues.Event.END_TIME + " <= ?",
            new String[]{
                String.valueOf(calendarId),
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

    public String[] getEventIdsByDay(int year, int month, int day) {
        String[] result = null;

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);

        calendar.set(year, month, day, 0, 0, 0);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        long startTime = calendar.getTimeInMillis();

        calendar.set(year, month, day, 0, 0, 0);
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        long endTime = calendar.getTimeInMillis();

        Cursor cursor = database.select(
            DatabaseValues.Event.TABLE,
            new String[]{
                DatabaseValues.Event.ID,
                DatabaseValues.Event.START_TIME,
                DatabaseValues.Event.TIMEZONE
            },
            DatabaseValues.Event.START_TIME + " BETWEEN ? AND ?",
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
            List<String> tempResult = new ArrayList<>(count);

            while (cursor.moveToNext()) {
                String id = cursor.getString(0);
                long time = cursor.getLong(1);
                String timeZone = cursor.getString(2);

                calendar.setTimeInMillis(time);
                calendar.setTimeZone(TimeZone.getTimeZone(timeZone));

                if (calendar.get(Calendar.DAY_OF_MONTH) == day) {
                    tempResult.add(id);
                }
            }

            if (!tempResult.isEmpty()) {
                result = tempResult.toArray(new String[tempResult.size()]);
            }
        }

        cursor.close();

        return result;
    }

    public Map<Integer, List<Integer>> getEventColorsByMonth(int year, int month) {
        Map<Integer, List<Integer>> result = new HashMap<>();

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);

        calendar.set(year, month, 1, 0, 0, 0);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        long startTime = calendar.getTimeInMillis();

        calendar.set(year, month, 2, 0, 0, 0);
        calendar.add(Calendar.MONTH, 1);
        long endTime = calendar.getTimeInMillis();

        Cursor cursor = database.select(
            DatabaseValues.Event.TABLE,
            new String[]{
                DatabaseValues.Event.COLOR,
                DatabaseValues.Event.START_TIME,
                DatabaseValues.Event.TIMEZONE
            },
            DatabaseValues.Event.START_TIME + " BETWEEN ? AND ?",
            new String[]{
                String.valueOf(startTime),
                String.valueOf(endTime)
            },
            null,
            DatabaseValues.Event.START_TIME + " DESC",
            null
        );

        int currentDay = -1;

        while (cursor.moveToNext()) {
            int color = cursor.getInt(0);
            long time = cursor.getLong(1);
            String timeZone = cursor.getString(2);

            calendar.setTimeInMillis(time);
            calendar.setTimeZone(TimeZone.getTimeZone(timeZone));

            if (calendar.get(Calendar.MONTH) == month) {
                int day = calendar.get(Calendar.DAY_OF_MONTH);
                if (day != currentDay) {
                    currentDay = day;
                    result.put(day, new ArrayList<Integer>());
                }

                List<Integer> colors = result.get(day);
                colors.add(color);
            }
        }

        cursor.close();

        return result;
    }

    public Event getUserstayEventByStartTime (long startTime) {
        Event event = null;
        Cursor cursor = database.select(
                DatabaseValues.Event.TABLE,
                DatabaseValues.Event.PROJECTION,
                DatabaseValues.Event.START_TIME + " == ? AND " +
                        DatabaseValues.Event.TYPE + " == ?",
                new String[]{
                        String.valueOf(startTime),
                        String.valueOf("userstay")
                }
        );


        if (cursor.moveToNext()) {
            event = cursorToEvent(cursor);
        }

        cursor.close();

        return event;

    }

    public Event getUserstayEventByEndTime (long endTime) {
        Event event = null;
        Cursor cursor = database.select(
                DatabaseValues.Event.TABLE,
                DatabaseValues.Event.PROJECTION,
                DatabaseValues.Event.END_TIME + " == ? AND " +
                        DatabaseValues.Event.TYPE + " == ?",
                new String[]{
                        String.valueOf(endTime),
                        String.valueOf("userstay")
                }
        );


        if (cursor.moveToNext()) {
            event = cursorToEvent(cursor);
        }

        cursor.close();

        return event;

    }

    /**
     * For PROJECTION only.
     */
    private Event cursorToEvent(Cursor cursor) {
        Event event = new Event(cursor.getString(DatabaseValues.Event.INDEX_ID));
        event.internalId = cursor.getLong(DatabaseValues.Event.INDEX_INTERNAL_ID);
        event.externalId = cursor.getString(DatabaseValues.Event.INDEX_EXTERNAL_ID);
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
        event.timeZone = cursor.getString(DatabaseValues.Event.INDEX_TIMEZONE);
        event.endTimeZone = cursor.getString(DatabaseValues.Event.INDEX_END_TIMEZONE);
        event.allDay = cursor.getInt(DatabaseValues.Event.INDEX_ALL_DAY) != 0;
        event.createTime = cursor.getLong(DatabaseValues.Event.INDEX_CREATE_TIME);

        return event;
    }
}
