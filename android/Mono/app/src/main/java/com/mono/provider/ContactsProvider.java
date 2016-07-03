package com.mono.provider;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import com.mono.model.Contact;
import com.mono.util.Common;

import java.util.ArrayList;
import java.util.Collections;
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
     * Retrieve a contact by ID.
     *
     * @param id The value of the contact ID.
     * @param normalized The value to return phone numbers as normalized format.
     * @return an instance of a contact.
     */
    public Contact getContact(long id, boolean normalized) {
        Contact contact = null;

        List<String> args = new ArrayList<>();

        String selection = getMimeTypeSelection(args);

        selection += " AND ";
        selection += ContactsContract.Data.CONTACT_ID + " = ?";
        args.add(String.valueOf(id));

        String[] selectionArgs = args.toArray(new String[args.size()]);

        Cursor cursor = context.getContentResolver().query(
            ContactsContract.Data.CONTENT_URI,
            ContactsValues.Contact.PROJECTION,
            selection,
            selectionArgs,
            null
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                if (contact == null) {
                    contact = new Contact(id);
                }

                cursorToData(cursor, contact, normalized);
            }

            cursor.close();
        }

        return contact;
    }

    /**
     * Retrieve a list of contacts starting at the given contact ID.
     *
     * @param startId The starting contact ID.
     * @param normalized The value to return phone numbers as normalized format.
     * @return a list of contacts.
     */
    public List<Contact> getContacts(long startId, boolean normalized) {
        List<Contact> contacts = new ArrayList<>();

        List<String> args = new ArrayList<>();
        String selection = getMimeTypeSelection(args);

        selection += " AND ";
        selection += ContactsContract.Data.IN_VISIBLE_GROUP + " >= 0";

        selection += " AND ";
        selection += ContactsContract.Data.CONTACT_ID + " >= ?";
        args.add(String.valueOf(startId));

        String[] selectionArgs = args.toArray(new String[args.size()]);
        String order = ContactsContract.Data.CONTACT_ID;

        Cursor cursor = context.getContentResolver().query(
            ContactsContract.Data.CONTENT_URI,
            ContactsValues.Contact.PROJECTION,
            selection,
            selectionArgs,
            order
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

                cursorToData(cursor, contact, normalized);
            }

            contacts.addAll(contactsMap.values());

            cursor.close();
        }

        return contacts;
    }

    /**
     * Retrieve a list of contacts stored on the device.
     *
     * @param visible The value to return only visible or not contacts.
     * @param terms The search terms to be used.
     * @param offset The offset to start with.
     * @param limit The max number of results to return.
     * @param normalized The value to return phone numbers as normalized format.
     * @return a list of contacts.
     */
    public List<Contact> getContacts(boolean visible, String[] terms, int offset, int limit,
            boolean normalized) {
        List<Contact> contacts = new ArrayList<>();

        List<String> args = new ArrayList<>();
        String selection = getMimeTypeSelection(args);

        selection += " AND ";
        selection += ContactsContract.Data.IN_VISIBLE_GROUP + (visible ? " > " : " = ") + "0";

        if (terms != null) {
            selection += " AND ";
            selection += getIdSelection(args, terms, offset, limit);
        }

        String[] selectionArgs = args.toArray(new String[args.size()]);

        String order = String.format(
            "LOWER(%s), %s",
            ContactsContract.Data.DISPLAY_NAME,
            ContactsContract.Data.CONTACT_ID
        );

        Cursor cursor = context.getContentResolver().query(
            ContactsContract.Data.CONTENT_URI,
            ContactsValues.Contact.PROJECTION,
            selection,
            selectionArgs,
            order
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

                cursorToData(cursor, contact, normalized);
            }

            contacts.addAll(contactsMap.values());

            cursor.close();
        }

        return contacts;
    }

    /**
     * Retrieve the contact ID that contains either the email or phone number.
     *
     * @param email The value of the email.
     * @param phone The value of the phone number.
     * @return the contact ID.
     */
    public long getContactId(String email, String phone) {
        long contactId = -1;

        List<String> args = new ArrayList<>();

        String selection = getMimeTypeSelection(args);

        selection += " AND ";

        selection += "(";

        if (!Common.isEmpty(email)) {
            selection += ContactsContract.CommonDataKinds.Email.ADDRESS + " = ?";
            args.add(email);
        }

        if (!Common.isEmpty(phone)) {
            if (!Common.isEmpty(email)) {
                selection += " OR ";
            }

            selection += String.format(
                "%s = ? OR %s = ?",
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER
            );
            args.add(phone);
            args.add(phone);
        }

        selection += ")";

        String[] selectionArgs = args.toArray(new String[args.size()]);

        Cursor cursor = context.getContentResolver().query(
            ContactsContract.Data.CONTENT_URI,
            new String[]{
                ContactsContract.Data.CONTACT_ID,
            },
            selection,
            selectionArgs,
            null
        );

        if (cursor != null) {
            if (cursor.moveToNext()) {
                contactId = cursor.getLong(0);
            }

            cursor.close();
        }

        return contactId;
    }

    /**
     * Retrieve a list of contact IDs that contains either the name, email, phone number, etc.
     *
     * @param terms The search terms to be used.
     * @param offset The offset to start with.
     * @param limit The max number of results to return.
     * @return a list of contact IDs.
     */
    private List<Long> getContactIds(String[] terms, int offset, int limit) {
        List<Long> result = new ArrayList<>();

        List<String> args = new ArrayList<>();

        String selection = getMimeTypeSelection(args);

        selection += " AND ";
        selection += getLikeSelection(args, terms);

        String[] selectionArgs = args.toArray(new String[args.size()]);

        String order = String.format(
            "LOWER(%s), %s",
            ContactsContract.Data.DISPLAY_NAME,
            ContactsContract.Data.CONTACT_ID
        );

        if (limit > 0) {
            order += String.format(
                " LIMIT %d OFFSET %d",
                limit,
                offset
            );
        }

        Cursor cursor = context.getContentResolver().query(
            ContactsContract.Data.CONTENT_URI,
            new String[]{
                ContactsContract.Data.CONTACT_ID,
            },
            selection,
            selectionArgs,
            order
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                result.add(id);
            }

            cursor.close();
        }

        return result;
    }

    /**
     * Helper function to get MIME type selection and arguments.
     *
     * @param args The list to insert arguments.
     * @return a selection string.
     */
    private String getMimeTypeSelection(List<String> args) {
        String[] mimeTypes = {
            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
        };

        String selection = String.format(
            "%s IN (%s)",
            ContactsContract.Data.MIMETYPE,
            Common.repeat("?", mimeTypes.length, ", ")
        );

        Collections.addAll(args, mimeTypes);

        return selection;
    }

    /**
     * Helper function to retrieve the selection to perform a LIKE query.
     *
     * @param args The list to insert arguments.
     * @param terms The search terms to be used.
     * @return a selection string.
     */
    private String getLikeSelection(List<String> args, String[] terms) {
        String selection = "(";

        for (int i = 0; i < terms.length; i++) {
            if (i > 0) selection += " AND ";

            selection += "(";

            String[] fields = {
                ContactsContract.Data.DISPLAY_NAME,
                ContactsContract.Data.DATA1
            };

            for (int j = 0; j < fields.length; j++) {
                if (j > 0) selection += " OR ";
                selection += fields[j] + " LIKE '%' || ? || '%'";
                args.add(terms[i]);
            }

            selection += ")";
        }

        selection += ")";

        return selection;
    }

    /**
     * Helper function to retrieve the selection for contact IDs.
     *
     * @param args The list to insert arguments.
     * @param terms The search terms to be used.
     * @param offset The offset to start with.
     * @param limit The max number of results to return.
     * @return a selection string.
     */
    private String getIdSelection(List<String> args, String[] terms, int offset, int limit) {
        List<Long> contactIds = getContactIds(terms, offset, limit);

        String selection = String.format(
            "%s IN (%s)",
            ContactsContract.Data.CONTACT_ID,
            Common.repeat("?", contactIds.size(), ", ")
        );

        for (long id : contactIds) {
            args.add(String.valueOf(id));
        }

        return selection;
    }

    /**
     * Read contact information depending on MIME type from the cursor.
     *
     * @param cursor The cursor to be accessed.
     * @param contact The contact to be updated.
     * @param normalized The value to return phone numbers as normalized format.
     */
    private void cursorToData(Cursor cursor, Contact contact, boolean normalized) {
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
    public byte[] getPhoto(long contactId) {
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
    public Map<Integer, String> getEmails(long contactId) {
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
    public Map<Integer, String> getPhones(long contactId, boolean normalized) {
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

    /**
     * Retrieve a list of contact IDs from the provider.
     *
     * @return a list of contact IDs.
     */
    public List<Long> getContactIds() {
        List<Long> result = new ArrayList<>();

        Cursor cursor = context.getContentResolver().query(
            ContactsContract.Data.CONTENT_URI,
            new String[]{
                ContactsContract.Data.CONTACT_ID
            },
            null,
            null,
            null
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                result.add(id);
            }

            cursor.close();
        }

        return result;
    }

    /**
     * Retrieve a list of IDs of contacts that were last updated since the given time.
     *
     * @param milliseconds The start time since last updated.
     * @return a list of contact IDs.
     */
    public List<Long> getLastUpdatedContactIds(long milliseconds) {
        List<Long> result = new ArrayList<>();

        Cursor cursor = context.getContentResolver().query(
            ContactsContract.Data.CONTENT_URI,
            new String[]{
                ContactsContract.Data.CONTACT_ID
            },
            ContactsContract.Data.CONTACT_LAST_UPDATED_TIMESTAMP + " >= ?",
            new String[]{
                String.valueOf(milliseconds)
            },
            null
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                if (!result.contains(id)) {
                    result.add(id);
                }
            }

            cursor.close();
        }

        return result;
    }
}
