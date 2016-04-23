package com.mono.db.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;

import com.mono.db.Database;
import com.mono.db.DatabaseValues;
import com.mono.model.Event;
import com.mono.model.Location;
import com.mono.util.Common;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;

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

    private String getCalendarSelection(List<String> args, long[] calendarIds) {
        String selection = String.format(
            "%s IN (%s)",
            DatabaseValues.Event.CALENDAR_ID,
            Common.repeat("?", calendarIds.length, ", ")
        );

        for (long calendarId : calendarIds) {
            args.add(String.valueOf(calendarId));
        }

        return selection;
    }

    private String getTimeSelection(List<String> args, long startTime, long endTime) {
        String selection = String.format(
            "(%s BETWEEN ? AND ? OR %s BETWEEN ? AND ?)",
            DatabaseValues.Event.START_TIME,
            DatabaseValues.Event.END_TIME
        );

        args.add(String.valueOf(startTime));
        args.add(String.valueOf(endTime));
        args.add(String.valueOf(startTime));
        args.add(String.valueOf(endTime));

        return selection;
    }

    public List<Event> getEvents(long startTime, int limit, long... calendarIds) {
        List<Event> events = new ArrayList<>();

        List<String> args = new ArrayList<>();

        String selection = "";
        if (calendarIds != null && calendarIds.length > 0) {
            selection = getCalendarSelection(args, calendarIds) + " AND ";
        }

        selection += DatabaseValues.Event.START_TIME + " < ?";
        args.add(String.valueOf(startTime));

        String[] selectionArgs = args.toArray(new String[args.size()]);

        Cursor cursor = database.select(
            DatabaseValues.Event.TABLE,
            DatabaseValues.Event.PROJECTION,
            selection,
            selectionArgs,
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

    public List<Event> getEvents(long startTime, long endTime, long... calendarIds) {
        List<Event> events = new ArrayList<>();

        List<String> args = new ArrayList<>();

        String selection = "";
        if (calendarIds != null && calendarIds.length > 0) {
            selection = getCalendarSelection(args, calendarIds) + " AND ";
        }

        selection += getTimeSelection(args, startTime, endTime);

        String[] selectionArgs = args.toArray(new String[args.size()]);

        Cursor cursor = database.select(
            DatabaseValues.Event.TABLE,
            DatabaseValues.Event.PROJECTION,
            selection,
            selectionArgs,
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

    public List<Event> getEvents(int year, int month, int day, long... calendarIds) {
        List<Event> events = new ArrayList<>();

        List<String> args = new ArrayList<>();

        String selection = "";
        if (calendarIds != null && calendarIds.length > 0) {
            selection = getCalendarSelection(args, calendarIds) + " AND ";
        }

        DateTime dateTime = new DateTime(year, month + 1, day, 0, 0);
        long startTime = dateTime.minusHours(12).getMillis();
        long endTime = dateTime.plusDays(1).plusHours(12).getMillis();

        selection += getTimeSelection(args, startTime, endTime);

        String[] selectionArgs = args.toArray(new String[args.size()]);

        Cursor cursor = database.select(
            DatabaseValues.Event.TABLE,
            DatabaseValues.Event.PROJECTION,
            selection,
            selectionArgs,
            null,
            DatabaseValues.Event.START_TIME + " DESC",
            null
        );

        Calendar calendar;
        Calendar local = Calendar.getInstance();
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        while (cursor.moveToNext()) {
            Event event = cursorToEvent(cursor);

            calendar = !event.allDay ? local : utc;

            calendar.setTimeInMillis(event.startTime);
            int startDay = calendar.get(Calendar.DAY_OF_MONTH);

            calendar.setTimeInMillis(!event.allDay ? event.endTime : event.endTime - 1);
            int endDay = calendar.get(Calendar.DAY_OF_MONTH);

            if (!Common.between(day, startDay, endDay)) {
                continue;
            }

            events.add(event);
        }

        cursor.close();

        return events;
    }

    public Map<Integer, List<Integer>> getEventColors(int year, int month, long... calendarIds) {
        Map<Integer, List<Integer>> result = new HashMap<>();

        List<String> args = new ArrayList<>();

        String selection = "";
        if (calendarIds != null && calendarIds.length > 0) {
            selection = getCalendarSelection(args, calendarIds) + " AND ";
        }

        DateTime dateTime = new DateTime(year, month + 1, 1, 0, 0);
        long startTime = dateTime.minusHours(12).getMillis();
        long endTime = dateTime.plusMonths(1).plusHours(12).getMillis();

        selection += getTimeSelection(args, startTime, endTime);

        String[] selectionArgs = args.toArray(new String[args.size()]);

        Cursor cursor = database.select(
            DatabaseValues.Event.TABLE,
            new String[]{
                DatabaseValues.Event.COLOR,
                DatabaseValues.Event.START_TIME,
                DatabaseValues.Event.END_TIME,
                DatabaseValues.Event.ALL_DAY
            },
            selection,
            selectionArgs,
            null,
            DatabaseValues.Event.START_TIME + " DESC",
            null
        );

        Calendar calendar;
        Calendar local = Calendar.getInstance();
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        while (cursor.moveToNext()) {
            int color = cursor.getInt(0);
            long startMillis = cursor.getLong(1);
            long endMillis = cursor.getLong(2);
            boolean allDay = cursor.getInt(3) > 0;

            if (!allDay) {
                calendar = local;
            } else {
                endMillis -= 1;
                calendar = utc;
            }

            int numDays = (int) (DateTimeUtils.toJulianDayNumber(endMillis) -
                DateTimeUtils.toJulianDayNumber(startMillis)) + 1;

            calendar.setTimeInMillis(startMillis);

            for (int i = 0; i < numDays; i++) {
                if (calendar.get(Calendar.MONTH) == month) {
                    int currentDay = calendar.get(Calendar.DAY_OF_MONTH);

                    if (!result.containsKey(currentDay)) {
                        result.put(currentDay, new ArrayList<Integer>());
                    }

                    List<Integer> colors = result.get(currentDay);
                    if (!colors.contains(color)) {
                        colors.add(color);
                    }
                }

                calendar.add(Calendar.DAY_OF_MONTH, 1);
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
