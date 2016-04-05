package com.mono.db.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;

import com.mono.db.Database;
import com.mono.db.DatabaseValues;
import com.mono.model.Attendee;
import com.mono.util.Common;

import java.util.ArrayList;
import java.util.List;

public class EventAttendeeDataSource extends DataSource {

    private EventAttendeeDataSource(Database database) {
        super(database);
    }

    public void setAttendee(String eventId, String attendeeId) {
        ContentValues values = new ContentValues();
        values.put(DatabaseValues.EventAttendee.EVENT_ID, eventId);
        values.put(DatabaseValues.EventAttendee.ATTENDEE_ID, attendeeId);

        try {
            database.insert(DatabaseValues.EventAttendee.TABLE, values);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Attendee> getAttendees(String eventId) {
        List<Attendee> attendees = new ArrayList<>();

        String[] projection = {
            "a." + DatabaseValues.User.U_ID,
            "a." + DatabaseValues.User.USER_NAME,
            "a." + DatabaseValues.User.EMAIL
        };

        Cursor cursor = database.rawQuery(
            " SELECT " + Common.implode(", ", projection) +
            " FROM " + DatabaseValues.EventAttendee.TABLE + " ea" +
            " INNER JOIN " + DatabaseValues.User.TABLE + " a" +
            " ON ea." + DatabaseValues.EventAttendee.ATTENDEE_ID + " = a." + DatabaseValues.User.U_ID +
            " WHERE ea." + DatabaseValues.EventAttendee.EVENT_ID + " = ?" +
            " ORDER BY a." + DatabaseValues.User.USER_NAME,
            new String[]{
                eventId
            }
        );

        while (cursor.moveToNext()) {
            Attendee attendee = new Attendee(cursor.getString(0));
            attendee.userName = cursor.getString(1);
            attendee.email = cursor.getString(2);
            attendees.add(attendee);
        }

        cursor.close();

        return attendees;
    }
}
