package com.mono.util;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.AsyncTask;

import com.google.android.gms.maps.model.LatLng;
import com.mono.model.Location;
import com.mono.util.NetworkHelper.NetworkTask;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * This class is used to provide basic location needs such as retrieving the last known location
 * from the device.
 *
 * @author Gary Ng
 */
public class LocationHelper {

    private static final int MAX_RESULTS = 1;

    private LocationHelper() {}

    /**
     * Check if GPS is enabled.
     *
     * @param context Context of application.
     * @return whether GPS is enabled.
     */
    public static boolean isGPSEnabled(Context context) {
        LocationManager manager =
            (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /**
     * Retrieve bounds with the given coordinates and radius.
     *
     * @param latitude Latitude at the center.
     * @param longitude Longitude at the center.
     * @param radius Radius in degrees.
     * @return bound array.
     */
    public static double[] getLatLngBounds(double latitude, double longitude, float radius) {
        double minLat = latitude - radius;
        if (minLat < -90) minLat = minLat % 90 + 90;

        double minLong = longitude - radius;
        if (minLong < -180) minLong = minLong % 180 + 180;

        double maxLat = latitude + radius;
        if (maxLat > 90) maxLat = maxLat % 90 - 90;

        double maxLong = longitude + radius;
        if (maxLong > 180) maxLong = maxLong % 180 - 180;

        return new double[]{
            minLat, minLong,
            maxLat, maxLong
        };
    }

    /**
     * Retrieve last known latitude and longitude cached on the device.
     *
     * @param context Context of application.
     * @return latitude and longitude.
     */
    public static LatLng getLastKnownLatLng(Context context) {
        LatLng latLng = null;

        try {
            LocationManager manager =
                (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            // Retrieve Location Providers
            List<String> providers = manager.getProviders(true);
            android.location.Location location = null;
            // Determine Best Location
            for (String provider : providers) {
                android.location.Location tempLocation = manager.getLastKnownLocation(provider);
                if (tempLocation == null) {
                    continue;
                }

                if (location == null || tempLocation.getAccuracy() < location.getAccuracy()) {
                    location = tempLocation;
                }
            }
            // Extract Latitude and Longitude
            if (location != null) {
                latLng = new LatLng(location.getLatitude(), location.getLongitude());
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        return latLng;
    }

    /**
     * Retrieve last known latitude and longitude cached on the device in the background.
     *
     * @param context Context of application.
     * @param callback Callback for latitude and longitude.
     */
    public static void getLastKnownLatLng(Context context, LocationCallback callback) {
        new AsyncTask<Object, Void, LatLng>() {
            private LocationCallback callback;

            @Override
            protected LatLng doInBackground(Object... params) {
                Context context = (Context) params[0];
                callback = (LocationCallback) params[1];

                return getLastKnownLatLng(context);
            }

            @Override
            protected void onPostExecute(LatLng result) {
                Location location = null;

                if (result != null) {
                    location = new Location(result.latitude, result.longitude);
                }

                callback.onFinish(location);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, context, callback);
    }

    /**
     * Retrieve location description with the given latitude and longitude.
     *
     * @param context Context of application.
     * @param latitude Latitude of location.
     * @param longitude Longitude of location.
     * @return instance of location.
     * @throws IOException
     */
    public static Location getLocationFromLatLng(Context context, double latitude,
            double longitude) throws IOException {
        Location location = null;

        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        List<Address> addresses =
            geocoder.getFromLocation(latitude, longitude, MAX_RESULTS);

        if (!addresses.isEmpty()) {
            location = new Location(latitude, longitude);

            Address address = addresses.get(0);

            location.address = new String[3];
            for (int i = 0; i < location.address.length; i++) {
                location.address[i] = address.getAddressLine(i);
            }
        }

        return location;
    }

    /**
     * Retrieve location description with the given latitude and longitude by first checking
     * for network activity.
     *
     * @param context Context of application.
     * @param latitude Latitude of location.
     * @param longitude Longitude of location.
     * @param callback Callback for latitude and longitude.
     */
    public static void getLocationFromLatLng(final Context context, final double latitude,
            final double longitude, final LocationCallback callback) {
        NetworkHelper.execute(context, new NetworkTask() {
            private Location location;

            @Override
            public void onRequest() throws IOException {
                location = getLocationFromLatLng(context, latitude, longitude);
            }

            @Override
            public void onResponse() {
                callback.onFinish(location);
            }
        });
    }

    /**
     * Retrieve last known location cached on the device.
     *
     * @param context Context of application.
     * @param callback Callback for location.
     */
    public static void getLastKnownLocation(final Context context,
            final LocationCallback callback) {
        NetworkHelper.execute(context, new NetworkTask() {
            private Location location;

            @Override
            public void onRequest() throws IOException {
                LatLng latLng = getLastKnownLatLng(context);

                if (latLng != null) {
                    location = getLocationFromLatLng(context, latLng.latitude, latLng.longitude);
                }
            }

            @Override
            public void onResponse() {
                callback.onFinish(location);
            }
        });
    }

    public interface LocationCallback {

        void onFinish(Location location);
    }
}
