package com.mono.details;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.MenuItem;
import android.view.View;
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

import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.model.LatLng;
import com.mono.R;
import com.mono.model.Attendee;
import com.mono.model.Event;
import com.mono.model.Location;
import com.mono.util.Colors;
import com.mono.util.GestureActivity;
import com.mono.util.TimeZoneHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class EventDetailsActivity extends GestureActivity {

    public static final String EXTRA_EVENT = "event";

    private static final int REQUEST_PLACE_PICKER = 1;

    private static final SimpleDateFormat DATE_FORMAT;
    private static final SimpleDateFormat TIME_FORMAT;
    private static final SimpleDateFormat DATETIME_FORMAT;

    private EditText title;
    private ImageView colorPicker;
    private TextView startDate;
    private TextView startTime;
    private TextView endDate;
    private TextView endTime;
    private CheckBox allDay;
    private TextView timeZoneView;
    private EditText location;
    private ImageView locationPicker;
    private EditText notes;
    private EditText guests;

    private Event original;
    private Event event;

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
                event.title = s.length() > 0 ? s.toString() : null;
            }
        });

        colorPicker = (ImageView) findViewById(R.id.color_picker);
        colorPicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showColorPicker();
            }
        });

        startDate = (TextView) findViewById(R.id.start_date);
        startDate.setOnClickListener(new View.OnClickListener() {
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
        startTime.setOnClickListener(new View.OnClickListener() {
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
        endDate.setOnClickListener(new View.OnClickListener() {
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
        endTime.setOnClickListener(new View.OnClickListener() {
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
        timeZoneView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                onTimeZoneClick(event.timeZone);
            }
        });

        location = (EditText) findViewById(R.id.location);
        location.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                event.location = s.length() > 0 ? new Location(s.toString()) : null;
            }
        });

        locationPicker = (ImageView) findViewById(R.id.location_picker);
        locationPicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPlacePicker();
            }
        });

        notes = (EditText) findViewById(R.id.notes);
        notes.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                event.description = s.length() > 0 ? s.toString() : null;
            }
        });

        guests = (EditText) findViewById(R.id.guests);
        guests.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        Intent intent = getIntent();
        Event event = intent.getParcelableExtra(EXTRA_EVENT);
        initialize(event);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_PLACE_PICKER:
                handlePlacePicker(resultCode, data);
                break;
        }
    }

    public void initialize(Event original) {
        if (original == null) {
            original = new Event();
        }

        this.original = original;
        event = new Event(original);

        if (event.title != null) {
            title.setText(event.title);
        }

        if (event.color == 0) {
            showColorPicker();
        }
        colorPicker.setColorFilter(event.color | 0xFF000000);

        if (event.timeZone == null) {
            event.timeZone = TimeZone.getDefault().getID();
        }

        TimeZone timeZone = TimeZone.getTimeZone(event.timeZone);
        DATE_FORMAT.setTimeZone(timeZone);
        TIME_FORMAT.setTimeZone(timeZone);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(timeZone);

        if (event.startTime > 0) {
            calendar.setTimeInMillis(event.startTime);
        } else {
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            event.startTime = calendar.getTimeInMillis();
        }

        startDate.setText(DATE_FORMAT.format(calendar.getTime()));
        startTime.setText(TIME_FORMAT.format(calendar.getTime()));

        if (event.endTime > 0) {
            calendar.setTimeInMillis(event.endTime);
        } else {
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            calendar.add(Calendar.HOUR_OF_DAY, 1);

            event.endTime = calendar.getTimeInMillis();
        }

        endDate.setText(DATE_FORMAT.format(calendar.getTime()));
        endTime.setText(TIME_FORMAT.format(calendar.getTime()));

        timeZoneView.setText(TimeZoneHelper.getTimeZoneGMTName(timeZone, event.startTime));
        allDay.setChecked(event.allDay);

        if (event.location != null) {
            location.setText(event.location.name);
        }

        if (event.description != null) {
            notes.setText(event.description);
        }

        if (!event.attendees.isEmpty()) {
            String str = "";
            for (int i = 0; i < event.attendees.size(); i++) {
                Attendee attendee = event.attendees.get(i);
                if (i > 0) str += '\n';
                str += attendee.userName;
            }
            guests.setText(str);
        }
    }

    public void close() {
        event = null;
        onBackPressed();
    }

    public void showColorPicker() {
        int[] colorIds = {
            R.color.blue,
            R.color.blue_dark,
            R.color.brown,
            R.color.green,
            R.color.lavender,
            R.color.orange,
            R.color.purple,
            R.color.red_1,
            R.color.yellow_1
        };

        int color;

        do {
            int colorId = colorIds[(int) (Math.random() * colorIds.length) % colorIds.length];
            color = Colors.getColor(this, colorId);

            if (colorIds.length == 1) {
                break;
            }
        } while (color == event.color);

        colorPicker.setColorFilter(color);
        event.color = color;
    }

    public void showPlacePicker() {
        try {
            PlaceAutocomplete.IntentBuilder builder =
                new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN);

            Intent intent = builder.build(this);
            startActivityForResult(intent, REQUEST_PLACE_PICKER);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handlePlacePicker(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Place place = PlaceAutocomplete.getPlace(this, data);
            LatLng latLng = place.getLatLng();

            Location location = new Location(latLng.latitude, latLng.longitude);
            String name = place.getName().toString();
            String address = place.getAddress().toString();

            if (!address.startsWith(name)) {
                location.name = name + ", " + address;
            } else {
                location.name = address;
            }

            setLocation(location);
        }
    }

    public void setLocation(Location location) {
        this.location.setText(location.name);
        event.location = location;
    }

    public void onDateClick(long milliseconds, String timeZone,
            final DateTimePickerCallback callback) {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliseconds);
        calendar.setTimeZone(TimeZone.getTimeZone(timeZone));

        DatePickerDialog dialog = new DatePickerDialog(
            this,
            new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    calendar.set(year, monthOfYear, dayOfMonth);
                    callback.onSet(calendar.getTime());
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    public void onTimeClick(long milliseconds, String timeZone,
            final DateTimePickerCallback callback) {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliseconds);
        calendar.setTimeZone(TimeZone.getTimeZone(timeZone));

        TimePickerDialog dialog = new TimePickerDialog(
            this,
            new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);
                    callback.onSet(calendar.getTime());
                }
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
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
