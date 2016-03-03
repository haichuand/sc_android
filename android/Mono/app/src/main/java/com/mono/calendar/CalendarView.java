package com.mono.calendar;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import com.mono.R;
import com.mono.calendar.CalendarPageAdapter.CalendarPageItem;
import com.mono.calendar.CalendarPageAdapter.CalendarPageListener;
import com.mono.db.DatabaseHelper;
import com.mono.db.dao.EventDataSource;
import com.mono.util.Pixels;
import com.mono.util.SimpleDataSource;
import com.mono.util.SimpleQuickAction;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CalendarView extends RelativeLayout implements CalendarPageListener,
        SimpleDataSource<CalendarPageItem> {

    private static final int AMOUNT = 5;

    private CalendarListener listener;
    private String[] actions;

    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;

    private CalendarPageAdapter adapter;
    private List<CalendarPageItem> items = new ArrayList<>();

    private Date lastSelected;
    private Date lastDropped;

    private int currentDay;

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

    public void onResume() {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        if (currentDay != day) {
            refresh(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH));
            currentDay = day;
        }
    }

    public void setListener(CalendarListener listener) {
        this.listener = listener;
    }

    public void setOnCellDropActions(String[] actions) {
        this.actions = actions;
    }

    @Override
    public void onCellClick(int year, int month, int day) {
        if (lastSelected != null) {
            select(lastSelected.year, lastSelected.month, lastSelected.day, false);
        }

        Date date = new Date(year, month, day);

        if (lastSelected == null || !date.equals(lastSelected)) {
            select(year, month, day, true);
            lastSelected = date;

            int index = items.indexOf(new CalendarPageItem(year, month, 1));
            int position = layoutManager.findFirstCompletelyVisibleItemPosition();

            if (index != position) {
                layoutManager.scrollToPositionWithOffset(index, 0);
            }
        } else {
            lastSelected = null;
        }

        if (listener != null) {
            listener.onCellClick(year, month, day, lastSelected != null);
        }
    }

    @Override
    public void onCellDrop(View view, final long id, final int year, final int month,
            final int day) {
        Date date = new Date(year, month, day);
        if (lastDropped != null && date.equals(lastDropped)) {
            return;
        }

        lastDropped = date;

        if (actions != null && actions.length > 0) {
            final CalendarTableCell cell = (CalendarTableCell) view;
            cell.setBackground(R.drawable.calendar_day_selected,
                getResources().getColor(R.color.yellow));
            cell.setTextColor(getResources().getColor(R.color.gray_dark));

            SimpleQuickAction actionView = SimpleQuickAction.newInstance(getContext());
            actionView.setColor(getResources().getColor(R.color.colorPrimary));
            actionView.setActions(actions);

            int[] location = new int[2];
            view.getLocationInWindow(location);
            location[1] -= Pixels.Display.getStatusBarHeight(getContext());
            location[1] -= Pixels.Display.getActionBarHeight(getContext());

            int offsetX = location[0] + view.getWidth() / 2;
            int offsetY = view.getHeight();

            actionView.setPosition(location[0], location[1], offsetX, offsetY);
            actionView.setListener(new SimpleQuickAction.SimpleQuickActionListener() {
                @Override
                public void onActionClick(int position) {
                    if (listener != null) {
                        listener.onCellDrop(id, year, month, day, position);
                    }
                }

                @Override
                public void onDismiss() {
                    cell.setLastStyle();
                    lastDropped = null;
                }
            });

            addView(actionView);
        } else {
            if (listener != null) {
                listener.onCellDrop(id, year, month, day, -1);
            }

            lastDropped = null;
        }
    }

    @Override
    public CalendarPageItem getItem(int position) {
        CalendarPageItem item = items.get(position);

        if (lastSelected != null && item.year == lastSelected.year &&
                item.month == lastSelected.month) {
            item.selectedDay = lastSelected.day;
        } else {
            item.selectedDay = -1;
        }

        return item;
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
        item.eventIds = dataSource.getEventIdsByMonth(year, month);

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

    public void select(int year, int month, int day, boolean selected) {
        int index = items.indexOf(new CalendarPageItem(year, month, 1));

        if (index >= 0) {
            CalendarPageAdapter.Holder holder =
                (CalendarPageAdapter.Holder) recyclerView.findViewHolderForAdapterPosition(index);

            if (holder != null) {
                holder.calendar.select(day, selected);
            }
        }
    }

    public void refresh(int year, int month) {
        int index = items.indexOf(new CalendarPageItem(year, month, 1));

        if (index >= 0) {
            CalendarPageItem item = items.get(index);

            EventDataSource dataSource =
                DatabaseHelper.getDataSource(getContext(), EventDataSource.class);
            item.eventIds = dataSource.getEventIdsByMonth(year, month);

            adapter.notifyItemChanged(index);
        }
    }

    public Date getCurrentSelected() {
        return lastSelected;
    }

    public void today() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        int index = items.indexOf(new CalendarPageItem(year, month, 1));

        if (index >= 0) {
            layoutManager.scrollToPositionWithOffset(index, 0);
        }
    }

    public int getPageHeight(int year, int month) {
        int index = items.indexOf(new CalendarPageItem(year, month, 1));

        if (index >= 0) {
            CalendarPageAdapter.Holder holder =
                (CalendarPageAdapter.Holder) recyclerView.findViewHolderForAdapterPosition(index);

            if (holder != null) {
                return holder.itemView.getMeasuredHeight();
            }
        }

        return 0;
    }

    public class Date {

        public int year;
        public int month;
        public int day;

        public Date(int year, int month, int day) {
            this.year = year;
            this.month = month;
            this.day = day;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof Date)) {
                return false;
            }

            Date date = (Date) object;

            if (year != date.year || month != date.month || day != date.day) {
                return false;
            }

            return true;
        }
    }

    public interface CalendarListener {

        void onCellClick(int year, int month, int day, boolean selected);

        void onCellDrop(long id, int year, int month, int day, int action);
    }
}
