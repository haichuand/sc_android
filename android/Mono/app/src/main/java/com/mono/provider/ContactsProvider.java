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
     * @param normalized The value to return phone numbers as normalized format.
     * @return a list of contacts.
     */
    public List<Contact> getContacts(boolean visibleOnly, boolean normalized) {
        List<Contact> contacts = new ArrayList<>();

        String selection = null;
        if (visibleOnly) {
            selection = ContactsContract.Data.IN_VISIBLE_GROUP + " > 0";
        }

        Cursor cursor = context.getContentResolver().query(
            ContactsContract.Data.CONTENT_URI,
            ContactsValues.Contact.PROJECTION,
            selection,
            null,
            ContactsContract.Data.CONTACT_ID
        );

        if (cursor != null) {
            Map<Long, Contact> contactsMap = new HashMap<>();

            while (cursor.moveToNext()) {
                long id = cursor.getLong(ContactsValues.Contact.INDEX_ID);

                Contact contact = contactsMap.get(id);
                if (contact == null) {
                    contactsMap.put(id, contact = new Contact(id));
                    contact.visible = cursor.getInt(ContactsValues.Contact.INDEX_VISIBLE) > 0;
                }

                String mimeType = cursor.getString(ContactsValues.Contact.INDEX_MIME_TYPE);

                switch (mimeType) {
                    case ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE:
                        cursorToName(cursor, contact);
                        break;
                    case ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE:
                        cursorToPhoto(cursor, contact);
                        break;
                    case ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE:
                        cursorToEmail(cursor, contact);
                        break;
                    case ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE:
                        cursorToPhone(cursor, contact, normalized);
                        break;
                }
            }

            contacts.addAll(contactsMap.values());

            cursor.close();
        }

        return contacts;
    }

    /**
     * Read contact name information from the cursor.
     *
     * @param cursor The cursor to be accessed.
     * @param contact The contact to be updated.
     */
    private void cursorToName(Cursor cursor, Contact contact) {
        contact.displayName = cursor.getString(ContactsValues.Contact.INDEX_DISPLAY_NAME);
        contact.fullName = cursor.getString(ContactsValues.Contact.INDEX_FULL_NAME);
        contact.firstName = cursor.getString(ContactsValues.Contact.INDEX_FIRST_NAME);
        contact.middleName = cursor.getString(ContactsValues.Contact.INDEX_MIDDLE_NAME);
        contact.lastName = cursor.getString(ContactsValues.Contact.INDEX_LAST_NAME);
    }

    /**
     * Read contact photo information from the cursor.
     *
     * @param cursor The cursor to be accessed.
     * @param contact The contact to be updated.
     */
    private void cursorToPhoto(Cursor cursor, Contact contact) {
        contact.photo = cursor.getBlob(ContactsValues.Contact.INDEX_PHOTO);
    }

    /**
     * Read contact email information from the cursor.
     *
     * @param cursor The cursor to be accessed.
     * @param contact The contact to be updated.
     */
    private void cursorToEmail(Cursor cursor, Contact contact) {
        int type = cursor.getInt(ContactsValues.Contact.INDEX_EMAIL_TYPE);
        String email = cursor.getString(ContactsValues.Contact.INDEX_EMAIL_ADDRESS);

        contact.setEmail(type, email);
    }

    /**
     * Read contact phone information from the cursor.
     *
     * @param cursor The cursor to be accessed.
     * @param contact The contact to be updated.
     * @param normalized The value to return phone numbers as normalized format.
     */
    private void cursorToPhone(Cursor cursor, Contact contact, boolean normalized) {
        int type = cursor.getInt(ContactsValues.Contact.INDEX_PHONE_TYPE);

        String phone;
        if (!normalized) {
            phone = cursor.getString(ContactsValues.Contact.INDEX_PHONE_NUMBER);
        } else {
            phone = cursor.getString(ContactsValues.Contact.INDEX_PHONE_NORMALIZED_NUMBER);
        }

        contact.setPhone(type, phone);
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
