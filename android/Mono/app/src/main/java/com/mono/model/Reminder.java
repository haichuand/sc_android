package com.mono.model;

import android.os.Parcel;
import android.os.Parcelable;

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
