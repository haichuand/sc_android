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
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
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
import com.mono.calendar.CalendarEventsFragment.CalendarEventsListener;
import com.mono.calendar.CalendarView.CalendarListener;
import com.mono.db.DatabaseHelper;
import com.mono.db.dao.EventDataSource;
import com.mono.model.Event;
import com.mono.settings.Settings;
import com.mono.util.OnBackPressedListener;
import com.mono.util.Pixels;
import com.mono.util.SimpleTabLayout.Scrollable;
import com.mono.util.SimpleTabLayout.TabPagerCallback;

import java.util.Calendar;
import java.util.TimeZone;

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
        SearchView searchView = (SearchView) item.getActionView();
        searchView.setOnQueryTextListener(new OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
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
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);

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

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(event.startTime);
        calendar.setTimeZone(TimeZone.getTimeZone(event.timeZone));

        int eventYear = calendar.get(Calendar.YEAR);
        int eventMonth = calendar.get(Calendar.MONTH);
        int eventDay = calendar.get(Calendar.DAY_OF_MONTH);

        if (eventYear == year && eventMonth == month && eventDay == day) {
            return;
        }

        calendar.set(year, month, day);
        long startTime = calendar.getTimeInMillis();
        long endTime = startTime + event.getDuration();

        switch (action) {
            case ACTION_MOVE:
                event.calendarId = System.currentTimeMillis();
                event.startTime = startTime;
                event.endTime = endTime;

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
                break;
            case ACTION_COPY:
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
                    new EventManager.EventActionCallback() {
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
                    }
                );
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

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(event.startTime);
                calendar.setTimeZone(TimeZone.getTimeZone(event.timeZone));

                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int day = calendar.get(Calendar.DAY_OF_MONTH);

                calendarView.refresh(year, month);

                CalendarView.Date date = calendarView.getCurrentSelected();
                if (date != null && year == date.year && month == date.month && day == date.day) {
                    switch (data.getAction()) {
                        case EventAction.ACTION_CREATE:
                            if (eventsFragment.getState() != CalendarEventsFragment.STATE_NONE) {
                                eventsFragment.insert(0, event, true);
                            } else {
                                showEventsFragment(year, month, day);
                            }
                            break;
                        case EventAction.ACTION_UPDATE:
                            eventsFragment.refresh(event, true);
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
