package com.mono.model;

import com.mono.util.Common;

public class Location {

    public final long id;
    public String name;
    public Double latitude;
    public Double longitude;
    public String[] address;

    public Location(long id) {
        this.id = id;
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
}
