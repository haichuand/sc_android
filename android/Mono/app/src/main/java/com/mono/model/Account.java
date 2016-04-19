package com.mono.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Account implements Parcelable {

    public final long id;
    public String username;
    public String firstName;
    public String lastName;

    public Account(long id) {
        this.id = id;
    }

    protected Account(Parcel in) {
        id = in.readLong();
        username = in.readString();
        firstName = in.readString();
        lastName = in.readString();
    }

    public static final Creator<Account> CREATOR = new Creator<Account>() {
        @Override
        public Account createFromParcel(Parcel in) {
            return new Account(in);
        }

        @Override
        public Account[] newArray(int size) {
            return new Account[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(username);
        dest.writeString(firstName);
        dest.writeString(lastName);
    }
}
