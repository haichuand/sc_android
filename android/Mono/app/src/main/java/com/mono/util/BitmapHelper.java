package com.mono.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * This class is used to provide helper functions to anything related to bitmaps such as
 * creating a bitmap from an image found on the device and even being able to create a circular
 * bitmap of the image.
 *
 * @author Gary Ng
 */
public class BitmapHelper {

    public static final int FORMAT_JPEG = 0;
    public static final int FORMAT_PNG = 1;

    private BitmapHelper() {}

    /**
     * Retrieve the rotation of any given image in degrees.
     *
     * @param path The path of the image file.
     * @param options The image dimensions to correct if rotation is incorrect.
     * @return the rotation degree.
     */
    public static int getImageOrientation(String path, Options options) {
        int rotation = 0;

        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            );

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotation = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotation = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotation = 270;
                    break;
            }

            if (rotation == 90 || rotation == 270) {
                int width = options.outWidth;
                options.outWidth = options.outHeight;
                options.outHeight = width;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return rotation;
    }

    /**
     * Calculate sample size to load images more efficiently at a smaller resolution.
     *
     * @param options The image dimensions.
     * @param reqWidth The width used as a bound.
     * @param reqHeight The height used as a bound.
     * @return the optimal sample size.
     */
    public static int calculateInSampleSize(Options options, int reqWidth, int reqHeight) {
        int width = options.outWidth;
        int height = options.outHeight;
        int inSampleSize = 1;

        if (width > reqWidth || height > reqHeight) {
            int halfWidth = width / 2;
            int halfHeight = height / 2;

            while (halfWidth / inSampleSize > reqWidth && halfHeight / inSampleSize > reqHeight) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * Create a bitmap from an image found on the device.
     *
     * @param path The path of the image file.
     * @param width The width of the resulting bitmap.
     * @param height The height of the resulting bitmap.
     * @return a bitmap of the image.
     */
    public static Bitmap createBitmap(String path, int width, int height) {
        Options options = new Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(path, options);

        int rotation = getImageOrientation(path, options);
        options.inSampleSize = calculateInSampleSize(options, width, height);
        options.inJustDecodeBounds = false;

        Bitmap tempBitmap = BitmapFactory.decodeFile(path, options);

        width = tempBitmap.getWidth();
        height = tempBitmap.getHeight();

        Matrix matrix = new Matrix();
        matrix.preRotate(rotation);

        Bitmap bitmap = Bitmap.createBitmap(tempBitmap, 0, 0, width, height, matrix, false);
        if (bitmap != tempBitmap) {
            tempBitmap.recycle();
        }

        return bitmap;
    }

    /**
     * Create a bitmap from an image stored in a byte array.
     *
     * @param data The byte array of the image.
     * @param width The width of the resulting bitmap.
     * @param height The height of the resulting bitmap.
     * @return a bitmap of the image.
     */
    public static Bitmap createBitmap(byte[] data, int width, int height) {
        Options options = new Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        options.inSampleSize = calculateInSampleSize(options, width, height);
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    /**
     * Return a circular cutout of an existing bitmap.
     *
     * @param bitmap The original bitmap.
     * @param color The color of the background.
     * @param padding The padding of the resulting bitmap.
     * @return a circular bitmap.
     */
    public static Bitmap createCircleBitmap(Bitmap bitmap, int color, int padding) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int dimension = width > height ? height : width;

        Bitmap result = Bitmap.createBitmap(dimension, dimension, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawARGB(0, 0, 0, 0);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(color);

        float center = dimension / 2f;
        float radius = center - padding;
        canvas.drawCircle(center, center, radius, paint);

        Rect src = new Rect(0, 0, width, height);
        Rect dst = new Rect(padding, padding, width - padding, height - padding);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, src, dst, paint);

        return result;
    }

    /**
     * Retrieve the image data in bytes of an image found on the device.
     *
     * @param path The path of the image file.
     * @param width The width used as a bound.
     * @param height The height used as a bound.
     * @param format The compression format.
     * @param quality The quality of the compression.
     * @return a byte array containing the image data.
     */
    public static byte[] getBytes(String path, int width, int height, int format, int quality) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        Bitmap.CompressFormat compressFormat;
        if (format == FORMAT_PNG) {
            compressFormat = Bitmap.CompressFormat.PNG;
        } else {
            compressFormat = Bitmap.CompressFormat.JPEG;
        }

        Bitmap bitmap = createBitmap(path, width, height);
        bitmap.compress(compressFormat, quality, output);

        return output.toByteArray();
    }
}
