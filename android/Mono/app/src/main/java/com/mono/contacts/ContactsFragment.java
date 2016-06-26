package com.mono.contacts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mono.R;
import com.mono.contacts.ContactsAdapter.ContactItem;
import com.mono.contacts.ContactsAdapter.ContactsAdapterListener;
import com.mono.contacts.ContactsManager.ContactsBroadcastListener;
import com.mono.contacts.ContactsManager.ContactsTaskCallback;
import com.mono.model.Contact;
import com.mono.util.Colors;
import com.mono.util.Common;
import com.mono.util.SimpleDataSource;
import com.mono.util.SimpleLinearLayoutManager;
import com.mono.util.SimpleViewHolder.HolderItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A fragment that displays a list view of contacts and users found on the device. Contacts and
 * users are also displayed in categories such as favorites, friends, users, etc. It supports
 * multiple modes such as viewing and serving as a contact picker. When in viewing mode, contacts
 * can be selected to perform various actions to move them around into different categories.
 *
 * @author Gary Ng
 */
public class ContactsFragment extends Fragment implements ContactsAdapterListener,
        ContactsBroadcastListener {

    public static final int GROUP_FAVORITES = 0;
    public static final int GROUP_FRIENDS = 1;
    public static final int GROUP_SUGGESTIONS = 2;
    public static final int GROUP_USERS = 3;
    public static final int GROUP_CONTACTS = 4;
    public static final int GROUP_OTHER = 5;

    private EditText search;
    private RecyclerView recyclerView;
    private ContactsAdapter adapter;
    private TextView resultsText;
    private AlertDialog dialog;

    private final Map<String, ContactItem> items = new HashMap<>();
    private ContactsMap contactsMap = new ContactsMap();

    private int mode;

    private ContactsManager contactsManager;

    private ContactsTaskCallback contactsCallback;
    private ContactsTaskCallback usersCallback;
    private ContactsTaskCallback suggestionsCallback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        contactsManager = ContactsManager.getInstance(getContext());
        contactsManager.addListener(this);

        Intent intent = getActivity().getIntent();
        if (intent != null) {
            mode = intent.getIntExtra(ContactsActivity.EXTRA_MODE, ContactsActivity.MODE_VIEW);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);

        search = (EditText) view.findViewById(R.id.search);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                final String str = s.toString();
                if (!str.isEmpty()) {
                    removeContactsCallback();

                    contactsManager.getContactsAsync(
                        contactsCallback = new ContactsTaskCallback() {
                            @Override
                            protected void onFinish(List<Contact> contacts) {
                                onContactsFinish(contacts, str.trim());
                            }
                        },
                        false
                    );
                } else {
                    getContacts(false);
                }
            }
        });

        recyclerView = (RecyclerView) view.findViewById(R.id.list);
        recyclerView.setLayoutManager(new SimpleLinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter = new ContactsAdapter(this));
        // Initialize Group Categories
        int[][] array = {
            {GROUP_FAVORITES, R.string.favorites},
            {GROUP_FRIENDS, R.string.friends},
            {GROUP_SUGGESTIONS, R.string.suggestions},
            {GROUP_USERS, R.string.users},
            {GROUP_CONTACTS, R.string.local_contacts},
            {GROUP_OTHER, R.string.other_contacts}
        };

        for (int[] element : array) {
            final int group = element[0];
            int labelResId = element[1];

            adapter.add(new ContactsAdapter.ContactsGroup(group, labelResId,
                new SimpleDataSource<HolderItem>() {
                    @Override
                    public HolderItem getItem(int position) {
                        return getItemFromSource(group, position);
                    }

                    @Override
                    public int getCount() {
                        return contactsMap.size(group);
                    }
                }
            ));
        }

        resultsText = (TextView) view.findViewById(R.id.results_text);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        getContacts(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        removeUsersCallback();
        removeContactsCallback();
        removeSuggestionsCallback();

        contactsManager.removeListener(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.contacts, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_refresh:
                return onRefresh();
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Clears the current contacts and retrieve a new set of contacts.
     *
     * @return a boolean of the status.
     */
    public boolean onRefresh() {
        items.clear();
        contactsMap.clear();

        adapter.notifyDataSetChanged();

        removeUsersCallback();
        // Retrieve Users and Contacts
        contactsManager.getUsersAsync(usersCallback = new ContactsTaskCallback() {
            @Override
            protected void onFinish(List<Contact> contacts) {
                getContacts(true);
            }
        }, true);

        return true;
    }

    /**
     * Retrieve contacts from the Contacts Manager using callbacks.
     *
     * @param refresh The value to force a refresh.
     */
    public void getContacts(boolean refresh) {
        removeContactsCallback();
        removeSuggestionsCallback();

        contactsManager.getContactsAsync(
            contactsCallback = new ContactsTaskCallback() {
                @Override
                protected void onFinish(List<Contact> contacts) {
                    onContactsFinish(contacts, null);
                    adapter.setGroupProgress(GROUP_SUGGESTIONS, true);
                }
            },
            suggestionsCallback = new ContactsTaskCallback() {
                @Override
                protected void onProgress(Contact contact) {
                    add(GROUP_SUGGESTIONS, contact, true, true);
                }

                @Override
                protected void onFinish(List<Contact> contacts) {
                    adapter.setGroupProgress(GROUP_SUGGESTIONS, false);
                }
            },
            refresh
        );
    }

    /**
     * Handle contacts retrieved from the Contacts Manager.
     *
     * @param contacts The list of contacts.
     * @param filter The query used to filter contacts.
     */
    public void onContactsFinish(List<Contact> contacts, String filter) {
        contactsMap.clear();
        recyclerView.scrollToPosition(0);
        // Convert Query into Terms
        String[] terms = null;
        if (filter != null && !filter.isEmpty()) {
            terms = Common.explode(" ", filter);
        }
        adapter.setHighlightTerms(terms);
        // Add Contact to Corresponding Group Category
        for (Contact contact : contacts) {
            // Filter by Terms
            if (terms != null) {
                String target = contact.displayName;
                if (contact.emails != null) {
                    for (String email : contact.emails.values()) {
                        target += " " + email;
                    }
                }

                if (!Common.containsAll(target, terms)) {
                    continue;
                }
            }
            // Determine Contact Group
            int group = -1;

            if (contact.isFavorite) {
                group = GROUP_FAVORITES;
            } else if (contact.isFriend) {
                group = GROUP_FRIENDS;
            } else if (contact.isSuggested == Contact.SUGGESTION_PENDING) {
                group = GROUP_SUGGESTIONS;
            } else if (contact.type == Contact.TYPE_USER) {
                group = GROUP_USERS;
            } else if (contact.type == Contact.TYPE_CONTACT) {
                group = contact.visible ? GROUP_CONTACTS : GROUP_OTHER;
            }
            // Add Contact to Group
            if (group != -1) {
                add(group, contact, false, false);
            }
        }

        contactsMap.sortAll();

        resultsText.setVisibility(!contactsMap.isEmpty() ? View.GONE : View.VISIBLE);

        adapter.setShowEmptyLabels(filter == null, true);
    }

    /**
     * Retrieve an adapter item for the adapter to use to display the contact.
     *
     * @param group The group category.
     * @param position The position in the list.
     * @return an adapter item representing the contact.
     */
    public ContactItem getItemFromSource(int group, int position) {
        ContactItem item = null;

        Contact contact = contactsMap.get(group, position);
        String id = contact.id + "." + contact.type;

        if (items.containsKey(id)) {
            item = items.get(id);
        } else {
            switch (group) {
                case GROUP_FAVORITES:
                case GROUP_FRIENDS:
                case GROUP_SUGGESTIONS:
                    item = createKnownUserItem(id, contact);
                    break;
                case GROUP_USERS:
                    item = createUserItem(id, contact);
                    break;
                case GROUP_CONTACTS:
                case GROUP_OTHER:
                    item = createContactItem(id, contact);
                    break;
            }

            if (item != null) {
                item.iconResId = R.drawable.ic_account_circle_48dp;
                item.iconColor = Colors.getColor(getContext(), R.color.colorPrimary);
                item.iconBytes = contact.photo;

                items.put(id, item);
            }
        }

        return item;
    }

    /**
     * Create a contact adapter item using the given contact.
     *
     * @param id The value of the contact ID.
     * @param contact The instance of the contact.
     * @return an adapter item representing the contact.
     */
    private ContactItem createContactItem(String id, Contact contact) {
        ContactItem item = new ContactItem(id);
        item.name = contact.displayName;

        String msg = null;

        if (contact.hasEmails()) {
            msg = Common.implode("\n", contact.getEmails());
        } else if (contact.hasPhones()) {
            msg = Common.implode("\n", contact.getFormattedPhones());
        }

        item.msg = msg;
        item.msgColor = Colors.getColor(getContext(), R.color.colorPrimary);

        return item;
    }

    /**
     * Create a favorite, friend, or suggested user adapter item using the given contact.
     *
     * @param id The value of the contact ID.
     * @param contact The instance of the contact.
     * @return an adapter item representing the contact.
     */
    private ContactItem createKnownUserItem(String id, Contact contact) {
        ContactItem item = new ContactItem(id);
        item.name = String.format("%s %s", contact.firstName, contact.lastName);

        String msg = null;

        if (contact.hasEmails()) {
            msg = contact.getEmail();
        } else if (contact.hasPhones()) {
            msg = contact.getFormattedPhone();
        }

        item.msg = msg;
        item.msgColor = Colors.getColor(getContext(), R.color.gray);

        return item;
    }

    /**
     * Create a user adapter item using the given contact.
     *
     * @param id The value of the contact ID.
     * @param contact The instance of the contact.
     * @return an adapter item representing the contact.
     */
    private ContactItem createUserItem(String id, Contact contact) {
        ContactItem item = new ContactItem(id);
        item.name = String.format("%s %s", contact.firstName, contact.lastName);

        item.msg = contact.displayName;
        item.msgColor = Colors.getColor(getContext(), R.color.gray);

        return item;
    }

    /**
     * Handle the click action generated from the contacts dialog.
     *
     * @param view The view of the contact item.
     * @param group The group category.
     * @param position The position in the list.
     */
    @Override
    public void onClick(View view, int group, int position) {
        Contact contact = contactsMap.get(group, position);

        switch (mode) {
            case ContactsActivity.MODE_PICKER:
                onPickerClick(contact);
                break;
            default:
                onViewClick(contact, group);
                break;
        }
    }

    /**
     * Display the contacts dialog to perform additional actions.
     *
     * @param contact The selected contact form the list.
     * @param group The group category.
     * @return a boolean of the status.
     */
    private boolean onViewClick(Contact contact, int group) {
        if (dialog == null || !dialog.isShowing()) {
            dialog = ContactsDialog.create(this, contact, group);
            dialog.show();
        }

        return false;
    }

    /**
     * Return the selected contact from this activity.
     *
     * @param contact The selected contact from the list.
     */
    private void onPickerClick(Contact contact) {
        ArrayList<Contact> result = new ArrayList<>();
        result.add(contact);

        Intent data = new Intent();
        data.putParcelableArrayListExtra(ContactsActivity.EXTRA_CONTACTS, result);

        getActivity().setResult(Activity.RESULT_OK, data);
        getActivity().finish();
    }

    /**
     * Handle contact addition being reported by the Contacts Manager.
     *
     * @param contact The instance of the contact.
     */
    @Override
    public void onContactAdd(Contact contact) {
        add(GROUP_CONTACTS, contact, true, true);
    }

    /**
     * Handle contact updates being reported by the Contacts Manager.
     *
     * @param contact The instance of the contact.
     */
    @Override
    public void onContactRefresh(Contact contact) {

    }

    /**
     * Handle contact removal being reported by the Contacts Manager.
     *
     * @param contact The instance of the contact.
     */
    @Override
    public void onContactRemove(Contact contact) {
        remove(contact, true, true);
    }

    /**
     * Handle suggestions being reported by the Contacts Manager.
     *
     * @param contact The instance of the contact.
     */
    @Override
    public void onSuggestionAdd(Contact contact) {
        add(GROUP_SUGGESTIONS, contact, true, true);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getContext(), "New Suggestion", Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Add contact to the specified group and notify the adapter to reflect the new addition.
     *
     * @param group The group category.
     * @param contact The contact to be added.
     * @param sort The value to sort contacts.
     * @param notify The value to notify adapter to refresh.
     */
    public void add(int group, Contact contact, boolean sort, boolean notify) {
        int index = contactsMap.indexOf(contact);
        final int fromPosition = index >= 0 ? adapter.getAdapterPosition(index) : -1;

        contactsMap.remove(contact);
        contactsMap.add(group, contact);

        if (sort) {
            contactsMap.sort(group);
        }

        if (notify) {
            final int position = adapter.getAdapterPosition(contactsMap.indexOf(contact));

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (fromPosition >= 0) {
                        adapter.notifyMoved(fromPosition, position);
                    } else {
                        adapter.notifyInserted(position);
                    }
                }
            });
        }
    }

    /**
     * Remove contact and notify the adapter to reflect the change.
     *
     * @param contact The contact to be removed.
     * @param sort The value to sort contacts.
     * @param notify The value to notify adapter to refresh.
     */
    public void remove(Contact contact, boolean sort, boolean notify) {
        int index = contactsMap.indexOf(contact);
        if (index < 0) {
            return;
        }

        index = adapter.getAdapterPosition(index);
        contactsMap.remove(contact);

        if (sort) {
            contactsMap.sortAll();
        }

        if (notify) {
            final int position = index;

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyRemoved(position);
                }
            });
        }
    }

    private void removeUsersCallback() {
        if (usersCallback != null) {
            contactsManager.removeUsersCallback(usersCallback);
            usersCallback = null;
        }
    }

    private void removeContactsCallback() {
        if (contactsCallback != null) {
            contactsManager.removeContactsCallback(contactsCallback);
            contactsCallback = null;
        }
    }

    private void removeSuggestionsCallback() {
        if (suggestionsCallback != null) {
            contactsManager.removeSuggestionsCallback(suggestionsCallback);
            suggestionsCallback = null;
        }
    }
}
