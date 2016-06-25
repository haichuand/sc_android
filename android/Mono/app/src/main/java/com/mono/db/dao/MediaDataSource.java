package com.mono.db.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;

import com.mono.db.Database;
import com.mono.db.DatabaseValues;
import com.mono.model.Media;
import com.mono.util.Common;

/**
 * This class is used to perform database actions related to media such as photos.
 *
 * @author Gary Ng
 */
public class MediaDataSource extends DataSource {

    private MediaDataSource(Database database) {
        super(database);
    }

    /**
     * Create a media in the database.
     *
     * @param path The image path.
     * @param type The type of media.
     * @param size The size in bytes of media.
     * @param thumbnail The image data of the thumbnail.
     * @return the media ID.
     */
    public long createMedia(String path, String type, long size, byte[] thumbnail) {
        long id = -1;

        ContentValues values = new ContentValues();
        values.put(DatabaseValues.Media.PATH, path);
        values.put(DatabaseValues.Media.TYPE, type);
        values.put(DatabaseValues.Media.SIZE, size);
        values.put(DatabaseValues.Media.THUMBNAIL, thumbnail);

        try {
            long rowId = database.insert(DatabaseValues.Media.TABLE, values);

            Cursor cursor = database.select(
                DatabaseValues.Media.TABLE,
                new String[]{
                    DatabaseValues.Media.ID
                },
                "`rowid` = ?",
                new String[]{
                    String.valueOf(rowId)
                }
            );

            if (cursor.moveToNext()) {
                id = cursor.getLong(0);
            }

            cursor.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return id;
    }

    /**
     * Retrieve media using the media ID.
     *
     * @param id The value of the media ID.
     * @return an instance of a media.
     */
    public Media getMedia(long id) {
        Media media = null;

        Cursor cursor = database.select(
            DatabaseValues.Media.TABLE,
            DatabaseValues.Media.PROJECTION,
            DatabaseValues.Media.ID + " = ?",
            new String[]{
                String.valueOf(id)
            }
        );

        if (cursor.moveToNext()) {
            media = cursorToMedia(cursor);
        }

        cursor.close();

        return media;
    }

    /**
     * Retrieve media using the path, type, and size.
     *
     * @param path The image path.
     * @param type The type of media.
     * @param size The size in bytes of media.
     * @return an instance of a media.
     */
    public Media getMedia(String path, String type, long size) {
        Media media = null;

        String[] selection = {
            DatabaseValues.Media.PATH + " = ?",
            DatabaseValues.Media.TYPE + " = ?",
            DatabaseValues.Media.SIZE + " = ?"
        };

        Cursor cursor = database.select(
            DatabaseValues.Media.TABLE,
            DatabaseValues.Media.PROJECTION,
            Common.implode(" AND ", selection),
            new String[]{
                path,
                type,
                String.valueOf(size)
            }
        );

        if (cursor.moveToNext()) {
            media = cursorToMedia(cursor);
        }

        cursor.close();

        return media;
    }

    /**
     * Update the thumbnail of an existing media.
     *
     * @param id The value of the media ID.
     * @param thumbnail The image data of the thumbnail.
     * @return the number of affected rows.
     */
    public int updateThumbnail(long id, byte[] thumbnail) {
        ContentValues values = new ContentValues();
        values.put(DatabaseValues.Media.THUMBNAIL, thumbnail);

        return database.update(
            DatabaseValues.Media.TABLE,
            values,
            DatabaseValues.Media.ID + " = ?",
            new String[]{
                String.valueOf(id)
            }
        );
    }

    /**
     * For PROJECTION only.
     */
    private Media cursorToMedia(Cursor cursor) {
        Media media = new Media(cursor.getLong(DatabaseValues.Media.INDEX_ID));

        String path = cursor.getString(DatabaseValues.Media.INDEX_PATH);
        media.uri = path != null ? Uri.parse(path) : null;

        media.type = cursor.getString(DatabaseValues.Media.INDEX_TYPE);
        media.size = cursor.getLong(DatabaseValues.Media.INDEX_SIZE);
        media.thumbnail = cursor.getBlob(DatabaseValues.Media.INDEX_THUMBNAIL);

        return media;
    }
}
