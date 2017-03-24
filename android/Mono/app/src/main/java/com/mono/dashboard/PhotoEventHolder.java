package com.mono.dashboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.mono.model.Media;
import com.mono.util.BitmapHelper;
import com.mono.util.Common;
import com.mono.util.Pixels;
import com.mono.util.SimpleListItemView;
import com.mono.util.Views;

/**
 * This adapter holder class is used to define the binding behavior of photo events.
 *
 * @author Gary Ng
 */
public class PhotoEventHolder extends EventHolder {

    private static final int PHOTO_WIDTH_DP = 80;
    private static final int PHOTO_HEIGHT_DP = 60;
    private static final int FADE_DURATION = 500;

    private AsyncTask<Object, Object, Void> task;

    public PhotoEventHolder(View itemView, SimpleListItemView contentView,
            EventItemListener listener) {
        super(itemView, contentView, listener);

        contentView.setPhotosVisible(true);
    }

    @Override
    public void onBind(HolderItem holderItem) {
        super.onBind(holderItem);

        PhotoEventItem item = (PhotoEventItem) holderItem;

        contentView.clearPhotos();

        if (task != null) {
            task.cancel(true);
            task = null;
        }

        task = new AsyncTask<Object, Object, Void>() {

            private Context context;

            @Override
            protected Void doInBackground(Object... params) {
                context = (Context) params[0];
                PhotoEventItem item = (PhotoEventItem) params[1];

                int width = Pixels.pxFromDp(context, PHOTO_WIDTH_DP);
                int height = Pixels.pxFromDp(context, PHOTO_HEIGHT_DP);

                for (int i = 0; i < item.photos.size(); i++) {
                    Bitmap bitmap = null;
                    boolean fade = false;

                    if (item.bitmaps != null && i < item.bitmaps.size()) {
                        // Cached Bitmap
                        bitmap = item.bitmaps.get(i);
                    } else {
                        // New Bitmap
                        Media photo = item.photos.get(i);
                        String path = photo.uri.toString();
                        byte[] data = photo.thumbnail;

                        if (data != null) {
                            bitmap = BitmapHelper.createBitmap(data, width, height);
                        } else if (Common.fileExists(path)) {
                            bitmap = BitmapHelper.createBitmap(path, width, height);
                        }

                        item.bitmaps.add(bitmap);
                        fade = true;
                    }

                    publishProgress(bitmap, fade);
                }

                return null;
            }

            @Override
            protected void onProgressUpdate(Object... values) {
                Bitmap bitmap = (Bitmap) values[0];
                boolean fade = (boolean) values[1];

                ImageView image = new ImageView(context);
                image.setImageBitmap(bitmap);
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    Pixels.pxFromDp(context, PHOTO_WIDTH_DP),
                    Pixels.pxFromDp(context, PHOTO_HEIGHT_DP)
                );

                contentView.addPhoto(image, params);

                if (fade) {
                    Views.fade(image, 0, 1, FADE_DURATION, null);
                }
            }

            @Override
            protected void onPostExecute(Void result) {
                task = null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, itemView.getContext(), item);
    }
}
