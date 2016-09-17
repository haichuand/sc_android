package com.mono.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This data structure is used to store information about a specific reminder.
 *
 * @author Gary Ng
 */
public class Reminder implements Parcelable {

    public final long id;
    public int minutes;
    public int method;

    public Reminder() {
        id = -1;
    }

    public Reminder(long id) {
        this.id = id;
    }

    public Reminder(Reminder reminder) {
        id = reminder.id;
        minutes = reminder.minutes;
        method = reminder.method;
    }

    protected Reminder(Parcel in) {
        id = in.readLong();
        minutes = in.readInt();
        method = in.readInt();
    }

    public static final Creator<Reminder> CREATOR = new Creator<Reminder>() {
        @Override
        public Reminder createFromParcel(Parcel in) {
            return new Reminder(in);
        }

        @Override
        public Reminder[] newArray(int size) {
            return new Reminder[size];
        }
    };

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Reminder)) {
            return false;
        }

        Reminder reminder = (Reminder) object;

        if (minutes != reminder.minutes || method != reminder.method) {
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
        dest.writeInt(minutes);
        dest.writeInt(method);
    }
}
