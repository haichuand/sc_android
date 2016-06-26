package com.mono.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This data structure is used to store information about the user account.
 *
 * @author Gary Ng
 */
public class Account implements Parcelable {

    public static final int STATUS_NONE = 0;
    public static final int STATUS_ONLINE = 1;

    public final long id;
    public String username;
    public String firstName;
    public String lastName;
    public String email;
    public String phone;
    public int status;

    public Account(long id) {
        this.id = id;
    }

    public Account(long id, String email, String phone, String firstName, String lastName,
            String username) {
        this.id = id;
        this.email = email;
        this.phone = phone;
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
    }

    protected Account(Parcel in) {
        id = in.readLong();
        username = in.readString();
        firstName = in.readString();
        lastName = in.readString();
        email = in.readString();
        phone = in.readString();
        status = in.readInt();
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
        dest.writeString(email);
        dest.writeString(phone);
        dest.writeInt(status);
    }
}
