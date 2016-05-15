package com.mono.events;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mono.EventManager;
import com.mono.EventManager.EventAction;
import com.mono.EventManager.EventBroadcastListener;
import com.mono.R;
import com.mono.events.ListAdapter.ListItem;
import com.mono.model.Event;
import com.mono.util.Colors;
import com.mono.util.SimpleDataSource;
import com.mono.util.SimpleLinearLayoutManager;
import com.mono.util.SimpleSlideView.SimpleSlideViewListener;
import com.mono.util.SimpleTabLayout.Scrollable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class ListFragment extends Fragment implements SimpleDataSource<ListItem>,
        SimpleSlideViewListener, EventBroadcastListener, Scrollable {

    private static final int PRECACHE_AMOUNT = 20;
    private static final int PRECACHE_OFFSET = 10;

    public static final String EXTRA_POSITION = "position";

    private static final SimpleDateFormat DATETIME_FORMAT;
    private static final SimpleDateFormat DATE_FORMAT;
    private static final SimpleDateFormat DATE_FORMAT_2;
    private static final SimpleDateFormat TIME_FORMAT;

    private int position;
    private ListListener listener;

    private RecyclerView recyclerView;
    private SimpleLinearLayoutManager layoutManager;
    private ListAdapter adapter;
    private TextView text;

    private final Map<String, ListItem> items = new HashMap<>();
    private final List<Event> events = new ArrayList<>();

    private AsyncTask<Void, Void, List<Event>> task;
    private long startTime;
    private int futureOffset;
    private int futureOffsetProvider;
    private int pastOffset;
    private int pastOffsetProvider;

    static {
        DATETIME_FORMAT = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault());
        DATE_FORMAT = new SimpleDateFormat("MMM d", Locale.getDefault());
        DATE_FORMAT_2 = new SimpleDateFormat("M/d/yy", Locale.getDefault());
        TIME_FORMAT = new SimpleDateFormat("h:mm a", Locale.getDefault());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        if (bundle != null) {
            position = bundle.getInt(EXTRA_POSITION);
        }

        Fragment fragment = getParentFragment();
        if (fragment != null && fragment instanceof ListListener) {
            listener = (ListListener) fragment;
        }

        startTime = System.currentTimeMillis();
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

        today();

        return view;
    }

    @Override
    public ListItem getItem(int position) {
        ListItem item;

        Event event = events.get(position);
        String id = event.id;

        if (items.containsKey(id)) {
            item = items.get(id);
        } else {
            item = new ListItem(id);

            item.type = ListItem.TYPE_EVENT;
            item.iconResId = R.drawable.circle;
            item.iconColor = event.color;

            item.title = event.title;
            item.description = event.description;

            items.put(id, item);
        }

        if (item != null) {
            TimeZone timeZone = event.allDay ? TimeZone.getTimeZone("UTC") : TimeZone.getDefault();
            item.dateTime = getDateString(event.startTime, timeZone);

            if (event.startTime > System.currentTimeMillis()) {
                item.dateTimeColor = Colors.getColor(getContext(), R.color.green);
            }
        }

        return item;
    }

    private String getDateString(long time, TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);

        calendar.setTimeInMillis(time);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        SimpleDateFormat dateFormat;

        if (year == currentYear && month == currentMonth && day == currentDay) {
            dateFormat = TIME_FORMAT;
        } else if (year == currentYear) {
            dateFormat = DATE_FORMAT;
        } else {
            dateFormat = DATE_FORMAT_2;
        }

        dateFormat.setTimeZone(timeZone);

        return dateFormat.format(calendar.getTime());
    }

    @Override
    public int getCount() {
        return events.size();
    }

    @Override
    public void onClick(View view) {
        int position = recyclerView.getChildAdapterPosition(view);
        ListItem item = getItem(position);

        if (item != null) {
            if (listener != null) {
                listener.onClick(this.position, item.id, view);
            }
        }
    }

    @Override
    public boolean onLongClick(View view) {
        return false;
    }

    @Override
    public void onLeftButtonClick(View view, int index) {
        int position = recyclerView.getChildAdapterPosition(view);
        ListItem item = getItem(position);

        if (item != null) {
            if (listener != null) {
                switch (index) {
                    case ListAdapter.BUTTON_CHAT_INDEX:
                        listener.onChatClick(this.position, item.id);
                        break;
                    case ListAdapter.BUTTON_FAVORITE_INDEX:
                        listener.onFavoriteClick(this.position, item.id);
                        break;
                }
            }
        }
    }

    @Override
    public void onRightButtonClick(View view, int index) {
        int position = recyclerView.getChildAdapterPosition(view);
        ListItem item = getItem(position);

        if (item != null) {
            if (listener != null) {
                switch (index) {
                    case ListAdapter.BUTTON_DELETE_INDEX:
                        listener.onDeleteClick(this.position, item.id);
                        break;
                }
            }
        }
    }

    @Override
    public void onGesture(View view, boolean state) {
        layoutManager.setScrollEnabled(state);
    }

    @Override
    public void onEventBroadcast(EventAction data) {
        switch (data.getAction()) {
            case EventAction.ACTION_CREATE:
                if (data.getStatus() == EventAction.STATUS_OK) {
                    insert(0, data.getEvent());

                    if (data.getActor() == EventAction.ACTOR_SELF) {
                        scrollTo(data.getEvent());
                    }
                }
                break;
            case EventAction.ACTION_UPDATE:
                if (data.getStatus() == EventAction.STATUS_OK) {
                    update(data.getEvent());
                }
                break;
            case EventAction.ACTION_REMOVE:
                if (data.getStatus() == EventAction.STATUS_OK) {
                    remove(data.getEvent());
                }
                break;
        }
    }

    public void insert(int index, Event event) {
        List<Event> events = new ArrayList<>();
        events.add(event);

        insert(index, events);
    }

    public void insert(int index, List<Event> items) {
        int size = 0;

        for (Event event : items) {
            if (events.contains(event)) {
                continue;
            }

            events.add(index + size, event);
            size++;
        }

        text.setVisibility(View.INVISIBLE);
        adapter.notifyItemRangeInserted(index, size);
    }

    public void update(Event event) {
        int index = events.indexOf(event);

        if (index >= 0) {
            adapter.notifyItemChanged(index);
        }
    }

    public void remove(Event event) {
        int index = events.indexOf(event);

        if (index >= 0) {
            events.remove(index);
            adapter.notifyItemRemoved(index);
        }

        if (events.isEmpty()) {
            text.setVisibility(View.VISIBLE);
        }
    }

    private void handleInfiniteScroll(int deltaY) {
        if (task != null) {
            return;
        }

        int position;

        if (deltaY < 0) {
            position = layoutManager.findFirstVisibleItemPosition();
            if (position <= PRECACHE_OFFSET) {
                prepend();
            }
        } else if (deltaY > 0) {
            position = layoutManager.findLastVisibleItemPosition();
            if (position >= Math.max(events.size() - 1 - PRECACHE_OFFSET, 0)) {
                append();
            }
        }
    }

    private void prepend() {
        if (task != null) {
            task.cancel(true);
            task = null;
        }

        task = new AsyncTask<Void, Void, List<Event>>() {
            @Override
            protected List<Event> doInBackground(Void... params) {
                EventManager manager = EventManager.getInstance(getContext());

                List<Event> result = manager.getEventsFromProviderByOffset(startTime,
                    futureOffsetProvider, PRECACHE_AMOUNT, 1);
                futureOffsetProvider += result.size();

                List<Event> events = manager.getEventsByOffset(startTime, futureOffset,
                    PRECACHE_AMOUNT, 1);
                futureOffset += events.size();

                combine(result, events);

                return result;
            }

            @Override
            protected void onPostExecute(List<Event> result) {
                if (!result.isEmpty()) {
                    insert(0, result);
                }

                task = null;
            }
        }.execute();
    }

    private void append() {
        if (task != null) {
            task.cancel(true);
            task = null;
        }

        task = new AsyncTask<Void, Void, List<Event>>() {
            @Override
            protected List<Event> doInBackground(Void... params) {
                EventManager manager = EventManager.getInstance(getContext());

                List<Event> result = manager.getEventsFromProviderByOffset(startTime,
                    pastOffsetProvider, PRECACHE_AMOUNT, -1);
                pastOffsetProvider += result.size();

                List<Event> events = manager.getEventsByOffset(startTime, pastOffset,
                    PRECACHE_AMOUNT, -1);
                pastOffset += events.size();

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

    private void combine(List<Event> result, List<Event> events) {
        for (Event event : events) {
            if (result.contains(event)) {
                int index = result.indexOf(event);
                result.remove(index);
                result.add(index, event);
            } else {
                result.add(event);
            }
        }

        Collections.sort(result, new Comparator<Event>() {
            @Override
            public int compare(Event e1, Event e2) {
                return Long.compare(e2.startTime, e1.startTime);
            }
        });
    }

    public void scrollTo(Event event) {
        int index = events.indexOf(event);

        if (index >= 0) {
            recyclerView.scrollToPosition(index);
        }
    }

    public void today() {
        events.clear();
        adapter.notifyDataSetChanged();

        startTime = System.currentTimeMillis();
        append();
    }

    @Override
    public void scrollToTop() {

    }

    public interface ListListener {

        void onClick(int position, String id, View view);

        void onLongClick(int position, String id, View view);

        void onChatClick(int position, String id);

        void onFavoriteClick(int position, String id);

        void onDeleteClick(int position, String id);
    }
}
