package com.mono.details;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.util.List;

/**
 * This class is used to handle the guests section located in Event Details.
 *
 * @author Gary Ng
 */
public class GuestPanel {

    private static final int ICON_DIMENSION_DP = 24;
    private static final int RADIUS_DP = 2;

    private EventDetailsActivity activity;

    private ViewGroup guests;
    private ImageView contactPicker;

    private Event event;

    public GuestPanel(EventDetailsActivity activity) {
        this.activity = activity;
    }

    public void onCreate(Bundle savedInstanceState) {
        guests = (ViewGroup) activity.findViewById(R.id.guests);

        contactPicker = (ImageView) activity.findViewById(R.id.contact_picker);
        contactPicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showContactPicker();
            }
        });
    }

    /**
     * Initialize this panel using the given event.
     *
     * @param event The instance of the event.
     */
    public void setEvent(Event event) {
        this.event = event;

        guests.removeAllViews();
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

        int margin = Pixels.pxFromDp(activity, 2);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, margin, 0, margin);
        // Show Dialog to Remove
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onGuestClick(guests.indexOfChild(view));
            }
        });

        guests.addView(view, params);
    }

    /**
     * Handle the action of clicking on the guest tag. A popup of additional options will be shown
     * upon click.
     *
     * @param position The position of the guest.
     */
    public void onGuestClick(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity,
            R.style.AppTheme_Dialog_Alert);
        builder.setItems(
            new CharSequence[]{
                activity.getString(R.string.remove)
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
}
