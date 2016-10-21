package com.mono.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * This data structure is used to store information about a specific calendar.
 *
 * @author Gary Ng
 */
public class Calendar implements Parcelable {

    public final long id;
    public String name;
    public String timeZone;
    public String owner;
    public String description;
    public int color;

    public String accountName;
    public String accountType;
    public boolean primary;
    public boolean local;

    public List<Event> events = new ArrayList<>();

    public Calendar(long id) {
        this.id = id;
    }

    public Calendar(long id, String name) {
        this(id);
        this.name = name;
    }

    protected Calendar(Parcel in) {
        id = in.readLong();
        name = in.readString();
        timeZone = in.readString();
        owner = in.readString();
        description = in.readString();
        color = in.readInt();
        accountName = in.readString();
        accountType = in.readString();
        primary = in.readInt() > 0;
        local = in.readInt() > 0;
        in.readTypedList(events, Event.CREATOR);
    }

    public static final Creator<Calendar> CREATOR = new Creator<Calendar>() {
        @Override
        public Calendar createFromParcel(Parcel in) {
            return new Calendar(in);
        }

        @Override
        public Calendar[] newArray(int size) {
            return new Calendar[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(name);
        dest.writeString(timeZone);
        dest.writeString(owner);
        dest.writeString(description);
        dest.writeInt(color);
        dest.writeString(accountName);
        dest.writeString(accountType);
        dest.writeInt(primary ? 1 : 0);
        dest.writeInt(local ? 1 : 0);
        dest.writeTypedList(events);
    }
}
