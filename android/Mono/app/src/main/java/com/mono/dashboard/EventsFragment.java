package com.mono.dashboard;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import com.mono.dashboard.ListAdapter.DashboardListListener;
import com.mono.dashboard.ListAdapter.ListItem;
import com.mono.dashboard.ListAdapter.PhotoItem;
import com.mono.model.Event;
import com.mono.util.Colors;
import com.mono.util.SimpleDataSource;
import com.mono.util.SimpleLinearLayoutManager;
import com.mono.util.SimpleTabLayout.Scrollable;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * A fragment that displays a list of events. Events selected can be viewed or edited. Using a
 * sliding gesture of left or right on the event will reveal additional options to trigger a
 * chat conversation or perform a quick deletion of unwanted events.
 *
 * @author Gary Ng
 */
public class EventsFragment extends Fragment implements SimpleDataSource<ListItem>,
        DashboardListListener, EventBroadcastListener, Scrollable {

    protected static final int PRECACHE_AMOUNT = 20;
    protected static final int PRECACHE_OFFSET = 10;

    protected static final SimpleDateFormat DATE_FORMAT;
    protected static final SimpleDateFormat DATE_FORMAT_2;
    protected static final SimpleDateFormat TIME_FORMAT;

    protected int position;
    protected ListListener listener;

    protected RecyclerView recyclerView;
    protected SimpleLinearLayoutManager layoutManager;
    protected ListAdapter adapter;
    protected TextView text;

    protected final Map<String, ListItem> items = new HashMap<>();
    protected final List<Event> events = new ArrayList<>();

    protected Comparator<Event> comparator;
    protected int defaultDateTimeColorId;

    protected AsyncTask<Void, Void, List<Event>> task;
    protected long startTime;
    protected int offset;
    protected int offsetProvider;

    protected boolean isEditModeEnabled;
    protected List<String> eventSelections = new LinkedList<>();

    static {
        DATE_FORMAT = new SimpleDateFormat("MMM d", Locale.getDefault());
        DATE_FORMAT_2 = new SimpleDateFormat("M/d/yy", Locale.getDefault());
        TIME_FORMAT = new SimpleDateFormat("h:mm a", Locale.getDefault());
    }

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
                return Long.compare(e2.startTime, e1.startTime);
            }
        };

        defaultDateTimeColorId = R.color.gray_dark;;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_2, container, false);

        recyclerView = (RecyclerView) view.findViewById(R.id.list);
        recyclerView.setVerticalScrollBarEnabled(false);
        recyclerView.setLayoutManager(layoutManager = new SimpleLinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter = new ListAdapter(this));
        recyclerView.addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                handleInfiniteScroll(dy);
            }
        });

        adapter.setDataSource(this);

        text = (TextView) view.findViewById(R.id.text);
        text.setVisibility(events.isEmpty() ? View.VISIBLE : View.INVISIBLE);

        LocalDate date = new LocalDate().plusDays(1);
        startTime = date.toDateTimeAtStartOfDay().minusMillis(1).getMillis();
        append();

        return view;
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
                setEditMode(true);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Retrieve events as items to be displayed by the adapter. Special case events such as one
     * with photos will return as a different type of item to be displayed differently.
     *
     * @param position Position of the event.
     * @return item to display event information.
     */
    @Override
    public ListItem getItem(int position) {
        ListItem item;

        Event event = events.get(position);
        String id = event.id;

        if (items.containsKey(id)) {
            item = items.get(id);
        } else {
            if (event.photos != null && !event.photos.isEmpty()) {
                PhotoItem photoItem = new PhotoItem(id);
                photoItem.photos = event.photos;

                item = photoItem;
            } else {
                item = new ListItem(id);
            }

            item.selected = eventSelections.contains(id);

            item.type = ListItem.TYPE_EVENT;
            item.iconResId = R.drawable.circle;
            item.iconColor = event.color;

            if (event.title != null && !event.title.isEmpty()) {
                item.title = event.title;
            } else {
                item.title = "(" + getString(R.string.no_subject) + ")";
            }

            item.description = event.description;

            items.put(id, item);
        }
        // Date Display
        if (item != null) {
            int colorId;
            boolean bold;

            if (event.viewTime == 0) {
                colorId = R.color.gray_dark;
                bold = true;
            } else {
                colorId = R.color.gray_light_3;
                bold = false;
            }

            item.titleColor = Colors.getColor(getContext(), colorId);
            item.titleBold = bold;

            TimeZone timeZone = event.allDay ? TimeZone.getTimeZone("UTC") : TimeZone.getDefault();
            item.dateTime = getDateString(event.startTime, timeZone, event.allDay);

            if (event.viewTime == 0) {
                colorId = defaultDateTimeColorId;
                bold = true;
            } else {
                colorId = R.color.gray_light_3;
                bold = false;
            }

            item.dateTimeColor = Colors.getColor(getContext(), colorId);
            item.dateTimeBold = bold;
        }

        return item;
    }

    /**
     * Helper function to convert milliseconds into a readable date string that takes time zone
     * into account.
     *
     * @param time Time in milliseconds.
     * @param timeZone Time zone to be used.
     * @param allDay All day event.
     * @return date string.
     */
    protected String getDateString(long time, TimeZone timeZone, boolean allDay) {
        LocalDate currentDate = new LocalDate();

        LocalDateTime dateTime = new LocalDateTime(time);
        LocalDate date = dateTime.toLocalDate();

        SimpleDateFormat dateFormat;

        if (date.isEqual(currentDate)) {
            if (allDay) {
                return getString(R.string.today);
            } else {
                dateFormat = TIME_FORMAT;
            }
        } else if (date.getYear() == currentDate.getYear()) {
            dateFormat = DATE_FORMAT;
        } else {
            dateFormat = DATE_FORMAT_2;
        }

        dateFormat.setTimeZone(timeZone);

        return dateFormat.format(dateTime.toDate());
    }

    /**
     * Retrieve the number of events to be used by the adapter.
     *
     * @return number of events.
     */
    @Override
    public int getCount() {
        return events.size();
    }

    /**
     * Handle the action of clicking an event and notify any listeners.
     *
     * @param view View of the event.
     */
    @Override
    public void onClick(View view) {
        int position = recyclerView.getChildAdapterPosition(view);
        ListItem item = getItem(position);
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

        setEditMode(true);

        int position = recyclerView.getChildAdapterPosition(view);
        ListItem item = getItem(position);
        if (item == null) {
            return false;
        }

        onSelectClick(view, true);

        return true;
    }

    /**
     * Enable edit mode to allow multiple item selection.
     *
     * @param value State of edit mode
     */
    public void setEditMode(boolean value) {
        isEditModeEnabled = value;
        // Clear Selections
        for (ListItem item : items.values()) {
            item.selected = false;
        }
        eventSelections.clear();

        adapter.setSelectable(isEditModeEnabled);
        adapter.notifyItemRangeChanged(0, adapter.getItemCount());

        if (isEditModeEnabled) {
            refreshActionBar();
            // Switch Activity to Edit Mode
            final MainInterface mainInterface = (MainInterface) getActivity();
            mainInterface.setEditMode(new MainInterface.EditModeListener() {
                @Override
                public void onFinish() {
                    mainInterface.setToolbarTitle(R.string.dashboard);
                    setEditMode(false);
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
    public void onSelectClick(View view, boolean value) {
        int position = recyclerView.getChildAdapterPosition(view);
        ListItem item = getItem(position);
        if (item == null) {
            return;
        }

        item.selected = value;

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
     * @param index Index of the action.
     */
    @Override
    public void onLeftButtonClick(View view, int index) {
        int position = recyclerView.getChildAdapterPosition(view);
        ListItem item = getItem(position);
        if (item == null) {
            return;
        }

        if (listener != null) {
            switch (index) {
                case ListAdapter.BUTTON_CHAT_INDEX:
                    listener.onChatClick(this.position, item.id);
                    break;
                case ListAdapter.BUTTON_FAVORITE_INDEX:
                    listener.onFavoriteClick(this.position, item.id);

                    items.remove(item.id);
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
        ListItem item = getItem(position);
        if (item == null) {
            return;
        }

        if (listener != null) {
            switch (index) {
                case ListAdapter.BUTTON_DELETE_INDEX:
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

    /**
     * Handle all event changes being reported by the Event Manager.
     *
     * @param data Event action data.
     */
    @Override
    public void onEventBroadcast(EventAction data) {
        boolean scrollTo = data.getActor() == EventAction.ACTOR_SELF;

        switch (data.getAction()) {
            case EventAction.ACTION_CREATE:
                if (data.getStatus() == EventAction.STATUS_OK) {
                    insert(data.getEvent(), scrollTo);
                }
                break;
            case EventAction.ACTION_UPDATE:
                if (data.getStatus() == EventAction.STATUS_OK) {
                    update(data.getEvent(), scrollTo);
                }
                break;
            case EventAction.ACTION_REMOVE:
                if (data.getStatus() == EventAction.STATUS_OK) {
                    remove(data.getEvent());
                }
                break;
        }
    }

    /**
     * Check if event is valid to be displayed within this fragment.
     *
     * @param event Event to check.
     * @return whether event is valid.
     */
    protected boolean checkEvent(Event event) {
        if (events.contains(event)) {
            return false;
        }

        LocalDateTime currentTime = new LocalDateTime();

        DateTimeZone timeZone = event.allDay ? DateTimeZone.UTC : DateTimeZone.getDefault();
        LocalDateTime dateTime = new LocalDateTime(event.startTime, timeZone);

        if (dateTime.isAfter(currentTime)) {
            return false;
        }

        return true;
    }

    /**
     * Handle the insertion of an event to be displayed.
     *
     * @param event Instance of the event.
     * @param scrollTo Scroll to the event after insertion.
     */
    public void insert(Event event, boolean scrollTo) {
        if (!checkEvent(event)) {
            return;
        }

        events.add(event);

        Collections.sort(events, comparator);

        int index = events.indexOf(event);
        adapter.notifyItemInserted(index);

        if (scrollTo) {
            recyclerView.smoothScrollToPosition(index);
        }

        text.setVisibility(View.INVISIBLE);
    }

    /**
     * Handle the insertion of multiple events at a starting index.
     *
     * @param index Index to insert.
     * @param items Events to be inserted.
     */
    public void insert(int index, List<Event> items) {
        int size = 0;

        for (Event event : items) {
            if (!checkEvent(event)) {
                continue;
            }

            events.add(index + size, event);
            size++;
        }

        text.setVisibility(View.INVISIBLE);
        adapter.notifyItemRangeInserted(index, size);
    }

    /**
     * Handle the refresh of an event if it was updated.
     *
     * @param event Instance of the event.
     * @param scrollTo Scroll to the event after refresh.
     */
    public void update(Event event, boolean scrollTo) {
        int index = events.indexOf(event);
        if (index < 0) {
            return;
        }

        events.remove(index);
        items.remove(event.id);

        events.add(event);

        Collections.sort(events, comparator);

        adapter.notifyItemChanged(index);

        int currentIndex = events.indexOf(event);
        if (currentIndex != index) {
            adapter.notifyItemMoved(index, currentIndex);
        }

        if (scrollTo) {
            recyclerView.smoothScrollToPosition(currentIndex);
        }
    }

    /**
     * Handle the removal of an event.
     *
     * @param event Instance of the event.
     */
    public void remove(Event event) {
        int index = events.indexOf(event);
        if (index < 0) {
            return;
        }

        events.remove(index);
        adapter.notifyItemRemoved(index);

        if (events.isEmpty()) {
            text.setVisibility(View.VISIBLE);
        }
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
            if (position >= Math.max(events.size() - 1 - PRECACHE_OFFSET, 0)) {
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
                EventManager manager = EventManager.getInstance(getContext());

                List<Event> result = manager.getEventsFromProviderByOffset(startTime,
                    offsetProvider, PRECACHE_AMOUNT, -1);
                offsetProvider += result.size();

                List<Event> events = manager.getEventsByOffset(startTime, offset,
                    PRECACHE_AMOUNT, -1);
                offset += events.size();

                combine(result, events);

                return result;
            }

            @Override
            protected void onPostExecute(List<Event> result) {
                if (!result.isEmpty()) {
                    insert(events.size(), result);
                }

                task = null;
            }
        }.execute();
    }

    /**
     * Used to combine two list of events into one sorted list.
     *
     * @param result List for events to be added.
     * @param events Events to be added.
     */
    protected void combine(List<Event> result, List<Event> events) {
        for (Event event : events) {
            if (result.contains(event)) {
                int index = result.indexOf(event);
                result.remove(index);
                result.add(index, event);
            } else {
                result.add(event);
            }
        }

        Collections.sort(result, comparator);
    }

    /**
     * Scroll to a specific event.
     *
     * @param event Instance of the event.
     */
    public void scrollTo(Event event) {
        int index = events.indexOf(event);

        if (index >= 0) {
            recyclerView.scrollToPosition(index);
        }
    }

    @Override
    public void scrollToTop() {
        recyclerView.scrollToPosition(0);
    }

    public interface ListListener {

        void onClick(int tab, String id, View view);

        void onLongClick(int tab, String id, View view);

        void onChatClick(int tab, String id);

        void onFavoriteClick(int tab, String id);

        void onDeleteClick(int tab, String id);
    }
}
