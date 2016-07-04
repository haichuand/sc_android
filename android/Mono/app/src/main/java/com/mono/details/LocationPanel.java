package com.mono.details;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.mono.PermissionManager;
import com.mono.R;
import com.mono.model.Event;
import com.mono.model.Location;
import com.mono.util.LocationHelper;
import com.mono.util.LocationHelper.LocationCallback;

/**
 * This class is used to handle the location section located in Event Details.
 *
 * @author Gary Ng
 */
public class LocationPanel implements OnMapReadyCallback, OnMyLocationButtonClickListener {

    private static final float DEFAULT_ZOOM_LEVEL = 16f;

    private EventDetailsActivity activity;
    private SupportMapFragment fragment;
    private GoogleMap map;
    private EditText location;
    private TextWatcher textWatcher;
    private View locationPicker;
    private View clear;

    private Event event;
    private Marker marker;

    public LocationPanel(EventDetailsActivity activity) {
        this.activity = activity;
    }

    public void onCreate(Bundle savedInstanceState) {
        FragmentManager manager = activity.getSupportFragmentManager();
        fragment = (SupportMapFragment) manager.findFragmentById(R.id.map);
        fragment.getMapAsync(this);

        location = (EditText) activity.findViewById(R.id.location);
        location.addTextChangedListener(textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString().trim();

                Location location = null;

                if (!value.isEmpty()) {
                    location = new Location();
                    location.name = value;
                }

                event.location = location;

                if (marker != null) {
                    marker.remove();
                    marker = null;
                }
            }
        });

        locationPicker = activity.findViewById(R.id.location_picker);
        locationPicker.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showPlacePicker();
            }
        });

        clear = activity.findViewById(R.id.clear);
        clear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onLocationClear();
            }
        });
    }

    /**
     * Initialize this panel using the given event.
     *
     * @param event The instance of the event.
     */
    public void setEvent(Event event) {
        this.event = event;

        if (event.location != null) {
            location.setText(event.location.name);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        // Display Current Location Button
        if (PermissionManager.checkPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) ||
                PermissionManager.checkPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            map.setMyLocationEnabled(true);
            map.setOnMyLocationButtonClickListener(this);
        }
        // Disable Map Gestures
        map.getUiSettings().setAllGesturesEnabled(false);
        // Initialize Map
        if (event != null && event.location != null && event.location.containsLatLng()) {
            setCamera(event.location.latitude, event.location.longitude);
        } else {
            setCurrentLocation(false);
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        setCurrentLocation(true);
        return true;
    }

    /**
     * Move the camera to the given position.
     *
     * @param latitude The value of the latitude.
     * @param longitude The value of the longitude.
     */
    public void setCamera(double latitude, double longitude) {
        LatLng latLng = new LatLng(latitude, longitude);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM_LEVEL));
    }

    /**
     * Place a marker onto the map.
     *
     * @param latitude The value of the latitude.
     * @param longitude The value of the longitude.
     */
    public void setMarker(double latitude, double longitude) {
        if (marker != null) {
            marker.remove();
        }

        LatLng latLng = new LatLng(latitude, longitude);

        MarkerOptions options = new MarkerOptions();
        options.position(latLng);

        marker = map.addMarker(options);
    }

    /**
     * Set the location for the event as well as placing the marker and moving the camera to that
     * position.
     *
     * @param location The value of the location.
     */
    public void setLocation(Location location) {
        this.location.removeTextChangedListener(textWatcher);
        this.location.setText(location.name);
        this.location.addTextChangedListener(textWatcher);

        event.location = location;

        setMarker(location.latitude, location.longitude);
        setCamera(location.latitude, location.longitude);
    }

    /**
     * Set the current location detected to be used for the event.
     *
     * @param marker The value to set the marker and use the current location.
     */
    public void setCurrentLocation(final boolean marker) {
        LocationHelper.getLastKnownLatLng(activity, new LocationCallback() {
            @Override
            public void onFinish(Location location) {
                if (location == null) {
                    return;
                }

                setCamera(location.latitude, location.longitude);

                if (marker) {
                    LocationHelper.getLocationFromLatLng(
                        activity,
                        location.latitude,
                        location.longitude,
                        new LocationCallback() {
                            @Override
                            public void onFinish(Location location) {
                                location.name = location.getAddress();
                                setLocation(location);
                            }
                        }
                    );
                }
            }
        });
    }

    /**
     * Display the place picker to add a location to the event.
     */
    public void showPlacePicker() {
        try {
            PlaceAutocomplete.IntentBuilder builder =
                new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN);

            Intent intent = builder.build(activity);
            activity.startActivityForResult(intent, EventDetailsActivity.REQUEST_PLACE_PICKER);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handle the result from the place picker. The result is a place containing the name and
     * address of the location.
     *
     * @param resultCode The result code returned from the activity.
     * @param data The data returned from the activity.
     */
    public void handlePlacePicker(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Place place = PlaceAutocomplete.getPlace(activity, data);
            LatLng latLng = place.getLatLng();

            Location location = new Location(latLng.latitude, latLng.longitude);
            String name = place.getName().toString();
            String address = place.getAddress().toString();

            if (!address.startsWith(name)) {
                location.name = name + ", " + address;
            } else {
                location.name = address;
            }

            setLocation(location);
        }
    }

    /**
     * Handle the action of clearing the current location.
     */
    public void onLocationClear() {
        location.setText("");

        if (marker != null) {
            marker.remove();
        }
    }
}
