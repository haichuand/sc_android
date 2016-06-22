package com.mono.contacts;

import android.os.Bundle;
import android.support.v7.app.ActionBar;

import com.mono.R;
import com.mono.util.GestureActivity;

/**
 * This activity is primarily used to hold the fragment that is responsible for creating the
 * contacts screen.
 *
 * @author Gary Ng
 */
public class ContactsActivity extends GestureActivity {

    public static final String EXTRA_MODE = "mode";
    public static final int MODE_VIEW = 0;
    public static final int MODE_PICKER = 1;

    public static final String EXTRA_CONTACTS = "contacts";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }
}
