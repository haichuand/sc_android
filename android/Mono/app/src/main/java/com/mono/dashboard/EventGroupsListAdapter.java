package com.mono.dashboard;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mono.R;
import com.mono.util.SimpleDataSource;
import com.mono.util.SimpleViewHolder;

import java.util.LinkedList;
import java.util.List;

/**
 * A adapter used to display event groups in the recycler view.
 *
 * @author Gary Ng
 */
public class EventGroupsListAdapter extends RecyclerView.Adapter<SimpleViewHolder> {

    public static final int BUTTON_CHAT_INDEX = 0;
    public static final int BUTTON_FAVORITE_INDEX = 1;
    public static final int BUTTON_DELETE_INDEX = 0;

    private static final int TYPE_EVENT = 100;
    private static final int TYPE_PHOTO_EVENT = 200;

    private SimpleDataSource<EventGroupItem> dataSource;
    private EventGroupsListListener listener;

    private boolean isSelectable;
    private List<String> selections = new LinkedList<>();

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
    public void setDataSource(SimpleDataSource<EventGroupItem> dataSource) {
        this.dataSource = dataSource;
        notifyDataSetChanged();
    }

    /**
     * Enable the display of checkboxes for item selection during Edit Mode.
     *
     * @param selectable Whether to display checkboxes.
     * @param id ID of item to be selected.
     */
    public void setSelectable(boolean selectable, String id) {
        this.isSelectable = selectable;

        selections.clear();
        if (id != null) {
            selections.add(id);
        }

        notifyDataSetChanged();
    }

    /**
     * Set item to be displayed as selected.
     *
     * @param id ID of item.
     * @param selected Display as selected.
     */
    public void setSelected(String id, boolean selected) {
        if (selected) {
            if (!selections.contains(id)) {
                selections.add(id);
            }
        } else {
            selections.remove(id);
        }
    }

    public interface EventGroupsListListener {

        void onClick(View view, int position);

        boolean onLongClick(View view, int position);

        void onLeftButtonClick(View view, int position, int option);

        void onRightButtonClick(View view, int position, int option);

        void onGesture(View view, boolean state);

        void onSelectClick(View view, int position, boolean value);
    }
}
