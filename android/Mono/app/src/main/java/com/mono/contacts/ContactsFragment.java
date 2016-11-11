package com.mono.contacts;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
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
import com.mono.util.OnBackPressedListener;
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
public class ContactsFragment extends Fragment implements OnBackPressedListener,
        ContactsAdapterListener, ContactsBroadcastListener {

    public static final int GROUP_FAVORITES = 0;
    public static final int GROUP_FRIENDS = 1;
    public static final int GROUP_SUGGESTIONS = 2;
    public static final int GROUP_USERS = 3;
    public static final int GROUP_CONTACTS = 4;
    public static final int GROUP_OTHER = 5;

    public static final int TASK_USERS = 0;
    public static final int TASK_CONTACTS = 1;

    private static final int REFRESH_LIMIT = 30;
    private static final int REFRESH_OFFSET = 5;
    private static final int SEARCH_LIMIT = 30;

    private EditText search;
    private RecyclerView recyclerView;
    private SimpleLinearLayoutManager layoutManager;
    private ContactsAdapter adapter;
    private TextView resultsText;
    private AlertDialog dialog;

    private final Map<String, ContactItem> items = new HashMap<>();
    private ContactsMap contactsMap = new ContactsMap();

    private int mode;

    private ContactsManager contactsManager;

    private final Map<Integer, AsyncTask> tasks = new HashMap<>();
    private int othersOffset;
    private String[] terms;

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
                String str = s.toString().trim();
                getContacts(!str.isEmpty() ? str : null, 0);
            }
        });

        recyclerView = (RecyclerView) view.findViewById(R.id.list);
        recyclerView.setLayoutManager(layoutManager = new SimpleLinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter = new ContactsAdapter(this));
        recyclerView.addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                handleInfiniteScroll(dy);
            }
        });
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
    public void onResume() {
        super.onResume();

        if (contactsMap.isEmpty()) {
            getContacts(null, 0);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        clearTasks();
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

    @Override
    public boolean onBackPressed() {
        if (search.hasFocus() && search.getText().length() > 0) {
            search.setText("");
            search.clearFocus();
            return true;
        }

        return false;
    }

    /**
     * Clears the current contacts and retrieve a new set of contacts.
     *
     * @return a boolean of the status.
     */
    public boolean onRefresh() {
        // Retrieve Users and Contacts
        getContacts(null, 0);

        return true;
    }

    /**
     * Retrieve contacts from the Contacts Manager using callbacks.
     *
     * @param query Search query.
     * @param offset Offset of contacts to start with.
     */
    public void getContacts(final String query, final int offset) {
        // Cancel Existing Tasks
        clearTasks();
        // Clear Cache
        items.clear();
        contactsMap.clear();
        othersOffset = 0;
        // Reset Scroll Position to Top
        recyclerView.scrollToPosition(0);
        // Show or Hide Labels
        adapter.setHideEmptyLabels(query != null, true);
        // Convert Query into Terms
        terms = !Common.isEmpty(query) ? Common.explode(" ", query) : null;
        adapter.setHighlightTerms(terms);
        // Retrieve Important Contacts First
        int[] types = {
            ContactsManager.TYPE_USERS,
            ContactsManager.TYPE_CONTACTS
        };
        // Search Limit
        final int limit = !Common.isEmpty(query) ? SEARCH_LIMIT : 0;
        // Start Retrieval
        setTask(TASK_USERS, contactsManager.getContactsAsync(types, terms, 0, limit,
            new ContactsTaskCallback() {
                @Override
                protected void onFinish(List<Contact> contacts) {
                    if (!contacts.isEmpty()) {
                        addAll(contacts, true);
                    }

                    getOtherContacts(query, 0);

                    removeTask(TASK_USERS);
                }
            }
        ));
    }

    /**
     * Retrieve other contacts from the Contacts Manager using callbacks.
     *
     * @param query Search query.
     * @param offset Offset of contacts to start with.
     */
    public void getOtherContacts(String query, int offset) {
        final int limit = !Common.isEmpty(query) ? SEARCH_LIMIT : REFRESH_LIMIT;

        setTask(TASK_CONTACTS, contactsManager.getOtherContactsAsync(terms, offset, limit,
            new ContactsTaskCallback() {
                @Override
                protected void onFinish(List<Contact> contacts) {
                    if (!contacts.isEmpty()) {
                        int offset = adapter.getItemCount();
                        int count = 0;

                        for (Contact contact : contacts) {
                            if (contactsMap.indexOf(contact) == -1 && (contact.hasEmails() ||
                                    contact.hasPhones())) {
                                contactsMap.add(GROUP_OTHER, contact);
                                count++;
                            }
                        }

                        if (count > 0) {
                            adapter.notifyItemRangeInserted(offset, count);
                        }

                        othersOffset += contacts.size();
                    }

                    // Handle Empty Message
                    handleEmptyMessage();

                    removeTask(TASK_CONTACTS);
                }
            }
        ));
    }

    /**
     * Handle scrolling to enable dynamic retrieval of contacts in chunks.
     *
     * @param deltaY Direction of vertical scrolling.
     */
    public void handleInfiniteScroll(int deltaY) {
        if (!tasks.isEmpty()) {
            return;
        }

        int position;

        if (deltaY > 0) {
            position = layoutManager.findLastVisibleItemPosition();
            if (position >= Math.max(contactsMap.size() - 1 - REFRESH_OFFSET, 0)) {
                getOtherContacts(null, othersOffset);
            }
        }
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
        add(GROUP_CONTACTS, contact, true);
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
        add(GROUP_SUGGESTIONS, contact, true);

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
     * @param notify The value to notify adapter to refresh.
     */
    public void add(int group, Contact contact, boolean notify) {
        int index = contactsMap.indexOf(contact);

        if (index >= 0) {
            Contact current = contactsMap.get(index);
            index = adapter.getAdapterPosition(index);

            if (current.type == Contact.TYPE_USER && contact.type == Contact.TYPE_CONTACT) {
                return;
            }

            contactsMap.remove(contact);
        }

        contactsMap.add(group, contact);

        if (notify) {
            // Sort Contact Group
            sort(group);
            // Handle Empty Message
            handleEmptyMessage();

            final int fromPosition = index;
            final int toPosition = adapter.getAdapterPosition(contactsMap.indexOf(contact));

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (fromPosition >= 0) {
                        adapter.notifyMoved(fromPosition, toPosition);
                    } else {
                        adapter.notifyInserted(toPosition);
                    }
                }
            });
        }
    }

    /**
     * Add multiple contacts to the specified group and notify the adapter to reflect the new
     * additions.
     *
     * @param contacts The contacts to be added.
     * @param notify The value to notify adapter to refresh.
     */
    public void addAll(List<Contact> contacts, boolean notify) {
        for (Contact contact : contacts) {
            int group = -1;
            // Determine Contact Group
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

            if (group != -1) {
                // Add Contact to Group
                add(group, contact, false);
            }
        }

        if (notify) {
            // Sort Contacts
            sortAll();
            // Handle Empty Message
            handleEmptyMessage();
            // Update UI
            adapter.notifyDataSetChanged();
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
            sortAll();
        }

        if (notify) {
            // Handle Empty Message
            handleEmptyMessage();

            final int position = index;

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyRemoved(position);
                }
            });
        }
    }

    public void handleEmptyMessage() {
        resultsText.setVisibility(!contactsMap.isEmpty() ? View.GONE : View.VISIBLE);
    }

    public void sort(int group) {
        contactsMap.sort(group, terms != null && terms.length > 0 ? terms[0] : null);
    }

    public void sortAll() {
        contactsMap.sortAll(terms != null && terms.length > 0 ? terms[0] : null);
    }

    private void setTask(int type, AsyncTask task) {
        removeTask(type);
        tasks.put(type, task);
    }

    private void removeTask(int type) {
        if (tasks.containsKey(type)) {
            tasks.remove(type).cancel(true);
        }
    }

    private void clearTasks() {
        for (AsyncTask task : tasks.values()) {
            task.cancel(true);
        }
        tasks.clear();
    }
}
