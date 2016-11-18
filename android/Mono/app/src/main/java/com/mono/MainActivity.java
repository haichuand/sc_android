package com.mono;

import android.content.DialogInterface;
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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.SupportMapFragment;
import com.mono.alarm.AlarmHelper;
import com.mono.chat.ChatRoomActivity;
import com.mono.chat.ConversationManager;
import com.mono.chat.CreateChatActivity;
import com.mono.contacts.ContactsActivity;
import com.mono.details.EventDetailsActivity;
import com.mono.intro.IntroActivity;
import com.mono.locationSetting.LocationSettingActivity;
import com.mono.model.Account;
import com.mono.model.Attendee;
import com.mono.model.Calendar;
import com.mono.model.Conversation;
import com.mono.model.Event;
import com.mono.network.ServerSyncManager;
import com.mono.provider.CalendarProvider;
import com.mono.settings.Settings;
import com.mono.settings.SettingsActivity;
import com.mono.util.Colors;
import com.mono.util.Common;
import com.mono.util.GoogleClient;
import com.mono.util.OnBackPressedListener;
import com.mono.util.SimpleTabLayout;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Main Activity is used to hold and display the various screens (specifically Fragments) in
 * a single Activity while providing a single set of components, which includes the action bar,
 * navigation tabs, navigation dock, etc. for the screens to use. Initializations that are
 * required on startup is handled here as well.
 *
 * @author Gary Ng
 */
public class MainActivity extends AppCompatActivity implements OnNavigationItemSelectedListener,
        MainInterface {

    public static final String APP_DIR = "MonoFiles/";

    public static final int HOME = R.id.nav_home;
    public static final int CONTACTS = R.id.nav_contacts;
    public static final int SETTINGS = R.id.nav_settings;
    public static final int LOCATION_SETTING = R.id.nav_location_setting;

    public static final String EXTRA_EVENT_ID = "eventId";

    private Toolbar toolbar;
    private Spinner toolbarSpinner;
    private DrawerLayout drawer;
    private ActionBarDrawerToggle drawerToggle;
    private SimpleTabLayout tabLayout;
    private SimpleTabLayout dockLayout;
    private FloatingActionButton actionButton;
    private Snackbar snackBar;
    private NavigationView navView;

    private ServiceScheduler scheduler;
    private GoogleClient googleClient;
    private ConversationManager conversationManager;
    private ServerSyncManager syncManager;

    private EditModeListener editModeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbarSpinner = (Spinner) findViewById(R.id.toolbar_spinner);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        drawerToggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);

                Account account = AccountManager.getInstance(getApplicationContext()).getAccount();
                boolean isOnline = account != null && account.status == Account.STATUS_ONLINE;

                View navHeaderView = navView.getHeaderView(0);

                TextView name = (TextView) navHeaderView.findViewById(R.id.display_name);
                Button button = (Button) navHeaderView.findViewById(R.id.login_btn);

                if (isOnline) {
                    name.setText(!Common.isEmpty(account.email) ? account.email : account.phone);

                    button.setText(R.string.action_logout);
                    button.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext(),
                                R.style.AppTheme_Dialog_Alert);
                            builder.setMessage(R.string.confirm_logout);

                            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which) {
                                        case DialogInterface.BUTTON_POSITIVE:
                                            AccountManager.getInstance(getApplicationContext()).logout();
                                            drawer.closeDrawer(GravityCompat.START);
                                            break;
                                        case DialogInterface.BUTTON_NEGATIVE:
                                            break;
                                    }

                                    dialog.dismiss();
                                }
                            };

                            builder.setPositiveButton(R.string.yes, listener);
                            builder.setNegativeButton(R.string.no, listener);

                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                    });
                } else {
                    name.setText(R.string.hello);

                    button.setText(R.string.action_login);
                    button.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            showLogin();
                            drawer.closeDrawer(GravityCompat.START);
                        }
                    });
                }
            }
        };

        drawer.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        tabLayout = (SimpleTabLayout) findViewById(R.id.tab_layout);
        dockLayout = (SimpleTabLayout) findViewById(R.id.dock_layout);
        actionButton = (FloatingActionButton) findViewById(R.id.action_btn);

        navView = (NavigationView) findViewById(R.id.nav_view);
        navView.setNavigationItemSelectedListener(this);

        scheduler = new ServiceScheduler();

        googleClient = new GoogleClient(this);
        googleClient.initialize();

        conversationManager = ConversationManager.getInstance(this);

        syncManager = ServerSyncManager.getInstance(this);

        start();
    }

    protected void start() {
        // Display Splash Screen for New Install
        if (Settings.getInstance(this).getDayOne() <= 0) {
            showIntro();
            return;
        }
        // Simple Trick for Faster Google Maps Loading
        triggerGooglePlayServices(this);
        // Load Initial Fragment
        showHome();
        // Handle Reminders
        AlarmHelper.startAll(this);
        // Handle Intent Extras
        handleIntentExtras();
    }

    protected void handleIntentExtras() {
        Intent intent = getIntent();
        if (intent == null) {
            return;
        }

        if (intent.hasExtra(EXTRA_EVENT_ID)) {
            String eventId = intent.getStringExtra(EXTRA_EVENT_ID);
            Event event = EventManager.getInstance(this).getEvent(eventId);

            if (event != null) {
                showEventDetails(event);
            }
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
        syncManager.processServerSyncItems();
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
            // Close Hidden Side Menu
            drawer.closeDrawer(GravityCompat.START);
        } else if (editModeListener != null) {
            // Exit Edit Mode
            setEditMode(null);
        } else if (fragment.onBackPressed()) {
            // Enable Back Button Handling (if any) within Fragments
        } else if (tabLayout.isVisible() && tabLayout.getSelectedTabPosition() > 0) {
            // Reset Tab to First Before Exiting App
            tabLayout.selectTab(0);
        } else if (dockLayout.isVisible() && dockLayout.getSelectedTabPosition() > 0) {
            // Reset Dock Tab to First Before Exiting App
            dockLayout.selectTab(0);
        } else {
            // Exit App
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
            case RequestCodes.Activity.LOCATION_SETTING:
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
            case CONTACTS:
                showContacts();
                break;
            case SETTINGS:
                showSettings();
                break;
            case LOCATION_SETTING:
                showLocationSetting();
                break;
        }

        drawer.closeDrawer(GravityCompat.START);

        return true;
    }

    /**
     * Set the action bar title using a string resource.
     *
     * @param resId String resource containing the title.
     */
    @Override
    public void setToolbarTitle(int resId) {
        setToolbarTitle(getString(resId));
    }

    /**
     * Set the action bar title using a string.
     *
     * @param title String containing the title.
     */
    @Override
    public void setToolbarTitle(CharSequence title) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
        }

        toolbar.setTitle(title);
        toolbarSpinner.setVisibility(View.GONE);
    }

    /**
     * Replace the action bar title with a drop down menu.
     *
     * @param items The array of drop down values.
     * @param position The default position to be selected.
     * @param listener The listener used to respond to drop down actions.
     */
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

    /**
     * Display the navigation tabs directly below the action bar.
     *
     * @param viewPager The view pager used to handle the tabs.
     */
    @Override
    public void setTabLayoutViewPager(ViewPager viewPager) {
        if (viewPager != null) {
            tabLayout.setupWithViewPager(viewPager, true);
            tabLayout.show();
        } else {
            tabLayout.hide();
        }
    }

    /**
     * Display a badge within the specified tab at the given position.
     *
     * @param position The position of the tab.
     * @param color The color of the badge background.
     * @param value The value to be displayed.
     */
    @Override
    public void setTabLayoutBadge(int position, int color, String value) {
        tabLayout.setBadge(position, color, value);
    }

    /**
     * Display the navigation dock at the bottom of the screen.
     *
     * @param viewPager The view pager used to handle the tabs.
     */
    @Override
    public void setDockLayoutViewPager(ViewPager viewPager) {
        if (viewPager != null) {
            dockLayout.setupWithViewPager(viewPager, false);
            dockLayout.show();
        } else {
            dockLayout.hide();
        }
    }

    /**
     * Replace the icon within the specified tab at the given position.
     *
     * @param position The position of the tab.
     * @param drawable The drawable icon.
     */
    @Override
    public void setDockLayoutDrawable(int position, Drawable drawable) {
        dockLayout.setDrawable(position, drawable);
    }

    /**
     * Display the floating action button at the bottom of the screen.
     *
     * @param resId The image resource.
     * @param color The color of the button background.
     * @param listener The listener used to respond to click actions.
     */
    @Override
    public void setActionButton(int resId, int color, OnClickListener listener) {
        // Default Image Resource
        if (resId == 0) {
            resId = R.drawable.ic_add;
        }
        // Default Background Color
        if (color == 0) {
            color = Colors.getColor(this, R.color.colorAccent);
        }

        actionButton.setImageResource(resId);
        actionButton.setBackgroundTintList(ColorStateList.valueOf(color));
        actionButton.setOnClickListener(listener);
        // Reveal if Listener is Present
        if (listener != null) {
            actionButton.show();
        } else {
            actionButton.hide();
        }
    }

    /**
     * Display the notification popup (Snackbar) at the bottom of the screen.
     *
     * @param resId String resource containing the message.
     * @param actionResId String resource to be used as the clickable text.
     * @param actionColor Color of the clickable text.
     * @param listener Listener used to respond to the clickable text.
     */
    @Override
    public void showSnackBar(int resId, int actionResId, int actionColor,
            OnClickListener listener) {
        View view = findViewById(R.id.snackbar);

        snackBar = Snackbar.make(view, resId, Snackbar.LENGTH_LONG);
//        snackBar.setAction(actionResId, listener);
        // Default Action Text Color
        if (actionColor == 0) {
            actionColor = Colors.getColor(this, android.R.color.white);
        }
        snackBar.setActionTextColor(actionColor);

        view = snackBar.getView();
        view.setBackgroundColor(Colors.getColor(this, R.color.colorPrimary));

        snackBar.show();
    }

    /**
     * Trigger the syncing action primarily used for calendar events.
     *
     * @param force Value used to bypass the sync delay.
     */
    @Override
    public void requestSync(boolean force) {
        scheduler.requestSync(this, force);
    }

    /**
     * Initializes the Main Fragment used to hold the multiple screens such as the Calendar,
     * Dashboard, Chat, etc.
     */
    @Override
    public void showHome() {
//        Account account = AccountManager.getInstance(this).getAccount();
//        if (account == null) {
//            Intent intent = new Intent(this, LoginActivity.class);
//            startActivityForResult(intent, RequestCodes.Activity.LOGIN_CHAT);
//            return;
//        }

        FragmentManager manager = getSupportFragmentManager();
        manager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        MainFragment mainFragment = new MainFragment();
        setFragment(this, mainFragment, getString(R.string.fragment_main), false);
    }

    /**
     * Display the splash screen activity that also handles any permissions required by this app.
     */
    @Override
    public void showIntro() {
        Intent intent = new Intent(this, IntroActivity.class);
        startActivityForResult(intent, RequestCodes.Activity.INTRO);
    }

    /**
     * Handles the result from the splash screen. Performs any additional steps for new users
     * such as pulling all calendars from the device.
     *
     * @param resultCode The result code returned from the activity.
     * @param data The data returned from the activity.
     */
    public void handleIntro(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Settings settings = Settings.getInstance(this);
            long milliseconds = settings.getDayOne();

            if (milliseconds <= 0) {
                try {
                    // Initialize Calendars
                    Set<Long> calendars = new HashSet<>();
                    long defaultId = -1;

                    List<Calendar> calendarList = CalendarProvider.getInstance(this).getCalendars();

                    for (Calendar calendar : calendarList) {
                        if (defaultId == -1 && calendar.primary && !calendar.local) {
                            defaultId = calendar.id;
                            settings.setCalendarDefault(defaultId);
                        }

                        calendars.add(calendar.id);
                    }
                    settings.setCalendars(calendars);
                    // Mark Now as Day One
                    settings.setDayOne(System.currentTimeMillis());
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }

            start();
        }
    }

    /**
     * Display the login screen activity.
     */
    @Override
    public void showLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivityForResult(intent, RequestCodes.Activity.LOGIN);
    }

    /**
     * Handles the result from the login screen. The account accessed will be stored for future
     * references.
     *
     * @param resultCode The result code returned from the activity.
     * @param data The data returned from the activity.
     */
    public void handleLogin(int resultCode, Intent data) {
//        if (resultCode == RESULT_OK) {
//            Account account = data.getParcelableExtra(LoginActivity.EXTRA_ACCOUNT);
//            AccountManager.getInstance(this).login(account);
//        }
        // add user self to database if not already
        Account account = AccountManager.getInstance(this).getAccount();
        Attendee attendee = conversationManager.getUserById(String.valueOf(account.id));
        if (attendee == null) {
            attendee = new Attendee(
                    String.valueOf(account.id),
                    account.mediaId,
                    account.email,
                    account.phone,
                    account.firstName,
                    account.lastName,
                    account.username,
                    false,
                    true
            );
            conversationManager.saveUserToDB(attendee);
        }
    }

    /**
     * Handles the result from the login screen when attempting to chat when user wasn't
     * initially logged in. Brings the user back to the chat upon successful login.
     *
     * @param resultCode The result code returned from the activity.
     * @param data The data returned from the activity.
     */
    public void handleLoginChat(int resultCode, Intent data) {
        handleLogin(resultCode, data);

        if (resultCode == RESULT_OK) {
            String eventId = data.getStringExtra(EXTRA_EVENT_ID);
            showChat(eventId);
        }
    }

    /**
     * Display the contacts screen activity.
     */
    @Override
    public void showContacts() {
        Intent intent = new Intent(this, ContactsActivity.class);
        startActivityForResult(intent, RequestCodes.Activity.CONTACTS);
    }

    /**
     * Display the settings screen activity.
     */
    @Override
    public void showSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivityForResult(intent, RequestCodes.Activity.SETTINGS);
    }

    /**
     * Handles the result from the settings screen.
     *
     * @param resultCode The result code returned from the activity.
     * @param data The data returned from the activity.
     */
    public void handleSettings(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {

        }
    }

    /**
     * Display the event details screen activity.
     *
     * @param event Event to be displayed or edited.
     */
    @Override
    public void showEventDetails(Event event) {
        Calendar calendar;

        if (event.id == null) {
            long calendarId = Settings.getInstance(this).getCalendarDefault();
            if (calendarId > 0) {
                event.source = Event.SOURCE_PROVIDER;
                event.calendarId = calendarId;
            }
        } else {
            EventManager manager = EventManager.getInstance(this);
            event = manager.getEvent(event.id, true);

            event.viewTime = System.currentTimeMillis();
            manager.updateEvent(EventManager.EventAction.ACTOR_SELF, event, null);
        }

        if (event.calendarId > 0) {
            calendar = CalendarProvider.getInstance(this).getCalendar(event.calendarId);
        } else {
            // Local Calendar (Specific for this App)
            calendar = new Calendar(event.calendarId, getString(R.string.local_calendar));
        }

        Intent intent = new Intent(this, EventDetailsActivity.class);
        intent.putExtra(EventDetailsActivity.EXTRA_CALENDAR, calendar);
        intent.putExtra(EventDetailsActivity.EXTRA_EVENT, event);

        startActivityForResult(intent, RequestCodes.Activity.EVENT_DETAILS);
    }

    /**
     * Handles the result from the event details screen. Either create a new event or update an
     * existing event depending on the event returned.
     *
     * @param resultCode The result code returned from the activity.
     * @param data The data returned from the activity.
     */
    public void handleEventDetails(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            EventManager manager = EventManager.getInstance(this);
            Event event = data.getParcelableExtra(EventDetailsActivity.EXTRA_EVENT);

            if (event.id != null) {
                if (event.repeats) {
                    manager.removeEvent(EventManager.EventAction.ACTOR_NONE, event.id, null);
                    manager.createEvent(EventManager.EventAction.ACTOR_SELF, event, null);
                } else {
                    manager.updateEvent(EventManager.EventAction.ACTOR_SELF, event, null);
                }
            } else {
                manager.createEvent(EventManager.EventAction.ACTOR_SELF, event, null);
            }
        }
    }

    @Override
    public void showChat(String eventId) {
        Event event = EventManager.getInstance(this).getEvent(eventId);
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

        List<Conversation> conversations = conversationManager.getConversationsDBFirst(event);
        if (conversations.isEmpty()) { //create new chat
            Intent intent = new Intent(this, CreateChatActivity.class);
            intent.putExtra(EventDetailsActivity.EXTRA_EVENT, event);
            startActivity(intent);
        } else {
            Conversation conversation = conversations.get(0);
            startChatRoomActivity(event.startTime, event.endTime, event.allDay, conversation.id, String.valueOf(account.id));
        }
    }

    @Override
    public void showExistingChat(String conversationId) {
        Conversation conversation = conversationManager.getCompleteConversation(conversationId);
        if (conversation == null) {
            Toast.makeText(this, "Cannot find conversation with id: " + conversationId, Toast.LENGTH_SHORT).show();
            return;
        }
        EventManager eventManager = EventManager.getInstance(this);
        Event event = null;
        if (conversation.eventId != null) {
            event = eventManager.getEvent(conversation.eventId);
        }

        String myId = String.valueOf(AccountManager.getInstance(this).getAccount().id);
        if (event == null) {
            startChatRoomActivity(0, 0, false, conversationId, myId);
        } else {
            startChatRoomActivity(event.startTime, event.endTime, event.allDay, conversationId, myId);
        }
    }

    @Override
    public void showLocationSetting() {
        Intent intent = new Intent(this, LocationSettingActivity.class);
        startActivityForResult(intent, RequestCodes.Activity.LOCATION_SETTING);
    }

    /**
     * Switch the current activity to Edit Mode by limiting certain functionality and maximizing
     * viewing space to improve item selection.
     *
     * @param listener Listener to handle action when switching modes.
     */
    @Override
    public void setEditMode(EditModeListener listener) {
        if (editModeListener != null) {
            editModeListener.onFinish();
        }

        boolean isEditEnabled = listener != null;

        if (isEditEnabled) {
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            drawerToggle.setDrawerIndicatorEnabled(false);
            // Show Back Arrow
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
            // Handle Back Arrow
            drawerToggle.setToolbarNavigationClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    onBackPressed();
                }
            });
        } else {
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            // Hide Back Arrow
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(false);
            }

            drawerToggle.setDrawerIndicatorEnabled(true);
            drawerToggle.setToolbarNavigationClickListener(null);
        }
        // Disable Tabs
        tabLayout.setEnabled(!isEditEnabled);
        dockLayout.setEnabled(!isEditEnabled);

        invalidateOptionsMenu();

        editModeListener = listener;
    }

    /**
     * Temporary solution to improve the initial load times of Google Maps by essentially
     * loading it before actual use.
     *
     * @param activity The activity used to retrieve fragment manager.
     */
    public static void triggerGooglePlayServices(AppCompatActivity activity) {
        FragmentManager manager = activity.getSupportFragmentManager();
        SupportMapFragment fragment = SupportMapFragment.newInstance();
        // Temporarily Load Google Maps Fragment
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.add(fragment, null);
        transaction.commit();
        // Immediately Remove Fragment
        transaction = manager.beginTransaction();
        transaction.remove(fragment);
        transaction.commit();
    }

    /**
     * Encapsulates the steps required to load any fragment into this activity.
     *
     * @param activity The activity used to retrieve fragment manager.
     * @param fragment The fragment to be added.
     * @param tag The tag value to later retrieve the fragment.
     * @param addToBackStack The value used to enable back stack usage.
     */
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

    private void startChatRoomActivity(long startTime, long endTime, boolean isAllDay, String conversationId, String accountId) {
        Intent intent = new Intent(this, ChatRoomActivity.class);
        intent.putExtra(ChatRoomActivity.EVENT_START_TIME, startTime);
        intent.putExtra(ChatRoomActivity.EVENT_END_TIME, endTime);
        intent.putExtra(ChatRoomActivity.EVENT_ALL_DAY, isAllDay);
        intent.putExtra(ChatRoomActivity.CONVERSATION_ID, conversationId);
        intent.putExtra(ChatRoomActivity.MY_ID, accountId);

        startActivity(intent);
    }
}
