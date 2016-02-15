package com.mono.util;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.Builder;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.mono.model.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GoogleClient implements ConnectionCallbacks, OnConnectionFailedListener {

    private Context context;
    private GoogleApiClient client;

    public GoogleClient(Context context) {
        this.context = context;
    }

    public void initialize() {
        Builder builder = new Builder(context);
        builder.addApi(Places.GEO_DATA_API);
        builder.addApi(Places.PLACE_DETECTION_API);
        builder.addConnectionCallbacks(this);
        builder.addOnConnectionFailedListener(this);

        client = builder.build();
    }

    public void onStart() {
        client.connect();
    }

    public void onStop() {
        client.disconnect();
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int cause) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {

    }

    public List<Location> getLocations(String query, float radius) {
        List<Location> locations = Collections.emptyList();

        LatLng latLng = LocationHelper.getLastKnownLatLng(context);

        if (latLng != null) {
            double[] tempBounds = LocationHelper.getLatLngBounds(latLng.latitude,
                latLng.longitude, radius);

            LatLngBounds bounds = new LatLngBounds(
                new LatLng(tempBounds[0], tempBounds[1]),
                new LatLng(tempBounds[2], tempBounds[3])
            );

            locations = getLocations(query, bounds, null);
        }

        return locations;
    }

    public List<Location> getLocations(String query, LatLngBounds bounds,
            AutocompleteFilter filter) {
        List<Location> locations = new ArrayList<>();

        PendingResult<AutocompletePredictionBuffer> result =
            Places.GeoDataApi.getAutocompletePredictions(client, query, bounds, filter);

        if (result != null) {
            AutocompletePredictionBuffer buffer = result.await();

            String[] placeIds = new String[buffer.getCount()];
            for (int i = 0; i < placeIds.length; i++) {
                AutocompletePrediction prediction = buffer.get(i);
                placeIds[i] = prediction.getPlaceId();
            }

            PendingResult<PlaceBuffer> placeResult =
                Places.GeoDataApi.getPlaceById(client, placeIds);
            PlaceBuffer placeBuffer = placeResult.await();

            for (Place place : placeBuffer) {
                LatLng latLng = place.getLatLng();

                Location location = new Location(latLng.latitude, latLng.longitude);
                String name = place.getName().toString();
                String address = place.getAddress().toString();

                location.name = name;

                if (address.contains(",")) {
                    String street = address.substring(0, address.indexOf(","));
                    String country = address.substring(address.lastIndexOf(",") + 1);
                    String city = address.substring(street.length() + 1,
                        address.lastIndexOf(country) - 1);

                    location.address = new String[]{street.trim(), city.trim(), country.trim()};
                } else {
                    location.address = new String[]{address};
                }

                locations.add(location);
            }

            placeBuffer.release();
            buffer.release();
        }

        return locations;
    }
}
