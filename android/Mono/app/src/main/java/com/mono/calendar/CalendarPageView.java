package com.mono.calendar;

import android.content.ClipData;
import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.mono.R;
import com.mono.util.Colors;
import com.mono.util.Common;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalendarPageView extends LinearLayout implements OnClickListener {

    private static final float DEFAULT_TEXT_SIZE_SP = 14;
    private static final String[] DEFAULT_WEEKDAYS = {
        "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
    };

    private static final int NUM_CELLS = 7;

    private TextView monthLabel;
    private List<TextView> weekdays = new ArrayList<>();
    private TableLayout tableLayout;
    private List<CalendarTableRow> rows = new ArrayList<>();
    private List<CalendarTableCell> cells = new ArrayList<>();

    private CalendarPageAdapter.CalendarPageListener listener;
    private CalendarPageAdapter.CalendarPageItem item;

    public CalendarPageView(Context context) {
        this(context, null);
    }

    public CalendarPageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CalendarPageView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CalendarPageView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initialize(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        LayoutInflater inflater = LayoutInflater.from(context);

        View view = inflater.inflate(R.layout.calendar_page_view, this, false);
        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onPageClick();
            }
        });

        monthLabel = (TextView) view.findViewById(R.id.month);

        ViewGroup row = (ViewGroup) view.findViewById(R.id.weekdays);

        String[] weekdayNames = DEFAULT_WEEKDAYS;

        LayoutParams params = new LayoutParams(
            0, LayoutParams.WRAP_CONTENT
        );
        params.weight = 1f / weekdayNames.length;

        Typeface typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL);

        for (int i = 0; i < weekdayNames.length; i++) {
            TextView text = new TextView(context);
            text.setGravity(Gravity.CENTER_HORIZONTAL);
            text.setSingleLine(true);
            text.setText("");
            text.setTextSize(DEFAULT_TEXT_SIZE_SP);
            text.setTypeface(typeface);

            row.addView(text, params);
            weekdays.add(text);
        }

        tableLayout = (TableLayout) view.findViewById(R.id.calendar);

        addView(view);
    }

    public void setListener(CalendarPageAdapter.CalendarPageListener listener) {
        this.listener = listener;
    }

    @Override
    public void onClick(View view) {
        if (view instanceof CalendarTableCell) {
            int index = cells.indexOf(view);
            int day = index - item.startIndex + 1;

            listener.onCellClick(item.year, item.month, day);
        }
    }

    public void setType(int type) {
        LayoutParams params = new LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
        );

        TableRow.LayoutParams cellParams = new TableRow.LayoutParams(
            0, TableRow.LayoutParams.WRAP_CONTENT
        );

        OnDragListener onDragListener = new OnDragListener() {
            @Override
            public boolean onDrag(View view, DragEvent event) {
                CalendarTableCell cell = (CalendarTableCell) view;
                int index = cells.indexOf(view);
                int day = index - item.startIndex + 1;

                int action = event.getAction();

                switch (action) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        String label = event.getClipDescription().getLabel().toString();
                        if (label.equals(CalendarFragment.EVENT_ITEM_LABEL)) {
                            return true;
                        }
                        return false;
                    case DragEvent.ACTION_DRAG_ENTERED:
                        cell.setBackground(R.drawable.calendar_day_selected,
                            Colors.getColor(getContext(), R.color.yellow));
                        cell.setTextColor(Colors.getColor(getContext(), R.color.gray_dark));

                        checkWeekNumber(day, true);
                        return true;
                    case DragEvent.ACTION_DRAG_EXITED:
                        cell.setLastStyle();

                        checkWeekNumber(day, false);
                        return true;
                    case DragEvent.ACTION_DROP:
                        ClipData.Item dragItem = event.getClipData().getItemAt(0);
                        String eventId = dragItem.getText().toString();

                        if (listener != null) {
                            listener.onCellDrop(view, eventId, item.year, item.month, day);
                        }
                        return true;
                }

                return false;
            }
        };

        for (int i = 0; i < type; i++) {
            CalendarTableRow row = new CalendarTableRow(getContext());
            row.setBackgroundResource(R.drawable.calendar_row);

            for (int j = 0; j < NUM_CELLS; j++) {
                CalendarTableCell cell = new CalendarTableCell(getContext());
                cell.setOnClickListener(this);
                cell.setOnDragListener(onDragListener);

                row.addView(cell, cellParams);
                cells.add(cell);
            }

            tableLayout.addView(row, params);
            rows.add(row);
        }
    }

    public void setMonthLabel(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day);

        String monthName =
            calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
        monthLabel.setText(String.format("%s %d", monthName, year));
    }

    private void createWeekdays(int firstDayOfWeek) {
        firstDayOfWeek = Common.clamp(firstDayOfWeek - 1, 0, 6);
        for (TextView weekday : weekdays) {
            weekday.setText(DEFAULT_WEEKDAYS[firstDayOfWeek]);
            firstDayOfWeek = (firstDayOfWeek + 1) % 7;
        }
    }

    public void setMonthData(CalendarPageAdapter.CalendarPageItem item) {
        this.item = item;

        createWeekdays(item.firstDayOfWeek);

        if (!item.showWeekNumbers) {
            for (CalendarTableRow row : rows) {
                row.setText(null);
            }
        } else {
            Calendar calendar = CalendarView.getCalendar(item.firstDayOfWeek);
            calendar.set(item.year, item.month, 1);

            for (CalendarTableRow row : rows) {
                int week = calendar.get(Calendar.WEEK_OF_YEAR);
                row.setText(String.valueOf(week));

                calendar.add(Calendar.DAY_OF_MONTH, 7);
            }
        }

        int index = 0, day = 1;
        for (CalendarTableCell cell : cells) {
            if (Common.between(index, item.startIndex, item.startIndex + item.numDays - 1)) {
                cell.setText(String.valueOf(day));
                cell.setVisibility(VISIBLE);

                cell.setToday(false);
                cell.setSelected(false);

                cell.clearMarkerColor();

                day++;
            } else {
                cell.setVisibility(INVISIBLE);
            }

            index++;
        }

        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);

        if (item.year == currentYear && item.month == currentMonth) {
            setToday(currentDay);
        }

        if (item.selectedDay > 0) {
            select(item.selectedDay, true);
        }
    }

    public void setMarkerData(Map<Integer, List<Integer>> data) {
        int index = 0, day = 1;
        for (CalendarTableCell cell : cells) {
            if (Common.between(index, item.startIndex, item.startIndex + item.numDays - 1)) {
                if (data.containsKey(day)) {
                    List<Integer> eventColors = data.get(day);

                    int size = Math.min(eventColors.size(), CalendarTableCell.MAX_MARKER_COLORS);
                    int[] colors = new int[size];

                    for (int i = 0; i < size; i++) {
                        colors[i] = eventColors.get(i) | 0xFF000000;
                    }

                    cell.setMarkerColor(colors);
                    cell.invalidate();
                }

                day++;
            }

            index++;
        }
    }

    public void setToday(int day) {
        CalendarTableCell cell = cells.get(item.startIndex + day - 1);
        cell.setToday(true);

        checkWeekNumber(day, true);
    }

    public void select(int day, boolean selected) {
        CalendarTableCell cell = cells.get(item.startIndex + day - 1);
        cell.setSelected(selected);

        checkWeekNumber(day, selected);
    }

    private void checkWeekNumber(int day, boolean selected) {
        if (!item.showWeekNumbers) {
            return;
        }

        Calendar calendar = CalendarView.getCalendar(item.firstDayOfWeek);
        boolean isTodayFirstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) == item.firstDayOfWeek;
        int currentWeek = calendar.get(Calendar.WEEK_OF_YEAR);

        calendar.set(item.year, item.month, day);
        boolean isFirstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) == item.firstDayOfWeek;
        int selectedWeek = calendar.get(Calendar.WEEK_OF_YEAR);

        boolean isSameWeek = currentWeek == selectedWeek;

        int index = calendar.get(Calendar.WEEK_OF_MONTH) - 1;
        CalendarTableRow row = rows.get(index);

        if (isSameWeek && isTodayFirstDayOfWeek || selected && isFirstDayOfWeek) {
            row.setText(null);
        } else {
            int week = calendar.get(Calendar.WEEK_OF_YEAR);
            row.setText(String.valueOf(week));
        }
    }
}
