package com.mono;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.NavigationView.OnNavigationItemSelectedListener;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.google.android.gms.maps.SupportMapFragment;
import com.mono.chat.ChatRoomActivity;
import com.mono.chat.ConversationManager;
import com.mono.details.EventDetailsActivity;
import com.mono.intro.IntroActivity;
import com.mono.model.Account;
import com.mono.model.Calendar;
import com.mono.model.Conversation;
import com.mono.model.Event;
import com.mono.provider.CalendarProvider;
import com.mono.settings.Settings;
import com.mono.settings.SettingsActivity;
import com.mono.util.Colors;
import com.mono.util.GoogleClient;
import com.mono.util.OnBackPressedListener;
import com.mono.util.SimpleTabLayout;
import com.mono.web.WebActivity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements OnNavigationItemSelectedListener,
        MainInterface {

    public static final String APP_DIR = "MonoFiles/";

    public static final int HOME = R.id.nav_home;
    public static final int INTRO = R.id.nav_intro;
    public static final int LOGIN = R.id.nav_login;
    public static final int LOGOUT = R.id.nav_logout;
    public static final int SETTINGS = R.id.nav_settings;

    private static final String EXTRA_EVENT_ID = "eventId";

    private Toolbar toolbar;
    private Spinner toolbarSpinner;
    private DrawerLayout drawer;
    private SimpleTabLayout tabLayout;
    private SimpleTabLayout dockLayout;
    private FloatingActionButton actionButton;
    private Snackbar snackBar;
    private NavigationView navView;

    private ServiceScheduler scheduler;
    private GoogleClient googleClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbarSpinner = (Spinner) findViewById(R.id.toolbar_spinner);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        tabLayout = (SimpleTabLayout) findViewById(R.id.tab_layout);
        dockLayout = (SimpleTabLayout) findViewById(R.id.dock_layout);
        actionButton = (FloatingActionButton) findViewById(R.id.action_btn);

        navView = (NavigationView) findViewById(R.id.nav_view);
        navView.setNavigationItemSelectedListener(this);

        scheduler = new ServiceScheduler();

        googleClient = new GoogleClient(this);
        googleClient.initialize();

        start();
    }

    protected void start() {
        triggerGooglePlayServices(this);
        showHome();

        if (Settings.getInstance(this).getDayOne() <= 0) {
            showIntro();
        }
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
        scheduler.run(this);
//        requestSync(false);
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
        String tag = getString(R.string.fragment_main);
        OnBackPressedListener fragment =
            (OnBackPressedListener) getSupportFragmentManager().findFragmentByTag(tag);

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (fragment.onBackPressed()) {

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
            case RequestCodes.Activity.LOGIN:
                handleLogin(resultCode, data);
                break;
            case RequestCodes.Activity.LOGIN_CHAT:
                handleLoginChat(resultCode, data);
                break;
            case RequestCodes.Activity.SETTINGS:
                handleSettings(resultCode, data);
                break;
            case RequestCodes.Activity.EVENT_DETAILS:
                handleEventDetails(resultCode, data);
                break;
            case RequestCodes.Activity.INTRO:
                handleIntro(resultCode, data);
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
    public boolean onNavigationItemSelected(MenuItem item) {
        if (item.isCheckable() && item.isChecked()) {
            return false;
        }

        int id = item.getItemId();

        switch (id) {
            case HOME:
                showHome();
                break;
            case INTRO:
                showIntro();
                break;
            case LOGIN:
                showLogin();
                break;
            case LOGOUT:
                AccountManager.getInstance(this).logout();
                break;
            case SETTINGS:
                showSettings();
                break;
        }

        drawer.closeDrawer(GravityCompat.START);

        return true;
    }

    @Override
    public void setToolbarTitle(int resId) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
        }

        toolbar.setTitle(resId);
        toolbarSpinner.setVisibility(View.GONE);
    }

    @Override
    public void setToolbarSpinner(CharSequence[] items, int position,
            OnItemSelectedListener listener) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(this,
            R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

        toolbarSpinner.setAdapter(adapter);
        toolbarSpinner.setOnItemSelectedListener(listener);
        toolbarSpinner.setSelection(position);
        toolbarSpinner.setVisibility(View.VISIBLE);
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
    public void setDockLayoutDrawable(int position, Drawable drawable) {
        dockLayout.setDrawable(position, drawable);
    }

    @Override
    public void setActionButton(int resId, int color, OnClickListener listener) {
        if (resId == 0) {
            resId = R.drawable.ic_add_white;
        }

        if (color == 0) {
            color = Colors.getColor(this, R.color.colorAccent);
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
    public void showSnackBar(int resId, int actionResId, int actionColor,
            OnClickListener listener) {
        View view = findViewById(android.R.id.content);

        snackBar = Snackbar.make(view, resId, Snackbar.LENGTH_LONG);
        snackBar.setAction(actionResId, listener);

        if (actionColor == 0) {
            actionColor = Colors.getColor(this, android.R.color.white);
        }
        snackBar.setActionTextColor(actionColor);

        view = snackBar.getView();
        view.setBackgroundColor(Colors.getColor(this, R.color.colorPrimary));

        snackBar.show();
    }

    @Override
    public void requestSync(boolean force) {
        scheduler.requestSync(this, force);
    }

    @Override
    public void showHome() {
        FragmentManager manager = getSupportFragmentManager();
        manager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        MainFragment mainFragment = new MainFragment();
        setFragment(this, mainFragment, getString(R.string.fragment_main), false);
    }

    @Override
    public void showIntro() {
        Intent intent = new Intent(this, IntroActivity.class);
        startActivityForResult(intent, RequestCodes.Activity.INTRO);
    }

    public void handleIntro(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Settings settings = Settings.getInstance(this);
            long milliseconds = settings.getDayOne();

            if (milliseconds <= 0) {
                try {
                    // Initialize Calendars
                    Set<Long> calendars = new HashSet<>();
                    List<Calendar> calendarList = CalendarProvider.getInstance(this).getCalendars();
                    for (Calendar calendar : calendarList) {
                        calendars.add(calendar.id);
                    }
                    settings.setCalendars(calendars);

                    settings.setDayOne(System.currentTimeMillis());
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void showLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivityForResult(intent, RequestCodes.Activity.LOGIN);
    }

    public void handleLogin(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Account account = data.getParcelableExtra(LoginActivity.EXTRA_ACCOUNT);
            AccountManager.getInstance(this).setAccount(account);
        }
    }

    public void handleLoginChat(int resultCode, Intent data) {
        handleLogin(resultCode, data);

        if (resultCode == RESULT_OK) {
            String eventId = data.getStringExtra(EXTRA_EVENT_ID);
            showChat(eventId);
        }
    }

    @Override
    public void showSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivityForResult(intent, RequestCodes.Activity.SETTINGS);
    }

    public void handleSettings(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {

        }
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

    @Override
    public void showEventDetails(Event event) {
        Calendar calendar = null;

        if (event.calendarId > 0) {
            calendar = CalendarProvider.getInstance(this).getCalendar(event.calendarId);
        } else {
            List<Calendar> calendars = CalendarProvider.getInstance(this).getCalendars();
            for (Calendar item : calendars) {
                if (item.primary) {
                    event.calendarId = item.id;
                    calendar = item;
                    break;
                }
            }
        }

        if (calendar == null) {
            return;
        }

        Intent intent = new Intent(this, EventDetailsActivity.class);
        intent.putExtra(EventDetailsActivity.EXTRA_CALENDAR, calendar);
        intent.putExtra(EventDetailsActivity.EXTRA_EVENT, event);

        startActivityForResult(intent, RequestCodes.Activity.EVENT_DETAILS);
    }

    public void handleEventDetails(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Event event = data.getParcelableExtra(EventDetailsActivity.EXTRA_EVENT);

            if (event.id != null) {
                EventManager.getInstance(this).updateEvent(
                    EventManager.EventAction.ACTOR_SELF,
                    event.id,
                    event,
                    null
                );
            } else {
//                event.internalId = System.currentTimeMillis();
//
//                EventManager.getInstance(this).createEvent(
//                    EventManager.EventAction.ACTOR_SELF,
//                    event.calendarId,
//                    event.internalId,
//                    event.externalId,
//                    event.type,
//                    event.title,
//                    event.description,
//                    event.location != null ? event.location.name : null,
//                    event.color,
//                    event.startTime,
//                    event.endTime,
//                    event.timeZone,
//                    event.endTimeZone,
//                    event.allDay,
//                    null
//                );

                EventManager.getInstance(this).createSyncEvent(
                    EventManager.EventAction.ACTOR_SELF,
                    event.calendarId,
                    event.title,
                    event.description,
                    event.location != null ? event.location.name : null,
                    event.color,
                    event.startTime,
                    event.endTime,
                    event.timeZone,
                    event.endTimeZone,
                    event.allDay,
                    null
                );
            }
        }
    }

    @Override
    public void showChat(String eventId) {
        Event event = EventManager.getInstance(this).getEvent(eventId, false);
        if (event == null) {
            return;
        }

        Account account = AccountManager.getInstance(this).getAccount();
        if (account == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra(EXTRA_EVENT_ID, eventId);

            startActivityForResult(intent, RequestCodes.Activity.LOGIN_CHAT);
            return;
        }

        ConversationManager conversationManager = ConversationManager.getInstance(this);
        List<Conversation> conversations = conversationManager.getConversations(eventId);

        String conversationId;
        if (conversations.isEmpty()) {
            conversationId = conversationManager.createConversation(event.title, event.id);
        } else {
            Conversation conversation = conversations.get(0);
            conversationId = conversation.id;
        }

        Intent intent = new Intent(this, ChatRoomActivity.class);
        intent.putExtra(ChatRoomActivity.EVENT_ID, event.id);
        intent.putExtra(ChatRoomActivity.EVENT_NAME, event.title);
        intent.putExtra(ChatRoomActivity.EVENT_START_TIME, event.startTime);
        intent.putExtra(ChatRoomActivity.EVENT_END_TIME, event.endTime);
        intent.putExtra(ChatRoomActivity.CONVERSATION_ID, conversationId);
        intent.putExtra(ChatRoomActivity.MY_ID, account.username);

        startActivityForResult(intent, RequestCodes.Activity.CHAT);
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
