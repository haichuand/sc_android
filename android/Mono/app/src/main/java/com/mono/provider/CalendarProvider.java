package com.mono.provider;

import android.content.Context;
import android.database.Cursor;
import android.provider.CalendarContract.Calendars;

import com.mono.model.Calendar;

import java.util.ArrayList;
import java.util.List;

public class CalendarProvider {

    private static CalendarProvider instance;

    private Context context;

    private CalendarProvider(Context context) {
        this.context = context;
    }

    public static CalendarProvider getInstance(Context context) {
        if (instance == null) {
            instance = new CalendarProvider(context.getApplicationContext());
        }

        return instance;
    }

    public Calendar getCalendar(long id) throws SecurityException {
        Calendar calendar = null;

        Cursor cursor = context.getContentResolver().query(
            Calendars.CONTENT_URI,
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

    public List<Calendar> getCalendars() throws SecurityException {
        List<Calendar> calendars = new ArrayList<>();

        Cursor cursor = context.getContentResolver().query(
            Calendars.CONTENT_URI,
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
        calendar.timeZone = cursor.getString(CalendarValues.Calendar.INDEX_TIME_ZONE);
        calendar.owner = cursor.getString(CalendarValues.Calendar.INDEX_OWNER_ACCOUNT);
        calendar.accountName = cursor.getString(CalendarValues.Calendar.INDEX_ACCOUNT_NAME);
        calendar.accountType = cursor.getString(CalendarValues.Calendar.INDEX_ACCOUNT_TYPE);

        return calendar;
    }
}
