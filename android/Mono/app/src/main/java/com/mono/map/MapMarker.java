package com.mono.map;

import android.graphics.Color;
import android.location.Location;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.mono.model.Event;

import java.util.ArrayList;
import java.util.List;

public class MapMarker<T extends Event> {

    public final String id;
    public Marker marker;

    protected String name;
    protected LatLng position;
    protected long startTime;
    protected long endTime;
    protected long duration;

    protected Circle circle;
    protected int circleStrokeColor;

    protected final List<T> events = new ArrayList<>();

    public MapMarker(String id, Marker marker) {
        this.id = id;
        this.marker = marker;
    }

    public MapMarker(String id, Marker marker, T event) {
        this(id, marker);
        add(event);
    }

    public String getName() {
        return name;
    }

    public LatLng getPosition() {
        return position;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public long getDuration() {
        return duration;
    }

    public void onMarkerClick(boolean state) {
        if (circle != null) {
            circle.setStrokeColor(state ? Color.WHITE : circleStrokeColor);
            circle.setZIndex(state ? 1 : 0);
        }
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof MapMarker)) {
            return false;
        }

        MapMarker marker = (MapMarker) object;

        if (id != marker.id) {
            return false;
        }

        return true;
    }

    public void clear() {
        if (marker != null) {
            marker.remove();
        }

        if (circle != null) {
            circle.remove();
        }
    }

    public void add(T event) {
        events.add(event);
    }

    public T get(int position) {
        return events.get(position);
    }

    public boolean remove(T event) {
        return events.remove(event);
    }

    public LatLng center(MapMarker marker) {
        LatLng current = getPosition(), target = marker.getPosition();
        return new LatLng((target.latitude + current.latitude) / 2,
            (target.longitude + current.longitude) / 2);
    }

    public float distanceTo(MapMarker marker) {
        LatLng current = getPosition(), target = marker.getPosition();
        float[] results = new float[3];

        Location.distanceBetween(current.latitude, current.longitude, target.latitude,
            target.longitude, results);

        return results[0];
    }

    public void setCircle(Circle circle) {
        this.circle = circle;
        circleStrokeColor = circle.getStrokeColor();
    }

    public void updateCircle(LatLng center, double radius, int color, int strokeColor) {
        circle.setCenter(center);
        circle.setRadius(radius);
        circle.setFillColor(color);
        circle.setStrokeColor(circleStrokeColor = strokeColor);
    }
}
