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
import com.mono.util.Colors;
import com.mono.util.Pixels;
import com.mono.util.SimpleDataSource;
import com.mono.util.SimpleQuickAction;
import com.mono.util.SimpleQuickAction.SimpleQuickActionListener;
import com.mono.util.Views;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class CalendarView extends RelativeLayout implements CalendarPageListener,
        SimpleDataSource<CalendarPageItem> {

    private static final float SCROLL_TEXT_SIZE_DP = 20f;

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

    private LocalDate currentDay;
    private LocalDate lastSelected;
    private LocalDate lastDropped;

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

        int textSize = Pixels.pxFromDp(context, SCROLL_TEXT_SIZE_DP);
        scrollText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
            array.getDimensionPixelSize(R.styleable.CalendarView_scrollTextSize, textSize));

        scrollText.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        params.addRule(RelativeLayout.CENTER_IN_PARENT);

        addView(scrollText, params);

        array.recycle();

        today();
    }

    public void onResume() {
        checkDayChange();
    }

    public void setListener(CalendarListener listener) {
        this.listener = listener;

        items.clear();
        today();
    }

    public void setOnCellDropActions(String[] actions) {
        this.actions = actions;
    }

    @Override
    public Map<Integer, List<Integer>> getMonthColors(int year, int month) {
        if (listener != null) {
            return listener.getMonthColors(year, month);
        }

        return null;
    }

    @Override
    public void onPageClick() {
        if (lastSelected != null) {
            int year = lastSelected.getYear();
            int month = lastSelected.getMonthOfYear() - 1;
            int day = lastSelected.getDayOfMonth();

            select(year, month, day, false);

            if (listener != null) {
                listener.onCellClick(year, month, day, false);
            }

            lastSelected = null;
        }
    }

    @Override
    public void onCellClick(int year, int month, int day) {
        if (lastSelected != null) {
            select(lastSelected.getYear(), lastSelected.getMonthOfYear() - 1,
                lastSelected.getDayOfMonth(), false);
        }

        LocalDate date = new LocalDate(year, month + 1, day);

        if (lastSelected == null || !date.isEqual(lastSelected)) {
            select(year, month, day, true);
            lastSelected = date;

            int index = indexOf(year, month);
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
        LocalDate date = new LocalDate(year, month + 1, day);
        if (lastDropped != null && date.isEqual(lastDropped)) {
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

        if (lastSelected != null && item.year == lastSelected.getYear() &&
                item.month == lastSelected.getMonthOfYear() - 1) {
            item.selectedDay = lastSelected.getDayOfMonth();
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

    private CalendarPageItem createItem(LocalDate date) {
        int year = date.getYear();
        int month = date.getMonthOfYear() - 1;
        int day = date.getDayOfMonth();

        CalendarPageItem item = new CalendarPageItem(year, month, day);
        item.numDays = date.dayOfMonth().getMaximumValue();

        return item;
    }

    private void prepend(int year, int month, int amount) {
        LocalDate date = new LocalDate(year, month + 1, 1);

        for (int i = 0; i < amount; i++) {
            date = date.minusMonths(1);
            items.add(0, createItem(date));
        }

        adapter.notifyItemRangeInserted(0, amount);
    }

    private void append(int year, int month, int amount) {
        int startPosition = items.size();

        LocalDate date = new LocalDate(year, month + 1, 1);

        for (int i = 0; i < amount; i++) {
            date = date.plusMonths(1);
            items.add(createItem(date));
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

    public void scrollToMonth(int year, int month) {
        recyclerView.stopScroll();

        int index = indexOf(year, month);

        if (index == -1) {
            items.clear();

            items.add(createItem(new LocalDate(year, month + 1, 1)));
            adapter.notifyDataSetChanged();

            prepend(year, month, PRECACHE_AMOUNT);
            append(year, month, PRECACHE_AMOUNT);

            index = indexOf(year, month);
        }

        layoutManager.scrollToPositionWithOffset(index, 0);
    }

    public void today() {
        LocalDate date = new LocalDate();
        scrollToMonth(date.getYear(), date.getMonthOfYear() - 1);
    }

    public void select(int year, int month, int day, boolean selected) {
        int index = indexOf(year, month);
        if (index < 0) {
            return;
        }

        CalendarPageAdapter.Holder holder =
            (CalendarPageAdapter.Holder) recyclerView.findViewHolderForAdapterPosition(index);

        if (holder != null) {
            holder.calendar.select(day, selected);
        }
    }

    public void refresh(int year, int month) {
        int index = indexOf(year, month);
        if (index < 0) {
            return;
        }

        CalendarPageItem item = items.get(index);
        item.eventColors.clear();

        adapter.notifyItemChanged(index);
    }

    private int indexOf(int year, int month) {
        return items.indexOf(new CalendarPageItem(year, month, 1));
    }

    public LocalDate getCurrentSelected() {
        return lastSelected;
    }

    public void removeCurrentSelected() {
        if (lastSelected != null) {
            select(lastSelected.getYear(), lastSelected.getMonthOfYear() - 1,
                lastSelected.getDayOfMonth(), false);
            lastSelected = null;
        }
    }

    public int getPageHeight(int year, int month) {
        int index = indexOf(year, month);
        if (index < 0) {
            return 0;
        }

        CalendarPageAdapter.Holder holder =
            (CalendarPageAdapter.Holder) recyclerView.findViewHolderForAdapterPosition(index);

        if (holder != null) {
            return holder.itemView.getMeasuredHeight();
        }

        return 0;
    }

    public void checkDayChange() {
        LocalDate date = new LocalDate();

        if (currentDay == null) {
            currentDay = date;
        } else if (!currentDay.isEqual(date)) {
            if (currentDay.getMonthOfYear() != date.getMonthOfYear()) {
                refresh(currentDay.getYear(), currentDay.getMonthOfYear() - 1);
            }

            refresh(date.getYear(), date.getMonthOfYear() - 1);
            currentDay = date;

            if (listener != null) {
                listener.onDayChange(date.getDayOfMonth());
            }
        }
    }

    public void setFirstDayOfWeek(int dayOfWeek) {
        if (dayOfWeek != firstDayOfWeek) {
            adapter.notifyDataSetChanged();
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

    public interface CalendarListener {

        Map<Integer, List<Integer>> getMonthColors(int year, int month);

        void onDayChange(int day);

        void onCellClick(int year, int month, int day, boolean selected);

        void onCellDrop(String id, int year, int month, int day, int action);
    }
}
