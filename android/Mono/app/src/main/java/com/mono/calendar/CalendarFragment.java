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
import com.mono.util.OnBackPressedListener;
import com.mono.util.SimpleTabLayout.TabPagerCallback;

import java.util.Calendar;

public class CalendarFragment extends Fragment implements OnBackPressedListener, CalendarListener,
        CalendarEventsListener, EventBroadcastListener, TabPagerCallback {

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
                Event event = new Event();
                event.type = Event.TYPE_CALENDAR;

                CalendarView.Date date = calendarView.getCurrentSelected();
                if (date != null) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(date.year, date.month, date.day);
                    event.startTime = calendar.getTimeInMillis();
                }

                mainInterface.showEventDetails(event);
                return true;
            case R.id.action_today:
                calendarView.today();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onBackPressed() {
        if (eventsFragment.onBackPressed()) {
            return true;
        }

        return false;
    }

    @Override
    public void onCellClick(int year, int month, int day, boolean selected) {
        if (selected) {
            showEventsFragment(year, month, day);
        } else {
            eventsFragment.hide(true);
        }
    }

    @Override
    public void onCellDrop(String id, final int year, final int month, final int day, int action) {
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
                event.externalId = System.currentTimeMillis();

                eventManager.createEvent(
                    EventAction.ACTOR_SELF,
                    event.externalId,
                    Event.TYPE_CALENDAR,
                    event.title,
                    event.description,
                    event.location != null ? event.location.name : null,
                    event.color,
                    startTime,
                    endTime,
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
    public void onClick(String id, View view) {
        Event event = eventManager.getEvent(id, false);
        mainInterface.showEventDetails(event);
    }

    @Override
    public void onLongClick(String id, View view) {
        ClipData.Item item = new ClipData.Item(String.valueOf(id));
        ClipData data = new ClipData(EVENT_ITEM_LABEL, new String[]{
            ClipDescription.MIMETYPE_TEXT_PLAIN
        }, item);

        View.DragShadowBuilder builder = new View.DragShadowBuilder(view);
        view.startDrag(data, builder, null, 0);
    }

    @Override
    public void onChatClick(String id) {
        mainInterface.showChat(id);
    }

    @Override
    public void onFavoriteClick(String id) {

    }

    @Override
    public void onDeleteClick(String id) {
        eventManager.removeEvent(EventAction.ACTOR_SELF, id,
            new EventManager.EventActionCallback() {
                @Override
                public void onEventAction(EventAction data) {
                    if (data.getStatus() == EventAction.STATUS_OK) {
                        Toast.makeText(getContext(), "Deleted Event", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );
    }

    @Override
    public void onEventBroadcast(EventAction data) {
        if (data.getStatus() != EventAction.STATUS_OK) {
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(data.getEvent().startTime);

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        calendarView.refresh(year, month);

        CalendarView.Date date = calendarView.getCurrentSelected();
        if (date != null && year == date.year && month == date.month && day == date.day) {
            switch (data.getAction()) {
                case EventAction.ACTION_CREATE:
                    if (eventsFragment.isShowing()) {
                        eventsFragment.insert(0, data.getEvent(), true);
                    } else {
                        showEventsFragment(year, month, day);
                    }
                    break;
                case EventAction.ACTION_UPDATE:
                    eventsFragment.refresh(data.getEvent(), true);
                    break;
                case EventAction.ACTION_REMOVE:
                    eventsFragment.remove(data.getEvent(), true);
                    break;
            }
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

    public void showEventsFragment(int year, int month, int day) {
        View view = getView();
        if (view == null) {
            return;
        }

        EventDataSource dataSource =
            DatabaseHelper.getDataSource(getContext(), EventDataSource.class);
        String[] eventIds = dataSource.getEventIdsByDay(year, month, day);

        if (eventIds != null) {
            eventsFragment.clear(true);

            for (String id : eventIds) {
                Event event = eventManager.getEvent(id, false);
                eventsFragment.insert(0, event, true);
            }

            int height = calendarView.getPageHeight(year, month);
            eventsFragment.show(view.getMeasuredHeight() - height, true);
        } else {
            eventsFragment.hide(true);
        }
    }
}
