package com.mono.events;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mono.EventManager.EventAction;
import com.mono.EventManager.EventBroadcastListener;
import com.mono.R;
import com.mono.events.ListAdapter.ListClickListener;
import com.mono.events.ListAdapter.ListItem;
import com.mono.model.Event;
import com.mono.util.Common;
import com.mono.util.SimpleDataSource;
import com.mono.util.SimpleTabLayout.Scrollable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ListFragment extends Fragment implements SimpleDataSource<ListItem>,
        ListClickListener, EventBroadcastListener, Scrollable {

    private static final int REFRESH_LIMIT = 10;

    public static final String EXTRA_POSITION = "position";

    private int position;
    private ListListener listener;

    private SwipeRefreshLayout refreshLayout;
    private RecyclerView recyclerView;
    private ListAdapter adapter;
    private TextView text;

    private final Map<Long, ListItem> items = new HashMap<>();
    private final List<Event> events = new ArrayList<>();

    private AsyncTask<Long, Void, List<? extends Event>> task;

    private Calendar calendar;
    private SimpleDateFormat simpleDateFormat;
    private SimpleDateFormat timeFormat;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat otherDateFormat;

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

        calendar = Calendar.getInstance();

        simpleDateFormat = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault());
        timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        dateFormat = new SimpleDateFormat("MMM d", Locale.getDefault());
        otherDateFormat = new SimpleDateFormat("M/d/yy", Locale.getDefault());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);

        refreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.refresh_layout);
        refreshLayout.setColorSchemeResources(R.color.colorAccent);
        refreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });

        recyclerView = (RecyclerView) view.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter = new ListAdapter(this));
        recyclerView.addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                LinearLayoutManager manager =
                    (LinearLayoutManager) recyclerView.getLayoutManager();

                int lastPosition = manager.findLastVisibleItemPosition();
                if (lastPosition == events.size() - 1) {
                    if (task == null) {
                        more();
                    }
                }
            }
        });

        adapter.setDataSource(this);

        text = (TextView) view.findViewById(R.id.text);
        text.setVisibility(events.isEmpty() ? View.VISIBLE : View.INVISIBLE);

        more();

        return view;
    }

    @Override
    public ListItem getItem(int position) {
        ListItem item;

        if (!Common.between(position, 0, events.size())) {
            return null;
        }

        Event event = events.get(position);
        long id = event.id;

        if (items.containsKey(id)) {
            item = items.get(id);
        } else {
            item = new ListItem(id);

            item.type = ListItem.TYPE_EVENT;
            item.color = event.color;
            item.title = event.title;

            StringBuilder builder = new StringBuilder();

            String format = getResources().getString(R.string.start_time_format);
            calendar.setTimeInMillis(event.startTime);
            builder.append(String.format(format, simpleDateFormat.format(calendar.getTime())));
            builder.append('\n');

            format = getResources().getString(R.string.end_time_format);
            calendar.setTimeInMillis(event.endTime);
            builder.append(String.format(format, simpleDateFormat.format(calendar.getTime())));
            builder.append('\n');

            item.description = builder.toString();

            items.put(id, item);
        }

        if (item != null) {
            item.date = getDateString(event.startTime);
        }

        return item;
    }

    private String getDateString(long time) {
        calendar.setTimeInMillis(System.currentTimeMillis());
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentDay = calendar.get(Calendar.DATE);

        calendar.setTimeInMillis(time);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DATE);

        SimpleDateFormat simpleDateFormat;

        if (year == currentYear && month == currentMonth && day == currentDay) {
            simpleDateFormat = timeFormat;
        } else if (year == currentYear) {
            simpleDateFormat = dateFormat;
        } else {
            simpleDateFormat = otherDateFormat;
        }

        return simpleDateFormat.format(calendar.getTime());
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
                listener.onClick(this.position, item.id);
            }
        }
    }

    @Override
    public void onEventBroadcast(EventAction data) {
        switch (data.getAction()) {
            case EventAction.ACTION_CREATE:
                if (data.getStatus() == EventAction.STATUS_OK) {
                    insert(0, data.getEvent(), true);

                    if (data.getActor() == EventAction.ACTOR_SELF) {
                        scrollTo(data.getEvent());
                    }
                }
                break;
            case EventAction.ACTION_UPDATE:
                if (data.getStatus() == EventAction.STATUS_OK) {
                    update(data.getEvent(), true);
                }
                break;
            case EventAction.ACTION_REMOVE:
                if (data.getStatus() == EventAction.STATUS_OK) {
                    remove(data.getEvent(), true);
                }
                break;
        }
    }

    public void insert(int index, Event event, boolean notify) {
        if (!events.contains(event)) {
            index = Common.clamp(index, 0, events.size());
            events.add(index, event);

            if (notify) {
                adapter.notifyItemInserted(index);
            }
        }

        text.setVisibility(View.INVISIBLE);
    }

    public void insert(int index, List<? extends Event> events, boolean notify) {
        int size = events.size();

        for (int i = 0; i < size; i++) {
            insert(index + i, events.get(i), false);
        }

        if (notify) {
            adapter.notifyItemRangeInserted(index, events.size() - size);
        }
    }

    public void update(Event event, boolean notify) {
        int index = events.indexOf(event);

        if (index >= 0) {
            if (notify) {
                adapter.notifyItemChanged(index);
            }
        }
    }

    public void remove(Event event, boolean notify) {
        int index = events.indexOf(event);

        if (index >= 0) {
            events.remove(index);

            if (notify) {
                adapter.notifyItemRemoved(index);
            }
        }

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

    public void refresh() {
        if (task != null) {
            task.cancel(true);
        }

        long startTime = 0, endTime = System.currentTimeMillis();
        if (!events.isEmpty()) {
            startTime = events.get(0).startTime;
        }

        task = new AsyncTask<Long, Void, List<? extends Event>>() {
            @Override
            protected List<? extends Event> doInBackground(Long... params) {
                long startTime = params[0];
                long endTime = params[1];

                if (listener != null) {
                    return listener.onRefresh(position, startTime, endTime);
                }

                return null;
            }

            @Override
            protected void onPostExecute(List<? extends Event> result) {
                if (result != null) {
                    insert(0, result, true);
                }

                refreshLayout.setRefreshing(false);
                task = null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, startTime, endTime);
    }

    public void more() {
        if (task != null) {
            task.cancel(true);
        }

        long startTime;

        if (!events.isEmpty()) {
            startTime = events.get(events.size() - 1).startTime;
        } else {
            startTime = System.currentTimeMillis();
        }

        task = new AsyncTask<Long, Void, List<? extends Event>>() {
            @Override
            protected List<? extends Event> doInBackground(Long... params) {
                long startTime = params[0];

                if (listener != null) {
                    return listener.onMore(position, startTime, REFRESH_LIMIT);
                }

                return null;
            }

            @Override
            protected void onPostExecute(List<? extends Event> result) {
                if (result != null) {
                    insert(events.size(), result, true);
                }

                task = null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, startTime);
    }

    @Override
    public void scrollToTop() {
        recyclerView.smoothScrollToPosition(0);
    }

    public interface ListListener {

        void onClick(int position, long id);

        List<? extends Event> onRefresh(int position, long startTime, long endTime);

        List<? extends Event> onMore(int position, long startTime, int limit);
    }
}
