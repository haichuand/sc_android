package com.mono.details;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.model.LatLng;
import com.mono.R;
import com.mono.model.Event;
import com.mono.model.Location;

/**
 * This class is used to handle the location section located in Event Details.
 *
 * @author Gary Ng
 */
public class LocationPanel {

    private EventDetailsActivity activity;
    private EditText location;
    private View locationPicker;
    private View submit;

    private Event event;

    public LocationPanel(EventDetailsActivity activity) {
        this.activity = activity;
    }

    public void onCreate(Bundle savedInstanceState) {
        location = (EditText) activity.findViewById(R.id.location);
        location.addTextChangedListener(new TextWatcher() {
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
            }
        });

        locationPicker = activity.findViewById(R.id.location_picker);
        locationPicker.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showPlacePicker();
            }
        });

        submit = activity.findViewById(R.id.submit);
        submit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onLocationSubmit(location);
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

            this.location.setText(location.name);
            event.location = location;
        }
    }

    /**
     * Handle the action of submitting the input of a user-defined location.
     *
     * @param editText The input view.
     */
    public void onLocationSubmit(EditText editText) {
        editText.clearFocus();
        // Hide Keyboard
        InputMethodManager manager =
            (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        manager.hideSoftInputFromWindow(editText.getWindowToken(), 0);

        String text = editText.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }

        Location location = null;

        if (!text.isEmpty()) {
            location = new Location();
            location.name = text;
        }

        event.location = location;
    }
}
