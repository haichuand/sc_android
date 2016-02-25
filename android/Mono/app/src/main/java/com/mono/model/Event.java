package com.mono.model;

import java.util.List;

public class Event {

    public static final String TYPE_CALENDAR = "calendar";
    public static final String TYPE_USERSTAY = "userstay";

    public final long id;
    public long externalId;
    public String title;
    public String description;
    public Location location;
    public int color;
    public long startTime;
    public long endTime;
    public long createTime;
    public List<Attendee> attendees;
    public String type; //calendar or userstay

    public Event(long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Event)) {
            return false;
        }

        Event event = (Event) object;

        if (id != event.id) {
            return false;
        }

        return true;
    }

    public long getDuration() {
        if (startTime > 0 && endTime > 0) {
            return endTime - startTime;
        }

        return 0;
    }
}
