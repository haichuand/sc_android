package com.mono.db.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;

import com.mono.db.Database;
import com.mono.db.DatabaseValues;
import com.mono.model.Attendee;

/**
 * Created by xuejing on 3/28/16.
 */
public class AttendeeDataSource extends DataSource {

    private AttendeeDataSource(Database database) {
        super(database);
    }

    public String createAttendee (String mediaId, String email, String phoneNumber, String firstName, String lastName, String userName, boolean isFriend) {

        String id = DataSource.UniqueIdGenerator(this.getClass().getSimpleName());
        int isFriendInt = isFriend ? 1 : 0;

        ContentValues values = new ContentValues();
        values.put(DatabaseValues.User.U_ID, id);
        values.put(DatabaseValues.User.MEDIA_ID, mediaId);
        values.put(DatabaseValues.User.EMAIL, email);
        values.put(DatabaseValues.User.PHONE_NUMBER, phoneNumber);
        values.put(DatabaseValues.User.FIRST_NAME, firstName);
        values.put(DatabaseValues.User.LAST_NAME, lastName);
        values.put(DatabaseValues.User.USER_NAME, userName);
        values.put(DatabaseValues.User.IS_FRIEND, isFriendInt);

        try {
            database.insert(DatabaseValues.User.TABLE, values);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return id;
    }

    public Attendee getAttendeeById (String id) {
        Attendee user= null;

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

    public String getLocalUserId () {
        String id = "";

        Cursor cursor = database.select(
                DatabaseValues.User.TABLE,
                new String[]{
                        DatabaseValues.User.U_ID
                },
                DatabaseValues.User.IS_FRIEND + " =?",
                new String[]{
                        String.valueOf(1)
                }
        );

        if(cursor.moveToNext()) {
            id = cursor.getString(0);
        }

        cursor.close();

        return id;
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
        user.isFriend = cursor.getInt(DatabaseValues.User.INDEX_IS_FRIEND) == 1 ? true : false;

        return user;
    }

}
