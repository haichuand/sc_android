package com.mono.db.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;

import com.mono.db.Database;
import com.mono.db.DatabaseValues;
import com.mono.model.Event;
import com.mono.model.Location;
import com.mono.model.Reminder;
import com.mono.util.Common;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class is used to perform database actions related to events.
 *
 * @author Gary Ng
 */
public class EventDataSource extends DataSource {

    private EventDataSource(Database database) {
        super(database);
    }

    /**
     * Create an event into the database.
     *
     * @param calendarId The value of the calendar ID.
     * @param internalId The event ID being used in the Calendar Provider.
     * @param externalId The event ID being used by Google Calendar.
     * @param type The type of event.
     * @param title The title of the event.
     * @param description The description of the event.
     * @param locationId The location ID of the event.
     * @param color The color of the event.
     * @param startTime The start time of the event.
     * @param endTime The end time of th event.
     * @param timeZone The time zone used for the start time.
     * @param endTimeZone The time zone used for the end time.
     * @param allDay The value of whether this is an all day event.
     * @param reminders Format: minutes,method;minutes,method;...
     * @return the event ID.
     */
    public String createEvent(long calendarId, long internalId, String externalId, String type,
            String title, String description, Long locationId, int color, long startTime,
            long endTime, String timeZone, String endTimeZone, int allDay, String reminders) {
        String id = DataSource.UniqueIdGenerator(this.getClass().getSimpleName());

        ContentValues values = new ContentValues();
        values.put(DatabaseValues.Event.ID, id);
        values.put(DatabaseValues.Event.CALENDAR_ID, calendarId);
        values.put(DatabaseValues.Event.INTERNAL_ID, internalId);
        values.put(DatabaseValues.Event.EXTERNAL_ID, externalId);
        values.put(DatabaseValues.Event.TYPE, type);
        values.put(DatabaseValues.Event.TITLE, title);
        values.put(DatabaseValues.Event.DESC, description);
        values.put(DatabaseValues.Event.LOCATION_ID, locationId);
        values.put(DatabaseValues.Event.COLOR, color);
        values.put(DatabaseValues.Event.START_TIME, startTime);
        values.put(DatabaseValues.Event.END_TIME, endTime);
        values.put(DatabaseValues.Event.TIMEZONE, timeZone);
        values.put(DatabaseValues.Event.END_TIMEZONE, endTimeZone);
        values.put(DatabaseValues.Event.ALL_DAY, allDay);
        values.put(DatabaseValues.Event.REMINDERS, reminders);

        try {
            database.insert(DatabaseValues.Event.TABLE, values);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return id;
    }

    public boolean createEvent(String eventId, long calendarId, long internalId, String externalId, String type,
                              String title, String description, Long locationId, int color, long startTime,
                              long endTime, String timeZone, String endTimeZone, int allDay, String reminders) {

        ContentValues values = new ContentValues();
        values.put(DatabaseValues.Event.ID, eventId);
        values.put(DatabaseValues.Event.CALENDAR_ID, calendarId);
        values.put(DatabaseValues.Event.INTERNAL_ID, internalId);
        values.put(DatabaseValues.Event.EXTERNAL_ID, externalId);
        values.put(DatabaseValues.Event.TYPE, type);
        values.put(DatabaseValues.Event.TITLE, title);
        values.put(DatabaseValues.Event.DESC, description);
        values.put(DatabaseValues.Event.LOCATION_ID, locationId);
        values.put(DatabaseValues.Event.COLOR, color);
        values.put(DatabaseValues.Event.START_TIME, startTime);
        values.put(DatabaseValues.Event.END_TIME, endTime);
        values.put(DatabaseValues.Event.TIMEZONE, timeZone);
        values.put(DatabaseValues.Event.END_TIMEZONE, endTimeZone);
        values.put(DatabaseValues.Event.ALL_DAY, allDay);
        values.put(DatabaseValues.Event.REMINDERS, reminders);

        try {
            database.insert(DatabaseValues.Event.TABLE, values);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public int updateEventId(String originalId, String newId) {
        try {
            ContentValues values = new ContentValues();
            values.put(DatabaseValues.Event.ID, newId);
            return updateValues(originalId, values);
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Retrieve an event using the ID.
     *
     * @param id The value of the event ID.
     * @return an instance of the event.
     */
    public Event getEvent(String id) {
        Event event = null;

        Cursor cursor = database.select(
            DatabaseValues.Event.TABLE,
            DatabaseValues.Event.PROJECTION,
            DatabaseValues.Event.ID + " = ?",
            new String[]{
                id
            }
        );

        if (cursor.moveToNext()) {
            event = cursorToEvent(cursor);
        }

        cursor.close();

        return event;
    }

    /**
     * Retrieve an event using the given time signature.
     *
     * @param internalId The event ID being used in the Calendar Provider.
     * @param startTime The start time of the event.
     * @param endTime The end time of th event.
     * @return an instance of the event.
     */
    public Event getEvent(long internalId, long startTime, long endTime) {
        Event event = null;

        String selection = String.format(
            "%s = ? AND %s = ? AND %s = ?",
            DatabaseValues.Event.INTERNAL_ID,
            DatabaseValues.Event.START_TIME,
            DatabaseValues.Event.END_TIME
        );

        String[] selectionArgs = new String[]{
            String.valueOf(internalId),
            String.valueOf(startTime),
            String.valueOf(endTime)
        };

        Cursor cursor = database.select(
            DatabaseValues.Event.TABLE,
            DatabaseValues.Event.PROJECTION,
            selection,
            selectionArgs
        );

        if (cursor.moveToNext()) {
            event = cursorToEvent(cursor);
        }

        cursor.close();

        return event;
    }

    /**
     * Find events given start time and end time
     * @param startTime
     * @param endTime
     * @return
     */
    public List<Event> getEvents(long startTime, long endTime) {
        List<Event> events = new LinkedList<>();
        String selection = String.format(
                "%s = ? AND %s = ?",
                DatabaseValues.Event.START_TIME,
                DatabaseValues.Event.END_TIME
        );

        String[] selectionArgs = new String[]{
                String.valueOf(startTime),
                String.valueOf(endTime)
        };

        Cursor cursor = database.select(
                DatabaseValues.Event.TABLE,
                DatabaseValues.Event.PROJECTION,
                selection,
                selectionArgs
        );

        while (cursor.moveToNext()) {
            events.add(cursorToEvent(cursor));
        }

        cursor.close();
        return events;
    }

    /**
     * Check if an event exists using the given time signature.
     *
     * @param internalId The event ID being used in the Calendar Provider.
     * @param startTime The start time of the event.
     * @param endTime The end time of th event.
     * @return the value whether there exists an event.
     */
    public boolean containsEvent(long internalId, long startTime, long endTime) {
        boolean status = false;

        String selection = String.format(
            "%s = ? AND %s = ? AND %s = ?",
            DatabaseValues.Event.INTERNAL_ID,
            DatabaseValues.Event.START_TIME,
            DatabaseValues.Event.END_TIME
        );

        String[] selectionArgs = new String[]{
            String.valueOf(internalId),
            String.valueOf(startTime),
            String.valueOf(endTime)
        };

        Cursor cursor = database.select(
            DatabaseValues.Event.TABLE,
            new String[]{
                "1"
            },
            selection,
            selectionArgs
        );

        if (cursor.moveToNext()) {
            status = true;
        }

        cursor.close();

        return status;
    }

    /**
     * Retrieve events using the internal ID.
     *
     * @param internalId The value of the internal ID.
     * @return a list of events.
     */
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

    /**
     * General function used to update values into the database.
     *
     * @param id The value of the event ID.
     * @param values The values to be updated.
     * @return the number of affected rows.
     */
    public int updateValues(String id, ContentValues values) {
        return database.update(
            DatabaseValues.Event.TABLE,
            values,
            DatabaseValues.Event.ID + " = ?",
            new String[]{
                id
            }
        );
    }

    /**
     * Update event with the following start and end time.
     *
     * @param id The value of the event ID.
     * @param startTime The start time of the event.
     * @param endTime The end time of the event.
     * @return the number of affected rows.
     */
    public int updateTime(String id, long startTime, long endTime) {
        ContentValues values = new ContentValues();
        values.put(DatabaseValues.Event.START_TIME, startTime);
        values.put(DatabaseValues.Event.END_TIME, endTime);

        return updateValues(id, values);
    }

    /**
     * Update event with the following location ID.
     *
     * @param id The value of the event ID.
     * @param locationId The value of the location ID.
     * @return the number of affected rows.
     */
    public int updateLocation(String id, Long locationId) {
        ContentValues values = new ContentValues();
        values.put(DatabaseValues.Event.LOCATION_ID, locationId);

        return updateValues(id, values);
    }

    /**
     * Remove an event from the database.
     *
     * @param id The value of the event ID.
     * @return the number of affected rows.
     */
    public int removeEvent(String id) {
        return database.delete(
            DatabaseValues.Event.TABLE,
            DatabaseValues.Event.ID + " = ?",
            new String[]{
                id
            }
        );
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
            "(%s <= 0 OR %s IN (%s))",
            DatabaseValues.Event.CALENDAR_ID,
            DatabaseValues.Event.CALENDAR_ID,
            Common.repeat("?", calendarIds.length, ", ")
        );

        for (long calendarId : calendarIds) {
            args.add(String.valueOf(calendarId));
        }

        return selection;
    }

    /**
     * Helper function to get time range selection and arguments.
     *
     * @param args The list to insert arguments.
     * @param startTime The start time of the event.
     * @param endTime The end time of the event.
     * @return a selection string.
     */
    private String getTimeSelection(List<String> args, long startTime, long endTime) {
        String selection = String.format(
            "((%s BETWEEN ? AND ? OR %s BETWEEN ? AND ?) OR (%s <= ? AND %s >= ? OR %s >= ? AND %s <= ?))",
            DatabaseValues.Event.START_TIME,
            DatabaseValues.Event.END_TIME,
            DatabaseValues.Event.START_TIME,
            DatabaseValues.Event.END_TIME,
            DatabaseValues.Event.END_TIME,
            DatabaseValues.Event.START_TIME
        );

        args.add(String.valueOf(startTime));
        args.add(String.valueOf(endTime));
        args.add(String.valueOf(startTime));
        args.add(String.valueOf(endTime));
        args.add(String.valueOf(startTime));
        args.add(String.valueOf(startTime));
        args.add(String.valueOf(endTime));
        args.add(String.valueOf(endTime));

        return selection;
    }

    /**
     * Retrieve events after a specific time.
     *
     * @param startTime The start time of the event.
     * @param offset The offset to start with.
     * @param limit The max number of results to return.
     * @param direction The ascending or descending order of events returned.
     * @param calendarIds The array of calendar IDs.
     * @return a list of events.
     */
    public List<Event> getEvents(long startTime, int offset, int limit, int direction,
            long... calendarIds) {
        List<Event> events = new ArrayList<>();

        List<String> args = new ArrayList<>();

        String selection = "";
        if (calendarIds != null && calendarIds.length > 0) {
            selection = getCalendarSelection(args, calendarIds) + " AND ";
        }

        String operator, order;
        if (direction >= 0) {
            operator = ">";
            order = " ASC";
        } else {
            operator = "<";
            order = " DESC";
        }

        selection += DatabaseValues.Event.START_TIME + " " + operator + " ?";
        args.add(String.valueOf(startTime));

        String[] selectionArgs = args.toArray(new String[args.size()]);

        Cursor cursor = database.select(
            DatabaseValues.Event.TABLE,
            DatabaseValues.Event.PROJECTION,
            selection,
            selectionArgs,
            null,
            DatabaseValues.Event.START_TIME + " " + order,
            offset,
            limit
        );

        while (cursor.moveToNext()) {
            Event event = cursorToEvent(cursor);
            events.add(event);
        }

        cursor.close();

        return events;
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
            DatabaseValues.Event.START_TIME + " DESC"
        );

        while (cursor.moveToNext()) {
            Event event = cursorToEvent(cursor);
            events.add(event);
        }

        cursor.close();

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
            DatabaseValues.Event.START_TIME + " DESC"
        );

        while (cursor.moveToNext()) {
            Event event = cursorToEvent(cursor);

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

        cursor.close();

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

        List<String> args = new ArrayList<>();

        String selection = "";
        if (calendarIds != null && calendarIds.length > 0) {
            selection = getCalendarSelection(args, calendarIds) + " AND ";
        }

        selection += getTimeSelection(args, startTime, endTime) + " AND ";

        String querySelection = "";
        String[] terms = Common.explode(" ", query);
        for (int i = 0; i < terms.length; i++) {
            if (i > 0) querySelection += " AND ";

            querySelection += "(";

            String[] fields = {
                DatabaseValues.Event.TITLE,
                DatabaseValues.Event.DESC,
                DatabaseValues.Event.LOCATION_ID
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

        Cursor cursor = database.select(
            DatabaseValues.Event.TABLE,
            DatabaseValues.Event.PROJECTION,
            selection,
            selectionArgs,
            null,
            DatabaseValues.Event.START_TIME + " DESC",
            null,
            limit
        );

        while (cursor.moveToNext()) {
            Event event = cursorToEvent(cursor);
            events.add(event);
        }

        cursor.close();

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

        List<String> args = new ArrayList<>();

        String selection = "";
        if (calendarIds != null && calendarIds.length > 0) {
            selection = getCalendarSelection(args, calendarIds) + " AND ";
        }

        selection += String.format(
            "%s BETWEEN ? AND ?",
            DatabaseValues.Event.START_TIME
        );
        args.add(String.valueOf(startTime));
        args.add(String.valueOf(endTime));

        selection += " AND ";
        selection += DatabaseValues.Event.REMINDERS + " IS NOT NULL";

        String[] selectionArgs = args.toArray(new String[args.size()]);

        Cursor cursor = database.select(
            DatabaseValues.Event.TABLE,
            DatabaseValues.Event.PROJECTION,
            selection,
            selectionArgs,
            DatabaseValues.Event.START_TIME
        );

        while (cursor.moveToNext()) {
            Event event = cursorToEvent(cursor);
            events.add(event);
        }

        cursor.close();

        return events;
    }

    /**
     * Retrieve event IDs belonging within a time range as well as within a latitude and longitude
     * bounding box.
     *
     * @param startTime The start time of the event.
     * @param endTime The end time of the event.
     * @param latLng The value containing latitude and longitude.
     * @param bounds The value containing lower and upper bounds in latitude and longitude.
     * @param offset The offset to start with.
     * @param limit The max number of results to return.
     * @param calendarIds The array of calendar IDs.
     * @return a list of event IDs.
     */
    public List<String> getEventIds(long startTime, long endTime, double[] latLng, double[] bounds,
            int offset, int limit, long... calendarIds) {
        List<String> result = new ArrayList<>();

        String DISTANCE = "`distance`";

        String[] projection = {
            "e." + DatabaseValues.Event.ID,
            DatabaseValues.Event.CALENDAR_ID,
            DatabaseValues.Event.LOCATION_ID,
            DatabaseValues.Event.START_TIME,
            DatabaseValues.Event.END_TIME,
            DatabaseValues.Location.LATITUDE,
            DatabaseValues.Location.LONGITUDE,
            getLatLongDistanceSquared(latLng) + " AS " + DISTANCE
        };

        String table = "(" +
            " SELECT " + Common.implode(", ", projection) +
            " FROM " + DatabaseValues.Event.TABLE + " e" +
            " INNER JOIN " + DatabaseValues.Location.TABLE + " l" +
            " ON e." + DatabaseValues.Event.LOCATION_ID + " = l." + DatabaseValues.Location.ID +
        ")";

        List<String> args = new ArrayList<>();

        String selection = getTimeSelection(args, startTime, endTime);

        if (bounds != null) {
            selection += " AND ";
            selection += getLatLongBoundsSelection(args, bounds);
        }

        if (calendarIds != null && calendarIds.length > 0) {
            selection += " AND ";
            selection += getCalendarSelection(args, calendarIds);
        }

        String[] selectionArgs = args.toArray(new String[args.size()]);

        Cursor cursor = database.select(
            table,
            new String[]{
                DatabaseValues.Event.ID,
                DatabaseValues.Event.LOCATION_ID,
                DatabaseValues.Location.LATITUDE,
                DatabaseValues.Location.LONGITUDE,
                DISTANCE
            },
            selection,
            selectionArgs,
            null,
            DatabaseValues.Event.START_TIME + " DESC, " + DISTANCE,
            offset,
            limit
        );

        while (cursor.moveToNext()) {
            String eventId = cursor.getString(0);
            result.add(eventId);
        }

        cursor.close();

        return result;
    }

    /**
     * Helper function to return a string to calculate distance of all locations to the given
     * position.
     *
     * @param latLng The value containing latitude and longitude.
     * @return a selection string.
     */
    private String getLatLongDistanceSquared(double[] latLng) {
        String latExpr = "(" + DatabaseValues.Location.LATITUDE + " - " + latLng[0] + ")";
        String longExpr = "(" + DatabaseValues.Location.LONGITUDE + " - " + latLng[1] + ")";

        return String.format("%s * %s + %s * %s", latExpr, latExpr, longExpr, longExpr);
    }

    /**
     * Helper function to get the latitude and longitude bounds selection and arguments.
     *
     * @param args The list to insert arguments.
     * @param bounds The value containing lower and upper bounds in latitude and longitude.
     * @return a selection string.
     */
    private String getLatLongBoundsSelection(List<String> args, double[] bounds) {
        String selection = String.format(
            "(%s BETWEEN ? AND ? AND %s BETWEEN ? AND ?)",
            DatabaseValues.Location.LATITUDE,
            DatabaseValues.Location.LONGITUDE
        );

        double minLat = bounds[0], minLong = bounds[1];
        double maxLat = bounds[2], maxLong = bounds[3];

        args.add(String.valueOf(minLat));
        args.add(String.valueOf(maxLat));
        args.add(String.valueOf(minLong));
        args.add(String.valueOf(maxLong));

        return selection;
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

        List<String> args = new ArrayList<>();

        String selection = "";
        if (calendarIds != null && calendarIds.length > 0) {
            selection = getCalendarSelection(args, calendarIds) + " AND ";
        }

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
            DatabaseValues.Event.START_TIME + " DESC"
        );

        while (cursor.moveToNext()) {
            int color = cursor.getInt(0);
            long startMillis = cursor.getLong(1);
            long endMillis = cursor.getLong(2);
            boolean allDay = cursor.getInt(3) > 0;

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

        return result;
    }

    public Event getUserstayEventByStartTime(long startTime) {
        Event event = null;

        Cursor cursor = database.select(
            DatabaseValues.Event.TABLE,
            DatabaseValues.Event.PROJECTION,
            DatabaseValues.Event.START_TIME + " = ?" +
            " AND " + DatabaseValues.Event.TYPE + " = ?",
            new String[]{
                String.valueOf(startTime),
                String.valueOf(Event.TYPE_USERSTAY)
            }
        );

        if (cursor.moveToNext()) {
            event = cursorToEvent(cursor);
        }

        cursor.close();

        return event;
    }

    public Event getUserstayEventByEndTime(long endTime) {
        Event event = null;

        Cursor cursor = database.select(
            DatabaseValues.Event.TABLE,
            DatabaseValues.Event.PROJECTION,
            DatabaseValues.Event.END_TIME + " = ?" +
            " AND " + DatabaseValues.Event.TYPE + " = ?",
            new String[]{
                String.valueOf(endTime),
                String.valueOf(Event.TYPE_USERSTAY)
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
        event.source = Event.SOURCE_DATABASE;
        event.internalId = cursor.getLong(DatabaseValues.Event.INDEX_INTERNAL_ID);
        event.externalId = cursor.getString(DatabaseValues.Event.INDEX_EXTERNAL_ID);
        event.type = cursor.getString(DatabaseValues.Event.INDEX_TYPE);
        event.title = cursor.getString(DatabaseValues.Event.INDEX_TITLE);
        event.description = cursor.getString(DatabaseValues.Event.INDEX_DESC);

        long locationId = cursor.getLong(DatabaseValues.Event.INDEX_LOCATION_ID);
        if (locationId > 0) {
            event.location = new Location(locationId);
        }

        event.color = cursor.getInt(DatabaseValues.Event.INDEX_COLOR);
        event.startTime = cursor.getLong(DatabaseValues.Event.INDEX_START_TIME);
        event.endTime = cursor.getLong(DatabaseValues.Event.INDEX_END_TIME);
        event.timeZone = cursor.getString(DatabaseValues.Event.INDEX_TIMEZONE);
        event.endTimeZone = cursor.getString(DatabaseValues.Event.INDEX_END_TIMEZONE);
        event.allDay = cursor.getInt(DatabaseValues.Event.INDEX_ALL_DAY) != 0;

        String reminders = cursor.getString(DatabaseValues.Event.INDEX_REMINDERS);
        if (reminders != null) {
            for (String value : Common.explode(";", reminders)) {
                if (value.isEmpty()) {
                    continue;
                }

                String[] values = Common.explode(",", value);

                Reminder reminder = new Reminder();
                reminder.minutes = Integer.parseInt(values[0]);
                reminder.method = Integer.parseInt(values[1]);

                event.reminders.add(reminder);
            }
        }

        return event;
    }
}
