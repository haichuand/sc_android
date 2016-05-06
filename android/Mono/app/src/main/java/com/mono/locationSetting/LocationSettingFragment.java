package com.mono.locationSetting;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mono.MainInterface;
import com.mono.R;

/**
 * Created by xuejing on 4/30/16.
 */
public class LocationSettingFragment extends Fragment {
    private LocationSettingActivity activity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.i("LocationSetting", "inside Location Seeting Fragment");
        if (context instanceof LocationSettingActivity) {
            activity = (LocationSettingActivity) context;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_location_setting, container, false);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}