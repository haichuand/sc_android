package com.mono.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.mono.util.Common;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * This data structure is used to store information about a specific event.
 *
 * @author Gary Ng
 */
public class Event implements Parcelable {

    public static final String TYPE_CALENDAR = "calendar";
    public static final String TYPE_USERSTAY = "userstay";

    public static final int SOURCE_DATABASE = 0;
    public static final int SOURCE_PROVIDER = 1;

    public String id;
    public String parentId;
    public int source;
    public long calendarId;
    public long internalId;
    public String externalId;
    public String type;
    public String title;
    public String description;
    public Location location;
    public int color;
    public long startTime;
    public long endTime;
    public String timeZone;
    public String endTimeZone;
    public boolean allDay;
    public long lastRepeatTime;
    public long createTime;
    public long updateTime;
    public List<Attendee> attendees = new ArrayList<>();
    public List<Reminder> reminders = new ArrayList<>();
    public List<Media> photos = new ArrayList<>();

    public Event() {
        id = null;
    }

    public Event(String id) {
        this.id = id;
    }

    public Event(Event event) {
        id = event.id;
        parentId = event.parentId;
        source = event.source;
        calendarId = event.calendarId;
        internalId = event.internalId;
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
        timeZone = event.timeZone;
        endTimeZone = event.endTimeZone;
        allDay = event.allDay;
        createTime = event.createTime;
        updateTime = event.updateTime;

        for (Attendee attendee : event.attendees) {
            attendees.add(new Attendee(attendee));
        }

        for (Reminder reminder : event.reminders) {
            reminders.add(new Reminder(reminder));
        }

        for (Media photo : event.photos) {
            photos.add(photo);
        }
    }

    protected Event(Parcel in) {
        id = in.readString();
        parentId = in.readString();
        source = in.readInt();
        calendarId = in.readLong();
        internalId = in.readLong();
        externalId = in.readString();
        type = in.readString();
        title = in.readString();
        description = in.readString();
        location = in.readParcelable(Location.class.getClassLoader());
        color = in.readInt();
        startTime = in.readLong();
        endTime = in.readLong();
        timeZone = in.readString();
        endTimeZone = in.readString();
        allDay = in.readByte() != 0;
        createTime = in.readLong();
        updateTime = in.readLong();
        in.readTypedList(attendees, Attendee.CREATOR);
        in.readTypedList(reminders, Reminder.CREATOR);
        in.readTypedList(photos, Media.CREATOR);
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

        if (!Common.compareStrings(id, event.id)) {
            return false;
        }

        return true;
    }

    public boolean equals(Event event) {
        if (!Common.compareStrings(id, event.id)) {
            return false;
        }

        if (calendarId != event.calendarId) {
            return false;
        }

        if (internalId != event.internalId) {
            return false;
        }

        if (!Common.compareStrings(externalId, event.externalId)) {
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

        if (!Common.compareStrings(timeZone, event.timeZone)) {
            return false;
        }

        if (!Common.compareStrings(endTimeZone, event.endTimeZone)) {
            return false;
        }

        if (allDay != event.allDay) {
            return false;
        }

        if (createTime != event.createTime) {
            return false;
        }

        if (!attendees.equals(event.attendees)) {
            return false;
        }

//        if (!reminders.equals(event.reminders)) {
//            return false;
//        }

        if (!photos.equals(event.photos)) {
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
        dest.writeString(id);
        dest.writeString(parentId);
        dest.writeInt(source);
        dest.writeLong(calendarId);
        dest.writeLong(internalId);
        dest.writeString(externalId);
        dest.writeString(type);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeParcelable(location, flags);
        dest.writeInt(color);
        dest.writeLong(startTime);
        dest.writeLong(endTime);
        dest.writeString(timeZone);
        dest.writeString(endTimeZone);
        dest.writeInt(allDay ? 1 : 0);
        dest.writeLong(createTime);
        dest.writeLong(updateTime);
        dest.writeTypedList(attendees);
        dest.writeTypedList(reminders);
        dest.writeTypedList(photos);
    }

    public long getDuration() {
        if (startTime > 0 && endTime > 0) {
            return endTime - startTime;
        }

        return 0;
    }

    public String getEndTimeZone() {
        return endTimeZone == null ? timeZone : endTimeZone;
    }

    public List<String> getAttendeeIdList () {
        List<String> attendeeIdList = new LinkedList<>();
        for (Attendee attendee : attendees) {
            attendeeIdList.add(attendee.id);
        }
        return attendeeIdList;
    }
}
