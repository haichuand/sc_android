package com.mono.map;

import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mono.R;
import com.mono.util.Colors;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * A adapter used to display dates in the view pager.
 *
 * @author Gary Ng
 */
public class MenuPagerAdapter extends PagerAdapter {

    public static final int TYPE_DAY = 0;
    public static final int TYPE_WEEK = 1;

    private static final DateTimeFormatter DATE_FORMAT;

    private static final float ITEM_WIDTH_DAY = 0.3f;
    private static final float ITEM_WIDTH_WEEK = 0.45f;

    private int type;
    private int count;
    private MenuPagerListener listener;

    private View selected;
    private int selectedPosition = -1;

    static {
        DATE_FORMAT = DateTimeFormat.forPattern("M/d");
    }

    public MenuPagerAdapter(int type, int count, MenuPagerListener listener) {
        this.type = type;
        this.count = count;
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public Object instantiateItem(ViewGroup container, final int position) {
        String text = null;
        OnClickListener clickListener = null;

        if (type == TYPE_DAY) {
            final LocalDate date = new LocalDate().plusDays(-getCount() + position + 1);
            text = DATE_FORMAT.print(date);

            clickListener = new OnClickListener() {
                @Override
                public void onClick(View view) {
                    onSelect(view, position);

                    if (listener != null) {
                        listener.onDaySelected(date.getYear(), date.getMonthOfYear(),
                            date.getDayOfMonth());;
                    }
                }
            };
        } else if (type == TYPE_WEEK) {
            final LocalDate date = new LocalDate().plusWeeks(-getCount() + position + 1);

            LocalDate startDate = date.dayOfWeek().withMinimumValue().minusDays(1);
            LocalDate endDate = date.dayOfWeek().withMaximumValue().minusDays(1);

            text = DATE_FORMAT.print(startDate) + " - " + DATE_FORMAT.print(endDate);

            clickListener = new OnClickListener() {
                @Override
                public void onClick(View view) {
                    onSelect(view, position);

                    if (listener != null) {
                        listener.onWeekSelected(date.getYear(), date.getWeekOfWeekyear());;
                    }
                }
            };
        }

        LayoutInflater inflater = LayoutInflater.from(container.getContext());

        TextView view = (TextView) inflater.inflate(R.layout.map_menu_pager_item, null, false);
        view.setOnClickListener(clickListener);
        view.setText(text);

        if (selectedPosition >= 0 && position == selectedPosition) {
            onSelect(view, position);
        }

        container.addView(view);

        return view;
    }

    /**
     * Select and highlight the specified item.
     *
     * @param view The view to be selected.
     * @param position The position of the view.
     */
    private void onSelect(View view, int position) {
        // Clear Previous Selection
        if (selected != null) {
            int color = Colors.getColor(view.getContext(), android.R.color.transparent);
            selected.setBackgroundColor(color);
        }
        // Select View
        int color = Colors.getColor(view.getContext(), R.color.translucent_50);
        view.setBackgroundColor(color);

        selected = view;
        selectedPosition = position;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public float getPageWidth(int position) {
        if (type == TYPE_DAY) {
            return ITEM_WIDTH_DAY;
        } else if (type == TYPE_WEEK) {
            return ITEM_WIDTH_WEEK;
        }

        return super.getPageWidth(position);
    }

    public static abstract class MenuPagerListener {

        public void onDaySelected(int year, int month, int day) {}

        public void onWeekSelected(int year, int week) {}
    }
}
