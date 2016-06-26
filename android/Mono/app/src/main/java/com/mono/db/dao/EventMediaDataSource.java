package com.mono.db.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;

import com.mono.db.Database;
import com.mono.db.DatabaseValues;
import com.mono.model.Media;
import com.mono.util.Common;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to perform database actions related to media such as photos.
 *
 * @author Gary Ng
 */
public class EventMediaDataSource extends DataSource {

    private EventMediaDataSource(Database database) {
        super(database);
    }

    /**
     * Attach a specific media to an event.
     *
     * @param eventId The value of the event ID.
     * @param mediaId The value of the media ID.
     */
    public void setMedia(String eventId, long mediaId) {
        ContentValues values = new ContentValues();
        values.put(DatabaseValues.EventMedia.EVENT_ID, eventId);
        values.put(DatabaseValues.EventMedia.MEDIA_ID, mediaId);

        try {
            database.insert(DatabaseValues.EventMedia.TABLE, values);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieve media for a specific event.
     *
     * @param eventId The value of the event ID.
     * @param type The type of media.
     * @return a list of media.
     */
    public List<Media> getMedia(String eventId, String type) {
        List<Media> result = new ArrayList<>();

        String[] projection = {
            "m." + DatabaseValues.Media.ID,
            "m." + DatabaseValues.Media.PATH,
            "m." + DatabaseValues.Media.TYPE,
            "m." + DatabaseValues.Media.SIZE,
            "m." + DatabaseValues.Media.THUMBNAIL
        };

        Cursor cursor = database.rawQuery(
            " SELECT " + Common.implode(", ", projection) +
            " FROM " + DatabaseValues.EventMedia.TABLE + " em" +
            " INNER JOIN " + DatabaseValues.Media.TABLE + " m" +
            " ON em." + DatabaseValues.EventMedia.MEDIA_ID + " = m." + DatabaseValues.Media.ID +
            " WHERE em." + DatabaseValues.EventMedia.EVENT_ID + " = ?" +
            " AND m." + DatabaseValues.Media.TYPE + " = ?",
            new String[]{
                eventId,
                type
            }
        );

        while (cursor.moveToNext()) {
            Media media = new Media(cursor.getLong(0));

            String path = cursor.getString(1);
            media.uri = path != null ? Uri.parse(path) : null;

            media.type = cursor.getString(2);
            media.size = cursor.getLong(3);
            media.thumbnail = cursor.getBlob(4);

            result.add(media);
        }

        cursor.close();

        return result;
    }

    /**
     * Remove all media for a specific event.
     *
     * @param eventId The value of the event ID.
     * @return the number of affected rows.
     */
    public int clearAll(String eventId) {
        return database.delete(
            DatabaseValues.EventMedia.TABLE,
            DatabaseValues.EventMedia.EVENT_ID + " = ?",
            new String[]{
                eventId
            }
        );
    }
}
