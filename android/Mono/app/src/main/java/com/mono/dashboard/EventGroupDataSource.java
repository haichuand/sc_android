package com.mono.dashboard;

import android.content.Context;

import com.mono.R;
import com.mono.dashboard.EventGroupsFragment.EventGroup;
import com.mono.model.Event;
import com.mono.model.EventFilter;
import com.mono.util.Colors;
import com.mono.util.SimpleDataSource;

import org.joda.time.DateTimeZone;
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
public class EventGroupDataSource implements SimpleDataSource<EventGroupItem> {

    private static final SimpleDateFormat DATE_FORMAT;
    private static final SimpleDateFormat DATE_FORMAT_2;
    private static final SimpleDateFormat TIME_FORMAT;

    private Context context;

    private final Map<String, EventGroupItem> items = new HashMap<>();
    private final List<EventGroup> eventGroups = new ArrayList<>();
    private final Map<String, EventGroup> events = new HashMap<>();

    private Comparator<EventGroup> groupComparator;
    private int dateTimeColorId;
    private List<EventFilter> filters = new ArrayList<>();

    static {
        DATE_FORMAT = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());
        DATE_FORMAT_2 = new SimpleDateFormat("M/d/yy", Locale.getDefault());
        TIME_FORMAT = new SimpleDateFormat("h:mm a", Locale.getDefault());
    }

    public EventGroupDataSource(Context context, int dateTimeColorId) {
        this.context = context;

        groupComparator = new Comparator<EventGroup>() {
            @Override
            public int compare(EventGroup e1, EventGroup e2) {
                int value = Long.compare(e2.getStartTime(), e1.getStartTime());
                if (value != 0) {
                    return value;
                }

                return e2.id.compareToIgnoreCase(e1.id);
            }
        };

        this.dateTimeColorId = dateTimeColorId;
    }

    /**
     * Retrieve event groups as items to be displayed by the adapter.
     *
     * @param position Position of the event group.
     * @return item to display event group information.
     */
    @Override
    public EventGroupItem getItem(int position) {
        EventGroupItem item;

        EventGroup group = eventGroups.get(position);
        String id = group.id;

        if (items.containsKey(id)) {
            item = items.get(id);
        } else {
            item = new EventGroupItem(id);
            item.title = group.title;

            for (Event event : group.events) {
                EventItem tempItem;

                if (event.hasPhotos()) {
                    PhotoEventItem photoItem = new PhotoEventItem(event.id);
                    photoItem.photos = event.getPhotos();

                    tempItem = photoItem;
                } else {
                    tempItem = new EventItem(event.id);
                }

                tempItem.type = EventItem.TYPE_EVENT;
                tempItem.iconResId = R.drawable.circle;
                tempItem.iconColor = event.color;

                if (event.title != null && !event.title.isEmpty()) {
                    tempItem.title = event.title;
                } else {
                    tempItem.title = "(" + context.getString(R.string.no_subject) + ")";
                }

                tempItem.description = event.description;

                item.items.add(tempItem);
            }

            items.put(id, item);
        }
        // Date Display
        if (item != null) {
            Event event = group.events.get(0);
            DateTimeZone timeZone = event.allDay ? DateTimeZone.UTC : DateTimeZone.getDefault();

            item.date = getDateString(event.startTime, timeZone.toTimeZone());
            item.dateColor = Colors.getColor(context, dateTimeColorId);

            for (int i = 0; i < item.items.size(); i++) {
                EventItem tempItem = item.items.get(i);
                event = group.events.get(i);

                int colorId;
                boolean bold;

                if (event.viewTime == 0) {
                    colorId = R.color.gray_dark;
                    bold = true;
                } else {
                    colorId = R.color.gray_dark;
                    bold = false;
                }

                tempItem.titleColor = Colors.getColor(context, colorId);
                tempItem.titleBold = bold;

                if (event.allDay) {
                    tempItem.startDateTime = null;
                    tempItem.endDateTime = null;
                } else {
                    SimpleDateFormat dateFormat = TIME_FORMAT;
                    dateFormat.setTimeZone(TimeZone.getDefault());

                    LocalDateTime dateTime = new LocalDateTime(event.startTime);
                    tempItem.startDateTime = dateFormat.format(dateTime.toDate());

                    dateTime = new LocalDateTime(event.endTime);
                    tempItem.endDateTime = dateFormat.format(dateTime.toDate());
                }

                if (event.viewTime == 0) {
                    colorId = R.color.gray_dark;
                    bold = true;
                } else {
                    colorId = R.color.gray_light_3;
                    bold = false;
                }

                tempItem.startDateTimeColor = Colors.getColor(context, colorId);
                tempItem.endDateTimeColor = Colors.getColor(context, R.color.gray_light_3);
                tempItem.dateTimeBold = bold;
            }
        }

        return item;
    }

    /**
     * Helper function to convert milliseconds into a readable date string that takes time zone
     * into account.
     *
     * @param time Time in milliseconds.
     * @param timeZone Time zone to be used.
     * @return date string.
     */
    private String getDateString(long time, TimeZone timeZone) {
        LocalDate currentDate = new LocalDate();

        LocalDateTime dateTime = new LocalDateTime(time);
        LocalDate date = dateTime.toLocalDate();

        SimpleDateFormat dateFormat;

        if (date.isEqual(currentDate)) {
            return context.getString(R.string.today);
        } else if (date.getYear() == currentDate.getYear()) {
            dateFormat = DATE_FORMAT;
        } else {
            dateFormat = DATE_FORMAT_2;
        }

        dateFormat.setTimeZone(timeZone);

        return dateFormat.format(dateTime.toDate());
    }

    /**
     * Retrieve the number of event groups to be used by the adapter.
     *
     * @return number of event groups.
     */
    @Override
    public int getCount() {
        return eventGroups.size();
    }

    public void clear() {
        items.clear();
        eventGroups.clear();
        events.clear();
    }

    public boolean isEmpty() {
        return eventGroups.isEmpty();
    }

    public void removeItem(String id) {
        items.remove(id);
    }

    public void addGroup(EventGroup group) {
        eventGroups.add(group);
    }

    public void removeGroup(EventGroup group) {
        eventGroups.remove(group);
    }

    public boolean containsGroup(EventGroup group) {
        return eventGroups.contains(group);
    }

    public EventGroup getGroup(int position) {
        return eventGroups.get(position);
    }

    public EventGroup getGroupByEvent(String eventId) {
        return events.get(eventId);
    }

    public int indexOf(EventGroup group) {
        return eventGroups.indexOf(group);
    }

    public int lastIndexOf(EventGroup group) {
        return eventGroups.lastIndexOf(group);
    }

    public void sortGroups() {
        Collections.sort(eventGroups, groupComparator);
    }

    public void addEvent(Event event, EventGroup group, Comparator<Event> comparator) {
        group.add(event, comparator);
        events.put(event.id, group);
    }

    public void removeEvent(Event event, EventGroup group) {
        group.remove(event);
        events.remove(event.id);
        items.remove(group.id);
    }

    public boolean containsEvent(String id) {
        return events.containsKey(id);
    }

    public List<EventFilter> getFilters() {
        return filters;
    }

    public void setFilters(List<EventFilter> filters) {
        this.filters.clear();
        this.filters.addAll(filters);
    }
}
