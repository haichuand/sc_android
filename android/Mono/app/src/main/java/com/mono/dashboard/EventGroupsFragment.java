package com.mono.dashboard;

import android.view.View;

import com.mono.R;
import com.mono.dashboard.EventGroupHolder.EventGroupsListListener;
import com.mono.model.Event;
import com.mono.model.EventGroup;
import com.mono.model.Location;
import com.mono.util.Common;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A fragment that displays a list of events as groups. Events selected can be viewed or edited.
 * Using a sliding gesture of left or right on the event will reveal additional options to trigger
 * a chat conversation or perform a quick deletion of unwanted events.
 *
 * @author Gary Ng
 */
public class EventGroupsFragment extends BaseEventsFragment<EventGroup, EventGroupItem>
        implements EventGroupsListListener {

    protected static final String PLACEHOLDER = "Events";

    protected EventGroupDataSource dataSource;

    @Override
    public void onPreCreate() {
        position = DashboardFragment.TAB_EVENTS;

        sortOrder = SORT_ORDER_DESC;

        adapter = new EventGroupsListAdapter(this);
        dataSource = new EventGroupDataSource(getContext(), R.color.gray_dark);
        super.dataSource = dataSource;
    }

    /**
     * Handle the action of clicking an event and notify any listeners.
     *
     * @param view View of the event.
     */
    @Override
    public void onClick(View view, int position) {
        int adapterPosition = recyclerView.getChildAdapterPosition(view);
        EventGroupItem item = dataSource.getItem(adapterPosition);
        if (item == null) {
            return;
        }

        if (listener != null) {
            EventItem eventItem = item.items.get(position);
            listener.onClick(this.position, eventItem.id, view);
        }
    }

    /**
     * Handle the action of long clicking an event.
     *
     * @param view View of the event.
     * @return whether the action has been consumed.
     */
    @Override
    public boolean onLongClick(View view, int position) {
        if (isEditModeEnabled) {
            return false;
        }

        int adapterPosition = recyclerView.getChildAdapterPosition(view);
        EventGroupItem item = dataSource.getItem(adapterPosition);
        if (item == null) {
            return false;
        }

        EventItem eventItem = item.items.get(position);
        setEditMode(true, eventItem.id);

        return true;
    }

    /**
     * Handle the action of selecting an item from the list during Edit Mode.
     *
     * @param view View of the event.
     * @param value Event is selected or unselected.
     */
    @Override
    public void onSelectClick(View view, int position, boolean value) {
        int adapterPosition = recyclerView.getChildAdapterPosition(view);
        EventGroupItem item = dataSource.getItem(adapterPosition);
        if (item == null) {
            return;
        }

        EventItem eventItem = item.items.get(position);
        adapter.setSelected(eventItem.id, value);

        if (value) {
            if (!eventSelections.contains(eventItem.id)) {
                eventSelections.add(eventItem.id);
            }
        } else {
            eventSelections.remove(eventItem.id);
        }

        refreshActionBar();
    }

    /**
     * Handle the action of clicking on a hidden option on the left side of the event.
     *
     * @param view View of the event.
     * @param option Index of the action.
     */
    @Override
    public void onLeftButtonClick(View view, int position, int option) {
        int adapterPosition = recyclerView.getChildAdapterPosition(view);
        EventGroupItem item = dataSource.getItem(adapterPosition);
        if (item == null) {
            return;
        }

        if (listener != null) {
            EventItem eventItem = item.items.get(position);

            switch (option) {
                case EventGroupsListAdapter.BUTTON_CHAT_INDEX:
                    listener.onChatClick(this.position, eventItem.id);
                    break;
                case EventGroupsListAdapter.BUTTON_FAVORITE_INDEX:
                    listener.onFavoriteClick(this.position, eventItem.id);

                    dataSource.removeItem(eventItem.id);
                    adapter.notifyItemChanged(adapterPosition);
                    break;
            }
        }
    }

    /**
     * Handle the action of clicking on a hidden option on the right side of the event.
     *
     * @param view View of the event.
     * @param option Index of the action.
     */
    @Override
    public void onRightButtonClick(View view, int position, int option) {
        int adapterPosition = recyclerView.getChildAdapterPosition(view);
        EventGroupItem item = dataSource.getItem(adapterPosition);
        if (item == null) {
            return;
        }

        if (listener != null) {
            EventItem eventItem = item.items.get(position);

            switch (option) {
                case EventGroupsListAdapter.BUTTON_DELETE_INDEX:
                    listener.onDeleteClick(this.position, eventItem.id);
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
        EventGroup scrollToGroup = null;
        // Check Event Display Criteria
        Iterator<Event> iterator = events.iterator();
        while (iterator.hasNext()) {
            Event event = iterator.next();

            if (!checkEvent(event)) {
                iterator.remove();
            }

            if (!dataSource.isEmpty()) {
                EventGroup group = dataSource.last();
                if (event.startTime < group.getStartTime()) {
                    iterator.remove();
                }
            }
        }

        if (events.isEmpty()) {
            return;
        }

        List<EventGroup> insertGroups = new ArrayList<>();
        List<EventGroup> updateGroups = new ArrayList<>();

        for (Event event : events) {
            Location location;
            // Prioritize Location Suggestions
            if (!event.tempLocations.isEmpty()) {
                location = event.tempLocations.get(0);
            } else {
                location = event.location;
            }

            String title = location != null ? location.name : PLACEHOLDER;
            DateTimeZone timeZone = event.allDay ? DateTimeZone.UTC : DateTimeZone.getDefault();
            EventGroup group = new EventGroup(null, title, event.startTime, timeZone);

            if (!dataSource.contains(group)) {
                group.id = event.startTime + String.valueOf((int) (Math.random() * 10000));
                dataSource.add(group);
                // For Adapter Use
                if (!insertGroups.contains(group)) {
                    insertGroups.add(group);
                }
            } else {
                int index = dataSource.indexOf(group);
                int lastIndex = dataSource.lastIndexOf(group);

                if (index == lastIndex) {
                    // Handle Single Group
                    group = dataSource.get(index);
                } else {
                    // Handle Multiple Groups
                    for (int i = index; i <= lastIndex; i++) {
                        EventGroup tempGroup = dataSource.get(i);
                        // Check Event Time w/ Group
                        if (Common.between(event.startTime, tempGroup.getStartTime(),
                                tempGroup.getEndTime())) {
                            group = tempGroup;
                            break;
                        }
                    }
                }
                // For Adapter Use
                if (!updateGroups.contains(group)) {
                    updateGroups.add(group);
                }
            }

            dataSource.addEvent(event, group, comparator);

            if (scrollToEvent != null && event.equals(scrollToEvent)) {
                scrollToGroup = group;
            }
        }

        dataSource.sort();

        for (EventGroup group : insertGroups) {
            adapter.notifyItemInserted(dataSource.indexOf(group));
        }

        for (EventGroup group : updateGroups) {
            dataSource.removeItem(group.id);
            adapter.notifyItemChanged(dataSource.indexOf(group));
        }

        if (scrollToGroup != null) {
            scrollToPosition = dataSource.indexOf(scrollToGroup);
            if (scrollToPosition >= 0) {
                recyclerView.smoothScrollToPosition(scrollToPosition);
            }
        }

        text.setVisibility(View.INVISIBLE);
    }

    @Override
    public void update(List<Event> events, int scrollToPosition) {
        Event scrollToEvent = scrollToPosition >= 0 ? events.get(scrollToPosition) : null;
        EventGroup scrollToGroup = null;

        for (Event event : events) {
            Event tempEvent;
            String eventId;

            if (event.oldId != null) {
                tempEvent = new Event(event);
                eventId = tempEvent.id = tempEvent.oldId;
            } else {
                tempEvent = event;
                eventId = event.id;
            }

            EventGroup group = null;
            int index = -1;

            if (dataSource.containsEvent(eventId)) {
                group = dataSource.getEvent(eventId);
                index = group.indexOf(tempEvent);
            }

            if (group == null || index < 0) {
                continue;
            }

            dataSource.removeEvent(tempEvent.id, group);

            DateTimeZone timeZone = event.allDay ? DateTimeZone.UTC : DateTimeZone.getDefault();
            if (new LocalDate(event.startTime, timeZone).isEqual(group.date)) {
                dataSource.addEvent(event, group, comparator);

                if (scrollToEvent != null && event.equals(scrollToEvent)) {
                    scrollToGroup = group;
                }

                adapter.notifyItemChanged(dataSource.indexOf(group));
            } else {
                insert(event, false);
            }
        }

        if (scrollToGroup != null) {
            scrollToPosition = dataSource.indexOf(scrollToGroup);
            if (scrollToPosition >= 0) {
                recyclerView.smoothScrollToPosition(scrollToPosition);
            }
        }
    }

    @Override
    public void remove(List<String> ids, boolean scrollTo) {
        int size = ids.size();

        for (int i = 0; i < size; i++) {
            String id = ids.get(i);

            if (!dataSource.containsEvent(id)) {
                continue;
            }

            EventGroup group = dataSource.getEvent(id);
            int index = group.indexOf(id);

            if (index < 0) {
                continue;
            }

            if (scrollTo && i == size - 1) {
                recyclerView.smoothScrollToPosition(index);
            }

            index = dataSource.indexOf(group);
            dataSource.removeEvent(id, group);

            if (!group.isEmpty()) {
                adapter.notifyItemChanged(index);
            } else {
                dataSource.remove(group);
                adapter.notifyItemRemoved(index);
            }
        }

        if (dataSource.isEmpty()) {
            text.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void processEvents(List<Event> events) {
        int initSize = 0;

        EventGroup group = null;
        if (!dataSource.isEmpty()) {
            initSize = dataSource.getCount();
            group = dataSource.get(initSize - 1);
        }
        // Group Events
        for (int i = 0; i < events.size(); i++) {
            Event event = events.get(i);
            if (!checkEvent(event)) {
                continue;
            }
            // Create Event Group
            if (group == null || !checkEventGroup(event, group)) {
                String id = String.valueOf((int) (Math.random() * 10000));

                Location location;
                // Prioritize Location Suggestions
                if (!event.tempLocations.isEmpty()) {
                    location = event.tempLocations.get(0);
                } else {
                    location = event.location;
                }

                String title = location != null ? location.name : PLACEHOLDER;

                DateTimeZone timeZone = event.allDay ? DateTimeZone.UTC : DateTimeZone.getDefault();

                group = new EventGroup(id, title, event.startTime, timeZone);
                dataSource.add(group);
            }
            // Insert Event into Group
            dataSource.addEvent(event, group, comparator);
        }
        // Update Existing Event Group
        if (initSize != 0) {
            adapter.notifyItemChanged(initSize - 1);
        }
        // Insert Event Groups
        if (dataSource.getCount() > initSize) {
            text.setVisibility(View.INVISIBLE);
            adapter.notifyItemRangeInserted(initSize, dataSource.getCount() - initSize);
        }
    }

    /**
     * Check for event group criteria, which is defined by the location and date of the event.
     *
     * @param event Event to check.
     * @param eventGroup Event group to check against.
     * @return whether event belongs to this group.
     */
    protected boolean checkEventGroup(Event event, EventGroup eventGroup) {
        Location location;
        // Prioritize Location Suggestions
        if (!event.tempLocations.isEmpty()) {
            location = event.tempLocations.get(0);
        } else {
            location = event.location;
        }

        String title = location != null ? location.name : PLACEHOLDER;

        DateTimeZone timeZone = event.allDay ? DateTimeZone.UTC : DateTimeZone.getDefault();
        LocalDate date = new LocalDate(event.startTime, timeZone);

        return Common.compareStrings(title, eventGroup.title) && date.isEqual(eventGroup.date);
    }
}
