package com.mono.calendar;

import android.animation.Animator;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mono.R;
import com.mono.calendar.CalendarEventsAdapter.CalendarEventsItem;
import com.mono.events.ListAdapter.ListItem;
import com.mono.model.Event;
import com.mono.util.Common;
import com.mono.util.OnBackPressedListener;
import com.mono.util.Pixels;
import com.mono.util.SimpleDataSource;
import com.mono.util.SimpleLinearLayoutManager;
import com.mono.util.SimpleSlideView.SimpleSlideViewListener;
import com.mono.util.Views;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalendarEventsFragment extends Fragment implements OnBackPressedListener,
        SimpleDataSource<CalendarEventsItem>, SimpleSlideViewListener {

    private static final long SCALE_DURATION = 300;

    private CalendarEventsListener listener;

    private RecyclerView recyclerView;
    private SimpleLinearLayoutManager layoutManager;
    private CalendarEventsAdapter adapter;

    private final Map<String, CalendarEventsItem> items = new HashMap<>();
    private final List<Event> events = new ArrayList<>();

    private SimpleDateFormat timeFormat;
    private Animator animator;

    private int currentHeight;
    private boolean isExpanded;

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
        final View view = inflater.inflate(R.layout.fragment_calendar_events, container, false);

        recyclerView = (RecyclerView) view.findViewById(R.id.list);
        recyclerView.setLayoutManager(layoutManager = new SimpleLinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter = new CalendarEventsAdapter(this));
        recyclerView.addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    if (!isExpanded) {
                        int height = Math.round(Pixels.Display.getHeight(getContext()) * 0.45f);
                        animator = Views.scale(view, height, SCALE_DURATION, null);
                        isExpanded = true;
                    }
                }
            }
        });

        adapter.setDataSource(this);

        return view;
    }

    @Override
    public boolean onBackPressed() {
        if (isExpanded) {
            show(currentHeight, true);
            return true;
        } else if (isShowing()) {
            hide(true);
            return true;
        }

        return false;
    }

    @Override
    public CalendarEventsItem getItem(int position) {
        CalendarEventsItem item;

        Event event = events.get(position);
        String id = event.id;

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
                listener.onClick(item.id, view);
            }
        }
    }

    @Override
    public boolean onLongClick(View view) {
        int position = recyclerView.getChildAdapterPosition(view);
        CalendarEventsItem item = getItem(position);

        if (item != null) {
            if (listener != null) {
                listener.onLongClick(item.id, view);
            }
        }

        return true;
    }

    @Override
    public void onLeftButtonClick(View view, int index) {
        int position = recyclerView.getChildAdapterPosition(view);
        CalendarEventsItem item = getItem(position);

        if (item != null) {
            if (listener != null) {
                switch (index) {
                    case CalendarEventsAdapter.BUTTON_CHAT_INDEX:
                        listener.onChatClick(item.id);
                        break;
                    case CalendarEventsAdapter.BUTTON_FAVORITE_INDEX:
                        listener.onFavoriteClick(item.id);
                        break;
                }
            }
        }
    }

    @Override
    public void onRightButtonClick(View view, int index) {
        int position = recyclerView.getChildAdapterPosition(view);
            CalendarEventsItem item = getItem(position);

        if (item != null) {
            if (listener != null) {
                switch (index) {
                    case CalendarEventsAdapter.BUTTON_DELETE_INDEX:
                        listener.onDeleteClick(item.id);
                        break;
                }
            }
        }
    }

    @Override
    public void onGesture(View view, boolean state) {
        layoutManager.setScrollEnabled(state);
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

    public void refresh(Event event, boolean notify) {
        int index = events.indexOf(event);

        if (index >= 0) {
            events.remove(index);
            events.add(index, event);
            items.remove(event.id);

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

            if (events.isEmpty()) {
                hide(true);
            }
        }
    }

    public void show(int height, boolean animate) {
        recyclerView.scrollToPosition(0);

        if (animator != null) {
            animator.cancel();
        }

        View view = getView();
        if (view == null) {
            return;
        }

        currentHeight = height;

        if (animate) {
            animator = Views.scale(view, height, SCALE_DURATION, null);
        } else {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.height = height;

            view.setLayoutParams(params);
        }

        isExpanded = false;
    }

    public void hide(boolean animate) {
        show(0, animate);
    }

    public boolean isShowing() {
        View view = getView();
        if (view == null) {
            return false;
        }

        return view.getLayoutParams().height > 0;
    }

    public interface CalendarEventsListener {

        void onClick(String id, View view);

        void onLongClick(String id, View view);

        void onChatClick(String id);

        void onFavoriteClick(String id);

        void onDeleteClick(String id);
    }
}
