package com.mono.calendar;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mono.R;
import com.mono.util.Colors;
import com.mono.util.Pixels;
import com.mono.util.SimpleDataSource;
import com.mono.util.SimpleSlideView;
import com.mono.util.SimpleSlideView.SimpleSlideViewListener;

public class CalendarEventsAdapter extends RecyclerView.Adapter<CalendarEventsAdapter.Holder> {

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
    public void onBindViewHolder(Holder holder, int position) {
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
    public void onViewRecycled(Holder holder) {
        holder.onViewRecycled();
    }

    public void setDataSource(SimpleDataSource<CalendarEventsItem> dataSource) {
        this.dataSource = dataSource;
        notifyDataSetChanged();
    }

    public class Holder extends RecyclerView.ViewHolder {

        public ImageView icon;
        public TextView startTime;
        public TextView endTime;
        public TextView title;
        public TextView description;
        public ImageView person;

        public Holder(View itemView) {
            super(itemView);

            icon = (ImageView) itemView.findViewById(R.id.icon);
            startTime = (TextView) itemView.findViewById(R.id.start_time);
            endTime = (TextView) itemView.findViewById(R.id.end_time);
            title = (TextView) itemView.findViewById(R.id.title);
            description = (TextView) itemView.findViewById(R.id.description);
            person = (ImageView) itemView.findViewById(R.id.person);
        }

        public void onBind(CalendarEventsItem holderItem) {
            SimpleSlideView tempView = (SimpleSlideView) itemView;
            tempView.clear();
            tempView.addLeftButton(Colors.getColor(tempView.getContext(), R.color.lavender),
                R.drawable.ic_chat_white);
            tempView.addLeftButton(Colors.getColor(tempView.getContext(), R.color.brown_light),
                R.drawable.ic_star_border_white);
            tempView.addRightButton(Colors.getColor(tempView.getContext(), R.color.red),
                R.drawable.ic_trash_white);

            icon.setImageResource(holderItem.iconResId);
            icon.setColorFilter(holderItem.iconColor | 0xFF000000);

            startTime.setText(holderItem.startTime);
            startTime.setTextColor(holderItem.startTimeColor);

            endTime.setText(holderItem.endTime);
            endTime.setTextColor(holderItem.endTimeColor);

            title.setText(holderItem.title);
            description.setText(holderItem.description);
        }

        public void onViewRecycled() {

        }
    }

    public static class CalendarEventsItem {

        public static final int TYPE_EVENT = 0;

        public String id;
        public int type;
        public int iconResId;
        public int iconColor;
        public String startTime;
        public int startTimeColor;
        public String endTime;
        public int endTimeColor;
        public String title;
        public String description;

        public CalendarEventsItem(String id) {
            this.id = id;
        }
    }
}
