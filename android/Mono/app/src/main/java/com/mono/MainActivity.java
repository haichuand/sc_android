package com.mono;

import android.app.Dialog;
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
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.SupportMapFragment;
import com.mono.chat.ChatRoomActivity;
import com.mono.chat.ConversationManager;
import com.mono.contacts.ContactsActivity;
import com.mono.contacts.ContactsManager;
import com.mono.details.EventDetailsActivity;
import com.mono.dummy.DummyActivity;
import com.mono.intro.IntroActivity;
import com.mono.locationSetting.LocationSettingActivity;
import com.mono.model.Account;
import com.mono.model.Attendee;
import com.mono.model.AttendeeUsernameComparator;
import com.mono.model.Calendar;
import com.mono.model.Conversation;
import com.mono.model.Event;
import com.mono.network.ChatServerManager;
import com.mono.network.HttpServerManager;
import com.mono.provider.CalendarProvider;
import com.mono.settings.Settings;
import com.mono.settings.SettingsActivity;
import com.mono.util.Colors;
import com.mono.util.Common;
import com.mono.util.GoogleClient;
import com.mono.util.OnBackPressedListener;
import com.mono.util.SimpleTabLayout;
import com.mono.web.WebActivity;

import java.util.ArrayList;
import java.util.Collections;
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
    public static final int INTRO = R.id.nav_intro;
    public static final int LOGIN = R.id.nav_login;
    public static final int LOGOUT = R.id.nav_logout;
    public static final int CONTACTS = R.id.nav_contacts;
    public static final int SETTINGS = R.id.nav_settings;
    public static final int LOCATION_SETTING = R.id.nav_location_setting;
    public static final int DUMMY = R.id.nav_dummy;

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

//    private Attendee selectedAttendee = null; //attendee selected in AutoCompleteTextView

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
        // Simple Trick for Faster Google Maps Loading
        triggerGooglePlayServices(this);
        // Preload Contacts for Faster Access
        ContactsManager.getInstance(this).getContactsAsync(null, false);
        // Load Initial Fragment
        showHome();
        // Display Splash Screen for New Install
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
            // Close Hidden Side Menu
            drawer.closeDrawer(GravityCompat.START);
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
            case INTRO:
                showIntro();
                break;
            case LOGIN:
                showLogin();
                break;
            case LOGOUT:
                AccountManager.getInstance(this).logout();
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
            case DUMMY:
                showDummy();
                break;
        }

        drawer.closeDrawer(GravityCompat.START);

        return true;
    }

    /**
     * Set the action bar title using a string resource.
     *
     * @param resId The string resource containing the title.
     */
    @Override
    public void setToolbarTitle(int resId) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
        }

        toolbar.setTitle(resId);
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
            resId = R.drawable.ic_add_white;
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
     * @param resId The string resource containing the message.
     * @param actionResId The string resource to be used as the clickable text.
     * @param actionColor The color of the clickable text.
     * @param listener The listener used to respond to the clickable text.
     */
    @Override
    public void showSnackBar(int resId, int actionResId, int actionColor,
            OnClickListener listener) {
        View view = findViewById(android.R.id.content);

        snackBar = Snackbar.make(view, resId, Snackbar.LENGTH_LONG);
        snackBar.setAction(actionResId, listener);
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
     * @param force The value used to bypass the sync delay.
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
        Account account = AccountManager.getInstance(this).getAccount();
        if (account == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivityForResult(intent, RequestCodes.Activity.LOGIN_CHAT);
            return;
        }

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
                    List<Calendar> calendarList = CalendarProvider.getInstance(this).getCalendars();
                    for (Calendar calendar : calendarList) {
                        calendars.add(calendar.id);
                    }
                    settings.setCalendars(calendars);
                    // Mark Now as Day One
                    settings.setDayOne(System.currentTimeMillis());
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
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
        if (resultCode == RESULT_OK) {
            Account account = data.getParcelableExtra(LoginActivity.EXTRA_ACCOUNT);
            AccountManager.getInstance(this).setAccount(account);
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
     * Display the Google login activity primarily used for location tracking.
     *
     * @param fragment The fragment used to handle the result from the activity.
     * @param requestCode The request code used to distinguish the result.
     */
    @Override
    public void showWebActivity(Fragment fragment, int requestCode) {
        Intent intent = new Intent(this, WebActivity.class);

        if (fragment == null) {
            startActivityForResult(intent, requestCode);
        } else {
            fragment.startActivityForResult(intent, requestCode);
        }
    }

    /**
     * Display the event details screen activity.
     *
     * @param event The event to be displayed or edited.
     */
    @Override
    public void showEventDetails(Event event) {
        Calendar calendar;

        if (event.calendarId > 0) {
            calendar = CalendarProvider.getInstance(this).getCalendar(event.calendarId);
        } else {
            // Local Calendar (Specific for this App)
            calendar = new Calendar(event.calendarId);
            calendar.name = getString(R.string.local_calendar);
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
            Event event = data.getParcelableExtra(EventDetailsActivity.EXTRA_EVENT);

            if (event.id != null) {
                // Update Existing Event
                EventManager.getInstance(this).updateEvent(
                    EventManager.EventAction.ACTOR_SELF,
                    event.id,
                    event,
                    null
                );
            } else {
                if (event.calendarId > 0) {
                    // Create Event into the Provider
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
                } else {
                    event.internalId = System.currentTimeMillis();
                    // Create Event into the Database
                    EventManager.getInstance(this).createEvent(
                        EventManager.EventAction.ACTOR_SELF,
                        event.calendarId,
                        event.internalId,
                        event.externalId,
                        event.type,
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
    }

    @Override
    public void showChat(String eventId) {
        Event event = EventManager.getInstance(this).getEvent(eventId, false);
        if (event == null) {
            return;
        }

//        Account account = AccountManager.getInstance(this).getAccount();
//        if (account == null) {
//            Intent intent = new Intent(this, LoginActivity.class);
//            intent.putExtra(EXTRA_EVENT_ID, eventId);
//
//            startActivityForResult(intent, RequestCodes.Activity.LOGIN_CHAT);
//            return;
//        }
        Account account = AccountManager.getInstance(this).getAccount();
        ConversationManager conversationManager = ConversationManager.getInstance(this);
        List<Conversation> conversations = conversationManager.getConversations(eventId);
        String conversationId;
        if (conversations.isEmpty()) { //create new chat
            if (!Common.isConnectedToInternet(this)) {
                Toast.makeText(this, "No network connection. Cannot create new event", Toast.LENGTH_SHORT).show();
                return;
            }

            showCreateChatDialog(account, event, conversationManager);
        } else {
            Conversation conversation = conversations.get(0);
            startChatRoomActivity(event.id, event.startTime, event.endTime, conversation.id, account.id+"");
        }
    }

    @Override
    public void showExistingChat(String conversationId) {
        ConversationManager conversationManager = ConversationManager.getInstance(this);
        Conversation conversation = conversationManager.getConversationById(conversationId);
        if (conversation == null) {
            Toast.makeText(this, "Cannot find conversation with id: " + conversationId, Toast.LENGTH_SHORT).show();
            return;
        }
        EventManager eventManager = EventManager.getInstance(this);
        Event event = null;
        if (conversation.eventId != null) {
            event = eventManager.getEvent(conversation.eventId, true);
        }

        String myId = String.valueOf(AccountManager.getInstance(this).getAccount().id);
        if (event == null) {
            startChatRoomActivity(null, 0, 0, conversationId, myId);
        } else {
            startChatRoomActivity(event.id, event.startTime, event.endTime, conversationId, myId);
        }
    }

    private void showCreateChatDialog(final Account account, final Event event, final ConversationManager conversationManager) {
        final HttpServerManager httpServerManager = new HttpServerManager(this);
        final String myId = account.id + "";
        final Dialog dialog = new Dialog(this);

        dialog.setContentView(R.layout.dialog_create_chat);
        final EditText titleInput = (EditText) dialog.findViewById(R.id.create_chat_title_input);
        titleInput.setText(event.title, TextView.BufferType.EDITABLE);
        final LinearLayout checkBoxLayout = (LinearLayout) dialog.findViewById(R.id.create_chat_attendees);

        final List<String> checkedChatAttendeeIds = new ArrayList<>(); //attendees with checkbox checked
        final List<String> listChatAttendeeIds = new ArrayList<>(); //all attendees in the checkbox list

        final CompoundButton.OnCheckedChangeListener checkedChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton checkBox, boolean isChecked) {
                String id = checkBox.getId() + "";
                if (isChecked) {
                    if (!checkedChatAttendeeIds.contains(id))
                        checkedChatAttendeeIds.add(id);
                } else {
                    checkedChatAttendeeIds.remove(id);
                }
            }
        };

        Attendee me = new Attendee(myId);
        me.firstName = "Me";
        me.isFriend = true;
        addCheckBoxFromAttendee(checkBoxLayout, me, checkedChangeListener, listChatAttendeeIds, checkedChatAttendeeIds, myId);
        for (Attendee attendee : event.attendees) {
            addCheckBoxFromAttendee(checkBoxLayout, attendee, checkedChangeListener, listChatAttendeeIds, checkedChatAttendeeIds, myId);
        }
        dialog.show();

        //set AutoCompleteTextView to show all users
        final AutoCompleteTextView addAttendeeTextView = (AutoCompleteTextView) dialog.findViewById(R.id.create_chat_add_attendees);
        List<Attendee> allUsersList = conversationManager.getAllUserList();
        Collections.sort(allUsersList, new AttendeeUsernameComparator());
        ArrayAdapter<Attendee> addAttendeeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, allUsersList);
        addAttendeeTextView.setAdapter(addAttendeeAdapter);
        addAttendeeTextView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    addAttendeeTextView.showDropDown();
                }
            }
        });
        addAttendeeTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addAttendeeTextView.showDropDown();
            }
        });

        addAttendeeTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Attendee attendee = (Attendee) adapterView.getItemAtPosition(i);
                if (listChatAttendeeIds.contains(attendee.id)) {
                    Toast.makeText(MainActivity.this, "User already in list", Toast.LENGTH_SHORT).show();
                    addAttendeeTextView.setText("");
                } else {
                    addCheckBoxFromAttendee(checkBoxLayout, attendee, checkedChangeListener, listChatAttendeeIds, checkedChatAttendeeIds, myId);
                    addAttendeeTextView.setText("");
                }
            }
        });

//        //set listener for add button
//        ImageView addAttendeeButton = (ImageView) dialog.findViewById(R.id.create_chat_add_button);
//        addAttendeeButton.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (selectedAttendee != null) {
//                    addCheckBoxFromAttendee(checkBoxLayout, selectedAttendee, checkedChangeListener);
//                    checkedChatAttendeeIds.add(selectedAttendee.id);
//                    listChatAttendeeIds.add(selectedAttendee.id);
//                    addAttendeeTextView.setText("");
//                    selectedAttendee = null;
//                }
//            }
//        });

        //set listeners for Create and Cancel buttons
        Button createButton = (Button) dialog.findViewById(R.id.create_chat_create_button);
        createButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkedChatAttendeeIds.size() <= 1) { //including myId
                    Toast.makeText(MainActivity.this, "Chat must have at least two participants", Toast.LENGTH_LONG).show();
                    return;
                }
                String conversationTitle = titleInput.getText().toString();
                if (conversationTitle.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Conversation title cannot be empty", Toast.LENGTH_LONG).show();
                    return;
                }
                String conversationId = conversationManager.createConversation(conversationTitle, event.id, checkedChatAttendeeIds, myId);
                Conversation conversation = conversationManager.getConversationById(conversationId);

                //checkedChatAttendeeIds should contain myId
                if (httpServerManager.createConversation(conversation.id, conversationTitle, myId, checkedChatAttendeeIds)) {
                    checkedChatAttendeeIds.remove(myId);
                    ChatServerManager chatServerManager = new ChatServerManager(MainActivity.this);
                    chatServerManager.startConversation(myId, conversation.id, checkedChatAttendeeIds);
                } else { //set conversation sync_needed flag to true
                    conversationManager.setConversationSyncNeeded(conversation.id, true);
                    checkedChatAttendeeIds.remove(myId);
                    Toast.makeText(MainActivity.this, "Error creating conversation on server.", Toast.LENGTH_LONG).show();
                }

//                ChatsFragment chatsFragment = (ChatsFragment) getSupportFragmentManager().findFragmentByTag(getString(R.string.fragment_chats));
//                if (chatsFragment != null) {
//                    chatsFragment.insert(Integer.MAX_VALUE, conversation, true);
//                }
                conversationManager.notifyListenersNewConversation(conversation, 0);

                startChatRoomActivity(event.id, event.startTime, event.endTime, conversation.id, myId);
                dialog.dismiss();
            }
        });
        Button cancelButton = (Button) dialog.findViewById(R.id.create_chat_cancel_button);
        cancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
    }

    private void addCheckBoxFromAttendee (LinearLayout checkBoxLayout, Attendee attendee, CompoundButton.OnCheckedChangeListener checkedChangeListener, List<String> attendeeIdList, List<String> checkedAttendeeIdList, String myId) {

        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        CheckBox checkBox = new CheckBox(this);
        checkBox.setLayoutParams(params);
        checkBox.setId(Integer.valueOf(attendee.id));
        checkBox.setText(attendee.toString());
        if (attendee.id.equals(myId)) {
            if (attendeeIdList.contains(myId)) { //do not add myId twice
                return;
            } else {
                attendeeIdList.add(myId);
                checkedAttendeeIdList.add(myId);
                checkBox.setChecked(true);
                checkBox.setEnabled(false);
            }
        } else if (attendee.isFriend) {
            checkBox.setChecked(true);
            checkBox.setEnabled(true);
            checkedAttendeeIdList.add(attendee.id);
        } else {
            checkBox.setChecked(false);
            checkBox.setEnabled(false);
        }
        attendeeIdList.add(attendee.id);
        checkBox.setOnCheckedChangeListener(checkedChangeListener);
        checkBoxLayout.addView(checkBox);
    }

    public void startChatRoomActivity(String eventId, long startTime, long endTime, String conversationId, String accountId) {
        Intent intent = new Intent(this, ChatRoomActivity.class);
        intent.putExtra(ChatRoomActivity.EVENT_ID, eventId);
//        intent.putExtra(ChatRoomActivity.EVENT_NAME, eventTitle);
        intent.putExtra(ChatRoomActivity.EVENT_START_TIME, startTime);
        intent.putExtra(ChatRoomActivity.EVENT_END_TIME, endTime);
        intent.putExtra(ChatRoomActivity.CONVERSATION_ID, conversationId);
        intent.putExtra(ChatRoomActivity.MY_ID, accountId);

        startActivityForResult(intent, RequestCodes.Activity.CHAT);
    }

    public void showLocationSetting() {
        Intent intent = new Intent(this, LocationSettingActivity.class);
        startActivityForResult(intent, RequestCodes.Activity.LOCATION_SETTING);
    }

    @Override
    public void showDummy() {
        Intent intent = new Intent(this, DummyActivity.class);
        startActivityForResult(intent, RequestCodes.Activity.DUMMY);
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
}
