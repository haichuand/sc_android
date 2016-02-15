package com.mono.settings;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.app.ActionBar;

import com.mono.R;
import com.mono.util.GestureActivity;

public class SettingsActivity extends GestureActivity {

    public static final OnPreferenceChangeListener listener;

    static {
        listener = new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                String stringValue = value.toString();

                if (preference instanceof ListPreference) {
                    ListPreference pref = (ListPreference) preference;
                    CharSequence title = pref.getEntries()[pref.findIndexOfValue(stringValue)];

                    preference.setSummary(title);
                } else {
                    preference.setSummary(stringValue);
                }

                return true;
            }
        };
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }
}
