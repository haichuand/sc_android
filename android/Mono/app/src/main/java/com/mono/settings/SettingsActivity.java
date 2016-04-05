package com.mono.settings;

import android.os.Bundle;
import android.support.v7.app.ActionBar;

import com.mono.R;
import com.mono.util.GestureActivity;

public class SettingsActivity extends GestureActivity {

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
