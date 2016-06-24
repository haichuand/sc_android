package com.mono.events;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mono.R;
import com.mono.model.Media;
import com.mono.util.BitmapHelper;
import com.mono.util.Colors;
import com.mono.util.Common;
import com.mono.util.Pixels;
import com.mono.util.SimpleDataSource;
import com.mono.util.SimpleSlideView;
import com.mono.util.SimpleSlideView.SimpleSlideViewListener;
import com.mono.util.SimpleViewHolder;
import com.mono.util.SimpleViewHolder.HolderItem;
import com.mono.util.Views;

import java.util.ArrayList;
import java.util.List;

/**
 * A adapter used to display events in the recycler view.
 *
 * @author Gary Ng
 */
public class ListAdapter extends RecyclerView.Adapter<SimpleViewHolder> {

    public static final int TYPE_EVENT = 0;
    public static final int TYPE_PHOTO_EVENT = 1;

    public static final int BUTTON_CHAT_INDEX = 0;
    public static final int BUTTON_FAVORITE_INDEX = 1;
    public static final int BUTTON_DELETE_INDEX = 0;

    private static final int ITEM_HEIGHT_DP = 60;
    private static final int ITEM_PHOTO_HEIGHT_DP = 120;

    private SimpleDataSource<ListItem> dataSource;
    private SimpleSlideViewListener listener;

    public ListAdapter(SimpleSlideViewListener listener) {
        this.listener = listener;
    }

    @Override
    public SimpleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        SimpleViewHolder holder = null;

        if (viewType == TYPE_EVENT) {
            int height = Pixels.pxFromDp(parent.getContext(), ITEM_HEIGHT_DP);

            SimpleSlideView view = new SimpleSlideView(parent.getContext());
            view.setContent(R.layout.list_item, height, listener);

            holder = new Holder(view);
        } else if (viewType == TYPE_PHOTO_EVENT) {
            int height = Pixels.pxFromDp(parent.getContext(), ITEM_PHOTO_HEIGHT_DP);

            SimpleSlideView view = new SimpleSlideView(parent.getContext());
            view.setContent(R.layout.list_item, height, listener);

            holder = new PhotoHolder(view);
        }

        return holder;
    }

    @Override
    public void onBindViewHolder(SimpleViewHolder holder, int position) {
        ListItem item = dataSource.getItem(position);
        holder.onBind(item);
    }

    @Override
    public int getItemViewType(int position) {
        int viewType;

        ListItem item = dataSource.getItem(position);

        if (item instanceof PhotoItem) {
            viewType = TYPE_PHOTO_EVENT;
        } else {
            viewType = TYPE_EVENT;
        }

        return viewType;
    }

    @Override
    public int getItemCount() {
        return dataSource == null ? 0 : dataSource.getCount();
    }

    @Override
    public void onViewRecycled(SimpleViewHolder holder) {
        holder.onViewRecycled();
    }

    /**
     * Set the source to retrieve items for this adapter to use.
     *
     * @param dataSource The item source.
     */
    public void setDataSource(SimpleDataSource<ListItem> dataSource) {
        this.dataSource = dataSource;
        notifyDataSetChanged();
    }

    public class Holder extends SimpleViewHolder {

        private ImageView icon;
        private TextView title;
        private TextView description;
        private TextView date;

        public Holder(View itemView) {
            super(itemView);

            icon = (ImageView) itemView.findViewById(R.id.icon);
            title = (TextView) itemView.findViewById(R.id.title);
            description = (TextView) itemView.findViewById(R.id.description);
            date = (TextView) itemView.findViewById(R.id.date);
        }

        @Override
        public void onBind(HolderItem holderItem) {
            ListItem item = (ListItem) holderItem;

            SimpleSlideView tempView = (SimpleSlideView) itemView;
            tempView.clear();

            Context context = tempView.getContext();

            tempView.addLeftButton(Colors.getColor(context, R.color.lavender),
                R.drawable.ic_chat_white);
            tempView.addLeftButton(Colors.getColor(context, R.color.brown_light),
                R.drawable.ic_star_border_white);
            tempView.addRightButton(Colors.getColor(context, R.color.red),
                R.drawable.ic_trash_white);

            icon.setImageResource(item.iconResId);
            icon.setColorFilter(item.iconColor | 0xFF000000);

            if (item.title != null && !item.title.isEmpty()) {
                title.setText(item.title);
                title.setTextColor(Colors.getColor(context, R.color.gray_dark));
            } else {
                title.setText(R.string.untitled);
                title.setTextColor(Colors.getColor(context, R.color.gray_light_3));
            }

            description.setText(item.description);
            date.setText(item.dateTime);

            if (item.dateTimeColor != 0) {
                date.setTextColor(item.dateTimeColor);
            } else {
                date.setTextColor(Colors.getColor(context, R.color.gray_light_3));
            }
        }
    }

    public static class ListItem extends HolderItem {

        public static final int TYPE_EVENT = 0;

        public int type;
        public int iconResId;
        public int iconColor;
        public String title;
        public String description;
        public String dateTime;
        public int dateTimeColor;

        public ListItem(String id) {
            this.id = id;
        }
    }

    public class PhotoHolder extends Holder {

        private static final int PHOTO_WIDTH_DP = 80;
        private static final int PHOTO_HEIGHT_DP = 60;
        private static final int FADE_DURATION = 500;

        private ViewGroup photos;

        private AsyncTask<Object, Object, Void> task;

        public PhotoHolder(View itemView) {
            super(itemView);

            photos = (ViewGroup) itemView.findViewById(R.id.photos);
            photos.setVisibility(View.VISIBLE);
        }

        @Override
        public void onBind(HolderItem holderItem) {
            super.onBind(holderItem);

            PhotoItem item = (PhotoItem) holderItem;

            photos.removeAllViews();

            if (task != null) {
                task.cancel(true);
                task = null;
            }

            task = new AsyncTask<Object, Object, Void>() {

                private Context context;

                @Override
                protected Void doInBackground(Object... params) {
                    context = (Context) params[0];
                    PhotoItem item = (PhotoItem) params[1];

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

                    photos.addView(image, params);

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

    public static class PhotoItem extends ListItem {

        public List<Media> photos;
        public List<Bitmap> bitmaps = new ArrayList<>();

        public PhotoItem(String id) {
            super(id);
        }
    }
}
