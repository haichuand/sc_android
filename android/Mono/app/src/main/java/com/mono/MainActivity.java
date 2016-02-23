package com.mono;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.NavigationView.OnNavigationItemSelectedListener;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View.OnClickListener;

import com.google.android.gms.maps.SupportMapFragment;
import com.mono.db.DatabaseHelper;
import com.mono.settings.Settings;
import com.mono.settings.SettingsActivity;
import com.mono.util.GoogleClient;
import com.mono.util.Log;
import com.mono.util.SimpleTabLayout;
import com.mono.web.WebActivity;

public class MainActivity extends AppCompatActivity implements OnNavigationItemSelectedListener,
        MainInterface {

    public static final String APP_DIR = "MonoFiles/";

    public static final int HOME = R.id.nav_home;
    public static final int SETTINGS = R.id.nav_settings;

    private DrawerLayout drawer;
    private SimpleTabLayout tabLayout;
    private SimpleTabLayout dockLayout;
    private FloatingActionButton actionButton;
    private NavigationView navView;

    private GoogleClient googleClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        tabLayout = (SimpleTabLayout) findViewById(R.id.tab_layout);
        dockLayout = (SimpleTabLayout) findViewById(R.id.dock_layout);
        actionButton = (FloatingActionButton) findViewById(R.id.action_btn);

        navView = (NavigationView) findViewById(R.id.nav_view);
        navView.setNavigationItemSelectedListener(this);

        googleClient = new GoogleClient(this);
        googleClient.initialize();

        start();
    }

    protected void start() {
        triggerGooglePlayServices(this);

        Log.initialize(this);
        Settings.initialize(this);

        if (!Settings.getPermissionCheck()) {
            PermissionManager.checkPermissions(this, RequestCodes.Permission.PERMISSION_CHECK);
        }

        showHome();
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleClient.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        navView.setCheckedItem(HOME);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        googleClient.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (tabLayout.isVisible() && tabLayout.getSelectedTabPosition() > 0) {
            tabLayout.selectTab(0);
        } else if (dockLayout.isVisible() && dockLayout.getSelectedTabPosition() > 0) {
            dockLayout.selectTab(0);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                showSettings();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RequestCodes.Activity.SETTINGS:
                if (resultCode == RESULT_OK) {

                }
                break;
        }
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        overridePendingTransition(R.anim.slide_enter_right, R.anim.slide_exit_left);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        super.startActivityForResult(intent, requestCode);
        overridePendingTransition(R.anim.slide_enter_right, R.anim.slide_exit_left);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == RequestCodes.Permission.PERMISSION_CHECK) {
            Settings.setPermissionCheck(true);
        } else {
            PermissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        if (item.isCheckable() && item.isChecked()) {
            return false;
        }

        int id = item.getItemId();

        switch (id) {
            case HOME:
                showHome();
                break;
            case SETTINGS:
                showSettings();
                break;
        }

        drawer.closeDrawer(GravityCompat.START);

        return true;
    }

    @Override
    public void setTabLayoutViewPager(ViewPager viewPager) {
        if (viewPager != null) {
            tabLayout.setupWithViewPager(viewPager, true);
            tabLayout.show();
        } else {
            tabLayout.hide();
        }
    }

    @Override
    public void setTabLayoutBadge(int position, int color, String value) {
        tabLayout.setBadge(position, color, value);
    }

    @Override
    public void setDockLayoutViewPager(ViewPager viewPager) {
        if (viewPager != null) {
            dockLayout.setupWithViewPager(viewPager, false);
            dockLayout.show();
        } else {
            dockLayout.hide();
        }
    }

    @Override
    public void setActionButton(int resId, int color, OnClickListener listener) {
        if (resId == 0) {
            resId = R.drawable.ic_add_white;
        }

        if (color == 0) {
            color = getResources().getColor(R.color.colorAccent);
        }

        actionButton.setImageResource(resId);
        actionButton.setBackgroundTintList(ColorStateList.valueOf(color));
        actionButton.setOnClickListener(listener);

        if (listener != null) {
            actionButton.show();
        } else {
            actionButton.hide();
        }
    }

    @Override
    public void showHome() {
        FragmentManager manager = getSupportFragmentManager();
        manager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        MainFragment mainFragment = new MainFragment();
        setFragment(this, mainFragment, getString(R.string.fragment_main), false);
    }

    @Override
    public void showSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivityForResult(intent, RequestCodes.Activity.SETTINGS);
    }

    @Override
    public void showWebActivity(Fragment fragment, int requestCode) {
        Intent intent = new Intent(this, WebActivity.class);

        if (fragment == null) {
            startActivityForResult(intent, requestCode);
        } else {
            fragment.startActivityForResult(intent, requestCode);
        }
    }

    public static void triggerGooglePlayServices(AppCompatActivity activity) {
        FragmentManager manager = activity.getSupportFragmentManager();
        SupportMapFragment fragment = SupportMapFragment.newInstance();

        FragmentTransaction transaction = manager.beginTransaction();
        transaction.add(fragment, null);
        transaction.commit();

        transaction = manager.beginTransaction();
        transaction.remove(fragment);
        transaction.commit();
    }

    public static void setFragment(AppCompatActivity activity, Fragment fragment, String tag,
            boolean addToBackStack) {
        FragmentManager manager = activity.getSupportFragmentManager();

        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment, tag);

        if (addToBackStack) {
            transaction.addToBackStack(tag);
        }

        transaction.commit();
    }
}
