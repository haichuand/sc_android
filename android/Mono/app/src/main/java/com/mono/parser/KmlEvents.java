package com.mono.parser;

/**
 * Created by anu on 8/29/2016.
 */
public class KmlEvents {

    private String name;
    private String address;
    private String distance;
    private double lat;
    private double lng;
    private long startTime;
    private long endTime;

    public KmlEvents () {

    }

    public KmlEvents (String name, String address, String distance,double latitude, double longitude, long startTime, long endTime) {
        this.name = name;
        this.address = address;
        this.distance = distance;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public String toString() {
        String result = lat+" "+lng+" "+startTime+" "+endTime+" end";
        return result;
    }
}

