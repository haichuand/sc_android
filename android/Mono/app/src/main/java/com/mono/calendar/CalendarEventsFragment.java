package com.mono.calendar;

import android.animation.Animator;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mono.R;
import com.mono.calendar.CalendarEventsAdapter.CalendarEventsClickListener;
import com.mono.calendar.CalendarEventsAdapter.CalendarEventsItem;
import com.mono.events.ListAdapter.ListItem;
import com.mono.model.Event;
import com.mono.util.Common;
import com.mono.util.SimpleDataSource;
import com.mono.util.Views;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalendarEventsFragment extends Fragment implements SimpleDataSource<CalendarEventsItem>,
        CalendarEventsClickListener {

    private static final long SCALE_DURATION = 300;

    private CalendarEventsListener listener;

    private RecyclerView recyclerView;
    private CalendarEventsAdapter adapter;

    private final Map<Long, CalendarEventsItem> items = new HashMap<>();
    private final List<Event> events = new ArrayList<>();

    private SimpleDateFormat timeFormat;
    private Animator animator;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Fragment fragment = getParentFragment();
        if (fragment != null && fragment instanceof CalendarEventsListener) {
            listener = (CalendarEventsListener) fragment;
        }

        timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar_events, container, false);

        recyclerView = (RecyclerView) view.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter = new CalendarEventsAdapter(this));
        recyclerView.addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {

            }
        });

        adapter.setDataSource(this);

        return view;
    }

    @Override
    public CalendarEventsItem getItem(int position) {
        CalendarEventsItem item;

        Event event = events.get(position);
        long id = event.id;

        if (items.containsKey(id)) {
            item = items.get(id);
        } else {
            item = new CalendarEventsItem(id);

            item.type = ListItem.TYPE_EVENT;
            item.iconResId = R.drawable.circle;
            item.iconColor = event.color;
            item.startTime = timeFormat.format(event.startTime);
            item.endTime = timeFormat.format(event.endTime);
            item.title = event.title;
            item.description = event.description;

            items.put(id, item);
        }

        return item;
    }

    @Override
    public int getCount() {
        return events.size();
    }

    @Override
    public void onClick(View view) {
        int position = recyclerView.getChildAdapterPosition(view);
        CalendarEventsItem item = getItem(position);

        if (item != null) {
            if (listener != null) {
                listener.onClick(item.id);
            }
        }
    }

    @Override
    public boolean onLongClick(long id, View view) {
        if (listener != null) {
            listener.onLongClick(id, view);
        }

        return true;
    }

    public void clear(boolean notify) {
        events.clear();

        if (notify) {
            adapter.notifyDataSetChanged();
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
    }

    public void show(int height, boolean animate) {
        if (animator != null) {
            animator.cancel();
        }

        View view = getView();
        if (view == null) {
            return;
        }

        if (animate) {
            animator = Views.scale(view, height, SCALE_DURATION, null);
        } else {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.height = height;

            view.setLayoutParams(params);
        }
    }

    public void hide(boolean animate) {
        show(0, animate);
    }

    public interface CalendarEventsListener {

        void onClick(long id);

        void onLongClick(long id, View view);
    }
}
