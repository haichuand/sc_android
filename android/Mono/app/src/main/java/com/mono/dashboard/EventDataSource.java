package com.mono.dashboard;

import android.content.Context;

import com.mono.R;
import com.mono.model.Event;
import com.mono.util.Colors;

import org.joda.time.DateTimeZone;

import java.util.Comparator;

/**
 * This data source class is to be used by an adapter to display its content.
 *
 * @author Gary Ng
 */
public class EventDataSource extends BaseEventsDataSource<Event, EventItem> {

    public EventDataSource(Context context, int dateTimeColorId) {
        super(context, dateTimeColorId);

        comparator = new Comparator<Event>() {
            @Override
            public int compare(Event e1, Event e2) {
                int value = Long.compare(e2.startTime, e1.startTime);
                if (value != 0) {
                    return value;
                }

                return e2.id.compareToIgnoreCase(e1.id);
            }
        };
    }

    /**
     * Retrieve events as items to be displayed by the adapter. Special case events such as one
     * with photos will return as a different type of item to be displayed differently.
     *
     * @param position Position of the event.
     * @return item to display event information.
     */
    @Override
    public EventItem getItem(int position) {
        EventItem item;

        Event event = events.get(position);
        String id = event.id;

        if (items.containsKey(id)) {
            item = items.get(id);
        } else {
            if (event.hasPhotos()) {
                PhotoEventItem photoItem = new PhotoEventItem(id);
                photoItem.photos = event.getPhotos();

                item = photoItem;
            } else {
                item = new EventItem(id);
            }

            item.type = EventItem.TYPE_EVENT;
            item.iconResId = R.drawable.circle;
            item.iconColor = event.color;

            if (event.title != null && !event.title.isEmpty()) {
                item.title = event.title;
            } else {
                item.title = "(" + context.getString(R.string.no_subject) + ")";
            }

            item.description = event.description;

            items.put(id, item);
        }
        // Date Display
        if (item != null) {
            int colorId;
            boolean bold;

            if (event.viewTime == 0) {
                colorId = R.color.gray_dark;
                bold = true;
            } else {
                colorId = R.color.gray_dark;
                bold = false;
            }

            item.titleColor = Colors.getColor(context, colorId);
            item.titleBold = bold;

            DateTimeZone timeZone = event.allDay ? DateTimeZone.UTC : DateTimeZone.getDefault();
            item.startDateTime = getDateString(event.startTime, timeZone.toTimeZone(),
                event.allDay);

            if (event.viewTime == 0) {
                colorId = dateTimeColorId;
                bold = true;
            } else {
                colorId = R.color.gray_light_3;
                bold = false;
            }

            item.startDateTimeColor = Colors.getColor(context, colorId);
            item.dateTimeBold = bold;
        }

        return item;
    }
}
