package com.mono.calendar;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.mono.R;
import com.mono.calendar.CalendarPageAdapter.CalendarPageItem;
import com.mono.db.DatabaseHelper;
import com.mono.db.dao.EventDataSource;
import com.mono.util.SimpleDataSource;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CalendarView extends RelativeLayout implements CalendarListener,
        SimpleDataSource<CalendarPageItem> {

    private static final int AMOUNT = 5;

    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;

    private CalendarPageAdapter adapter;
    private List<CalendarPageItem> items = new ArrayList<>();

    private CalendarTableCell lastSelected;

    public CalendarView(Context context) {
        this(context, null);
    }

    public CalendarView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CalendarView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CalendarView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context, attrs, defStyleAttr, defStyleRes);

        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);

        items.add(createItem(calendar));
        adapter.notifyItemInserted(0);
        prepend(year, month, AMOUNT);
        append(year, month, AMOUNT);

        layoutManager.scrollToPosition(AMOUNT);
    }

    private void initialize(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.CalendarView);

        recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(layoutManager = new LinearLayoutManager(context));
        recyclerView.setAdapter(adapter = new CalendarPageAdapter(this));
        recyclerView.addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy < 0) {
                    int firstPosition = layoutManager.findFirstVisibleItemPosition();
                    if (firstPosition == 0) {
                        CalendarPageItem item = items.get(firstPosition);
                        prepend(item.year, item.month, AMOUNT);
                    }
                } else if (dy > 0) {
                    int lastPosition = layoutManager.findLastVisibleItemPosition();
                    if (lastPosition == items.size() - 1) {
                        CalendarPageItem item = items.get(lastPosition);
                        append(item.year, item.month, AMOUNT);
                    }
                }
            }
        });

        addView(recyclerView);
        adapter.setDataSource(this);
        array.recycle();
    }

    @Override
    public void onCellClick(CalendarTableCell cell, CalendarPageItem item) {
        if (lastSelected != null) {
            lastSelected.setSelected(false);
        }
        cell.setSelected(true);
        lastSelected = cell;

        int index = items.indexOf(item);
        int position = layoutManager.findFirstCompletelyVisibleItemPosition();

        if (index != position) {
            layoutManager.scrollToPositionWithOffset(index, 0);
        }
    }

    @Override
    public CalendarPageItem getItem(int position) {
        return items.get(position);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    public CalendarPageItem createItem(Calendar calendar) {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        calendar.set(Calendar.DAY_OF_MONTH, 1);

        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int numWeeks = calendar.getActualMaximum(Calendar.WEEK_OF_MONTH);
        int numDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        CalendarPageItem item = new CalendarPageItem(year, month, day);
        item.startIndex = dayOfWeek - 1;
        item.numDays = numDays;
        item.numWeeks = numWeeks;

        EventDataSource dataSource =
            DatabaseHelper.getDataSource(getContext(), EventDataSource.class);
        item.markerColors = dataSource.getEventColorsByMonth(year, month);

        return item;
    }

    public void prepend(int year, int month, int amount) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, 1);

        for (int i = 0; i < amount; i++) {
            calendar.add(Calendar.MONTH, -1);
            items.add(0, createItem(calendar));
        }

        adapter.notifyItemRangeInserted(0, amount);
    }

    public void append(int year, int month, int amount) {
        int startPosition = items.size() - 1;

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, 1);

        for (int i = 0; i < amount; i++) {
            calendar.add(Calendar.MONTH, 1);
            items.add(createItem(calendar));
        }

        adapter.notifyItemRangeInserted(startPosition, amount);
    }

    public void refresh(int year, int month, int day) {

    }
}
