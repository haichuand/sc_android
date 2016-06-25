package com.mono.provider;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.CalendarContract;
import android.provider.ContactsContract;

import com.mono.EventManager;
import com.mono.contacts.ContactsManager;

/**
 * This content observer is used to detect changes in content providers such as the Calendar and
 * Contacts Provider. Changes are delivered to the proper manager classes to be handled.
 *
 * @author Gary Ng
 */
public class MainContentObserver extends ContentObserver {

    private static final Uri[] CONTENT_URIS = {
        CalendarContract.CONTENT_URI,
        ContactsContract.AUTHORITY_URI
    };

    private Context context;

    public MainContentObserver(Context context, Handler handler) {
        super(handler);
        this.context = context.getApplicationContext();
    }

    @Override
    public void onChange(boolean selfChange) {
        onChange(selfChange, null);
    }

    /**
     * Handle any changes detected and determine where the change occurred to inform the
     * appropriate handler.
     *
     * @param selfChange The status of the change.
     * @param uri The content URI where change has occurred.
     */
    @Override
    public void onChange(boolean selfChange, Uri uri) {
        if (uri.equals(CONTENT_URIS[0])) {
            onCalendarChange(selfChange);
        } else if (uri.equals(CONTENT_URIS[1])) {
            onContactsChange(selfChange);
        }
    }

    /**
     * Register this content observer to the content resolver.
     */
    public void register() {
        for (Uri contentUri : CONTENT_URIS) {
            context.getContentResolver().registerContentObserver(contentUri, true, this);
        }
    }

    /**
     * Unregister this content observer from the content resolver.
     */
    public void unregister() {
        context.getContentResolver().unregisterContentObserver(this);
    }

    /**
     * Inform a calendar change has occurred to the Event Manager.
     *
     * @param selfChange The status of the change.
     */
    private void onCalendarChange(boolean selfChange) {
        if (!selfChange) {
            EventManager.getInstance(context).onProviderChange();
        }
    }

    /**
     * Inform a contact change has occurred to the Contacts Manager.
     *
     * @param selfChange The status of the change.
     */
    private void onContactsChange(boolean selfChange) {
        if (!selfChange) {
            ContactsManager.getInstance(context).onProviderChange();
        }
    }
}
