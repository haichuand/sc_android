package com.mono.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.mono.util.Common;

/**
 * This data structure is used to store information about a specific media.
 *
 * @author Gary Ng
 */
public class Media implements Parcelable {

    public static final String IMAGE = "image/*";
    public static final String VIDEO = "video/*";

    public long id;
    public Uri uri;
    public String type;
    public long size;
    public byte[] thumbnail;

    public Media(long id) {
        this.id = id;
    }

    public Media(Uri uri, String type, long size) {
        this.uri = uri;
        this.type = type;
        this.size = size;
    }

    protected Media(Parcel in) {
        id = in.readLong();
        uri = in.readParcelable(Uri.class.getClassLoader());
        type = in.readString();
        size = in.readLong();
        thumbnail = in.createByteArray();
    }

    public static final Creator<Media> CREATOR = new Creator<Media>() {
        @Override
        public Media createFromParcel(Parcel in) {
            return new Media(in);
        }

        @Override
        public Media[] newArray(int size) {
            return new Media[size];
        }
    };

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Media)) {
            return false;
        }

        Media media = (Media) object;

        if (id > 0 && id == media.id) {
            return true;
        }

        if (uri != null && uri.equals(media.uri) && Common.compareStrings(type, media.type)) {
            return true;
        }

        return false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeParcelable(uri, flags);
        dest.writeString(type);
        dest.writeLong(size);
        dest.writeByteArray(thumbnail);
    }
}
