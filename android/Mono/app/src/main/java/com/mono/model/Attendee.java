package com.mono.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.mono.util.Common;

/**
 * This data structure is used to store information about a specific attendee.
 *
 * @author Gary Ng, Xuejing Dong
 */
public class Attendee implements Parcelable {

    public final String id;
    public String mediaId;
    public String email;
    public String phoneNumber;
    public String firstName;
    public String lastName;
    public String userName;
    public int relationship;
    public int type;
    public int status;
    public boolean isFavorite;
    public boolean isFriend;
    public int isSuggested;

    public Attendee(long id) {
        this.id = String.valueOf(id);
    }

    public Attendee(String id) {
        this.id = id;
    }

    public Attendee(Attendee user) {
        this.id = user.id;
        this.mediaId = user.mediaId;
        this.email = user.email;
        this.phoneNumber = user.phoneNumber;
        this.firstName = user.firstName;
        this.lastName = user.lastName;
        this.userName = user.userName;
        this.relationship = user.relationship;
        this.type = user.type;
        this.status = user.status;
        this.isFavorite = user.isFavorite;
        this.isFriend = user.isFriend;
        this.isSuggested = user.isSuggested;
    }

    public Attendee(String id, String name, String email) {
        this.id = id;
        this.userName = name;
        this.email = email;
    }

    public Attendee(String id, String mediaId, String email, String phoneNumber, String firstName,
            String lastName, String userName, boolean isFavorite, boolean isFriend) {
        this.id = id;
        this.mediaId = mediaId;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.userName = userName;
        this.isFavorite = isFavorite;
        this.isFriend = isFriend;
    }

    protected Attendee(Parcel in) {
        id = in.readString();
        mediaId = in.readString();
        email = in.readString();
        phoneNumber = in.readString();
        firstName = in.readString();
        lastName = in.readString();
        userName = in.readString();
        relationship = in.readInt();
        type = in.readInt();
        status = in.readInt();
        isFavorite = in.readByte() != 0;
        isFriend = in.readByte() != 0;
        isSuggested = in.readInt();
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
    public boolean equals(Object object) {
        if (!(object instanceof Attendee)) {
            return false;
        }

        Attendee attendee = (Attendee) object;

        if (!Common.compareStrings(id, attendee.id)) {
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
        dest.writeString(mediaId);
        dest.writeString(email);
        dest.writeString(phoneNumber);
        dest.writeString(firstName);
        dest.writeString(lastName);
        dest.writeString(userName);
        dest.writeInt(relationship);
        dest.writeInt(type);
        dest.writeInt(status);
        dest.writeByte((byte) (isFavorite ? 1 : 0));
        dest.writeByte((byte) (isFriend ? 1 : 0));
        dest.writeInt(isSuggested);
    }

    @Override
    public String toString() {
        if (firstName != null && !firstName.isEmpty()) {
            if (lastName != null && !lastName.isEmpty()) {
                return firstName + " " + lastName;
            }
            return firstName;
        } else if (userName != null && !userName.isEmpty()) {
            return userName;
        } else {
            return email;
        }
    }
}
