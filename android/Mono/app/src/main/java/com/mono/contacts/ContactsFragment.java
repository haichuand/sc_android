package com.mono.contacts;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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
import java.util.LinkedList;
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

    private static final int REFRESH_LIMIT = 30;
    private static final int REFRESH_OFFSET = 5;
    private static final int SEARCH_LIMIT = 30;

    private EditText search;
    private RecyclerView recyclerView;
    private SimpleLinearLayoutManager layoutManager;
    private ContactsAdapter adapter;
    private TextView resultsText;
    private AlertDialog dialog;

    private int[][] groups;
    private int groupPosition;

    private final Map<String, ContactItem> items = new HashMap<>();
    private ContactsMap contactsMap = new ContactsMap();

    private int mode;
    private String actionBarTitle;

    private ContactsManager contactsManager;

    private AsyncTask task;
    private int groupOffset;
    private String currentQuery;
    private String[] terms;

    private Toast toast;

    protected List<Contact> contactSelections = new LinkedList<>();

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

        if (mode == ContactsActivity.MODE_PICKER) {
            actionBarTitle = getString(R.string.select_contacts);
        } else {
            actionBarTitle = getString(R.string.contacts);
        }

        setToolbarTitle(actionBarTitle);

        // Initialize Group Categories
        groups = new int[][]{
            {GROUP_FAVORITES, R.string.favorites},
            {GROUP_FRIENDS, R.string.friends},
            {GROUP_SUGGESTIONS, R.string.suggestions},
            {GROUP_USERS, R.string.users},
            {GROUP_CONTACTS, R.string.local_contacts},
            {GROUP_OTHER, R.string.other_contacts}
        };
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
                currentQuery = !str.isEmpty() ? str : null;
                onRefresh();
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

        if (mode == ContactsActivity.MODE_PICKER) {
            adapter.setSelectable(true);
        }

        resultsText = (TextView) view.findViewById(R.id.results_text);

        return view;
    }

    /**
     * Initialize adapter groups.
     */
    private void initialize() {
        adapter.removeGroups();

        for (int[] element : groups) {
            final int group = element[0];
            int labelResId = element[1];

            int count = 0;
            int type = 0;

            switch (group) {
                case GROUP_FAVORITES:
                    type = ContactsManager.TYPE_FAVORITES;
                    break;
                case GROUP_FRIENDS:
                    type = ContactsManager.TYPE_FRIENDS;
                    break;
                case GROUP_SUGGESTIONS:
                    type = ContactsManager.TYPE_SUGGESTIONS;
                    break;
                case GROUP_USERS:
                    type = ContactsManager.TYPE_USERS;
                    break;
                case GROUP_CONTACTS:
                    type = ContactsManager.TYPE_CONTACTS;
                    break;
                case GROUP_OTHER:
                    type = ContactsManager.TYPE_OTHERS;
                    break;
            }

            if (type != 0) {
                // Initial Estimated Number of Contacts
                count = contactsManager.getContactsCount(new int[]{type}, terms);
            }

            adapter.add(new ContactsAdapter.ContactsGroup(group, labelResId, count,
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
    }

    @Override
    public void onResume() {
        super.onResume();

        if (contactsMap.isEmpty()) {
            onRefresh();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (task != null) {
            task.cancel(true);
        }

        contactsManager.removeListener(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mode == ContactsActivity.MODE_PICKER) {
            inflater.inflate(R.menu.contacts_picker, menu);
        } else {
            inflater.inflate(R.menu.contacts, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_done:
                return onPickerDone();
            case R.id.action_clear:
                return onClearSelections();
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
     * Return from this activity with the selected contacts.
     *
     * @return whether action was consumed.
     */
    public boolean onPickerDone() {
        if (contactSelections.isEmpty()) {
            return true;
        }

        ArrayList<Contact> result = new ArrayList<>();
        result.addAll(contactSelections);

        Intent data = new Intent();
        data.putParcelableArrayListExtra(ContactsActivity.EXTRA_CONTACTS, result);

        getActivity().setResult(Activity.RESULT_OK, data);
        getActivity().finish();

        return true;
    }

    /**
     * Clears current selected contacts during picker mode.
     *
     * @return whether action was consumed.
     */
    public boolean onClearSelections() {
        contactSelections.clear();
        adapter.setSelectable(true);
        setToolbarTitle(actionBarTitle);

        return true;
    }

    /**
     * Clears the current contacts and retrieve a new set of contacts.
     *
     * @return whether action was consumed.
     */
    public boolean onRefresh() {
        // Cancel Existing Task
        if (task != null) {
            task.cancel(true);
        }
        // Clear Cache
        items.clear();
        contactsMap.clear();
        // Clear Offsets
        groupPosition = 0;
        groupOffset = 0;
        // Reset Scroll Position to Top
        recyclerView.scrollToPosition(0);
        // Convert Query into Terms
        terms = !Common.isEmpty(currentQuery) ? Common.explode(" ", currentQuery) : null;
        adapter.setHighlightTerms(terms);
        // Initialize Adapter Groups
        initialize();
        // Show or Hide Labels
        adapter.setHideEmptyLabels(currentQuery != null, true);
        // Hide Results Text
        resultsText.setVisibility(View.GONE);
        // Retrieve Contacts
        getContacts();

        return true;
    }

    /**
     * Set the action bar title using a string.
     *
     * @param title String containing the title.
     */
    private void setToolbarTitle(CharSequence title) {
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }

    /**
     * Retrieve group type by position.
     *
     * @param position Position of group.
     * @return group type.
     */
    public int getGroupByPosition(int position) {
        return groups[position][0];
    }

    /**
     * Retrieve group position.
     *
     * @param group Group type.
     * @return group position.
     */
    public int getGroupPosition(int group) {
        int position = -1;

        for (int i = 0; i < groups.length; i++) {
            if (groups[i][0] == group) {
                position = i;
                break;
            }
        }

        return position;
    }

    /**
     * Retrieve contacts from the Contacts Manager using callbacks.
     */
    public void getContacts() {
        // Reached End of Contacts -> Return
        if (groupPosition == -1) {
            return;
        }
        // Limit Contacts Per Retrieval
        final int limit = !Common.isEmpty(currentQuery) ? SEARCH_LIMIT : REFRESH_LIMIT;
        // Callback to Process Retrieved Contacts
        ContactsTaskCallback callback = new ContactsTaskCallback() {
            @Override
            protected void onFinish(List<Contact> contacts) {
                boolean forceRetrieve = false;
                int group = getGroupByPosition(groupPosition);

                if (!contacts.isEmpty()) {
                    int offset = 0;

                    int count = 0, skipped = 0;
                    // Process Contacts
                    for (Contact contact : contacts) {
                        if (contactsMap.indexOf(contact) == -1 && (contact.hasEmails() ||
                                contact.hasPhones())) {
                            contactsMap.add(group, contact);

                            if (count == 0) {
                                offset = contactsMap.indexOf(contact);
                                offset = adapter.getAdapterPosition(offset);
                            }

                            count++;
                        } else {
                            skipped++;
                        }
                    }
                    // Refresh UI
                    if (count > 0) {
                        adapter.notifyRangeInserted(offset, count);
                    }
                    // Refresh Count
                    if (skipped > 0) {
                        ContactsAdapter.ContactsGroup tempGroup = adapter.getGroup(groupPosition);
                        adapter.setGroupCount(groupPosition, tempGroup.getInitialCount() - skipped);
                    }
                    // Remember Offset for Next Iteration
                    groupOffset += contacts.size();
                    // Force Next Iteration Due to Insufficient Contacts
                    if (contacts.size() < limit) {
                        forceRetrieve = true;
                    }
                } else if (groupPosition < groups.length - 1) {
                    // Update Actual Group Size
                    ContactsAdapter.ContactsGroup tempGroup = adapter.getGroup(groupPosition);
                    adapter.setGroupCount(groupPosition, contactsMap.size(group));
                    // Move to Next Group
                    groupPosition++;
                    groupOffset = 0;
                    // Start Next Iteration
                    forceRetrieve = true;
                } else {
                    // Reached End of Contacts
                    groupPosition = -1;
                    // Handle Empty Message
                    handleEmptyMessage();
                }
                // Remove Task Reference
                task = null;
                // Force Next Iteration
                if (forceRetrieve) {
                    getContacts();
                }
            }
        };
        // Determine Contact Type Using Group Type
        int type = 0;

        switch (getGroupByPosition(groupPosition)) {
            case GROUP_FAVORITES:
                type = ContactsManager.TYPE_FAVORITES;
                break;
            case GROUP_FRIENDS:
                type = ContactsManager.TYPE_FRIENDS;
                break;
            case GROUP_SUGGESTIONS:
                type = ContactsManager.TYPE_SUGGESTIONS;
                break;
            case GROUP_USERS:
                type = ContactsManager.TYPE_USERS;
                break;
            case GROUP_CONTACTS:
                type = ContactsManager.TYPE_CONTACTS;
                break;
            case GROUP_OTHER:
                type = ContactsManager.TYPE_OTHERS;
                break;
        }
        // Retrieve Contacts
        if (type != 0) {
            task = contactsManager.getContactsAsync(new int[]{type}, terms, groupOffset,
                limit, callback);
        }
    }

    /**
     * Handle scrolling to enable dynamic retrieval of contacts in chunks.
     *
     * @param deltaY Direction of vertical scrolling.
     */
    public void handleInfiniteScroll(int deltaY) {
        if (task != null) {
            return;
        }

        if (deltaY > 0) {
            int position = layoutManager.findLastVisibleItemPosition();

            if (position >= Math.max(contactsMap.size() - 1 - REFRESH_OFFSET, 0)) {
                getContacts();
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
     * Handle the action of selecting a contact from the list.
     *
     * @param view View of the event.
     * @param group Group category.
     * @param position Position in the list.
     * @param value Contact is selected or unselected.
     */
    @Override
    public void onSelectClick(View view, int group, int position, boolean value) {
        ContactItem item = getItemFromSource(group, position);
        if (item == null) {
            return;
        }

        adapter.setSelected(item.id, value);

        Contact contact = contactsMap.get(group, position);

        if (value) {
            if (!contactSelections.contains(contact)) {
                contactSelections.add(contact);
            }
        } else {
            contactSelections.remove(contact);
        }

        refreshActionBar();
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
     * Display the number of contacts selected in the action bar.
     */
    private void refreshActionBar() {
        if (contactSelections.isEmpty()) {
            setToolbarTitle(actionBarTitle);
        } else {
            setToolbarTitle(getString(R.string.value_selected, contactSelections.size()));
        }
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

        if (toast != null) {
            toast.cancel();
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                toast = Toast.makeText(getContext(), "New Suggestion", Toast.LENGTH_LONG);
                toast.show();
            }
        });
    }

    /**
     * Handle suggestions removal.
     *
     * @param contact Contact to remove.
     */
    @Override
    public void onSuggestionRemove(Contact contact) {
        add(GROUP_USERS, contact, true);
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

            int groupPosition = contactsMap.getGroupPosition(contact);
            contactsMap.remove(contact);
            // Update Group Count
            ContactsAdapter.ContactsGroup tempGroup = adapter.getGroup(groupPosition);
            adapter.setGroupCount(groupPosition, tempGroup.getInitialCount() - 1);
        }

        contactsMap.add(group, contact);
        // Update Group Count
        ContactsAdapter.ContactsGroup tempGroup = adapter.getGroup(getGroupPosition(group));
        adapter.setGroupCount(getGroupPosition(group), tempGroup.getInitialCount() + 1);

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

        int groupPosition = contactsMap.getGroupPosition(contact);
        contactsMap.remove(contact);
        // Update Group Count
        ContactsAdapter.ContactsGroup tempGroup = adapter.getGroup(groupPosition);
        adapter.setGroupCount(groupPosition, tempGroup.getInitialCount() - 1);

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
}
