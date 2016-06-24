package com.mono.details;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.model.LatLng;
import com.mono.EventManager;
import com.mono.R;
import com.mono.contacts.ContactsActivity;
import com.mono.contacts.ContactsManager;
import com.mono.db.DatabaseHelper;
import com.mono.db.dao.MediaDataSource;
import com.mono.model.Attendee;
import com.mono.model.Calendar;
import com.mono.model.Contact;
import com.mono.model.Event;
import com.mono.model.Location;
import com.mono.model.Media;
import com.mono.provider.CalendarProvider;
import com.mono.util.BitmapHelper;
import com.mono.util.Colors;
import com.mono.util.Common;
import com.mono.util.GestureActivity;
import com.mono.util.Pixels;
import com.mono.util.SimpleQuickAction;
import com.mono.util.SimpleQuickAction.SimpleQuickActionListener;
import com.mono.util.TimeZoneHelper;
import com.mono.util.UriHelper;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * This activity displays information of an event as well as allowing the user to create and
 * modify an existing event.
 *
 * @author Gary Ng
 */
public class EventDetailsActivity extends GestureActivity {

    public static final String EXTRA_CALENDAR = "calendar";
    public static final String EXTRA_EVENT = "event";

    private static final int REQUEST_PLACE_PICKER = 1;
    private static final int REQUEST_CONTACT_PICKER = 2;
    private static final int REQUEST_PHOTO_PICKER = 3;

    private static final int ICON_DIMENSION_DP = 24;
    private static final int RADIUS_DP = 2;
    private static final int PHOTO_WIDTH_DP = 80;
    private static final int PHOTO_HEIGHT_DP = 60;

    private static final int THUMBNAIL_DIMENSION_PX = 128;

    private static final String[] PHOTO_ACTIONS = {"View", "Remove"};
    private static final int PHOTO_ACTION_VIEW = 0;
    private static final int PHOTO_ACTION_REMOVE = 1;

    private static final SimpleDateFormat DATE_FORMAT;
    private static final SimpleDateFormat TIME_FORMAT;
    private static final SimpleDateFormat DATETIME_FORMAT;

    private TextView calendar;
    private EditText title;
    private ImageView colorPicker;
    private TextView startDate;
    private TextView startTime;
    private TextView endDate;
    private TextView endTime;
    private CheckBox allDay;
    private TextView timeZoneView;
    private EditText location;
    private ImageView locationPicker;
    private EditText notes;
    private ViewGroup guests;
    private ImageView contactPicker;
    private ViewGroup photos;

    private Event original;
    private Event event;

    private Calendar currentCalendar;
    private int color;

    static {
        DATE_FORMAT = new SimpleDateFormat("EEE, MMMM d, yyyy", Locale.getDefault());
        TIME_FORMAT = new SimpleDateFormat("h:mm a", Locale.getDefault());
        DATETIME_FORMAT = new SimpleDateFormat("EEE, MMMM d, yyyy h:mm a", Locale.getDefault());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        calendar = (TextView) findViewById(R.id.calendar);
        calendar.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showCalendarPicker();
            }
        });

        title = (EditText) findViewById(R.id.title);
        title.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString().trim();
                event.title = !value.isEmpty() ? value : null;
            }
        });

        colorPicker = (ImageView) findViewById(R.id.color_picker);
        colorPicker.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showColorPicker();
            }
        });

        startDate = (TextView) findViewById(R.id.start_date);
        startDate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                long time = getTime(startDate, startTime, event.timeZone);
                onDateClick(time, event.timeZone, new DateTimePickerCallback() {
                    @Override
                    public void onSet(Date date) {
                        setStartTime(date);
                    }
                });
            }
        });

        startTime = (TextView) findViewById(R.id.start_time);
        startTime.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                long time = getTime(startDate, startTime, event.timeZone);
                onTimeClick(time, event.timeZone, new DateTimePickerCallback() {
                    @Override
                    public void onSet(Date date) {
                        setStartTime(date);
                    }
                });
            }
        });

        endDate = (TextView) findViewById(R.id.end_date);
        endDate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                long time = getTime(endDate, endTime, event.getEndTimeZone());
                onDateClick(time, event.getEndTimeZone(), new DateTimePickerCallback() {
                    @Override
                    public void onSet(Date date) {
                        setEndTime(date);
                    }
                });
            }
        });

        endTime = (TextView) findViewById(R.id.end_time);
        endTime.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                long time = getTime(endDate, endTime, event.getEndTimeZone());
                onTimeClick(time, event.getEndTimeZone(), new DateTimePickerCallback() {
                    @Override
                    public void onSet(Date date) {
                        setEndTime(date);
                    }
                });
            }
        });

        allDay = (CheckBox) findViewById(R.id.all_day);
        allDay.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onAllDayChecked(isChecked);
            }
        });

        timeZoneView = (TextView) findViewById(R.id.timezone);
        timeZoneView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
//                onTimeZoneClick(event.timeZone);
            }
        });

        location = (EditText) findViewById(R.id.location);
        location.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString().trim();

                Location location = null;

                if (!value.isEmpty()) {
                    location = new Location();
                    location.name = value;
                }

                event.location = location;
            }
        });

        locationPicker = (ImageView) findViewById(R.id.location_picker);
        locationPicker.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showPlacePicker();
            }
        });

        notes = (EditText) findViewById(R.id.notes);
        notes.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString().trim();
                event.description = !value.isEmpty() ? value : null;
            }
        });

        guests = (ViewGroup) findViewById(R.id.guests);

        contactPicker = (ImageView) findViewById(R.id.contact_picker);
        contactPicker.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showContactPicker();
            }
        });

        photos = (ViewGroup) findViewById(R.id.photos);

        Intent intent = getIntent();
        Event event = intent.getParcelableExtra(EXTRA_EVENT);
        Calendar calendar = intent.getParcelableExtra(EXTRA_CALENDAR);

        initialize(calendar, event);
    }

    @Override
    public void onBackPressed() {
        if (event != null && !event.equals(original)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this,
                R.style.AppTheme_Dialog_Alert);
            builder.setMessage(R.string.confirm_event_save);

            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            Intent data = new Intent();
                            data.putExtra(EXTRA_EVENT, event);
                            setResult(RESULT_OK, data);
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            break;
                    }

                    dialog.dismiss();
                    close();
                }
            };

            builder.setPositiveButton(R.string.yes, listener);
            builder.setNegativeButton(R.string.no, listener);

            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.event_details, menu);

        if (event.id != null) {
            menu.findItem(R.id.action_delete).setVisible(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_delete:
                onDelete();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_PLACE_PICKER:
                handlePlacePicker(resultCode, data);
                break;
            case REQUEST_CONTACT_PICKER:
                handleContactPicker(resultCode, data);
                break;
            case REQUEST_PHOTO_PICKER:
                handlePhotoPicker(resultCode, data);
                break;
        }
    }

    public void initialize(Calendar calendar, Event original) {
        if (original == null) {
            original = new Event();
        }

        this.original = original;
        event = new Event(original);
        currentCalendar = calendar;

        this.calendar.setText(calendar.name);

        if (event.title != null) {
            title.setText(event.title);
        }

        color = event.color != 0 ? event.color : calendar.color;
        setColorPicker(color);

        if (event.timeZone == null) {
            event.timeZone = TimeZone.getDefault().getID();
        }

        TimeZone timeZone = TimeZone.getTimeZone(event.timeZone);
        DATE_FORMAT.setTimeZone(timeZone);
        TIME_FORMAT.setTimeZone(timeZone);

        DateTime dateTime = new DateTime(DateTimeZone.forTimeZone(timeZone));

        if (event.startTime > 0) {
            dateTime = dateTime.withMillis(event.startTime);
        } else {
            dateTime = dateTime.withTime(dateTime.getHourOfDay(), 0, 0, 0);
            event.startTime = dateTime.getMillis();
        }

        startDate.setText(DATE_FORMAT.format(dateTime.toDate()));
        startTime.setText(TIME_FORMAT.format(dateTime.toDate()));

        if (event.endTime > 0) {
            dateTime = dateTime.withMillis(event.endTime);
        } else {
            dateTime = dateTime.withTime(dateTime.getHourOfDay(), 0, 0, 0);
            dateTime = dateTime.plusHours(1);
            event.endTime = dateTime.getMillis();
        }

        endDate.setText(DATE_FORMAT.format(dateTime.toDate()));
        endTime.setText(TIME_FORMAT.format(dateTime.toDate()));

        timeZoneView.setText(TimeZoneHelper.getTimeZoneGMTName(timeZone, event.startTime));
        allDay.setChecked(event.allDay);

        if (event.location != null) {
            location.setText(event.location.name);
        }

        if (event.description != null) {
            notes.setText(event.description);
        }

        guests.removeAllViews();
        if (!event.attendees.isEmpty()) {
            ContactsManager manager = ContactsManager.getInstance(this);

            for (Attendee user : event.attendees) {
                Contact contact = manager.getContact(user.email, user.phoneNumber);
                if (contact == null) {
                    contact = ContactsManager.userToContact(user);
                }

                addGuest(contact);
            }
        }

        photos.removeAllViews();
        createPhotoButton();

        if (!event.photos.isEmpty()) {
            for (Media photo : event.photos) {
                createPhoto(photo.uri, photo.thumbnail);
            }
        }
    }

    public void close() {
        event = null;
        onBackPressed();
    }

    public void onDelete() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppTheme_Dialog_Alert);
        builder.setMessage(R.string.confirm_event_delete);

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        EventManager manager = EventManager.getInstance(EventDetailsActivity.this);
                        manager.removeEvent(EventManager.EventAction.ACTOR_SELF, event.id,
                            new EventManager.EventActionCallback() {
                                @Override
                                public void onEventAction(EventManager.EventAction data) {
                                    if (data.getStatus() == EventManager.EventAction.STATUS_OK) {
                                        finish();
                                    }
                                }
                            }
                        );
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

    public void showCalendarPicker() {
        final List<Calendar> calendars = CalendarProvider.getInstance(this).getCalendars();
        final CharSequence[] items = new CharSequence[1 + calendars.size()];

        items[0] = getString(R.string.local_calendar);
        for (int i = 0; i < calendars.size(); i++) {
            items[1 + i] = calendars.get(i).name;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppTheme_Dialog_Alert);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                currentCalendar = which > 0 ? calendars.get(which - 1) : new Calendar(-1);
                event.calendarId = currentCalendar.id;

                if (event.color == 0) {
                    setColorPicker(currentCalendar.color);
                }

                calendar.setText(items[which]);
            }
        });
        builder.create().show();
    }

    public void showColorPicker() {
        int[] colors;
        if (event.color == 0) {
            colors = new int[]{currentCalendar.color};
        } else if (event.color != currentCalendar.color) {
            colors = new int[]{currentCalendar.color, event.color};
        } else {
            colors = new int[]{event.color};
        }

        ColorPickerDialog dialog = new ColorPickerDialog(
            this,
            colors,
            color,
            new ColorPickerDialog.OnColorSetListener() {
                @Override
                public void onColorSet(int color) {
                    setColorPicker(color);
                    event.color = color;
                }
            }
        );

        dialog.show();
    }

    public void setColorPicker(int color) {
        this.color = color;
        color |= 0xFF000000;
        colorPicker.setColorFilter(color);
    }

    public void showPlacePicker() {
        try {
            PlaceAutocomplete.IntentBuilder builder =
                new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN);

            Intent intent = builder.build(this);
            startActivityForResult(intent, REQUEST_PLACE_PICKER);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handlePlacePicker(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Place place = PlaceAutocomplete.getPlace(this, data);
            LatLng latLng = place.getLatLng();

            Location location = new Location(latLng.latitude, latLng.longitude);
            String name = place.getName().toString();
            String address = place.getAddress().toString();

            if (!address.startsWith(name)) {
                location.name = name + ", " + address;
            } else {
                location.name = address;
            }

            setLocation(location);
        }
    }

    public void setLocation(Location location) {
        this.location.setText(location.name);
        event.location = location;
    }

    public void addGuest(Contact contact) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.contacts_tag, null, false);
        // Handle Profile Photo
        ImageView icon = (ImageView) view.findViewById(R.id.icon);

        if (contact.photo != null) {
            int dimension = Pixels.pxFromDp(this, ICON_DIMENSION_DP);
            Bitmap bitmap = BitmapHelper.createBitmap(contact.photo, dimension, dimension);

            int color = Colors.getColor(this, R.color.colorPrimary);
            int radius = Pixels.pxFromDp(this, RADIUS_DP);

            bitmap = BitmapHelper.createCircleBitmap(bitmap, color, radius);

            icon.setImageBitmap(bitmap);
        } else {
            icon.setImageResource(R.drawable.ic_account_circle_48dp);

            int color = Colors.getColor(this, R.color.colorPrimary);
            icon.setColorFilter(color);
        }
        // Handle Contact Information
        String name;
        if (!Common.isEmpty(contact.firstName)) {
            name = String.format("%s %s", contact.firstName, contact.lastName);
        } else {
            name = contact.displayName;
        }
        // Contact Name
        TextView nameView = (TextView) view.findViewById(R.id.name);
        nameView.setText(name);

        int margin = Pixels.pxFromDp(this, 2);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, margin, 0, margin);
        // Show Dialog to Remove
        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onGuestClick(guests.indexOfChild(view));
            }
        });

        guests.addView(view, params);
    }

    public void onGuestClick(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppTheme_Dialog_Alert);
        builder.setItems(
            new CharSequence[]{
                getString(R.string.remove)
            },
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 0) {
                        event.attendees.remove(position);
                        guests.removeViewAt(position);
                    }
                }
            }
        );
        builder.create().show();
    }

    public void showContactPicker() {
        Intent intent = new Intent(this, ContactsActivity.class);
        intent.putExtra(ContactsActivity.EXTRA_MODE, ContactsActivity.MODE_PICKER);
        startActivityForResult(intent, REQUEST_CONTACT_PICKER);
    }

    public void handleContactPicker(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            List<Contact> contacts =
                data.getParcelableArrayListExtra(ContactsActivity.EXTRA_CONTACTS);

            for (Contact contact : contacts) {
                Attendee user = new Attendee(contact.id);

                String[] emails = contact.getEmails();
                if (emails != null && emails.length > 0) {
                    user.email = emails[0];
                }

                String[] phones = contact.getPhones();
                if (phones != null && phones.length > 0) {
                    user.phoneNumber = phones[0];
                }

                user.firstName = contact.firstName;
                user.lastName = contact.lastName;

                if (user.firstName == null && user.lastName == null) {
                    user.firstName = contact.displayName;
                }

                if (!event.attendees.contains(user)) {
                    event.attendees.add(user);
                    addGuest(contact);
                }
            }
        }
    }

    /**
     * Create a thumbnail from a byte array otherwise attempt to load it using the path given.
     * The resulting thumbnail will be appended to the photo section.
     *
     * @param uri The image path.
     * @param data The image data.
     */
    public void createPhoto(Uri uri, byte[] data) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.photos_item, null, false);

        ImageView image = (ImageView) view.findViewById(R.id.image);

        int width = Pixels.pxFromDp(this, PHOTO_WIDTH_DP);
        int height = Pixels.pxFromDp(this, PHOTO_HEIGHT_DP);

        Bitmap bitmap = null;
        if (data != null) {
            bitmap = BitmapHelper.createBitmap(data, width, height);
        } else if (Common.fileExists(uri.toString())) {
            bitmap = BitmapHelper.createBitmap(uri.toString(), width, height);
        }
        image.setImageBitmap(bitmap);

        int margin = Pixels.pxFromDp(this, 2);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            Pixels.pxFromDp(this, PHOTO_WIDTH_DP),
            Pixels.pxFromDp(this, PHOTO_HEIGHT_DP)
        );
        params.setMargins(margin, margin, margin, margin);

        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onPhotoClick(photos.indexOfChild(view));
            }
        });

        photos.addView(view, Math.max(photos.getChildCount() - 1, 0), params);
    }

    /**
     * Create a button to show photo picker to add images to event.
     */
    public void createPhotoButton() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.photos_item, null, false);

        ImageView image = (ImageView) view.findViewById(R.id.image);
        image.setImageResource(R.drawable.ic_photo_add);

        int dimension = Pixels.pxFromDp(this, PHOTO_HEIGHT_DP * 0.5f);
        RelativeLayout.LayoutParams imageParams =
            new RelativeLayout.LayoutParams(dimension, dimension);
        imageParams.addRule(RelativeLayout.CENTER_IN_PARENT);

        image.setLayoutParams(imageParams);

        int color = Colors.getColor(this, R.color.gray);
        image.getDrawable().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);

        int margin = Pixels.pxFromDp(this, 2);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            Pixels.pxFromDp(this, PHOTO_WIDTH_DP),
            Pixels.pxFromDp(this, PHOTO_HEIGHT_DP)
        );
        params.setMargins(margin, margin, margin, margin);

        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showPhotoPicker();
            }
        });

        photos.addView(view, params);
    }

    /**
     * Handle the action of clicking on the photo button. A popup of additional options will be
     * shown upon click.
     *
     * @param position The position of the photo.
     */
    public void onPhotoClick(int position) {
        final int photoPosition = position;

        View view = photos.getChildAt(position);

        SimpleQuickAction actionView = SimpleQuickAction.newInstance(this);
        actionView.setColor(Colors.getColor(this, R.color.colorPrimary));
        actionView.setActions(PHOTO_ACTIONS);

        int[] location = new int[2];
        view.getLocationInWindow(location);
        location[1] -= Pixels.Display.getStatusBarHeight(this);
        location[1] -= Pixels.Display.getActionBarHeight(this);

        int offsetX = location[0] + view.getWidth() / 2;
        int offsetY = view.getHeight();

        actionView.setPosition(location[0], location[1], offsetX, offsetY);
        actionView.setListener(new SimpleQuickActionListener() {
            @Override
            public void onActionClick(int position) {
                switch (position) {
                    case PHOTO_ACTION_VIEW:
                        Media photo = event.photos.get(photoPosition);
                        showPhotoViewer(photo);
                        break;
                    case PHOTO_ACTION_REMOVE:
                        event.photos.remove(photoPosition);
                        photos.removeViewAt(photoPosition);
                        break;
                }
            }

            @Override
            public void onDismiss() {

            }
        });

        ViewGroup content = (ViewGroup) findViewById(android.R.id.content);
        if (content != null) {
            content.addView(actionView);
        }
    }

    /**
     * Display the original photo in the photo viewer.
     *
     * @param photo The photo to be shown.
     */
    public void showPhotoViewer(Media photo) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("file://" + photo.uri), photo.type);

        startActivity(intent);
    }

    /**
     * Display the photo picker to add photos to the event.
     */
    public void showPhotoPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);

        startActivityForResult(Intent.createChooser(intent, "Select Photos"), REQUEST_PHOTO_PICKER);
    }

    /**
     * Handle the result from the photo picker. The result can either be a single or a set of
     * photos returned from the photo picker.
     *
     * @param resultCode The result code returned from the activity.
     * @param data The data returned from the activity.
     */
    public void handlePhotoPicker(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (data.getClipData() == null) {
                // Handle Single Image
                addPhoto(data.getData());
            } else {
                // Handle Multiple Images
                ClipData clipData = data.getClipData();
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    addPhoto(clipData.getItemAt(i).getUri());
                }
            }
        }
    }

    /**
     * Photos retrieved from the photo picker will be added to the event.
     *
     * @param contentUri The content URI of the photo.
     */
    private void addPhoto(Uri contentUri) {
        Uri uri = UriHelper.resolve(this, contentUri);
        // Check File Exists
        String path = uri.toString();
        if (!Common.fileExists(path)) {
            return;
        }
        // Check File Size
        long size = Common.fileSize(path);
        Media photo = new Media(uri, Media.IMAGE, size);
        // Prevent Duplicate Photo
        if (event.photos.contains(photo)) {
            return;
        }
        // Check for Existing Photo
        MediaDataSource dataSource = DatabaseHelper.getDataSource(this, MediaDataSource.class);
        Media media = dataSource.getMedia(path, Media.IMAGE, size);

        if (media != null) {
            photo = media;
        } else {
            photo.thumbnail = BitmapHelper.getBytes(path, THUMBNAIL_DIMENSION_PX,
                THUMBNAIL_DIMENSION_PX, BitmapHelper.FORMAT_JPEG, 100);
        }
        // Add Photo to Event
        event.photos.add(photo);
        createPhoto(uri, photo.thumbnail);
    }

    public void onDateClick(long milliseconds, String timeZone,
            final DateTimePickerCallback callback) {
        final DateTime dateTime = new DateTime(milliseconds, DateTimeZone.forID(timeZone));

        DatePickerDialog dialog = new DatePickerDialog(
            this,
            new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    DateTime newDateTime = dateTime.withDate(year, monthOfYear + 1, dayOfMonth);
                    callback.onSet(newDateTime.toDate());
                }
            },
            dateTime.getYear(),
            dateTime.getMonthOfYear() - 1,
            dateTime.getDayOfMonth()
        );

        dialog.show();
    }

    public void onTimeClick(long milliseconds, String timeZone,
            final DateTimePickerCallback callback) {
        final DateTime dateTime = new DateTime(milliseconds, DateTimeZone.forID(timeZone));

        TimePickerDialog dialog = new TimePickerDialog(
            this,
            new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    DateTime newDateTime = dateTime.withTime(hourOfDay, minute, 0, 0);
                    callback.onSet(newDateTime.toDate());
                }
            },
            dateTime.getHourOfDay(),
            dateTime.getMinuteOfHour(),
            DateFormat.is24HourFormat(this)
        );

        dialog.show();
    }

    public void onAllDayChecked(boolean isChecked) {
        int visibility = isChecked ? View.INVISIBLE : View.VISIBLE;

        startTime.setVisibility(visibility);
        endTime.setVisibility(visibility);
        timeZoneView.setVisibility(visibility);

        setTimeZone(isChecked ? "UTC" : TimeZone.getDefault().getID());

        event.allDay = isChecked;
    }

    public void onTimeZoneClick(String timeZone) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.dialog_timezone_item,
            TimeZoneHelper.getTimeZones(event.startTime));

        AlertDialog.Builder dialog = new AlertDialog.Builder(this, R.style.AppTheme_Dialog_Alert);
        dialog.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        dialog.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView parent) {

            }
        });

        dialog.show();
    }

    public void setStartTime(Date date) {
        event.startTime = date.getTime();

        TimeZone timeZone = TimeZone.getTimeZone(event.timeZone);

        DATE_FORMAT.setTimeZone(timeZone);
        startDate.setText(DATE_FORMAT.format(date));

        TIME_FORMAT.setTimeZone(timeZone);
        startTime.setText(TIME_FORMAT.format(date));

        timeZoneView.setText(TimeZoneHelper.getTimeZoneGMTName(timeZone, event.startTime));
    }

    public void setEndTime(Date date) {
        event.endTime = date.getTime();

        TimeZone timeZone = TimeZone.getTimeZone(event.getEndTimeZone());

        DATE_FORMAT.setTimeZone(timeZone);
        endDate.setText(DATE_FORMAT.format(date));

        TIME_FORMAT.setTimeZone(timeZone);
        endTime.setText(TIME_FORMAT.format(date));

        timeZoneView.setText(TimeZoneHelper.getTimeZoneGMTName(timeZone, event.endTime));
    }

    public void setTimeZone(String id) {
        event.timeZone = id;

        event.startTime = getTime(startDate, startTime, event.timeZone);
        event.endTime = getTime(endDate, endTime, event.getEndTimeZone());

        TimeZone timeZone = TimeZone.getTimeZone(id);
        timeZoneView.setText(TimeZoneHelper.getTimeZoneGMTName(timeZone, event.startTime));
    }

    public long getTime(TextView date, TextView time, String timeZone) {
        long milliseconds = -1;

        try {
            DATETIME_FORMAT.setTimeZone(TimeZone.getTimeZone(timeZone));
            milliseconds = DATETIME_FORMAT.parse(date.getText() + " " + time.getText()).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return milliseconds;
    }

    public interface DateTimePickerCallback {

        void onSet(Date date);
    }
}
