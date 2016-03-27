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
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TimePicker;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.model.LatLng;
import com.mono.R;
import com.mono.model.Event;
import com.mono.model.Location;
import com.mono.util.Colors;
import com.mono.util.GestureActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EventDetailsActivity extends GestureActivity {

    public static final String EXTRA_EVENT = "event";

    private static final int REQUEST_PLACE_PICKER = 1;

    private static final SimpleDateFormat DATE_FORMAT;
    private static final SimpleDateFormat TIME_FORMAT;

    private EditText title;
    private ImageView colorPicker;
    private EditText startDate;
    private EditText startTime;
    private EditText endDate;
    private EditText endTime;
    private EditText location;
    private ImageView locationPicker;
    private EditText notes;

    private Event original;
    private Event event;

    static {
        DATE_FORMAT = new SimpleDateFormat("EEE, MMMM d, yyyy", Locale.getDefault());
        TIME_FORMAT = new SimpleDateFormat("h:mm a", Locale.getDefault());
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

        startDate = (EditText) findViewById(R.id.start_date);
        startDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onDateClick(event.startTime, new DateTimePickerCallback() {
                    @Override
                    public void onSet(Date date) {
                        startDate.setText(DATE_FORMAT.format(date));
                        event.startTime = date.getTime();
                    }
                });
            }
        });

        startTime = (EditText) findViewById(R.id.start_time);
        startTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onTimeClick(event.startTime, new DateTimePickerCallback() {
                    @Override
                    public void onSet(Date date) {
                        startTime.setText(TIME_FORMAT.format(date));
                        event.startTime = date.getTime();
                    }
                });
            }
        });

        endDate = (EditText) findViewById(R.id.end_date);
        endDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onDateClick(event.endTime, new DateTimePickerCallback() {
                    @Override
                    public void onSet(Date date) {
                        endDate.setText(DATE_FORMAT.format(date));
                        event.endTime = date.getTime();
                    }
                });
            }
        });

        endTime = (EditText) findViewById(R.id.end_time);
        endTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onTimeClick(event.endTime, new DateTimePickerCallback() {
                    @Override
                    public void onSet(Date date) {
                        endTime.setText(TIME_FORMAT.format(date));
                        event.endTime = date.getTime();
                    }
                });
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

        Intent intent = getIntent();
        Event event = intent.getParcelableExtra(EXTRA_EVENT);
        initialize(event);
    }

    @Override
    public void onBackPressed() {
        if (event != null && !event.equals(original)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this,
                R.style.AppTheme_Dialog_Alert);
            builder.setMessage(R.string.confirm_save);

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

    public void initialize(Event event) {
        if (event == null) {
            event = new Event();
        }

        original = event;
        this.event = new Event(event);

        if (event.title != null) {
            title.setText(event.title);
        }

        if (event.color == 0) {
            showColorPicker();
        }
        colorPicker.setColorFilter(this.event.color | 0xFF000000);

        Calendar calendar = Calendar.getInstance();
        if (event.startTime > 0) {
            calendar.setTimeInMillis(event.startTime);
        }

        startDate.setText(DATE_FORMAT.format(calendar.getTime()));
        startTime.setText(TIME_FORMAT.format(calendar.getTime()));

        if (event.endTime > 0) {
            calendar.setTimeInMillis(event.endTime);
        } else {
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);

            this.event.endTime = calendar.getTimeInMillis();
        }

        endDate.setText(DATE_FORMAT.format(calendar.getTime()));
        endTime.setText(TIME_FORMAT.format(calendar.getTime()));

        if (event.location != null) {
            location.setText(event.location.name);
        }

        if (event.description != null) {
            notes.setText(event.description);
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

    public void onDateClick(long milliseconds, final DateTimePickerCallback callback) {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliseconds);

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

    public void onTimeClick(long milliseconds, final DateTimePickerCallback callback) {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliseconds);

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

    public interface DateTimePickerCallback {

        void onSet(Date date);
    }
}
