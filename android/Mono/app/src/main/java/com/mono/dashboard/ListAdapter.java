package com.mono.dashboard;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.mono.R;
import com.mono.util.Pixels;
import com.mono.util.SimpleDataSource;
import com.mono.util.SimpleSlideView;
import com.mono.util.SimpleViewHolder;

import java.util.LinkedList;
import java.util.List;

/**
 * A adapter used to display events in the recycler view.
 *
 * @author Gary Ng
 */
public class ListAdapter extends RecyclerView.Adapter<SimpleViewHolder> {

    public static final int BUTTON_CHAT_INDEX = 0;
    public static final int BUTTON_FAVORITE_INDEX = 1;
    public static final int BUTTON_DELETE_INDEX = 0;

    private static final int TYPE_EVENT = 100;
    private static final int TYPE_PHOTO_EVENT = 200;

    private static final int ITEM_HEIGHT_DP = 60;
    private static final int ITEM_PHOTO_HEIGHT_DP = 120;

    private SimpleDataSource<EventItem> dataSource;
    private EventItemListener listener;

    private boolean isSelectable;
    private List<String> selections = new LinkedList<>();

    public ListAdapter(EventItemListener listener) {
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
    public void setDataSource(SimpleDataSource<EventItem> dataSource) {
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
}
