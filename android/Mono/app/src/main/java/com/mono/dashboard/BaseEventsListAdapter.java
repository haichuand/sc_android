package com.mono.dashboard;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.mono.util.SimpleDataSource;
import com.mono.util.SimpleViewHolder;

import java.util.LinkedList;
import java.util.List;

/**
 * An adapter used to display events in the recycler view.
 *
 * @author Gary Ng
 */
public abstract class BaseEventsListAdapter<T> extends RecyclerView.Adapter<SimpleViewHolder> {

    public static final int BUTTON_CHAT_INDEX = 0;
    public static final int BUTTON_FAVORITE_INDEX = 1;
    public static final int BUTTON_DELETE_INDEX = 0;

    protected static final int TYPE_EVENT = 100;
    protected static final int TYPE_PHOTO_EVENT = 200;

    protected SimpleDataSource<T> dataSource;

    protected boolean isSelectable;
    protected List<String> selections = new LinkedList<>();

    @Override
    public abstract SimpleViewHolder onCreateViewHolder(ViewGroup parent, int viewType);

    @Override
    public abstract void onBindViewHolder(SimpleViewHolder holder, int position);

    @Override
    public abstract int getItemViewType(int position);

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
     * @param dataSource Event item source.
     */
    public void setDataSource(SimpleDataSource<T> dataSource) {
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
