package com.mono.details;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.mono.EventManager;
import com.mono.R;
import com.mono.SuperCalyPreferences;
import com.mono.model.Calendar;
import com.mono.model.Event;
import com.mono.model.Location;
import com.mono.provider.CalendarProvider;
import com.mono.util.GestureActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This activity displays information of an event as well as allowing the user to create and
 * modify an existing event.
 *
 * @author Gary Ng
 */
public class EventDetailsActivity extends GestureActivity {

    public static final String EXTRA_CALENDAR = "calendar";
    public static final String EXTRA_EVENT = "event";

    public static final int REQUEST_PLACE_PICKER = 100;
    public static final int REQUEST_CONTACT_PICKER = 200;
    public static final int REQUEST_PHOTO_PICKER = 300;

    private TextView calendar;
    private EditText title;
    private ImageView colorPicker;

    private DateTimePanel dateTimePanel;
    private ReminderPanel reminderPanel;
    private LocationPanel locationPanel;
    private NotePanel notePanel;
    private GuestPanel guestPanel;
    private PhotoPanel photoPanel;

    private Event original;
    private Event event;

    private Calendar currentCalendar;
    private int color;
    SharedPreferences sharedPreferences;
    private EventManager manager;
    private List<Event> events = new ArrayList<>();
    private HashMap<String, Location> LocationHashmap = new HashMap<>();

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

        dateTimePanel = new DateTimePanel(this);
        dateTimePanel.onCreate(savedInstanceState);

        reminderPanel = new ReminderPanel(this);
        reminderPanel.onCreate(savedInstanceState);

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
                                            for (int i = 0; i < events.size(); i++) {
                                                events.get(i).location.name = locationPanel.location.getText().toString().trim();
                                                events.get(i).title = locationPanel.location.getText().toString().trim();
                                                manager.updateEvent(
                                                        EventManager.EventAction.ACTOR_SELF,
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
            case R.id.action_save:
                onSave();
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

    /**
     * Populate the interface with the given event data.
     *
     * @param calendar Calendar containing the event.
     * @param original Event data to be used.
     */
    public void initialize(Calendar calendar, Event original) {
        if (original == null) {
            original = new Event(Event.TYPE_CALENDAR);
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

        dateTimePanel.setEvent(event);
        reminderPanel.setEvent(event);
        locationPanel.setEvent(event);
        notePanel.setEvent(event);
        guestPanel.setEvent(event);
        photoPanel.setEvent(event);
    }

    /**
     * Close the activity.
     */
    public void close() {
        event = null;
        onBackPressed();
    }

    /**
     * Handle the action of saving an event.
     */
    public void onSave() {
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

                            close();
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
        } else {
            close();
        }
    }

    /**
     * Handle the action of deleting an event.
     */
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
                                public void onEventAction(EventManager.EventAction... data) {
                                    if (data[0].getStatus() == EventManager.EventAction.STATUS_OK) {
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

    /**
     * Display the calendar picker to choose the current calendar for the event to belong to.
     */
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

                if (event.calendarId <= 0) {
                    event.source = Event.SOURCE_DATABASE;
                } else {
                    event.source = Event.SOURCE_PROVIDER;
                }

                if (event.color == 0) {
                    setColorPicker(currentCalendar.color);
                }

                calendar.setText(items[which]);
            }
        });
        builder.create().show();
    }

    /**
     * Display the color picker to choose colors for event.
     */
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

    /**
     * Set the color for the event.
     *
     * @param color Color value.
     */
    public void setColorPicker(int color) {
        this.color = color;
        color |= 0xFF000000;
        colorPicker.setColorFilter(color);
    }
}
