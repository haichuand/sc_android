package com.mono.map;

import android.app.DatePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;

import com.mono.R;
import com.mono.map.MenuPagerAdapter.MenuPagerListener;
import com.mono.util.Constants;
import com.mono.util.Pixels;
import com.mono.util.SimpleViewPager;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * This class is used to handle the bar section of time range selection used for displaying
 * events in the map.
 *
 * @author Gary Ng
 */
public class MapMenuBar implements OnItemSelectedListener {

    private static final CharSequence[] MENU_ITEMS = {"Day", "Week", "Custom"};
    private static final int MENU_DAY = 0;
    private static final int MENU_WEEK = 1;
    private static final int MENU_CUSTOM = 2;

    private static final float MENU_TEXT_SIZE_SP = 16;

    private static final SimpleDateFormat DATE_FORMAT;

    private Context context;
    private MapMenuBarListener listener;

    private Spinner menu;
    private ViewGroup menuContainer;

    private long startTime;
    private long endTime;

    static {
        DATE_FORMAT = new SimpleDateFormat("MM/dd/yy", Locale.getDefault());
    }

    public MapMenuBar(Context context, MapMenuBarListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void onCreateView(View view) {
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(context,
            R.layout.simple_spinner_item, MENU_ITEMS);
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

        menu = (Spinner) view.findViewById(R.id.menu);
        menu.setAdapter(adapter);
        menu.setOnItemSelectedListener(this);

        menuContainer = (ViewGroup) view.findViewById(R.id.menu_container);

        startTime = System.currentTimeMillis() - Constants.DAY_MS;
        endTime = System.currentTimeMillis();
    }

    @Override
    public void onItemSelected(AdapterView parent, View view, int position, long id) {
        ((TextView) parent.getChildAt(0)).setTextSize(MENU_TEXT_SIZE_SP);

        switch (position) {
            case MENU_DAY:
                onMenuDay();
                break;
            case MENU_WEEK:
                onMenuWeek();
                break;
            case MENU_CUSTOM:
                onMenuCustom();
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView parent) {

    }

    private void showMenuPager(int type, MenuPagerListener listener) {
        menuContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.map_menu_pager, null, false);

        int count = 100;
        SimpleViewPager viewPager = (SimpleViewPager) view.findViewById(R.id.container);
        viewPager.setAdapter(new MenuPagerAdapter(type, count, listener));

        viewPager.setCurrentItem(count - 1);
        viewPager.setOffscreenPageLimit(10);
        viewPager.setSwipeEnabled(true);
        viewPager.setPageMargin(Pixels.pxFromDp(context, 1));
        viewPager.setPageMarginDrawable(android.R.color.white);

        menuContainer.addView(view);
    }

    /**
     * Upon day type selection, change menu selection to show days.
     */
    private void onMenuDay() {
        showMenuPager(MenuPagerAdapter.TYPE_DAY, new MenuPagerListener() {
            @Override
            public void onDaySelected(int year, int month, int day) {
                DateTime dateTime = new DateTime(year, month, day, 0, 0);

                startTime = dateTime.millisOfDay().withMinimumValue().getMillis();
                endTime = dateTime.millisOfDay().withMaximumValue().getMillis();

                if (listener != null) {
                    listener.onTimeSelected(startTime, endTime);
                }
            }
        });
    }

    /**
     * Upon week type selection, change menu selection to show weeks.
     */
    private void onMenuWeek() {
        showMenuPager(MenuPagerAdapter.TYPE_WEEK, new MenuPagerListener() {
            @Override
            public void onWeekSelected(int year, int week) {
                DateTime dateTime = new DateTime().withYear(year).withWeekOfWeekyear(week);

                DateTime startDateTime = dateTime.dayOfWeek().withMinimumValue().minusDays(1);
                startTime = startDateTime.millisOfDay().withMinimumValue().getMillis();

                DateTime endDateTime = dateTime.dayOfWeek().withMaximumValue().minusDays(1);
                endTime = endDateTime.millisOfDay().withMaximumValue().getMillis();

                if (listener != null) {
                    listener.onTimeSelected(startTime, endTime);
                }
            }
        });
    }

    /**
     * Upon custom type selection, change menu selection to show time range.
     */
    private void onMenuCustom() {
        menuContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.map_menu_range, null, false);

        final TextView startDate = (TextView) view.findViewById(R.id.start_date);
        startDate.setText(DATE_FORMAT.format(startTime));
        startDate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                String timeZone = TimeZone.getDefault().getID();
                onDateClick(startTime, timeZone, new DateTimePickerCallback() {
                    @Override
                    public void onSet(Date date) {
                        startDate.setText(DATE_FORMAT.format(startTime = date.getTime()));

                        if (listener != null) {
                            listener.onTimeSelected(startTime, endTime);
                        }
                    }
                });
            }
        });

        final TextView endDate = (TextView) view.findViewById(R.id.end_date);
        endDate.setText(DATE_FORMAT.format(endTime));
        endDate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                String timeZone = TimeZone.getDefault().getID();
                onDateClick(endTime, timeZone, new DateTimePickerCallback() {
                    @Override
                    public void onSet(Date date) {
                        endDate.setText(DATE_FORMAT.format(endTime = date.getTime()));

                        if (listener != null) {
                            listener.onTimeSelected(startTime, endTime);
                        }
                    }
                });
            }
        });

        menuContainer.addView(view);
    }

    public void onDateClick(long milliseconds, String timeZone,
            final DateTimePickerCallback callback) {
        final DateTime dateTime = new DateTime(milliseconds, DateTimeZone.forID(timeZone));

        DatePickerDialog dialog = new DatePickerDialog(
            context,
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

    public interface DateTimePickerCallback {

        void onSet(Date date);
    }

    public interface MapMenuBarListener {

        void onTimeSelected(long startTime, long endTime);
    }
}
