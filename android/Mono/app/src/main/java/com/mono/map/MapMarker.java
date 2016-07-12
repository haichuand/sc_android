package com.mono.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Handler;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.mono.R;
import com.mono.model.Event;
import com.mono.util.Colors;
import com.mono.util.Common;
import com.mono.util.Pixels;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to encapsulate the map marker used for Google Maps. In addition to the
 * marker, important information that describes this marker such as the event it is representing
 * is stored here for reference.
 *
 * @author Gary Ng
 */
public class MapMarker {

    private static final int DEFAULT_MARKER_COLOR = R.color.red;
    private static final int POINT_MARKER_DIMENSION_DP = 12;

    private static final int ANIMATION_DELAY_MS = 16;
    private static final int ANIMATION_DURATION = 300;
    private static final int ANIMATION_START_Y = 200;

    public final String id;
    private Marker marker;

    protected String name;
    protected LatLng position;
    protected long startTime;
    protected long endTime;
    protected long duration;

    protected Circle circle;
    protected int circleStrokeColor;

    protected final List<Event> events = new ArrayList<>();

    private Handler handler;
    private Runnable handlerCallback;

    public MapMarker(String id, Event event, LatLng position) {
        this.id = id;
        this.position = position;

        add(event);
    }

    public Marker getActualMarker() {
        return marker;
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

        if (Common.compareStrings(id, marker.id)) {
            return true;
        }

        return false;
    }

    public void remove() {
        if (marker != null) {
            marker.remove();
        }

        if (circle != null) {
            circle.remove();
        }
    }

    public void add(Event event) {
        events.add(event);
    }

    public Event get(int position) {
        return events.get(position);
    }

    public boolean remove(Event event) {
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

    /**
     * Create and place a standard place marker on the map.
     *
     * @param context The value of the context.
     * @param map The instance of the map.
     * @param iconResId The custom icon resource ID.
     * @param color The color value.
     * @param animate The value to perform an animation.
     */
    public void drawPlaceMarker(Context context, GoogleMap map, int iconResId, int color,
            boolean animate) {
        if (marker != null) {
            marker.remove();
        }

        MarkerOptions options = new MarkerOptions();
        options.position(position);

        if (iconResId == 0) {
            iconResId = R.drawable.ic_place;
        }

        Drawable drawable = context.getDrawable(iconResId);

        if (drawable != null) {
            float scale = 1.5f;

            Bitmap bitmap = Bitmap.createBitmap(Math.round(drawable.getIntrinsicWidth() * scale),
                Math.round(drawable.getIntrinsicHeight() * scale), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());

            if (color == 0) {
                color = Colors.getColor(context, DEFAULT_MARKER_COLOR);
            }
            drawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);

            drawable.draw(canvas);

            BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
            options.icon(descriptor);
        }

        marker = map.addMarker(options);

        if (animate) {
            animate(map.getProjection());
        }
    }

    /**
     * Create and place a circular point marker on the map.
     *
     * @param context The value of the context.
     * @param map The instance of the map.
     * @param color The color value.
     * @param animate The value to perform an animation.
     */
    public void drawPointMarker(Context context, GoogleMap map, int color, boolean animate) {
        if (marker != null) {
            marker.remove();
        }

        MarkerOptions options = new MarkerOptions();
        options.anchor(0.5f, 0.5f);
        options.position(position);

        Drawable drawable = context.getDrawable(R.drawable.circle_solid);

        if (drawable != null) {
            int dimension = Pixels.pxFromDp(context, POINT_MARKER_DIMENSION_DP);
            Bitmap bitmap = Bitmap.createBitmap(dimension, dimension, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());

            if (color == 0) {
                color = Colors.getColor(context, DEFAULT_MARKER_COLOR);
            }
            drawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);

            drawable.draw(canvas);

            BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
            options.icon(descriptor);
        }

        marker = map.addMarker(options);
    }

    /**
     * Perform a dropping down animation towards the designated position.
     *
     * @param projection The value of the map projection.
     */
    public void animate(Projection projection) {
        if (handler != null) {
            handler.removeCallbacks(handlerCallback);
        } else {
            handler = new Handler();
        }

        final LatLng target = marker.getPosition();
        Point startPoint = projection.toScreenLocation(target);
        startPoint.y -= ANIMATION_START_Y;

        final LatLng startLatLng = projection.fromScreenLocation(startPoint);
        marker.setPosition(startLatLng);

        final long startTime = System.currentTimeMillis();
        final Interpolator interpolator = new LinearInterpolator();

        handler.post(handlerCallback = new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startTime;
                float t = interpolator.getInterpolation((float) elapsed / ANIMATION_DURATION);

                double latitude = t * target.latitude + (1 - t) * startLatLng.latitude;
                double longitude = t * target.longitude + (1 - t) * startLatLng.longitude;
                marker.setPosition(new LatLng(latitude, longitude));

                if (t < 1) {
                    // Start Next "Frame"
                    handler.postDelayed(this, ANIMATION_DELAY_MS);
                } else {
                    // On Finish, Set Position
                    marker.setPosition(target);
                }
            }
        });
    }
}
