package com.mono.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.mono.util.Common;

import java.util.Arrays;

/**
 * This data structure is used to store information about a specific location.
 *
 * @author Gary Ng, Xuejing Dong
 */
public class Location implements Parcelable {

    public long id;
    public String name;
    public double[] latLng;
    public String[] address;
    public String googlePlaceId;

    public Location(long id) {
        this.id = id;
    }

    public Location(String name) {
        this.name = name;
    }

    public Location(String name, String googlePlaceId, double latitude, double longitude,
            String[] address) {
        this.name = name;
        this.googlePlaceId = googlePlaceId;

        setLatLng(latitude, longitude);

        if (address != null) {
            this.address = Arrays.copyOf(address, address.length);
        }
    }

    public Location(double latitude, double longitude) {
        setLatLng(latitude, longitude);
    }

    public Location(Location location) {
        id = location.id;
        name = location.name;
        latLng = location.latLng;

        if (location.address != null) {
            address = Arrays.copyOf(location.address, location.address.length);
        }

        googlePlaceId = location.googlePlaceId;
    }

    protected Location(Parcel in) {
        id = in.readLong();
        name = in.readString();
        latLng = in.createDoubleArray();
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

        if (id > 0 && id == location.id) {
            return true;
        }

        if (Common.compareStrings(name, location.name)) {
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
        dest.writeString(name);
        dest.writeDoubleArray(latLng);
        dest.writeStringArray(address);
        dest.writeString(googlePlaceId);
    }

    public void setLatLng(double latitude, double longitude) {
        latLng = new double[]{latitude, longitude};
    }

    public boolean containsLatLng() {
        return latLng != null;
    }

    public Double getLatitude() {
        return latLng != null ? latLng[0] : null;
    }

    public Double getLongitude() {
        return latLng != null ? latLng[1] : null;
    }

    public String getAddress() {
        if (address != null && address.length > 0) {
            return Common.implode(", ", address);
        }

        return null;
    }

    public void setAddress(String[] address) {
        if (address != null) {
            this.address = Arrays.copyOf(address, address.length);
        }
    }

    //for database testing purpose
    public String toString() {
        return "Location: location_id: "+this.id+", name: "+this.name + ", address: "+this.getAddress() + ", lattitude :"+ getLatitude()
                + ", longitude: "+ getLongitude()+", googleId: "+this.googlePlaceId;
    }
}
