package com.mono.details;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;

import com.mono.R;
import com.mono.model.Event;
import com.mono.util.TimeZoneHelper;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * This class is used to handle the date and time sections located in Event Details.
 *
 * @author Gary Ng
 */
public class DateTimePanel implements EventDetailsActivity.PanelInterface {

    private static final SimpleDateFormat DATE_FORMAT;
    private static final SimpleDateFormat TIME_FORMAT;
    private static final SimpleDateFormat DATETIME_FORMAT;

    private EventDetailsActivity activity;
    private TextView startDate;
    private TextView startTime;
    private TextView endDate;
    private TextView endTime;
    private CheckBox allDay;
    private TextView timeZoneView;

    private Event event;

    static {
        DATE_FORMAT = new SimpleDateFormat("EEE, MMMM d, yyyy", Locale.getDefault());
        TIME_FORMAT = new SimpleDateFormat("h:mm a", Locale.getDefault());
        DATETIME_FORMAT = new SimpleDateFormat("EEE, MMMM d, yyyy h:mm a", Locale.getDefault());
    }

    public DateTimePanel(EventDetailsActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        startDate = (TextView) activity.findViewById(R.id.start_date);
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

        startTime = (TextView) activity.findViewById(R.id.start_time);
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

        endDate = (TextView) activity.findViewById(R.id.end_date);
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

        endTime = (TextView) activity.findViewById(R.id.end_time);
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

        allDay = (CheckBox) activity.findViewById(R.id.all_day);
        allDay.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onAllDayChecked(isChecked);
            }
        });

        timeZoneView = (TextView) activity.findViewById(R.id.timezone);
        timeZoneView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
//                onTimeZoneClick(event.timeZone);
            }
        });
    }

    @Override
    public void setVisible(boolean visible) {
        View view = activity.findViewById(R.id.start_date_layout);
        view.setVisibility(visible ? View.VISIBLE : View.GONE);

        view = activity.findViewById(R.id.end_date_layout);
        view.setVisibility(visible ? View.VISIBLE : View.GONE);

        view = activity.findViewById(R.id.options_layout);
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setEnabled(boolean enabled) {
        startDate.setEnabled(enabled);
        startTime.setEnabled(enabled);
        endDate.setEnabled(enabled);
        endTime.setEnabled(enabled);
        allDay.setEnabled(enabled);
    }

    /**
     * Initialize this panel using the given event.
     *
     * @param event The instance of the event.
     */
    @Override
    public void setEvent(Event event) {
        this.event = event;

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
            if (dateTime.getMinuteOfHour() < 30) {
                dateTime = dateTime.withMinuteOfHour(30);
            } else {
                dateTime = dateTime.plusHours(1).withMinuteOfHour(0);
            }

            event.startTime = dateTime.getMillis();
        }

        startDate.setText(DATE_FORMAT.format(dateTime.toDate()));
        startTime.setText(TIME_FORMAT.format(dateTime.toDate()));

        if (event.endTime > 0) {
            dateTime = dateTime.withMillis(event.endTime);
        } else {
            dateTime = dateTime.plusHours(1);
            event.endTime = dateTime.getMillis();
        }

        endDate.setText(DATE_FORMAT.format(dateTime.toDate()));
        endTime.setText(TIME_FORMAT.format(dateTime.toDate()));

        timeZoneView.setText(TimeZoneHelper.getTimeZoneGMTName(timeZone, event.startTime));
        allDay.setChecked(event.allDay);
    }

    /**
     * Handle the action of clicking on the date.
     *
     * @param milliseconds Initial time.
     * @param timeZone Current time zone.
     * @param callback Callback to return results.
     */
    public void onDateClick(long milliseconds, String timeZone,
            final DateTimePickerCallback callback) {
        final DateTime dateTime = new DateTime(milliseconds, DateTimeZone.forID(timeZone));

        DatePickerDialog dialog = new DatePickerDialog(
            activity,
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

    /**
     * Handle the action of clicking on the time.
     *
     * @param milliseconds Initial time.
     * @param timeZone Current time zone.
     * @param callback Callback to return results.
     */
    public void onTimeClick(long milliseconds, String timeZone,
            final DateTimePickerCallback callback) {
        final DateTime dateTime = new DateTime(milliseconds, DateTimeZone.forID(timeZone));

        TimePickerDialog dialog = new TimePickerDialog(
            activity,
            new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    DateTime newDateTime = dateTime.withTime(hourOfDay, minute, 0, 0);
                    callback.onSet(newDateTime.toDate());
                }
            },
            dateTime.getHourOfDay(),
            dateTime.getMinuteOfHour(),
            DateFormat.is24HourFormat(activity)
        );

        dialog.show();
    }

    /**
     * Handle the action of selecting All Day checkbox.
     *
     * @param isChecked Checked status.
     */
    public void onAllDayChecked(boolean isChecked) {
        int visibility = isChecked ? View.INVISIBLE : View.VISIBLE;

        startTime.setVisibility(visibility);
        endTime.setVisibility(visibility);
        timeZoneView.setVisibility(visibility);

        setTimeZone(isChecked ? "UTC" : TimeZone.getDefault().getID());

        event.allDay = isChecked;
    }

    /**
     * Handle the action of clicking on the time zone.
     *
     * @param timeZone Current time zone.
     */
    public void onTimeZoneClick(String timeZone) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, R.layout.dialog_timezone_item,
            TimeZoneHelper.getTimeZones(event.startTime));

        AlertDialog.Builder dialog = new AlertDialog.Builder(activity,
            R.style.AppTheme_Dialog_Alert);
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

    /**
     * Set the start time for the event.
     *
     * @param date Date of the start time.
     */
    public void setStartTime(Date date) {
        event.startTime = date.getTime();

        TimeZone timeZone = TimeZone.getTimeZone(event.timeZone);

        DATE_FORMAT.setTimeZone(timeZone);
        startDate.setText(DATE_FORMAT.format(date));

        TIME_FORMAT.setTimeZone(timeZone);
        startTime.setText(TIME_FORMAT.format(date));

        timeZoneView.setText(TimeZoneHelper.getTimeZoneGMTName(timeZone, event.startTime));
    }

    /**
     * Set the end time for the event.
     *
     * @param date Date of the end time.
     */
    public void setEndTime(Date date) {
        event.endTime = date.getTime();

        TimeZone timeZone = TimeZone.getTimeZone(event.getEndTimeZone());

        DATE_FORMAT.setTimeZone(timeZone);
        endDate.setText(DATE_FORMAT.format(date));

        TIME_FORMAT.setTimeZone(timeZone);
        endTime.setText(TIME_FORMAT.format(date));

        timeZoneView.setText(TimeZoneHelper.getTimeZoneGMTName(timeZone, event.endTime));
    }

    /**
     * Set the time zone for the event.
     *
     * @param id Time zone string.
     */
    public void setTimeZone(String id) {
        event.timeZone = id;

        event.startTime = getTime(startDate, startTime, event.timeZone);
        event.endTime = getTime(endDate, endTime, event.getEndTimeZone());

        TimeZone timeZone = TimeZone.getTimeZone(id);
        timeZoneView.setText(TimeZoneHelper.getTimeZoneGMTName(timeZone, event.startTime));
    }

    /**
     * Parse and retrieve the date and time string as milliseconds.
     *
     * @param date Date view.
     * @param time Time view.
     * @param timeZone Current time zone.
     * @return the time in milliseconds.
     */
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
