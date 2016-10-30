package com.mono.details;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.mono.R;
import com.mono.model.Event;
import com.mono.model.Location;
import com.mono.util.Colors;
import com.mono.util.LocationHelper;
import com.mono.util.LocationHelper.LocationCallback;

/**
 * This class is used to handle the location section located in Event Details.
 *
 * @author Gary Ng
 */
public class LocationPanel implements OnMapReadyCallback {

    private static final float DEFAULT_ZOOM_LEVEL = 16f;

    private EventDetailsActivity activity;
    private SupportMapFragment fragment;
    private GoogleMap map;
    private ImageButton currentButton;
    public EditText location;
    private TextWatcher textWatcher;
    private View locationPicker;
    private View clear;
    public boolean locationChanged = false;

    private Event event;
    private Marker marker;

    public LocationPanel(EventDetailsActivity activity) {
        this.activity = activity;
    }

    public void onCreate(Bundle savedInstanceState) {
        FragmentManager manager = activity.getSupportFragmentManager();
        fragment = (SupportMapFragment) manager.findFragmentById(R.id.map);
        fragment.getMapAsync(this);

        currentButton = (ImageButton) activity.findViewById(R.id.current_location);
        currentButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                setCurrentLocation(true);
            }
        });

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
                locationChanged = true;
                Location location = null;

                if (!value.isEmpty()) {
                    location = new Location(value);
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

        if (event.location == null && !event.tempLocations.isEmpty()) {
            event.location = event.tempLocations.get(0);
        }

        if (event.location != null) {
            location.removeTextChangedListener(textWatcher);
            location.setText(event.location.name);
            location.addTextChangedListener(textWatcher);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        // Disable Map Gestures
        map.getUiSettings().setAllGesturesEnabled(false);
        // Initialize Map
        if (event != null && event.location != null && event.location.containsLatLng()) {
            double latitude = event.location.getLatitude();
            double longitude = event.location.getLongitude();

            setMarker(latitude, longitude);
            setCamera(latitude, longitude);
        } else {
            setCurrentLocation(false);
        }
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
        // Custom Marker Icon
        Drawable drawable = activity.getDrawable(R.drawable.ic_place);
        if (drawable != null) {
            float scale = 1.5f;

            Bitmap bitmap = Bitmap.createBitmap(Math.round(drawable.getIntrinsicWidth() * scale),
                Math.round(drawable.getIntrinsicHeight() * scale), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());

            int color = Colors.getColor(activity, R.color.red);
            drawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);

            drawable.draw(canvas);

            BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
            options.icon(descriptor);
        }

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

        double latitude = event.location.getLatitude();
        double longitude = event.location.getLongitude();

        setMarker(latitude, longitude);
        setCamera(latitude, longitude);
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

                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                setCamera(latitude, longitude);

                if (marker) {
                    LocationHelper.getLocationFromLatLng(
                        activity,
                        latitude,
                        longitude,
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
            PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

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
            Place place = PlacePicker.getPlace(activity, data);
            LatLng latLng = place.getLatLng();

            Location location = new Location(latLng.latitude, latLng.longitude);
            String name = place.getName().toString();
            String address = place.getAddress().toString();

            if (!address.startsWith(name)) {
                location.name = name + ", " + address;
            } else {
                location.name = address;
            }

            location.address = new String[]{address};

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
