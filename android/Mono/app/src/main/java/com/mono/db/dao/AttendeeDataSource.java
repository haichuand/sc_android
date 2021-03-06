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
 * This class is used to perform database actions related to users.
 *
 * @author Xuejing Dong, Gary Ng
 */
public class AttendeeDataSource extends DataSource {

    public static final int TYPE_ALL = 0;
    public static final int TYPE_FAVORITE = 1;
    public static final int TYPE_FRIEND = 2;
    public static final int TYPE_SUGGESTED = 3;

    private AttendeeDataSource(Database database) {
        super(database);
    }

    public String createAttendee(String mediaId, String email, String phoneNumber,
            String firstName, String lastName, String userName, boolean favorite, boolean friend) {
        String id = String.valueOf((long) (Math.random() * -10000));

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
            boolean favorite, boolean friend, int suggested) {
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
        values.put(DatabaseValues.User.SUGGESTED, suggested);

        try {
            database.insert(DatabaseValues.User.TABLE, values);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean createAttendee (Attendee attendee) {
        return createAttendeeWithAttendeeId(attendee.id, attendee.mediaId, attendee.email, attendee.phoneNumber,
                attendee.firstName, attendee.lastName, attendee.userName, attendee.isFavorite, attendee.isFriend, attendee.isSuggested);
    }

    /**
     * Set user as friend.
     *
     * @param id The value of the user ID.
     * @param status The status of friend.
     * @return the number of affected rows.
     */
    public int setFriend(String id, boolean status) {
        ContentValues values = new ContentValues();
        values.put(DatabaseValues.User.FRIEND, status ? 1 : 0);

        return updateValues(id, values);
    }

    /**
     * Set user as favorite.
     *
     * @param id The value of the user ID.
     * @param status The status of favorite.
     * @return the number of affected rows.
     */
    public int setFavorite(String id, boolean status) {
        ContentValues values = new ContentValues();
        values.put(DatabaseValues.User.FAVORITE, status ? 1 : 0);

        return updateValues(id, values);
    }

    /**
     * Set user as suggested.
     *
     * @param id The value of the user ID.
     * @param value The status of the suggestion.
     * @return the number of affected rows.
     */
    public int setSuggested(String id, int value) {
        ContentValues values = new ContentValues();
        values.put(DatabaseValues.User.SUGGESTED, value);

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
            new String[]{id}
        );

        if (cursor.moveToNext()) {
            user = cursorToAttendee(cursor);
        }

        cursor.close();

        return user;
    }

    public Attendee getAttendeeByEmail (String email) {
        Attendee user = null;

        Cursor cursor = database.select(
                DatabaseValues.User.TABLE,
                DatabaseValues.User.PROJECTION,
                DatabaseValues.User.EMAIL + " = ?",
                new String[]{email}
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

    public boolean updateAttendeeId(String originalId, String newId) {
        ContentValues values = new ContentValues();
        values.put(DatabaseValues.User.U_ID, newId);
        return updateValues(originalId, values) == 1;
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
     * Retrieve the user that contains either the email or phone number.
     *
     * @param email The value of the email.
     * @param phone The value of the phone number.
     * @return an instance of the user.
     */
    public Attendee getUser(String email, String phone) {
        Attendee user = null;

        List<String> args = new ArrayList<>();

        String selection = DatabaseValues.User.USER_NAME + " IS NOT NULL";

        selection += " AND ";

        selection += "(";

        if (!Common.isEmpty(email)) {
            selection += DatabaseValues.User.EMAIL + " = ?";
            args.add(email);
        }

        if (!Common.isEmpty(phone)) {
            if (!Common.isEmpty(email)) {
                selection += " OR ";
            }

            selection += DatabaseValues.User.PHONE_NUMBER + " = ?";
            args.add(phone);
        }

        selection += ")";

        String[] selectionArgs = args.toArray(new String[args.size()]);

        Cursor cursor = database.select(
            DatabaseValues.User.TABLE,
            DatabaseValues.User.PROJECTION,
            selection,
            selectionArgs,
            null
        );

        if (cursor.moveToNext()) {
            user = cursorToAttendee(cursor);
        }

        cursor.close();

        return user;
    }

    /**
     * Check whether there are registered users.
     *
     * @return the status of registered users.
     */
    public boolean hasUsers() {
        boolean result = false;

        Cursor cursor = database.select(
            DatabaseValues.User.TABLE,
            new String[]{
                "COUNT(*)"
            },
            DatabaseValues.User.USER_NAME + " IS NOT NULL",
            null,
            null
        );

        if (cursor.moveToNext()) {
            result = cursor.getInt(0) > 0;
        }

        cursor.close();

        return result;
    }

    /**
     * Retrieve the number of users that satisfies the given criteria.
     *
     * @param terms Search terms to be used.
     * @return a count of users.
     */
    public int getUsersCount(int type, String[] terms) {
        int count = 0;

        List<String> args = new ArrayList<>();

        String selection = DatabaseValues.User.USER_NAME + " IS NOT NULL";

        if (type != TYPE_ALL) {
            selection += " AND ";
            selection += getUserTypeSelection(type);
        }

        if (terms != null) {
            String[] fields = {
                DatabaseValues.User.FIRST_NAME,
                DatabaseValues.User.LAST_NAME,
                DatabaseValues.User.USER_NAME
            };

            selection += " AND ";
            selection += getLikeSelection(args, terms, fields);
        }

        String[] selectionArgs = args.toArray(new String[args.size()]);

        Cursor cursor = database.select(
            DatabaseValues.User.TABLE,
            new String[]{
                "COUNT(*) AS `count`"
            },
            selection,
            selectionArgs,
            null
        );

        if (cursor.moveToNext()) {
            count = cursor.getInt(0);
        }

        cursor.close();

        return count;
    }

    /**
     * Retrieve registered users.
     *
     * @param type Type of user.
     * @param terms Search terms to be used.
     * @param offset Offset of users to start with.
     * @param limit Max number of results to return.
     * @return a list of users.
     */
    public List<Attendee> getUsers(int type, String[] terms, int offset, int limit) {
        List<Attendee> users = new ArrayList<>();

        List<String> args = new ArrayList<>();

        String selection = DatabaseValues.User.USER_NAME + " IS NOT NULL";

        if (type != TYPE_ALL) {
            selection += " AND ";
            selection += getUserTypeSelection(type);
        }

        if (terms != null) {
            String[] fields = {
                DatabaseValues.User.FIRST_NAME,
                DatabaseValues.User.LAST_NAME,
                DatabaseValues.User.USER_NAME
            };

            selection += " AND ";
            selection += getLikeSelection(args, terms, fields);
        }

        String[] selectionArgs = args.toArray(new String[args.size()]);
        String order = String.format(
            "LOWER(%s)",
            DatabaseValues.User.FIRST_NAME
        );

        Cursor cursor = database.select(
            DatabaseValues.User.TABLE,
            DatabaseValues.User.PROJECTION,
            selection,
            selectionArgs,
            null,
            order,
            offset > 0 ? offset : null,
            limit > 0 || offset > 0 ? limit : null
        );

        while (cursor.moveToNext()) {
            Attendee user = cursorToAttendee(cursor);
            users.add(user);
        }

        cursor.close();

        return users;
    }

    /**
     * Construct a selection query to restrict user type.
     *
     * @param type Type of user.
     * @return a selection query.
     */
    private String getUserTypeSelection(int type) {
        String selection = "";

        String field = null;

        switch (type) {
            case TYPE_FAVORITE:
                field = DatabaseValues.User.FAVORITE;
                break;
            case TYPE_FRIEND:
                field = DatabaseValues.User.FRIEND;
                break;
            case TYPE_SUGGESTED:
                field = DatabaseValues.User.SUGGESTED;
                break;
        }

        if (field != null) {
            selection += field + " > 0";
        }

        return selection;
    }

    /**
     * Helper function to retrieve the selection to perform a LIKE query.
     *
     * @param args The list to insert arguments.
     * @param terms The search terms to be used.
     * @param fields The table columns to be searched.
     * @return a string of the selection query.
     */
    private String getLikeSelection(List<String> args, String[] terms, String[] fields) {
        String selection = "(";

        for (int i = 0; i < terms.length; i++) {
            if (i > 0) selection += " AND ";

            selection += "(";

            for (int j = 0; j < fields.length; j++) {
                if (j > 0) selection += " OR ";
                selection += fields[j] + " LIKE '%' || ? || '%'";
                args.add(terms[i]);
            }

            selection += ")";
        }

        selection += ")";

        return selection;
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
        user.isSuggested = cursor.getInt(DatabaseValues.User.INDEX_SUGGESTED);

        return user;
    }
}
