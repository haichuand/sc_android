package com.mono.calendar;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mono.R;
import com.mono.util.SimpleDataSource;

public class CalendarEventsAdapter extends RecyclerView.Adapter<CalendarEventsAdapter.Holder> {

    private SimpleDataSource<CalendarEventsItem> dataSource;
    private CalendarEventsClickListener listener;

    public CalendarEventsAdapter(CalendarEventsClickListener listener) {
        this.listener = listener;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.calendar_events_item, parent, false);

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

        public void onBind(final CalendarEventsItem holderItem) {
            itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onClick(itemView);
                }
            });

            itemView.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    return listener.onLongClick(holderItem.id, view);
                }
            });

            icon.setImageResource(holderItem.iconResId);
            icon.setColorFilter(holderItem.iconColor | 0xFF000000);

            startTime.setText(holderItem.startTime);
            endTime.setText(holderItem.endTime);
            title.setText(holderItem.title);
            description.setText(holderItem.description);
        }

        public void onViewRecycled() {

        }
    }

    public static class CalendarEventsItem {

        public static final int TYPE_EVENT = 0;

        public long id;
        public int type;
        public int iconResId;
        public int iconColor;
        public String startTime;
        public String endTime;
        public String title;
        public String description;

        public CalendarEventsItem(long id) {
            this.id = id;
        }
    }

    public interface CalendarEventsClickListener {

        void onClick(View view);

        boolean onLongClick(long id, View view);
    }
}
