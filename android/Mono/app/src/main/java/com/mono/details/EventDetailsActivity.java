package com.mono.details;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;

import com.google.gson.Gson;
import com.mono.EventManager;
import com.mono.R;
import com.mono.SuperCalyPreferences;
import com.mono.model.Calendar;
import com.mono.model.Event;
import com.mono.model.Location;
import com.mono.provider.CalendarProvider;
import com.mono.util.GestureActivity;
import com.mono.util.TimeZoneHelper;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * This activity displays information of an event as well as allowing the user to create and
 * modify an existing event.
 *
 * @author Gary Ng
 */
public class EventDetailsActivity extends GestureActivity {

    public static final String EXTRA_CALENDAR = "calendar";
    public static final String EXTRA_EVENT = "event";

    public static final int REQUEST_PLACE_PICKER = 1;
    public static final int REQUEST_CONTACT_PICKER = 2;
    public static final int REQUEST_PHOTO_PICKER = 3;

    private static final SimpleDateFormat DATE_FORMAT;
    private static final SimpleDateFormat TIME_FORMAT;
    private static final SimpleDateFormat DATETIME_FORMAT;

    private TextView calendar;
    private EditText title;
    private ImageView colorPicker;
    private TextView startDate;
    private TextView startTime;
    private TextView endDate;
    private TextView endTime;
    private CheckBox allDay;
    private TextView timeZoneView;
    private LocationPanel locationPanel;
    private NotePanel notePanel;
    private GuestPanel guestPanel;
    private PhotoPanel photoPanel;

    private Event original;
    private Event event;

    private Calendar currentCalendar;
    private int color;
    SharedPreferences sharedPreferences;
    private  EventManager manager;
    private List<Event> events = new ArrayList<>();
    private HashMap<String, Location> LocationHashmap = new HashMap<String, Location>();

    static {
        DATE_FORMAT = new SimpleDateFormat("EEE, MMMM d, yyyy", Locale.getDefault());
        TIME_FORMAT = new SimpleDateFormat("h:mm a", Locale.getDefault());
        DATETIME_FORMAT = new SimpleDateFormat("EEE, MMMM d, yyyy h:mm a", Locale.getDefault());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        calendar = (TextView) findViewById(R.id.calendar);
        calendar.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showCalendarPicker();
            }
        });

        title = (EditText) findViewById(R.id.title);
        title.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString().trim();
                event.title = !value.isEmpty() ? value : null;
            }
        });

        colorPicker = (ImageView) findViewById(R.id.color_picker);
        colorPicker.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showColorPicker();
            }
        });

        startDate = (TextView) findViewById(R.id.start_date);
        startDate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                long time = getTime(startDate, startTime, event.timeZone);
                onDateClick(time, event.timeZone, new DateTimePickerCallback() {
                    @Override
                    public void onSet(Date date) {
                        setStartTime(date);
                    }
                });
            }
        });

        startTime = (TextView) findViewById(R.id.start_time);
        startTime.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                long time = getTime(startDate, startTime, event.timeZone);
                onTimeClick(time, event.timeZone, new DateTimePickerCallback() {
                    @Override
                    public void onSet(Date date) {
                        setStartTime(date);
                    }
                });
            }
        });

        endDate = (TextView) findViewById(R.id.end_date);
        endDate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                long time = getTime(endDate, endTime, event.getEndTimeZone());
                onDateClick(time, event.getEndTimeZone(), new DateTimePickerCallback() {
                    @Override
                    public void onSet(Date date) {
                        setEndTime(date);
                    }
                });
            }
        });

        endTime = (TextView) findViewById(R.id.end_time);
        endTime.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                long time = getTime(endDate, endTime, event.getEndTimeZone());
                onTimeClick(time, event.getEndTimeZone(), new DateTimePickerCallback() {
                    @Override
                    public void onSet(Date date) {
                        setEndTime(date);
                    }
                });
            }
        });

        allDay = (CheckBox) findViewById(R.id.all_day);
        allDay.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onAllDayChecked(isChecked);
            }
        });

        timeZoneView = (TextView) findViewById(R.id.timezone);
        timeZoneView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
//                onTimeZoneClick(event.timeZone);
            }
        });

        locationPanel = new LocationPanel(this);
        locationPanel.onCreate(savedInstanceState);

        notePanel = new NotePanel(this);
        notePanel.onCreate(savedInstanceState);

        guestPanel = new GuestPanel(this);
        guestPanel.onCreate(savedInstanceState);

        photoPanel = new PhotoPanel(this);
        photoPanel.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Event event = intent.getParcelableExtra(EXTRA_EVENT);
        Calendar calendar = intent.getParcelableExtra(EXTRA_CALENDAR);

        initialize(calendar, event);
    }
    @Override
    public boolean dispatchTouchEvent(final MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if ( locationPanel.location.isFocused()) {
                if(locationPanel.locationChanged) {
                    if (original.location != null) {
                        Rect outRect = new Rect();
                        v.getGlobalVisibleRect(outRect);
                        if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                            v.clearFocus();
                            AlertDialog.Builder builder = new AlertDialog.Builder(EventDetailsActivity.this);
                            builder.setMessage(R.string.verify_location_change_forAll);

                            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which) {
                                        case DialogInterface.BUTTON_POSITIVE:

                                            title.setText(locationPanel.location.getText().toString().trim());
                                            //get all events with same location
                                            manager = EventManager.getInstance(getApplicationContext());
                                            events = manager.getEvents(original.location.getAddress(), 365);
                                            Log.d("test", "address" + original.location.getAddress());
                                            LocationHashmap.put(locationPanel.location.getText().toString().trim(), original.location);
                                            //convert to string using gson
                                            Gson gson = new Gson();
                                            String hashMapString = gson.toJson(LocationHashmap);
                                            //save in shared prefs
                                            sharedPreferences = getSharedPreferences(SuperCalyPreferences.USER_DEFINED_LOCATION, MODE_PRIVATE);
                                            sharedPreferences.edit().putString(SuperCalyPreferences.USER_DEFINED_LOCATION, hashMapString).apply();

                                            //make changes to all the events
                                            String eventid = "";
                                            for (int i = 0; i < events.size(); i++) {
                                                eventid = events.get(i).id;
                                                events.get(i).location.name = locationPanel.location.getText().toString().trim();
                                                events.get(i).title = locationPanel.location.getText().toString().trim();
                                                manager.updateEvent(
                                                        EventManager.EventAction.ACTOR_SELF,
                                                        eventid,
                                                        events.get(i),
                                                        null
                                                );
                                            }

                                            break;

                                        case DialogInterface.BUTTON_NEGATIVE:
                                            break;
                                    }

                                    dialog.dismiss();
                                    locationPanel.locationChanged = false;
                                }
                            };

                            builder.setPositiveButton(R.string.yes, listener);
                            builder.setNegativeButton(R.string.no, listener);

                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                    }
                }
            }
        }
        return super.dispatchTouchEvent( event );
    }

    @Override
    public void onBackPressed() {
        if (event != null && !event.equals(original)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this,
                R.style.AppTheme_Dialog_Alert);
            builder.setMessage(R.string.confirm_event_save);

            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            Intent data = new Intent();
                            data.putExtra(EXTRA_EVENT, event);
                            setResult(RESULT_OK, data);
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            break;
                    }

                    dialog.dismiss();
                    close();
                }
            };

            builder.setPositiveButton(R.string.yes, listener);
            builder.setNegativeButton(R.string.no, listener);

            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.event_details, menu);

        if (event.id != null) {
            menu.findItem(R.id.action_delete).setVisible(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_delete:
                onDelete();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_PLACE_PICKER:
                locationPanel.handlePlacePicker(resultCode, data);
                break;
            case REQUEST_CONTACT_PICKER:
                guestPanel.handleContactPicker(resultCode, data);
                break;
            case REQUEST_PHOTO_PICKER:
                photoPanel.handlePhotoPicker(resultCode, data);
                break;
        }
    }

    public void initialize(Calendar calendar, Event original) {
        if (original == null) {
            original = new Event();
        }

        this.original = original;
        event = new Event(original);
        currentCalendar = calendar;

        this.calendar.setText(calendar.name);

        if (event.title != null) {
            title.setText(event.title);
        }

        color = event.color != 0 ? event.color : calendar.color;
        setColorPicker(color);

        if (event.timeZone == null) {
            event.timeZone = TimeZone.getDefault().getID();
        }

        TimeZone timeZone = TimeZone.getTimeZone(event.timeZone);
        DATE_FORMAT.setTimeZone(timeZone);
        TIME_FORMAT.setTimeZone(timeZone);

        DateTime dateTime = new DateTime(DateTimeZone.forTimeZone(timeZone));

        if (event.startTime > 0) {
            dateTime = dateTime.withMillis(event.startTime);
        } else {
            dateTime = dateTime.withTime(dateTime.getHourOfDay(), 0, 0, 0);
            event.startTime = dateTime.getMillis();
        }

        startDate.setText(DATE_FORMAT.format(dateTime.toDate()));
        startTime.setText(TIME_FORMAT.format(dateTime.toDate()));

        if (event.endTime > 0) {
            dateTime = dateTime.withMillis(event.endTime);
        } else {
            dateTime = dateTime.withTime(dateTime.getHourOfDay(), 0, 0, 0);
            dateTime = dateTime.plusHours(1);
            event.endTime = dateTime.getMillis();
        }

        endDate.setText(DATE_FORMAT.format(dateTime.toDate()));
        endTime.setText(TIME_FORMAT.format(dateTime.toDate()));

        timeZoneView.setText(TimeZoneHelper.getTimeZoneGMTName(timeZone, event.startTime));
        allDay.setChecked(event.allDay);

        locationPanel.setEvent(event);
        notePanel.setEvent(event);
        guestPanel.setEvent(event);
        photoPanel.setEvent(event);
    }

    public void close() {
        event = null;
        onBackPressed();
    }

    public void onDelete() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppTheme_Dialog_Alert);
        builder.setMessage(R.string.confirm_event_delete);

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        EventManager manager = EventManager.getInstance(EventDetailsActivity.this);
                        manager.removeEvent(EventManager.EventAction.ACTOR_SELF, event.id,
                            new EventManager.EventActionCallback() {
                                @Override
                                public void onEventAction(EventManager.EventAction data) {
                                    if (data.getStatus() == EventManager.EventAction.STATUS_OK) {
                                        finish();
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

    public void showCalendarPicker() {
        final List<Calendar> calendars = CalendarProvider.getInstance(this).getCalendars();
        final CharSequence[] items = new CharSequence[1 + calendars.size()];

        items[0] = getString(R.string.local_calendar);
        for (int i = 0; i < calendars.size(); i++) {
            items[1 + i] = calendars.get(i).name;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppTheme_Dialog_Alert);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                currentCalendar = which > 0 ? calendars.get(which - 1) : new Calendar(-1);
                event.calendarId = currentCalendar.id;

                if (event.color == 0) {
                    setColorPicker(currentCalendar.color);
                }

                calendar.setText(items[which]);
            }
        });
        builder.create().show();
    }

    public void showColorPicker() {
        int[] colors;
        if (event.color == 0) {
            colors = new int[]{currentCalendar.color};
        } else if (event.color != currentCalendar.color) {
            colors = new int[]{currentCalendar.color, event.color};
        } else {
            colors = new int[]{event.color};
        }

        ColorPickerDialog dialog = new ColorPickerDialog(
            this,
            colors,
            color,
            new ColorPickerDialog.OnColorSetListener() {
                @Override
                public void onColorSet(int color) {
                    setColorPicker(color);
                    event.color = color;
                }
            }
        );

        dialog.show();
    }

    public void setColorPicker(int color) {
        this.color = color;
        color |= 0xFF000000;
        colorPicker.setColorFilter(color);
    }

    public void onDateClick(long milliseconds, String timeZone,
            final DateTimePickerCallback callback) {
        final DateTime dateTime = new DateTime(milliseconds, DateTimeZone.forID(timeZone));

        DatePickerDialog dialog = new DatePickerDialog(
            this,
            new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    DateTime newDateTime = dateTime.withDate(year, monthOfYear + 1, dayOfMonth);
                    callback.onSet(newDateTime.toDate());
                }
            },
            dateTime.getYear(),
            dateTime.getMonthOfYear() - 1,
            dateTime.getDayOfMonth()
        );

        dialog.show();
    }

    public void onTimeClick(long milliseconds, String timeZone,
            final DateTimePickerCallback callback) {
        final DateTime dateTime = new DateTime(milliseconds, DateTimeZone.forID(timeZone));

        TimePickerDialog dialog = new TimePickerDialog(
            this,
            new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    DateTime newDateTime = dateTime.withTime(hourOfDay, minute, 0, 0);
                    callback.onSet(newDateTime.toDate());
                }
            },
            dateTime.getHourOfDay(),
            dateTime.getMinuteOfHour(),
            DateFormat.is24HourFormat(this)
        );

        dialog.show();
    }

    public void onAllDayChecked(boolean isChecked) {
        int visibility = isChecked ? View.INVISIBLE : View.VISIBLE;

        startTime.setVisibility(visibility);
        endTime.setVisibility(visibility);
        timeZoneView.setVisibility(visibility);

        setTimeZone(isChecked ? "UTC" : TimeZone.getDefault().getID());

        event.allDay = isChecked;
    }

    public void onTimeZoneClick(String timeZone) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.dialog_timezone_item,
            TimeZoneHelper.getTimeZones(event.startTime));

        AlertDialog.Builder dialog = new AlertDialog.Builder(this, R.style.AppTheme_Dialog_Alert);
        dialog.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        dialog.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView parent) {

            }
        });

        dialog.show();
    }

    public void setStartTime(Date date) {
        event.startTime = date.getTime();

        TimeZone timeZone = TimeZone.getTimeZone(event.timeZone);

        DATE_FORMAT.setTimeZone(timeZone);
        startDate.setText(DATE_FORMAT.format(date));

        TIME_FORMAT.setTimeZone(timeZone);
        startTime.setText(TIME_FORMAT.format(date));

        timeZoneView.setText(TimeZoneHelper.getTimeZoneGMTName(timeZone, event.startTime));
    }

    public void setEndTime(Date date) {
        event.endTime = date.getTime();

        TimeZone timeZone = TimeZone.getTimeZone(event.getEndTimeZone());

        DATE_FORMAT.setTimeZone(timeZone);
        endDate.setText(DATE_FORMAT.format(date));

        TIME_FORMAT.setTimeZone(timeZone);
        endTime.setText(TIME_FORMAT.format(date));

        timeZoneView.setText(TimeZoneHelper.getTimeZoneGMTName(timeZone, event.endTime));
    }

    public void setTimeZone(String id) {
        event.timeZone = id;

        event.startTime = getTime(startDate, startTime, event.timeZone);
        event.endTime = getTime(endDate, endTime, event.getEndTimeZone());

        TimeZone timeZone = TimeZone.getTimeZone(id);
        timeZoneView.setText(TimeZoneHelper.getTimeZoneGMTName(timeZone, event.startTime));
    }

    public long getTime(TextView date, TextView time, String timeZone) {
        long milliseconds = -1;

        try {
            DATETIME_FORMAT.setTimeZone(TimeZone.getTimeZone(timeZone));
            milliseconds = DATETIME_FORMAT.parse(date.getText() + " " + time.getText()).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return milliseconds;
    }

    public interface DateTimePickerCallback {

        void onSet(Date date);
    }

}
