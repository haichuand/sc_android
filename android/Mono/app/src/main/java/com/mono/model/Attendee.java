package com.mono.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Attendee implements Parcelable {

    public final long id;
    public String name;
    public String email;

    public Attendee(long id) {
        this.id = id;
    }

    public Attendee(Attendee attendee) {
        id = attendee.id;
        name = attendee.name;
        email = attendee.email;
    }

    protected Attendee(Parcel in) {
        id = in.readLong();
        name = in.readString();
        email = in.readString();
    }

    public static final Creator<Attendee> CREATOR = new Creator<Attendee>() {
        @Override
        public Attendee createFromParcel(Parcel in) {
            return new Attendee(in);
        }

        @Override
        public Attendee[] newArray(int size) {
            return new Attendee[size];
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
        dest.writeString(email);
    }
}
