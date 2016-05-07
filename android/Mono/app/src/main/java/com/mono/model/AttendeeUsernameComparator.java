package com.mono.model;

import java.util.Comparator;

public class AttendeeUsernameComparator implements Comparator<Attendee> {

    @Override
    public int compare(Attendee attendee1, Attendee attendee2) {
        return attendee1.toString().compareToIgnoreCase(attendee2.toString());
    }
}
