package com.mono.calendar;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mono.R;
import com.mono.util.SimpleDataSource;
import com.mono.util.Views;

import java.util.HashMap;
import java.util.Map;

public class CalendarPageAdapter extends RecyclerView.Adapter<CalendarPageAdapter.Holder> {

    private static final int FADE_DURATION = 500;

    private SimpleDataSource<CalendarPageItem> dataSource;
    private CalendarPageListener listener;

    public CalendarPageAdapter(CalendarPageListener listener) {
        this.listener = listener;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.calendar_page_item, parent, false);
        return new Holder(view, viewType);
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        CalendarPageItem item = dataSource.getItem(position);
        holder.onBind(item);
    }

    @Override
    public int getItemViewType(int position) {
        CalendarPageItem item = dataSource.getItem(position);
        return item.numWeeks;
    }

    @Override
    public int getItemCount() {
        return dataSource == null ? 0 : dataSource.getCount();
    }

    @Override
    public void onViewRecycled(Holder holder) {
        holder.onViewRecycled();
    }

    public void setDataSource(SimpleDataSource<CalendarPageItem> dataSource) {
        this.dataSource = dataSource;
        notifyDataSetChanged();
    }

    public class Holder extends RecyclerView.ViewHolder {

        public CalendarPageView calendar;

        public Holder(View itemView, int viewType) {
            super(itemView);

            calendar = (CalendarPageView) itemView.findViewById(R.id.calendar);
            calendar.setListener(listener);
            calendar.setType(viewType);
        }

        public void onBind(CalendarPageItem holderItem) {
            calendar.setMonthLabel(holderItem.year, holderItem.month, holderItem.day);
            calendar.setMonthData(holderItem);

            Views.fade(calendar, 0, 1, FADE_DURATION, null);
        }

        public void onViewRecycled() {

        }
    }

    public static class CalendarPageItem {

        public int year;
        public int month;
        public int day;

        public int startIndex;
        public int numDays;
        public int numWeeks;

        public Map<Integer, Integer[]> eventColors = new HashMap<>();

        public int selectedDay = -1;

        public CalendarPageItem(int year, int month, int day) {
            this.year = year;
            this.month = month;
            this.day = day;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof CalendarPageItem)) {
                return false;
            }

            CalendarPageItem item = (CalendarPageItem) object;

            if (year != item.year || month != item.month) {
                return false;
            }

            return true;
        }
    }

    public interface CalendarPageListener {

        void onCellClick(int year, int month, int day);

        void onCellDrop(View view, long id, int year, int month, int day);
    }
}
