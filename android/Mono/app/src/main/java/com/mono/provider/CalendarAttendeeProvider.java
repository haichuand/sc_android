package com.mono.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract.Attendees;

import com.mono.model.Attendee;
import com.mono.model.Event;
import com.mono.util.Common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendarAttendeeProvider {

    private static CalendarAttendeeProvider instance;

    private Context context;

    private CalendarAttendeeProvider(Context context) {
        this.context = context;
    }

    public static CalendarAttendeeProvider getInstance(Context context) {
        if (instance == null) {
            instance = new CalendarAttendeeProvider(context.getApplicationContext());
        }

        return instance;
    }

    public boolean createAttendee(long eventId, String name, String email, int relationship,
            int type, int status) throws SecurityException {
        boolean result = false;

        ContentValues values = new ContentValues();
        values.put(Attendees.EVENT_ID, eventId);
        values.put(Attendees.ATTENDEE_NAME, name);
        values.put(Attendees.ATTENDEE_EMAIL, email);
        values.put(Attendees.ATTENDEE_RELATIONSHIP, relationship);
        values.put(Attendees.ATTENDEE_TYPE, type);
        values.put(Attendees.ATTENDEE_STATUS, status);

        Uri uri = context.getContentResolver().insert(Attendees.CONTENT_URI, values);

        if (uri != null) {
            result = true;
        }

        return result;
    }

    public List<Attendee> getAttendees(long eventId) {
        List<Attendee> attendees = new ArrayList<>();

        Cursor cursor = Attendees.query(
            context.getContentResolver(),
            eventId,
            CalendarValues.Attendee.PROJECTION
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                Attendee attendee = cursorToAttendee(cursor);
                attendees.add(attendee);
            }

            cursor.close();
        }

        return attendees;
    }

    public int removeAttendee(long eventId, String name) throws SecurityException {
        String selection = String.format(
            "%s = ? AND %s = ?",
            Attendees.EVENT_ID,
            Attendees.ATTENDEE_NAME
        );

        String[] selectionArgs = {
            String.valueOf(eventId),
            name
        };

        return context.getContentResolver().delete(
            Attendees.CONTENT_URI,
            selection,
            selectionArgs
        );
    }

    public void resolveAttendees(List<Event> events) {
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

    private Cursor createAttendeeCursor(List<Long> eventIds) throws SecurityException {
        int size = eventIds.size();

        String selection = String.format(
            "%s IN (%s)",
            Attendees.EVENT_ID,
            Common.repeat("?", size, ", ")
        );

        String[] selectionArgs = new String[size];
        for (int i = 0; i < size; i++) {
            selectionArgs[i] = String.valueOf(eventIds.get(i));
        }

        return context.getContentResolver().query(
            Attendees.CONTENT_URI,
            CalendarValues.Attendee.PROJECTION,
            selection,
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
}
