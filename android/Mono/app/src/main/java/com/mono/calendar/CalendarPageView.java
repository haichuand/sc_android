package com.mono.calendar;

import android.content.ClipData;
import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.mono.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CalendarPageView extends LinearLayout implements View.OnClickListener {

    public static final float DEFAULT_TEXT_SIZE_SP = 16;
    public static final String[] DEFAULT_WEEKDAYS = {
        "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
    };

    private static final int NUM_CELLS = 7;

    private TextView monthLabel;
    private TableLayout tableLayout;

    private List<TableRow> rows = new ArrayList<>();
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

        monthLabel = (TextView) view.findViewById(R.id.month);

        ViewGroup row = (ViewGroup) view.findViewById(R.id.weekdays);

        String[] weekdays = DEFAULT_WEEKDAYS;

        LayoutParams params = new LayoutParams(
            0, LayoutParams.WRAP_CONTENT
        );
        params.weight = 1f / weekdays.length;

        for (String day : weekdays) {
            TextView text = new TextView(context);
            text.setGravity(Gravity.CENTER_HORIZONTAL);
            text.setSingleLine(true);
            text.setText(day);
            text.setTextSize(DEFAULT_TEXT_SIZE_SP);
            text.setTypeface(null, Typeface.BOLD);

            row.addView(text, params);
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
                            getResources().getColor(R.color.yellow));
                        cell.setTextColor(getResources().getColor(R.color.gray_dark));
                        return true;
                    case DragEvent.ACTION_DRAG_EXITED:
                        cell.setLastStyle();
                        return true;
                    case DragEvent.ACTION_DROP:
                        ClipData.Item dragItem = event.getClipData().getItemAt(0);
                        String eventId = dragItem.getText().toString();

                        int index = cells.indexOf(view);
                        int day = index - item.startIndex + 1;

                        if (listener != null) {
                            listener.onCellDrop(view, eventId, item.year, item.month, day);
                        }
                        return true;
                }

                return false;
            }
        };

        for (int i = 0; i < type; i++) {
            TableRow row = new TableRow(getContext());
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

    public void setMonthData(CalendarPageAdapter.CalendarPageItem item) {
        this.item = item;

        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);

        calendar.set(item.year, item.month, item.day);

        int index = 0, day = 1;
        for (CalendarTableCell cell : cells) {
            if (index < item.startIndex || index >= item.startIndex + item.numDays) {
                cell.setVisibility(INVISIBLE);
            } else {
                cell.setVisibility(VISIBLE);
                cell.setText(String.valueOf(day));

                cell.setToday(item.year == currentYear && item.month == currentMonth && day == currentDay);
                cell.setSelected(day == item.selectedDay);

                if (item.eventColors.containsKey(day)) {
                    int color = item.eventColors.get(day)[0] | 0xFF000000;
                    cell.setMarkerColor(color);
                } else {
                    cell.setMarkerColor(0);
                }

                day++;
            }

            index++;
        }
    }

    public void select(int day, boolean selected) {
        CalendarTableCell cell = cells.get(item.startIndex + day - 1);
        cell.setSelected(selected);
    }
}
