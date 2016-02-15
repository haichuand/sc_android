package com.mono.settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.mono.R;

public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
    }
}
