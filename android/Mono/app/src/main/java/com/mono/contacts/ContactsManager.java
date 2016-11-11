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
import com.mono.util.Common;
import com.mono.util.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This manager class is used to centralize all contacts related actions such as retrieving
 * contacts from the Contacts Provider. Contacts retrieved from the provider are also cached here
 * to improve efficiency.
 *
 * @author Gary Ng
 */
public class ContactsManager {

    public static final int TYPE_USERS = 0;
    public static final int TYPE_CONTACTS = 1;
    public static final int TYPE_OTHERS = 2;

    private static ContactsManager instance;

    private Context context;

    private final Map<Long, Contact> usersMap = new HashMap<>();
    private final Map<Long, Contact> contactsMap = new HashMap<>();
    private final List<ContactsBroadcastListener> listeners = new ArrayList<>();

    private List<Long> contactIds;
    private long lastProviderChange;

    private ContactsManager(Context context) {
        this.context = context;
    }

    public static ContactsManager getInstance(Context context) {
        if (instance == null) {
            instance = new ContactsManager(context.getApplicationContext());
            // Keep Track of Contact IDs
            instance.contactIds = ContactsProvider.getInstance(context).getContactIds();
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
        Contact contact = null;

        if (type == Contact.TYPE_CONTACT) {
            contact = contactsMap.get(id);

            if (contact == null) {
                ContactsProvider provider = ContactsProvider.getInstance(context);
                contact = provider.getContact(id, true);

                if (contact != null) {
                    contactsMap.put(id, contact);
                }
            }
        } else if (type == Contact.TYPE_USER) {
            contact = usersMap.get(id);

            if (contact == null) {
                AttendeeDataSource dataSource =
                    DatabaseHelper.getDataSource(context, AttendeeDataSource.class);
                Attendee user = dataSource.getAttendeeById(String.valueOf(id));

                if (user != null) {
                    contact = userToContact(user);
                    usersMap.put(id, contact);
                }
            }
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
        Contact contact = null;

        AttendeeDataSource dataSource =
            DatabaseHelper.getDataSource(context, AttendeeDataSource.class);
        Attendee user = dataSource.getUser(email, phone);

        if (user != null) {
            contact = userToContact(user);
        } else {
            ContactsProvider provider = ContactsProvider.getInstance(context);
            long id = provider.getContactId(email, phone);

            if (id > 0) {
                contact = getContact(id, Contact.TYPE_CONTACT);
            }
        }

        return contact;
    }

    /**
     * Insert a contact into the cache.
     *
     * @param contact The instance of a contact.
     */
    public void setContact(Contact contact) {
        long id = contact.id;

        if (contact.type == Contact.TYPE_USER) {
            usersMap.put(id, contact);
        } else {
            contactsMap.put(id, contact);
        }
    }

    /**
     * Remove a contact from the cache.
     *
     * @param id The value of the contact ID.
     * @param type The type of contact.
     * @return an instance of the removed contact.
     */
    public Contact removeContact(long id, int type) {
        Contact contact = null;

        if (type == Contact.TYPE_CONTACT) {
            contact = contactsMap.remove(id);
        } else if (type == Contact.TYPE_USER) {
            contact = usersMap.remove(id);
        }

        return contact;
    }

    /**
     * Clears all users stored in the database.
     */
    public void reset() {
        AttendeeDataSource dataSource =
            DatabaseHelper.getDataSource(context, AttendeeDataSource.class);
        dataSource.clearAttendeeTable();

        usersMap.clear();
        contactsMap.clear();
    }

    /**
     * Retrieve a list of users.
     *
     * @param terms Terms used to filter users.
     * @param offset Offset of contacts to start with.
     * @param limit Max number of results to return.
     * @return a list of contacts.
     */
    public List<Contact> getUsers(String[] terms, int offset, int limit) {
        List<Contact> result = new ArrayList<>();

//        Account account = AccountManager.getInstance(context).getAccount();
//        if (account == null || account.status == Account.STATUS_NONE) {
//            return result;
//        }

        AttendeeDataSource dataSource =
            DatabaseHelper.getDataSource(context, AttendeeDataSource.class);

        if (!dataSource.hasUsers()) {
            HttpServerManager manager = HttpServerManager.getInstance(context);
            manager.addAllRegisteredUsersToUserTable(dataSource);

            for (Attendee user : dataSource.getUsers(null, 0, 0)) {
                dataSource.setFriend(user.id, false);
            }
        }

        List<Attendee> users = dataSource.getUsers(terms, offset, limit);

        if (!users.isEmpty()) {
            for (Attendee user : users) {
                Contact contact = userToContact(user);
                result.add(contact);
            }

            Contact self = getAccountContact();
            if (self != null) {
                result.remove(self);
            }
        }

        return result;
    }

    /**
     * Retrieve a list of either local or other contacts.
     *
     * @param visible Return only visible contacts.
     * @param terms Terms used to filter users.
     * @param offset Offset of contacts to start with.
     * @param limit Max number of results to return.
     * @return a list of contacts.
     */
    public List<Contact> getContacts(boolean visible, String[] terms, int offset, int limit) {
        ContactsProvider provider = ContactsProvider.getInstance(context);
        List<Contact> result = provider.getContacts(visible, terms, offset, limit, true);

        if (!result.isEmpty()) {
            Contact self = getAccountContact();
            if (self != null) {
                result.remove(self);
            }
        }

        return result;
    }

    /**
     * Retrieve users or contacts in the background. Results are passed through callbacks.
     *
     * @param types Types of contacts to retrieve.
     * @param terms Terms used to filter users.
     * @param offset Offset of contacts to start with.
     * @param limit Max number of results to return.
     * @param callback Callback to invoke.
     * @return a list of contacts.
     */
    public AsyncTask<Object, Contact, List<Contact>> getContactsAsync(int[] types, String[] terms,
            final int offset, int limit, ContactsTaskCallback callback) {
        return new AsyncTask<Object, Contact, List<Contact>>() {
            private ContactsTaskCallback callback;

            @Override
            protected List<Contact> doInBackground(Object... params) {
                int[] types = (int[]) params[0];
                String[] terms = (String[]) params[1];
                int limit = (int) params[2];
                callback = (ContactsTaskCallback) params[3];

                List<Contact> result = new ArrayList<>();

                for (int type : types) {
                    switch (type) {
                        case TYPE_USERS:
                            result.addAll(getUsers(terms, offset, limit));
                            break;
                        case TYPE_CONTACTS:
                            result.addAll(getContacts(true, terms, offset, limit));
                            break;
                        case TYPE_OTHERS:
                            result.addAll(getContacts(false, terms, offset, limit));
                            break;
                    }
                }

                return result;
            }

            @Override
            protected void onPostExecute(List<Contact> result) {
                if (callback != null) {
                    callback.onFinish(result);
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, types, terms, limit, callback);
    }

    /**
     * Retrieve users in the background. Results are passed through callbacks.
     *
     * @param terms Terms used to filter users.
     * @param offset Offset of contacts to start with.
     * @param limit Max number of results to return.
     * @param callback Callback to invoke.
     */
    public AsyncTask<Object, Contact, List<Contact>> getUsersAsync(String[] terms, int offset,
            int limit, ContactsTaskCallback callback) {
        return getContactsAsync(new int[]{TYPE_USERS}, terms, offset, limit, callback);
    }

    /**
     * Retrieve local contacts in the background. Results are passed through callbacks.
     *
     * @param terms Terms used to filter users.
     * @param offset Offset of contacts to start with.
     * @param limit Max number of results to return.
     * @param callback Callback to invoke.
     */
    public AsyncTask<Object, Contact, List<Contact>> getLocalContactsAsync(String[] terms,
            int offset, int limit, ContactsTaskCallback callback) {
        return getContactsAsync(new int[]{TYPE_CONTACTS}, terms, offset, limit, callback);
    }

    /**
     * Retrieve other contacts in the background. Results are passed through callbacks.
     *
     * @param terms Terms used to filter users.
     * @param offset Offset of contacts to start with.
     * @param limit Max number of results to return.
     * @param callback Callback to invoke.
     */
    public AsyncTask<Object, Contact, List<Contact>> getOtherContactsAsync(String[] terms,
            int offset, int limit, ContactsTaskCallback callback) {
        return getContactsAsync(new int[]{TYPE_OTHERS}, terms, offset, limit, callback);
    }

    /**
     * Helper function to convert a user to a contact.
     *
     * @param user User to be converted.
     * @return instance of contact.
     */
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
        } else if (!Common.isEmpty(contact.getEmail()) || !Common.isEmpty(contact.getPhone())) {
            // Use Existing Photo from Contact
            ContactsProvider provider = ContactsProvider.getInstance(instance.context);
            long id = provider.getContactId(contact.getEmail(), contact.getPhone());

            if (id > 0) {
                Contact temp = instance.getContact(id, Contact.TYPE_CONTACT);
                if (temp != null && temp.photo != null) {
                    contact.photo = temp.photo;
                }
            }
        }

        contact.isFavorite = user.isFavorite;
        contact.isFriend = user.isFriend;
        contact.isSuggested = user.isSuggested;

        return contact;
    }

    /**
     * Helper function to convert an account to a contact.
     *
     * @return an instance of the contact.
     */
    public Contact getAccountContact() {
        Contact contact = null;

        Account account = AccountManager.getInstance(context).getAccount();

        if (account != null) {
            contact = new Contact(account.id);

            if (account.email != null) {
                contact.setEmail(account.email);
            }

            if (account.phone != null) {
                contact.setPhone(account.phone);
            }
        }

        return contact;
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

        for (ContactsBroadcastListener listener : listeners) {
            listener.onSuggestionAdd(contact);
        }
    }

    /**
     * Handle changes occurred in the Contacts Provider.
     */
    public void onProviderChange() {
        long currentTime = System.currentTimeMillis();
        if (lastProviderChange == 0) {
            lastProviderChange = currentTime - Constants.DAY_MS;
        }

        ContactsProvider provider = ContactsProvider.getInstance(context);
        List<Long> result = provider.getLastUpdatedContactIds(lastProviderChange);

        if (!result.isEmpty()) {
            // Handle New and Updated Contacts
            System.out.format("Contacts Provider has %d changes.\n", result.size());

            for (long id : result) {
                Contact contact = getContact(id, Contact.TYPE_CONTACT);

                if (!contactIds.contains(id)) {
                    contactIds.add(id);

                    for (ContactsBroadcastListener listener : listeners) {
                        listener.onContactAdd(contact);
                    }
                } else {
                    for (ContactsBroadcastListener listener : listeners) {
                        listener.onContactRefresh(contact);
                    }
                }
                // Handle Suggestions
                SuggestionsTask task = new SuggestionsTask(context, 0);
                List<Contact> suggestions = task.checkSuggestions(contact);

                for (Contact suggestion : suggestions) {
                    setSuggested(suggestion.id, Contact.SUGGESTION_PENDING);
                }
            }
        } else {
            // Handle Deleted Contacts
            System.out.println("Contacts Provider has at least 1 deletion.");
            // Check for Contacts to Remove
            List<Long> removeIds = new ArrayList<>(contactIds);
            removeIds.removeAll(provider.getContactIds());
            // Remove Contacts
            for (long id : removeIds) {
                Contact contact = removeContact(id, Contact.TYPE_CONTACT);

                if (contact != null) {
                    for (ContactsBroadcastListener listener : listeners) {
                        listener.onContactRemove(contact);
                    }
                }
            }

            contactIds.removeAll(removeIds);
        }

        lastProviderChange = currentTime;
    }

    public interface ContactsBroadcastListener {

        void onContactAdd(Contact contact);

        void onContactRefresh(Contact contact);

        void onContactRemove(Contact contact);

        void onSuggestionAdd(Contact contact);
    }

    public static abstract class ContactsTaskCallback {

        protected void onProgress(Contact contact) {}

        protected void onFinish(List<Contact> contacts) {}
    }
}
