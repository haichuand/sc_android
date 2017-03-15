package com.mono.dashboard;

import android.view.ViewGroup;

import com.mono.R;
import com.mono.dashboard.EventHolder.EventItemListener;
import com.mono.util.Pixels;
import com.mono.util.SimpleSlideView;
import com.mono.util.SimpleViewHolder;

/**
 * An adapter used to display events in the recycler view.
 *
 * @author Gary Ng
 */
public class EventsListAdapter extends BaseEventsListAdapter<EventItem> {

    private static final int ITEM_HEIGHT_DP = 60;
    private static final int ITEM_PHOTO_HEIGHT_DP = 120;

    private EventItemListener listener;

    public EventsListAdapter(EventItemListener listener) {
        this.listener = listener;
    }

    @Override
    public SimpleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        SimpleViewHolder holder = null;

        if (viewType == TYPE_EVENT) {
            int height = Pixels.pxFromDp(parent.getContext(), ITEM_HEIGHT_DP);

            SimpleSlideView view = new SimpleSlideView(parent.getContext());
            view.setContent(R.layout.list_item, height, listener);

            holder = new EventHolder(view, listener);
        } else if (viewType == TYPE_PHOTO_EVENT) {
            int height = Pixels.pxFromDp(parent.getContext(), ITEM_PHOTO_HEIGHT_DP);

            SimpleSlideView view = new SimpleSlideView(parent.getContext());
            view.setContent(R.layout.list_item, height, listener);

            holder = new PhotoEventHolder(view, listener);
        }

        return holder;
    }

    @Override
    public void onBindViewHolder(SimpleViewHolder holder, int position) {
        EventItem item = dataSource.getItem(position);
        item.isSelectable = isSelectable;
        item.selected = selections.contains(item.id);

        holder.onBind(item);
    }

    @Override
    public int getItemViewType(int position) {
        int viewType;

        EventItem item = dataSource.getItem(position);

        if (item instanceof PhotoEventItem) {
            viewType = TYPE_PHOTO_EVENT;
        } else {
            viewType = TYPE_EVENT;
        }

        return viewType;
    }
}
