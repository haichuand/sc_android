package com.mono;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;

import com.mono.db.DatabaseHelper;
import com.mono.db.dao.MediaDataSource;
import com.mono.model.Media;
import com.mono.provider.MediaImageProvider;
import com.mono.util.BitmapHelper;
import com.mono.util.Common;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.provider.MediaStore.Video.*;

/**
 * This manager class is used to centralize all media related actions such as retrieve media from
 * the database or Media Provider. Media retrieved are also cached here to improve efficiency.
 *
 * @author Gary Ng
 */
public class MediaManager {

    private static final int THUMBNAIL_DIMENSION_PX = 128;

    private static MediaManager instance;

    private Context context;

    private final Map<Long, Media> cache = new HashMap<>();

    private MediaManager(Context context) {
        this.context = context;
    }

    public static MediaManager getInstance(Context context) {
        if (instance == null) {
            instance = new MediaManager(context.getApplicationContext());
        }

        return instance;
    }

    /**
     * Insert a media into the cache.
     *
     * @param media The instance of a media.
     */
    private void add(Media media) {
        if (Common.compareStrings(media.type, Media.IMAGE)) {
            if (media.thumbnail == null || media.thumbnail.length == 0) {
                String path = media.uri.toString();

                if (Common.fileExists(path)) {
                    media.thumbnail = createThumbnail(path);

                    MediaDataSource dataSource =
                        DatabaseHelper.getDataSource(context, MediaDataSource.class);
                    dataSource.updateThumbnail(media.id, media.thumbnail);
                }
            }
        }

        cache.put(media.id, media);
    }

    /**
     * Retrieve a media using the ID.
     *
     * @param id The value of the media ID.
     * @return an instance of the media.
     */
    public Media getMedia(long id) {
        Media media;

        if (cache.containsKey(id)) {
            media = cache.get(id);
        } else {
            MediaDataSource dataSource =
                DatabaseHelper.getDataSource(context, MediaDataSource.class);
            media = dataSource.getMedia(id);
            // Cache Media
            if (media != null) {
                add(media);
            }
        }

        return media;
    }

    /**
     * Retrieve an image media using the path and file size.
     *
     * @param path The image path.
     * @param size The size in bytes of image.
     * @return an instance of the image.
     */
    public Media getImage(String path, long size) {
        Media image;

        MediaDataSource dataSource =
            DatabaseHelper.getDataSource(context, MediaDataSource.class);
        image = dataSource.getMedia(path, Media.IMAGE, size);

        if (image != null) {
            if (cache.containsKey(image.id)) {
                image = getMedia(image.id);
            } else {
                add(image);
            }
        } else {
            image = new Media(Uri.parse(path), Media.IMAGE, size);
        }

        return image;
    }

    /**
     * Retrieve images that were taken with the time range.
     *
     * @param startTime The start time of images taken after.
     * @param endTime The end time of images taken before.
     * @return a list of images.
     */
    public List<Media> getImages(long startTime, long endTime) {
        List<Media> result = new ArrayList<>();

        MediaImageProvider provider = MediaImageProvider.getInstance(context);
        List<Media> images = provider.getImages(startTime, endTime);

        for (Media image : images) {
            Media tempImage = getImage(image.uri.toString(), image.size);
            tempImage.addTime = image.addTime;

            result.add(tempImage);
        }

        return result;
    }

    /**
     * Create a thumbnail of an image of a specific size with the image path given.
     *
     * @param path The image path.
     * @return the image data of the thumbnail.
     */
    public static byte[] createThumbnail(String path) {
        return BitmapHelper.getBytes(path, THUMBNAIL_DIMENSION_PX, THUMBNAIL_DIMENSION_PX,
            BitmapHelper.FORMAT_JPEG, 100);
    }

    /**
     * Create a thumbnail of a video of a specific size with the video path given.
     *
     * @param path The video path.
     * @return the image data of the thumbnail.
     */
    public static byte[] createVideoThumbnail(String path) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(path, Thumbnails.MINI_KIND);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);

        return output.toByteArray();
    }
}
