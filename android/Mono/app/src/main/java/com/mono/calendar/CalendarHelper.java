package com.mono.calendar;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Instances;
import android.provider.CalendarContract.Reminders;

import com.mono.model.Attendee;
import com.mono.model.Calendar;
import com.mono.model.Event;
import com.mono.model.Location;
import com.mono.model.Reminder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalendarHelper {

    private static final SimpleDateFormat DATE_FORMAT;

    private static CalendarHelper instance;

    private Context context;

    static {
        DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
    }

    private CalendarHelper(Context context) {
        this.context = context;
    }

    public static CalendarHelper getInstance(Context context) {
        if (instance == null) {
            instance = new CalendarHelper(context);
        }

        return instance;
    }

    public Calendar getCalendar(long id) {
        Calendar calendar = null;

        Uri.Builder builder = Calendars.CONTENT_URI.buildUpon();

        Cursor cursor = context.getContentResolver().query(
            builder.build(),
            CalendarValues.Calendar.PROJECTION,
            Calendars._ID + " = ?",
            new String[]{
                String.valueOf(id)
            },
            null
        );

        if (cursor != null) {
            if (cursor.moveToNext()) {
                calendar = cursorToCalendar(cursor);
            }

            cursor.close();
        }

        return calendar;
    }

    public List<Calendar> getCalendars() {
        List<Calendar> calendars = new ArrayList<>();

        Uri.Builder builder = Calendars.CONTENT_URI.buildUpon();

        Cursor cursor = context.getContentResolver().query(
            builder.build(),
            CalendarValues.Calendar.PROJECTION,
            null,
            null,
            null
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                Calendar calendar = cursorToCalendar(cursor);
                calendars.add(calendar);
            }

            cursor.close();
        }

        return calendars;
    }

    private Calendar cursorToCalendar(Cursor cursor) {
        Calendar calendar = new Calendar(cursor.getLong(CalendarValues.Calendar.INDEX_ID));
        calendar.name = cursor.getString(CalendarValues.Calendar.INDEX_NAME);
        calendar.color = cursor.getInt(CalendarValues.Calendar.INDEX_COLOR);

        calendar.accountName =
            cursor.getString(CalendarValues.Calendar.INDEX_ACCOUNT_NAME);
        calendar.accountType =
            cursor.getString(CalendarValues.Calendar.INDEX_ACCOUNT_TYPE);

        return calendar;
    }

    public Calendar getEvents(long calendarId, long startMin, long startMax,
            long endMin, long endMax) {
        Calendar calendar = getCalendar(calendarId);
        if (calendar == null) {
            return null;
        }

        Cursor cursor = createInstancesCursor(calendarId, startMin, startMax, endMin, endMax);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                Event event = cursorToInstance(cursor);
                calendar.events.add(event);
            }

            cursor.close();

            resolveEventsData(calendar.events);
        }

        return calendar;
    }

    public List<Event> getEvents(long calendarId, long startTime, long endTime) {
        List<Event> events = new ArrayList<>();

        Cursor cursor = createInstancesCursor(calendarId, startTime, endTime, startTime, endTime);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                Event event = cursorToInstance(cursor);
                events.add(event);
            }

            cursor.close();

            resolveEventsData(events);
        }

        return events;
    }

    public List<Event> getEvents(long calendarId, long startTime, long endTime,
            List<Long> eventIds) {
        List<Event> events = new ArrayList<>();

        Uri.Builder builder = Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, startTime);
        ContentUris.appendId(builder, endTime);

        String selectionIn = "";

        String[] selectionArgs = new String[1 + eventIds.size()];
        selectionArgs[0] = String.valueOf(calendarId);

        for (int i = 0; i < eventIds.size(); i++) {
            if (i > 0) selectionIn += ", ";
            selectionIn += "?";

            selectionArgs[1 + i] = String.valueOf(eventIds.get(i));
        }

        Cursor cursor = context.getContentResolver().query(
            builder.build(),
            CalendarValues.Instance.PROJECTION,
            Instances.CALENDAR_ID + " = ?" +
            " AND " + Instances.EVENT_ID + " IN (" + selectionIn + ")",
            selectionArgs,
            Instances.BEGIN + " ASC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                Event event = cursorToInstance(cursor);
                events.add(event);
            }

            cursor.close();

            resolveEventsData(events);
        }

        return events;
    }

    private void resolveEvents(List<Event> events) {
        List<Long> eventIds = new ArrayList<>();
        for (Event event : events) {
            eventIds.add(event.internalId);
        }

        Cursor cursor = createEventsCursor(eventIds);

        if (cursor != null) {
            Map<Long, Event> tempEvents = new HashMap<>();

            while (cursor.moveToNext()) {
                Event event = cursorToEvent(cursor);
                tempEvents.put(event.internalId, event);
            }

            cursor.close();

            for (Event event : events) {
                Event tempEvent = tempEvents.get(event.internalId);
                event.externalId = tempEvent.externalId;
                event.updateTime = tempEvent.updateTime;
            }
        }
    }

    private Cursor createEventsCursor(List<Long> eventIds) {
        Uri.Builder builder = Events.CONTENT_URI.buildUpon();

        String selectionIn = "";
        String[] selectionArgs = new String[eventIds.size()];

        for (int i = 0; i < eventIds.size(); i++) {
            if (i > 0) selectionIn += ", ";
            selectionIn += "?";

            selectionArgs[i] = String.valueOf(eventIds.get(i));
        }

        return context.getContentResolver().query(
            builder.build(),
            CalendarValues.Event.PROJECTION,
            Events._ID + " IN (" + selectionIn + ")",
            selectionArgs,
            Events.DTSTART + " ASC"
        );
    }

    private Event cursorToEvent(Cursor cursor) {
        Event event = new Event();
        event.internalId = cursor.getLong(CalendarValues.Event.INDEX_ID);
        event.externalId = cursor.getString(CalendarValues.Event.INDEX_REMOTE_ID);

        try {
            String dateTime = cursor.getString(CalendarValues.Event.INDEX_UPDATE_TIME);
            event.updateTime = DATE_FORMAT.parse(dateTime).getTime();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return event;
    }

    private Cursor createInstancesCursor(long calendarId, long startMin, long startMax,
            long endMin, long endMax) {
        Uri.Builder builder = Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, startMin);
        ContentUris.appendId(builder, endMax);

        return context.getContentResolver().query(
            builder.build(),
            CalendarValues.Instance.PROJECTION,
            Instances.CALENDAR_ID + " = ?" +
            " AND (" +
                Instances.BEGIN + " <= ?" +
                " OR " +
                Instances.BEGIN + " >= ?" +
            ")",
            new String[]{
                String.valueOf(calendarId),
                String.valueOf(startMax),
                String.valueOf(endMin)
            },
            Instances.BEGIN + " DESC"
        );
    }

    private Event cursorToInstance(Cursor cursor) {
        Event event = new Event();
        event.internalId = cursor.getLong(CalendarValues.Instance.INDEX_EVENT_ID);
        event.type = Event.TYPE_CALENDAR;
        event.startTime = cursor.getLong(CalendarValues.Instance.INDEX_BEGIN);
        event.endTime = cursor.getLong(CalendarValues.Instance.INDEX_END);
        event.timeZone = cursor.getString(CalendarValues.Instance.INDEX_TIMEZONE);
        event.endTimeZone = cursor.getString(CalendarValues.Instance.INDEX_END_TIMEZONE);
        event.allDay = cursor.getInt(CalendarValues.Instance.INDEX_ALL_DAY) > 0;

        event.title = cursor.getString(CalendarValues.Instance.INDEX_TITLE);
        if (event.title != null && event.title.isEmpty()) {
            event.title = null;
        }

        event.description = cursor.getString(CalendarValues.Instance.INDEX_DESCRIPTION);
        if (event.description != null && event.description.isEmpty()) {
            event.description = null;
        }

        String location = cursor.getString(CalendarValues.Instance.INDEX_LOCATION);
        if (location != null) {
            event.location = new Location(location);
        }

        event.color = cursor.getInt(CalendarValues.Instance.INDEX_EVENT_COLOR);

        return event;
    }

    private void resolveAttendees(List<Event> events) {
        List<Long> eventIds = new ArrayList<>();
        for (Event event : events) {
            eventIds.add(event.internalId);
        }

        Cursor cursor = createAttendeeCursor(eventIds);

        if (cursor != null) {
            Map<Long, List<Attendee>> attendees = new HashMap<>();

            while (cursor.moveToNext()) {
                long eventId = cursor.getLong(CalendarValues.Attendee.INDEX_EVENT_ID);
                Attendee attendee = cursorToAttendee(cursor);

                if (!attendees.containsKey(eventId)) {
                    attendees.put(eventId, new ArrayList<Attendee>());
                }

                attendees.get(eventId).add(attendee);
            }

            cursor.close();

            for (Event event : events) {
                if (attendees.containsKey(event.internalId)) {
                    event.attendees = attendees.get(event.internalId);
                }
            }
        }
    }

    private Cursor createAttendeeCursor(List<Long> eventIds) {
        Uri.Builder builder = Attendees.CONTENT_URI.buildUpon();

        String selectionIn = "";
        String[] selectionArgs = new String[eventIds.size()];

        for (int i = 0; i < eventIds.size(); i++) {
            if (i > 0) selectionIn += ", ";
            selectionIn += "?";

            selectionArgs[i] = String.valueOf(eventIds.get(i));
        }

        return context.getContentResolver().query(
            builder.build(),
            CalendarValues.Attendee.PROJECTION,
            Attendees.EVENT_ID + " IN (" + selectionIn + ")",
            selectionArgs,
            Attendees.EVENT_ID + ", " + Attendees.ATTENDEE_NAME
        );
    }

    private Attendee cursorToAttendee(Cursor cursor) {
        Attendee attendee = new Attendee(cursor.getLong(CalendarValues.Attendee.INDEX_ID));
        attendee.userName = cursor.getString(CalendarValues.Attendee.INDEX_NAME);
        attendee.email = cursor.getString(CalendarValues.Attendee.INDEX_EMAIL);

        return attendee;
    }

    private void resolveReminders(List<Event> events) {
        List<Long> eventIds = new ArrayList<>();
        for (Event event : events) {
            eventIds.add(event.internalId);
        }

        Cursor cursor = createReminderCursor(eventIds);

        if (cursor != null) {
            Map<Long, List<Reminder>> reminders = new HashMap<>();

            while (cursor.moveToNext()) {
                long eventId = cursor.getLong(CalendarValues.Reminder.INDEX_EVENT_ID);
                Reminder reminder = cursorToReminder(cursor);

                if (!reminders.containsKey(eventId)) {
                    reminders.put(eventId, new ArrayList<Reminder>());
                }

                reminders.get(eventId).add(reminder);
            }

            cursor.close();

            for (Event event : events) {
                if (reminders.containsKey(event.internalId)) {
                    event.reminders = reminders.get(event.internalId);
                }
            }
        }
    }

    private Cursor createReminderCursor(List<Long> eventIds) {
        Uri.Builder builder = Reminders.CONTENT_URI.buildUpon();

        String selectionIn = "";
        String[] selectionArgs = new String[eventIds.size()];

        for (int i = 0; i < eventIds.size(); i++) {
            if (i > 0) selectionIn += ", ";
            selectionIn += "?";

            selectionArgs[i] = String.valueOf(eventIds.get(i));
        }

        return context.getContentResolver().query(
            builder.build(),
            CalendarValues.Reminder.PROJECTION,
            Reminders.EVENT_ID + " IN (" + selectionIn + ")",
            selectionArgs,
            Reminders.EVENT_ID
        );
    }

    private Reminder cursorToReminder(Cursor cursor) {
        Reminder reminder = new Reminder(cursor.getLong(CalendarValues.Reminder.INDEX_ID));
        reminder.minutes = cursor.getInt(CalendarValues.Reminder.INDEX_MINUTES);
        reminder.method = cursor.getInt(CalendarValues.Reminder.INDEX_METHOD);

        return reminder;
    }

    public Calendar getUpdates(long calendarId, long startTime, long endTime) {
        Calendar calendar = getCalendar(calendarId);
        if (calendar == null) {
            return null;
        }

        java.util.Calendar tempCalendar = java.util.Calendar.getInstance();

        tempCalendar.setTimeInMillis(startTime);
        String start = DATE_FORMAT.format(tempCalendar.getTime());

        tempCalendar.setTimeInMillis(endTime);
        String end = DATE_FORMAT.format(tempCalendar.getTime());

        Uri.Builder builder = Events.CONTENT_URI.buildUpon();

        Cursor cursor = context.getContentResolver().query(
            builder.build(),
            CalendarValues.Event.PROJECTION,
            Events.CALENDAR_ID + " = ?" +
            " AND " + Events.SYNC_DATA5 + " > ?" +
            " AND " + Events.SYNC_DATA5 + " <= ?",
            new String[]{
                String.valueOf(calendarId),
                start,
                end
            },
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
                calendar.events.addAll(getEvents(calendarId, 0, endTime, eventIds));
                resolveEventsData(calendar.events);
            }
        }

        return calendar;
    }

    private void resolveEventsData(List<Event> events) {
        if (events.isEmpty()) {
            return;
        }

        resolveEvents(events);
        resolveAttendees(events);
        resolveReminders(events);
    }

    private static class CalendarValues {

        private static class Calendar {

            public static final String[] PROJECTION = {
                Calendars._ID,
                Calendars.CALENDAR_DISPLAY_NAME,
                Calendars.CALENDAR_COLOR,
                Calendars.ACCOUNT_NAME,
                Calendars.ACCOUNT_TYPE
            };

            private static final int INDEX_ID = 0;
            private static final int INDEX_NAME = 1;
            private static final int INDEX_COLOR = 2;
            private static final int INDEX_ACCOUNT_NAME = 3;
            private static final int INDEX_ACCOUNT_TYPE = 4;
        }

        private static class Event {

            public static final String[] PROJECTION = {
                Events._ID,
                Events.SYNC_DATA1,
                Events.SYNC_DATA5
            };

            private static final int INDEX_ID = 0;
            private static final int INDEX_REMOTE_ID = 1;
            private static final int INDEX_UPDATE_TIME = 2;
        }

        private static class Instance {

            public static final String[] PROJECTION = {
                Instances._ID,
                Instances.EVENT_ID,
                Instances.BEGIN,
                Instances.END,
                Instances.EVENT_TIMEZONE,
                Instances.EVENT_END_TIMEZONE,
                Instances.ALL_DAY,
                Instances.TITLE,
                Instances.DESCRIPTION,
                Instances.EVENT_LOCATION,
                Instances.EVENT_COLOR
            };

            private static final int INDEX_ID = 0;
            private static final int INDEX_EVENT_ID = 1;
            private static final int INDEX_BEGIN = 2;
            private static final int INDEX_END = 3;
            private static final int INDEX_TIMEZONE = 4;
            private static final int INDEX_END_TIMEZONE = 5;
            private static final int INDEX_ALL_DAY = 6;
            private static final int INDEX_TITLE = 7;
            private static final int INDEX_DESCRIPTION = 8;
            private static final int INDEX_LOCATION = 9;
            private static final int INDEX_EVENT_COLOR = 10;
        }

        private static class Attendee {

            public static final String[] PROJECTION = {
                Attendees._ID,
                Attendees.EVENT_ID,
                Attendees.ATTENDEE_NAME,
                Attendees.ATTENDEE_EMAIL
            };

            private static final int INDEX_ID = 0;
            private static final int INDEX_EVENT_ID = 1;
            private static final int INDEX_NAME = 2;
            private static final int INDEX_EMAIL = 3;
        }

        private static class Reminder {

            public static final String[] PROJECTION = {
                Reminders._ID,
                Reminders.EVENT_ID,
                Reminders.MINUTES,
                Reminders.METHOD
            };

            private static final int INDEX_ID = 0;
            private static final int INDEX_EVENT_ID = 1;
            private static final int INDEX_MINUTES = 2;
            private static final int INDEX_METHOD = 3;
        }
    }
}
