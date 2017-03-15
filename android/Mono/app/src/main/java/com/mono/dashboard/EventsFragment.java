package com.mono.dashboard;

import android.view.View;

import com.mono.R;
import com.mono.dashboard.EventHolder.EventItemListener;
import com.mono.model.Event;

import java.util.Iterator;
import java.util.List;

/**
 * A fragment that displays a list of events. Events selected can be viewed or edited. Using a
 * sliding gesture of left or right on the event will reveal additional options to trigger a
 * chat conversation or perform a quick deletion of unwanted events.
 *
 * @author Gary Ng
 */
public class EventsFragment extends BaseEventsFragment<Event, EventItem> implements
        EventItemListener {

    @Override
    public void onPreCreate() {
        position = DashboardFragment.TAB_EVENTS;

        sortOrder = SORT_ORDER_DESC;

        adapter = new EventsListAdapter(this);
        dataSource = new EventDataSource(getContext(), R.color.gray_dark);
    }

    /**
     * Handle the action of clicking an event and notify any listeners.
     *
     * @param view View of the event.
     */
    @Override
    public void onClick(View view) {
        int position = recyclerView.getChildAdapterPosition(view);
        EventItem item = dataSource.getItem(position);
        if (item == null) {
            return;
        }

        if (listener != null) {
            listener.onClick(this.position, item.id, view);
        }
    }

    /**
     * Handle the action of long clicking an event.
     *
     * @param view View of the event.
     * @return whether the action has been consumed.
     */
    @Override
    public boolean onLongClick(View view) {
        if (isEditModeEnabled) {
            return false;
        }

        int position = recyclerView.getChildAdapterPosition(view);
        EventItem item = dataSource.getItem(position);

        setEditMode(true, item != null ? item.id : null);

        return true;
    }

    /**
     * Handle the action of selecting an item from the list during Edit Mode.
     *
     * @param view View of the event.
     * @param value Event is selected or unselected.
     */
    @Override
    public void onSelectClick(View view, boolean value) {
        int position = recyclerView.getChildAdapterPosition(view);
        EventItem item = dataSource.getItem(position);
        if (item == null) {
            return;
        }

        adapter.setSelected(item.id, value);

        if (value) {
            if (!eventSelections.contains(item.id)) {
                eventSelections.add(item.id);
            }
        } else {
            eventSelections.remove(item.id);
        }

        refreshActionBar();
    }

    /**
     * Handle the action of clicking on a hidden option on the left side of the event.
     *
     * @param view View of the event.
     * @param index Index of the action.
     */
    @Override
    public void onLeftButtonClick(View view, int index) {
        int position = recyclerView.getChildAdapterPosition(view);
        EventItem item = dataSource.getItem(position);
        if (item == null) {
            return;
        }

        if (listener != null) {
            switch (index) {
                case EventsListAdapter.BUTTON_CHAT_INDEX:
                    listener.onChatClick(this.position, item.id);
                    break;
                case EventsListAdapter.BUTTON_FAVORITE_INDEX:
                    listener.onFavoriteClick(this.position, item.id);

                    dataSource.removeItem(item.id);
                    adapter.notifyItemChanged(position);
                    break;
            }
        }
    }

    /**
     * Handle the action of clicking on a hidden option on the right side of the event.
     *
     * @param view View of the event.
     * @param index Index of the action.
     */
    @Override
    public void onRightButtonClick(View view, int index) {
        int position = recyclerView.getChildAdapterPosition(view);
        EventItem item = dataSource.getItem(position);
        if (item == null) {
            return;
        }

        if (listener != null) {
            switch (index) {
                case EventsListAdapter.BUTTON_DELETE_INDEX:
                    listener.onDeleteClick(this.position, item.id);
                    break;
            }
        }
    }

    /**
     * Used to disable any vertical scrolling if event sliding gestures are active.
     *
     * @param view View of the event.
     * @param state Scrolling should be enabled.
     */
    @Override
    public void onGesture(View view, boolean state) {
        layoutManager.setScrollEnabled(state);
    }

    @Override
    public void insert(List<Event> events, int scrollToPosition) {
        Event scrollToEvent = scrollToPosition >= 0 ? events.get(scrollToPosition) : null;

        Iterator<Event> iterator = events.iterator();
        while (iterator.hasNext()) {
            if (!checkEvent(iterator.next())) {
                iterator.remove();
            }
        }

        if (events.isEmpty()) {
            return;
        }

        dataSource.addAll(events);
        dataSource.sort();

        for (Event event : events) {
            adapter.notifyItemInserted(dataSource.indexOf(event));
        }

        if (scrollToEvent != null) {
            scrollToPosition = dataSource.indexOf(scrollToEvent);
            if (scrollToPosition >= 0) {
                recyclerView.smoothScrollToPosition(scrollToPosition);
            }
        }

        text.setVisibility(View.INVISIBLE);
    }

    @Override
    public void update(List<Event> events, int scrollToPosition) {
        Event scrollToEvent = scrollToPosition >= 0 ? events.get(scrollToPosition) : null;

        for (Event event : events) {
            int index;

            if (event.oldId != null) {
                Event tempEvent = new Event(event);
                tempEvent.id = tempEvent.oldId;
                index = dataSource.indexOf(tempEvent);
            } else {
                index = dataSource.indexOf(event);
            }

            if (index < 0) {
                continue;
            }

            dataSource.remove(index);
            dataSource.removeItem(event.oldId != null ? event.oldId : event.id);

            dataSource.add(event);
            dataSource.sort();

            adapter.notifyItemChanged(index);

            int currentIndex = dataSource.indexOf(event);
            if (currentIndex != index) {
                adapter.notifyItemMoved(index, currentIndex);
            }
        }

        if (scrollToEvent != null) {
            scrollToPosition = dataSource.indexOf(scrollToEvent);
            if (scrollToPosition >= 0) {
                recyclerView.smoothScrollToPosition(scrollToPosition);
            }
        }
    }

    @Override
    public void remove(List<String> ids, boolean scrollTo) {
        for (String id : ids) {
            if (!dataSource.containsEvent(id)) {
                continue;
            }

            Event event = dataSource.getEvent(id);

            if (event != null) {
                int index = dataSource.indexOf(event);

                dataSource.remove(event);
                dataSource.removeItem(id);

                adapter.notifyItemRemoved(index);
            }
        }

        if (dataSource.isEmpty()) {
            text.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void processEvents(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        int index = dataSource.getCount();

        int size = 0;

        for (Event event : events) {
            if (!checkEvent(event)) {
                continue;
            }

            dataSource.add(index + size, event);
            size++;
        }

        text.setVisibility(View.INVISIBLE);
        adapter.notifyItemRangeInserted(index, size);
    }
}
