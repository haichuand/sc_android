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

    public List<Reminder> getReminders(long eventId) {
        List<Reminder> reminders = new ArrayList<>();

        Cursor cursor = Reminders.query(
            context.getContentResolver(),
            eventId,
            CalendarValues.Attendee.PROJECTION
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                Reminder attendee = cursorToReminder(cursor);
                reminders.add(attendee);
            }

            cursor.close();
        }

        return reminders;
    }

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
