package com.mono.provider;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Instances;

import com.mono.model.Calendar;
import com.mono.model.Event;
import com.mono.model.Location;
import com.mono.util.Common;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class CalendarEventProvider {

    private static final SimpleDateFormat DATE_FORMAT;

    private static CalendarEventProvider instance;

    private Context context;

    static {
        DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
    }

    private CalendarEventProvider(Context context) {
        this.context = context;
    }

    public static CalendarEventProvider getInstance(Context context) {
        if (instance == null) {
            instance = new CalendarEventProvider(context.getApplicationContext());
        }

        return instance;
    }

    public long createEvent(int calendarId, String title, String description, String location,
            int color, long startTime, long endTime, String timeZone, String endTimeZone,
            int allDay) throws SecurityException {
        long eventId = -1;

        ContentValues values = new ContentValues();
        values.put(Events.CALENDAR_ID, calendarId);
        values.put(Events.TITLE, title);
        values.put(Events.DESCRIPTION, description);
        values.put(Events.EVENT_LOCATION, location);
        values.put(Events.EVENT_COLOR, color);
        values.put(Events.DTSTART, startTime);
        values.put(Events.DTEND, endTime);
        values.put(Events.EVENT_TIMEZONE, timeZone);
        values.put(Events.EVENT_END_TIMEZONE, endTimeZone);
        values.put(Events.ALL_DAY, allDay);

        Uri uri = context.getContentResolver().insert(Events.CONTENT_URI, values);

        if (uri != null) {
            eventId = Long.parseLong(uri.getLastPathSegment());
        }

        return eventId;
    }

    public Event getEvent(long id) {
        Event event = null;

        Uri.Builder builder = Events.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, id);

        Cursor cursor = context.getContentResolver().query(
            builder.build(),
            CalendarValues.Event.PROJECTION,
            null,
            null,
            null
        );

        if (cursor != null && cursor.moveToNext()) {
            event = cursorToEvent(cursor);
            cursor.close();

            Calendar calendar =
                CalendarProvider.getInstance(context).getCalendar(event.calendarId);

            if (event.color == 0) {
                event.color = calendar.color;
            }
        }

        return event;
    }

    public Event getEvent(long id, long startTime, long endTime) {
        Event event = getEvent(id);

        if (event != null) {
            Uri.Builder builder = Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, startTime);
            ContentUris.appendId(builder, endTime);

            Cursor cursor = context.getContentResolver().query(
                builder.build(),
                CalendarValues.Instance.PROJECTION,
                Instances.EVENT_ID + " = ?",
                new String[]{
                    String.valueOf(id)
                },
                null
            );

            if (cursor != null && cursor.moveToNext()) {
                Instance instance = cursorToInstance(cursor);

                event.id = instance.id;
                event.startTime = instance.startTime;
                event.endTime = instance.endTime;

                List<Event> events = new ArrayList<>();
                events.add(event);
                resolveEventsData(events);

                cursor.close();
            }
        }

        return event;
    }

    public int updateEvent(long eventId, String title, String description, Long startTime,
            Long endTime, String timeZone) throws SecurityException {
        ContentValues values = new ContentValues();

        if (title != null) {
            values.put(Events.TITLE, title);
        }

        if (description != null) {
            values.put(Events.DESCRIPTION, description);
        }

        if (startTime != null) {
            values.put(Events.DTSTART, startTime);
        }

        if (endTime != null) {
            values.put(Events.DTEND, endTime);
        }

        if (timeZone != null) {
            values.put(Events.EVENT_TIMEZONE, timeZone);
        }

        return context.getContentResolver().update(
            Events.CONTENT_URI,
            values,
            Events._ID + " = ?",
            new String[]{
                String.valueOf(eventId)
            }
        );
    }

    public int removeEvent(long eventId) throws SecurityException {
        return context.getContentResolver().delete(
            Events.CONTENT_URI,
            Events._ID + " = ?",
            new String[]{
                String.valueOf(eventId)
            }
        );
    }

    private String getCalendarSelection(List<String> args, long[] calendarIds) {
        String selection = String.format(
            "%s IN (%s)",
            Instances.CALENDAR_ID,
            Common.repeat("?", calendarIds.length, ", ")
        );

        for (long calendarId : calendarIds) {
            args.add(String.valueOf(calendarId));
        }

        return selection;
    }

    public Calendar getEvents(long calendarId, long startMin, long startMax, long endMin,
            long endMax) {
        Calendar calendar = CalendarProvider.getInstance(context).getCalendar(calendarId);
        if (calendar == null) {
            return null;
        }

        Cursor cursor = createInstancesCursor(startMin, startMax, endMin, endMax, calendarId);

        if (cursor != null) {
            calendar.events.addAll(cursorInstancesToEvents(cursor));
            cursor.close();

            resolveEventsData(calendar.events);
        }

        return calendar;
    }

    public List<Event> getEvents(long startTime, long endTime, long... calendarIds) {
        List<Event> events = new ArrayList<>();

        Cursor cursor = createInstancesCursor(startTime, endTime, startTime, endTime, calendarIds);

        if (cursor != null) {
            events.addAll(cursorInstancesToEvents(cursor));
            cursor.close();

            resolveEventsData(events);
        }

        return events;
    }

    public List<Event> getEvents(long startTime, long endTime, List<Long> eventIds) {
        List<Event> events = new ArrayList<>();

        Uri.Builder builder = Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, startTime);
        ContentUris.appendId(builder, endTime);

        List<String> args = new ArrayList<>();

        String selection = "";
        if (eventIds != null && !eventIds.isEmpty()) {
            selection = String.format(
                "%s IN (%s)",
                Instances.EVENT_ID,
                Common.repeat("?", eventIds.size(), ", ")
            );

            for (long eventId : eventIds) {
                args.add(String.valueOf(eventId));
            }
        }

        String[] selectionArgs = args.toArray(new String[args.size()]);

        Cursor cursor = context.getContentResolver().query(
            builder.build(),
            CalendarValues.Instance.PROJECTION,
            selection,
            selectionArgs,
            Instances.BEGIN
        );

        if (cursor != null) {
            events.addAll(cursorInstancesToEvents(cursor));
            cursor.close();

            resolveEventsData(events);
        }

        return events;
    }

    public List<Event> getEvents(long startTime, long endTime, int limit, long... calendarIds) {
        List<Event> events = new ArrayList<>();

        Uri.Builder builder = Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, startTime);
        ContentUris.appendId(builder, endTime);

        List<String> args = new ArrayList<>();

        String selection = "";
        if (calendarIds != null && calendarIds.length > 0) {
            selection = getCalendarSelection(args, calendarIds);
        }

        String[] selectionArgs = args.toArray(new String[args.size()]);

        Cursor cursor = context.getContentResolver().query(
            builder.build(),
            CalendarValues.Instance.PROJECTION,
            selection,
            selectionArgs,
            Instances.BEGIN + " LIMIT " + limit
        );

        if (cursor != null) {
            events.addAll(cursorInstancesToEvents(cursor));
            cursor.close();

            resolveEventsData(events);
        }

        return events;
    }

    public List<Event> getEvents(int year, int month, int day, long... calendarIds) {
        List<Event> events = new ArrayList<>();

        DateTime dateTime = new DateTime(year, month + 1, day, 0, 0);
        long startTime = dateTime.minusHours(12).getMillis();
        long endTime = dateTime.plusDays(1).plusHours(12).getMillis();

        Uri.Builder builder = Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, startTime);
        ContentUris.appendId(builder, endTime);

        List<String> args = new ArrayList<>();

        String selection = "";
        if (calendarIds != null && calendarIds.length > 0) {
            selection = getCalendarSelection(args, calendarIds);
        }

        String[] selectionArgs = args.toArray(new String[args.size()]);

        Cursor cursor = context.getContentResolver().query(
            builder.build(),
            CalendarValues.Instance.PROJECTION,
            selection,
            selectionArgs,
            Instances.BEGIN + " DESC"
        );

        if (cursor != null) {
            List<Event> tempEvents = cursorInstancesToEvents(cursor);
            cursor.close();

            java.util.Calendar current;
            java.util.Calendar local = java.util.Calendar.getInstance();
            java.util.Calendar utc = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC"));

            for (Event event : tempEvents) {
                current = !event.allDay ? local : utc;

                current.setTimeInMillis(event.startTime);
                int startDay = current.get(java.util.Calendar.DAY_OF_MONTH);

                current.setTimeInMillis(!event.allDay ? event.endTime : event.endTime - 1);
                int endDay = current.get(java.util.Calendar.DAY_OF_MONTH);

                if (!Common.between(day, startDay, endDay)) {
                    continue;
                }

                events.add(event);
            }

            resolveEventsData(events);
        }

        return events;
    }

    public Map<Integer, List<Integer>> getEventColors(int year, int month, long... calendarIds) {
        Map<Integer, List<Integer>> result = new HashMap<>();

        DateTime dateTime = new DateTime(year, month + 1, 1, 0, 0);
        long startTime = dateTime.minusHours(12).getMillis();
        long endTime = dateTime.plusMonths(1).plusHours(12).getMillis();

        Uri.Builder builder = Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, startTime);
        ContentUris.appendId(builder, endTime);

        List<String> args = new ArrayList<>();

        String selection = "";
        if (calendarIds != null && calendarIds.length > 0) {
            selection = getCalendarSelection(args, calendarIds);
        }

        String[] selectionArgs = args.toArray(new String[args.size()]);

        Cursor cursor = context.getContentResolver().query(
            builder.build(),
            new String[]{
                Instances.CALENDAR_COLOR,
                Instances.EVENT_COLOR,
                Instances.BEGIN,
                Instances.END,
                Instances.ALL_DAY
            },
            selection,
            selectionArgs,
            Instances.BEGIN + " DESC"
        );

        if (cursor != null) {
            java.util.Calendar calendar;
            java.util.Calendar local = java.util.Calendar.getInstance();
            java.util.Calendar utc = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC"));

            while (cursor.moveToNext()) {
                int color = cursor.getInt(1);
                if (color == 0) {
                    color = cursor.getInt(0);
                }

                long startMillis = cursor.getLong(2);
                long endMillis = cursor.getLong(3);
                boolean allDay = cursor.getInt(4) > 0;

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
                    if (calendar.get(java.util.Calendar.MONTH) == month) {
                        int currentDay = calendar.get(java.util.Calendar.DAY_OF_MONTH);

                        if (!result.containsKey(currentDay)) {
                            result.put(currentDay, new ArrayList<Integer>());
                        }

                        List<Integer> colors = result.get(currentDay);
                        if (!colors.contains(color)) {
                            colors.add(color);
                        }
                    }

                    calendar.add(java.util.Calendar.DAY_OF_MONTH, 1);
                }
            }

            cursor.close();
        }

        return result;
    }


    public Calendar getUpdates(long calendarId, long startTime, long endTime)
            throws SecurityException {
        Calendar calendar = CalendarProvider.getInstance(context).getCalendar(calendarId);
        if (calendar == null) {
            return null;
        }

        String selection = String.format(
            "%s = ? AND %s > ? AND %s <= ?",
            Events.CALENDAR_ID,
            Events.SYNC_DATA5,
            Events.SYNC_DATA5
        );

        java.util.Calendar tempCalendar = java.util.Calendar.getInstance();

        tempCalendar.setTimeInMillis(startTime);
        String start = DATE_FORMAT.format(tempCalendar.getTime());

        tempCalendar.setTimeInMillis(endTime);
        String end = DATE_FORMAT.format(tempCalendar.getTime());

        String[] selectionArgs = {
            String.valueOf(calendarId),
            start,
            end
        };

        Cursor cursor = context.getContentResolver().query(
            Events.CONTENT_URI,
            CalendarValues.Event.PROJECTION,
            selection,
            selectionArgs,
            Events.DTSTART + " ASC"
        );

        if (cursor != null) {
            List<Long> eventIds = new ArrayList<>();

            while (cursor.moveToNext()) {
                Event event = cursorToEvent(cursor);
                eventIds.add(event.internalId);
            }

            cursor.close();

            if (!eventIds.isEmpty()) {
                calendar.events.addAll(getEvents(0, endTime, eventIds));
                resolveEventsData(calendar.events);
            }
        }

        return calendar;
    }

    private void resolveEventsData(List<Event> events) {
        if (events.isEmpty()) {
            return;
        }

        CalendarAttendeeProvider.getInstance(context).resolveAttendees(events);
        CalendarReminderProvider.getInstance(context).resolveReminders(events);
    }

    private Cursor createEventsCursor(List<Long> eventIds) throws SecurityException {
        int size = eventIds.size();

        String selection = String.format(
            "%s IN (%s)",
            Events._ID,
            Common.repeat("?", size, ", ")
        );

        String[] selectionArgs = new String[size];
        for (int i = 0; i < size; i++) {
            selectionArgs[i] = String.valueOf(eventIds.get(i));
        }

        return context.getContentResolver().query(
            Events.CONTENT_URI,
            CalendarValues.Event.PROJECTION,
            selection,
            selectionArgs,
            Events.DTSTART + " ASC"
        );
    }

    private Cursor createInstancesCursor(long startMin, long startMax, long endMin, long endMax,
            long... calendarIds) {
        Uri.Builder builder = Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, startMin);
        ContentUris.appendId(builder, endMax);

        List<String> args = new ArrayList<>();

        String selection = "";
        if (calendarIds != null && calendarIds.length > 0) {
            selection = getCalendarSelection(args, calendarIds) + " AND ";
        }

        selection += String.format(
            "(%s <= ? OR %s >= ?)",
            Instances.BEGIN,
            Instances.BEGIN
        );

        args.add(String.valueOf(startMax));
        args.add(String.valueOf(endMin));

        String[] selectionArgs = args.toArray(new String[args.size()]);

        return context.getContentResolver().query(
            builder.build(),
            CalendarValues.Instance.PROJECTION,
            selection,
            selectionArgs,
            Instances.BEGIN + " DESC"
        );
    }

    private Event cursorToEvent(Cursor cursor) {
        String id = String.format(
            Locale.getDefault(),
            "%d.%d.%d",
            cursor.getLong(CalendarValues.Event.INDEX_ID),
            cursor.getLong(CalendarValues.Event.INDEX_START_TIME),
            cursor.getLong(CalendarValues.Event.INDEX_END_TIME)
        );

        Event event = new Event(id);
        event.calendarId = cursor.getLong(CalendarValues.Event.INDEX_CALENDAR_ID);
        event.internalId = cursor.getLong(CalendarValues.Event.INDEX_ID);
        event.externalId = cursor.getString(CalendarValues.Event.INDEX_REMOTE_ID);
        event.type = Event.TYPE_CALENDAR;

        event.title = cursor.getString(CalendarValues.Event.INDEX_TITLE);
        if (event.title != null && event.title.isEmpty()) {
            event.title = null;
        }

        event.description = cursor.getString(CalendarValues.Event.INDEX_DESCRIPTION);
        if (event.description != null && event.description.isEmpty()) {
            event.description = null;
        }

        String location = cursor.getString(CalendarValues.Event.INDEX_LOCATION);
        if (location != null && !location.isEmpty()) {
            event.location = new Location(location);
        }

        event.color = cursor.getInt(CalendarValues.Event.INDEX_COLOR);

        event.startTime = cursor.getLong(CalendarValues.Event.INDEX_START_TIME);
        event.endTime = cursor.getLong(CalendarValues.Event.INDEX_END_TIME);
        event.timeZone = cursor.getString(CalendarValues.Event.INDEX_TIMEZONE);
        event.endTimeZone = cursor.getString(CalendarValues.Event.INDEX_END_TIMEZONE);
        event.allDay = cursor.getInt(CalendarValues.Event.INDEX_ALL_DAY) > 0;
        event.lastRepeatTime = cursor.getLong(CalendarValues.Event.INDEX_LAST_REPEAT_TIME);

        try {
            String dateTime = cursor.getString(CalendarValues.Event.INDEX_UPDATE_TIME);
            event.updateTime = DATE_FORMAT.parse(dateTime).getTime();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return event;
    }

    private Instance cursorToInstance(Cursor cursor) {
        String id = String.format(
            Locale.getDefault(),
            "%d.%d.%d",
            cursor.getLong(CalendarValues.Instance.INDEX_EVENT_ID),
            cursor.getLong(CalendarValues.Instance.INDEX_BEGIN),
            cursor.getLong(CalendarValues.Instance.INDEX_END)
        );

        Instance instance = new Instance(id);
        instance.calendarId = cursor.getLong(CalendarValues.Instance.INDEX_CALENDAR_ID);
        instance.eventId = cursor.getLong(CalendarValues.Instance.INDEX_EVENT_ID);
        instance.startTime = cursor.getLong(CalendarValues.Instance.INDEX_BEGIN);
        instance.endTime = cursor.getLong(CalendarValues.Instance.INDEX_END);

        return instance;
    }

    private List<Event> cursorInstancesToEvents(Cursor cursor) {
        List<Event> result = new ArrayList<>();

        Map<Long, Calendar> calendars = new HashMap<>();
        Map<Long, Event> events = new HashMap<>();

        while (cursor.moveToNext()) {
            Instance instance = cursorToInstance(cursor);

            if (!calendars.containsKey(instance.calendarId)) {
                calendars.put(instance.calendarId,
                    CalendarProvider.getInstance(context).getCalendar(instance.calendarId));
            }

            Calendar calendar = calendars.get(instance.calendarId);

            if (!events.containsKey(instance.eventId)) {
                events.put(instance.eventId, getEvent(instance.eventId));
            }

            Event event = new Event(events.get(instance.eventId));
            event.id = instance.id;
            event.startTime = instance.startTime;
            event.endTime = instance.endTime;

            if (event.color == 0) {
                event.color = calendar.color;
            }

            result.add(event);
        }

        return result;
    }

    private class Instance {

        public String id;
        public long calendarId;
        public long eventId;
        public long startTime;
        public long endTime;

        public Instance(String id) {
            this.id = id;
        }
    }
}
