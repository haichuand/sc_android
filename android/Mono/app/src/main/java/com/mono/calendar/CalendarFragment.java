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
import com.mono.model.Event;
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

/**
 * A fragment that displays the scrollable calendar and the view that lists all events from the
 * selected day. Communication between the calendar and events listing is being handled here as
 * well.
 *
 * @author Gary Ng
 */
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
            fragment.setSearchView(searchView, new SearchHandler(fragment, true, true));
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
                // Create Event Using the Selected Date
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
        // Handle Search View
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

    /**
     * Retrieve all color markers for the specific month to be displayed in the calendar.
     *
     * @param year The value of the year.
     * @param month The value of the month.
     * @return a map of colors for each day of the month.
     */
    @Override
    public Map<Integer, List<Integer>> getMonthColors(int year, int month) {
        long[] calendarIds = Settings.getInstance(getContext()).getCalendarsArray();
        return EventManager.getInstance(getContext()).getEventColorsByMonth(year, month, calendarIds);
    }

    /**
     * Primarily used to update the current day value of the calendar icon located at the dock
     * whenever a day change occurs.
     *
     * @param day The current day.
     */
    @Override
    public void onDayChange(int day) {
        Drawable drawable = createCalendarIcon(getContext(), String.valueOf(day));
        mainInterface.setDockLayoutDrawable(MainFragment.TAB_CALENDAR, drawable);
    }

    /**
     * Handle the action of selecting a day of the month in the calendar.
     *
     * @param year The selected year.
     * @param month The selected month.
     * @param day The selected day.
     * @param selected The value of whether it was selected or unselected.
     */
    @Override
    public void onCellClick(int year, int month, int day, boolean selected) {
        if (selected) {
            showEventsFragment(year, month, day);
        } else {
            eventsFragment.hide(true);
        }
    }

    /**
     * Handle the action of dropping an event into a day of the month in the calendar.
     *
     * @param id The value of the event ID.
     * @param year The targeted year.
     * @param month The targeted month.
     * @param day The targeted day.
     * @param action The type of action to perform.
     */
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
        // Determine New Event Time
        long startTime = dateTime.getMillis();
        long endTime = startTime + event.getDuration();
        // Perform Drop Action
        switch (action) {
            case ACTION_MOVE:
                event.startTime = startTime;
                event.endTime = endTime;

                if (event.source == Event.SOURCE_DATABASE) {
                    // Update Existing Event
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
                    // Create Event into the Database
                    eventManager.createEvent(
                        EventAction.ACTOR_SELF,
                        event.calendarId,
                        internalId,
                        event.externalId,
                        Event.TYPE_CALENDAR,
                        event.title,
                        event.description,
                        event.location,
                        event.color,
                        startTime,
                        endTime,
                        event.timeZone,
                        event.endTimeZone,
                        event.allDay,
                        event.attendees,
                        event.photos,
                        callback
                    );
                } else if (event.source == Event.SOURCE_PROVIDER) {
                    // Create Event into the Provider
                    eventManager.createSyncEvent(
                        EventAction.ACTOR_SELF,
                        event.calendarId,
                        event.title,
                        event.description,
                        event.location,
                        event.color,
                        startTime,
                        endTime,
                        event.timeZone,
                        event.endTimeZone,
                        event.allDay,
                        event.attendees,
                        event.photos,
                        callback
                    );
                }
                break;
        }
    }

    /**
     * Handle view state changes of the events listing.
     *
     * @param state The current view state.
     */
    @Override
    public void onStateChange(int state) {
        switch (state) {
            case CalendarEventsFragment.STATE_NONE:
//                calendarView.removeCurrentSelected();
                break;
        }
    }

    /**
     * Handle the action of performing a click on an event from the events listing.
     *
     * @param id The value of the event ID.
     * @param view The value of the view.
     */
    @Override
    public void onClick(String id, View view) {
        Event event = eventManager.getEvent(id, false);
        if (event != null) {
            mainInterface.showEventDetails(event);
        }
    }

    /**
     * Handle the action of performing a long click on an event from the events listing to
     * trigger drag and drop action.
     *
     * @param id The value of the event ID.
     * @param view The value of the view.
     */
    @Override
    public void onLongClick(String id, View view) {
        ClipData.Item item = new ClipData.Item(String.valueOf(id));
        ClipData data = new ClipData(EVENT_ITEM_LABEL, new String[]{
            ClipDescription.MIMETYPE_TEXT_PLAIN
        }, item);

        View.DragShadowBuilder builder = new View.DragShadowBuilder(view);
        view.startDrag(data, builder, null, 0);
    }

    /**
     * Handle the action of clicking on the chat option.
     *
     * @param id The value of the event ID.
     */
    @Override
    public void onChatClick(String id) {
        mainInterface.showChat(id);
    }

    /**
     * Handle the action of clicking on the favorite option.
     *
     * @param id The value of the event ID.
     */
    @Override
    public void onFavoriteClick(String id) {

    }

    /**
     * Handle the action of clicking on the deleting an event option.
     *
     * @param id
     */
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

    /**
     * Handle all event changes being reported by the Event Manager.
     *
     * @param data The event action data.
     */
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
                // Special Handling of All Day Events
                if (!event.allDay) {
                    startDate = new LocalDate(event.startTime);
                    endDate = new LocalDate(event.endTime);
                } else {
                    startDate = new LocalDate(event.startTime, DateTimeZone.UTC);
                    endDate = new LocalDate(event.endTime - 1, DateTimeZone.UTC);
                }
                // Handle Multi-Day Events
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

    /**
     * Display events that occur on the selected day.
     *
     * @param year The value of the year.
     * @param month The value of the month.
     * @param day The value of the day.
     */
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

    /**
     * Check calendar related settings such as the first day of the week and whether to display
     * week numbers in the calendar.
     */
    public void checkSettings() {
        Settings settings = Settings.getInstance(getContext());
        calendarView.setFirstDayOfWeek(settings.getCalendarWeekStart());
        calendarView.showWeekNumbers(settings.getCalendarWeekNumber());
    }

    /**
     * Construct a dynamic calendar icon displaying the day number.
     *
     * @param context The value of the context.
     * @param value The value to be displayed within the icon.
     * @return a drawable of the calendar icon.
     */
    public static Drawable createCalendarIcon(Context context, String value) {
        Drawable background = context.getDrawable(R.drawable.ic_calendar);
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
