package com.mono.locationSetting;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.mono.R;
import com.mono.RequestCodes;
import com.mono.SuperCalyPreferences;
import com.mono.SupercalyAlarmManager;
import com.mono.web.WebActivity;

/**
 * Created by xuejing on 4/30/16.
 */
public class LocationSettingActivity extends AppCompatActivity {
    private final String TAG = "LocationSettingActivity";

    protected LocationRequest mLocationRequest;
    protected GoogleApiClient mGoogleClient;
    protected SupercalyAlarmManager alarmManager;
    SharedPreferences sharedPreferences;
    Button locationStatusButton;
    Button turnOnlocationButton;
    Button webViewButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_setting);
        alarmManager = SupercalyAlarmManager.getInstance(this.getApplicationContext());
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        showLocationSettingFragment();
    }

    @Override
    protected void onStart() {
        super.onStart();
        buildGoogleApiClient();
        locationSettingRequest();
        locationStatusButton = (Button)findViewById(R.id.location_setting_button);
        turnOnlocationButton = (Button)findViewById(R.id.turn_on_location_button);
        webViewButton = (Button)findViewById(R.id.webview_button);
        turnOnlocationButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                locationSettingRequest();
            }
        });

        webViewButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(sharedPreferences != null && !sharedPreferences.getBoolean(SuperCalyPreferences.GOOGLE_ACCOUNT_SIGN_IN, false)) {

                    Intent intent = new Intent(LocationSettingActivity.this, WebActivity.class);
                    startActivityForResult(intent, RequestCodes.Activity.DUMMY_WEB);
                }
                else {

                    Toast.makeText(getApplicationContext(), "Connection has been established already!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    protected void locationSettingRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleClient,
                        builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates state = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        Log.i(TAG, "Location setting is satisfied.");
                        locationStatusButton.setText("IS ON");
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        Log.i(TAG, "Location setting is not satisfied, show dialog to request location setting here.");
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(
                                    LocationSettingActivity.this,
                                    RequestCodes.DeviceSettings.DEVICE_LOCATION_SETTING);
                        } catch (IntentSender.SendIntentException e) {
                            e.printStackTrace();
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.
                        locationStatusButton.setText("IS OFF");
                        break;
                }
            }
        });
    }

    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleClient.connect();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case RequestCodes.DeviceSettings.DEVICE_LOCATION_SETTING:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i(TAG, "User agreed to make required location settings changes.");
                        locationStatusButton.setText("IS ON");
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(TAG, "User chose not to make required location settings changes.");
                        locationStatusButton.setText("IS OFF");
                        break;
                }
                break;
            case RequestCodes.Activity.DUMMY_WEB:
                if (resultCode == Activity.RESULT_OK) {
                    Log.i(TAG, "User successfully login google account!");
                    sharedPreferences.edit().putBoolean(SuperCalyPreferences.GOOGLE_ACCOUNT_SIGN_IN, true).apply();
                    alarmManager.scheduleAlarm(3); // schedule an alarm for every 3-hour
                }
                break;
        }
    }

    public void showFragment(Fragment fragment, String tag, boolean addToBackStack) {
        FragmentManager manager = getSupportFragmentManager();

        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment, tag);

        if (addToBackStack) {
            transaction.addToBackStack(tag);
        }

        transaction.commit();
    }

    public void showLocationSettingFragment() {
        String tag = getString(R.string.fragment_location_setting);
        showFragment(new LocationSettingFragment(), tag, false);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    protected void onStop() {
        super.onStop();
        mGoogleClient.disconnect();
    }

}