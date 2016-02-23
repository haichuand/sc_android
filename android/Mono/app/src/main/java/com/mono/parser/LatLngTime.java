package com.mono.parser;

/**
 * Created by xuejing on 2/17/16.
 */
class LatLngTime {
    private double lat;
    private double lng;
    private long startTime;
    private long endTime;

    public LatLngTime () {

    }

    public LatLngTime (double latitude, double longitude, long startTime, long endTime) {
        this.lat = latitude;
        this.lng = longitude;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String toString() {
        String result = lat+" "+lng+" "+startTime+" "+endTime+" end";
        return result;
    }
}
