package com.mono.provider;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.mono.model.Media;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to provide access to the Media Provider to allow the retrieval of
 * images stored on the device.
 *
 * @author Gary Ng
 */
public class MediaImageProvider {

    private static MediaImageProvider instance;

    private Context context;

    private MediaImageProvider(Context context) {
        this.context = context;
    }

    public static MediaImageProvider getInstance(Context context) {
        if (instance == null) {
            instance = new MediaImageProvider(context.getApplicationContext());
        }

        return instance;
    }

    /**
     * Retrieve images that were taken with the time range.
     *
     * @param startTime The start time of images taken after.
     * @param endTime The end time of images taken before.
     * @return a list of images.
     */
    public List<Media> getImages(long startTime, long endTime) {
        List<Media> images = new ArrayList<>();

        List<String> args = new ArrayList<>();

        String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + " = ?";
        args.add(String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE));

        selection += " AND ";
        selection += String.format(
            "(%s >= ? AND %s <= ?)",
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_TAKEN
        );

        args.add(String.valueOf(startTime));
        args.add(String.valueOf(endTime));

        String[] selectionArgs = args.toArray(new String[args.size()]);

        Cursor cursor = context.getContentResolver().query(
            MediaStore.Files.getContentUri("external"),
            MediaValues.Image.PROJECTION,
            selection,
            selectionArgs,
            MediaStore.Images.Media.DATE_TAKEN + " DESC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String path = cursor.getString(MediaValues.Image.INDEX_PATH);
                long size = cursor.getLong(MediaValues.Image.INDEX_SIZE);

                Media image = new Media(Uri.parse(path), Media.IMAGE, size);
                images.add(image);
            }

            cursor.close();
        }

        return images;
    }
}
