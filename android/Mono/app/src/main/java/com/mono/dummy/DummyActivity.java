package com.mono.dummy;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.mono.RequestCodes;
import com.mono.web.WebActivity;

public class DummyActivity extends AppCompatActivity {

    private Fragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(android.R.id.content, fragment = new DummyFragment(), null);
        transaction.commit();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RequestCodes.Activity.DUMMY_WEB:
                fragment.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    public void showWebActivity(Fragment fragment, int requestCode) {
        Intent intent = new Intent(this, WebActivity.class);

        if (fragment == null) {
            startActivityForResult(intent, requestCode);
        } else {
            fragment.startActivityForResult(intent, requestCode);
        }
    }
}
