package com.mono.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract.Reminders;

import com.mono.model.Event;
import com.mono.model.Reminder;
import com.mono.util.Common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is used to provide access to the Calendar Reminder Provider to allow the retrieval
 * of calendar event reminders stored on the device.
 *
 * @author Gary Ng
 */
public class CalendarReminderProvider {

    private static CalendarReminderProvider instance;

    private Context context;

    private CalendarReminderProvider(Context context) {
        this.context = context;
    }

    public static CalendarReminderProvider getInstance(Context context) {
        if (instance == null) {
            instance = new CalendarReminderProvider(context.getApplicationContext());
        }

        return instance;
    }

    /**
     * Create an event reminder into the provider.
     *
     * @param eventId Event ID of the reminder.
     * @param minutes Time in minutes.
     * @param method Type of reminder.
     * @return the result status.
     * @throws SecurityException
     */
    public boolean createReminder(long eventId, int minutes, int method) throws SecurityException {
        boolean result = false;

        ContentValues values = new ContentValues();
        values.put(Reminders.EVENT_ID, eventId);
        values.put(Reminders.MINUTES, minutes);
        values.put(Reminders.METHOD, method);

        Uri uri = context.getContentResolver().insert(Reminders.CONTENT_URI, values);

        if (uri != null) {
            result = true;
        }

        return result;
    }

    /**
     * Retrieve reminders for an event.
     *
     * @param eventId Event ID of the reminders.
     * @return a list of reminders.
     */
    public List<Reminder> getReminders(long eventId) {
        List<Reminder> reminders = new ArrayList<>();

        Cursor cursor = Reminders.query(
            context.getContentResolver(),
            eventId,
            CalendarValues.Reminder.PROJECTION
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                Reminder reminder = cursorToReminder(cursor);
                reminders.add(reminder);
            }

            cursor.close();
        }

        return reminders;
    }

    /**
     * Remove a reminder with the given signature.
     *
     * @param eventId Event ID of the reminder.
     * @param minutes Time in minutes.
     * @return the number of affected rows.
     * @throws SecurityException
     */
    public int removeReminder(long eventId, int minutes) throws SecurityException {
        String selection = String.format(
            "%s = ? AND %s = ?",
            Reminders.EVENT_ID,
            Reminders.MINUTES
        );

        String[] selectionArgs = {
            String.valueOf(eventId),
            String.valueOf(minutes)
        };

        return context.getContentResolver().delete(
            Reminders.CONTENT_URI,
            selection,
            selectionArgs
        );
    }

    /**
     * Remove all reminders of a specific event.
     *
     * @param eventId Event ID of the reminders.
     * @return the number of affected rows.
     * @throws SecurityException
     */
    public int clearAll(long eventId) throws SecurityException {
        return context.getContentResolver().delete(
            Reminders.CONTENT_URI,
            Reminders.EVENT_ID + " = ?",
            new String[]{
                String.valueOf(eventId)
            }
        );
    }

    /**
     * Retrieve reminders for all the following events.
     *
     * @param events Events needed to be resolved.
     */
    public void resolveReminders(List<Event> events) {
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

    /**
     * Create a reminder cursor using the given event IDs.
     *
     * @param eventIds Event IDs for the cursor.
     * @return an instance of a cursor.
     * @throws SecurityException
     */
    private Cursor createReminderCursor(List<Long> eventIds) throws SecurityException {
        int size = eventIds.size();

        String selection = String.format(
            "%s IN (%s)",
            Reminders.EVENT_ID,
            Common.repeat("?", size, ", ")
        );

        String[] selectionArgs = new String[size];
        for (int i = 0; i < size; i++) {
            selectionArgs[i] = String.valueOf(eventIds.get(i));
        }

        return context.getContentResolver().query(
            Reminders.CONTENT_URI,
            CalendarValues.Reminder.PROJECTION,
            selection,
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
}
