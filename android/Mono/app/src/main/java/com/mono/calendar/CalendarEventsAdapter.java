package com.mono.calendar;

import android.content.Context;
import android.graphics.PorterDuff;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mono.R;
import com.mono.util.Colors;
import com.mono.util.Pixels;
import com.mono.util.SimpleDataSource;
import com.mono.util.SimpleSlideView;
import com.mono.util.SimpleSlideView.SimpleSlideViewListener;
import com.mono.util.SimpleViewHolder;
import com.mono.util.SimpleViewHolder.HolderItem;

/**
 * A adapter used to display events in the recycler view.
 *
 * @author Gary Ng
 */
public class CalendarEventsAdapter extends RecyclerView.Adapter<SimpleViewHolder> {

    private static final int ITEM_HEIGHT_DP = 60;

    public static final int BUTTON_CHAT_INDEX = 0;
    public static final int BUTTON_FAVORITE_INDEX = 1;
    public static final int BUTTON_DELETE_INDEX = 0;

    private SimpleDataSource<CalendarEventsItem> dataSource;
    private SimpleSlideViewListener listener;

    public CalendarEventsAdapter(SimpleSlideViewListener listener) {
        this.listener = listener;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        int height = Pixels.pxFromDp(parent.getContext(), ITEM_HEIGHT_DP);

        SimpleSlideView view = new SimpleSlideView(parent.getContext());
        view.setContent(R.layout.calendar_events_item, height, listener);

        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(SimpleViewHolder holder, int position) {
        CalendarEventsItem item = dataSource.getItem(position);
        holder.onBind(item);
    }

    @Override
    public int getItemViewType(int position) {
        CalendarEventsItem item = dataSource.getItem(position);
        return item.type;
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
    public void setDataSource(SimpleDataSource<CalendarEventsItem> dataSource) {
        this.dataSource = dataSource;
        notifyDataSetChanged();
    }

    public class Holder extends SimpleViewHolder {

        private static final int OPTION_DIMENSION_DP = 24;
        private static final int OPTION_MARGIN_DP = 2;

        private ImageView icon;
        private TextView startTime;
        private TextView endTime;
        private TextView title;
        private TextView description;
        private ViewGroup options;

        public Holder(View itemView) {
            super(itemView);

            icon = (ImageView) itemView.findViewById(R.id.icon);
            startTime = (TextView) itemView.findViewById(R.id.start_time);
            endTime = (TextView) itemView.findViewById(R.id.end_time);
            title = (TextView) itemView.findViewById(R.id.title);
            description = (TextView) itemView.findViewById(R.id.description);
            options = (ViewGroup) itemView.findViewById(R.id.options);
        }

        @Override
        public void onBind(HolderItem holderItem) {
            CalendarEventsItem item = (CalendarEventsItem) holderItem;

            SimpleSlideView tempView = (SimpleSlideView) itemView;
            tempView.clear();
            tempView.addLeftButton(Colors.getColor(tempView.getContext(), R.color.lavender),
                R.drawable.ic_chat);
            tempView.addLeftButton(Colors.getColor(tempView.getContext(), R.color.brown_light),
                R.drawable.ic_star_border);
            tempView.addRightButton(Colors.getColor(tempView.getContext(), R.color.red),
                R.drawable.ic_trash);

            icon.setImageResource(item.iconResId);
            icon.setColorFilter(item.iconColor | 0xFF000000);

            startTime.setText(item.startTime);
            startTime.setTextColor(item.startTimeColor);

            endTime.setText(item.endTime);
            endTime.setTextColor(item.endTimeColor);

            title.setText(item.title);
            description.setText(item.description);

            options.removeAllViews();

            if (item.hasPhotos) {
                createOption(R.drawable.ic_camera);
            }

            if (item.hasChat) {
                createOption(R.drawable.ic_chat);
            }
        }

        private void createOption(int resId) {
            Context context = itemView.getContext();

            ImageView image = new ImageView(context);
            image.setImageResource(resId);

            int color = Colors.getColor(context, R.color.lavender);
            image.getDrawable().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);

            int dimension = Pixels.pxFromDp(context, OPTION_DIMENSION_DP);
            int margin = Pixels.pxFromDp(context, OPTION_MARGIN_DP);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dimension, dimension);
            params.setMargins(margin, 0, margin, 0);

            options.addView(image, params);
        }
    }

    public static class CalendarEventsItem extends HolderItem {

        public static final int TYPE_EVENT = 0;

        public int type;
        public int iconResId;
        public int iconColor;
        public String startTime;
        public int startTimeColor;
        public String endTime;
        public int endTimeColor;
        public String title;
        public String description;
        public boolean hasPhotos;
        public boolean hasChat;

        public CalendarEventsItem(String id) {
            this.id = id;
        }
    }
}
