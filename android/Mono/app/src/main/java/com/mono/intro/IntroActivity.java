package com.mono.intro;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.mono.PermissionManager;
import com.mono.R;
import com.mono.RequestCodes;
import com.mono.settings.Settings;
import com.mono.util.SimpleTabPagerAdapter;
import com.mono.util.SimpleViewPager;

/**
 * This activity is primarily used to hold the fragment that is responsible for creating the
 * intro screen.
 *
 * @author Gary Ng
 */
public class IntroActivity extends AppCompatActivity {

    private SimpleTabPagerAdapter tabPagerAdapter;
    private SimpleViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        tabPagerAdapter = new SimpleTabPagerAdapter(getSupportFragmentManager(), this);
        tabPagerAdapter.add(null, null, new IntroFragment());

        viewPager = (SimpleViewPager) findViewById(R.id.container);
        viewPager.setAdapter(tabPagerAdapter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == RequestCodes.Permission.PERMISSION_CHECK) {
            Settings.getInstance(this).setPermissionCheck(true);
        } else {
            PermissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
