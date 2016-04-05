package com.mono.calendar;

import android.animation.Animator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mono.R;
import com.mono.calendar.CalendarPageAdapter.CalendarPageItem;
import com.mono.calendar.CalendarPageAdapter.CalendarPageListener;
import com.mono.db.DatabaseHelper;
import com.mono.db.dao.EventDataSource;
import com.mono.util.Colors;
import com.mono.util.Pixels;
import com.mono.util.SimpleDataSource;
import com.mono.util.SimpleQuickAction;
import com.mono.util.SimpleQuickAction.SimpleQuickActionListener;
import com.mono.util.Views;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class CalendarView extends RelativeLayout implements CalendarPageListener,
        SimpleDataSource<CalendarPageItem> {

    private static final int FAST_SCROLL_DELTA_Y = 80;
    private static final int FADE_IN_DURATION = 300;
    private static final int FADE_OUT_DURATION = 200;

    private static final int PRECACHE_AMOUNT = 5;

    private CalendarListener listener;
    private String[] actions;

    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private TextView scrollText;

    private CalendarPageAdapter adapter;
    private List<CalendarPageItem> items = new ArrayList<>();

    private Date currentDay;
    private Date lastSelected;
    private Date lastDropped;

    private int firstDayOfWeek;
    private boolean showWeekNumbers;

    private Animator fadeInAnimator;
    private Animator fadeOutAnimator;

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

        scrollTo(year, month);
    }

    private void initialize(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.CalendarView);

        recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(layoutManager = new LinearLayoutManager(context));
        recyclerView.setAdapter(adapter = new CalendarPageAdapter(this));
        recyclerView.addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    fadeOutScrollText(FADE_OUT_DURATION / 2);
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                handleInfiniteScroll(dy);
                handleScrollText(dy);
            }
        });

        addView(recyclerView);
        adapter.setDataSource(this);

        scrollText = new TextView(context);
        scrollText.setAlpha(0);
        scrollText.setTextColor(Colors.getColor(context, R.color.colorPrimary));
        scrollText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
            array.getDimensionPixelSize(R.styleable.CalendarView_scrollTextSize, 40));
        scrollText.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        params.addRule(RelativeLayout.CENTER_IN_PARENT);

        addView(scrollText, params);

        array.recycle();
    }

    public void onResume() {
        checkDayChange();
    }

    public void setListener(CalendarListener listener) {
        this.listener = listener;
    }

    public void setOnCellDropActions(String[] actions) {
        this.actions = actions;
    }

    @Override
    public void onPageClick() {
        if (lastSelected != null) {
            select(lastSelected.year, lastSelected.month, lastSelected.day, false);

            if (listener != null) {
                listener.onCellClick(lastSelected.year, lastSelected.month,
                    lastSelected.day, false);
            }

            lastSelected = null;
        }
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
    public void onCellDrop(View view, final String id, final int year, final int month,
            final int day) {
        Date date = new Date(year, month, day);
        if (lastDropped != null && date.equals(lastDropped)) {
            return;
        }

        lastDropped = date;

        if (actions != null && actions.length > 0) {
            final CalendarTableCell cell = (CalendarTableCell) view;
            cell.setBackground(R.drawable.calendar_day_selected,
                Colors.getColor(getContext(), R.color.yellow));
            cell.setTextColor(Colors.getColor(getContext(), R.color.gray_dark));

            SimpleQuickAction actionView = SimpleQuickAction.newInstance(getContext());
            actionView.setColor(Colors.getColor(getContext(), R.color.colorPrimary));
            actionView.setActions(actions);

            int[] location = new int[2];
            view.getLocationInWindow(location);
            location[1] -= Pixels.Display.getStatusBarHeight(getContext());
            location[1] -= Pixels.Display.getActionBarHeight(getContext());

            int offsetX = location[0] + view.getWidth() / 2;
            int offsetY = view.getHeight();

            actionView.setPosition(location[0], location[1], offsetX, offsetY);
            actionView.setListener(new SimpleQuickActionListener() {
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

        item.firstDayOfWeek = firstDayOfWeek;
        item.showWeekNumbers = showWeekNumbers;

        Calendar calendar = getCalendar(firstDayOfWeek);
        calendar.set(item.year, item.month, 1);

        item.numWeeks = calendar.getActualMaximum(Calendar.WEEK_OF_MONTH);

        item.startIndex = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        item.startIndex = item.startIndex - (calendar.getFirstDayOfWeek() - 1);
        item.startIndex = (item.startIndex + 7) % 7;

        if (lastSelected != null && item.year == lastSelected.year &&
                item.month == lastSelected.month) {
            item.selectedDay = lastSelected.day;
        } else {
            item.selectedDay = 0;
        }

        return item;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    private void handleInfiniteScroll(int deltaY) {
        int position;

        if (deltaY < 0) {
            position = layoutManager.findFirstVisibleItemPosition();
            if (position == 0) {
                CalendarPageItem item = items.get(position);
                prepend(item.year, item.month, PRECACHE_AMOUNT);
            }
        } else if (deltaY > 0) {
            position = layoutManager.findLastVisibleItemPosition();
            if (position == items.size() - 1) {
                CalendarPageItem item = items.get(position);
                append(item.year, item.month, PRECACHE_AMOUNT);
            }
        }
    }

    private CalendarPageItem createItem(Calendar calendar) {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        CalendarPageItem item = new CalendarPageItem(year, month, day);
        item.numDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        EventDataSource dataSource =
            DatabaseHelper.getDataSource(getContext(), EventDataSource.class);
        item.eventColors = dataSource.getEventColorsByMonth(year, month);

        return item;
    }

    private void prepend(int year, int month, int amount) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, 1);

        for (int i = 0; i < amount; i++) {
            calendar.add(Calendar.MONTH, -1);
            items.add(0, createItem(calendar));
        }

        adapter.notifyItemRangeInserted(0, amount);
    }

    private void append(int year, int month, int amount) {
        int startPosition = items.size() - 1;

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, 1);

        for (int i = 0; i < amount; i++) {
            calendar.add(Calendar.MONTH, 1);
            items.add(createItem(calendar));
        }

        adapter.notifyItemRangeInserted(startPosition, amount);
    }

    private void handleScrollText(int deltaY) {
        if (Math.abs(deltaY) > FAST_SCROLL_DELTA_Y) {
            int position = layoutManager.findFirstVisibleItemPosition();
            CalendarPageItem item = items.get(position);

            Calendar calendar = Calendar.getInstance();
            calendar.set(item.year, item.month, 1);

            String monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG,
                Locale.getDefault());
            String text = monthName + " " + item.year;

            scrollText.setText(text);

            fadeInScrollText(FADE_IN_DURATION);
        } else {
            fadeOutScrollText(FADE_OUT_DURATION);
        }
    }

    private void fadeInScrollText(int duration) {
        if (fadeOutAnimator != null) {
            fadeOutAnimator.cancel();
            fadeOutAnimator = null;
        }

        if (fadeInAnimator == null) {
            fadeInAnimator = Views.fade(scrollText, scrollText.getAlpha(), 1, duration, null);
        }
    }

    private void fadeOutScrollText(int duration) {
        if (fadeInAnimator != null) {
            fadeInAnimator.cancel();
            fadeInAnimator = null;
        }

        if (fadeOutAnimator == null) {
            fadeOutAnimator = Views.fade(scrollText, scrollText.getAlpha(), 0, duration, null);
        }
    }

    public void scrollTo(int year, int month) {
        recyclerView.stopScroll();

        int index = items.indexOf(new CalendarPageItem(year, month, 1));

        if (index == -1) {
            items.clear();
            adapter.notifyDataSetChanged();

            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, 1);

            items.add(createItem(calendar));
            adapter.notifyItemInserted(0);
            prepend(year, month, PRECACHE_AMOUNT);
            append(year, month, PRECACHE_AMOUNT);

            index = PRECACHE_AMOUNT;
        }

        layoutManager.scrollToPositionWithOffset(index, 0);
    }

    public void today() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);

        scrollTo(year, month);
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
            item.eventColors = dataSource.getEventColorsByMonth(year, month);

            adapter.notifyItemChanged(index);
        }
    }

    public Date getCurrentSelected() {
        return lastSelected;
    }

    public void removeCurrentSelected() {
        if (lastSelected != null) {
            select(lastSelected.year, lastSelected.month, lastSelected.day, false);
            lastSelected = null;
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

    public void checkDayChange() {
        Calendar calendar = Calendar.getInstance();

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        Date date = new Date(year, month, day);

        if (currentDay == null) {
            currentDay = date;
        } else if (!currentDay.equals(date)) {
            if (currentDay.month != month) {
                refresh(currentDay.year, currentDay.month);
            }

            refresh(year, month);
            currentDay = date;

            if (listener != null) {
                listener.onDayChange(day);
            }
        }
    }

    public void setFirstDayOfWeek(int dayOfWeek) {
        if (dayOfWeek != firstDayOfWeek) {
            adapter.notifyDataSetChanged();
        }

        if (dayOfWeek == 0) {
            dayOfWeek = Calendar.getInstance().getFirstDayOfWeek();
        }

        firstDayOfWeek = dayOfWeek;
    }

    public void showWeekNumbers(boolean state) {
        if (state != showWeekNumbers) {
            adapter.notifyDataSetChanged();
        }

        showWeekNumbers = state;
    }

    public static Calendar getCalendar(int firstDayOfWeek) {
        Calendar calendar = Calendar.getInstance();

        switch (firstDayOfWeek) {
            case Calendar.SUNDAY:
                calendar.setFirstDayOfWeek(firstDayOfWeek);
                calendar.setMinimalDaysInFirstWeek(1);
                break;
            case Calendar.MONDAY:
                calendar.setFirstDayOfWeek(firstDayOfWeek);
                calendar.setMinimalDaysInFirstWeek(4);
                break;
        }

        calendar.setTimeZone(TimeZone.getDefault());

        return calendar;
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

        void onDayChange(int day);

        void onCellClick(int year, int month, int day, boolean selected);

        void onCellDrop(String id, int year, int month, int day, int action);
    }
}
