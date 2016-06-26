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

/**
 * This class is used to perform database actions related to event participants.
 *
 * @author Gary Ng
 */
public class EventAttendeeDataSource extends DataSource {

    private EventAttendeeDataSource(Database database) {
        super(database);
    }

    /**
     * Attach a specific participant to an event.
     *
     * @param eventId The value of the event ID.
     * @param attendeeId The value of the participant ID.
     */
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

    /**
     * Retrieve participants of a specific event.
     *
     * @param eventId The value of the event ID.
     * @return a list of participants.
     */
    public List<Attendee> getAttendees(String eventId) {
        List<Attendee> users = new ArrayList<>();

        String[] projection = {
            "a." + DatabaseValues.User.U_ID,
            "a." + DatabaseValues.User.MEDIA_ID,
            "a." + DatabaseValues.User.PHONE_NUMBER,
            "a." + DatabaseValues.User.EMAIL,
            "a." + DatabaseValues.User.FIRST_NAME,
            "a." + DatabaseValues.User.LAST_NAME,
            "a." + DatabaseValues.User.USER_NAME,
            "a." + DatabaseValues.User.FAVORITE,
            "a." + DatabaseValues.User.FRIEND,
            "a." + DatabaseValues.User.SUGGESTED
        };

        Cursor cursor = database.rawQuery(
            " SELECT " + Common.implode(", ", projection) +
            " FROM " + DatabaseValues.EventAttendee.TABLE + " ea" +
            " INNER JOIN " + DatabaseValues.User.TABLE + " a" +
            " ON ea." + DatabaseValues.EventAttendee.ATTENDEE_ID + " = a." + DatabaseValues.User.U_ID +
            " WHERE ea." + DatabaseValues.EventAttendee.EVENT_ID + " = ?" +
            " ORDER BY LOWER(a." + DatabaseValues.User.FIRST_NAME + ")",
            new String[]{
                eventId
            }
        );

        while (cursor.moveToNext()) {
            Attendee user = new Attendee(cursor.getString(0));
            user.mediaId = cursor.getString(1);
            user.phoneNumber = cursor.getString(2);
            user.email = cursor.getString(3);
            user.firstName = cursor.getString(4);
            user.lastName = cursor.getString(5);
            user.userName = cursor.getString(6);
            user.isFavorite = cursor.getInt(7) > 0;
            user.isFriend = cursor.getInt(8) > 0;
            user.isSuggested = cursor.getInt(9);

            users.add(user);
        }

        cursor.close();

        return users;
    }

    /**
     * Remove all participants of a specific event.
     *
     * @param eventId The value of the event ID.
     * @return the number of affected rows.
     */
    public int clearAll(String eventId) {
        return database.delete(
            DatabaseValues.EventAttendee.TABLE,
            DatabaseValues.EventAttendee.EVENT_ID + " = ?",
            new String[]{
                eventId
            }
        );
    }
}
