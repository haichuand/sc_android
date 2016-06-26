package com.mono.details;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mono.R;
import com.mono.contacts.ContactsActivity;
import com.mono.contacts.ContactsManager;
import com.mono.model.Attendee;
import com.mono.model.Contact;
import com.mono.model.Event;
import com.mono.util.BitmapHelper;
import com.mono.util.Colors;
import com.mono.util.Common;
import com.mono.util.Pixels;
import com.mono.util.SimpleQuickAction;

import java.util.List;

/**
 * This class is used to handle the guests section located in Event Details.
 *
 * @author Gary Ng
 */
public class GuestPanel {

    private static final int ICON_DIMENSION_DP = 24;
    private static final int MARGIN_DP = 2;
    private static final int RADIUS_DP = 2;

    private static final String[] ACTIONS = {"Remove"};
    private static final int ACTION_REMOVE = 0;

    private EventDetailsActivity activity;
    private ViewGroup guests;

    private Event event;

    public GuestPanel(EventDetailsActivity activity) {
        this.activity = activity;
    }

    public void onCreate(Bundle savedInstanceState) {
        guests = (ViewGroup) activity.findViewById(R.id.guests);
    }

    /**
     * Initialize this panel using the given event.
     *
     * @param event The instance of the event.
     */
    public void setEvent(Event event) {
        this.event = event;

        guests.removeAllViews();
        createGuestInput();

        if (!event.attendees.isEmpty()) {
            ContactsManager manager = ContactsManager.getInstance(activity);

            for (Attendee user : event.attendees) {
                Contact contact = manager.getContact(user.email, user.phoneNumber);
                if (contact == null) {
                    contact = ContactsManager.userToContact(user);
                }

                createGuest(contact);
            }
        }
    }

    /**
     * Create a guest tag using the contact information available.
     *
     * @param contact The instance of the contact.
     */
    public void createGuest(Contact contact) {
        LayoutInflater inflater = LayoutInflater.from(activity);
        View view = inflater.inflate(R.layout.contacts_tag, null, false);
        // Handle Profile Photo
        ImageView icon = (ImageView) view.findViewById(R.id.icon);

        if (contact.photo != null) {
            int dimension = Pixels.pxFromDp(activity, ICON_DIMENSION_DP);
            Bitmap bitmap = BitmapHelper.createBitmap(contact.photo, dimension, dimension);

            int color = Colors.getColor(activity, R.color.colorPrimary);
            int radius = Pixels.pxFromDp(activity, RADIUS_DP);

            bitmap = BitmapHelper.createCircleBitmap(bitmap, color, radius);

            icon.setImageBitmap(bitmap);
        } else {
            icon.setImageResource(R.drawable.ic_account_circle_48dp);

            int color = Colors.getColor(activity, R.color.colorPrimary);
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

        int margin = Pixels.pxFromDp(activity, MARGIN_DP);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, margin, 0, margin);
        // Show Option to Remove
        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onGuestClick(guests.indexOfChild(view));
            }
        });

        guests.addView(view, Math.max(guests.getChildCount() - 1, 0), params);
    }

    /**
     * Handle the action of clicking on the guest tag. A popup of additional options will be shown
     * upon click.
     *
     * @param position The position of the guest.
     */
    public void onGuestClick(final int position) {
        final int guestPosition = position;

        View view = guests.getChildAt(position);

        SimpleQuickAction actionView = SimpleQuickAction.newInstance(activity);
        actionView.setColor(Colors.getColor(activity, R.color.colorPrimary));
        actionView.setActions(ACTIONS);

        int[] location = new int[2];
        view.getLocationInWindow(location);
        location[1] -= Pixels.Display.getStatusBarHeight(activity);
        location[1] -= Pixels.Display.getActionBarHeight(activity);

        int offsetX = location[0] + view.getWidth() / 2;
        int offsetY = view.getHeight();

        actionView.setPosition(location[0], location[1], offsetX, offsetY);
        actionView.setListener(new SimpleQuickAction.SimpleQuickActionListener() {
            @Override
            public void onActionClick(int position) {
                switch (position) {
                    case ACTION_REMOVE:
                        event.attendees.remove(guestPosition);
                        guests.removeViewAt(guestPosition);
                        break;
                }
            }

            @Override
            public void onDismiss() {

            }
        });

        ViewGroup content = (ViewGroup) activity.findViewById(android.R.id.content);
        if (content != null) {
            content.addView(actionView);
        }
    }

    /**
     * Create an input to enable user to create a user-defined contact with only a name.
     * Alternatively, user can use the contact picker for existing contacts.
     */
    public void createGuestInput() {
        LayoutInflater inflater = LayoutInflater.from(activity);
        View view = inflater.inflate(R.layout.contacts_tag_input, null, false);

        View contactPicker = view.findViewById(R.id.contact_picker);
        contactPicker.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showContactPicker();
            }
        });

        final EditText input = (EditText) view.findViewById(R.id.input);

        View submit = view.findViewById(R.id.submit);
        submit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onContactSubmit(input);
            }
        });

        int margin = Pixels.pxFromDp(activity, MARGIN_DP);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, margin, 0, margin);

        guests.addView(view, params);
    }

    /**
     * Display the contact picker to add guests to the event.
     */
    public void showContactPicker() {
        Intent intent = new Intent(activity, ContactsActivity.class);
        intent.putExtra(ContactsActivity.EXTRA_MODE, ContactsActivity.MODE_PICKER);
        activity.startActivityForResult(intent, EventDetailsActivity.REQUEST_CONTACT_PICKER);
    }

    /**
     * Handle the result from the contact picker. The result will be a contact returned from the
     * contact picker.
     *
     * @param resultCode The result code returned from the activity.
     * @param data The data returned from the activity.
     */
    public void handleContactPicker(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
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
                    createGuest(contact);
                }
            }
        }
    }

    /**
     * Handle the action of submitting the input of a user-defined contact.
     *
     * @param editText The input view.
     */
    public void onContactSubmit(EditText editText) {
        String text = editText.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }

        editText.setText("");

        Attendee user = new Attendee((long) (-10000 + Math.random() * -10000));
        user.firstName = text;
        user.lastName = "";

        if (!event.attendees.contains(user)) {
            event.attendees.add(user);

            Contact contact = new Contact(Long.parseLong(user.id));
            contact.displayName = user.firstName;

            createGuest(contact);
        }
    }
}
