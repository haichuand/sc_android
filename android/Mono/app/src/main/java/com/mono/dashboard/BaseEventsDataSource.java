package com.mono.dashboard;

import android.content.Context;

import com.mono.R;
import com.mono.model.EventFilter;
import com.mono.model.Instance;
import com.mono.util.SimpleDataSource;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * This data source class is to be used by an adapter to display its content.
 *
 * @author Gary Ng
 */
public abstract class BaseEventsDataSource<E extends Instance, I> implements SimpleDataSource<I> {

    protected SimpleDateFormat DATE_FORMAT;
    protected SimpleDateFormat DATE_FORMAT_2;
    protected SimpleDateFormat TIME_FORMAT;

    protected Context context;

    protected final Map<String, I> items = new HashMap<>();
    protected final List<E> events = new ArrayList<>();
    protected final Map<String, E> eventsMap = new HashMap<>();

    protected Comparator<E> comparator;
    protected int dateTimeColorId;
    protected List<EventFilter> filters = new ArrayList<>();

    public BaseEventsDataSource(Context context, int dateTimeColorId) {
        DATE_FORMAT = new SimpleDateFormat("MMM d", Locale.getDefault());
        DATE_FORMAT_2 = new SimpleDateFormat("M/d/yy", Locale.getDefault());
        TIME_FORMAT = new SimpleDateFormat("h:mm a", Locale.getDefault());

        this.context = context;
        this.dateTimeColorId = dateTimeColorId;
    }

    @Override
    public int getCount() {
        return events.size();
    }

    /**
     * Helper function to convert milliseconds into a readable date string that takes time zone
     * into account.
     *
     * @param time Time in milliseconds.
     * @param timeZone Time zone to be used.
     * @param allDay All day event.
     * @return date string.
     */
    protected String getDateString(long time, TimeZone timeZone, boolean allDay) {
        LocalDate currentDate = new LocalDate();

        LocalDateTime dateTime = new LocalDateTime(time);
        LocalDate date = dateTime.toLocalDate();

        SimpleDateFormat dateFormat;

        if (date.isEqual(currentDate)) {
            if (allDay) {
                return context.getString(R.string.today);
            } else {
                dateFormat = TIME_FORMAT;
            }
        } else if (date.getYear() == currentDate.getYear()) {
            dateFormat = DATE_FORMAT;
        } else {
            dateFormat = DATE_FORMAT_2;
        }

        dateFormat.setTimeZone(timeZone);

        return dateFormat.format(dateTime.toDate());
    }

    public void add(E event) {
        add(events.size(), event);
    }

    public void add(int index, E event) {
        events.add(index, event);
        eventsMap.put(event.id, event);
    }

    public void addAll(List<E> events) {
        for (E event : events) {
            add(event);
        }
    }

    public void clear() {
        items.clear();
        eventsMap.clear();
        events.clear();
    }

    public boolean contains(E event) {
        return events.contains(event);
    }

    public boolean containsEvent(String id) {
        return eventsMap.containsKey(id);
    }

    public E get(int position) {
        return events.get(position);
    }

    public E getEvent(String eventId) {
        return eventsMap.get(eventId);
    }

    public int indexOf(E event) {
        return events.indexOf(event);
    }

    public boolean isEmpty() {
        return events.isEmpty();
    }

    public E last() {
        return events.get(events.size() - 1);
    }

    public int lastIndexOf(E event) {
        return events.lastIndexOf(event);
    }

    public void remove(E event) {
        events.remove(event);
        eventsMap.remove(event.id);
    }

    public void remove(int index) {
        events.remove(index);
    }

    public void removeItem(String id) {
        items.remove(id);
    }

    public void sort() {
        Collections.sort(events, comparator);
    }

    public List<EventFilter> getFilters() {
        return filters;
    }

    public void setFilters(List<EventFilter> filters) {
        this.filters.clear();
        this.filters.addAll(filters);
    }
}
