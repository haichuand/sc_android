package com.mono.db.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;

import com.mono.db.Database;
import com.mono.db.DatabaseValues;
import com.mono.model.Attendee;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xuejing on 3/28/16.
 */
public class AttendeeDataSource extends DataSource {

    private AttendeeDataSource(Database database) {
        super(database);
    }

    public String createAttendee(String mediaId, String email, String phoneNumber,
            String firstName, String lastName, String userName, boolean favorite, boolean friend) {
        String id = DataSource.UniqueIdGenerator(this.getClass().getSimpleName());

        ContentValues values = new ContentValues();
        values.put(DatabaseValues.User.U_ID, id);
        values.put(DatabaseValues.User.MEDIA_ID, mediaId);
        values.put(DatabaseValues.User.EMAIL, email);
        values.put(DatabaseValues.User.PHONE_NUMBER, phoneNumber);
        values.put(DatabaseValues.User.FIRST_NAME, firstName);
        values.put(DatabaseValues.User.LAST_NAME, lastName);
        values.put(DatabaseValues.User.USER_NAME, userName);
        values.put(DatabaseValues.User.FAVORITE, favorite ? 1 : 0);
        values.put(DatabaseValues.User.FRIEND, friend ? 1 : 0);

        try {
            database.insert(DatabaseValues.User.TABLE, values);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return id;
    }

    public boolean createAttendeeWithAttendeeId(String attendeeId, String mediaId, String email,
            String phoneNumber, String firstName, String lastName, String userName,
            boolean favorite, boolean friend) {
        ContentValues values = new ContentValues();
        values.put(DatabaseValues.User.U_ID, attendeeId);
        values.put(DatabaseValues.User.MEDIA_ID, mediaId);
        values.put(DatabaseValues.User.EMAIL, email);
        values.put(DatabaseValues.User.PHONE_NUMBER, phoneNumber);
        values.put(DatabaseValues.User.FIRST_NAME, firstName);
        values.put(DatabaseValues.User.LAST_NAME, lastName);
        values.put(DatabaseValues.User.USER_NAME, userName);
        values.put(DatabaseValues.User.FAVORITE, favorite ? 1 : 0);
        values.put(DatabaseValues.User.FRIEND, friend ? 1 : 0);

        try {
            database.insert(DatabaseValues.User.TABLE, values);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public int setFriend(String id, boolean status) {
        ContentValues values = new ContentValues();
        values.put(DatabaseValues.User.FRIEND, status ? 1 : 0);

        return updateValues(id, values);
    }

    public int setFavorite(String id, boolean status) {
        ContentValues values = new ContentValues();
        values.put(DatabaseValues.User.FAVORITE, status ? 1 : 0);

        return updateValues(id, values);
    }

    public int setSuggested(String id, boolean status) {
        ContentValues values = new ContentValues();
        values.put(DatabaseValues.User.SUGGESTED, status ? 1 : 0);

        return updateValues(id, values);
    }

    public int clearAttendeeTable() {
        return database.delete(DatabaseValues.User.TABLE, null, null);
    }

    public Attendee getAttendeeById(String id) {
        Attendee user = null;

        Cursor cursor = database.select(
            DatabaseValues.User.TABLE,
            DatabaseValues.User.PROJECTION,
            DatabaseValues.User.U_ID + " = ?",
            new String[]{
                String.valueOf(id)
            }
        );

        if (cursor.moveToNext()) {
            user = cursorToAttendee(cursor);
        }

        cursor.close();

        return user;
    }

    public List<Attendee> getAttendees() {
        List<Attendee> attendees = new ArrayList<>();

        Cursor cursor = database.select(
            DatabaseValues.User.TABLE,
            DatabaseValues.User.PROJECTION
        );

        while (cursor.moveToNext()) {
            Attendee attendee = cursorToAttendee(cursor);
            attendees.add(attendee);
        }

        cursor.close();

        return attendees;
    }

    public int removeAttendee(String id) {
        return database.delete(
            DatabaseValues.User.TABLE,
            DatabaseValues.User.U_ID + " = ?",
            new String[]{
                id
            }
        );
    }

    public int updateValues(String id, ContentValues values) {
        return database.update(
            DatabaseValues.User.TABLE,
            values,
            DatabaseValues.User.U_ID + " = ?",
            new String[]{
                String.valueOf(id)
            }
        );
    }

    /**
     * For PROJECTION only.
     */
    private Attendee cursorToAttendee(Cursor cursor) {
        Attendee user = new Attendee(cursor.getString(DatabaseValues.User.INDEX_U_ID));
        user.mediaId = cursor.getString(DatabaseValues.User.INDEX_MEDIA_ID);
        user.email = cursor.getString(DatabaseValues.User.INDEX_EMAIL);
        user.phoneNumber = cursor.getString(DatabaseValues.User.INDEX_PHONE_NUMBER);
        user.firstName = cursor.getString(DatabaseValues.User.INDEX_FIRST_NAME);
        user.lastName = cursor.getString(DatabaseValues.User.INDEX_LAST_NAME);
        user.userName = cursor.getString(DatabaseValues.User.INDEX_USER_NAME);
        user.isFavorite = cursor.getInt(DatabaseValues.User.INDEX_FAVORITE) > 0;
        user.isFriend = cursor.getInt(DatabaseValues.User.INDEX_FRIEND) > 0;
        user.isSuggested = cursor.getInt(DatabaseValues.User.INDEX_SUGGESTED) > 0;

        return user;
    }
}
