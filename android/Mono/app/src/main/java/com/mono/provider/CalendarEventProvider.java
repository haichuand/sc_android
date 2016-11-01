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
import org.joda.time.DateTimeZone;
import org.joda.time.Days;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This class is used to provide access to the Calendar Provider to allow the retrieval of
 * calendar events stored on the device.
 *
 * @author Gary Ng
 */
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

    /**
     * Create an event into the provider.
     *
     * @param calendarId The value of the calendar ID.
     * @param title The title of the event.
     * @param description The description of the event.
     * @param location The location of the event.
     * @param color The color of the event.
     * @param startTime The start time of the event.
     * @param endTime The end time of th event.
     * @param timeZone The time zone used for the start time.
     * @param endTimeZone The time zone used for the end time.
     * @param allDay The value of whether this is an all day event.
     * @return the event ID.
     * @throws SecurityException
     */
    public long createEvent(long calendarId, String title, String description, String location,
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

    /**
     * Retrieve an event using the ID.
     *
     * @param id The value of the event ID.
     * @return an instance of the event.
     */
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

    /**
     * Retrieve an event using the given time signature.
     *
     * @param id The value of the event ID.
     * @param startTime The start time of the event.
     * @param endTime The end time of th event.
     * @return an instance of the event.
     */
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

    /**
     * General function used to update values related to event.
     *
     * @param eventId Event of ID to be updated.
     * @param values Values to be updated.
     * @return the number of affected rows.
     * @throws SecurityException
     */
    public int updateValues(long eventId, ContentValues values) throws SecurityException {
        return context.getContentResolver().update(
            Events.CONTENT_URI,
            values,
            Events._ID + " = ?",
            new String[]{
                String.valueOf(eventId)
            }
        );
    }

    /**
     * Remove an event from the provider.
     *
     * @param eventId ID of event.
     * @return the number of affected rows.
     * @throws SecurityException
     */
    public int removeEvent(long eventId) throws SecurityException {
        return context.getContentResolver().delete(
            Events.CONTENT_URI,
            Events._ID + " = ?",
            new String[]{
                String.valueOf(eventId)
            }
        );
    }

    /**
     * Remove an instance of recurring event from the provider. If no recurring events remain,
     * the whole event series will be removed.
     *
     * @param eventId ID of event.
     * @param startTime Time of recurring event.
     * @return the exception event ID.
     */
    public long removeEvent(long eventId, long startTime) {
        long exceptionId = -1;

        boolean removeAll = false;

        long dtStart = 0;
        long lastDate = 0;

        Uri.Builder builder = Events.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, eventId);

        Cursor cursor = context.getContentResolver().query(
            builder.build(),
            new String[]{
                Events.DTSTART,
                Events.LAST_DATE
            },
            null,
            null,
            null
        );

        if (cursor != null && cursor.moveToNext()) {
            dtStart = cursor.getLong(0);
            lastDate = cursor.getLong(1);

            cursor.close();
        }

        ContentValues values = new ContentValues();
        values.put(Events.ORIGINAL_INSTANCE_TIME, startTime);
        values.put(Events.STATUS, Events.STATUS_CANCELED);

        builder = Events.CONTENT_EXCEPTION_URI.buildUpon();
        ContentUris.appendId(builder, eventId);

        Uri uri = context.getContentResolver().insert(builder.build(), values);

        if (uri != null) {
            exceptionId = Long.parseLong(uri.getLastPathSegment());

            builder = Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, dtStart);
            ContentUris.appendId(builder, lastDate);

            cursor = context.getContentResolver().query(
                builder.build(),
                new String[]{
                    Instances._ID
                },
                null,
                null,
                null
            );

            if (cursor != null) {
                removeAll = cursor.getCount() == 0;
                cursor.close();
            }
        } else {
            removeAll = true;
        }

        if (removeAll) {
            if (removeEvent(eventId) > 0) {
                exceptionId = 0;
            }
        }

        return exceptionId;
    }

    /**
     * Helper function to get the calendar IDs selection and arguments.
     *
     * @param args The list to insert arguments.
     * @param calendarIds The array of calendar IDs.
     * @return a selection string.
     */
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

    /**
     * Retrieve a calendar containing events belonging within a time range.
     *
     * @param calendarId The value of the calendar ID.
     * @param startMin The minimum start time of the event.
     * @param startMax The maximum start time of the event.
     * @param endMin The minimum end time of the event.
     * @param endMax The maximum end time of the event.
     * @return an instance of a calendar with events.
     */
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

    /**
     * Retrieve events belonging within a time range.
     *
     * @param startTime The start time of the event.
     * @param endTime The end time of the event.
     * @param calendarIds The array of calendar IDs.
     * @return a list of events.
     */
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

    /**
     * Retrieve specific events belonging within a time range
     *
     * @param startTime The start time of the event.
     * @param endTime The end time of the event.
     * @param eventIds The events to be returned.
     * @return a list of events.
     */
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

    /**
     * Retrieve events belonging within a time range starting at a specified offset.
     *
     * @param startTime The start time of the event.
     * @param offset The offset to start with.
     * @param limit The max number of results to return.
     * @param direction The ascending or descending order of events returned.
     * @param calendarIds The array of calendar IDs.
     * @return a list of events.
     */
    public List<Event> getEvents(long startTime, long endTime, int offset, int limit,
            int direction, long... calendarIds) {
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
        String order = direction < 0 ? " DESC" : "";

        Cursor cursor = context.getContentResolver().query(
            builder.build(),
            CalendarValues.Instance.PROJECTION,
            selection,
            selectionArgs,
            Instances.BEGIN + " " + order + " LIMIT " + limit + " OFFSET " + offset
        );

        if (cursor != null) {
            events.addAll(cursorInstancesToEvents(cursor));
            cursor.close();

            resolveEventsData(events);
        }

        return events;
    }

    /**
     * Retrieve events belonging to a specific day of the year.
     *
     * @param year The value of the year.
     * @param month The value of the month.
     * @param day The value of the day.
     * @param calendarIds The array of calendar IDs.
     * @return a list of events.
     */
    public List<Event> getEvents(int year, int month, int day, long... calendarIds) {
        List<Event> events = new ArrayList<>();

        month++; // Joda-Time Compatible

        DateTime dateTime = new DateTime(year, month, day, 0, 0);
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

            for (Event event : tempEvents) {
                long startMillis = event.startTime;
                long endMillis = event.endTime;

                if (!event.allDay) {
                    dateTime = dateTime.withZone(DateTimeZone.getDefault());
                } else {
                    endMillis -= 1;
                    dateTime = dateTime.withZone(DateTimeZone.UTC);
                }

                DateTime current = new DateTime(year, month, day, 0, 0);
                DateTime start = dateTime.withMillis(startMillis).withTimeAtStartOfDay();

                DateTime end = dateTime.withMillis(endMillis).withTimeAtStartOfDay();
                end = end.plusDays(1).minusMillis(1);

                if (current.isBefore(start) || current.isAfter(end)) {
                    continue;
                }

                events.add(event);
            }

            resolveEventsData(events);
        }

        return events;
    }

    /**
     * Retrieve events belonging within a time range that contains terms found in the query.
     *
     * @param startTime The start time of the event.
     * @param endTime The end time of the event.
     * @param query The filtering query.
     * @param limit The value of the limit.
     * @param calendarIds The array of calendar IDs.
     * @return a list of events.
     */
    public List<Event> getEvents(long startTime, long endTime, String query, int limit,
            long... calendarIds) {
        List<Event> events = new ArrayList<>();

        Uri.Builder builder = Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, startTime);
        ContentUris.appendId(builder, endTime);

        List<String> args = new ArrayList<>();

        String selection = "";
        if (calendarIds != null && calendarIds.length > 0) {
            selection = getCalendarSelection(args, calendarIds);
        }

        String querySelection = "";
        String[] terms = Common.explode(" ", query);
        for (int i = 0; i < terms.length; i++) {
            if (i > 0) querySelection += " AND ";

            querySelection += "(";

            String[] fields = {
                CalendarValues.Event.CALENDAR_NAME,
                CalendarValues.Event.TITLE,
                CalendarValues.Event.DESC,
                CalendarValues.Event.LOCATION
            };

            for (int j = 0; j < fields.length; j++) {
                if (j > 0) querySelection += " OR ";
                querySelection += fields[j] + " LIKE '%' || ? || '%'";
                args.add(terms[i]);
            }

            querySelection += ")";
        }
        selection += String.format("(%s)", querySelection);

        String[] selectionArgs = args.toArray(new String[args.size()]);

        Cursor cursor = context.getContentResolver().query(
            builder.build(),
            CalendarValues.Instance.PROJECTION,
            selection,
            selectionArgs,
            Instances.BEGIN + " DESC LIMIT " + limit
        );

        if (cursor != null) {
            events.addAll(cursorInstancesToEvents(cursor));
            cursor.close();

            resolveEventsData(events);
        }

        return events;
    }

    /**
     * Retrieve events with reminders belonging within a time range.
     *
     * @param startTime Start time of the events.
     * @param endTime End time of the events.
     * @param calendarIds Restrict events to these calendars.
     * @return a list of events.
     */
    public List<Event> getEventsWithReminders(long startTime, long endTime, long... calendarIds) {
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
            Instances.BEGIN
        );

        if (cursor != null) {
            events.addAll(cursorInstancesToEvents(cursor));
            cursor.close();

            resolveEventsData(events);

            Iterator<Event> iterator = events.iterator();
            while (iterator.hasNext()) {
                Event event = iterator.next();
                if (event.reminders == null || event.reminders.isEmpty()) {
                    iterator.remove();
                }
            }
        }

        return events;
    }

    /**
     * Retrieve all color markers for the specific month.
     *
     * @param year The value of the year.
     * @param month The value of the month.
     * @param calendarIds The array of calendar IDs.
     * @return a map of colors for each day of the month.
     */
    public Map<Integer, List<Integer>> getEventColors(int year, int month, long... calendarIds) {
        Map<Integer, List<Integer>> result = new HashMap<>();

        month++; // Joda-Time Compatible

        DateTime dateTime = new DateTime(year, month, 1, 0, 0);
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
            while (cursor.moveToNext()) {
                int calendarColor = cursor.getInt(0);
                int eventColor = cursor.getInt(1);
                long startMillis = cursor.getLong(2);
                long endMillis = cursor.getLong(3);
                boolean allDay = cursor.getInt(4) > 0;

                int color = eventColor == 0 ? calendarColor : eventColor;

                if (!allDay) {
                    dateTime = dateTime.withZone(DateTimeZone.getDefault());
                } else {
                    endMillis -= 1;
                    dateTime = dateTime.withZone(DateTimeZone.UTC);
                }

                DateTime start = dateTime.withMillis(startMillis).withTimeAtStartOfDay();
                DateTime end = dateTime.withMillis(endMillis).withTimeAtStartOfDay();
                int numDays = Days.daysBetween(start, end).getDays() + 1;

                dateTime = dateTime.withMillis(startMillis);

                for (int i = 0; i < numDays; i++) {
                    if (dateTime.getMonthOfYear() == month) {
                        int currentDay = dateTime.getDayOfMonth();

                        if (!result.containsKey(currentDay)) {
                            result.put(currentDay, new ArrayList<Integer>());
                        }

                        List<Integer> colors = result.get(currentDay);
                        if (!colors.contains(color)) {
                            colors.add(color);
                        }
                    }

                    dateTime = dateTime.plusDays(1);
                }
            }

            cursor.close();
        }

        return result;
    }

    /**
     * Retrieve a calendar containing events updated within a time range.
     *
     * @param calendarId The value of the calendar ID.
     * @param startTime The start time of the event updates.
     * @param endTime The end time of the event updates.
     * @return an instance of a calendar with events.
     * @throws SecurityException
     */
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

        DateTime dateTime = new DateTime(startTime);
        String start = DATE_FORMAT.format(dateTime.toDate());

        dateTime = dateTime.withMillis(endTime);
        String end = DATE_FORMAT.format(dateTime.toDate());

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
                eventIds.add(event.providerId);
            }

            cursor.close();

            if (!eventIds.isEmpty()) {
                calendar.events.addAll(getEvents(0, endTime, eventIds));
                resolveEventsData(calendar.events);
            }
        }

        return calendar;
    }

    /**
     * Helper function to retrieve attendees and reminders for the given events.
     *
     * @param events The list of events to resolve.
     */
    private void resolveEventsData(List<Event> events) {
        if (events.isEmpty()) {
            return;
        }

        CalendarAttendeeProvider.getInstance(context).resolveAttendees(events);
        CalendarReminderProvider.getInstance(context).resolveReminders(events);
    }

    /**
     * Create an events cursor using the given event IDs.
     *
     * @param eventIds The list of event IDs.
     * @return an instance of a cursor.
     * @throws SecurityException
     */
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

    /**
     * Create an instances cursor using the given time range.
     *
     * @param startMin The minimum start time of the event.
     * @param startMax The maximum start time of the event.
     * @param endMin The minimum end time of the event.
     * @param endMax The maximum end time of the event.
     * @param calendarIds The array of calendar IDs.
     * @return an instance of a cursor.
     */
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
        String id = createId(
            cursor.getLong(CalendarValues.Event.INDEX_ID),
            cursor.getLong(CalendarValues.Event.INDEX_START_TIME),
            cursor.getLong(CalendarValues.Event.INDEX_END_TIME)
        );

        long providerId = cursor.getLong(CalendarValues.Event.INDEX_ID);

        Event event = new Event(id, providerId, null, Event.TYPE_CALENDAR);
        event.calendarId = cursor.getLong(CalendarValues.Event.INDEX_CALENDAR_ID);

        event.title = cursor.getString(CalendarValues.Event.INDEX_TITLE);
        if (event.title != null && event.title.isEmpty()) {
            event.title = null;
        }

        event.description = cursor.getString(CalendarValues.Event.INDEX_DESC);
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
        event.organizer = cursor.getString(CalendarValues.Event.INDEX_ORGANIZER);

        try {
            String dateTime = cursor.getString(CalendarValues.Event.INDEX_UPDATE_TIME);
            if (dateTime != null) {
                event.updateTime = DATE_FORMAT.parse(dateTime).getTime();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return event;
    }

    private Instance cursorToInstance(Cursor cursor) {
        String id = createId(
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

    /**
     * Create a composite ID using the time signature.
     *
     * @param eventId The value of the event ID.
     * @param startTime The start time of the event.
     * @param endTime The end time of the event.
     * @return a string representing the composite ID.
     */
    public static String createId(long eventId, long startTime, long endTime) {
        return String.format("%d.%d.%d", eventId, startTime, endTime);
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
