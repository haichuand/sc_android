package com.mono.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mono.R;
import com.mono.dashboard.EventGroupHolder.EventGroupsListListener;
import com.mono.util.Colors;
import com.mono.util.Common;
import com.mono.util.SimpleViewHolder;

/**
 * An adapter used to display event groups in the recycler view.
 *
 * @author Gary Ng
 */
public class EventGroupsListAdapter extends BaseEventsListAdapter<EventGroupItem> {

    private EventGroupsListListener listener;

    public EventGroupsListAdapter(EventGroupsListListener listener) {
        this.listener = listener;
    }

    @Override
    public SimpleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.list_card, null, false);

        return new EventGroupHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(SimpleViewHolder holder, int position) {
        EventGroupItem item = dataSource.getItem(position);

        int firstColor = Colors.getColor(holder.itemView.getContext(), R.color.brown_light_1);
        int secondColor = Colors.getColor(holder.itemView.getContext(), R.color.gray_light_6);

        if (position == 0) {
            item.color = firstColor;
        } else {
            EventGroupItem prevItem = dataSource.getItem(position - 1);

            if (Common.compareStrings(item.date, prevItem.date)) {
                item.color = prevItem.color;
            } else if (prevItem.color == secondColor) {
                item.color = firstColor;
            } else {
                item.color = secondColor;
            }
        }

        for (EventItem eventItem : item.items) {
            eventItem.isSelectable = isSelectable;
            eventItem.selected = selections.contains(eventItem.id);
        }

        holder.onBind(item);
    }

    @Override
    public int getItemViewType(int position) {
        int viewType;

        EventGroupItem item = dataSource.getItem(position);

        if (item.hasPhotos) {
            viewType = TYPE_PHOTO_EVENT;
        } else {
            viewType = TYPE_EVENT;
        }

        viewType += item.items.size();

        return viewType;
    }
}
