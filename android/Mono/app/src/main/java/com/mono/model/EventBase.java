package com.mono.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * This data structure is used to store information about a specific event.
 *
 * @author Gary Ng
 */
public class EventBase implements Parcelable {

    public long baseId;
    public int source;
    public String eventId;
    public String syncId;
    public boolean favorite;
    public long createTime;
    public long modifyTime;
    public long viewTime;
    public long syncTime;

    public List<Media> photos = new ArrayList<>();

    public EventBase(long baseId, int source, String eventId, String syncId) {
        this.baseId = baseId;
        this.source = source;
        this.eventId = eventId;
        this.syncId = syncId;
    }

    public EventBase(EventBase eventBase) {
        baseId = eventBase.baseId;
        source = eventBase.source;
        eventId = eventBase.eventId;
        syncId = eventBase.syncId;
        favorite = eventBase.favorite;
        createTime = eventBase.createTime;
        modifyTime = eventBase.modifyTime;
        viewTime = eventBase.viewTime;
        syncTime = eventBase.syncTime;
    }

    protected EventBase(Parcel in) {
        baseId = in.readLong();
        source = in.readInt();
        eventId = in.readString();
        syncId = in.readString();
        favorite = in.readByte() != 0;
        createTime = in.readLong();
        modifyTime = in.readLong();
        viewTime = in.readLong();
        syncTime = in.readLong();

        in.readTypedList(photos, Media.CREATOR);
    }

    public static final Creator<EventBase> CREATOR = new Creator<EventBase>() {
        @Override
        public EventBase createFromParcel(Parcel in) {
            return new EventBase(in);
        }

        @Override
        public EventBase[] newArray(int size) {
            return new EventBase[size];
        }
    };

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof EventBase)) {
            return false;
        }

        EventBase eventBase = (EventBase) object;

        if (baseId != eventBase.baseId) {
            return false;
        }

        return true;
    }

    public boolean equals(EventBase eventBase) {
        if (!photos.equals(eventBase.photos)) {
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
        dest.writeLong(baseId);
        dest.writeInt(source);
        dest.writeString(eventId);
        dest.writeString(syncId);
        dest.writeInt(favorite ? 1 : 0);
        dest.writeLong(createTime);
        dest.writeLong(modifyTime);
        dest.writeLong(viewTime);
        dest.writeLong(syncTime);

        dest.writeTypedList(photos);
    }
}
