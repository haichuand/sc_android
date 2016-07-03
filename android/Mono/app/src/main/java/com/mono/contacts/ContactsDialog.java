package com.mono.contacts;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.mono.R;
import com.mono.model.Contact;
import com.mono.util.BitmapHelper;
import com.mono.util.Colors;
import com.mono.util.Common;
import com.mono.util.Pixels;

/**
 * A dialog that displays the profile of the selected contact consisting of name, email, phone,
 * photo, and etc. In addition, a set of selectable options is displayed as well to allow the user
 * to perform actions such as marking other users as friends, set them as favorites, and respond
 * to user suggestions that may be related to the current user.
 *
 * @author Gary Ng
 */
public class ContactsDialog extends AlertDialog.Builder {

    private static final int ICON_DIMENSION_DP = 50;
    private static final int RADIUS_DP = 2;

    private Context context;
    private ContactsFragment fragment;
    private Contact contact;

    private ContactsDialog(Context context, ContactsFragment fragment, Contact contact) {
        super(context, R.style.AppTheme_Dialog_Alert);

        this.context = context;
        this.fragment = fragment;
        this.contact = contact;
    }

    /**
     * Constructs an instance of this dialog using the available contact information and display
     * the corresponding list of options depending on the criteria given.
     *
     * @param fragment The contacts fragment used to pass back actions generated from this dialog.
     * @param contact The selected contact to be used to display.
     * @param group The group category that this contact belongs.
     * @return an instance of this dialog.
     */
    public static AlertDialog create(ContactsFragment fragment, Contact contact, int group) {
        Context context = fragment.getContext();

        ContactsDialog builder = new ContactsDialog(context, fragment, contact);
        // Custom Profile Header
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_contacts, null, false);
        // Handle Profile Photo
        ImageView icon = (ImageView) view.findViewById(R.id.icon);

        if (contact.photo != null) {
            int dimension = Pixels.pxFromDp(context, ICON_DIMENSION_DP);
            Bitmap bitmap = BitmapHelper.createBitmap(contact.photo, dimension, dimension);

            int color = Colors.getColor(context, R.color.colorPrimary);
            int radius = Pixels.pxFromDp(context, RADIUS_DP);

            bitmap = BitmapHelper.createCircleBitmap(bitmap, color, radius);

            icon.setImageBitmap(bitmap);
        } else {
            icon.setImageResource(R.drawable.ic_account_circle_48dp);

            int color = Colors.getColor(context, R.color.colorPrimary);
            icon.setColorFilter(color);
        }
        // Handle Contact Information
        String name = null, msg = null;
        if (contact.type == Contact.TYPE_CONTACT) {
            name = contact.displayName;

            if (contact.hasEmails()) {
                msg = Common.implode("\n", contact.getEmails());
            } else if (contact.hasPhones()) {
                msg = Common.implode("\n", contact.getFormattedPhones());
            }
        } else if (contact.type == Contact.TYPE_USER) {
            name = String.format("%s %s", contact.firstName, contact.lastName);

            if (contact.hasEmails()) {
                msg = contact.getEmail();
            } else if (contact.hasPhones()) {
                msg = contact.getFormattedPhone();
            }
        }
        // Contact Name
        TextView nameView = (TextView) view.findViewById(R.id.name);
        nameView.setText(name);
        // Contact Information (Email/Phone)
        TextView msgView = (TextView) view.findViewById(R.id.msg);
        msgView.setText(msg);
        // Set Custom Profile Header
        builder.setCustomTitle(view);
        // Selectable Options
        builder.setItems(builder.getItems(group), builder.getOnClickListener(group));

        return builder.create();
    }

    /**
     * Switch statement used to return an array of selectable options depending on the group the
     * contact belongs to.
     *
     * @param group The group category that this contact belongs.
     * @return an array of options.
     */
    private CharSequence[] getItems(int group) {
        CharSequence[] items = null;
        int[] resIds = null;

        switch (group) {
            case ContactsFragment.GROUP_FAVORITES:
                resIds = new int[]{
                    R.string.favorites_remove
                };
                break;
            case ContactsFragment.GROUP_FRIENDS:
                resIds = new int[]{
                    R.string.favorites_add,
                    R.string.friends_remove
                };
                break;
            case ContactsFragment.GROUP_SUGGESTIONS:
                resIds = new int[]{
                    R.string.friends_add,
                    R.string.skip
                };
                break;
            case ContactsFragment.GROUP_USERS:
                resIds = new int[]{
                    R.string.friends_add
                };
                break;
            case ContactsFragment.GROUP_CONTACTS:
            case ContactsFragment.GROUP_OTHER:
                resIds = new int[]{
                    R.string.invite
                };
                break;
        }

        if (resIds != null) {
            items = new CharSequence[resIds.length];
            for (int i = 0; i < resIds.length; i++) {
                items[i] = context.getString(resIds[i]);
            }
        }

        return items;
    }

    /**
     * Switch statement used to return a click listener depending on the group the contact belongs
     * to.
     *
     * @param group The group category that this contact belongs.
     * @return an instance of a click listener.
     */
    private OnClickListener getOnClickListener(int group) {
        OnClickListener listener = null;

        switch (group) {
            case ContactsFragment.GROUP_FAVORITES:
                listener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            onFavoritesRemove();
                        }
                    }
                };
                break;
            case ContactsFragment.GROUP_FRIENDS:
                listener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            onFavoritesAdd();
                        } else if (which == 1) {
                            onFriendsRemove();
                        }
                    }
                };
                break;
            case ContactsFragment.GROUP_SUGGESTIONS:
                listener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            onFriendsAdd();
                        } else if (which == 1) {
                            onSuggestionsRemove();
                        }
                    }
                };
                break;
            case ContactsFragment.GROUP_USERS:
                listener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            onFriendsAdd();
                        }
                    }
                };
                break;
            case ContactsFragment.GROUP_CONTACTS:
            case ContactsFragment.GROUP_OTHER:
                listener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            onInvite();
                        }
                    }
                };
                break;
        }

        return listener;
    }

    /**
     * Passes the result of the action performed using this dialog to the fragment to further
     * handle the following action.
     *
     * @param group The group category that this contact belongs.
     */
    private void add(int group) {
        fragment.add(group, contact, true);
    }

    /**
     * Handles the action of adding a friend as a favorite. Friend user now belongs to the
     * favorites category.
     */
    private void onFavoritesAdd() {
        add(ContactsFragment.GROUP_FAVORITES);
        // Inform the Contacts Manager
        ContactsManager.getInstance(context).setFavorite(contact.id, true);
    }

    /**
     * Handles the action of removing a friend as a favorite. Favorite user is returned back to
     * either the friends, suggestions, or users category.
     */
    private void onFavoritesRemove() {
        int group;

        if (contact.isFriend) {
            group = ContactsFragment.GROUP_FRIENDS;
        } else if (contact.isSuggested == Contact.SUGGESTION_PENDING) {
            group = ContactsFragment.GROUP_SUGGESTIONS;
        } else {
            group = ContactsFragment.GROUP_USERS;
        }

        add(group);
        // Inform the Contacts Manager
        ContactsManager.getInstance(context).setFavorite(contact.id, false);
    }

    /**
     * Handles the action of adding a user as a friend. User now belongs to the friends category.
     */
    private void onFriendsAdd() {
        add(ContactsFragment.GROUP_FRIENDS);
        // Inform the Contacts Manager
        ContactsManager.getInstance(context).setFriend(contact.id, true);
    }

    /**
     * Handles the action of removing a user as a friend. Friend user is returned back to either
     * the suggestions or users category.
     */
    private void onFriendsRemove() {
        int group;

        if (contact.isSuggested == Contact.SUGGESTION_PENDING) {
            group = ContactsFragment.GROUP_SUGGESTIONS;
        } else {
            group = ContactsFragment.GROUP_USERS;
        }

        add(group);
        // Inform the Contacts Manager
        ContactsManager.getInstance(context).setFriend(contact.id, false);
    }

    /**
     * Handles the action of removing a user as a suggestion. Suggested user is returned back to
     * the users category.
     */
    private void onSuggestionsRemove() {
        add(ContactsFragment.GROUP_USERS);
        // Inform the Contacts Manager
        ContactsManager.getInstance(context).setSuggested(contact.id, Contact.SUGGESTION_IGNORED);
    }

    /**
     * Handles the action of inviting a local contact to join the network.
     */
    private void onInvite() {

    }
}
