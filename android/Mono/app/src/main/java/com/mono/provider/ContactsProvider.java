package com.mono.provider;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import com.mono.model.Contact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is used to provide access to the Contacts Provider to allow the retrieval of
 * contacts stored on the device.
 *
 * @author Gary Ng
 */
public class ContactsProvider {

    private static ContactsProvider instance;

    private Context context;

    private ContactsProvider(Context context) {
        this.context = context;
    }

    public static ContactsProvider getInstance(Context context) {
        if (instance == null) {
            instance = new ContactsProvider(context.getApplicationContext());
        }

        return instance;
    }

    /**
     * Retrieve a list of contacts stored on the device.
     *
     * @param visibleOnly The value to return only visible contacts.
     * @return a list of contacts.
     */
    public List<Contact> getContacts(boolean visibleOnly) {
        List<Contact> contacts = new ArrayList<>();

        String selection = ContactsContract.Data.MIMETYPE + " = ?";
        if (visibleOnly) {
            selection += " AND ";
            selection += ContactsContract.Data.IN_VISIBLE_GROUP + " > 0";
        }

        Cursor cursor = context.getContentResolver().query(
            ContactsContract.Data.CONTENT_URI,
            ContactsValues.Name.PROJECTION,
            selection,
            new String[]{
                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
            },
            ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME_PRIMARY
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                Contact contact = cursorToContact(cursor);
                contacts.add(contact);
            }

            cursor.close();
        }

        return contacts;
    }

    /**
     * Return a contacts instance with contact information retrieved from the cursor.
     *
     * @param cursor The cursor to be accessed.
     * @return an instance of a contact.
     */
    private Contact cursorToContact(Cursor cursor) {
        Contact contact = new Contact(cursor.getLong(ContactsValues.Name.INDEX_ID));
        contact.visible = cursor.getInt(ContactsValues.Name.INDEX_VISIBLE) > 0;
        contact.displayName = cursor.getString(ContactsValues.Name.INDEX_DISPLAY_NAME);
        contact.fullName = cursor.getString(ContactsValues.Name.INDEX_FULL_NAME);
        contact.firstName = cursor.getString(ContactsValues.Name.INDEX_FIRST_NAME);
        contact.middleName = cursor.getString(ContactsValues.Name.INDEX_MIDDLE_NAME);
        contact.lastName = cursor.getString(ContactsValues.Name.INDEX_LAST_NAME);

        contact.photo = getPhoto(contact.id);
        contact.emails = getEmails(contact.id);
        contact.phones = getPhones(contact.id, true);

        return contact;
    }

    /**
     * Retrieve the thumbnail of a contact's photo.
     *
     * @param contactId The value of the contact ID.
     * @return a byte array of the photo.
     */
    private byte[] getPhoto(long contactId) {
        byte[] result = null;

        Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
        uri = Uri.withAppendedPath(uri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);

        Cursor cursor = context.getContentResolver().query(
            uri,
            new String[]{
                ContactsContract.Contacts.Photo.PHOTO
            },
            null,
            null,
            null
        );

        if (cursor != null) {
            if (cursor.moveToNext()) {
                result = cursor.getBlob(0);
            }

            cursor.close();
        }

        return result;
    }

    /**
     * Retrieve all emails of a contact.
     *
     * @param contactId The value of the contact ID.
     * @return a map of emails.
     */
    private Map<Integer, String> getEmails(long contactId) {
        Map<Integer, String> result = new HashMap<>();

        Cursor cursor = context.getContentResolver().query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            ContactsValues.Email.PROJECTION,
            ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
            new String[]{
                String.valueOf(contactId)
            },
            ContactsContract.CommonDataKinds.Email.TYPE
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                int type = cursor.getInt(ContactsValues.Email.INDEX_TYPE);
                String email = cursor.getString(ContactsValues.Email.INDEX_EMAIL);

                result.put(type, email);
            }

            cursor.close();
        }

        return result;
    }

    /**
     * Retrieve all phone numbers of a contact.
     *
     * @param contactId The value of the contact ID.
     * @param normalized The value to return phone numbers as normalized format.
     * @return a map of phone numbers.
     */
    private Map<Integer, String> getPhones(long contactId, boolean normalized) {
        Map<Integer, String> result = new HashMap<>();

        Cursor cursor = context.getContentResolver().query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            ContactsValues.Phone.PROJECTION,
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
            new String[]{
                String.valueOf(contactId)
            },
            ContactsContract.CommonDataKinds.Phone.TYPE
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                int type = cursor.getInt(ContactsValues.Phone.INDEX_TYPE);

                String number;
                if (!normalized) {
                    number = cursor.getString(ContactsValues.Phone.INDEX_NUMBER);
                } else {
                    number = cursor.getString(ContactsValues.Phone.INDEX_NORMALIZED_NUMBER);
                }

                result.put(type, number);
            }

            cursor.close();
        }

        return result;
    }
}
