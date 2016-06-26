package com.mono.provider;

import android.provider.MediaStore.Images;

/**
 * This class stores all constants to be used in conjunction with the Media Provider.
 *
 * @author Gary Ng
 */
public class MediaValues {

    public static class Image {

        public static final String[] PROJECTION = {
            Images.Media._ID,
            Images.Media.DATA,
            Images.Media.DATE_ADDED, // Seconds (Epoch)
            Images.Media.MIME_TYPE,
            Images.Media.SIZE,
            Images.Media.TITLE,
            Images.Media.BUCKET_DISPLAY_NAME,
            Images.Media.DATE_TAKEN, // Milliseconds (UTC)
            Images.Media.DESCRIPTION,
            Images.Media.LATITUDE,
            Images.Media.LONGITUDE
        };

        public static final int INDEX_ID = 0;
        public static final int INDEX_PATH = 1;
        public static final int INDEX_DATE_ADDED = 2;
        public static final int INDEX_MIME_TYPE = 3;
        public static final int INDEX_SIZE = 4;
        public static final int INDEX_TITLE = 5;
        public static final int INDEX_DISPLAY_NAME = 6;
        public static final int INDEX_DATE_TAKEN = 7;
        public static final int INDEX_DESCRIPTION = 8;
        public static final int INDEX_LATITUDE = 9;
        public static final int INDEX_LONGITUDE = 10;
    }
}
