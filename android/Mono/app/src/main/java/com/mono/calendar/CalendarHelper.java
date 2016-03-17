package com.mono.calendar;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

import com.mono.model.Attendee;
import com.mono.model.Calendar;
import com.mono.model.Event;
import com.mono.model.Location;
import com.mono.settings.Settings;
import com.mono.util.Common;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CalendarHelper {

    private static CalendarHelper instance;

    private Context context;

    private CalendarHelper(Context context) {
        this.context = context;
    }

    public static CalendarHelper getInstance(Context context) {
        if (instance == null) {
            instance = new CalendarHelper(context);
        }

        return instance;
    }

    public List<Calendar> getCalendars() {
        List<Calendar> calendars = new ArrayList<>();

        try {
            Cursor cursor = context.getContentResolver().query(
                CalendarContract.Calendars.CONTENT_URI,
                CalendarValues.Calendar.PROJECTION,
                null,
                null,
                null
            );

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    Calendar calendar =
                        new Calendar(cursor.getLong(CalendarValues.Calendar.INDEX_ID));
                    calendar.name = cursor.getString(CalendarValues.Calendar.INDEX_NAME);
                    calendar.color = cursor.getInt(CalendarValues.Calendar.INDEX_COLOR);

                    calendars.add(calendar);
                }

                cursor.close();
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        return calendars;
    }

    private List<Attendee> getAttendees(long eventId) {
        List<Attendee> attendees = new ArrayList<>();

        try {
            Cursor cursor = context.getContentResolver().query(
                CalendarContract.Attendees.CONTENT_URI,
                CalendarValues.Attendee.PROJECTION,
                CalendarContract.Attendees.EVENT_ID + " = ?",
                new String[]{
                    String.valueOf(eventId)
                },
                null
            );

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    Attendee attendee = new Attendee(-1);
                    attendee.name = cursor.getString(CalendarValues.Attendee.INDEX_NAME);
                    attendee.email = cursor.getString(CalendarValues.Attendee.INDEX_EMAIL);

                    attendees.add(attendee);
                }

                cursor.close();
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        return attendees;
    }

    private Cursor createCursor() {
        Set<String> calendarIds = Settings.getCalendars();
        if (calendarIds.isEmpty()) {
            return null;
        }

        long startTime = Settings.getCalendarsStartTime();
        long currentTime = System.currentTimeMillis();

        Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, startTime);
        ContentUris.appendId(builder, currentTime);

        int size = calendarIds.size();

        String[] selection = new String[size];
        String[] selectionArgs = new String[size * 3]; // {ID, START, END}

        int i = 0;

        for (String id : calendarIds) {
            selection[i] = "(" +
                CalendarContract.Instances.CALENDAR_ID + " = ?" +
                " AND " +
                CalendarContract.Instances.BEGIN + " NOT BETWEEN ? AND ?" +
            ")";

            int index = i * 3;
            selectionArgs[index] = id;

            long start = Settings.getCalendarStartTime(id, startTime);
            selectionArgs[index + 1] = String.valueOf(start);

            long end = Settings.getCalendarEndTime(id, startTime);
            selectionArgs[index + 2] = String.valueOf(end);

            i++;
        }

        return context.getContentResolver().query(
            builder.build(),
            CalendarValues.Event.PROJECTION,
            Common.implode(" OR ", selection),
            selectionArgs,
            CalendarContract.Instances.BEGIN + " DESC"
        );
    }

    public List<Event> getNewEvents() {
        List<Event> events = new ArrayList<>();

        Cursor cursor = createCursor();

        if (cursor != null) {
            while (cursor.moveToNext()) {
                Event event = new Event(cursor.getString(CalendarValues.Event.INDEX_ID));
                event.externalId = cursor.getLong(CalendarValues.Event.INDEX_EVENT_ID);
                event.type = Event.TYPE_CALENDAR;
                event.startTime = cursor.getLong(CalendarValues.Event.INDEX_BEGIN);
                event.endTime = cursor.getLong(CalendarValues.Event.INDEX_END);
                event.title = cursor.getString(CalendarValues.Event.INDEX_TITLE);
                event.description = cursor.getString(CalendarValues.Event.INDEX_DESCRIPTION);

                String location = cursor.getString(CalendarValues.Event.INDEX_LOCATION);
                if (location != null) {
                    event.location = new Location(location);
                }

                boolean allDay = cursor.getInt(CalendarValues.Event.INDEX_ALL_DAY) != 0;
                if (allDay) {
                    event.startTime = event.endTime;
                }

                int color = cursor.getInt(CalendarValues.Event.INDEX_EVENT_COLOR);
                if (color == 0) {
                    color = cursor.getInt(CalendarValues.Event.INDEX_CALENDAR_COLOR);
                }
                event.color = color;

                event.attendees = getAttendees(event.externalId);

                events.add(event);
            }

            cursor.close();
        }

        return events;
    }

    public void resetStartTimeToNow() {
        Set<String> calendarIds = Settings.getCalendars();
        if (calendarIds.isEmpty()) {
            return;
        }

        long startTime = Settings.getCalendarsStartTime();
        long currentTime = System.currentTimeMillis();

        for (String id : calendarIds) {
            if (Settings.getCalendarStartTime(id, startTime) >= startTime) {
                Settings.setCalendarStartTime(id, startTime);
            }

            Settings.setCalendarEndTime(id, currentTime);
        }
    }

    private static class CalendarValues {

        private static class Calendar {

            public static final String[] PROJECTION = {
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.CALENDAR_COLOR
            };

            private static final int INDEX_ID = 0;
            private static final int INDEX_NAME = 1;
            private static final int INDEX_COLOR = 2;
        }

        private static class Event {

            public static final String[] PROJECTION = {
                CalendarContract.Instances._ID,
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.DESCRIPTION,
                CalendarContract.Instances.EVENT_LOCATION,
                CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.CALENDAR_COLOR,
                CalendarContract.Instances.EVENT_COLOR
            };

            private static final int INDEX_ID = 0;
            private static final int INDEX_EVENT_ID = 1;
            private static final int INDEX_BEGIN = 2;
            private static final int INDEX_END = 3;
            private static final int INDEX_TITLE = 4;
            private static final int INDEX_DESCRIPTION = 5;
            private static final int INDEX_LOCATION = 6;
            private static final int INDEX_CALENDAR_NAME = 7;
            private static final int INDEX_ALL_DAY = 8;
            private static final int INDEX_CALENDAR_COLOR = 9;
            private static final int INDEX_EVENT_COLOR = 10;
        }

        private static class Attendee {

            public static final String[] PROJECTION = {
                CalendarContract.Attendees.ATTENDEE_NAME,
                CalendarContract.Attendees.ATTENDEE_EMAIL
            };

            private static final int INDEX_NAME = 0;
            private static final int INDEX_EMAIL = 1;
        }
    }
}
