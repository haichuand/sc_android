package com.mono.contacts;

import com.mono.model.Contact;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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

    private Map<Integer, List<Contact>> map = new HashMap<>();

    public boolean add(int group, Contact contact) {
        return get(group).add(contact);
    }

    public void clear() {
        map.clear();
    }

    public List<Contact> get(int group) {
        if (!map.containsKey(group)) {
            map.put(group, new ArrayList<Contact>());
        }

        return map.get(group);
    }

    public Contact get(int group, int position) {
        return get(group).get(position);
    }

    public int indexOf(Contact contact) {
        int position = -1;

        List<Integer> keys = new ArrayList<>(map.keySet());
        Collections.sort(keys);

        int index = 0;
        for (int key : keys) {
            List<Contact> contacts = get(key);

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

        List<Integer> keys = new ArrayList<>(map.keySet());
        Collections.sort(keys);

        int index = 0;
        for (int key : keys) {
            List<Contact> contacts = get(key);

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
        return get(group).size();
    }

    public void sort(int group) {
        sort(group, COMPARATOR);
    }

    public void sort(int group, Comparator<Contact> comparator) {
        Collections.sort(get(group), comparator);
    }

    public void sortAll() {
        sortAll(COMPARATOR);
    }

    public void sortAll(Comparator<Contact> comparator) {
        for (int key : map.keySet()) {
            sort(key, comparator);
        }
    }
}
