package com.mono.model;

import com.mono.util.Common;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * This data structure is used to store information about a specific event group.
 *
 * @author Gary Ng
 */
public class EventGroup extends Instance {

    public String title;
    public LocalDate date;

    private List<Event> events = new ArrayList<>();

    public EventGroup(String id, String title, long time, DateTimeZone timeZone) {
        super(id);

        this.title = title;
        this.date = new LocalDate(time, timeZone);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof EventGroup)) {
            return false;
        }

        EventGroup eventGroup = (EventGroup) object;

        if (!Common.compareStrings(title, eventGroup.title)) {
            return false;
        }

        if (!date.isEqual(eventGroup.date)) {
            return false;
        }

        return true;
    }

    public void add(Event event, Comparator<Event> comparator) {
        if (!events.contains(event)) {
            events.add(event);

            if (comparator != null) {
                Collections.sort(events, comparator);
            }
        }
    }

    public List<Event> events() {
        return events;
    }

    public Event get(int index) {
        return events.get(index);
    }

    public int indexOf(Event event) {
        return events.indexOf(event);
    }

    public int indexOf(String eventId) {
        return events.indexOf(new Event(eventId));
    }

    public boolean isEmpty() {
        return events.isEmpty();
    }

    public void remove(Event event) {
        events.remove(event);
    }

    public int size() {
        return events.size();
    }

    public long getStartTime() {
        return events.get(events.size() - 1).startTime;
    }

    public long getEndTime() {
        return events.get(0).startTime;
    }
}
