package com.mono.dashboard;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
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
import com.mono.dashboard.EventHolder.ListListener;
import com.mono.model.Calendar;
import com.mono.model.Event;
import com.mono.model.EventFilter;
import com.mono.model.Instance;
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
import java.util.LinkedList;
import java.util.List;

/**
 * A fragment that displays a list of events as groups. Events selected can be viewed or edited.
 * Using a sliding gesture of left or right on the event will reveal additional options to trigger
 * a chat conversation or perform a quick deletion of unwanted events.
 *
 * @author Gary Ng
 */
public abstract class BaseEventsFragment<E extends Instance, I> extends Fragment implements
        EventBroadcastListener, SwipeRefreshLayout.OnRefreshListener, Scrollable {

    protected static final int PRECACHE_AMOUNT = 20;
    protected static final int PRECACHE_OFFSET = 10;
    protected static final int REFRESH_THRESHOLD = 10;
    protected static final int MAX_VISIBLE_ITEMS = 10;

    protected static final int SORT_ORDER_DESC = -1;
    protected static final int SORT_ORDER_ASC = 1;

    protected int position;
    protected ListListener listener;

    protected RecyclerView recyclerView;
    protected SimpleLinearLayoutManager layoutManager;
    protected BaseEventsListAdapter<I> adapter;
    protected TextView text;

    protected BaseEventsDataSource<E, I> dataSource;

    protected Comparator<Event> comparator;

    protected AsyncTask<Void, Void, List<Event>> task;
    protected long startTime;
    protected int offset;
    protected int offsetProvider;
    protected int sortOrder;

    protected boolean isEditModeEnabled;
    protected List<String> eventSelections = new LinkedList<>();

    public abstract void onPreCreate();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        onPreCreate();

        if (sortOrder > 0) {
            comparator = new Comparator<Event>() {
                @Override
                public int compare(Event e1, Event e2) {
                    int value = Long.compare(e1.startTime, e2.startTime);
                    if (value != 0) {
                        return value;
                    }

                    return e1.id.compareToIgnoreCase(e2.id);
                }
            };
        } else {
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
        }

        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        Fragment fragment = getParentFragment();
        if (fragment != null && fragment instanceof ListListener) {
            listener = (ListListener) fragment;
        }

        dataSource.setFilters(Settings.getInstance(getContext()).getEventFilters());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_2, container, false);

        recyclerView = (RecyclerView) view.findViewById(R.id.list);
        recyclerView.setVerticalScrollBarEnabled(false);
        recyclerView.setLayoutManager(layoutManager = new SimpleLinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new SimpleOnScrollListener(PRECACHE_OFFSET) {
            @Override
            public boolean canScroll() {
                return task == null;
            }

            @Override
            public void onScrolledTop() {

            }

            @Override
            public void onScrolledBottom() {
                append();
            }

            @Override
            public int getCount() {
                return dataSource.getCount();
            }
        });

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
            case R.id.action_refresh:
                onRefresh();
                return true;
            case R.id.action_edit:
                setEditMode(true, null);
                return true;
            case R.id.action_filter:
                onFilterClick();
                return true;
        }

        return super.onOptionsItemSelected(item);
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
     * Display the number of events selected in the action bar during Edit Mode.
     */
    public void refreshActionBar() {
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
     * Handle all event changes being reported by the Event Manager.
     *
     * @param data Event action data.
     */
    @Override
    public void onEventBroadcast(EventAction... data) {
        if (data.length > REFRESH_THRESHOLD) {
            onRefresh();
        } else {
            EventManager manager = EventManager.getInstance(getContext());

            for (EventAction action : data) {
                if (action.getStatus() == EventAction.STATUS_OK) {
                    Event event = manager.getEvent(action.getId());
                    boolean scrollToPosition = action.getActor() == EventAction.ACTOR_SELF;

                    switch (action.getAction()) {
                        case EventAction.ACTION_CREATE:
                            insert(event, scrollToPosition);
                            break;
                        case EventAction.ACTION_UPDATE:
                            update(event, scrollToPosition);
                            break;
                        case EventAction.ACTION_REMOVE:
                            remove(action.getId(), scrollToPosition);
                            break;
                    }
                }
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
    }

    /**
     * Check if event is valid to be displayed within this fragment.
     *
     * @param event Event to check.
     * @return whether event is valid.
     */
    public boolean checkEvent(Event event) {
        if (dataSource.containsEvent(event.id)) {
            return false;
        }

        LocalDateTime currentTime = new LocalDateTime();

        DateTimeZone timeZone = event.allDay ? DateTimeZone.UTC : DateTimeZone.getDefault();
        LocalDateTime dateTime = new LocalDateTime(event.startTime, timeZone);

        if (sortOrder <= 0 && dateTime.isAfter(currentTime) || sortOrder > 0 &&
                dateTime.isBefore(currentTime)) {
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
     * @param events Events to be inserted.
     * @param scrollToPosition Scroll to the event after insertion.
     */
    public abstract void insert(List<Event> events, int scrollToPosition);

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
     * @param events Events to be updated.
     * @param scrollToPosition Scroll to the event after refresh.
     */
    public abstract void update(List<Event> events, int scrollToPosition);

    /**
     * Handle the refresh of an event if it was updated.
     *
     * @param event Event to be updated.
     * @param scrollTo Scroll to the event after refresh.
     */
    public void update(Event event, boolean scrollTo) {
        List<Event> events = new ArrayList<>(1);
        events.add(event);

        update(events, scrollTo ? 0 : -1);
    }

    /**
     * Handle the removal of events.
     *
     * @param ids Events to be removed.
     * @param scrollTo Scroll to the event before removal.
     */
    public abstract void remove(List<String> ids, boolean scrollTo);

    /**
     * Handle the removal of an event.
     *
     * @param id Event to be removed.
     * @param scrollTo Scroll to the event before removal.
     */
    public void remove(String id, boolean scrollTo) {
        List<String> ids = new ArrayList<>(1);
        ids.add(id);

        remove(ids, scrollTo);
    }

    /**
     * Retrieve and append events at the bottom of the list.
     */
    private void append() {
        if (task != null) {
            task.cancel(true);
            task = null;
        }

        task = new AsyncTask<Void, Void, List<Event>>() {
            @Override
            protected List<Event> doInBackground(Void... params) {
                return retrieveEvents();
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
     * Retrieve events.
     *
     * @return events.
     */
    public List<Event> retrieveEvents() {
        List<Event> result = new ArrayList<>();

        EventManager manager = EventManager.getInstance(getContext());
        List<Event> tempResult = new ArrayList<>();
        // Retrieve Provider Events
        List<Event> providerEvents = manager.getEventsFromProviderByOffset(startTime,
            offsetProvider, PRECACHE_AMOUNT, sortOrder);
        tempResult.addAll(providerEvents);
        // Retrieve Local Events
        List<Event> events = manager.getEventsByOffset(startTime, offset, PRECACHE_AMOUNT,
            sortOrder);
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

    /**
     * Group events into their corresponding groups.
     *
     * @param events Events to be grouped.
     */
    public abstract void processEvents(List<Event> events);

    @Override
    public void scrollToTop() {
        recyclerView.scrollToPosition(0);
    }

    /**
     * Display the calendar picker to switch calendars.
     */
    private void showCalendarPicker() {
        final List<Calendar> calendars = CalendarProvider.getInstance(getContext()).getCalendars();
        CharSequence[] items = new CharSequence[calendars.size()];

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
    private void confirmSync(final Calendar calendar) {
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

    /**
     * Display event filter dialog to manage to filters to hide events.
     */
    private void onFilterClick() {
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
}
