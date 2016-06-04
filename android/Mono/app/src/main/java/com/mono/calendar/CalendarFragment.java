package com.mono.calendar;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.mono.EventManager;
import com.mono.EventManager.EventAction;
import com.mono.EventManager.EventBroadcastListener;
import com.mono.MainFragment;
import com.mono.MainInterface;
import com.mono.R;
import com.mono.search.SearchFragment;
import com.mono.calendar.CalendarEventsFragment.CalendarEventsListener;
import com.mono.calendar.CalendarView.CalendarListener;
import com.mono.model.Calendar;
import com.mono.model.Event;
import com.mono.provider.CalendarProvider;
import com.mono.search.SearchHandler;
import com.mono.settings.Settings;
import com.mono.util.Common;
import com.mono.util.OnBackPressedListener;
import com.mono.util.Pixels;
import com.mono.util.SimpleTabLayout.Scrollable;
import com.mono.util.SimpleTabLayout.TabPagerCallback;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;

import java.util.List;
import java.util.Map;

public class CalendarFragment extends Fragment implements OnBackPressedListener, CalendarListener,
        CalendarEventsListener, EventBroadcastListener, TabPagerCallback, Scrollable {

    private static final String[] ACTIONS = {"Move", "Copy", "Cancel"};
    private static final int ACTION_MOVE = 0;
    private static final int ACTION_COPY = 1;

    public static final String EVENT_ITEM_LABEL = "event_item";

    private MainInterface mainInterface;
    private EventManager eventManager;

    private CalendarView calendarView;
    private CalendarEventsFragment eventsFragment;

    private SearchView searchView;

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

        checkSettings();

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
        checkSettings();
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

        MenuItem item = menu.findItem(R.id.action_search);
        searchView = (SearchView) MenuItemCompat.getActionView(item);

        FragmentManager manager = getActivity().getSupportFragmentManager();
        SearchFragment fragment = (SearchFragment) manager.findFragmentById(R.id.search_fragment);
        if (fragment != null) {
            fragment.setSearchView(searchView, new SearchHandler(fragment));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_search:
                return true;
            case R.id.action_add:
                Event event = new Event();
                event.type = Event.TYPE_CALENDAR;

                List<Calendar> calendars =
                    CalendarProvider.getInstance(getContext()).getCalendars();
                for (Calendar calendar : calendars) {
                    if (calendar.primary) {
                        event.calendarId = calendar.id;
                        break;
                    }
                }

                LocalDate date = calendarView.getCurrentSelected();
                if (date != null) {
                    DateTime dateTime = new DateTime(date.getYear(), date.getMonthOfYear(),
                        date.getDayOfMonth(), new DateTime().getHourOfDay(), 0, 0, 0);
                    event.startTime = dateTime.getMillis();
                }

                mainInterface.showEventDetails(event);
                return true;
            case R.id.action_today:
                calendarView.today();
                return true;
            case R.id.action_refresh:
                mainInterface.requestSync(true);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onBackPressed() {
        if (searchView != null && !searchView.isIconified()) {
            searchView.setIconified(true);
            searchView.onActionViewCollapsed();

            FragmentManager manager = getActivity().getSupportFragmentManager();
            SearchFragment fragment = (SearchFragment) manager.findFragmentById(R.id.search_fragment);
            if (fragment != null) {
                fragment.setVisible(false);
            }
            return true;
        }

        return eventsFragment.onBackPressed();
    }

    @Override
    public Map<Integer, List<Integer>> getMonthColors(int year, int month) {
        long[] calendarIds = Settings.getInstance(getContext()).getCalendarsArray();
        return EventManager.getInstance(getContext()).getEventColorsByMonth(year, month, calendarIds);
    }

    @Override
    public void onDayChange(int day) {
        Drawable drawable = createCalendarIcon(getContext(), String.valueOf(day));
        mainInterface.setDockLayoutDrawable(MainFragment.TAB_CALENDAR, drawable);
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

        DateTime dateTime = new DateTime(event.startTime, DateTimeZone.forID(event.timeZone));
        LocalDate startDate = dateTime.toLocalDate();
        dateTime = dateTime.withDate(year, month + 1, day);

        if (startDate.isEqual(dateTime.toLocalDate())) {
            return;
        }

        long startTime = dateTime.getMillis();
        long endTime = startTime + event.getDuration();

        switch (action) {
            case ACTION_MOVE:
                event.startTime = startTime;
                event.endTime = endTime;

                if (event.source == Event.SOURCE_DATABASE) {
                    eventManager.updateEvent(EventAction.ACTOR_SELF, id, event,
                        new EventManager.EventActionCallback() {
                            @Override
                            public void onEventAction(EventAction data) {
                                if (data.getStatus() == EventAction.STATUS_OK) {
                                    calendarView.onCellClick(year, month, day);
                                    mainInterface.showSnackBar(R.string.event_action_move,
                                        R.string.undo, 0, new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {

                                            }
                                        }
                                    );
                                }
                            }
                        }
                    );
                } else if (event.source == Event.SOURCE_PROVIDER) {

                }
                break;
            case ACTION_COPY:
                EventManager.EventActionCallback callback = new EventManager.EventActionCallback() {
                    @Override
                    public void onEventAction(EventAction data) {
                        if (data.getStatus() == EventAction.STATUS_OK) {
                            calendarView.onCellClick(year, month, day);
                            mainInterface.showSnackBar(R.string.event_action_copy,
                                R.string.undo, 0, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {

                                    }
                                }
                            );
                        }
                    }
                };

                if (event.source == Event.SOURCE_DATABASE) {
                    long internalId = System.currentTimeMillis();

                    eventManager.createEvent(
                        EventAction.ACTOR_SELF,
                        event.calendarId,
                        internalId,
                        event.externalId,
                        Event.TYPE_CALENDAR,
                        event.title,
                        event.description,
                        event.location != null ? event.location.name : null,
                        event.color,
                        startTime,
                        endTime,
                        event.timeZone,
                        event.endTimeZone,
                        event.allDay,
                        callback
                    );
                } else if (event.source == Event.SOURCE_PROVIDER) {
                    eventManager.createSyncEvent(
                        EventAction.ACTOR_SELF,
                        event.calendarId,
                        event.title,
                        event.description,
                        event.location != null ? event.location.name : null,
                        event.color,
                        startTime,
                        endTime,
                        event.timeZone,
                        event.endTimeZone,
                        event.allDay,
                        callback
                    );
                }
                break;
        }
    }

    @Override
    public void onStateChange(int state) {
        switch (state) {
            case CalendarEventsFragment.STATE_NONE:
//                calendarView.removeCurrentSelected();
                break;
        }
    }

    @Override
    public void onClick(String id, View view) {
        Event event = eventManager.getEvent(id, false);
        if (event != null) {
            mainInterface.showEventDetails(event);
        }
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
    public void onDeleteClick(final String id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(),
            R.style.AppTheme_Dialog_Alert);
        builder.setMessage(R.string.confirm_event_delete);

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        eventManager.removeEvent(EventAction.ACTOR_SELF, id,
                            new EventManager.EventActionCallback() {
                                @Override
                                public void onEventAction(EventAction data) {
                                    if (data.getStatus() == EventAction.STATUS_OK) {
                                        mainInterface.showSnackBar(R.string.event_action_delete,
                                            R.string.undo, 0, new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {

                                                }
                                            }
                                        );
                                    }
                                }
                            }
                        );
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }

                dialog.dismiss();
            }
        };

        builder.setPositiveButton(R.string.yes, listener);
        builder.setNegativeButton(R.string.no, listener);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onEventBroadcast(final EventAction data) {
        if (data.getStatus() != EventAction.STATUS_OK) {
            return;
        }

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Event event = data.getEvent();

                LocalDate startDate, endDate;

                if (!event.allDay) {
                    startDate = new LocalDate(event.startTime);
                    endDate = new LocalDate(event.endTime);
                } else {
                    startDate = new LocalDate(event.startTime, DateTimeZone.UTC);
                    endDate = new LocalDate(event.endTime - 1, DateTimeZone.UTC);
                }

                LocalDate date = startDate;
                while (date.isBefore(endDate) || date.isEqual(endDate)) {
                    calendarView.refresh(date.getYear(), date.getMonthOfYear() - 1);
                    date = date.plusDays(1);
                }

                LocalDate selectedDate = calendarView.getCurrentSelected();

                if (selectedDate != null && Common.between(selectedDate, startDate, endDate)) {
                    boolean scrollTo = data.getActor() == EventAction.ACTOR_SELF;

                    switch (data.getAction()) {
                        case EventAction.ACTION_CREATE:
                            if (eventsFragment.getState() != CalendarEventsFragment.STATE_NONE) {
                                eventsFragment.insert(event, scrollTo, true);
                            } else {
                                showEventsFragment(startDate.getYear(),
                                    startDate.getMonthOfYear() - 1, startDate.getDayOfMonth());
                            }
                            break;
                        case EventAction.ACTION_UPDATE:
                            eventsFragment.refresh(event, scrollTo, true);
                            break;
                        case EventAction.ACTION_REMOVE:
                            eventsFragment.remove(event, true);
                            break;
                    }
                }
            }
        });
    }

    @Override
    public int getPageTitle() {
        return 0;
    }

    @Override
    public ViewPager getTabLayoutViewPager() {
        return null;
    }

    @Override
    public ActionButton getActionButton() {
        return null;
    }

    @Override
    public void onPageSelected() {

    }

    @Override
    public void scrollToTop() {
        calendarView.today();
    }

    public void showEventsFragment(int year, int month, int day) {
        View view = getView();
        if (view == null) {
            return;
        }

        long[] calendarIds = Settings.getInstance(getContext()).getCalendarsArray();
        List<Event> events = eventManager.getEvents(year, month, day, calendarIds);

        if (!events.isEmpty()) {
            eventsFragment.setEvents(year, month, day, events);

            int height = calendarView.getPageHeight(year, month);
            eventsFragment.setHalfHeight(view.getMeasuredHeight() - height);
            eventsFragment.show(CalendarEventsFragment.STATE_HALF, true, true);
        } else {
            eventsFragment.hide(true);
        }
    }

    public void checkSettings() {
        Settings settings = Settings.getInstance(getContext());
        calendarView.setFirstDayOfWeek(settings.getCalendarWeekStart());
        calendarView.showWeekNumbers(settings.getCalendarWeekNumber());
    }

    public static Drawable createCalendarIcon(Context context, String value) {
        Drawable background = context.getDrawable(R.drawable.ic_calendar_white);
        if (background == null) {
            return null;
        }

        int width = Pixels.pxFromDp(context, 24);
        int height = Pixels.pxFromDp(context, 24);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        background.draw(canvas);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(20f);
        paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        canvas.drawText(value, width / 2 * 0.98f, height / 2 * 1.4f, paint);

        Drawable text = new BitmapDrawable(context.getResources(), bitmap);
        return new LayerDrawable(new Drawable[]{background, text});
    }
}
