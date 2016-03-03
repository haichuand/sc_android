package com.mono.calendar;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.mono.EventManager;
import com.mono.EventManager.EventAction;
import com.mono.EventManager.EventBroadcastListener;
import com.mono.MainInterface;
import com.mono.R;
import com.mono.calendar.CalendarEventsFragment.CalendarEventsListener;
import com.mono.calendar.CalendarView.CalendarListener;
import com.mono.db.DatabaseHelper;
import com.mono.db.dao.EventDataSource;
import com.mono.model.Event;
import com.mono.util.Colors;
import com.mono.util.Constants;
import com.mono.util.SimpleTabLayout.TabPagerCallback;

import java.util.Calendar;

public class CalendarFragment extends Fragment implements CalendarListener, CalendarEventsListener,
        EventBroadcastListener, TabPagerCallback {

    private static final String[] ACTIONS = {"Move", "Copy", "Cancel"};
    private static final int ACTION_MOVE = 0;
    private static final int ACTION_COPY = 1;

    public static final String EVENT_ITEM_LABEL = "event_item";

    private MainInterface mainInterface;
    private EventManager eventManager;

    private CalendarView calendarView;
    private CalendarEventsFragment eventsFragment;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        eventManager = EventManager.getInstance(context);

        if (context instanceof MainInterface) {
            mainInterface = (MainInterface) context;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        eventManager.addEventBroadcastListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        calendarView = (CalendarView) view.findViewById(R.id.calendar);
        calendarView.setListener(this);
        calendarView.setOnCellDropActions(ACTIONS);

        String tag = getString(R.string.fragment_calendar_events);

        FragmentManager manager = getChildFragmentManager();
        eventsFragment = new CalendarEventsFragment();

        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(R.id.events, eventsFragment, tag);
        transaction.commit();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        calendarView.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        eventManager.removeEventBroadcastListener(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.calendar, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_add:
                add();
                return true;
            case R.id.action_today:
                calendarView.today();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCellClick(int year, int month, int day, boolean selected) {
        EventDataSource dataSource =
            DatabaseHelper.getDataSource(getContext(), EventDataSource.class);
        long[] eventIds = dataSource.getEventIdsByDay(year, month, day);

        if (eventIds != null) {
            eventsFragment.clear(true);

            for (long id : eventIds) {
                Event event = eventManager.getEvent(id, false);
                eventsFragment.insert(0, event, true);
            }
        }

        View view = getView();

        if (view != null && selected && eventIds != null) {
            int height = calendarView.getPageHeight(year, month);
            eventsFragment.show(view.getMeasuredHeight() - height, true);
        } else {
            eventsFragment.hide(true);
        }
    }

    @Override
    public void onCellDrop(long id, final int year, final int month, final int day, int action) {
        Event event = eventManager.getEvent(id, false);
        if (event == null) {
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(event.startTime);

        int eventYear = calendar.get(Calendar.YEAR);
        int eventMonth = calendar.get(Calendar.MONTH);
        int eventDay = calendar.get(Calendar.DAY_OF_MONTH);

        if (eventYear == year && eventMonth == month && eventDay == day) {
            return;
        }

        calendar.set(year, month, day);
        long startTime = calendar.getTimeInMillis();

        calendar.setTimeInMillis(event.endTime);
        calendar.set(year, month, day);
        long endTime = calendar.getTimeInMillis();

        switch (action) {
            case ACTION_MOVE:
                eventManager.updateEventTime(EventAction.ACTOR_SELF, id, startTime, endTime,
                    new EventManager.EventActionCallback() {
                        @Override
                        public void onEventAction(EventAction data) {
                            if (data.getStatus() == EventAction.STATUS_OK) {
                                calendarView.onCellClick(year, month, day);
                                Toast.makeText(getContext(), "Moved Event",
                                    Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                );
                break;
            case ACTION_COPY:
                eventManager.createEvent(
                    EventAction.ACTOR_SELF,
                    (int) (Math.random() * -1000),
                    event.title,
                    event.description,
                    event.location != null ? event.location.name : null,
                    event.color,
                    startTime,
                    endTime,
                    Event.TYPE_CALENDAR,
                    new EventManager.EventActionCallback() {
                        @Override
                        public void onEventAction(EventAction data) {
                            if (data.getStatus() == EventAction.STATUS_OK) {
                                calendarView.onCellClick(year, month, day);
                                Toast.makeText(getContext(), "Copied Event",
                                    Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                );
                break;
        }
    }

    @Override
    public void onClick(long id) {

    }

    @Override
    public void onLongClick(long id, View view) {
        ClipData.Item item = new ClipData.Item(String.valueOf(id));
        ClipData data = new ClipData(EVENT_ITEM_LABEL, new String[]{
            ClipDescription.MIMETYPE_TEXT_PLAIN
        }, item);

        View.DragShadowBuilder builder = new View.DragShadowBuilder(view);
        view.startDrag(data, builder, null, 0);
    }

    @Override
    public void onEventBroadcast(EventAction data) {
        if (data.getStatus() == EventAction.STATUS_OK) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(data.getEvent().startTime);

            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);

            calendarView.refresh(year, month);
        }
    }

    @Override
    public void onPageSelected() {

    }

    @Override
    public ViewPager getTabLayoutViewPager() {
        return null;
    }

    @Override
    public ActionButton getActionButton() {
        return null;
    }

    public void add() {
        CalendarView.Date date = calendarView.getCurrentSelected();
        if (date == null) {
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(date.year, date.month, date.day);

        String[] locations = {"San Francisco", "San Jose", "San Leandro"};
        int[] colors = {Colors.BROWN, Colors.BROWN_LIGHT, Colors.LAVENDAR};

        eventManager.createEvent(
            EventAction.ACTOR_SELF,
            (int) (Math.random() * -1000),
            "Title " + (int) (Math.random() * 100),
            "Description " + (int) (Math.random() * 100),
            locations[(int) (Math.random() * locations.length) % locations.length],
            colors[(int) (Math.random() * colors.length) % colors.length],
            calendar.getTimeInMillis(),
            calendar.getTimeInMillis() + (int) (Math.random() * 23) * Constants.HOUR_MS,
            Event.TYPE_CALENDAR,
            null
        );
    }
}
