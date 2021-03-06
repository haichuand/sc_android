package com.mono.contacts;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.mono.AccountManager;
import com.mono.R;
import com.mono.db.DatabaseHelper;
import com.mono.db.dao.AttendeeDataSource;
import com.mono.model.Account;
import com.mono.model.Attendee;
import com.mono.model.Contact;
import com.mono.network.HttpServerManager;
import com.mono.provider.ContactsProvider;
import com.mono.settings.Settings;
import com.mono.util.Common;
import com.mono.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * This task is used to retrieve contact suggestions by cross-referencing contact information
 * against the existing users in the server.
 *
 * @author Gary Ng
 */
public class SuggestionsTask extends AsyncTask<Object, Contact, List<Contact>> {

    private Context context;
    private long startId;

    private ContactsManager manager;
    private ContactsProvider provider;
    private Settings settings;
    private AttendeeDataSource attendeeDataSource;

    public SuggestionsTask(Context context, long startId) {
        this.context = context;
        this.startId = startId;

        manager = ContactsManager.getInstance(context);
        provider = ContactsProvider.getInstance(context);
        settings = Settings.getInstance(context);
        attendeeDataSource = DatabaseHelper.getDataSource(context, AttendeeDataSource.class);
    }

    @Override
    protected List<Contact> doInBackground(Object... params) {
        Log.getInstance(context).debug(getClass().getSimpleName(), "Running");

        List<Contact> result = new ArrayList<>();

        // Retrieve Suggestions from Server
        List<Contact> contacts = provider.getContacts(startId, true);
        if (contacts == null || contacts.isEmpty()) {
            return result;
        }
        List<Contact> ignoreList = getSuggestionsIgnoreList();
        if (ignoreList != null && !ignoreList.isEmpty()) {
            contacts.removeAll(ignoreList);
        }
        HttpServerManager httpServerManager = HttpServerManager.getInstance(context);
        result = httpServerManager.getSuggestedContacts(contacts);
        for (Contact suggestedContact : result) {
            if (saveSuggestionToDB(suggestedContact)) {
                publishProgress(suggestedContact);
            }
        }

//        for (Contact contact : contacts) {
//            List<Contact> suggestions = checkSuggestions(contact);
//            for (Contact suggestion : suggestions) {
//                if (suggestion != null) {
//                    result.add(suggestion);
//                    publishProgress(suggestion);
//                }
//            }
//        }

        return result;
    }

    @Override
    protected void onProgressUpdate(Contact... values) {
        Contact contact = values[0];
        // Handle Suggestion
        manager.setSuggested(contact.id, Contact.SUGGESTION_PENDING);
        settings.setContactsScanId(contact.id);
    }

    @Override
    protected void onPostExecute(List<Contact> result) {
        // Reset Contacts Scan Time
        settings.setContactsScan(System.currentTimeMillis());
        settings.setContactsScanId(0);
    }

    /**
     * Retrieve a suggestions ignore list of contacts that is either a friend or have been
     * suggested before.
     *
     * @return a list of contacts.
     */
    private List<Contact> getSuggestionsIgnoreList() {
        List<Contact> result = new ArrayList<>();
        // Ignore Self
        Contact self = getAccountContact();

        if (self != null) {
            result.add(self);
        }
        // Ignore Friends and Suggested Contacts
        AttendeeDataSource dataSource =
            DatabaseHelper.getDataSource(context, AttendeeDataSource.class);

        for (Attendee user : dataSource.getAttendees()) {
            if (user.isFriend || user.isSuggested != 0) {
                Contact contact = ContactsManager.userToContact(user);
                result.add(contact);
            }
        }

        return result;
    }

    /**
     * Check if contact meets the criteria of a suggestion.
     *
     * @param contact The instance of the contact.
     * @return a list of suggestions.
     */
    public List<Contact> checkSuggestions(Contact contact) {
        return checkSuggestions(contact, getSuggestionsIgnoreList());
    }

    /**
     * Check if contact meets the criteria of a suggestion.
     *
     * @param contact The instance of the contact.
     * @param exclude The list of contacts to be excluded.
     * @return a list of suggestions.
     */
    public List<Contact> checkSuggestions(Contact contact, List<Contact> exclude) {
        List<Contact> suggestions = new ArrayList<>();
        HttpServerManager manager = HttpServerManager.getInstance(context);
        Contact suggestion;

        // Cross-reference by Emails
        if (contact.emails != null) {
            for (String email : contact.emails.values()) {
                JSONObject json = manager.send(null, HttpServerManager.GET_USER_BY_EMAIL_URL +
                    email, HttpServerManager.GET);

                if (json == null) {
                    continue;
                }

                suggestion = parse(json, contact);

                if (suggestion != null) {
                    //pass if fcmId is email, i.e. temporary user
                    if (suggestion.fcmId.contains("@")) {
                        continue;
                    }

                    if (exclude != null && exclude.contains(suggestion)) {
                        continue;
                    }
                    suggestion.isSuggested = Contact.SUGGESTION_PENDING;
                    saveSuggestionToDB(suggestion);
                    suggestions.add(suggestion);
                }
            }
        }
        // Cross-reference by Phone Numbers
        if (contact.phones != null) {
            for (String phone : contact.phones.values()) {
                JSONObject json = manager.send(null, HttpServerManager.GET_USER_BY_PHONE_URL +
                    phone, HttpServerManager.GET);

                if (json == null) {
                    continue;
                }

                suggestion = parse(json, contact);

                if (suggestion != null) {
                    //pass if fcmId is email, i.e. temporary user
                    if (suggestion.fcmId.contains("@")) {
                        continue;
                    }

                    if (exclude != null && exclude.contains(suggestion)) {
                        continue;
                    }
                    suggestion.isSuggested = Contact.SUGGESTION_PENDING;
                    saveSuggestionToDB(suggestion);
                    suggestions.add(suggestion);
                }
            }
        }

        return suggestions;
    }

    /**
     * Parse user information from the JSON object.
     *
     * @param json The user information.
     * @param contact The contact to be used to fill in missing information.
     * @return an instance of a user contact.
     */
    private Contact parse(JSONObject json, Contact contact) {
        Contact result = null;

        try {
            if (json.has("status") && json.getInt("status") == HttpServerManager.STATUS_NO_USER) {
                return result;
            }

            String id = json.getString(HttpServerManager.UID);
            String mediaId = json.getString(HttpServerManager.MEDIA_ID);
            String email = json.getString(HttpServerManager.EMAIL);
            String phone = json.getString(HttpServerManager.PHONE_NUMBER);
            String firstName = json.getString(HttpServerManager.FIRST_NAME);
            String lastName = json.getString(HttpServerManager.LAST_NAME);
            String userName = json.getString(HttpServerManager.USER_NAME);
            String fcmId = json.getString(HttpServerManager.FCM_ID);

            result = new Contact(Long.parseLong(id), Contact.TYPE_USER);
            if (!Common.isEmpty(email)) {
                result.setEmail(email);
            }
            if (!Common.isEmpty(phone)) {
                result.setPhone(phone);
            }

            result.displayName = contact.displayName;
            result.firstName = firstName;
            result.lastName = lastName;
            result.userName = userName;
            result.fcmId = fcmId;

            result.photo = !Common.isEmpty(mediaId) ? null : contact.photo;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Helper function to convert an account to a contact.
     *
     * @return an instance of the contact.
     */
    private Contact getAccountContact() {
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

    private boolean saveSuggestionToDB (Contact suggestion) {
        return attendeeDataSource.createAttendeeWithAttendeeId(
                String.valueOf(suggestion.id),
                suggestion.mediaId,
                (String) suggestion.emails.values().toArray()[0],
                suggestion.phones == null ? null : ((String) suggestion.phones.values().toArray()[0]),
                suggestion.firstName,
                suggestion.lastName,
                suggestion.userName,
                false,
                false,
                Contact.SUGGESTION_PENDING
        );
    }
}
