package com.mono.calendar;

import android.animation.Animator;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mono.R;
import com.mono.util.SimpleDataSource;
import com.mono.util.Views;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendarPageAdapter extends RecyclerView.Adapter<CalendarPageAdapter.Holder> {

    private static final int FADE_DURATION = 500;

    private SimpleDataSource<CalendarPageItem> dataSource;
    private CalendarPageListener listener;

    private AsyncTask<CalendarPageItem, Void, Map<Integer, List<Integer>>> task;

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
        private Animator animator;

        public Holder(View itemView, int viewType) {
            super(itemView);

            calendar = (CalendarPageView) itemView.findViewById(R.id.calendar);
            calendar.setAlpha(0);
            calendar.setListener(listener);
            calendar.setType(viewType);
        }

        public void onBind(CalendarPageItem holderItem) {
            calendar.setAlpha(0);
            calendar.setMonthLabel(holderItem.year, holderItem.month, holderItem.day);
            calendar.setMonthData(holderItem);

            if (!holderItem.eventColors.isEmpty()) {
                calendar.setMarkerData(holderItem.eventColors);
            } else {
                setMarkerDataAsync(holderItem);
            }

            animator = Views.fade(calendar, calendar.getAlpha(), 1, FADE_DURATION, null);
        }

        public void onViewRecycled() {
            if (animator != null) {
                animator.cancel();
                animator = null;
            }
        }

        private void setMarkerDataAsync(CalendarPageItem item) {
            task = new AsyncTask<CalendarPageItem, Void, Map<Integer, List<Integer>>>() {

                private CalendarPageItem item;

                @Override
                protected Map<Integer, List<Integer>> doInBackground(CalendarPageItem... params) {
                    item = params[0];
                    return listener.getMonthColors(item.year, item.month);
                }

                @Override
                protected void onPostExecute(Map<Integer, List<Integer>> result) {
                    if (result != null) {
                        calendar.setMarkerData(item.eventColors = result);
                    }

                    task = null;
                }
            }.execute(item);
        }
    }

    public static class CalendarPageItem {

        public int year;
        public int month;
        public int day;

        public int firstDayOfWeek;
        public boolean showWeekNumbers;

        public int numDays;
        public int numWeeks;
        public int startIndex;

        public Map<Integer, List<Integer>> eventColors = new HashMap<>();

        public int selectedDay;

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

        Map<Integer, List<Integer>> getMonthColors(int year, int month);

        void onPageClick();

        void onCellClick(int year, int month, int day);

        void onCellDrop(View view, String id, int year, int month, int day);
    }
}
