package com.mono.contacts;

import com.mono.model.Contact;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is used to store contacts by group categories using a combination of a HashMap and
 * ArrayLists.
 *
 * @author Gary Ng
 */
public class ContactsMap {

    private static final Comparator<Contact> COMPARATOR = new Comparator<Contact>() {
        @Override
        public int compare(Contact c1, Contact c2) {
            return c1.displayName.compareToIgnoreCase(c2.displayName);
        }
    };

    private Map<Integer, List<Contact>> map = new LinkedHashMap<>();

    public boolean add(int group, Contact contact) {
        return getContacts(group).add(contact);
    }

    public void clear() {
        map.clear();
    }

    public Contact get(int position) {
        Contact contact = null;

        int size = 0, index = 0;
        for (int key : map.keySet()) {
            List<Contact> contacts = getContacts(key);
            size += contacts.size();

            if (position < size) {
                contact = contacts.get(position - index);
                break;
            }

            index += contacts.size();
        }

        return contact;
    }

    public Contact get(int group, int position) {
        return getContacts(group).get(position);
    }

    public List<Contact> getContacts(int group) {
        if (!map.containsKey(group)) {
            map.put(group, new ArrayList<Contact>());
        }

        return map.get(group);
    }

    public int getGroupPosition(Contact contact) {
        List<Integer> keys = new ArrayList<>(map.keySet());

        for (int key : keys) {
            if (map.get(key).contains(contact)) {
                return keys.indexOf(key);
            }
        }

        return -1;
    }

    public int indexOf(Contact contact) {
        int position = -1;

        int index = 0;
        for (int key : map.keySet()) {
            List<Contact> contacts = getContacts(key);

            position = contacts.indexOf(contact);
            if (position >= 0) {
                position = index + contacts.indexOf(contact);
                break;
            }

            index += contacts.size();
        }

        return position;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public int remove(Contact contact) {
        int position = -1;

        int index = 0;
        for (int key : map.keySet()) {
            List<Contact> contacts = getContacts(key);

            position = contacts.indexOf(contact);
            if (position >= 0) {
                contacts.remove(contact);
                position = index + position;
                break;
            }

            index += contacts.size();
        }

        return position;
    }

    public int size() {
        int size = 0;

        for (List<Contact> value : map.values()) {
            size += value.size();
        }

        return size;
    }

    public int size(int group) {
        return getContacts(group).size();
    }

    public void sort(int group, String startsWith) {
        sort(group, COMPARATOR, startsWith);
    }

    public void sort(int group, Comparator<Contact> comparator, String startsWith) {
        List<Contact> contacts = getContacts(group);
        Collections.sort(contacts, comparator);
        // Prioritize Prefix
        if (startsWith != null) {
            startsWith = startsWith.trim();

            if (!startsWith.isEmpty()) {
                startsWith = startsWith.toLowerCase();

                List<Contact> result = new ArrayList<>();
                Iterator<Contact> iterator = contacts.iterator();

                while (iterator.hasNext()) {
                    Contact contact = iterator.next();

                    if (contact.displayName.toLowerCase().startsWith(startsWith)) {
                        result.add(contact);
                        iterator.remove();
                    } else if (!result.isEmpty()) {
                        break;
                    }
                }

                if (!result.isEmpty()) {
                    contacts.addAll(0, result);
                }
            }
        }
    }

    public void sortAll(String startsWith) {
        for (int group : map.keySet()) {
            sort(group, COMPARATOR, startsWith);
        }
    }
}
