package com.mono.dashboard;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mono.EventManager;
import com.mono.EventManager.EventAction;
import com.mono.EventManager.EventBroadcastListener;
import com.mono.MainInterface;
import com.mono.R;
import com.mono.dashboard.EventsFragment.ListListener;
import com.mono.dashboard.EventGroupsListAdapter.EventGroupsListListener;
import com.mono.model.Calendar;
import com.mono.model.Event;
import com.mono.model.EventFilter;
import com.mono.model.Location;
import com.mono.provider.CalendarProvider;
import com.mono.settings.Settings;
import com.mono.util.Common;
import com.mono.util.Constants;
import com.mono.util.SimpleLinearLayoutManager;
import com.mono.util.SimpleTabLayout.Scrollable;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * A fragment that displays a list of events as groups. Events selected can be viewed or edited.
 * Using a sliding gesture of left or right on the event will reveal additional options to trigger
 * a chat conversation or perform a quick deletion of unwanted events.
 *
 * @author Gary Ng
 */
public class EventGroupsFragment extends Fragment implements EventGroupsListListener,
        EventBroadcastListener, SwipeRefreshLayout.OnRefreshListener, Scrollable {

    protected static final int PRECACHE_AMOUNT = 20;
    protected static final int PRECACHE_OFFSET = 10;

    protected static final String PLACEHOLDER = "Events";

    protected int position;
    protected ListListener listener;

    protected SwipeRefreshLayout refreshLayout;
    protected RecyclerView recyclerView;
    protected SimpleLinearLayoutManager layoutManager;
    protected EventGroupsListAdapter adapter;
    protected TextView text;

    protected EventGroupDataSource dataSource;

    protected Comparator<Event> comparator;
    protected int defaultDateTimeColorId;

    protected AsyncTask<Void, Void, List<Event>> task;
    protected long startTime;
    protected int offset;
    protected int offsetProvider;
    protected int direction;

    protected boolean isEditModeEnabled;
    protected List<String> eventSelections = new LinkedList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        position = DashboardFragment.TAB_EVENTS;

        Fragment fragment = getParentFragment();
        if (fragment != null && fragment instanceof ListListener) {
            listener = (ListListener) fragment;
        }

        comparator = new Comparator<Event>() {
            @Override
            public int compare(Event e1, Event e2) {
                int value = Long.compare(e2.startTime, e1.startTime);
                if (value != 0) {
                    return value;
                }

                return e2.id.compareToIgnoreCase(e1.id);
            }
        };

        defaultDateTimeColorId = R.color.gray_dark;;
        direction = -1;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);

        refreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.refresh_layout);
        refreshLayout.setColorSchemeResources(R.color.colorAccent);
        refreshLayout.setOnRefreshListener(this);

        recyclerView = (RecyclerView) view.findViewById(R.id.list);
        recyclerView.setVerticalScrollBarEnabled(false);
        recyclerView.setLayoutManager(layoutManager = new SimpleLinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter = new EventGroupsListAdapter(this));
        recyclerView.addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                handleInfiniteScroll(dy);
            }
        });

        dataSource = new EventGroupDataSource(getContext(), defaultDateTimeColorId);
        dataSource.setFilters(Settings.getInstance(getContext()).getEventFilters());

        adapter.setDataSource(dataSource);

        text = (TextView) view.findViewById(R.id.text);
        text.setVisibility(dataSource.isEmpty() ? View.VISIBLE : View.INVISIBLE);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (startTime > 0 && (System.currentTimeMillis() - startTime > Constants.HOUR_MS ||
                !new LocalDate(startTime).isEqual(new LocalDate()))) {
            dataSource.clear();
            adapter.notifyDataSetChanged();
        }

        if (dataSource.isEmpty()) {
            startTime = System.currentTimeMillis();
            offset = 0;
            offsetProvider = 0;

            append();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (isEditModeEnabled) {
            menu.clear();

            if (!eventSelections.isEmpty()) {
                inflater.inflate(R.menu.dashboard_edit, menu);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_sync:
                if (eventSelections.isEmpty()) {
                    return true;
                }

                showCalendarPicker();
                return true;
            case R.id.action_delete:
                if (eventSelections.isEmpty()) {
                    return true;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(getContext(),
                    R.style.AppTheme_Dialog_Alert);
                builder.setMessage(R.string.confirm_event_delete_multiple);

                DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                EventManager eventManager = EventManager.getInstance(getContext());
                                for (String id : eventSelections) {
                                    eventManager.removeEvent(EventAction.ACTOR_SELF, id, null);
                                }

                                getActivity().onBackPressed();
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }
                    }
                };

                builder.setPositiveButton(R.string.yes, listener);
                builder.setNegativeButton(R.string.no, listener);

                AlertDialog dialog = builder.create();
                dialog.show();

                return true;
            case R.id.action_edit:
                setEditMode(true, null);
                return true;
            case R.id.action_filter:
                onFilterClick();
                return true;
            case R.id.action_refresh:
                onRefresh();
                return true;
        }

        return super.onOptionsItemSelected(item);
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
     * Enable edit mode to allow multiple item selection.
     *
     * @param value State of edit mode.
     * @param id ID of item to be selected.
     */
    public void setEditMode(boolean value, String id) {
        isEditModeEnabled = value;

        eventSelections.clear();
        if (id != null) {
            eventSelections.add(id);
        }

        adapter.setSelectable(isEditModeEnabled, id);

        if (isEditModeEnabled) {
            refreshActionBar();
            // Switch Activity to Edit Mode
            final MainInterface mainInterface = (MainInterface) getActivity();
            mainInterface.setEditMode(new MainInterface.EditModeListener() {
                @Override
                public void onFinish() {
                    mainInterface.setToolbarTitle(R.string.dashboard);
                    setEditMode(false, null);
                }
            });
        }
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
     * Display the number of events selected in the action bar during Edit Mode.
     */
    private void refreshActionBar() {
        MainInterface mainInterface = (MainInterface) getActivity();

        if (eventSelections.isEmpty()) {
            mainInterface.setToolbarTitle(R.string.select_items);
        } else {
            mainInterface.setToolbarTitle(getString(R.string.value_selected, eventSelections.size()));
        }

        if (eventSelections.size() <= 1) {
            getActivity().invalidateOptionsMenu();
        }
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

    /**
     * Handle all event changes being reported by the Event Manager.
     *
     * @param data Event action data.
     */
    @Override
    public void onEventBroadcast(EventAction... data) {
        for (int i = 0; i < data.length; i++) {
            int action = -1;
            int scrollToPosition = -1;

            List<Event> events = new ArrayList<>();
            for (; i < data.length; i++) {
                EventAction item = data[i];

                if (action != -1 && action != item.getAction()) {
                    break;
                }

                action = item.getAction();

                if (item.getStatus() == EventAction.STATUS_OK) {
                    if (scrollToPosition == -1 && item.getActor() == EventAction.ACTOR_SELF) {
                        scrollToPosition = events.size();
                    }

                    events.add(item.getEvent());
                }
            }

            if (events.isEmpty()) {
                continue;
            }

            switch (action) {
                case EventAction.ACTION_CREATE:
                    insert(events, scrollToPosition);
                    break;
                case EventAction.ACTION_UPDATE:
                    update(events, scrollToPosition);
                    break;
                case EventAction.ACTION_REMOVE:
                    remove(events);
                    break;
            }
        }
    }

    /**
     * Handle refresh of UI.
     */
    @Override
    public void onRefresh() {
        if (task != null) {
            task.cancel(true);
            task = null;
        }

        dataSource.clear();
        adapter.notifyDataSetChanged();

        startTime = System.currentTimeMillis();
        offset = 0;
        offsetProvider = 0;

        append();

        refreshLayout.setRefreshing(false);
    }

    /**
     * Check if event is valid to be displayed within this fragment.
     *
     * @param event Event to check.
     * @return whether event is valid.
     */
    protected boolean checkEvent(Event event) {
        if (dataSource.containsEvent(event.id)) {
            return false;
        }

        LocalDateTime currentTime = new LocalDateTime();

        DateTimeZone timeZone = event.allDay ? DateTimeZone.UTC : DateTimeZone.getDefault();
        LocalDateTime dateTime = new LocalDateTime(event.startTime, timeZone);

        if (dateTime.isAfter(currentTime)) {
            return false;
        }

        for (EventFilter filter : dataSource.getFilters()) {
            if (!filter.status) {
                continue;
            }

            String text = filter.text.toLowerCase();
            if (event.title != null && event.title.toLowerCase().contains(text)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Handle the insertion of multiple events to be displayed.
     *
     * @param items Events to be inserted.
     * @param scrollToPosition Scroll to the event after insertion.
     */
    public void insert(List<Event> items, int scrollToPosition) {
        Event scrollToEvent = scrollToPosition >= 0 ? items.get(scrollToPosition) : null;
        EventGroup scrollToGroup = null;
        // Check Event Display Criteria
        Iterator<Event> iterator = items.iterator();
        while (iterator.hasNext()) {
            if (!checkEvent(iterator.next())) {
                iterator.remove();
            }
        }

        if (items.isEmpty()) {
            return;
        }

        List<EventGroup> insertGroups = new ArrayList<>();
        List<EventGroup> updateGroups = new ArrayList<>();

        for (Event event : items) {
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

            if (!dataSource.containsGroup(group)) {
                group.id = String.valueOf((int) (Math.random() * 10000));
                dataSource.addGroup(group);
                // For Adapter Use
                if (!insertGroups.contains(group)) {
                    insertGroups.add(group);
                }
            } else {
                int index = dataSource.indexOf(group);
                int lastIndex = dataSource.lastIndexOf(group);

                if (index == lastIndex) {
                    // Handle Single Group
                    group = dataSource.getGroup(index);
                } else {
                    // Handle Multiple Groups
                    for (int i = index; i <= lastIndex; i++) {
                        EventGroup tempGroup = dataSource.getGroup(i);
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

        dataSource.sortGroups();

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

    /**
     * Handle the insertion of an event to be displayed.
     *
     * @param event Event to be inserted.
     * @param scrollTo Scroll to the event after insertion.
     */
    public void insert(Event event, boolean scrollTo) {
        List<Event> events = new ArrayList<>(1);
        events.add(event);

        insert(events, scrollTo ? 0 : -1);
    }

    /**
     * Handle the refresh of events if it was updated.
     *
     * @param items Events to be updated.
     * @param scrollToPosition Scroll to the event after refresh.
     */
    public void update(List<Event> items, int scrollToPosition) {
        Event scrollToEvent = scrollToPosition >= 0 ? items.get(scrollToPosition) : null;
        EventGroup scrollToGroup = null;

        for (Event event : items) {
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
                group = dataSource.getGroupByEvent(eventId);
                index = group.events.indexOf(tempEvent);
            }

            if (group == null || index < 0) {
                continue;
            }

            dataSource.removeEvent(tempEvent, group);

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

    /**
     * Handle the removal of events.
     *
     * @param items Events to be removed.
     */
    public void remove(List<Event> items) {
        for (Event event : items) {
            if (!dataSource.containsEvent(event.id)) {
                continue;
            }

            EventGroup group = dataSource.getGroupByEvent(event.id);
            int index = group.events.indexOf(event);

            if (index < 0) {
                continue;
            }

            dataSource.removeEvent(event, group);

            index = dataSource.indexOf(group);

            if (!group.events.isEmpty()) {
                adapter.notifyItemChanged(index);
            } else {
                dataSource.removeGroup(group);
                adapter.notifyItemRemoved(index);
            }
        }

        if (dataSource.isEmpty()) {
            text.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Handle the removal of an event.
     *
     * @param event Event to be removed.
     */
    public void remove(Event event) {
        List<Event> events = new ArrayList<>(1);
        events.add(event);

        remove(events);
    }

    /**
     * Allows the list view to scroll infinitely in both directions depending on the available
     * events.
     *
     * @param deltaY Direction of the vertical scrolling.
     */
    protected void handleInfiniteScroll(int deltaY) {
        if (task != null) {
            return;
        }

        int position;

        if (deltaY > 0) {
            position = layoutManager.findLastVisibleItemPosition();
            if (position >= Math.max(dataSource.getCount() - 1 - PRECACHE_OFFSET, 0)) {
                append();
            }
        }
    }

    /**
     * Retrieve and append events at the bottom of the list.
     */
    protected void append() {
        if (task != null) {
            task.cancel(true);
            task = null;
        }

        task = new AsyncTask<Void, Void, List<Event>>() {
            @Override
            protected List<Event> doInBackground(Void... params) {
                List<Event> result = new ArrayList<>();

                EventManager manager = EventManager.getInstance(getContext());
                List<Event> tempResult = new ArrayList<>();
                // Retrieve Provider Events
                List<Event> providerEvents = manager.getEventsFromProviderByOffset(startTime,
                    offsetProvider, PRECACHE_AMOUNT, direction);
                tempResult.addAll(providerEvents);
                // Retrieve Local Events
                List<Event> events = manager.getEventsByOffset(startTime, offset,
                    PRECACHE_AMOUNT, direction);
                tempResult.addAll(events);
                // Sort By Time
                Collections.sort(tempResult, comparator);
                // Keep Set Amount
                for (Event event : tempResult) {
                    if (providerEvents.contains(event)) {
                        offsetProvider++;
                    } else {
                        offset++;
                    }

                    result.add(event);

                    if (result.size() >= PRECACHE_AMOUNT) {
                        break;
                    }
                }

                return result;
            }

            @Override
            protected void onPostExecute(List<Event> result) {
                if (!result.isEmpty()) {
                    processEvents(result);
                }

                task = null;
            }
        }.execute();
    }

    /**
     * Group events into their corresponding groups.
     *
     * @param events Events to be grouped.
     */
    protected void processEvents(List<Event> events) {
        int initSize = 0;

        EventGroup group = null;
        if (!dataSource.isEmpty()) {
            initSize = dataSource.getCount();
            group = dataSource.getGroup(initSize - 1);
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
                dataSource.addGroup(group);
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

    @Override
    public void scrollToTop() {
        recyclerView.scrollToPosition(0);
    }

    /**
     * Display the calendar picker to switch calendars.
     */
    public void showCalendarPicker() {
        final List<Calendar> calendars = CalendarProvider.getInstance(getContext()).getCalendars();
        final CharSequence[] items = new CharSequence[calendars.size()];

        for (int i = 0; i < calendars.size(); i++) {
            Calendar calendar = calendars.get(i);

            String text = calendar.name;
            if (!Common.isEmpty(calendar.accountName)) {
                text += "\n(" + calendar.accountName + ")";
            }
            items[i] = text;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(),
            R.style.AppTheme_Dialog_Alert);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                confirmSync(calendars.get(which));
            }
        });
        builder.create().show();
    }

    /**
     * Display a dialog asking user to confirm sync of events.
     *
     * @param calendar Target calendar for syncing.
     */
    public void confirmSync(final Calendar calendar) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(),
            R.style.AppTheme_Dialog_Alert);
        builder.setMessage(getString(R.string.confirm_event_sync_multiple, calendar.name));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        EventManager.getInstance(getContext()).syncEvents(EventAction.ACTOR_SELF,
                            eventSelections, calendar.id, null);

                        getActivity().onBackPressed();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        builder.setPositiveButton(R.string.yes, listener);
        builder.setNegativeButton(R.string.no, listener);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void syncEvents(Calendar calendar) {

    }

    /**
     * Display event filter dialog to manage to filters to hide events.
     */
    public void onFilterClick() {
        List<EventFilter> filters = Settings.getInstance(getContext()).getEventFilters();

        FilterDialog.create(getContext(), filters, new FilterDialog.FilterDialogCallback() {
            @Override
            public void onFinish(List<EventFilter> filters) {
                Settings settings = Settings.getInstance(getContext());

                List<EventFilter> original = settings.getEventFilters();
                settings.setEventFilters(filters);

                if (!filters.equals(original)) {
                    dataSource.setFilters(filters);
                    onRefresh();
                }
            }
        });
    }

    public static class EventGroup {

        public String id;
        public String title;
        public LocalDate date;
        public List<Event> events = new ArrayList<>();

        public EventGroup(String id, String title, long time, DateTimeZone timeZone) {
            this.id = id;
            this.title = title;
            this.date = new LocalDate(time, timeZone);
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof EventGroup)) {
                return false;
            }

            EventGroup eventGroup = (EventGroup) object;

            if (!Common.compareStrings(title, eventGroup.title)) {
                return false;
            }

            if (!date.isEqual(eventGroup.date)) {
                return false;
            }

            return true;
        }

        public void add(Event event, Comparator<Event> comparator) {
            if (!events.contains(event)) {
                events.add(event);

                if (comparator != null) {
                    Collections.sort(events, comparator);
                }
            }
        }

        public void remove(Event event) {
            events.remove(event);
        }

        public long getStartTime() {
            return events.get(events.size() - 1).startTime;
        }

        public long getEndTime() {
            return events.get(0).startTime;
        }
    }
}
