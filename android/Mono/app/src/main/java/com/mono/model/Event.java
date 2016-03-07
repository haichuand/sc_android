package com.mono.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.mono.util.Common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Event implements Parcelable {

    public static final String TYPE_CALENDAR = "calendar";
    public static final String TYPE_USERSTAY = "userstay";

    public final long id;
    public long externalId;
    public String type;
    public String title;
    public String description;
    public Location location;
    public int color;
    public long startTime;
    public long endTime;
    public long createTime;
    public List<Attendee> attendees = new ArrayList<>();

    public Event(long id) {
        this.id = id;
    }

    public Event(Event event) {
        id = event.id;
        externalId = event.externalId;
        type = event.type;
        title = event.title;
        description = event.description;

        if (event.location != null) {
            location = new Location(event.location);
        }

        color = event.color;
        startTime = event.startTime;
        endTime = event.endTime;
        createTime = event.createTime;

        Collections.copy(attendees, event.attendees);
    }

    protected Event(Parcel in) {
        id = in.readLong();
        externalId = in.readLong();
        type = in.readString();
        title = in.readString();
        description = in.readString();
        location = in.readParcelable(Location.class.getClassLoader());
        color = in.readInt();
        startTime = in.readLong();
        endTime = in.readLong();
        createTime = in.readLong();
        in.readTypedList(attendees, Attendee.CREATOR);
    }

    public static final Creator<Event> CREATOR = new Creator<Event>() {
        @Override
        public Event createFromParcel(Parcel in) {
            return new Event(in);
        }

        @Override
        public Event[] newArray(int size) {
            return new Event[size];
        }
    };

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

    public boolean equals(Event event) {
        if (id != event.id) {
            return false;
        }

        if (externalId != event.externalId) {
            return false;
        }

        if (!Common.compareStrings(type, event.type)) {
            return false;
        }

        if (!Common.compareStrings(title, event.title)) {
            return false;
        }

        if (!Common.compareStrings(description, event.description)) {
            return false;
        }

        if (location != null && !location.equals(event.location) ||
                location == null && event.location != null) {
            return false;
        }

        if (color != event.color) {
            return false;
        }

        if (startTime != event.startTime) {
            return false;
        }

        if (endTime != event.endTime) {
            return false;
        }

        if (createTime != event.createTime) {
            return false;
        }

        if (!attendees.equals(event.attendees)) {
            return false;
        }

        return true;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeLong(externalId);
        dest.writeString(type);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeParcelable(location, flags);
        dest.writeInt(color);
        dest.writeLong(startTime);
        dest.writeLong(endTime);
        dest.writeLong(createTime);
        dest.writeTypedList(attendees);
    }

    public long getDuration() {
        if (startTime > 0 && endTime > 0) {
            return endTime - startTime;
        }

        return 0;
    }
}
