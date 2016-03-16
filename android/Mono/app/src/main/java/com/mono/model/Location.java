package com.mono.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.mono.util.Common;

import java.util.Arrays;

public class Location implements Parcelable {

    public final long id;
    public String name;
    public Double latitude;
    public Double longitude;
    public String[] address;
    public String googlePlaceId;

    public Location(long id) {
        this.id = id;
    }

    public Location(String name, String googlePlaceId, Double latitude, Double longitude,
            String[] address) {
        this(-1);
        this.name = name;
        this.googlePlaceId = googlePlaceId;
        this.latitude = latitude;
        this.longitude = longitude;
        if (address != null) {
            this.address = Arrays.copyOf(address, address.length);
        }
    }

    public Location(String name) {
        this(-1);
        this.name = name;
    }

    public Location(double latitude, double longitude) {
        this(-1);
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Location(Location location) {
        id = location.id;
        name = location.name;
        latitude = location.latitude;
        longitude = location.longitude;

        if (location.address != null) {
            address = Arrays.copyOf(location.address, location.address.length);
        }

        googlePlaceId = location.googlePlaceId;
    }

    protected Location(Parcel in) {
        id = in.readLong();
        name = in.readString();
        address = in.createStringArray();
        googlePlaceId = in.readString();
    }

    public static final Creator<Location> CREATOR = new Creator<Location>() {
        @Override
        public Location createFromParcel(Parcel in) {
            return new Location(in);
        }

        @Override
        public Location[] newArray(int size) {
            return new Location[size];
        }
    };

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Location)) {
            return false;
        }

        Location location = (Location) object;

        if (!Common.compareStrings(name, location.name)) {
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
        dest.writeString(name);
        dest.writeStringArray(address);
        dest.writeString(googlePlaceId);
    }

    public boolean containsLatLng() {
        return latitude != null && longitude != null;
    }

    public String getAddress() {
        String str = "";

        if (address != null) {
            for (int i = 0; i < address.length; i++) {
                if (i > 0) str += ", ";
                str += address[i];
            }
        }

        return str;
    }
    //for database testing purpose
    public String toString() {
        return "Location: location_id: "+this.id+", name: "+this.name + ", address: "+this.getAddress() + ", lattitude :"+ this.latitude
                + ", longitude: "+ this.longitude+", googleId: "+this.googlePlaceId;
    }
}
