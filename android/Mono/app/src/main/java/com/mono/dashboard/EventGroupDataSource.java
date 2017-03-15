package com.mono.dashboard;

import android.content.Context;

import com.mono.R;
import com.mono.model.Event;
import com.mono.model.EventGroup;
import com.mono.util.Colors;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Locale;
import java.util.TimeZone;

/**
 * This data source class is to be used by an adapter to display its content.
 *
 * @author Gary Ng
 */
public class EventGroupDataSource extends BaseEventsDataSource<EventGroup, EventGroupItem> {

    public EventGroupDataSource(Context context, int dateTimeColorId) {
        super(context, dateTimeColorId);

        DATE_FORMAT = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());

        comparator = new Comparator<EventGroup>() {
            @Override
            public int compare(EventGroup e1, EventGroup e2) {
                int value = Long.compare(e2.getStartTime(), e1.getStartTime());
                if (value != 0) {
                    return value;
                }

                return e2.id.compareToIgnoreCase(e1.id);
            }
        };
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

        EventGroup group = events.get(position);
        String id = group.id;

        if (items.containsKey(id)) {
            item = items.get(id);
        } else {
            item = new EventGroupItem(id);
            item.title = group.title;

            for (Event event : group.events()) {
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
            Event event = group.get(0);
            DateTimeZone timeZone = event.allDay ? DateTimeZone.UTC : DateTimeZone.getDefault();

            item.date = getDateString(event.startTime, timeZone.toTimeZone(), event.allDay);
            item.dateColor = Colors.getColor(context, dateTimeColorId);

            for (int i = 0; i < item.items.size(); i++) {
                EventItem tempItem = item.items.get(i);
                event = group.get(i);

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

    public void addEvent(Event event, EventGroup group, Comparator<Event> comparator) {
        group.add(event, comparator);
        eventsMap.put(event.id, group);
    }

    public void removeEvent(String id, EventGroup group) {
        group.remove(new Event(id));
        eventsMap.remove(id);
    }
}
