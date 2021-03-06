package com.mono.calendar;

import android.animation.Animator;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnItemTouchListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.mono.R;
import com.mono.calendar.CalendarEventsAdapter.CalendarEventsItem;
import com.mono.chat.ConversationManager;
import com.mono.model.Event;
import com.mono.util.Colors;
import com.mono.util.OnBackPressedListener;
import com.mono.util.Pixels;
import com.mono.util.SimpleDataSource;
import com.mono.util.SimpleLinearLayoutManager;
import com.mono.util.SimpleSlideView.SimpleSlideViewListener;
import com.mono.util.Views;

import org.joda.time.DateTime;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
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
public class CalendarEventsFragment extends Fragment implements OnBackPressedListener,
        OnItemTouchListener, SimpleDataSource<CalendarEventsItem>, SimpleSlideViewListener {

    public static final int STATE_NONE = 0;
    public static final int STATE_HALF = 1;
    public static final int STATE_FULL = 2;

    private static final int MIN_DELTA_Y = 30;
    private static final long SCALE_DURATION = 300;

    private static final SimpleDateFormat DATE_FORMAT;
    private static final SimpleDateFormat TIME_FORMAT;

    private CalendarEventsListener listener;

    private RecyclerView recyclerView;
    private SimpleLinearLayoutManager layoutManager;
    private CalendarEventsAdapter adapter;

    private final Map<String, CalendarEventsItem> items = new HashMap<>();
    private final List<Event> events = new ArrayList<>();

    private long currentTime;

    private Animator animator;

    private int maxHeight;
    private int halfHeight;

    private GestureDetectorCompat detector;
    private int state;

    static {
        DATE_FORMAT = new SimpleDateFormat("M/d/yy", Locale.getDefault());
        TIME_FORMAT = new SimpleDateFormat("h:mm a", Locale.getDefault());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Fragment fragment = getParentFragment();
        if (fragment != null && fragment instanceof CalendarEventsListener) {
            listener = (CalendarEventsListener) fragment;
        }

        maxHeight = Math.round(Pixels.Display.getHeight(getContext()) * 0.45f);
        halfHeight = maxHeight;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar_events, container, false);
        detector = new GestureDetectorCompat(getContext(), new GestureListener());

        recyclerView = (RecyclerView) view.findViewById(R.id.list);
        recyclerView.setLayoutManager(layoutManager = new SimpleLinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter = new CalendarEventsAdapter(this));
        recyclerView.addOnItemTouchListener(this);

        adapter.setDataSource(this);

        return view;
    }

    /**
     * Handle the action of when the back button is pushed. This list will go through multiple
     * stages of visibility before fully collapsed.
     *
     * @return the value of whether the action has been consumed.
     */
    @Override
    public boolean onBackPressed() {
        if (state == STATE_FULL) {
            show(STATE_HALF, true, true);
            return true;
        } else if (state == STATE_HALF) {
            hide(true);
            return true;
        }

        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent event) {
        if (layoutManager.isScrollEnabled()) {
            return detector.onTouchEvent(event);
        }

        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView recyclerView, MotionEvent event) {

    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }

    /**
     * Retrieve events as items to be displayed by the adapter. Special case events such as one
     * with photos or if the event was used to start a chat will show an icon.
     *
     * @param position The position of the event.
     * @return an item to display event information.
     */
    @Override
    public CalendarEventsItem getItem(int position) {
        CalendarEventsItem item;

        Event event = events.get(position);
        String id = event.id;

        if (items.containsKey(id)) {
            item = items.get(id);
        } else {
            item = new CalendarEventsItem(id);

            item.type = CalendarEventsItem.TYPE_EVENT;
            item.iconResId = R.drawable.circle;
            item.iconColor = event.color;

            if (event.title != null && !event.title.isEmpty()) {
                item.title = event.title;
            } else {
                item.title = "(" + getString(R.string.no_subject) + ")";
            }

            item.description = event.description;
            // Check Event Photos
            item.hasPhotos = event.hasPhotos();
            // Check Event Chat
            ConversationManager manager = ConversationManager.getInstance(getContext());
            item.hasChat = !manager.getConversations(id).isEmpty();
            // Date Display
            TimeZone timeZone = TimeZone.getDefault();
            DATE_FORMAT.setTimeZone(timeZone);
            TIME_FORMAT.setTimeZone(timeZone);

            String currentDate = DATE_FORMAT.format(currentTime);
            String startDate, endDate;

            if (!event.allDay) {
                startDate = DATE_FORMAT.format(event.startTime);
                endDate = DATE_FORMAT.format(event.endTime);

                if (startDate.equals(currentDate)) {
                    item.startTime = TIME_FORMAT.format(event.startTime);
                } else {
                    item.startTime = startDate;
                }

                if (endDate.equals(currentDate)) {
                    item.endTime = TIME_FORMAT.format(event.endTime);
                } else {
                    item.endTime = endDate;
                }
            } else {
                DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));

                startDate  = DATE_FORMAT.format(event.startTime);
                endDate = DATE_FORMAT.format(event.endTime - 1);

                item.startTime = startDate;

                if (!startDate.equals(endDate)) {
                    item.endTime = endDate;
                }
            }
            // Date Display Color
            int colorId;

            if (startDate.equals(currentDate)) {
                colorId = R.color.gray_dark;
            } else {
                colorId = R.color.gray_light_3;
            }
            item.startTimeColor = Colors.getColor(getContext(), colorId);

            if (endDate.equals(startDate) || !endDate.equals(currentDate)) {
                colorId = R.color.gray_light_3;
            } else {
                colorId =  R.color.gray_dark;
            }
            item.endTimeColor = Colors.getColor(getContext(), colorId);

            items.put(id, item);
        }

        return item;
    }

    /**
     * Retrieve the number of events to be used by the adapter.
     *
     * @return the number of events.
     */
    @Override
    public int getCount() {
        return events.size();
    }

    /**
     * Handle the action of clicking an event and notify any listeners.
     *
     * @param view The view of the event.
     */
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

    /**
     * Handle the action of long clicking an event and notify any listeners.
     *
     * @param view The view of the event.
     * @return the value of whether the action has been consumed.
     */
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

    /**
     * Handle the action of clicking on a hidden option on the left side of the event.
     *
     * @param view The view of the event.
     * @param index The index of the action.
     */
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

    /**
     * Handle the action of clicking on a hidden option on the right side of the event.
     *
     * @param view The view of the event.
     * @param index The index of the action.
     */
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

    /**
     * Used to disable any vertical scrolling if event sliding gestures are active.
     *
     * @param view The view of the event.
     * @param state The value of the state.
     */
    @Override
    public void onGesture(View view, boolean state) {
        layoutManager.setScrollEnabled(state);
    }

    /**
     * Initialize the list with events of the specific day.
     *
     * @param year The value of the year.
     * @param month The value of the month;
     * @param day The value of the day;
     * @param events The list of events.
     */
    public void setEvents(int year, int month, int day, List<Event> events) {
        currentTime = new DateTime(year, month + 1, day, 0, 0).getMillis();

        this.items.clear();
        this.events.clear();
        this.events.addAll(events);

        Collections.sort(this.events, new Comparator<Event>() {
            @Override
            public int compare(Event e1, Event e2) {
                return Long.compare(e1.startTime, e2.startTime);
            }
        });

        adapter.notifyDataSetChanged();
    }

    /**
     * Handle the insertion of an event to be displayed.
     *
     * @param items Events to be inserted.
     * @param scrollToPosition Scroll to the event after insertion.
     * @param notify Notify the adapter.
     */
    public void insert(List<Event> items, int scrollToPosition, boolean notify) {
        Event scrollToEvent = scrollToPosition >= 0 ? items.get(scrollToPosition) : null;

        Iterator<Event> iterator = items.iterator();
        while (iterator.hasNext()) {
            if (events.contains(iterator.next())) {
                iterator.remove();
            }
        }

        if (items.isEmpty()) {
            return;
        }

        events.addAll(items);

        Collections.sort(events, new Comparator<Event>() {
            @Override
            public int compare(Event e1, Event e2) {
                return Long.compare(e1.startTime, e2.startTime);
            }
        });

        if (notify) {
            for (Event event : items) {
                int index = events.indexOf(event);
                adapter.notifyItemInserted(index);
            }

            if (scrollToEvent != null) {
                scrollToPosition = events.indexOf(scrollToEvent);
                if (scrollToPosition >= 0) {
                    recyclerView.smoothScrollToPosition(scrollToPosition);
                }
            }
        }
    }

    /**
     * Handle the refresh of an event if it was updated.
     *
     * @param items Events to be refreshed.
     * @param scrollToPosition Scroll to the event after refresh.
     * @param notify Notify the adapter.
     */
    public void refresh(List<Event> items, int scrollToPosition, boolean notify) {
        Event scrollToEvent = scrollToPosition >= 0 ? items.get(scrollToPosition) : null;

        for (Event event : items) {
            int index;

            if (event.oldId != null) {
                Event tempEvent = new Event(event);
                tempEvent.id = tempEvent.oldId;
                index = events.indexOf(tempEvent);
            } else {
                index = events.indexOf(event);
            }

            if (index < 0) {
                continue;
            }

            events.remove(index);
            this.items.remove(event.oldId != null ? event.oldId : event.id);

            events.add(event);
        }

        Collections.sort(events, new Comparator<Event>() {
            @Override
            public int compare(Event e1, Event e2) {
                return Long.compare(e1.startTime, e2.startTime);
            }
        });

        if (notify) {
            for (Event event : items) {
                int index = events.indexOf(event);
                adapter.notifyItemChanged(index);

                int currentIndex = events.indexOf(event);
                if (currentIndex != index) {
                    adapter.notifyItemMoved(index, currentIndex);
                }
            }

            if (scrollToEvent != null) {
                scrollToPosition = events.indexOf(scrollToEvent);
                if (scrollToPosition >= 0) {
                    recyclerView.smoothScrollToPosition(scrollToPosition);
                }
            }
        }
    }

    /**
     * Handle the removal of an event.
     *
     * @param items Events to be removed.
     * @param notify Notify the adapter.
     */
    public void remove(List<Event> items, boolean notify) {
        for (Event event : items) {
            int index = events.indexOf(event);
            if (index < 0) {
                continue;
            }

            events.remove(index);

            if (notify) {
                adapter.notifyItemRemoved(index);
            }
        }

        if (events.isEmpty()) {
            hide(true);
        }
    }

    /**
     * Set the height of this list when it's not fully expanded.
     *
     * @param height The value of the height.
     */
    public void setHalfHeight(int height) {
        halfHeight = height;
    }

    /**
     * Set the list to be visible.
     *
     * @param state The state of how expanded the list to be displayed.
     * @param scrollToTop The value of whether to reset the scroll to the top.
     * @param animate The value of whether to animate the expansion.
     */
    public void show(int state, boolean scrollToTop, boolean animate) {
        View view = getView();
        if (view == null) {
            return;
        }

        if (scrollToTop) {
            recyclerView.scrollToPosition(0);
        }

        if (animator != null) {
            animator.cancel();
            animator = null;
        }

        int height = 0;

        switch (state) {
            case STATE_NONE:
                height = 0;
                break;
            case STATE_HALF:
                height = halfHeight;
                break;
            case STATE_FULL:
                height = maxHeight;
                break;
        }

        if (animate) {
            animator = Views.scale(view, height, SCALE_DURATION, null);
        } else {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.height = height;
            view.setLayoutParams(params);
        }

        if (state != this.state) {
            if (listener != null) {
                listener.onStateChange(state);
            }
        }

        this.state = state;
    }

    /**
     * Set the list to be hidden.
     *
     * @param animate The value of whether to animate the collapse.
     */
    public void hide(boolean animate) {
        show(STATE_NONE, true, animate);
    }

    /**
     * Determine the visibility state using the current height of the list.
     *
     * @return the state of the visibility.
     */
    public int getState() {
        int state = STATE_NONE;

        View view = getView();
        if (view != null) {
            int height = view.getMeasuredHeight();

            if (height < halfHeight) {
                state = STATE_NONE;
            } else if (height < maxHeight) {
                state = STATE_HALF;
            } else {
                state = STATE_FULL;
            }
        }

        return state;
    }

    private class GestureListener extends SimpleOnGestureListener {

        private boolean canScrollUp;

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            float deltaY = e2.getY() - e1.getY();

            if (Math.abs(deltaY) < MIN_DELTA_Y) {
                return false;
            }

            int nextState = getState();

            if (deltaY < 0) {
                if (state == STATE_NONE) {
                    nextState = STATE_HALF;
                } else if (state == STATE_HALF) {
                    nextState = STATE_FULL;
                }
            } else if (deltaY > 0 && !canScrollUp) {
                if (state == STATE_FULL)  {
                    nextState = STATE_HALF;
                } else if (state == STATE_HALF) {
                    nextState = STATE_NONE;
                }
            }

            if (nextState == -1) {
                return false;
            }

            if (nextState != state) {
                show(nextState, false, true);
            }

            return animator != null && animator.isRunning();
        }

        @Override
        public boolean onDown(MotionEvent event) {
            canScrollUp = recyclerView.canScrollVertically(-1);
            return false;
        }
    }

    public interface CalendarEventsListener {

        void onStateChange(int state);

        void onClick(String id, View view);

        void onLongClick(String id, View view);

        void onChatClick(String id);

        void onFavoriteClick(String id);

        void onDeleteClick(String id);
    }
}
