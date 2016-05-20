package com.mono.events;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
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
import com.mono.util.Common;
import com.mono.util.SimpleDataSource;
import com.mono.util.SimpleLinearLayoutManager;
import com.mono.util.SimpleSlideView.SimpleSlideViewListener;
import com.mono.util.SimpleTabLayout.Scrollable;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class FavoritesFragment extends Fragment implements SimpleDataSource<ListItem>,
        SimpleSlideViewListener, EventBroadcastListener, Scrollable {

    public static final String EXTRA_POSITION = "position";

    private static final SimpleDateFormat DATE_FORMAT;
    private static final SimpleDateFormat DATE_FORMAT_2;
    private static final SimpleDateFormat TIME_FORMAT;

    private int position;
    private ListFragment.ListListener listener;

    private RecyclerView recyclerView;
    private SimpleLinearLayoutManager layoutManager;
    private ListAdapter adapter;
    private TextView text;

    private final Map<String, ListItem> items = new HashMap<>();
    private final List<Event> events = new ArrayList<>();

    private AsyncTask<Void, Void, List<Event>> task;

    static {
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
        if (fragment != null && fragment instanceof ListFragment.ListListener) {
            listener = (ListFragment.ListListener) fragment;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_2, container, false);

        recyclerView = (RecyclerView) view.findViewById(R.id.list);
        recyclerView.setVerticalScrollBarEnabled(false);
        recyclerView.setLayoutManager(layoutManager = new SimpleLinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter = new ListAdapter(this));

        adapter.setDataSource(this);

        text = (TextView) view.findViewById(R.id.text);
        text.setVisibility(events.isEmpty() ? View.VISIBLE : View.INVISIBLE);

        initialize();

        return view;
    }

    private void initialize() {
        if (task != null) {
            task.cancel(true);
            task = null;
        }

        task = new AsyncTask<Void, Void, List<Event>>() {
            @Override
            protected List<Event> doInBackground(Void... params) {
                EventManager manager = EventManager.getInstance(getContext());
                return new ArrayList<>();
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

            LocalDate currentDate = new LocalDate();
            LocalDate startDate = new LocalDate(event.startTime);
            LocalDate endDate = new LocalDate(event.endTime);

            int colorId;

            if (Common.between(currentDate, startDate, endDate)) {
                colorId = R.color.gray_dark;
            } else if (event.startTime > System.currentTimeMillis()) {
                colorId = R.color.green;
            } else {
                colorId = R.color.gray_light_3;
            }
            item.dateTimeColor = Colors.getColor(getContext(), colorId);
        }

        return item;
    }

    private String getDateString(long time, TimeZone timeZone) {
        LocalDate currentDate = new LocalDate();

        LocalDateTime dateTime = new LocalDateTime(time);
        LocalDate date = dateTime.toLocalDate();

        SimpleDateFormat dateFormat;

        if (date.isEqual(currentDate)) {
            dateFormat = TIME_FORMAT;
        } else if (date.getYear() == currentDate.getYear()) {
            dateFormat = DATE_FORMAT;
        } else {
            dateFormat = DATE_FORMAT_2;
        }

        dateFormat.setTimeZone(timeZone);

        return dateFormat.format(dateTime.toDate());
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
        boolean scrollTo = data.getActor() == EventAction.ACTOR_SELF;

        switch (data.getAction()) {
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

    public void insert(Event event, boolean scrollTo) {
        if (events.contains(event)) {
            return;
        }

        events.add(event);

        Collections.sort(events, new Comparator<Event>() {
            @Override
            public int compare(Event e1, Event e2) {
                return Long.compare(e2.startTime, e1.startTime);
            }
        });

        int index = events.indexOf(event);
        adapter.notifyItemInserted(index);

        if (scrollTo) {
            recyclerView.smoothScrollToPosition(index);
        }

        text.setVisibility(View.INVISIBLE);
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

    public void update(Event event, boolean scrollTo) {
        int index = events.indexOf(event);
        if (index < 0) {
            return;
        }

        events.remove(index);
        items.remove(event.id);

        events.add(event);

        Collections.sort(events, new Comparator<Event>() {
            @Override
            public int compare(Event e1, Event e2) {
                return Long.compare(e2.startTime, e1.startTime);
            }
        });

        adapter.notifyItemChanged(index);

        int currentIndex = events.indexOf(event);
        if (currentIndex != index) {
            adapter.notifyItemMoved(index, currentIndex);
        }

        if (scrollTo) {
            recyclerView.smoothScrollToPosition(currentIndex);
        }
    }

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

    public void scrollTo(Event event) {
        int index = events.indexOf(event);

        if (index >= 0) {
            recyclerView.scrollToPosition(index);
        }
    }

    @Override
    public void scrollToTop() {

    }
}
