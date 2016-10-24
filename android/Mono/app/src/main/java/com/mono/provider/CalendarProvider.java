package com.mono.provider;

import android.content.Context;
import android.database.Cursor;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;

import com.mono.model.Calendar;
import com.mono.util.Common;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to provide access to the Calendar Provider to allow the retrieval of
 * calendars stored on the device.
 *
 * @author Gary Ng
 */
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

    /**
     * Retrieve a calendar using the ID.
     *
     * @param id ID of calendar.
     * @return Instance of calendar.
     * @throws SecurityException
     */
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

    /**
     * Retrieve all calendars.
     *
     * @return List of calendars.
     * @throws SecurityException
     */
    public List<Calendar> getCalendars() throws SecurityException {
        List<Calendar> calendars = new ArrayList<>();

        Cursor cursor = context.getContentResolver().query(
            Calendars.CONTENT_URI,
            CalendarValues.Calendar.PROJECTION,
            null,
            null,
            CalendarValues.Calendar.ACCOUNT_NAME
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

    /**
     * For PROJECTION only.
     */
    private Calendar cursorToCalendar(Cursor cursor) {
        Calendar calendar = new Calendar(cursor.getLong(CalendarValues.Calendar.INDEX_ID));
        calendar.name = cursor.getString(CalendarValues.Calendar.INDEX_NAME);
        calendar.color = cursor.getInt(CalendarValues.Calendar.INDEX_COLOR);
        calendar.timeZone = cursor.getString(CalendarValues.Calendar.INDEX_TIME_ZONE);
        calendar.owner = cursor.getString(CalendarValues.Calendar.INDEX_OWNER_ACCOUNT);
        calendar.accountName = cursor.getString(CalendarValues.Calendar.INDEX_ACCOUNT_NAME);
        calendar.accountType = cursor.getString(CalendarValues.Calendar.INDEX_ACCOUNT_TYPE);
        calendar.primary = cursor.getInt(CalendarValues.Calendar.INDEX_PRIMARY) > 0;
        calendar.local = Common.compareStrings(calendar.accountType,
            CalendarContract.ACCOUNT_TYPE_LOCAL);

        return calendar;
    }
}
