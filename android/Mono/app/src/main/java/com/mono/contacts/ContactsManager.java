package com.mono.contacts;

import android.content.Context;
import android.os.AsyncTask;

import com.mono.AccountManager;
import com.mono.db.DatabaseHelper;
import com.mono.db.dao.AttendeeDataSource;
import com.mono.model.Account;
import com.mono.model.Attendee;
import com.mono.model.Contact;
import com.mono.network.HttpServerManager;
import com.mono.provider.ContactsProvider;
import com.mono.settings.Settings;
import com.mono.util.Common;
import com.mono.util.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * This manager class is used to centralize all contacts related actions such as retrieving
 * contacts from the Contacts Provider. Contacts retrieved from the provider are also cached here
 * to improve efficiency.
 *
 * @author Gary Ng
 */
public class ContactsManager {

    private static final long DELAY = Constants.DAY_MS;

    private static ContactsManager instance;

    private Context context;

    private List<Contact> cache;
    private final List<ContactsBroadcastListener> listeners = new ArrayList<>();

    private List<Contact> suggestions;

    private AsyncTask<Boolean, Contact, List<Contact>> usersTask;
    private final List<ContactsTaskCallback> usersTaskCallbacks = new ArrayList<>();

    private AsyncTask<Boolean, Contact, List<Contact>> contactsTask;
    private final List<ContactsTaskCallback> contactsTaskCallbacks = new ArrayList<>();

    private AsyncTask<Boolean, Contact, List<Contact>> suggestionsTask;
    private final List<ContactsTaskCallback> suggestionsTaskCallbacks = new ArrayList<>();

    private ContactsManager(Context context) {
        this.context = context;
    }

    public static ContactsManager getInstance(Context context) {
        if (instance == null) {
            instance = new ContactsManager(context.getApplicationContext());
        }

        return instance;
    }

    /**
     * Add listener to observe changes in new and existing contacts.
     *
     * @param listener The callback listener.
     */
    public void addListener(ContactsBroadcastListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove listener from observing any future changes in new and existing contacts.
     *
     * @param listener The callback listener.
     */
    public void removeListener(ContactsBroadcastListener listener) {
        Iterator<ContactsBroadcastListener> iterator = listeners.iterator();

        while (iterator.hasNext()) {
            if (iterator.next() == listener) {
                iterator.remove();
            }
        }
    }

    /**
     * Retrieve a contact by ID and type whether it's a user or contact.
     *
     * @param id The value of the contact ID.
     * @param type The type of contact.
     * @return an instance of a contact.
     */
    public Contact getContact(long id, int type) {
        Contact contact = new Contact(id, type);

        int index = cache.indexOf(contact);
        if (index >= 0) {
            contact = cache.get(index);
        } else {
            contact = null;
        }

        return contact;
    }

    /**
     * Retrieve a contact by email or phone.
     *
     * @param email The email belonging to the contact.
     * @param phone The phone number belonging to the contact.
     * @return an instance of a contact.
     */
    public Contact getContact(String email, String phone) {
        Contact contact = new Contact(-1);
        contact.setEmail(email);
        contact.setPhone(phone);

        int index = cache.indexOf(contact);
        if (index >= 0) {
            contact = cache.get(index);
        } else {
            contact = null;
        }

        return contact;
    }

    /**
     * Insert a contact into the cache.
     *
     * @param contact The instance of a contact.
     */
    public void setContact(Contact contact) {
        if (cache.contains(contact)) {
            cache.remove(contact);
        }

        cache.add(contact);
    }

    /**
     * Clears all users stored in the database.
     */
    public void reset() {
        AttendeeDataSource dataSource =
            DatabaseHelper.getDataSource(context, AttendeeDataSource.class);
        dataSource.clearAttendeeTable();

        if (cache != null) {
            cache.clear();
            cache = null;
        }

        if (suggestions != null) {
            suggestions.clear();
            suggestions = null;
        }
    }

    public void addUsersCallback(ContactsTaskCallback callback) {
        if (callback == null) {
            return;
        }

        removeUsersCallback(callback);
        usersTaskCallbacks.add(callback);
    }

    public void removeUsersCallback(ContactsTaskCallback callback) {
        usersTaskCallbacks.remove(callback);
    }

    /**
     * Retrieve users in the background. Results are passed through callbacks.
     *
     * @param callback The callback to invoke.
     * @param refresh The value to trigger a wipe beforehand.
     */
    public void getUsersAsync(ContactsTaskCallback callback, boolean refresh) {
        addUsersCallback(callback);

        if (usersTask != null && usersTask.getStatus() == AsyncTask.Status.RUNNING) {
            System.out.println("Users Not Ready!");
            return;
        }

        usersTask = new AsyncTask<Boolean, Contact, List<Contact>>() {
            @Override
            protected List<Contact> doInBackground(Boolean... params) {
                boolean refresh = params[0];

                if (refresh) {
                    reset();
                }

                AttendeeDataSource dataSource =
                    DatabaseHelper.getDataSource(context, AttendeeDataSource.class);

                if (dataSource.getAttendees().isEmpty() || refresh) {
                    // Retrieve Users from Server
                    HttpServerManager manager = new HttpServerManager(context);
                    manager.addAllRegisteredUsersToUserTable(dataSource);
                }

                List<Contact> result = new ArrayList<>();
                // Retrieve Users from Database
                for (Attendee user : dataSource.getAttendees()) {
                    if (user.userName == null) {
                        continue;
                    }

                    Contact contact = userToContact(user);

                    if (isSelf(contact)) {
                        continue;
                    }

                    result.add(contact);
                }

                return result;
            }

            @Override
            protected void onPostExecute(List<Contact> result) {
                for (ContactsTaskCallback callback : usersTaskCallbacks) {
                    callback.onFinish(result);
                }
                usersTaskCallbacks.clear();

                usersTask = null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, refresh);
    }

    public void addContactsCallback(ContactsTaskCallback callback) {
        if (callback == null) {
            return;
        }

        removeContactsCallback(callback);
        contactsTaskCallbacks.add(callback);
    }

    public void removeContactsCallback(ContactsTaskCallback callback) {
        contactsTaskCallbacks.remove(callback);
    }

    /**
     * Retrieve contacts without a callback for suggestions.
     *
     * @param callback The callback to invoke.
     * @param refresh The value to trigger a wipe beforehand.
     */
    public void getContactsAsync(ContactsTaskCallback callback, boolean refresh) {
        getContactsAsync(callback, null, refresh);
    }

    /**
     * Retrieve contacts in the background. Results are passed through callbacks.
     *
     * @param callback The callback to invoke.
     * @param suggestionsCallback The callback for suggestions to invoke.
     * @param refresh The value to trigger a wipe beforehand.
     */
    public void getContactsAsync(ContactsTaskCallback callback,
            final ContactsTaskCallback suggestionsCallback, boolean refresh) {
        addContactsCallback(callback);
        addSuggestionsCallback(suggestionsCallback);

        if (contactsTask != null && contactsTask.getStatus() == AsyncTask.Status.RUNNING) {
            System.out.println("Contacts Not Ready!");
            return;
        }

        contactsTask = new AsyncTask<Boolean, Contact, List<Contact>>() {

            private boolean refresh;

            @Override
            protected List<Contact> doInBackground(Boolean... params) {
                refresh = params[0];

                List<Contact> result = new ArrayList<>();

                if (refresh && cache != null && !cache.isEmpty()) {
                    cache.clear();
                    cache = null;
                }

                if (cache == null) {
                    cache = new ArrayList<>();
                    // Retrieve Local Contacts from Device
                    List<Contact> contacts =
                        ContactsProvider.getInstance(context).getContacts(false);
                    for (Contact contact : contacts) {
                        if (isSelf(contact)) {
                            continue;
                        }

                        setContact(contact);
                    }
                    // Retrieve Users
                    AttendeeDataSource dataSource =
                        DatabaseHelper.getDataSource(context, AttendeeDataSource.class);
                    for (Attendee user : dataSource.getAttendees()) {
                        if (user.userName == null) {
                            continue;
                        }

                        Contact contact = userToContact(user);

                        if (isSelf(contact)) {
                            continue;
                        }

                        Contact localContact = null;
                        if (contacts.contains(contact)) {
                            localContact = contacts.get(contacts.indexOf(contact));
                        }

                        if (localContact != null && localContact.photo != null) {
                            contact.photo = localContact.photo;
                        }

                        setContact(contact);
                    }
                }

                if (!cache.isEmpty()) {
                    result.addAll(cache);
                    Collections.sort(result, new Comparator<Contact>() {
                        @Override
                        public int compare(Contact c1, Contact c2) {
                            return c1.displayName.compareToIgnoreCase(c2.displayName);
                        }
                    });
                }

                return result;
            }

            @Override
            protected void onPostExecute(List<Contact> result) {
                for (ContactsTaskCallback callback : contactsTaskCallbacks) {
                    callback.onFinish(result);
                }
                contactsTaskCallbacks.clear();

                contactsTask = null;
                // Retrieve User Suggestions
                long milliseconds = Settings.getInstance(context).getContactsScan();
                if (System.currentTimeMillis() - milliseconds > DELAY || refresh) {
                    getSuggestionsAsync(null, false);
                } else if (suggestionsCallback != null) {
                    suggestionsCallback.onFinish(null);
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, refresh);
    }

    public static Contact userToContact(Attendee user) {
        Contact contact = new Contact(Long.parseLong(user.id), Contact.TYPE_USER);

        if (!Common.isEmpty(user.email)) {
            contact.setEmail(user.email);
        }

        if (!Common.isEmpty(user.phoneNumber)) {
            contact.setPhone(user.phoneNumber);
        }

        contact.displayName = user.userName;
        contact.firstName = user.firstName;
        contact.lastName = user.lastName;

        if (!Common.isEmpty(user.mediaId)) {
            contact.photo = null;
        }

        contact.isFavorite = user.isFavorite;
        contact.isFriend = user.isFriend;
        contact.isSuggested = user.isSuggested;

        return contact;
    }

    public void addSuggestionsCallback(ContactsTaskCallback callback) {
        if (callback == null) {
            return;
        }

        removeSuggestionsCallback(callback);
        suggestionsTaskCallbacks.add(callback);
    }

    public void removeSuggestionsCallback(ContactsTaskCallback callback) {
        suggestionsTaskCallbacks.remove(callback);
    }

    /**
     * Retrieve user suggestions in the background. Results are passed through callbacks.
     *
     * @param callback The callback to invoke.
     * @param refresh The value to trigger a wipe beforehand.
     */
    public void getSuggestionsAsync(ContactsTaskCallback callback, boolean refresh) {
        addSuggestionsCallback(callback);

        if (suggestionsTask != null && suggestionsTask.getStatus() == AsyncTask.Status.RUNNING) {
            System.out.println("Suggestions Not Ready!");
            return;
        }

        suggestionsTask = new AsyncTask<Boolean, Contact, List<Contact>>() {
            @Override
            protected List<Contact> doInBackground(Boolean... params) {
                boolean refresh = params[0];

                List<Contact> result = new ArrayList<>();

                if (refresh && suggestions != null && !suggestions.isEmpty()) {
                    suggestions.clear();
                    suggestions = null;
                }

                if (suggestions == null) {
                    suggestions = new ArrayList<>();

                    List<Contact> exclude = new ArrayList<>();
                    // Exclude Friends and Suggested Users
                    AttendeeDataSource dataSource =
                        DatabaseHelper.getDataSource(context, AttendeeDataSource.class);
                    for (Attendee user : dataSource.getAttendees()) {
                        if (user.isFriend || user.isSuggested != 0) {
                            Contact contact = userToContact(user);
                            exclude.add(contact);

                            if (user.isSuggested != 0) {
                                result.add(contact);
                            }
                        }
                    }
                    // Retrieve Suggestions from Server
                    HttpServerManager manager = new HttpServerManager(context);

                    List<Contact> contacts =
                        ContactsProvider.getInstance(context).getContacts(false);
                    for (Contact contact : contacts) {
                        JSONObject json;
                        // Cross-reference by Emails
                        for (String email : contact.emails.values()) {
                            json = manager.send(null, HttpServerManager.GET_USER_BY_EMAIL_URL +
                                email, HttpServerManager.GET);
                            if (json == null) {
                                continue;
                            }

                            parse(result, contact, json, exclude);
                        }
                        // Cross-reference by Phone Numbers
                        for (String phone : contact.phones.values()) {
                            json = manager.send(null, HttpServerManager.GET_USER_BY_PHONE_URL +
                                phone, HttpServerManager.GET);
                            if (json == null) {
                                continue;
                            }

                            parse(result, contact, json, exclude);
                        }
                    }
                }

                return result;
            }

            private Contact parse(List<Contact> contacts, Contact contact, JSONObject json,
                    List<Contact> exclude) {
                Contact result = null;

                try {
                    String id = json.getString(HttpServerManager.UID);
                    String mediaId = json.getString(HttpServerManager.MEDIA_ID);
                    String email = json.getString(HttpServerManager.EMAIL);
                    String phone = json.getString(HttpServerManager.PHONE_NUMBER);
                    String firstName = json.getString(HttpServerManager.FIRST_NAME);
                    String lastName = json.getString(HttpServerManager.LAST_NAME);
                    String userName = json.getString(HttpServerManager.USER_NAME);

                    result = new Contact(Long.parseLong(id), Contact.TYPE_USER);
                    if (!Common.isEmpty(email)) {
                        result.setEmail(email);
                    }
                    if (!Common.isEmpty(phone)) {
                        result.setPhone(phone);
                    }

                    if (isSelf(contact) || exclude.contains(result)) {
                        return null;
                    }

                    result.displayName = contact.displayName;
                    result.firstName = firstName;
                    result.lastName = lastName;

                    result.photo = !Common.isEmpty(mediaId) ? null : contact.photo;

                    contact.isSuggested = Contact.SUGGESTION_PENDING;

                    contacts.add(result);
                    publishProgress(result);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                return result;
            }

            @Override
            protected void onProgressUpdate(Contact... values) {
                if (values.length > 0) {
                    Contact contact = values[0];
                    // Remember Suggestion
                    setSuggested(contact.id, Contact.SUGGESTION_PENDING);

                    for (ContactsTaskCallback callback : suggestionsTaskCallbacks) {
                        callback.onProgress(contact);
                    }
                }
            }

            @Override
            protected void onPostExecute(List<Contact> result) {
                suggestions = result;
                // Reset Contacts Scan Time
                Settings settings = Settings.getInstance(context);
                settings.setContactsScan(System.currentTimeMillis());

                for (ContactsTaskCallback callback : suggestionsTaskCallbacks) {
                    callback.onFinish(result);
                }
                suggestionsTaskCallbacks.clear();

                suggestionsTask = null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, refresh);
    }

    /**
     * Check if contact matches current user from the account.
     *
     * @param contact The contact to be checked.
     * @return a boolean of the status.
     */
    public boolean isSelf(Contact contact) {
        Account account = AccountManager.getInstance(context).getAccount();

        if (account != null) {
            if (contact.containsEmail(account.email) || contact.containsPhone(account.phone)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Save friend status into the database and update the cache.
     *
     * @param id The value of the contact ID.
     * @param status The value of status.
     */
    public void setFriend(long id, boolean status) {
        AttendeeDataSource dataSource =
            DatabaseHelper.getDataSource(context, AttendeeDataSource.class);
        dataSource.setFriend(String.valueOf(id), status);

        Contact contact = getContact(id, Contact.TYPE_USER);
        if (contact != null) {
            contact.isFriend = status;
        }
    }

    /**
     * Save favorite status into the database and update the cache.
     *
     * @param id The value of the contact ID.
     * @param status The value of status.
     */
    public void setFavorite(long id, boolean status) {
        AttendeeDataSource dataSource =
            DatabaseHelper.getDataSource(context, AttendeeDataSource.class);
        dataSource.setFavorite(String.valueOf(id), status);

        Contact contact = getContact(id, Contact.TYPE_USER);
        if (contact != null) {
            contact.isFavorite = status;
        }
    }

    /**
     * Save suggested status into the database and update the cache.
     *
     * @param id The value of the contact ID.
     * @param value The type of suggestion.
     */
    public void setSuggested(long id, int value) {
        AttendeeDataSource dataSource =
            DatabaseHelper.getDataSource(context, AttendeeDataSource.class);
        dataSource.setSuggested(String.valueOf(id), value);

        Contact contact = getContact(id, Contact.TYPE_USER);
        if (contact != null) {
            contact.isSuggested = value;
        }
    }

    public interface ContactsBroadcastListener {

        void onContact();
    }

    public static abstract class ContactsTaskCallback {

        protected void onProgress(Contact contact) {}

        protected void onFinish(List<Contact> contacts) {}
    }
}
